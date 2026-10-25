package dev.neverpaid.health.features.exercises

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class ExercisesViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ExercisesUiAction, ExercisesUiState, ExercisesUiEffect>(
    initialState = ExercisesUiState(isLoading = true)
) {

    init {
        loadExercises()
    }

    override fun onAction(action: ExercisesUiAction) {
        when (action) {
            is ExercisesUiAction.OnSearchQueryChanged -> {
                updateState { copy(searchQuery = action.query) }
                loadExercises()
            }
            is ExercisesUiAction.OnSelectMuscle -> {
                updateState { copy(selectedMuscle = action.muscle) }
                loadExercises()
            }
            is ExercisesUiAction.OnSelectEquipment -> {
                updateState { copy(selectedEquipment = action.equipment) }
                loadExercises()
            }
            is ExercisesUiAction.OnSelectExercise -> {
                updateState { copy(selectedExerciseDetail = action.exercise) }
            }
            ExercisesUiAction.OnDismissDetail -> {
                updateState { copy(selectedExerciseDetail = null) }
            }
        }
    }

    private fun loadExercises() {
        launchGuarded {
            repository.searchExercises(
                query = currentState.searchQuery,
                muscle = currentState.selectedMuscle,
                equipment = currentState.selectedEquipment
            ).collectLatest { filtered ->
                updateState {
                    copy(
                        exercises = filtered,
                        isLoading = false
                    )
                }
            }
        }
    }
}
