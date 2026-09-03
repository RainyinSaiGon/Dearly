package com.dearly.app.data.remote

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Multipart
import retrofit2.http.Path
import retrofit2.http.Part
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Query
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface DearlyApi {
    @POST("auth/session")
    suspend fun createSession(@Body request: SessionRequest): SessionDto

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): SessionDto

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest)

    @GET("users/me/elders")
    suspend fun elders(): List<UserDto>

    @POST("users/me/link-code")
    suspend fun createElderLinkCode(): ElderLinkCodeDto

    @POST("users/me/elders")
    suspend fun linkElder(@Body request: ElderLinkRequest): UserDto

    @DELETE("users/me/elders/{elderId}")
    suspend fun unlinkElder(@Path("elderId") elderId: String)

    @GET("contacts")
    suspend fun contacts(@Query("elder_id") elderId: String? = null): List<ContactDto>

    @POST("contacts")
    suspend fun createContact(@Body request: ContactRequest): ContactDto

    @PUT("contacts/{id}")
    suspend fun updateContact(@Path("id") contactId: String, @Body request: ContactRequest): ContactDto

    @DELETE("contacts/{id}")
    suspend fun deleteContact(@Path("id") contactId: String, @Query("elder_id") elderId: String?)

    @GET("medications")
    suspend fun medications(@Query("elder_id") elderId: String? = null): List<MedicationDto>

    @POST("medications")
    suspend fun createMedication(@Body request: MedicationRequest): MedicationDto

    @PUT("medications/{id}")
    suspend fun updateMedication(@Path("id") medicationId: String, @Body request: MedicationRequest): MedicationDto

    @DELETE("medications/{id}")
    suspend fun deleteMedication(@Path("id") medicationId: String, @Query("elder_id") elderId: String?)

    @POST("medications/{id}/taken")
    suspend fun markTaken(
        @Path("id") medicationId: String,
        @Header("X-Voice-Grant") verificationGrant: String,
        @Body request: DoseRequest
    )

    @Multipart
    @POST("voice/enroll")
    suspend fun enrollVoice(
        @Part audio: MultipartBody.Part,
        @Part("phrase_index") phraseIndex: RequestBody
    ): VoiceEnrollmentDto

    @Multipart
    @POST("voice/query")
    suspend fun queryVoice(@Part audio: MultipartBody.Part): VoiceQueryDto

    @Multipart
    @POST("public/voice/query")
    suspend fun queryPublicVoice(@Part audio: MultipartBody.Part): VoiceQueryDto

    @Multipart
    @POST("voice/verify")
    suspend fun verifyVoice(
        @Part audio: MultipartBody.Part,
        @Part("intent") intent: RequestBody
    ): VoiceVerificationDto

    @POST("notifications/register")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest)
}
