package dev.neverpaid.health.data.repository

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NeverPaidRepositoryTest {

    private lateinit var repository: NeverPaidRepositoryImpl

    @Before
    fun setup() {
        repository = NeverPaidRepositoryImpl()
    }

    @Test
    fun testExerciseCatalogFiltering() = runBlocking {
        val chestBarbellExercises = repository.searchExercises(
            query = "Bench",
            muscle = "chest",
            equipment = "barbell"
        ).first()

        assertFalse(chestBarbellExercises.isEmpty())
        assertTrue(chestBarbellExercises.any { it.name.contains("Bench") })
    }

    @Test
    fun testWorkoutLifecycle() = runBlocking {
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
    fun testCoachChat() = runBlocking {
        val reply = repository.sendCoachChatMessage("How is my bench press looking?")
        assertNotNull(reply)
        assertFalse(reply.isUser)
        assertTrue(reply.text.contains("Bench Press", ignoreCase = true))
    }
}
