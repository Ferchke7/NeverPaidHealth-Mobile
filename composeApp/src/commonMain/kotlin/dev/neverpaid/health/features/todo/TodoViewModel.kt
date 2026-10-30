package dev.neverpaid.health.features.todo

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class TodoViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<TodoUiAction, TodoUiState, TodoUiEffect>(
    initialState = TodoUiState(isLoading = true)
) {

    init {
        val todayStr = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        val days = generateCalendarDays(todayStr, todayStr)
        updateState {
            copy(
                selectedDate = todayStr,
                calendarDays = days
            )
        }
        observeTodoStreams()
        refreshRemoteTodos()
    }

    override fun onAction(action: TodoUiAction) {
        when (action) {
            is TodoUiAction.OnSetViewMode -> {
                updateState { copy(viewMode = action.mode) }
                if (action.mode == "log") {
                    launchGuarded { repository.getActivityLogs() }
                }
            }
            is TodoUiAction.OnSelectDate -> selectDate(action.date)
            is TodoUiAction.OnSelectCategoryFilter -> {
                updateState { copy(selectedCategoryFilter = action.category) }
                recomputeFilteredTasks()
            }
            is TodoUiAction.OnToggleTask -> toggleTask(action.taskId)
            is TodoUiAction.OnDeleteTask -> deleteTask(action.taskId)
            is TodoUiAction.OnOpenTaskModal -> {
                updateState { copy(isTaskModalOpen = true, editingTask = action.task) }
            }
            TodoUiAction.OnCloseTaskModal -> {
                updateState { copy(isTaskModalOpen = false, editingTask = null) }
            }
            is TodoUiAction.OnSaveTask -> saveTask(action.request, action.editingId)
            is TodoUiAction.OnStartFocusTimer -> {
                val task = action.task
                if (task != null) {
                    repository.startFocusForTodo(task)
                } else {
                    repository.startQuickFocusSession(
                        title = "Фокус-сессия атлета",
                        targetMinutes = 25
                    )
                }
            }
            TodoUiAction.OnRefresh -> refreshRemoteTodos()
        }
    }

    private fun observeTodoStreams() {
        combine(
            repository.observeAllTodos(),
            repository.observeActivityLogs(),
            repository.getWorkoutHistory()
        ) { allTodos, logs, workouts ->
            Triple(allTodos, logs, workouts)
        }.also { flow ->
            flow.combine(state) { (allTodos, logs, workouts), s ->
                val date = s.selectedDate
                val forDate = allTodos.filter { t ->
                    t.repeatDaily || t.scheduledDate.isBlank() || t.scheduledDate.take(10) == date
                }
                val filtered = if (s.selectedCategoryFilter == "all") {
                    forDate
                } else {
                    forDate.filter { it.category.equals(s.selectedCategoryFilter, ignoreCase = true) }
                }
                val completed = forDate.count { it.isCompletedOn(date) }
                val highRemaining = forDate.count {
                    !it.isCompletedOn(date) && it.priority.equals("high", ignoreCase = true)
                }
                updateState {
                    copy(
                        allTasks = allTodos,
                        tasksForSelectedDate = filtered,
                        activityLogs = logs,
                        workouts = workouts,
                        completedCount = completed,
                        totalCount = forDate.size,
                        highPriorityRemaining = highRemaining,
                        isLoading = false
                    )
                }
            }.launchIn(viewModelScope)
        }
    }

    private fun recomputeFilteredTasks() {
        val date = currentState.selectedDate
        val forDate = currentState.allTasks.filter { t ->
            t.repeatDaily || t.scheduledDate.isBlank() || t.scheduledDate.take(10) == date
        }
        val filtered = if (currentState.selectedCategoryFilter == "all") {
            forDate
        } else {
            forDate.filter { it.category.equals(currentState.selectedCategoryFilter, ignoreCase = true) }
        }
        updateState {
            copy(
                tasksForSelectedDate = filtered,
                completedCount = forDate.count { it.isCompletedOn(date) },
                totalCount = forDate.size,
                highPriorityRemaining = forDate.count { !it.isCompletedOn(date) && it.priority.equals("high", ignoreCase = true) }
            )
        }
    }

    private fun refreshRemoteTodos() {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.refreshTodos()
            repository.getActivityLogs()
            updateState { copy(isLoading = false) }
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
        recomputeFilteredTasks()
    }

    private fun toggleTask(taskId: String) {
        val date = currentState.selectedDate
        val task = currentState.allTasks.find { it.id == taskId } ?: return
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

    private fun saveTask(request: SaveTodoRequestDto, editingId: String?) {
        updateState { copy(isTaskModalOpen = false, editingTask = null) }
        launchGuarded {
            if (!editingId.isNullOrBlank()) {
                repository.updateTodo(editingId, request)
                sendEffect(TodoUiEffect.ShowToast("Задача обновлена"))
            } else {
                val task = TodoTask(
                    id = PlatformBridge.generateUuid(),
                    title = request.title,
                    description = request.notes,
                    category = request.category,
                    priority = request.priority,
                    scheduledDate = request.scheduledDate,
                    startTime = request.scheduledTime.ifBlank { null },
                    isRecurring = request.repeatDaily,
                    recurrencePattern = if (request.repeatDaily) "daily" else "none"
                )
                repository.addTodo(task)
                sendEffect(TodoUiEffect.ShowToast("Задача добавлена"))
            }
        }
    }

    private fun generateCalendarDays(todayDateStr: String, selectedDateStr: String): List<DayCalendarItem> {
        val now = PlatformBridge.currentTimeMillis()
        val dayMs = 86400000L
        val result = mutableListOf<DayCalendarItem>()

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
}
