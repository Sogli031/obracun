package com.rad.shifttracker

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.time.LocalDate

@Composable
fun CalcAutoFillDialog(
    year: Int,
    month: Int,
    onConfirm: (ShiftFillSettings) -> Unit,
    onDismiss: () -> Unit
) {
    var mode by remember { mutableStateOf(ShiftFillMode.THREE_BRIGADE) }
    var shift by remember { mutableStateOf(Shift.FIRST) }
    var blockDay by remember { mutableStateOf(0) }

    val shiftOptions = when (mode) {
        ShiftFillMode.ONE_SHIFT -> listOf(Shift.FIRST)
        ShiftFillMode.FIRST_SECOND -> listOf(Shift.FIRST, Shift.SECOND)
        ShiftFillMode.THREE_BRIGADE -> listOf(Shift.FIRST, Shift.SECOND, Shift.THIRD)
        ShiftFillMode.FOUR_BRIGADE -> listOf(Shift.FIRST, Shift.SECOND, Shift.THIRD, Shift.OFF)
    }
    if (shift !in shiftOptions) {
        shift = shiftOptions.first()
    }

    PopupDialog(
        title = "Auto-popuna meseca",
        onDismiss = onDismiss,
        accent = THEME.accentSecondary
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentSecondary) {
            SectionLabel("Ciklus smena")
            CalcChoiceRow(
                options = ShiftFillMode.values().toList(),
                selected = mode,
                label = { it.label },
                onSelect = { mode = it },
                maxPerRow = 2
            )

            SectionLabel("Početna smena (prva sedmica)")
            CalcChoiceRow(
                options = shiftOptions,
                selected = shift,
                label = { it.fullName },
                onSelect = { shift = it },
                maxPerRow = 2
            )

            if (mode == ShiftFillMode.FOUR_BRIGADE) {
                SectionLabel("Dan u bloku")
                CalcChoiceRow(
                    options = listOf(0, 1),
                    selected = blockDay,
                    label = { if (it == 0) "Prvi dan" else "Drugi dan" },
                    onSelect = { blockDay = it },
                    maxPerRow = 2
                )
            }

            Text(
                text = "Popunjava radne dane izabranog meseca po ciklusu. Vikendi i praznici se preskaču.",
                color = THEME.textMuted,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall
            )

            GlowButton(
                text = "Popuni",
                modifier = Modifier.fillMaxWidth().height(52.dp),
                bgColor = THEME.accentPrimary,
                textColor = THEME.textDark
            ) {
                val anchor = LocalDate.of(year, month, 1)
                val settings = if (mode == ShiftFillMode.FOUR_BRIGADE) {
                    ShiftFillSettings(
                        mode = mode,
                        anchorDate = anchor,
                        selectedShift = shift,
                        cycleDayIndex = ShiftScheduler().getFourBrigadeCycleStartIndex(shift, blockDay)
                    )
                } else {
                    ShiftFillSettings(mode = mode, anchorDate = anchor, selectedShift = shift)
                }
                onConfirm(settings)
                onDismiss()
            }
        }
    }
}
