package br.app.relatoriocampo.ui

import android.Manifest
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import br.app.relatoriocampo.domain.Calc
import br.app.relatoriocampo.domain.Settings
import br.app.relatoriocampo.domain.Str
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.ui.theme.appColors

@Composable
fun App(vm: AppViewModel, gotoMonth: String?, onGotoHandled: () -> Unit) {
    val s by vm.settings.collectAsStateWithLifecycle()
    val entriesOrNull by vm.entries.collectAsStateWithLifecycle()
    val contactsOrNull by vm.contacts.collectAsStateWithLifecycle()
    val monthsOrNull by vm.months.collectAsStateWithLifecycle()
    val epoch by vm.epoch.collectAsStateWithLifecycle()
    val c = appColors

    val cur = Ym.current()
    var tab by rememberSaveable { mutableStateOf("month") }
    var view by rememberSaveable { mutableStateOf(cur) }
    val t = remember(s.lang) { Str(s.lang) }
    val host = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val snack = remember(host, scope) { Snack(host, scope) }

    LaunchedEffect(gotoMonth) {
        if (gotoMonth != null) { view = gotoMonth; tab = "month"; onGotoHandled() }
    }

    // Pede permissão de notificação uma vez (Android 13+).
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= 33 && !s.askedNotif) {
            vm.updateSettings { it.copy(askedNotif = true) }
            permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    BackHandler(enabled = tab != "month") { tab = "month" }

    CompositionLocalProvider(LocalStr provides t, LocalEpoch provides epoch) {
        Scaffold(
            containerColor = c.bg,
            snackbarHost = { SnackbarHost(host) },
            bottomBar = { BottomBar(tab) { tab = it } },
        ) { pad ->
            val entries = entriesOrNull
            val contacts = contactsOrNull
            val months = monthsOrNull
            if (entries == null || contacts == null || months == null) {
                Box(Modifier.fillMaxSize().padding(pad))
            } else {
            val all = remember(s, entries, months, view, cur) { Calc.calcAll(s, entries, months, view, cur) }
            val mc = all.getValue(view)
            val scroll = key(tab) { rememberScrollState() }
            Column(
                Modifier.fillMaxSize().padding(pad).imePadding().verticalScroll(scroll).padding(horizontal = 16.dp),
            ) {
                Header(s) { l -> vm.updateSettings { it.copy(lang = l) } }
                if (tab == "month" || tab == "entries" || tab == "year") {
                    MonthNav(view, cur, onPrev = { view = Ym.add(view, -1) }, onNext = { view = Ym.add(view, 1) }, onToday = { view = cur })
                }
                when (tab) {
                    "month" -> MonthScreen(vm, s, mc, all, months, view, cur, contacts, snack, onGoto = { view = it }, onTab = { tab = it })
                    "entries" -> EntriesScreen(vm, mc, view, cur, snack, onView = { view = it })
                    "contacts" -> ContactsScreen(vm, contacts, snack)
                    "year" -> YearScreen(s, all, months, view, cur, onOpen = { view = it; tab = "month" })
                    "settings" -> SettingsScreen(vm, s, snack)
                }
                Spacer(Modifier.height(24.dp))
            }
            }
        }
    }
}

@Composable
private fun Header(s: Settings, onLang: (String) -> Unit) {
    val c = appColors
    val t = LocalStr.current
    Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(t("title"), style = MaterialTheme.typography.headlineSmall, color = c.ink)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(if (s.name.isBlank()) Icons.Outlined.Lock else Icons.Outlined.Person, null, tint = c.muted, modifier = Modifier.size(15.dp))
                Spacer(Modifier.width(5.dp))
                Text(s.name.ifBlank { t("privacy") }, fontSize = 13.sp, color = c.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.width(12.dp))
        val shape = RoundedCornerShape(50)
        Row(Modifier.clip(shape).background(c.card).border(1.dp, c.line, shape).padding(3.dp)) {
            listOf("pt", "es").forEach { l ->
                val on = s.lang == l
                Box(
                    Modifier.size(width = 46.dp, height = 40.dp).clip(shape)
                        .background(if (on) c.ink else Color.Transparent)
                        .selectable(selected = on, role = Role.RadioButton, onClick = { onLang(l) }),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(l.uppercase(), color = if (on) c.bg else c.muted, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }
        }
    }
}

@Composable
private fun MonthNav(view: String, cur: String, onPrev: () -> Unit, onNext: () -> Unit, onToday: () -> Unit) {
    val c = appColors
    val t = LocalStr.current
    Row(Modifier.fillMaxWidth().padding(bottom = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        SquareIconButton(Icons.Rounded.ChevronLeft, t("prevM"), onClick = onPrev)
        Column(
            Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(12.dp))
                .clickable(onClickLabel = t("backToday"), onClick = onToday),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(Ym.name(view, t.lang), fontWeight = FontWeight.Bold, fontSize = 17.sp, color = c.ink)
            if (view != cur) Text(t("backToday"), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = c.accentText)
        }
        SquareIconButton(Icons.Rounded.ChevronRight, t("nextM"), onClick = onNext)
    }
}

/** Barra inferior: Mês e Registros em destaque; Estudos, Ano e Ajustes como itens secundários. */
@Composable
private fun BottomBar(tab: String, onTab: (String) -> Unit) {
    val c = appColors
    val t = LocalStr.current
    Surface(color = c.card) {
        Column {
            AppDivider()
            Row(
                Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                PrimaryTab(Icons.Outlined.CalendarMonth, t("tMonth"), tab == "month", Modifier.weight(1f)) { onTab("month") }
                PrimaryTab(Icons.Outlined.Schedule, t("tEntries"), tab == "entries", Modifier.weight(1f)) { onTab("entries") }
                Box(Modifier.width(1.dp).height(34.dp).background(c.line))
                SecondaryTab(Icons.Outlined.AutoStories, t("tContacts"), tab == "contacts") { onTab("contacts") }
                SecondaryTab(Icons.Outlined.BarChart, t("tYear"), tab == "year") { onTab("year") }
                SecondaryTab(Icons.Outlined.Tune, t("tSettings"), tab == "settings") { onTab("settings") }
            }
        }
    }
}

@Composable
private fun PrimaryTab(icon: ImageVector, label: String, on: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = appColors
    val shape = RoundedCornerShape(16.dp)
    val fg = if (on) c.accentInk else c.ink
    Column(
        modifier.height(56.dp).clip(shape)
            .background(if (on) c.accent else c.card)
            .border(1.dp, if (on) c.accent else c.line, shape)
            .selectable(selected = on, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
        Spacer(Modifier.height(2.dp))
        Text(label, color = fg, fontWeight = FontWeight.Bold, fontSize = 13.sp, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun SecondaryTab(icon: ImageVector, label: String, on: Boolean, onClick: () -> Unit) {
    val c = appColors
    val fg = if (on) c.accentText else c.muted
    Column(
        Modifier.width(54.dp).height(56.dp).clip(RoundedCornerShape(12.dp))
            .selectable(selected = on, role = Role.Tab, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(width = 46.dp, height = 28.dp).clip(RoundedCornerShape(50))
                .background(if (on) c.accentSoft else Color.Transparent),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp)) }
        Spacer(Modifier.height(3.dp))
        Text(label, color = fg, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis)
    }
}
