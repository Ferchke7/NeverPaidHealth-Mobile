package dev.neverpaid.health.features.dashboard

import dev.neverpaid.health.data.repository.FakeNeverPaidRepository
import dev.neverpaid.health.domain.model.Exercise
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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DashboardViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNeverPaidRepository
    private lateinit var viewModel: DashboardViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeNeverPaidRepository()
        viewModel = DashboardViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun myRoutinesCustomOrderSupportsMoveUpDownAndMoveToTop() = runTest {
        val sampleEx = Exercise(
            id = "ex-1",
            name = "Bench Press",
            primaryMuscleGroup = "chest",
            equipment = "barbell"
        )
        val r1 = repository.createRoutine("Push 1", "", listOf(sampleEx))
        val r2 = repository.createRoutine("Pull 1", "", listOf(sampleEx))
        val r3 = repository.createRoutine("Legs 1", "", listOf(sampleEx))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(DashboardUiAction.OnToggleReorderRoutines)
        assertTrue(viewModel.state.value.isReorderingRoutines)

        // Move Legs 1 (index 2) directly to #1 ("top")
        viewModel.onAction(DashboardUiAction.OnMoveRoutineOrder(r3.id, "top"))
        testDispatcher.scheduler.advanceUntilIdle()

        var personalIds = viewModel.state.value.customRoutines
            .filter { !it.id.startsWith("prebuilt-") }
            .map { it.id }
        assertEquals(listOf(r3.id, r1.id, r2.id), personalIds)

        // Move Push 1 down below Pull 1
        viewModel.onAction(DashboardUiAction.OnMoveRoutineOrder(r1.id, "down"))
        testDispatcher.scheduler.advanceUntilIdle()

        personalIds = viewModel.state.value.customRoutines
            .filter { !it.id.startsWith("prebuilt-") }
            .map { it.id }
        assertEquals(listOf(r3.id, r2.id, r1.id), personalIds)

        viewModel.onAction(DashboardUiAction.OnToggleReorderRoutines)
        assertFalse(viewModel.state.value.isReorderingRoutines)
    }

    @Test
    fun routineEditorSupportsReorderingExercises() = runTest {
        viewModel.onAction(DashboardUiAction.OnOpenRoutineEditor(null))
        viewModel.onAction(DashboardUiAction.OnAddExerciseToEditor(Exercise(id = "e1", name = "Squat")))
        viewModel.onAction(DashboardUiAction.OnAddExerciseToEditor(Exercise(id = "e2", name = "Leg Press")))

        viewModel.onAction(DashboardUiAction.OnMoveEditorExercise(index = 1, moveUp = true))
        val names = viewModel.state.value.editorExercises.map { it.exerciseName }
        assertEquals(listOf("Leg Press", "Squat"), names)
    }
}
