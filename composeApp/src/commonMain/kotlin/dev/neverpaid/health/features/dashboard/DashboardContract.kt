package dev.neverpaid.health.features.dashboard

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.Routine

data class DashboardUiState(
    val cnsScore: Int = 88,
    val readinessLevel: String = "Full Throttle",
    val statusMessage: String = "CNS recovery is optimal. High power output expected today.",
    val hasActiveWorkout: Boolean = false,
    val activeWorkoutTitle: String = "",
    val activeWorkoutStartTimeMs: Long = 0,
    val currentStreakDays: Int = 4,
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val recentRoutines: List<Routine> = emptyList(),
    val isLoading: Boolean = false
) : UiState

sealed interface DashboardUiAction : UiAction {
    data object OnResumeWorkoutClick : DashboardUiAction
    data object OnStartEmptyWorkoutClick : DashboardUiAction
    data class OnSelectRoutineClick(val routine: Routine) : DashboardUiAction
    data object OnRefreshClick : DashboardUiAction
}

sealed interface DashboardUiEffect : UiEffect {
    data object NavigateToActiveWorkout : DashboardUiEffect
    data class ShowSnackbar(val message: String) : DashboardUiEffect
}
