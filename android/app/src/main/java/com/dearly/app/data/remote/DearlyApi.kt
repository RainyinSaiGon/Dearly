package com.dearly.app.data.remote

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query
import retrofit2.http.Path

interface DearlyApi {
    @POST("auth/session")
    suspend fun createSession(@Body request: SessionRequest): SessionDto

    @POST("auth/refresh")
    suspend fun refresh(@Body request: RefreshRequest): SessionDto

    @POST("auth/logout")
    suspend fun logout(@Body request: RefreshRequest)

    @GET("users/me/elders")
    suspend fun elders(): List<UserDto>

    @GET("contacts")
    suspend fun contacts(@Query("elder_id") elderId: String? = null): List<ContactDto>

    @POST("contacts")
    suspend fun createContact(@Body request: ContactRequest): ContactDto

    @GET("medications")
    suspend fun medications(@Query("elder_id") elderId: String? = null): List<MedicationDto>

    @POST("medications")
    suspend fun createMedication(@Body request: MedicationRequest): MedicationDto

    @POST("medications/{id}/taken")
    suspend fun markTaken(@Path("id") medicationId: String, @Body request: DoseRequest)

    @POST("notifications/register")
    suspend fun registerFcmToken(@Body request: FcmTokenRequest)
}
