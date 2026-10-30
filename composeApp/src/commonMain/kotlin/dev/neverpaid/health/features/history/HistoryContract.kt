package dev.neverpaid.health.features.history

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.Workout

data class HistoryUiState(
    val workouts: List<Workout> = emptyList(),
    val heatmapDensityMatrix: List<Int> = emptyList(),
    val expandedWorkoutId: String? = null,
    val totalWorkoutsLogged: Int = 0,
    val isLoading: Boolean = false
) : UiState

sealed interface HistoryUiAction : UiAction {
    data class OnToggleWorkoutExpand(val workoutId: String) : HistoryUiAction
    data class OnRepeatWorkout(val workout: Workout) : HistoryUiAction
    data class OnDeleteWorkout(val workoutId: String) : HistoryUiAction
    data object OnRefreshClick : HistoryUiAction
}

sealed interface HistoryUiEffect : UiEffect {
    data object NavigateToActiveWorkout : HistoryUiEffect
    data class ShowToast(val message: String) : HistoryUiEffect
}
