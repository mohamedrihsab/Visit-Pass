package com.example.network

import android.util.Base64
import android.util.Log
import com.example.data.SmtpConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.InetSocketAddress
import java.net.Socket
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.net.ssl.SSLSocket
import javax.net.ssl.SSLSocketFactory

object SmtpMailer {
    private const val TAG = "SmtpMailer"
    private const val TIMEOUT_MS = 12000

    suspend fun sendEmail(
        config: SmtpConfig,
        recipients: List<String>,
        subject: String,
        bodyText: String,
        bodyHtml: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        if (!config.enabled) {
            return@withContext Result.failure(IllegalStateException("SMTP Outgoing Mail Server is not enabled"))
        }
        if (config.host.isBlank()) {
            return@withContext Result.failure(IllegalArgumentException("SMTP Host cannot be empty"))
        }
        if (recipients.isEmpty()) {
            return@withContext Result.failure(IllegalArgumentException("No email recipients specified"))
        }

        var socket: Socket? = null
        try {
            val fromEmail = if (config.senderEmail.isNotBlank()) config.senderEmail else config.username
            val fromName = if (config.senderName.isNotBlank()) config.senderName else "SAIT Group Visitor Pass"

            val cleanRecipients = recipients.flatMap { it.split(",") }
                .map { it.trim() }
                .filter { it.contains("@") }

            if (cleanRecipients.isEmpty()) {
                return@withContext Result.failure(IllegalArgumentException("Valid recipient email addresses required"))
            }

            Log.d(TAG, "Connecting to SMTP ${config.host}:${config.port} (${config.encryption})...")

            val isSsl = config.encryption.equals("SSL", ignoreCase = true) || config.port == 465
            val isStartTls = config.encryption.equals("STARTTLS", ignoreCase = true) || config.port == 587

            if (isSsl) {
                val sslFactory = SSLSocketFactory.getDefault()
                val sslSocket = sslFactory.createSocket() as SSLSocket
                sslSocket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
                sslSocket.soTimeout = TIMEOUT_MS
                sslSocket.startHandshake()
                socket = sslSocket
            } else {
                val plainSocket = Socket()
                plainSocket.connect(InetSocketAddress(config.host, config.port), TIMEOUT_MS)
                plainSocket.soTimeout = TIMEOUT_MS
                socket = plainSocket
            }

            var reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
            var writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))

            fun readResponse(): String {
                val sb = StringBuilder()
                var line: String?
                while (true) {
                    line = reader.readLine() ?: break
                    sb.append(line).append("\n")
                    // Multi-line SMTP response code ends when 4th character is space or end of code
                    if (line.length >= 4 && line[3] == ' ') break
                    if (line.length == 3) break
                }
                val resp = sb.toString().trim()
                Log.d(TAG, "<-- $resp")
                return resp
            }

            fun sendCommand(cmd: String, hideLog: Boolean = false): String {
                if (hideLog) Log.d(TAG, "--> [HIDDEN]") else Log.d(TAG, "--> $cmd")
                writer.write(cmd + "\r\n")
                writer.flush()
                return readResponse()
            }

            // 1. Initial Greeting (expect 220)
            val welcome = readResponse()
            if (!welcome.startsWith("220")) {
                return@withContext Result.failure(Exception("SMTP greeting failed: $welcome"))
            }

            // 2. EHLO
            var ehloResp = sendCommand("EHLO saitgroup.local")
            if (!ehloResp.startsWith("250")) {
                ehloResp = sendCommand("HELO saitgroup.local")
                if (!ehloResp.startsWith("250")) {
                    return@withContext Result.failure(Exception("HELO failed: $ehloResp"))
                }
            }

            if (isStartTls && !isSsl && socket != null) {
                val tlsResp = sendCommand("STARTTLS")
                if (tlsResp.startsWith("220")) {
                    val sslFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
                    val sslSocket = sslFactory.createSocket(socket, config.host, config.port, true) as SSLSocket
                    sslSocket.soTimeout = TIMEOUT_MS
                    sslSocket.startHandshake()
                    socket = sslSocket

                    reader = BufferedReader(InputStreamReader(socket.getInputStream(), "UTF-8"))
                    writer = BufferedWriter(OutputStreamWriter(socket.getOutputStream(), "UTF-8"))

                    // Send EHLO again post-TLS handshake
                    ehloResp = sendCommand("EHLO saitgroup.local")
                } else {
                    Log.w(TAG, "STARTTLS not accepted ($tlsResp), proceeding...")
                }
            }

            // 4. AUTH LOGIN if credentials provided
            if (config.username.isNotBlank() && config.password.isNotBlank()) {
                val authResp = sendCommand("AUTH LOGIN")
                if (authResp.startsWith("334")) {
                    val userB64 = Base64.encodeToString(config.username.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val userResp = sendCommand(userB64, hideLog = true)
                    if (!userResp.startsWith("334")) {
                        return@withContext Result.failure(Exception("SMTP user auth failed: $userResp"))
                    }

                    val passB64 = Base64.encodeToString(config.password.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                    val passResp = sendCommand(passB64, hideLog = true)
                    if (!passResp.startsWith("235")) {
                        return@withContext Result.failure(Exception("SMTP password auth failed: $passResp"))
                    }
                } else {
                    Log.w(TAG, "AUTH LOGIN not accepted ($authResp)")
                }
            }

            // 5. MAIL FROM
            val mailFromResp = sendCommand("MAIL FROM:<$fromEmail>")
            if (!mailFromResp.startsWith("250")) {
                return@withContext Result.failure(Exception("MAIL FROM failed: $mailFromResp"))
            }

            // 6. RCPT TO for each recipient
            var recipientCount = 0
            for (rcpt in cleanRecipients) {
                val rcptResp = sendCommand("RCPT TO:<$rcpt>")
                if (rcptResp.startsWith("250") || rcptResp.startsWith("251")) {
                    recipientCount++
                } else {
                    Log.w(TAG, "RCPT TO failed for $rcpt: $rcptResp")
                }
            }

            if (recipientCount == 0) {
                return@withContext Result.failure(Exception("No recipients accepted by SMTP server"))
            }

            // 7. DATA
            val dataResp = sendCommand("DATA")
            if (!dataResp.startsWith("354")) {
                return@withContext Result.failure(Exception("DATA command rejected: $dataResp"))
            }

            // 8. Headers & Body
            val dateStr = SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z", Locale.US).format(Date())
            val msgBuilder = StringBuilder()
            msgBuilder.append("From: \"$fromName\" <$fromEmail>\r\n")
            msgBuilder.append("To: ${cleanRecipients.joinToString(", ")}\r\n")
            msgBuilder.append("Subject: $subject\r\n")
            msgBuilder.append("Date: $dateStr\r\n")
            msgBuilder.append("MIME-Version: 1.0\r\n")

            if (!bodyHtml.isNullOrBlank()) {
                msgBuilder.append("Content-Type: text/html; charset=UTF-8\r\n")
                msgBuilder.append("Content-Transfer-Encoding: 8bit\r\n\r\n")
                msgBuilder.append(bodyHtml)
            } else {
                msgBuilder.append("Content-Type: text/plain; charset=UTF-8\r\n")
                msgBuilder.append("Content-Transfer-Encoding: 8bit\r\n\r\n")
                msgBuilder.append(bodyText)
            }
            msgBuilder.append("\r\n.\r\n")

            writer.write(msgBuilder.toString())
            writer.flush()

            val endResp = readResponse()
            if (!endResp.startsWith("250")) {
                return@withContext Result.failure(Exception("Failed to finalize email delivery: $endResp"))
            }

            // 9. QUIT
            try {
                sendCommand("QUIT")
            } catch (e: Exception) {
                // Ignore QUIT errors
            }

            Log.i(TAG, "Email successfully sent to $recipientCount recipient(s)")
            Result.success("Sent to $recipientCount recipient(s)")
        } catch (e: Exception) {
            Log.e(TAG, "SMTP Error: ${e.message}", e)
            Result.failure(e)
        } finally {
            try {
                socket?.close()
            } catch (e: Exception) {
                // Ignore close error
            }
        }
    }

    suspend fun testConnection(config: SmtpConfig, targetRecipient: String): Result<String> {
        val testSubject = "SAIT Group Visitor Pass - SMTP Test Connection"
        val testBody = "This is an automated test message sent from SAIT Group Visitor Pass system.\n\n" +
                "SMTP Server: ${config.host}:${config.port}\n" +
                "Encryption: ${config.encryption}\n" +
                "Sender: ${config.senderEmail}\n" +
                "Timestamp: ${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}\n\n" +
                "Outgoing mail server configuration is operational."

        val testHtml = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; padding: 20px; border: 1px solid #e2e8f0; border-radius: 8px;">
                <h2 style="color: #0F2942; margin-top: 0;">SAIT Group Visitor Pass</h2>
                <h4 style="color: #0D9488; margin-top: 0;">SMTP Test Connection Successful</h4>
                <p>This is an automated confirmation verifying that your outgoing mail server settings are configured correctly.</p>
                <table style="width: 100%; border-collapse: collapse; margin-top: 15px;">
                    <tr style="background-color: #f8fafc;"><td style="padding: 8px; font-weight: bold;">Host</td><td style="padding: 8px;">${config.host}:${config.port}</td></tr>
                    <tr><td style="padding: 8px; font-weight: bold;">Encryption</td><td style="padding: 8px;">${config.encryption}</td></tr>
                    <tr style="background-color: #f8fafc;"><td style="padding: 8px; font-weight: bold;">Sender</td><td style="padding: 8px;">${config.senderEmail}</td></tr>
                    <tr><td style="padding: 8px; font-weight: bold;">Time</td><td style="padding: 8px;">${SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())}</td></tr>
                </table>
                <p style="font-size: 12px; color: #64748b; margin-top: 20px;">SAIT Group Security & Visitor Administration</p>
            </div>
        """.trimIndent()

        return sendEmail(
            config = config,
            recipients = listOf(targetRecipient),
            subject = testSubject,
            bodyText = testBody,
            bodyHtml = testHtml
        )
    }
}
