package com.dearly.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val elderId: String,
    val nickname: String,
    val fullName: String,
    val phoneNumber: String,
    val relationship: String,
    val callMethod: String
)

@Entity(tableName = "medications")
data class MedicationEntity(
    @PrimaryKey val id: String,
    val elderId: String,
    val name: String,
    val dosage: String,
    val frequencyPerDay: Int,
    val timeSlotsJson: String,
    val notes: String?
)

@Entity(tableName = "dose_logs")
data class DoseLogEntity(
    @PrimaryKey val id: String,
    val medicationId: String,
    val medicationName: String,
    val scheduledTime: String,
    val status: String,
    val takenAt: String?
)
