package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "visitors")
data class VisitorEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val passNumber: String,
    val branchName: String = "Sait FZCO - Dubai",
    val branchCode: String = "SAIT-DXB",
    val fullName: String,
    val mobileNumber: String,
    val companyName: String,
    val visitType: String,         // "Walk-In" or "Appointment"
    val department: String,
    val purpose: String,
    val customPurpose: String = "",
    val personToMeet: String = "",
    val email: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val checkInFormattedTime: String,
    val status: String = "CHECKED_IN", // "CHECKED_IN"
    val checkOutFormattedTime: String? = null,
    val isSyncedToGoogleForm: Boolean = false // Synced to Cloud Firestore
)

