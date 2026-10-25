package dev.neverpaid.health.features.todo

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.TodoTask

data class DayCalendarItem(
    val dateString: String, // "YYYY-MM-DD"
    val dayName: String,   // "ПН", "ВТ", ...
    val dayNumber: String, // "25", "26", ...
    val isToday: Boolean,
    val isSelected: Boolean
)

data class TodoUiState(
    val selectedDate: String = "",
    val calendarDays: List<DayCalendarItem> = emptyList(),
    val tasks: List<TodoTask> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val streakDays: Int = 4,

    // Task Creation Sheet
    val isAddTaskSheetOpen: Boolean = false,
    val newTaskTitle: String = "",
    val newTaskTarget: String = "",
    val newTaskCategory: String = "WORKOUT",
    val newTaskPriority: String = "NORMAL",
    val newTaskRecurring: Boolean = true,

    // Pomodoro Timer Sheet
    val isPomodoroSheetOpen: Boolean = false,
    val activePomodoroTaskId: String? = null,
    val pomodoroSecondsRemaining: Int = 25 * 60,
    val isPomodoroRunning: Boolean = false,
    val isBreakMode: Boolean = false,

    val isLoading: Boolean = false
) : UiState

sealed interface TodoUiAction : UiAction {
    data class OnSelectDate(val date: String) : TodoUiAction
    data class OnToggleTask(val taskId: String) : TodoUiAction
    data class OnDeleteTask(val taskId: String) : TodoUiAction

    // Add Task
    data object OnOpenAddTaskSheet : TodoUiAction
    data object OnCloseAddTaskSheet : TodoUiAction
    data class OnNewTaskTitleChanged(val title: String) : TodoUiAction
    data class OnNewTaskTargetChanged(val target: String) : TodoUiAction
    data class OnNewTaskCategoryChanged(val category: String) : TodoUiAction
    data class OnNewTaskPriorityChanged(val priority: String) : TodoUiAction
    data class OnNewTaskRecurringChanged(val recurring: Boolean) : TodoUiAction
    data object OnSaveNewTask : TodoUiAction

    // Pomodoro Timer
    data class OnOpenPomodoro(val taskId: String?) : TodoUiAction
    data object OnClosePomodoro : TodoUiAction
    data object OnTogglePomodoroPlayPause : TodoUiAction
    data object OnResetPomodoro : TodoUiAction
    data class OnSetPomodoroMode(val isBreak: Boolean) : TodoUiAction
}

sealed interface TodoUiEffect : UiEffect {
    data class StartWorkoutSession(val title: String) : TodoUiEffect
    data class ShowToast(val message: String) : TodoUiEffect
    data object TriggerHaptic : TodoUiEffect
}
