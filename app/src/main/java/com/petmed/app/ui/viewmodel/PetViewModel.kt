package com.petmed.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.data.database.AppDatabase
import com.petmed.app.data.model.Pet
import com.petmed.app.data.repository.PetRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class PetViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PetRepository

    val firstPet: StateFlow<Pet?>
    val allPets: StateFlow<List<Pet>>

    init {
        val dao = AppDatabase.getInstance(application).petDao()
        repository = PetRepository(dao)
        firstPet = repository.firstPet.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )
        allPets = repository.allPets.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    }

    fun insertOrUpdate(pet: Pet) {
        viewModelScope.launch {
            if (pet.id == 0) repository.insert(pet)
            else repository.update(pet)
        }
    }
}
