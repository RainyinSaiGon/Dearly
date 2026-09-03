package com.dearly.app.data.repository

import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.data.remote.VoiceEnrollmentDto
import com.dearly.app.data.remote.VoiceQueryDto
import com.dearly.app.data.remote.VoiceVerificationDto
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class VoiceRepository @Inject constructor(
    private val api: DearlyApi,
    private val vietnameseTextToSpeech: VietnameseTextToSpeech
) {
    suspend fun enroll(audio: File, phraseIndex: Int): VoiceEnrollmentDto =
        api.enrollVoice(audioPart(audio), phraseIndex.toString().textPart())

    suspend fun query(audio: File): SpokenVoiceQuery {
        val query = queryRaw(audio)
        return SpokenVoiceQuery(
            query = query,
            speechWarning = speak(query.responseText)
        )
    }

    suspend fun queryRaw(audio: File): VoiceQueryDto = api.queryVoice(audioPart(audio))

    suspend fun speak(text: String, speechRate: Float? = null): String? =
        vietnameseTextToSpeech.speak(text, speechRate)

    suspend fun queryPublic(audio: File): SpokenVoiceQuery {
        val query = api.queryPublicVoice(audioPart(audio))
        return SpokenVoiceQuery(
            query = query,
            speechWarning = speak(query.responseText)
        )
    }

    suspend fun verify(audio: File, intent: String): VoiceVerificationDto =
        api.verifyVoice(audioPart(audio), intent.textPart())

    private fun audioPart(file: File): MultipartBody.Part = MultipartBody.Part.createFormData(
        "audio",
        file.name,
        file.asRequestBody("audio/mp4".toMediaType())
    )

    private fun String.textPart() = toRequestBody("text/plain".toMediaType())

}

data class SpokenVoiceQuery(
    val query: VoiceQueryDto,
    val speechWarning: String?
)
