package com.dearly.app.ui.components

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import java.io.File
import kotlinx.coroutines.delay

@Composable
fun VoiceCaptureButton(
    onAudioReady: (File) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    idleLabel: String = "Nhấn để nói",
    recordingLabel: String = "Dừng ghi âm",
    onError: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val recorder = remember { VoiceAudioRecorder(context) }
    var recording by remember { mutableStateOf(false) }

    fun startRecording() {
        runCatching { recorder.start() }
            .onSuccess { recording = true }
            .onFailure { onError(it.localizedMessage ?: "Không thể bắt đầu ghi âm.") }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startRecording() else onError("Dearly cần quyền micro để nghe giọng nói.")
    }

    DisposableEffect(Unit) {
        onDispose { recorder.release() }
    }

    LaunchedEffect(recording) {
        if (recording) {
            delay(30_000)
            runCatching { recorder.stop() }
                .onSuccess(onAudioReady)
                .onFailure { onError(it.localizedMessage ?: "Không thể lưu bản ghi âm.") }
            recording = false
        }
    }

    Button(
        onClick = {
            if (recording) {
                runCatching { recorder.stop() }
                    .onSuccess(onAudioReady)
                    .onFailure { onError(it.localizedMessage ?: "Không thể lưu bản ghi âm.") }
                recording = false
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
        modifier = modifier.height(52.dp),
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
