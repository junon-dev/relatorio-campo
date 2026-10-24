package br.app.relatoriocampo.data

import android.content.Context
import android.content.SharedPreferences
import br.app.relatoriocampo.domain.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Configurações guardadas em SharedPreferences e expostas como StateFlow. */
class SettingsStore private constructor(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val state = MutableStateFlow(read())
    val flow: StateFlow<Settings> = state.asStateFlow()
    val value: Settings get() = state.value

    @Synchronized
    fun update(transform: (Settings) -> Settings) {
        val next = transform(state.value)
        write(next)
        state.value = next
    }

    private fun read(): Settings {
        val d = Settings()
        return Settings(
            name = prefs.getString("name", d.name) ?: "",
            group = prefs.getString("group", d.group) ?: "",
            category = prefs.getString("category", d.category) ?: d.category,
            annualGoal = prefs.getInt("annualGoal", d.annualGoal),
            creditLimit = prefs.getInt("creditLimit", d.creditLimit),
            startMonth = prefs.getString("startMonth", d.startMonth) ?: "",
            carry = prefs.getBoolean("carry", d.carry),
            lang = prefs.getString("lang", d.lang) ?: d.lang,
            theme = prefs.getString("theme", d.theme) ?: d.theme,
            entryMode = prefs.getString("entryMode", d.entryMode) ?: d.entryMode,
            remEnabled = prefs.getBoolean("remEnabled", d.remEnabled),
            remEnd = prefs.getInt("remEnd", d.remEnd),
            remDaily = prefs.getBoolean("remDaily", d.remDaily),
            remSendDay = prefs.getInt("remSendDay", d.remSendDay),
            remHour = prefs.getInt("remHour", d.remHour),
            remMinute = prefs.getInt("remMinute", d.remMinute),
            timerStart = if (prefs.contains("timerStart")) prefs.getLong("timerStart", 0L) else null,
            askedNotif = prefs.getBoolean("askedNotif", d.askedNotif),
        )
    }

    private fun write(s: Settings) {
        prefs.edit().apply {
            putString("name", s.name); putString("group", s.group); putString("category", s.category)
            putInt("annualGoal", s.annualGoal); putInt("creditLimit", s.creditLimit); putString("startMonth", s.startMonth)
            putBoolean("carry", s.carry); putString("lang", s.lang); putString("theme", s.theme); putString("entryMode", s.entryMode)
            putBoolean("remEnabled", s.remEnabled); putInt("remEnd", s.remEnd); putBoolean("remDaily", s.remDaily)
            putInt("remSendDay", s.remSendDay); putInt("remHour", s.remHour); putInt("remMinute", s.remMinute)
            if (s.timerStart != null) putLong("timerStart", s.timerStart) else remove("timerStart")
            putBoolean("askedNotif", s.askedNotif)
        }.apply()
    }

    companion object {
        @Volatile private var instance: SettingsStore? = null
        fun get(context: Context): SettingsStore =
            instance ?: synchronized(this) { instance ?: SettingsStore(context.applicationContext).also { instance = it } }
    }
}
