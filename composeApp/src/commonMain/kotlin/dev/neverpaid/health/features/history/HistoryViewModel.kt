package dev.neverpaid.health.features.history

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class HistoryViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<HistoryUiAction, HistoryUiState, HistoryUiEffect>(
    initialState = HistoryUiState(isLoading = true)
) {

    init {
        refreshHistory()
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
            is HistoryUiAction.OnRepeatWorkout -> repeatWorkout(action.workout)
            is HistoryUiAction.OnDeleteWorkout -> deleteWorkout(action.workoutId)
            HistoryUiAction.OnRefreshClick -> refreshHistory()
        }
    }

    private fun refreshHistory() {
        launchGuarded {
            repository.refreshWorkoutHistory()
        }
    }

    private fun loadHistory() {
        launchGuarded {
            repository.getWorkoutHistory().collectLatest { list ->
                val now = PlatformBridge.currentTimeMillis()
                val dayMs = 86400000L

                val matrix = (0 until 84).map { dayIndex ->
                    val targetDayEpoch = now - ((83 - dayIndex) * dayMs)
                    val dayStart = targetDayEpoch - (targetDayEpoch % dayMs)
                    val dayEnd = dayStart + dayMs
                    val count = list.count { it.startTimeEpochMs in dayStart until dayEnd }
                    when {
                        count >= 2 -> 3
                        count == 1 -> 2
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

    private fun repeatWorkout(workout: Workout) {
        launchGuarded {
            val resetExercises = workout.exercises.map { ex ->
                ex.copy(
                    sets = ex.sets.map { s ->
                        s.copy(
                            isCompleted = false,
                            previousWeightKg = s.weightKg,
                            previousReps = s.reps
                        )
                    }
                )
            }
            repository.startWorkoutWithExercises(
                title = workout.title,
                exercises = resetExercises,
                routineId = workout.routineId
            )
            sendEffect(HistoryUiEffect.NavigateToActiveWorkout)
        }
    }

    private fun deleteWorkout(workoutId: String) {
        launchGuarded(
            onError = { err ->
                sendEffect(HistoryUiEffect.ShowToast(err.message ?: "Ошибка удаления тренировки"))
            }
        ) {
            repository.deleteWorkoutFromHistory(workoutId)
            sendEffect(HistoryUiEffect.ShowToast("Тренировка удалена"))
        }
    }
}
