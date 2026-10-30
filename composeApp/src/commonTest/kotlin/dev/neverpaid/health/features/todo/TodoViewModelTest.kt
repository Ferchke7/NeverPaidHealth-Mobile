package dev.neverpaid.health.features.todo

import dev.neverpaid.health.data.repository.FakeNeverPaidRepository
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
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
class TodoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNeverPaidRepository
    private lateinit var viewModel: TodoViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeNeverPaidRepository()
        viewModel = TodoViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialTasksLoaded() = runTest {
        val state = viewModel.state.value
        assertFalse(state.selectedDate.isEmpty())
        assertTrue(state.tasksForSelectedDate.isNotEmpty(), "Should load tasks for selected date")
        assertEquals(state.tasksForSelectedDate.size, state.totalCount)
    }

    @Test
    fun testDateIsolatedTaskCompletion() = runTest {
        val today = viewModel.state.value.selectedDate
        val task = viewModel.state.value.tasksForSelectedDate.first()
        val initialCompleted = task.isCompletedOn(today)

        viewModel.onAction(TodoUiAction.OnToggleTask(task.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedTaskToday = viewModel.state.value.tasksForSelectedDate.first { it.id == task.id }
        assertEquals(!initialCompleted, updatedTaskToday.isCompletedOn(today))

        val tomorrow = "2026-10-26"
        viewModel.onAction(TodoUiAction.OnSelectDate(tomorrow))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedTaskTomorrow = viewModel.state.value.tasksForSelectedDate.first { it.id == task.id }
        assertFalse(
            updatedTaskTomorrow.isCompletedOn(tomorrow),
            "Task should NOT be completed on tomorrow because it was only completed today"
        )
    }

    @Test
    fun testAddNewTask() = runTest {
        val today = viewModel.state.value.selectedDate
        val initialSize = viewModel.state.value.tasksForSelectedDate.size

        viewModel.onAction(TodoUiAction.OnOpenTaskModal(null))
        assertTrue(viewModel.state.value.isTaskModalOpen)

        viewModel.onAction(
            TodoUiAction.OnSaveTask(
                request = SaveTodoRequestDto(
                    title = "Drink 3L of water",
                    description = "3000 ml",
                    category = "nutrition",
                    priority = "high",
                    scheduledDate = today,
                    durationMinutes = 15
                ),
                editingId = null
            )
        )
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isTaskModalOpen)
        val newTasks = viewModel.state.value.tasksForSelectedDate
        assertEquals(initialSize + 1, newTasks.size)
        assertTrue(newTasks.any { it.title == "Drink 3L of water" })
    }

    @Test
    fun testStartFocusTimerForTask() = runTest {
        val task = viewModel.state.value.tasksForSelectedDate.first()
        viewModel.onAction(TodoUiAction.OnStartFocusTimer(task))
        testDispatcher.scheduler.advanceUntilIdle()

        val focusState = repository.getFocusTimerState().first()
        assertTrue(focusState.isActive)
        assertEquals(task.id, focusState.todoId)
    }
}
