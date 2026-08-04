package com.petmed.app.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.data.database.AppDatabase
import com.petmed.app.data.model.MedicationRecord
import com.petmed.app.data.model.Pet
import com.petmed.app.data.repository.FirestoreRepository
import com.petmed.app.data.repository.MedicationRepository
import com.petmed.app.data.repository.UploadConsentStore
import com.petmed.app.notification.AlarmScheduler
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MedicationViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: MedicationRepository

    val records: StateFlow<List<MedicationRecord>>
    val nextUpcoming: StateFlow<MedicationRecord?>

    init {
        val dao = AppDatabase.getInstance(application).medicationRecordDao()
        repository = MedicationRepository(dao)
        records = repository.allRecords.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
        nextUpcoming = repository.allRecords
            .map { list ->
                list.filter { !it.isRepeat && !it.isCompleted && it.dateTime.isNotBlank() }
                    .minByOrNull { it.dateTime }
            }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
    }

    fun insert(record: MedicationRecord) {
        viewModelScope.launch {
            val newId = repository.insert(record).toInt()
            val recordWithId = record.copy(id = newId)
            if (record.isRepeat) {
                if (record.isRepeatActive) AlarmScheduler.scheduleRepeat(getApplication(), recordWithId)
            } else {
                AlarmScheduler.schedule(getApplication(), recordWithId)
            }
        }
    }

    fun update(record: MedicationRecord, pet: Pet? = null) {
        viewModelScope.launch {
            repository.update(record)
            if (record.isCompleted && !record.isRepeat &&
                UploadConsentStore.hasConsented(getApplication())
            ) {
                runCatching { FirestoreRepository.uploadMedicationLog(record, pet) }
            }
        }
    }

    fun toggleRepeatActive(record: MedicationRecord, active: Boolean) {
        val updated = record.copy(isRepeatActive = active)
        viewModelScope.launch {
            repository.update(updated)
            if (active) AlarmScheduler.scheduleRepeat(getApplication(), updated)
            else AlarmScheduler.cancelRepeat(getApplication(), record)
        }
    }

    fun delete(record: MedicationRecord) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), record)
            repository.delete(record)
        }
    }
}
