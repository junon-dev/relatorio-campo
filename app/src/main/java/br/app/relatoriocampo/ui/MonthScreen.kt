package br.app.relatoriocampo.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AssignmentTurnedIn
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.HourglassBottom
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.relatoriocampo.data.Backup
import br.app.relatoriocampo.domain.Calc
import br.app.relatoriocampo.domain.Contact
import br.app.relatoriocampo.domain.MonthCalc
import br.app.relatoriocampo.domain.MonthData
import br.app.relatoriocampo.domain.ReminderCalc
import br.app.relatoriocampo.domain.Settings
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.domain.fmtMin
import br.app.relatoriocampo.ui.theme.NumStyle
import br.app.relatoriocampo.ui.theme.appColors
import br.app.relatoriocampo.ui.theme.catColor
import br.app.relatoriocampo.ui.theme.catSoft
import br.app.relatoriocampo.ui.theme.catText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate

@Composable
fun MonthScreen(
    vm: AppViewModel,
    s: Settings,
    c: MonthCalc,
    all: Map<String, MonthCalc>,
    months: Map<String, MonthData>,
    view: String,
    cur: String,
    contacts: List<Contact>,
    snack: Snack,
    onGoto: (String) -> Unit,
    onTab: (String) -> Unit,
) {
    val md = months[view]
    Banners(s, all, view, cur, onGoto) { m -> vm.updateMonth(m) { it.copy(sent = true) } }
    Hero(s, c, md)
    ReportCard(vm, c, md, view, contacts.count { it.kind == "study" && it.active })
    HoursCard(vm, s, c, md, view, onTab)
    if (c.mode == "daily") TimerCard(vm, s, snack)
    SendCard(vm, s, c, md, view, snack)
}

@Composable
private fun Banners(s: Settings, all: Map<String, MonthCalc>, view: String, cur: String, onGoto: (String) -> Unit, onMarkSent: (String) -> Unit) {
    val t = LocalStr.current
    val today = LocalDate.now()
    val left = ReminderCalc.daysLeft(today)
    if (view == cur && left <= s.remEnd) {
        val m = Ym.monthOnly(cur, s.lang)
        Banner(Icons.Outlined.HourglassBottom, if (left <= 1) t("bLast", "m" to m) else t("bEnd", "n" to left, "m" to m))
    }
    val prev = Ym.add(cur, -1)
    val pc = all[prev]
    if (today.dayOfMonth <= 10 && pc != null && !pc.sent && pc.hasData) {
        val m = Ym.monthOnly(prev, s.lang)
        Banner(Icons.Outlined.NotificationsActive, t("bSend", "m" to m)) {
            if (view != prev) SmallButton(t("openM", "m" to m)) { onGoto(prev) }
            SmallButton(t("markSent"), Icons.Rounded.Check) { onMarkSent(prev) }
        }
    }
}

/** Resumo em destaque: horas a relatar (pioneiros) ou estudos (publicador). */
@Composable
private fun Hero(s: Settings, c: MonthCalc, md: MonthData?) {
    val col = appColors
    val t = LocalStr.current
    val cc = col.catColor(c.cat)
    val shape = RoundedCornerShape(20.dp)
    Column(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(shape).background(col.catSoft(c.cat))
            .border(1.dp, cc.copy(alpha = 0.2f), shape).padding(16.dp),
    ) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pill(t(c.cat), catIcon(c.cat), col.catText(c.cat), col.card)
            if (c.participated) Pill(t("partYes"), Icons.Rounded.Check, col.accentText, col.card)
            if (c.sent) Pill(t("sentChip"), Icons.AutoMirrored.Outlined.Send, col.accentText, col.card)
        }
        if (c.isPio) {
            val goal = if (c.cat == "aux") Calc.auxGoal(md) else Calc.monthlyGoal(s)
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
                Text("${c.reported}", fontSize = 52.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold, color = col.ink, style = NumStyle)
                Spacer(Modifier.width(8.dp))
                Text(t("toReport"), color = col.muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            }
            Spacer(Modifier.height(10.dp))
            ProgressBar(if (goal > 0) c.reported / goal.toFloat() else 0f, cc)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.TrackChanges, null, tint = col.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                val label = if (c.cat == "aux") t("auxGoal") else t("suggested")
                val status = if (c.reported >= goal) t("done") else t("remaining", "n" to goal - c.reported)
                Text("$label: $goal h · $status", fontSize = 14.sp, color = col.muted)
            }
            Row(Modifier.padding(top = 14.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Stat(Icons.Outlined.AutoStories, "${c.studies}", t("st"), Modifier.weight(1f))
                Stat(Icons.Outlined.Place, fmtMin(c.field), t("field"), Modifier.weight(1f))
                Stat(Icons.Outlined.AddCircleOutline, fmtMin(c.credit), t("credit"), Modifier.weight(1f))
            }
        } else {
            Row(Modifier.padding(top = 14.dp), verticalAlignment = Alignment.Bottom) {
                Text("${c.studies}", fontSize = 52.sp, lineHeight = 52.sp, fontWeight = FontWeight.ExtraBold, color = col.ink, style = NumStyle)
                Spacer(Modifier.width(8.dp))
                Text(t("studiesUnit"), color = col.muted, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(bottom = 6.dp))
            }
            Row(Modifier.padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(if (c.participated) Icons.Rounded.Check else Icons.Outlined.Info, null, tint = col.muted, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(if (c.participated) t("partYesLong") else t("partNo"), fontSize = 14.sp, color = col.muted)
            }
        }
    }
}

@Composable
private fun Stat(icon: ImageVector, value: String, label: String, modifier: Modifier) {
    val col = appColors
    Row(modifier.clip(RoundedCornerShape(12.dp)).background(col.card).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = col.accentText, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(7.dp))
        Column {
            Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = col.ink, style = NumStyle, maxLines = 1)
            Text(label, fontSize = 12.sp, color = col.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ReportCard(vm: AppViewModel, c: MonthCalc, md: MonthData?, view: String, activeStudies: Int) {
    val col = appColors
    val t = LocalStr.current
    SectionCard(t("report"), Icons.Outlined.AssignmentTurnedIn) {
        FieldLabel(t("monthCat"))
        Segmented(catOptions(t, col), c.cat, { v -> vm.updateMonth(view) { it.copy(cat = v) } }, stacked = true)
        Spacer(Modifier.height(4.dp))
        ToggleRow(t("participated"), c.participated) { v -> vm.updateMonth(view) { it.copy(participated = v) } }
        AppDivider()
        Row(Modifier.fillMaxWidth().heightIn(min = 58.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(t("studies"), color = col.ink)
                Text(
                    t("useActive", "n" to activeStudies),
                    modifier = Modifier.padding(top = 2.dp).clip(RoundedCornerShape(6.dp))
                        .clickable(role = Role.Button) { vm.updateMonth(view) { it.copy(studies = activeStudies) } }
                        .padding(vertical = 4.dp),
                    color = col.accentText, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textDecoration = TextDecoration.Underline,
                )
            }
            Stepper(c.studies) { n -> vm.updateMonth(view) { it.copy(studies = n) } }
        }
        if (c.cat == "aux") {
            AppDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 58.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(t("auxGoal"), Modifier.weight(1f), color = col.ink)
                Segmented(
                    listOf(SegOpt("15", "15 h"), SegOpt("30", "30 h")),
                    Calc.auxGoal(md).toString(),
                    { v -> vm.updateMonth(view) { it.copy(auxGoal = v.toInt()) } },
                    modifier = Modifier.width(150.dp),
                )
            }
        }
    }
}

@Composable
private fun HoursCard(vm: AppViewModel, s: Settings, c: MonthCalc, md: MonthData?, view: String, onTab: (String) -> Unit) {
    val t = LocalStr.current
    SectionCard(t("howToLog"), Icons.Outlined.EditNote) {
        Segmented(
            listOf(
                SegOpt("daily", t("modeDaily"), Icons.AutoMirrored.Rounded.FormatListBulleted),
                SegOpt("total", t("modeTotal"), Icons.Rounded.Functions),
            ),
            c.mode,
            { v -> vm.updateMonth(view) { it.copy(mode = v) }; vm.updateSettings { it.copy(entryMode = v) } },
        )
        if (c.mode == "total") {
            TotalInputs(vm, c, md, view)
        } else {
            Spacer(Modifier.height(4.dp))
            InfoRow(t("field"), fmtMin(c.field))
            AppDivider()
            InfoRow(t("credit"), fmtMin(c.credit))
            AppButton(t("logHours"), Icons.Rounded.Add, BtnStyle.Tonal, Modifier.fillMaxWidth().padding(top = 6.dp)) { onTab("entries") }
        }
        if (c.credit > c.creditUsed) Note(t("creditCap", "n" to fmtMin(c.credit - c.creditUsed)), Icons.Outlined.ErrorOutline, warn = true)
        if (c.isPio && s.carry && (c.carryIn > 0 || c.carryOut > 0)) {
            Spacer(Modifier.height(4.dp))
            InfoRow(t("carryIn"), "${c.carryIn} min", small = true)
            InfoRow(t("carryOut"), "${c.carryOut} min", small = true)
        }
        if (!c.isPio) Note(t("pubNote"))
    }
}

@Composable
private fun TotalInputs(vm: AppViewModel, c: MonthCalc, md: MonthData?, view: String) {
    val col = appColors
    val t = LocalStr.current
    val epoch = LocalEpoch.current
    fun part(v: Int?, hours: Boolean): String {
        val x = v ?: 0
        val p = if (hours) x / 60 else x % 60
        return if (p > 0) p.toString() else ""
    }
    var fh by remember(view, epoch) { mutableStateOf(part(md?.totField, true)) }
    var fm by remember(view, epoch) { mutableStateOf(part(md?.totField, false)) }
    var ch by remember(view, epoch) { mutableStateOf(part(md?.totCredit, true)) }
    var cm by remember(view, epoch) { mutableStateOf(part(md?.totCredit, false)) }
    fun save() {
        val f = (fh.toIntOrNull() ?: 0) * 60 + minOf(59, fm.toIntOrNull() ?: 0)
        val cr = (ch.toIntOrNull() ?: 0) * 60 + minOf(59, cm.toIntOrNull() ?: 0)
        vm.updateMonth(view) { it.copy(mode = "total", totField = f, totCredit = cr) }
    }
    Text(t("totalHint"), fontSize = 13.sp, color = col.muted, modifier = Modifier.padding(top = 12.dp, bottom = 4.dp))
    HmRow(t("field"), fh, fm, { fh = it; save() }, { fm = it; save() })
    AppDivider()
    HmRow(t("credit"), ch, cm, { ch = it; save() }, { cm = it; save() })
    if (c.entries.isNotEmpty()) Note(t("ignored", "n" to c.entries.size))
}

@Composable
private fun HmRow(label: String, h: String, m: String, onH: (String) -> Unit, onM: (String) -> Unit) {
    val col = appColors
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = col.ink)
        NumField(h, onH, Modifier.width(72.dp), center = true, maxLen = 3)
        Text(" h ", color = col.muted)
        NumField(m, { onM(it) }, Modifier.width(64.dp), center = true, maxLen = 2)
        Text(" min", color = col.muted)
    }
}

@Composable
private fun TimerCard(vm: AppViewModel, s: Settings, snack: Snack) {
    val col = appColors
    val t = LocalStr.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val start = s.timerStart
    LaunchedEffect(start) {
        while (start != null) { now = System.currentTimeMillis(); delay(1000) }
    }
    val secs = if (start != null) ((now - start) / 1000).coerceAtLeast(0) else 0L
    SectionCard(t("timer"), Icons.Outlined.Timer) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (start != null) PulsingDot(col.danger)
            Text(
                "%d:%02d:%02d".format(secs / 3600, secs / 60 % 60, secs % 60),
                Modifier.weight(1f), fontSize = 32.sp, fontWeight = FontWeight.ExtraBold,
                color = if (start != null) col.accentText else col.ink, style = NumStyle,
            )
            if (start != null) {
                SquareIconButton(Icons.Rounded.Close, t("discard"), size = 50.dp) { vm.discardTimer() }
                Spacer(Modifier.width(8.dp))
                AppButton(t("stop"), Icons.Rounded.Stop) {
                    val m = vm.stopTimer(t("timerNote"))
                    if (m > 0) snack.show("${t("added")} · ${fmtMin(m)}")
                }
            } else {
                AppButton(t("start"), Icons.Rounded.PlayArrow) { vm.startTimer() }
            }
        }
    }
}

@Composable
private fun SendCard(vm: AppViewModel, s: Settings, c: MonthCalc, md: MonthData?, view: String, snack: Snack) {
    val t = LocalStr.current
    val ctx = LocalContext.current
    val clipboard = LocalClipboardManager.current
    val scope = rememberCoroutineScope()
    val epoch = LocalEpoch.current
    var notes by remember(view, epoch) { mutableStateOf(md?.notes ?: "") }
    var pendingJson by remember { mutableStateOf("") }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch {
            snack.show(if (vm.writeText(uri, pendingJson)) t("exportOk") else t("importBad"))
        }
    }
    SectionCard(t("sendTitle"), Icons.AutoMirrored.Outlined.Send) {
        FieldLabel(t("rNotes"))
        AppTextField(
            notes,
            { v -> notes = v; vm.updateMonth(view) { it.copy(notes = v.trim().ifEmpty { null }) } },
            placeholder = t("notesPh"), singleLine = false, minLines = 2,
        )
        Spacer(Modifier.height(12.dp))
        val text = Calc.reportText(view, c, s, notes, t)
        AppButton(t("share"), Icons.AutoMirrored.Outlined.Send, BtnStyle.Filled, Modifier.fillMaxWidth()) {
            shareText(ctx, text, t("share"))
            snack.show(t("askSent"), t("markSent")) { vm.updateMonth(view) { it.copy(sent = true) } }
        }
        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(t("copy"), Icons.Rounded.ContentCopy, BtnStyle.Tonal, Modifier.weight(1f)) {
                clipboard.setText(AnnotatedString(text))
                snack.show(t("copied"))
            }
            AppButton(t("exportMonth"), Icons.Outlined.FileDownload, BtnStyle.Tonal, Modifier.weight(1f)) {
                pendingJson = Backup.monthJson(view, c, s, notes)
                exportLauncher.launch("relatorio-$view.json")
            }
        }
        Spacer(Modifier.height(4.dp))
        ToggleRow(t("sent"), c.sent) { v -> vm.updateMonth(view) { it.copy(sent = v) } }
    }
}
