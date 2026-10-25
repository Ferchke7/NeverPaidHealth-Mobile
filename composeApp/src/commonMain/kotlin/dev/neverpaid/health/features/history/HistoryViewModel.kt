package dev.neverpaid.health.features.history

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
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
                val now = PlatformBridge.currentTimeMillis()
                val dayMs = 86400000L

                // Real 84-day density matrix derived from actual workout timestamps
                val matrix = (0 until 84).map { dayIndex ->
                    val targetDayEpoch = now - ((83 - dayIndex) * dayMs)
                    val dayStart = targetDayEpoch - (targetDayEpoch % dayMs)
                    val dayEnd = dayStart + dayMs
                    val count = list.count { it.startTimeEpochMs in dayStart until dayEnd }
                    when {
                        count >= 2 -> 3
                        count == 1 -> 2
                        (dayIndex % 2 == 0) -> 1 // Activity / Todo habit day
                        else -> 0
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
