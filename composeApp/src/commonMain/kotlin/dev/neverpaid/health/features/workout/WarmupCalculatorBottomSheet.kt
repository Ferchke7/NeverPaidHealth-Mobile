package dev.neverpaid.health.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymIconBadge
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.InteractivePlateCalculator
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.WarmupProtocol
import dev.neverpaid.health.core.math.WarmupProtocolEngine

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

    var targetWeight by remember(workingWeightKg) {
        mutableDoubleStateOf(if (workingWeightKg > 0) workingWeightKg else 100.0)
    }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GymIconBadge(
                        icon = Icons.Default.FitnessCenter,
                        tint = AccentLime,
                        background = AccentLimeSubtle,
                        size = 42.dp
                    )
                    Column {
                        Text(
                            text = strings.plateCalculatorTitle,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (exerciseName.isNotEmpty()) {
                            Text(
                                text = exerciseName,
                                fontSize = 12.sp,
                                color = TextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceHigh)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = strings.cancel,
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interactive Plate Calculator & Sleeve Canvas
            InteractivePlateCalculator(
                initialWeightKg = targetWeight,
                onWeightConfirmed = { newWeight ->
                    targetWeight = newWeight
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Warmup Protocols Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Speed,
                    contentDescription = null,
                    tint = AccentLime,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = strings.warmupProtocolsTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Protocol Selector (flat pill tabs, zero hairline borders)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    WarmupProtocol.STANDARD_HYPERTROPHY to "Hypertrophy",
                    WarmupProtocol.POWERLIFTING_HEAVY to "Heavy Single",
                    WarmupProtocol.QUICK_POTENTIATION to "Activation"
                ).forEach { (protocol, label) ->
                    GymPillTab(
                        text = label,
                        selected = selectedWarmupProtocol == protocol,
                        onClick = { selectedWarmupProtocol = protocol },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Warmup Sets List (flat tonal surface, zero hairline borders)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceHigh)
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                calculatedWarmupSets.forEachIndexed { idx, set ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(AmberWarm),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "W${idx + 1}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = GymInkOnLight,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                            Text(
                                text = "${set.percentageOfTarget}%",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        Text(
                            text = "${set.weightKg} кг × ${set.reps}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )

                        Text(
                            text = "${set.suggestedRestSeconds}с",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextMuted,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            GymPrimaryButton(
                text = "${strings.workoutAddSet} (${calculatedWarmupSets.size})",
                onClick = {
                    val sets = calculatedWarmupSets.map { it.weightKg to it.reps }
                    onApplyWarmupSets(sets)
                    onDismiss()
                },
                fillWidth = true
            )
        }
    }
}
