package com.rad.shifttracker

import org.junit.Assert.assertEquals
import org.junit.Test

class CalcStateTest {
    @Test
    fun `obracun koristi rucno unetu vrednost sata`() {
        val state = CalcState(initialHourlyValue = 240.5)
        state.coefficient = 2.82

        val result = state.compute()

        assertEquals(240.5, result.hourlyValue, 0.001)
        assertEquals(240.5 * 2.82, result.grossHourlyRate, 0.001)
    }
}
