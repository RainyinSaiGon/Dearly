package com.dearly.app.data.repository

import android.content.Context
import android.media.AudioAttributes
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Device-local Vietnamese speech. The AI service returns text only, which keeps
 * local demos independent of paid cloud TTS credentials.
 */
@Singleton
class VietnameseTextToSpeech @Inject constructor(
    @ApplicationContext context: Context
) {
    private val vietnamese = Locale("vi", "VN")
    private val initialization = CompletableDeferred<String?>()
    private var engine: TextToSpeech? = null

    init {
        engine = TextToSpeech(context.applicationContext) { status ->
            val textToSpeech = engine
            val issue = when {
                status != TextToSpeech.SUCCESS || textToSpeech == null ->
                    "Không thể khởi tạo trình đọc tiếng Việt trên thiết bị này."
                textToSpeech.setLanguage(vietnamese) < TextToSpeech.LANG_AVAILABLE ->
                    "Chưa có giọng đọc tiếng Việt. Vào Cài đặt > Chuyển văn bản thành giọng nói để tải giọng vi-VN."
                else -> {
                    textToSpeech.setSpeechRate(0.92f)
                    textToSpeech.setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_ASSISTANCE_ACCESSIBILITY)
                            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                            .build()
                    )
                    null
                }
            }
            initialization.complete(issue)
        }
    }

    /** Returns a displayable warning when the device cannot speak Vietnamese. */
    suspend fun speak(text: String): String? {
        if (text.isBlank()) return null

        initialization.await()?.let { return it }
        return withContext(Dispatchers.Main.immediate) {
            val status = engine?.speak(
                text,
                TextToSpeech.QUEUE_FLUSH,
                null,
                "dearly-${UUID.randomUUID()}"
            )
            if (status == TextToSpeech.SUCCESS) {
                null
            } else {
                "Không thể đọc phản hồi. Hãy kiểm tra giọng đọc tiếng Việt của thiết bị."
            }
        }
    }
}
