package com.rad.shifttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private enum class DayType(val label: String) {
    WORK("Radio sam"),
    SICK("Bolovanje"),
    VACATION("Godišnji"),
    OFF("Nisam radio")
}

@Composable
fun CalcDayEditDialog(
    year: Int,
    month: Int,
    day: Int,
    record: DayRecord?,
    onSave: (DayRecord?) -> Unit,
    onDismiss: () -> Unit
) {
    val initialType = when (record?.shift) {
        Shift.SICK -> DayType.SICK
        Shift.VACATION -> DayType.VACATION
        Shift.FIRST, Shift.SECOND, Shift.THIRD -> DayType.WORK
        else -> DayType.OFF
    }
    var type by remember { mutableStateOf(initialType) }
    var shift1 by remember { mutableStateOf(record?.shift?.takeIf { it in Shift.workShifts() } ?: Shift.FIRST) }
    var hours1 by remember { mutableStateOf(record?.hours?.takeIf { it > 0 } ?: 8) }
    var hasDouble by remember { mutableStateOf(record?.shift2 != null) }
    var shift2 by remember { mutableStateOf(record?.shift2 ?: Shift.SECOND) }
    var hours2 by remember { mutableStateOf(record?.hours2?.takeIf { it > 0 } ?: 4) }
    var overtimeType by remember { mutableStateOf(record?.overtimeType ?: OvertimeType.PAID) }
    var sickRate by remember { mutableStateOf(record?.sickPayRate ?: CONFIG.pay.sickPayRate) }

    PopupDialog(
        title = "$day. — unos dana",
        onDismiss = onDismiss,
        accent = THEME.accentPrimary
    ) {
        PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentPrimary) {
            SectionLabel("Tip dana")
            CalcChoiceRow(
                options = DayType.values().toList(),
                selected = type,
                label = { it.label },
                onSelect = { type = it },
                maxPerRow = 2
            )

            when (type) {
                DayType.WORK -> {
                    SectionLabel("Smena")
                    CalcChoiceRow(
                        options = Shift.workShifts(),
                        selected = shift1,
                        label = { it.fullName },
                        onSelect = { shift1 = it },
                        maxPerRow = 3
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Sati", color = THEME.textSecondary, style = MaterialTheme.typography.bodyMedium)
                        CalcStepper(value = hours1, onChange = { hours1 = it }, min = 1, max = 14)
                    }

                    SectionLabel("Dodatni rad (dupla smena / prekovremeno)")
                    CalcChoiceRow(
                        options = listOf(false, true),
                        selected = hasDouble,
                        label = { if (it) "Da" else "Ne" },
                        onSelect = { hasDouble = it },
                        maxPerRow = 2
                    )
                    if (hasDouble) {
                        CalcChoiceRow(
                            options = Shift.workShifts(),
                            selected = shift2,
                            label = { it.fullName },
                            onSelect = { shift2 = it },
                            maxPerRow = 3
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                        ) {
                            Text("Sati dodatno", color = THEME.textSecondary, style = MaterialTheme.typography.bodyMedium)
                            CalcStepper(value = hours2, onChange = { hours2 = it }, min = 1, max = 14)
                        }
                        CalcChoiceRow(
                            options = OvertimeType.values().toList(),
                            selected = overtimeType,
                            label = { if (it == OvertimeType.PAID) "Plaćeno" else "Kompenzacija" },
                            onSelect = { overtimeType = it },
                            maxPerRow = 2
                        )
                    }
                }

                DayType.SICK -> {
                    SectionLabel("Stopa bolovanja")
                    CalcChoiceRow(
                        options = listOf(0.65, 1.0),
                        selected = sickRate,
                        label = { "${(it * 100).toInt()}%" },
                        onSelect = { sickRate = it },
                        maxPerRow = 2
                    )
                }

                DayType.VACATION, DayType.OFF -> {
                    Text(
                        text = if (type == DayType.VACATION) "Godišnji odmor (100% osnovice)." else "Dan se ne računa u platu.",
                        color = THEME.textMuted,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RoundedButton(
                    text = "Obriši",
                    modifier = Modifier.weight(1f).height(50.dp),
                    bgColor = THEME.bgCard,
                    textColor = THEME.danger,
                    onClick = { onSave(null); onDismiss() }
                )
                GlowButton(
                    text = "Sačuvaj",
                    modifier = Modifier.weight(1f).height(50.dp),
                    bgColor = THEME.accentPrimary,
                    textColor = THEME.textDark
                ) {
                    onSave(buildRecord(year, month, day, type, shift1, hours1, hasDouble, shift2, hours2, overtimeType, sickRate))
                    onDismiss()
                }
            }
        }
    }
}

@Suppress("LongParameterList")
private fun buildRecord(
    year: Int,
    month: Int,
    day: Int,
    type: DayType,
    shift1: Shift,
    hours1: Int,
    hasDouble: Boolean,
    shift2: Shift,
    hours2: Int,
    overtimeType: OvertimeType,
    sickRate: Double
): DayRecord? {
    return when (type) {
        DayType.OFF -> null
        DayType.SICK -> DayRecord(year = year, month = month, day = day, shift = Shift.SICK, hours = 8, sickPayRate = sickRate)
        DayType.VACATION -> DayRecord(year = year, month = month, day = day, shift = Shift.VACATION, hours = 8)
        DayType.WORK -> DayRecord(
            year = year,
            month = month,
            day = day,
            shift = shift1,
            hours = hours1,
            shift2 = if (hasDouble) shift2 else null,
            hours2 = if (hasDouble) hours2 else 0,
            overtimeType = overtimeType
        )
    }
}
