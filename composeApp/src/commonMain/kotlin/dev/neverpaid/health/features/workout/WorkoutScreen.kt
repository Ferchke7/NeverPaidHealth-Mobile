package dev.neverpaid.health.features.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderDivider
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExercisePickerBottomSheet
import dev.neverpaid.health.core.designsystem.ExerciseThumbnail
import dev.neverpaid.health.core.designsystem.ExerciseThumbnailSize
import dev.neverpaid.health.core.designsystem.InteractivePlateCalculator
import dev.neverpaid.health.core.designsystem.ObsidianCard
import dev.neverpaid.health.core.designsystem.ObsidianDivider
import dev.neverpaid.health.core.designsystem.ObsidianEmptyCard
import dev.neverpaid.health.core.designsystem.ObsidianOutlineButton
import dev.neverpaid.health.core.designsystem.ObsidianPageHeader
import dev.neverpaid.health.core.designsystem.ObsidianPillTab
import dev.neverpaid.health.core.designsystem.ObsidianPrimaryButton
import dev.neverpaid.health.core.designsystem.ObsidianProgressBar
import dev.neverpaid.health.core.designsystem.ObsidianTextField
import dev.neverpaid.health.core.designsystem.ObsidianInnerWell
import dev.neverpaid.health.core.designsystem.ObsidianWhiteButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.designsystem.localizeMuscleName
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.HistoricalSetRecord
import dev.neverpaid.health.core.math.SetHistoryCalculator
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import kotlinx.coroutines.delay
import kotlin.math.round

private enum class FinishSheetStage {
    CONFIRM_INCOMPLETE,
    SUMMARY
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutRoute(
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscardSheet by remember { mutableStateOf(false) }
    var finishSheetStage by remember { mutableStateOf<FinishSheetStage?>(null) }
    var completedSummaryWorkout by remember { mutableStateOf<Workout?>(null) }
    var warmupExercise by remember { mutableStateOf<WorkoutExercise?>(null) }
    var plateCalculatorWeightKg by remember { mutableStateOf<Double?>(null) }
    var plateCalculatorTargetExId by remember { mutableStateOf<String?>(null) }
    var plateCalculatorTargetSetId by remember { mutableStateOf<String?>(null) }
    var restSheetTotalSeconds by remember { mutableIntStateOf(90) }
    var showRestSheet by remember { mutableStateOf(false) }
    var lastRestPresetSeconds by remember { mutableIntStateOf(90) }

    LaunchedEffect(state.isRestTimerActive, state.restTimerSecondsRemaining) {
        if (state.isRestTimerActive && state.restTimerSecondsRemaining > restSheetTotalSeconds) {
            restSheetTotalSeconds = state.restTimerSecondsRemaining
        }
    }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutUiEffect.WorkoutFinishedSummary -> completedSummaryWorkout = effect.workout
            WorkoutUiEffect.NavigateBack -> onNavigateBack()
            WorkoutUiEffect.TriggerHaptic -> PlatformBridge.vibrate()
            is WorkoutUiEffect.ShowToast -> {}
        }
    }

    val workout = state.workout

    WorkoutScreen(
        state = state,
        restPresetSeconds = lastRestPresetSeconds,
        restTotalSeconds = restSheetTotalSeconds,
        onAction = viewModel::onAction,
        onRequestFinish = {
            if (workout != null) {
                val hasIncomplete = workout.exercises.any { ex -> ex.sets.any { !it.isCompleted } }
                finishSheetStage = if (hasIncomplete) {
                    FinishSheetStage.CONFIRM_INCOMPLETE
                } else {
                    FinishSheetStage.SUMMARY
                }
            }
        },
        onRequestDiscard = { showDiscardSheet = true },
        onOpenWarmup = { ex -> warmupExercise = ex },
        onOpenPlates = { exId, setId, wKg ->
            plateCalculatorTargetExId = exId
            plateCalculatorTargetSetId = setId
            plateCalculatorWeightKg = wKg.coerceAtLeast(20.0)
        },
        onStartOrOpenRest = { secs ->
            lastRestPresetSeconds = secs
            restSheetTotalSeconds = secs
            if (!state.isRestTimerActive) {
                viewModel.onAction(WorkoutUiAction.OnStartManualRestTimer(secs))
            }
            showRestSheet = true
        },
        onCycleRestPreset = {
            val presets = listOf(60, 90, 120, 180)
            val nextIdx = (presets.indexOf(lastRestPresetSeconds) + 1) % presets.size
            lastRestPresetSeconds = presets[nextIdx]
            restSheetTotalSeconds = lastRestPresetSeconds
            viewModel.onAction(WorkoutUiAction.OnStartManualRestTimer(lastRestPresetSeconds))
        },
        modifier = modifier
    )

    if (state.showExercisePicker) {
        ExercisePickerBottomSheet(
            exercises = state.availableExercises,
            title = if (state.swappingExerciseId != null) {
                "Заменить упражнение"
            } else {
                "Добавить упражнение"
            },
            onSelectExercise = { ex ->
                viewModel.onAction(WorkoutUiAction.OnSelectExerciseToAdd(ex))
            },
            onInspectExercise = { ex ->
                viewModel.onAction(WorkoutUiAction.OnInspectExercise(ex))
            },
            onDismiss = {
                viewModel.onAction(WorkoutUiAction.OnCloseExercisePicker)
            }
        )
    }

    if (state.inspectedExercise != null) {
        ExerciseInfoBottomSheet(
            exercise = state.inspectedExercise!!,
            onDismiss = {
                viewModel.onAction(WorkoutUiAction.OnInspectExercise(null))
            }
        )
    }

    if (warmupExercise != null) {
        WarmupCalculatorBottomSheet(
            isOpen = true,
            workingWeightKg = warmupExercise?.sets?.lastOrNull { it.weightKg > 0 }?.weightKg ?: 80.0,
            exerciseName = warmupExercise?.exerciseName ?: "",
            onDismiss = { warmupExercise = null },
            onApplyWarmupSets = { sets ->
                val exId = warmupExercise?.exerciseId ?: ""
                sets.forEach { _ ->
                    viewModel.onAction(WorkoutUiAction.OnAddSet(exId))
                }
                warmupExercise = null
            }
        )
    }

    if (state.historySheetExerciseId != null) {
        val exerciseHistory = remember(state.workoutHistory, state.historySheetExerciseId) {
            SetHistoryCalculator.extractExerciseHistory(
                workouts = state.workoutHistory,
                exerciseId = state.historySheetExerciseId ?: "",
                exerciseName = state.historySheetExerciseName ?: ""
            )
        }

        ExerciseSetHistoryBottomSheet(
            exerciseName = state.historySheetExerciseName ?: "",
            initialSetNumber = state.historySheetSetNumber,
            history = exerciseHistory,
            isKgUnit = state.isKgUnit,
            onApplyValues = { w, r ->
                val exId = state.historySheetExerciseId ?: ""
                val setId = state.historySheetActiveSetId ?: ""
                if (exId.isNotEmpty() && setId.isNotEmpty()) {
                    viewModel.onAction(WorkoutUiAction.OnApplySetValues(exId, setId, w, r))
                }
                viewModel.onAction(WorkoutUiAction.OnCloseSetHistorySheet)
            },
            onDismiss = {
                viewModel.onAction(WorkoutUiAction.OnCloseSetHistorySheet)
            }
        )
    }

    if (plateCalculatorWeightKg != null) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { plateCalculatorWeightKg = null },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Text(
                    text = LocalDudaStrings.current.plateCalculatorTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))
                InteractivePlateCalculator(
                    initialWeightKg = plateCalculatorWeightKg ?: 60.0,
                    onWeightConfirmed = { confirmedKg ->
                        val exId = plateCalculatorTargetExId
                        val setId = plateCalculatorTargetSetId
                        if (!exId.isNullOrBlank() && !setId.isNullOrBlank()) {
                            viewModel.onAction(WorkoutUiAction.OnUpdateWeight(exId, setId, confirmedKg))
                        }
                        plateCalculatorWeightKg = null
                    }
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showRestSheet && workout != null) {
        WorkoutRestModalSheet(
            secondsRemaining = state.restTimerSecondsRemaining.coerceAtLeast(0),
            totalSeconds = restSheetTotalSeconds.coerceAtLeast(1),
            restPresetSeconds = lastRestPresetSeconds,
            workout = workout,
            isKgUnit = state.isKgUnit,
            onAddSeconds = { viewModel.onAction(WorkoutUiAction.OnRestTimerAddSeconds(it)) },
            onSkip = {
                viewModel.onAction(WorkoutUiAction.OnRestTimerSkip)
                showRestSheet = false
            },
            onCyclePreset = {
                val presets = listOf(60, 90, 120, 180)
                val nextIdx = (presets.indexOf(lastRestPresetSeconds) + 1) % presets.size
                lastRestPresetSeconds = presets[nextIdx]
                restSheetTotalSeconds = lastRestPresetSeconds
                viewModel.onAction(WorkoutUiAction.OnStartManualRestTimer(lastRestPresetSeconds))
            },
            onDismiss = { showRestSheet = false }
        )
    }

    val activeStage = finishSheetStage
    if (activeStage != null && workout != null) {
        WorkoutFinishModalSheet(
            stage = activeStage,
            workout = workout,
            startTimeEpochMs = state.workoutStartTimeEpochMs,
            isKgUnit = state.isKgUnit,
            isFinishing = state.isFinishing,
            onProceedToSummary = { finishSheetStage = FinishSheetStage.SUMMARY },
            onConfirmSave = {
                finishSheetStage = null
                viewModel.onAction(WorkoutUiAction.OnFinishWorkoutClick)
            },
            onDismiss = { finishSheetStage = null }
        )
    }

    if (completedSummaryWorkout != null) {
        val summaryWorkout = completedSummaryWorkout!!
        WorkoutFinishModalSheet(
            stage = FinishSheetStage.SUMMARY,
            workout = summaryWorkout,
            startTimeEpochMs = summaryWorkout.startTimeEpochMs,
            isKgUnit = state.isKgUnit,
            isFinishing = false,
            onProceedToSummary = {},
            onConfirmSave = {
                completedSummaryWorkout = null
                onNavigateBack()
            },
            onDismiss = {
                completedSummaryWorkout = null
                onNavigateBack()
            }
        )
    }

    if (showDiscardSheet) {
        WorkoutDiscardModalSheet(
            onConfirmDiscard = {
                showDiscardSheet = false
                viewModel.onAction(WorkoutUiAction.OnDiscardWorkoutClick)
            },
            onDismiss = { showDiscardSheet = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    restPresetSeconds: Int = 90,
    restTotalSeconds: Int = 90,
    onAction: (WorkoutUiAction) -> Unit,
    onRequestFinish: () -> Unit = { onAction(WorkoutUiAction.OnFinishWorkoutClick) },
    onRequestDiscard: () -> Unit = {},
    onOpenWarmup: (WorkoutExercise) -> Unit = {},
    onOpenPlates: (String, String, Double) -> Unit = { _, _, _ -> },
    onStartOrOpenRest: (Int) -> Unit = { onAction(WorkoutUiAction.OnStartManualRestTimer(it)) },
    onCycleRestPreset: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val workout = state.workout
    var showAllExercises by remember(workout?.id) { mutableStateOf(false) }
    var prevExerciseCount by remember(workout?.id) { mutableIntStateOf(workout?.exercises?.size ?: 0) }
    var showSessionNoteEditor by remember { mutableStateOf(false) }
    var sessionTitleDraft by remember(workout?.id, workout?.title) { mutableStateOf(workout?.title ?: "") }

    LaunchedEffect(workout?.exercises?.size) {
        val count = workout?.exercises?.size ?: 0
        if (count > prevExerciseCount) {
            showAllExercises = true
        }
        prevExerciseCount = count
    }

    var elapsedSeconds by remember(state.workoutStartTimeEpochMs) { mutableStateOf(0L) }
    LaunchedEffect(state.workoutStartTimeEpochMs, workout?.id) {
        while (true) {
            val nowMs = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val start = if (state.workoutStartTimeEpochMs > 0L) state.workoutStartTimeEpochMs else nowMs
            elapsedSeconds = ((nowMs - start) / 1000L).coerceAtLeast(0L)
            delay(1000L)
        }
    }

    if (workout == null) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            containerColor = DarkBackground
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 20.dp, vertical = 16.dp)
            ) {
                ObsidianPageHeader(
                    title = strings.navWorkouts,
                    subtitle = "Нет активной сессии"
                )
                Spacer(modifier = Modifier.height(16.dp))
                ObsidianEmptyCard(
                    title = "Тренировка не запущена",
                    description = "Выберите шаблон на вкладке «Тренировки» или начните пустую сессию."
                )
            }
        }
        return
    }

    val totalVolumeKg = remember(workout) {
        workout.exercises.sumOf { ex ->
            ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
        }
    }
    val displayTotalVolume = if (state.isKgUnit) totalVolumeKg else StrengthCalculator.kgToLbs(totalVolumeKg)
    val unitLabel = if (state.isKgUnit) strings.unitKg else strings.unitLbs

    val visibleExercises = if (showAllExercises || workout.exercises.size <= 2) {
        workout.exercises
    } else {
        workout.exercises.take(2)
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "page_header", contentType = "header") {
                ObsidianPageHeader(
                    title = "Тренировка",
                    subtitle = workout.title.ifBlank { "Свободная тренировка" }
                )
            }

            // Card 1: Session Metrics & Controls (1:1 Figma 04)
            item(key = "session_summary_card", contentType = "summary") {
                ObsidianCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ВРЕМЯ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = formatElapsedShort(elapsedSeconds),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ОБЪЁМ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "${formatRoundedInt(displayTotalVolume)} $unitLabel",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "УПРАЖНЕНИЯ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${workout.exercises.size}",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianPrimaryButton(
                            text = strings.workoutFinish,
                            onClick = onRequestFinish
                        )
                        ObsidianOutlineButton(
                            text = strings.workoutDiscard,
                            onClick = onRequestDiscard
                        )
                        ObsidianOutlineButton(
                            text = if (showSessionNoteEditor) "Скрыть заметку" else "Заметка сессии",
                            onClick = { showSessionNoteEditor = !showSessionNoteEditor }
                        )
                    }

                    AnimatedVisibility(visible = showSessionNoteEditor) {
                        Column {
                            Spacer(modifier = Modifier.height(14.dp))
                            ObsidianDivider()
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "Название / заметка тренировки",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            ObsidianTextField(
                                value = sessionTitleDraft,
                                onValueChange = {
                                    sessionTitleDraft = it
                                    onAction(WorkoutUiAction.OnUpdateWorkoutTitle(it))
                                },
                                placeholder = "Push Hypertrophy · День 1"
                            )
                        }
                    }
                }
            }

            // Inline Active Rest Card when countdown runs (1:1 Figma 15 Card 1)
            item(key = "active_rest_card", contentType = "rest") {
                AnimatedVisibility(
                    visible = state.isRestTimerActive && state.restTimerSecondsRemaining > 0,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    val lastCompleted = remember(workout) { findLastCompletedSetInfo(workout) }
                    ObsidianCard {
                        Text(
                            text = "01 / ОТДЫХ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldNeon,
                            letterSpacing = 0.8.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Время восстановиться",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = formatCountdown(state.restTimerSecondsRemaining),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Осталось ${state.restTimerSecondsRemaining} секунд",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        val progress = (state.restTimerSecondsRemaining.toFloat() / restTotalSeconds.coerceAtLeast(1).toFloat())
                            .coerceIn(0f, 1f)
                        ObsidianProgressBar(
                            progress = progress,
                            color = EmeraldNeon
                        )

                        if (lastCompleted != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            ObsidianDivider()
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = lastCompleted.exerciseName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val lw = if (state.isKgUnit) lastCompleted.weightKg else StrengthCalculator.kgToLbs(lastCompleted.weightKg)
                            val lv = lw * lastCompleted.reps
                            Text(
                                text = "Последний подход · ${formatCleanWeight(lw)} $unitLabel × ${lastCompleted.reps}",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "${formatRoundedInt(lv)} $unitLabel",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "объём последнего подхода",
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ObsidianOutlineButton(
                                text = "−15 сек",
                                onClick = { onAction(WorkoutUiAction.OnRestTimerAddSeconds(-15)) }
                            )
                            ObsidianOutlineButton(
                                text = "+30 сек",
                                onClick = { onAction(WorkoutUiAction.OnRestTimerAddSeconds(30)) }
                            )
                            ObsidianOutlineButton(
                                text = "Пропустить",
                                onClick = { onAction(WorkoutUiAction.OnRestTimerSkip) }
                            )
                            ObsidianOutlineButton(
                                text = "Отдых ${formatCountdown(restPresetSeconds)}",
                                onClick = onCycleRestPreset
                            )
                        }
                    }
                }
            }

            if (workout.exercises.isEmpty()) {
                item(key = "empty_exercises_card", contentType = "empty") {
                    ObsidianEmptyCard(
                        title = "Упражнений пока нет",
                        description = "Добавьте упражнения из каталога, чтобы начать фиксировать подходы и тоннаж.",
                        primaryActionLabel = strings.workoutAddExercise,
                        onPrimaryAction = { onAction(WorkoutUiAction.OnOpenExercisePicker) }
                    )
                }
            } else {
                itemsIndexed(
                    items = visibleExercises,
                    key = { idx, ex -> "${ex.id}_$idx" },
                    contentType = { _, _ -> "exercise_card" }
                ) { index, exercise ->
                    WorkoutExerciseCard(
                        index = index,
                        totalExercises = workout.exercises.size,
                        exercise = exercise,
                        workoutHistory = state.workoutHistory,
                        isKgUnit = state.isKgUnit,
                        restPresetSeconds = restPresetSeconds,
                        onInspect = { onAction(WorkoutUiAction.OnInspectExercise(exercise.exercise)) },
                        onSwap = { onAction(WorkoutUiAction.OnOpenSwapExercisePicker(exercise.exerciseId)) },
                        onRemove = { onAction(WorkoutUiAction.OnRemoveExercise(exercise.exerciseId)) },
                        onUpdateNotes = { notes -> onAction(WorkoutUiAction.OnUpdateExerciseNotes(exercise.exerciseId, notes)) },
                        onUpdateWeight = { setId, wKg -> onAction(WorkoutUiAction.OnUpdateWeight(exercise.exerciseId, setId, wKg)) },
                        onUpdateReps = { setId, reps -> onAction(WorkoutUiAction.OnUpdateReps(exercise.exerciseId, setId, reps)) },
                        onApplyPrevious = { setId -> onAction(WorkoutUiAction.OnApplyPreviousSet(exercise.exerciseId, setId)) },
                        onOpenSetHistory = { setNumber, setId ->
                            onAction(
                                WorkoutUiAction.OnOpenSetHistorySheet(
                                    exerciseId = exercise.exerciseId,
                                    exerciseName = exercise.exerciseName,
                                    setNumber = setNumber,
                                    setId = setId
                                )
                            )
                        },
                        onToggleSetComplete = { setId -> onAction(WorkoutUiAction.OnToggleSetComplete(exercise.exerciseId, setId)) },
                        onToggleSetType = { setId -> onAction(WorkoutUiAction.OnToggleSetType(exercise.exerciseId, setId)) },
                        onAddSet = { onAction(WorkoutUiAction.OnAddSet(exercise.exerciseId)) },
                        onDeleteSet = { setId -> onAction(WorkoutUiAction.OnDeleteSet(exercise.exerciseId, setId)) },
                        onOpenWarmup = { onOpenWarmup(exercise) },
                        onOpenPlates = { setId, wKg -> onOpenPlates(exercise.exerciseId, setId, wKg) },
                        onOpenRest = { onStartOrOpenRest(restPresetSeconds) }
                    )
                }

                if (workout.exercises.size > 2 && !showAllExercises) {
                    val remaining = workout.exercises.size - 2
                    item(key = "show_more_card", contentType = "show_more") {
                        ObsidianCard {
                            Text(
                                text = "Ещё $remaining упражнения",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Откройте, чтобы продолжить тренировку",
                                fontSize = 14.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(14.dp))
                            ObsidianOutlineButton(
                                text = "Показать ещё $remaining",
                                onClick = { showAllExercises = true }
                            )
                        }
                    }
                }

                item(key = "add_exercise_footer", contentType = "footer") {
                    ObsidianCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Нужно больше объёма?",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Добавьте упражнение в текущую сессию",
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            ObsidianOutlineButton(
                                text = "+ Добавить",
                                onClick = { onAction(WorkoutUiAction.OnOpenExercisePicker) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutExerciseCard(
    index: Int,
    totalExercises: Int,
    exercise: WorkoutExercise,
    workoutHistory: List<Workout>,
    isKgUnit: Boolean,
    restPresetSeconds: Int,
    onInspect: () -> Unit,
    onSwap: () -> Unit,
    onRemove: () -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateWeight: (String, Double) -> Unit,
    onUpdateReps: (String, Int) -> Unit,
    onApplyPrevious: (String) -> Unit,
    onOpenSetHistory: (Int, String) -> Unit,
    onToggleSetComplete: (String) -> Unit,
    onToggleSetType: (String) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit,
    onOpenWarmup: () -> Unit,
    onOpenPlates: (String, Double) -> Unit,
    onOpenRest: () -> Unit
) {
    val strings = LocalDudaStrings.current
    val unitLabel = if (isKgUnit) strings.unitKg else strings.unitLbs

    val exerciseHistory = remember(workoutHistory, exercise.exerciseId) {
        SetHistoryCalculator.extractExerciseHistory(
            workouts = workoutHistory,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.exerciseName
        )
    }
    val prData = remember(exerciseHistory) {
        SetHistoryCalculator.getAllTimePR(exerciseHistory)
    }
    val prRecord = prData.bestRecord

    var selectedSetId by remember(exercise.id, exercise.sets.size) {
        mutableStateOf(
            exercise.sets.firstOrNull { !it.isCompleted }?.id
                ?: exercise.sets.firstOrNull()?.id
                ?: ""
        )
    }
    val activeSet = exercise.sets.find { it.id == selectedSetId }
        ?: exercise.sets.firstOrNull { !it.isCompleted }
        ?: exercise.sets.firstOrNull()

    var noteExpanded by remember { mutableStateOf(exercise.notes.isNotBlank()) }
    var editingNotes by remember { mutableStateOf(false) }
    var noteDraft by remember(exercise.id, exercise.notes) { mutableStateOf(exercise.notes) }

    val indexPadded = (index + 1).toString().padStart(2, '0')
    val totalPadded = totalExercises.coerceAtLeast(1).toString().padStart(2, '0')
    val muscleUpper = localizeMuscleName(exercise.primaryMuscleGroup).uppercase()

    ObsidianCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ExerciseThumbnail(
                exerciseName = exercise.exerciseName.ifBlank { exercise.exercise.name },
                sourceId = exercise.exercise.sourceId,
                exerciseId = exercise.exerciseId,
                fallbackMuscle = exercise.primaryMuscleGroup,
                size = ExerciseThumbnailSize.MD,
                onClick = onInspect
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "$indexPadded / $totalPadded · $muscleUpper",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = exercise.exerciseName.ifBlank { exercise.exercise.name },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.clickable(onClick = onInspect)
                )
                Spacer(modifier = Modifier.height(4.dp))
                if (prRecord != null) {
                    val prW = if (isKgUnit) prRecord.weightKg else StrengthCalculator.kgToLbs(prRecord.weightKg)
                    Text(
                        text = "Личный рекорд · ${formatCleanWeight(prW)} $unitLabel × ${prRecord.reps}",
                        fontSize = 13.sp,
                        color = EmeraldNeon,
                        modifier = Modifier.clickable(onClick = onInspect)
                    )
                } else {
                    Text(
                        text = "Техника и инструкции →",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        modifier = Modifier.clickable(onClick = onInspect)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianOutlineButton(
                text = "Заменить",
                onClick = onSwap
            )
            ObsidianOutlineButton(
                text = "Убрать",
                onClick = onRemove
            )
        }

        if (exercise.notes.isNotBlank() || editingNotes) {
            Spacer(modifier = Modifier.height(14.dp))
            ObsidianInnerWell {
                Text(
                    text = if (exercise.notes.isNotBlank()) "Заметка · 1 активна" else "Заметка к упражнению",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EmeraldNeon
                )
                if (editingNotes) {
                    Spacer(modifier = Modifier.height(8.dp))
                    ObsidianTextField(
                        value = noteDraft,
                        onValueChange = { noteDraft = it },
                        placeholder = "Пауза 1 сек в нижней точке, лопатки сведены",
                        singleLine = false
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianPrimaryButton(
                            text = strings.save,
                            onClick = {
                                onUpdateNotes(noteDraft.trim())
                                editingNotes = false
                                noteExpanded = noteDraft.isNotBlank()
                            }
                        )
                        ObsidianOutlineButton(
                            text = strings.cancel,
                            onClick = {
                                noteDraft = exercise.notes
                                editingNotes = false
                            }
                        )
                    }
                } else {
                    if (noteExpanded && exercise.notes.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exercise.notes,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextSoft
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianOutlineButton(
                            text = if (noteExpanded) "Свернуть" else "Открыть",
                            onClick = { noteExpanded = !noteExpanded }
                        )
                        ObsidianOutlineButton(
                            text = "Комментировать",
                            onClick = {
                                noteExpanded = true
                                editingNotes = true
                            }
                        )
                    }
                }
            }
        }

        if (activeSet != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Тип подхода · подход ${activeSet.setNumber}",
                fontSize = 12.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            val setTypeOrder = listOf("normal", "warmup", "drop", "failure")
            val setTypeLabels = listOf("Обычный", "Разминка", "Дроп", "Отказ")
            val currentIdx = setTypeOrder.indexOf(activeSet.setType).coerceAtLeast(0)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                setTypeOrder.forEachIndexed { targetIdx, typeKey ->
                    ObsidianPillTab(
                        text = setTypeLabels[targetIdx],
                        selected = activeSet.setType == typeKey,
                        onClick = {
                            val steps = (targetIdx - currentIdx + setTypeOrder.size) % setTypeOrder.size
                            repeat(steps) { onToggleSetType(activeSet.id) }
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Column headers
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "№",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.width(26.dp)
            )
            Text(
                text = "Вес, $unitLabel",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "Повторы",
                fontSize = 12.sp,
                color = TextMuted,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier.width(36.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✓",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        exercise.sets.forEachIndexed { sIdx, set ->
            val displayNum = when (set.setType) {
                "warmup" -> "W"
                "drop" -> "D"
                "failure" -> "F"
                else -> "${set.setNumber}"
            }
            val displayColor = when {
                set.id == activeSet?.id -> EmeraldNeon
                set.setType == "warmup" -> AmberWarm
                else -> TextPrimary
            }

            WorkoutSetRow(
                set = set,
                displayBadge = displayNum,
                badgeColor = displayColor,
                isKgUnit = isKgUnit,
                unitLabel = unitLabel,
                prRecord = prRecord,
                canDelete = exercise.sets.size > 1,
                onSelectSet = { selectedSetId = set.id },
                onUpdateWeight = { onUpdateWeight(set.id, it) },
                onUpdateReps = { onUpdateReps(set.id, it) },
                onApplyPrevious = { onApplyPrevious(set.id) },
                onOpenHistory = {
                    selectedSetId = set.id
                    onOpenSetHistory(set.setNumber, set.id)
                },
                onToggleComplete = {
                    selectedSetId = set.id
                    if (!set.isCompleted) {
                        val isNewPr = prRecord != null &&
                            set.weightKg > 0 &&
                            set.reps > 0 &&
                            ((StrengthCalculator.calculateE1RM(set.weightKg, set.reps) ?: 0.0) > prRecord.e1rmKg)
                        if (isNewPr) {
                            AthleteSoundManager.playPersonalRecordFanfare()
                        } else {
                            AthleteSoundManager.playSetComplete()
                        }
                        PlatformBridge.vibrate()
                    }
                    onToggleSetComplete(set.id)
                },
                onDelete = { onDeleteSet(set.id) }
            )

            if (sIdx < exercise.sets.lastIndex) {
                ObsidianDivider()
            }
        }

        if (activeSet != null) {
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Быстрые повторы",
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(8, 6, 4).forEach { repsPreset ->
                    ObsidianPillTab(
                        text = "$repsPreset",
                        selected = activeSet.reps == repsPreset,
                        onClick = { onUpdateReps(activeSet.id, repsPreset) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        val plateWeightKg = activeSet?.weightKg?.takeIf { it > 0 }
            ?: exercise.sets.lastOrNull { it.weightKg > 0 }?.weightKg
            ?: 60.0

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianOutlineButton(
                text = "+ Подход",
                onClick = onAddSet
            )
            ObsidianOutlineButton(
                text = "+ Разминка",
                onClick = onOpenWarmup
            )
            if (exercise.equipment.lowercase() in setOf("barbell", "ez_bar", "smith_machine", "machine")) {
                ObsidianOutlineButton(
                    text = "Блины",
                    onClick = { onOpenPlates(activeSet?.id ?: "", plateWeightKg) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianOutlineButton(
                text = "Отдых · ${formatCountdown(restPresetSeconds)}",
                onClick = onOpenRest
            )
            ObsidianOutlineButton(
                text = "Заметка",
                onClick = {
                    noteExpanded = true
                    editingNotes = !editingNotes
                }
            )
        }
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    displayBadge: String,
    badgeColor: Color,
    isKgUnit: Boolean,
    unitLabel: String,
    prRecord: HistoricalSetRecord?,
    canDelete: Boolean,
    onSelectSet: () -> Unit,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onApplyPrevious: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleComplete: () -> Unit,
    onDelete: () -> Unit
) {
    val displayedWeight = if (isKgUnit) set.weightKg else StrengthCalculator.kgToLbs(set.weightKg)
    var weightText by remember(set.id, set.weightKg, isKgUnit) {
        mutableStateOf(
            if (displayedWeight <= 0.0) "" else formatCleanWeight(displayedWeight)
        )
    }
    var repsText by remember(set.id, set.reps) {
        mutableStateOf(if (set.reps <= 0) "" else set.reps.toString())
    }

    val hasPrev = set.previousWeightKg != null && set.previousReps != null
    val prevDisplayedW = if (set.previousWeightKg != null) {
        if (isKgUnit) set.previousWeightKg else StrengthCalculator.kgToLbs(set.previousWeightKg)
    } else 0.0

    val inputBorder = if (set.isCompleted) EmeraldNeon.copy(alpha = 0.45f) else BorderSubtle

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelectSet)
            .padding(vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .width(26.dp)
                    .clickable(onClick = onOpenHistory),
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    text = displayBadge,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = badgeColor
                )
            }

            // Weight Input (#0A0A0A)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
                    .border(1.dp, inputBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = weightText,
                    onValueChange = { raw ->
                        val cleaned = raw.replace(",", ".").filter { it.isDigit() || it == '.' }
                        weightText = cleaned
                        val parsed = cleaned.toDoubleOrNull() ?: 0.0
                        val kgVal = if (isKgUnit) parsed else StrengthCalculator.lbsToKg(parsed)
                        onUpdateWeight(kgVal)
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(EmeraldNeon),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    decorationBox = { inner ->
                        if (weightText.isEmpty()) {
                            Text("0", color = TextMuted, fontSize = 14.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Reps Input (#0A0A0A)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBackground)
                    .border(1.dp, inputBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                BasicTextField(
                    value = repsText,
                    onValueChange = { raw ->
                        val cleaned = raw.filter { it.isDigit() }
                        repsText = cleaned
                        onUpdateReps(cleaned.toIntOrNull() ?: 0)
                    },
                    singleLine = true,
                    textStyle = LocalTextStyle.current.copy(
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    cursorBrush = SolidColor(EmeraldNeon),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    decorationBox = { inner ->
                        if (repsText.isEmpty()) {
                            Text("0", color = TextMuted, fontSize = 14.sp)
                        }
                        inner()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Completion Check Box
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (set.isCompleted) EmeraldNeon else DarkBackground)
                    .border(
                        width = 1.dp,
                        color = if (set.isCompleted) EmeraldNeon else BorderSubtle,
                        shape = RoundedCornerShape(8.dp)
                    )
                    .clickable(onClick = onToggleComplete),
                contentAlignment = Alignment.Center
            ) {
                if (set.isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = Color.Black,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val prevLabel = if (hasPrev) {
                "Ранее: ${formatCleanWeight(prevDisplayedW)} × ${set.previousReps} $unitLabel / повт."
            } else {
                "Ранее: —"
            }
            Text(
                text = prevLabel,
                fontSize = 12.sp,
                color = if (hasPrev) TextSecondary else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable(enabled = hasPrev) { onApplyPrevious() }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "История",
                    fontSize = 11.sp,
                    color = EmeraldNeon,
                    modifier = Modifier.clickable(onClick = onOpenHistory)
                )
                if (canDelete) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Удалить подход",
                        tint = TextMuted,
                        modifier = Modifier
                            .size(16.dp)
                            .clickable(onClick = onDelete)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WorkoutRestModalSheet(
    secondsRemaining: Int,
    totalSeconds: Int,
    restPresetSeconds: Int,
    workout: Workout,
    isKgUnit: Boolean,
    onAddSeconds: (Int) -> Unit,
    onSkip: () -> Unit,
    onCyclePreset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalDudaStrings.current
    val unitLabel = if (isKgUnit) strings.unitKg else strings.unitLbs
    val lastCompleted = remember(workout) { findLastCompletedSetInfo(workout) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "01 / ОТДЫХ",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = EmeraldNeon,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Время восстановиться",
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = formatCountdown(secondsRemaining),
                fontSize = 40.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Осталось $secondsRemaining секунд",
                fontSize = 13.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(12.dp))
            ObsidianProgressBar(
                progress = (secondsRemaining.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                color = EmeraldNeon
            )

            if (lastCompleted != null) {
                Spacer(modifier = Modifier.height(16.dp))
                ObsidianDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = lastCompleted.exerciseName,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary
                )
                val lw = if (isKgUnit) lastCompleted.weightKg else StrengthCalculator.kgToLbs(lastCompleted.weightKg)
                val lv = lw * lastCompleted.reps
                Text(
                    text = "Последний подход · ${formatCleanWeight(lw)} $unitLabel × ${lastCompleted.reps}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${formatRoundedInt(lv)} $unitLabel",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "объём последнего подхода",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ObsidianOutlineButton(
                    text = "−15 сек",
                    onClick = { onAddSeconds(-15) }
                )
                ObsidianOutlineButton(
                    text = "+30 сек",
                    onClick = { onAddSeconds(30) }
                )
                ObsidianOutlineButton(
                    text = "Пропустить",
                    onClick = onSkip
                )
                ObsidianOutlineButton(
                    text = "Настроить отдых (${formatCountdown(restPresetSeconds)})",
                    onClick = onCyclePreset
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WorkoutFinishModalSheet(
    stage: FinishSheetStage,
    workout: Workout,
    startTimeEpochMs: Long,
    isKgUnit: Boolean,
    isFinishing: Boolean,
    onProceedToSummary: () -> Unit,
    onConfirmSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalDudaStrings.current
    val unitLabel = if (isKgUnit) strings.unitKg else strings.unitLbs

    val completedSetsCount = remember(workout) {
        workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    }
    val exactVolumeKg = remember(workout) {
        workout.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps } }
    }
    val displayedExactVolume = if (isKgUnit) exactVolumeKg else StrengthCalculator.kgToLbs(exactVolumeKg)
    val tonnageStr = if (isKgUnit && exactVolumeKg >= 1000.0) {
        val tons = round((exactVolumeKg / 1000.0) * 100.0) / 100.0
        "${tons.toString().replace('.', ',')} т"
    } else {
        "${formatRoundedInt(displayedExactVolume)} $unitLabel"
    }

    val durationSeconds = remember(workout, startTimeEpochMs) {
        if (workout.durationSeconds > 0L) workout.durationSeconds
        else {
            val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            val start = if (startTimeEpochMs > 0L) startTimeEpochMs else now
            ((now - start) / 1000L).coerceAtLeast(1L)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            when (stage) {
                FinishSheetStage.CONFIRM_INCOMPLETE -> {
                    Text(
                        text = "03 / ПОДТВЕРЖДЕНИЕ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Есть незавершённые подходы",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Вернитесь к тренировке или сохраните только отмеченные подходы.",
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(18.dp))
                    ObsidianDivider()
                    Spacer(modifier = Modifier.height(18.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianPrimaryButton(
                            text = "Вернуться к тренировке",
                            onClick = onDismiss
                        )
                        ObsidianOutlineButton(
                            text = "Сохранить только отмеченные",
                            onClick = onProceedToSummary
                        )
                    }
                }

                FinishSheetStage.SUMMARY -> {
                    Text(
                        text = "02 / ЗАВЕРШЕНИЕ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldNeon,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Хорошая работа",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${workout.exercises.size} упражнения · $completedSetsCount подходов",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ОБЪЁМ",
                                fontSize = 11.sp,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tonnageStr,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ВРЕМЯ",
                                fontSize = 11.sp,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCountdown(durationSeconds.toInt()),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Точный объём · ${formatRoundedInt(displayedExactVolume)} $unitLabel",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )

                    if (workout.exercises.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        ObsidianDivider()
                        workout.exercises.forEachIndexed { idx, ex ->
                            val exCompleted = ex.sets.count { it.isCompleted }
                            val exVol = ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
                            val exDispVol = if (isKgUnit) exVol else StrengthCalculator.kgToLbs(exVol)
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp)
                            ) {
                                Text(
                                    text = ex.exerciseName.ifBlank { ex.exercise.name },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$exCompleted подхода · ${formatRoundedInt(exDispVol)} $unitLabel",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            if (idx < workout.exercises.lastIndex) {
                                ObsidianDivider()
                            }
                        }
                        ObsidianDivider()
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianPrimaryButton(
                            text = if (isFinishing) "Сохранение..." else strings.save,
                            onClick = onConfirmSave,
                            enabled = !isFinishing
                        )
                        ObsidianOutlineButton(
                            text = "Вернуться к тренировке",
                            onClick = onDismiss
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun WorkoutDiscardModalSheet(
    onConfirmDiscard: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalDudaStrings.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
        ) {
            Text(
                text = "04 / СБРОС",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted,
                letterSpacing = 0.8.sp
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.workoutDiscardConfirmTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = strings.workoutDiscardConfirmText,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(18.dp))
            ObsidianDivider()
            Spacer(modifier = Modifier.height(18.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ObsidianOutlineButton(
                    text = strings.cancel,
                    onClick = onDismiss
                )
                ObsidianWhiteButton(
                    text = "Сбросить тренировку",
                    onClick = onConfirmDiscard
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private data class CompletedSetBrief(
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int
)

private fun findLastCompletedSetInfo(workout: Workout): CompletedSetBrief? {
    for (ex in workout.exercises.asReversed()) {
        val last = ex.sets.asReversed().firstOrNull { it.isCompleted && it.weightKg > 0 && it.reps > 0 }
        if (last != null) {
            return CompletedSetBrief(
                exerciseName = ex.exerciseName.ifBlank { ex.exercise.name },
                weightKg = last.weightKg,
                reps = last.reps
            )
        }
    }
    return null
}

private fun formatElapsedShort(seconds: Long): String {
    val safe = seconds.coerceAtLeast(0L)
    val h = safe / 3600L
    val m = (safe % 3600L) / 60L
    val s = safe % 60L
    return if (h > 0) {
        "$h:${m.toString().padStart(2, '0')}:${s.toString().padStart(2, '0')}"
    } else {
        "$m:${s.toString().padStart(2, '0')}"
    }
}

private fun formatCountdown(seconds: Int): String {
    val safe = seconds.coerceAtLeast(0)
    val m = safe / 60
    val s = safe % 60
    return "$m:${s.toString().padStart(2, '0')}"
}

private fun formatCleanWeight(weight: Double): String {
    val rounded = round(weight * 10.0) / 10.0
    return if (rounded % 1.0 == 0.0) {
        rounded.toInt().toString()
    } else {
        rounded.toString()
    }
}

private fun formatRoundedInt(value: Double): String {
    val intVal = round(value).toLong()
    val raw = intVal.toString()
    if (raw.length <= 3) return raw
    val sb = StringBuilder()
    var count = 0
    for (i in raw.length - 1 downTo 0) {
        sb.append(raw[i])
        count++
        if (count % 3 == 0 && i > 0) {
            sb.append(' ')
        }
    }
    return sb.reverse().toString()
}
