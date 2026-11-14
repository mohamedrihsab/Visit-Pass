package com.example.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONObject

data class SmtpConfig(
    val enabled: Boolean = false,
    val host: String = "",
    val port: Int = 587,
    val encryption: String = "STARTTLS", // "STARTTLS", "SSL", "NONE"
    val username: String = "",
    val password: String = "",
    val senderEmail: String = "",
    val senderName: String = "SAIT Pass+",
    val firebaseTriggerEnabled: Boolean = true
) {
    fun toJson(): String {
        return try {
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
            obj.toString()
        } catch (_: Throwable) {
            moshiAdapter.toJson(this)
        }
    }

    companion object {
        private val moshiAdapter by lazy {
            Moshi.Builder()
                .addLast(KotlinJsonAdapterFactory())
                .build()
                .adapter(SmtpConfig::class.java)
        }

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
                    senderName = obj.optString("senderName", "SAIT Pass+"),
                    firebaseTriggerEnabled = obj.optBoolean("firebaseTriggerEnabled", true)
                )
            } catch (_: Throwable) {
                try {
                    moshiAdapter.fromJson(json) ?: SmtpConfig()
                } catch (_: Throwable) {
                    SmtpConfig()
                }
            }
        }
    }
}
