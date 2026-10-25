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
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.RestTimerFloatingBar
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.SetTypeBadge
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Workout
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
    var finishedWorkoutSummary by remember { mutableStateOf<Workout?>(null) }

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is WorkoutUiEffect.WorkoutFinishedSummary -> {
                finishedWorkoutSummary = effect.workout
            }
            WorkoutUiEffect.NavigateBack -> onNavigateBack()
            WorkoutUiEffect.TriggerHaptic -> {
                PlatformBridge.vibrate()
            }
            is WorkoutUiEffect.ShowToast -> {
                PlatformBridge.showToast(effect.message)
            }
        }
    }

    if (finishedWorkoutSummary != null) {
        WorkoutCompletionDialog(
            workout = finishedWorkoutSummary!!,
            onDismiss = {
                finishedWorkoutSummary = null
                onNavigateBack()
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            WorkoutTopBar(
                title = state.workout?.title ?: "Workout",
                startTimeEpochMs = state.workoutStartTimeEpochMs,
                onFinishClick = { viewModel.onAction(WorkoutUiAction.OnFinishWorkoutClick) },
                onDiscardClick = { viewModel.onAction(WorkoutUiAction.OnDiscardWorkoutClick) }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = state.isRestTimerActive,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                RestTimerFloatingBar(
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
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun WorkoutScreen(
    state: WorkoutUiState,
    onAction: (WorkoutUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val workout = state.workout

    if (workout == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(text = "No active workout", color = TextSecondary)
        }
        return
    }

    if (state.showExercisePicker) {
        ExercisePickerBottomSheet(
            exercises = state.availableExercises,
            onSelectExercise = { onAction(WorkoutUiAction.OnSelectExerciseToAdd(it)) },
            onDismiss = { onAction(WorkoutUiAction.OnCloseExercisePicker) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "volume_banner") {
            Spacer(modifier = Modifier.height(8.dp))
            WorkoutVolumeHeader(
                totalVolumeKg = workout.totalVolumeKg,
                totalExercises = workout.exercises.size
            )
        }

        items(
            items = workout.exercises,
            key = { it.exerciseId }
        ) { exercise ->
            WorkoutExerciseCard(
                exercise = exercise,
                onUpdateWeight = { setId, w -> onAction(WorkoutUiAction.OnUpdateWeight(exercise.exerciseId, setId, w)) },
                onUpdateReps = { setId, r -> onAction(WorkoutUiAction.OnUpdateReps(exercise.exerciseId, setId, r)) },
                onToggleComplete = { setId -> onAction(WorkoutUiAction.OnToggleSetComplete(exercise.exerciseId, setId)) },
                onToggleSetType = { setId -> onAction(WorkoutUiAction.OnToggleSetType(exercise.exerciseId, setId)) },
                onAddSet = { onAction(WorkoutUiAction.OnAddSet(exercise.exerciseId)) },
                onDeleteSet = { setId -> onAction(WorkoutUiAction.OnDeleteSet(exercise.exerciseId, setId)) },
                onRemoveExercise = { onAction(WorkoutUiAction.OnRemoveExercise(exercise.exerciseId)) }
            )
        }

        item(key = "add_exercise_button") {
            SecondaryAthleticButton(
                text = "+ Add Exercise",
                onClick = { onAction(WorkoutUiAction.OnOpenExercisePicker) }
            )
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun WorkoutTopBar(
    title: String,
    startTimeEpochMs: Long,
    onFinishClick: () -> Unit,
    onDiscardClick: () -> Unit
) {
    var elapsedSeconds by remember { mutableStateOf(0L) }
    var showMenu by remember { mutableStateOf(false) }

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
    val timerText = if (hrs > 0) {
        String.format("%02d:%02d:%02d", hrs, mins, secs)
    } else {
        String.format("%02d:%02d", mins, secs)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = timerText,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = EmeraldNeon
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldNeon)
                    .clickable { onFinishClick() }
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Finish",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = DarkBackground
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { showMenu = !showMenu }) {
                Icon(imageVector = Icons.Default.MoreVert, contentDescription = "Menu", tint = TextSecondary)
            }

            if (showMenu) {
                AlertDialog(
                    onDismissRequest = { showMenu = false },
                    title = { Text(text = "Discard Workout?", color = TextPrimary) },
                    text = { Text(text = "Are you sure you want to discard this workout session? Logged sets will not be saved.", color = TextSecondary) },
                    confirmButton = {
                        TextButton(onClick = {
                            showMenu = false
                            onDiscardClick()
                        }) {
                            Text(text = "Discard", color = CrimsonRed, fontWeight = FontWeight.Bold)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showMenu = false }) {
                            Text(text = "Cancel", color = TextSecondary)
                        }
                    },
                    containerColor = DarkSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun WorkoutVolumeHeader(
    totalVolumeKg: Double,
    totalExercises: Int
) {
    AthleticCard(backgroundColor = DarkSurfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            StatCounter(
                title = "Total Volume",
                value = String.format("%,.0f", totalVolumeKg),
                unit = "kg",
                accentColor = EmeraldNeon
            )
            StatCounter(
                title = "Exercises",
                value = "$totalExercises",
                accentColor = IndigoElectric
            )
        }
    }
}

@Composable
private fun WorkoutExerciseCard(
    exercise: WorkoutExercise,
    onUpdateWeight: (String, Double) -> Unit,
    onUpdateReps: (String, Int) -> Unit,
    onToggleComplete: (String) -> Unit,
    onToggleSetType: (String) -> Unit,
    onAddSet: () -> Unit,
    onDeleteSet: (String) -> Unit,
    onRemoveExercise: () -> Unit
) {
    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Exercise Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = exercise.exerciseName,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${exercise.primaryMuscleGroup.replaceFirstChar { it.uppercase() }} • ${exercise.equipment}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }

                IconButton(
                    onClick = onRemoveExercise,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Remove Exercise",
                        tint = TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Set Table Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = "SET", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                Text(text = "PREV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(60.dp), textAlign = TextAlign.Center)
                Text(text = "KG", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(64.dp), textAlign = TextAlign.Center)
                Text(text = "REPS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(52.dp), textAlign = TextAlign.Center)
                Text(text = "✓", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, modifier = Modifier.width(40.dp), textAlign = TextAlign.Center)
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Set Rows
            exercise.sets.forEach { set ->
                WorkoutSetRow(
                    set = set,
                    onUpdateWeight = { onUpdateWeight(set.id, it) },
                    onUpdateReps = { onUpdateReps(set.id, it) },
                    onToggleComplete = { onToggleComplete(set.id) },
                    onToggleSetType = { onToggleSetType(set.id) },
                    onDelete = { onDeleteSet(set.id) }
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Add Set Button
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
                Text(text = "Add Set", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
            }
        }
    }
}

@Composable
private fun WorkoutSetRow(
    set: WorkoutSet,
    onUpdateWeight: (Double) -> Unit,
    onUpdateReps: (Int) -> Unit,
    onToggleComplete: () -> Unit,
    onToggleSetType: () -> Unit,
    onDelete: () -> Unit
) {
    val rowBackground = if (set.isCompleted) EmeraldDark.copy(alpha = 0.15f) else Color.Transparent

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(rowBackground)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Set type badge
        SetTypeBadge(
            type = set.type,
            setIndex = set.setNumber,
            onClick = onToggleSetType,
            modifier = Modifier.size(32.dp)
        )

        // Previous performance
        Text(
            text = if (set.previousWeightKg != null && set.previousReps != null) {
                "${set.previousWeightKg.toInt()}k × ${set.previousReps}"
            } else "-",
            fontSize = 12.sp,
            color = TextMuted,
            modifier = Modifier.width(60.dp),
            textAlign = TextAlign.Center
        )

        // Weight Input
        var weightText by remember(set.weightKg) { mutableStateOf(set.weightKg.toString().removeSuffix(".0")) }
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
                    fontSize = 14.sp,
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
                .width(52.dp)
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
                    fontSize = 14.sp,
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
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (set.isCompleted) EmeraldNeon else DarkSurfaceHigh)
                .clickable { onToggleComplete() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Complete Set",
                tint = if (set.isCompleted) DarkBackground else TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePickerBottomSheet(
    exercises: List<Exercise>,
    onSelectExercise: (Exercise) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("All") }

    val muscles = listOf("All", "chest", "back", "quads", "hamstrings", "shoulders", "biceps", "triceps", "core")

    val filtered = exercises.filter { ex ->
        val matchesQuery = searchQuery.isBlank() || ex.name.contains(searchQuery, ignoreCase = true)
        val matchesMuscle = selectedMuscle == "All" || ex.primaryMuscleGroup.equals(selectedMuscle, ignoreCase = true)
        matchesQuery && matchesMuscle
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "Add Exercise",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text(text = "Search exercises...", color = TextMuted) },
                leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = filtered,
                    key = { it.id }
                ) { ex ->
                    AthleticCard(
                        modifier = Modifier.clickable { onSelectExercise(ex) },
                        backgroundColor = DarkSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = ex.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${ex.primaryMuscleGroup.replaceFirstChar { it.uppercase() }} • ${ex.equipment}",
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add",
                                tint = EmeraldNeon,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun WorkoutCompletionDialog(
    workout: Workout,
    onDismiss: () -> Unit
) {
    val totalSets = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Text(text = "🏆 WORKOUT COMPLETE!", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
                Spacer(modifier = Modifier.height(4.dp))
                Text(text = workout.title, fontSize = 15.sp, color = TextSecondary)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCounter(title = "Volume", value = String.format("%,.0f", workout.totalVolumeKg), unit = "kg", accentColor = EmeraldNeon)
                    StatCounter(title = "Sets Done", value = "$totalSets", accentColor = IndigoElectric)
                    StatCounter(title = "Duration", value = "${workout.durationSeconds / 60}", unit = "min", accentColor = TextPrimary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = DarkBackground)
            ) {
                Text(text = "Awesome!", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}
