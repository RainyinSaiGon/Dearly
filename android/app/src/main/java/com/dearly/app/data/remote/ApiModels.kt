package com.dearly.app.data.remote

import com.google.gson.annotations.SerializedName

data class SessionRequest(
    @SerializedName("firebase_id_token") val firebaseIdToken: String,
    val role: String
)

data class RefreshRequest(@SerializedName("refresh_token") val refreshToken: String)

data class SessionDto(
    @SerializedName("access_token") val accessToken: String,
    @SerializedName("refresh_token") val refreshToken: String,
    @SerializedName("expires_in") val expiresIn: Long,
    val user: UserDto
)

data class UserDto(
    val id: String,
    @SerializedName("phone_number") val phoneNumber: String?,
    val email: String?,
    val name: String,
    val role: String,
    @SerializedName("avatar_url") val avatarUrl: String?
)

data class ContactDto(
    val id: String,
    @SerializedName("elder_id") val elderId: String,
    val nickname: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    val relationship: String,
    @SerializedName("call_method") val callMethod: String
)

data class ContactRequest(
    @SerializedName("elder_id") val elderId: String?,
    val nickname: String,
    @SerializedName("full_name") val fullName: String,
    @SerializedName("phone_number") val phoneNumber: String,
    val relationship: String,
    @SerializedName("call_method") val callMethod: String
)

data class MedicationDto(
    val id: String,
    @SerializedName("elder_id") val elderId: String,
    val name: String,
    val dosage: String,
    @SerializedName("frequency_per_day") val frequencyPerDay: Int,
    @SerializedName("time_slots") val timeSlots: List<String>,
    val notes: String?,
    @SerializedName("today_logs") val todayLogs: List<DoseLogDto>
)

data class DoseLogDto(
    val id: String,
    @SerializedName("medication_id") val medicationId: String,
    @SerializedName("medication_name") val medicationName: String,
    @SerializedName("scheduled_time") val scheduledTime: String,
    val status: String,
    @SerializedName("taken_at") val takenAt: String?
)

data class MedicationRequest(
    @SerializedName("elder_id") val elderId: String?,
    val name: String,
    val dosage: String,
    @SerializedName("frequency_per_day") val frequencyPerDay: Int,
    @SerializedName("time_slots") val timeSlots: List<String>,
    val notes: String? = null
)

data class DoseRequest(
    @SerializedName("elder_id") val elderId: String?,
    @SerializedName("scheduled_time") val scheduledTime: String
)

data class FcmTokenRequest(@SerializedName("fcm_token") val fcmToken: String)
