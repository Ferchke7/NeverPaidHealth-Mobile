package dev.neverpaid.health.features.workout

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.i18n.getDudaStrings
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.platform.TimerNotificationIds
import dev.neverpaid.health.core.storage.DudaLocalStorage
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.PersistedRestTimerState
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil

class WorkoutViewModel(
    private val repository: NeverPaidRepository,
    private val nowProvider: () -> Long = { PlatformBridge.currentTimeMillis() }
) : BaseViewModel<WorkoutUiAction, WorkoutUiState, WorkoutUiEffect>(
    initialState = WorkoutUiState()
) {

    private var restTimerJob: Job? = null
    private var restTimerEndEpochMs: Long = 0L
    private var restTimerTotalSeconds: Int = 90
    private var restTimerVirtualRemaining: Int = 0
    private var lastForegroundRestAlertEndMs: Long = 0L
    private var restoredRestWorkoutId: String? = null
    private var defaultRestSeconds = 90

    init {
        observeWorkoutAndCatalog()
    }

    override fun onAction(action: WorkoutUiAction) {
        when (action) {
            is WorkoutUiAction.OnUpdateWorkoutTitle -> updateWorkoutTitle(action.title)
            is WorkoutUiAction.OnUpdateWorkoutNotes -> updateWorkoutNotes(action.notes)
            is WorkoutUiAction.OnUpdateWeight -> updateWeight(action.exerciseId, action.setId, action.weight)
            is WorkoutUiAction.OnUpdateReps -> updateReps(action.exerciseId, action.setId, action.reps)
            is WorkoutUiAction.OnApplyPreviousSet -> applyPreviousSet(action.exerciseId, action.setId)
            is WorkoutUiAction.OnApplySetValues -> {
                updateWeight(action.exerciseId, action.setId, action.weightKg)
                updateReps(action.exerciseId, action.setId, action.reps)
                sendEffect(WorkoutUiEffect.TriggerHaptic)
            }
            is WorkoutUiAction.OnOpenSetHistorySheet -> {
                updateState {
                    copy(
                        historySheetExerciseId = action.exerciseId,
                        historySheetExerciseName = action.exerciseName,
                        historySheetSetNumber = action.setNumber,
                        historySheetActiveSetId = action.setId
                    )
                }
            }
            WorkoutUiAction.OnCloseSetHistorySheet -> {
                updateState {
                    copy(
                        historySheetExerciseId = null,
                        historySheetExerciseName = null,
                        historySheetSetNumber = null,
                        historySheetActiveSetId = null
                    )
                }
            }
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
            WorkoutUiAction.OnSyncRestTimer -> syncRestTimerFromClock()
        }
    }

    private fun observeWorkoutAndCatalog() {
        launchGuarded(onError = { err -> sendError(err) }) {
            repository.getPreviousNotesMap().collect { notesMap ->
                updateState { copy(previousNotesMap = notesMap) }
            }
        }
        launchGuarded(onError = { err -> sendError(err) }) {
            combine(
                repository.getActiveWorkout(),
                repository.getExercises(),
                repository.getUserPreferences(),
                repository.getWorkoutHistory()
            ) { active, exercises, prefs, history ->
                WorkoutInitData(active, exercises, prefs, history)
            }.collect { (active, exercises, prefs, history) ->
                defaultRestSeconds = prefs.defaultRestSeconds
                updateState {
                    copy(
                        workout = active,
                        workoutStartTimeEpochMs = active?.startTimeEpochMs ?: nowProvider(),
                        availableExercises = exercises,
                        isKgUnit = prefs.useKilograms,
                        workoutHistory = history
                    )
                }
                if (active != null && restoredRestWorkoutId != active.id) {
                    restoredRestWorkoutId = active.id
                    restoreRestTimerIfNeeded(active.id)
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

    private fun updateWorkoutNotes(notes: String) {
        val currentWorkout = currentState.workout ?: return
        val updated = currentWorkout.copy(notes = notes)
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
                    weightKg = 0.0,
                    reps = 10,
                    setType = "normal",
                    isCompleted = false
                )
            )
        )

        val updatedList = if (swappingId != null) {
            currentWorkout.exercises.map { existing ->
                if (existing.exerciseId == swappingId) {
                    newExercise.copy(
                        orderIndex = existing.orderIndex,
                        sets = existing.sets.mapIndexed { index, set ->
                            WorkoutSet(
                                id = PlatformBridge.generateUuid(),
                                setNumber = index + 1,
                                setType = set.setType,
                                weightKg = 0.0,
                                reps = 10,
                                isCompleted = false
                            )
                        }.ifEmpty { newExercise.sets }
                    )
                } else existing
            }
        } else {
            currentWorkout.exercises + newExercise
        }

        val updated = currentWorkout.copy(exercises = updatedList)
        updateState { copy(workout = updated, showExercisePicker = false, swappingExerciseId = null) }
        repository.updateActiveWorkout(updated)

        // Fetch previous performance for this exercise from backend
        launchGuarded(onError = { err -> sendError(err) }) {
            val prevMap = repository.getPreviousPerformance(listOf(exercise.id))
            val prevSets = prevMap[exercise.id].orEmpty()
            if (prevSets.isNotEmpty()) {
                val latestWorkout = currentState.workout ?: return@launchGuarded
                if (latestWorkout.id != currentWorkout.id) return@launchGuarded
                val enriched = latestWorkout.copy(
                    exercises = latestWorkout.exercises.map { ex ->
                        if (ex.instanceId == newExercise.instanceId) {
                            ex.copy(
                                sets = ex.sets.mapIndexed { idx, s ->
                                    val prev = prevSets.find { it.setNumber == idx + 1 } ?: prevSets.lastOrNull()
                                    if (prev != null) {
                                        s.copy(
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

    private fun currentStrings() = getDudaStrings(DudaLocalStorage.getUserPreferences().languageCode)

    private fun scheduleRestAlarm(endMs: Long) {
        val strings = currentStrings()
        PlatformBridge.requestNotificationPermission()
        PlatformBridge.scheduleTimerNotification(
            timerId = TimerNotificationIds.REST,
            triggerAtEpochMs = endMs,
            title = strings.restNotificationTitle,
            body = strings.workoutRestUp,
            showOngoingCountdown = true,
            ongoingTitle = strings.restOngoingTitle,
            ongoingBody = currentState.workout?.title.orEmpty()
        )
    }

    private fun restoreRestTimerIfNeeded(workoutId: String) {
        if (restTimerEndEpochMs > 0L) return
        val saved = DudaLocalStorage.getRestTimerState() ?: return
        if (saved.workoutId.isNotBlank() && saved.workoutId != workoutId) return
        val now = nowProvider()
        val remaining = ceil((saved.endEpochMs - now) / 1000.0).toInt().coerceAtLeast(0)
        if (remaining <= 0) {
            DudaLocalStorage.setRestTimerState(null)
            PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
            return
        }
        restTimerEndEpochMs = saved.endEpochMs
        restTimerTotalSeconds = saved.totalSeconds.coerceAtLeast(remaining)
        restTimerVirtualRemaining = remaining
        updateState {
            copy(
                restTimerSecondsRemaining = remaining,
                restTimerTotalSeconds = restTimerTotalSeconds,
                restTimerEndEpochMs = restTimerEndEpochMs,
                isRestTimerActive = true
            )
        }
        launchRestTickerLoop()
    }

    private fun startRestTimer(seconds: Int) {
        restTimerJob?.cancel()
        val safeSec = seconds.coerceAtLeast(1)
        val now = nowProvider()
        val endMs = now + safeSec * 1000L
        restTimerEndEpochMs = endMs
        restTimerTotalSeconds = safeSec
        restTimerVirtualRemaining = safeSec
        DudaLocalStorage.setRestTimerState(
            PersistedRestTimerState(
                workoutId = currentState.workout?.id.orEmpty(),
                endEpochMs = endMs,
                totalSeconds = safeSec
            )
        )
        scheduleRestAlarm(endMs)
        updateState {
            copy(
                restTimerSecondsRemaining = safeSec,
                restTimerTotalSeconds = safeSec,
                restTimerEndEpochMs = endMs,
                isRestTimerActive = true
            )
        }
        launchRestTickerLoop()
    }

    private fun launchRestTickerLoop() {
        restTimerJob?.cancel()
        restTimerJob = viewModelScope.launch {
            while (isActive && restTimerEndEpochMs > 0L && currentState.isRestTimerActive) {
                delay(1000L)
                if (restTimerEndEpochMs <= 0L || !currentState.isRestTimerActive) break
                val now = nowProvider()
                val wallRemaining = ceil((restTimerEndEpochMs - now) / 1000.0).toInt().coerceAtLeast(0)
                restTimerVirtualRemaining = (restTimerVirtualRemaining - 1).coerceAtLeast(0)
                val remaining = minOf(restTimerVirtualRemaining, wallRemaining)
                restTimerVirtualRemaining = remaining
                if (remaining <= 0) {
                    val expiredLongAgo = wallRemaining <= 0 && (now - restTimerEndEpochMs) > 2500L
                    completeRestTimer(expiredInBackground = expiredLongAgo)
                    break
                } else {
                    if (remaining in 1..3) {
                        AthleteSoundManager.playRestCountdown(remaining)
                    }
                    updateState {
                        copy(
                            restTimerSecondsRemaining = remaining,
                            isRestTimerActive = true
                        )
                    }
                }
            }
        }
    }

    fun syncRestTimerFromClock() {
        val now = nowProvider()
        var endMs = restTimerEndEpochMs
        var totalSec = restTimerTotalSeconds
        if (endMs <= 0L) {
            val saved = DudaLocalStorage.getRestTimerState() ?: return
            val activeWorkoutId = currentState.workout?.id
            if (!activeWorkoutId.isNullOrBlank() && saved.workoutId.isNotBlank() && saved.workoutId != activeWorkoutId) {
                return
            }
            endMs = saved.endEpochMs
            totalSec = saved.totalSeconds.coerceAtLeast(1)
            restTimerEndEpochMs = endMs
            restTimerTotalSeconds = totalSec
        }
        if (endMs <= 0L) return
        val remaining = ceil((endMs - now) / 1000.0).toInt().coerceAtLeast(0)
        if (remaining <= 0) {
            completeRestTimer(expiredInBackground = (now - endMs) > 2500L)
        } else {
            restTimerVirtualRemaining = remaining
            updateState {
                copy(
                    restTimerSecondsRemaining = remaining,
                    restTimerTotalSeconds = totalSec,
                    restTimerEndEpochMs = endMs,
                    isRestTimerActive = true
                )
            }
            if (restTimerJob == null || restTimerJob?.isActive != true) {
                launchRestTickerLoop()
            }
        }
    }

    private fun completeRestTimer(expiredInBackground: Boolean) {
        val finishedEndMs = restTimerEndEpochMs
        restTimerJob?.cancel()
        restTimerJob = null
        restTimerEndEpochMs = 0L
        restTimerVirtualRemaining = 0
        DudaLocalStorage.setRestTimerState(null)
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
        updateState {
            copy(
                restTimerSecondsRemaining = 0,
                restTimerEndEpochMs = 0L,
                isRestTimerActive = false
            )
        }
        if (!expiredInBackground && finishedEndMs > 0L && finishedEndMs != lastForegroundRestAlertEndMs) {
            lastForegroundRestAlertEndMs = finishedEndMs
            AthleteSoundManager.playRestCountdown(0)
            sendEffect(WorkoutUiEffect.TriggerHaptic)
            sendEffect(WorkoutUiEffect.ShowToast(currentStrings().workoutRestUp))
        }
    }

    private fun addSecondsToRestTimer(seconds: Int) {
        val now = nowProvider()
        val baseEnd = if (restTimerEndEpochMs > now) restTimerEndEpochMs else (now + currentState.restTimerSecondsRemaining * 1000L)
        val newEnd = baseEnd + seconds * 1000L
        val wallUpdated = ceil((newEnd - now) / 1000.0).toInt().coerceAtLeast(0)
        val virtualUpdated = (restTimerVirtualRemaining + seconds).coerceAtLeast(0)
        val updated = minOf(wallUpdated, virtualUpdated)
        if (updated <= 0 || newEnd <= now) {
            stopRestTimer()
            return
        }
        val newTotal = maxOf(restTimerTotalSeconds, updated)
        restTimerEndEpochMs = newEnd
        restTimerTotalSeconds = newTotal
        restTimerVirtualRemaining = updated
        DudaLocalStorage.setRestTimerState(
            PersistedRestTimerState(
                workoutId = currentState.workout?.id.orEmpty(),
                endEpochMs = newEnd,
                totalSeconds = newTotal
            )
        )
        scheduleRestAlarm(newEnd)
        updateState {
            copy(
                restTimerSecondsRemaining = updated,
                restTimerTotalSeconds = newTotal,
                restTimerEndEpochMs = newEnd,
                isRestTimerActive = true
            )
        }
        if (restTimerJob == null || restTimerJob?.isActive != true) {
            launchRestTickerLoop()
        }
    }

    private fun stopRestTimer() {
        restTimerJob?.cancel()
        restTimerJob = null
        restTimerEndEpochMs = 0L
        restTimerVirtualRemaining = 0
        DudaLocalStorage.setRestTimerState(null)
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
        updateState {
            copy(
                restTimerSecondsRemaining = 0,
                restTimerEndEpochMs = 0L,
                isRestTimerActive = false
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        restTimerJob?.cancel()
    }
}

private data class WorkoutInitData(
    val active: Workout?,
    val exercises: List<Exercise>,
    val prefs: UserPreferencesData,
    val history: List<Workout>
)
