package dev.neverpaid.health.features.progress

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.domain.model.PersonalRecord

@Composable
fun ProgressRoute(
    viewModel: ProgressViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        ProgressScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun ProgressScreen(
    state: ProgressUiState,
    onAction: (ProgressUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Analytics & Benchmarks",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Volume trends, hypertrophy load & personal records",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        item(key = "volume_chart_card") {
            VolumeProgressionChartCard(volumeBars = state.weeklyVolumeBars)
        }

        item(key = "hypertrophy_card") {
            HypertrophyDistributionCard(muscleSets = state.muscleHypertrophySets)
        }

        item(key = "prs_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PERSONAL RECORDS HALL OF FAME",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
            }
        }

        items(
            items = state.personalRecords,
            key = { it.exerciseName }
        ) { pr ->
            PersonalRecordItemCard(pr = pr)
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VolumeProgressionChartCard(volumeBars: List<Pair<String, Double>>) {
    val maxVolume = (volumeBars.maxOfOrNull { it.second } ?: 1.0).coerceAtLeast(1000.0)

    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.ShowChart,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "WEEKLY VOLUME PROGRESSION (KG)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    text = "+18.2%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Bar chart representation
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.Bottom
            ) {
                volumeBars.forEach { (label, vol) ->
                    val ratio = (vol / maxVolume).toFloat().coerceIn(0.1f, 1.0f)
                    val isCurrent = label == "Current"
                    val barColor = if (isCurrent) EmeraldNeon else DarkSurfaceHigh

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxHeight(),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = String.format("%.0fk", vol / 1000),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isCurrent) EmeraldNeon else TextMuted
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .width(28.dp)
                                .fillMaxHeight(ratio)
                                .clip(RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp))
                                .background(barColor)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HypertrophyDistributionCard(muscleSets: Map<String, Int>) {
    AthleticCard(backgroundColor = DarkSurface) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "HYPERTROPHY VOLUME (SETS / WEEK)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(12.dp))

            muscleSets.forEach { (muscle, sets) ->
                val progress = (sets / 20f).coerceIn(0f, 1f)
                val statusText = when {
                    sets >= 12 -> "Optimal"
                    sets >= 8 -> "Maintenance"
                    else -> "Under-trained"
                }
                val statusColor = when {
                    sets >= 12 -> EmeraldNeon
                    sets >= 8 -> IndigoElectric
                    else -> AmberWarm
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = muscle, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary, modifier = Modifier.width(90.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .weight(1f)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = statusColor,
                        trackColor = DarkSurfaceHigh
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "$sets sets", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = statusColor, modifier = Modifier.width(50.dp))
                }
            }
        }
    }
}

@Composable
private fun PersonalRecordItemCard(pr: PersonalRecord) {
    AthleticCard(backgroundColor = DarkSurfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(AmberWarm.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = AmberWarm,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = pr.exerciseName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${pr.weightKg} kg × ${pr.reps} reps",
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "E1RM",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Text(
                    text = "${pr.e1rmKg} kg",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }
        }
    }
}
