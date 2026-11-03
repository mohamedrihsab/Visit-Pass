package com.example.repository

import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.data.FirestoreService
import com.example.data.KioskPreferences
import com.example.data.VisitorDao
import com.example.data.VisitorEntity
import com.example.util.GoogleDriveExportHelper
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VisitorRepository(
    private val visitorDao: VisitorDao,
    private val firestoreService: FirestoreService,
    private val kioskPrefs: KioskPreferences? = null
) {

    val allVisitors: Flow<List<VisitorEntity>> = visitorDao.getAllVisitors()
    val unsyncedCount: Flow<Int> = visitorDao.getUnsyncedCount()
    val syncedCount: Flow<Int> = visitorDao.getSyncedCount()
    val activeCount: Flow<Int> = visitorDao.getActiveCount()

    fun resolvePrefixAndCity(branch: Branch): Pair<String, String> {
        val shortCode = branch.shortCode.trim()
        val parts = shortCode.split("-")
        val prefix = if (parts.isNotEmpty() && parts[0].isNotBlank()) parts[0] else "SAIT"
        val cityCode = if (parts.size > 1 && parts[1].isNotBlank()) {
            parts[1]
        } else {
            when (branch.city.lowercase(Locale.US)) {
                "dubai" -> "DXB"
                "abu dhabi" -> "AUH"
                "ras al khaimah" -> "RAK"
                "sharjah" -> "SHJ"
                else -> branch.city.filter { it.isLetter() }.take(3).uppercase(Locale.US).ifBlank { "DXB" }
            }
        }
        return Pair(prefix.uppercase(Locale.US), cityCode.uppercase(Locale.US))
    }

    suspend fun allocateSequentialPassNumber(branch: Branch): String {
        val now = System.currentTimeMillis()
        val (prefix, cityCode) = resolvePrefixAndCity(branch)
        val yy = SimpleDateFormat("yy", Locale.US).format(Date(now))
        val pattern = "$prefix-$cityCode-$yy%"

        val existing = visitorDao.getRecentPassNumbers(pattern)
        var maxExistingSeq = 0
        for (pass in existing) {
            val seqStr = pass.takeLast(4)
            val parsed = seqStr.toIntOrNull() ?: 0
            // Ignore legacy random numbers (>= 1000) so sequence stays clean and sequential
            if (parsed in 1..999 && parsed > maxExistingSeq) {
                maxExistingSeq = parsed
            }
        }

        val prefsSeq = kioskPrefs?.getNextSequence(prefix, cityCode, yy) ?: (maxExistingSeq + 1)
        val cleanPrefsSeq = if (prefsSeq >= 1000) (maxExistingSeq + 1) else prefsSeq
        val nextSeq = maxOf(cleanPrefsSeq, maxExistingSeq + 1)
        kioskPrefs?.setSequenceCounter(prefix, cityCode, yy, nextSeq)
        val formattedSeq = String.format(Locale.US, "%04d", nextSeq)
        return "$prefix-$cityCode-$yy$formattedSeq"
    }

    suspend fun registerVisitor(
        fullName: String,
        mobileNumber: String,
        companyName: String,
        visitType: String,
        department: String,
        purpose: String,
        customPurpose: String,
        personToMeet: String,
        email: String = "",
        branch: Branch = BranchRegistry.ALL_BRANCHES[1]
    ): Result<VisitorEntity> {
        val now = System.currentTimeMillis()
        val timeFormat = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val formattedTime = timeFormat.format(Date(now))

        val (prefix, cityCode) = resolvePrefixAndCity(branch)
        val passNumber = allocateSequentialPassNumber(branch)

        val visitor = VisitorEntity(
            passNumber = passNumber,
            branchName = branch.name,
            branchCode = "$prefix-$cityCode",
            fullName = fullName.trim(),
            mobileNumber = mobileNumber.trim(),
            companyName = companyName.trim(),
            visitType = visitType,
            department = department,
            purpose = if (purpose == "Other" && customPurpose.isNotBlank()) customPurpose.trim() else purpose,
            customPurpose = customPurpose.trim(),
            personToMeet = personToMeet.trim(),
            email = email.trim(),
            timestamp = now,
            checkInFormattedTime = formattedTime,
            status = "CHECKED_IN",
            isSyncedToGoogleForm = false
        )

        // 1. Save locally in Room
        val generatedId = visitorDao.insertVisitor(visitor)
        val savedVisitor = visitor.copy(id = generatedId)

        // 2. Broadcast immediately to Cloud Firestore for multi-device sync
        if (firestoreService.isAuthenticated) {
            val firestoreResult = firestoreService.saveVisitorToFirestore(savedVisitor)
            if (firestoreResult.isSuccess) {
                visitorDao.updateSyncStatus(generatedId, true)
                return Result.success(savedVisitor.copy(isSyncedToGoogleForm = true))
            }
        }

        return Result.success(savedVisitor)
    }

    suspend fun checkOutVisitor(id: Long) {
        val timeFormat = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val checkOutTime = timeFormat.format(Date())
        visitorDao.markCheckedOut(id, checkOutTime)

        val visitor = visitorDao.getVisitorById(id)
        if (visitor != null && firestoreService.isAuthenticated) {
            firestoreService.updateCheckOutInFirestore(visitor.passNumber, checkOutTime)
        }
    }

    data class RemoteUpsertResult(
        val visitor: VisitorEntity,
        val isNewlyInserted: Boolean,
        val obsoleteRemotePassNumber: String? = null
    )

    suspend fun cleanupDuplicateVisitors() {
        try {
            visitorDao.deduplicateByIdentityAndTimestamp()
            visitorDao.deduplicateByPassNumber()
            val allLocal = visitorDao.getAllVisitorsOnce()
            for (v in allLocal) {
                val sig = "${v.fullName.trim().lowercase(Locale.US)}|${v.mobileNumber.trim()}|${v.timestamp}"
                kioskPrefs?.markRemoteVisitorProcessed(v.passNumber, sig)
            }
            // Recalibrate sequence counters to actual highest valid sequential pass after deduplication
            val yy = SimpleDateFormat("yy", Locale.US).format(Date())
            val branches = kioskPrefs?.getBranches() ?: BranchRegistry.INITIAL_BRANCHES
            for (branch in branches) {
                val (prefix, cityCode) = resolvePrefixAndCity(branch)
                val pattern = "$prefix-$cityCode-$yy%"
                val existingPasses = visitorDao.getRecentPassNumbers(pattern)
                var maxSeq = 0
                for (pass in existingPasses) {
                    val seq = pass.takeLast(4).toIntOrNull() ?: 0
                    if (seq in 1..999 && seq > maxSeq) {
                        maxSeq = seq
                    }
                }
                kioskPrefs?.setSequenceCounter(prefix, cityCode, yy, maxSeq)
            }
            if (firestoreService.isAuthenticated) {
                firestoreService.deduplicateRemoteVisitors()
            }
        } catch (ignore: Exception) {}
    }

    suspend fun upsertRemoteVisitor(remote: VisitorEntity, fallbackBranch: Branch? = null): RemoteUpsertResult {
        // 1. Check if this visitor already exists in Room by passNumber OR by (fullName + mobileNumber + timestamp)
        val existing = visitorDao.findExistingVisitor(
            passNumber = remote.passNumber,
            timestamp = remote.timestamp,
            fullName = remote.fullName,
            mobileNumber = remote.mobileNumber
        )

        if (existing != null) {
            // Visitor already exists locally — never generate a new pass number or duplicate row!
            val obsoletePass = if (!existing.passNumber.equals(remote.passNumber, ignoreCase = true)) {
                remote.passNumber
            } else {
                null
            }
            val updated = existing.copy(
                status = remote.status,
                checkOutFormattedTime = remote.checkOutFormattedTime ?: existing.checkOutFormattedTime,
                isSyncedToGoogleForm = true
            )
            visitorDao.updateVisitor(updated)
            return RemoteUpsertResult(
                visitor = updated,
                isNewlyInserted = false,
                obsoleteRemotePassNumber = obsoletePass
            )
        }

        // 2. Genuinely new remote visitor: resolve canonical branch
        val branches = kioskPrefs?.getBranches() ?: BranchRegistry.INITIAL_BRANCHES
        val resolvedBranch = branches.find {
            it.shortCode.equals(remote.branchCode, ignoreCase = true) ||
            it.name.equals(remote.branchName, ignoreCase = true)
        } ?: fallbackBranch ?: branches.firstOrNull() ?: BranchRegistry.INITIAL_BRANCHES[1]

        val (prefix, cityCode) = resolvePrefixAndCity(resolvedBranch)
        val yy = SimpleDateFormat("yy", Locale.US).format(Date(remote.timestamp))
        val expectedPrefix = "$prefix-$cityCode-$yy"

        // Check if remote passNumber has a random/legacy sequence (>= 1000) or mismatched prefix
        val seqPart = remote.passNumber.takeLast(4).toIntOrNull() ?: 0
        val isCleanSequential = remote.passNumber.startsWith(expectedPrefix, ignoreCase = true) && seqPart in 1..999

        val finalPassNumber = if (isCleanSequential) {
            remote.passNumber.uppercase(Locale.US)
        } else {
            allocateSequentialPassNumber(resolvedBranch)
        }

        val obsoletePass = if (!finalPassNumber.equals(remote.passNumber, ignoreCase = true)) {
            remote.passNumber
        } else {
            null
        }

        val normalizedVisitor = remote.copy(
            id = 0,
            passNumber = finalPassNumber,
            branchName = resolvedBranch.name,
            branchCode = "$prefix-$cityCode",
            isSyncedToGoogleForm = true
        )

        val existingNormalized = visitorDao.getVisitorByPassNumber(finalPassNumber)
        return if (existingNormalized != null) {
            val saved = normalizedVisitor.copy(id = existingNormalized.id)
            visitorDao.updateVisitor(saved)
            RemoteUpsertResult(
                visitor = saved,
                isNewlyInserted = false,
                obsoleteRemotePassNumber = obsoletePass
            )
        } else {
            val newId = visitorDao.insertVisitor(normalizedVisitor)
            RemoteUpsertResult(
                visitor = normalizedVisitor.copy(id = newId),
                isNewlyInserted = true,
                obsoleteRemotePassNumber = obsoletePass
            )
        }
    }

    suspend fun syncAllPending(): Int {
        if (!firestoreService.isAuthenticated) return 0
        val pending = visitorDao.getUnsyncedVisitors()
        var count = 0
        for (v in pending) {
            val result = firestoreService.saveVisitorToFirestore(v)
            if (result.isSuccess) {
                visitorDao.updateSyncStatus(v.id, true)
                count++
            }
        }
        return count
    }

    suspend fun retrySync(visitor: VisitorEntity): Boolean {
        if (!firestoreService.isAuthenticated) return false
        val result = firestoreService.saveVisitorToFirestore(visitor)
        if (result.isSuccess) {
            visitorDao.updateSyncStatus(visitor.id, true)
            return true
        }
        return false
    }

    suspend fun deleteVisitor(id: Long): Boolean {
        val visitor = visitorDao.getVisitorById(id)
        visitorDao.deleteVisitor(id)
        if (visitor != null && firestoreService.isAuthenticated) {
            return firestoreService.deleteVisitorFromFirestore(visitor).isSuccess
        }
        return false
    }

    suspend fun deleteRemoteVisitorByPassNumber(passNumber: String) {
        if (passNumber.isNotBlank()) {
            visitorDao.deleteVisitorByPassNumber(passNumber)
        }
    }

    suspend fun clearAllVisitors(): Boolean {
        visitorDao.clearAllVisitors()
        if (firestoreService.isAuthenticated) {
            return firestoreService.clearAllVisitorsFromFirestore().isSuccess
        }
        return false
    }

    fun exportToCsv(visitors: List<VisitorEntity>): String {
        return GoogleDriveExportHelper.generateCsv(visitors)
    }
}
