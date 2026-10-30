package dev.neverpaid.health.features.coach

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.CyanElectric
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.ReadinessAssessment
import dev.neverpaid.health.domain.model.ReadinessTier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadinessSurveyModal(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSubmitAssessment: (ReadinessAssessment) -> Unit
) {
    if (!isOpen) return

    val strings = LocalDudaStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var sleepHours by remember { mutableFloatStateOf(7.5f) }
    var sleepQuality by remember { mutableIntStateOf(4) }
    var muscleSoreness by remember { mutableIntStateOf(2) }
    var cnsFatigue by remember { mutableIntStateOf(2) }
    var stressLevel by remember { mutableIntStateOf(2) }

    val todayDate = remember { PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd") }

    val tempAssessment = remember(sleepHours, sleepQuality, muscleSoreness, cnsFatigue, stressLevel) {
        ReadinessAssessment(
            id = PlatformBridge.generateUuid(),
            date = todayDate,
            sleepHours = sleepHours.toDouble(),
            sleepQuality = sleepQuality,
            muscleSoreness = muscleSoreness,
            cnsFatigue = cnsFatigue,
            stressLevel = stressLevel
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
            // Header
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
                        imageVector = Icons.Default.MonitorHeart,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = strings.readinessSurveyTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Live Computed Readiness Score Preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                border = BorderStroke(1.dp, when (tempAssessment.tier) {
                    ReadinessTier.PEAK_READINESS -> EmeraldNeon
                    ReadinessTier.NORMAL_READINESS -> CyanElectric
                    ReadinessTier.MILD_FATIGUE -> AmberWarm
                    ReadinessTier.CRITICAL_RECOVERY -> CrimsonRed
                })
            ) {
                Row(
                    modifier = Modifier.padding(14.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = strings.readinessScore,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "${tempAssessment.score} / 100",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = when (tempAssessment.tier) {
                                ReadinessTier.PEAK_READINESS -> EmeraldNeon
                                ReadinessTier.NORMAL_READINESS -> CyanElectric
                                ReadinessTier.MILD_FATIGUE -> AmberWarm
                                ReadinessTier.CRITICAL_RECOVERY -> CrimsonRed
                            }
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceElevated,
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = "Объём: ${tempAssessment.volumeModifierPercentage}%",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 1. Sleep Hours Slider
            Text(
                text = "Продолжительность сна: ${((sleepHours * 10).toInt() / 10.0)} ч",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Slider(
                value = sleepHours,
                onValueChange = { sleepHours = it },
                valueRange = 4f..11f,
                steps = 13,
                colors = SliderDefaults.colors(
                    thumbColor = EmeraldNeon,
                    activeTrackColor = EmeraldNeon,
                    inactiveTrackColor = DarkSurfaceElevated
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Sleep Quality Segmented (1-5)
            SurveyRatingRow(
                title = strings.sleepQuality,
                currentValue = sleepQuality,
                onValueChange = { sleepQuality = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 3. Muscle Soreness DOMS (1-5)
            SurveyRatingRow(
                title = strings.muscleSoreness,
                currentValue = muscleSoreness,
                onValueChange = { muscleSoreness = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. CNS Fatigue (1-5)
            SurveyRatingRow(
                title = strings.cnsFatigue,
                currentValue = cnsFatigue,
                onValueChange = { cnsFatigue = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 5. Psychological Stress (1-5)
            SurveyRatingRow(
                title = strings.stressLevel,
                currentValue = stressLevel,
                onValueChange = { stressLevel = it }
            )

            Spacer(modifier = Modifier.height(18.dp))

            FilledTonalButton(
                onClick = {
                    onSubmitAssessment(tempAssessment)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmeraldNeon,
                    contentColor = DarkBackground
                )
            ) {
                Text(
                    text = "Применить и настроить тренировку",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SurveyRatingRow(
    title: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(4.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            (1..5).forEachIndexed { index, value ->
                SegmentedButton(
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = 5),
                    onClick = { onValueChange(value) },
                    selected = currentValue == value,
                    colors = SegmentedButtonDefaults.colors(
                        activeContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                        activeContentColor = EmeraldNeon,
                        inactiveContainerColor = DarkSurface,
                        inactiveContentColor = TextSecondary
                    )
                ) {
                    Text("$value", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
