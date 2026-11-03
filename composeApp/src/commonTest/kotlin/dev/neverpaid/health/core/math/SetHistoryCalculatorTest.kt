package dev.neverpaid.health.core.math

import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class SetHistoryCalculatorTest {

    @Test
    fun testNormalizeExerciseName() {
        assertEquals("weightedpullup", SetHistoryCalculator.normalizeExerciseName("Weighted Pull-Up (Lat Focus)"))
        assertEquals("benchpress", SetHistoryCalculator.normalizeExerciseName("Bench Press"))
        assertEquals("жимлёжа", SetHistoryCalculator.normalizeExerciseName("Жим лёжа (штанга)"))
        assertEquals("", SetHistoryCalculator.normalizeExerciseName(null))
    }

    @Test
    fun testFormatPlateBreakdown() {
        assertEquals("(20+15кг)", SetHistoryCalculator.formatPlateBreakdown(50.0))
        assertEquals("(20+2.5кг)", SetHistoryCalculator.formatPlateBreakdown(25.0))
        assertEquals("(20+40кг)", SetHistoryCalculator.formatPlateBreakdown(100.0))
        assertNull(SetHistoryCalculator.formatPlateBreakdown(24.0))
        assertNull(SetHistoryCalculator.formatPlateBreakdown(20.0))
        assertNull(SetHistoryCalculator.formatPlateBreakdown(15.0))
    }

    @Test
    fun testExtractExerciseHistoryAndTop3Records() {
        val workout1 = Workout(
            id = "w1",
            name = "Push Day",
            startTimeEpochMs = 1000000L,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    exerciseId = "ex1",
                    exerciseName = "Bench Press",
                    sets = listOf(
                        WorkoutSet(id = "s1", setNumber = 1, weightKg = 80.0, reps = 10, isCompleted = true),
                        WorkoutSet(id = "s2", setNumber = 2, weightKg = 85.0, reps = 8, isCompleted = true),
                        WorkoutSet(id = "s3", setNumber = 3, weightKg = 90.0, reps = 5, isCompleted = true)
                    )
                )
            )
        )

        val workout2 = Workout(
            id = "w2",
            name = "Push Day 2",
            startTimeEpochMs = 2000000L,
            isFinished = true,
            exercises = listOf(
                WorkoutExercise(
                    exerciseId = "ex1",
                    exerciseName = "Bench Press (Barbell)",
                    sets = listOf(
                        WorkoutSet(id = "s4", setNumber = 1, weightKg = 82.5, reps = 10, isCompleted = true),
                        WorkoutSet(id = "s5", setNumber = 2, weightKg = 90.0, reps = 8, isCompleted = true),
                        WorkoutSet(id = "s6", setNumber = 3, weightKg = 95.0, reps = 5, isCompleted = true)
                    )
                )
            )
        )

        val history = SetHistoryCalculator.extractExerciseHistory(
            workouts = listOf(workout1, workout2),
            exerciseId = "ex1",
            exerciseName = "Bench Press"
        )

        assertEquals(6, history.size)

        // Top records for Set #1
        val (topSet1, isFallback1) = SetHistoryCalculator.getTop3BestForSet(history, 1)
        assertFalse(isFallback1)
        assertEquals(2, topSet1.size)
        // 82.5 x 10 has higher 1RM than 80 x 10
        assertEquals(82.5, topSet1[0].weightKg)
        assertEquals(80.0, topSet1[1].weightKg)

        // Top records for Set #3
        val (topSet3, isFallback3) = SetHistoryCalculator.getTop3BestForSet(history, 3)
        assertFalse(isFallback3)
        assertEquals(2, topSet3.size)
        assertEquals(95.0, topSet3[0].weightKg)
        assertEquals(90.0, topSet3[1].weightKg)

        // All time PR
        val allTimePR = SetHistoryCalculator.getAllTimePR(history)
        assertNotNull(allTimePR.bestRecord)
        assertEquals(95.0, allTimePR.bestWeightKg)
        assertEquals(10, allTimePR.bestReps)
        assertEquals(6, allTimePR.totalSets)
    }

    @Test
    fun testFallbackWhenSetNumberHasNoDirectRecords() {
        val records = listOf(
            HistoricalSetRecord(
                workoutId = "w1",
                workoutName = "Session 1",
                dateEpochMs = 1000L,
                setNumber = 1,
                setType = "normal",
                weightKg = 50.0,
                reps = 8,
                e1rmKg = 62.0,
                volumeKg = 400.0
            )
        )

        // Looking for Set #5 which doesn't exist
        val (topSet5, isFallback) = SetHistoryCalculator.getTop3BestForSet(records, 5)
        assertTrue(isFallback)
        assertEquals(1, topSet5.size)
        assertEquals(50.0, topSet5[0].weightKg)
    }
}
