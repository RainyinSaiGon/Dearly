package com.dearly.app.domain.model

enum class CallMethod {
    PHONE,
    ZALO_VIDEO
}

data class Contact(
    val id: String,
    val elderId: String,
    val nickname: String,
    val fullName: String,
    val phoneNumber: String,
    val relationship: String,
    val callMethod: CallMethod = CallMethod.PHONE,
    val avatarUrl: String? = null
)

data class NewContact(
    val nickname: String,
    val fullName: String,
    val phoneNumber: String,
    val relationship: String,
    val callMethod: CallMethod
)
