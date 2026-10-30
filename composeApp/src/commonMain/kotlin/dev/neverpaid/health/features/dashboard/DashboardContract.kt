package dev.neverpaid.health.features.dashboard

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.ActiveProgramBundle
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise

data class DashboardUiState(
    val hasActiveWorkout: Boolean = false,
    val activeWorkoutTitle: String = "",
    val activeWorkoutStartTimeMs: Long = 0,
    val activeWorkoutExercisesCount: Int = 0,
    val activeWorkoutCompletedSets: Int = 0,
    val currentStreakDays: Int = 0,
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val activeProgramBundle: ActiveProgramBundle? = null,
    val selectedProgramDayNumber: Int = 1,
    val customRoutines: List<Routine> = emptyList(),
    val prebuiltRoutines: List<Routine> = emptyList(),
    val allExercises: List<Exercise> = emptyList(),
    val previewRoutine: Routine? = null,
    val isRoutineEditorOpen: Boolean = false,
    val editingRoutineId: String? = null,
    val editorTitle: String = "",
    val editorDescription: String = "",
    val editorExercises: List<RoutineExercise> = emptyList(),
    val isExercisePickerOpen: Boolean = false,
    val inspectedExercise: Exercise? = null,
    val isLoading: Boolean = false
) : UiState

sealed interface DashboardUiAction : UiAction {
    data object OnResumeWorkoutClick : DashboardUiAction
    data object OnStartEmptyWorkoutClick : DashboardUiAction
    data class OnSelectRoutineClick(val routine: Routine) : DashboardUiAction
    data class OnPreviewRoutineClick(val routine: Routine?) : DashboardUiAction
    data class OnSelectProgramDayTab(val dayNumber: Int) : DashboardUiAction
    data class OnStartProgramDayClick(val day: ProgramDay) : DashboardUiAction
    data object OnSkipRestDayClick : DashboardUiAction
    data class OnOpenRoutineEditor(val routine: Routine? = null) : DashboardUiAction
    data object OnCloseRoutineEditor : DashboardUiAction
    data class OnEditorTitleChanged(val title: String) : DashboardUiAction
    data class OnEditorDescriptionChanged(val description: String) : DashboardUiAction
    data object OnOpenExercisePicker : DashboardUiAction
    data object OnCloseExercisePicker : DashboardUiAction
    data class OnAddExerciseToEditor(val exercise: Exercise) : DashboardUiAction
    data class OnRemoveExerciseFromEditor(val index: Int) : DashboardUiAction
    data class OnUpdateEditorExerciseSets(val index: Int, val sets: Int) : DashboardUiAction
    data class OnUpdateEditorExerciseReps(val index: Int, val minReps: Int, val maxReps: Int) : DashboardUiAction
    data object OnSaveRoutineClick : DashboardUiAction
    data class OnDeleteRoutineClick(val routineId: String) : DashboardUiAction
    data class OnInspectExercise(val exercise: Exercise?) : DashboardUiAction
    data class OnInspectExerciseById(val exerciseId: String, val fallbackName: String) : DashboardUiAction
    data object OnRefreshClick : DashboardUiAction
}

sealed interface DashboardUiEffect : UiEffect {
    data object NavigateToActiveWorkout : DashboardUiEffect
    data class ShowSnackbar(val message: String) : DashboardUiEffect
}
