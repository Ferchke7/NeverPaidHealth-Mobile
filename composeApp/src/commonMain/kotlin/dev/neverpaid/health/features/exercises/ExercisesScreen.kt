package dev.neverpaid.health.features.exercises

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AnimatedExerciseFramesCard
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExerciseThumbnail
import dev.neverpaid.health.core.designsystem.ExerciseThumbnailSize
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymEmptyCard
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.designsystem.localizeEquipmentName
import dev.neverpaid.health.core.designsystem.localizeMuscleName
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.SaveExerciseRequestDto

@Composable
fun ExercisesRoute(
    viewModel: ExercisesViewModel,
    onNavigateToActiveWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            ExercisesUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
            is ExercisesUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        ExercisesScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ExercisesScreen(
    state: ExercisesUiState,
    onAction: (ExercisesUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = when (strings.navWorkouts) {
        "Mashg'ulotlar" -> "uz"
        "Workouts" -> "en"
        else -> "ru"
    }

    val muscles = listOf(
        "All" to when (lang) {
            "uz" -> "Barchasi"
            "en" -> "All"
            else -> "Все"
        },
        "chest" to when (lang) {
            "uz" -> "Ko'krak"
            "en" -> "Chest"
            else -> "Грудь"
        },
        "back" to when (lang) {
            "uz" -> "Orqa"
            "en" -> "Back"
            else -> "Спина"
        },
        "legs" to when (lang) {
            "uz" -> "Oyoqlar"
            "en" -> "Legs"
            else -> "Ноги"
        },
        "shoulders" to when (lang) {
            "uz" -> "Yelkalar"
            "en" -> "Shoulders"
            else -> "Плечи"
        },
        "arms" to when (lang) {
            "uz" -> "Qo'llar"
            "en" -> "Arms"
            else -> "Руки"
        },
        "core" to when (lang) {
            "uz" -> "Press"
            "en" -> "Core"
            else -> "Пресс"
        }
    )
    val equipments = listOf(
        "All" to when (lang) {
            "uz" -> "Barchasi"
            "en" -> "All"
            else -> "Всё"
        },
        "barbell" to when (lang) {
            "uz" -> "Shtanga"
            "en" -> "Barbell"
            else -> "Штанга"
        },
        "dumbbell" to when (lang) {
            "uz" -> "Gantel"
            "en" -> "Dumbbell"
            else -> "Гантели"
        },
        "cable" to when (lang) {
            "uz" -> "Blok"
            "en" -> "Cable"
            else -> "Блок"
        },
        "machine" to when (lang) {
            "uz" -> "Trenajyor"
            "en" -> "Machine"
            else -> "Тренажёр"
        },
        "bodyweight" to when (lang) {
            "uz" -> "Tana vazni"
            "en" -> "Bodyweight"
            else -> "Свой вес"
        }
    )

    var showEquipFilter by remember { mutableStateOf(false) }
    var expandedTechniqueId by remember { mutableStateOf<String?>(null) }

    if (state.selectedExerciseDetail != null) {
        ExerciseInfoBottomSheet(
            exercise = state.selectedExerciseDetail!!,
            onStartWithExercise = { ex ->
                onAction(ExercisesUiAction.OnStartWorkoutWithExercise(ex))
            },
            onDismiss = { onAction(ExercisesUiAction.OnDismissDetail) }
        )
    }

    if (state.isEditorOpen) {
        CustomExerciseEditorBottomSheet(
            existing = state.editingExercise,
            lang = lang,
            onSave = { req ->
                onAction(ExercisesUiAction.OnSaveCustomExercise(req, state.editingExercise?.id))
            },
            onDismiss = { onAction(ExercisesUiAction.OnCloseEditor) }
        )
    }

    val selectedEquipLabel = equipments.find { it.first.equals(state.selectedEquipment, ignoreCase = true) }?.second
        ?: equipments.first().second

    val catalogExercises = remember(state.exercises) { state.exercises.filter { !it.isCustom } }
    val customExercises = remember(state.exercises) { state.exercises.filter { it.isCustom } }

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
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = strings.navExercises,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.7).sp,
                            color = InkWhite
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(InkWhite.copy(alpha = 0.16f))
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${state.exercises.size}",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = InkWhite,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }
        }

        item(key = "controls", contentType = "controls") {
            Column(
                modifier = Modifier.padding(bottom = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GymTextField(
                        value = state.searchQuery,
                        onValueChange = { onAction(ExercisesUiAction.OnSearchQueryChanged(it)) },
                        placeholder = when (lang) {
                            "uz" -> "Mashq qidirish"
                            "en" -> "Search exercises"
                            else -> "Поиск упражнения"
                        },
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    GymPrimaryButton(
                        text = when (lang) {
                            "uz" -> "+ O'zimniki"
                            "en" -> "+ Custom"
                            else -> "+ Своё"
                        },
                        onClick = { onAction(ExercisesUiAction.OnOpenEditor(null)) }
                    )
                }

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(muscles, key = { it.first }) { (key, label) ->
                        GymPillTab(
                            text = label,
                            selected = state.selectedMuscle.equals(key, ignoreCase = true),
                            onClick = { onAction(ExercisesUiAction.OnSelectMuscle(key)) }
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "Barchasi"
                            "en" -> "All"
                            else -> "Все"
                        },
                        selected = state.selectedTab == "all",
                        onClick = { onAction(ExercisesUiAction.OnSelectTab("all")) }
                    )
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "O'zimniki"
                            "en" -> "Custom"
                            else -> "Свои"
                        },
                        selected = state.selectedTab == "custom",
                        onClick = { onAction(ExercisesUiAction.OnSelectTab("custom")) }
                    )
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "Jihoz: $selectedEquipLabel"
                            "en" -> "Gear: $selectedEquipLabel"
                            else -> "Снаряд: $selectedEquipLabel"
                        },
                        selected = !state.selectedEquipment.equals("All", ignoreCase = true) || showEquipFilter,
                        onClick = { showEquipFilter = !showEquipFilter }
                    )
                }

                if (showEquipFilter) {
                    LazyRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(equipments, key = { it.first }) { (key, label) ->
                            GymPillTab(
                                text = label,
                                selected = state.selectedEquipment.equals(key, ignoreCase = true),
                                onClick = {
                                    onAction(ExercisesUiAction.OnSelectEquipment(key))
                                    showEquipFilter = false
                                }
                            )
                        }
                    }
                }
            }
        }

        item(key = "library_heading", contentType = "section_heading") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                GymSectionTitle(
                    title = when (lang) {
                        "uz" -> "Mashqlar"
                        "en" -> "Exercises"
                        else -> "Упражнения"
                    },
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = "${state.exercises.size}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextSecondary,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }

        if (state.isLoading && state.exercises.isEmpty()) {
            item(key = "loading", contentType = "loading") {
                GymLoadingCard(
                    title = strings.stateLoadingTitle,
                    hint = strings.stateLoadingHint
                )
            }
        } else if (state.exercises.isEmpty()) {
            item(key = "empty", contentType = "empty") {
                GymEmptyCard(
                    title = when (lang) {
                        "uz" -> "Mashqlar topilmadi"
                        "en" -> "No exercises found"
                        else -> "Упражнения не найдены"
                    },
                    description = when (lang) {
                        "uz" -> "Filtrni o'zgartiring yoki yangi mashq qo'shing."
                        "en" -> "Try adjusting filters or create a custom movement."
                        else -> "Измените параметры фильтра или создайте собственное упражнение."
                    },
                    primaryActionLabel = when (lang) {
                        "uz" -> "O'zimniki yaratish"
                        "en" -> "Create custom"
                        else -> "Создать своё"
                    },
                    onPrimaryAction = { onAction(ExercisesUiAction.OnOpenEditor(null)) }
                )
            }
        } else {
            if (customExercises.isNotEmpty()) {
                items(
                    items = customExercises,
                    key = { ex -> "custom_${ex.id}" },
                    contentType = { "custom_card" }
                ) { ex ->
                    ExerciseCatalogCard(
                        exercise = ex,
                        isCustom = true,
                        isExpanded = expandedTechniqueId == ex.id,
                        lang = lang,
                        onToggleTechnique = {
                            expandedTechniqueId = if (expandedTechniqueId == ex.id) null else ex.id
                        },
                        onAddToWorkout = { onAction(ExercisesUiAction.OnStartWorkoutWithExercise(ex)) },
                        onInspectFull = { onAction(ExercisesUiAction.OnSelectExercise(ex)) },
                        onEdit = { onAction(ExercisesUiAction.OnOpenEditor(ex)) },
                        onDelete = { onAction(ExercisesUiAction.OnDeleteCustomExercise(ex.id)) }
                    )
                }
            }

            if (catalogExercises.isNotEmpty()) {
                items(
                    items = catalogExercises,
                    key = { ex -> ex.id },
                    contentType = { "catalog_row" }
                ) { ex ->
                    ExerciseCatalogCard(
                        exercise = ex,
                        isCustom = false,
                        isExpanded = expandedTechniqueId == ex.id,
                        lang = lang,
                        onToggleTechnique = {
                            expandedTechniqueId = if (expandedTechniqueId == ex.id) null else ex.id
                        },
                        onAddToWorkout = { onAction(ExercisesUiAction.OnStartWorkoutWithExercise(ex)) },
                        onInspectFull = { onAction(ExercisesUiAction.OnSelectExercise(ex)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExerciseCatalogCard(
    exercise: Exercise,
    isCustom: Boolean,
    isExpanded: Boolean,
    lang: String,
    onToggleTechnique: () -> Unit,
    onAddToWorkout: () -> Unit,
    onInspectFull: () -> Unit,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null
) {
    GymCard(
        backgroundColor = DarkSurface,
        contentPadding = PaddingValues(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .clickable { onInspectFull() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ExerciseThumbnail(
                exerciseName = exercise.name,
                sourceId = exercise.sourceId,
                exerciseId = exercise.id,
                fallbackMuscle = exercise.primaryMuscle,
                size = ExerciseThumbnailSize.MD,
                onClick = onInspectFull
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isCustom) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(AccentLimeSubtle)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "O'zimniki"
                                    "en" -> "Custom"
                                    else -> "Своё"
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentLime
                            )
                        }
                    }
                }
                Text(
                    text = "${localizeMuscleName(exercise.primaryMuscle)} · ${localizeEquipmentName(exercise.equipment)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymPrimaryButton(
                text = when (lang) {
                    "uz" -> "Qo'shish"
                    "en" -> "Add"
                    else -> "Добавить"
                },
                onClick = onAddToWorkout
            )
            GymOutlineButton(
                text = if (isExpanded) {
                    when (lang) {
                        "uz" -> "Yopish"
                        "en" -> "Hide"
                        else -> "Скрыть"
                    }
                } else {
                    when (lang) {
                        "uz" -> "Texnika"
                        "en" -> "Technique"
                        else -> "Техника"
                    }
                },
                onClick = onToggleTechnique
            )
            if (isCustom && onEdit != null) {
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "Tahrirlash"
                        "en" -> "Edit"
                        else -> "Изменить"
                    },
                    onClick = onEdit
                )
            }
            if (isCustom && onDelete != null) {
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
        }

        if (isExpanded) {
            ExpandedTechniquePanel(
                exercise = exercise,
                lang = lang,
                onAddToWorkout = onAddToWorkout,
                onInspectFull = onInspectFull
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ExpandedTechniquePanel(
    exercise: Exercise,
    lang: String,
    onAddToWorkout: () -> Unit,
    onInspectFull: () -> Unit
) {
    GymInnerWell {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            AnimatedExerciseFramesCard(
                exerciseName = exercise.name,
                sourceId = exercise.sourceId,
                exerciseId = exercise.id,
                primaryMuscle = exercise.primaryMuscle,
                lang = lang
            )
            Text(
                text = exercise.instructions.ifBlank {
                    when (lang) {
                        "uz" -> "Asosiy: ${localizeMuscleName(exercise.primaryMuscle)}. Jihoz: ${localizeEquipmentName(exercise.equipment)}."
                        "en" -> "Primary: ${localizeMuscleName(exercise.primaryMuscle)}. Equipment: ${localizeEquipmentName(exercise.equipment)}."
                        else -> "Основные: ${localizeMuscleName(exercise.primaryMuscle)}. Оборудование: ${localizeEquipmentName(exercise.equipment)}."
                    }
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextSoft
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "Mashg'ulotga qo'shish"
                        "en" -> "Add to workout"
                        else -> "В тренировку"
                    },
                    onClick = onAddToWorkout
                )
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "Batafsil"
                        "en" -> "Details"
                        else -> "История и детали"
                    },
                    onClick = onInspectFull
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseEditorBottomSheet(
    existing: Exercise?,
    lang: String,
    onSave: (SaveExerciseRequestDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var primaryMuscle by remember { mutableStateOf(existing?.primaryMuscle ?: "chest") }
    var equipment by remember { mutableStateOf(existing?.equipment ?: "barbell") }
    val category by remember { mutableStateOf(existing?.category ?: "compound") }
    var instructions by remember { mutableStateOf(existing?.instructions ?: "") }

    val muscleChoices = listOf(
        "chest" to when (lang) { "uz" -> "Ko'krak"; "en" -> "Chest"; else -> "Грудь" },
        "back" to when (lang) { "uz" -> "Orqa"; "en" -> "Back"; else -> "Спина" },
        "legs" to when (lang) { "uz" -> "Oyoqlar"; "en" -> "Legs"; else -> "Ноги" },
        "shoulders" to when (lang) { "uz" -> "Yelkalar"; "en" -> "Shoulders"; else -> "Плечи" },
        "arms" to when (lang) { "uz" -> "Qo'llar"; "en" -> "Arms"; else -> "Руки" },
        "core" to when (lang) { "uz" -> "Press"; "en" -> "Core"; else -> "Пресс" }
    )
    val equipmentChoices = listOf(
        "barbell" to when (lang) { "uz" -> "Shtanga"; "en" -> "Barbell"; else -> "Штанга" },
        "dumbbell" to when (lang) { "uz" -> "Gantel"; "en" -> "Dumbbell"; else -> "Гантели" },
        "machine" to when (lang) { "uz" -> "Trenajyor"; "en" -> "Machine"; else -> "Тренажёр" },
        "cable" to when (lang) { "uz" -> "Blok"; "en" -> "Cable"; else -> "Блок" },
        "bodyweight" to when (lang) { "uz" -> "Tana vazni"; "en" -> "Bodyweight"; else -> "Свой вес" }
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
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (existing != null) {
                    when (lang) {
                        "uz" -> "Mashqni tahrirlash"
                        "en" -> "Edit exercise"
                        else -> "Изменить упражнение"
                    }
                } else {
                    when (lang) {
                        "uz" -> "Yangi mashq"
                        "en" -> "New custom exercise"
                        else -> "Новое упражнение"
                    }
                },
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = (-0.5).sp,
                color = TextPrimary
            )

            GymLabeledField(
                label = when (lang) {
                    "uz" -> "Mashq nomi"
                    "en" -> "Exercise name"
                    else -> "Название упражнения"
                },
                value = name,
                onValueChange = { name = it },
                placeholder = when (lang) {
                    "uz" -> "Masalan: Pauzali jim"
                    "en" -> "e.g. Pause bench press"
                    else -> "Например: Жим с паузой"
                }
            )

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (lang) {
                        "uz" -> "Maqsadli mushak"
                        "en" -> "Primary muscle"
                        else -> "Целевая мышца"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    muscleChoices.forEach { (key, label) ->
                        GymPillTab(
                            text = label,
                            selected = primaryMuscle == key,
                            onClick = { primaryMuscle = key }
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = when (lang) {
                        "uz" -> "Jihoz"
                        "en" -> "Equipment"
                        else -> "Оборудование"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    equipmentChoices.forEach { (key, label) ->
                        GymPillTab(
                            text = label,
                            selected = equipment == key,
                            onClick = { equipment = key }
                        )
                    }
                }
            }

            GymLabeledField(
                label = when (lang) {
                    "uz" -> "Texnika eslatmalari"
                    "en" -> "Technique cues"
                    else -> "Заметки по технике"
                },
                value = instructions,
                onValueChange = { instructions = it },
                placeholder = when (lang) {
                    "uz" -> "Tirsak holati, pauza, nafas..."
                    "en" -> "Elbow path, pause, tempo..."
                    else -> "Положение локтей, пауза, темп..."
                },
                singleLine = false,
                minLines = 2
            )

            GymPrimaryButton(
                text = when (lang) {
                    "uz" -> "Saqlash"
                    "en" -> "Save exercise"
                    else -> "Сохранить упражнение"
                },
                enabled = name.isNotBlank(),
                fillWidth = true,
                onClick = {
                    if (name.isBlank()) return@GymPrimaryButton
                    onSave(
                        SaveExerciseRequestDto(
                            name = name.trim(),
                            primaryMuscle = primaryMuscle,
                            equipment = equipment,
                            category = category,
                            instructions = instructions.trim()
                        )
                    )
                }
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
