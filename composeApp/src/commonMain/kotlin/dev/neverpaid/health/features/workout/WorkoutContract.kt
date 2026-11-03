package dev.neverpaid.health.features.workout

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Workout

data class WorkoutUiState(
    val workout: Workout? = null,
    val restTimerSecondsRemaining: Int = 0,
    val isRestTimerActive: Boolean = false,
    val workoutStartTimeEpochMs: Long = 0,
    val isFinishing: Boolean = false,
    val showExercisePicker: Boolean = false,
    val swappingExerciseId: String? = null,
    val inspectedExercise: Exercise? = null,
    val availableExercises: List<Exercise> = emptyList(),
    val isKgUnit: Boolean = true,
    val workoutHistory: List<Workout> = emptyList(),
    val historySheetExerciseId: String? = null,
    val historySheetExerciseName: String? = null,
    val historySheetSetNumber: Int? = null,
    val historySheetActiveSetId: String? = null
) : UiState

sealed interface WorkoutUiAction : UiAction {
    data class OnUpdateWorkoutTitle(val title: String) : WorkoutUiAction
    data class OnUpdateWeight(val exerciseId: String, val setId: String, val weight: Double) : WorkoutUiAction
    data class OnUpdateReps(val exerciseId: String, val setId: String, val reps: Int) : WorkoutUiAction
    data class OnApplyPreviousSet(val exerciseId: String, val setId: String) : WorkoutUiAction
    data class OnApplySetValues(val exerciseId: String, val setId: String, val weightKg: Double, val reps: Int) : WorkoutUiAction
    data class OnOpenSetHistorySheet(val exerciseId: String, val exerciseName: String, val setNumber: Int, val setId: String) : WorkoutUiAction
    data object OnCloseSetHistorySheet : WorkoutUiAction
    data class OnToggleSetComplete(val exerciseId: String, val setId: String) : WorkoutUiAction
    data class OnToggleSetType(val exerciseId: String, val setId: String) : WorkoutUiAction
    data class OnAddSet(val exerciseId: String) : WorkoutUiAction
    data class OnDeleteSet(val exerciseId: String, val setId: String) : WorkoutUiAction
    data class OnUpdateExerciseNotes(val exerciseId: String, val notes: String) : WorkoutUiAction
    data object OnOpenExercisePicker : WorkoutUiAction
    data class OnOpenSwapExercisePicker(val exerciseIdToSwap: String) : WorkoutUiAction
    data object OnCloseExercisePicker : WorkoutUiAction
    data class OnSelectExerciseToAdd(val exercise: Exercise) : WorkoutUiAction
    data class OnInspectExercise(val exercise: Exercise?) : WorkoutUiAction
    data class OnRemoveExercise(val exerciseId: String) : WorkoutUiAction
    data object OnFinishWorkoutClick : WorkoutUiAction
    data object OnDiscardWorkoutClick : WorkoutUiAction
    data class OnRestTimerAddSeconds(val seconds: Int) : WorkoutUiAction
    data class OnStartManualRestTimer(val seconds: Int) : WorkoutUiAction
    data object OnRestTimerSkip : WorkoutUiAction
}

sealed interface WorkoutUiEffect : UiEffect {
    data class WorkoutFinishedSummary(val workout: Workout) : WorkoutUiEffect
    data object NavigateBack : WorkoutUiEffect
    data object TriggerHaptic : WorkoutUiEffect
    data class ShowToast(val message: String) : WorkoutUiEffect
}
