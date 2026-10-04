package com.rad.shifttracker

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateMap
import java.time.LocalDate
import java.time.YearMonth

/** Rezultat obračuna za jedan mesec. */
data class CalcResult(
    val summary: MonthSummary,
    val netBreakdown: MonthlyNetPayBreakdown,
    val payDetails: MonthlyPayDetails,
    val grossHourlyRate: Double,
    val hourlyValue: Double
)

/**
 * In-memory držač stanja za prost obračun plate za JEDAN mesec.
 * Ništa se ne perzistira — kad se app zatvori, stanje nestaje.
 */
class CalcState(initialHourlyValue: Double = defaultHourlyValueForMonth(YearMonth.now().year, YearMonth.now().monthValue)) {
    // --- Osnovno ---
    var year by mutableStateOf(YearMonth.now().year)
    var month by mutableStateOf(YearMonth.now().monthValue)
    var coefficient by mutableStateOf(CONFIG.pay.defaultCoefficient)
    var hourlyValue by mutableStateOf(initialHourlyValue)
    var minuliRadPercent by mutableStateOf(0.0)
    var brigadeType by mutableStateOf(BrigadeType.TROBRIGADA)

    // --- Napredno (default iz CONFIG.pay) ---
    var contributionsPercent by mutableStateOf(CONFIG.pay.defaultEmployeeContributionsPercent)
    var taxPercent by mutableStateOf(CONFIG.pay.defaultTaxPercent)
    var taxRelief by mutableStateOf(CONFIG.pay.defaultTaxRelief)
    var foodPerDay by mutableStateOf(CONFIG.pay.foodPerDay)
    var payBaseHourly by mutableStateOf(CONFIG.pay.sickPayBaseHourly)
    var difficultWorkConditionsPercent by mutableStateOf(CONFIG.pay.difficultWorkConditionsPercent)
    var startingCompBalance by mutableStateOf(0)
    var unionMember by mutableStateOf(false)
    var unionDuesRate by mutableStateOf(DEFAULT_UNION_DUES_RATE)

    /** Dan u mesecu (1..31) -> zapis. Prazni dani (OFF) se ne čuvaju. */
    val records: SnapshotStateMap<Int, DayRecord> = mutableStateMapOf()

    fun setDay(day: Int, record: DayRecord?) {
        if (record == null || (record.shift == Shift.OFF && record.shift2 == null)) {
            records.remove(day)
        } else {
            records[day] = record
        }
    }

    fun clearMonth() {
        records.clear()
    }

    /** Auto-popuna meseca po izabranom ciklusu smena (kao u glavnoj app, ali samo za ovaj mesec). */
    fun autoFill(settings: ShiftFillSettings) {
        brigadeType = settings.mode.brigadeType
        val scheduler = ShiftScheduler()
        records.clear()
        val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = LocalDate.of(year, month, day)
            val shift = scheduler.calculateShiftForDate(date, settings)
            if (shift in Shift.workShifts()) {
                records[day] = DayRecord(year, month, day, shift, CONFIG.pay.defaultHours)
            }
        }
    }

    /**
     * Bulk popuna opsega dana (od..do) istom smenom/tipom.
     * [shift] == OFF briše dane u opsegu. Radne smene i bolovanje/godišnji dobijaju podrazumevane sate.
     */
    fun fillRange(fromDay: Int, toDay: Int, shift: Shift) {
        val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
        val start = minOf(fromDay, toDay).coerceIn(1, daysInMonth)
        val end = maxOf(fromDay, toDay).coerceIn(1, daysInMonth)
        for (day in start..end) {
            val record = when (shift) {
                Shift.OFF -> null
                Shift.SICK -> DayRecord(year, month, day, Shift.SICK, CONFIG.pay.defaultHours, sickPayRate = CONFIG.pay.sickPayRate)
                else -> DayRecord(year, month, day, shift, CONFIG.pay.defaultHours)
            }
            setDay(day, record)
        }
    }

    // Korisnik direktno unosi procenat minulog rada (%). Obračun ga primenjuje preko
    // minuliRadPerYear uz yearsOfService = 1, pa je efektivna stopa tačno uneti procenat.
    private fun payConfig(): PayConfig = CONFIG.pay.copy(
        foodPerDay = foodPerDay,
        sickPayBaseHourly = payBaseHourly,
        difficultWorkConditionsPercent = difficultWorkConditionsPercent,
        minuliRadPerYear = minuliRadPercent / 100.0
    )

    private fun payBases(): ResolvedPayBases = ResolvedPayBases.fromDefaults(
        PayBaseDefaults(
            vacationHourlyBase = payBaseHourly,
            paidHolidayHourlyBase = payBaseHourly,
            sickHourlyBase = payBaseHourly
        )
    )

    fun compute(): CalcResult {
        val selectedHourlyValue = hourlyValue
        val grossHourlyRate = coefficient * selectedHourlyValue
        val config = payConfig()
        val calculator = PayCalculator(config)
        val bases = payBases()
        val recordList = records.values.sortedBy { it.day }

        val summary = calculator.calculateMonthSummary(
            records = recordList,
            baseRate = grossHourlyRate,
            startingCompBalance = startingCompBalance,
            yearsOfService = if (minuliRadPercent > 0.0) 1 else 0,
            brigadeType = brigadeType,
            summaryYear = year,
            summaryMonth = month,
            payBases = bases
        )

        val netBreakdown = calculateMonthlyNetPayBreakdown(
            summary = summary,
            grossHourlyRate = grossHourlyRate,
            monthlyFundHours = calculateMonthlyFundHours(year, month, config.defaultHours),
            contributionsPercent = contributionsPercent,
            taxRelief = taxRelief,
            taxPercent = taxPercent,
            includeRegress = false,
            unionMember = unionMember,
            unionDuesRate = unionDuesRate
        )

        val payDetails = calculator.calculateMonthlyPayDetails(
            records = recordList,
            summary = summary,
            baseRate = grossHourlyRate,
            netBreakdown = netBreakdown,
            brigadeType = brigadeType,
            payBases = bases
        )

        return CalcResult(
            summary = summary,
            netBreakdown = netBreakdown,
            payDetails = payDetails,
            grossHourlyRate = grossHourlyRate,
            hourlyValue = selectedHourlyValue
        )
    }
}
