package dev.neverpaid.health.features.dashboard

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.designsystem.BorderDivider
import dev.neverpaid.health.core.designsystem.ObsidianCard
import dev.neverpaid.health.core.designsystem.ObsidianDivider
import dev.neverpaid.health.core.designsystem.ObsidianEmptyCard
import dev.neverpaid.health.core.designsystem.ObsidianLabeledField
import dev.neverpaid.health.core.designsystem.ObsidianLoadingCard
import dev.neverpaid.health.core.designsystem.ObsidianOutlineButton
import dev.neverpaid.health.core.designsystem.ObsidianPageHeader
import dev.neverpaid.health.core.designsystem.ObsidianPillTab
import dev.neverpaid.health.core.designsystem.ObsidianPrimaryButton
import dev.neverpaid.health.core.designsystem.ObsidianTextField
import dev.neverpaid.health.core.designsystem.TextSoft

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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
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
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExercisePickerBottomSheet
import dev.neverpaid.health.core.designsystem.ObsidianWell
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.domain.model.ActiveProgramBundle
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.Routine
import kotlinx.coroutines.launch

@Composable
fun DashboardRoute(
    viewModel: DashboardViewModel,
    onNavigateToActiveWorkout: () -> Unit,
    onNavigateToScreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            DashboardUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
            is DashboardUiEffect.ShowSnackbar -> {
                scope.launch { snackbarHostState.showSnackbar(effect.message) }
            }
        }
    }

    if (state.previewRoutine != null) {
        val routine = state.previewRoutine!!
        RoutineDetailBottomSheet(
            routine = routine,
            isCustom = !routine.id.startsWith("prebuilt-"),
            onStartWorkout = { viewModel.onAction(DashboardUiAction.OnSelectRoutineClick(routine)) },
            onEditRoutine = { viewModel.onAction(DashboardUiAction.OnOpenRoutineEditor(routine)) },
            onInspectExercise = { exId, name ->
                viewModel.onAction(DashboardUiAction.OnInspectExerciseById(exId, name))
            },
            onDismiss = { viewModel.onAction(DashboardUiAction.OnPreviewRoutineClick(null)) }
        )
    }

    if (state.isRoutineEditorOpen) {
        RoutineEditorBottomSheet(
            state = state,
            onAction = viewModel::onAction,
            onDismiss = { viewModel.onAction(DashboardUiAction.OnCloseRoutineEditor) }
        )
    }

    if (state.isExercisePickerOpen) {
        ExercisePickerBottomSheet(
            exercises = state.allExercises,
            title = "Добавить упражнение в сплит",
            onSelectExercise = { ex ->
                viewModel.onAction(DashboardUiAction.OnAddExerciseToEditor(ex))
            },
            onInspectExercise = { ex ->
                viewModel.onAction(DashboardUiAction.OnInspectExercise(ex))
            },
            onDismiss = { viewModel.onAction(DashboardUiAction.OnCloseExercisePicker) }
        )
    }

    if (state.inspectedExercise != null) {
        ExerciseInfoBottomSheet(
            exercise = state.inspectedExercise!!,
            onDismiss = { viewModel.onAction(DashboardUiAction.OnInspectExercise(null)) }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground,
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        DashboardScreen(
            state = state,
            onAction = viewModel::onAction,
            onNavigateToScreen = onNavigateToScreen,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    onNavigateToScreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = when (strings.navWorkouts) {
        "Mashg'ulotlar" -> "uz"
        "Workouts" -> "en"
        else -> "ru"
    }

    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf("custom") } // "custom" | "prebuilt" | "alpha"

    if (state.isLoading && state.prebuiltRoutines.isEmpty() && state.customRoutines.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            ObsidianLoadingCard(
                title = strings.stateLoadingTitle,
                hint = strings.stateLoadingHint,
                kicker = "01 · ${strings.navWorkouts}"
            )
        }
        return
    }

    val combinedRoutines = remember(state.customRoutines, state.prebuiltRoutines, searchQuery, sortMode) {
        val taggedCustom = state.customRoutines.map { true to it }
        val taggedPrebuilt = state.prebuiltRoutines.map { false to it }
        val ordered = when (sortMode) {
            "prebuilt" -> taggedPrebuilt + taggedCustom
            "alpha" -> (taggedCustom + taggedPrebuilt).sortedBy { it.second.title.lowercase() }
            else -> taggedCustom + taggedPrebuilt
        }
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) {
            ordered
        } else {
            ordered.filter { (_, r) ->
                r.title.lowercase().contains(q) ||
                    r.description.lowercase().contains(q) ||
                    r.exercises.any { it.exerciseName.lowercase().contains(q) }
            }
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // 1. Page Header ("Тренировки" / "Ваши программы и шаблоны")
        item(key = "page_header", contentType = "header") {
            ObsidianPageHeader(
                title = strings.navWorkouts,
                subtitle = when (lang) {
                    "uz" -> "Dasturlar va shablonlaringiz"
                    "en" -> "Your programs and templates"
                    else -> "Ваши программы и шаблоны"
                }
            )
        }

        // 2. Active Workout Banner (if in progress)
        if (state.hasActiveWorkout) {
            item(key = "active_workout_banner", contentType = "active_banner") {
                ActiveWorkoutBannerCard(
                    title = state.activeWorkoutTitle,
                    exercisesCount = state.activeWorkoutExercisesCount,
                    completedSetsCount = state.activeWorkoutCompletedSets,
                    lang = lang,
                    onResumeClick = { onAction(DashboardUiAction.OnResumeWorkoutClick) }
                )
            }
        }

        // 3. Primary Quick Actions Wrap
        item(key = "quick_actions", contentType = "actions") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ObsidianPrimaryButton(
                    text = when (lang) {
                        "uz" -> "Bo'sh mashg'ulotni boshlash"
                        "en" -> "Start empty workout"
                        else -> "Начать пустую тренировку"
                    },
                    onClick = { onAction(DashboardUiAction.OnStartEmptyWorkoutClick) }
                )
                ObsidianOutlineButton(
                    text = when (lang) {
                        "uz" -> "Yangi shablon"
                        "en" -> "New template"
                        else -> "Новый шаблон"
                    },
                    onClick = { onAction(DashboardUiAction.OnOpenRoutineEditor(null)) }
                )
            }
        }

        // 4. Active Program Schedule Card
        item(key = "active_program_schedule", contentType = "active_program") {
            ActiveProgramScheduleSection(
                bundle = state.activeProgramBundle,
                selectedDayNumber = state.selectedProgramDayNumber,
                lang = lang,
                onSelectDayTab = { onAction(DashboardUiAction.OnSelectProgramDayTab(it)) },
                onStartProgramDay = { onAction(DashboardUiAction.OnStartProgramDayClick(it)) },
                onSkipRestDay = { onAction(DashboardUiAction.OnSkipRestDayClick) },
                onInspectExercise = { id, name ->
                    onAction(DashboardUiAction.OnInspectExerciseById(id, name))
                },
                onNavigateToPrograms = { onNavigateToScreen("programs") }
            )
        }

        // 5. Templates Header & Search + Order Filter
        item(key = "templates_controls", contentType = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = when (lang) {
                        "uz" -> "Mening shablonlarim"
                        "en" -> "My templates"
                        else -> "Мои шаблоны"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ObsidianTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = when (lang) {
                            "uz" -> "Shablon qidirish"
                            "en" -> "Search templates"
                            else -> "Поиск шаблонов"
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    ObsidianOutlineButton(
                        text = when (sortMode) {
                            "prebuilt" -> when (lang) {
                                "uz" -> "Tartib: baza"
                                "en" -> "Order: starter"
                                else -> "Порядок: базовые"
                            }
                            "alpha" -> when (lang) {
                                "uz" -> "Tartib: A–Z"
                                "en" -> "Order: A–Z"
                                else -> "Порядок: А–Я"
                            }
                            else -> when (lang) {
                                "uz" -> "Tartib: meniki"
                                "en" -> "Order: mine"
                                else -> "Порядок: мои"
                            }
                        },
                        onClick = {
                            sortMode = when (sortMode) {
                                "custom" -> "prebuilt"
                                "prebuilt" -> "alpha"
                                else -> "custom"
                            }
                        }
                    )
                }
            }
        }

        // 6. Templates List
        if (combinedRoutines.isEmpty()) {
            item(key = "empty_templates", contentType = "empty") {
                ObsidianEmptyCard(
                    title = when (lang) {
                        "uz" -> "Shablonlar topilmadi"
                        "en" -> "No templates found"
                        else -> "Шаблоны не найдены"
                    },
                    description = when (lang) {
                        "uz" -> "Yangi shablon yarating yoki qidiruv so'zini tozalang."
                        "en" -> "Create a new template or clear your search filter."
                        else -> "Создайте новый шаблон тренировки или сбросьте поисковый запрос."
                    },
                    primaryActionLabel = when (lang) {
                        "uz" -> "Yangi shablon"
                        "en" -> "New template"
                        else -> "Новый шаблон"
                    },
                    onPrimaryAction = { onAction(DashboardUiAction.OnOpenRoutineEditor(null)) },
                    secondaryActionLabel = if (searchQuery.isNotBlank()) {
                        when (lang) {
                            "uz" -> "Qidiruvni tozalash"
                            "en" -> "Clear search"
                            else -> "Сбросить поиск"
                        }
                    } else null,
                    onSecondaryAction = { searchQuery = "" }
                )
            }
        } else {
            items(
                items = combinedRoutines,
                key = { (isCustom, r) -> "${if (isCustom) "custom" else "prebuilt"}_${r.id}" },
                contentType = { "routine_card" }
            ) { (isCustom, routine) ->
                RoutineCardItem(
                    routine = routine,
                    isCustom = isCustom,
                    lang = lang,
                    onStart = { onAction(DashboardUiAction.OnSelectRoutineClick(routine)) },
                    onPreview = { onAction(DashboardUiAction.OnPreviewRoutineClick(routine)) },
                    onEdit = if (isCustom) {
                        { onAction(DashboardUiAction.OnOpenRoutineEditor(routine)) }
                    } else null,
                    onDuplicate = {
                        val copySuffix = when (lang) {
                            "uz" -> "(nusxa)"
                            "en" -> "(copy)"
                            else -> "(копия)"
                        }
                        onAction(
                            DashboardUiAction.OnOpenRoutineEditor(
                                routine.copy(
                                    id = "prebuilt-copy-${routine.id}",
                                    name = "${routine.title} $copySuffix"
                                )
                            )
                        )
                    },
                    onDelete = if (isCustom) {
                        { onAction(DashboardUiAction.OnDeleteRoutineClick(routine.id)) }
                    } else null
                )
            }
        }
    }
}

@Composable
private fun ActiveWorkoutBannerCard(
    title: String,
    exercisesCount: Int,
    completedSetsCount: Int,
    lang: String,
    onResumeClick: () -> Unit
) {
    ObsidianCard(
        backgroundColor = EmeraldSubtle,
        borderColor = BorderHighlight
    ) {
        Text(
            text = when (lang) {
                "uz" -> "FAOL MASHG'ULOT"
                "en" -> "ACTIVE WORKOUT"
                else -> "ТРЕНИРОВКА АКТИВНА"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = EmeraldNeon
        )
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = when (lang) {
                "uz" -> "$exercisesCount mashq · $completedSetsCount bajarilgan yondashuv"
                "en" -> "$exercisesCount exercises · $completedSetsCount completed sets"
                else -> "$exercisesCount упражнений · $completedSetsCount выполненных подходов"
            },
            fontSize = 14.sp,
            color = TextSecondary
        )
        ObsidianPrimaryButton(
            text = when (lang) {
                "uz" -> "Davom etish"
                "en" -> "Resume workout"
                else -> "Продолжить"
            },
            onClick = onResumeClick
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveProgramScheduleSection(
    bundle: ActiveProgramBundle?,
    selectedDayNumber: Int,
    lang: String,
    onSelectDayTab: (Int) -> Unit,
    onStartProgramDay: (ProgramDay) -> Unit,
    onSkipRestDay: () -> Unit,
    onInspectExercise: (String, String) -> Unit,
    onNavigateToPrograms: () -> Unit
) {
    val program = bundle?.program
    val currentDayNumber = bundle?.userProgram?.currentDayNumber ?: 1

    if (program == null) {
        ObsidianCard(
            backgroundColor = DarkSurface,
            borderColor = BorderSubtle
        ) {
            Text(
                text = when (lang) {
                    "uz" -> "FAOL DASTUR"
                    "en" -> "ACTIVE PROGRAM"
                    else -> "АКТИВНАЯ ПРОГРАММА"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.8.sp,
                color = EmeraldNeon
            )
            Text(
                text = when (lang) {
                    "uz" -> "Faol dastur tanlanmagan"
                    "en" -> "No active program selected"
                    else -> "Нет активной программы"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Text(
                text = when (lang) {
                    "uz" -> "Katalogdan dastur tanlang (PPL, Upper/Lower, Full Body)"
                    "en" -> "Pick a structured split (PPL, Upper/Lower, Full Body) in the catalog"
                    else -> "Выберите структурированный сплит (PPL, Upper/Lower, Full Body) в каталоге"
                },
                fontSize = 14.sp,
                color = TextSecondary
            )
            ObsidianOutlineButton(
                text = when (lang) {
                    "uz" -> "Dasturlarni ochish"
                    "en" -> "Browse programs"
                    else -> "Открыть программы"
                },
                onClick = onNavigateToPrograms
            )
        }
        return
    }

    val days = program.days.sortedBy { it.dayNumber }
    val selectedDay = days.find { it.dayNumber == selectedDayNumber }
        ?: days.find { it.dayNumber == currentDayNumber }
        ?: days.firstOrNull()
    var showDaySelector by remember { mutableStateOf(false) }

    ObsidianCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle
    ) {
        Text(
            text = when (lang) {
                "uz" -> "FAOL DASTUR"
                "en" -> "ACTIVE PROGRAM"
                else -> "АКТИВНАЯ ПРОГРАММА"
            },
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.8.sp,
            color = EmeraldNeon
        )

        Text(
            text = when (lang) {
                "uz" -> "${selectedDay?.dayNumber ?: currentDayNumber}-kun / ${days.size} · ${program.title}"
                "en" -> "Day #${selectedDay?.dayNumber ?: currentDayNumber} of ${days.size} · ${program.title}"
                else -> "День #${selectedDay?.dayNumber ?: currentDayNumber} из ${days.size} · ${program.title}"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        Text(
            text = if (selectedDay == null) {
                program.description
            } else if (selectedDay.isRestDay || selectedDay.exercises.isEmpty()) {
                when (lang) {
                    "uz" -> "Dam olish kuni · Tiklanish"
                    "en" -> "Rest day · CNS & muscle recovery"
                    else -> "День отдыха · Восстановление ЦНС и сон"
                }
            } else {
                when (lang) {
                    "uz" -> "${selectedDay.title} · ${selectedDay.exercises.size} mashq"
                    "en" -> "${selectedDay.title} · ${selectedDay.exercises.size} exercises"
                    else -> "${selectedDay.title} · ${selectedDay.exercises.size} упражнений"
                }
            },
            fontSize = 14.sp,
            color = TextSecondary
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (selectedDay != null) {
                if (selectedDay.isRestDay || selectedDay.exercises.isEmpty()) {
                    ObsidianPrimaryButton(
                        text = when (lang) {
                            "uz" -> "Dam olishni o'tkazish"
                            "en" -> "Skip rest day"
                            else -> "Пропустить отдых"
                        },
                        onClick = onSkipRestDay
                    )
                } else {
                    ObsidianPrimaryButton(
                        text = when (lang) {
                            "uz" -> "Mashg'ulotni boshlash"
                            "en" -> "Start workout"
                            else -> "Начать тренировку"
                        },
                        onClick = { onStartProgramDay(selectedDay) }
                    )
                }
            }

            ObsidianOutlineButton(
                text = when (lang) {
                    "uz" -> "Kunni tanlash"
                    "en" -> "Choose day"
                    else -> "Выбрать день"
                },
                onClick = { showDaySelector = !showDaySelector }
            )

            ObsidianOutlineButton(
                text = when (lang) {
                    "uz" -> "Dasturni ochish"
                    "en" -> "Open program"
                    else -> "Открыть программу"
                },
                onClick = onNavigateToPrograms
            )
        }

        if (showDaySelector && days.isNotEmpty()) {
            ObsidianDivider(color = BorderSubtle)

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEach { day ->
                    val isSelected = day.dayNumber == (selectedDay?.dayNumber ?: 1)
                    ObsidianPillTab(
                        text = when (lang) {
                            "uz" -> "${day.dayNumber}-kun"
                            "en" -> "Day ${day.dayNumber}"
                            else -> "День ${day.dayNumber}"
                        },
                        selected = isSelected,
                        onClick = { onSelectDayTab(day.dayNumber) }
                    )
                }
            }

            if (selectedDay != null && selectedDay.exercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    selectedDay.exercises.forEach { ex ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInspectExercise(ex.exerciseId, ex.exerciseName) },
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${ex.targetSets} × ${ex.exerciseName}",
                                fontSize = 14.sp,
                                color = TextSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${ex.targetRepsMin}–${ex.targetRepsMax}",
                                fontSize = 13.sp,
                                color = TextMuted
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
private fun RoutineCardItem(
    routine: Routine,
    isCustom: Boolean,
    lang: String,
    onStart: () -> Unit,
    onPreview: () -> Unit,
    onEdit: (() -> Unit)?,
    onDuplicate: () -> Unit,
    onDelete: (() -> Unit)?
) {
    val estimatedMinutes = (routine.exercises.sumOf { it.targetSets } * 3).coerceAtLeast(20)
    ObsidianCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = routine.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            if (routine.description.isNotBlank()) {
                Text(
                    text = routine.description,
                    fontSize = 14.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Text(
                text = when (lang) {
                    "uz" -> "${routine.exercises.size} mashq · ~$estimatedMinutes daq"
                    "en" -> "${routine.exercises.size} exercises · ~$estimatedMinutes min"
                    else -> "${routine.exercises.size} упражнений · ~$estimatedMinutes мин"
                },
                fontSize = 13.sp,
                color = TextMuted
            )
        }

        if (routine.exercises.isNotEmpty()) {
            ObsidianDivider(color = BorderSubtle)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onPreview() },
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                routine.exercises.take(5).forEach { ex ->
                    Text(
                        text = "${ex.targetSets} × ${ex.exerciseName}",
                        fontSize = 14.sp,
                        color = TextSoft,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (routine.exercises.size > 5) {
                    Text(
                        text = "+${routine.exercises.size - 5} ещё",
                        fontSize = 13.sp,
                        color = EmeraldLight
                    )
                }
            }
        }

        ObsidianDivider(color = BorderSubtle)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ObsidianPrimaryButton(
                text = when (lang) {
                    "uz" -> "Boshlash"
                    "en" -> "Start"
                    else -> "Начать"
                },
                onClick = onStart
            )
            ObsidianOutlineButton(
                text = if (isCustom) {
                    when (lang) {
                        "uz" -> "Tahrirlash"
                        "en" -> "Edit"
                        else -> "Изменить"
                    }
                } else {
                    when (lang) {
                        "uz" -> "Tarkib"
                        "en" -> "Details"
                        else -> "Состав"
                    }
                },
                onClick = {
                    if (isCustom && onEdit != null) onEdit() else onPreview()
                }
            )
            ObsidianOutlineButton(
                text = when (lang) {
                    "uz" -> "Nusxalash"
                    "en" -> "Duplicate"
                    else -> "Копировать"
                },
                onClick = onDuplicate
            )
            if (isCustom && onDelete != null) {
                ObsidianOutlineButton(
                    text = when (lang) {
                        "uz" -> "O'chirish"
                        "en" -> "Delete"
                        else -> "Удалить"
                    },
                    onClick = onDelete
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineDetailBottomSheet(
    routine: Routine,
    isCustom: Boolean,
    onStartWorkout: () -> Unit,
    onEditRoutine: () -> Unit,
    onInspectExercise: (String, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                routine.exercises.forEachIndexed { idx, ex ->
                    AthleticCard(
                        backgroundColor = DarkSurfaceVariant,
                        borderColor = BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onInspectExercise(ex.exerciseId, ex.exerciseName) }
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(EmeraldSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${idx + 1}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = ex.exerciseName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${ex.targetSets} подходов × ${ex.targetRepsMin}–${ex.targetRepsMax} повторений",
                                        fontSize = 12.sp,
                                        color = EmeraldLight
                                    )
                                }
                            }
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Техника",
                                tint = TextSecondary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onEditRoutine,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isCustom) "Изменить" else "Копировать себе",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }

                Button(
                    onClick = onStartWorkout,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Начать тренировку", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoutineEditorBottomSheet(
    state: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.editingRoutineId != null) "Редактировать сплит" else "Новый сплит / рутина",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.editorTitle,
                onValueChange = { onAction(DashboardUiAction.OnEditorTitleChanged(it)) },
                label = { Text("Название (напр. День Груди и Бицепса)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianWell,
                    unfocusedContainerColor = ObsidianWell,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = state.editorDescription,
                onValueChange = { onAction(DashboardUiAction.OnEditorDescriptionChanged(it)) },
                label = { Text("Описание / заметка к сплиту") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = ObsidianWell,
                    unfocusedContainerColor = ObsidianWell,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "УПРАЖНЕНИЯ (${state.editorExercises.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                TextButton(onClick = { onAction(DashboardUiAction.OnOpenExercisePicker) }) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Добавить упражнение", color = EmeraldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.editorExercises.size) { idx ->
                    val ex = state.editorExercises[idx]
                    AthleticCard(
                        backgroundColor = DarkSurfaceVariant,
                        borderColor = BorderSubtle
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${idx + 1}. ${ex.exerciseName}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = { onAction(DashboardUiAction.OnRemoveExerciseFromEditor(idx)) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Удалить",
                                        tint = CrimsonRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = ex.targetSets.toString(),
                                    onValueChange = { v ->
                                        v.toIntOrNull()?.let { sets ->
                                            onAction(DashboardUiAction.OnUpdateEditorExerciseSets(idx, sets))
                                        }
                                    },
                                    label = { Text("Подходы", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ex.targetRepsMin.toString(),
                                    onValueChange = { v ->
                                        v.toIntOrNull()?.let { minR ->
                                            onAction(DashboardUiAction.OnUpdateEditorExerciseReps(idx, minR, ex.targetRepsMax))
                                        }
                                    },
                                    label = { Text("Мин повт.", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = ex.targetRepsMax.toString(),
                                    onValueChange = { v ->
                                        v.toIntOrNull()?.let { maxR ->
                                            onAction(DashboardUiAction.OnUpdateEditorExerciseReps(idx, ex.targetRepsMin, maxR))
                                        }
                                    },
                                    label = { Text("Макс повт.", fontSize = 10.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            PrimaryAthleticButton(
                text = "Сохранить сплит",
                onClick = { onAction(DashboardUiAction.OnSaveRoutineClick) }
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun TrainingHubQuickActions(
    onNavigateToScreen: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        Triple("programs", "Программы", Icons.Default.Layers),
        Triple("exercises", "Упражнения", Icons.Default.FitnessCenter),
        Triple("history", "История", Icons.Default.History),
        Triple("body", "Замеры тела", Icons.Default.MonitorWeight)
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items.forEach { (route, title, icon) ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
                    .clickable { onNavigateToScreen(route) }
                    .padding(vertical = 10.dp, horizontal = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = title,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
