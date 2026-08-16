package com.dearly.app.data.repository

import androidx.room.withTransaction
import com.dearly.app.data.local.DearlyDatabase
import com.dearly.app.data.local.DoseLogEntity
import com.dearly.app.data.local.MedicationDao
import com.dearly.app.data.local.MedicationEntity
import com.dearly.app.data.remote.DearlyApi
import com.dearly.app.data.remote.DoseRequest
import com.dearly.app.data.remote.MedicationRequest
import com.dearly.app.domain.model.DoseStatus
import com.dearly.app.domain.model.MedicationLog
import com.dearly.app.domain.model.Medication
import com.dearly.app.domain.model.NewMedication
import com.google.gson.Gson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MedicationRepository @Inject constructor(
    private val api: DearlyApi,
    private val database: DearlyDatabase,
    private val dao: MedicationDao,
    private val gson: Gson
) {
    fun observeMedications(elderId: String): Flow<List<Medication>> =
        dao.observeMedications(elderId).map { items ->
            items.map {
                Medication(
                    id = it.id,
                    elderId = it.elderId,
                    name = it.name,
                    dosage = it.dosage,
                    frequencyPerDay = it.frequencyPerDay,
                    timeSlots = runCatching {
                        gson.fromJson(it.timeSlotsJson, Array<String>::class.java).toList()
                    }.getOrDefault(emptyList()),
                    notes = it.notes
                )
            }
        }

    fun observeTodayLogs(elderId: String): Flow<List<MedicationLog>> =
        dao.observeTodayLogs(elderId).map { items ->
            items.map {
                MedicationLog(
                    id = it.id, medicationId = it.medicationId, medicationName = it.medicationName,
                    scheduledTime = it.scheduledTime,
                    status = runCatching { DoseStatus.valueOf(it.status) }.getOrDefault(DoseStatus.PENDING),
                    takenAt = it.takenAt
                )
            }
        }

    suspend fun refresh(elderId: String?) {
        val remote = api.medications(elderId)
        val resolvedElderId = elderId ?: remote.firstOrNull()?.elderId ?: return
        database.withTransaction {
            dao.deleteLogsForElder(resolvedElderId)
            dao.deleteMedicationsForElder(resolvedElderId)
            dao.upsertMedications(remote.map {
                MedicationEntity(
                    it.id, it.elderId, it.name, it.dosage, it.frequencyPerDay,
                    gson.toJson(it.timeSlots), it.notes
                )
            })
            dao.upsertLogs(remote.flatMap { medication ->
                medication.todayLogs.map {
                    DoseLogEntity(
                        it.id, it.medicationId, it.medicationName,
                        it.scheduledTime, it.status, it.takenAt
                    )
                }
            })
        }
    }

    suspend fun create(elderId: String?, item: NewMedication) {
        api.createMedication(
            MedicationRequest(
                elderId, item.name, item.dosage, item.timeSlots.size,
                item.timeSlots, item.notes
            )
        )
        refresh(elderId)
    }

    suspend fun update(elderId: String?, medicationId: String, item: NewMedication) {
        api.updateMedication(
            medicationId,
            MedicationRequest(
                elderId, item.name, item.dosage, item.timeSlots.size,
                item.timeSlots, item.notes
            )
        )
        refresh(elderId)
    }

    suspend fun delete(elderId: String?, medicationId: String) {
        api.deleteMedication(medicationId, elderId)
        refresh(elderId)
    }

    suspend fun markTaken(elderId: String?, log: MedicationLog, verificationGrant: String) {
        api.markTaken(log.medicationId, verificationGrant, DoseRequest(elderId, log.scheduledTime))
        refresh(elderId)
    }
}
