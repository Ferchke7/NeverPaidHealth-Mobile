package dev.neverpaid.health.core.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class StrengthCalculatorTest {

    @Test
    fun testGoldenE1RMVectors() {
        // 1 rep returns exact weight
        assertEquals(100.0, StrengthCalculator.calculateE1RM(100.0, 1)!!, 0.001)

        // 100 kg x 5 reps -> 116.7
        assertEquals(116.7, StrengthCalculator.calculateE1RM(100.0, 5)!!, 0.001)

        // 80 kg x 10 reps -> 106.7
        assertEquals(106.7, StrengthCalculator.calculateE1RM(80.0, 10)!!, 0.001)

        // 60 kg x 12 reps -> 84.0
        assertEquals(84.0, StrengthCalculator.calculateE1RM(60.0, 12)!!, 0.001)

        // > 12 reps are unreliable -> null
        assertNull(StrengthCalculator.calculateE1RM(100.0, 13))
        assertNull(StrengthCalculator.calculateE1RM(100.0, 20))
        assertNull(StrengthCalculator.calculateE1RM(0.0, 5))
    }

    @Test
    fun testGoldenVolumeVectors() {
        val sets = listOf(
            SetCalculationInput(weightKg = 100.0, reps = 5, type = "normal", completed = true),
            SetCalculationInput(weightKg = 60.0, reps = 10, type = "warmup", completed = true),
            SetCalculationInput(weightKg = 100.0, reps = 5, type = "normal", completed = false),
            SetCalculationInput(weightKg = 90.0, reps = 6, type = "drop", completed = true)
        )
        // (100 * 5) + (60 * 10) + (90 * 6) = 500 + 600 + 540 = 1640.0
        val volume = StrengthCalculator.calculateVolume(sets)
        assertEquals(1640.0, volume, 0.001)
    }

    @Test
    fun testGoldenBMIVectors() {
        assertEquals(24.7, StrengthCalculator.calculateBMI(80.0, 180.0)!!, 0.001)
        assertEquals(31.0, StrengthCalculator.calculateBMI(95.0, 175.0)!!, 0.001)
        assertEquals(22.5, StrengthCalculator.calculateBMI(65.0, 170.0)!!, 0.001)
    }

    @Test
    fun testGolden7DayMovingAverageVectors() {
        val weights1 = listOf(80.0, 80.5, 80.2, 79.8, 80.1, 79.9, 80.3)
        assertEquals(80.1, StrengthCalculator.calculate7DayMovingAverage(weights1)!!, 0.001)

        val weights2 = listOf(85.0, 84.5, 84.0)
        assertEquals(84.5, StrengthCalculator.calculate7DayMovingAverage(weights2)!!, 0.001)
    }
}
