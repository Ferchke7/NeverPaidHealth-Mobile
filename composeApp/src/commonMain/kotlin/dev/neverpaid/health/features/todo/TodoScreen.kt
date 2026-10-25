package dev.neverpaid.health.features.todo

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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
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
import dev.neverpaid.health.core.designsystem.DudaTopHeader
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.TodoTask

@Composable
fun TodoRoute(
    viewModel: TodoViewModel,
    onStartWorkout: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is TodoUiEffect.StartWorkoutSession -> onStartWorkout(effect.title)
            is TodoUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
            TodoUiEffect.TriggerHaptic -> PlatformBridge.vibrate()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        TodoScreen(
            state = state,
            onAction = viewModel::onAction,
            onStartWorkout = onStartWorkout,
            onNavigateToProfile = onNavigateToProfile,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun TodoScreen(
    state: TodoUiState,
    onAction: (TodoUiAction) -> Unit,
    onStartWorkout: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current

    if (state.isAddTaskSheetOpen) {
        AddTaskBottomSheet(
            title = state.newTaskTitle,
            target = state.newTaskTarget,
            category = state.newTaskCategory,
            priority = state.newTaskPriority,
            isRecurring = state.newTaskRecurring,
            onTitleChanged = { onAction(TodoUiAction.OnNewTaskTitleChanged(it)) },
            onTargetChanged = { onAction(TodoUiAction.OnNewTaskTargetChanged(it)) },
            onCategoryChanged = { onAction(TodoUiAction.OnNewTaskCategoryChanged(it)) },
            onPriorityChanged = { onAction(TodoUiAction.OnNewTaskPriorityChanged(it)) },
            onRecurringChanged = { onAction(TodoUiAction.OnNewTaskRecurringChanged(it)) },
            onSave = { onAction(TodoUiAction.OnSaveNewTask) },
            onDismiss = { onAction(TodoUiAction.OnCloseAddTaskSheet) }
        )
    }

    if (state.isPomodoroSheetOpen) {
        PomodoroBottomSheet(
            secondsRemaining = state.pomodoroSecondsRemaining,
            isRunning = state.isPomodoroRunning,
            isBreak = state.isBreakMode,
            onTogglePlay = { onAction(TodoUiAction.OnTogglePomodoroPlayPause) },
            onReset = { onAction(TodoUiAction.OnResetPomodoro) },
            onSetBreak = { onAction(TodoUiAction.OnSetPomodoroMode(it)) },
            onDismiss = { onAction(TodoUiAction.OnClosePomodoro) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            DudaTopHeader(
                streakDays = state.streakDays,
                onProfileClick = onNavigateToProfile,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = strings.todoTitle,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight,
                letterSpacing = 1.5.sp
            )
            Text(
                text = strings.todoSubtitle,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }

        // Real Interactive 7-Day Date Strip
        item(key = "date_strip") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.calendarDays, key = { it.dateString }) { day ->
                    val bg = if (day.isSelected) EmeraldSubtle else DarkSurfaceVariant
                    val border = if (day.isSelected) BorderHighlight else BorderSubtle

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(width = 1.dp, color = border, shape = RoundedCornerShape(14.dp))
                            .clickable { onAction(TodoUiAction.OnSelectDate(day.dateString)) }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (day.isSelected) EmeraldLight else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.dayNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (day.isSelected) TextPrimary else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (day.isToday) EmeraldNeon else DarkSurfaceHigh)
                        )
                    }
                }
            }
        }

        // Daily Checklist Section Header with Completed ratio & Focus Timer button
        item(key = "checklist_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.todoDailyChecklist,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${state.completedCount} из ${state.totalCount} выполнено",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = EmeraldLight
                    )
                }

                // Pomodoro Focus button
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                        .clickable { onAction(TodoUiAction.OnOpenPomodoro(null)) }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = "Focus Timer",
                            tint = EmeraldNeon,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Pomodoro",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }

        // Task Items list
        items(state.tasks, key = { it.id }) { task ->
            val isCompleted = task.isCompletedOn(state.selectedDate)
            AthleticCard(
                backgroundColor = DarkSurface,
                borderColor = if (isCompleted) BorderHighlight else BorderSubtle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onAction(TodoUiAction.OnToggleTask(task.id)) }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        // Standard Material 3 Checkbox
                        Checkbox(
                            checked = isCompleted,
                            onCheckedChange = { onAction(TodoUiAction.OnToggleTask(task.id)) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = EmeraldNeon,
                                checkmarkColor = DarkBackground,
                                uncheckedColor = TextMuted
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Column {
                            Text(
                                text = task.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isCompleted) TextPrimary else TextSecondary
                            )
                            Text(
                                text = task.target,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Quick start focus timer for this task
                        IconButton(
                            onClick = { onAction(TodoUiAction.OnOpenPomodoro(task.id)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Focus",
                                tint = EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        IconButton(
                            onClick = { onAction(TodoUiAction.OnDeleteTask(task.id)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        item(key = "add_task_button") {
            SecondaryAthleticButton(
                text = strings.todoAddTask,
                onClick = { onAction(TodoUiAction.OnOpenAddTaskSheet) },
                icon = Icons.Default.Add
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddTaskBottomSheet(
    title: String,
    target: String,
    category: String,
    priority: String,
    isRecurring: Boolean,
    onTitleChanged: (String) -> Unit,
    onTargetChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onPriorityChanged: (String) -> Unit,
    onRecurringChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalDudaStrings.current
    val categories = listOf("WORKOUT", "NUTRITION", "RECOVERY", "FOCUS")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = strings.todoNewTaskTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedTextField(
                value = title,
                onValueChange = onTitleChanged,
                label = { Text(strings.todoTaskTitleLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = target,
                onValueChange = onTargetChanged,
                label = { Text(strings.todoTargetLabel) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Category Chips
            Text(
                text = strings.todoCategoryLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(categories) { cat ->
                    val isSelected = cat == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { onCategoryChanged(cat) },
                        label = { Text(cat) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Recurring Daily Switch
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.todoRecurringDaily,
                    fontSize = 14.sp,
                    color = TextPrimary
                )
                Switch(
                    checked = isRecurring,
                    onCheckedChange = onRecurringChanged,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = EmeraldNeon
                    )
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onSave,
                enabled = title.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = strings.save, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PomodoroBottomSheet(
    secondsRemaining: Int,
    isRunning: Boolean,
    isBreak: Boolean,
    onTogglePlay: () -> Unit,
    onReset: () -> Unit,
    onSetBreak: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalDudaStrings.current

    val mins = secondsRemaining / 60
    val secs = secondsRemaining % 60
    val formattedTime = StrengthCalculator.formatTime(mins, secs)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = strings.todoPomodoroTitle,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Focus / Break Segmented Buttons
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = !isBreak,
                    onClick = { onSetBreak(false) },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = EmeraldNeon,
                        activeContentColor = DarkBackground,
                        inactiveContainerColor = DarkSurfaceVariant,
                        inactiveContentColor = TextSecondary
                    )
                ) {
                    Text(strings.todoPomodoroFocus, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                SegmentedButton(
                    selected = isBreak,
                    onClick = { onSetBreak(true) },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = EmeraldNeon,
                        activeContentColor = DarkBackground,
                        inactiveContainerColor = DarkSurfaceVariant,
                        inactiveContentColor = TextSecondary
                    )
                ) {
                    Text(strings.todoPomodoroShortBreak, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Timer Display
            Text(
                text = formattedTime,
                fontSize = 52.sp,
                fontWeight = FontWeight.Black,
                color = if (isBreak) EmeraldLight else EmeraldNeon
            )

            Spacer(modifier = Modifier.height(28.dp))

            // Play / Pause / Reset Controls
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onReset,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceVariant)
                ) {
                    Icon(
                        imageVector = Icons.Default.Refresh,
                        contentDescription = "Reset",
                        tint = TextSecondary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(20.dp))

                Button(
                    onClick = onTogglePlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .height(52.dp)
                        .width(160.dp)
                ) {
                    Text(
                        text = if (isRunning) strings.todoPomodoroPause else strings.todoPomodoroStart,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
