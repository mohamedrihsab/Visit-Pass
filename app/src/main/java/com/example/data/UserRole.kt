package com.example.data

enum class UserRole(val displayName: String, val description: String) {
    VISITOR_KIOSK(
        displayName = "Visitor / Kiosk User",
        description = "Self-service check-in portal for guests. Confidential logs and settings are secured."
    ),
    RECEPTIONIST(
        displayName = "Receptionist",
        description = "Manual visitor registration, real-time logbook, on-site headcount, pass details & check-out actions."
    ),
    ADMIN(
        displayName = "Admin",
        description = "Full control including branch selection, security PINs, Cloud Firestore live sync, Google Drive export, and kiosk reset timers."
    )
}
