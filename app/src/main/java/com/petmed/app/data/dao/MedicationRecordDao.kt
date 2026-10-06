package com.petmed.app.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.petmed.app.data.model.MedicationRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicationRecordDao {
    @Query("SELECT * FROM medication_records ORDER BY id DESC")
    fun getAll(): Flow<List<MedicationRecord>>

    @Insert
    suspend fun insert(record: MedicationRecord): Long

    @Update
    suspend fun update(record: MedicationRecord)

    @Delete
    suspend fun delete(record: MedicationRecord)
}
