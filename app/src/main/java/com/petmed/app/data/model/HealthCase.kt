package com.petmed.app.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/** 一次寵物狀況紀錄（問診、每日紀錄、照片都會掛在它底下）。寵物資料為建立當下的快照。 */
@Entity(tableName = "health_cases")
data class HealthCase(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val petId: Int = 0,
    val petName: String = "",
    val species: String = "",
    val breed: String = "",
    val birthDate: String = "",
    val title: String = "",
    val status: String = STATUS_INTERVIEWING,
    /** AI 依欄位整理的客觀摘要（可能為空）。 */
    val narrative: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATUS_INTERVIEWING = "interviewing"
        const val STATUS_DONE = "completed"

        /** 飼主選擇不接續的未完成紀錄：不再主動詢問，但仍可從選單接續。 */
        const val STATUS_ABANDONED = "abandoned"
    }
}

/** 問診收集到的一個欄位答案。飼主可修改，修改後 origin 變為 OWNER_EDITED。 */
@Entity(
    tableName = "case_fields",
    indices = [Index(value = ["caseId", "fieldKey"], unique = true)]
)
data class CaseField(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val fieldKey: String,
    val section: String,
    val label: String,
    val value: String,
    /** ANSWERED / UNKNOWN / NOT_PROVIDED */
    val state: String,
    /** OWNER / OWNER_EDITED / AUTO */
    val origin: String,
    /** 題目出處（供可追溯） */
    val sourceRef: String,
    val sortOrder: Int,
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val STATE_ANSWERED = "ANSWERED"
        const val STATE_UNKNOWN = "UNKNOWN"
        const val STATE_NOT_PROVIDED = "NOT_PROVIDED"
        const val ORIGIN_OWNER = "OWNER"
        const val ORIGIN_EDITED = "OWNER_EDITED"
        const val ORIGIN_AUTO = "AUTO"
    }
}

/**
 * 每日紀錄：一個狀況、一天一筆。追蹤項目依 WSAVA 營養評估檢核表「建議客戶持續關注」清單
 * （體重、進食狀態、食慾、消化道症狀、活力、外觀整體狀況），睡眠為業師建議的補充項目（無文獻）。
 */
@Entity(
    tableName = "daily_logs",
    indices = [Index(value = ["caseId", "date"], unique = true)]
)
data class DailyLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    /** yyyy-MM-dd */
    val date: String,
    val appetite: String = "",
    val water: String = "",
    val stool: String = "",
    val urine: String = "",
    val energy: String = "",
    val sleep: String = "",
    val vomitCount: String = "",
    val weight: String = "",
    val note: String = "",
    val updatedAt: Long = System.currentTimeMillis()
)

/** 照片：同一個狀況的照片組成一個相簿。檔案存在 App 私有資料夾，已重新編碼（不含位置等 EXIF）。 */
@Entity(
    tableName = "case_photos",
    indices = [Index(value = ["caseId"])]
)
data class CasePhoto(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val filePath: String,
    val note: String = "",
    val topic: String = "",
    val takenAt: Long = System.currentTimeMillis()
)

/** 稽核事件：問了什麼、答了什麼、改了什麼、觸發過什麼緊急提示。 */
@Entity(
    tableName = "case_events",
    indices = [Index(value = ["caseId"])]
)
data class CaseEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val caseId: Long,
    val at: Long = System.currentTimeMillis(),
    /** ASKED / ANSWERED / EDITED / RED_FLAG / AI_SUMMARY / EXPORTED */
    val kind: String,
    val detail: String
) {
    companion object {
        const val ASKED = "ASKED"
        const val ANSWERED = "ANSWERED"
        const val EDITED = "EDITED"
        const val RED_FLAG = "RED_FLAG"
        const val AI_SUMMARY = "AI_SUMMARY"
        const val EXPORTED = "EXPORTED"

        /** 自由對話的逐句紀錄：detail 以「U｜」（飼主）或「B｜」（助理）開頭。供接續與稽核。 */
        const val CHAT = "CHAT"

        /** AI 從對話中整理出並填入欄位（不在還原對話時顯示）。 */
        const val FILLED = "FILLED"
    }
}
