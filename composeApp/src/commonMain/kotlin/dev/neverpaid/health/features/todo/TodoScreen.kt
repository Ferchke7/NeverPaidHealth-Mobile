package dev.neverpaid.health.features.todo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.data.catalog.StandardHabitsCatalog
import dev.neverpaid.health.domain.model.AthleteHabit
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask

private val todoCategories = listOf(
    "all" to "Все",
    "workout" to "Тренировка",
    "nutrition" to "Питание",
    "supplement" to "Добавки",
    "cardio" to "Кардио",
    "recovery" to "Восстановление",
    "general" to "Общее"
)

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
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoScreen(
    state: TodoUiState,
    onAction: (TodoUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isTaskModalOpen) {
        CreateOrEditTaskBottomSheet(
            existingTask = state.editingTask,
            defaultDate = state.selectedDate,
            onSave = { req ->
                onAction(TodoUiAction.OnSaveTask(req, state.editingTask?.id))
            },
            onDismiss = { onAction(TodoUiAction.OnCloseTaskModal) }
        )
    }

    var habits by remember {
        mutableStateOf(StandardHabitsCatalog.standardPresets.take(4))
    }

    val progressFraction = if (state.totalCount > 0) {
        (state.completedCount.toFloat() / state.totalCount.toFloat()).coerceIn(0f, 1f)
    } else 0f

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "План дня и Календарь",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Чек-лист атлета, Помодоро таймер и лог дисциплины",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(IndigoSubtle)
                            .border(1.dp, IndigoElectric.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                            .clickable { onAction(TodoUiAction.OnStartFocusTimer(null)) }
                            .padding(horizontal = 10.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = "Фокус",
                                tint = IndigoElectric,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Фокус", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                    }

                    Button(
                        onClick = { onAction(TodoUiAction.OnOpenTaskModal(null)) },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier.height(36.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Задача", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 4 KPI Cards Row (matching TodoCalendarPage.tsx)
        item(key = "kpi_row") {
            AthleticCard(backgroundColor = DarkSurfaceVariant) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCounter(
                            title = "Выполнено",
                            value = "${state.completedCount}/${state.totalCount}",
                            accentColor = EmeraldNeon
                        )
                        StatCounter(
                            title = "Прогресс дня",
                            value = "${(progressFraction * 100).toInt()}%",
                            accentColor = IndigoElectric
                        )
                        StatCounter(
                            title = "Важные",
                            value = "${state.highPriorityRemaining}",
                            unit = "ост.",
                            accentColor = AmberWarm
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LinearProgressIndicator(
                        progress = { progressFraction },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = EmeraldNeon,
                        trackColor = DarkSurfaceHigh
                    )
                }
            }
        }

        // Athlete Habits Tracker Card
        item(key = "athlete_habits_card") {
            HabitTrackerView(
                habits = habits,
                currentDateIso = state.selectedDate,
                onToggleHabitToday = { habitId ->
                    val today = state.selectedDate
                    habits = habits.map { h ->
                        if (h.id == habitId) {
                            val newDates = if (h.isCompletedOn(today)) h.completedDates - today else h.completedDates + today
                            h.copy(completedDates = newDates)
                        } else h
                    }
                },
                onAddHabitPreset = { preset ->
                    if (habits.none { it.id == preset.id }) {
                        habits = habits + preset
                    }
                }
            )
        }

        // View Mode Switcher: Список задач / Календарь месяца / Журнал активности
        item(key = "view_mode_tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("list", "Список", Icons.Default.List),
                    Triple("calendar", "Календарь", Icons.Default.CalendarMonth),
                    Triple("log", "Журнал (${state.activityLogs.size})", Icons.Default.History)
                ).forEach { (modeKey, label, icon) ->
                    val selected = state.viewMode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) EmeraldNeon else Color.Transparent)
                            .clickable { onAction(TodoUiAction.OnSetViewMode(modeKey)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (selected) Color.Black else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) Color.Black else TextSecondary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        if (state.viewMode == "calendar") {
            // Month Calendar Overview
            item(key = "month_calendar") {
                MonthCalendarOverviewCard(
                    selectedDate = state.selectedDate,
                    allTasks = state.allTasks,
                    onSelectDate = { date ->
                        onAction(TodoUiAction.OnSelectDate(date))
                        onAction(TodoUiAction.OnSetViewMode("list"))
                    }
                )
            }
        } else if (state.viewMode == "log") {
            // Activity Log View (TodoActivityLog.tsx)
            if (state.activityLogs.isEmpty()) {
                item(key = "empty_logs") {
                    AthleticCard(backgroundColor = DarkSurface) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Журнал активности пока пуст", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "Выполняйте задачи и запускайте Фокус-сессии, чтобы видеть полную хронологию.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(state.activityLogs, key = { it.id }) { logItem ->
                    AthleticCard(backgroundColor = DarkSurface) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Box(
                                    modifier = Modifier
                                        .size(32.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (logItem.actionType == "focus_session") IndigoSubtle else EmeraldSubtle
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = if (logItem.actionType == "focus_session") Icons.Default.Timer else Icons.Default.Check,
                                        contentDescription = null,
                                        tint = if (logItem.actionType == "focus_session") IndigoElectric else EmeraldLight,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = logItem.taskTitle,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = when (logItem.actionType) {
                                            "focus_session" -> "Фокус-сессия (${logItem.durationSeconds / 60} мин)"
                                            "created" -> "Создана задача"
                                            else -> "Выполнено"
                                        },
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }
                            Text(
                                text = logItem.loggedDate.take(10),
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        } else {
            // LIST VIEW: 7-day strip + Category chips + Tasks
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

            item(key = "category_filters") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(todoCategories, key = { it.first }) { (key, label) ->
                        val selected = state.selectedCategoryFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { onAction(TodoUiAction.OnSelectCategoryFilter(key)) },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSubtle,
                                selectedLabelColor = EmeraldLight,
                                containerColor = DarkSurfaceVariant,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }

            if (state.tasksForSelectedDate.isEmpty()) {
                item(key = "empty_tasks") {
                    AthleticCard(backgroundColor = DarkSurface) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "На ${state.selectedDate} задач нет",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Добавьте тренировочную цель, приём креатина или сессию фокуса",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            } else {
                items(state.tasksForSelectedDate, key = { it.id }) { task ->
                    val isCompleted = task.isCompletedOn(state.selectedDate)
                    TodoTaskItemCard(
                        task = task,
                        isCompleted = isCompleted,
                        onToggle = { onAction(TodoUiAction.OnToggleTask(task.id)) },
                        onStartFocus = { onAction(TodoUiAction.OnStartFocusTimer(task)) },
                        onEdit = { onAction(TodoUiAction.OnOpenTaskModal(task)) },
                        onDelete = { onAction(TodoUiAction.OnDeleteTask(task.id)) }
                    )
                }
            }

            item(key = "add_task_button") {
                SecondaryAthleticButton(
                    text = "+ Добавить задачу",
                    onClick = { onAction(TodoUiAction.OnOpenTaskModal(null)) },
                    icon = Icons.Default.Add
                )
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun TodoTaskItemCard(
    task: TodoTask,
    isCompleted: Boolean,
    onToggle: () -> Unit,
    onStartFocus: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val priorityColor = when (task.priority.lowercase()) {
        "high" -> CrimsonRed
        "low" -> TextMuted
        else -> AmberWarm
    }

    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = if (isCompleted) BorderHighlight else BorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { onToggle() },
                    colors = CheckboxDefaults.colors(
                        checkedColor = EmeraldNeon,
                        checkmarkColor = DarkBackground,
                        uncheckedColor = TextMuted
                    )
                )

                Spacer(modifier = Modifier.width(6.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) TextSecondary else TextPrimary,
                        textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                    )
                    if (task.notes.isNotBlank()) {
                        Text(
                            text = task.notes,
                            fontSize = 12.sp,
                            color = TextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(DarkSurfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = todoCategories.find { it.first == task.category.lowercase() }?.second ?: task.category,
                                fontSize = 10.sp,
                                color = EmeraldLight
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(priorityColor)
                        )
                        if (!task.scheduledTime.isNullOrBlank()) {
                            Text(
                                text = "⏰ ${task.scheduledTime}",
                                fontSize = 10.sp,
                                color = TextMuted
                            )
                        }
                        if (task.repeatDaily) {
                            Text(
                                text = "↻ ежедневно",
                                fontSize = 10.sp,
                                color = IndigoElectric
                            )
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onStartFocus,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = "Фокус",
                        tint = EmeraldLight,
                        modifier = Modifier.size(17.dp)
                    )
                }
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Изменить",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun MonthCalendarOverviewCard(
    selectedDate: String,
    allTasks: List<TodoTask>,
    onSelectDate: (String) -> Unit
) {
    val yearMonth = selectedDate.take(7) // "YYYY-MM"
    val daysInMonth = (1..30).map { day ->
        "$yearMonth-${day.toString().padStart(2, '0')}"
    }

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Календарь выполнения ($yearMonth)",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            FlowRow(
                maxItemsInEachRow = 7,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                daysInMonth.forEach { dateStr ->
                    val completedOnDate = allTasks.count { it.isCompletedOn(dateStr) }
                    val isSelected = dateStr == selectedDate
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                when {
                                    isSelected -> EmeraldNeon
                                    completedOnDate > 0 -> EmeraldSubtle
                                    else -> DarkSurfaceVariant
                                }
                            )
                            .border(
                                1.dp,
                                if (isSelected) EmeraldLight else BorderSubtle,
                                RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectDate(dateStr) },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = dateStr.takeLast(2).removePrefix("0"),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                            if (completedOnDate > 0) {
                                Text(
                                    text = "✓$completedOnDate",
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else EmeraldLight
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateOrEditTaskBottomSheet(
    existingTask: TodoTask?,
    defaultDate: String,
    onSave: (SaveTodoRequestDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var category by remember { mutableStateOf(existingTask?.category?.lowercase() ?: "workout") }
    var priority by remember { mutableStateOf(existingTask?.priority?.lowercase() ?: "medium") }
    var scheduledTime by remember { mutableStateOf(existingTask?.scheduledTime ?: "") }
    var repeatDaily by remember { mutableStateOf(existingTask?.repeatDaily ?: true) }

    val quickTemplates = listOf(
        Triple("Силовая тренировка дня", "Разминка + рабочие подходы", "workout"),
        Triple("Креатин моногидрат 5г", "Запить стаканом воды", "supplement"),
        Triple("Дневная норма белка (2г/кг)", "Контроль КБЖУ", "nutrition"),
        Triple("Кардио 30 минут (Зона 2)", "Пульс 125–135 уд/мин", "cardio"),
        Triple("Здоровый сон 8 часов", "Восстановление ЦНС", "recovery")
    )

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
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTask != null) "Редактировать задачу" else "Новая задача атлета",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                }
            }

            if (existingTask == null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text("БЫСТРЫЕ ШАБЛОНЫ", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(quickTemplates) { (tplTitle, tplNotes, tplCat) ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable {
                                    title = tplTitle
                                    notes = tplNotes
                                    category = tplCat
                                }
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text("+ $tplTitle", fontSize = 11.sp, color = EmeraldLight, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Название задачи") },
                singleLine = true,
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Заметка / целевое значение (напр. 5г, 2.5л)") },
                singleLine = true,
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

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = scheduledTime,
                onValueChange = { scheduledTime = it },
                label = { Text("Время напоминания (напр. 18:00)") },
                singleLine = true,
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

            Spacer(modifier = Modifier.height(12.dp))

            Text("Категория", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(todoCategories.filter { it.first != "all" }, key = { it.first }) { (catKey, catLabel) ->
                    FilterChip(
                        selected = category == catKey,
                        onClick = { category = catKey },
                        label = { Text(catLabel, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Приоритет", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("high" to "Высокий", "medium" to "Средний", "low" to "Низкий").forEach { (pKey, pLabel) ->
                    FilterChip(
                        selected = priority == pKey,
                        onClick = { priority = pKey },
                        label = { Text(pLabel, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Повторять ежедневно", fontSize = 14.sp, color = TextPrimary)
                Switch(
                    checked = repeatDaily,
                    onCheckedChange = { repeatDaily = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBackground,
                        checkedTrackColor = EmeraldNeon
                    )
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    onSave(
                        SaveTodoRequestDto(
                            title = title.trim(),
                            notes = notes.trim(),
                            category = category,
                            priority = priority,
                            scheduledDate = if (repeatDaily) "" else defaultDate,
                            scheduledTime = scheduledTime.trim().ifBlank { null },
                            repeatDaily = repeatDaily
                        )
                    )
                },
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
                Text("Сохранить задачу", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
