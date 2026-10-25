package dev.neverpaid.health.features.exercises

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.Exercise

data class ExercisesUiState(
    val searchQuery: String = "",
    val selectedMuscle: String = "All",
    val selectedEquipment: String = "All",
    val exercises: List<Exercise> = emptyList(),
    val selectedExerciseDetail: Exercise? = null,
    val isLoading: Boolean = false
) : UiState

sealed interface ExercisesUiAction : UiAction {
    data class OnSearchQueryChanged(val query: String) : ExercisesUiAction
    data class OnSelectMuscle(val muscle: String) : ExercisesUiAction
    data class OnSelectEquipment(val equipment: String) : ExercisesUiAction
    data class OnSelectExercise(val exercise: Exercise) : ExercisesUiAction
    data object OnDismissDetail : ExercisesUiAction
}

sealed interface ExercisesUiEffect : UiEffect
