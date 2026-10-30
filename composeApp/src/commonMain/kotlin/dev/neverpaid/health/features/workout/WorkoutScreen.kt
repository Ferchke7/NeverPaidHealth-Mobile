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
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import dev.neverpaid.health.core.designsystem.SetTypeBadge
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.localizeEquipmentName
import dev.neverpaid.health.core.designsystem.localizeMuscleName
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import kotlinx.coroutines.delay

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            ActiveWorkoutTopBar(
                title = state.workout?.title ?: "Тренировка",
                startTimeEpochMs = state.workoutStartTimeEpochMs,
                onMinimizeClick = onNavigateBack,
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
                unitLabel = if (state.isKgUnit) "КГ" else "LB",
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
    onMinimizeClick: () -> Unit,
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
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = timerText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onStartRestTimer,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = "Таймер отдыха",
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(6.dp))

            Button(
                onClick = onFinishClick,
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Завершить", fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
        backgroundColor = DarkSurfaceVariant,
        borderColor = EmeraldNeon
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(EmeraldSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ОТДЫХ",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    Text(
                        text = timeFormatted,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldNeon
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceHigh)
                        .clickable { onAddSeconds(-15) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text("-15с", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceHigh)
                        .clickable { onAddSeconds(30) }
                        .padding(horizontal = 9.dp, vertical = 6.dp)
                ) {
                    Text("+30с", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                IconButton(onClick = onSkip, modifier = Modifier.size(30.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Пропустить", tint = TextSecondary)
                }
            }
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    unitLabel: String,
    onOpenWarmup: () -> Unit = {},
    onUpdateWeight: (String, Double) -> Unit,
    onUpdateReps: (String, Int) -> Unit,
    onApplyPrevious: (String) -> Unit,
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

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onInspectExercise() }
                ) {
                    Text(
                        text = exercise.exerciseName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                    Text(
                        text = "${localizeMuscleName(exercise.primaryMuscleGroup)} • ${localizeEquipmentName(exercise.equipment)}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onOpenWarmup, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = "Калькулятор блинов и разминки",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(17.dp)
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
                    IconButton(onClick = { showNotes = !showNotes }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Заметка",
                            tint = if (showNotes) EmeraldLight else TextSecondary,
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

            // Set Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("СЕТ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(32.dp), textAlign = TextAlign.Center)
                Text("ПРЕД.", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(68.dp), textAlign = TextAlign.Center)
                Text(unitLabel, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                Text("ПОВТ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(54.dp), textAlign = TextAlign.Center)
                Text("✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(34.dp), textAlign = TextAlign.Center)
                Spacer(modifier = Modifier.width(24.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            exercise.sets.forEach { set ->
                WorkoutSetRow(
                    set = set,
                    onUpdateWeight = { onUpdateWeight(set.id, it) },
                    onUpdateReps = { onUpdateReps(set.id, it) },
                    onApplyPrevious = { onApplyPrevious(set.id) },
                    onToggleComplete = { onToggleComplete(set.id) },
                    onToggleSetType = { onToggleSetType(set.id) },
                    onDelete = { onDeleteSet(set.id) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkSurfaceHigh)
                    .clickable { onAddSet() }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(text = "+ Добавить подход", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onApplyPrevious: () -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSetType: () -> Unit,
    onDelete: () -> Unit
) {
    val rowBackground = if (set.isCompleted) EmeraldDark.copy(alpha = 0.16f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .padding(horizontal = 2.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        SetTypeBadge(
            type = set.type,
            setIndex = set.setNumber,
            onClick = onToggleSetType,
            modifier = Modifier.size(30.dp)
        )

        // Tappable Previous Performance Pill (autofills weight & reps on tap!)
        val hasPrev = set.previousWeightKg != null && set.previousReps != null
        Box(
            modifier = Modifier
                .width(68.dp)
                .clip(RoundedCornerShape(6.dp))
                .clickable(enabled = hasPrev) { onApplyPrevious() }
                .padding(vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (hasPrev) {
                    "${set.previousWeightKg!!.toInt()}×${set.previousReps}"
                } else "—",
                fontSize = 11.sp,
                fontWeight = if (hasPrev) FontWeight.SemiBold else FontWeight.Normal,
                color = if (hasPrev) EmeraldLight else TextMuted,
                textAlign = TextAlign.Center
            )
        }

        // Weight Input
        var weightText by remember(set.weightKg) {
            mutableStateOf(set.weightKg.toString().removeSuffix(".0"))
        }
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
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
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        // Reps Input
        var repsText by remember(set.reps) { mutableStateOf(set.reps.toString()) }
        Box(
            modifier = Modifier
                .width(54.dp)
                .height(36.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp)),
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
                textStyle = androidx.compose.ui.text.TextStyle(
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                ),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent
                )
            )
        }

        // Completion checkbox
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (set.isCompleted) EmeraldNeon else DarkSurfaceHigh)
                .clickable {
                    if (!set.isCompleted) {
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

        IconButton(
            onClick = onDelete,
            modifier = Modifier.size(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Удалить сет",
                tint = TextMuted,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}
