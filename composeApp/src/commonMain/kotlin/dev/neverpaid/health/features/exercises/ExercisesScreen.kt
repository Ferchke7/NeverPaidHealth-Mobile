package dev.neverpaid.health.features.exercises

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AnimatedExerciseFramesCard
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.ExerciseInfoBottomSheet
import dev.neverpaid.health.core.designsystem.ExerciseThumbnail
import dev.neverpaid.health.core.designsystem.ExerciseThumbnailSize
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymEmptyCard
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.TextMuted
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
            "uz" -> "barchasi"
            "en" -> "all"
            else -> "все"
        },
        "chest" to when (lang) {
            "uz" -> "ko'krak"
            "en" -> "chest"
            else -> "грудь"
        },
        "back" to when (lang) {
            "uz" -> "orqa"
            "en" -> "back"
            else -> "спина"
        },
        "legs" to when (lang) {
            "uz" -> "oyoqlar"
            "en" -> "legs"
            else -> "ноги"
        },
        "shoulders" to when (lang) {
            "uz" -> "yelkalar"
            "en" -> "shoulders"
            else -> "плечи"
        },
        "arms" to when (lang) {
            "uz" -> "qo'llar"
            "en" -> "arms"
            else -> "руки"
        },
        "core" to when (lang) {
            "uz" -> "press"
            "en" -> "core"
            else -> "пресс"
        }
    )
    val equipments = listOf(
        "All" to when (lang) {
            "uz" -> "barchasi"
            "en" -> "all"
            else -> "всё"
        },
        "barbell" to when (lang) {
            "uz" -> "shtanga"
            "en" -> "barbell"
            else -> "штанга"
        },
        "dumbbell" to when (lang) {
            "uz" -> "gantel"
            "en" -> "dumbbell"
            else -> "гантели"
        },
        "cable" to when (lang) {
            "uz" -> "blok"
            "en" -> "cable"
            else -> "блок"
        },
        "machine" to when (lang) {
            "uz" -> "trenajyor"
            "en" -> "machine"
            else -> "тренажёр"
        },
        "bodyweight" to when (lang) {
            "uz" -> "tana vazni"
            "en" -> "bodyweight"
            else -> "свой вес"
        }
    )

    var showMuscleFilter by remember { mutableStateOf(false) }
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

    val selectedMuscleLabel = muscles.find { it.first.equals(state.selectedMuscle, ignoreCase = true) }?.second ?: muscles.first().second
    val selectedEquipLabel = equipments.find { it.first.equals(state.selectedEquipment, ignoreCase = true) }?.second ?: equipments.first().second

    val catalogExercises = remember(state.exercises) { state.exercises.filter { !it.isCustom } }
    val customExercises = remember(state.exercises) { state.exercises.filter { it.isCustom } }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header", contentType = "header") {
            GymPageHeader(
                title = strings.navExercises,
                subtitle = when (lang) {
                    "uz" -> "Harakatlar katalogi va bajarish texnikasi"
                    "en" -> "Movement catalog and execution cues"
                    else -> "Каталог движений и техника выполнения"
                }
            )
        }

        item(key = "controls", contentType = "controls") {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
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
                            "uz" -> "O'zimniki"
                            "en" -> "Create custom"
                            else -> "Создать своё"
                        },
                        onClick = { onAction(ExercisesUiAction.OnOpenEditor(null)) }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymOutlineButton(
                        text = when (lang) {
                            "uz" -> "Mushaklar: $selectedMuscleLabel"
                            "en" -> "Muscles: $selectedMuscleLabel"
                            else -> "Мышцы: $selectedMuscleLabel"
                        },
                        onClick = {
                            showMuscleFilter = !showMuscleFilter
                            if (showMuscleFilter) showEquipFilter = false
                        }
                    )
                    GymOutlineButton(
                        text = when (lang) {
                            "uz" -> "Jihoz: $selectedEquipLabel"
                            "en" -> "Equipment: $selectedEquipLabel"
                            else -> "Оборудование: $selectedEquipLabel"
                        },
                        onClick = {
                            showEquipFilter = !showEquipFilter
                            if (showEquipFilter) showMuscleFilter = false
                        }
                    )
                }

                if (showMuscleFilter) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        muscles.forEach { (key, label) ->
                            GymPillTab(
                                text = label.replaceFirstChar { it.uppercase() },
                                selected = state.selectedMuscle.equals(key, ignoreCase = true),
                                onClick = {
                                    onAction(ExercisesUiAction.OnSelectMuscle(key))
                                    showMuscleFilter = false
                                }
                            )
                        }
                    }
                }

                if (showEquipFilter) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        equipments.forEach { (key, label) ->
                            GymPillTab(
                                text = label.replaceFirstChar { it.uppercase() },
                                selected = state.selectedEquipment.equals(key, ignoreCase = true),
                                onClick = {
                                    onAction(ExercisesUiAction.OnSelectEquipment(key))
                                    showEquipFilter = false
                                }
                            )
                        }
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymPillTab(
                        text = when (lang) {
                            "uz" -> "Barcha mashqlar"
                            "en" -> "All exercises"
                            else -> "Все упражнения"
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
                }
            }
        }

        item(key = "library_heading", contentType = "section_heading") {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = when (lang) {
                        "uz" -> "Mashqlar kutubxonasi"
                        "en" -> "Exercise library"
                        else -> "Библиотека упражнений"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = when (lang) {
                        "uz" -> "${state.exercises.size} mashq · Kuch · Vazn va takrorlar"
                        "en" -> "${state.exercises.size} exercises · Strength · Weight & reps"
                        else -> "${state.exercises.size} упражнений · Силовые · Вес и повторения"
                    },
                    fontSize = 13.sp,
                    color = TextSecondary
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
            if (catalogExercises.isNotEmpty()) {
                itemsIndexed(
                    items = catalogExercises,
                    key = { _, ex -> ex.id },
                    contentType = { _, _ -> "catalog_row" }
                ) { index, ex ->
                    GroupedCatalogExerciseRow(
                        exercise = ex,
                        isFirst = index == 0,
                        isLast = index == catalogExercises.lastIndex,
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

            if (customExercises.isNotEmpty()) {
                itemsIndexed(
                    items = customExercises,
                    key = { _, ex -> "custom_${ex.id}" },
                    contentType = { _, _ -> "custom_card" }
                ) { _, ex ->
                    CustomExerciseCard(
                        exercise = ex,
                        isExpanded = expandedTechniqueId == ex.id,
                        lang = lang,
                        onToggleTechnique = {
                            expandedTechniqueId = if (expandedTechniqueId == ex.id) null else ex.id
                        },
                        onAddToWorkout = { onAction(ExercisesUiAction.OnStartWorkoutWithExercise(ex)) },
                        onEdit = { onAction(ExercisesUiAction.OnOpenEditor(ex)) },
                        onDelete = { onAction(ExercisesUiAction.OnDeleteCustomExercise(ex.id)) },
                        onInspectFull = { onAction(ExercisesUiAction.OnSelectExercise(ex)) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GroupedCatalogExerciseRow(
    exercise: Exercise,
    isFirst: Boolean,
    isLast: Boolean,
    isExpanded: Boolean,
    lang: String,
    onToggleTechnique: () -> Unit,
    onAddToWorkout: () -> Unit,
    onInspectFull: () -> Unit
) {
    val shape = RoundedCornerShape(
        topStart = if (isFirst) 14.dp else 0.dp,
        topEnd = if (isFirst) 14.dp else 0.dp,
        bottomStart = if (isLast) 14.dp else 0.dp,
        bottomEnd = if (isLast) 14.dp else 0.dp
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(DarkSurface, shape)
            .border(1.dp, BorderSubtle, shape)
            .drawWithContent {
                drawContent()
                val stroke = 1.dp.toPx()
                if (!isFirst) {
                    drawLine(
                        color = DarkSurface,
                        start = Offset(stroke, stroke / 2f),
                        end = Offset(size.width - stroke, stroke / 2f),
                        strokeWidth = stroke * 1.6f
                    )
                }
                if (!isLast) {
                    drawLine(
                        color = DarkSurface,
                        start = Offset(stroke, size.height - stroke / 2f),
                        end = Offset(size.width - stroke, size.height - stroke / 2f),
                        strokeWidth = stroke * 1.6f
                    )
                }
            }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onInspectFull() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                    Text(
                        text = exercise.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "${localizeMuscleName(exercise.primaryMuscle)} · ${localizeEquipmentName(exercise.equipment)}",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GymOutlineButton(
                    text = if (isExpanded) {
                        when (lang) {
                            "uz" -> "Texnikani yopish"
                            "en" -> "Hide cues"
                            else -> "Скрыть технику"
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
                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "Qo'shish"
                        "en" -> "Add"
                        else -> "Добавить"
                    },
                    onClick = onAddToWorkout
                )
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

        if (!isLast) {
            GymDivider(
                modifier = Modifier.padding(horizontal = 24.dp),
                color = BorderSubtle
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CustomExerciseCard(
    exercise: Exercise,
    isExpanded: Boolean,
    lang: String,
    onToggleTechnique: () -> Unit,
    onAddToWorkout: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onInspectFull: () -> Unit
) {
    GymCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onInspectFull() },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                Text(
                    text = exercise.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = when (lang) {
                        "uz" -> "Shaxsiy mashq · ${localizeMuscleName(exercise.primaryMuscle)} · ${localizeEquipmentName(exercise.equipment)}"
                        "en" -> "Custom exercise · ${localizeMuscleName(exercise.primaryMuscle)} · ${localizeEquipmentName(exercise.equipment)}"
                        else -> "Своё упражнение · ${localizeMuscleName(exercise.primaryMuscle)} · ${localizeEquipmentName(exercise.equipment)}"
                    },
                    fontSize = 13.sp,
                    color = TextSecondary
                )
            }
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymOutlineButton(
                text = if (isExpanded) {
                    when (lang) {
                        "uz" -> "Texnikani yopish"
                        "en" -> "Hide cues"
                        else -> "Скрыть технику"
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
            GymPrimaryButton(
                text = when (lang) {
                    "uz" -> "Qo'shish"
                    "en" -> "Add"
                    else -> "Добавить"
                },
                onClick = onAddToWorkout
            )
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
                onClick = onDelete
            )
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = when (lang) {
                "uz" -> "Texnika · ${exercise.name}"
                "en" -> "Technique · ${exercise.name}"
                else -> "Техника · ${exercise.name}"
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
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
            fontSize = 13.sp,
            lineHeight = 19.sp,
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
                    else -> "Добавить в тренировку"
                },
                onClick = onAddToWorkout
            )
            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "Batafsil va rekordlar"
                    "en" -> "Details & records"
                    else -> "История и детали"
                },
                onClick = onInspectFull
            )
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
        contentColor = TextPrimary
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
                        "uz" -> "Yangi mashq yaratish"
                        "en" -> "Create custom exercise"
                        else -> "Создать своё упражнение"
                    }
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
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
                        "uz" -> "Maqsadli mushak guruhi"
                        "en" -> "Primary muscle"
                        else -> "Целевая мышечная группа"
                    },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
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
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
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
                    "uz" -> "Bajarish texnikasi bo'yicha eslatmalar"
                    "en" -> "Technique cues"
                    else -> "Заметки по технике выполнения"
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
