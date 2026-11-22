package dev.neverpaid.health.features.programs

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderLilac
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.EmeraldBg
import dev.neverpaid.health.core.designsystem.EmeraldBorder
import dev.neverpaid.health.core.designsystem.EmeraldSuccess
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
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.SurfaceLilac
import dev.neverpaid.health.core.designsystem.SurfaceWhite
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.AthleteProfile
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.ProgramDayExercise
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.TrainingProgram

@Composable
fun ProgramsRoute(
    viewModel: ProgramsViewModel,
    onNavigateToActiveWorkout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            ProgramsUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
            is ProgramsUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        ProgramsScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProgramsScreen(
    state: ProgramsUiState,
    onAction: (ProgramsUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = when (strings.navWorkouts) {
        "Mashg'ulotlar" -> "uz"
        "Workouts" -> "en"
        else -> "ru"
    }

    if (state.isLoading && state.programs.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(horizontal = 20.dp, vertical = 24.dp)
        ) {
            GymLoadingCard(
                title = strings.stateLoadingTitle,
                hint = strings.stateLoadingHint
            )
        }
        return
    }

    if (state.detailProgram != null) {
        val detail = state.detailProgram!!
        val canEditDetail = detail.authorUserId == state.currentUserId && state.currentUserId.isNotBlank()
        ProgramDetailBottomSheet(
            program = detail,
            canEdit = canEditDetail,
            lang = lang,
            onActivate = { onAction(ProgramsUiAction.OnActivateProgram(detail.id)) },
            onDeactivate = { onAction(ProgramsUiAction.OnDeactivateProgram) },
            onStartDay = { day -> onAction(ProgramsUiAction.OnStartProgramDay(detail, day)) },
            onEdit = { onAction(ProgramsUiAction.OnOpenEditor(detail)) },
            onDelete = { onAction(ProgramsUiAction.OnDeleteProgram(detail.id)) },
            onOpenComments = { onAction(ProgramsUiAction.OnOpenComments(detail.id)) },
            onDismiss = { onAction(ProgramsUiAction.OnOpenProgramDetail(null)) }
        )
    }

    if (state.activeCommentsProgramId != null) {
        ProgramCommentsBottomSheet(
            comments = state.comments,
            currentUserId = state.currentUserId,
            inputText = state.newCommentText,
            replyingTo = state.replyingToComment,
            lang = lang,
            onTextChanged = { onAction(ProgramsUiAction.OnCommentTextChanged(it)) },
            onReply = { onAction(ProgramsUiAction.OnReplyClick(it)) },
            onCancelReply = { onAction(ProgramsUiAction.OnCancelReply) },
            onUpvote = { onAction(ProgramsUiAction.OnUpvoteComment(it)) },
            onDelete = { onAction(ProgramsUiAction.OnDeleteComment(it)) },
            onSend = { onAction(ProgramsUiAction.OnSendComment) },
            onDismiss = { onAction(ProgramsUiAction.OnCloseComments) }
        )
    }

    if (state.athleteProfile != null) {
        AthleteProfileBottomSheet(
            profile = state.athleteProfile!!,
            lang = lang,
            onSelectProgram = { prog ->
                onAction(ProgramsUiAction.OnCloseAthleteProfile)
                onAction(ProgramsUiAction.OnOpenProgramDetail(prog))
            },
            onDismiss = { onAction(ProgramsUiAction.OnCloseAthleteProfile) }
        )
    }

    if (state.isEditorOpen) {
        ProgramEditorBottomSheet(
            existingProgram = state.editingProgram,
            allExercises = state.allExercises,
            lang = lang,
            onSave = { req ->
                onAction(ProgramsUiAction.OnSaveProgram(req, state.editingProgram?.id))
            },
            onDismiss = { onAction(ProgramsUiAction.OnCloseEditor) }
        )
    }

    val filteredPrograms = remember(state.programs, state.activeTab, state.selectedSplitFilter, state.searchQuery, state.currentUserId) {
        state.programs.filter { prog ->
            val matchesTab = when (state.activeTab) {
                "official" -> prog.isPrebuilt || prog.authorUserId.isBlank() || prog.authorName.equals("duda.uz", ignoreCase = true) || prog.authorName.equals("NeverPaidHealth", ignoreCase = true)
                "mine" -> prog.authorUserId.isNotBlank() && prog.authorUserId == state.currentUserId
                else -> prog.isPublic && !prog.isPrebuilt
            }
            val matchesSplit = state.selectedSplitFilter == "all" ||
                prog.splitType.equals(state.selectedSplitFilter, ignoreCase = true)
            val matchesQuery = state.searchQuery.isBlank() ||
                prog.title.contains(state.searchQuery, ignoreCase = true) ||
                prog.description.contains(state.searchQuery, ignoreCase = true) ||
                prog.authorName.contains(state.searchQuery, ignoreCase = true)
            matchesTab && matchesSplit && matchesQuery
        }
    }

    val activeProgram = remember(state.programs) { state.programs.find { it.isActive } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header", contentType = "header") {
            RoyalHeroCard(
                modifier = Modifier.padding(bottom = 8.dp),
                bottomExtraPadding = 20.dp
            ) {
                Text(
                    text = strings.navPrograms,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.7).sp,
                    color = InkWhite
                )
            }
        }

        if (activeProgram != null) {
            item(key = "active_program_banner", contentType = "active_program") {
                ActiveProgramHeroCard(
                    program = activeProgram,
                    lang = lang,
                    onStartDay = { day -> onAction(ProgramsUiAction.OnStartProgramDay(activeProgram, day)) },
                    onOpenDetail = { onAction(ProgramsUiAction.OnOpenProgramDetail(activeProgram)) },
                    onOpenComments = { onAction(ProgramsUiAction.OnOpenComments(activeProgram.id)) },
                    onDeactivate = { onAction(ProgramsUiAction.OnDeactivateProgram) },
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
        }

        item(key = "controls", contentType = "controls") {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "Kutubxona"
                            "en" -> "Library"
                            else -> "Библиотека"
                        },
                        selected = state.activeTab == "official",
                        onClick = { onAction(ProgramsUiAction.OnSelectTab("official")) }
                    )
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "Hamjamiyat"
                            "en" -> "Community"
                            else -> "Сообщество"
                        },
                        selected = state.activeTab == "community",
                        onClick = { onAction(ProgramsUiAction.OnSelectTab("community")) }
                    )
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "Mening"
                            "en" -> "Mine"
                            else -> "Мои сплиты"
                        },
                        selected = state.activeTab == "mine",
                        onClick = { onAction(ProgramsUiAction.OnSelectTab("mine")) }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GymTextField(
                        value = state.searchQuery,
                        onValueChange = { onAction(ProgramsUiAction.OnSearchQueryChanged(it)) },
                        placeholder = when (lang) {
                            "uz" -> "Dastur qidirish"
                            "en" -> "Search programs"
                            else -> "Поиск программ"
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    GymPrimaryButton(
                        text = when (lang) {
                            "uz" -> "+ Yaratish"
                            "en" -> "+ Create"
                            else -> "+ Создать"
                        },
                        onClick = { onAction(ProgramsUiAction.OnOpenEditor(null)) }
                    )
                }

                val splits = listOf(
                    "all" to when (lang) { "uz" -> "Barchasi"; "en" -> "All"; else -> "Все" },
                    "ppl" to "PPL",
                    "upper_lower" to "Upper / Lower",
                    "full_body" to "Full Body",
                    "custom" to when (lang) { "uz" -> "Shaxsiy"; "en" -> "Custom"; else -> "Авторский" }
                )
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(splits, key = { it.first }) { (key, label) ->
                        GymPillTab(
                            text = label,
                            selected = state.selectedSplitFilter == key,
                            onClick = { onAction(ProgramsUiAction.OnSelectSplitFilter(key)) }
                        )
                    }
                }
            }
        }

        item(key = "catalog_heading", contentType = "section_heading") {
            GymSectionTitle(
                title = when (state.activeTab) {
                    "community" -> when (lang) { "uz" -> "Hamjamiyat"; "en" -> "Community"; else -> "Сообщество" }
                    "mine" -> when (lang) { "uz" -> "Mening splitlarim"; "en" -> "My splits"; else -> "Мои сплиты" }
                    else -> when (lang) { "uz" -> "Dasturlar katalogi"; "en" -> "Program catalog"; else -> "Каталог программ" }
                },
                modifier = Modifier.padding(bottom = 4.dp)
            )
        }

        if (filteredPrograms.isEmpty()) {
            item(key = "empty_programs", contentType = "empty") {
                GymEmptyCard(
                    title = when (lang) {
                        "uz" -> "Dasturlar topilmadi"
                        "en" -> "No programs found"
                        else -> "Программы не найдены"
                    },
                    description = when (lang) {
                        "uz" -> "Filtrni o'zgartiring yoki shaxsiy dastur tuzing."
                        "en" -> "Adjust filters or build your own training program."
                        else -> "Измените фильтр или создайте собственную программу тренировок."
                    },
                    primaryActionLabel = when (lang) {
                        "uz" -> "Dastur yaratish"
                        "en" -> "Create program"
                        else -> "Создать программу"
                    },
                    onPrimaryAction = { onAction(ProgramsUiAction.OnOpenEditor(null)) }
                )
            }
        } else {
            itemsIndexed(
                items = filteredPrograms,
                key = { idx, p -> "${p.id}_$idx" },
                contentType = { _, _ -> "program_card" }
            ) { _, program ->
                val isOwn = program.authorUserId == state.currentUserId && state.currentUserId.isNotBlank()
                ProgramCard(
                    program = program,
                    isCommunityTab = state.activeTab == "community",
                    isOwn = isOwn,
                    lang = lang,
                    onOpenDetail = { onAction(ProgramsUiAction.OnOpenProgramDetail(program)) },
                    onActivate = { onAction(ProgramsUiAction.OnActivateProgram(program.id)) },
                    onEdit = { onAction(ProgramsUiAction.OnOpenEditor(program)) },
                    onDelete = { onAction(ProgramsUiAction.OnDeleteProgram(program.id)) },
                    onOpenComments = { onAction(ProgramsUiAction.OnOpenComments(program.id)) },
                    onOpenAuthor = {
                        if (program.authorUserId.isNotBlank()) {
                            onAction(ProgramsUiAction.OnOpenAthleteProfile(program.authorUserId))
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ActiveProgramHeroCard(
    program: TrainingProgram,
    lang: String,
    onStartDay: (ProgramDay) -> Unit,
    onOpenDetail: () -> Unit,
    onOpenComments: () -> Unit = {},
    onDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sortedDays = remember(program.days) { program.days.sortedBy { it.dayNumber } }
    var selectedDayIndex by remember(program.id, sortedDays.size) {
        val firstWorkoutIdx = sortedDays.indexOfFirst { !it.isRestDay && it.exercises.isNotEmpty() }
        mutableIntStateOf(if (firstWorkoutIdx >= 0) firstWorkoutIdx else 0)
    }
    val safeIndex = selectedDayIndex.coerceIn(0, (sortedDays.size - 1).coerceAtLeast(0))
    val selectedDay = sortedDays.getOrNull(safeIndex)

    GymCard(
        modifier = modifier,
        backgroundColor = SurfaceWhite,
        borderColor = BorderHighlight
    ) {
        val strings = LocalDudaStrings.current
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            GymKicker(
                text = when (lang) {
                    "uz" -> "Faol dastur"
                    "en" -> "Active program"
                    else -> "Активная программа"
                },
                color = AccentLime
            )
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(EmeraldBg)
                    .padding(horizontal = 10.dp, vertical = 4.dp)
            ) {
                Text(
                    text = strings.dashboardPersonalCopyAutoSave,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldSuccess,
                    maxLines = 1
                )
            }
        }

        Text(
            text = program.title,
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.5).sp,
            color = TextPrimary
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProgramMetaPill(
                text = formatDaysPerWeekLabel(program.daysPerWeek, lang),
                highlight = true
            )
            ProgramMetaPill(
                text = formatSplitLabel(program.splitType, lang)
            )
            ProgramMetaPill(
                text = formatLevelLabel(program.difficulty, lang)
            )
        }

        if (sortedDays.isNotEmpty()) {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(sortedDays, key = { idx, d -> "${idx}_${d.dayNumber}" }) { idx, day ->
                    GymPillTab(
                        text = "${if (lang == "en") "Day" else if (lang == "uz") "Kun" else "День"} ${day.dayNumber} · ${day.title}",
                        selected = idx == safeIndex,
                        onClick = { selectedDayIndex = idx }
                    )
                }
            }
        }

        if (selectedDay != null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (selectedDay.isRestDay || selectedDay.exercises.isEmpty()) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Dam olish va tiklanish kuni"
                            "en" -> "Rest & recovery day"
                            else -> "День отдыха и восстановления"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                } else {
                    selectedDay.exercises.forEach { ex ->
                        ProgramExerciseRow(exercise = ex)
                    }
                }
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymPrimaryButton(
                text = when (lang) {
                    "uz" -> "Davom etish"
                    "en" -> "Start day"
                    else -> "Продолжить"
                },
                onClick = {
                    val targetDay = selectedDay?.takeIf { !it.isRestDay && it.exercises.isNotEmpty() }
                        ?: sortedDays.firstOrNull { !it.isRestDay && it.exercises.isNotEmpty() }
                    if (targetDay != null) {
                        onStartDay(targetDay)
                    } else {
                        onOpenDetail()
                    }
                }
            )
            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "Batafsil"
                    "en" -> "Details"
                    else -> "Подробнее"
                },
                onClick = onOpenDetail
            )
            GymOutlineButton(
                text = "${if (lang == "en") "Comments" else if (lang == "uz") "Izohlar" else "Комментарии"} · ${program.commentsCount}",
                onClick = onOpenComments
            )
            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "O'chirish"
                    "en" -> "Deactivate"
                    else -> "Снять"
                },
                textColor = TextSecondary,
                onClick = onDeactivate
            )
        }
    }
}

@Composable
private fun ProgramExerciseRow(
    exercise: ProgramDayExercise,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ExerciseThumbnail(
                exerciseName = exercise.exerciseName,
                exerciseId = exercise.exerciseId,
                size = ExerciseThumbnailSize.SM
            )
            Text(
                text = exercise.exerciseName,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        SetRepBadge(
            sets = exercise.targetSets,
            repsMin = exercise.targetRepsMin,
            repsMax = exercise.targetRepsMax
        )
    }
}

@Composable
private fun SetRepBadge(
    sets: Int,
    repsMin: Int,
    repsMax: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "$sets × $repsMin–$repsMax",
            fontSize = 13.sp,
            fontWeight = FontWeight.ExtraBold,
            color = AccentLime,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun ProgramMetaPill(
    text: String,
    highlight: Boolean = false,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(if (highlight) AccentLime else DarkSurfaceHigh)
            .padding(horizontal = 12.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (highlight) GymInkOnLight else TextSoft,
            maxLines = 1
        )
    }
}

private fun formatDaysPerWeekLabel(daysPerWeek: Int, lang: String): String = when (lang) {
    "uz" -> "$daysPerWeek kun / hafta"
    "en" -> "$daysPerWeek days / wk"
    else -> "$daysPerWeek дн / нед"
}

private fun formatLevelLabel(level: String, lang: String): String = when (level.lowercase()) {
    "beginner", "novice" -> when (lang) { "uz" -> "Boshlang'ich"; "en" -> "Beginner"; else -> "Новичок" }
    "advanced", "pro" -> when (lang) { "uz" -> "Pro"; "en" -> "Advanced"; else -> "Продвинутый" }
    else -> when (lang) { "uz" -> "O'rta"; "en" -> "Intermediate"; else -> "Средний" }
}

private fun formatSplitLabel(splitType: String, lang: String): String = when (splitType.lowercase()) {
    "ppl" -> "PPL · Гипертрофия"
    "upper_lower" -> "Upper / Lower · Сила"
    "full_body" -> "Full Body · База"
    else -> when (lang) { "uz" -> "Shaxsiy split"; "en" -> "Custom split"; else -> "Сплит" }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ProgramCard(
    program: TrainingProgram,
    isCommunityTab: Boolean,
    isOwn: Boolean,
    lang: String,
    onOpenDetail: () -> Unit,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenAuthor: () -> Unit
) {
    GymCard(
        backgroundColor = DarkSurface,
        borderColor = if (program.isActive) BorderHighlight else Color.Transparent
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onOpenDetail() },
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = program.title,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.4).sp,
                color = TextPrimary
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProgramMetaPill(
                    text = formatDaysPerWeekLabel(program.daysPerWeek, lang),
                    highlight = program.isActive
                )
                ProgramMetaPill(
                    text = formatLevelLabel(program.difficulty, lang)
                )
                ProgramMetaPill(
                    text = formatSplitLabel(program.splitType, lang)
                )
            }

            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (program.days.isNotEmpty()) {
                Text(
                    text = program.days.joinToString(" · ") { it.title },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSoft,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (program.isActive) {
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "O'rnatilgan"
                        "en" -> "Active"
                        else -> "Установлена"
                    },
                    textColor = AccentLime,
                    borderColor = BorderHighlight,
                    onClick = onOpenDetail
                )
            } else {
                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "O'rnatish"
                        "en" -> "Install"
                        else -> "Установить"
                    },
                    onClick = onActivate
                )
            }

            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "Batafsil"
                    "en" -> "Details"
                    else -> "Подробнее"
                },
                onClick = onOpenDetail
            )

            if (isOwn) {
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "Tahrirlash"
                        "en" -> "Edit"
                        else -> "Изменить"
                    },
                    onClick = onEdit
                )
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "O'chirish"
                        "en" -> "Delete"
                        else -> "Удалить"
                    },
                    textColor = CrimsonRed,
                    onClick = onDelete
                )
            }

            if (isCommunityTab && program.authorName.isNotBlank()) {
                GymOutlineButton(
                    text = program.authorName.replace("NeverPaidHealth", "duda.uz"),
                    textColor = TextSecondary,
                    onClick = onOpenAuthor
                )
            }
            val strings = LocalDudaStrings.current
            if (program.installsCount > 0) {
                ProgramMetaPill(
                    text = "${program.installsCount} ${strings.programsInstallsLabel}",
                    highlight = false
                )
            }
            GymOutlineButton(
                text = "${if (lang == "en") "Comments" else if (lang == "uz") "Izohlar" else "Комментарии"} · ${program.commentsCount}",
                onClick = onOpenComments
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProgramDetailBottomSheet(
    program: TrainingProgram,
    canEdit: Boolean,
    lang: String,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit,
    onStartDay: (ProgramDay) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenComments: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sortedDays = remember(program.days) { program.days.sortedBy { it.dayNumber } }
    var selectedDayNumber by remember(program.id) {
        mutableIntStateOf(sortedDays.firstOrNull()?.dayNumber ?: 1)
    }

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
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "detail_header") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = program.title,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ProgramMetaPill(
                            text = formatDaysPerWeekLabel(program.daysPerWeek, lang),
                            highlight = program.isActive
                        )
                        ProgramMetaPill(
                            text = formatLevelLabel(program.difficulty, lang)
                        )
                        ProgramMetaPill(
                            text = formatSplitLabel(program.splitType, lang)
                        )
                    }
                    if (program.description.isNotBlank()) {
                        Text(
                            text = program.description,
                            fontSize = 14.sp,
                            lineHeight = 20.sp,
                            color = TextSecondary
                        )
                    }
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (program.isActive) {
                            GymOutlineButton(
                                text = when (lang) { "uz" -> "Faolsizlantirish"; "en" -> "Deactivate"; else -> "Деактивировать" },
                                onClick = onDeactivate
                            )
                        } else {
                            GymPrimaryButton(
                                text = when (lang) { "uz" -> "Faollashtirish"; "en" -> "Activate"; else -> "Активировать" },
                                onClick = onActivate
                            )
                        }
                        if (canEdit) {
                            GymOutlineButton(
                                text = when (lang) { "uz" -> "Tahrirlash"; "en" -> "Edit"; else -> "Изменить" },
                                onClick = onEdit
                            )
                            GymOutlineButton(
                                text = when (lang) { "uz" -> "O'chirish"; "en" -> "Delete"; else -> "Удалить" },
                                textColor = CrimsonRed,
                                onClick = onDelete
                            )
                        }
                        GymOutlineButton(
                            text = "${if (lang == "en") "Discussion" else if (lang == "uz") "Muhokama" else "Обсуждение"} · ${program.commentsCount}",
                            onClick = onOpenComments
                        )
                    }

                    if (sortedDays.size > 1) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            itemsIndexed(sortedDays, key = { idx, d -> "tab_${idx}_${d.dayNumber}" }) { _, d ->
                                GymPillTab(
                                    text = "${if (lang == "en") "Day" else if (lang == "uz") "Kun" else "День"} ${d.dayNumber}",
                                    selected = selectedDayNumber == d.dayNumber,
                                    onClick = { selectedDayNumber = d.dayNumber }
                                )
                            }
                        }
                    }
                }
            }

            itemsIndexed(sortedDays, key = { idx, day -> "card_${idx}_${day.dayNumber}" }) { _, day ->
                val isHighlighted = day.dayNumber == selectedDayNumber
                GymCard(
                    backgroundColor = GymWell,
                    borderColor = if (isHighlighted) BorderHighlight else Color.Transparent
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            GymKicker(
                                text = "${if (lang == "en") "Day" else if (lang == "uz") "Kun" else "День"} ${day.dayNumber}",
                                color = if (isHighlighted) AccentLime else TextSecondary
                            )
                            Text(
                                text = day.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = if (day.isRestDay || day.exercises.isEmpty()) {
                                    when (lang) { "uz" -> "Dam olish kuni"; "en" -> "Rest day"; else -> "День отдыха" }
                                } else {
                                    "${day.exercises.size} ${if (lang == "en") "exercises" else if (lang == "uz") "mashq" else "упражнений"} · ${day.exercises.sumOf { it.targetSets }} ${if (lang == "en") "sets" else if (lang == "uz") "yondashuv" else "подходов"}"
                                },
                                fontSize = 13.sp,
                                color = TextSecondary
                            )
                        }
                        if (!day.isRestDay && day.exercises.isNotEmpty()) {
                            GymPrimaryButton(
                                text = when (lang) { "uz" -> "Boshlash"; "en" -> "Start"; else -> "Старт" },
                                onClick = { onStartDay(day) }
                            )
                        }
                    }

                    if (day.exercises.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            day.exercises.forEach { ex ->
                                ProgramExerciseRow(exercise = ex)
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
private fun AthleteProfileBottomSheet(
    profile: AthleteProfile,
    lang: String,
    onSelectProgram: (TrainingProgram) -> Unit,
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(AccentLimeSubtle),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentLime
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = profile.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    val strings = LocalDudaStrings.current
                    Text(
                        text = when (lang) {
                            "uz" -> "${profile.totalWorkouts} mashg'ulot · ${StrengthCalculator.formatVolume(profile.totalVolumeKg)} kg · ${profile.publicPrograms.size} ${strings.programsPublicCount}"
                            "en" -> "${profile.totalWorkouts} workouts · ${StrengthCalculator.formatVolume(profile.totalVolumeKg)} kg · ${profile.publicPrograms.size} ${strings.programsPublicCount}"
                            else -> "${profile.totalWorkouts} тренировок · ${StrengthCalculator.formatVolume(profile.totalVolumeKg)} кг · ${profile.publicPrograms.size} ${strings.programsPublicCount}"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            if (profile.publicPrograms.isNotEmpty()) {
                Text(
                    text = when (lang) {
                        "uz" -> "Sportchi dasturlari"
                        "en" -> "Athlete programs"
                        else -> "Программы атлета"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    profile.publicPrograms.take(4).forEach { prog ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceHigh)
                                .clickable { onSelectProgram(prog) }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Text(
                                    text = prog.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val strings = LocalDudaStrings.current
                                Text(
                                    text = "${formatDaysPerWeekLabel(prog.daysPerWeek, lang)} · ${prog.installsCount} ${strings.programsInstallsLabel}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                            Text(
                                text = when (lang) { "uz" -> "Ochish"; "en" -> "Open"; else -> "Открыть" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentLime
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProgramEditorBottomSheet(
    existingProgram: TrainingProgram?,
    allExercises: List<Exercise>,
    lang: String,
    onSave: (SaveProgramRequestDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingProgram?.title ?: "") }
    var description by remember { mutableStateOf(existingProgram?.description ?: "") }
    val splitType by remember { mutableStateOf(existingProgram?.splitType ?: "ppl") }
    val difficulty by remember { mutableStateOf(existingProgram?.difficulty ?: "intermediate") }
    var isPublic by remember { mutableStateOf(existingProgram?.isPublic ?: true) }
    val days = remember {
        mutableStateListOf<ProgramDay>().apply {
            if (existingProgram != null && existingProgram.days.isNotEmpty()) {
                addAll(existingProgram.days.sortedBy { it.dayNumber })
            } else {
                add(ProgramDay(dayNumber = 1, name = "Push Hypertrophy", exercises = emptyList()))
            }
        }
    }
    var selectedDayIndex by remember { mutableIntStateOf(0) }
    var pickingForDayIndex by remember { mutableIntStateOf(-1) }
    var replacingSlot by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var expandedSetsExIdx by remember { mutableIntStateOf(-1) }

    if (pickingForDayIndex >= 0 || replacingSlot != null) {
        val targetDayIdx = replacingSlot?.first ?: pickingForDayIndex
        ExercisePickerBottomSheet(
            exercises = allExercises,
            title = if (replacingSlot != null) {
                when (lang) { "uz" -> "Mashqni almashtirish"; "en" -> "Replace exercise"; else -> "Заменить упражнение" }
            } else {
                when (lang) { "uz" -> "${targetDayIdx + 1}-kunga mashq qo'shish"; "en" -> "Add exercise to Day ${targetDayIdx + 1}"; else -> "Добавить упражнение в День ${targetDayIdx + 1}" }
            },
            onSelectExercise = { ex ->
                val replacePair = replacingSlot
                if (replacePair != null && replacePair.first in days.indices) {
                    val d = days[replacePair.first]
                    val updated = d.exercises.mapIndexed { idx, old ->
                        if (idx == replacePair.second) old.copy(exerciseId = ex.id, exerciseName = ex.name) else old
                    }
                    days[replacePair.first] = d.copy(exercises = updated)
                } else if (targetDayIdx in days.indices) {
                    val currentDay = days[targetDayIdx]
                    val nextItem = ProgramDayExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        orderIndex = currentDay.exercises.size + 1,
                        targetSets = 3,
                        targetRepsMin = 8,
                        targetRepsMax = 12
                    )
                    days[targetDayIdx] = currentDay.copy(exercises = currentDay.exercises + nextItem)
                }
                pickingForDayIndex = -1
                replacingSlot = null
            },
            onDismiss = {
                pickingForDayIndex = -1
                replacingSlot = null
            }
        )
    }

    val safeDayIndex = selectedDayIndex.coerceIn(0, (days.size - 1).coerceAtLeast(0))
    val activeDay = days.getOrNull(safeDayIndex)

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
                .fillMaxHeight(0.92f)
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (existingProgram != null) {
                            when (lang) { "uz" -> "Dastur muharriri"; "en" -> "Program editor"; else -> "Редактор программы" }
                        } else {
                            when (lang) { "uz" -> "Yangi dastur"; "en" -> "Program builder"; else -> "Конструктор программы" }
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )
                    Text(
                        text = when (lang) {
                            "uz" -> "Kunlar, mashqlar va ishchi yondashuvlar"
                            "en" -> "Days, exercises and working sets"
                            else -> "Дни, упражнения и рабочие подходы"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GymLabeledField(
                    label = when (lang) { "uz" -> "Dastur nomi"; "en" -> "Program name"; else -> "Название программы" },
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Push Pull Legs"
                )

                GymLabeledField(
                    label = when (lang) { "uz" -> "Tavsif"; "en" -> "Description"; else -> "Описание" },
                    value = description,
                    onValueChange = { description = it },
                    placeholder = when (lang) { "uz" -> "Maqsad va tavsiyalar..."; "en" -> "Focus and training split..."; else -> "Акцент и рекомендации..." },
                    singleLine = false,
                    minLines = 2
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(DarkSurfaceHigh)
                        .clickable { isPublic = !isPublic }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text(
                        text = when (lang) {
                            "uz" -> "Hamjamiyat katalogida nashr etish"
                            "en" -> "Publish to duda.uz community catalog"
                            else -> "Опубликовать в каталоге сообщества duda.uz"
                        },
                        fontSize = 13.sp,
                        color = TextSoft
                    )
                }

                GymSectionTitle(
                    title = when (lang) { "uz" -> "Dastur tuzilishi"; "en" -> "Program structure"; else -> "Структура программы" }
                )

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(days, key = { idx, _ -> idx }) { idx, day ->
                        GymPillTab(
                            text = "${if (lang == "en") "Day" else if (lang == "uz") "Kun" else "День"} ${day.dayNumber} · ${day.title.take(14)}",
                            selected = idx == safeDayIndex,
                            onClick = {
                                selectedDayIndex = idx
                                expandedSetsExIdx = -1
                            }
                        )
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymOutlineButton(
                        text = when (lang) { "uz" -> "+ Kun qo'shish"; "en" -> "+ Add day"; else -> "+ Добавить день" },
                        onClick = {
                            val nextNum = days.size + 1
                            days.add(ProgramDay(dayNumber = nextNum, name = "День $nextNum", exercises = emptyList()))
                            selectedDayIndex = days.lastIndex
                            expandedSetsExIdx = -1
                        }
                    )
                    if (days.size > 1) {
                        GymOutlineButton(
                            text = when (lang) { "uz" -> "Keyingi kun"; "en" -> "Next day"; else -> "Следующий день" },
                            onClick = {
                                selectedDayIndex = (safeDayIndex + 1) % days.size
                                expandedSetsExIdx = -1
                            }
                        )
                        GymOutlineButton(
                            text = when (lang) { "uz" -> "Kunni o'chirish"; "en" -> "Remove day"; else -> "Удалить день" },
                            textColor = CrimsonRed,
                            onClick = {
                                days.removeAt(safeDayIndex)
                                days.forEachIndexed { i, d -> days[i] = d.copy(dayNumber = i + 1) }
                                selectedDayIndex = safeDayIndex.coerceAtMost(days.lastIndex)
                                expandedSetsExIdx = -1
                            }
                        )
                    }
                }

                if (activeDay != null) {
                    GymCard(
                        backgroundColor = GymWell
                    ) {
                        GymLabeledField(
                            label = when (lang) { "uz" -> "Kun nomi"; "en" -> "Day name"; else -> "Название дня" },
                            value = activeDay.title,
                            onValueChange = { newName ->
                                days[safeDayIndex] = activeDay.copy(name = newName)
                            },
                            placeholder = "Push Hypertrophy"
                        )

                        Text(
                            text = when (lang) {
                                "uz" -> "${activeDay.exercises.size} mashq · ${activeDay.exercises.sumOf { it.targetSets }} ishchi yondashuv"
                                "en" -> "${activeDay.exercises.size} exercises · ${activeDay.exercises.sumOf { it.targetSets }} working sets"
                                else -> "${activeDay.exercises.size} упражнений · ${activeDay.exercises.sumOf { it.targetSets }} рабочих подходов"
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )

                        if (activeDay.exercises.isNotEmpty()) {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                activeDay.exercises.forEachIndexed { exIndex, ex ->
                                    GymInnerWell {
                                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                            ProgramExerciseRow(exercise = ex)

                                            FlowRow(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                GymOutlineButton(
                                                    text = when (lang) { "uz" -> "Yondashuvlar"; "en" -> "Sets"; else -> "Подходы" },
                                                    borderColor = if (expandedSetsExIdx == exIndex) BorderHighlight else BorderSubtle,
                                                    onClick = {
                                                        expandedSetsExIdx = if (expandedSetsExIdx == exIndex) -1 else exIndex
                                                    }
                                                )
                                                GymOutlineButton(
                                                    text = when (lang) { "uz" -> "Almashtirish"; "en" -> "Replace"; else -> "Заменить" },
                                                    onClick = { replacingSlot = safeDayIndex to exIndex }
                                                )
                                                GymOutlineButton(
                                                    text = when (lang) { "uz" -> "Olib tashlash"; "en" -> "Remove"; else -> "Убрать" },
                                                    textColor = CrimsonRed,
                                                    onClick = {
                                                        val updatedEx = activeDay.exercises.filterIndexed { i, _ -> i != exIndex }
                                                        days[safeDayIndex] = activeDay.copy(exercises = updatedEx)
                                                        expandedSetsExIdx = -1
                                                    }
                                                )
                                            }

                                            if (expandedSetsExIdx == exIndex) {
                                                FlowRow(
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    GymOutlineButton(
                                                        text = "− подход",
                                                        onClick = {
                                                            val updated = activeDay.exercises.mapIndexed { idx, old ->
                                                                if (idx == exIndex) old.copy(targetSets = (old.targetSets - 1).coerceAtLeast(1)) else old
                                                            }
                                                            days[safeDayIndex] = activeDay.copy(exercises = updated)
                                                        }
                                                    )
                                                    GymOutlineButton(
                                                        text = "+ подход",
                                                        onClick = {
                                                            val updated = activeDay.exercises.mapIndexed { idx, old ->
                                                                if (idx == exIndex) old.copy(targetSets = (old.targetSets + 1).coerceAtMost(10)) else old
                                                            }
                                                            days[safeDayIndex] = activeDay.copy(exercises = updated)
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymOutlineButton(
                                text = when (lang) { "uz" -> "+ Mashq qo'shish"; "en" -> "+ Add exercise"; else -> "+ Добавить упражнение" },
                                onClick = { pickingForDayIndex = safeDayIndex }
                            )
                            GymPrimaryButton(
                                text = when (lang) { "uz" -> "Dasturni saqlash"; "en" -> "Save program"; else -> "Сохранить программу" },
                                enabled = title.isNotBlank(),
                                onClick = {
                                    if (title.isBlank()) return@GymPrimaryButton
                                    onSave(
                                        SaveProgramRequestDto(
                                            title = title.trim(),
                                            description = description.trim(),
                                            splitType = splitType,
                                            difficulty = difficulty,
                                            daysPerWeek = days.count { !it.isRestDay }.coerceAtLeast(1),
                                            durationWeeks = 8,
                                            isPublic = isPublic,
                                            days = days.toList()
                                        )
                                    )
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun ProgramCommentsBottomSheet(
    comments: List<ProgramComment>,
    currentUserId: String,
    inputText: String,
    replyingTo: ProgramComment?,
    lang: String,
    onTextChanged: (String) -> Unit,
    onReply: (ProgramComment) -> Unit,
    onCancelReply: () -> Unit,
    onUpvote: (String) -> Unit,
    onDelete: (String) -> Unit,
    onSend: () -> Unit,
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
                .height(540.dp)
                .imePadding()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${if (lang == "en") "Discussion" else if (lang == "uz") "Muhokama" else "Обсуждение"} (${comments.size})",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Text(
                            text = when (lang) {
                                "uz" -> "Hali fikrlar yo'q. Birinchi bo'lib yozing!"
                                "en" -> "No comments yet. Start the discussion!"
                                else -> "Поделитесь опытом или задайте вопрос о программе."
                            },
                            color = TextMuted,
                            fontSize = 14.sp
                        )
                    }
                } else {
                    itemsIndexed(comments, key = { idx, c -> "${c.id}_$idx" }) { _, comment ->
                        CommentItem(
                            comment = comment,
                            canDelete = comment.userId == currentUserId && currentUserId.isNotBlank(),
                            lang = lang,
                            onReply = { onReply(comment) },
                            onUpvote = { onUpvote(comment.id) },
                            onDelete = { onDelete(comment.id) }
                        )
                    }
                }
            }

            if (replyingTo != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (lang == "en") "Reply to" else if (lang == "uz") "Javob" else "Ответ для"} @${replyingTo.userName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentLime
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                GymTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = when (lang) { "uz" -> "Fikringiz..."; "en" -> "Your comment"; else -> "Ваш комментарий" },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                GymPrimaryButton(
                    text = when (lang) { "uz" -> "Yuborish"; "en" -> "Send"; else -> "Отправить" },
                    enabled = inputText.isNotBlank(),
                    onClick = onSend
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommentItem(
    comment: ProgramComment,
    canDelete: Boolean,
    lang: String,
    onReply: () -> Unit,
    onUpvote: () -> Unit,
    onDelete: () -> Unit
) {
    GymInnerWell {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = comment.userName,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = comment.content,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSoft
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GymOutlineButton(
                    text = "${if (lang == "en") "Helpful" else if (lang == "uz") "Foydali" else "Полезно"} · ${comment.upvotes}",
                    textColor = if (comment.isUpvoted) AccentLime else TextPrimary,
                    borderColor = if (comment.isUpvoted) BorderHighlight else BorderSubtle,
                    onClick = onUpvote
                )
                GymOutlineButton(
                    text = when (lang) { "uz" -> "Javob berish"; "en" -> "Reply"; else -> "Ответить" },
                    onClick = onReply
                )
                if (canDelete) {
                    GymOutlineButton(
                        text = when (lang) { "uz" -> "O'chirish"; "en" -> "Delete"; else -> "Удалить" },
                        textColor = CrimsonRed,
                        onClick = onDelete
                    )
                }
            }

            if (comment.replies.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    comment.replies.forEach { reply ->
                        Text(
                            text = "${reply.userName} → ${comment.userName}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLime
                        )
                        Text(
                            text = reply.content,
                            fontSize = 13.sp,
                            color = TextSoft
                        )
                    }
                }
            }
        }
    }
}
