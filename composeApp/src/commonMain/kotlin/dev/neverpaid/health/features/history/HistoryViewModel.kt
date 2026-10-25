package dev.neverpaid.health.features.history

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class HistoryViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<HistoryUiAction, HistoryUiState, HistoryUiEffect>(
    initialState = HistoryUiState(isLoading = true)
) {

    init {
        loadHistory()
    }

    override fun onAction(action: HistoryUiAction) {
        when (action) {
            is HistoryUiAction.OnToggleWorkoutExpand -> {
                val current = currentState.expandedWorkoutId
                updateState {
                    copy(expandedWorkoutId = if (current == action.workoutId) null else action.workoutId)
                }
            }
            HistoryUiAction.OnRefreshClick -> loadHistory()
        }
    }

    private fun loadHistory() {
        launchGuarded {
            repository.getWorkoutHistory().collectLatest { list ->
                // Generate a 12-week (84 days) consistency heatmap
                val matrix = (0 until 84).map { dayIndex ->
                    // Simulated training density pattern
                    when {
                        dayIndex % 7 == 1 || dayIndex % 7 == 3 || dayIndex % 7 == 5 -> 3 // Heavy workout
                        dayIndex % 7 == 2 || dayIndex % 7 == 6 -> 1 // Active recovery / light
                        else -> 0 // Rest day
                    }
                }

                updateState {
                    copy(
                        workouts = list,
                        heatmapDensityMatrix = matrix,
                        totalWorkoutsLogged = list.size,
                        isLoading = false
                    )
                }
            }
        }
    }
}
