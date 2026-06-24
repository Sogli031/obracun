package com.rad.shifttracker

import java.time.YearMonth

internal fun getMonthMatrix(year: Int, month: Int): List<List<Int>> {
    val yearMonth = YearMonth.of(year, month)
    val firstDay = yearMonth.atDay(1)
    val startOffset = firstDay.dayOfWeek.value - 1
    val daysInMonth = yearMonth.lengthOfMonth()
    val cells = MutableList(startOffset) { 0 } + (1..daysInMonth).toList()
    return cells.chunked(7).map { week ->
        if (week.size == 7) week else week + List(7 - week.size) { 0 }
    }
}

/** Opseg radne sedmice (pon–pet) u mesecu. */
data class WeekRange(val index: Int, val fromDay: Int, val toDay: Int)

/**
 * Radne sedmice meseca: za svaku kalendarsku sedmicu (pon–ned) uzima radne dane (pon–pet)
 * koji padaju u mesec i vraća njihov opseg. Npr. mesec koji počinje ponedeljkom: 1–5, 8–12, ...
 */
internal fun getWorkWeekRanges(year: Int, month: Int): List<WeekRange> {
    return getMonthMatrix(year, month)
        .mapNotNull { week ->
            val workdays = week.take(WORKDAYS_PER_WEEK).filter { it != 0 }
            if (workdays.isEmpty()) null else workdays.min() to workdays.max()
        }
        .mapIndexed { i, (from, to) -> WeekRange(index = i + 1, fromDay = from, toDay = to) }
}

private const val WORKDAYS_PER_WEEK = 5
