package dev.neverpaid.health.features.workout

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeDark
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderLilac
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.EmeraldBg
import dev.neverpaid.health.core.designsystem.EmeraldBorder
import dev.neverpaid.health.core.designsystem.EmeraldSuccess
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExercisePickerBottomSheet
import dev.neverpaid.health.core.designsystem.ExerciseThumbnail
import dev.neverpaid.health.core.designsystem.ExerciseThumbnailSize
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymEmptyCard
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymKicker
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.GymWhiteButton
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.InteractivePlateCalculator
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.RoyalWell
import dev.neverpaid.health.core.designsystem.SetTypeBadge
import dev.neverpaid.health.core.designsystem.SurfaceLilac
import dev.neverpaid.health.core.designsystem.SurfaceWhite
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.designsystem.VioletPrimary
import dev.neverpaid.health.core.designsystem.localizeMuscleName
import dev.neverpaid.health.core.i18n.LocalDudaStrings
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

    LaunchedEffect(state.isRestTimerActive, state.restTimerSecondsRemaining, state.restTimerTotalSeconds) {
        if (state.isRestTimerActive) {
            restSheetTotalSeconds = maxOf(state.restTimerTotalSeconds, state.restTimerSecondsRemaining, 1)
        }
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner, viewModel) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.onAction(WorkoutUiAction.OnSyncRestTimer)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutUiEffect.WorkoutFinishedSummary -> completedSummaryWorkout = effect.workout
            WorkoutUiEffect.NavigateBack -> onNavigateBack()
            WorkoutUiEffect.TriggerHaptic -> PlatformBridge.vibrate()
            is WorkoutUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
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
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = { SheetDragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding()
            ) {
                Text(
                    text = LocalDudaStrings.current.plateCalculatorTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
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
    val lang = when (strings.navWorkouts) {
        "Mashg'ulotlar" -> "uz"
        "Workouts" -> "en"
        else -> "ru"
    }
    val workout = state.workout
    var showAllExercises by remember(workout?.id) { mutableStateOf(false) }
    var prevExerciseCount by remember(workout?.id) { mutableIntStateOf(workout?.exercises?.size ?: 0) }
    var showSessionNoteEditor by remember { mutableStateOf(false) }
    var sessionTitleDraft by remember(workout?.id, workout?.title) { mutableStateOf(workout?.title ?: "") }
    var sessionNotesDraft by remember(workout?.id) { mutableStateOf(workout?.notes ?: "") }

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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                GymPageHeader(
                    title = strings.navWorkouts,
                    subtitle = "Нет активной сессии"
                )
                Spacer(modifier = Modifier.height(20.dp))
                GymEmptyCard(
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
    val totalSetsCount = remember(workout) {
        workout.exercises.sumOf { it.sets.size }
    }
    val completedSetsCount = remember(workout) {
        workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    }
    val setsProgress = if (totalSetsCount > 0) {
        (completedSetsCount.toFloat() / totalSetsCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Top workout header card: Royal Violet Hero Card with clean bold workout title,
            // big heavy tabular live elapsed timer, emerald progress bar, and crisp white actions
            item(key = "session_summary_card", contentType = "summary") {
                RoyalHeroCard(bottomExtraPadding = 20.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showSessionNoteEditor = !showSessionNoteEditor }
                        ) {
                            Text(
                                text = workout.title.ifBlank {
                                    when (lang) {
                                        "uz" -> "Erkin mashg'ulot"
                                        "en" -> "Freestyle Workout"
                                        else -> "Свободная тренировка"
                                    }
                                },
                                fontSize = 23.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.6).sp,
                                lineHeight = 27.sp,
                                color = InkWhite,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = when {
                                    showSessionNoteEditor -> when (lang) {
                                        "uz" -> "Yopish"
                                        "en" -> "Hide editor"
                                        else -> "Скрыть название"
                                    }
                                    else -> when (lang) {
                                        "uz" -> "Nomini o'zgartirish"
                                        "en" -> "Edit title & notes"
                                        else -> "Изменить название"
                                    }
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeroMuted
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Text(
                            text = formatElapsedShort(elapsedSeconds),
                            fontSize = 36.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.2).sp,
                            color = InkWhite,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    // Emerald progress bar of completed sets + compact live stats strip
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "$completedSetsCount / $totalSetsCount yondashuv · ${workout.exercises.size} mashq"
                                    "en" -> "$completedSetsCount of $totalSetsCount sets · ${workout.exercises.size} ex."
                                    else -> "$completedSetsCount из $totalSetsCount подходов · ${workout.exercises.size} упр."
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeroMuted,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "${formatRoundedInt(displayTotalVolume)} $unitLabel",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = InkWhite,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                        GymProgressBar(
                            progress = setsProgress,
                            heightDp = 7,
                            color = EmeraldSuccess,
                            trackColor = InkWhite.copy(alpha = 0.22f)
                        )
                    }

                    // Primary crisp white Finish pill, translucent Comment & quiet Discard pills
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(InkWhite)
                                .clickable(onClick = onRequestFinish),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.workoutFinish,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VioletPrimary,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    if (workout.notes.isNotBlank() || showSessionNoteEditor) {
                                        InkWhite.copy(alpha = 0.24f)
                                    } else {
                                        InkWhite.copy(alpha = 0.14f)
                                    }
                                )
                                .clickable { showSessionNoteEditor = !showSessionNoteEditor }
                                .padding(horizontal = 14.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> if (workout.notes.isNotBlank()) "Izoh •" else "Izoh"
                                    "en" -> if (workout.notes.isNotBlank()) "Note •" else "Note"
                                    else -> if (workout.notes.isNotBlank()) "Коммент •" else "Коммент"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkWhite,
                                maxLines = 1
                            )
                        }
                        Box(
                            modifier = Modifier
                                .height(46.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(InkWhite.copy(alpha = 0.10f))
                                .clickable(onClick = onRequestDiscard)
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = strings.workoutDiscard,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = HeroMuted,
                                maxLines = 1
                            )
                        }
                    }

                    if (workout.notes.isNotBlank() && !showSessionNoteEditor) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(InkWhite.copy(alpha = 0.14f))
                                .clickable { showSessionNoteEditor = true }
                                .padding(12.dp)
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Mashg'ulot izohi"
                                    "en" -> "Workout note"
                                    else -> "Комментарий к тренировке"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = HeroMuted
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = workout.notes,
                                fontSize = 13.sp,
                                color = InkWhite
                            )
                        }
                    }

                    AnimatedVisibility(visible = showSessionNoteEditor) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(SurfaceWhite)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Mashg'ulot izohi"
                                    "en" -> "Workout notes"
                                    else -> "Комментарий к тренировке"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = VioletPrimary
                            )
                            GymTextField(
                                value = sessionNotesDraft,
                                onValueChange = {
                                    sessionNotesDraft = it
                                    onAction(WorkoutUiAction.OnUpdateWorkoutNotes(it))
                                },
                                placeholder = when (lang) {
                                    "uz" -> "Holat, ishchi og'irliklar, eslatma..."
                                    "en" -> "Feeling, working weights, cue for next time..."
                                    else -> "Самочувствие, рабочие веса, заметка на следующий раз..."
                                },
                                singleLine = false
                            )
                            Text(
                                text = when (lang) {
                                    "uz" -> "Mashg'ulot nomi"
                                    "en" -> "Workout title"
                                    else -> "Название тренировки"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            GymTextField(
                                value = sessionTitleDraft,
                                onValueChange = {
                                    sessionTitleDraft = it
                                    onAction(WorkoutUiAction.OnUpdateWorkoutTitle(it))
                                },
                                placeholder = "Push Hypertrophy"
                            )
                        }
                    }
                }
            }

            // Inline Active Rest Card when countdown runs
            if (state.isRestTimerActive && state.restTimerSecondsRemaining > 0) {
                item(key = "active_rest_card", contentType = "rest") {
                    val lastCompleted = remember(workout) { findLastCompletedSetInfo(workout) }
                    GymCard(backgroundColor = DarkSurfaceElevated) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                GymKicker(text = "Отдых")
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Время восстановиться",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                            Text(
                                text = formatCountdown(state.restTimerSecondsRemaining),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp,
                                color = AccentLime,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        val progress = (state.restTimerSecondsRemaining.toFloat() / restTotalSeconds.coerceAtLeast(1).toFloat())
                            .coerceIn(0f, 1f)
                        GymProgressBar(
                            progress = progress,
                            heightDp = 6,
                            color = AccentLime
                        )

                        if (lastCompleted != null) {
                            val lw = if (state.isKgUnit) lastCompleted.weightKg else StrengthCalculator.kgToLbs(lastCompleted.weightKg)
                            Text(
                                text = "${lastCompleted.exerciseName} · ${formatCleanWeight(lw)} $unitLabel × ${lastCompleted.reps}",
                                fontSize = 13.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            WorkoutUtilityChip(
                                text = "−15с",
                                onClick = { onAction(WorkoutUiAction.OnRestTimerAddSeconds(-15)) },
                                backgroundColor = DarkSurface,
                                textColor = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            WorkoutUtilityChip(
                                text = "+30с",
                                onClick = { onAction(WorkoutUiAction.OnRestTimerAddSeconds(30)) },
                                backgroundColor = DarkSurface,
                                textColor = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            WorkoutUtilityChip(
                                text = formatCountdown(restPresetSeconds),
                                onClick = onCycleRestPreset,
                                backgroundColor = DarkSurface,
                                textColor = TextSecondary,
                                modifier = Modifier.weight(1f)
                            )
                            WorkoutUtilityChip(
                                text = "Сброс",
                                onClick = { onAction(WorkoutUiAction.OnRestTimerSkip) },
                                backgroundColor = AccentLime,
                                textColor = GymInkOnLight,
                                modifier = Modifier.weight(1.1f)
                            )
                        }
                    }
                }
            }

            if (workout.exercises.isEmpty()) {
                item(key = "empty_exercises_card", contentType = "empty") {
                    GymEmptyCard(
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
                ) { _, exercise ->
                    val prevNoteFromApi = state.previousNotesMap[exercise.exerciseId]?.note
                        ?: state.previousNotesMap[exercise.exerciseName]?.note
                        ?: state.previousNotesMap[exercise.exerciseName.lowercase()]?.note
                    val prevNoteFromHistory = remember(state.workoutHistory, exercise.exerciseId, exercise.exerciseName) {
                        state.workoutHistory.firstNotNullOfOrNull { w ->
                            w.exercises.find {
                                it.exerciseId == exercise.exerciseId ||
                                    it.exerciseName.equals(exercise.exerciseName, ignoreCase = true)
                            }?.notes?.trim()?.takeIf { it.isNotEmpty() }
                        }
                    }
                    val effectivePreviousNote = prevNoteFromApi?.takeIf { it.isNotBlank() } ?: prevNoteFromHistory

                    WorkoutExerciseCard(
                        exercise = exercise,
                        previousNote = effectivePreviousNote,
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
                        GymCard {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Ещё $remaining упр.",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextPrimary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Откройте весь список сессии",
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                GymOutlineButton(
                                    text = "Показать ($remaining)",
                                    onClick = { showAllExercises = true },
                                    textColor = AccentLime
                                )
                            }
                        }
                    }
                }

                item(key = "add_exercise_footer", contentType = "footer") {
                    GymOutlineButton(
                        text = "+ ${strings.workoutAddExercise}",
                        onClick = { onAction(WorkoutUiAction.OnOpenExercisePicker) },
                        fillWidth = true,
                        textColor = AccentLime
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    previousNote: String? = null,
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
    var actionsExpanded by remember { mutableStateOf(false) }
    var noteDraft by remember(exercise.id, exercise.notes) { mutableStateOf(exercise.notes) }

    val muscleLabel = localizeMuscleName(exercise.primaryMuscleGroup)
    val plateWeightKg = activeSet?.weightKg?.takeIf { it > 0 }
        ?: exercise.sets.lastOrNull { it.weightKg > 0 }?.weightKg
        ?: 60.0
    val supportsPlates = exercise.equipment.lowercase() in setOf("barbell", "ez_bar", "smith_machine", "machine")

    GymCard {
        // 1. Clean Exercise Header: ExerciseThumbnail (MD) + bold exercise name + muted muscle label + single-line PR/technique link
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ExerciseThumbnail(
                exerciseName = exercise.exerciseName.ifBlank { exercise.exercise.name },
                sourceId = exercise.exercise.sourceId,
                exerciseId = exercise.exerciseId,
                fallbackMuscle = exercise.primaryMuscleGroup,
                size = ExerciseThumbnailSize.MD,
                onClick = onInspect
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = exercise.exerciseName.ifBlank { exercise.exercise.name },
                    fontSize = 19.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    lineHeight = 23.sp,
                    color = TextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onInspect)
                )
                Text(
                    text = muscleLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (prRecord != null) {
                    val prW = if (isKgUnit) prRecord.weightKg else StrengthCalculator.kgToLbs(prRecord.weightKg)
                    Text(
                        text = "Рекорд ${formatCleanWeight(prW)} $unitLabel × ${prRecord.reps} · Техника →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarm,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        modifier = Modifier.clickable(onClick = onInspect)
                    )
                } else {
                    Text(
                        text = "Техника и инструкции →",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable(onClick = onInspect)
                    )
                }
            }
        }

        // 2. Minimalist Secondary Actions Strip ([Comment] + [...] utility toggle)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            WorkoutUtilityChip(
                text = if (exercise.notes.isNotBlank() || !previousNote.isNullOrBlank()) "Комментарий •" else "Комментарий",
                onClick = {
                    noteExpanded = true
                    editingNotes = !editingNotes
                },
                backgroundColor = if (exercise.notes.isNotBlank() || !previousNote.isNullOrBlank() || editingNotes) AccentLimeSubtle else DarkSurfaceHigh,
                textColor = if (exercise.notes.isNotBlank() || !previousNote.isNullOrBlank() || editingNotes) AccentLime else TextSecondary
            )
            WorkoutUtilityChip(
                text = if (actionsExpanded) "Скрыть ▴" else "Ещё ···",
                onClick = { actionsExpanded = !actionsExpanded },
                backgroundColor = if (actionsExpanded) AccentLimeSubtle else DarkSurfaceHigh,
                textColor = if (actionsExpanded) AccentLime else TextSecondary
            )
        }

        AnimatedVisibility(visible = actionsExpanded) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                WorkoutUtilityChip(
                    text = "Разминка",
                    onClick = {
                        actionsExpanded = false
                        onOpenWarmup()
                    },
                    textColor = TextSecondary
                )
                if (supportsPlates) {
                    WorkoutUtilityChip(
                        text = "Блины",
                        onClick = {
                            actionsExpanded = false
                            onOpenPlates(activeSet?.id ?: "", plateWeightKg)
                        },
                        textColor = TextSecondary
                    )
                }
                WorkoutUtilityChip(
                    text = "Заменить",
                    onClick = {
                        actionsExpanded = false
                        onSwap()
                    },
                    textColor = TextSecondary
                )
                WorkoutUtilityChip(
                    text = "Убрать",
                    onClick = {
                        actionsExpanded = false
                        onRemove()
                    },
                    textColor = CrimsonRed
                )
            }
        }

        // Previous Workout Note + Current Exercise Comment Editor / Viewer
        if (!previousNote.isNullOrBlank() || exercise.notes.isNotBlank() || editingNotes) {
            GymInnerWell {
                if (!previousNote.isNullOrBlank()) {
                    Text(
                        text = "Комментарий с прошлой тренировки",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarm
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = previousNote,
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        color = TextPrimary
                    )
                    if (exercise.notes.isNotBlank() || editingNotes) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GymDivider()
                        Spacer(modifier = Modifier.height(10.dp))
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        WorkoutUtilityChip(
                            text = "Оставить комментарий на следующую тренировку",
                            onClick = {
                                noteExpanded = true
                                editingNotes = true
                            },
                            backgroundColor = GymWell,
                            textColor = AccentLime
                        )
                    }
                }

                if (exercise.notes.isNotBlank() || editingNotes) {
                    Text(
                        text = "Комментарий к упражнению",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentLime
                    )
                    if (editingNotes) {
                        Spacer(modifier = Modifier.height(8.dp))
                        GymTextField(
                            value = noteDraft,
                            onValueChange = {
                                noteDraft = it
                                onUpdateNotes(it)
                            },
                            placeholder = "Пауза 1 сек в нижней точке, лопатки сведены...",
                            singleLine = false
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GymPrimaryButton(
                                text = strings.save,
                                onClick = {
                                    onUpdateNotes(noteDraft.trim())
                                    editingNotes = false
                                    noteExpanded = noteDraft.isNotBlank()
                                }
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = {
                                    editingNotes = false
                                }
                            )
                        }
                    } else {
                        if (noteExpanded && exercise.notes.isNotBlank()) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = exercise.notes,
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = TextSoft
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            WorkoutUtilityChip(
                                text = if (noteExpanded) "Свернуть" else "Показать",
                                onClick = { noteExpanded = !noteExpanded },
                                backgroundColor = GymWell,
                                textColor = TextSecondary
                            )
                            WorkoutUtilityChip(
                                text = "Изменить",
                                onClick = {
                                    noteExpanded = true
                                    editingNotes = true
                                },
                                backgroundColor = GymWell,
                                textColor = AccentLime
                            )
                        }
                    }
                }
            }
        }

        // 3. Sets Table (SET / ПРЕД / КГ / ПОВТ / ✓)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "SET",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.width(36.dp)
                )
                Text(
                    text = "ПРЕД",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.95f)
                )
                Text(
                    text = unitLabel.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "ПОВТ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.6.sp,
                    color = TextMuted,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(0.9f)
                )
                Box(
                    modifier = Modifier.width(38.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✓",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                }
            }

            exercise.sets.forEach { set ->
                WorkoutSetRow(
                    set = set,
                    isSelected = set.id == activeSet?.id,
                    isKgUnit = isKgUnit,
                    onSelectSet = { selectedSetId = set.id },
                    onToggleSetType = {
                        selectedSetId = set.id
                        onToggleSetType(set.id)
                    },
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
                    }
                )
            }
        }

        // Active Set Quick Controls (Set Type, Quick Reps, History, Delete Set)
        if (activeSet != null) {
            val setTypeOrder = listOf("normal", "warmup", "drop", "failure")
            val setTypeLabels = listOf("Обычный", "Разминка", "Дроп", "Отказ")
            val currentIdx = setTypeOrder.indexOf(activeSet.setType).coerceAtLeast(0)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(GymWell)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Подход #${activeSet.setNumber}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "История подхода",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLime,
                            modifier = Modifier.clickable {
                                onOpenSetHistory(activeSet.setNumber, activeSet.id)
                            }
                        )
                        if (exercise.sets.size > 1) {
                            Text(
                                text = "Удалить",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CrimsonRed,
                                modifier = Modifier.clickable {
                                    onDeleteSet(activeSet.id)
                                }
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    setTypeOrder.forEachIndexed { targetIdx, typeKey ->
                        val isTypeSelected = activeSet.setType == typeKey
                        WorkoutUtilityChip(
                            text = setTypeLabels[targetIdx],
                            onClick = {
                                val steps = (targetIdx - currentIdx + setTypeOrder.size) % setTypeOrder.size
                                repeat(steps) { onToggleSetType(activeSet.id) }
                            },
                            backgroundColor = if (isTypeSelected) AccentLime else DarkSurfaceHigh,
                            textColor = if (isTypeSelected) GymInkOnLight else TextSecondary
                        )
                    }
                    listOf(8, 6, 4).forEach { repsPreset ->
                        val isPresetSelected = activeSet.reps == repsPreset
                        WorkoutUtilityChip(
                            text = "${repsPreset}×",
                            onClick = { onUpdateReps(activeSet.id, repsPreset) },
                            backgroundColor = if (isPresetSelected) AccentLimeSubtle else DarkSurfaceHigh,
                            textColor = if (isPresetSelected) AccentLime else TextMuted
                        )
                    }
                }
            }
        }

        // Primary Footer Actions: + Подход & Отдых
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymOutlineButton(
                text = "+ Подход",
                onClick = onAddSet,
                textColor = AccentLime,
                modifier = Modifier.weight(1f)
            )
            GymOutlineButton(
                text = "Отдых ${formatCountdown(restPresetSeconds)}",
                onClick = onOpenRest,
                textColor = TextSecondary
            )
        }
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    isSelected: Boolean,
    isKgUnit: Boolean,
    onSelectSet: () -> Unit,
    onToggleSetType: () -> Unit,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onApplyPrevious: () -> Unit,
    onOpenHistory: () -> Unit,
    onToggleComplete: () -> Unit
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

    var weightFocused by remember { mutableStateOf(false) }
    var repsFocused by remember { mutableStateOf(false) }

    val hasPrev = set.previousWeightKg != null && set.previousReps != null
    val prevDisplayedW = if (set.previousWeightKg != null) {
        if (isKgUnit) set.previousWeightKg else StrengthCalculator.kgToLbs(set.previousWeightKg)
    } else 0.0

    val rowBackground = when {
        set.isCompleted -> EmeraldBg
        isSelected -> SurfaceLilac
        else -> Color.Transparent
    }
    val inputBackground = if (set.isCompleted) SurfaceWhite else RoyalWell

    val weightBorder = when {
        weightFocused -> VioletPrimary
        set.isCompleted -> EmeraldBorder
        else -> BorderLilac
    }
    val repsBorder = when {
        repsFocused -> VioletPrimary
        set.isCompleted -> EmeraldBorder
        else -> BorderLilac
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(rowBackground)
            .clickable(onClick = onSelectSet)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 1. SET column (SetTypeBadge)
        SetTypeBadge(
            type = set.setType,
            setIndex = set.setNumber,
            onClick = onToggleSetType
        )

        // 2. ПРЕД column (Previous weight x reps or history trigger)
        Box(
            modifier = Modifier
                .weight(0.95f)
                .height(40.dp)
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                    onSelectSet()
                    if (hasPrev) {
                        onApplyPrevious()
                    } else {
                        onOpenHistory()
                    }
                }
                .padding(horizontal = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasPrev) {
                    "${formatCleanWeight(prevDisplayedW)}×${set.previousReps}"
                } else {
                    "—"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = if (hasPrev) TextSecondary else TextMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }

        // 3. КГ / LBS column (Compact rounded filled numeric input)
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(inputBackground)
                .border(1.dp, weightBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum"
                ),
                cursorBrush = SolidColor(AccentLime),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (weightText.isEmpty()) {
                            Text(
                                text = if (hasPrev) formatCleanWeight(prevDisplayedW) else "0",
                                color = TextMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        weightFocused = focusState.isFocused
                        if (focusState.isFocused) onSelectSet()
                    }
            )
        }

        // 4. ПОВТ column (Compact rounded filled numeric input)
        Box(
            modifier = Modifier
                .weight(0.9f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(inputBackground)
                .border(1.dp, repsBorder, RoundedCornerShape(12.dp))
                .padding(horizontal = 8.dp),
            contentAlignment = Alignment.Center
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
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                    fontFeatureSettings = "tnum"
                ),
                cursorBrush = SolidColor(AccentLime),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                decorationBox = { inner ->
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (repsText.isEmpty()) {
                            Text(
                                text = if (hasPrev) "${set.previousReps}" else "0",
                                color = TextMuted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Center,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { focusState ->
                        repsFocused = focusState.isFocused
                        if (focusState.isFocused) onSelectSet()
                    }
            )
        }

        // 5. ✓ Completion Check Button (Figma Emerald #10B981 + InkWhite when completed)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (set.isCompleted) EmeraldSuccess else SurfaceLilac)
                .clickable(onClick = onToggleComplete),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Mark set complete",
                tint = if (set.isCompleted) InkWhite else TextMuted,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun WorkoutUtilityChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = DarkSurfaceHigh,
    textColor: Color = TextSecondary
) {
    Box(
        modifier = modifier
            .height(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun SheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 4.dp)
            .width(40.dp)
            .height(4.dp)
            .clip(CircleShape)
            .background(DarkSurfaceElevated)
    )
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
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            GymKicker(text = "Отдых")
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Время восстановиться",
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = formatCountdown(secondsRemaining),
                fontSize = 48.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.5).sp,
                color = AccentLime,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Осталось $secondsRemaining секунд",
                fontSize = 13.sp,
                color = TextSecondary,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Spacer(modifier = Modifier.height(14.dp))
            GymProgressBar(
                progress = (secondsRemaining.toFloat() / totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f),
                color = AccentLime
            )

            if (lastCompleted != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceHigh)
                        .padding(14.dp)
                ) {
                    Text(
                        text = lastCompleted.exerciseName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    val lw = if (isKgUnit) lastCompleted.weightKg else StrengthCalculator.kgToLbs(lastCompleted.weightKg)
                    val lv = lw * lastCompleted.reps
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Последний подход · ${formatCleanWeight(lw)} $unitLabel × ${lastCompleted.reps} (${formatRoundedInt(lv)} $unitLabel)",
                        fontSize = 13.sp,
                        color = TextSecondary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GymOutlineButton(
                    text = "−15 сек",
                    onClick = { onAddSeconds(-15) },
                    modifier = Modifier.weight(1f)
                )
                GymOutlineButton(
                    text = "+30 сек",
                    onClick = { onAddSeconds(30) },
                    modifier = Modifier.weight(1f)
                )
                GymOutlineButton(
                    text = formatCountdown(restPresetSeconds),
                    onClick = onCyclePreset,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            GymPrimaryButton(
                text = "Пропустить отдых",
                onClick = onSkip,
                fillWidth = true
            )
            Spacer(modifier = Modifier.height(16.dp))
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
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            when (stage) {
                FinishSheetStage.CONFIRM_INCOMPLETE -> {
                    Text(
                        text = "Есть незавершённые подходы",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary,
                        lineHeight = 28.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Вернитесь к тренировке или сохраните только отмеченные подходы.",
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GymPrimaryButton(
                            text = "Вернуться к тренировке",
                            onClick = onDismiss,
                            fillWidth = true
                        )
                        GymOutlineButton(
                            text = "Сохранить только отмеченные",
                            onClick = onProceedToSummary,
                            fillWidth = true
                        )
                    }
                }

                FinishSheetStage.SUMMARY -> {
                    val lang = when (strings.navWorkouts) {
                        "Mashg'ulotlar" -> "uz"
                        "Workouts" -> "en"
                        else -> "ru"
                    }
                    // Figma Receipt Header: Emerald Checkmark Halo + Kicker + Headline
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(76.dp)
                                .clip(CircleShape)
                                .background(EmeraldBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = InkWhite,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        GymKicker(
                            text = when (lang) {
                                "uz" -> "Mashg'ulot yakunlandi"
                                "en" -> "Session complete"
                                else -> "Тренировка завершена"
                            },
                            color = EmeraldSuccess
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = strings.workoutAwesome,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.6).sp,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (lang) {
                                "uz" -> "${workout.exercises.size} mashq · $completedSetsCount yondashuv"
                                "en" -> "${workout.exercises.size} exercises · $completedSetsCount sets"
                                else -> "${workout.exercises.size} упражнения · $completedSetsCount подходов"
                            },
                            fontSize = 14.sp,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(RoyalWell)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = strings.workoutTotalVolume.uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = tonnageStr,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = VioletPrimary,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(RoyalWell)
                                .padding(14.dp)
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "VAQT"
                                    "en" -> "DURATION"
                                    else -> "ВРЕМЯ"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formatCountdown(durationSeconds.toInt()),
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    if (workout.exercises.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(RoyalWell)
                                .padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            workout.exercises.forEachIndexed { idx, ex ->
                                val exCompleted = ex.sets.count { it.isCompleted }
                                val exVol = ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
                                val exDispVol = if (isKgUnit) exVol else StrengthCalculator.kgToLbs(exVol)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = ex.exerciseName.ifBlank { ex.exercise.name },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "$exCompleted · ${formatRoundedInt(exDispVol)} $unitLabel",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextSecondary,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                                if (idx < workout.exercises.lastIndex) {
                                    GymDivider()
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        GymPrimaryButton(
                            text = if (isFinishing) {
                                when (lang) {
                                    "uz" -> "Saqlanmoqda..."
                                    "en" -> "Saving..."
                                    else -> "Сохранение..."
                                }
                            } else {
                                strings.save
                            },
                            onClick = onConfirmSave,
                            enabled = !isFinishing,
                            fillWidth = true
                        )
                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Mashg'ulotga qaytish"
                                "en" -> "Back to workout"
                                else -> "Вернуться к тренировке"
                            },
                            onClick = onDismiss,
                            fillWidth = true
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = { SheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .navigationBarsPadding()
        ) {
            Text(
                text = strings.workoutDiscardConfirmTitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = strings.workoutDiscardConfirmText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GymOutlineButton(
                    text = strings.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(50.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(CrimsonRed)
                        .clickable(onClick = onConfirmDiscard),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.workoutDiscard,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = InkWhite
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
