package com.dearly.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.min

enum class VoiceCaptureState {
    IDLE,
    LISTENING,
    SPEECH_DETECTED,
    PROCESSING,
    SPEECH_PROCESSING
}

@Composable
fun VoiceCaptureButton(
    onAudioReady: (File) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    idleLabel: String = "Nhấn để nói",
    recordingLabel: String = "Dừng ghi âm",
    onStateChanged: (VoiceCaptureState) -> Unit = {},
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val recorder = remember { VoiceAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }
    var speechDetected by remember { mutableStateOf(false) }
    var firstSpeechAtMs by remember { mutableLongStateOf(0L) }
    var lastSpeechAtMs by remember { mutableLongStateOf(0L) }
    var activeSpeechSamples by remember { mutableIntStateOf(0) }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var captureError by remember { mutableStateOf<String?>(null) }

    fun startRecording() {
        runCatching { recorder.start() }
            .onSuccess {
                speechDetected = false
                firstSpeechAtMs = 0L
                lastSpeechAtMs = 0L
                activeSpeechSamples = 0
                volumeLevel = 0f
                captureError = null
                recording = true
                onStateChanged(VoiceCaptureState.LISTENING)
            }
            .onFailure {
                onStateChanged(VoiceCaptureState.IDLE)
                captureError = it.localizedMessage ?: "Không thể bắt đầu ghi âm."
                onError(captureError.orEmpty())
            }
    }

    fun stopAndSubmit() {
        runCatching { recorder.stop() }
            .onSuccess { audio ->
                recording = false
                volumeLevel = 0f
                val activeSpeechDurationMs = activeSpeechSamples * AMPLITUDE_POLL_INTERVAL_MS
                if (!speechDetected || activeSpeechDurationMs < MIN_ACTIVE_SPEECH_DURATION_MS) {
                    audio.delete()
                    onStateChanged(VoiceCaptureState.IDLE)
                    captureError = "Dearly chưa nghe đủ rõ. Bác hãy nói gần micro và lâu hơn một chút nhé."
                    onError(captureError.orEmpty())
                    return@onSuccess
                }
                onStateChanged(VoiceCaptureState.SPEECH_PROCESSING)
                onAudioReady(audio)
            }
            .onFailure {
                recording = false
                volumeLevel = 0f
                onStateChanged(VoiceCaptureState.IDLE)
                captureError = it.localizedMessage ?: "Không thể lưu bản ghi âm."
                onError(captureError.orEmpty())
            }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else {
            captureError = "Dearly cần quyền micro để nghe giọng nói."
            onError(captureError.orEmpty())
        }
    }

    DisposableEffect(Unit) {
        onDispose { recorder.release() }
    }

    LaunchedEffect(recording) {
        if (recording) {
            repeat(MAX_RECORDING_DURATION_MS / AMPLITUDE_POLL_INTERVAL_MS) {
                delay(AMPLITUDE_POLL_INTERVAL_MS.toLong())
                if (!recording) return@LaunchedEffect
                val amplitude = recorder.maxAmplitude()
                volumeLevel = min(1f, amplitude.toFloat() / VOLUME_NORMALIZER)
                val now = System.currentTimeMillis()
                if (amplitude >= SPEECH_AMPLITUDE_THRESHOLD) {
                    activeSpeechSamples += 1
                    if (!speechDetected) {
                        speechDetected = true
                        firstSpeechAtMs = now
                        onStateChanged(VoiceCaptureState.SPEECH_DETECTED)
                    }
                    lastSpeechAtMs = now
                } else if (speechDetected && now - lastSpeechAtMs >= SILENCE_AUTO_STOP_MS) {
                    stopAndSubmit()
                    return@LaunchedEffect
                }
            }
            if (recording) stopAndSubmit()
        }
    }

    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Button(
            onClick = {
                if (recording) {
                    stopAndSubmit()
                } else if (
                    ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                    PackageManager.PERMISSION_GRANTED
                ) {
                    startRecording()
                } else {
                    permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                }
            },
            enabled = enabled || recording,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (recording) Color(0xFFC83A32) else Color(0xFF174D3B)
            )
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (recording) Icons.Outlined.Stop else Icons.Outlined.Mic,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Text(if (recording) recordingLabel else idleLabel)
            }
        }
        if (recording) VoiceWaveform(volumeLevel)
        captureError?.let {
            Text(it, color = Color(0xFFC62828), modifier = Modifier.padding(top = 8.dp))
        }
    }
}

private const val MAX_RECORDING_DURATION_MS = 30_000
private const val AMPLITUDE_POLL_INTERVAL_MS = 100
private const val SPEECH_AMPLITUDE_THRESHOLD = 1_000
private const val MIN_ACTIVE_SPEECH_DURATION_MS = 800
private const val SILENCE_AUTO_STOP_MS = 1_400L
private const val VOLUME_NORMALIZER = 12_000f

@Composable
private fun VoiceWaveform(level: Float) {
    val bars = 9
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp)
            .padding(top = 8.dp)
    ) {
        val barWidth = size.width / (bars * 1.7f)
        val gap = barWidth * 0.7f
        repeat(bars) { index ->
            val distance = abs(index - (bars - 1) / 2f) / (bars / 2f)
            val heightFactor = (0.25f + level * (1f - distance * 0.55f)).coerceIn(0.2f, 1f)
            val barHeight = size.height * heightFactor
            drawRoundRect(
                color = Color(0xFF2B7A5E),
                topLeft = Offset(index * (barWidth + gap), (size.height - barHeight) / 2),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2, barWidth / 2)
            )
        }
    }
}

private class VoiceAudioRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var output: File? = null

    @Suppress("DEPRECATION")
    fun start() {
        check(recorder == null) { "A recording is already active" }
        val file = File.createTempFile("dearly-voice-", ".m4a", context.cacheDir)
        val mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
        try {
            mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC)
            mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mediaRecorder.setAudioSamplingRate(16_000)
            mediaRecorder.setAudioEncodingBitRate(96_000)
            mediaRecorder.setOutputFile(file.absolutePath)
            mediaRecorder.prepare()
            mediaRecorder.start()
            recorder = mediaRecorder
            output = file
        } catch (error: Exception) {
            mediaRecorder.release()
            file.delete()
            throw error
        }
    }

    fun stop(): File {
        val active = checkNotNull(recorder) { "No recording is active" }
        val file = checkNotNull(output)
        try {
            active.stop()
            return file
        } catch (error: RuntimeException) {
            file.delete()
            throw error
        } finally {
            active.release()
            recorder = null
            output = null
        }
    }

    fun maxAmplitude(): Int = runCatching { recorder?.maxAmplitude ?: 0 }.getOrDefault(0)

    fun release() {
        val active = recorder
        val file = output
        if (active != null) {
            runCatching { active.stop() }
            active.release()
        }
        recorder = null
        file?.delete()
        output = null
    }
}
