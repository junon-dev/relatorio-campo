package br.app.relatoriocampo

import android.app.Application
import br.app.relatoriocampo.notify.Reminders

class RelatorioApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Reminders.createChannel(this)
        Reminders.scheduleAll(this)
    }
}
