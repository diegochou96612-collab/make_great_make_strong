package com.petmed.app.data.repository

import com.petmed.app.data.dao.PetDao
import com.petmed.app.data.model.Pet
import kotlinx.coroutines.flow.Flow

class PetRepository(private val dao: PetDao) {

    val firstPet: Flow<Pet?> = dao.getFirstPet()
    val allPets: Flow<List<Pet>> = dao.getAll()

    suspend fun insert(pet: Pet): Long = dao.insert(pet)
    suspend fun update(pet: Pet) = dao.update(pet)
}
