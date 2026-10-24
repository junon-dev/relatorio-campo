package br.app.relatoriocampo.domain

import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Utilitários de mês no formato "yyyy-MM". */
object Ym {
    fun of(ym: YearMonth): String = "%04d-%02d".format(Locale.ROOT, ym.year, ym.monthValue)
    fun parse(m: String): YearMonth = YearMonth.of(m.substring(0, 4).toInt(), m.substring(5, 7).toInt())
    fun current(today: LocalDate = LocalDate.now()): String = of(YearMonth.from(today))
    fun add(m: String, n: Int): String = of(parse(m).plusMonths(n.toLong()))
    fun daysIn(m: String): Int = parse(m).lengthOfMonth()

    /** Ano de serviço: setembro a agosto. Retorna o ano em que começa. */
    fun serviceYear(m: String): Int {
        val ym = parse(m)
        return if (ym.monthValue >= 9) ym.year else ym.year - 1
    }

    fun serviceYearMonths(sy: Int): List<String> = (0 until 12).map { add("%04d-09".format(Locale.ROOT, sy), it) }

    fun locale(lang: String): Locale = if (lang == "es") Locale.forLanguageTag("es-419") else Locale.forLanguageTag("pt-BR")

    private fun cap(s: String) = s.replaceFirstChar { it.titlecase(Locale.ROOT) }

    /** "Outubro de 2026" / "Octubre de 2026" */
    fun name(m: String, lang: String): String =
        cap(parse(m).atDay(1).format(DateTimeFormatter.ofPattern("MMMM 'de' yyyy", locale(lang))))

    /** "outubro" / "octubre" */
    fun monthOnly(m: String, lang: String): String =
        parse(m).atDay(1).format(DateTimeFormatter.ofPattern("MMMM", locale(lang)))

    /** "Out" / "Oct" */
    fun short(m: String, lang: String): String =
        cap(parse(m).atDay(1).format(DateTimeFormatter.ofPattern("MMM", locale(lang))).replace(".", ""))

    /** "Segunda-feira, 5" */
    fun dayLabel(date: String, lang: String): String =
        cap(LocalDate.parse(date).format(DateTimeFormatter.ofPattern("EEEE, d", locale(lang))))
}

fun fmtMin(minutes: Int): String {
    val m = max(0, minutes)
    return "%dh%02d".format(Locale.ROOT, m / 60, m % 60)
}

object Calc {
    fun modeOf(m: String, months: Map<String, MonthData>, entries: List<Entry>, s: Settings): String =
        months[m]?.mode ?: if (entries.any { it.date.startsWith(m) }) "daily" else s.entryMode

    fun catOf(m: String, months: Map<String, MonthData>, s: Settings): String = months[m]?.cat ?: s.category

    /**
     * Calcula todos os meses do primeiro com dados até o último (incluindo o atual e o visualizado),
     * para que os minutos que sobram passem corretamente de um mês para o outro.
     */
    fun calcAll(
        s: Settings,
        entries: List<Entry>,
        months: Map<String, MonthData>,
        view: String,
        cur: String,
    ): Map<String, MonthCalc> {
        val set = sortedSetOf(cur, view)
        entries.forEach { set.add(it.date.substring(0, 7)) }
        set.addAll(months.keys)
        val first = set.first()
        val last = set.last()
        val limit = max(0, s.creditLimit) * 60
        val res = LinkedHashMap<String, MonthCalc>()
        var carry = 0
        var m = first
        while (m <= last) {
            val es = entries.filter { it.date.startsWith(m) }
            val md = months[m]
            val mode = modeOf(m, months, entries, s)
            val field = if (mode == "total") md?.totField ?: 0 else es.filter { it.kind != "credit" }.sumOf { it.min }
            val credit = if (mode == "total") md?.totCredit ?: 0 else es.filter { it.kind == "credit" }.sumOf { it.min }
            val cat = catOf(m, months, s)
            val isPio = cat != "pub"
            val creditUsed = min(credit, limit)
            val carryIn = if (isPio && s.carry) carry else 0
            val total = field + carryIn + creditUsed
            val reported = if (isPio) total / 60 else 0
            val carryOut = if (isPio && s.carry) total % 60 else 0
            carry = carryOut
            val studies = md?.studies ?: 0
            val participated = md?.participated ?: (field + credit > 0 || studies > 0)
            res[m] = MonthCalc(m, field, credit, creditUsed, carryIn, total, reported, carryOut, isPio, cat,
                studies, participated, es, mode, md?.sent ?: false)
            m = Ym.add(m, 1)
        }
        return res
    }

    fun auxGoal(md: MonthData?): Int = md?.auxGoal ?: 30
    fun monthlyGoal(s: Settings): Int = (s.annualGoal / 12.0).roundToInt()

    data class YearProgress(val hours: Int, val goal: Int, val expected: Int) {
        val diff: Int get() = hours - expected
    }

    /** Progresso anual do pioneiro regular, proporcional ao mês de início. Null se não houver meses como regular. */
    fun yearProgress(sy: Int, all: Map<String, MonthCalc>, months: Map<String, MonthData>, s: Settings, cur: String): YearProgress? {
        val ms = Ym.serviceYearMonths(sy)
        val reg = ms.filter { catOf(it, months, s) == "reg" }
        if (reg.isEmpty()) return null
        val hours = reg.filter { it <= cur }.sumOf { all[it]?.reported ?: 0 }
        var start = ms.first()
        val sm = s.startMonth
        if (sm.length == 7 && sm > start && sm <= ms.last()) start = sm
        val goal = (s.annualGoal * ms.count { it >= start } / 12.0).roundToInt()
        val expected = (s.annualGoal / 12.0 * reg.count { it <= cur && it >= start }).roundToInt()
        return YearProgress(hours, goal, expected)
    }

    fun reportText(m: String, c: MonthCalc, s: Settings, notes: String?, t: Str): String {
        val lines = mutableListOf(t("rTitle"), "")
        if (s.name.isNotBlank()) lines += "${t("rName")}: ${s.name}"
        lines += "${t("rMonth")}: ${Ym.name(m, s.lang)}"
        lines += "${t("rPart")}: ${if (c.participated) t("yes") else t("no")}"
        lines += "${t("rStudies")}: ${c.studies}"
        if (c.isPio) {
            val cr = c.creditUsed / 60
            lines += "${t(c.cat)} — ${t("rHours")}: ${c.reported}" + if (cr > 0) " (${t("rCredit", "n" to cr)})" else ""
        }
        if (!notes.isNullOrBlank()) lines += "${t("rNotes")}: $notes"
        return lines.joinToString("\n")
    }
}

/** Cálculo da próxima data de cada lembrete. */
object ReminderCalc {
    private fun at(m: String, day: Int, s: Settings): LocalDateTime =
        Ym.parse(m).atDay(day).atTime(s.remHour, s.remMinute)

    /** Dias do mês em que o aviso de fim de mês dispara. */
    fun endDays(m: String, s: Settings): List<Int> {
        val n = s.remEnd.coerceIn(1, 28)
        val last = Ym.daysIn(m)
        val first = last + 1 - n
        return if (s.remDaily) (first..last).toList() else listOf(first)
    }

    fun nextEnd(now: LocalDateTime, s: Settings): LocalDateTime {
        val cur = Ym.of(YearMonth.from(now))
        for (m in listOf(cur, Ym.add(cur, 1))) {
            endDays(m, s).map { at(m, it, s) }.firstOrNull { it.isAfter(now) }?.let { return it }
        }
        return at(Ym.add(cur, 2), endDays(Ym.add(cur, 2), s).first(), s)
    }

    fun nextSend(now: LocalDateTime, s: Settings): LocalDateTime {
        val d = s.remSendDay.coerceIn(1, 28)
        val cur = Ym.of(YearMonth.from(now))
        val a = at(cur, d, s)
        return if (a.isAfter(now)) a else at(Ym.add(cur, 1), d, s)
    }

    /** Dias que faltam até o fim do mês, contando hoje (último dia = 1). */
    fun daysLeft(today: LocalDate): Int = today.lengthOfMonth() - today.dayOfMonth + 1
}
