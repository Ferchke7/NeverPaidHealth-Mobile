package dev.neverpaid.health.features.dashboard

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine

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
                repository.getProgressSummary(),
                repository.getPrograms()
            ) { active, coach, progress, programs ->
                val activeProgram = programs.find { it.isActive } ?: programs.firstOrNull()
                val activeDay = activeProgram?.days?.find { !it.isRestDay } ?: activeProgram?.days?.firstOrNull()

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
                        activeProgramTitle = activeDay?.title ?: "Push Day (Hypertrophy)",
                        activeProgramExercisesCount = activeDay?.exercises?.size ?: 4,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }
}
