package dev.neverpaid.health.features.todo

import dev.neverpaid.health.data.repository.NeverPaidRepositoryImpl
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
class TodoViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NeverPaidRepositoryImpl
    private lateinit var viewModel: TodoViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = NeverPaidRepositoryImpl()
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
        assertTrue(state.tasks.isNotEmpty(), "Should load seeded tasks")
        assertEquals(state.tasks.size, state.totalCount)
    }

    @Test
    fun testDateIsolatedTaskCompletion() = runTest {
        val today = viewModel.state.value.selectedDate
        val task = viewModel.state.value.tasks.first()
        val initialCompleted = task.isCompletedOn(today)

        // Toggle task today
        viewModel.onAction(TodoUiAction.OnToggleTask(task.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedTaskToday = viewModel.state.value.tasks.first { it.id == task.id }
        assertEquals(!initialCompleted, updatedTaskToday.isCompletedOn(today))

        // Switch to tomorrow: "2026-10-26" (or another date)
        val tomorrow = "2026-10-26"
        viewModel.onAction(TodoUiAction.OnSelectDate(tomorrow))
        testDispatcher.scheduler.advanceUntilIdle()

        // Verify date isolation: task completion on `today` does NOT leak into `tomorrow`
        val updatedTaskTomorrow = viewModel.state.value.tasks.first { it.id == task.id }
        assertFalse(
            updatedTaskTomorrow.isCompletedOn(tomorrow),
            "Task should NOT be completed on tomorrow because it was only completed today"
        )
    }

    @Test
    fun testAddNewTask() = runTest {
        val initialSize = viewModel.state.value.tasks.size

        viewModel.onAction(TodoUiAction.OnOpenAddTaskSheet)
        assertTrue(viewModel.state.value.isAddTaskSheetOpen)

        viewModel.onAction(TodoUiAction.OnNewTaskTitleChanged("Drink 3L of water"))
        viewModel.onAction(TodoUiAction.OnNewTaskTargetChanged("3000 ml"))
        viewModel.onAction(TodoUiAction.OnSaveNewTask)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddTaskSheetOpen)
        val newTasks = viewModel.state.value.tasks
        assertEquals(initialSize + 1, newTasks.size)
        assertTrue(newTasks.any { it.title == "Drink 3L of water" })
    }

    @Test
    fun testPomodoroTimerToggle() = runTest {
        viewModel.onAction(TodoUiAction.OnOpenPomodoro("test-task"))
        assertTrue(viewModel.state.value.isPomodoroSheetOpen)
        assertEquals("test-task", viewModel.state.value.activePomodoroTaskId)
        assertFalse(viewModel.state.value.isPomodoroRunning)

        viewModel.onAction(TodoUiAction.OnTogglePomodoroPlayPause)
        assertTrue(viewModel.state.value.isPomodoroRunning)

        viewModel.onAction(TodoUiAction.OnTogglePomodoroPlayPause)
        assertFalse(viewModel.state.value.isPomodoroRunning)

        viewModel.onAction(TodoUiAction.OnClosePomodoro)
        assertFalse(viewModel.state.value.isPomodoroSheetOpen)
    }
}
