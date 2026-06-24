package com.rad.shifttracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.YearMonth

private val WEEKDAY_LABELS = listOf("Pon", "Uto", "Sre", "Čet", "Pet", "Sub", "Ned")

@Composable
fun CalcScreen() {
    val state = remember { CalcState() }
    var editDay by remember { mutableStateOf<Int?>(null) }
    var showAutoFill by remember { mutableStateOf(false) }
    var showRangeFill by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    val result = state.compute()
    val topPadding = WindowInsets.safeDrawing.asPaddingValues().calculateTopPadding()
    val bottomPadding = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()

    Box(modifier = Modifier.fillMaxSize().background(THEME.bgDark)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(top = topPadding + 12.dp, bottom = bottomPadding + 24.dp)
                .padding(horizontal = 14.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Obračun plate — jedan mesec",
                color = THEME.textPrimary,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )

            BasicInputsCard(state, onBulkFill = { showRangeFill = true })
            CalendarCard(state, onDayClick = { editDay = it }, onAutoFill = { showAutoFill = true })
            AdvancedCard(state, expanded = showAdvanced, onToggle = { showAdvanced = !showAdvanced })
            ResultCard(state, result)
        }
    }

    editDay?.let { day ->
        CalcDayEditDialog(
            year = state.year,
            month = state.month,
            day = day,
            record = state.records[day],
            onSave = { state.setDay(day, it) },
            onDismiss = { editDay = null }
        )
    }

    if (showAutoFill) {
        CalcAutoFillDialog(
            year = state.year,
            month = state.month,
            onConfirm = { state.autoFill(it) },
            onDismiss = { showAutoFill = false }
        )
    }

    if (showRangeFill) {
        CalcRangeFillDialog(
            year = state.year,
            month = state.month,
            onConfirm = { from, to, shift -> state.fillRange(from, to, shift) },
            onDismiss = { showRangeFill = false }
        )
    }
}

@Composable
private fun BasicInputsCard(state: CalcState, onBulkFill: () -> Unit) {
    PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentPrimary) {
        SectionLabel("Osnovni podaci")

        var coeffText by remember { mutableStateOf(numberText(state.coefficient)) }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Koeficijent", color = THEME.textSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            ModernTextField(
                value = coeffText,
                onValueChange = {
                    coeffText = filterDecimalInput(it)
                    coeffText.replace(',', '.').toDoubleOrNull()?.let { v -> state.coefficient = v }
                },
                hintText = "2.82",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.width(140.dp).height(56.dp)
            )
        }

        BoundDecimalField("Minuli rad (%)", state.minuliRadPercent, suffix = "%") { state.minuliRadPercent = it }

        SectionLabel("Popuna")
        RoundedButton(
            text = "Popuni opseg dana",
            modifier = Modifier.fillMaxWidth().height(46.dp),
            bgColor = THEME.accentSecondary,
            textColor = THEME.textDark,
            bold = true,
            onClick = onBulkFill
        )

        SectionLabel("Mesec")
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            RoundedButton(
                text = "◀",
                modifier = Modifier.width(64.dp).height(46.dp),
                bgColor = THEME.bgCard,
                textColor = THEME.textPrimary,
                onClick = {
                    val prev = YearMonth.of(state.year, state.month).minusMonths(1)
                    state.year = prev.year; state.month = prev.monthValue
                }
            )
            Text(
                text = monthYearText(MonthKey(state.year, state.month)),
                color = THEME.textPrimary,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            RoundedButton(
                text = "▶",
                modifier = Modifier.width(64.dp).height(46.dp),
                bgColor = THEME.bgCard,
                textColor = THEME.textPrimary,
                onClick = {
                    val next = YearMonth.of(state.year, state.month).plusMonths(1)
                    state.year = next.year; state.month = next.monthValue
                }
            )
        }
    }
}

@Composable
private fun CalendarCard(state: CalcState, onDayClick: (Int) -> Unit, onAutoFill: () -> Unit) {
    PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentSecondary) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            RoundedButton(
                text = "Auto-popuna",
                modifier = Modifier.weight(1f).height(46.dp),
                bgColor = THEME.accentSecondary,
                textColor = THEME.textPrimary,
                bold = true,
                onClick = onAutoFill
            )
            RoundedButton(
                text = "Obriši mesec",
                modifier = Modifier.weight(1f).height(46.dp),
                bgColor = THEME.bgCard,
                textColor = THEME.danger,
                onClick = { state.clearMonth() }
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            WEEKDAY_LABELS.forEach { label ->
                Text(
                    text = label,
                    color = THEME.textMuted,
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        getMonthMatrix(state.year, state.month).forEach { week ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                week.forEach { day ->
                    if (day == 0) {
                        Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                    } else {
                        DayCell(
                            day = day,
                            record = state.records[day],
                            modifier = Modifier.weight(1f),
                            onClick = { onDayClick(day) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(day: Int, record: DayRecord?, modifier: Modifier, onClick: () -> Unit) {
    val shift = record?.shift
    val background = shift?.color?.copy(alpha = 0.85f) ?: THEME.bgCard
    val textColor = if (shift != null) shift.textColor else THEME.textSecondary
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(background)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = day.toString(),
                color = textColor,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1
            )
            if (shift != null) {
                val label = if (record.shift2 != null) {
                    "${shift.displayName}+${record.shift2!!.displayName}"
                } else {
                    shift.displayName
                }
                Text(text = label, color = textColor, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

@Composable
private fun AdvancedCard(state: CalcState, expanded: Boolean, onToggle: () -> Unit) {
    PopupCard {
        RoundedButton(
            text = if (expanded) "Napredno  ▲" else "Napredno  ▼",
            modifier = Modifier.fillMaxWidth().height(46.dp),
            bgColor = THEME.bgCard,
            textColor = THEME.textPrimary,
            onClick = onToggle
        )
        if (expanded) {
            BoundDecimalField("Doprinosi (%)", state.contributionsPercent) { state.contributionsPercent = it }
            BoundDecimalField("Porez (%)", state.taxPercent) { state.taxPercent = it }
            BoundDecimalField("Poreska olakšica", state.taxRelief) { state.taxRelief = it }
            BoundDecimalField("Topli obrok / dan", state.foodPerDay) { state.foodPerDay = it }
            BoundDecimalField("Osnovica (bolovanje/GO)", state.payBaseHourly) { state.payBaseHourly = it }
            BoundDecimalField("Otežani uslovi (%)", state.difficultWorkConditionsPercent) { state.difficultWorkConditionsPercent = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sindikalna članarina", color = THEME.textSecondary, style = MaterialTheme.typography.bodyMedium)
                CalcChoiceRow(
                    options = listOf(false, true),
                    selected = state.unionMember,
                    label = { if (it) "Da" else "Ne" },
                    onSelect = { state.unionMember = it },
                    maxPerRow = 2
                )
            }
            if (state.unionMember) {
                BoundDecimalField("Stopa članarine (%)", state.unionDuesRate * 100.0, suffix = "%") {
                    state.unionDuesRate = it / 100.0
                }
            }
        }
    }
}

@Composable
private fun BoundDecimalField(label: String, value: Double, suffix: String? = null, onValue: (Double) -> Unit) {
    var text by remember { mutableStateOf(numberText(value)) }
    CalcDecimalField(
        label = label,
        value = text,
        suffix = suffix,
        onValueChange = {
            text = it
            it.replace(',', '.').toDoubleOrNull()?.let(onValue)
        }
    )
}

@Composable
private fun ResultCard(state: CalcState, result: CalcResult) {
    val net = result.netBreakdown
    PopupCard(bgColor = THEME.bgCardElevated, borderColor = THEME.accentPrimary) {
        SectionLabel("Neto plata")
        Text(
            text = "${formatMoney2(net.displayPay)} ${CONFIG.pay.currency}",
            color = THEME.accentPrimary,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold)
        )

        SectionLabel("Razrada")
        CalcInfoRow("Bruto ukupno", money(net.grossTotal))
        if (net.grossSeniorityBonus > 0.0) CalcInfoRow("— minuli rad (bruto)", money(net.grossSeniorityBonus), indent = true)
        CalcInfoRow("Doprinosi", "− ${money(net.employeeContributions)}", THEME.danger)
        CalcInfoRow("Poreska osnovica", money(net.taxableBase))
        CalcInfoRow("Porez", "− ${money(net.tax)}", THEME.danger)
        CalcInfoRow("Neto pre obustava", money(net.netPayBeforeDeductions), bold = true)
        if (net.mealSpentNet > 0.0) CalcInfoRow("Topli obrok", "− ${money(net.mealSpentNet)}", THEME.danger, indent = true)
        if (net.unionDuesNet > 0.0) CalcInfoRow("Sindikalna članarina", "− ${money(net.unionDuesNet)}", THEME.danger, indent = true)
        CalcInfoRow("= Neto plata", money(net.displayPay), THEME.accentPrimary, bold = true)

        SectionLabel("Ulazni podaci")
        CalcInfoRow("Bruto satnica", "${money(result.grossHourlyRate)} (${numberText(result.hourlyValue)} × ${numberText(state.coefficient)})")
        CalcInfoRow("Fond meseca", "${net.monthlyFundHours}h")
    }
}

private fun money(value: Double): String = "${formatMoney2(value)} RSD"

private fun numberText(value: Double): String {
    return if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
