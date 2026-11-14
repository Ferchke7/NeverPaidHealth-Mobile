package dev.neverpaid.health.features.workout

import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.data.repository.FakeNeverPaidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class WorkoutViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNeverPaidRepository
    private lateinit var viewModel: WorkoutViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeNeverPaidRepository()
        val routine = StandardExerciseCatalog.prebuiltRoutines.first()
        repository.startWorkout("Push Day", routine)

        viewModel = WorkoutViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
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

        viewModel.onAction(WorkoutUiAction.OnUpdateWeight(exercise.exerciseId, firstSet.id, 100.0))
        viewModel.onAction(WorkoutUiAction.OnUpdateReps(exercise.exerciseId, firstSet.id, 5))
        viewModel.onAction(WorkoutUiAction.OnToggleSetComplete(exercise.exerciseId, firstSet.id))

        assertTrue(viewModel.state.value.isRestTimerActive)

        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isRestTimerActive)
        val updatedWorkout = viewModel.state.value.workout!!
        assertEquals(500.0, updatedWorkout.totalVolume)
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
    @Test
    fun addingExerciseStartsWithOneEmptySetLikeWeb() = runTest {
        val exercise = viewModel.state.value.availableExercises.first()
        viewModel.onAction(WorkoutUiAction.OnSelectExerciseToAdd(exercise))
        testDispatcher.scheduler.advanceUntilIdle()
        val sets = viewModel.state.value.workout!!.exercises.last().sets
        assertEquals(1, sets.size)
        assertEquals(0.0, sets.single().weightKg)
        assertFalse(sets.single().isCompleted)
    }

    @Test
    fun swappingExercisePreservesSetTypesAndResetsPerformance() = runTest {
        val previous = viewModel.state.value.workout!!.exercises.first()
        viewModel.onAction(WorkoutUiAction.OnOpenSwapExercisePicker(previous.exerciseId))
        val replacement = viewModel.state.value.availableExercises.first { it.id != previous.exerciseId }
        viewModel.onAction(WorkoutUiAction.OnSelectExerciseToAdd(replacement))
        testDispatcher.scheduler.advanceUntilIdle()
        val updated = viewModel.state.value.workout!!.exercises.first()
        assertEquals(replacement.id, updated.exerciseId)
        assertEquals(previous.sets.map { it.setType }, updated.sets.map { it.setType })
        assertTrue(updated.sets.all { it.weightKg == 0.0 && !it.isCompleted })
    }

}
