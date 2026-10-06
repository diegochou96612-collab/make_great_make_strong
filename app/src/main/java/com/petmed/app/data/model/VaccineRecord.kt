package com.petmed.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 飼主記錄的「某隻寵物哪天打了哪一項疫苗或做了哪項檢測」。itemKey 對應 VaccineGuide 的項目。 */
@Entity(tableName = "vaccine_records", indices = [Index("petId")])
data class VaccineRecord(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Int,
    val itemKey: String,
    /** yyyy-MM-dd */
    val doneDate: String,
    val note: String = "",
    val createdAt: Long = System.currentTimeMillis()
)
