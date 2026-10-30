package dev.neverpaid.health.core.math

import dev.neverpaid.health.data.catalog.StandardFoodCatalog
import dev.neverpaid.health.domain.model.AthleteHabit
import dev.neverpaid.health.domain.model.FastingProtocol
import dev.neverpaid.health.domain.model.FastingSession
import dev.neverpaid.health.domain.model.FastingStage
import dev.neverpaid.health.domain.model.FoodCategory
import dev.neverpaid.health.domain.model.HabitCategory
import dev.neverpaid.health.domain.model.ReadinessAssessment
import dev.neverpaid.health.domain.model.ReadinessTier
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class AnthropometryAndAthleticModelsTest {

    @Test
    fun testNavyBodyFat_MaleCalculation() {
        val result = AnthropometryEngine.calculateNavyBodyFat(
            isMale = true,
            heightCm = 180.0,
            neckCm = 40.0,
            waistCm = 82.0,
            weightKg = 80.0
        )

        assertNotNull(result)
        // Expected body fat for waist 82 / neck 40 at 180cm is roughly 12-14%
        assertTrue(result.bodyFatPercentage in 11.0..15.0)
        assertTrue(result.leanMassKg in 68.0..72.0)
        assertEquals(BodyFatCategory.ATHLETES, result.category)
    }

    @Test
    fun testNavyBodyFat_FemaleCalculation() {
        val result = AnthropometryEngine.calculateNavyBodyFat(
            isMale = false,
            heightCm = 168.0,
            neckCm = 34.0,
            waistCm = 68.0,
            hipCm = 96.0,
            weightKg = 58.0
        )

        assertNotNull(result)
        // Female healthy fitness range 20-25%
        assertTrue(result.bodyFatPercentage in 18.0..26.0)
        assertTrue(result.leanMassKg in 42.0..48.0)
    }

    @Test
    fun testAdonisGoldenRatio_Calculation() {
        val result = AnthropometryEngine.calculateAdonisIndex(
            heightCm = 180.0,
            waistCm = 80.0,
            shouldersCm = 129.5, // 129.5 / 80 = 1.61875 (Golden Ratio!)
            chestCm = 106.0
        )

        assertEquals(1.62, result.shoulderToWaistRatio)
        assertTrue(result.adonisScorePercentage >= 95)
    }

    @Test
    fun testCaseyButt_NaturalMaxMuscularPotential() {
        val result = AnthropometryEngine.calculateMaxMuscularPotential(
            heightCm = 180.0,
            wristCm = 18.0,
            ankleCm = 22.5,
            currentWeightKg = 82.0,
            currentBodyFatPct = 12.0
        )

        assertTrue(result.maxLeanMassKg in 75.0..88.0)
        assertTrue(result.maxWeightAt10PercentFatKg in 83.0..98.0)
        assertTrue(result.maxBicepsCm in 40.0..46.0)
        assertTrue(result.maxChestCm in 110.0..125.0)
    }

    @Test
    fun testReadinessAssessment_Scoring() {
        val optimalAssessment = ReadinessAssessment(
            id = "test_opt",
            date = "2026-10-30",
            sleepHours = 8.5,
            sleepQuality = 5,
            muscleSoreness = 1,
            cnsFatigue = 1,
            stressLevel = 1
        )

        assertTrue(optimalAssessment.score >= 90)
        assertEquals(ReadinessTier.PEAK_READINESS, optimalAssessment.tier)
        assertEquals(105, optimalAssessment.volumeModifierPercentage)

        val fatiguedAssessment = ReadinessAssessment(
            id = "test_fat",
            date = "2026-10-30",
            sleepHours = 4.5,
            sleepQuality = 1,
            muscleSoreness = 4,
            cnsFatigue = 4,
            stressLevel = 4
        )

        assertTrue(fatiguedAssessment.score <= 50)
        assertTrue(fatiguedAssessment.tier in listOf(ReadinessTier.MILD_FATIGUE, ReadinessTier.CRITICAL_RECOVERY))
        assertTrue(fatiguedAssessment.volumeModifierPercentage <= 80)
    }

    @Test
    fun testAthleteHabit_StreakCalculation() {
        val habit = AthleteHabit(
            id = "habit_creatine",
            title = "Креатин 5г",
            category = HabitCategory.NUTRITION,
            targetValue = "5г",
            completedDates = setOf(
                "2026-10-27",
                "2026-10-28",
                "2026-10-29",
                "2026-10-30"
            )
        )

        assertTrue(habit.isCompletedOn("2026-10-30"))
        assertEquals(4, habit.calculateCurrentStreak("2026-10-30"))

        // Missed today, completed yesterday
        val habitYesterday = habit.copy(completedDates = setOf("2026-10-28", "2026-10-29"))
        assertEquals(2, habitYesterday.calculateCurrentStreak("2026-10-30"))
    }

    @Test
    fun testFastingSession_Stages() {
        val baseTime = 1000000000000L
        val session16 = FastingSession(
            id = "fast_test",
            protocol = FastingProtocol.LEAN_GAINS_16_8,
            startTimeEpochMs = baseTime,
            targetDurationHours = 16.0
        )

        // At 14.5 hours elapsed -> Ketosis stage
        val at14Hours = baseTime + (14.5 * 3600 * 1000).toLong()
        assertEquals(FastingStage.KETOSIS, session16.currentStage(at14Hours))

        // At 16.5 hours elapsed -> Autophagy stage
        val at16Hours = baseTime + (16.5 * 3600 * 1000).toLong()
        assertEquals(FastingStage.AUTOPHAGY, session16.currentStage(at16Hours))
    }

    @Test
    fun testStandardFoodCatalog_SearchAndPortions() {
        val chicken = StandardFoodCatalog.findById("chicken_breast_cooked")
        assertNotNull(chicken)
        assertEquals(FoodCategory.PROTEIN_SOURCE, chicken.category)

        // 150g portion nutrient calculation
        val nutrients = chicken.calculateNutrients(150.0)
        assertEquals(150.0, nutrients.grams)
        // 165 kcal / 100g * 1.5 = 247.5
        assertEquals(247.5, nutrients.calories)
        // 31g protein / 100g * 1.5 = 46.5g
        assertEquals(46.5, nutrients.protein)

        // Search test
        val oatsSearch = StandardFoodCatalog.search("овсян")
        assertTrue(oatsSearch.isNotEmpty())
        assertTrue(oatsSearch.any { it.id == "oats_rolled_dry" })
    }
}
