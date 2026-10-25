package dev.neverpaid.health.features.dashboard

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class DashboardViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<DashboardUiAction, DashboardUiState, DashboardUiEffect>(
    initialState = DashboardUiState(isLoading = true)
) {

    init {
        loadDashboardData()
    }

    override fun onAction(action: DashboardUiAction) {
        when (action) {
            DashboardUiAction.OnResumeWorkoutClick -> {
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            DashboardUiAction.OnStartEmptyWorkoutClick -> {
                repository.startWorkout("Quick Workout")
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            is DashboardUiAction.OnSelectRoutineClick -> {
                repository.startWorkout(action.routine.title, action.routine)
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            DashboardUiAction.OnRefreshClick -> {
                loadDashboardData()
            }
        }
    }

    private fun loadDashboardData() {
        launchGuarded {
            combine(
                repository.getActiveWorkout(),
                repository.getCoachInsight(),
                repository.getProgressSummary()
            ) { active, coach, progress ->
                Triple(active, coach, progress)
            }.collect { (active, coach, progress) ->
                updateState {
                    copy(
                        cnsScore = coach.cnsScore,
                        readinessLevel = coach.readinessLevel,
                        statusMessage = coach.statusMessage,
                        hasActiveWorkout = active != null,
                        activeWorkoutTitle = active?.title ?: "",
                        activeWorkoutStartTimeMs = active?.startTimeEpochMs ?: 0,
                        currentStreakDays = progress.currentStreakDays,
                        totalWorkouts = progress.totalWorkouts,
                        totalVolumeKg = progress.totalVolumeKg,
                        recentRoutines = repository.getRoutines(),
                        isLoading = false
                    )
                }
            }
        }
    }
}
