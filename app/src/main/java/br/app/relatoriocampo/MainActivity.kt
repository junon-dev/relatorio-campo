package br.app.relatoriocampo

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import br.app.relatoriocampo.notify.Reminders
import br.app.relatoriocampo.ui.App
import br.app.relatoriocampo.ui.AppViewModel
import br.app.relatoriocampo.ui.theme.RelatorioTheme

class MainActivity : ComponentActivity() {
    /** Mês a abrir quando o app é aberto por uma notificação. */
    private val gotoMonth = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        gotoMonth.value = intent?.getStringExtra(Reminders.EXTRA_GOTO)
        setContent {
            val vm: AppViewModel = viewModel()
            val settings by vm.settings.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val dark = when (settings.theme) { "dark" -> true; "light" -> false; else -> systemDark }
            DisposableEffect(dark) {
                enableEdgeToEdge(
                    statusBarStyle = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark },
                    navigationBarStyle = SystemBarStyle.auto(LIGHT_SCRIM, DARK_SCRIM) { dark },
                )
                onDispose {}
            }
            RelatorioTheme(dark = dark) {
                App(vm = vm, gotoMonth = gotoMonth.value, onGotoHandled = { gotoMonth.value = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        intent.getStringExtra(Reminders.EXTRA_GOTO)?.let { gotoMonth.value = it }
    }

    private companion object {
        val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
