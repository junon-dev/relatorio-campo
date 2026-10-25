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

    /** Mês cujo relatório deve abrir direto no compartilhamento (botão Enviar do widget). */
    private val shareMonth = mutableStateOf<String?>(null)

    private fun readIntent(intent: Intent?) {
        val m = intent?.getStringExtra(Reminders.EXTRA_GOTO) ?: return
        gotoMonth.value = m
        if (intent.getBooleanExtra(EXTRA_SHARE, false)) shareMonth.value = m
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        readIntent(intent)
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
                App(
                    vm = vm,
                    gotoMonth = gotoMonth.value, onGotoHandled = { gotoMonth.value = null },
                    shareMonth = shareMonth.value, onShareHandled = { shareMonth.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        readIntent(intent)
    }

    companion object {
        const val EXTRA_SHARE = "share_report"
        private val LIGHT_SCRIM = Color.argb(0xe6, 0xFF, 0xFF, 0xFF)
        private val DARK_SCRIM = Color.argb(0x80, 0x1b, 0x1b, 0x1b)
    }
}
