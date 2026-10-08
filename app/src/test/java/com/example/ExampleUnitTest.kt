package com.example

import com.example.data.util.SecurityUtils
import org.junit.Assert.*
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testPasswordHashing() {
        val hash1 = SecurityUtils.hashPassword("123456")
        val hash2 = SecurityUtils.hashPassword("123456")
        assertEquals(hash1, hash2)
        assertTrue(SecurityUtils.verifyPassword("123456", hash1))
        assertFalse(SecurityUtils.verifyPassword("wrongpass", hash1))
    }

    @Test
    fun testFinancialFormula_SurplusAndDeficit() {
        val totalCommonBuildingExpenses = 14000.0 // 14 flats -> 1000 each
        val commonSharePerFlat = totalCommonBuildingExpenses / 14.0
        assertEquals(1000.0, commonSharePerFlat, 0.001)

        // Case A: Owner with deposit 3500, no assigned expense
        val depositA = 3500.0
        val assignedA = 0.0
        val balanceA = depositA - commonSharePerFlat - assignedA
        assertEquals(2500.0, balanceA, 0.001)
        assertTrue("Balance A should be surplus", balanceA > 0)

        // Case B: Owner with deposit 500, individual assigned repair 1000
        val depositB = 500.0
        val assignedB = 1000.0
        val balanceB = depositB - commonSharePerFlat - assignedB
        assertEquals(-1500.0, balanceB, 0.001)
        assertTrue("Balance B should be deficit", balanceB < 0)
    }
}
