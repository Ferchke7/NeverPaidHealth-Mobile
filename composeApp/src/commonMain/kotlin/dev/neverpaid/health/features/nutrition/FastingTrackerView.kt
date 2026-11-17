package dev.neverpaid.health.features.nutrition

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymIconBadge
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.InfoAccent
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.FastingProtocol
import dev.neverpaid.health.domain.model.FastingSession
import dev.neverpaid.health.domain.model.FastingStage

/**
 * Intermittent Fasting widget tracking cellular autophagy,
 * ketosis, and fasting progress with stage indicators.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FastingTrackerView(
    activeSession: FastingSession?,
    onStartFast: (FastingProtocol) -> Unit,
    onEndFast: () -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    var selectedProtocol by remember { mutableStateOf(FastingProtocol.LEAN_GAINS_16_8) }

    val currentNow = remember { PlatformBridge.currentTimeMillis() }

    val isFasting = activeSession != null && !activeSession.isCompleted
    val elapsedHours = activeSession?.elapsedHours(currentNow) ?: 0.0
    val progress = activeSession?.progressFraction(currentNow) ?: 0f
    val currentStage = activeSession?.currentStage(currentNow) ?: FastingStage.ANABOLIC_DIGESTION

    val animProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "fastingProgress"
    )

    GymCard(modifier = modifier) {
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
                    icon = Icons.Default.HourglassBottom,
                    tint = if (isFasting) AccentLime else AmberWarm,
                    background = if (isFasting) AccentLimeSubtle else AmberWarm.copy(alpha = 0.16f),
                    size = 40.dp
                )
                Text(
                    text = strings.intermittentFasting,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.4).sp,
                    color = TextPrimary
                )
            }

            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(if (isFasting) AccentLimeSubtle else DarkSurfaceHigh)
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = if (isFasting) strings.fastingStatusFast else strings.fastingStatusEating,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isFasting) AccentLime else TextSecondary
                )
            }
        }

        if (!isFasting) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Протокол",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        FastingProtocol.LEAN_GAINS_16_8,
                        FastingProtocol.WARRIOR_18_6,
                        FastingProtocol.OMAD_20_4
                    ).forEach { proto ->
                        GymPillTab(
                            text = proto.title,
                            selected = selectedProtocol == proto,
                            onClick = { selectedProtocol = proto }
                        )
                    }
                }
            }

            GymPrimaryButton(
                text = strings.startFastBtn,
                onClick = { onStartFast(selectedProtocol) },
                fillWidth = true
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurfaceHigh)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier.size(104.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        progress = { animProgress },
                        modifier = Modifier.fillMaxSize(),
                        color = AccentLime,
                        trackColor = DarkSurface,
                        strokeWidth = 10.dp,
                        strokeCap = StrokeCap.Round
                    )
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${(animProgress * 100).toInt()}%",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Black,
                            color = TextPrimary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "${elapsedHours.toInt()} ч",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLime,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Протокол ${activeSession?.protocol?.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )

                    val stageTitle = when (strings.languageCode.lowercase()) {
                        "uz" -> currentStage.titleUz
                        "en" -> currentStage.titleEn
                        else -> currentStage.titleRu
                    }

                    Text(
                        text = stageTitle,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (elapsedHours >= 16.0) InfoAccent else AccentLime
                    )

                    Text(
                        text = "${strings.hoursRemaining}: ${(activeSession?.remainingHours(currentNow) ?: 0.0).toInt()} ч",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            GymOutlineButton(
                text = strings.endFastBtn,
                onClick = onEndFast,
                fillWidth = true,
                textColor = AmberWarm
            )
        }
    }
}
