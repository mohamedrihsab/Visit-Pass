package com.example.data

import android.content.Context
import android.content.SharedPreferences
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.json.JSONArray
import org.json.JSONObject

class KioskPreferences(
    private val prefs: SharedPreferences
) {
    constructor(context: Context) : this(
        context.getSharedPreferences("sait_kiosk_prefs", Context.MODE_PRIVATE)
    )

    var userRole: UserRole
        get() {
            val roleName = prefs.getString(KEY_USER_ROLE, UserRole.VISITOR_KIOSK.name) ?: UserRole.VISITOR_KIOSK.name
            return try {
                UserRole.valueOf(roleName)
            } catch (e: Exception) {
                UserRole.VISITOR_KIOSK
            }
        }
        set(value) = prefs.edit().putString(KEY_USER_ROLE, value.name).apply()

    var isKioskMode: Boolean
        get() = userRole == UserRole.VISITOR_KIOSK
        set(value) {
            userRole = if (value) UserRole.VISITOR_KIOSK else UserRole.ADMIN
        }

    var activeBranchName: String
        get() = prefs.getString(KEY_ACTIVE_BRANCH, "Sait FZCO - Dubai") ?: "Sait FZCO - Dubai"
        set(value) = prefs.edit().putString(KEY_ACTIVE_BRANCH, value).apply()

    var adminPin: String
        get() = prefs.getString(KEY_ADMIN_PIN, DEFAULT_ADMIN_PIN) ?: DEFAULT_ADMIN_PIN
        set(value) = prefs.edit().putString(KEY_ADMIN_PIN, value).apply()

    var receptionistPin: String
        get() = prefs.getString(KEY_RECEPTIONIST_PIN, DEFAULT_RECEPTIONIST_PIN) ?: DEFAULT_RECEPTIONIST_PIN
        set(value) = prefs.edit().putString(KEY_RECEPTIONIST_PIN, value).apply()

    var kioskAutoResetSeconds: Int
        get() = prefs.getInt(KEY_AUTO_RESET, 15)
        set(value) = prefs.edit().putInt(KEY_AUTO_RESET, value).apply()

    var screenTimeoutMinutes: Int
        get() = prefs.getInt(KEY_SCREEN_TIMEOUT_MINUTES, TIMEOUT_ALWAYS_ON)
        set(value) = prefs.edit().putInt(KEY_SCREEN_TIMEOUT_MINUTES, value).apply()

    fun getBranches(): List<Branch> {
        val json = prefs.getString(KEY_BRANCHES_JSON, null)
        return if (json.isNullOrBlank()) {
            BranchRegistry.INITIAL_BRANCHES
        } else {
            BranchRegistry.branchesFromJson(json)
        }
    }

    fun saveBranches(branches: List<Branch>) {
        val json = BranchRegistry.branchesToJson(branches)
        prefs.edit().putString(KEY_BRANCHES_JSON, json).apply()
    }

    private val moshi by lazy {
        Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    }

    fun getVisitPurposes(): List<String> {
        val json = prefs.getString(KEY_VISIT_PURPOSES_JSON, null)
        if (json.isNullOrBlank()) return BranchRegistry.DEFAULT_VISIT_PURPOSES
        return try {
            val array = JSONArray(json)
            val list = mutableListOf<String>()
            for (i in 0 until array.length()) {
                val item = array.optString(i).trim()
                if (item.isNotBlank()) list.add(item)
            }
            if (list.isEmpty()) BranchRegistry.DEFAULT_VISIT_PURPOSES else list
        } catch (_: Throwable) {
            try {
                val type = Types.newParameterizedType(List::class.java, String::class.java)
                moshi.adapter<List<String>>(type).fromJson(json)?.takeIf { it.isNotEmpty() }
                    ?: BranchRegistry.DEFAULT_VISIT_PURPOSES
            } catch (_: Throwable) {
                BranchRegistry.DEFAULT_VISIT_PURPOSES
            }
        }
    }

    fun saveVisitPurposes(purposes: List<String>) {
        val serialized = try {
            val array = JSONArray()
            purposes.forEach { array.put(it) }
            array.toString()
        } catch (_: Throwable) {
            val type = Types.newParameterizedType(List::class.java, String::class.java)
            moshi.adapter<List<String>>(type).toJson(purposes)
        }
        prefs.edit().putString(KEY_VISIT_PURPOSES_JSON, serialized).apply()
    }

    fun getDepartmentEmails(): Map<String, String> {
        val json = prefs.getString(KEY_DEPT_EMAILS_JSON, null) ?: return defaultDepartmentEmails()
        return try {
            val obj = JSONObject(json)
            val map = mutableMapOf<String, String>()
            val keys = obj.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                map[key] = obj.getString(key)
            }
            if (map.isEmpty()) defaultDepartmentEmails() else map
        } catch (_: Throwable) {
            try {
                val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
                moshi.adapter<Map<String, String>>(type).fromJson(json)?.takeIf { it.isNotEmpty() }
                    ?: defaultDepartmentEmails()
            } catch (_: Throwable) {
                defaultDepartmentEmails()
            }
        }
    }

    fun saveDepartmentEmails(map: Map<String, String>) {
        val serialized = try {
            val obj = JSONObject()
            map.forEach { (k, v) -> obj.put(k, v) }
            obj.toString()
        } catch (_: Throwable) {
            val type = Types.newParameterizedType(Map::class.java, String::class.java, String::class.java)
            moshi.adapter<Map<String, String>>(type).toJson(map)
        }
        prefs.edit().putString(KEY_DEPT_EMAILS_JSON, serialized).apply()
    }

    private fun defaultDepartmentEmails(): Map<String, String> {
        return mapOf(
            "bss_dxb::QHSE" to "qhse.dxb@saitgroup.com, safety.dxb@saitgroup.com",
            "bss_dxb::PROJECTS" to "projects.dxb@saitgroup.com",
            "bss_dxb::SALES & MARKETING" to "sales.dxb@saitgroup.com",
            "bss_dxb::FINANCE" to "finance.dxb@saitgroup.com",
            "bss_dxb::MANAGEMENT" to "management.dxb@saitgroup.com",
            "bss_dxb::OPERATIONS" to "operations.dxb@saitgroup.com",
            "sait_dxb::QHSE" to "qhse@saitgroup.com, safety@saitgroup.com",
            "sait_dxb::PROJECTS" to "projects@saitgroup.com",
            "sait_dxb::SALES & MARKETING" to "sales@saitgroup.com",
            "sait_dxb::FINANCE" to "finance@saitgroup.com",
            "sait_dxb::MANAGEMENT" to "gm@saitgroup.com, admin@saitgroup.com",
            "sait_dxb::MAINTENANCE" to "facility@saitgroup.com",
            "bss_auh::QHSE" to "qhse.auh@saitgroup.com",
            "bss_auh::PROJECTS" to "projects.auh@saitgroup.com",
            "bss_auh::COMMERCIAL" to "commercial.auh@saitgroup.com",
            "ssec_auh::OPERATIONS" to "ops.ssec@saitgroup.com",
            "ssec_auh::QHSE" to "qhse.ssec@saitgroup.com",
            "ssmi_auh::PRODUCTION" to "production.ssmi@saitgroup.com",
            "3e_rak::PROJECTS" to "projects.rak@saitgroup.com"
        )
    }

    var themeMode: String
        get() = prefs.getString(KEY_THEME_MODE, THEME_LIGHT) ?: THEME_LIGHT
        set(value) = prefs.edit().putString(KEY_THEME_MODE, value).apply()

    fun getSmtpConfig(): SmtpConfig {
        val json = prefs.getString(KEY_SMTP_CONFIG, null)
        return SmtpConfig.fromJson(json)
    }

    fun saveSmtpConfig(config: SmtpConfig) {
        prefs.edit().putString(KEY_SMTP_CONFIG, config.toJson()).apply()
    }

    fun getNextSequence(prefix: String, city: String, year: String): Int {
        val cleanPrefix = prefix.trim().uppercase()
        val cleanCity = city.trim().uppercase()
        val key = "seq_${cleanPrefix}_${cleanCity}_$year"
        val current = prefs.getInt(key, 0)
        // If poisoned by legacy random 4-digit numbers (>= 1000), reset cleanly
        val base = if (current >= 1000) 0 else current
        val next = base + 1
        prefs.edit().putInt(key, next).apply()
        return next
    }

    fun resetPassSequence(prefix: String, city: String, year: String, startFrom: Int = 1) {
        val cleanPrefix = prefix.trim().uppercase()
        val cleanCity = city.trim().uppercase()
        val key = "seq_${cleanPrefix}_${cleanCity}_$year"
        prefs.edit().putInt(key, (startFrom - 1).coerceAtLeast(0)).apply()
    }

    fun setSequenceCounter(prefix: String, city: String, year: String, currentSeq: Int) {
        val cleanPrefix = prefix.trim().uppercase()
        val cleanCity = city.trim().uppercase()
        val key = "seq_${cleanPrefix}_${cleanCity}_$year"
        prefs.edit().putInt(key, currentSeq).apply()
    }

    companion object {
        private const val KEY_USER_ROLE = "key_user_role"
        private const val KEY_ACTIVE_BRANCH = "key_active_branch"
        private const val KEY_ADMIN_PIN = "key_admin_pin"
        private const val KEY_RECEPTIONIST_PIN = "key_receptionist_pin"
        private const val KEY_AUTO_RESET = "key_auto_reset"
        private const val KEY_SCREEN_TIMEOUT_MINUTES = "key_screen_timeout_minutes"
        private const val KEY_BRANCHES_JSON = "key_branches_json"
        private const val KEY_VISIT_PURPOSES_JSON = "key_visit_purposes_json"
        private const val KEY_DEPT_EMAILS_JSON = "key_dept_emails_json"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_LIQUID_PALETTE_STYLE = "key_liquid_palette_style"
        private const val KEY_SMTP_CONFIG = "key_smtp_config"

        const val TIMEOUT_ALWAYS_ON = -1
        const val TIMEOUT_SYSTEM = 0
        const val TIMEOUT_1_MIN = 1
        const val TIMEOUT_2_MIN = 2
        const val TIMEOUT_5_MIN = 5
        const val TIMEOUT_10_MIN = 10

        const val THEME_LIGHT = "LIGHT"
        const val THEME_DARK = "DARK"
        const val THEME_SYSTEM = "SYSTEM"

        const val PALETTE_OCEANIC = "OCEANIC"

        const val DEFAULT_ADMIN_PIN = "1234"
        const val DEFAULT_RECEPTIONIST_PIN = "0000"
        private const val KEY_TOUCHLESS_GITHUB_URL = "key_touchless_github_url"
        private const val KEY_TOUCHLESS_MODE = "key_touchless_mode"
        private const val KEY_KIOSK_TERMINAL_ID = "key_kiosk_terminal_id"
        private const val KEY_PROCESSED_REMOTE_KEYS = "key_processed_remote_keys"

        // Default GitHub Pages URL for the live visitor registration portal
        const val DEFAULT_GITHUB_PAGES_URL = "https://mohamedrihsab.github.io/Visit-Pass/"
    }

    var liquidPaletteStyle: String
        get() = PALETTE_OCEANIC
        set(value) = prefs.edit().putString(KEY_LIQUID_PALETTE_STYLE, PALETTE_OCEANIC).apply()

    fun isRemoteVisitorProcessed(key: String): Boolean {
        val cleanKey = key.trim().uppercase()
        if (cleanKey.isBlank()) return false
        val set = prefs.getStringSet(KEY_PROCESSED_REMOTE_KEYS, emptySet()) ?: emptySet()
        return set.contains(cleanKey)
    }

    fun markRemoteVisitorProcessed(vararg keys: String) {
        val existing = prefs.getStringSet(KEY_PROCESSED_REMOTE_KEYS, emptySet()) ?: emptySet()
        val updated = existing.toMutableSet()
        keys.forEach { k ->
            val clean = k.trim().uppercase()
            if (clean.isNotBlank()) updated.add(clean)
        }
        // Cap size at 500 entries to stay lightweight
        val capped = if (updated.size > 500) updated.toList().takeLast(500).toSet() else updated
        prefs.edit().putStringSet(KEY_PROCESSED_REMOTE_KEYS, capped).apply()
    }

    var touchlessGithubPageUrl: String
        get() {
            val url = prefs.getString(KEY_TOUCHLESS_GITHUB_URL, DEFAULT_GITHUB_PAGES_URL) ?: DEFAULT_GITHUB_PAGES_URL
            return if (url.isBlank() || url.contains("sait-visitor")) DEFAULT_GITHUB_PAGES_URL else url
        }
        set(value) = prefs.edit().putString(KEY_TOUCHLESS_GITHUB_URL, value.trim()).apply()

    var touchlessKioskMode: Boolean
        get() = prefs.getBoolean(KEY_TOUCHLESS_MODE, false)
        set(value) = prefs.edit().putBoolean(KEY_TOUCHLESS_MODE, value).apply()

    var kioskTerminalId: String
        get() = prefs.getString(KEY_KIOSK_TERMINAL_ID, "01") ?: "01"
        set(value) = prefs.edit().putString(KEY_KIOSK_TERMINAL_ID, value.trim()).apply()
}
