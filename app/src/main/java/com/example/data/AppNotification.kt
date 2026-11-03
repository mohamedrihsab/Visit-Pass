package com.example.data

enum class NotificationType {
    EMAIL_SENT,
    EMAIL_FAILED,
    VISITOR_CHECK_IN,
    VISITOR_CHECK_OUT,
    CLOUD_SYNC,
    SYSTEM_ALERT
}

data class AppNotification(
    val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val type: NotificationType,
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val passNumber: String? = null
)
