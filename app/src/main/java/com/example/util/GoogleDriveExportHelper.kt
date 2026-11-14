package com.example.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.data.VisitorEntity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object GoogleDriveExportHelper {

    private fun escapeCsvField(raw: String?): String {
        val cleaned = (raw ?: "")
            .replace("\r\n", " ")
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()
        return "\"${cleaned.replace("\"", "\"\"")}\""
    }

    private fun resolveRegistrationDateAndTime(visitor: VisitorEntity): Pair<String, String> {
        val parts = visitor.checkInFormattedTime.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val regDate = when {
            parts.size >= 2 -> parts[0]
            visitor.timestamp > 0L -> SimpleDateFormat("dd MMM yyyy", Locale.US).format(Date(visitor.timestamp))
            else -> visitor.checkInFormattedTime
        }
        val regTime = when {
            parts.size >= 2 -> parts.drop(1).joinToString(" ")
            visitor.checkInFormattedTime.isNotBlank() && !visitor.checkInFormattedTime.contains(",") && visitor.checkInFormattedTime.length <= 12 -> visitor.checkInFormattedTime
            visitor.timestamp > 0L -> SimpleDateFormat("hh:mm a", Locale.US).format(Date(visitor.timestamp))
            else -> visitor.checkInFormattedTime
        }
        return Pair(regDate, regTime)
    }

    /**
     * Generates an RFC-4180 compliant, UTF-8 BOM tabular CSV string with structured columns
     * ready for Microsoft Excel, Google Sheets, Numbers, and database import tools.
     */
    fun generateCsv(visitors: List<VisitorEntity>): String {
        val sb = StringBuilder()
        // UTF-8 BOM ensures Excel and spreadsheet viewers automatically detect UTF-8 tabular columns
        sb.append('\uFEFF')

        val headers = listOf(
            "S.No",
            "Pass ID",
            "Registration Date",
            "Registration Time",
            "Branch Name",
            "Branch Code",
            "Full Name",
            "Company",
            "Mobile Number",
            "Email Address",
            "Visit Type",
            "Department",
            "Purpose of Visit",
            "Person to Meet",
            "Status",
            "Check-Out Time",
            "Cloud Sync"
        )
        sb.append(headers.joinToString(",") { escapeCsvField(it) }).append("\r\n")

        visitors.forEachIndexed { index, v ->
            val (regDate, regTime) = resolveRegistrationDateAndTime(v)
            val statusLabel = if (v.status == "CHECKED_IN") "Checked In (Active)" else "Checked Out"
            val hostLabel = v.personToMeet.ifBlank { "Reception Desk" }
            val syncLabel = if (v.isSyncedToGoogleForm) "Synced (Cloud Firestore)" else "Local Only"

            val row = listOf(
                (index + 1).toString(),
                v.passNumber,
                regDate,
                regTime,
                v.branchName,
                v.branchCode,
                v.fullName,
                v.companyName,
                v.mobileNumber,
                v.email,
                v.visitType,
                v.department,
                v.purpose,
                hostLabel,
                statusLabel,
                v.checkOutFormattedTime ?: "-",
                syncLabel
            ).joinToString(",") { escapeCsvField(it) }

            sb.append(row).append("\r\n")
        }
        return sb.toString()
    }

    /**
     * Writes the structured CSV content to an actual `.csv` file on disk and returns it.
     */
    fun writeCsvFile(context: Context, csvContent: String, branchName: String? = null): File {
        val dateStr = SimpleDateFormat("yyyy-MM-dd_HHmm", Locale.US).format(Date())
        val safeBranch = (branchName ?: "Visitor_Logbook")
            .replace(Regex("[^A-Za-z0-9_-]"), "_")
            .replace(Regex("_+"), "_")
            .trim('_')
            .ifBlank { "Visitor_Logbook" }
        val fileName = "${safeBranch}_Visitors_$dateStr.csv"

        val exportDir = File(context.cacheDir, "exports").apply {
            if (!exists()) mkdirs()
        }
        val csvFile = File(exportDir, fileName)
        val contentWithBom = if (csvContent.startsWith('\uFEFF')) csvContent else "\uFEFF$csvContent"
        csvFile.writeText(contentWithBom, Charsets.UTF_8)
        return csvFile
    }

    /**
     * Shares the tabular `.csv` file attachment via Android FileProvider so spreadsheet apps,
     * Google Drive, Files, and email clients receive a real structured `.csv` file.
     */
    fun shareCsvFile(context: Context, csvContent: String, branchName: String? = null) {
        try {
            val csvFile = writeCsvFile(context, csvContent, branchName)
            val authority = "${context.packageName}.fileprovider"
            val fileUri = FileProvider.getUriForFile(context, authority, csvFile)

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, csvFile.nameWithoutExtension.replace("_", " "))
                putExtra(Intent.EXTRA_STREAM, fileUri)
                clipData = ClipData.newRawUri(csvFile.name, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(shareIntent, "Export Tabular CSV File (${csvFile.name})").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(
                context,
                "Unable to export CSV file: ${e.localizedMessage ?: "Unknown error"}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun shareToGoogleDrive(context: Context, csvContent: String, branchName: String? = null) {
        try {
            val csvFile = writeCsvFile(context, csvContent, branchName)
            val authority = "${context.packageName}.fileprovider"
            val fileUri = FileProvider.getUriForFile(context, authority, csvFile)

            val driveIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/csv"
                putExtra(Intent.EXTRA_SUBJECT, csvFile.name)
                putExtra(Intent.EXTRA_TITLE, csvFile.name)
                putExtra(Intent.EXTRA_STREAM, fileUri)
                clipData = ClipData.newRawUri(csvFile.name, fileUri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                setPackage("com.google.android.apps.docs")
            }

            try {
                context.startActivity(driveIntent)
            } catch (_: Exception) {
                shareCsvFile(context, csvContent, branchName)
            }
        } catch (_: Exception) {
            shareCsvFile(context, csvContent, branchName)
        }
    }
}
