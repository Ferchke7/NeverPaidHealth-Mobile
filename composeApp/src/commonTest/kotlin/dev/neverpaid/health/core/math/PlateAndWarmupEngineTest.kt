package dev.neverpaid.health.core.math

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PlateAndWarmupEngineTest {

    @Test
    fun testPlateCalculator_100kg_on_OlympicMensBar() {
        val result = PlateCalculatorEngine.calculatePlates(
            targetWeightKg = 100.0,
            barbell = BarbellType.OLYMPIC_MENS,
            useCollars = false
        )

        assertEquals(100.0, result.targetWeightKg)
        assertEquals(20.0, result.barbellWeightKg)
        assertEquals(40.0, result.weightPerSideKg)
        assertEquals(100.0, result.achievedTotalKg)
        assertTrue(result.isExactMatch)
        assertFalse(result.isOverLimit)

        // 40 kg per side should be 25kg + 15kg or 20kg + 20kg depending on greedy inventory
        val sumSide = result.platesPerSide.sumOf { it.spec.weightKg * it.count }
        assertEquals(40.0, sumSide)
    }

    @Test
    fun testPlateCalculator_with_Collars() {
        val result = PlateCalculatorEngine.calculatePlates(
            targetWeightKg = 140.0,
            barbell = BarbellType.OLYMPIC_MENS,
            useCollars = true, // 2.5kg collars pair = 5kg total
            collarPairWeightKg = 5.0
        )

        assertEquals(140.0, result.targetWeightKg)
        assertEquals(20.0, result.barbellWeightKg)
        assertEquals(5.0, result.collarWeightKg)
        // 140 - 25 = 115 / 2 = 57.5 kg per side
        assertEquals(57.5, result.weightPerSideKg)
        assertTrue(result.isExactMatch)
    }

    @Test
    fun testPlateCalculator_subBarWeight_clampsToBar() {
        val result = PlateCalculatorEngine.calculatePlates(
            targetWeightKg = 15.0,
            barbell = BarbellType.OLYMPIC_MENS // 20kg bar
        )

        assertEquals(20.0, result.achievedTotalKg)
        assertEquals(0.0, result.weightPerSideKg)
        assertTrue(result.platesPerSide.isEmpty())
    }

    @Test
    fun testWarmupProtocol_StandardHypertrophy() {
        val sets = WarmupProtocolEngine.generateWarmupSets(
            targetWorkingWeightKg = 100.0,
            protocol = WarmupProtocol.STANDARD_HYPERTROPHY,
            barWeightKg = 20.0
        )

        assertTrue(sets.isNotEmpty())
        // First set should always be empty bar
        val firstSet = sets.first()
        assertEquals(20.0, firstSet.weightKg)
        assertTrue(firstSet.reps >= 8)

        // Last warmup set should be approx 85-90% of working weight
        val lastWarmup = sets.last()
        assertTrue(lastWarmup.weightKg in 80.0..92.5)
        assertTrue(lastWarmup.reps in 2..5)
    }

    @Test
    fun testWarmupProtocol_PowerliftingHeavySingle() {
        val sets = WarmupProtocolEngine.generateWarmupSets(
            targetWorkingWeightKg = 180.0,
            protocol = WarmupProtocol.POWERLIFTING_HEAVY,
            barWeightKg = 20.0
        )

        assertTrue(sets.size >= 5)
        // Verify reps decay as weight ascends
        for (i in 0 until sets.size - 1) {
            assertTrue(sets[i].weightKg <= sets[i + 1].weightKg)
        }
        // Final heavy single primer should be 1 rep
        assertEquals(1, sets.last().reps)
    }

    @Test
    fun testWarmupProtocol_Wendler531() {
        val sets = WarmupProtocolEngine.generateWarmupSets(
            targetWorkingWeightKg = 100.0,
            protocol = WarmupProtocol.WENDLER_531,
            barWeightKg = 20.0
        )

        assertEquals(3, sets.size)
        assertEquals(40.0, sets[0].weightKg)
        assertEquals(5, sets[0].reps)
        assertEquals(50.0, sets[1].weightKg)
        assertEquals(5, sets[1].reps)
        assertEquals(60.0, sets[2].weightKg)
        assertEquals(3, sets[2].reps)
    }
}
