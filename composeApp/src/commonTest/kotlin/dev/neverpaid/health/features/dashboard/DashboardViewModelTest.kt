package dev.neverpaid.health.features.dashboard

import dev.neverpaid.health.data.repository.FakeNeverPaidRepository
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.TrainingProgram
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
    fun weeklyPlanSupportsMultipleProgramsAndRoutinesPerDay_reorder_swap_and_done() = runTest {
        val coreRoutine = Routine(id = "r-core", name = "Morning Core")
        val program = TrainingProgram(
            id = "p-ppl",
            name = "Jeff PPL",
            difficulty = "intermediate",
            days = listOf(ProgramDay(dayNumber = 1, name = "Push Focus"))
        )

        viewModel.onAction(DashboardUiAction.OnOpenAddToPlanSheet(dayOfWeek = 1))
        viewModel.onAction(DashboardUiAction.OnAddRoutineToPlan(coreRoutine))
        testDispatcher.scheduler.advanceUntilIdle()

        viewModel.onAction(DashboardUiAction.OnOpenAddToPlanSheet(dayOfWeek = 1))
        viewModel.onAction(DashboardUiAction.OnAddProgramDayToPlan(program, program.days.first()))
        testDispatcher.scheduler.advanceUntilIdle()

        var monday = viewModel.state.value.weeklyPlanItems.filter { it.dayOfWeek == 1 }
        assertEquals(2, monday.size)
        assertEquals("Morning Core", monday[0].title)
        assertEquals("Push Focus", monday[1].title)

        // Reorder Push Focus above Morning Core within Monday
        viewModel.onAction(DashboardUiAction.OnMoveWeeklyPlanItemWithinDay(monday[1].id, moveUp = true))
        testDispatcher.scheduler.advanceUntilIdle()
        monday = viewModel.state.value.weeklyPlanItems.filter { it.dayOfWeek == 1 }
        assertEquals("Push Focus", monday[0].title)
        assertEquals("Morning Core", monday[1].title)

        // Toggle weekly completion check
        viewModel.onAction(DashboardUiAction.OnToggleWeeklyPlanItemDone(monday[0].id, completed = true))
        testDispatcher.scheduler.advanceUntilIdle()
        assertTrue(viewModel.state.value.weeklyPlanItems.first { it.id == monday[0].id }.isCompletedThisWeek)

        // Swap Monday (1) with Thursday (4)
        viewModel.onAction(DashboardUiAction.OnSelectPlanWeekday(1))
        viewModel.onAction(DashboardUiAction.OnToggleSwapDaysMode)
        viewModel.onAction(DashboardUiAction.OnSelectPlanWeekday(4))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(0, viewModel.state.value.weeklyPlanItems.count { it.dayOfWeek == 1 })
        assertEquals(2, viewModel.state.value.weeklyPlanItems.count { it.dayOfWeek == 4 })
    }

    @Test
    fun routineEditorAssignsMultipleWeekdaysOnSave() = runTest {
        val sampleEx = repository.getPrebuiltRoutines().first().exercises.first()
        viewModel.onAction(DashboardUiAction.OnOpenRoutineEditor(routine = null, defaultWeekday = 2))
        viewModel.onAction(DashboardUiAction.OnEditorTitleChanged("Arms & Delts"))
        viewModel.onAction(DashboardUiAction.OnToggleEditorWeekday(5))
        viewModel.onAction(
            DashboardUiAction.OnAddExerciseToEditor(
                dev.neverpaid.health.domain.model.Exercise(
                    id = sampleEx.exerciseId,
                    name = sampleEx.exerciseName,
                    primaryMuscleGroup = "arms",
                    equipment = "dumbbell"
                )
            )
        )
        viewModel.onAction(DashboardUiAction.OnSaveRoutineClick)
        testDispatcher.scheduler.advanceUntilIdle()

        val scheduledDays = viewModel.state.value.weeklyPlanItems.map { it.dayOfWeek }.sorted()
        assertEquals(listOf(2, 5), scheduledDays)
    }
}
