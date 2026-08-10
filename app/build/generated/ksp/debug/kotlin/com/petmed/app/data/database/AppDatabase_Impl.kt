package com.petmed.app.`data`.database

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import com.petmed.app.`data`.dao.MedicationRecordDao
import com.petmed.app.`data`.dao.MedicationRecordDao_Impl
import com.petmed.app.`data`.dao.PetDao
import com.petmed.app.`data`.dao.PetDao_Impl
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _medicationRecordDao: Lazy<MedicationRecordDao> = lazy {
    MedicationRecordDao_Impl(this)
  }

  private val _petDao: Lazy<PetDao> = lazy {
    PetDao_Impl(this)
  }

  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(3,
        "76aae5a8e7c57c48585916df6cb9b504", "0bd57de3386c39a997957cbf5d83c404") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `medication_records` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `medicationName` TEXT NOT NULL, `dateTime` TEXT NOT NULL, `isCompleted` INTEGER NOT NULL, `notes` TEXT NOT NULL, `isRepeat` INTEGER NOT NULL, `repeatDays` TEXT NOT NULL, `repeatTime` TEXT NOT NULL, `isRepeatActive` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `pets` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `name` TEXT NOT NULL, `species` TEXT NOT NULL, `breed` TEXT NOT NULL, `birthDate` TEXT NOT NULL, `weight` TEXT NOT NULL, `notes` TEXT NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '76aae5a8e7c57c48585916df6cb9b504')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `medication_records`")
        connection.execSQL("DROP TABLE IF EXISTS `pets`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsMedicationRecords: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsMedicationRecords.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("medicationName", TableInfo.Column("medicationName", "TEXT",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("dateTime", TableInfo.Column("dateTime", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("isCompleted", TableInfo.Column("isCompleted", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("isRepeat", TableInfo.Column("isRepeat", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("repeatDays", TableInfo.Column("repeatDays", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("repeatTime", TableInfo.Column("repeatTime", "TEXT", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsMedicationRecords.put("isRepeatActive", TableInfo.Column("isRepeatActive",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysMedicationRecords: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesMedicationRecords: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoMedicationRecords: TableInfo = TableInfo("medication_records",
            _columnsMedicationRecords, _foreignKeysMedicationRecords, _indicesMedicationRecords)
        val _existingMedicationRecords: TableInfo = read(connection, "medication_records")
        if (!_infoMedicationRecords.equals(_existingMedicationRecords)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |medication_records(com.petmed.app.data.model.MedicationRecord).
              | Expected:
              |""".trimMargin() + _infoMedicationRecords + """
              |
              | Found:
              |""".trimMargin() + _existingMedicationRecords)
        }
        val _columnsPets: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsPets.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPets.put("name", TableInfo.Column("name", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPets.put("species", TableInfo.Column("species", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPets.put("breed", TableInfo.Column("breed", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPets.put("birthDate", TableInfo.Column("birthDate", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPets.put("weight", TableInfo.Column("weight", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsPets.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysPets: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesPets: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoPets: TableInfo = TableInfo("pets", _columnsPets, _foreignKeysPets, _indicesPets)
        val _existingPets: TableInfo = read(connection, "pets")
        if (!_infoPets.equals(_existingPets)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |pets(com.petmed.app.data.model.Pet).
              | Expected:
              |""".trimMargin() + _infoPets + """
              |
              | Found:
              |""".trimMargin() + _existingPets)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "medication_records", "pets")
  }

  public override fun clearAllTables() {
    super.performClear(false, "medication_records", "pets")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(MedicationRecordDao::class,
        MedicationRecordDao_Impl.getRequiredConverters())
    _typeConvertersMap.put(PetDao::class, PetDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun medicationRecordDao(): MedicationRecordDao = _medicationRecordDao.value

  public override fun petDao(): PetDao = _petDao.value
}
