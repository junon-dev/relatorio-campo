package br.app.relatoriocampo.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.RemoteViews
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.ColumnScope
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import br.app.relatoriocampo.MainActivity
import br.app.relatoriocampo.R
import br.app.relatoriocampo.data.AppDatabase
import br.app.relatoriocampo.data.EntryEntity
import br.app.relatoriocampo.data.MonthEntity
import br.app.relatoriocampo.data.SettingsStore
import br.app.relatoriocampo.data.toDomain
import br.app.relatoriocampo.data.toEntity
import br.app.relatoriocampo.domain.Calc
import br.app.relatoriocampo.domain.Entry
import br.app.relatoriocampo.domain.MonthCalc
import br.app.relatoriocampo.domain.MonthData
import br.app.relatoriocampo.domain.ReminderCalc
import br.app.relatoriocampo.domain.Settings
import br.app.relatoriocampo.domain.Str
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.domain.fmtMin
import br.app.relatoriocampo.notify.Reminders
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.util.UUID
import androidx.glance.color.ColorProvider as DayNight

// ===================== dados =====================

data class WidgetData(val s: Settings, val entries: List<Entry>, val months: Map<String, MonthData>) {
    val cur: String = Ym.current()
    val all: Map<String, MonthCalc> = Calc.calcAll(s, entries, months, cur, cur)
    val t = Str(s.lang)
}

private fun dataFlow(context: Context): Flow<WidgetData> {
    val dao = AppDatabase.get(context).dao()
    return combine(dao.entries(), dao.months(), SettingsStore.get(context).flow) { e, m, s ->
        WidgetData(s, e.map { it.toDomain() }, m.associate { it.month to it.toDomain() })
    }
}

/** Mesma paleta do app, com variantes para o modo escuro do sistema. */
private object WC {
    val bg = DayNight(day = Color(0xFFFFFFFF), night = Color(0xFF18201C))
    val ink = DayNight(day = Color(0xFF18211D), night = Color(0xFFE7ECE9))
    val muted = DayNight(day = Color(0xFF56605A), night = Color(0xFF9BA8A1))
    val accent = DayNight(day = Color(0xFF2B6A4C), night = Color(0xFF6CC296))
    val accentInk = DayNight(day = Color(0xFFFFFFFF), night = Color(0xFF0B1410))
    val accentSoft = DayNight(day = Color(0xFFE2EFE7), night = Color(0xFF1F3329))
    val accentText = DayNight(day = Color(0xFF225A40), night = Color(0xFF8FD6B0))
    val amber = DayNight(day = Color(0xFF8A5410), night = Color(0xFFE9B46B))
    val amberSoft = DayNight(day = Color(0xFFFBECD6), night = Color(0xFF3A2B17))
    val slate = DayNight(day = Color(0xFF44515F), night = Color(0xFFB5C0CC))
    val slateSoft = DayNight(day = Color(0xFFE7EBF0), night = Color(0xFF27303A))

    fun cat(cat: String) = when (cat) { "aux" -> amber; "reg" -> accentText; else -> slate }
    fun catSoft(cat: String) = when (cat) { "aux" -> amberSoft; "reg" -> accentSoft; else -> slateSoft }
}

private fun style(color: ColorProvider, size: Int, bold: Boolean = false) =
    TextStyle(color = color, fontSize = size.sp, fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal)

@Composable
private fun Card(modifier: GlanceModifier = GlanceModifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(WC.bg).padding(14.dp).then(modifier),
        verticalAlignment = Alignment.CenterVertically,
        content = content,
    )
}

@Composable
private fun Loading() {
    Box(GlanceModifier.fillMaxSize().cornerRadius(22.dp).background(WC.bg)) {}
}

@Composable
private fun Pill(text: String, fg: ColorProvider, bg: ColorProvider, modifier: GlanceModifier, action: androidx.glance.action.Action) {
    Box(
        modifier.height(38.dp).cornerRadius(19.dp).background(bg).clickable(action),
        contentAlignment = Alignment.Center,
    ) { Text(text, style = style(fg, 13, bold = true), maxLines = 1) }
}

private fun openApp() = actionStartActivity<MainActivity>()

private fun daysLeftText(t: Str, d: WidgetData): String {
    val left = ReminderCalc.daysLeft(LocalDate.now())
    val m = Ym.monthOnly(d.cur, d.s.lang)
    return if (left <= 1) t("bLast", "m" to m) else t("bEnd", "n" to left, "m" to m)
}

/** Atualiza todos os widgets (chamado quando os dados mudam). */
object Widgets {
    suspend fun updateAll(context: Context) {
        runCatching {
            ResumoWidget().updateAll(context)
            CronometroWidget().updateAll(context)
            StatusWidget().updateAll(context)
            MetaWidget().updateAll(context)
        }
    }

    /** Pede ao launcher para colocar o widget na tela inicial. Retorna false se o launcher não suportar. */
    fun requestPin(context: Context, receiver: Class<*>): Boolean {
        val m = AppWidgetManager.getInstance(context)
        return m.isRequestPinAppWidgetSupported && m.requestPinAppWidget(ComponentName(context, receiver), null, null)
    }
}

// ===================== 1. Resumo do mês =====================

class ResumoWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Responsive(setOf(DpSize(110.dp, 110.dp), DpSize(250.dp, 110.dp)))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val flow = dataFlow(context)
        provideContent {
            val d by remember { flow }.collectAsState(initial = null)
            d?.let { ResumoContent(it) } ?: Loading()
        }
    }
}

@Composable
private fun ResumoContent(d: WidgetData) {
    val t = d.t
    val c = d.all.getValue(d.cur)
    val wide = LocalSize.current.width >= 220.dp
    val left = ReminderCalc.daysLeft(LocalDate.now())
    Card(GlanceModifier.clickable(openApp())) {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(t(c.cat + "S"), GlanceModifier.defaultWeight(), style = style(WC.cat(c.cat), 12, bold = true), maxLines = 1)
            Text(Ym.short(d.cur, d.s.lang), style = style(WC.muted, 12), maxLines = 1)
        }
        Spacer(GlanceModifier.height(2.dp))
        if (c.isPio) {
            val goal = if (c.cat == "aux") Calc.auxGoal(d.months[d.cur]) else Calc.monthlyGoal(d.s)
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${c.reported}", style = style(WC.ink, 34, bold = true))
                Spacer(GlanceModifier.width(6.dp))
                Text(t("toReport"), GlanceModifier.padding(bottom = 6.dp), style = style(WC.muted, 12), maxLines = 1)
            }
            LinearProgressIndicator(
                progress = if (goal > 0) (c.reported / goal.toFloat()).coerceIn(0f, 1f) else 0f,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp),
                color = WC.cat(c.cat), backgroundColor = WC.catSoft(c.cat),
            )
            Spacer(GlanceModifier.height(4.dp))
            Text(
                if (c.reported >= goal) "$goal h · ${t("done")}" else "$goal h · ${t("remaining", "n" to goal - c.reported)}",
                style = style(WC.muted, 11), maxLines = 1,
            )
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${c.studies}", style = style(WC.ink, 34, bold = true))
                Spacer(GlanceModifier.width(6.dp))
                Text(t("studiesUnit"), GlanceModifier.padding(bottom = 6.dp), style = style(WC.muted, 12), maxLines = 1)
            }
            Text(if (c.participated) "✓ " + t("partYes") else t("wNotYet"), style = style(if (c.participated) WC.accentText else WC.muted, 12, bold = c.participated), maxLines = 1)
        }
        if (wide) {
            Spacer(GlanceModifier.height(6.dp))
            Row(GlanceModifier.fillMaxWidth()) {
                Stat("${c.studies}", t("st"), GlanceModifier.defaultWeight())
                Stat(fmtMin(c.field), t("field"), GlanceModifier.defaultWeight())
                Stat(fmtMin(c.credit), t("credit"), GlanceModifier.defaultWeight())
            }
        }
        Spacer(GlanceModifier.height(8.dp))
        Text(daysLeftText(t, d), style = style(if (left <= d.s.remEnd) WC.amber else WC.muted, 11, bold = left <= d.s.remEnd), maxLines = 1)
    }
}

@Composable
private fun Stat(value: String, label: String, modifier: GlanceModifier) {
    Column(modifier) {
        Text(value, style = style(WC.ink, 14, bold = true), maxLines = 1)
        Text(label, style = style(WC.muted, 11), maxLines = 1)
    }
}

// ===================== 2. Cronômetro + lançamento rápido =====================

val MinutesKey = ActionParameters.Key<Int>("minutos")
val MonthKey = ActionParameters.Key<String>("mes")
private const val WIDGET_NOTE = "Widget"

class CronometroWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val flow = dataFlow(context)
        provideContent {
            val d by remember { flow }.collectAsState(initial = null)
            d?.let { CronometroContent(it) } ?: Loading()
        }
    }
}

@Composable
private fun CronometroContent(d: WidgetData) {
    val t = d.t
    val ctx = LocalContext.current
    val start = d.s.timerStart
    val c = d.all.getValue(d.cur)
    val today = LocalDate.now().toString()
    val todayMin = d.entries.filter { it.date == today }.sumOf { it.min }
    Card {
        Row(GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(GlanceModifier.defaultWeight().clickable(openApp())) {
                if (start != null) {
                    val rv = RemoteViews(ctx.packageName, R.layout.widget_chronometer).apply {
                        setChronometer(R.id.chrono, SystemClock.elapsedRealtime() - (System.currentTimeMillis() - start), null, true)
                    }
                    AndroidRemoteViews(rv)
                } else {
                    Text("0:00", style = style(WC.ink, 28, bold = true))
                }
                Text(
                    if (c.mode == "total") "${t("modeTotal")}: ${fmtMin(c.field)}" else "${t("wToday")}: ${fmtMin(todayMin)}",
                    style = style(WC.muted, 11), maxLines = 1,
                )
            }
            Box(
                GlanceModifier.size(48.dp).cornerRadius(24.dp).background(if (start != null) WC.amber else WC.accent)
                    .clickable(if (start != null) actionRunCallback<StopTimerAction>() else actionRunCallback<StartTimerAction>()),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    ImageProvider(if (start != null) R.drawable.ic_w_stop else R.drawable.ic_w_play),
                    contentDescription = if (start != null) t("stop") else t("start"),
                    modifier = GlanceModifier.size(24.dp),
                    colorFilter = ColorFilter.tint(WC.accentInk),
                )
            }
        }
        Spacer(GlanceModifier.height(12.dp))
        Row(GlanceModifier.fillMaxWidth()) {
            listOf(15 to "+15", 30 to "+30", 60 to "+1h", 120 to "+2h").forEachIndexed { i, (q, label) ->
                if (i > 0) Spacer(GlanceModifier.width(6.dp))
                Pill(label, WC.accentText, WC.accentSoft, GlanceModifier.defaultWeight(),
                    actionRunCallback<AddMinutesAction>(actionParametersOf(MinutesKey to q)))
            }
        }
    }
}

class StartTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        SettingsStore.get(context).update { it.copy(timerStart = System.currentTimeMillis()) }
        Widgets.updateAll(context)
    }
}

class StopTimerAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val store = SettingsStore.get(context)
        val start = store.value.timerStart ?: return
        val minutes = ((System.currentTimeMillis() - start) / 60_000L).toInt()
        store.update { it.copy(timerStart = null) }
        if (minutes > 0) {
            val note = Str(store.value.lang)("timerNote")
            AppDatabase.get(context).dao().upsertEntry(
                EntryEntity(UUID.randomUUID().toString().substring(0, 12), LocalDate.now().toString(), minutes, "field", note),
            )
        }
        Widgets.updateAll(context)
    }
}

/** Soma minutos ao campo de hoje (ou ao total do mês, se o mês estiver no modo "Só o total"). */
class AddMinutesAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val minutes = parameters[MinutesKey] ?: return
        val dao = AppDatabase.get(context).dao()
        val s = SettingsStore.get(context).value
        val today = LocalDate.now().toString()
        val cur = Ym.current()
        val entries = dao.allEntries().map { it.toDomain() }
        val months = dao.allMonths().associate { it.month to it.toDomain() }
        if (Calc.modeOf(cur, months, entries, s) == "total") {
            val md = months[cur] ?: MonthData(cur)
            dao.upsertMonth(md.copy(mode = "total", totField = (md.totField ?: 0) + minutes).toEntity())
        } else {
            val existing = entries.firstOrNull { it.date == today && it.kind == "field" && it.note == WIDGET_NOTE }
            val e = existing?.copy(min = existing.min + minutes)
                ?: Entry(UUID.randomUUID().toString().substring(0, 12), today, minutes, "field", WIDGET_NOTE)
            dao.upsertEntry(e.toEntity())
        }
        Widgets.updateAll(context)
    }
}

// ===================== 3. Status do relatório =====================

class StatusWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val flow = dataFlow(context)
        provideContent {
            val d by remember { flow }.collectAsState(initial = null)
            d?.let { StatusContent(it) } ?: Loading()
        }
    }
}

@Composable
private fun StatusContent(d: WidgetData) {
    val t = d.t
    val ctx = LocalContext.current
    val prev = Ym.add(d.cur, -1)
    val pc = d.all[prev]
    val pending = pc != null && !pc.sent && pc.hasData
    val sent = pc?.sent == true
    val prevName = Ym.monthOnly(prev, d.s.lang)
    Card(if (pending) GlanceModifier else GlanceModifier.clickable(openApp())) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Image(
                ImageProvider(if (pending) R.drawable.ic_w_send else R.drawable.ic_w_check), contentDescription = null,
                modifier = GlanceModifier.size(18.dp),
                colorFilter = ColorFilter.tint(if (pending) WC.amber else WC.accentText),
            )
            Spacer(GlanceModifier.width(6.dp))
            Text(t("wReportOf", "m" to prevName), style = style(WC.muted, 12, bold = true), maxLines = 1)
        }
        Spacer(GlanceModifier.height(4.dp))
        Text(
            when { pending -> t("wPending"); sent -> t("wSent"); else -> t("wNothing") },
            style = style(if (pending) WC.amber else WC.accentText, 18, bold = true), maxLines = 1,
        )
        Spacer(GlanceModifier.height(10.dp))
        if (pending) {
            val share = Intent(ctx, MainActivity::class.java)
                .putExtra(Reminders.EXTRA_GOTO, prev)
                .putExtra(MainActivity.EXTRA_SHARE, true)
            Row(GlanceModifier.fillMaxWidth()) {
                Pill(t("wSend"), WC.accentInk, WC.accent, GlanceModifier.defaultWeight(), actionStartActivity(share))
                Spacer(GlanceModifier.width(6.dp))
                Pill(t("markSent"), WC.accentText, WC.accentSoft, GlanceModifier.defaultWeight(),
                    actionRunCallback<MarkSentAction>(actionParametersOf(MonthKey to prev)))
            }
        } else {
            Text(daysLeftText(t, d), style = style(WC.muted, 11), maxLines = 2)
        }
    }
}

class MarkSentAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val m = parameters[MonthKey] ?: return
        val dao = AppDatabase.get(context).dao()
        val cur: MonthEntity = dao.month(m) ?: MonthData(m).toEntity()
        dao.upsertMonth(cur.copy(sent = true))
        Widgets.updateAll(context)
    }
}

// ===================== 4. Meta anual =====================

class MetaWidget : GlanceAppWidget() {
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val flow = dataFlow(context)
        provideContent {
            val d by remember { flow }.collectAsState(initial = null)
            d?.let { MetaContent(it) } ?: Loading()
        }
    }
}

@Composable
private fun MetaContent(d: WidgetData) {
    val t = d.t
    val sy = Ym.serviceYear(d.cur)
    val yp = Calc.yearProgress(sy, d.all, d.months, d.s, d.cur)
    Card(GlanceModifier.clickable(openApp())) {
        Text("${t("goalYear")} · $sy–${(sy + 1) % 100}", style = style(WC.muted, 12, bold = true), maxLines = 1)
        Spacer(GlanceModifier.height(2.dp))
        if (yp == null) {
            Spacer(GlanceModifier.height(6.dp))
            Text(t("wOnlyReg"), style = style(WC.muted, 13), maxLines = 2)
        } else {
            Row(verticalAlignment = Alignment.Bottom) {
                Text("${yp.hours}", style = style(WC.ink, 30, bold = true))
                Text(" / ${yp.goal} h", GlanceModifier.padding(bottom = 5.dp), style = style(WC.muted, 13), maxLines = 1)
            }
            LinearProgressIndicator(
                progress = if (yp.goal > 0) (yp.hours / yp.goal.toFloat()).coerceIn(0f, 1f) else 0f,
                modifier = GlanceModifier.fillMaxWidth().height(6.dp), color = WC.accent, backgroundColor = WC.accentSoft,
            )
            Spacer(GlanceModifier.height(8.dp))
            val diff = yp.diff
            Text(
                when { diff == 0 -> t("onPace"); diff > 0 -> t("ahead", "n" to diff); else -> t("behind", "n" to -diff) },
                style = style(if (diff < 0) WC.amber else WC.accentText, 13, bold = true), maxLines = 1,
            )
            Text(t("wExpected", "n" to yp.expected), style = style(WC.muted, 11), maxLines = 1)
        }
    }
}

// ===================== receivers =====================

class ResumoReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = ResumoWidget() }
class CronometroReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = CronometroWidget() }
class StatusReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = StatusWidget() }
class MetaReceiver : GlanceAppWidgetReceiver() { override val glanceAppWidget: GlanceAppWidget = MetaWidget() }
