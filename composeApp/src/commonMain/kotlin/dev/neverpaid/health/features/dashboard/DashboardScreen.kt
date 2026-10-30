package dev.neverpaid.health.features.dashboard

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
import androidx.compose.material.icons.filled.Info
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
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
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

@Composable
fun DashboardScreen(
    state: DashboardUiState,
    onAction: (DashboardUiAction) -> Unit,
    onNavigateToScreen: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.prebuiltRoutines.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "top_spacer") {
            Spacer(modifier = Modifier.height(2.dp))
        }

        // 1. Active Workout Banner (if in progress)
        if (state.hasActiveWorkout) {
            item(key = "active_workout_banner") {
                ActiveWorkoutBannerCard(
                    title = state.activeWorkoutTitle,
                    exercisesCount = state.activeWorkoutExercisesCount,
                    completedSetsCount = state.activeWorkoutCompletedSets,
                    onResumeClick = { onAction(DashboardUiAction.OnResumeWorkoutClick) }
                )
            }
        }

        // 2. Quick Start Hero Card (1:1 with WorkoutsPage.tsx)
        item(key = "quick_start_card") {
            QuickStartHeroCard(
                totalWorkouts = state.totalWorkouts,
                totalVolumeKg = state.totalVolumeKg,
                streakDays = state.currentStreakDays,
                onStartEmpty = { onAction(DashboardUiAction.OnStartEmptyWorkoutClick) },
                onCreateRoutine = { onAction(DashboardUiAction.OnOpenRoutineEditor(null)) }
            )
        }

        // 3. Active Program Schedule Card (GET /api/v1/programs/user/active)
        item(key = "active_program_schedule") {
            ActiveProgramScheduleSection(
                bundle = state.activeProgramBundle,
                selectedDayNumber = state.selectedProgramDayNumber,
                onSelectDayTab = { onAction(DashboardUiAction.OnSelectProgramDayTab(it)) },
                onStartProgramDay = { onAction(DashboardUiAction.OnStartProgramDayClick(it)) },
                onSkipRestDay = { onAction(DashboardUiAction.OnSkipRestDayClick) },
                onInspectExercise = { id, name ->
                    onAction(DashboardUiAction.OnInspectExerciseById(id, name))
                },
                onNavigateToPrograms = { onNavigateToScreen("programs") }
            )
        }

        // 4. My Custom Routines (GET /api/v1/routines)
        item(key = "my_routines_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "МОИ СПЛИТЫ И РУТИНЫ",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceHigh)
                            .padding(horizontal = 7.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "${state.customRoutines.size}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldSubtle)
                        .border(1.dp, BorderHighlight, RoundedCornerShape(8.dp))
                        .clickable { onAction(DashboardUiAction.OnOpenRoutineEditor(null)) }
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Создать сплит",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }
            }
        }

        if (state.customRoutines.isEmpty()) {
            item(key = "empty_custom_routines") {
                AthleticCard(
                    backgroundColor = DarkSurface,
                    borderColor = BorderSubtle
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "У вас пока нет собственных рутин",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Создайте свой тренировочный день или начните по готовым шаблонам ниже",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(state.customRoutines, key = { "custom_${it.id}" }) { routine ->
                RoutineCardItem(
                    routine = routine,
                    isCustom = true,
                    onStart = { onAction(DashboardUiAction.OnSelectRoutineClick(routine)) },
                    onPreview = { onAction(DashboardUiAction.OnPreviewRoutineClick(routine)) },
                    onEdit = { onAction(DashboardUiAction.OnOpenRoutineEditor(routine)) },
                    onDelete = { onAction(DashboardUiAction.OnDeleteRoutineClick(routine.id)) }
                )
            }
        }

        // 5. Starter Prebuilt Templates (5 PREBUILT_ROUTINES from WorkoutsPage.tsx)
        item(key = "prebuilt_routines_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ГОТОВЫЕ БАЗОВЫЕ ШАБЛОНЫ",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Каталог программ →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight,
                    modifier = Modifier.clickable { onNavigateToScreen("programs") }
                )
            }
        }

        items(state.prebuiltRoutines, key = { it.id }) { routine ->
            RoutineCardItem(
                routine = routine,
                isCustom = false,
                onStart = { onAction(DashboardUiAction.OnSelectRoutineClick(routine)) },
                onPreview = { onAction(DashboardUiAction.OnPreviewRoutineClick(routine)) },
                onEdit = null,
                onDelete = null
            )
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ActiveWorkoutBannerCard(
    title: String,
    exercisesCount: Int,
    completedSetsCount: Int,
    onResumeClick: () -> Unit
) {
    AthleticCard(
        backgroundColor = DarkSurfaceVariant,
        borderColor = EmeraldNeon
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onResumeClick() }
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EmeraldNeon)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "ТРЕНИРОВКА АКТИВНА",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$exercisesCount упражнений • $completedSetsCount выполненных подходов",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldNeon)
                    .padding(horizontal = 12.dp, vertical = 7.dp)
            ) {
                Text(
                    text = "Продолжить →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }
        }
    }
}

@Composable
private fun QuickStartHeroCard(
    totalWorkouts: Int,
    totalVolumeKg: Double,
    streakDays: Int,
    onStartEmpty: () -> Unit,
    onCreateRoutine: () -> Unit
) {
    AthleticCard(
        backgroundColor = DarkSurfaceVariant,
        borderColor = BorderHighlight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Тренировки",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Логируйте рабочие подходы, таймер отдыха и 1ПМ в реальном времени",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurface)
                    .padding(vertical = 10.dp, horizontal = 8.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatCounter(
                    title = "Тренировок",
                    value = "$totalWorkouts",
                    unit = "",
                    accentColor = EmeraldNeon
                )
                StatCounter(
                    title = "Объём",
                    value = StrengthCalculator.formatVolume(totalVolumeKg),
                    unit = "кг",
                    accentColor = IndigoElectric
                )
                StatCounter(
                    title = "Серия",
                    value = "$streakDays",
                    unit = "дн.",
                    accentColor = AmberWarm
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            PrimaryAthleticButton(
                text = "Начать пустую тренировку",
                onClick = onStartEmpty,
                icon = Icons.Default.ElectricBolt
            )
        }
    }
}

@Composable
private fun ActiveProgramScheduleSection(
    bundle: ActiveProgramBundle?,
    selectedDayNumber: Int,
    onSelectDayTab: (Int) -> Unit,
    onStartProgramDay: (ProgramDay) -> Unit,
    onSkipRestDay: () -> Unit,
    onInspectExercise: (String, String) -> Unit,
    onNavigateToPrograms: () -> Unit
) {
    val program = bundle?.program
    val currentDayNumber = bundle?.userProgram?.currentDayNumber ?: 1

    if (program == null) {
        AthleticCard(
            backgroundColor = DarkSurface,
            borderColor = BorderSubtle
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onNavigateToPrograms() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = EmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Нет активной программы",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Выберите программу (PPL, Upper/Lower, Full Body) в каталоге",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
                Text(
                    text = "Выбрать →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )
            }
        }
        return
    }

    val days = program.days.sortedBy { it.dayNumber }
    val selectedDay = days.find { it.dayNumber == selectedDayNumber } ?: days.firstOrNull()

    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderHighlight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldSubtle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "АКТИВНЫЙ ПЛАН",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "По очереди: День $currentDayNumber",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }

                Text(
                    text = "Сменить →",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = EmeraldLight,
                    modifier = Modifier.clickable { onNavigateToPrograms() }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = program.title,
                fontSize = 17.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Day selector tabs
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(days, key = { it.dayNumber }) { day ->
                    val isSelected = day.dayNumber == (selectedDay?.dayNumber ?: 1)
                    val isCurrentQueue = day.dayNumber == currentDayNumber
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) EmeraldNeon else DarkSurfaceVariant)
                            .border(
                                width = 1.dp,
                                color = if (isCurrentQueue && !isSelected) EmeraldLight else BorderSubtle,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { onSelectDayTab(day.dayNumber) }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isCurrentQueue) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(if (isSelected) Color.Black else EmeraldNeon)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                            }
                            Text(
                                text = "День ${day.dayNumber}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.Black else TextPrimary
                            )
                        }
                    }
                }
            }

            if (selectedDay != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(12.dp)
                ) {
                    if (selectedDay.isRestDay || selectedDay.exercises.isEmpty()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "День отдыха и восстановления",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Восстановление ЦНС, сон 8 часов и выполнение нормы белка.",
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            OutlinedButton(
                                onClick = onSkipRestDay,
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Default.SkipNext, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Пропустить день отдыха →", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Column {
                            Text(
                                text = selectedDay.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            selectedDay.exercises.forEachIndexed { idx, ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onInspectExercise(ex.exerciseId, ex.exerciseName) }
                                        .padding(vertical = 4.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${ex.exerciseName}",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${ex.targetSets} × ${ex.targetRepsMin}–${ex.targetRepsMax}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { onStartProgramDay(selectedDay) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp),
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldNeon,
                                    contentColor = Color.Black
                                )
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Начать День ${selectedDay.dayNumber}: ${selectedDay.title}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
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
    onStart: () -> Unit,
    onPreview: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onPreview() }
                .padding(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isCustom) IndigoSubtle else EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = if (isCustom) IndigoElectric else EmeraldLight,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = routine.title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (routine.description.isNotBlank()) {
                            Text(
                                text = routine.description,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                if (isCustom) {
                    Row {
                        if (onEdit != null) {
                            IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Редактировать",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (onDelete != null) {
                            IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Удалить",
                                    tint = CrimsonRed,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                routine.exercises.take(4).forEach { ex ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${ex.exerciseName} (${ex.targetSets}×${ex.targetRepsMin}-${ex.targetRepsMax})",
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }
                if (routine.exercises.size > 4) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(DarkSurfaceHigh)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "+${routine.exercises.size - 4} ещё",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onPreview,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Состав (${routine.exercises.size})", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Начать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
                value = state.editorDescription,
                onValueChange = { onAction(DashboardUiAction.OnEditorDescriptionChanged(it)) },
                label = { Text("Описание / заметка к сплиту") },
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
