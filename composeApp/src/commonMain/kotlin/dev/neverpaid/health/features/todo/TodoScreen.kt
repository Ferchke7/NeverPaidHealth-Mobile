package dev.neverpaid.health.features.todo

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
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.EmeraldBg
import dev.neverpaid.health.core.designsystem.EmeraldBorder
import dev.neverpaid.health.core.designsystem.EmeraldSuccess
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymKicker
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.DudaStrings
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.data.catalog.StandardHabitsCatalog
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

private enum class FocusMode {
    POMODORO,
    TIMER,
    STOPWATCH
}

private enum class FocusPhase {
    WORK,
    BREAK
}

@Composable
fun TodoRoute(
    viewModel: TodoViewModel,
    onStartWorkout: (String) -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.effect.collect { effect ->
            when (effect) {
                is TodoUiEffect.StartWorkoutSession -> onStartWorkout(effect.title)
                is TodoUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
                TodoUiEffect.TriggerHaptic -> PlatformBridge.vibrate()
            }
        }
    }

    TodoScreen(
        state = state,
        onAction = viewModel::onAction,
        onNavigateToProfile = onNavigateToProfile,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TodoScreen(
    state: TodoUiState,
    onAction: (TodoUiAction) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = strings.languageCode

    var showHabitsSection by rememberSaveable { mutableStateOf(false) }
    var habits by remember { mutableStateOf(StandardHabitsCatalog.standardPresets.take(4)) }
    var inlineEditingTask by remember { mutableStateOf<TodoTask?>(null) }

    // Inline Focus Timer State
    var focusMode by rememberSaveable { mutableStateOf(FocusMode.POMODORO) }
    var focusPhase by rememberSaveable { mutableStateOf(FocusPhase.WORK) }
    var activeTaskTitle by rememberSaveable { mutableStateOf<String?>(null) }
    var timerSecondsLeft by rememberSaveable { mutableIntStateOf(25 * 60) }
    var stopwatchElapsedSeconds by rememberSaveable { mutableIntStateOf(0) }
    var isTimerRunning by rememberSaveable { mutableStateOf(false) }
    var hasStartedSession by rememberSaveable { mutableStateOf(false) }
    var soundEnabled by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(isTimerRunning, focusMode, focusPhase) {
        while (isTimerRunning) {
            delay(1000L)
            if (focusMode == FocusMode.STOPWATCH) {
                stopwatchElapsedSeconds += 1
            } else {
                if (timerSecondsLeft > 0) {
                    timerSecondsLeft -= 1
                } else {
                    isTimerRunning = false
                    if (soundEnabled) {
                        AthleteSoundManager.playIntervalGong()
                    }
                    PlatformBridge.showToast(
                        when (lang) {
                            "uz" -> "Fokus sessiyasi yakunlandi"
                            "en" -> "Focus session finished"
                            else -> "Сессия фокуса завершена"
                        }
                    )
                }
            }
        }
    }

    // Keep inline edit selection synchronized if task is updated/deleted
    LaunchedEffect(state.allTasks) {
        val currentEdit = inlineEditingTask
        if (currentEdit != null) {
            inlineEditingTask = state.allTasks.firstOrNull { it.id == currentEdit.id }
        }
    }

    val activeDate = state.selectedDate.ifBlank { currentTodayIso() }
    val todayIso = remember { currentTodayIso() }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 32.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "page_header", contentType = "header") {
                RoyalHeroCard(bottomExtraPadding = 20.dp) {
                    Text(
                        text = strings.todoTitle,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-0.7).sp,
                        color = InkWhite
                    )
                }
            }

            if (state.isLoading && state.allTasks.isEmpty()) {
                item(key = "loading_card", contentType = "loading") {
                    GymLoadingCard(
                        title = strings.stateLoadingTitle,
                        hint = strings.stateLoadingHint
                    )
                }
            } else {
                // Hero Card: Daily Progress + 7-Day Date Strip
                item(key = "date_strip", contentType = "date_strip") {
                    val progress = if (state.totalCount > 0) {
                        (state.completedCount.toFloat() / state.totalCount.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val habitsDone = habits.count { it.isCompletedOn(activeDate) }

                    GymCard {
                        GymKicker(text = formatFullPlanDate(activeDate, lang))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = "${state.completedCount}",
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.2).sp,
                                        color = AccentLime,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    Text(
                                        text = " / ${state.totalCount}",
                                        fontSize = 24.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = TextSecondary,
                                        style = TextStyle(fontFeatureSettings = "tnum"),
                                        modifier = Modifier.padding(bottom = 5.dp)
                                    )
                                }
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Bajarilgan vazifalar"
                                        "en" -> "Tasks completed"
                                        else -> "Выполнено задач"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                            }

                            if (state.highPriorityRemaining > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(AmberWarm.copy(alpha = 0.16f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = when (lang) {
                                            "uz" -> "${state.highPriorityRemaining} muhim"
                                            "en" -> "${state.highPriorityRemaining} high priority"
                                            else -> "${state.highPriorityRemaining} важных"
                                        },
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AmberWarm,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                }
                            }
                        }

                        GymProgressBar(
                            progress = progress,
                            heightDp = 8,
                            color = AccentLime
                        )

                        if (state.calendarDays.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                state.calendarDays.forEach { day ->
                                    val dayShape = RoundedCornerShape(14.dp)
                                    val bgColor = if (day.isSelected) AccentLime else DarkSurfaceHigh
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .defaultMinSize(minHeight = 58.dp)
                                            .clip(dayShape)
                                            .background(bgColor)
                                            .then(
                                                if (day.isToday && !day.isSelected) {
                                                    Modifier.border(1.dp, BorderHighlight, dayShape)
                                                } else {
                                                    Modifier
                                                }
                                            )
                                            .clickable { onAction(TodoUiAction.OnSelectDate(day.dateString)) }
                                            .padding(vertical = 8.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = day.dayName.take(2).uppercase(),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (day.isSelected) GymInkOnLight else TextSecondary,
                                            maxLines = 1
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = day.dayNumber,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = if (day.isSelected) GymInkOnLight else TextPrimary,
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }
                            }
                        }

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            item(key = "nav_prev") {
                                GymPillTab(
                                    text = when (lang) {
                                        "uz" -> "← Oldingi"
                                        "en" -> "← Prev"
                                        else -> "← Назад"
                                    },
                                    selected = false,
                                    onClick = { onAction(TodoUiAction.OnSelectDate(shiftIsoDate(activeDate, -1))) }
                                )
                            }
                            item(key = "nav_today") {
                                GymPillTab(
                                    text = when (lang) {
                                        "uz" -> "Bugun"
                                        "en" -> "Today"
                                        else -> "Сегодня"
                                    },
                                    selected = activeDate == todayIso,
                                    onClick = { onAction(TodoUiAction.OnSelectDate(currentTodayIso())) }
                                )
                            }
                            item(key = "nav_next") {
                                GymPillTab(
                                    text = when (lang) {
                                        "uz" -> "Keyingi →"
                                        "en" -> "Next →"
                                        else -> "Вперёд →"
                                    },
                                    selected = false,
                                    onClick = { onAction(TodoUiAction.OnSelectDate(shiftIsoDate(activeDate, 1))) }
                                )
                            }
                            item(key = "toggle_habits") {
                                GymPillTab(
                                    text = when (lang) {
                                        "uz" -> "Odatlar $habitsDone/${habits.size}"
                                        "en" -> "Habits $habitsDone/${habits.size}"
                                        else -> "Привычки $habitsDone/${habits.size}"
                                    },
                                    selected = showHabitsSection,
                                    onClick = { showHabitsSection = !showHabitsSection }
                                )
                            }
                        }
                    }
                }

                // Toggleable Habit Tracker section
                if (showHabitsSection) {
                    item(key = "habits_section", contentType = "habits") {
                        HabitTrackerView(
                            habits = habits,
                            currentDateIso = activeDate,
                            onToggleHabitToday = { habitId ->
                                habits = habits.map { item ->
                                    if (item.id == habitId) {
                                        val updatedDates = if (activeDate in item.completedDates) {
                                            item.completedDates - activeDate
                                        } else {
                                            item.completedDates + activeDate
                                        }
                                        item.copy(completedDates = updatedDates)
                                    } else {
                                        item
                                    }
                                }
                            },
                            onAddHabitPreset = { preset ->
                                if (habits.none { it.id == preset.id }) {
                                    habits = habits + preset
                                }
                            }
                        )
                    }
                }

                // Tasks & Filters Card
                item(key = "checklist_card", contentType = "checklist") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) {
                                "uz" -> "Kunlik reja"
                                "en" -> "Daily Tasks"
                                else -> "Задачи дня"
                            },
                            actionLabel = "+ ${strings.todoAddTask}",
                            onAction = { onAction(TodoUiAction.OnOpenTaskModal(null)) }
                        )

                        val categories = listOf(
                            "all" to when (lang) { "uz" -> "Hammasi"; "en" -> "All"; else -> "Все" },
                            "workout" to when (lang) { "uz" -> "Mashg'ulot"; "en" -> "Workout"; else -> "Тренировка" },
                            "nutrition" to when (lang) { "uz" -> "Oziqlanish"; "en" -> "Nutrition"; else -> "Питание" },
                            "supplement" to when (lang) { "uz" -> "Qo'shimcha"; "en" -> "Supps"; else -> "Добавки" },
                            "work" to when (lang) { "uz" -> "Ish"; "en" -> "Work"; else -> "Работа" },
                            "recovery" to when (lang) { "uz" -> "Tiklanish"; "en" -> "Recovery"; else -> "Восстановление" }
                        )

                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(categories, key = { it.first }) { (key, label) ->
                                GymPillTab(
                                    text = label,
                                    selected = state.selectedCategoryFilter == key,
                                    onClick = { onAction(TodoUiAction.OnSelectCategoryFilter(key)) }
                                )
                            }
                        }

                        if (state.tasksForSelectedDate.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Tanlangan kun uchun vazifalar yo'q"
                                        "en" -> "No tasks for selected day"
                                        else -> "На выбранный день задач нет"
                                    },
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Mashg'ulot maqsadi, kreatin yoki fokus sessiyasini qo'shing"
                                        "en" -> "Add a workout goal, supplement, or focus session"
                                        else -> "Добавьте тренировочную цель, приём добавок или сессию фокуса"
                                    },
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.tasksForSelectedDate.forEach { task ->
                                    val isCompleted = task.isCompletedOn(activeDate)
                                    TodoChecklistRow(
                                        task = task,
                                        isCompleted = isCompleted,
                                        lang = lang,
                                        onToggle = { onAction(TodoUiAction.OnToggleTask(task.id)) },
                                        onEdit = { inlineEditingTask = task },
                                        onMoveTomorrow = {
                                            val nextDate = shiftIsoDate(
                                                task.scheduledDate.ifBlank { activeDate },
                                                1
                                            )
                                            onAction(
                                                TodoUiAction.OnSaveTask(
                                                    request = SaveTodoRequestDto(
                                                        title = task.title,
                                                        notes = task.notes,
                                                        category = task.category,
                                                        priority = task.priority,
                                                        scheduledDate = nextDate,
                                                        scheduledTime = task.startTime,
                                                        repeatDaily = task.repeatDaily,
                                                        durationMinutes = task.durationMinutes
                                                    ),
                                                    editingId = task.id
                                                )
                                            )
                                            PlatformBridge.showToast(
                                                when (lang) {
                                                    "uz" -> "Ertaga ko'chirildi ($nextDate)"
                                                    "en" -> "Moved to next day ($nextDate)"
                                                    else -> "Перенесено на $nextDate"
                                                }
                                            )
                                        },
                                        onStartFocus = {
                                            activeTaskTitle = task.title
                                            focusMode = FocusMode.POMODORO
                                            focusPhase = FocusPhase.WORK
                                            timerSecondsLeft = task.pomodoroMinutes * 60
                                            hasStartedSession = true
                                            isTimerRunning = true
                                            onAction(TodoUiAction.OnStartFocusTimer(task))
                                        }
                                    )
                                }
                            }
                        }

                        GymPrimaryButton(
                            text = "+ ${strings.todoAddTask}",
                            onClick = { onAction(TodoUiAction.OnOpenTaskModal(null)) },
                            fillWidth = true
                        )
                    }
                }

                // Inline Task Editor Card
                inlineEditingTask?.let { editing ->
                    item(key = "inline_task_editor_${editing.id}", contentType = "inline_editor") {
                        InlineTaskEditorCard(
                            task = editing,
                            strings = strings,
                            onSave = { request ->
                                onAction(TodoUiAction.OnSaveTask(request, editing.id))
                                inlineEditingTask = null
                            },
                            onDelete = {
                                onAction(TodoUiAction.OnDeleteTask(editing.id))
                                inlineEditingTask = null
                            },
                            onCancel = { inlineEditingTask = null }
                        )
                    }
                }

                // Focus Timer Card
                item(key = "focus_timer_card", contentType = "focus_timer") {
                    GymCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Fokus vaqti"
                                    "en" -> "Focus Timer"
                                    else -> "Таймер фокуса"
                                },
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = (-0.4).sp,
                                color = TextPrimary
                            )

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(if (soundEnabled) AccentLimeSubtle else DarkSurfaceHigh)
                                    .clickable {
                                        soundEnabled = !soundEnabled
                                        if (soundEnabled) {
                                            AthleteSoundManager.playIntervalGong()
                                        }
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = when (lang) {
                                        "uz" -> if (soundEnabled) "Ovoz: yoq." else "Ovoz: o'ch."
                                        "en" -> if (soundEnabled) "Sound: ON" else "Sound: OFF"
                                        else -> if (soundEnabled) "Звук: вкл." else "Звук: выкл."
                                    },
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (soundEnabled) AccentLime else TextMuted
                                )
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPillTab(
                                text = when (lang) { "uz" -> "Pomodoro"; "en" -> "Pomodoro"; else -> "Помодоро" },
                                selected = focusMode == FocusMode.POMODORO,
                                onClick = {
                                    focusMode = FocusMode.POMODORO
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    timerSecondsLeft = if (focusPhase == FocusPhase.WORK) 25 * 60 else 5 * 60
                                }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Taymer"; "en" -> "Timer"; else -> "Таймер" },
                                selected = focusMode == FocusMode.TIMER,
                                onClick = {
                                    focusMode = FocusMode.TIMER
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    timerSecondsLeft = 15 * 60
                                }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Sekundomer"; "en" -> "Stopwatch"; else -> "Секундомер" },
                                selected = focusMode == FocusMode.STOPWATCH,
                                onClick = {
                                    focusMode = FocusMode.STOPWATCH
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    stopwatchElapsedSeconds = 0
                                }
                            )
                        }

                        if (focusMode == FocusMode.POMODORO) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GymPillTab(
                                    text = when (lang) { "uz" -> "Ish 25m"; "en" -> "Work 25m"; else -> "Работа 25м" },
                                    selected = focusPhase == FocusPhase.WORK,
                                    onClick = {
                                        focusPhase = FocusPhase.WORK
                                        isTimerRunning = false
                                        timerSecondsLeft = 25 * 60
                                    }
                                )
                                GymPillTab(
                                    text = when (lang) { "uz" -> "Tanaffus 5m"; "en" -> "Break 5m"; else -> "Перерыв 5м" },
                                    selected = focusPhase == FocusPhase.BREAK,
                                    onClick = {
                                        focusPhase = FocusPhase.BREAK
                                        isTimerRunning = false
                                        timerSecondsLeft = 5 * 60
                                    }
                                )
                            }
                        }

                        val displayedSeconds = if (focusMode == FocusMode.STOPWATCH) {
                            stopwatchElapsedSeconds
                        } else {
                            timerSecondsLeft
                        }

                        val statusText = when {
                            activeTaskTitle != null -> activeTaskTitle!!
                            focusMode == FocusMode.STOPWATCH -> when (lang) {
                                "uz" -> "Sekundomer"
                                "en" -> "Stopwatch"
                                else -> "Секундомер"
                            }
                            focusPhase == FocusPhase.WORK -> when (lang) {
                                "uz" -> "Ish sessiyasi (25 min)"
                                "en" -> "Work session (25 min)"
                                else -> "Рабочая сессия (25 мин)"
                            }
                            else -> when (lang) {
                                "uz" -> "Tanaffus (5 min)"
                                "en" -> "Break (5 min)"
                                else -> "Перерыв (5 мин)"
                            }
                        }

                        GymInnerWell(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = formatMmSs(displayedSeconds),
                                    fontSize = 56.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1.5).sp,
                                    color = if (isTimerRunning) AccentLime else TextPrimary,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    text = statusText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPrimaryButton(
                                text = when {
                                    isTimerRunning -> when (lang) { "uz" -> "Faol"; "en" -> "Running"; else -> "Идёт" }
                                    hasStartedSession -> when (lang) { "uz" -> "Davom"; "en" -> "Resume"; else -> "Старт" }
                                    else -> strings.todoPomodoroStart
                                },
                                onClick = {
                                    hasStartedSession = true
                                    isTimerRunning = true
                                },
                                enabled = !isTimerRunning,
                                modifier = Modifier.weight(1.2f)
                            )
                            GymOutlineButton(
                                text = strings.todoPomodoroPause,
                                onClick = { isTimerRunning = false },
                                enabled = isTimerRunning,
                                modifier = Modifier.weight(1f)
                            )
                            GymOutlineButton(
                                text = strings.todoPomodoroReset,
                                onClick = {
                                    isTimerRunning = false
                                    hasStartedSession = false
                                    activeTaskTitle = null
                                    stopwatchElapsedSeconds = 0
                                    timerSecondsLeft = when (focusMode) {
                                        FocusMode.POMODORO -> if (focusPhase == FocusPhase.WORK) 25 * 60 else 5 * 60
                                        FocusMode.TIMER -> 15 * 60
                                        FocusMode.STOPWATCH -> 0
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Activity Log Card
                item(key = "activity_log_card", contentType = "activity_log") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) {
                                "uz" -> "Faoliyat jurnali"
                                "en" -> "Activity Log"
                                else -> "Журнал активности"
                            }
                        )

                        val completedForDay = state.tasksForSelectedDate.filter { it.isCompletedOn(activeDate) }

                        if (state.activityLogs.isEmpty() && completedForDay.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Hozircha yozuvlar yo'q"
                                        "en" -> "No activity entries yet"
                                        else -> "Пока нет записей активности"
                                    },
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                state.activityLogs.take(8).forEach { log ->
                                    val timeLabel = log.startedAt
                                        .substringAfter("T", "")
                                        .take(5)
                                        .ifBlank { log.loggedDate }
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(DarkSurfaceHigh)
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = timeLabel.ifBlank { "●" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AccentLime,
                                                style = TextStyle(fontFeatureSettings = "tnum")
                                            )
                                            Text(
                                                text = log.taskTitle,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = if (log.durationMinutes > 0) "${log.durationMinutes} мин" else log.actionType,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                    }
                                }

                                completedForDay.take(5).forEach { task ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .background(DarkSurfaceHigh)
                                            .padding(horizontal = 14.dp, vertical = 12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            modifier = Modifier.weight(1f),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Text(
                                                text = task.scheduledTime.ifBlank { "✓" },
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Black,
                                                color = AccentLime,
                                                style = TextStyle(fontFeatureSettings = "tnum")
                                            )
                                            Text(
                                                text = task.title,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = TextPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = when (lang) {
                                                "uz" -> "Bajarildi"
                                                "en" -> "Done"
                                                else -> "Выполнена"
                                            },
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = AccentLime
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (state.isTaskModalOpen) {
        CreateOrEditTaskBottomSheet(
            existingTask = state.editingTask,
            selectedDate = activeDate,
            strings = strings,
            onDismiss = { onAction(TodoUiAction.OnCloseTaskModal) },
            onSave = { request ->
                onAction(TodoUiAction.OnSaveTask(request, state.editingTask?.id))
            },
            onDelete = state.editingTask?.let { task ->
                {
                    onAction(TodoUiAction.OnDeleteTask(task.id))
                    onAction(TodoUiAction.OnCloseTaskModal)
                }
            }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TodoChecklistRow(
    task: TodoTask,
    isCompleted: Boolean,
    lang: String,
    onToggle: () -> Unit,
    onEdit: () -> Unit,
    onMoveTomorrow: () -> Unit,
    onStartFocus: () -> Unit
) {
    val cardShape = RoundedCornerShape(16.dp)
    val priorityLower = task.priority.lowercase()
    val (priorityColor, priorityBg) = when (priorityLower) {
        "high" -> CrimsonRed to CrimsonRed.copy(alpha = 0.16f)
        "medium" -> AmberWarm to AmberWarm.copy(alpha = 0.16f)
        else -> TextSecondary to DarkSurface
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(if (isCompleted) EmeraldBg else DarkSurfaceHigh)
            .then(
                if (isCompleted) {
                    Modifier.border(1.dp, EmeraldBorder, cardShape)
                } else {
                    Modifier
                }
            )
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onToggle),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(if (isCompleted) EmeraldSuccess else DarkSurfaceElevated)
                    .clickable(onClick = onToggle),
                contentAlignment = Alignment.Center
            ) {
                if (isCompleted) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = InkWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = task.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isCompleted) TextSecondary else TextPrimary,
                    textDecoration = if (isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = localizeTodoCategory(task.category, lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }

                    // Priority pill
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(priorityBg)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = localizePriority(task.priority, lang),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = priorityColor
                        )
                    }

                    // Duration / Time pill
                    val timePart = if (task.scheduledTime.isNotBlank()) "${task.scheduledTime} · " else ""
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurface)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "$timePart${task.durationMinutes} мин",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                if (task.notes.isNotBlank()) {
                    Text(
                        text = task.notes,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 40.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaskActionChip(
                text = when (lang) { "uz" -> "Fokus"; "en" -> "Focus"; else -> "Фокус" },
                onClick = onStartFocus,
                accent = true
            )
            TaskActionChip(
                text = when (lang) { "uz" -> "Ertaga"; "en" -> "+1d"; else -> "Перенести" },
                onClick = onMoveTomorrow
            )
            TaskActionChip(
                text = when (lang) { "uz" -> "Tahrir"; "en" -> "Edit"; else -> "Изменить" },
                onClick = onEdit
            )
        }
    }
}

@Composable
private fun TaskActionChip(
    text: String,
    onClick: () -> Unit,
    accent: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (accent) AccentLimeSubtle else DarkSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (accent) AccentLime else TextSecondary
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineTaskEditorCard(
    task: TodoTask,
    strings: DudaStrings,
    onSave: (SaveTodoRequestDto) -> Unit,
    onDelete: () -> Unit,
    onCancel: () -> Unit
) {
    val lang = strings.languageCode
    var title by remember(task.id) { mutableStateOf(task.title) }
    var date by remember(task.id) { mutableStateOf(task.scheduledDate) }
    var category by remember(task.id) { mutableStateOf(task.category) }
    var durationText by remember(task.id) { mutableStateOf(task.durationMinutes.toString()) }
    var priority by remember(task.id) { mutableStateOf(task.priority) }

    GymCard(borderColor = BorderHighlight) {
        GymSectionTitle(
            title = when (lang) {
                "uz" -> "Vazifani tahrirlash"
                "en" -> "Edit Task"
                else -> "Редактирование задачи"
            }
        )

        GymLabeledField(
            label = strings.todoTaskTitleLabel,
            value = title,
            onValueChange = { title = it },
            placeholder = strings.todoTaskTitleLabel
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            GymLabeledField(
                label = when (lang) { "uz" -> "Sana"; "en" -> "Date"; else -> "Дата" },
                value = date,
                onValueChange = { date = it },
                placeholder = "YYYY-MM-DD",
                modifier = Modifier.weight(1f)
            )
            GymLabeledField(
                label = when (lang) {
                    "uz" -> "Davomiyligi, min"
                    "en" -> "Duration, min"
                    else -> "Длительность, мин"
                },
                value = durationText,
                onValueChange = { input -> durationText = input.filter { it.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "30",
                modifier = Modifier.weight(1f)
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.todoCategoryLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("workout", "nutrition", "supplement", "work", "recovery").forEach { key ->
                    GymPillTab(
                        text = localizeTodoCategory(key, lang),
                        selected = category == key,
                        onClick = { category = key }
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = strings.todoPriorityLabel,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("low", "medium", "high").forEach { p ->
                    GymPillTab(
                        text = localizePriority(p, lang),
                        selected = priority == p,
                        onClick = { priority = p }
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymPrimaryButton(
                text = strings.save,
                onClick = {
                    if (title.isNotBlank()) {
                        onSave(
                            SaveTodoRequestDto(
                                title = title.trim(),
                                notes = task.notes,
                                category = category,
                                priority = priority,
                                scheduledDate = date.trim().ifBlank { task.scheduledDate },
                                scheduledTime = task.startTime,
                                repeatDaily = task.repeatDaily,
                                durationMinutes = durationText.toIntOrNull()?.coerceAtLeast(1) ?: task.durationMinutes
                            )
                        )
                    }
                },
                enabled = title.isNotBlank(),
                modifier = Modifier.weight(1.2f)
            )
            GymOutlineButton(
                text = strings.cancel,
                onClick = onCancel,
                modifier = Modifier.weight(1f)
            )
            GymOutlineButton(
                text = strings.delete,
                onClick = onDelete,
                textColor = CrimsonRed,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CreateOrEditTaskBottomSheet(
    existingTask: TodoTask?,
    selectedDate: String,
    strings: DudaStrings,
    onDismiss: () -> Unit,
    onSave: (SaveTodoRequestDto) -> Unit,
    onDelete: (() -> Unit)? = null
) {
    val lang = strings.languageCode
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var title by remember { mutableStateOf(existingTask?.title ?: "") }
    var notes by remember { mutableStateOf(existingTask?.notes ?: "") }
    var scheduledDate by remember { mutableStateOf(existingTask?.scheduledDate?.ifBlank { selectedDate } ?: selectedDate) }
    var scheduledTime by remember { mutableStateOf(existingTask?.scheduledTime ?: "") }
    var durationText by remember { mutableStateOf((existingTask?.durationMinutes ?: 30).toString()) }
    var category by remember { mutableStateOf(existingTask?.category ?: "workout") }
    var priority by remember { mutableStateOf(existingTask?.priority ?: "medium") }
    var repeatDaily by remember { mutableStateOf(existingTask?.repeatDaily ?: false) }

    val quickTemplates = listOf(
        Triple(
            when (lang) { "uz" -> "Kreatin 5g ichish"; "en" -> "Take 5g Creatine"; else -> "Приём креатина 5г" },
            when (lang) { "uz" -> "Mashg'ulotdan keyin suv bilan"; "en" -> "With water post-workout"; else -> "С водой после тренировки" },
            "supplement"
        ),
        Triple(
            when (lang) { "uz" -> "3L suv ichish"; "en" -> "Drink 3L Water"; else -> "Выпить 3 литра воды" },
            when (lang) { "uz" -> "Kun davomida gidratatsiya"; "en" -> "Hydration throughout the day"; else -> "Гидратация в течение дня" },
            "nutrition"
        ),
        Triple(
            when (lang) { "uz" -> "20 min cho'zilish"; "en" -> "20 min Mobility"; else -> "Растяжка и МФР 20 мин" },
            when (lang) { "uz" -> "Yelka va bel"; "en" -> "Shoulders & Lower back"; else -> "Плечи и поясница" },
            "recovery"
        ),
        Triple(
            when (lang) { "uz" -> "8 soat uyqu"; "en" -> "8 Hours Sleep"; else -> "Сон 8+ часов" },
            when (lang) { "uz" -> "23:00 gacha uxlash"; "en" -> "Sleep before 23:00"; else -> "Отбой до 23:00" },
            "recovery"
        )
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .width(40.dp)
                    .height(4.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingTask != null) {
                        when (lang) {
                            "uz" -> "Vazifani tahrirlash"
                            "en" -> "Edit Task"
                            else -> "Редактировать задачу"
                        }
                    } else {
                        strings.todoNewTaskTitle
                    },
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceHigh)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (existingTask == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Tezkor shablonlar"
                            "en" -> "Quick templates"
                            else -> "Быстрые шаблоны"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickTemplates, key = { it.first }) { (tplTitle, tplNotes, tplCat) ->
                            GymPillTab(
                                text = "+ $tplTitle",
                                selected = title == tplTitle,
                                onClick = {
                                    title = tplTitle
                                    notes = tplNotes
                                    category = tplCat
                                }
                            )
                        }
                    }
                }
            }

            GymLabeledField(
                label = strings.todoTaskTitleLabel,
                value = title,
                onValueChange = { title = it },
                placeholder = strings.todoTaskTitleLabel
            )

            GymLabeledField(
                label = strings.todoTargetLabel,
                value = notes,
                onValueChange = { notes = it },
                placeholder = strings.todoTargetLabel
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GymLabeledField(
                    label = when (lang) { "uz" -> "Sana"; "en" -> "Date"; else -> "Дата" },
                    value = scheduledDate,
                    onValueChange = { scheduledDate = it },
                    placeholder = "YYYY-MM-DD",
                    modifier = Modifier.weight(1f)
                )
                GymLabeledField(
                    label = when (lang) { "uz" -> "Vaqt"; "en" -> "Time"; else -> "Время" },
                    value = scheduledTime,
                    onValueChange = { scheduledTime = it },
                    placeholder = "18:00",
                    modifier = Modifier.weight(1f)
                )
            }

            GymLabeledField(
                label = when (lang) {
                    "uz" -> "Davomiyligi, min"
                    "en" -> "Duration, min"
                    else -> "Длительность, мин"
                },
                value = durationText,
                onValueChange = { input -> durationText = input.filter { it.isDigit() } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                placeholder = "30"
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.todoCategoryLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("workout", "nutrition", "supplement", "work", "recovery").forEach { key ->
                        GymPillTab(
                            text = localizeTodoCategory(key, lang),
                            selected = category == key,
                            onClick = { category = key }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = strings.todoPriorityLabel,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("low", "medium", "high").forEach { key ->
                        GymPillTab(
                            text = localizePriority(key, lang),
                            selected = priority == key,
                            onClick = { priority = key }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceHigh)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings.todoRecurringDaily,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSoft
                )
                Switch(
                    checked = repeatDaily,
                    onCheckedChange = { repeatDaily = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = GymInkOnLight,
                        checkedTrackColor = AccentLime,
                        uncheckedThumbColor = TextSecondary,
                        uncheckedTrackColor = DarkSurfaceElevated,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (onDelete != null) {
                    GymOutlineButton(
                        text = strings.delete,
                        onClick = onDelete,
                        textColor = CrimsonRed
                    )
                }
                GymOutlineButton(
                    text = strings.cancel,
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
                GymPrimaryButton(
                    text = strings.save,
                    onClick = {
                        if (title.isNotBlank()) {
                            onSave(
                                SaveTodoRequestDto(
                                    title = title.trim(),
                                    notes = notes.trim(),
                                    category = category,
                                    priority = priority,
                                    scheduledDate = scheduledDate.trim().ifBlank { selectedDate },
                                    scheduledTime = scheduledTime.trim().ifBlank { null },
                                    repeatDaily = repeatDaily,
                                    durationMinutes = durationText.toIntOrNull()?.coerceAtLeast(1) ?: 30
                                )
                            )
                        }
                    },
                    enabled = title.isNotBlank(),
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

private fun localizeTodoCategory(category: String, lang: String): String = when (category.lowercase()) {
    "workout" -> when (lang) { "uz" -> "Mashg'ulot"; "en" -> "Workout"; else -> "Тренировка" }
    "nutrition" -> when (lang) { "uz" -> "Oziqlanish"; "en" -> "Nutrition"; else -> "Питание" }
    "supplement" -> when (lang) { "uz" -> "Qo'shimcha"; "en" -> "Supps"; else -> "Добавки" }
    "cardio" -> when (lang) { "uz" -> "Kardio"; "en" -> "Cardio"; else -> "Кардио" }
    "recovery" -> when (lang) { "uz" -> "Tiklanish"; "en" -> "Recovery"; else -> "Восстановление" }
    else -> when (lang) { "uz" -> "Ish"; "en" -> "Work"; else -> "Работа" }
}

private fun localizePriority(priority: String, lang: String): String = when (priority.lowercase()) {
    "high" -> when (lang) { "uz" -> "Yuqori"; "en" -> "High"; else -> "Высокий" }
    "low" -> when (lang) { "uz" -> "Past"; "en" -> "Low"; else -> "Низкий" }
    else -> when (lang) { "uz" -> "O'rta"; "en" -> "Medium"; else -> "Средний" }
}

private fun formatMmSs(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val mins = safe / 60
    val secs = safe % 60
    return "${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}"
}

private fun currentTodayIso(): String =
    Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

private fun shiftIsoDate(isoDate: String, deltaDays: Int): String {
    val base = runCatching { LocalDate.parse(isoDate) }
        .getOrElse { Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date }
    return if (deltaDays >= 0) {
        base.plus(deltaDays, DateTimeUnit.DAY).toString()
    } else {
        base.minus(-deltaDays, DateTimeUnit.DAY).toString()
    }
}

private fun formatFullPlanDate(isoDate: String, lang: String): String {
    val date = runCatching { LocalDate.parse(isoDate) }.getOrNull() ?: return isoDate
    val weekday = when (date.dayOfWeek) {
        DayOfWeek.MONDAY -> when (lang) { "uz" -> "Dushanba"; "en" -> "Monday"; else -> "Понедельник" }
        DayOfWeek.TUESDAY -> when (lang) { "uz" -> "Seshanba"; "en" -> "Tuesday"; else -> "Вторник" }
        DayOfWeek.WEDNESDAY -> when (lang) { "uz" -> "Chorshanba"; "en" -> "Wednesday"; else -> "Среда" }
        DayOfWeek.THURSDAY -> when (lang) { "uz" -> "Payshanba"; "en" -> "Thursday"; else -> "Четверг" }
        DayOfWeek.FRIDAY -> when (lang) { "uz" -> "Juma"; "en" -> "Friday"; else -> "Пятница" }
        DayOfWeek.SATURDAY -> when (lang) { "uz" -> "Shanba"; "en" -> "Saturday"; else -> "Суббота" }
        DayOfWeek.SUNDAY -> when (lang) { "uz" -> "Yakshanba"; "en" -> "Sunday"; else -> "Воскресенье" }
        else -> ""
    }
    val month = when (date.monthNumber) {
        1 -> when (lang) { "uz" -> "yanvar"; "en" -> "January"; else -> "января" }
        2 -> when (lang) { "uz" -> "fevral"; "en" -> "February"; else -> "февраля" }
        3 -> when (lang) { "uz" -> "mart"; "en" -> "March"; else -> "марта" }
        4 -> when (lang) { "uz" -> "aprel"; "en" -> "April"; else -> "апреля" }
        5 -> when (lang) { "uz" -> "may"; "en" -> "May"; else -> "мая" }
        6 -> when (lang) { "uz" -> "iyun"; "en" -> "June"; else -> "июня" }
        7 -> when (lang) { "uz" -> "iyul"; "en" -> "July"; else -> "июля" }
        8 -> when (lang) { "uz" -> "avgust"; "en" -> "August"; else -> "августа" }
        9 -> when (lang) { "uz" -> "sentyabr"; "en" -> "September"; else -> "сентября" }
        10 -> when (lang) { "uz" -> "oktyabr"; "en" -> "October"; else -> "октября" }
        11 -> when (lang) { "uz" -> "noyabr"; "en" -> "November"; else -> "ноября" }
        else -> when (lang) { "uz" -> "dekabr"; "en" -> "December"; else -> "декабря" }
    }
    return "$weekday, ${date.dayOfMonth} $month ${date.year}"
}
