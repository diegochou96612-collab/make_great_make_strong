package com.petmed.app.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.petmed.app.data.model.Hospital
import com.petmed.app.data.repository.HospitalRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HospitalViewModel : ViewModel() {

    private val repository = HospitalRepository()

    private val _hospitals = MutableStateFlow<List<Hospital>>(emptyList())
    val hospitals: StateFlow<List<Hospital>> = _hospitals

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    init {
        fetchHospitals()
    }

    fun fetchHospitals() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _hospitals.value = repository.fetchHospitals()
            } catch (e: Exception) {
                _error.value = "載入失敗，請檢查網路連線"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
