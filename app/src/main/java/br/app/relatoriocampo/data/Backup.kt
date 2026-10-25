package br.app.relatoriocampo.data

import br.app.relatoriocampo.domain.Contact
import br.app.relatoriocampo.domain.Entry
import br.app.relatoriocampo.domain.MonthCalc
import br.app.relatoriocampo.domain.MonthData
import br.app.relatoriocampo.domain.Settings
import org.json.JSONArray
import org.json.JSONObject

/**
 * Backup em JSON no mesmo formato da versão HTML (relatorio-servico-campo.html),
 * para que os dados possam ir e voltar entre as duas versões.
 */
object Backup {
    data class Data(val settings: Settings, val entries: List<Entry>, val contacts: List<Contact>, val months: List<MonthData>)

    fun toJson(s: Settings, entries: List<Entry>, contacts: List<Contact>, months: List<MonthData>): String {
        val root = JSONObject()
        root.put("settings", JSONObject().apply {
            put("name", s.name); put("group", s.group); put("category", s.category)
            put("annualGoal", s.annualGoal); put("creditLimit", s.creditLimit); put("startMonth", s.startMonth)
            put("carry", s.carry); put("lang", s.lang); put("theme", s.theme); put("entryMode", s.entryMode)
            put("remEnd", s.remEnd); put("remDaily", s.remDaily); put("remSendDay", s.remSendDay)
            put("remTime", "%02d:%02d".format(s.remHour, s.remMinute))
            put("widgetOpacity", s.widgetOpacity)
        })
        root.put("entries", JSONArray().apply {
            entries.forEach { e -> put(JSONObject().put("id", e.id).put("date", e.date).put("min", e.min).put("kind", e.kind).put("note", e.note)) }
        })
        root.put("contacts", JSONArray().apply {
            contacts.forEach { c -> put(JSONObject().put("id", c.id).put("name", c.name).put("kind", c.kind).put("notes", c.notes).put("active", c.active)) }
        })
        root.put("months", JSONObject().apply {
            months.forEach { m ->
                put(m.month, JSONObject().apply {
                    m.cat?.let { put("cat", it) }
                    m.participated?.let { put("participated", it) }
                    m.studies?.let { put("studies", it) }
                    m.notes?.let { put("notes", it) }
                    m.mode?.let { put("mode", it) }
                    m.totField?.let { put("totField", it) }
                    m.totCredit?.let { put("totCredit", it) }
                    m.auxGoal?.let { put("auxGoal", it) }
                    put("sent", m.sent)
                })
            }
        })
        root.put("timer", s.timerStart ?: JSONObject.NULL)
        return root.toString(2)
    }

    /** Lê um backup. Lança exceção se o arquivo não tiver o formato esperado. */
    fun fromJson(text: String, current: Settings): Data {
        val root = JSONObject(text)
        val so = root.getJSONObject("settings")
        val ea = root.getJSONArray("entries")
        fun JSONObject.optStr(k: String, d: String) = if (has(k) && !isNull(k)) optString(k, d) else d
        fun JSONObject.optIntOrNull(k: String): Int? = if (has(k) && !isNull(k)) optInt(k) else null
        fun JSONObject.optBoolOrNull(k: String): Boolean? = if (has(k) && !isNull(k)) optBoolean(k) else null

        val time = so.optStr("remTime", "09:00").split(":")
        val settings = current.copy(
            name = so.optStr("name", ""), group = so.optStr("group", ""), category = so.optStr("category", "pub"),
            annualGoal = so.optInt("annualGoal", 600), creditLimit = so.optInt("creditLimit", 55),
            startMonth = so.optStr("startMonth", ""), carry = so.optBoolean("carry", true),
            lang = so.optStr("lang", current.lang), theme = so.optStr("theme", "auto"), entryMode = so.optStr("entryMode", "daily"),
            remEnd = so.optInt("remEnd", 5), remDaily = so.optBoolean("remDaily", false), remSendDay = so.optInt("remSendDay", 1),
            remHour = time.getOrNull(0)?.toIntOrNull() ?: 9, remMinute = time.getOrNull(1)?.toIntOrNull() ?: 0,
            widgetOpacity = so.optInt("widgetOpacity", current.widgetOpacity),
            timerStart = if (root.has("timer") && !root.isNull("timer")) root.optLong("timer") else null,
        )
        val entries = (0 until ea.length()).map { i ->
            val o = ea.getJSONObject(i)
            Entry(o.getString("id"), o.getString("date"), o.getInt("min"), o.optStr("kind", "field"), o.optStr("note", ""))
        }
        val ca = root.optJSONArray("contacts") ?: JSONArray()
        val contacts = (0 until ca.length()).map { i ->
            val o = ca.getJSONObject(i)
            Contact(o.getString("id"), o.optStr("name", ""), o.optStr("kind", "study"), o.optStr("notes", ""), o.optBoolean("active", true))
        }
        val mo = root.optJSONObject("months") ?: JSONObject()
        val months = mo.keys().asSequence().map { k ->
            val o = mo.getJSONObject(k)
            MonthData(
                month = k, cat = o.optStr("cat", "").ifEmpty { null }, participated = o.optBoolOrNull("participated"),
                studies = o.optIntOrNull("studies"), notes = o.optStr("notes", "").ifEmpty { null }, mode = o.optStr("mode", "").ifEmpty { null },
                totField = o.optIntOrNull("totField"), totCredit = o.optIntOrNull("totCredit"), auxGoal = o.optIntOrNull("auxGoal"),
                sent = o.optBoolean("sent", false),
            )
        }.toList()
        return Data(settings, entries, contacts, months)
    }

    /** Relatório de um mês em JSON (para importar no organizador S-21). */
    fun monthJson(m: String, c: MonthCalc, s: Settings, notes: String?): String = JSONObject().apply {
        put("type", "relatorio-servico-campo"); put("version", 1); put("name", s.name); put("group", s.group)
        put("month", m); put("category", c.cat); put("participated", c.participated); put("bibleStudies", c.studies)
        put("hours", if (c.isPio) c.reported else JSONObject.NULL)
        put("creditHours", if (c.isPio) c.creditUsed / 60 else JSONObject.NULL)
        put("notes", notes ?: "")
    }.toString(2)
}
