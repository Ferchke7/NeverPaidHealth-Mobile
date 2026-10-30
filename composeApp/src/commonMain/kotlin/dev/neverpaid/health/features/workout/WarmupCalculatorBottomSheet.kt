package dev.neverpaid.health.features.workout

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.InteractivePlateCalculator
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.BarbellType
import dev.neverpaid.health.core.math.WarmupProtocol
import dev.neverpaid.health.core.math.WarmupProtocolEngine
import dev.neverpaid.health.core.math.WarmupSetStep

/**
 * BottomSheet providing an Olympic barbell plate loader and scientific warmup protocol generator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WarmupCalculatorBottomSheet(
    isOpen: Boolean,
    workingWeightKg: Double,
    exerciseName: String,
    onDismiss: () -> Unit,
    onApplyWarmupSets: (List<Pair<Double, Int>>) -> Unit = {}
) {
    if (!isOpen) return

    val strings = LocalDudaStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var targetWeight by remember(workingWeightKg) { mutableDoubleStateOf(if (workingWeightKg > 0) workingWeightKg else 100.0) }
    var selectedWarmupProtocol by remember { mutableStateOf(WarmupProtocol.STANDARD_HYPERTROPHY) }

    val calculatedWarmupSets = remember(targetWeight, selectedWarmupProtocol) {
        WarmupProtocolEngine.generateWarmupSets(
            targetWorkingWeightKg = targetWeight,
            protocol = selectedWarmupProtocol,
            barWeightKg = 20.0
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.FitnessCenter,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = strings.plateCalculatorTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (exerciseName.isNotEmpty()) {
                            Text(
                                text = exerciseName,
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary
                            )
                        }
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(imageVector = Icons.Default.Close, contentDescription = strings.cancel, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Plate Calculator & Sleeve Canvas
            InteractivePlateCalculator(
                initialWeightKg = targetWeight,
                onWeightConfirmed = { newWeight ->
                    targetWeight = newWeight
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Warmup Protocols Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.warmupProtocolsTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Protocol Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    WarmupProtocol.STANDARD_HYPERTROPHY to "Hypertrophy",
                    WarmupProtocol.POWERLIFTING_HEAVY to "Heavy Single",
                    WarmupProtocol.QUICK_POTENTIATION to "Activation"
                ).forEach { (protocol, label) ->
                    FilterChip(
                        selected = selectedWarmupProtocol == protocol,
                        onClick = { selectedWarmupProtocol = protocol },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldNeon,
                            containerColor = DarkSurfaceElevated,
                            labelColor = TextSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selectedWarmupProtocol == protocol,
                            borderColor = BorderSubtle,
                            selectedBorderColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Warmup Sets Table
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface),
                border = BorderStroke(1.dp, BorderSubtle)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    calculatedWarmupSets.forEachIndexed { idx, set ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "W-${idx + 1}: ${set.percentageOfTarget}%",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            Text(
                                text = "${set.weightKg} kg × ${set.reps} reps",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            Text(
                                text = "${set.suggestedRestSeconds}s rest",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextMuted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Button: Apply Warmup Sets
            FilledTonalButton(
                onClick = {
                    val sets = calculatedWarmupSets.map { it.weightKg to it.reps }
                    onApplyWarmupSets(sets)
                    onDismiss()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmeraldNeon,
                    contentColor = DarkBackground
                )
            ) {
                Text(
                    text = "${strings.workoutAddSet} (${calculatedWarmupSets.size})",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
