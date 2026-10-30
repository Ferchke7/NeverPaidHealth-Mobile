package dev.neverpaid.health.features.exercises

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.SaveExerciseRequestDto

data class ExercisesUiState(
    val selectedTab: String = "all", // "all" | "custom"
    val searchQuery: String = "",
    val selectedMuscle: String = "All",
    val selectedEquipment: String = "All",
    val exercises: List<Exercise> = emptyList(),
    val selectedExerciseDetail: Exercise? = null,
    val isEditorOpen: Boolean = false,
    val editingExercise: Exercise? = null,
    val isLoading: Boolean = false
) : UiState

sealed interface ExercisesUiAction : UiAction {
    data class OnSelectTab(val tab: String) : ExercisesUiAction
    data class OnSearchQueryChanged(val query: String) : ExercisesUiAction
    data class OnSelectMuscle(val muscle: String) : ExercisesUiAction
    data class OnSelectEquipment(val equipment: String) : ExercisesUiAction
    data class OnSelectExercise(val exercise: Exercise) : ExercisesUiAction
    data object OnDismissDetail : ExercisesUiAction
    data class OnOpenEditor(val exercise: Exercise? = null) : ExercisesUiAction
    data object OnCloseEditor : ExercisesUiAction
    data class OnSaveCustomExercise(val request: SaveExerciseRequestDto, val editingId: String? = null) : ExercisesUiAction
    data class OnDeleteCustomExercise(val exerciseId: String) : ExercisesUiAction
    data class OnStartWorkoutWithExercise(val exercise: Exercise) : ExercisesUiAction
}

sealed interface ExercisesUiEffect : UiEffect {
    data object NavigateToActiveWorkout : ExercisesUiEffect
    data class ShowToast(val message: String) : ExercisesUiEffect
}
