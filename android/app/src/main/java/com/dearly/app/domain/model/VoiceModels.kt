package com.dearly.app.domain.model

enum class HealthStatusLevel {
    NORMAL,
    WARNING,
    CRITICAL
}

data class HealthStatus(
    val level: HealthStatusLevel,
    val summaryMessage: String,
    val missedDosesCount: Int = 0
)

data class VoiceQueryResult(
    val isProtectedAction: Boolean,
    val verificationPassed: Boolean?,
    val recognizedText: String,
    val responseSpeechText: String,
    val actionExecuted: String? = null
)
