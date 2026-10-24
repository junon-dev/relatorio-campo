package br.app.relatoriocampo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Upsert
import br.app.relatoriocampo.domain.Contact
import br.app.relatoriocampo.domain.Entry
import br.app.relatoriocampo.domain.MonthData
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "entries")
data class EntryEntity(
    @PrimaryKey val id: String,
    val date: String,
    val min: Int,
    val kind: String,
    val note: String,
)

@Entity(tableName = "contacts")
data class ContactEntity(
    @PrimaryKey val id: String,
    val name: String,
    val kind: String,
    val notes: String,
    val active: Boolean,
)

@Entity(tableName = "months")
data class MonthEntity(
    @PrimaryKey val month: String,
    val cat: String?,
    val participated: Boolean?,
    val studies: Int?,
    val notes: String?,
    val mode: String?,
    val totField: Int?,
    val totCredit: Int?,
    val auxGoal: Int?,
    val sent: Boolean,
)

fun EntryEntity.toDomain() = Entry(id, date, min, kind, note)
fun Entry.toEntity() = EntryEntity(id, date, min, kind, note)
fun ContactEntity.toDomain() = Contact(id, name, kind, notes, active)
fun Contact.toEntity() = ContactEntity(id, name, kind, notes, active)
fun MonthEntity.toDomain() = MonthData(month, cat, participated, studies, notes, mode, totField, totCredit, auxGoal, sent)
fun MonthData.toEntity() = MonthEntity(month, cat, participated, studies, notes, mode, totField, totCredit, auxGoal, sent)

@Dao
abstract class AppDao {
    @Query("SELECT * FROM entries ORDER BY date DESC")
    abstract fun entries(): Flow<List<EntryEntity>>

    @Query("SELECT * FROM contacts")
    abstract fun contacts(): Flow<List<ContactEntity>>

    @Query("SELECT * FROM months")
    abstract fun months(): Flow<List<MonthEntity>>

    @Query("SELECT * FROM entries")
    abstract suspend fun allEntries(): List<EntryEntity>

    @Query("SELECT * FROM contacts")
    abstract suspend fun allContacts(): List<ContactEntity>

    @Query("SELECT * FROM months")
    abstract suspend fun allMonths(): List<MonthEntity>

    @Query("SELECT * FROM months WHERE month = :month")
    abstract suspend fun month(month: String): MonthEntity?

    @Upsert
    abstract suspend fun upsertEntry(e: EntryEntity)

    @Delete
    abstract suspend fun deleteEntry(e: EntryEntity)

    @Upsert
    abstract suspend fun upsertContact(c: ContactEntity)

    @Delete
    abstract suspend fun deleteContact(c: ContactEntity)

    @Upsert
    abstract suspend fun upsertMonth(m: MonthEntity)

    @Query("DELETE FROM entries")
    abstract suspend fun clearEntries()

    @Query("DELETE FROM contacts")
    abstract suspend fun clearContacts()

    @Query("DELETE FROM months")
    abstract suspend fun clearMonths()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertEntries(list: List<EntryEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertContacts(list: List<ContactEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract suspend fun insertMonths(list: List<MonthEntity>)

    @Transaction
    open suspend fun replaceAll(e: List<EntryEntity>, c: List<ContactEntity>, m: List<MonthEntity>) {
        clearEntries(); clearContacts(); clearMonths()
        insertEntries(e); insertContacts(c); insertMonths(m)
    }
}

@Database(entities = [EntryEntity::class, ContactEntity::class, MonthEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        @Volatile private var instance: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "relatorio.db")
                    .build().also { instance = it }
            }
    }
}
