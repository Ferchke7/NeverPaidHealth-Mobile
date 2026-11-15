package dev.neverpaid.health.features.programs

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
import androidx.compose.foundation.lazy.itemsIndexed
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
import dev.neverpaid.health.core.i18n.LocalDudaStrings
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ModeComment
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.neverpaid.health.core.designsystem.ExercisePickerBottomSheet
import dev.neverpaid.health.core.designsystem.InfoAccent
import dev.neverpaid.health.core.designsystem.ObsidianWell
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
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
            ObsidianLoadingCard(
                title = strings.stateLoadingTitle,
                hint = strings.stateLoadingHint,
                kicker = "02 · ${strings.navPrograms}"
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
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header", contentType = "header") {
            ObsidianPageHeader(
                title = strings.navPrograms,
                subtitle = when (lang) {
                    "uz" -> "Kutubxona, hamjamiyat va shaxsiy splitlar"
                    "en" -> "Library, community and your custom splits"
                    else -> "Библиотека, сообщество и ваши сплиты"
                }
            )
        }

        // Top Tabs: Библиотека / Сообщество / Мои сплиты
        item(key = "tabs", contentType = "tabs") {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ObsidianPillTab(
                    text = when (lang) {
                        "uz" -> "Kutubxona"
                        "en" -> "Library"
                        else -> "Библиотека"
                    },
                    selected = state.activeTab == "official",
                    onClick = { onAction(ProgramsUiAction.OnSelectTab("official")) }
                )
                ObsidianPillTab(
                    text = when (lang) {
                        "uz" -> "Hamjamiyat"
                        "en" -> "Community"
                        else -> "Сообщество"
                    },
                    selected = state.activeTab == "community",
                    onClick = { onAction(ProgramsUiAction.OnSelectTab("community")) }
                )
                ObsidianPillTab(
                    text = when (lang) {
                        "uz" -> "Mening splitlarim"
                        "en" -> "My splits"
                        else -> "Мои сплиты"
                    },
                    selected = state.activeTab == "mine",
                    onClick = { onAction(ProgramsUiAction.OnSelectTab("mine")) }
                )
            }
        }

        // Search + Create Program
        item(key = "search_create", contentType = "search") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ObsidianTextField(
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
                    ObsidianPrimaryButton(
                        text = when (lang) {
                            "uz" -> "Dastur yaratish"
                            "en" -> "Create program"
                            else -> "Создать программу"
                        },
                        onClick = { onAction(ProgramsUiAction.OnOpenEditor(null)) }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val splits = listOf(
                        "all" to when (lang) { "uz" -> "Barchasi"; "en" -> "All"; else -> "Все" },
                        "ppl" to "PPL",
                        "upper_lower" to "Upper / Lower",
                        "full_body" to "Full Body",
                        "custom" to when (lang) { "uz" -> "Shaxsiy"; "en" -> "Custom"; else -> "Авторский" }
                    )
                    splits.forEach { (key, label) ->
                        ObsidianPillTab(
                            text = label,
                            selected = state.selectedSplitFilter == key,
                            onClick = { onAction(ProgramsUiAction.OnSelectSplitFilter(key)) }
                        )
                    }
                }
            }
        }

        // Current Active Program Card
        if (activeProgram != null) {
            item(key = "active_program_banner", contentType = "active_program") {
                ObsidianCard(
                    backgroundColor = DarkSurface,
                    borderColor = BorderHighlight
                ) {
                    Text(
                        text = when (lang) {
                            "uz" -> "SIZ USHBU DASTUR BO'YICHA MASHQ QILYAPSIZ"
                            "en" -> "CURRENTLY FOLLOWING THIS PROGRAM"
                            else -> "ВЫ ТРЕНИРУЕТЕСЬ ПО ЭТОЙ ПРОГРАММЕ"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = EmeraldNeon
                    )
                    Text(
                        text = "${activeProgram.title} · ${activeProgram.daysPerWeek} ${if (lang == "en") "days" else if (lang == "uz") "kun" else "дн."}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ObsidianPrimaryButton(
                            text = when (lang) {
                                "uz" -> "Davom etish"
                                "en" -> "Continue"
                                else -> "Продолжить"
                            },
                            onClick = {
                                val nextDay = activeProgram.days.firstOrNull { !it.isRestDay }
                                if (nextDay != null) {
                                    onAction(ProgramsUiAction.OnStartProgramDay(activeProgram, nextDay))
                                } else {
                                    onAction(ProgramsUiAction.OnOpenProgramDetail(activeProgram))
                                }
                            }
                        )
                        ObsidianOutlineButton(
                            text = when (lang) {
                                "uz" -> "Batafsil"
                                "en" -> "Details"
                                else -> "Подробнее"
                            },
                            onClick = { onAction(ProgramsUiAction.OnOpenProgramDetail(activeProgram)) }
                        )
                        ObsidianOutlineButton(
                            text = when (lang) {
                                "uz" -> "O'chirish"
                                "en" -> "Deactivate"
                                else -> "Снять"
                            },
                            textColor = TextSecondary,
                            onClick = { onAction(ProgramsUiAction.OnDeactivateProgram) }
                        )
                    }
                }
            }
        }

        item(key = "catalog_heading", contentType = "section_heading") {
            Text(
                text = when (state.activeTab) {
                    "community" -> when (lang) { "uz" -> "Hamjamiyat"; "en" -> "Community"; else -> "Сообщество" }
                    "mine" -> when (lang) { "uz" -> "Mening splitlarim"; "en" -> "My splits"; else -> "Мои сплиты" }
                    else -> when (lang) { "uz" -> "O'zingizga mos ritmni tanlang"; "en" -> "Find your training rhythm"; else -> "Подберите свой ритм" }
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
        }

        if (filteredPrograms.isEmpty()) {
            item(key = "empty_programs", contentType = "empty") {
                ObsidianEmptyCard(
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
            items(
                items = filteredPrograms,
                key = { it.id },
                contentType = { "program_card" }
            ) { program ->
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
                    onToggleLike = { onAction(ProgramsUiAction.OnToggleLike(program.id)) },
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
private fun ProgramCard(
    program: TrainingProgram,
    isCommunityTab: Boolean,
    isOwn: Boolean,
    lang: String,
    onOpenDetail: () -> Unit,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenAuthor: () -> Unit
) {
    ObsidianCard(
        backgroundColor = DarkSurface,
        borderColor = if (program.isActive) BorderHighlight else BorderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDetail() },
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = when (lang) {
                    "uz" -> "${program.splitType.uppercase()} · ${program.daysPerWeek} kun"
                    "en" -> "${program.splitType.uppercase()} · ${program.daysPerWeek} days"
                    else -> "${program.splitType.uppercase()} · ${program.daysPerWeek} дня"
                },
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = if (program.isActive) EmeraldNeon else TextSecondary
            )

            Text(
                text = program.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )

            if (program.description.isNotBlank()) {
                Text(
                    text = program.description,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (program.days.isNotEmpty()) {
                Text(
                    text = program.days.joinToString(" / ") { it.title },
                    fontSize = 13.sp,
                    color = TextSoft,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        ObsidianDivider(color = BorderSubtle)

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (program.isActive) {
                ObsidianOutlineButton(
                    text = when (lang) {
                        "uz" -> "O'rnatilgan"
                        "en" -> "Installed"
                        else -> "Установлена"
                    },
                    textColor = EmeraldNeon,
                    borderColor = BorderHighlight,
                    onClick = onOpenDetail
                )
            } else {
                ObsidianPrimaryButton(
                    text = when (lang) {
                        "uz" -> "O'rnatish"
                        "en" -> "Install"
                        else -> "Установить"
                    },
                    onClick = onActivate
                )
            }

            ObsidianOutlineButton(
                text = when (lang) {
                    "uz" -> "Batafsil"
                    "en" -> "Details"
                    else -> "Подробнее"
                },
                onClick = onOpenDetail
            )

            if (isOwn) {
                ObsidianOutlineButton(
                    text = when (lang) {
                        "uz" -> "Tahrirlash"
                        "en" -> "Edit"
                        else -> "Редактировать"
                    },
                    onClick = onEdit
                )
                ObsidianOutlineButton(
                    text = when (lang) {
                        "uz" -> "O'chirish"
                        "en" -> "Delete"
                        else -> "Удалить"
                    },
                    onClick = onDelete
                )
            }

            if (isCommunityTab || program.likesCount > 0 || program.commentsCount > 0) {
                if (program.authorName.isNotBlank()) {
                    ObsidianOutlineButton(
                        text = "${if (lang == "en") "Author" else if (lang == "uz") "Muallif" else "Автор"}: ${program.authorName.replace("NeverPaidHealth", "duda.uz")}",
                        textColor = TextSecondary,
                        onClick = onOpenAuthor
                    )
                }
                ObsidianOutlineButton(
                    text = "${if (lang == "en") "Like" else if (lang == "uz") "Yoqdi" else "Нравится"} · ${program.likesCount}",
                    textColor = if (program.isLiked) EmeraldNeon else TextPrimary,
                    borderColor = if (program.isLiked) BorderHighlight else BorderSubtle,
                    onClick = onToggleLike
                )
                ObsidianOutlineButton(
                    text = "${if (lang == "en") "Comments" else if (lang == "uz") "Fikrlar" else "Комментарии"} · ${program.commentsCount}",
                    onClick = onOpenComments
                )
            }
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

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "detail_header") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "ПРЕДПРОСМОТР · ${program.splitType.uppercase()} · ${program.days.size} ${if (lang == "en") "DAYS" else if (lang == "uz") "KUN" else "ДНЯ"}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        letterSpacing = 0.8.sp,
                        color = EmeraldNeon
                    )
                    Text(
                        text = program.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
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
                            ObsidianOutlineButton(
                                text = when (lang) { "uz" -> "Faolsizlantirish"; "en" -> "Deactivate"; else -> "Деактивировать" },
                                onClick = onDeactivate
                            )
                        } else {
                            ObsidianPrimaryButton(
                                text = when (lang) { "uz" -> "Faollashtirish"; "en" -> "Activate"; else -> "Активировать" },
                                onClick = onActivate
                            )
                        }
                        if (canEdit) {
                            ObsidianOutlineButton(
                                text = when (lang) { "uz" -> "Tahrirlash"; "en" -> "Edit"; else -> "Изменить" },
                                onClick = onEdit
                            )
                            ObsidianOutlineButton(
                                text = when (lang) { "uz" -> "O'chirish"; "en" -> "Delete"; else -> "Удалить" },
                                textColor = CrimsonRed,
                                onClick = onDelete
                            )
                        }
                        ObsidianOutlineButton(
                            text = "${if (lang == "en") "Discussion" else if (lang == "uz") "Muhokama" else "Обсуждение"} · ${program.commentsCount}",
                            onClick = onOpenComments
                        )
                    }
                }
            }

            items(program.days.sortedBy { it.dayNumber }, key = { it.dayNumber }) { day ->
                ObsidianCard(
                    backgroundColor = DarkBackground,
                    borderColor = BorderSubtle
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${if (lang == "en") "Day" else if (lang == "uz") "Kun" else "День"} ${day.dayNumber} · ${day.title}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
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
                            ObsidianPrimaryButton(
                                text = when (lang) { "uz" -> "Boshlash"; "en" -> "Start"; else -> "Старт" },
                                onClick = { onStartDay(day) }
                            )
                        }
                    }

                    if (day.exercises.isNotEmpty()) {
                        ObsidianDivider(color = BorderSubtle)
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            day.exercises.forEachIndexed { idx, ex ->
                                val num = (idx + 1).toString().padStart(2, '0')
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier.weight(1f),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = num,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EmeraldNeon
                                        )
                                        Text(
                                            text = ex.exerciseName,
                                            fontSize = 14.sp,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    Text(
                                        text = "${ex.targetSets} × ${ex.targetRepsMin}–${ex.targetRepsMax}",
                                        fontSize = 13.sp,
                                        color = TextSecondary
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
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(EmeraldSubtle)
                        .border(1.dp, BorderHighlight, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldNeon
                    )
                }
                Column {
                    Text(
                        text = profile.name,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Text(
                        text = when (lang) {
                            "uz" -> "${profile.totalWorkouts} mashg'ulot · ${StrengthCalculator.formatVolume(profile.totalVolumeKg)} kg · ${profile.totalLikesReceived} yoqdi"
                            "en" -> "${profile.totalWorkouts} workouts · ${StrengthCalculator.formatVolume(profile.totalVolumeKg)} kg · ${profile.totalLikesReceived} likes"
                            else -> "${profile.totalWorkouts} тренировок · ${StrengthCalculator.formatVolume(profile.totalVolumeKg)} кг · ${profile.totalLikesReceived} лайков"
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            if (profile.publicPrograms.isNotEmpty()) {
                ObsidianDivider(color = BorderSubtle)
                Text(
                    text = when (lang) {
                        "uz" -> "Sportchi dasturlari"
                        "en" -> "Athlete programs"
                        else -> "Программы атлета"
                    },
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    profile.publicPrograms.take(4).forEach { prog ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(DarkSurfaceHigh)
                                .clickable { onSelectProgram(prog) }
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prog.title, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                Text("${prog.daysPerWeek} дн/нед · Нравится ${prog.likesCount}", fontSize = 12.sp, color = TextSecondary)
                            }
                            Text(
                                text = when (lang) { "uz" -> "Ochish →"; "en" -> "Open →"; else -> "Открыть →" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = EmeraldNeon
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
        contentColor = TextPrimary
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
                            when (lang) { "uz" -> "Yangi dastur"; "en" -> "Program builder"; else -> "Редактор программы" }
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
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
                ObsidianLabeledField(
                    label = when (lang) { "uz" -> "Dastur nomi"; "en" -> "Program name"; else -> "Название программы" },
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "Push Pull Legs"
                )

                ObsidianLabeledField(
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
                        .clickable { isPublic = !isPublic },
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

                ObsidianDivider(color = BorderSubtle)

                Text(
                    text = when (lang) { "uz" -> "Dastur tuzilishi"; "en" -> "Program structure"; else -> "Структура программы" },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                // Day Tabs
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    days.forEachIndexed { idx, day ->
                        ObsidianPillTab(
                            text = "${if (lang == "en") "Day" else if (lang == "uz") "Kun" else "День"} ${day.dayNumber} · ${day.title.take(14)}",
                            selected = idx == safeDayIndex,
                            onClick = {
                                selectedDayIndex = idx
                                expandedSetsExIdx = -1
                            }
                        )
                    }
                }

                // Add / Next / Delete Day Wrap
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ObsidianOutlineButton(
                        text = when (lang) { "uz" -> "Kun qo'shish"; "en" -> "Add day"; else -> "Добавить день" },
                        onClick = {
                            val nextNum = days.size + 1
                            days.add(ProgramDay(dayNumber = nextNum, name = "День $nextNum", exercises = emptyList()))
                            selectedDayIndex = days.lastIndex
                            expandedSetsExIdx = -1
                        }
                    )
                    if (days.size > 1) {
                        ObsidianOutlineButton(
                            text = when (lang) { "uz" -> "Keyingi kun"; "en" -> "Next day"; else -> "Следующий день" },
                            onClick = {
                                selectedDayIndex = (safeDayIndex + 1) % days.size
                                expandedSetsExIdx = -1
                            }
                        )
                        ObsidianOutlineButton(
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

                // Active Day Detail & Exercises
                if (activeDay != null) {
                    ObsidianCard(
                        backgroundColor = DarkBackground,
                        borderColor = BorderSubtle
                    ) {
                        ObsidianLabeledField(
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
                            ObsidianDivider(color = BorderSubtle)

                            activeDay.exercises.forEachIndexed { exIndex, ex ->
                                val num = (exIndex + 1).toString().padStart(2, '0')
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = num,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = EmeraldNeon
                                        )
                                        Text(
                                            text = ex.exerciseName,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = TextPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                    }

                                    Text(
                                        text = when (lang) {
                                            "uz" -> "${ex.targetSets} yondashuv · ${ex.targetRepsMin}–${ex.targetRepsMax} takror"
                                            "en" -> "${ex.targetSets} sets · ${ex.targetRepsMin}–${ex.targetRepsMax} reps"
                                            else -> "${ex.targetSets} подхода · ${ex.targetRepsMin}–${ex.targetRepsMax} повторений"
                                        },
                                        fontSize = 13.sp,
                                        color = TextSecondary
                                    )

                                    FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        ObsidianOutlineButton(
                                            text = when (lang) { "uz" -> "Yondashuvlar"; "en" -> "Sets"; else -> "Подходы" },
                                            borderColor = if (expandedSetsExIdx == exIndex) BorderHighlight else BorderSubtle,
                                            onClick = {
                                                expandedSetsExIdx = if (expandedSetsExIdx == exIndex) -1 else exIndex
                                            }
                                        )
                                        ObsidianOutlineButton(
                                            text = when (lang) { "uz" -> "Almashtirish"; "en" -> "Replace"; else -> "Заменить" },
                                            onClick = { replacingSlot = safeDayIndex to exIndex }
                                        )
                                        ObsidianOutlineButton(
                                            text = when (lang) { "uz" -> "Olib tashlash"; "en" -> "Remove"; else -> "Убрать" },
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
                                            ObsidianOutlineButton(
                                                text = "− подход",
                                                onClick = {
                                                    val updated = activeDay.exercises.mapIndexed { idx, old ->
                                                        if (idx == exIndex) old.copy(targetSets = (old.targetSets - 1).coerceAtLeast(1)) else old
                                                    }
                                                    days[safeDayIndex] = activeDay.copy(exercises = updated)
                                                }
                                            )
                                            ObsidianOutlineButton(
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

                                if (exIndex != activeDay.exercises.lastIndex) {
                                    ObsidianDivider(color = BorderDivider)
                                }
                            }
                        }

                        ObsidianDivider(color = BorderSubtle)

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ObsidianOutlineButton(
                                text = when (lang) { "uz" -> "Mashq qo'shish"; "en" -> "Add exercise"; else -> "Добавить упражнение" },
                                onClick = { pickingForDayIndex = safeDayIndex }
                            )
                            ObsidianPrimaryButton(
                                text = when (lang) { "uz" -> "Dasturni saqlash"; "en" -> "Save program"; else -> "Сохранить программу" },
                                enabled = title.isNotBlank(),
                                onClick = {
                                    if (title.isBlank()) return@ObsidianPrimaryButton
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
        contentColor = TextPrimary
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
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
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
                    items(comments, key = { it.id }) { comment ->
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceHigh)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${if (lang == "en") "Reply to" else if (lang == "uz") "Javob" else "Ответ для"} @${replyingTo.userName}",
                        fontSize = 12.sp,
                        color = EmeraldNeon
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
                ObsidianTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = when (lang) { "uz" -> "Fikringiz..."; "en" -> "Your comment"; else -> "Ваш комментарий" },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                ObsidianPrimaryButton(
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
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = comment.userName,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
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
            ObsidianOutlineButton(
                text = "${if (lang == "en") "Helpful" else if (lang == "uz") "Foydali" else "Полезно"} · ${comment.upvotes}",
                textColor = if (comment.isUpvoted) EmeraldNeon else TextPrimary,
                borderColor = if (comment.isUpvoted) BorderHighlight else BorderSubtle,
                onClick = onUpvote
            )
            ObsidianOutlineButton(
                text = when (lang) { "uz" -> "Javob berish"; "en" -> "Reply"; else -> "Ответить" },
                onClick = onReply
            )
            if (canDelete) {
                ObsidianOutlineButton(
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
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceHigh)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                comment.replies.forEach { reply ->
                    Text(
                        text = "${reply.userName} · ${comment.userName}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldNeon
                    )
                    Text(
                        text = reply.content,
                        fontSize = 13.sp,
                        color = TextSoft
                    )
                }
            }
        }

        ObsidianDivider(color = BorderDivider)
    }
}
