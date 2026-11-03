package dev.neverpaid.health.features.workout

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExercisePickerBottomSheet
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
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
import kotlinx.coroutines.launch
import kotlin.math.round

@Composable
fun WorkoutRoute(
    viewModel: WorkoutViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    var showDiscardDialog by remember { mutableStateOf(false) }
    var warmupExercise by remember { mutableStateOf<WorkoutExercise?>(null) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutUiEffect.WorkoutFinishedSummary -> onNavigateBack()
            WorkoutUiEffect.NavigateBack -> onNavigateBack()
            WorkoutUiEffect.TriggerHaptic -> PlatformBridge.vibrate()
            is WorkoutUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    if (showDiscardDialog) {
        AlertDialog(
            onDismissRequest = { showDiscardDialog = false },
            title = { Text("Отменить тренировку?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Все записанные подходы в текущей сессии будут удалены без сохранения.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDiscardDialog = false
                        viewModel.onAction(WorkoutUiAction.OnDiscardWorkoutClick)
                    }
                ) {
                    Text("Сбросить", color = CrimsonRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDiscardDialog = false }) {
                    Text("Отмена", color = TextSecondary)
                }
            },
            containerColor = DarkSurfaceVariant
        )
    }

    if (state.showExercisePicker) {
        ExercisePickerBottomSheet(
            exercises = state.availableExercises,
            title = if (state.swappingExerciseId != null) "Заменить упражнение" else "Добавить упражнение",
            onSelectExercise = { viewModel.onAction(WorkoutUiAction.OnSelectExerciseToAdd(it)) },
            onInspectExercise = { viewModel.onAction(WorkoutUiAction.OnInspectExercise(it)) },
            onDismiss = { viewModel.onAction(WorkoutUiAction.OnCloseExercisePicker) }
        )
    }

    if (state.inspectedExercise != null) {
        ExerciseInfoBottomSheet(
            exercise = state.inspectedExercise!!,
            onDismiss = { viewModel.onAction(WorkoutUiAction.OnInspectExercise(null)) }
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
                sets.forEach { (w, r) ->
                    viewModel.onAction(WorkoutUiAction.OnAddSet(exId))
                }
                warmupExercise = null
            }
        )
    }

    if (state.historySheetExerciseId != null && state.historySheetExerciseName != null) {
        val exerciseHistory = remember(state.workoutHistory, state.historySheetExerciseId, state.historySheetExerciseName) {
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
            },
            onDismiss = {
                viewModel.onAction(WorkoutUiAction.OnCloseSetHistorySheet)
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            ActiveWorkoutTopBar(
                title = state.workout?.title ?: "Тренировка",
                startTimeEpochMs = state.workoutStartTimeEpochMs,
                totalVolumeKg = state.workout?.totalVolumeKg ?: 0.0,
                exercisesCount = state.workout?.exercises?.size ?: 0,
                isKgUnit = state.isKgUnit,
                onMinimizeClick = onNavigateBack,
                onDiscardClick = { showDiscardDialog = true },
                onFinishClick = { viewModel.onAction(WorkoutUiAction.OnFinishWorkoutClick) },
                onStartRestTimer = { viewModel.onAction(WorkoutUiAction.OnStartManualRestTimer(90)) }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isRestTimerActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                EnhancedRestTimerBar(
                    secondsRemaining = state.restTimerSecondsRemaining,
                    onAddSeconds = { viewModel.onAction(WorkoutUiAction.OnRestTimerAddSeconds(it)) },
                    onSkip = { viewModel.onAction(WorkoutUiAction.OnRestTimerSkip) }
                )
            }
        }
    ) { innerPadding ->
        WorkoutScreen(
            state = state,
            onAction = viewModel::onAction,
            onRequestDiscard = { showDiscardDialog = true },
            onOpenWarmup = { warmupExercise = it },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onAction: (WorkoutUiAction) -> Unit,
    onRequestDiscard: () -> Unit = {},
    onOpenWarmup: (WorkoutExercise) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val workout = state.workout

    if (workout == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "Нет активной тренировки", color = TextSecondary)
        }
        return
    }

    val completedSetsCount = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "volume_banner") {
            Spacer(modifier = Modifier.height(6.dp))
            AthleticCard(
                backgroundColor = DarkSurfaceVariant,
                borderColor = BorderHighlight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCounter(
                        title = "Объём",
                        value = StrengthCalculator.formatVolume(workout.totalVolumeKg),
                        unit = if (state.isKgUnit) "кг" else "lb",
                        accentColor = EmeraldNeon
                    )
                    StatCounter(
                        title = "Упражнения",
                        value = "${workout.exercises.size}",
                        accentColor = IndigoElectric
                    )
                    StatCounter(
                        title = "Подходы",
                        value = "$completedSetsCount",
                        unit = "вып.",
                        accentColor = AmberWarm
                    )
                }
            }
        }

        items(
            items = workout.exercises,
            key = { it.id.ifBlank { it.exerciseId } }
        ) { exerciseItem ->
            WorkoutExerciseCard(
                exercise = exerciseItem,
                workoutHistory = state.workoutHistory,
                unitLabel = if (state.isKgUnit) "КГ" else "LB",
                isKgUnit = state.isKgUnit,
                onOpenWarmup = { onOpenWarmup(exerciseItem) },
                onUpdateWeight = { setId, w ->
                    onAction(WorkoutUiAction.OnUpdateWeight(exerciseItem.exerciseId, setId, w))
                },
                onUpdateReps = { setId, r ->
                    onAction(WorkoutUiAction.OnUpdateReps(exerciseItem.exerciseId, setId, r))
                },
                onApplyPrevious = { setId ->
                    onAction(WorkoutUiAction.OnApplyPreviousSet(exerciseItem.exerciseId, setId))
                },
                onApplyValues = { setId, w, r ->
                    onAction(WorkoutUiAction.OnApplySetValues(exerciseItem.exerciseId, setId, w, r))
                },
                onOpenHistoryMenu = { setNum, setId ->
                    onAction(
                        WorkoutUiAction.OnOpenSetHistorySheet(
                            exerciseId = exerciseItem.exerciseId,
                            exerciseName = exerciseItem.exerciseName,
                            setNumber = setNum,
                            setId = setId
                        )
                    )
                },
                onToggleComplete = { setId ->
                    onAction(WorkoutUiAction.OnToggleSetComplete(exerciseItem.exerciseId, setId))
                },
                onToggleSetType = { setId ->
                    onAction(WorkoutUiAction.OnToggleSetType(exerciseItem.exerciseId, setId))
                },
                onAddSet = { onAction(WorkoutUiAction.OnAddSet(exerciseItem.exerciseId)) },
                onDeleteSet = { setId ->
                    onAction(WorkoutUiAction.OnDeleteSet(exerciseItem.exerciseId, setId))
                },
                onUpdateNotes = { notes ->
                    onAction(WorkoutUiAction.OnUpdateExerciseNotes(exerciseItem.exerciseId, notes))
                },
                onInspectExercise = {
                    onAction(WorkoutUiAction.OnInspectExercise(exerciseItem.exercise))
                },
                onSwapExercise = {
                    onAction(WorkoutUiAction.OnOpenSwapExercisePicker(exerciseItem.exerciseId))
                },
                onRemoveExercise = {
                    onAction(WorkoutUiAction.OnRemoveExercise(exerciseItem.exerciseId))
                }
            )
        }

        item(key = "add_exercise_button") {
            SecondaryAthleticButton(
                text = "+ Добавить упражнение",
                onClick = { onAction(WorkoutUiAction.OnOpenExercisePicker) }
            )
        }

        item(key = "discard_workout_button") {
            OutlinedButton(
                onClick = onRequestDiscard,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Отменить тренировку",
                    color = CrimsonRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(96.dp))
        }
    }
}

@Composable
private fun ActiveWorkoutTopBar(
    title: String,
    startTimeEpochMs: Long,
    totalVolumeKg: Double,
    exercisesCount: Int,
    isKgUnit: Boolean,
    onMinimizeClick: () -> Unit,
    onDiscardClick: () -> Unit,
    onFinishClick: () -> Unit,
    onStartRestTimer: () -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(0L) }

    LaunchedEffect(startTimeEpochMs) {
        while (true) {
            val now = PlatformBridge.currentTimeMillis()
            elapsedSeconds = ((now - startTimeEpochMs) / 1000).coerceAtLeast(0)
            delay(1000)
        }
    }

    val hrs = elapsedSeconds / 3600
    val mins = (elapsedSeconds % 3600) / 60
    val secs = elapsedSeconds % 60
    val timerText = StrengthCalculator.formatDuration(hrs, mins, secs)

    val unitLabel = if (isKgUnit) "kg" else "lb"
    val volumeText = StrengthCalculator.formatVolume(totalVolumeKg)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            IconButton(
                onClick = onMinimizeClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowDown,
                    contentDescription = "Свернуть",
                    tint = TextPrimary
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(EmeraldNeon)
                    )
                }

                Spacer(modifier = Modifier.height(2.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = timerText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon
                    )
                    Text("•", fontSize = 10.sp, color = TextMuted)
                    Text(
                        text = "$volumeText $unitLabel volume",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                    if (exercisesCount > 0) {
                        Text("•", fontSize = 10.sp, color = TextMuted)
                        Text(
                            text = "$exercisesCount exercises",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onStartRestTimer,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Таймер отдыха",
                    tint = EmeraldLight,
                    modifier = Modifier.size(18.dp)
                )
            }

            TextButton(
                onClick = onDiscardClick,
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Text(
                    text = "Discard",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = CrimsonRed
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            Button(
                onClick = onFinishClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = Color.Black
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                modifier = Modifier.height(34.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(15.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Done", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EnhancedRestTimerBar(
    secondsRemaining: Int,
    onAddSeconds: (Int) -> Unit,
    onSkip: () -> Unit
) {
    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timeFormatted = StrengthCalculator.formatTime(minutes, seconds)

    AthleticCard(
        modifier = Modifier.padding(12.dp),
        backgroundColor = AmberWarm.copy(alpha = 0.15f),
        borderColor = AmberWarm.copy(alpha = 0.45f)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rest Timer: ",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = AmberWarm
                )
                Text(
                    text = "${secondsRemaining}s ($timeFormatted)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Black,
                    color = AmberWarm
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = AmberWarm.copy(alpha = 0.25f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarm.copy(alpha = 0.4f)),
                    modifier = Modifier.clickable { onAddSeconds(30) }
                ) {
                    Text(
                        text = "+30s",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AmberWarm,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DarkSurfaceHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.clickable { onSkip() }
                ) {
                    Text(
                        text = "Skip",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    workoutHistory: List<Workout>,
    unitLabel: String,
    isKgUnit: Boolean,
    onOpenWarmup: () -> Unit = {},
    onUpdateWeight: (String, Double) -> Unit,
    onUpdateReps: (String, Int) -> Unit,
    onApplyPrevious: (String) -> Unit,
    onApplyValues: (String, Double, Int) -> Unit,
    onOpenHistoryMenu: (Int, String) -> Unit,
    onToggleComplete: (String) -> Unit,
    onToggleSetType: (String) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onInspectExercise: () -> Unit,
    onSwapExercise: () -> Unit,
    onRemoveExercise: () -> Unit
) {
    var showNotes by remember(exercise.notes) { mutableStateOf(exercise.notes.isNotBlank()) }

    val exerciseHistory = remember(workoutHistory, exercise.exerciseId, exercise.exerciseName) {
        SetHistoryCalculator.extractExerciseHistory(
            workouts = workoutHistory,
            exerciseId = exercise.exerciseId,
            exerciseName = exercise.exerciseName
        )
    }

    val prRecord = remember(exerciseHistory) {
        SetHistoryCalculator.getAllTimePR(exerciseHistory)
    }

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onInspectExercise() },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(DarkSurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = exercise.exerciseName,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (prRecord.bestRecord != null && prRecord.bestWeightKg > 0) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = AmberWarm.copy(alpha = 0.15f),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarm.copy(alpha = 0.35f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.EmojiEvents,
                                            contentDescription = null,
                                            tint = AmberWarm,
                                            modifier = Modifier.size(10.dp)
                                        )
                                        Text(
                                            text = "PR: ${prRecord.bestWeightKg.toInt()} $unitLabel × ${prRecord.bestReps}",
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AmberWarm
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "${exercise.sets.size} SET",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted
                            )
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showNotes = !showNotes }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Заметка",
                            tint = if (showNotes) EmeraldNeon else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onSwapExercise, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Заменить упражнение",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onInspectExercise, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Техника",
                            tint = TextSecondary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    IconButton(onClick = onRemoveExercise, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Удалить",
                            tint = CrimsonRed.copy(alpha = 0.8f),
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            if (showNotes) {
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = exercise.notes,
                    onValueChange = onUpdateNotes,
                    placeholder = { Text("Заметка к упражнению (напр. пауза внизу)...", fontSize = 12.sp) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Set Table Header: # | PREV | +KG | REPS | ✓
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("#", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(30.dp), textAlign = TextAlign.Center)
                Text("PREV", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(72.dp), textAlign = TextAlign.Center)
                Text(unitLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(70.dp), textAlign = TextAlign.Center)
                Text("REPS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(56.dp), textAlign = TextAlign.Center)
                Text("✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            exercise.sets.forEach { set ->
                WorkoutSetRow(
                    set = set,
                    allExerciseHistory = exerciseHistory,
                    isKgUnit = isKgUnit,
                    unitLabel = unitLabel,
                    onUpdateWeight = { onUpdateWeight(set.id, it) },
                    onUpdateReps = { onUpdateReps(set.id, it) },
                    onApplyPrevious = { onApplyPrevious(set.id) },
                    onApplyValues = { w, r -> onApplyValues(set.id, w, r) },
                    onOpenHistoryMenu = { onOpenHistoryMenu(set.setNumber, set.id) },
                    onToggleComplete = { onToggleComplete(set.id) },
                    onToggleSetType = { onToggleSetType(set.id) },
                    onDelete = { onDeleteSet(set.id) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action buttons row: + Add Set and + Warmup
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .height(38.dp)
                        .clickable { onAddSet() },
                    shape = RoundedCornerShape(12.dp),
                    color = DarkSurfaceHigh,
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                ) {
                    Row(
                        modifier = Modifier.fillMaxSize(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+ Add Set", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    }
                }

                Surface(
                    modifier = Modifier
                        .height(38.dp)
                        .clickable { onOpenWarmup() },
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarm.copy(alpha = 0.12f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarm.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = AmberWarm, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("+ Warmup", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AmberWarm)
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    allExerciseHistory: List<HistoricalSetRecord>,
    isKgUnit: Boolean,
    unitLabel: String,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onApplyPrevious: () -> Unit,
    onApplyValues: (Double, Int) -> Unit,
    onOpenHistoryMenu: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSetType: () -> Unit,
    onDelete: () -> Unit
) {
    val strings = LocalDudaStrings.current
    val coroutineScope = rememberCoroutineScope()
    var isExpanded by remember { mutableStateOf(false) }
    var appliedKey by remember { mutableStateOf<String?>(null) }

    // Last 3 recent performances for this specific set number
    val (topRecords, isFallback) = remember(allExerciseHistory, set.setNumber) {
        SetHistoryCalculator.getLast3RecentForSet(allExerciseHistory, set.setNumber)
    }

    val hasPrev = set.previousWeightKg != null && set.previousReps != null
    val prevWeight = set.previousWeightKg ?: 0.0
    val prevReps = set.previousReps ?: 0

    val effectiveTopRecords = remember(topRecords, hasPrev, prevWeight, prevReps, set.setNumber, set.type) {
        if (topRecords.isNotEmpty()) {
            topRecords
        } else if (hasPrev && (prevWeight > 0 || prevReps > 0)) {
            val calc1RM = StrengthCalculator.calculateE1RM(prevWeight, prevReps) ?: prevWeight
            listOf(
                HistoricalSetRecord(
                    workoutId = "prev-session",
                    workoutName = "Previous Session",
                    dateEpochMs = PlatformBridge.currentTimeMillis(),
                    setNumber = set.setNumber,
                    setType = set.type,
                    weightKg = prevWeight,
                    reps = prevReps,
                    e1rmKg = round(calc1RM * 10.0) / 10.0,
                    volumeKg = round(prevWeight * prevReps * 10.0) / 10.0
                )
            )
        } else {
            emptyList()
        }
    }

    val rowBorderColor = when {
        isExpanded -> EmeraldNeon.copy(alpha = 0.5f)
        set.isCompleted -> EmeraldNeon.copy(alpha = 0.35f)
        else -> BorderSubtle
    }

    val rowBackground = when {
        isExpanded -> DarkSurfaceVariant
        set.isCompleted -> EmeraldDark.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(rowBackground)
                .border(1.dp, rowBorderColor, RoundedCornerShape(12.dp))
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // 1. SET # / TYPE BADGE (Round circle)
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            set.type == "warmup" -> AmberWarm.copy(alpha = 0.2f)
                            set.isCompleted -> EmeraldNeon.copy(alpha = 0.2f)
                            else -> DarkSurfaceHigh
                        }
                    )
                    .clickable { onToggleSetType() },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (set.type) {
                        "warmup" -> "W"
                        "drop" -> "D"
                        "failure" -> "F"
                        else -> "${set.setNumber}"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = when {
                        set.type == "warmup" -> AmberWarm
                        set.isCompleted -> EmeraldNeon
                        else -> TextSecondary
                    }
                )
            }

            // 2. PREV PILL (Tapping toggles top 3 history accordion!)
            Box(
                modifier = Modifier
                    .width(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isExpanded) EmeraldNeon.copy(alpha = 0.15f) else Color.Transparent)
                    .clickable { isExpanded = !isExpanded }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (hasPrev && (prevWeight > 0 || prevReps > 0)) {
                            "${prevWeight.toInt()} $unitLabel × $prevReps"
                        } else "—",
                        fontSize = 11.sp,
                        fontWeight = if (hasPrev) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (hasPrev) EmeraldLight else TextMuted,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (isExpanded) EmeraldNeon else TextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            // 3. WEIGHT INPUT + Plate breakdown badge
            var weightText by remember(set.weightKg) {
                mutableStateOf(if (set.weightKg == 0.0) "" else set.weightKg.toString().removeSuffix(".0"))
            }
            val plateBreakdown = remember(set.weightKg) {
                SetHistoryCalculator.formatPlateBreakdown(set.weightKg)
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(70.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, if (set.isCompleted) EmeraldNeon.copy(alpha = 0.4f) else BorderSubtle, RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    OutlinedTextField(
                        value = weightText,
                        onValueChange = { input ->
                            weightText = input
                            val parsed = input.toDoubleOrNull()
                            if (parsed != null && parsed >= 0) onUpdateWeight(parsed)
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = if (prevWeight > 0) "${prevWeight.toInt()}" else "0",
                                fontSize = 12.sp,
                                color = TextMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (set.isCompleted) EmeraldLight else TextPrimary,
                            textAlign = TextAlign.Center
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent
                        )
                    )
                }
                if (plateBreakdown != null) {
                    Text(
                        text = plateBreakdown,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            // 4. REPS INPUT
            var repsText by remember(set.reps) {
                mutableStateOf(if (set.reps == 0) "" else set.reps.toString())
            }
            Box(
                modifier = Modifier
                    .width(56.dp)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, if (set.isCompleted) EmeraldNeon.copy(alpha = 0.4f) else BorderSubtle, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                OutlinedTextField(
                    value = repsText,
                    onValueChange = { input ->
                        repsText = input
                        val parsed = input.toIntOrNull()
                        if (parsed != null && parsed >= 0) onUpdateReps(parsed)
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = if (prevReps > 0) "$prevReps" else "10",
                            fontSize = 12.sp,
                            color = TextMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (set.isCompleted) EmeraldLight else TextPrimary,
                        textAlign = TextAlign.Center
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    )
                )
            }

            // 5. COMPLETION CHECKMARK BUTTON
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (set.isCompleted) EmeraldNeon else DarkSurfaceHigh)
                    .clickable {
                        if (!set.isCompleted) {
                            if (set.weightKg == 0.0 && set.reps == 0 && hasPrev) {
                                onApplyPrevious()
                            }
                            AthleteSoundManager.playSetComplete()
                        }
                        onToggleComplete()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Выполнено",
                    tint = if (set.isCompleted) Color.Black else TextMuted,
                    modifier = Modifier.size(18.dp)
                )
            }

            // 6. DELETE BUTTON
            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Удалить сет",
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
            }
        }

        // EXPANDED ACCORDION: Top 3 Best Records + Menu Trigger
        AnimatedVisibility(visible = isExpanded) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                shape = RoundedCornerShape(12.dp),
                color = DarkSurfaceVariant,
                border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.35f))
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EmojiEvents,
                                contentDescription = null,
                                tint = AmberWarm,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isFallback) strings.setRecordsFallbackTitle else "${strings.setRecordsTitle} #${set.setNumber}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AmberWarm
                            )
                        }

                        IconButton(
                            onClick = { isExpanded = false },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Свернуть",
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    if (effectiveTopRecords.isEmpty()) {
                        Text(
                            text = strings.noRecordsYet,
                            fontSize = 11.sp,
                            color = TextMuted,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        effectiveTopRecords.take(3).forEachIndexed { rIdx, rec ->
                            val uniqueKey = "${rec.workoutId}-${rec.setNumber}-$rIdx"
                            val isApplied = appliedKey == uniqueKey
                            val medal = when (rIdx) {
                                0 -> "🥇"
                                1 -> "🥈"
                                else -> "🥉"
                            }

                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = DarkSurfaceHigh,
                                border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(text = medal, fontSize = 14.sp)
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "${rec.weightKg.toInt()} $unitLabel × ${rec.reps}",
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary
                                                )
                                                if (rec.e1rmKg > 0) {
                                                    Surface(
                                                        shape = RoundedCornerShape(4.dp),
                                                        color = EmeraldNeon.copy(alpha = 0.15f)
                                                    ) {
                                                        Text(
                                                            text = "1RM: ${rec.e1rmKg.toInt()} $unitLabel",
                                                            fontSize = 9.sp,
                                                            fontWeight = FontWeight.SemiBold,
                                                            color = EmeraldLight,
                                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                                        )
                                                    }
                                                }
                                            }

                                            Text(
                                                text = "${PlatformBridge.formatDate(rec.dateEpochMs, "dd MMM yyyy")} • ${rec.workoutName}",
                                                fontSize = 10.sp,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }

                                    // Apply Button
                                    Button(
                                        onClick = {
                                            onApplyValues(rec.weightKg, rec.reps)
                                            appliedKey = uniqueKey
                                            coroutineScope.launch {
                                                delay(1500)
                                                if (appliedKey == uniqueKey) appliedKey = null
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isApplied) EmeraldNeon else DarkSurfaceVariant,
                                            contentColor = if (isApplied) Color.Black else TextPrimary
                                        ),
                                        shape = RoundedCornerShape(6.dp),
                                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                        modifier = Modifier.height(28.dp)
                                    ) {
                                        Text(
                                            text = if (isApplied) strings.applied else strings.applyRecord,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Separate Menu Trigger Button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenHistoryMenu() },
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceHigh,
                        border = androidx.compose.foundation.BorderStroke(1.dp, EmeraldNeon.copy(alpha = 0.3f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp, horizontal = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = strings.openHistoryMenu,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldNeon
                            )
                        }
                    }
                }
            }
        }
    }
}
