package com.dearly.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ContactEntity::class, MedicationEntity::class, DoseLogEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DearlyDatabase : RoomDatabase() {
    abstract fun contactDao(): ContactDao
    abstract fun medicationDao(): MedicationDao
}
