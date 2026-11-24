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

    private lateinit var repository: FakeNeverPaidRepository

    @BeforeTest
    fun setup() {
        repository = FakeNeverPaidRepository()
    }

    @Test
    fun testExerciseCatalogFiltering() = runTest {
        val chestBarbellExercises = repository.searchExercises(
            query = "Bench",
            muscle = "chest",
            equipment = "barbell"
        ).first()

        assertFalse(chestBarbellExercises.isEmpty())
        assertTrue(chestBarbellExercises.any { it.name.contains("Bench", ignoreCase = true) })
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

    @Test
    fun focusTimerAdvancesAcrossBackgroundPomodoroPhasesAndStopwatch() = runTest {
        var fakeNow = 5_000_000L
        val realRepo = NeverPaidRepositoryImpl(apiClient = null, nowProvider = { fakeNow })
        realRepo.discardFocusTimer()

        // Start 50m Pomodoro (2 x 25m work rounds separated by 5m short break)
        realRepo.startQuickFocusSession(
            title = "Deep Work",
            targetMinutes = 50,
            strategy = "25m",
            mode = "pomodoro"
        )
        var state = realRepo.getFocusTimerState().first()
        assertTrue(state.isActive)
        assertTrue(state.isRunning)
        assertEquals("work", state.pomodoroPhase)
        assertEquals(25 * 60, state.secondsRemaining)

        // App stays backgrounded for 10 minutes
        fakeNow += 10 * 60 * 1000L
        realRepo.syncFocusTimerFromClock()
        state = realRepo.getFocusTimerState().first()
        assertEquals("work", state.pomodoroPhase)
        assertEquals(15 * 60, state.secondsRemaining)
        assertEquals(10 * 60, state.secondsElapsedTotal)

        // Pause for 5 minutes -> remaining must not decrease
        realRepo.pauseFocusTimer()
        fakeNow += 5 * 60 * 1000L
        realRepo.syncFocusTimerFromClock()
        state = realRepo.getFocusTimerState().first()
        assertFalse(state.isRunning)
        assertEquals(15 * 60, state.secondsRemaining)

        // Resume and lock screen for 17 minutes (crosses remaining 15m work + 2m into short break)
        realRepo.resumeFocusTimer()
        fakeNow += 17 * 60 * 1000L
        realRepo.syncFocusTimerFromClock()
        state = realRepo.getFocusTimerState().first()
        assertTrue(state.isRunning)
        assertEquals("short_break", state.pomodoroPhase)
        assertEquals(3 * 60, state.secondsRemaining)
        assertEquals(25 * 60, state.secondsElapsedTotal)

        // Switch to stopwatch and jump 125 seconds
        realRepo.setFocusMode("stopwatch")
        realRepo.resumeFocusTimer()
        fakeNow += 125_000L
        realRepo.syncFocusTimerFromClock()
        state = realRepo.getFocusTimerState().first()
        assertEquals("stopwatch", state.mode)
        assertEquals(125, state.secondsElapsedTotal)

        realRepo.discardFocusTimer()
    }
}
