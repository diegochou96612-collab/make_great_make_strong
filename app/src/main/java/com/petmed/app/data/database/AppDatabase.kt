package com.petmed.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.petmed.app.data.dao.CaseDao
import com.petmed.app.data.dao.ChatHistoryDao
import com.petmed.app.data.dao.MedicationRecordDao
import com.petmed.app.data.dao.PetDao
import com.petmed.app.data.dao.VaccineDao
import com.petmed.app.data.model.VaccineRecord
import com.petmed.app.data.model.CaseEvent
import com.petmed.app.data.model.CaseField
import com.petmed.app.data.model.CasePhoto
import com.petmed.app.data.model.DailyLog
import com.petmed.app.data.model.ChatMessageEntity
import com.petmed.app.data.model.ChatSessionEntity
import com.petmed.app.data.model.HealthCase
import com.petmed.app.data.model.MedicationRecord
import com.petmed.app.data.model.Pet

@Database(
    entities = [
        MedicationRecord::class, Pet::class, ChatSessionEntity::class, ChatMessageEntity::class,
        HealthCase::class, CaseField::class, CaseEvent::class, DailyLog::class, CasePhoto::class,
        VaccineRecord::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationRecordDao(): MedicationRecordDao
    abstract fun petDao(): PetDao
    abstract fun chatHistoryDao(): ChatHistoryDao
    abstract fun caseDao(): CaseDao
    abstract fun vaccineDao(): VaccineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /** 4 → 5：新增「狀況紀錄」三張表，不動既有資料。SQL 與 Room 產生的版本一致。 */
        private val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `health_cases` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `petId` INTEGER NOT NULL, `petName` TEXT NOT NULL, `species` TEXT NOT NULL, `breed` TEXT NOT NULL, `birthDate` TEXT NOT NULL, `title` TEXT NOT NULL, `status` TEXT NOT NULL, `narrative` TEXT NOT NULL, `createdAt` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `case_fields` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `fieldKey` TEXT NOT NULL, `section` TEXT NOT NULL, `label` TEXT NOT NULL, `value` TEXT NOT NULL, `state` TEXT NOT NULL, `origin` TEXT NOT NULL, `sourceRef` TEXT NOT NULL, `sortOrder` INTEGER NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_case_fields_caseId_fieldKey` ON `case_fields` (`caseId`, `fieldKey`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `case_events` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `at` INTEGER NOT NULL, `kind` TEXT NOT NULL, `detail` TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_case_events_caseId` ON `case_events` (`caseId`)")
            }
        }

        /** 5 → 6：新增「每日紀錄」「照片」兩張表，不動既有資料。SQL 與 Room 產生的版本一致。 */
        private val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `daily_logs` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `date` TEXT NOT NULL, `appetite` TEXT NOT NULL, `water` TEXT NOT NULL, `stool` TEXT NOT NULL, `urine` TEXT NOT NULL, `energy` TEXT NOT NULL, `sleep` TEXT NOT NULL, `vomitCount` TEXT NOT NULL, `weight` TEXT NOT NULL, `note` TEXT NOT NULL, `updatedAt` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_daily_logs_caseId_date` ON `daily_logs` (`caseId`, `date`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `case_photos` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `caseId` INTEGER NOT NULL, `filePath` TEXT NOT NULL, `note` TEXT NOT NULL, `topic` TEXT NOT NULL, `takenAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_case_photos_caseId` ON `case_photos` (`caseId`)")
            }
        }

        /** 6 → 7：新增「疫苗與檢測紀錄」一張表，不動既有資料。 */
        private val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `vaccine_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `petId` INTEGER NOT NULL, `itemKey` TEXT NOT NULL, `doneDate` TEXT NOT NULL, `note` TEXT NOT NULL, `createdAt` INTEGER NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_vaccine_records_petId` ON `vaccine_records` (`petId`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "petmed.db")
                    .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
