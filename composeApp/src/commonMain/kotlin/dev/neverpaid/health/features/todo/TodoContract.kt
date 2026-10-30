package dev.neverpaid.health.features.todo

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.ActivityLogItem
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.Workout

data class DayCalendarItem(
    val dateString: String, // "YYYY-MM-DD"
    val dayName: String,
    val dayNumber: String,
    val isToday: Boolean,
    val isSelected: Boolean
)

data class TodoUiState(
    val viewMode: String = "list", // "list" | "calendar" | "log"
    val selectedDate: String = "",
    val calendarDays: List<DayCalendarItem> = emptyList(),
    val allTasks: List<TodoTask> = emptyList(),
    val tasksForSelectedDate: List<TodoTask> = emptyList(),
    val selectedCategoryFilter: String = "all",
    val activityLogs: List<ActivityLogItem> = emptyList(),
    val workouts: List<Workout> = emptyList(),
    val completedCount: Int = 0,
    val totalCount: Int = 0,
    val highPriorityRemaining: Int = 0,
    val isTaskModalOpen: Boolean = false,
    val editingTask: TodoTask? = null,
    val isLoading: Boolean = false
) : UiState

sealed interface TodoUiAction : UiAction {
    data class OnSetViewMode(val mode: String) : TodoUiAction
    data class OnSelectDate(val date: String) : TodoUiAction
    data class OnSelectCategoryFilter(val category: String) : TodoUiAction
    data class OnToggleTask(val taskId: String) : TodoUiAction
    data class OnDeleteTask(val taskId: String) : TodoUiAction
    data class OnOpenTaskModal(val task: TodoTask? = null) : TodoUiAction
    data object OnCloseTaskModal : TodoUiAction
    data class OnSaveTask(val request: SaveTodoRequestDto, val editingId: String? = null) : TodoUiAction
    data class OnStartFocusTimer(val task: TodoTask?) : TodoUiAction
    data object OnRefresh : TodoUiAction
}

sealed interface TodoUiEffect : UiEffect {
    data class StartWorkoutSession(val title: String) : TodoUiEffect
    data class ShowToast(val message: String) : TodoUiEffect
    data object TriggerHaptic : TodoUiEffect
}
