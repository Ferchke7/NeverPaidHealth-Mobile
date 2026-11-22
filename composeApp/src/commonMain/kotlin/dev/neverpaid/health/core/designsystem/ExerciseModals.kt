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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.i18n.LocalDudaStrings
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
    title: String = "",
    onSelectExercise: (Exercise) -> Unit,
    onInspectExercise: ((Exercise) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val strings = LocalDudaStrings.current
    val effectiveTitle = title.ifBlank { strings.exercisePickTitle }
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
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderLilacStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = effectiveTitle,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.commonClose, tint = TextSecondary)
                }
            }

            GymTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = strings.exerciseSearchPlaceholder,
                singleLine = true
            )

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(muscleFilterOptions, key = { it.first }) { (key, label) ->
                    GymPillTab(
                        text = label,
                        selected = selectedMuscle == key,
                        onClick = { selectedMuscle = key }
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filtered, key = { it.id }) { exercise ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(RoyalWell)
                            .border(1.dp, BorderLilac, RoundedCornerShape(18.dp))
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
                            ExerciseThumbnail(
                                exerciseName = exercise.name,
                                sourceId = exercise.sourceId,
                                exerciseId = exercise.id,
                                fallbackMuscle = exercise.primaryMuscle,
                                size = ExerciseThumbnailSize.MD,
                                onClick = if (onInspectExercise != null) {
                                    { onInspectExercise(exercise) }
                                } else null
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = exercise.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${localizeMuscleName(exercise.primaryMuscle)} · ${localizeEquipmentName(exercise.equipment)}",
                                    fontSize = 12.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (onInspectExercise != null) {
                                IconButton(
                                    onClick = { onInspectExercise(exercise) },
                                    modifier = Modifier.size(36.dp)
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
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(AccentLime),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "Выбрать",
                                    tint = GymInkOnLight,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
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
fun ExerciseInfoBottomSheet(
    exercise: Exercise,
    onStartWithExercise: ((Exercise) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val strings = LocalDudaStrings.current
    val lang = strings.languageCode

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
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderLilacStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = exercise.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(SurfaceLilac)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = localizeMuscleName(exercise.primaryMuscle),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VioletPrimary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(RoyalWell)
                                .border(1.dp, BorderLilac, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = localizeEquipmentName(exercise.equipment),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(RoyalWell)
                                .border(1.dp, BorderLilac, CircleShape)
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = if (exercise.category == "isolation") "Изоляция" else "База",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSoft
                            )
                        }
                    }
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.commonClose, tint = TextSecondary)
                }
            }

            AnimatedExerciseFramesCard(
                exerciseName = exercise.name,
                sourceId = exercise.sourceId,
                exerciseId = exercise.id,
                primaryMuscle = exercise.primaryMuscle,
                lang = lang
            )

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GymKicker(text = strings.exerciseTechniqueLabel)

                instructionsList.forEachIndexed { idx, step ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoyalWell)
                            .border(1.dp, BorderLilac, RoundedCornerShape(14.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(VioletPrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${idx + 1}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = InkWhite
                            )
                        }
                        Text(
                            text = step,
                            fontSize = 14.sp,
                            color = TextPrimary,
                            lineHeight = 20.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (onStartWithExercise != null) {
                PrimaryAthleticButton(
                    text = strings.exerciseAddToWorkout,
                    onClick = {
                        onStartWithExercise(exercise)
                        onDismiss()
                    },
                    icon = Icons.Default.FitnessCenter
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkoutSummaryBottomSheet(
    workout: Workout,
    onDismiss: () -> Unit
) {
    val strings = LocalDudaStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val durationMinutes = (workout.durationSeconds / 60).coerceAtLeast(1)
    val completedSetsCount = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }
    val tonsFormatted = ((workout.totalVolume / 100.0).toInt() / 10.0)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderLilacStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Royal Violet Receipt Hero Header + Overlapping Emerald Checkmark (Figma E-Wallet Receipt Card)
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(84.dp)
                        .clip(RoundedCornerShape(22.dp))
                        .background(RoyalHeroBrush),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.summaryReceiptTitle.uppercase(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.2.sp,
                        color = HeroMuted
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(y = (-28).dp)
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(EmeraldSuccess)
                        .border(4.dp, SurfaceWhite, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = InkWhite,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.offset(y = (-14).dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = workout.title,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(EmeraldBg)
                        .border(1.dp, EmeraldBorder, CircleShape)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "✓ ${strings.summaryAutoSavedBadge}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldDark
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(RoyalWell)
                    .border(1.dp, BorderLilac, RoundedCornerShape(20.dp))
                    .padding(vertical = 16.dp, horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                StatCounter(
                    title = strings.summaryDurationLabel,
                    value = "$durationMinutes",
                    unit = "мин",
                    accentColor = TextPrimary
                )
                StatCounter(
                    title = strings.volumeTonnage,
                    value = if (workout.totalVolume >= 1000) "$tonsFormatted" else StrengthCalculator.formatVolume(workout.totalVolume),
                    unit = if (workout.totalVolume >= 1000) strings.summaryTonsUnit else strings.unitKg,
                    accentColor = VioletPrimary
                )
                StatCounter(
                    title = strings.dashboardSetsShort,
                    value = "$completedSetsCount",
                    unit = "",
                    accentColor = EmeraldDark
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                workout.exercises.forEach { ex ->
                    val bestSet = ex.sets.filter { it.isCompleted }.maxByOrNull { it.weightKg * (1 + it.reps / 30.0) }
                        ?: ex.sets.firstOrNull()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(RoyalWell)
                            .border(1.dp, BorderLilac, RoundedCornerShape(14.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${ex.sets.size}× ${ex.exercise.name}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        if (bestSet != null) {
                            Text(
                                text = "${bestSet.weightKg.toInt()} ${strings.unitKg} × ${bestSet.reps}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VioletPrimary
                            )
                        }
                    }
                }
            }

            GymPrimaryButton(
                text = strings.done,
                onClick = onDismiss,
                fillWidth = true
            )

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
