package br.app.relatoriocampo.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class CalcTest {
    private val s = Settings(category = "reg", startMonth = "2026-09")
    private val entries = listOf(
        Entry("a", "2026-09-10", 52 * 60 + 20, "field"),
        Entry("b", "2026-10-02", 3 * 60, "field"),
        Entry("c", "2026-10-02", 60 * 60, "credit"),
        Entry("d", "2026-10-05", 2 * 60 + 30, "field"),
    )
    private val months = mapOf(
        "2026-10" to MonthData("2026-10", studies = 2),
        "2026-11" to MonthData("2026-11", mode = "total", totField = 48 * 60, totCredit = 5 * 60),
    )
    private val all = Calc.calcAll(s, entries, months, "2026-10", "2026-10")

    @Test fun minutosQueSobramPassamParaOMesSeguinte() {
        assertEquals(52, all.getValue("2026-09").reported)
        assertEquals(20, all.getValue("2026-09").carryOut)
        assertEquals(20, all.getValue("2026-10").carryIn)
    }

    @Test fun creditoLimitado() {
        val o = all.getValue("2026-10")
        assertEquals(55 * 60, o.creditUsed)
        assertEquals((5 * 60 + 30 + 20 + 55 * 60) / 60, o.reported)
    }

    @Test fun modoSoOTotal() {
        val n = all.getValue("2026-11")
        assertEquals("total", n.mode)
        assertEquals(48 * 60, n.field)
    }

    @Test fun publicadorNaoRelataHoras() {
        val p = Calc.calcAll(s.copy(category = "pub"), entries, months, "2026-10", "2026-10")
        assertEquals(0, p.getValue("2026-10").reported)
    }

    @Test fun metaAnualProporcional() {
        val yp = Calc.yearProgress(2026, all, months, s, "2026-10")!!
        assertEquals(600, yp.goal)
        assertEquals(100, yp.expected)
        assertEquals(300, Calc.yearProgress(2026, all, months, s.copy(startMonth = "2027-03"), "2026-10")!!.goal)
    }

    @Test fun lembretes() {
        val now = LocalDateTime.of(2026, 10, 28, 10, 0)
        assertEquals(LocalDateTime.of(2026, 11, 26, 9, 0), ReminderCalc.nextEnd(now, s))
        assertEquals(LocalDateTime.of(2026, 10, 29, 9, 0), ReminderCalc.nextEnd(now, s.copy(remDaily = true)))
        assertEquals(LocalDateTime.of(2026, 11, 1, 9, 0), ReminderCalc.nextSend(now, s))
        assertEquals(4, ReminderCalc.daysLeft(LocalDate.of(2026, 10, 28)))
    }

    @Test fun textosPtEEsCompletos() {
        assertEquals(Str.PT.keys, Str.ES.keys)
        assertTrue(Calc.reportText("2026-10", all.getValue("2026-10"), s, null, Str("pt")).contains("Outubro de 2026"))
    }
}
