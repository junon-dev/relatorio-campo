package br.app.relatoriocampo.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import br.app.relatoriocampo.data.AppDatabase
import br.app.relatoriocampo.data.Backup
import br.app.relatoriocampo.data.SettingsStore
import br.app.relatoriocampo.data.toDomain
import br.app.relatoriocampo.data.toEntity
import br.app.relatoriocampo.domain.Contact
import br.app.relatoriocampo.domain.Entry
import br.app.relatoriocampo.domain.MonthData
import br.app.relatoriocampo.domain.Settings
import br.app.relatoriocampo.notify.Reminders
import br.app.relatoriocampo.widget.Widgets
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.UUID

class AppViewModel(app: Application) : AndroidViewModel(app) {
    private val dao = AppDatabase.get(app).dao()
    private val store = SettingsStore.get(app)

    val settings: StateFlow<Settings> = store.flow

    // null = ainda carregando do banco
    val entries: StateFlow<List<Entry>?> = dao.entries().map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val contacts: StateFlow<List<Contact>?> = dao.contacts().map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val months: StateFlow<Map<String, MonthData>?> = dao.months().map { list -> list.associate { it.month to it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Muda quando todos os dados são substituídos (importar / apagar tudo). */
    private val _epoch = MutableStateFlow(0)
    val epoch: StateFlow<Int> = _epoch.asStateFlow()

    private val monthLock = Mutex()

    init {
        // Qualquer mudança nos dados (no app ou nos widgets) atualiza os widgets da tela inicial.
        viewModelScope.launch {
            combine(dao.entries(), dao.months(), store.flow) { _, _, _ -> Unit }
                .collectLatest { delay(400); Widgets.updateAll(getApplication()) }
        }
    }

    private fun newId() = UUID.randomUUID().toString().substring(0, 12)

    // ---------- configurações ----------
    fun updateSettings(transform: (Settings) -> Settings) {
        val before = store.value
        store.update(transform)
        val after = store.value
        val remChanged = before.remEnabled != after.remEnabled || before.remEnd != after.remEnd || before.remDaily != after.remDaily ||
            before.remSendDay != after.remSendDay || before.remHour != after.remHour || before.remMinute != after.remMinute
        if (remChanged) Reminders.scheduleAll(getApplication())
        if (before.lang != after.lang) Reminders.createChannel(getApplication())
    }

    // ---------- mês ----------
    fun updateMonth(month: String, transform: (MonthData) -> MonthData) {
        // Mutex garante que edições rápidas (digitação) sejam gravadas na ordem certa.
        viewModelScope.launch {
            monthLock.withLock {
                val cur = dao.month(month)?.toDomain() ?: MonthData(month)
                dao.upsertMonth(transform(cur).toEntity())
            }
        }
    }

    // ---------- registros ----------
    fun addEntry(date: String, minutes: Int, kind: String, note: String) {
        if (minutes <= 0) return
        viewModelScope.launch { dao.upsertEntry(Entry(newId(), date, minutes, kind, note.trim()).toEntity()) }
    }

    fun deleteEntry(e: Entry) = viewModelScope.launch { dao.deleteEntry(e.toEntity()) }
    fun restoreEntry(e: Entry) = viewModelScope.launch { dao.upsertEntry(e.toEntity()) }

    // ---------- contatos ----------
    fun addContact(name: String, kind: String, notes: String) {
        if (name.isBlank()) return
        viewModelScope.launch { dao.upsertContact(Contact(newId(), name.trim(), kind, notes.trim(), true).toEntity()) }
    }

    fun setContactActive(c: Contact, active: Boolean) = viewModelScope.launch { dao.upsertContact(c.copy(active = active).toEntity()) }
    fun deleteContact(c: Contact) = viewModelScope.launch { dao.deleteContact(c.toEntity()) }
    fun restoreContact(c: Contact) = viewModelScope.launch { dao.upsertContact(c.toEntity()) }

    // ---------- cronômetro ----------
    fun startTimer() = updateSettings { it.copy(timerStart = System.currentTimeMillis()) }
    fun discardTimer() = updateSettings { it.copy(timerStart = null) }

    /** Para o cronômetro e salva os minutos como registro de campo de hoje. Retorna os minutos salvos. */
    fun stopTimer(note: String): Int {
        val start = store.value.timerStart ?: return 0
        val minutes = ((System.currentTimeMillis() - start) / 60_000L).toInt()
        updateSettings { it.copy(timerStart = null) }
        if (minutes > 0) addEntry(LocalDate.now().toString(), minutes, "field", note)
        return minutes
    }

    // ---------- backup ----------
    suspend fun exportBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val json = Backup.toJson(
                store.value,
                dao.allEntries().map { it.toDomain() },
                dao.allContacts().map { it.toDomain() },
                dao.allMonths().map { it.toDomain() },
            )
            writeText(uri, json)
        }.isSuccess
    }

    suspend fun importBackup(uri: Uri): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            val text = getApplication<Application>().contentResolver.openInputStream(uri)!!.use { it.readBytes().toString(Charsets.UTF_8) }
            val data = Backup.fromJson(text, store.value)
            dao.replaceAll(data.entries.map { it.toEntity() }, data.contacts.map { it.toEntity() }, data.months.map { it.toEntity() })
            withContext(Dispatchers.Main) { updateSettings { data.settings }; _epoch.value++ }
            Reminders.scheduleAll(getApplication())
        }.isSuccess
    }

    suspend fun writeText(uri: Uri, text: String): Boolean = withContext(Dispatchers.IO) {
        runCatching {
            getApplication<Application>().contentResolver.openOutputStream(uri, "wt")!!.use { it.write(text.toByteArray(Charsets.UTF_8)) }
        }.isSuccess
    }

    fun clearAll() {
        viewModelScope.launch { dao.replaceAll(emptyList(), emptyList(), emptyList()) }
        val lang = store.value.lang
        updateSettings { Settings(lang = lang, askedNotif = it.askedNotif) }
        _epoch.value++
    }
}
