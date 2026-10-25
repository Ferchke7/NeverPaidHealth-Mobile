package dev.neverpaid.health.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class NeverPaidRepositoryTest {

    private lateinit var repository: NeverPaidRepositoryImpl

    @BeforeTest
    fun setup() {
        repository = NeverPaidRepositoryImpl()
    }

    @Test
    fun testExerciseCatalogFiltering() = runTest {
        val chestBarbellExercises = repository.searchExercises(
            query = "Bench",
            muscle = "chest",
            equipment = "barbell"
        ).first()

        assertFalse(chestBarbellExercises.isEmpty())
        assertTrue(chestBarbellExercises.any { it.name.contains("Bench") })
    }

    @Test
    fun testWorkoutLifecycle() = runTest {
        assertNull(repository.getActiveWorkout().first())

        val started = repository.startWorkout("Leg Day")
        assertNotNull(repository.getActiveWorkout().first())
        assertEquals("Leg Day", started.title)

        val finished = repository.finishActiveWorkout()
        assertNotNull(finished)
        assertTrue(finished!!.isFinished)
        assertNull(repository.getActiveWorkout().first())

        val history = repository.getWorkoutHistory().first()
        assertTrue(history.any { it.id == started.id })
    }

    @Test
    fun testCoachChat() = runTest {
        val reply = repository.sendCoachChatMessage("How is my bench press looking?")
        assertNotNull(reply)
        assertFalse(reply.isUser)
        assertTrue(reply.text.contains("Bench Press", ignoreCase = true))
    }
}
