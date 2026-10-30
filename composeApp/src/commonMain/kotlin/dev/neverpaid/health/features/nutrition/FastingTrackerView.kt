package dev.neverpaid.health.features.nutrition

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CyanElectric
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.FastingProtocol
import dev.neverpaid.health.domain.model.FastingSession
import dev.neverpaid.health.domain.model.FastingStage

/**
 * High-performance Intermittent Fasting widget tracking cellular autophagy,
 * ketosis, and fasting progress with scientific stage indicators.
 */
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

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                        imageVector = Icons.Default.HourglassBottom,
                        contentDescription = null,
                        tint = if (isFasting) EmeraldNeon else AmberWarm,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.intermittentFasting,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (isFasting) EmeraldNeon.copy(alpha = 0.15f) else DarkSurfaceElevated,
                    border = BorderStroke(1.dp, if (isFasting) EmeraldNeon.copy(alpha = 0.4f) else BorderSubtle)
                ) {
                    Text(
                        text = if (isFasting) strings.fastingStatusFast else strings.fastingStatusEating,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isFasting) EmeraldNeon else TextSecondary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            if (!isFasting) {
                // Protocol Selector
                Text(
                    text = "Выберите протокол:",
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        FastingProtocol.LEAN_GAINS_16_8,
                        FastingProtocol.WARRIOR_18_6,
                        FastingProtocol.OMAD_20_4
                    ).forEach { proto ->
                        FilterChip(
                            selected = selectedProtocol == proto,
                            onClick = { selectedProtocol = proto },
                            label = { Text(proto.title, style = MaterialTheme.typography.labelSmall) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldNeon,
                                containerColor = DarkSurfaceElevated,
                                labelColor = TextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selectedProtocol == proto,
                                borderColor = BorderSubtle,
                                selectedBorderColor = EmeraldNeon
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FilledTonalButton(
                    onClick = { onStartFast(selectedProtocol) },
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = EmeraldNeon,
                        contentColor = DarkBackground
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.startFastBtn, fontWeight = FontWeight.Bold)
                }
            } else {
                // Active Fasting Tracker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Gauge
                    Box(
                        modifier = Modifier.size(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { animProgress },
                            modifier = Modifier.fillMaxSize(),
                            color = EmeraldNeon,
                            trackColor = DarkSurfaceElevated,
                            strokeWidth = 8.dp,
                            strokeCap = StrokeCap.Round
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(animProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${elapsedHours.toInt()} ч",
                                style = MaterialTheme.typography.labelSmall,
                                color = EmeraldNeon
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Status and Stage details
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Протокол ${activeSession?.protocol?.title}",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )

                        val stageTitle = when (strings.languageCode.lowercase()) {
                            "uz" -> currentStage.titleUz
                            "en" -> currentStage.titleEn
                            else -> currentStage.titleRu
                        }

                        Text(
                            text = stageTitle,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (elapsedHours >= 16.0) CyanElectric else EmeraldNeon
                        )

                        Text(
                            text = "${strings.hoursRemaining}: ${(activeSession?.remainingHours(currentNow) ?: 0.0).toInt()} ч",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                FilledTonalButton(
                    onClick = onEndFast,
                    modifier = Modifier.fillMaxWidth().height(42.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = DarkSurfaceElevated,
                        contentColor = AmberWarm
                    )
                ) {
                    Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(strings.endFastBtn, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
