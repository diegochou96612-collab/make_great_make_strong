package com.petmed.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "pets")
data class Pet(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val species: String,
    val breed: String = "",
    val birthDate: String = "",
    val weight: String = "",
    val notes: String = ""
)
