package dev.neverpaid.health.features.dashboard

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class DashboardViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<DashboardUiAction, DashboardUiState, DashboardUiEffect>(
    initialState = DashboardUiState(
        prebuiltRoutines = repository.getPrebuiltRoutines(),
        isLoading = true
    )
) {

    init {
        observeDashboardStreams()
        refreshRemoteData()
    }

    override fun onAction(action: DashboardUiAction) {
        when (action) {
            DashboardUiAction.OnResumeWorkoutClick -> {
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            DashboardUiAction.OnStartEmptyWorkoutClick -> {
                repository.startWorkout("Свободная тренировка")
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            is DashboardUiAction.OnSelectRoutineClick -> {
                updateState { copy(previewRoutine = null) }
                repository.startWorkout(action.routine.title, action.routine)
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            is DashboardUiAction.OnPreviewRoutineClick -> {
                updateState { copy(previewRoutine = action.routine) }
            }
            is DashboardUiAction.OnSelectProgramDayTab -> {
                updateState { copy(selectedProgramDayNumber = action.dayNumber) }
            }
            is DashboardUiAction.OnStartProgramDayClick -> {
                val program = currentState.activeProgramBundle?.program ?: return
                repository.startWorkoutFromProgramDay(program, action.day)
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            DashboardUiAction.OnSkipRestDayClick -> {
                launchGuarded {
                    val updated = repository.advanceActiveProgramDay()
                    val nextDay = updated?.userProgram?.currentDayNumber ?: 1
                    updateState {
                        copy(
                            activeProgramBundle = updated,
                            selectedProgramDayNumber = nextDay
                        )
                    }
                    sendEffect(DashboardUiEffect.ShowSnackbar("День отдыха пропущен! Переход к дню $nextDay"))
                }
            }
            is DashboardUiAction.OnOpenRoutineEditor -> {
                val r = action.routine
                updateState {
                    copy(
                        previewRoutine = null,
                        isRoutineEditorOpen = true,
                        editingRoutineId = r?.id?.takeIf { !r.id.startsWith("prebuilt-") },
                        editorTitle = r?.title ?: "",
                        editorDescription = r?.description ?: "",
                        editorExercises = r?.exercises ?: emptyList()
                    )
                }
            }
            DashboardUiAction.OnCloseRoutineEditor -> {
                updateState { copy(isRoutineEditorOpen = false) }
            }
            is DashboardUiAction.OnEditorTitleChanged -> {
                updateState { copy(editorTitle = action.title) }
            }
            is DashboardUiAction.OnEditorDescriptionChanged -> {
                updateState { copy(editorDescription = action.description) }
            }
            DashboardUiAction.OnOpenExercisePicker -> {
                updateState { copy(isExercisePickerOpen = true) }
            }
            DashboardUiAction.OnCloseExercisePicker -> {
                updateState { copy(isExercisePickerOpen = false) }
            }
            is DashboardUiAction.OnAddExerciseToEditor -> {
                val nextIndex = currentState.editorExercises.size + 1
                val item = RoutineExercise(
                    exerciseId = action.exercise.id,
                    exerciseName = action.exercise.name,
                    orderIndex = nextIndex,
                    targetSets = 3,
                    targetRepsMin = 8,
                    targetRepsMax = 12
                )
                updateState {
                    copy(
                        editorExercises = editorExercises + item,
                        isExercisePickerOpen = false
                    )
                }
            }
            is DashboardUiAction.OnRemoveExerciseFromEditor -> {
                val updated = currentState.editorExercises
                    .filterIndexed { idx, _ -> idx != action.index }
                    .mapIndexed { idx, ex -> ex.copy(orderIndex = idx + 1) }
                updateState { copy(editorExercises = updated) }
            }
            is DashboardUiAction.OnUpdateEditorExerciseSets -> {
                val updated = currentState.editorExercises.mapIndexed { idx, ex ->
                    if (idx == action.index) ex.copy(targetSets = action.sets.coerceIn(1, 15)) else ex
                }
                updateState { copy(editorExercises = updated) }
            }
            is DashboardUiAction.OnUpdateEditorExerciseReps -> {
                val updated = currentState.editorExercises.mapIndexed { idx, ex ->
                    if (idx == action.index) {
                        ex.copy(
                            targetRepsMin = action.minReps.coerceIn(1, 100),
                            targetRepsMax = action.maxReps.coerceIn(1, 100)
                        )
                    } else ex
                }
                updateState { copy(editorExercises = updated) }
            }
            DashboardUiAction.OnSaveRoutineClick -> {
                saveRoutine()
            }
            is DashboardUiAction.OnDeleteRoutineClick -> {
                launchGuarded {
                    repository.deleteRoutine(action.routineId)
                    sendEffect(DashboardUiEffect.ShowSnackbar("Рутина удалена"))
                }
            }
            is DashboardUiAction.OnInspectExercise -> {
                updateState { copy(inspectedExercise = action.exercise) }
            }
            is DashboardUiAction.OnInspectExerciseById -> {
                val found = currentState.allExercises.find {
                    it.id == action.exerciseId || it.name.equals(action.fallbackName, ignoreCase = true)
                } ?: Exercise(
                    id = action.exerciseId,
                    name = action.fallbackName,
                    primaryMuscleGroup = "full_body",
                    equipment = "barbell"
                )
                updateState { copy(inspectedExercise = found) }
            }
            DashboardUiAction.OnRefreshClick -> {
                refreshRemoteData()
            }
        }
    }

    private fun observeDashboardStreams() {
        combine(
            repository.getActiveWorkout(),
            repository.observeRoutines(),
            repository.observeActiveProgram(),
            repository.getProgressSummary(),
            repository.getExercises()
        ) { active, routines, activeProgram, progress, exercises ->
            val currentDay = activeProgram?.userProgram?.currentDayNumber ?: 1
            val selectedDay = if (currentState.activeProgramBundle?.program?.id != activeProgram?.program?.id) {
                currentDay
            } else {
                currentState.selectedProgramDayNumber
            }
            updateState {
                copy(
                    hasActiveWorkout = active != null,
                    activeWorkoutTitle = active?.title ?: "",
                    activeWorkoutStartTimeMs = active?.startTimeEpochMs ?: 0L,
                    activeWorkoutExercisesCount = active?.exercises?.size ?: 0,
                    activeWorkoutCompletedSets = active?.exercises?.sumOf { ex -> ex.sets.count { it.isCompleted } } ?: 0,
                    currentStreakDays = progress.currentStreakDays,
                    totalWorkouts = progress.totalWorkouts,
                    totalVolumeKg = progress.totalVolumeKg,
                    customRoutines = routines,
                    activeProgramBundle = activeProgram,
                    selectedProgramDayNumber = selectedDay,
                    allExercises = exercises,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshRemoteData() {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.getRoutines()
            repository.getActiveProgram()
            repository.refreshExercises()
            repository.refreshWorkouts()
            updateState { copy(isLoading = false) }
        }
    }

    private fun saveRoutine() {
        val title = currentState.editorTitle.trim()
        if (title.isEmpty()) {
            sendEffect(DashboardUiEffect.ShowSnackbar("Введите название сплита"))
            return
        }
        if (currentState.editorExercises.isEmpty()) {
            sendEffect(DashboardUiEffect.ShowSnackbar("Добавьте хотя бы одно упражнение"))
            return
        }
        val req = SaveRoutineRequestDto(
            name = title,
            notes = currentState.editorDescription.trim(),
            exercises = currentState.editorExercises
        )
        val editingId = currentState.editingRoutineId
        launchGuarded(
            onError = { err ->
                sendEffect(DashboardUiEffect.ShowSnackbar(err.message ?: "Ошибка сохранения"))
            }
        ) {
            if (editingId != null) {
                repository.updateRoutine(editingId, req)
                sendEffect(DashboardUiEffect.ShowSnackbar("Рутина обновлена"))
            } else {
                repository.createRoutine(req)
                sendEffect(DashboardUiEffect.ShowSnackbar("Новая рутина сохранена"))
            }
            updateState { copy(isRoutineEditorOpen = false) }
        }
    }
}
