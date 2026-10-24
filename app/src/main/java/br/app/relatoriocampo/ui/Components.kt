package br.app.relatoriocampo.ui

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import br.app.relatoriocampo.domain.Str
import br.app.relatoriocampo.domain.Ym
import br.app.relatoriocampo.ui.theme.AppColors
import br.app.relatoriocampo.ui.theme.NumStyle
import br.app.relatoriocampo.ui.theme.appColors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Textos no idioma escolhido. */
val LocalStr = staticCompositionLocalOf { Str("pt") }

/** Aumenta quando os dados são substituídos (importação/limpeza), para recarregar campos de texto locais. */
val LocalEpoch = staticCompositionLocalOf { 0 }

/** Snackbar com ação opcional (Desfazer, Já enviei...). */
class Snack(private val host: SnackbarHostState, private val scope: CoroutineScope) {
    fun show(message: String, action: String? = null, onAction: () -> Unit = {}) {
        scope.launch {
            host.currentSnackbarData?.dismiss()
            val r = host.showSnackbar(
                message = message, actionLabel = action, withDismissAction = false,
                duration = if (action != null) SnackbarDuration.Long else SnackbarDuration.Short,
            )
            if (r == SnackbarResult.ActionPerformed) onAction()
        }
    }
}

fun catIcon(cat: String): ImageVector = when (cat) {
    "aux" -> Icons.Outlined.StarOutline
    "reg" -> Icons.Outlined.WorkspacePremium
    else -> Icons.Outlined.Person
}

fun catOptions(t: Str, c: AppColors) = listOf(
    SegOpt("pub", t("pubS"), catIcon("pub"), c.slate),
    SegOpt("aux", t("auxS"), catIcon("aux"), c.amber),
    SegOpt("reg", t("regS"), catIcon("reg"), c.accentText),
)

@Composable
fun SectionCard(
    title: String? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val c = appColors
    Surface(
        modifier = modifier.fillMaxWidth().padding(bottom = 12.dp),
        shape = RoundedCornerShape(18.dp),
        color = c.card,
        border = BorderStroke(1.dp, c.line),
    ) {
        Column(Modifier.padding(16.dp)) {
            if (title != null) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 12.dp)) {
                    if (icon != null) {
                        Icon(icon, contentDescription = null, tint = c.accentText, modifier = Modifier.size(19.dp))
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(title, style = MaterialTheme.typography.titleMedium, color = c.ink)
                }
            }
            content()
        }
    }
}

@Composable
fun FieldLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text, modifier = modifier.padding(bottom = 6.dp),
        fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = appColors.muted,
    )
}

@Composable
fun AppDivider() = HorizontalDivider(color = appColors.line, thickness = 1.dp)

data class SegOpt(val value: String, val label: String, val icon: ImageVector? = null, val tint: Color? = null)

/** Seletor segmentado: todas as opções visíveis, a escolhida em destaque. */
@Composable
fun Segmented(
    options: List<SegOpt>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    stacked: Boolean = false,
) {
    val c = appColors
    val outer = RoundedCornerShape(14.dp)
    Row(
        modifier.fillMaxWidth().clip(outer).background(c.field).border(1.dp, c.line, outer).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        options.forEach { o ->
            val on = o.value == selected
            val shape = RoundedCornerShape(10.dp)
            val fg = if (on) c.ink else c.muted
            val iconTint = if (on) (o.tint ?: c.accentText) else c.muted
            Box(
                Modifier.weight(1f)
                    .heightIn(min = if (stacked) 58.dp else 44.dp)
                    .clip(shape)
                    .background(if (on) c.card else Color.Transparent)
                    .then(if (on) Modifier.border(1.dp, c.line, shape) else Modifier)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(o.value) })
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (stacked) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        o.icon?.let { Icon(it, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
                        Text(o.label, color = fg, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
                            maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        o.icon?.let { Icon(it, null, tint = iconTint, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(6.dp)) }
                        Text(o.label, color = fg, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun AppSwitch(checked: Boolean, onChange: ((Boolean) -> Unit)?) {
    val c = appColors
    Switch(
        checked = checked, onCheckedChange = onChange,
        colors = SwitchDefaults.colors(
            checkedTrackColor = c.accent, checkedThumbColor = Color.White, checkedBorderColor = Color.Transparent,
            uncheckedTrackColor = c.off, uncheckedThumbColor = Color.White, uncheckedBorderColor = Color.Transparent,
        ),
    )
}

/** Linha inteira clicável que liga/desliga o interruptor. */
@Composable
fun ToggleRow(label: String, checked: Boolean, small: Boolean = false, onChange: (Boolean) -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 54.dp)
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, Modifier.weight(1f).padding(end = 12.dp), color = appColors.ink, fontSize = if (small) 14.sp else 16.sp)
        AppSwitch(checked, null)
    }
}

@Composable
fun InfoRow(label: String, value: String, small: Boolean = false) {
    Row(Modifier.fillMaxWidth().heightIn(min = if (small) 44.dp else 52.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.weight(1f), color = appColors.ink, fontSize = if (small) 14.sp else 16.sp)
        Text(value, color = appColors.ink, fontWeight = FontWeight.SemiBold, style = NumStyle)
    }
}

@Composable
fun SquareIconButton(
    icon: ImageVector,
    description: String,
    size: Dp = 44.dp,
    ghost: Boolean = false,
    onClick: () -> Unit,
) {
    val c = appColors
    val shape = RoundedCornerShape(12.dp)
    Box(
        Modifier.size(size).clip(shape)
            .background(if (ghost) Color.Transparent else c.field)
            .then(if (!ghost) Modifier.border(1.dp, c.line, shape) else Modifier)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = description, tint = if (ghost) c.muted else c.ink, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun Stepper(value: Int, onChange: (Int) -> Unit) {
    val haptic = LocalHapticFeedback.current
    Row(verticalAlignment = Alignment.CenterVertically) {
        SquareIconButton(Icons.Rounded.Remove, "−1") {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(maxOf(0, value - 1))
        }
        Text("$value", Modifier.widthIn(min = 40.dp), textAlign = TextAlign.Center, fontSize = 20.sp,
            fontWeight = FontWeight.Bold, color = appColors.ink, style = NumStyle)
        SquareIconButton(Icons.Rounded.Add, "+1") {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove); onChange(value + 1)
        }
    }
}

enum class BtnStyle { Filled, Tonal, Outline, Danger }

@Composable
fun AppButton(
    text: String,
    icon: ImageVector? = null,
    style: BtnStyle = BtnStyle.Filled,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val c = appColors
    val (bg, fg, border) = when (style) {
        BtnStyle.Filled -> Triple(c.accent, c.accentInk, null)
        BtnStyle.Tonal -> Triple(c.accentSoft, c.accentText, null)
        BtnStyle.Outline -> Triple(Color.Transparent, c.ink, c.line)
        BtnStyle.Danger -> Triple(Color.Transparent, c.danger, c.danger)
    }
    val shape = RoundedCornerShape(14.dp)
    Row(
        modifier.heightIn(min = 50.dp).clip(shape).background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(20.dp)); Spacer(Modifier.width(8.dp)) }
        Text(text, color = fg, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
fun SmallButton(text: String, icon: ImageVector? = null, onClick: () -> Unit) {
    val c = appColors
    val shape = RoundedCornerShape(10.dp)
    Row(
        Modifier.heightIn(min = 40.dp).clip(shape).background(c.card).border(1.dp, c.line, shape)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = c.ink, modifier = Modifier.size(16.dp)); Spacer(Modifier.width(5.dp)) }
        Text(text, color = c.ink, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
fun Pill(text: String, icon: ImageVector?, fg: Color, bg: Color) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(bg).padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon?.let { Icon(it, null, tint = fg, modifier = Modifier.size(15.dp)); Spacer(Modifier.width(5.dp)) }
        Text(text, color = fg, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

@Composable
fun ProgressBar(fraction: Float, color: Color, modifier: Modifier = Modifier) {
    val f by animateFloatAsState(fraction.coerceIn(0f, 1f), label = "progress")
    Box(modifier.fillMaxWidth().height(10.dp).clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f))) {
        Box(Modifier.fillMaxWidth(f).fillMaxHeight().clip(RoundedCornerShape(50)).background(color))
    }
}

@Composable
fun Note(text: String, icon: ImageVector = Icons.Outlined.Info, warn: Boolean = false, modifier: Modifier = Modifier.padding(top = 10.dp)) {
    val c = appColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        modifier.fillMaxWidth().clip(shape).background(if (warn) c.amberSoft else c.field)
            .then(if (!warn) Modifier.border(1.dp, c.line, shape) else Modifier)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        Icon(icon, null, tint = if (warn) c.amber else c.muted, modifier = Modifier.padding(top = 1.dp).size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 13.sp, lineHeight = 18.sp, color = if (warn) c.ink else c.muted)
    }
}

@Composable
fun Banner(icon: ImageVector, text: String, info: Boolean = false, actions: (@Composable () -> Unit)? = null) {
    val c = appColors
    val shape = RoundedCornerShape(16.dp)
    val tone = if (info) c.accentText else c.amber
    Row(
        Modifier.fillMaxWidth().padding(bottom = 12.dp).clip(shape)
            .background(if (info) c.accentSoft else c.amberSoft)
            .border(1.dp, tone.copy(alpha = 0.25f), shape)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        Icon(icon, null, tint = tone, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = c.ink)
            if (actions != null) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(6.dp)) { actions() }
            }
        }
    }
}

@Composable
fun KindIcon(icon: ImageVector, fg: Color, bg: Color) {
    Box(Modifier.size(42.dp).clip(RoundedCornerShape(12.dp)).background(bg), contentAlignment = Alignment.Center) {
        Icon(icon, null, tint = fg, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun EmptyState(icon: ImageVector, text: String) {
    Column(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 6.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, null, tint = appColors.muted, modifier = Modifier.size(40.dp).alpha(0.5f))
        Spacer(Modifier.height(8.dp))
        Text(text, color = appColors.muted, fontSize = 14.sp, textAlign = TextAlign.Center)
    }
}

@Composable
fun fieldColors(): TextFieldColors {
    val c = appColors
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = c.accent, unfocusedBorderColor = c.line,
        focusedContainerColor = c.field, unfocusedContainerColor = c.field,
        cursorColor = c.accent, focusedTextColor = c.ink, unfocusedTextColor = c.ink,
        focusedPlaceholderColor = c.muted, unfocusedPlaceholderColor = c.muted,
    )
}

@Composable
fun AppTextField(
    value: String,
    onChange: (String) -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth(),
    placeholder: String = "",
    singleLine: Boolean = true,
    minLines: Int = 1,
) {
    OutlinedTextField(
        value = value, onValueChange = onChange, modifier = modifier,
        placeholder = { if (placeholder.isNotEmpty()) Text(placeholder) },
        singleLine = singleLine, minLines = minLines,
        shape = RoundedCornerShape(12.dp), colors = fieldColors(),
    )
}

/** Campo numérico (só dígitos). */
@Composable
fun NumField(value: String, onChange: (String) -> Unit, modifier: Modifier = Modifier, center: Boolean = false, maxLen: Int = 4) {
    OutlinedTextField(
        value = value,
        onValueChange = { v -> onChange(v.filter { it.isDigit() }.take(maxLen)) },
        modifier = modifier,
        placeholder = { Text("0", modifier = if (center) Modifier.fillMaxWidth() else Modifier, textAlign = if (center) TextAlign.Center else TextAlign.Start) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        textStyle = LocalTextStyle.current.copy(textAlign = if (center) TextAlign.Center else TextAlign.Start, fontFeatureSettings = "tnum"),
        shape = RoundedCornerShape(12.dp), colors = fieldColors(),
    )
}

/** Campo que parece um campo de texto mas abre um seletor ao tocar. */
@Composable
fun PickerField(text: String, icon: ImageVector, description: String, onClick: () -> Unit) {
    val c = appColors
    val shape = RoundedCornerShape(12.dp)
    Row(
        Modifier.fillMaxWidth().heightIn(min = 56.dp).clip(shape).background(c.field).border(1.dp, c.line, shape)
            .clickable(onClickLabel = description, role = Role.Button, onClick = onClick).padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text, Modifier.weight(1f), color = c.ink, fontSize = 16.sp)
        Icon(icon, null, tint = c.muted, modifier = Modifier.size(20.dp))
    }
}

@Composable
fun DateField(date: LocalDate, label: String, onChange: (LocalDate) -> Unit) {
    val ctx = LocalContext.current
    val lang = LocalStr.current.lang
    val text = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Ym.locale(lang)))
    PickerField(text, Icons.Outlined.CalendarMonth, label) {
        DatePickerDialog(ctx, { _, y, m, d -> onChange(LocalDate.of(y, m + 1, d)) }, date.year, date.monthValue - 1, date.dayOfMonth).show()
    }
}

@Composable
fun TimeField(hour: Int, minute: Int, label: String, onChange: (Int, Int) -> Unit) {
    val ctx = LocalContext.current
    PickerField("%02d:%02d".format(hour, minute), Icons.Outlined.Schedule, label) {
        TimePickerDialog(ctx, { _, h, m -> onChange(h, m) }, hour, minute, true).show()
    }
}

@Composable
fun PulsingDot(color: Color) {
    val tr = rememberInfiniteTransition(label = "pulse")
    val a by tr.animateFloat(1f, 0.25f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "alpha")
    Box(Modifier.padding(end = 10.dp).size(10.dp).alpha(a).clip(RoundedCornerShape(50)).background(color))
}

fun shareText(ctx: Context, text: String, title: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, text)
    }
    ctx.startActivity(Intent.createChooser(send, title))
}
