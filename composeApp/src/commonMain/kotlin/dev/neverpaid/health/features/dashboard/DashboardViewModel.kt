package dev.neverpaid.health.features.dashboard

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.WeeklyPlanItem
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.toLocalDateTime

class DashboardViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<DashboardUiAction, DashboardUiState, DashboardUiEffect>(
    initialState = DashboardUiState(
        selectedPlanWeekday = currentIsoWeekday(),
        addToPlanWeekdays = listOf(currentIsoWeekday()),
        prebuiltRoutines = repository.getPrebuiltRoutines(),
        isLoading = true
    )
) {

    init {
        observeDashboardStreams()
        refreshRemoteData()
    }

    private fun refreshRemoteData() {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.syncWithServer()
            updateState { copy(isLoading = false) }
        }
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
                launchGuarded(
                    onError = { err ->
                        sendError(err)
                        sendEffect(DashboardUiEffect.ShowSnackbar(err.message ?: "Ошибка переключения дня"))
                    }
                ) {
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
            is DashboardUiAction.OnSelectPlanWeekday -> {
                val day = action.dayOfWeek.coerceIn(1, 7)
                val swapSrc = currentState.swapSourceWeekday
                if (swapSrc != null) {
                    updateState { copy(swapSourceWeekday = null, selectedPlanWeekday = day) }
                    if (swapSrc != day) {
                        val swapped = currentState.weeklyPlanItems.map { it ->
                            when (it.dayOfWeek) {
                                swapSrc -> it.copy(dayOfWeek = day)
                                day -> it.copy(dayOfWeek = swapSrc)
                                else -> it
                            }
                        }
                        persistWeeklyPlan(swapped)
                    }
                } else {
                    updateState { copy(selectedPlanWeekday = day, movingPlanItemId = null) }
                }
            }
            DashboardUiAction.OnToggleSwapDaysMode -> {
                val next = if (currentState.swapSourceWeekday == null) currentState.selectedPlanWeekday else null
                updateState { copy(swapSourceWeekday = next) }
            }
            DashboardUiAction.OnAutoFillProgramToWeek -> {
                val prog = currentState.activeProgramBundle?.program ?: return
                val distributed = distributeProgramAcrossWeek(currentState.weeklyPlanItems, prog)
                persistWeeklyPlan(distributed)
            }
            is DashboardUiAction.OnOpenAddToPlanSheet -> {
                val day = action.dayOfWeek.coerceIn(1, 7)
                updateState {
                    copy(
                        isAddToPlanSheetOpen = true,
                        addToPlanWeekdays = listOf(day)
                    )
                }
            }
            DashboardUiAction.OnCloseAddToPlanSheet -> {
                updateState { copy(isAddToPlanSheetOpen = false) }
            }
            is DashboardUiAction.OnToggleAddToPlanWeekday -> {
                val day = action.dayOfWeek.coerceIn(1, 7)
                val curr = currentState.addToPlanWeekdays
                val next = if (day in curr) {
                    if (curr.size > 1) curr - day else curr
                } else {
                    (curr + day).sorted()
                }
                updateState { copy(addToPlanWeekdays = next) }
            }
            is DashboardUiAction.OnAddRoutineToPlan -> {
                val routine = action.routine
                val targetDays = currentState.addToPlanWeekdays.ifEmpty { listOf(currentState.selectedPlanWeekday) }
                val additions = targetDays.mapIndexed { idx, dow ->
                    WeeklyPlanItem(
                        id = PlatformBridge.generateUuid(),
                        dayOfWeek = dow,
                        orderIndex = 900 + idx,
                        itemType = "routine",
                        routineId = routine.id,
                        title = routine.title,
                        subtitle = routine.description,
                        exercises = routine.exercises
                    )
                }
                updateState { copy(isAddToPlanSheetOpen = false) }
                persistWeeklyPlan(currentState.weeklyPlanItems + additions)
            }
            is DashboardUiAction.OnAddProgramDayToPlan -> {
                val prog = action.program
                val day = action.day
                val targetDays = currentState.addToPlanWeekdays.ifEmpty { listOf(currentState.selectedPlanWeekday) }
                val mappedExs = day.exercises.mapIndexed { exIdx, ex ->
                    RoutineExercise(
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.exerciseName,
                        orderIndex = exIdx,
                        targetSets = ex.targetSets,
                        targetRepsMin = ex.targetRepsMin,
                        targetRepsMax = ex.targetRepsMax
                    )
                }
                val additions = targetDays.mapIndexed { idx, dow ->
                    WeeklyPlanItem(
                        id = PlatformBridge.generateUuid(),
                        dayOfWeek = dow,
                        orderIndex = 900 + idx,
                        itemType = "program_day",
                        programId = prog.id,
                        programDayNumber = day.dayNumber,
                        title = day.title,
                        subtitle = prog.title,
                        exercises = mappedExs
                    )
                }
                updateState { copy(isAddToPlanSheetOpen = false) }
                persistWeeklyPlan(currentState.weeklyPlanItems + additions)
            }
            is DashboardUiAction.OnStartWeeklyPlanItem -> {
                val item = action.item
                val mappedExercises = item.exercises.mapIndexed { idx, rEx ->
                    val catalogEx = repository.getExerciseById(rEx.exerciseId)
                    val setsCount = rEx.targetSets.coerceAtLeast(1)
                    val defaultReps = rEx.targetRepsMax ?: rEx.targetRepsMin ?: 10
                    WorkoutExercise(
                        instanceId = PlatformBridge.generateUuid(),
                        exerciseId = rEx.exerciseId,
                        exerciseName = rEx.exerciseName,
                        measurementType = catalogEx?.measurementType ?: "weight_reps",
                        primaryMuscleGroup = catalogEx?.effectiveMuscle ?: "chest",
                        equipment = catalogEx?.equipment ?: "barbell",
                        orderIndex = idx,
                        sets = (1..setsCount).map { sIdx ->
                            WorkoutSet(
                                id = PlatformBridge.generateUuid(),
                                setNumber = sIdx,
                                weightKg = 0.0,
                                reps = defaultReps
                            )
                        }
                    )
                }
                val workoutTitle = if (item.itemType == "program_day" && item.subtitle.isNotBlank()) {
                    "${item.subtitle} — ${item.title}"
                } else {
                    item.title.ifBlank { "Тренировка" }
                }
                repository.startWorkoutWithExercises(
                    title = workoutTitle,
                    exercises = mappedExercises,
                    routineId = item.routineId,
                    programId = item.programId,
                    programDayNumber = item.programDayNumber
                )
                sendEffect(DashboardUiEffect.NavigateToActiveWorkout)
            }
            is DashboardUiAction.OnToggleWeeklyPlanItemDone -> {
                launchGuarded(onError = { err -> sendError(err) }) {
                    repository.toggleWeeklyPlanItemComplete(action.itemId, action.completed)
                }
            }
            is DashboardUiAction.OnMoveWeeklyPlanItemWithinDay -> {
                val items = currentState.weeklyPlanItems
                val target = items.find { it.id == action.itemId } ?: return
                val dayItems = items
                    .filter { it.dayOfWeek == target.dayOfWeek }
                    .sortedBy { it.orderIndex }
                    .toMutableList()
                val idx = dayItems.indexOfFirst { it.id == action.itemId }
                val newIdx = if (action.moveUp) idx - 1 else idx + 1
                if (idx < 0 || newIdx !in dayItems.indices) return
                val moved = dayItems.removeAt(idx)
                dayItems.add(newIdx, moved)
                val reindexed = dayItems.mapIndexed { pos, it -> it.copy(orderIndex = pos) }
                val others = items.filter { it.dayOfWeek != target.dayOfWeek }
                persistWeeklyPlan(others + reindexed)
            }
            is DashboardUiAction.OnToggleMovePlanItemDayPicker -> {
                val next = if (currentState.movingPlanItemId == action.itemId) null else action.itemId
                updateState { copy(movingPlanItemId = next) }
            }
            is DashboardUiAction.OnMoveWeeklyPlanItemToDay -> {
                val targetDay = action.targetDayOfWeek.coerceIn(1, 7)
                val items = currentState.weeklyPlanItems
                val maxOrder = items.filter { it.dayOfWeek == targetDay }.maxOfOrNull { it.orderIndex } ?: -1
                val updated = items.map {
                    if (it.id == action.itemId) it.copy(dayOfWeek = targetDay, orderIndex = maxOrder + 1) else it
                }
                updateState { copy(movingPlanItemId = null, selectedPlanWeekday = targetDay) }
                persistWeeklyPlan(updated)
            }
            is DashboardUiAction.OnRemoveWeeklyPlanItem -> {
                persistWeeklyPlan(currentState.weeklyPlanItems.filterNot { it.id == action.itemId })
            }
            is DashboardUiAction.OnOpenRoutineEditor -> {
                val r = action.routine
                val editingId = r?.id?.takeIf { !r.id.startsWith("prebuilt-") }
                val existingDays = if (editingId != null) {
                    currentState.weeklyPlanItems
                        .filter { it.itemType == "routine" && it.routineId == editingId }
                        .map { it.dayOfWeek }
                        .distinct()
                        .sorted()
                } else {
                    listOfNotNull(action.defaultWeekday?.coerceIn(1, 7))
                }
                updateState {
                    copy(
                        isAddToPlanSheetOpen = false,
                        previewRoutine = null,
                        isRoutineEditorOpen = true,
                        editingRoutineId = editingId,
                        editorTitle = r?.title ?: "",
                        editorDescription = r?.description ?: "",
                        editorExercises = r?.exercises ?: emptyList(),
                        editorWeekdays = existingDays
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
            is DashboardUiAction.OnToggleEditorWeekday -> {
                val d = action.dayOfWeek.coerceIn(1, 7)
                val curr = currentState.editorWeekdays
                val next = if (d in curr) curr - d else (curr + d).sorted()
                updateState { copy(editorWeekdays = next) }
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
            is DashboardUiAction.OnMoveEditorExercise -> {
                val list = currentState.editorExercises.toMutableList()
                val targetIdx = if (action.moveUp) action.index - 1 else action.index + 1
                if (action.index !in list.indices || targetIdx !in list.indices) return
                val moved = list.removeAt(action.index)
                list.add(targetIdx, moved)
                updateState {
                    copy(editorExercises = list.mapIndexed { idx, ex -> ex.copy(orderIndex = idx + 1) })
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
                launchGuarded(
                    onError = { err ->
                        sendError(err)
                        sendEffect(DashboardUiEffect.ShowSnackbar(err.message ?: "Ошибка удаления рутины"))
                    }
                ) {
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

    private fun persistWeeklyPlan(items: List<WeeklyPlanItem>) {
        launchGuarded(
            onError = { err ->
                sendError(err)
                sendEffect(DashboardUiEffect.ShowSnackbar(err.message ?: "Ошибка сохранения расписания"))
            }
        ) {
            repository.saveWeeklyPlan(items)
        }
    }

    private fun distributeProgramAcrossWeek(
        existing: List<WeeklyPlanItem>,
        program: TrainingProgram
    ): List<WeeklyPlanItem> {
        val days = program.days.filterNot { it.isRestDay && it.exercises.isEmpty() }
        if (days.isEmpty()) return existing
        val spread = when (days.size) {
            1 -> listOf(1)
            2 -> listOf(1, 4)
            3 -> listOf(1, 3, 5)
            4 -> listOf(1, 2, 4, 5)
            5 -> listOf(1, 2, 3, 5, 6)
            6 -> listOf(1, 2, 3, 4, 5, 6)
            else -> listOf(1, 2, 3, 4, 5, 6, 7)
        }
        val kept = existing.filterNot { it.programId == program.id }
        val added = days.mapIndexed { idx, d ->
            val dow = spread.getOrElse(idx) { (idx % 7) + 1 }
            WeeklyPlanItem(
                id = PlatformBridge.generateUuid(),
                dayOfWeek = dow,
                orderIndex = 900 + idx,
                itemType = "program_day",
                programId = program.id,
                programDayNumber = d.dayNumber,
                title = d.title,
                subtitle = program.title,
                exercises = d.exercises.mapIndexed { exIdx, ex ->
                    RoutineExercise(
                        exerciseId = ex.exerciseId,
                        exerciseName = ex.exerciseName,
                        orderIndex = exIdx,
                        targetSets = ex.targetSets,
                        targetRepsMin = ex.targetRepsMin,
                        targetRepsMax = ex.targetRepsMax
                    )
                }
            )
        }
        return kept + added
    }

    private fun observeDashboardStreams() {
        val topGroup = combine(
            repository.getActiveWorkout(),
            repository.observeRoutines(),
            repository.observeActiveProgram(),
            repository.getInstalledProgramsFlow()
        ) { active, routines, activeProgram, installed ->
            Quadruple(active, routines, activeProgram, installed)
        }
        val bottomGroup = combine(
            repository.observeWeeklyPlan(),
            repository.getProgressSummary(),
            repository.getExercises()
        ) { weeklyPlan, progress, exercises ->
            Triple(weeklyPlan, progress, exercises)
        }

        combine(topGroup, bottomGroup) { (active, routines, activeProgram, installed), (weeklyPlan, progress, exercises) ->
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
                    installedPrograms = installed,
                    weeklyPlanItems = weeklyPlan,
                    selectedProgramDayNumber = selectedDay,
                    allExercises = exercises,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
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
        val chosenDays = currentState.editorWeekdays
        launchGuarded(
            onError = { err ->
                sendEffect(DashboardUiEffect.ShowSnackbar(err.message ?: "Ошибка сохранения"))
            }
        ) {
            val saved = if (editingId != null) {
                repository.updateRoutine(editingId, req)
            } else {
                repository.createRoutine(req)
            }
            if (editingId != null || chosenDays.isNotEmpty()) {
                repository.setRoutineWeekdays(saved.id, chosenDays)
            }
            sendEffect(
                DashboardUiEffect.ShowSnackbar(
                    if (editingId != null) "Рутина обновлена" else "Новая рутина сохранена"
                )
            )
            updateState { copy(isRoutineEditorOpen = false) }
        }
    }

    private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

    companion object {
        fun currentIsoWeekday(): Int {
            return runCatching {
                Instant.fromEpochMilliseconds(PlatformBridge.currentTimeMillis())
                    .toLocalDateTime(TimeZone.currentSystemDefault())
                    .date
                    .dayOfWeek
                    .isoDayNumber
            }.getOrDefault(1).coerceIn(1, 7)
        }
    }
}
