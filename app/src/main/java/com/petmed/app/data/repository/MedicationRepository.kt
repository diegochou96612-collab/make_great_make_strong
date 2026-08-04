package com.petmed.app.data.repository

import com.petmed.app.data.dao.MedicationRecordDao
import com.petmed.app.data.model.MedicationRecord
import kotlinx.coroutines.flow.Flow

class MedicationRepository(private val dao: MedicationRecordDao) {

    val allRecords: Flow<List<MedicationRecord>> = dao.getAll()

    suspend fun insert(record: MedicationRecord): Long = dao.insert(record)

    suspend fun update(record: MedicationRecord) = dao.update(record)

    suspend fun delete(record: MedicationRecord) = dao.delete(record)
}
