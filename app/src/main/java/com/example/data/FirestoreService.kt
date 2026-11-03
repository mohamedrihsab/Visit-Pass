package com.example.data

import android.content.Context
import android.util.Log
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class FirestoreService(private val context: Context) {
    private val TAG = "FirestoreService"

    private val db: FirebaseFirestore by lazy {
        val dbId = context.getString(R.string.firestore_database_id)
        FirebaseFirestore.getInstance(dbId)
    }

    private val auth by lazy { Firebase.auth }

    val currentUserId: String?
        get() = auth.currentUser?.uid

    val isAuthenticated: Boolean
        get() = auth.currentUser != null

    private var realtimeListener: ListenerRegistration? = null
    private var orgSettingsListener: ListenerRegistration? = null

    suspend fun saveVisitorToFirestore(visitor: VisitorEntity): Result<String> = withContext(Dispatchers.IO) {
        val uid = currentUserId
        if (uid == null) {
            return@withContext Result.failure(IllegalStateException("Must be signed in with Google to sync with Firebase."))
        }

        try {
            val docId = if (visitor.passNumber.isNotBlank()) visitor.passNumber else "VIS_${visitor.id}_${visitor.timestamp}"
            val payload = hashMapOf<String, Any>(
                "passNumber" to visitor.passNumber,
                "branchName" to visitor.branchName,
                "branchCode" to visitor.branchCode,
                "fullName" to visitor.fullName,
                "mobileNumber" to visitor.mobileNumber,
                "companyName" to visitor.companyName,
                "visitType" to visitor.visitType,
                "department" to visitor.department,
                "purpose" to visitor.purpose,
                "personToMeet" to visitor.personToMeet,
                "email" to visitor.email,
                "status" to visitor.status,
                "checkInFormattedTime" to visitor.checkInFormattedTime,
                "userId" to uid,
                "timestamp" to visitor.timestamp,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )

            visitor.checkOutFormattedTime?.let {
                payload["checkOutFormattedTime"] = it
            }

            // 1. Write to central multi-branch & multi-device collection
            db.collection("visitors").document(docId).set(payload, SetOptions.merge()).await()

            // 2. Also write to user-scoped subcollection
            db.collection("users").document(uid).collection("visitors").document(docId).set(payload, SetOptions.merge()).await()

            Log.d(TAG, "Visitor successfully synced to multi-device Firestore: $docId")
            Result.success(docId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save visitor to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun deleteVisitorFromFirestore(visitor: VisitorEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val docId = if (visitor.passNumber.isNotBlank()) visitor.passNumber else "VIS_${visitor.id}_${visitor.timestamp}"

            // 1. Delete from central multi-branch & multi-device collection
            db.collection("visitors").document(docId).delete().await()

            // Also delete any document matching passNumber if stored under a different document ID
            if (visitor.passNumber.isNotBlank()) {
                try {
                    val matchingDocs = db.collection("visitors")
                        .whereEqualTo("passNumber", visitor.passNumber)
                        .get()
                        .await()
                    for (doc in matchingDocs.documents) {
                        if (doc.id != docId) {
                            doc.reference.delete().await()
                        }
                    }
                } catch (ignore: Exception) {}
            }

            // 2. Also delete from user-scoped subcollection
            try {
                db.collection("users").document(uid).collection("visitors").document(docId).delete().await()
            } catch (ignore: Exception) {}

            Log.d(TAG, "Visitor deleted from multi-device Firestore: $docId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete visitor from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllVisitorsFromFirestore(): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val centralDocs = db.collection("visitors").get().await()
            for (doc in centralDocs.documents) {
                try {
                    doc.reference.delete().await()
                } catch (ignore: Exception) {}
            }

            try {
                val userDocs = db.collection("users").document(uid).collection("visitors").get().await()
                for (doc in userDocs.documents) {
                    doc.reference.delete().await()
                }
            } catch (ignore: Exception) {}

            Log.d(TAG, "All visitor records cleared from Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear all visitors from Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun updateCheckOutInFirestore(passNumber: String, checkOutTime: String): Result<Unit> = withContext(Dispatchers.IO) {
        val uid = currentUserId ?: return@withContext Result.failure(IllegalStateException("Not authenticated"))
        try {
            val updateData = mapOf(
                "status" to "CHECKED_OUT",
                "checkOutFormattedTime" to checkOutTime,
                "updatedAt" to FieldValue.serverTimestamp()
            )

            db.collection("visitors").document(passNumber).update(updateData).await()

            try {
                db.collection("users").document(uid).collection("visitors").document(passNumber).update(updateData).await()
            } catch (ignore: Exception) {}

            Log.d(TAG, "Check-out synced to multi-device Firestore: $passNumber")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to update checkout in Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun saveOrganizationConfig(
        branches: List<Branch>,
        departmentEmails: Map<String, String>,
        activeBranchName: String? = null,
        adminPin: String? = null,
        receptionistPin: String? = null,
        autoResetSeconds: Int? = null,
        themeMode: String? = null,
        smtpConfig: SmtpConfig? = null,
        visitPurposes: List<String>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val uid = currentUserId ?: return@withContext Result.failure(
                IllegalStateException("Must be signed in with Firebase to sync admin settings")
            )
            val branchesList = branches.map { b ->
                val map = mutableMapOf<String, Any?>(
                    "id" to b.id,
                    "name" to b.name,
                    "shortCode" to b.shortCode,
                    "city" to b.city,
                    "subtitle" to b.subtitle,
                    "countryCode" to b.countryCode,
                    "facilityLocation" to b.facilityLocation,
                    "departments" to b.defaultDepartments,
                    "safetyGuidelines" to b.safetyGuidelines,
                    "safetyHelpline" to b.safetyHelpline,
                    "safetyContactName" to b.safetyContactName,
                    "logoUri" to b.logoUri,
                    "emergencyExitDrawingUri" to b.emergencyExitDrawingUri
                )
                map
            }

            val data = hashMapOf<String, Any>(
                "branches" to branchesList,
                "departmentEmails" to departmentEmails,
                "updatedByUid" to uid,
                "updatedByEmail" to (auth.currentUser?.email ?: "admin"),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            activeBranchName?.let { data["activeBranchName"] = it }
            adminPin?.let { data["adminPin"] = it }
            receptionistPin?.let { data["receptionistPin"] = it }
            autoResetSeconds?.let { data["autoResetSeconds"] = it }
            themeMode?.let { data["themeMode"] = it }
            visitPurposes?.let { data["visitPurposes"] = it }
            smtpConfig?.let { smtp ->
                data["smtpConfig"] = hashMapOf(
                    "enabled" to smtp.enabled,
                    "host" to smtp.host,
                    "port" to smtp.port,
                    "encryption" to smtp.encryption,
                    "username" to smtp.username,
                    "password" to smtp.password,
                    "senderEmail" to smtp.senderEmail,
                    "senderName" to smtp.senderName,
                    "firebaseTriggerEnabled" to smtp.firebaseTriggerEnabled
                )
            }

            db.collection("settings").document("organization").set(data, SetOptions.merge()).await()
            Log.d(TAG, "All organization & kiosk settings synced to Firestore")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save organization config to Firestore", e)
            Result.failure(e)
        }
    }

    suspend fun recordEmailNotification(
        visitorPassNumber: String,
        branchName: String,
        department: String,
        recipients: List<String>,
        subject: String,
        body: String,
        htmlBody: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val logData = hashMapOf<String, Any>(
                "passNumber" to visitorPassNumber,
                "branchName" to branchName,
                "department" to department,
                "recipients" to recipients,
                "subject" to subject,
                "body" to body,
                "status" to "DISPATCHED",
                "timestamp" to System.currentTimeMillis(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            htmlBody?.let { logData["html"] = it }
            currentUserId?.let { logData["senderUid"] = it }

            // 1. Audit trail in email_notifications
            db.collection("email_notifications").document().set(logData).await()

            // 2. Automated Firebase Trigger Email Extension collection (`mail`)
            val mailDoc = hashMapOf<String, Any>(
                "to" to recipients,
                "message" to hashMapOf<String, Any>(
                    "subject" to subject,
                    "text" to body,
                    "html" to (htmlBody ?: "<p>${body.replace("\n", "<br/>")}</p>")
                ),
                "createdAt" to FieldValue.serverTimestamp(),
                "source" to "SAIT_VISITOR_PASS_KIOSK",
                "passNumber" to visitorPassNumber,
                "branchName" to branchName,
                "department" to department
            )
            db.collection("mail").document().set(mailDoc).await()

            Log.d(TAG, "Email recorded and queued in Firebase 'mail' collection for automated delivery")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Note: Email notification log not saved to Firestore: ${e.message}")
            Result.failure(e)
        }
    }

    fun startRealtimeMultiDeviceSync(
        scope: CoroutineScope,
        onVisitorDeleted: (suspend (String) -> Unit)? = null,
        onVisitorUpsert: suspend (VisitorEntity) -> Unit
    ) {
        if (!isAuthenticated) return

        realtimeListener?.remove()
        realtimeListener = db.collection("visitors")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Listen failed for multi-device sync", error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    scope.launch(Dispatchers.IO) {
                        for (change in snapshot.documentChanges) {
                            val doc = change.document
                            val passNumber = doc.getString("passNumber") ?: doc.id
                            if (change.type == DocumentChange.Type.REMOVED) {
                                try {
                                    onVisitorDeleted?.invoke(passNumber)
                                } catch (e: Exception) {
                                    Log.e(TAG, "Error handling deleted remote visitor", e)
                                }
                                continue
                            }

                            try {
                                val branchName = doc.getString("branchName") ?: "Sait FZCO - Dubai"
                                val branchCode = doc.getString("branchCode") ?: "SAIT-DXB"
                                val fullName = doc.getString("fullName") ?: continue
                                val mobileNumber = doc.getString("mobileNumber") ?: ""
                                val companyName = doc.getString("companyName") ?: ""
                                val visitType = doc.getString("visitType") ?: "Walk-In"
                                val department = doc.getString("department") ?: "PROJECTS"
                                val purpose = doc.getString("purpose") ?: "Meeting"
                                val personToMeet = doc.getString("personToMeet") ?: ""
                                val email = doc.getString("email") ?: ""
                                val status = doc.getString("status") ?: "CHECKED_IN"
                                val checkInTime = doc.getString("checkInFormattedTime") ?: ""
                                val checkOutTime = doc.getString("checkOutFormattedTime")
                                val timestamp = doc.getLong("timestamp") ?: System.currentTimeMillis()

                                val remoteVisitor = VisitorEntity(
                                    id = 0,
                                    passNumber = passNumber,
                                    branchName = branchName,
                                    branchCode = branchCode,
                                    fullName = fullName,
                                    mobileNumber = mobileNumber,
                                    companyName = companyName,
                                    visitType = visitType,
                                    department = department,
                                    purpose = purpose,
                                    personToMeet = personToMeet,
                                    email = email,
                                    timestamp = timestamp,
                                    checkInFormattedTime = checkInTime,
                                    checkOutFormattedTime = checkOutTime,
                                    status = status,
                                    isSyncedToGoogleForm = true
                                )

                                onVisitorUpsert(remoteVisitor)
                            } catch (e: Exception) {
                                Log.e(TAG, "Error parsing remote visitor", e)
                            }
                        }
                    }
                }
            }
    }

    fun startOrganizationConfigSync(
        scope: CoroutineScope,
        onSeedDefaults: (() -> Unit)? = null,
        onConfigUpdated: (
            branches: List<Branch>,
            departmentEmails: Map<String, String>,
            activeBranchName: String?,
            adminPin: String?,
            receptionistPin: String?,
            autoResetSeconds: Int?,
            themeMode: String?,
            smtpConfig: SmtpConfig?,
            visitPurposes: List<String>?
        ) -> Unit
    ) {
        orgSettingsListener?.remove()
        orgSettingsListener = db.collection("settings").document("organization")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.w(TAG, "Settings listener failed", error)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.exists()) {
                    // Seed default settings to Firebase when document doesn't exist yet
                    onSeedDefaults?.invoke()
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    try {
                        @Suppress("UNCHECKED_CAST")
                        val branchesRaw = snapshot.get("branches") as? List<Map<String, Any?>>
                        val parsedBranches = mutableListOf<Branch>()
                        branchesRaw?.forEach { map ->
                            val id = map["id"] as? String ?: ""
                            val name = map["name"] as? String ?: ""
                            val shortCode = map["shortCode"] as? String ?: ""
                            val city = map["city"] as? String ?: ""
                            val subtitle = map["subtitle"] as? String ?: ""
                            val countryCode = (map["countryCode"] as? String)?.ifBlank {
                                BranchRegistry.inferCountryCode(city, name)
                            } ?: BranchRegistry.inferCountryCode(city, name)
                            val facilityLocation = (map["facilityLocation"] as? String)?.ifBlank {
                                if (subtitle.isNotBlank() && city.isNotBlank()) "$subtitle, $city" else subtitle
                            } ?: if (subtitle.isNotBlank() && city.isNotBlank()) "$subtitle, $city" else subtitle
                            @Suppress("UNCHECKED_CAST")
                            val depts = (map["departments"] as? List<String>) ?: listOf("PROJECTS", "QHSE", "MANAGEMENT")
                            val logoUri = map["logoUri"] as? String
                            val safetyGuidelines = (map["safetyGuidelines"] as? String)?.ifBlank { Branch.DEFAULT_SAFETY_GUIDELINES } ?: Branch.DEFAULT_SAFETY_GUIDELINES
                            val safetyHelpline = (map["safetyHelpline"] as? String)?.ifBlank { Branch.DEFAULT_SAFETY_HELPLINE } ?: Branch.DEFAULT_SAFETY_HELPLINE
                            val safetyContactName = (map["safetyContactName"] as? String) ?: Branch.DEFAULT_SAFETY_CONTACT_NAME
                            val emergencyExitDrawingUri = map["emergencyExitDrawingUri"] as? String

                            if (name.isNotBlank()) {
                                parsedBranches.add(
                                    Branch(
                                        id = id.ifBlank { name.lowercase().replace(" ", "_") },
                                        name = name,
                                        shortCode = shortCode,
                                        city = city,
                                        subtitle = subtitle,
                                        defaultDepartments = depts,
                                        countryCode = countryCode,
                                        facilityLocation = facilityLocation,
                                        logoUri = logoUri,
                                        safetyGuidelines = safetyGuidelines,
                                        safetyHelpline = safetyHelpline,
                                        safetyContactName = safetyContactName,
                                        emergencyExitDrawingUri = emergencyExitDrawingUri
                                    )
                                )
                            }
                        }

                        @Suppress("UNCHECKED_CAST")
                        val emailsRaw = snapshot.get("departmentEmails") as? Map<String, String>
                        val parsedEmails = emailsRaw ?: emptyMap()

                        val activeBranchName = snapshot.getString("activeBranchName")
                        val adminPin = snapshot.getString("adminPin")
                        val receptionistPin = snapshot.getString("receptionistPin")
                        val autoResetSeconds = snapshot.getLong("autoResetSeconds")?.toInt()
                        val themeMode = snapshot.getString("themeMode")

                        @Suppress("UNCHECKED_CAST")
                        val visitPurposesRaw = snapshot.get("visitPurposes") as? List<String>
                        val parsedVisitPurposes = visitPurposesRaw
                            ?.map { it.trim() }
                            ?.filter { it.isNotBlank() }
                            ?.takeIf { it.isNotEmpty() }

                        @Suppress("UNCHECKED_CAST")
                        val smtpMap = snapshot.get("smtpConfig") as? Map<String, Any?>
                        val parsedSmtp = smtpMap?.let {
                            SmtpConfig(
                                enabled = it["enabled"] as? Boolean ?: false,
                                host = it["host"] as? String ?: "",
                                port = (it["port"] as? Number)?.toInt() ?: 587,
                                encryption = it["encryption"] as? String ?: "STARTTLS",
                                username = it["username"] as? String ?: "",
                                password = it["password"] as? String ?: "",
                                senderEmail = it["senderEmail"] as? String ?: "",
                                senderName = it["senderName"] as? String ?: "SAIT Group Visitor Pass",
                                firebaseTriggerEnabled = it["firebaseTriggerEnabled"] as? Boolean ?: true
                            )
                        }

                        if (parsedBranches.isNotEmpty()) {
                            scope.launch(Dispatchers.Main) {
                                onConfigUpdated(
                                    parsedBranches,
                                    parsedEmails,
                                    activeBranchName,
                                    adminPin,
                                    receptionistPin,
                                    autoResetSeconds,
                                    themeMode,
                                    parsedSmtp,
                                    parsedVisitPurposes
                                )
                            }
                        } else {
                            onSeedDefaults?.invoke()
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing remote organization settings", e)
                    }
                }
            }
    }

    fun stopRealtimeSync() {
        realtimeListener?.remove()
        realtimeListener = null
        orgSettingsListener?.remove()
        orgSettingsListener = null
    }
}
