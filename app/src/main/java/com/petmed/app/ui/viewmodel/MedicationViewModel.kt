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
                val candidates = mutableListOf<Pair<MedicationRecord, Long>>()
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault())
                list.filter { !it.isRepeat && !it.isCompleted && it.dateTime.isNotBlank() }
                    .forEach { r ->
                        val ms = runCatching { sdf.parse(r.dateTime)?.time }.getOrNull()
                            ?: Long.MAX_VALUE
                        candidates.add(r to ms)
                    }
                list.filter { it.isRepeat && it.isRepeatActive && it.repeatDays.isNotBlank() && it.repeatTime.isNotBlank() }
                    .forEach { r ->
                        val parts = r.repeatTime.split(":")
                        val h = parts.getOrNull(0)?.toIntOrNull() ?: return@forEach
                        val m = parts.getOrNull(1)?.toIntOrNull() ?: return@forEach
                        val soonest = r.repeatDays.split(",")
                            .mapNotNull { it.trim().toIntOrNull() }
                            .minOfOrNull { day -> nextOccurrenceMs(day, h, m) }
                            ?: return@forEach
                        candidates.add(r to soonest)
                    }
                candidates.minByOrNull { it.second }?.first
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

    fun editRecord(old: MedicationRecord, new: MedicationRecord) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), old)
            repository.update(new)
            if (new.isRepeat) {
                if (new.isRepeatActive) AlarmScheduler.scheduleRepeat(getApplication(), new)
            } else {
                if (new.dateTime.isNotBlank()) AlarmScheduler.schedule(getApplication(), new)
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

    private fun nextOccurrenceMs(dayOfWeek: Int, hour: Int, minute: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, hour)
            set(java.util.Calendar.MINUTE, minute)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val current = cal.get(java.util.Calendar.DAY_OF_WEEK)
        var daysToAdd = (dayOfWeek - current + 7) % 7
        if (daysToAdd == 0 && cal.timeInMillis <= System.currentTimeMillis()) daysToAdd = 7
        cal.add(java.util.Calendar.DAY_OF_YEAR, daysToAdd)
        return cal.timeInMillis
    }

    fun delete(record: MedicationRecord) {
        viewModelScope.launch {
            AlarmScheduler.cancel(getApplication(), record)
            repository.delete(record)
        }
    }
}
