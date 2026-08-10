package com.petmed.app.`data`.dao

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import com.petmed.app.`data`.model.Pet
import javax.`annotation`.processing.Generated
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
public class PetDao_Impl(
  __db: RoomDatabase,
) : PetDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfPet: EntityInsertAdapter<Pet>

  private val __deleteAdapterOfPet: EntityDeleteOrUpdateAdapter<Pet>

  private val __updateAdapterOfPet: EntityDeleteOrUpdateAdapter<Pet>
  init {
    this.__db = __db
    this.__insertAdapterOfPet = object : EntityInsertAdapter<Pet>() {
      protected override fun createQuery(): String =
          "INSERT OR ABORT INTO `pets` (`id`,`name`,`species`,`breed`,`birthDate`,`weight`,`notes`) VALUES (nullif(?, 0),?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: Pet) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.species)
        statement.bindText(4, entity.breed)
        statement.bindText(5, entity.birthDate)
        statement.bindText(6, entity.weight)
        statement.bindText(7, entity.notes)
      }
    }
    this.__deleteAdapterOfPet = object : EntityDeleteOrUpdateAdapter<Pet>() {
      protected override fun createQuery(): String = "DELETE FROM `pets` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Pet) {
        statement.bindLong(1, entity.id.toLong())
      }
    }
    this.__updateAdapterOfPet = object : EntityDeleteOrUpdateAdapter<Pet>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `pets` SET `id` = ?,`name` = ?,`species` = ?,`breed` = ?,`birthDate` = ?,`weight` = ?,`notes` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: Pet) {
        statement.bindLong(1, entity.id.toLong())
        statement.bindText(2, entity.name)
        statement.bindText(3, entity.species)
        statement.bindText(4, entity.breed)
        statement.bindText(5, entity.birthDate)
        statement.bindText(6, entity.weight)
        statement.bindText(7, entity.notes)
        statement.bindLong(8, entity.id.toLong())
      }
    }
  }

  public override suspend fun insert(pet: Pet): Long = performSuspending(__db, false, true) {
      _connection ->
    val _result: Long = __insertAdapterOfPet.insertAndReturnId(_connection, pet)
    _result
  }

  public override suspend fun delete(pet: Pet): Unit = performSuspending(__db, false, true) {
      _connection ->
    __deleteAdapterOfPet.handle(_connection, pet)
  }

  public override suspend fun update(pet: Pet): Unit = performSuspending(__db, false, true) {
      _connection ->
    __updateAdapterOfPet.handle(_connection, pet)
  }

  public override fun getFirstPet(): Flow<Pet?> {
    val _sql: String = "SELECT * FROM pets ORDER BY id ASC LIMIT 1"
    return createFlow(__db, false, arrayOf("pets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfSpecies: Int = getColumnIndexOrThrow(_stmt, "species")
        val _columnIndexOfBreed: Int = getColumnIndexOrThrow(_stmt, "breed")
        val _columnIndexOfBirthDate: Int = getColumnIndexOrThrow(_stmt, "birthDate")
        val _columnIndexOfWeight: Int = getColumnIndexOrThrow(_stmt, "weight")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _result: Pet?
        if (_stmt.step()) {
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpSpecies: String
          _tmpSpecies = _stmt.getText(_columnIndexOfSpecies)
          val _tmpBreed: String
          _tmpBreed = _stmt.getText(_columnIndexOfBreed)
          val _tmpBirthDate: String
          _tmpBirthDate = _stmt.getText(_columnIndexOfBirthDate)
          val _tmpWeight: String
          _tmpWeight = _stmt.getText(_columnIndexOfWeight)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          _result = Pet(_tmpId,_tmpName,_tmpSpecies,_tmpBreed,_tmpBirthDate,_tmpWeight,_tmpNotes)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAll(): Flow<List<Pet>> {
    val _sql: String = "SELECT * FROM pets ORDER BY id ASC"
    return createFlow(__db, false, arrayOf("pets")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _columnIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _columnIndexOfName: Int = getColumnIndexOrThrow(_stmt, "name")
        val _columnIndexOfSpecies: Int = getColumnIndexOrThrow(_stmt, "species")
        val _columnIndexOfBreed: Int = getColumnIndexOrThrow(_stmt, "breed")
        val _columnIndexOfBirthDate: Int = getColumnIndexOrThrow(_stmt, "birthDate")
        val _columnIndexOfWeight: Int = getColumnIndexOrThrow(_stmt, "weight")
        val _columnIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _result: MutableList<Pet> = mutableListOf()
        while (_stmt.step()) {
          val _item: Pet
          val _tmpId: Int
          _tmpId = _stmt.getLong(_columnIndexOfId).toInt()
          val _tmpName: String
          _tmpName = _stmt.getText(_columnIndexOfName)
          val _tmpSpecies: String
          _tmpSpecies = _stmt.getText(_columnIndexOfSpecies)
          val _tmpBreed: String
          _tmpBreed = _stmt.getText(_columnIndexOfBreed)
          val _tmpBirthDate: String
          _tmpBirthDate = _stmt.getText(_columnIndexOfBirthDate)
          val _tmpWeight: String
          _tmpWeight = _stmt.getText(_columnIndexOfWeight)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_columnIndexOfNotes)
          _item = Pet(_tmpId,_tmpName,_tmpSpecies,_tmpBreed,_tmpBirthDate,_tmpWeight,_tmpNotes)
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
