package dev.neverpaid.health.features.coach

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymIconBadge
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.InfoAccent
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

    val tierColor = when (tempAssessment.tier) {
        ReadinessTier.PEAK_READINESS -> AccentLime
        ReadinessTier.NORMAL_READINESS -> InfoAccent
        ReadinessTier.MILD_FATIGUE -> AmberWarm
        ReadinessTier.CRITICAL_RECOVERY -> CrimsonRed
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceElevated)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
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
                    GymIconBadge(icon = Icons.Default.MonitorHeart, size = 42.dp)
                    Text(
                        text = strings.readinessSurveyTitle,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = TextMuted)
                }
            }

            // Live Score Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkSurfaceHigh)
                    .padding(18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = strings.readinessScore.uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = TextSecondary
                    )
                    Text(
                        text = "${tempAssessment.score} / 100",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = tierColor,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "Объём: ${tempAssessment.volumeModifierPercentage}%",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = tierColor
                    )
                }
            }

            // 1. Sleep Hours Slider
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Сон",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${((sleepHours * 10).toInt() / 10.0)} ч",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentLime
                    )
                }
                Slider(
                    value = sleepHours,
                    onValueChange = { sleepHours = it },
                    valueRange = 4f..11f,
                    steps = 13,
                    colors = SliderDefaults.colors(
                        thumbColor = AccentLime,
                        activeTrackColor = AccentLime,
                        inactiveTrackColor = DarkSurfaceHigh
                    )
                )
            }

            SurveyRatingRow(
                title = strings.sleepQuality,
                currentValue = sleepQuality,
                onValueChange = { sleepQuality = it }
            )
            SurveyRatingRow(
                title = strings.muscleSoreness,
                currentValue = muscleSoreness,
                onValueChange = { muscleSoreness = it }
            )
            SurveyRatingRow(
                title = strings.cnsFatigue,
                currentValue = cnsFatigue,
                onValueChange = { cnsFatigue = it }
            )
            SurveyRatingRow(
                title = strings.stressLevel,
                currentValue = stressLevel,
                onValueChange = { stressLevel = it }
            )

            Spacer(modifier = Modifier.height(4.dp))

            GymPrimaryButton(
                text = "Применить к тренировке",
                onClick = {
                    onSubmitAssessment(tempAssessment)
                    onDismiss()
                },
                fillWidth = true
            )
        }
    }
}

@Composable
private fun SurveyRatingRow(
    title: String,
    currentValue: Int,
    onValueChange: (Int) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = title,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            (1..5).forEach { value ->
                val selected = currentValue == value
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (selected) AccentLime else DarkSurfaceHigh)
                        .clickable { onValueChange(value) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$value",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = if (selected) GymInkOnLight else TextSecondary
                    )
                }
            }
        }
    }
}
