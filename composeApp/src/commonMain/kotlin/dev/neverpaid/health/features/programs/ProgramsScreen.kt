package dev.neverpaid.health.features.programs

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
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
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

@Composable
fun ProgramsScreen(
    state: ProgramsUiState,
    onAction: (ProgramsUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.programs.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    if (state.detailProgram != null) {
        ProgramDetailBottomSheet(
            program = state.detailProgram!!,
            canEdit = state.detailProgram!!.authorUserId == state.currentUserId && state.currentUserId.isNotBlank(),
            onActivate = { onAction(ProgramsUiAction.OnActivateProgram(state.detailProgram!!.id)) },
            onDeactivate = { onAction(ProgramsUiAction.OnDeactivateProgram) },
            onStartDay = { day -> onAction(ProgramsUiAction.OnStartProgramDay(state.detailProgram!!, day)) },
            onEdit = { onAction(ProgramsUiAction.OnOpenEditor(state.detailProgram)) },
            onDelete = { onAction(ProgramsUiAction.OnDeleteProgram(state.detailProgram!!.id)) },
            onOpenComments = { onAction(ProgramsUiAction.OnOpenComments(state.detailProgram!!.id)) },
            onDismiss = { onAction(ProgramsUiAction.OnOpenProgramDetail(null)) }
        )
    }

    if (state.activeCommentsProgramId != null) {
        ProgramCommentsBottomSheet(
            comments = state.comments,
            currentUserId = state.currentUserId,
            inputText = state.newCommentText,
            replyingTo = state.replyingToComment,
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
                else -> prog.isPublic && !prog.isPrebuilt // community
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
                        text = "Программы тренировок",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Каталог сплитов, программы атлетов и отзывы",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { onAction(ProgramsUiAction.OnOpenEditor(null)) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = Color.Black
                    ),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Создать", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Program Banner
        if (activeProgram != null) {
            item(key = "active_program_banner") {
                AthleticCard(
                    backgroundColor = DarkSurfaceVariant,
                    borderColor = BorderHighlight
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onAction(ProgramsUiAction.OnOpenProgramDetail(activeProgram)) }
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ВАША АКТИВНАЯ ПРОГРАММА",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                            Text(
                                text = activeProgram.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${activeProgram.daysPerWeek} дн./нед. • ${activeProgram.days.size} дней в цикле",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        TextButton(onClick = { onAction(ProgramsUiAction.OnDeactivateProgram) }) {
                            Text("Снять", color = TextSecondary, fontSize = 12.sp)
                        }
                    }
                }
            }
        }

        // 3 Segmented Tabs: База программ / Сообщество / Мои сплиты
        item(key = "tabs") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    "official" to "База программ",
                    "community" to "Сообщество",
                    "mine" to "Мои программы"
                ).forEach { (key, label) ->
                    val selected = state.activeTab == key
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(9.dp))
                            .background(if (selected) EmeraldNeon else Color.Transparent)
                            .clickable { onAction(ProgramsUiAction.OnSelectTab(key)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
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

        // Search + Split Filter Chips
        item(key = "filters") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.searchQuery,
                    onValueChange = { onAction(ProgramsUiAction.OnSearchQueryChanged(it)) },
                    placeholder = { Text("Поиск программы или автора...", fontSize = 13.sp, color = TextMuted) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextSecondary) },
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

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val splits = listOf(
                        "all" to "Все сплиты",
                        "ppl" to "PPL (Тяни/Толкай/Ноги)",
                        "upper_lower" to "Верх / Низ",
                        "full_body" to "Full Body",
                        "custom" to "Авторский"
                    )
                    items(splits, key = { it.first }) { (key, label) ->
                        val selected = state.selectedSplitFilter == key
                        FilterChip(
                            selected = selected,
                            onClick = { onAction(ProgramsUiAction.OnSelectSplitFilter(key)) },
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
        }

        if (filteredPrograms.isEmpty()) {
            item(key = "empty_programs") {
                AthleticCard(backgroundColor = DarkSurface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Программы не найдены",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Попробуйте изменить фильтр или создайте собственную программу тренировок.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredPrograms, key = { it.id }) { program ->
                ProgramCard(
                    program = program,
                    onOpenDetail = { onAction(ProgramsUiAction.OnOpenProgramDetail(program)) },
                    onActivate = { onAction(ProgramsUiAction.OnActivateProgram(program.id)) },
                    onToggleLike = { onAction(ProgramsUiAction.OnToggleLike(program.id)) },
                    onOpenComments = { onAction(ProgramsUiAction.OnOpenComments(program.id)) },
                    onOpenAuthor = {
                        program.authorUserId?.let { uid ->
                            onAction(ProgramsUiAction.OnOpenAthleteProfile(uid))
                        }
                    },
                    onStartDay = { day -> onAction(ProgramsUiAction.OnStartProgramDay(program, day)) }
                )
            }
        }

        item(key = "bottom_space") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun ProgramCard(
    program: TrainingProgram,
    onOpenDetail: () -> Unit,
    onActivate: () -> Unit,
    onToggleLike: () -> Unit,
    onOpenComments: () -> Unit,
    onOpenAuthor: () -> Unit,
    onStartDay: (ProgramDay) -> Unit
) {
    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = if (program.isActive) BorderHighlight else BorderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onOpenDetail() }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (program.isActive) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(EmeraldSubtle)
                                .border(1.dp, BorderHighlight, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(12.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("АКТИВНА", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${program.daysPerWeek} дн/нед • ${program.level.uppercase()}",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onToggleLike() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = if (program.isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Like",
                            tint = if (program.isLiked) CrimsonRed else TextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${program.likesCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (program.isLiked) CrimsonRed else TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onOpenComments() }
                            .padding(horizontal = 6.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ModeComment,
                            contentDescription = "Comments",
                            tint = EmeraldLight,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${program.commentsCount}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = program.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            if (program.authorName.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = program.authorUserId.isNotBlank()) { onOpenAuthor() }
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        tint = EmeraldLight,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = program.authorName.replace("NeverPaidHealth", "duda.uz"),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = program.description,
                fontSize = 13.sp,
                color = TextSecondary,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                program.days.take(4).forEach { day ->
                    ProgramDayRow(day = day, onStart = { onStartDay(day) })
                }
                if (program.days.size > 4) {
                    Text(
                        text = "+ ещё ${program.days.size - 4} тренировочных дня (нажмите для просмотра)",
                        fontSize = 11.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenDetail,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(42.dp)
                ) {
                    Text("Подробнее", fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
                }

                if (!program.isActive) {
                    Button(
                        onClick = onActivate,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon,
                            contentColor = Color.Black
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(42.dp)
                    ) {
                        Text("Сделать активной", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgramDayRow(
    day: ProgramDay,
    onStart: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .clickable(enabled = !day.isRestDay) { onStart() }
            .padding(horizontal = 12.dp, vertical = 9.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(if (day.isRestDay) DarkSurfaceHigh else EmeraldSubtle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "${day.dayNumber}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (day.isRestDay) TextMuted else EmeraldNeon
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = day.title,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (day.isRestDay) TextMuted else TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (!day.isRestDay && day.exercises.isNotEmpty()) {
                    Text(
                        text = "${day.exercises.size} упр. • ${day.exercises.take(2).joinToString { it.exerciseName }}",
                        fontSize = 11.sp,
                        color = TextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        if (!day.isRestDay) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = "Start",
                tint = EmeraldNeon,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramDetailBottomSheet(
    program: TrainingProgram,
    canEdit: Boolean,
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
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = program.title,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "${program.daysPerWeek} дней в неделю • ${program.durationWeeks} недель",
                        fontSize = 12.sp,
                        color = EmeraldLight
                    )
                }
                Row {
                    if (canEdit) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Изменить", tint = TextSecondary)
                        }
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = CrimsonRed)
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = program.description,
                fontSize = 13.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(program.days.sortedBy { it.dayNumber }, key = { it.dayNumber }) { day ->
                    AthleticCard(
                        backgroundColor = DarkSurfaceVariant,
                        borderColor = BorderSubtle
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "День ${day.dayNumber}: ${day.title}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!day.isRestDay && day.exercises.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(EmeraldNeon)
                                            .clickable { onStartDay(day) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = "Старт дня",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.Black
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            day.exercises.forEachIndexed { idx, ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${ex.exerciseName}",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Text(
                                        text = "${ex.targetSets} × ${ex.targetRepsMin}-${ex.targetRepsMax}",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = EmeraldLight
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onOpenComments,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.ModeComment, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Отзывы (${program.commentsCount})", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                Button(
                    onClick = if (program.isActive) onDeactivate else onActivate,
                    modifier = Modifier
                        .weight(1.3f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (program.isActive) DarkSurfaceHigh else EmeraldNeon,
                        contentColor = if (program.isActive) TextPrimary else Color.Black
                    )
                ) {
                    Text(
                        text = if (program.isActive) "Отключить план" else "Выбрать программу",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AthleteProfileBottomSheet(
    profile: AthleteProfile,
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
                .padding(horizontal = 18.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(EmeraldSubtle)
                    .border(2.dp, EmeraldNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = EmeraldLight
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = profile.name,
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(14.dp))

            AthleticCard(backgroundColor = DarkSurfaceVariant) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCounter("Тренировок", "${profile.totalWorkouts}", accentColor = EmeraldNeon)
                    StatCounter("Тоннаж", StrengthCalculator.formatVolume(profile.totalVolumeKg), "кг", accentColor = IndigoElectric)
                    StatCounter("Лайков", "${profile.totalLikesReceived}", accentColor = AmberWarm)
                }
            }

            if (profile.publicPrograms.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "ПРОГРАММЫ АТЛЕТА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                profile.publicPrograms.take(4).forEach { prog ->
                    AthleticCard(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .clickable { onSelectProgram(prog) },
                        backgroundColor = DarkSurfaceVariant
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(prog.title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                Text("${prog.daysPerWeek} дн/нед • ❤ ${prog.likesCount}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Text("Открыть →", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldLight)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramEditorBottomSheet(
    existingProgram: TrainingProgram?,
    allExercises: List<Exercise>,
    onSave: (SaveProgramRequestDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var title by remember { mutableStateOf(existingProgram?.title ?: "") }
    var description by remember { mutableStateOf(existingProgram?.description ?: "") }
    var splitType by remember { mutableStateOf(existingProgram?.splitType ?: "ppl") }
    var difficulty by remember { mutableStateOf(existingProgram?.difficulty ?: "intermediate") }
    var isPublic by remember { mutableStateOf(existingProgram?.isPublic ?: true) }
    val days = remember {
        mutableStateListOf<ProgramDay>().apply {
            if (existingProgram != null && existingProgram.days.isNotEmpty()) {
                addAll(existingProgram.days)
            } else {
                add(ProgramDay(dayNumber = 1, name = "День 1: Жим (Push)", exercises = emptyList()))
            }
        }
    }
    var pickingForDayIndex by remember { mutableIntStateOf(-1) }

    if (pickingForDayIndex >= 0) {
        ExercisePickerBottomSheet(
            exercises = allExercises,
            title = "Добавить упражнение в День ${pickingForDayIndex + 1}",
            onSelectExercise = { ex ->
                val dayIdx = pickingForDayIndex
                if (dayIdx in days.indices) {
                    val currentDay = days[dayIdx]
                    val nextItem = ProgramDayExercise(
                        exerciseId = ex.id,
                        exerciseName = ex.name,
                        orderIndex = currentDay.exercises.size + 1,
                        targetSets = 3,
                        targetRepsMin = 8,
                        targetRepsMax = 12
                    )
                    days[dayIdx] = currentDay.copy(exercises = currentDay.exercises + nextItem)
                }
                pickingForDayIndex = -1
            },
            onDismiss = { pickingForDayIndex = -1 }
        )
    }

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
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (existingProgram != null) "Редактировать программу" else "Конструктор программы",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Название программы") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Описание и рекомендации") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = isPublic, onCheckedChange = { isPublic = it })
                    Text("Опубликовать в каталоге сообщества duda.uz", fontSize = 13.sp, color = TextPrimary)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ТРЕНИРОВОЧНЫЕ ДНИ (${days.size})", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    TextButton(
                        onClick = {
                            val nextNum = days.size + 1
                            days.add(ProgramDay(dayNumber = nextNum, name = "День $nextNum", exercises = emptyList()))
                        }
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Добавить день", color = EmeraldLight, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                days.forEachIndexed { dayIndex, day ->
                    AthleticCard(backgroundColor = DarkSurfaceVariant) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = day.title,
                                    onValueChange = { newTitle ->
                                        days[dayIndex] = day.copy(name = newTitle)
                                    },
                                    label = { Text("День ${day.dayNumber}") },
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                if (days.size > 1) {
                                    IconButton(
                                        onClick = {
                                            days.removeAt(dayIndex)
                                            days.forEachIndexed { i, d -> days[i] = d.copy(dayNumber = i + 1) }
                                        }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Удалить день", tint = CrimsonRed)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            day.exercises.forEachIndexed { exIndex, ex ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 3.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${exIndex + 1}. ${ex.exerciseName} (${ex.targetSets}×${ex.targetRepsMin}-${ex.targetRepsMax})",
                                        fontSize = 12.sp,
                                        color = TextPrimary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = {
                                            val updatedEx = day.exercises.filterIndexed { i, _ -> i != exIndex }
                                            days[dayIndex] = day.copy(exercises = updatedEx)
                                        },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Default.Close, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }

                            TextButton(onClick = { pickingForDayIndex = dayIndex }) {
                                Icon(Icons.Default.Add, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(15.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Добавить упражнение", color = EmeraldLight, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            PrimaryAthleticButton(
                text = "Сохранить программу",
                onClick = {
                    if (title.isBlank()) return@PrimaryAthleticButton
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

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProgramCommentsBottomSheet(
    comments: List<ProgramComment>,
    currentUserId: String,
    inputText: String,
    replyingTo: ProgramComment?,
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
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .height(520.dp)
                .imePadding()
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Комментарии (${comments.size})",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = "Close", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                if (comments.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Пока нет комментариев. Оставьте первый отзыв!",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(comments, key = { it.id }) { comment ->
                        CommentItem(
                            comment = comment,
                            canDelete = comment.userId == currentUserId && currentUserId.isNotBlank(),
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
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ответ для @${replyingTo.userName}",
                        fontSize = 12.sp,
                        color = EmeraldLight
                    )
                    IconButton(onClick = onCancelReply, modifier = Modifier.size(24.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = TextMuted, modifier = Modifier.size(16.dp))
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onTextChanged,
                    placeholder = { Text(text = "Написать комментарий...", color = TextMuted, fontSize = 13.sp) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(20.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3
                )

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (inputText.isNotBlank()) EmeraldNeon else DarkSurfaceHigh)
                        .clickable(enabled = inputText.isNotBlank()) { onSend() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        tint = if (inputText.isNotBlank()) DarkBackground else TextMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CommentItem(
    comment: ProgramComment,
    canDelete: Boolean,
    onReply: () -> Unit,
    onUpvote: () -> Unit,
    onDelete: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(EmeraldSubtle),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = comment.userName.take(1).uppercase(),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = comment.userName,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = comment.content,
                    fontSize = 13.sp,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )

                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Ответить",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldLight,
                        modifier = Modifier.clickable { onReply() }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { onUpvote() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ThumbUp,
                            contentDescription = null,
                            tint = if (comment.isUpvoted) EmeraldNeon else TextMuted,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = "${comment.upvotes}",
                            fontSize = 11.sp,
                            color = if (comment.isUpvoted) EmeraldNeon else TextMuted
                        )
                    }
                    if (canDelete) {
                        Text(
                            text = "Удалить",
                            fontSize = 11.sp,
                            color = CrimsonRed,
                            modifier = Modifier.clickable { onDelete() }
                        )
                    }
                }
            }
        }

        if (comment.replies.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .padding(start = 42.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                comment.replies.forEach { reply ->
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(DarkSurfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = reply.userName.take(1).uppercase(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = reply.userName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = reply.content,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}
