package br.app.relatoriocampo.notify

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import br.app.relatoriocampo.MainActivity
import br.app.relatoriocampo.R
import br.app.relatoriocampo.data.AppDatabase
import br.app.relatoriocampo.data.SettingsStore
import br.app.relatoriocampo.domain.ReminderCalc
import br.app.relatoriocampo.domain.Str
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.widget.Widgets
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object Reminders {
    const val CHANNEL = "lembretes"
    const val ACTION_END = "br.app.relatoriocampo.action.END_OF_MONTH"
    const val ACTION_SEND = "br.app.relatoriocampo.action.SEND_REPORT"
    const val EXTRA_GOTO = "goto_month"
    private const val ID_END = 101
    private const val ID_SEND = 102

    fun createChannel(context: Context) {
        val t = Str(SettingsStore.get(context).value.lang)
        val channel = NotificationChannel(CHANNEL, t("chEnd"), NotificationManager.IMPORTANCE_DEFAULT)
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private fun pending(context: Context, action: String, code: Int): PendingIntent =
        PendingIntent.getBroadcast(
            context, code,
            Intent(context, ReminderReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    /** (Re)agenda os dois lembretes de acordo com as configurações atuais. */
    fun scheduleAll(context: Context) {
        val s = SettingsStore.get(context).value
        val am = context.getSystemService(AlarmManager::class.java)
        val end = pending(context, ACTION_END, ID_END)
        val send = pending(context, ACTION_SEND, ID_SEND)
        am.cancel(end); am.cancel(send)
        if (!s.remEnabled) return
        val now = LocalDateTime.now()
        // setAndAllowWhileIdle não exige permissão de alarme exato; pode atrasar alguns minutos, o que é suficiente aqui.
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis(ReminderCalc.nextEnd(now, s)), end)
        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, millis(ReminderCalc.nextSend(now, s)), send)
    }

    private fun millis(t: LocalDateTime) = t.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

    fun canNotify(context: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(context).areNotificationsEnabled()

    @SuppressLint("MissingPermission")
    fun show(context: Context, id: Int, title: String, text: String, gotoMonth: String) {
        if (!canNotify(context)) return
        val open = PendingIntent.getActivity(
            context, id,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                .putExtra(EXTRA_GOTO, gotoMonth),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setColor(0xFF2B6A4C.toInt())
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(context).notify(id, n)
    }

    /** Executado quando um alarme dispara. */
    suspend fun fire(context: Context, action: String) {
        val s = SettingsStore.get(context).value
        val t = Str(s.lang)
        val today = LocalDate.now()
        val cur = Ym.current(today)
        when (action) {
            ACTION_END -> {
                val left = ReminderCalc.daysLeft(today)
                val m = Ym.monthOnly(cur, s.lang)
                val text = if (left <= 1) t("bLast", "m" to m) else t("bEnd", "n" to left, "m" to m)
                show(context, ID_END, t("nEndT"), text, cur)
            }
            ACTION_SEND -> {
                val prev = Ym.add(cur, -1)
                val sent = AppDatabase.get(context).dao().month(prev)?.sent ?: false
                if (!sent) {
                    val m = Ym.monthOnly(prev, s.lang)
                    show(context, ID_SEND, t("nSendT"), t("bSend", "m" to m) + " " + t("nSendD", "m" to m), prev)
                }
            }
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                Reminders.fire(context.applicationContext, action)
            } finally {
                Reminders.scheduleAll(context.applicationContext)
                Widgets.updateAll(context.applicationContext)
                result.finish()
            }
        }
    }
}

/** Reagenda os lembretes depois de reiniciar o aparelho, atualizar o app ou mudar a hora. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.scheduleAll(context.applicationContext)
        val result = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { Widgets.updateAll(context.applicationContext) } finally { result.finish() }
        }
    }
}
