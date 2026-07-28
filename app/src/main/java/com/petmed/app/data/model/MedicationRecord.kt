package com.petmed.app.data.model

data class MedicationRecord(
    val id: Int,
    val medicationName: String,
    val dateTime: String,
    val isCompleted: Boolean = false,
    val notes: String = ""
)
