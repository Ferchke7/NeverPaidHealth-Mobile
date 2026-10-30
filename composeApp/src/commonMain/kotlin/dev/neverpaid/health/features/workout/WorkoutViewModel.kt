package dev.neverpaid.health.features.workout

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class WorkoutViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<WorkoutUiAction, WorkoutUiState, WorkoutUiEffect>(
    initialState = WorkoutUiState()
) {

    private var restTimerJob: Job? = null
    private var defaultRestSeconds = 90

    init {
        observeWorkoutAndCatalog()
    }

    override fun onAction(action: WorkoutUiAction) {
        when (action) {
            is WorkoutUiAction.OnUpdateWorkoutTitle -> updateWorkoutTitle(action.title)
            is WorkoutUiAction.OnUpdateWeight -> updateWeight(action.exerciseId, action.setId, action.weight)
            is WorkoutUiAction.OnUpdateReps -> updateReps(action.exerciseId, action.setId, action.reps)
            is WorkoutUiAction.OnApplyPreviousSet -> applyPreviousSet(action.exerciseId, action.setId)
            is WorkoutUiAction.OnToggleSetComplete -> toggleSetComplete(action.exerciseId, action.setId)
            is WorkoutUiAction.OnToggleSetType -> cycleSetType(action.exerciseId, action.setId)
            is WorkoutUiAction.OnAddSet -> addSetToExercise(action.exerciseId)
            is WorkoutUiAction.OnDeleteSet -> deleteSet(action.exerciseId, action.setId)
            is WorkoutUiAction.OnUpdateExerciseNotes -> updateExerciseNotes(action.exerciseId, action.notes)
            WorkoutUiAction.OnOpenExercisePicker -> {
                updateState { copy(showExercisePicker = true, swappingExerciseId = null) }
            }
            is WorkoutUiAction.OnOpenSwapExercisePicker -> {
                updateState { copy(showExercisePicker = true, swappingExerciseId = action.exerciseIdToSwap) }
            }
            WorkoutUiAction.OnCloseExercisePicker -> {
                updateState { copy(showExercisePicker = false, swappingExerciseId = null) }
            }
            is WorkoutUiAction.OnSelectExerciseToAdd -> addOrSwapExercise(action.exercise)
            is WorkoutUiAction.OnInspectExercise -> {
                updateState { copy(inspectedExercise = action.exercise) }
            }
            is WorkoutUiAction.OnRemoveExercise -> removeExerciseFromWorkout(action.exerciseId)
            WorkoutUiAction.OnFinishWorkoutClick -> finishWorkout()
            WorkoutUiAction.OnDiscardWorkoutClick -> discardWorkout()
            is WorkoutUiAction.OnRestTimerAddSeconds -> addSecondsToRestTimer(action.seconds)
            is WorkoutUiAction.OnStartManualRestTimer -> startRestTimer(action.seconds)
            WorkoutUiAction.OnRestTimerSkip -> stopRestTimer()
        }
    }

    private fun observeWorkoutAndCatalog() {
        launchGuarded {
            combine(
                repository.getActiveWorkout(),
                repository.getExercises(),
                repository.getUserPreferences()
            ) { active, exercises, prefs ->
                Triple(active, exercises, prefs)
            }.collect { (active, exercises, prefs) ->
                defaultRestSeconds = prefs.defaultRestSeconds
                updateState {
                    copy(
                        workout = active,
                        workoutStartTimeEpochMs = active?.startTimeEpochMs ?: PlatformBridge.currentTimeMillis(),
                        availableExercises = exercises,
                        isKgUnit = prefs.useKilograms
                    )
                }
            }
        }
    }

    private fun updateWorkoutTitle(title: String) {
        val currentWorkout = currentState.workout ?: return
        val updated = currentWorkout.copy(name = title)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun updateWeight(exerciseId: String, setId: String, weight: Double) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { s ->
                    if (s.id == setId) s.copy(weightKg = weight) else s
                })
            } else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun updateReps(exerciseId: String, setId: String, reps: Int) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { s ->
                    if (s.id == setId) s.copy(reps = reps.coerceAtLeast(0)) else s
                })
            } else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun applyPreviousSet(exerciseId: String, setId: String) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { s ->
                    if (s.id == setId && s.previousWeightKg != null && s.previousReps != null) {
                        s.copy(weightKg = s.previousWeightKg, reps = s.previousReps)
                    } else s
                })
            } else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
        sendEffect(WorkoutUiEffect.TriggerHaptic)
    }

    private fun updateExerciseNotes(exerciseId: String, notes: String) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) ex.copy(notes = notes) else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun toggleSetComplete(exerciseId: String, setId: String) {
        val currentWorkout = currentState.workout ?: return
        var becomesCompleted = false

        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { s ->
                    if (s.id == setId) {
                        val newCompleted = !s.isCompleted
                        becomesCompleted = newCompleted
                        s.copy(isCompleted = newCompleted)
                    } else s
                })
            } else ex
        }

        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)

        if (becomesCompleted) {
            sendEffect(WorkoutUiEffect.TriggerHaptic)
            startRestTimer(defaultRestSeconds)
        }
    }

    private fun cycleSetType(exerciseId: String, setId: String) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                ex.copy(sets = ex.sets.map { s ->
                    if (s.id == setId) {
                        val nextType = when (s.type) {
                            "normal" -> "warmup"
                            "warmup" -> "drop"
                            "drop" -> "failure"
                            else -> "normal"
                        }
                        s.copy(setType = nextType)
                    } else s
                })
            } else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun addSetToExercise(exerciseId: String) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val lastSet = ex.sets.lastOrNull()
                val nextSetNumber = ex.sets.size + 1
                val newSet = WorkoutSet(
                    id = PlatformBridge.generateUuid(),
                    setNumber = nextSetNumber,
                    weightKg = lastSet?.weightKg ?: 40.0,
                    reps = lastSet?.reps ?: 10,
                    setType = "normal",
                    isCompleted = false,
                    previousWeightKg = lastSet?.previousWeightKg ?: lastSet?.weightKg,
                    previousReps = lastSet?.previousReps ?: lastSet?.reps
                )
                ex.copy(sets = ex.sets + newSet)
            } else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun deleteSet(exerciseId: String, setId: String) {
        val currentWorkout = currentState.workout ?: return
        val updatedExercises = currentWorkout.exercises.map { ex ->
            if (ex.exerciseId == exerciseId) {
                val filtered = ex.sets.filter { it.id != setId }
                val renumbered = filtered.mapIndexed { index, s -> s.copy(setNumber = index + 1) }
                ex.copy(sets = renumbered)
            } else ex
        }
        val updated = currentWorkout.copy(exercises = updatedExercises)
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun addOrSwapExercise(exercise: Exercise) {
        val currentWorkout = currentState.workout ?: return
        val swappingId = currentState.swappingExerciseId

        val newExercise = WorkoutExercise(
            instanceId = PlatformBridge.generateUuid(),
            exerciseId = exercise.id,
            exerciseName = exercise.name,
            measurementType = exercise.measurementType,
            primaryMuscleGroup = exercise.effectiveMuscle,
            equipment = exercise.equipment,
            orderIndex = currentWorkout.exercises.size + 1,
            sets = listOf(
                WorkoutSet(
                    id = PlatformBridge.generateUuid(),
                    setNumber = 1,
                    weightKg = 40.0,
                    reps = 10,
                    setType = "normal",
                    isCompleted = false
                ),
                WorkoutSet(
                    id = PlatformBridge.generateUuid(),
                    setNumber = 2,
                    weightKg = 40.0,
                    reps = 10,
                    setType = "normal",
                    isCompleted = false
                ),
                WorkoutSet(
                    id = PlatformBridge.generateUuid(),
                    setNumber = 3,
                    weightKg = 40.0,
                    reps = 10,
                    setType = "normal",
                    isCompleted = false
                )
            )
        )

        val updatedList = if (swappingId != null) {
            currentWorkout.exercises.map { existing ->
                if (existing.exerciseId == swappingId) {
                    newExercise.copy(orderIndex = existing.orderIndex)
                } else existing
            }
        } else {
            currentWorkout.exercises + newExercise
        }

        val updated = currentWorkout.copy(exercises = updatedList)
        updateState { copy(workout = updated, showExercisePicker = false, swappingExerciseId = null) }
        repository.updateActiveWorkout(updated)

        // Fetch previous performance for this exercise from backend
        launchGuarded {
            val prevMap = repository.getPreviousPerformance(listOf(exercise.id))
            val prevSets = prevMap[exercise.id].orEmpty()
            if (prevSets.isNotEmpty()) {
                val latestWorkout = currentState.workout ?: return@launchGuarded
                val enriched = latestWorkout.copy(
                    exercises = latestWorkout.exercises.map { ex ->
                        if (ex.exerciseId == exercise.id) {
                            ex.copy(
                                sets = ex.sets.mapIndexed { idx, s ->
                                    val prev = prevSets.find { it.setNumber == idx + 1 } ?: prevSets.lastOrNull()
                                    if (prev != null) {
                                        s.copy(
                                            weightKg = prev.weightKg,
                                            reps = prev.reps,
                                            previousWeightKg = prev.weightKg,
                                            previousReps = prev.reps
                                        )
                                    } else s
                                }
                            )
                        } else ex
                    }
                )
                updateState { copy(workout = enriched) }
                repository.updateActiveWorkout(enriched)
            }
        }
    }

    private fun removeExerciseFromWorkout(exerciseId: String) {
        val currentWorkout = currentState.workout ?: return
        val updated = currentWorkout.copy(exercises = currentWorkout.exercises.filter { it.exerciseId != exerciseId })
        updateState { copy(workout = updated) }
        repository.updateActiveWorkout(updated)
    }

    private fun finishWorkout() {
        val finished = repository.finishActiveWorkout()
        stopRestTimer()
        if (finished != null) {
            sendEffect(WorkoutUiEffect.NavigateBack)
        } else {
            sendEffect(WorkoutUiEffect.NavigateBack)
        }
    }

    private fun discardWorkout() {
        repository.discardActiveWorkout()
        stopRestTimer()
        sendEffect(WorkoutUiEffect.NavigateBack)
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        updateState {
            copy(
                restTimerSecondsRemaining = seconds,
                isRestTimerActive = true
            )
        }
        restTimerJob = viewModelScope.launch {
            while (isActive && currentState.restTimerSecondsRemaining > 0) {
                delay(1000)
                updateState {
                    val remaining = restTimerSecondsRemaining - 1
                    copy(
                        restTimerSecondsRemaining = remaining,
                        isRestTimerActive = remaining > 0
                    )
                }
            }
            if (currentState.restTimerSecondsRemaining <= 0) {
                sendEffect(WorkoutUiEffect.TriggerHaptic)
                sendEffect(WorkoutUiEffect.ShowToast("Отдых завершён! Пора на следующий подход."))
            }
        }
    }

    private fun addSecondsToRestTimer(seconds: Int) {
        val updated = (currentState.restTimerSecondsRemaining + seconds).coerceAtLeast(0)
        updateState {
            copy(
                restTimerSecondsRemaining = updated,
                isRestTimerActive = updated > 0
            )
        }
    }

    private fun stopRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        updateState {
            copy(
                restTimerSecondsRemaining = 0,
                isRestTimerActive = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        restTimerJob?.cancel()
    }
}
