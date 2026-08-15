package com.dearly.app.domain.model

enum class DoseStatus {
    TAKEN,
    PENDING,
    SNOOZED,
    MISSED
}

data class Medication(
    val id: String,
    val elderId: String,
    val name: String,
    val dosage: String,
    val frequencyPerDay: Int,
    val timeSlots: List<String>, // e.g. ["08:00", "20:00"]
    val notes: String? = null
)

data class MedicationLog(
    val id: String,
    val medicationId: String,
    val medicationName: String,
    val scheduledTime: String,
    val status: DoseStatus = DoseStatus.PENDING,
    val takenAt: String? = null
)

data class NewMedication(
    val name: String,
    val dosage: String,
    val timeSlots: List<String>,
    val notes: String? = null
)
