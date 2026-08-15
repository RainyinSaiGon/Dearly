package com.dearly.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ContactDao {
    @Query("SELECT * FROM contacts WHERE elderId = :elderId ORDER BY nickname, fullName")
    fun observe(elderId: String): Flow<List<ContactEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(items: List<ContactEntity>)

    @Query("DELETE FROM contacts WHERE elderId = :elderId")
    suspend fun deleteForElder(elderId: String)
}

@Dao
interface MedicationDao {
    @Query("SELECT * FROM medications WHERE elderId = :elderId ORDER BY name")
    fun observeMedications(elderId: String): Flow<List<MedicationEntity>>

    @Query("""
        SELECT dose_logs.* FROM dose_logs
        INNER JOIN medications ON medications.id = dose_logs.medicationId
        WHERE medications.elderId = :elderId
        ORDER BY scheduledTime
    """)
    fun observeTodayLogs(elderId: String): Flow<List<DoseLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertMedications(items: List<MedicationEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLogs(items: List<DoseLogEntity>)

    @Query("DELETE FROM dose_logs WHERE medicationId IN (SELECT id FROM medications WHERE elderId = :elderId)")
    suspend fun deleteLogsForElder(elderId: String)

    @Query("DELETE FROM medications WHERE elderId = :elderId")
    suspend fun deleteMedicationsForElder(elderId: String)
}
