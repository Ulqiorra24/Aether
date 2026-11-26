package com.example.v2rayng

import org.junit.Test
import org.junit.Assert.*

class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun testSubscriptionDaysCalculation() {
        val expireSec = 1798705874L // Dec 31, 2026 08:31:14 UTC
        val nowMs = System.currentTimeMillis() // Nov 26, 2026
        val diffMs = expireSec * 1000L - nowMs
        val days = if (diffMs <= 0) 0 else ((diffMs + 86400_000L - 1) / 86400_000L).toInt()
        println("Calculated days from header expire 1798705874: $days")
        assertTrue("Days should be around 35", days in 34..36)
    }

    @Test
    fun testExpiredTodayReturnsZero() {
        val nowMs = System.currentTimeMillis()
        val expiredSecToday = (nowMs - 3600_000L) / 1000L // 1 hour ago today
        val diffMs = expiredSecToday * 1000L - nowMs
        val days = if (diffMs <= 0) 0 else ((diffMs + 86400_000L - 1) / 86400_000L).toInt()
        assertEquals(0, days)
    }
}
