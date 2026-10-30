package dev.neverpaid.health.features.exercises

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.SaveExerciseRequestDto
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first

class ExercisesViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ExercisesUiAction, ExercisesUiState, ExercisesUiEffect>(
    initialState = ExercisesUiState(isLoading = true)
) {

    init {
        refreshFromApi()
        loadExercises()
    }

    override fun onAction(action: ExercisesUiAction) {
        when (action) {
            is ExercisesUiAction.OnSelectTab -> {
                updateState { copy(selectedTab = action.tab) }
                loadExercises()
            }
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
            is ExercisesUiAction.OnOpenEditor -> {
                updateState { copy(isEditorOpen = true, editingExercise = action.exercise) }
            }
            ExercisesUiAction.OnCloseEditor -> {
                updateState { copy(isEditorOpen = false, editingExercise = null) }
            }
            is ExercisesUiAction.OnSaveCustomExercise -> saveCustomExercise(action.request, action.editingId)
            is ExercisesUiAction.OnDeleteCustomExercise -> deleteCustomExercise(action.exerciseId)
            is ExercisesUiAction.OnStartWorkoutWithExercise -> startWorkoutWithExercise(action.exercise)
        }
    }

    private fun refreshFromApi() {
        launchGuarded {
            repository.refreshExercises()
        }
    }

    private fun loadExercises() {
        launchGuarded {
            repository.searchExercises(
                query = currentState.searchQuery,
                muscle = currentState.selectedMuscle,
                equipment = currentState.selectedEquipment
            ).collectLatest { filtered ->
                val tabFiltered = if (currentState.selectedTab == "custom") {
                    filtered.filter { it.isCustom }
                } else {
                    filtered
                }
                updateState {
                    copy(
                        exercises = tabFiltered,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun saveCustomExercise(request: SaveExerciseRequestDto, editingId: String?) {
        launchGuarded(
            onError = { err ->
                sendEffect(ExercisesUiEffect.ShowToast(err.message ?: "Ошибка сохранения упражнения"))
            }
        ) {
            if (editingId != null) {
                repository.updateCustomExercise(
                    exerciseId = editingId,
                    name = request.name,
                    primaryMuscleGroup = request.primaryMuscleGroup,
                    equipment = request.equipment,
                    measurementType = request.measurementType
                )
            } else {
                repository.createCustomExercise(
                    name = request.name,
                    primaryMuscleGroup = request.primaryMuscleGroup,
                    equipment = request.equipment,
                    measurementType = request.measurementType
                )
            }
            updateState { copy(isEditorOpen = false, editingExercise = null) }
            loadExercises()
            sendEffect(ExercisesUiEffect.ShowToast("Упражнение сохранено"))
        }
    }

    private fun deleteCustomExercise(exerciseId: String) {
        launchGuarded(
            onError = { err ->
                sendEffect(ExercisesUiEffect.ShowToast(err.message ?: "Ошибка удаления"))
            }
        ) {
            repository.deleteCustomExercise(exerciseId)
            loadExercises()
            sendEffect(ExercisesUiEffect.ShowToast("Упражнение удалено"))
        }
    }

    private fun startWorkoutWithExercise(exercise: Exercise) {
        launchGuarded {
            val active = repository.getActiveWorkout().first()
            val newEx = WorkoutExercise(
                instanceId = "we-${exercise.id}",
                exerciseId = exercise.id,
                exerciseName = exercise.name,
                measurementType = exercise.measurementType,
                primaryMuscleGroup = exercise.effectiveMuscle,
                equipment = exercise.equipment,
                sets = listOf(
                    WorkoutSet(id = "set-1", setNumber = 1, weightKg = 0.0, reps = 10)
                )
            )
            if (active != null) {
                repository.updateActiveWorkout(active.copy(exercises = active.exercises + newEx))
            } else {
                repository.startWorkoutWithExercises(
                    title = "Тренировка: ${exercise.name}",
                    exercises = listOf(newEx)
                )
            }
            sendEffect(ExercisesUiEffect.NavigateToActiveWorkout)
        }
    }
}
