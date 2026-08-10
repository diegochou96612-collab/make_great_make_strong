package com.petmed.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.petmed.app.data.model.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Query("SELECT * FROM pets ORDER BY id ASC LIMIT 1")
    fun getFirstPet(): Flow<Pet?>

    @Query("SELECT * FROM pets ORDER BY id ASC")
    fun getAll(): Flow<List<Pet>>

    @Insert
    suspend fun insert(pet: Pet): Long

    @Update
    suspend fun update(pet: Pet)

    @androidx.room.Delete
    suspend fun delete(pet: Pet)
}
