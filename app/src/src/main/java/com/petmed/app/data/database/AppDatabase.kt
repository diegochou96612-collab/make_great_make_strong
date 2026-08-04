package com.petmed.app.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.petmed.app.data.dao.MedicationRecordDao
import com.petmed.app.data.dao.PetDao
import com.petmed.app.data.model.MedicationRecord
import com.petmed.app.data.model.Pet

@Database(entities = [MedicationRecord::class, Pet::class], version = 3, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun medicationRecordDao(): MedicationRecordDao
    abstract fun petDao(): PetDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                Room.databaseBuilder(context, AppDatabase::class.java, "petmed.db")
                    .fallbackToDestructiveMigration(true)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
