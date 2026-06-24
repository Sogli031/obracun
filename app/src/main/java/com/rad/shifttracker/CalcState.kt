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
    val finalPayment: MonthlyFinalPaymentBreakdown,
    val grossHourlyRate: Double,
    val hourlyValue: Double
)

/**
 * In-memory držač stanja za prost obračun plate za JEDAN mesec.
 * Ništa se ne perzistira — kad se app zatvori, stanje nestaje.
 */
class CalcState {
    // --- Osnovno ---
    var year by mutableStateOf(YearMonth.now().year)
    var month by mutableStateOf(YearMonth.now().monthValue)
    var coefficient by mutableStateOf(CONFIG.pay.defaultCoefficient)
    var yearsOfService by mutableStateOf(0)
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
    var regressGross by mutableStateOf(0.0)
    var bonusGross by mutableStateOf(0.0)
    var advanceNet by mutableStateOf(0.0)
    var creditDeductionNet by mutableStateOf(0.0)

    /** Dan u mesecu (1..31) -> zapis. Prazni dani (OFF) se ne čuvaju. */
    val records: SnapshotStateMap<Int, DayRecord> = mutableStateMapOf()

    val monthKey: MonthKey get() = MonthKey(year, month)

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

    private fun payConfig(): PayConfig = CONFIG.pay.copy(
        foodPerDay = foodPerDay,
        sickPayBaseHourly = payBaseHourly,
        difficultWorkConditionsPercent = difficultWorkConditionsPercent
    )

    private fun payBases(): ResolvedPayBases = ResolvedPayBases.fromDefaults(
        PayBaseDefaults(
            vacationHourlyBase = payBaseHourly,
            paidHolidayHourlyBase = payBaseHourly,
            sickHourlyBase = payBaseHourly
        )
    )

    fun compute(): CalcResult {
        val hourlyValue = defaultHourlyValueForMonth(year, month)
        val grossHourlyRate = coefficient * hourlyValue
        val config = payConfig()
        val calculator = PayCalculator(config)
        val bases = payBases()
        val recordList = records.values.sortedBy { it.day }

        val summary = calculator.calculateMonthSummary(
            records = recordList,
            baseRate = grossHourlyRate,
            startingCompBalance = startingCompBalance,
            yearsOfService = yearsOfService,
            brigadeType = brigadeType,
            summaryYear = year,
            summaryMonth = month,
            payBases = bases
        )

        val grossAdditions = MonthlyGrossAdditions(
            year = year,
            month = month,
            regressGross = regressGross,
            bonusGross = bonusGross
        )

        val netBreakdown = calculateMonthlyNetPayBreakdown(
            summary = summary,
            grossHourlyRate = grossHourlyRate,
            monthlyFundHours = calculateMonthlyFundHours(year, month, config.defaultHours),
            contributionsPercent = contributionsPercent,
            taxRelief = taxRelief,
            taxPercent = taxPercent,
            grossAdditions = grossAdditions,
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

        val finalPayment = calculateMonthlyFinalPaymentBreakdown(
            month = monthKey,
            isNetPayAvailable = netBreakdown.displayPay > 0.0,
            netPayBeforeAdvance = netBreakdown.displayPay,
            advancePaymentNet = advanceNet,
            loanDeductionNet = creditDeductionNet
        )

        return CalcResult(
            summary = summary,
            netBreakdown = netBreakdown,
            payDetails = payDetails,
            finalPayment = finalPayment,
            grossHourlyRate = grossHourlyRate,
            hourlyValue = hourlyValue
        )
    }
}
