package com.example.data

import org.json.JSONObject

data class SmtpConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 587,
    val encryption: String = "STARTTLS", // "STARTTLS", "SSL", "NONE"
    val username: String = "",
    val password: String = "",
    val senderEmail: String = "",
    val senderName: String = "SAIT Group Visitor Pass",
    val firebaseTriggerEnabled: Boolean = true
) {
    fun toJson(): String {
        val obj = JSONObject()
        obj.put("enabled", enabled)
        obj.put("host", host)
        obj.put("port", port)
        obj.put("encryption", encryption)
        obj.put("username", username)
        obj.put("password", password)
        obj.put("senderEmail", senderEmail)
        obj.put("senderName", senderName)
        obj.put("firebaseTriggerEnabled", firebaseTriggerEnabled)
        return obj.toString()
    }

    companion object {
        fun fromJson(json: String?): SmtpConfig {
            if (json.isNullOrBlank()) return SmtpConfig()
            return try {
                val obj = JSONObject(json)
                SmtpConfig(
                    enabled = obj.optBoolean("enabled", false),
                    host = obj.optString("host", ""),
                    port = obj.optInt("port", 587),
                    encryption = obj.optString("encryption", "STARTTLS"),
                    username = obj.optString("username", ""),
                    password = obj.optString("password", ""),
                    senderEmail = obj.optString("senderEmail", ""),
                    senderName = obj.optString("senderName", "SAIT Group Visitor Pass"),
                    firebaseTriggerEnabled = obj.optBoolean("firebaseTriggerEnabled", true)
                )
            } catch (e: Exception) {
                SmtpConfig()
            }
        }
    }
}
