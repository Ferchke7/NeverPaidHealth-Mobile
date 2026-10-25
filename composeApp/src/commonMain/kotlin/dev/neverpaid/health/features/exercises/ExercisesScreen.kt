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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.domain.model.Exercise

@Composable
fun ExercisesRoute(
    viewModel: ExercisesViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
    val muscles = listOf("All", "Chest", "Back", "Quads", "Hamstrings", "Shoulders", "Biceps", "Triceps", "Core")
    val equipments = listOf("All", "Barbell", "Dumbbell", "Cable", "Machine", "Bodyweight")

    if (state.selectedExerciseDetail != null) {
        ExerciseDetailDialog(
            exercise = state.selectedExerciseDetail,
            onDismiss = { onAction(ExercisesUiAction.OnDismissDetail) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Title
        Text(
            text = "Exercise Catalog",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Text(
            text = "870+ illustrated movements & form guides",
            fontSize = 13.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        // Search Input
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onAction(ExercisesUiAction.OnSearchQueryChanged(it)) },
            placeholder = { Text(text = "Search exercises...", color = TextMuted) },
            leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            trailingIcon = {
                if (state.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onAction(ExercisesUiAction.OnSearchQueryChanged("")) }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Clear", tint = TextMuted)
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = DarkSurfaceVariant,
                unfocusedContainerColor = DarkSurfaceVariant,
                focusedBorderColor = EmeraldNeon,
                unfocusedBorderColor = BorderSubtle
            )
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Muscle filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(muscles) { muscle ->
                val isSelected = state.selectedMuscle.equals(muscle, ignoreCase = true)
                FilterChipPill(
                    text = muscle,
                    isSelected = isSelected,
                    onClick = { onAction(ExercisesUiAction.OnSelectMuscle(muscle)) }
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Equipment filter chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(equipments) { eq ->
                val isSelected = state.selectedEquipment.equals(eq, ignoreCase = true)
                FilterChipPill(
                    text = eq,
                    isSelected = isSelected,
                    onClick = { onAction(ExercisesUiAction.OnSelectEquipment(eq)) },
                    selectedColor = IndigoElectric
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (state.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = EmeraldNeon)
            }
        } else if (state.exercises.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = "No movements found matching filters", color = TextSecondary)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(
                    items = state.exercises,
                    key = { it.id }
                ) { ex ->
                    ExerciseCatalogItem(
                        exercise = ex,
                        onClick = { onAction(ExercisesUiAction.OnSelectExercise(ex)) }
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
    onClick: () -> Unit
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
            Row(verticalAlignment = Alignment.CenterVertically) {
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

                Column {
                    Text(
                        text = exercise.name,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${exercise.primaryMuscleGroup.replaceFirstChar { it.uppercase() }} • ${exercise.equipment}",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Details",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ExerciseDetailDialog(
    exercise: Exercise,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(text = exercise.name, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Spacer(modifier = Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldSubtle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = exercise.primaryMuscleGroup.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(IndigoSubtle)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(text = exercise.equipment.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = IndigoElectric)
                    }
                }
            }
        },
        text = {
            Column {
                Text(text = "FORM & EXECUTION GUIDE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = exercise.instructions.ifBlank { "Perform controlled reps through a full range of motion. Maintain steady breathing and core engagement." },
                    fontSize = 14.sp,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
                if (exercise.secondaryMuscleGroups.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(text = "SECONDARY MUSCLES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = exercise.secondaryMuscleGroups.joinToString(", ") { it.replaceFirstChar { c -> c.uppercase() } }, fontSize = 13.sp, color = TextSecondary)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = DarkBackground)
            ) {
                Text(text = "Got It", fontWeight = FontWeight.Bold)
            }
        },
        containerColor = DarkSurface
    )
}
