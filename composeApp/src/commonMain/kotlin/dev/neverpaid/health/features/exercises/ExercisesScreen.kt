package dev.neverpaid.health.features.exercises

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
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
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.localizeEquipmentName
import dev.neverpaid.health.core.designsystem.localizeMuscleName
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

@Composable
fun ExercisesScreen(
    state: ExercisesUiState,
    onAction: (ExercisesUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val muscles = listOf(
        "All" to "Все мышцы",
        "chest" to "Грудь",
        "back" to "Спина",
        "legs" to "Ноги",
        "shoulders" to "Плечи",
        "arms" to "Руки",
        "core" to "Пресс"
    )
    val equipments = listOf(
        "All" to "Любой инвентарь",
        "barbell" to "Штанга",
        "dumbbell" to "Гантели",
        "cable" to "Блок",
        "machine" to "Тренажёр",
        "bodyweight" to "Свой вес"
    )

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
            onSave = { req ->
                onAction(ExercisesUiAction.OnSaveCustomExercise(req, state.editingExercise?.id))
            },
            onDismiss = { onAction(ExercisesUiAction.OnCloseEditor) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(6.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "База упражнений",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Техника выполнения, биомеханика и свои движения",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Button(
                onClick = { onAction(ExercisesUiAction.OnOpenEditor(null)) },
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = Color.Black
                ),
                modifier = Modifier.height(38.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Своё упр.", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Tab Switcher: Все / Мои упражнения
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(DarkSurfaceVariant)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("all" to "Все упражнения", "custom" to "Мои упражнения").forEach { (key, label) ->
                val selected = state.selectedTab == key
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) EmeraldNeon else Color.Transparent)
                        .clickable { onAction(ExercisesUiAction.OnSelectTab(key)) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.Black else TextSecondary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(ExercisesUiAction.OnSearchQueryChanged(it)) },
            placeholder = { Text(text = "Поиск по названию или мышце...", color = TextMuted, fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onAction(ExercisesUiAction.OnSearchQueryChanged("")) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
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

        Spacer(modifier = Modifier.height(10.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(muscles, key = { it.first }) { (key, label) ->
                val isSelected = state.selectedMuscle.equals(key, ignoreCase = true)
                FilterChipPill(
                    text = label,
                    isSelected = isSelected,
                    onClick = { onAction(ExercisesUiAction.OnSelectMuscle(key)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(equipments, key = { it.first }) { (key, label) ->
                val isSelected = state.selectedEquipment.equals(key, ignoreCase = true)
                FilterChipPill(
                    text = label,
                    isSelected = isSelected,
                    onClick = { onAction(ExercisesUiAction.OnSelectEquipment(key)) },
                    selectedColor = IndigoElectric
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        if (state.isLoading && state.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldNeon)
            }
        } else if (state.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "Упражнения не найдены", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(
                    items = state.exercises,
                    key = { it.id }
                ) { ex ->
                    ExerciseCatalogItem(
                        exercise = ex,
                        onClick = { onAction(ExercisesUiAction.OnSelectExercise(ex)) },
                        onEdit = if (ex.isCustom) {
                            { onAction(ExercisesUiAction.OnOpenEditor(ex)) }
                        } else null,
                        onDelete = if (ex.isCustom) {
                            { onAction(ExercisesUiAction.OnDeleteCustomExercise(ex.id)) }
                        } else null
                    )
                }
                item { Spacer(modifier = Modifier.height(24.dp)) }
            }
        }
    }
}

@Composable
private fun FilterChipPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    selectedColor: Color = EmeraldNeon
) {
    val bg = if (isSelected) selectedColor.copy(alpha = 0.2f) else DarkSurfaceVariant
    val border = if (isSelected) selectedColor else BorderSubtle
    val txt = if (isSelected) selectedColor else TextSecondary

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(bg)
            .border(1.dp, border, RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        Text(text = text, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = txt)
    }
}

@Composable
private fun ExerciseCatalogItem(
    exercise: Exercise,
    onClick: () -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    AthleticCard(
        modifier = Modifier.clickable { onClick() },
        backgroundColor = DarkSurfaceVariant
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${localizeMuscleName(exercise.primaryMuscle)} • ${localizeEquipmentName(exercise.equipment)}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onEdit != null) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    }
                }
                if (onDelete != null) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = null, tint = CrimsonRed, modifier = Modifier.size(16.dp))
                    }
                }
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Details",
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomExerciseEditorBottomSheet(
    existing: Exercise?,
    onSave: (SaveExerciseRequestDto) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var primaryMuscle by remember { mutableStateOf(existing?.primaryMuscle ?: "chest") }
    var equipment by remember { mutableStateOf(existing?.equipment ?: "barbell") }
    var category by remember { mutableStateOf(existing?.category ?: "compound") }
    var instructions by remember { mutableStateOf(existing?.instructions ?: "") }

    val muscleChoices = listOf(
        "chest" to "Грудь",
        "back" to "Спина",
        "legs" to "Ноги",
        "shoulders" to "Плечи",
        "arms" to "Руки",
        "core" to "Пресс"
    )
    val equipmentChoices = listOf(
        "barbell" to "Штанга",
        "dumbbell" to "Гантели",
        "machine" to "Тренажёр",
        "cable" to "Блок",
        "bodyweight" to "Свой вес"
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
            Text(
                text = if (existing != null) "Изменить упражнение" else "Создать своё упражнение",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название упражнения") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text("Целевая мышечная группа", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscleChoices) { (key, label) ->
                    FilterChip(
                        selected = primaryMuscle == key,
                        onClick = { primaryMuscle = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text("Оборудование", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(6.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(equipmentChoices) { (key, label) ->
                    FilterChip(
                        selected = equipment == key,
                        onClick = { equipment = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = instructions,
                onValueChange = { instructions = it },
                label = { Text("Заметки по технике выполнения") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (name.isBlank()) return@Button
                    onSave(
                        SaveExerciseRequestDto(
                            name = name.trim(),
                            primaryMuscle = primaryMuscle,
                            equipment = equipment,
                            category = category,
                            instructions = instructions.trim()
                        )
                    )
                },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = Color.Black
                )
            ) {
                Text("Сохранить упражнение", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(22.dp))
        }
    }
}
