package dev.neverpaid.health.core.designsystem

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.Workout

val muscleFilterOptions = listOf(
    "all" to "Все",
    "chest" to "Грудь",
    "back" to "Спина",
    "legs" to "Ноги",
    "shoulders" to "Плечи",
    "arms" to "Руки",
    "core" to "Пресс"
)

fun localizeMuscleName(muscle: String): String = when (muscle.lowercase()) {
    "chest" -> "Грудь"
    "back" -> "Спина"
    "legs", "quadriceps", "hamstrings", "glutes", "calves" -> "Ноги"
    "shoulders" -> "Плечи"
    "arms", "biceps", "triceps", "forearms" -> "Руки"
    "core", "abs" -> "Пресс"
    "cardio" -> "Кардио"
    "full_body" -> "Всё тело"
    else -> muscle
}

fun localizeEquipmentName(equipment: String): String = when (equipment.lowercase()) {
    "barbell" -> "Штанга"
    "dumbbell" -> "Гантели"
    "machine" -> "Тренажёр"
    "cable" -> "Блок / Кроссовер"
    "bodyweight" -> "Свой вес"
    "kettlebell" -> "Гиря"
    "other" -> "Другое"
    else -> equipment
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExercisePickerBottomSheet(
    exercises: List<Exercise>,
    title: String = "Выбрать упражнение",
    onSelectExercise: (Exercise) -> Unit,
    onInspectExercise: ((Exercise) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedMuscle by remember { mutableStateOf("all") }

    val filtered = remember(exercises, searchQuery, selectedMuscle) {
        exercises.filter { ex ->
            val matchesMuscle = selectedMuscle == "all" ||
                ex.primaryMuscle.equals(selectedMuscle, ignoreCase = true) ||
                (selectedMuscle == "arms" && ex.primaryMuscle.lowercase() in listOf("arms", "biceps", "triceps")) ||
                (selectedMuscle == "legs" && ex.primaryMuscle.lowercase() in listOf("legs", "quadriceps", "hamstrings", "glutes", "calves"))
            val matchesSearch = searchQuery.isBlank() ||
                ex.name.contains(searchQuery, ignoreCase = true) ||
                localizeMuscleName(ex.primaryMuscle).contains(searchQuery, ignoreCase = true)
            matchesMuscle && matchesSearch
        }
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
                .fillMaxHeight(0.88f)
                .padding(horizontal = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
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
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Поиск упражнения...", fontSize = 13.sp, color = TextMuted) },
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

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscleFilterOptions, key = { it.first }) { (key, label) ->
                    val isSelected = selectedMuscle == key
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedMuscle = key },
                        label = {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldLight,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    AthleticCard(
                        backgroundColor = DarkSurfaceVariant,
                        borderColor = BorderSubtle
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectExercise(exercise)
                                    onDismiss()
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(EmeraldSubtle),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FitnessCenter,
                                        contentDescription = null,
                                        tint = EmeraldLight,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = exercise.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Text(
                                        text = "${localizeMuscleName(exercise.primaryMuscle)} • ${localizeEquipmentName(exercise.equipment)}",
                                        fontSize = 11.sp,
                                        color = TextSecondary
                                    )
                                }
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (onInspectExercise != null) {
                                    IconButton(
                                        onClick = { onInspectExercise(exercise) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Info,
                                            contentDescription = "Техника",
                                            tint = TextSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(EmeraldNeon)
                                        .padding(horizontal = 10.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        text = "+ Выбрать",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Black
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExerciseInfoBottomSheet(
    exercise: Exercise,
    onStartWithExercise: ((Exercise) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var activeFrame by remember { mutableIntStateOf(0) }

    val instructionsList = remember(exercise) {
        if (exercise.instructions.isNotBlank()) {
            exercise.instructions.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        } else {
            listOf(
                "Зафиксируйте корпус, сведите лопатки и создайте жёсткую опору стопами.",
                "Выполняйте негативную фазу подконтрольно (2–3 секунды) с полным растяжением целевой мышцы.",
                "На выдохе мощно выполните концентрическую фазу без рывков и читинга.",
                "В пиковой точке задержитесь на 0.5–1 секунду для максимального рекрутирования волокон."
            )
        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 18.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = exercise.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(EmeraldSubtle)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = localizeMuscleName(exercise.primaryMuscle),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(DarkSurfaceHigh)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = localizeEquipmentName(exercise.equipment),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(IndigoSubtle)
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (exercise.category == "isolation") "Изолирующее" else "Базовое",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = IndigoElectric
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 2-Frame Biomechanics Visual Card (matching ExerciseInfoModal.tsx)
            AthleticCard(
                backgroundColor = DarkSurfaceVariant,
                borderColor = BorderHighlight
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (activeFrame == 0) "ФАЗА 1: ИСХОДНОЕ ПОЛОЖЕНИЕ" else "ФАЗА 2: ПИКОВОЕ СОКРАЩЕНИЕ",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(0 to "Старт", 1 to "Пик").forEach { (idx, label) ->
                                val selected = activeFrame == idx
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (selected) EmeraldNeon else DarkSurfaceHigh)
                                        .clickable { activeFrame = idx }
                                        .padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (selected) Color.Black else TextSecondary
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkSurface)
                            .padding(16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.FitnessCenter,
                                contentDescription = null,
                                tint = if (activeFrame == 0) EmeraldLight else AmberWarm,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (activeFrame == 0) {
                                    "Растяжение под контролем • Вдох • Стабилизация лопаток и кора"
                                } else {
                                    "Мощное сокращение ${localizeMuscleName(exercise.primaryMuscle).lowercase()} • Выдох • Пауза 0.5с в верхней точке"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "ТЕХНИКА ВЫПОЛНЕНИЯ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            instructionsList.forEachIndexed { idx, step ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${idx + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = step,
                        fontSize = 13.sp,
                        color = TextPrimary,
                        lineHeight = 19.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            if (onStartWithExercise != null) {
                Spacer(modifier = Modifier.height(16.dp))
                PrimaryAthleticButton(
                    text = "Добавить в активную тренировку",
                    onClick = {
                        onStartWithExercise(exercise)
                        onDismiss()
                    },
                    icon = Icons.Default.FitnessCenter
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryBottomSheet(
    workout: Workout,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val durationMinutes = (workout.durationSeconds / 60).coerceAtLeast(1)
    val completedSetsCount = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(EmeraldSubtle)
                    .border(2.dp, EmeraldNeon, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = AmberWarm,
                    modifier = Modifier.size(34.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "ТРЕНИРОВКА ЗАВЕРШЕНА!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            Text(
                text = workout.title,
                fontSize = 14.sp,
                color = EmeraldLight,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(16.dp))

            AthleticCard(
                backgroundColor = DarkSurfaceVariant,
                borderColor = BorderHighlight
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 14.dp, horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    StatCounter(
                        title = "Время",
                        value = "$durationMinutes",
                        unit = "мин",
                        accentColor = TextPrimary
                    )
                    StatCounter(
                        title = "Тоннаж",
                        value = StrengthCalculator.formatVolume(workout.totalVolume),
                        unit = "кг",
                        accentColor = EmeraldNeon
                    )
                    StatCounter(
                        title = "Подходы",
                        value = "$completedSetsCount",
                        unit = "вып.",
                        accentColor = AmberWarm
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                workout.exercises.forEach { ex ->
                    val bestSet = ex.sets.filter { it.isCompleted }.maxByOrNull { it.weightKg * (1 + it.reps / 30.0) }
                        ?: ex.sets.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ex.sets.size}× ${ex.exercise.name}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (bestSet != null) {
                            Text(
                                text = "Лучший: ${bestSet.weightKg.toInt()} кг × ${bestSet.reps}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = Color.Black
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Отличная работа!", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
