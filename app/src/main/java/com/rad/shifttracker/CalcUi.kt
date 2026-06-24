package com.rad.shifttracker

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/** Zadržava samo cifre i jedan decimalni separator (zapeta se normalizuje u tačku). */
fun filterDecimalInput(input: String): String {
    val filtered = StringBuilder()
    var hasSeparator = false
    for (ch in input) {
        when {
            ch.isDigit() -> filtered.append(ch)
            (ch == '.' || ch == ',') && !hasSeparator -> {
                filtered.append('.')
                hasSeparator = true
            }
        }
    }
    return filtered.toString()
}

/** Red dugmadi za izbor jedne od opcija (smena, mod, brigada). */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> CalcChoiceRow(
    options: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    maxPerRow: Int = 3
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        maxItemsInEachRow = maxPerRow
    ) {
        options.forEach { option ->
            val isSelected = option == selected
            RoundedButton(
                text = label(option),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp),
                bgColor = if (isSelected) THEME.accentPrimary else THEME.bgCardElevated,
                textColor = if (isSelected) THEME.textDark else THEME.textPrimary,
                bold = isSelected,
                onClick = { onSelect(option) }
            )
        }
    }
}

/** Stepper za cele brojeve (sati, godine staža). */
@Composable
fun CalcStepper(
    value: Int,
    onChange: (Int) -> Unit,
    min: Int = 0,
    max: Int = 99
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        RoundedButton(
            text = "−",
            modifier = Modifier.width(52.dp).height(46.dp),
            bgColor = THEME.bgCardElevated,
            textColor = THEME.textPrimary,
            bold = true,
            onClick = { onChange((value - 1).coerceAtLeast(min)) }
        )
        Text(
            text = value.toString(),
            color = THEME.textPrimary,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.width(56.dp),
            textAlign = TextAlign.Center,
            maxLines = 1
        )
        RoundedButton(
            text = "+",
            modifier = Modifier.width(52.dp).height(46.dp),
            bgColor = THEME.bgCardElevated,
            textColor = THEME.textPrimary,
            bold = true,
            onClick = { onChange((value + 1).coerceAtMost(max)) }
        )
    }
}

/** Labelovano polje za decimalni unos. */
@Composable
fun CalcDecimalField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    suffix: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = label,
            color = THEME.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        ModernTextField(
            value = value,
            onValueChange = { onValueChange(filterDecimalInput(it)) },
            hintText = "0",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.width(if (suffix == null) 130.dp else 110.dp)
        )
        if (suffix != null) {
            Text(
                text = suffix,
                color = THEME.textSecondary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

/** Red labela — vrednost (za razradu plate). */
@Composable
fun CalcInfoRow(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color = THEME.textPrimary,
    bold: Boolean = false,
    indent: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = if (indent) 14.dp else 0.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = THEME.textSecondary,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            color = color,
            style = if (bold) {
                MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            textAlign = TextAlign.End,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
