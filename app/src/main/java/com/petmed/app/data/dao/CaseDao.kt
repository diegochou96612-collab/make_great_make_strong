package com.petmed.app.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.petmed.app.data.model.CaseEvent
import com.petmed.app.data.model.CaseField
import com.petmed.app.data.model.CasePhoto
import com.petmed.app.data.model.DailyLog
import com.petmed.app.data.model.HealthCase
import kotlinx.coroutines.flow.Flow

@Dao
interface CaseDao {

    @Insert
    suspend fun insertCase(c: HealthCase): Long

    @Update
    suspend fun updateCase(c: HealthCase)

    @Query("SELECT * FROM health_cases WHERE id = :id")
    suspend fun getCase(id: Long): HealthCase?

    @Query("SELECT * FROM health_cases WHERE id = :id")
    fun observeCase(id: Long): Flow<HealthCase?>

    @Query("SELECT * FROM health_cases ORDER BY updatedAt DESC")
    fun observeAllCases(): Flow<List<HealthCase>>

    @Query("DELETE FROM health_cases WHERE id = :id")
    suspend fun deleteCase(id: Long)

    /** 最近一份「還沒問完、且沒被放棄」的紀錄，用來詢問是否接續。 */
    @Query("SELECT * FROM health_cases WHERE status = 'interviewing' ORDER BY updatedAt DESC LIMIT 1")
    suspend fun getLatestUnfinished(): HealthCase?

    @Query("SELECT COUNT(*) FROM case_fields WHERE caseId = :caseId AND state != 'NOT_PROVIDED'")
    suspend fun countAnswered(caseId: Long): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertField(f: CaseField)

    @Query("SELECT * FROM case_fields WHERE caseId = :caseId ORDER BY sortOrder ASC")
    fun observeFields(caseId: Long): Flow<List<CaseField>>

    @Query("SELECT * FROM case_fields WHERE caseId = :caseId ORDER BY sortOrder ASC")
    suspend fun getFields(caseId: Long): List<CaseField>

    @Query("DELETE FROM case_fields WHERE caseId = :caseId")
    suspend fun deleteFields(caseId: Long)

    // ───── 每日紀錄 ─────
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertLog(l: DailyLog)

    @Query("SELECT * FROM daily_logs WHERE caseId = :caseId ORDER BY date DESC")
    fun observeLogs(caseId: Long): Flow<List<DailyLog>>

    @Query("SELECT * FROM daily_logs WHERE caseId = :caseId ORDER BY date DESC")
    suspend fun getLogs(caseId: Long): List<DailyLog>

    @Query("DELETE FROM daily_logs WHERE id = :id")
    suspend fun deleteLog(id: Long)

    @Query("DELETE FROM daily_logs WHERE caseId = :caseId")
    suspend fun deleteLogs(caseId: Long)

    // ───── 照片 ─────
    @Insert
    suspend fun insertPhoto(p: CasePhoto): Long

    @Update
    suspend fun updatePhoto(p: CasePhoto)

    @Query("SELECT * FROM case_photos WHERE caseId = :caseId ORDER BY takenAt ASC")
    fun observePhotos(caseId: Long): Flow<List<CasePhoto>>

    @Query("SELECT * FROM case_photos WHERE caseId = :caseId ORDER BY takenAt ASC")
    suspend fun getPhotos(caseId: Long): List<CasePhoto>

    @Query("DELETE FROM case_photos WHERE id = :id")
    suspend fun deletePhoto(id: Long)

    @Query("DELETE FROM case_photos WHERE caseId = :caseId")
    suspend fun deletePhotos(caseId: Long)

    @Insert
    suspend fun insertEvent(e: CaseEvent)

    @Query("SELECT * FROM case_events WHERE caseId = :caseId ORDER BY at ASC")
    suspend fun getEvents(caseId: Long): List<CaseEvent>

    @Query("DELETE FROM case_events WHERE caseId = :caseId")
    suspend fun deleteEvents(caseId: Long)
}
