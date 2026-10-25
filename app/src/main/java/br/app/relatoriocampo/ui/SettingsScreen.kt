package br.app.relatoriocampo.ui

import android.Manifest
import android.app.DatePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.rounded.Add
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import br.app.relatoriocampo.widget.CronometroReceiver
import br.app.relatoriocampo.widget.MetaReceiver
import br.app.relatoriocampo.widget.ResumoReceiver
import br.app.relatoriocampo.widget.StatusReceiver
import br.app.relatoriocampo.widget.Widgets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Contrast
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.FileUpload
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.automirrored.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Functions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import br.app.relatoriocampo.domain.Settings
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.notify.Reminders
import br.app.relatoriocampo.ui.theme.appColors
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import android.provider.Settings as AndroidSettings

@Composable
fun SettingsScreen(vm: AppViewModel, s: Settings, snack: Snack) {
    val col = appColors
    val t = LocalStr.current
    val ctx = LocalContext.current
    val epoch = LocalEpoch.current
    val scope = rememberCoroutineScope()

    // ---------- você ----------
    var name by remember(epoch) { mutableStateOf(s.name) }
    var group by remember(epoch) { mutableStateOf(s.group) }
    SectionCard(t("you"), Icons.Outlined.Person) {
        FieldLabel(t("yourName"))
        AppTextField(name, { v -> name = v; vm.updateSettings { it.copy(name = v.trim()) } })
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("group"))
        AppTextField(group, { v -> group = v; vm.updateSettings { it.copy(group = v.trim()) } })
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("defCat"))
        Segmented(catOptions(t, col), s.category, { v -> vm.updateSettings { it.copy(category = v) } }, stacked = true)
    }

    // ---------- lembretes ----------
    var canNotify by remember { mutableStateOf(Reminders.canNotify(ctx)) }
    LifecycleResumeEffect(Unit) {
        canNotify = Reminders.canNotify(ctx)
        onPauseOrDispose { }
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        canNotify = Reminders.canNotify(ctx)
        if (!granted) openNotificationSettings(ctx)
    }
    var remEnd by remember(epoch) { mutableStateOf(s.remEnd.toString()) }
    var remDay by remember(epoch) { mutableStateOf(s.remSendDay.toString()) }
    SectionCard(t("reminders"), Icons.Outlined.Notifications) {
        ToggleRow(t("remOn"), s.remEnabled) { v -> vm.updateSettings { it.copy(remEnabled = v) } }
        if (s.remEnabled && !canNotify) {
            Note(t("notifOff"), Icons.Outlined.NotificationsActive, warn = true, modifier = Modifier.padding(bottom = 8.dp))
            AppButton(t("allowNotif"), Icons.Outlined.NotificationsActive, BtnStyle.Tonal, Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                if (Build.VERSION.SDK_INT >= 33) permLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                else openNotificationSettings(ctx)
            }
        }
        Row(Modifier.padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel(t("remEnd"))
                NumField(remEnd, { v ->
                    remEnd = v
                    v.toIntOrNull()?.let { n -> vm.updateSettings { it.copy(remEnd = n.coerceIn(1, 28)) } }
                }, Modifier.fillMaxWidth(), maxLen = 2)
            }
            Column(Modifier.weight(1f)) {
                FieldLabel(t("remSendDay"))
                NumField(remDay, { v ->
                    remDay = v
                    v.toIntOrNull()?.let { n -> vm.updateSettings { it.copy(remSendDay = n.coerceIn(1, 28)) } }
                }, Modifier.fillMaxWidth(), maxLen = 2)
            }
        }
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("remTime"))
        TimeField(s.remHour, s.remMinute, t("remTime")) { h, m -> vm.updateSettings { it.copy(remHour = h, remMinute = m) } }
        Spacer(Modifier.height(4.dp))
        ToggleRow(t("remDaily"), s.remDaily) { v -> vm.updateSettings { it.copy(remDaily = v) } }
        Note(t("remHint"))
    }

    // ---------- widgets ----------
    SectionCard(t("widgets"), Icons.Outlined.Widgets) {
        listOf(
            Triple(ResumoReceiver::class.java, "wResumo", Icons.Outlined.CalendarMonth),
            Triple(CronometroReceiver::class.java, "wCrono", Icons.Outlined.Timer),
            Triple(StatusReceiver::class.java, "wStatus", Icons.AutoMirrored.Outlined.Send),
            Triple(MetaReceiver::class.java, "wMeta", Icons.Outlined.TrackChanges),
        ).forEachIndexed { i, (cls, key, icon) ->
            if (i > 0) AppDivider()
            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp).padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                KindIcon(icon, col.accentText, col.accentSoft)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(t(key), fontWeight = FontWeight.Bold, color = col.ink)
                    Text(t(key + "D"), fontSize = 13.sp, color = col.muted)
                }
                Spacer(Modifier.width(8.dp))
                SmallButton(t("wAdd"), Icons.Rounded.Add) {
                    snack.show(if (Widgets.requestPin(ctx, cls)) t("wAsked") else t("wHint"))
                }
            }
        }
        Note(t("wHint"))
    }

    // ---------- regras ----------
    var goal by remember(epoch) { mutableStateOf(s.annualGoal.toString()) }
    var limit by remember(epoch) { mutableStateOf(s.creditLimit.toString()) }
    SectionCard(t("rules"), Icons.Outlined.Tune) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(Modifier.weight(1f)) {
                FieldLabel(t("annualGoal"))
                NumField(goal, { v -> goal = v; v.toIntOrNull()?.let { n -> vm.updateSettings { it.copy(annualGoal = n) } } }, Modifier.fillMaxWidth())
            }
            Column(Modifier.weight(1f)) {
                FieldLabel(t("creditLimit"))
                NumField(limit, { v -> limit = v; v.toIntOrNull()?.let { n -> vm.updateSettings { it.copy(creditLimit = n) } } }, Modifier.fillMaxWidth(), maxLen = 3)
            }
        }
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("defMode"))
        Segmented(
            listOf(SegOpt("daily", t("modeDaily"), Icons.AutoMirrored.Rounded.FormatListBulleted), SegOpt("total", t("modeTotal"), Icons.Rounded.Functions)),
            s.entryMode, { v -> vm.updateSettings { it.copy(entryMode = v) } },
        )
        Spacer(Modifier.height(12.dp))
        FieldLabel(t("startMonth"))
        val sm = s.startMonth
        PickerField(if (sm.length == 7) Ym.name(sm, s.lang) else "—", Icons.Outlined.CalendarMonth, t("startMonth")) {
            val base = if (sm.length == 7) Ym.parse(sm).atDay(1) else LocalDate.now()
            DatePickerDialog(ctx, { _, y, m, _ -> vm.updateSettings { it.copy(startMonth = Ym.of(YearMonth.of(y, m + 1))) } },
                base.year, base.monthValue - 1, 1).show()
        }
        Spacer(Modifier.height(4.dp))
        ToggleRow(t("carry"), s.carry) { v -> vm.updateSettings { it.copy(carry = v) } }
        Note(t("verify"))
    }

    // ---------- aparência ----------
    SectionCard(t("theme"), Icons.Outlined.Contrast) {
        Segmented(
            listOf(
                SegOpt("auto", t("auto"), Icons.Outlined.Contrast),
                SegOpt("light", t("light"), Icons.Outlined.LightMode),
                SegOpt("dark", t("dark"), Icons.Outlined.DarkMode),
            ),
            s.theme, { v -> vm.updateSettings { it.copy(theme = v) } },
        )
    }

    // ---------- backup ----------
    var confirmClear by remember { mutableStateOf(false) }
    var pendingImport by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) scope.launch { snack.show(if (vm.exportBackup(uri)) t("exportOk") else t("importBad")) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> pendingImport = uri }
    SectionCard(t("backup"), Icons.Outlined.Inventory2) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AppButton(t("exportAll"), Icons.Outlined.FileDownload, BtnStyle.Tonal, Modifier.weight(1f)) {
                exportLauncher.launch("backup-relatorio-campo-${LocalDate.now()}.json")
            }
            AppButton(t("importAll"), Icons.Outlined.FileUpload, BtnStyle.Tonal, Modifier.weight(1f)) {
                importLauncher.launch(arrayOf("application/json", "text/plain", "application/octet-stream"))
            }
        }
        AppButton(t("clear"), Icons.Outlined.Delete, BtnStyle.Danger, Modifier.fillMaxWidth().padding(top = 8.dp)) { confirmClear = true }
        Note(t("privacy"), Icons.Outlined.Lock)
    }
    Text(
        "Ícones: Material Symbols (Apache 2.0)",
        Modifier.fillMaxWidth().padding(top = 4.dp), fontSize = 12.sp, color = col.muted, textAlign = TextAlign.Center,
    )

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            text = { Text(t("confirmClear")) },
            confirmButton = { TextButton(onClick = { confirmClear = false; vm.clearAll() }) { Text(t("clear"), color = col.danger) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(t("cancel")) } },
        )
    }
    pendingImport?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImport = null },
            text = { Text(t("confirmImport")) },
            confirmButton = {
                TextButton(onClick = {
                    pendingImport = null
                    scope.launch { snack.show(if (vm.importBackup(uri)) t("importOk") else t("importBad")) }
                }) { Text(t("ok")) }
            },
            dismissButton = { TextButton(onClick = { pendingImport = null }) { Text(t("cancel")) } },
        )
    }
}

private fun openNotificationSettings(ctx: Context) {
    val i = Intent(AndroidSettings.ACTION_APP_NOTIFICATION_SETTINGS)
        .putExtra(AndroidSettings.EXTRA_APP_PACKAGE, ctx.packageName)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    ctx.startActivity(i)
}
