package br.app.relatoriocampo.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.relatoriocampo.domain.Calc
import br.app.relatoriocampo.domain.Contact
import br.app.relatoriocampo.domain.Entry
import br.app.relatoriocampo.domain.MonthCalc
import br.app.relatoriocampo.domain.MonthData
import br.app.relatoriocampo.domain.Settings
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.domain.fmtMin
import br.app.relatoriocampo.ui.theme.NumStyle
import br.app.relatoriocampo.ui.theme.appColors
import br.app.relatoriocampo.ui.theme.catSoft
import br.app.relatoriocampo.ui.theme.catText
import java.time.LocalDate

// ======================= REGISTROS =======================

@Composable
fun EntriesScreen(vm: AppViewModel, c: MonthCalc, view: String, cur: String, snack: Snack, onView: (String) -> Unit) {
    val t = LocalStr.current
    if (c.mode == "total") {
        SectionCard {
            Note(t("totalModeNote"), warn = true, modifier = Modifier)
            AppButton(t("useDaily"), Icons.AutoMirrored.Rounded.FormatListBulleted, BtnStyle.Filled, Modifier.fillMaxWidth().padding(top = 12.dp)) {
                vm.updateMonth(view) { it.copy(mode = "daily") }
            }
        }
    } else {
        AddEntryCard(vm, view, cur, snack, onView)
    }
    EntryList(vm, c, snack)
}

@Composable
private fun AddEntryCard(vm: AppViewModel, view: String, cur: String, snack: Snack, onView: (String) -> Unit) {
    val col = appColors
    val t = LocalStr.current
    val haptic = LocalHapticFeedback.current
    val focus = LocalFocusManager.current
    var kind by rememberSaveable { mutableStateOf("field") }
    var date by remember(view) { mutableStateOf(if (view == cur) LocalDate.now() else Ym.parse(view).atDay(1)) }
    var h by rememberSaveable { mutableStateOf("") }
    var m by rememberSaveable { mutableStateOf("") }
    var note by rememberSaveable { mutableStateOf("") }

    SectionCard(t("addEntry"), Icons.Outlined.AddCircleOutline) {
        FieldLabel(t("kind"))
        Segmented(
            listOf(SegOpt("field", t("field"), Icons.Outlined.Place), SegOpt("credit", t("credit"), Icons.Outlined.AddCircleOutline, col.amber)),
            kind, { kind = it },
        )
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("date"))
        DateField(date, t("date")) { date = it }
        Spacer(Modifier.height(12.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) { FieldLabel(t("h")); NumField(h, { h = it }, Modifier.fillMaxWidth(), center = true, maxLen = 2) }
            Column(Modifier.weight(1f)) { FieldLabel(t("m")); NumField(m, { m = it }, Modifier.fillMaxWidth(), center = true, maxLen = 2) }
        }
        Spacer(Modifier.height(10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(15, 30, 60, 120).forEach { q ->
                val shape = RoundedCornerShape(50)
                Box(
                    Modifier.weight(1f).heightIn(min = 44.dp).clip(shape).background(col.card).border(1.dp, col.line, shape)
                        .clickable(role = Role.Button) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            val tot = (h.toIntOrNull() ?: 0) * 60 + (m.toIntOrNull() ?: 0) + q
                            h = if (tot / 60 > 0) "${tot / 60}" else ""
                            m = if (tot % 60 > 0) "${tot % 60}" else ""
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(if (q < 60) "+$q min" else "+${q / 60} h", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = col.ink, maxLines = 1, style = NumStyle)
                }
            }
        }
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("note"))
        AppTextField(note, { note = it })
        Spacer(Modifier.height(12.dp))
        AppButton(t("add"), Icons.Rounded.Add, BtnStyle.Filled, Modifier.fillMaxWidth()) {
            val tot = (h.toIntOrNull() ?: 0) * 60 + minOf(59, m.toIntOrNull() ?: 0)
            if (tot > 0) {
                vm.addEntry(date.toString(), tot, kind, note)
                snack.show("${t("added")} · ${fmtMin(tot)}")
                h = ""; m = ""; note = ""
                focus.clearFocus()
                val dm = Ym.of(java.time.YearMonth.from(date))
                if (dm != view) onView(dm)
            }
        }
    }
}

@Composable
private fun EntryList(vm: AppViewModel, c: MonthCalc, snack: Snack) {
    val col = appColors
    val t = LocalStr.current
    SectionCard("${t("tEntries")} · ${fmtMin(c.entries.sumOf { it.min })}", Icons.Outlined.Schedule) {
        if (c.entries.isEmpty()) {
            EmptyState(Icons.Outlined.Schedule, t("noEntries"))
        } else {
            val groups = c.entries.groupBy { it.date }.toSortedMap(compareByDescending<String> { it })
            var first = true
            groups.forEach { (day, list) ->
                Row(Modifier.fillMaxWidth().padding(top = if (first) 0.dp else 14.dp, bottom = 2.dp)) {
                    Text(Ym.dayLabel(day, t.lang), Modifier.weight(1f), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = col.muted)
                    Text(fmtMin(list.sumOf { it.min }), fontSize = 13.sp, fontWeight = FontWeight.Bold, color = col.muted, style = NumStyle)
                }
                first = false
                list.forEachIndexed { i, e ->
                    if (i > 0) AppDivider()
                    EntryItem(e) {
                        vm.deleteEntry(e)
                        snack.show(t("removed"), t("undo")) { vm.restoreEntry(e) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EntryItem(e: Entry, onDelete: () -> Unit) {
    val col = appColors
    val t = LocalStr.current
    val credit = e.kind == "credit"
    Row(Modifier.fillMaxWidth().heightIn(min = 60.dp), verticalAlignment = Alignment.CenterVertically) {
        KindIcon(
            if (credit) Icons.Outlined.AddCircleOutline else Icons.Outlined.Place,
            if (credit) col.amber else col.accentText,
            if (credit) col.amberSoft else col.accentSoft,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(fmtMin(e.min), fontWeight = FontWeight.Bold, color = col.ink, style = NumStyle)
            Text(
                t(if (credit) "credit" else "field") + if (e.note.isNotBlank()) " · ${e.note}" else "",
                fontSize = 13.sp, color = col.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        SquareIconButton(Icons.Outlined.Delete, t("del"), ghost = true, onClick = onDelete)
    }
}

// ======================= ESTUDOS / REVISITAS =======================

@Composable
fun ContactsScreen(vm: AppViewModel, contacts: List<Contact>, snack: Snack) {
    val col = appColors
    val t = LocalStr.current
    val focus = LocalFocusManager.current
    var kind by rememberSaveable { mutableStateOf("study") }
    var name by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }

    SectionCard(t("contacts"), Icons.Outlined.PersonAdd) {
        FieldLabel(t("kind"))
        Segmented(
            listOf(SegOpt("study", t("study"), Icons.Outlined.AutoStories), SegOpt("rv", t("rv"), Icons.Rounded.Repeat, col.slate)),
            kind, { kind = it },
        )
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("name"))
        AppTextField(name, { name = it })
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("note"))
        AppTextField(notes, { notes = it })
        Spacer(Modifier.height(12.dp))
        AppButton(t("add"), Icons.Rounded.Add, BtnStyle.Filled, Modifier.fillMaxWidth()) {
            if (name.isNotBlank()) {
                vm.addContact(name, kind, notes)
                name = ""; notes = ""
                focus.clearFocus()
            }
        }
    }

    val active = contacts.count { it.kind == "study" && it.active }
    val sorted = contacts.sortedWith(compareByDescending<Contact> { it.active }.thenBy { it.kind }.thenBy { it.name.lowercase() })
    SectionCard(t("activeStudies", "n" to active), Icons.Outlined.AutoStories) {
        if (sorted.isEmpty()) {
            EmptyState(Icons.Outlined.AutoStories, t("noContacts"))
        } else {
            sorted.forEachIndexed { i, x ->
                if (i > 0) AppDivider()
                val rv = x.kind == "rv"
                Row(Modifier.fillMaxWidth().heightIn(min = 64.dp), verticalAlignment = Alignment.CenterVertically) {
                    Row(Modifier.weight(1f).alpha(if (x.active) 1f else 0.55f), verticalAlignment = Alignment.CenterVertically) {
                        KindIcon(
                            if (rv) Icons.Rounded.Repeat else Icons.Outlined.AutoStories,
                            if (rv) col.slate else col.accentText,
                            if (rv) col.slateSoft else col.accentSoft,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(x.name, fontWeight = FontWeight.Bold, color = col.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(
                                t(x.kind) + if (x.notes.isNotBlank()) " · ${x.notes}" else "",
                                fontSize = 13.sp, color = col.muted, maxLines = 1, overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    AppSwitch(x.active) { vm.setContactActive(x, it) }
                    Spacer(Modifier.width(4.dp))
                    SquareIconButton(Icons.Outlined.Delete, t("del"), ghost = true) {
                        vm.deleteContact(x)
                        snack.show(t("cRemoved"), t("undo")) { vm.restoreContact(x) }
                    }
                }
            }
        }
    }
}

// ======================= ANO DE SERVIÇO =======================

@Composable
fun YearScreen(s: Settings, all: Map<String, MonthCalc>, months: Map<String, MonthData>, view: String, cur: String, onOpen: (String) -> Unit) {
    val col = appColors
    val t = LocalStr.current
    val sy = Ym.serviceYear(view)
    val ms = Ym.serviceYearMonths(sy)
    val maxH = maxOf(1, Calc.monthlyGoal(s), ms.maxOf { m -> if (m <= cur && Calc.catOf(m, months, s) != "pub") all[m]?.reported ?: 0 else 0 })
    var total = 0

    SectionCard(t("sy", "a" to sy, "b" to sy + 1), Icons.Outlined.BarChart) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 6.dp, vertical = 4.dp)) {
            HeaderCell(t("month"), Modifier.weight(1.1f))
            HeaderCell(t("cat"), Modifier.width(40.dp))
            HeaderCell(t("part"), Modifier.width(40.dp))
            HeaderCell(t("st"), Modifier.weight(0.9f), TextAlign.End)
            HeaderCell(t("hrs"), Modifier.weight(1.4f), TextAlign.End)
        }
        ms.forEach { m ->
            val x = all[m]
            val cat = Calc.catOf(m, months, s)
            val future = m > cur
            val rep = x?.reported ?: 0
            if (cat != "pub" && !future) total += rep
            AppDivider()
            Row(
                Modifier.fillMaxWidth().heightIn(min = 46.dp).clip(RoundedCornerShape(8.dp))
                    .background(if (m == view) col.accentSoft else Color.Transparent)
                    .clickable(role = Role.Button) { onOpen(m) }
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(Ym.short(m, t.lang), Modifier.weight(1.1f), color = col.ink, fontWeight = if (m == view) FontWeight.Bold else FontWeight.Normal)
                Box(Modifier.width(40.dp)) {
                    Box(Modifier.size(26.dp).clip(RoundedCornerShape(8.dp)).background(col.catSoft(cat)), contentAlignment = Alignment.Center) {
                        Icon(catIcon(cat), t(cat), tint = col.catText(cat), modifier = Modifier.size(15.dp))
                    }
                }
                Box(Modifier.width(40.dp)) {
                    if (!future && x != null) {
                        if (x.participated) Icon(Icons.Rounded.Check, t("yes"), tint = col.accentText, modifier = Modifier.size(18.dp))
                        else Text("—", color = col.muted)
                    }
                }
                Text(if (!future && x != null) "${x.studies}" else "", Modifier.weight(0.9f), textAlign = TextAlign.End, color = col.ink, style = NumStyle)
                Row(Modifier.weight(1.4f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                    if (cat != "pub" && !future && x != null) {
                        Box(Modifier.width((rep.toFloat() / maxH * 44f).coerceAtLeast(3f).dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(col.accent))
                        Spacer(Modifier.width(8.dp))
                        Text("$rep", color = col.ink, fontWeight = FontWeight.SemiBold, style = NumStyle)
                    }
                }
            }
        }
        AppDivider()
        Row(Modifier.fillMaxWidth().heightIn(min = 46.dp).padding(horizontal = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(t("total"), Modifier.weight(1f), fontWeight = FontWeight.Bold, color = col.ink)
            Text("$total", fontWeight = FontWeight.Bold, color = col.ink, style = NumStyle)
        }
        Row(Modifier.padding(top = 10.dp).horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("pub", "aux", "reg").forEach { k -> Pill(t(k), catIcon(k), col.catText(k), col.catSoft(k)) }
        }
    }

    val yp = Calc.yearProgress(sy, all, months, s, cur)
    if (yp != null) {
        SectionCard("${t("goalYear")} · ${t("reg")}", Icons.Outlined.TrackChanges) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${yp.hours}", fontSize = 40.sp, lineHeight = 40.sp, fontWeight = FontWeight.ExtraBold, color = col.ink, style = NumStyle)
                Spacer(Modifier.width(8.dp))
                Text("/ ${yp.goal} h", color = col.muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 4.dp))
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(if (yp.goal > 0) yp.hours / yp.goal.toFloat() else 0f, col.accent)
            Spacer(Modifier.height(4.dp))
            InfoRow(t("pace"), "${yp.expected} h")
            val d = yp.diff
            Note(
                when { d == 0 -> t("onPace"); d > 0 -> t("ahead", "n" to d); else -> t("behind", "n" to -d) },
                if (d < 0) Icons.Outlined.ErrorOutline else Icons.AutoMirrored.Rounded.TrendingUp,
                warn = d < 0,
            )
        }
    } else {
        Note(t("notReg"))
    }
}

@Composable
private fun HeaderCell(text: String, modifier: Modifier, align: TextAlign = TextAlign.Start) {
    Text(text, modifier, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = appColors.muted, textAlign = align, maxLines = 1)
}
