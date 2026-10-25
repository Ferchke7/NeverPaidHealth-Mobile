package dev.neverpaid.health.features.todo

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class TodoViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<TodoUiAction, TodoUiState, TodoUiEffect>(
    initialState = TodoUiState()
) {

    private var pomodoroJob: Job? = null

    init {
        val todayStr = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        val days = generateCalendarDays(todayStr, todayStr)
        updateState {
            copy(
                selectedDate = todayStr,
                calendarDays = days
            )
        }
        loadTasksForDate(todayStr)
    }

    override fun onAction(action: TodoUiAction) {
        when (action) {
            is TodoUiAction.OnSelectDate -> selectDate(action.date)
            is TodoUiAction.OnToggleTask -> toggleTask(action.taskId)
            is TodoUiAction.OnDeleteTask -> deleteTask(action.taskId)

            // Add Task
            TodoUiAction.OnOpenAddTaskSheet -> updateState { copy(isAddTaskSheetOpen = true) }
            TodoUiAction.OnCloseAddTaskSheet -> updateState { copy(isAddTaskSheetOpen = false) }
            is TodoUiAction.OnNewTaskTitleChanged -> updateState { copy(newTaskTitle = action.title) }
            is TodoUiAction.OnNewTaskTargetChanged -> updateState { copy(newTaskTarget = action.target) }
            is TodoUiAction.OnNewTaskCategoryChanged -> updateState { copy(newTaskCategory = action.category) }
            is TodoUiAction.OnNewTaskPriorityChanged -> updateState { copy(newTaskPriority = action.priority) }
            is TodoUiAction.OnNewTaskRecurringChanged -> updateState { copy(newTaskRecurring = action.recurring) }
            TodoUiAction.OnSaveNewTask -> saveNewTask()

            // Pomodoro
            is TodoUiAction.OnOpenPomodoro -> {
                updateState {
                    copy(
                        isPomodoroSheetOpen = true,
                        activePomodoroTaskId = action.taskId
                    )
                }
            }
            TodoUiAction.OnClosePomodoro -> {
                updateState { copy(isPomodoroSheetOpen = false) }
            }
            TodoUiAction.OnTogglePomodoroPlayPause -> togglePomodoro()
            TodoUiAction.OnResetPomodoro -> resetPomodoro()
            is TodoUiAction.OnSetPomodoroMode -> setPomodoroMode(action.isBreak)
        }
    }

    private fun selectDate(date: String) {
        val todayStr = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        val days = generateCalendarDays(todayStr, date)
        updateState {
            copy(
                selectedDate = date,
                calendarDays = days
            )
        }
        loadTasksForDate(date)
    }

    private fun loadTasksForDate(date: String) {
        launchGuarded {
            repository.getTodos(date).collectLatest { taskList ->
                val completed = taskList.count { it.isCompletedOn(date) }
                updateState {
                    copy(
                        tasks = taskList,
                        completedCount = completed,
                        totalCount = taskList.size,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun toggleTask(taskId: String) {
        val date = currentState.selectedDate
        val task = currentState.tasks.find { it.id == taskId } ?: return
        val willBeCompleted = !task.isCompletedOn(date)

        launchGuarded {
            repository.toggleTodo(taskId, date)
            if (willBeCompleted) {
                sendEffect(TodoUiEffect.TriggerHaptic)
            }
        }
    }

    private fun deleteTask(taskId: String) {
        launchGuarded {
            repository.deleteTodo(taskId)
        }
    }

    private fun saveNewTask() {
        val title = currentState.newTaskTitle.trim()
        val target = currentState.newTaskTarget.trim()
        if (title.isEmpty()) return

        val newTask = TodoTask(
            id = PlatformBridge.generateUuid(),
            title = title,
            target = target.ifEmpty { "Target completed" },
            category = currentState.newTaskCategory,
            priority = currentState.newTaskPriority,
            isRecurringDaily = currentState.newTaskRecurring,
            scheduledDate = if (currentState.newTaskRecurring) null else currentState.selectedDate
        )

        updateState {
            copy(
                isAddTaskSheetOpen = false,
                newTaskTitle = "",
                newTaskTarget = ""
            )
        }

        launchGuarded {
            repository.addTodo(newTask)
        }
    }

    private fun togglePomodoro() {
        if (currentState.isPomodoroRunning) {
            pomodoroJob?.cancel()
            updateState { copy(isPomodoroRunning = false) }
        } else {
            updateState { copy(isPomodoroRunning = true) }
            pomodoroJob = viewModelScope.launch {
                while (isActive && currentState.pomodoroSecondsRemaining > 0) {
                    delay(1000)
                    updateState {
                        val remaining = pomodoroSecondsRemaining - 1
                        copy(
                            pomodoroSecondsRemaining = remaining,
                            isPomodoroRunning = remaining > 0
                        )
                    }
                }
                if (currentState.pomodoroSecondsRemaining <= 0) {
                    sendEffect(TodoUiEffect.TriggerHaptic)
                    sendEffect(TodoUiEffect.ShowToast("Focus session completed! Great work!"))
                    // Auto-complete task if selected
                    val taskId = currentState.activePomodoroTaskId
                    if (taskId != null) {
                        repository.toggleTodo(taskId, currentState.selectedDate)
                    }
                }
            }
        }
    }

    private fun resetPomodoro() {
        pomodoroJob?.cancel()
        val defaultSecs = if (currentState.isBreakMode) 5 * 60 else 25 * 60
        updateState {
            copy(
                pomodoroSecondsRemaining = defaultSecs,
                isPomodoroRunning = false
            )
        }
    }

    private fun setPomodoroMode(isBreak: Boolean) {
        pomodoroJob?.cancel()
        val defaultSecs = if (isBreak) 5 * 60 else 25 * 60
        updateState {
            copy(
                isBreakMode = isBreak,
                pomodoroSecondsRemaining = defaultSecs,
                isPomodoroRunning = false
            )
        }
    }

    private fun generateCalendarDays(todayDateStr: String, selectedDateStr: String): List<DayCalendarItem> {
        val now = PlatformBridge.currentTimeMillis()
        val dayMs = 86400000L
        val result = mutableListOf<DayCalendarItem>()

        // 3 days before today up to 3 days after today (7 days window)
        for (offset in -3..3) {
            val dateEpoch = now + (offset * dayMs)
            val dateStr = PlatformBridge.formatDate(dateEpoch, "yyyy-MM-dd")
            val dayName = PlatformBridge.formatDate(dateEpoch, "EEE")
            val dayNumber = PlatformBridge.formatDate(dateEpoch, "d")

            result.add(
                DayCalendarItem(
                    dateString = dateStr,
                    dayName = dayName.uppercase(),
                    dayNumber = dayNumber,
                    isToday = dateStr == todayDateStr,
                    isSelected = dateStr == selectedDateStr
                )
            )
        }
        return result
    }

    override fun onCleared() {
        super.onCleared()
        pomodoroJob?.cancel()
    }
}
