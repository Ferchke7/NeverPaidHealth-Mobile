package dev.neverpaid.health.features.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExercisePickerBottomSheet
import dev.neverpaid.health.core.designsystem.ExerciseThumbnail
import dev.neverpaid.health.core.designsystem.ExerciseThumbnailSize
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymEmptyCard
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymKicker
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.LocalDudaStrings
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
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            GymLoadingCard(
                title = strings.stateLoadingTitle,
                hint = strings.stateLoadingHint
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // 1. Page Header + Quick Hub Shortcuts
        item(key = "page_header", contentType = "header") {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                GymPageHeader(title = strings.navWorkouts)
                TrainingHubQuickActions(onNavigateToScreen = onNavigateToScreen)
            }
        }

        // 2. Active Workout Hero Banner (if in progress)
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

        // 3. Quick Start Actions
        item(key = "quick_actions", contentType = "actions") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "+ Bo'sh mashg'ulot"
                        "en" -> "+ Empty workout"
                        else -> "+ Пустая тренировка"
                    },
                    onClick = { onAction(DashboardUiAction.OnStartEmptyWorkoutClick) },
                    modifier = Modifier.weight(1.3f),
                    fillWidth = true
                )
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "Yangi shablon"
                        "en" -> "New template"
                        else -> "Новый сплит"
                    },
                    onClick = { onAction(DashboardUiAction.OnOpenRoutineEditor(null)) },
                    modifier = Modifier.weight(1f),
                    fillWidth = true
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

        // 5. Templates Header, Search & Sort Chips
        item(key = "templates_controls", contentType = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                GymSectionTitle(
                    title = when (lang) {
                        "uz" -> "Shablonlar"
                        "en" -> "Templates"
                        else -> "Мои сплиты и шаблоны"
                    },
                    actionLabel = when (lang) {
                        "uz" -> "+ Yaratish"
                        "en" -> "+ Create"
                        else -> "+ Создать"
                    },
                    onAction = { onAction(DashboardUiAction.OnOpenRoutineEditor(null)) }
                )

                GymTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = when (lang) {
                        "uz" -> "Shablon yoki mashq qidirish..."
                        "en" -> "Search templates or exercises..."
                        else -> "Поиск по названию или упражнению..."
                    },
                    singleLine = true
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "custom" to when (lang) { "uz" -> "Meniki"; "en" -> "Mine"; else -> "Мои" },
                        "prebuilt" to when (lang) { "uz" -> "Bazaviy"; "en" -> "Starter"; else -> "Базовые" },
                        "alpha" to when (lang) { "uz" -> "A–Z"; "en" -> "A–Z"; else -> "А–Я" }
                    ).forEach { (key, label) ->
                        GymPillTab(
                            text = label,
                            selected = sortMode == key,
                            onClick = { sortMode = key }
                        )
                    }
                }
            }
        }

        // 6. Templates List
        if (combinedRoutines.isEmpty()) {
            item(key = "empty_templates", contentType = "empty") {
                GymEmptyCard(
                    title = when (lang) {
                        "uz" -> "Shablonlar topilmadi"
                        "en" -> "No templates found"
                        else -> "Шаблоны не найдены"
                    },
                    description = when (lang) {
                        "uz" -> "Yangi shablon yarating yoki qidiruv so'zini tozalang."
                        "en" -> "Create a new template or clear your search filter."
                        else -> "Создайте свой сплит или сбросьте фильтр поиска."
                    },
                    primaryActionLabel = when (lang) {
                        "uz" -> "Yangi shablon"
                        "en" -> "New template"
                        else -> "Новый сплит"
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
    GymCard(
        backgroundColor = AccentLime,
        contentPadding = PaddingValues(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "LIVE MASHG'ULOT"
                        "en" -> "LIVE WORKOUT"
                        else -> "ИДЁТ ТРЕНИРОВКА"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = GymInkOnLight.copy(alpha = 0.7f)
                )
                Text(
                    text = title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = GymInkOnLight,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = when (lang) {
                        "uz" -> "$exercisesCount mashq · $completedSetsCount yondashuv"
                        "en" -> "$exercisesCount exercises · $completedSetsCount sets done"
                        else -> "$exercisesCount упр. · $completedSetsCount подходов выполнено"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = GymInkOnLight.copy(alpha = 0.8f)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(GymInkOnLight)
                    .clickable(onClick = onResumeClick)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "Ochish"
                        "en" -> "Resume"
                        else -> "Продолжить"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = AccentLime
                )
            }
        }
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
        GymCard {
            GymKicker(
                text = when (lang) {
                    "uz" -> "DASTUR"
                    "en" -> "TRAINING PLAN"
                    else -> "ПРОГРАММА"
                }
            )
            Text(
                text = when (lang) {
                    "uz" -> "Faol dastur tanlanmagan"
                    "en" -> "No active program selected"
                    else -> "Выберите программу тренировок"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
            Text(
                text = when (lang) {
                    "uz" -> "PPL, Upper/Lower yoki Full Body dasturlarini katalogdan tanlang."
                    "en" -> "Pick a structured split (PPL, Upper/Lower, Full Body) from the catalog."
                    else -> "Готовые сплиты PPL, Upper/Lower и Full Body с расписанием по дням."
                },
                fontSize = 14.sp,
                color = TextSecondary
            )
            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "Dasturlar katalogi"
                    "en" -> "Browse programs"
                    else -> "Открыть каталог программ"
                },
                onClick = onNavigateToPrograms,
                fillWidth = true
            )
        }
        return
    }

    val days = program.days.sortedBy { it.dayNumber }
    val selectedDay = days.find { it.dayNumber == selectedDayNumber }
        ?: days.find { it.dayNumber == currentDayNumber }
        ?: days.firstOrNull()

    GymCard(backgroundColor = DarkSurface) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymKicker(
                text = when (lang) {
                    "uz" -> "BUGUNGI REJA · ${selectedDay?.dayNumber ?: currentDayNumber}/${days.size}-KUN"
                    "en" -> "TODAY'S PLAN · DAY ${selectedDay?.dayNumber ?: currentDayNumber}/${days.size}"
                    else -> "ПО ПРОГРАММЕ · ДЕНЬ ${selectedDay?.dayNumber ?: currentDayNumber} ИЗ ${days.size}"
                }
            )
            Text(
                text = when (lang) {
                    "uz" -> "Barchasi →"
                    "en" -> "Program →"
                    else -> "Вся программа →"
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = AccentLime,
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onNavigateToPrograms)
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        Text(
            text = program.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        if (days.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days, key = { it.dayNumber }) { day ->
                    val isSelected = day.dayNumber == (selectedDay?.dayNumber ?: 1)
                    GymPillTab(
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
        }

        if (selectedDay != null) {
            if (selectedDay.isRestDay || selectedDay.exercises.isEmpty()) {
                GymInnerWell {
                    Text(
                        text = when (lang) {
                            "uz" -> "Dam olish kuni · Asab tizimi va mushaklar tiklanishi"
                            "en" -> "Rest day · Muscle & CNS recovery"
                            else -> "День отдыха · Восстановление мышц и ЦНС"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }
                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "Dam olishni o'tkazish"
                        "en" -> "Skip rest day"
                        else -> "Пропустить день отдыха"
                    },
                    onClick = onSkipRestDay,
                    fillWidth = true
                )
            } else {
                Text(
                    text = "${selectedDay.title} · ${selectedDay.exercises.size} упр.",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary
                )

                GymInnerWell {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        selectedDay.exercises.forEach { ex ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onInspectExercise(ex.exerciseId, ex.exerciseName) },
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ExerciseThumbnail(
                                    exerciseName = ex.exerciseName,
                                    exerciseId = ex.exerciseId,
                                    size = ExerciseThumbnailSize.XS,
                                    onClick = { onInspectExercise(ex.exerciseId, ex.exerciseName) }
                                )
                                Text(
                                    text = ex.exerciseName,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${ex.targetSets} × ${ex.targetRepsMin}–${ex.targetRepsMax}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentLime
                                )
                            }
                        }
                    }
                }

                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "Mashg'ulotni boshlash"
                        "en" -> "Start workout"
                        else -> "Начать тренировку дня"
                    },
                    onClick = { onStartProgramDay(selectedDay) },
                    fillWidth = true
                )
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
    GymCard {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPreview() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = routine.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                if (routine.description.isNotBlank()) {
                    Text(
                        text = routine.description,
                        fontSize = 13.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(DarkSurfaceHigh)
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text(
                    text = "~$estimatedMinutes мин",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentLime
                )
            }
        }

        if (routine.exercises.isNotEmpty()) {
            GymInnerWell(
                modifier = Modifier.clickable { onPreview() }
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    routine.exercises.take(4).forEach { ex ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            ExerciseThumbnail(
                                exerciseName = ex.exerciseName,
                                exerciseId = ex.exerciseId,
                                size = ExerciseThumbnailSize.XS
                            )
                            Text(
                                text = ex.exerciseName,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSoft,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${ex.targetSets}×${ex.targetRepsMin}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                    if (routine.exercises.size > 4) {
                        Text(
                            text = "+ ещё ${routine.exercises.size - 4} упр.",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLime
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymPrimaryButton(
                text = when (lang) {
                    "uz" -> "Boshlash"
                    "en" -> "Start"
                    else -> "Начать"
                },
                onClick = onStart,
                modifier = Modifier.weight(1.2f),
                fillWidth = true
            )
            GymOutlineButton(
                text = if (isCustom) {
                    when (lang) { "uz" -> "Tahrir"; "en" -> "Edit"; else -> "Изменить" }
                } else {
                    when (lang) { "uz" -> "Tarkib"; "en" -> "View"; else -> "Состав" }
                },
                onClick = { if (isCustom && onEdit != null) onEdit() else onPreview() },
                modifier = Modifier.weight(1f),
                fillWidth = true
            )
            GymOutlineButton(
                text = when (lang) { "uz" -> "Nusxa"; "en" -> "Copy"; else -> "Копия" },
                onClick = onDuplicate
            )
            if (isCustom && onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceHigh)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Удалить",
                        tint = CrimsonRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
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
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = routine.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    if (routine.description.isNotBlank()) {
                        Text(
                            text = routine.description,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                routine.exercises.forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceHigh)
                            .clickable { onInspectExercise(ex.exerciseId, ex.exerciseName) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ExerciseThumbnail(
                                exerciseName = ex.exerciseName,
                                exerciseId = ex.exerciseId,
                                size = ExerciseThumbnailSize.SM,
                                onClick = { onInspectExercise(ex.exerciseId, ex.exerciseName) }
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = ex.exerciseName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${ex.targetSets} подходов × ${ex.targetRepsMin}–${ex.targetRepsMax} повторений",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AccentLime
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

            Spacer(modifier = Modifier.height(18.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GymOutlineButton(
                    text = if (isCustom) "Изменить" else "Копировать",
                    onClick = onEditRoutine,
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
                GymPrimaryButton(
                    text = "Начать тренировку",
                    onClick = onStartWorkout,
                    modifier = Modifier.weight(1.3f),
                    fillWidth = true
                )
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
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.9f)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (state.editingRoutineId != null) "Редактировать сплит" else "Новый сплит",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            GymTextField(
                value = state.editorTitle,
                onValueChange = { onAction(DashboardUiAction.OnEditorTitleChanged(it)) },
                placeholder = "Название (напр. Грудь + Бицепс)",
                singleLine = true
            )

            GymTextField(
                value = state.editorDescription,
                onValueChange = { onAction(DashboardUiAction.OnEditorDescriptionChanged(it)) },
                placeholder = "Описание или заметка к тренировке",
                singleLine = true
            )

            GymSectionTitle(
                title = "Упражнения (${state.editorExercises.size})",
                actionLabel = "+ Добавить",
                onAction = { onAction(DashboardUiAction.OnOpenExercisePicker) }
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(state.editorExercises.size) { idx ->
                    val ex = state.editorExercises[idx]
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(DarkSurfaceHigh)
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ExerciseThumbnail(
                                exerciseName = ex.exerciseName,
                                exerciseId = ex.exerciseId,
                                size = ExerciseThumbnailSize.XS
                            )
                            Text(
                                text = "${idx + 1}. ${ex.exerciseName}",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAction(DashboardUiAction.OnRemoveExerciseFromEditor(idx)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymLabeledField(
                                label = "Подходы",
                                value = ex.targetSets.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.let { sets ->
                                        onAction(DashboardUiAction.OnUpdateEditorExerciseSets(idx, sets))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = "Мин повт.",
                                value = ex.targetRepsMin.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.let { minR ->
                                        onAction(DashboardUiAction.OnUpdateEditorExerciseReps(idx, minR, ex.targetRepsMax))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = "Макс повт.",
                                value = ex.targetRepsMax.toString(),
                                onValueChange = { v ->
                                    v.toIntOrNull()?.let { maxR ->
                                        onAction(DashboardUiAction.OnUpdateEditorExerciseReps(idx, ex.targetRepsMin, maxR))
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            PrimaryAthleticButton(
                text = "Сохранить сплит",
                onClick = { onAction(DashboardUiAction.OnSaveRoutineClick) }
            )

            Spacer(modifier = Modifier.height(12.dp))
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
        Triple("body", "Замеры", Icons.Default.MonitorWeight)
    )

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        items.forEach { (route, title, icon) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .clickable { onNavigateToScreen(route) }
                    .padding(vertical = 12.dp, horizontal = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(AccentLimeSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = AccentLime,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Text(
                    text = title,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
