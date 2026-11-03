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
        val yy = SimpleDateFormat("yy", Locale.US).format(Date(now))
        val pattern = "$prefix-$cityCode-$yy%"

        val existing = visitorDao.getRecentPassNumbers(pattern)
        var maxExistingSeq = 0
        for (pass in existing) {
            val seqStr = pass.takeLast(4)
            val parsed = seqStr.toIntOrNull() ?: 0
            if (parsed > maxExistingSeq) {
                maxExistingSeq = parsed
            }
        }

        val prefsSeq = kioskPrefs?.getNextSequence(prefix, cityCode, yy) ?: (maxExistingSeq + 1)
        val nextSeq = maxOf(prefsSeq, maxExistingSeq + 1)
        val formattedSeq = String.format(Locale.US, "%04d", nextSeq)
        val passNumber = "$prefix-$cityCode-$yy$formattedSeq"

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

    suspend fun upsertRemoteVisitor(remote: VisitorEntity) {
        val existing = visitorDao.getVisitorByPassNumber(remote.passNumber)
        if (existing != null) {
            visitorDao.updateVisitor(
                remote.copy(
                    id = existing.id,
                    isSyncedToGoogleForm = true
                )
            )
        } else {
            visitorDao.insertVisitor(remote.copy(id = 0, isSyncedToGoogleForm = true))
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
