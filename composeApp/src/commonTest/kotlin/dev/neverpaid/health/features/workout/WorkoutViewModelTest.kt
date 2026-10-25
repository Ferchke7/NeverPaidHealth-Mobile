package dev.neverpaid.health.features.workout

import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.data.repository.NeverPaidRepositoryImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NeverPaidRepositoryImpl
    private lateinit var viewModel: WorkoutViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = NeverPaidRepositoryImpl()
        // Start a workout with the first routine
        val routine = StandardExerciseCatalog.prebuiltRoutines.first()
        repository.startWorkout("Push Day", routine)

        viewModel = WorkoutViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testWorkoutInitialStateLoaded() = runTest {
        val state = viewModel.state.value
        assertNotNull(state.workout)
        assertEquals("Push Day", state.workout?.title)
        assertFalse(state.workout?.exercises.isNullOrEmpty())
    }

    @Test
    fun testUpdateWeightAndRepsRecalculatesVolume() = runTest {
        val workout = viewModel.state.value.workout!!
        val exercise = workout.exercises.first()
        val firstSet = exercise.sets.first()

        // Complete the first set with 100 kg x 5 reps
        viewModel.onAction(WorkoutUiAction.OnUpdateWeight(exercise.exerciseId, firstSet.id, 100.0))
        viewModel.onAction(WorkoutUiAction.OnUpdateReps(exercise.exerciseId, firstSet.id, 5))
        viewModel.onAction(WorkoutUiAction.OnToggleSetComplete(exercise.exerciseId, firstSet.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedWorkout = viewModel.state.value.workout!!
        // Volume for this set = 100 * 5 = 500.0 kg
        assertEquals(500.0, updatedWorkout.totalVolumeKg, 0.001)
        assertTrue(viewModel.state.value.isRestTimerActive)
    }

    @Test
    fun testAddAndDeleteSet() = runTest {
        val workout = viewModel.state.value.workout!!
        val exercise = workout.exercises.first()
        val initialSetCount = exercise.sets.size

        viewModel.onAction(WorkoutUiAction.OnAddSet(exercise.exerciseId))
        testDispatcher.scheduler.advanceUntilIdle()

        val afterAddWorkout = viewModel.state.value.workout!!
        val updatedEx = afterAddWorkout.exercises.first()
        assertEquals(initialSetCount + 1, updatedEx.sets.size)

        val newSet = updatedEx.sets.last()
        viewModel.onAction(WorkoutUiAction.OnDeleteSet(exercise.exerciseId, newSet.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val afterDeleteWorkout = viewModel.state.value.workout!!
        assertEquals(initialSetCount, afterDeleteWorkout.exercises.first().sets.size)
    }
}
