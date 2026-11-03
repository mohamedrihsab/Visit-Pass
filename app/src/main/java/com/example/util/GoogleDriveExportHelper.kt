package com.example.util

import android.content.Context
import android.content.Intent
import com.example.data.VisitorEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveExportHelper {

    fun generateCsv(visitors: List<VisitorEntity>): String {
        val sb = StringBuilder()
        sb.append("Pass ID,Branch,Full Name,Mobile Number,Company,Visit Type,Department,Purpose,Person to Meet,Check-In Time,Status,Check-Out Time,Stored in Firebase\n")
        for (v in visitors) {
            val row = listOf(
                "\"${v.passNumber}\"",
                "\"${v.branchName.replace("\"", "\"\"")}\"",
                "\"${v.fullName.replace("\"", "\"\"")}\"",
                "\"${v.mobileNumber}\"",
                "\"${v.companyName.replace("\"", "\"\"")}\"",
                "\"${v.visitType}\"",
                "\"${v.department}\"",
                "\"${v.purpose.replace("\"", "\"\"")}\"",
                "\"${v.personToMeet.replace("\"", "\"\"")}\"",
                "\"${v.checkInFormattedTime}\"",
                "\"${v.status}\"",
                "\"${v.checkOutFormattedTime ?: ""}\"",
                "\"${if (v.isSyncedToGoogleForm) "Yes (Cloud Firestore)" else "Local Only"}\""
            ).joinToString(",")
            sb.append(row).append("\n")
        }
        return sb.toString()
    }

    fun shareToGoogleDrive(context: Context, csvContent: String, branchName: String? = null) {
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmm", Locale.getDefault()).format(Date())
        val branchPrefix = if (branchName != null) branchName.replace(" ", "_").replace("/", "-") else "SAIT_Group"
        val title = "${branchPrefix}_Visitors_$dateStr.csv"

        val sendIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/csv"
            putExtra(Intent.EXTRA_SUBJECT, "SAIT Group Visitor Logbook Export - $dateStr")
            putExtra(Intent.EXTRA_TEXT, csvContent)
            setPackage("com.google.android.apps.docs")
        }

        try {
            context.startActivity(sendIntent)
        } catch (e: Exception) {
            val chooserIntent = Intent.createChooser(
                Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_SUBJECT, "SAIT Group Visitor Logbook Export")
                    putExtra(Intent.EXTRA_TEXT, csvContent)
                },
                "Save to Google Drive / Share Logbook"
            )
            context.startActivity(chooserIntent)
        }
    }
}
