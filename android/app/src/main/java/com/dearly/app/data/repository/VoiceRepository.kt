package com.dearly.app.data.repository

import android.content.Context
import android.media.MediaPlayer
import android.util.Base64
import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.data.remote.VoiceEnrollmentDto
import com.dearly.app.data.remote.VoiceQueryDto
import com.dearly.app.data.remote.VoiceVerificationDto
import java.io.File
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody

@Singleton
class VoiceRepository @Inject constructor(
    private val api: DearlyApi,
    @ApplicationContext private val context: Context
) {
    private var activePlayer: MediaPlayer? = null
    private var activePlaybackFile: File? = null

    suspend fun enroll(audio: File, phraseIndex: Int): VoiceEnrollmentDto =
        api.enrollVoice(audioPart(audio), phraseIndex.toString().textPart())

    suspend fun query(audio: File): VoiceQueryDto {
        val result = api.queryVoice(audioPart(audio))
        result.responseAudioBase64?.takeIf(String::isNotBlank)?.let { encoded ->
            runCatching { playResponse(encoded) }
        }
        return result
    }

    suspend fun verify(audio: File, intent: String): VoiceVerificationDto =
        api.verifyVoice(audioPart(audio), intent.textPart())

    private fun audioPart(file: File): MultipartBody.Part = MultipartBody.Part.createFormData(
        "audio",
        file.name,
        file.asRequestBody("audio/mp4".toMediaType())
    )

    private fun String.textPart() = toRequestBody("text/plain".toMediaType())

    private suspend fun playResponse(encoded: String) {
        val file = withContext(Dispatchers.IO) {
            File.createTempFile("dearly-response-", ".mp3", context.cacheDir).also {
                it.writeBytes(Base64.decode(encoded, Base64.DEFAULT))
            }
        }
        withContext(Dispatchers.Main.immediate) {
            releasePlayback()
            val player = MediaPlayer()
            activePlayer = player
            activePlaybackFile = file
            try {
                player.setDataSource(file.absolutePath)
                player.setOnPreparedListener(MediaPlayer::start)
                player.setOnCompletionListener { completed -> finishPlayback(completed, file) }
                player.setOnErrorListener { failed, _, _ ->
                    finishPlayback(failed, file)
                    true
                }
                player.prepareAsync()
            } catch (error: Exception) {
                finishPlayback(player, file)
                throw error
            }
        }
    }

    private fun finishPlayback(player: MediaPlayer, file: File) {
        if (activePlayer === player) {
            activePlayer = null
            activePlaybackFile = null
        }
        player.release()
        file.delete()
    }

    private fun releasePlayback() {
        activePlayer?.release()
        activePlaybackFile?.delete()
        activePlayer = null
        activePlaybackFile = null
    }
}
