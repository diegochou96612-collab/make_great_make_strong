package com.petmed.app.`data`.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.petmed.app.`data`.model.MedicationRecord
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class MedicationRecordDao_Impl(
  __db: RoomDatabase,
) : MedicationRecordDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfMedicationRecord: EntityInsertAdapter<MedicationRecord>

  private val __deleteAdapterOfMedicationRecord: EntityDeleteOrUpdateAdapter<MedicationRecord>

  private val __updateAdapterOfMedicationRecord: EntityDeleteOrUpdateAdapter<MedicationRecord>
  init {
    this.__db = __db
    this.__insertAdapterOfMedicationRecord = object : EntityInsertAdapter<MedicationRecord>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `medication_records` (`id`,`medicationName`,`dateTime`,`isCompleted`,`notes`,`isRepeat`,`repeatDays`,`repeatTime`,`isRepeatActive`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: MedicationRecord) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.medicationName)
        statement.bindText(3, entity.dateTime)
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindText(5, entity.notes)
        val _tmp_1: Int = if (entity.isRepeat) 1 else 0
        statement.bindLong(6, _tmp_1.toLong())
        statement.bindText(7, entity.repeatDays)
        statement.bindText(8, entity.repeatTime)
        val _tmp_2: Int = if (entity.isRepeatActive) 1 else 0
        statement.bindLong(9, _tmp_2.toLong())
      }
    }
    this.__deleteAdapterOfMedicationRecord = object :
        EntityDeleteOrUpdateAdapter<MedicationRecord>() {
      protected override fun createQuery(): String =
          "DELETE FROM `medication_records` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MedicationRecord) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
    this.__updateAdapterOfMedicationRecord = object :
        EntityDeleteOrUpdateAdapter<MedicationRecord>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `medication_records` SET `id` = ?,`medicationName` = ?,`dateTime` = ?,`isCompleted` = ?,`notes` = ?,`isRepeat` = ?,`repeatDays` = ?,`repeatTime` = ?,`isRepeatActive` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: MedicationRecord) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.medicationName)
        statement.bindText(3, entity.dateTime)
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(4, _tmp.toLong())
        statement.bindText(5, entity.notes)
        val _tmp_1: Int = if (entity.isRepeat) 1 else 0
        statement.bindLong(6, _tmp_1.toLong())
        statement.bindText(7, entity.repeatDays)
        statement.bindText(8, entity.repeatTime)
        val _tmp_2: Int = if (entity.isRepeatActive) 1 else 0
        statement.bindLong(9, _tmp_2.toLong())
        statement.bindLong(10, entity.id.toLong())
      }
    }
  }

  public override suspend fun insert(record: MedicationRecord): Long = performSuspending(__db,
      false, true) { _connection ->
    val _result: Long = __insertAdapterOfMedicationRecord.insertAndReturnId(_connection, record)
    _result
  }

  public override suspend fun delete(record: MedicationRecord): Unit = performSuspending(__db,
      false, true) { _connection ->
    __deleteAdapterOfMedicationRecord.handle(_connection, record)
  }

  public override suspend fun update(record: MedicationRecord): Unit = performSuspending(__db,
      false, true) { _connection ->
    __updateAdapterOfMedicationRecord.handle(_connection, record)
  }

  public override fun getAll(): Flow<List<MedicationRecord>> {
    val _sql: String = "SELECT * FROM medication_records ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("medication_records")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfMedicationName: Int = getColumnIndexOrThrow(_stmt, "medicationName")
        val _columnIndexOfDateTime: Int = getColumnIndexOrThrow(_stmt, "dateTime")
        val _columnIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _columnIndexOfIsRepeat: Int = getColumnIndexOrThrow(_stmt, "isRepeat")
        val _columnIndexOfRepeatDays: Int = getColumnIndexOrThrow(_stmt, "repeatDays")
        val _columnIndexOfRepeatTime: Int = getColumnIndexOrThrow(_stmt, "repeatTime")
        val _columnIndexOfIsRepeatActive: Int = getColumnIndexOrThrow(_stmt, "isRepeatActive")
        val _result: MutableList<MedicationRecord> = mutableListOf()
        while (_stmt.step()) {
          val _item: MedicationRecord
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpMedicationName: String
          _tmpMedicationName = _stmt.getText(_columnIndexOfMedicationName)
          val _tmpDateTime: String
          _tmpDateTime = _stmt.getText(_columnIndexOfDateTime)
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_columnIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          val _tmpIsRepeat: Boolean
          val _tmp_1: Int
          _tmp_1 = _stmt.getLong(_columnIndexOfIsRepeat).toInt()
          _tmpIsRepeat = _tmp_1 != 0
          val _tmpRepeatDays: String
          _tmpRepeatDays = _stmt.getText(_columnIndexOfRepeatDays)
          val _tmpRepeatTime: String
          _tmpRepeatTime = _stmt.getText(_columnIndexOfRepeatTime)
          val _tmpIsRepeatActive: Boolean
          val _tmp_2: Int
          _tmp_2 = _stmt.getLong(_columnIndexOfIsRepeatActive).toInt()
          _tmpIsRepeatActive = _tmp_2 != 0
          _item =
              MedicationRecord(_tmpId,_tmpMedicationName,_tmpDateTime,_tmpIsCompleted,_tmpNotes,_tmpIsRepeat,_tmpRepeatDays,_tmpRepeatTime,_tmpIsRepeatActive)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
