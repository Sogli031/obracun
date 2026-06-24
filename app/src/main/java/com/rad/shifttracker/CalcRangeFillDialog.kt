package com.rad.shifttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import java.time.YearMonth

/** Tipovi koje bulk popuna nudi (radne smene + bolovanje/godišnji + brisanje). */
private val RANGE_FILL_OPTIONS = listOf(
    Shift.FIRST,
    Shift.SECOND,
    Shift.THIRD,
    Shift.VACATION,
    Shift.SICK,
    Shift.OFF
)

// 100% = obračun po punoj osnovici (godišnji), 65% = obračun po stopi bolovanja.
// Tip dana nije bitan korisniku — bitna je stopa po kojoj se računa.
private fun rangeFillLabel(shift: Shift): String = when (shift) {
    Shift.FIRST -> "Prva"
    Shift.SECOND -> "Druga"
    Shift.THIRD -> "Treća"
    Shift.VACATION -> "100%"
    Shift.SICK -> "65%"
    Shift.OFF -> "Obriši"
}

@Composable
fun CalcRangeFillDialog(
    year: Int,
    month: Int,
    onConfirm: (fromDay: Int, toDay: Int, shift: Shift) -> Unit,
    onDismiss: () -> Unit
) {
    val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
    val weeks = remember(year, month) { getWorkWeekRanges(year, month) }
    var fromText by remember { mutableStateOf("1") }
    var toText by remember { mutableStateOf(daysInMonth.toString()) }
    var shift by remember { mutableStateOf(Shift.FIRST) }
    val selectedWeek = weeks.firstOrNull { it.fromDay.toString() == fromText && it.toDay.toString() == toText }

    PopupDialog(
        title = "Popuna opsega dana",
        onDismiss = onDismiss,
        accent = THEME.accentSecondary
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentSecondary) {
            SectionLabel("Opseg (dani u mesecu)")
            DayInputRow("Od dana", fromText, daysInMonth) { fromText = it }
            DayInputRow("Do dana", toText, daysInMonth) { toText = it }

            SectionLabel("Sedmice")
            CalcChoiceRow(
                options = weeks,
                selected = selectedWeek ?: WeekRange(-1, -1, -1),
                label = { "${it.index}. sedmica  ${it.fromDay}–${it.toDay}" },
                onSelect = {
                    fromText = it.fromDay.toString()
                    toText = it.toDay.toString()
                },
                maxPerRow = 2
            )

            SectionLabel("Šta da popunim")
            CalcChoiceRow(
                options = RANGE_FILL_OPTIONS,
                selected = shift,
                label = ::rangeFillLabel,
                onSelect = { shift = it },
                maxPerRow = 3
            )

            Text(
                text = "Popunjava sve dane u opsegu izabranim tipom. „Obriši“ uklanja unose u opsegu.",
                color = THEME.textMuted,
                style = MaterialTheme.typography.bodySmall
            )

            GlowButton(
                text = "Popuni",
                modifier = Modifier.fillMaxWidth().height(52.dp),
                bgColor = THEME.accentPrimary,
                textColor = THEME.textDark
            ) {
                val from = fromText.toIntOrNull()?.coerceIn(1, daysInMonth) ?: 1
                val to = toText.toIntOrNull()?.coerceIn(1, daysInMonth) ?: daysInMonth
                onConfirm(from, to, shift)
                onDismiss()
            }
        }
    }
}

@Composable
private fun DayInputRow(label: String, value: String, maxDay: Int, onChange: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = THEME.textSecondary, style = MaterialTheme.typography.bodyMedium)
        ModernTextField(
            value = value,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }.take(2)
                val clamped = digits.toIntOrNull()?.let { if (it > maxDay) maxDay.toString() else digits } ?: digits
                onChange(clamped)
            },
            hintText = "1",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.width(96.dp).height(56.dp)
        )
    }
}
