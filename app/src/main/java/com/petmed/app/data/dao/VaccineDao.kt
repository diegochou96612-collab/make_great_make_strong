package com.petmed.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.petmed.app.data.model.VaccineRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccine_records WHERE petId = :petId ORDER BY doneDate DESC, id DESC")
    fun observeByPet(petId: Int): Flow<List<VaccineRecord>>

    @Insert
    suspend fun insert(record: VaccineRecord): Long

    @Query("DELETE FROM vaccine_records WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM vaccine_records WHERE petId = :petId")
    suspend fun deleteByPet(petId: Int)
}
