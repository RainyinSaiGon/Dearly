package com.dearly.app.domain.model

enum class UserRole {
    ELDER,
    CAREGIVER
}

data class User(
    val id: String,
    val phoneNumber: String,
    val name: String,
    val age: Int? = null,
    val city: String? = null,
    val role: UserRole = UserRole.ELDER,
    val avatarUrl: String? = null,
    val isVoiceEnrolled: Boolean = false
)
