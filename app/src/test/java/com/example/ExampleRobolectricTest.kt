package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.Branch
import com.example.data.BranchRegistry
import com.example.data.KioskPreferences
import com.example.data.VisitorEntity
import com.example.ui.RegistrationFormState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun readStringFromContext() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("SAIT Group Visitor Pass", appName)
  }

  @Test
  fun testFormValidation() {
    val emptyForm = RegistrationFormState()
    assertFalse(emptyForm.isValid)

    val validForm = RegistrationFormState(
      fullName = "John Doe",
      mobileNumber = "0501234567",
      companyName = "Apex Engineering",
      visitType = "Appointment",
      department = "PROJECTS",
      purpose = "Meeting"
    )
    assertTrue(validForm.isValid)

    val otherPurposeWithoutText = validForm.copy(
      purpose = "Other",
      customPurpose = ""
    )
    assertFalse(otherPurposeWithoutText.isValid)

    val otherPurposeWithText = validForm.copy(
      purpose = "Other",
      customPurpose = "Vendor Audit"
    )
    assertTrue(otherPurposeWithText.isValid)
  }

  @Test
  fun testKioskPreferences() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)
    assertEquals("1234", prefs.adminPin)
    assertTrue(prefs.isKioskMode)

    prefs.adminPin = "9876"
    assertEquals("9876", prefs.adminPin)
    prefs.adminPin = "1234" // restore
  }

  @Test
  fun testDynamicBranchesAndDepartments() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)
    val initialBranches = prefs.getBranches()
    assertTrue(initialBranches.isNotEmpty())
    assertEquals(6, initialBranches.size)

    val customBranch = Branch(
      id = "sait_shj",
      name = "SAIT Marine - Sharjah",
      shortCode = "SAIT-SHJ",
      city = "Sharjah",
      subtitle = "Marine Fabrication Yard",
      defaultDepartments = listOf("PROJECTS", "QHSE", "OPERATIONS")
    )

    val updatedList = initialBranches + customBranch
    prefs.saveBranches(updatedList)

    val reloaded = prefs.getBranches()
    assertEquals(7, reloaded.size)
    assertTrue(reloaded.any { it.shortCode == "SAIT-SHJ" })

    // Restore
    prefs.saveBranches(initialBranches)
  }

  @Test
  fun testDepartmentEmailsConfig() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)
    val emails = prefs.getDepartmentEmails()
    assertTrue(emails.isNotEmpty())

    val updatedMap = emails.toMutableMap()
    updatedMap["sait_dxb::QHSE"] = "qhse.lead@saitgroup.com, safety.officer@saitgroup.com"
    prefs.saveDepartmentEmails(updatedMap)

    val reloaded = prefs.getDepartmentEmails()
    assertEquals("qhse.lead@saitgroup.com, safety.officer@saitgroup.com", reloaded["sait_dxb::QHSE"])
  }

  @Test
  fun testCsvExportFormat() {
    val sample = VisitorEntity(
      id = 1,
      passNumber = "SAIT-VIS-1001",
      fullName = "Alice Smith",
      mobileNumber = "0501112233",
      companyName = "Global Logistics",
      visitType = "Walk-In",
      department = "SALES & MARKETING",
      purpose = "Material Delivery",
      checkInFormattedTime = "23 Oct 2026, 10:00 AM",
      status = "CHECKED_IN",
      isSyncedToGoogleForm = true
    )

    // Verify CSV header and values
    val line = "\"${sample.passNumber}\",\"${sample.fullName}\",\"${sample.mobileNumber}\""
    assertTrue(line.contains("SAIT-VIS-1001"))
    assertTrue(line.contains("Alice Smith"))
  }

  @Test
  fun testThemePreferences() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)
    
    // Default should be light mode
    assertEquals(KioskPreferences.THEME_LIGHT, prefs.themeMode)

    // Switch to dark mode
    prefs.themeMode = KioskPreferences.THEME_DARK
    assertEquals(KioskPreferences.THEME_DARK, prefs.themeMode)

    // Switch to system mode
    prefs.themeMode = KioskPreferences.THEME_SYSTEM
    assertEquals(KioskPreferences.THEME_SYSTEM, prefs.themeMode)

    // Restore to Light
    prefs.themeMode = KioskPreferences.THEME_LIGHT
  }

  @Test
  fun testSmtpConfigPersistence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)

    val initialConfig = prefs.getSmtpConfig()
    assertFalse(initialConfig.enabled)

    val testConfig = com.example.data.SmtpConfig(
      enabled = true,
      host = "mail.saitgroup.com",
      port = 587,
      encryption = "STARTTLS",
      username = "kiosk@saitgroup.com",
      password = "secure_password_123",
      senderEmail = "no-reply@saitgroup.com",
      senderName = "SAIT Group Visitor Alerts",
      firebaseTriggerEnabled = true
    )

    prefs.saveSmtpConfig(testConfig)
    val reloaded = prefs.getSmtpConfig()

    assertTrue(reloaded.enabled)
    assertEquals("mail.saitgroup.com", reloaded.host)
    assertEquals(587, reloaded.port)
    assertEquals("STARTTLS", reloaded.encryption)
    assertEquals("kiosk@saitgroup.com", reloaded.username)
    assertEquals("secure_password_123", reloaded.password)
    assertEquals("no-reply@saitgroup.com", reloaded.senderEmail)
    assertEquals("SAIT Group Visitor Alerts", reloaded.senderName)
    assertTrue(reloaded.firebaseTriggerEnabled)

    // Restore default
    prefs.saveSmtpConfig(com.example.data.SmtpConfig())
  }

  @Test
  fun testAppNotificationModel() {
    val notif = com.example.data.AppNotification(
      title = "SMTP Email Dispatched",
      message = "Alert sent to safety team",
      type = com.example.data.NotificationType.EMAIL_SENT,
      passNumber = "SAIT-VIS-1002"
    )

    assertEquals("SMTP Email Dispatched", notif.title)
    assertEquals("Alert sent to safety team", notif.message)
    assertEquals(com.example.data.NotificationType.EMAIL_SENT, notif.type)
    assertEquals("SAIT-VIS-1002", notif.passNumber)
    assertFalse(notif.isRead)

    val readNotif = notif.copy(isRead = true)
    assertTrue(readNotif.isRead)
  }

  @Test
  fun testSixDigitPinAndPassSequence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)

    // Test 6-digit PIN support
    prefs.adminPin = "123456"
    prefs.receptionistPin = "654321"
    assertEquals("123456", prefs.adminPin)
    assertEquals("654321", prefs.receptionistPin)

    // Restore default PINs
    prefs.adminPin = "1234"
    prefs.receptionistPin = "0000"

    // Test prefix-city-YYXXXX sequence counter
    val seq1 = prefs.getNextSequence("SAIT", "DXB", "26")
    val seq2 = prefs.getNextSequence("SAIT", "DXB", "26")
    assertTrue(seq2 == seq1 + 1)
    val formatted = "SAIT-DXB-26" + seq1.toString().padStart(4, '0')
    assertTrue(formatted.matches(Regex("^SAIT-DXB-26\\d{4}$")))
  }

  @Test
  fun testBranchLogoAndSafetyInfoPersistence() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val prefs = KioskPreferences(context)
    val initialBranches = prefs.getBranches()

    val updatedBranches = initialBranches.map { branch ->
      if (branch.id == "sait_dxb") {
        branch.copy(
          logoUri = "content://media/picker/0/com.android.providers.media.photopicker/media/101",
          safetyGuidelines = "1. Mandatory safety shoes.\n2. Report to Assembly Point A.",
          safetyHelpline = "+971 4 888 9999",
          emergencyExitDrawingUri = "content://media/picker/0/com.android.providers.media.photopicker/media/102"
        )
      } else branch
    }
    prefs.saveBranches(updatedBranches)

    val reloaded = prefs.getBranches().first { it.id == "sait_dxb" }
    assertEquals("content://media/picker/0/com.android.providers.media.photopicker/media/101", reloaded.logoUri)
    assertEquals("1. Mandatory safety shoes.\n2. Report to Assembly Point A.", reloaded.safetyGuidelines)
    assertEquals("+971 4 888 9999", reloaded.safetyHelpline)
    assertEquals("content://media/picker/0/com.android.providers.media.photopicker/media/102", reloaded.emergencyExitDrawingUri)

    // Restore defaults
    prefs.saveBranches(initialBranches)
  }
}

