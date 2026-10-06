package com.petmed.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medication_records")
data class MedicationRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val medicationName: String,
    val dateTime: String = "",
    val isCompleted: Boolean = false,
    val notes: String = "",
    val isRepeat: Boolean = false,
    val repeatDays: String = "",      // "2,4,6" = 週一,週三,週五 (Calendar.DAY_OF_WEEK)
    val repeatTime: String = "",      // "18:00"
    val isRepeatActive: Boolean = true
)
