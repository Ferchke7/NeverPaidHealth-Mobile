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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
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
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.HistoricalSetRecord
import dev.neverpaid.health.core.math.SetHistoryCalculator
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.platform.PlatformBridge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExerciseSetHistoryBottomSheet(
    exerciseName: String,
    initialSetNumber: Int? = null,
    history: List<HistoricalSetRecord>,
    isKgUnit: Boolean = true,
    onApplyValues: (Double, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val strings = LocalDudaStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedSetFilter by remember(initialSetNumber) {
        mutableStateOf<Int?>(initialSetNumber)
    }

    // Sort by: "best" (1RM) or "date" (most recent)
    var sortBy by remember { mutableStateOf("best") }
    var appliedKey by remember { mutableStateOf<String?>(null) }

    val prSummary = remember(history) {
        SetHistoryCalculator.getAllTimePR(history)
    }

    val availableSetNumbers = remember(history) {
        history.map { it.setNumber }.distinct().sorted()
    }

    val displayRecords = remember(history, selectedSetFilter, sortBy) {
        val filtered = if (selectedSetFilter == null) {
            history
        } else {
            history.filter { it.setNumber == selectedSetFilter }
        }

        if (sortBy == "best") {
            filtered.sortedWith { a, b ->
                val scoreA = if (a.weightKg > 0) a.e1rmKg * 1000 + a.reps else a.reps.toDouble()
                val scoreB = if (b.weightKg > 0) b.e1rmKg * 1000 + b.reps else b.reps.toDouble()
                val diff = scoreB.compareTo(scoreA)
                if (diff != 0) return@sortedWith diff
                if (b.weightKg != a.weightKg) return@sortedWith b.weightKg.compareTo(a.weightKg)
                if (b.reps != a.reps) return@sortedWith b.reps.compareTo(a.reps)
                b.dateEpochMs.compareTo(a.dateEpochMs)
            }
        } else {
            filtered.sortedByDescending { it.dateEpochMs }
        }
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
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
                    modifier = Modifier.weight(1f)
                ) {
                    GymIconBadge(
                        icon = Icons.Default.FitnessCenter,
                        tint = AccentLime,
                        background = AccentLimeSubtle,
                        size = 42.dp
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = exerciseName,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = strings.historyModalTitle,
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
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
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Flat tonal PR Summary Strip (zero borders)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Card 1: All-Time PR 1RM
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceHigh)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = AmberWarm,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.allTimePR,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberWarm,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (prSummary.best1RMKg > 0) {
                            StrengthCalculator.formatWeight(prSummary.best1RMKg, isKgUnit)
                        } else {
                            "—"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(text = "1RM", fontSize = 10.sp, color = TextMuted)
                }

                // Card 2: Best Weight x Reps
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceHigh)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AccentLime,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.bestWeight,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLime,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (prSummary.bestWeightKg > 0) {
                            "${prSummary.bestWeightKg.toInt()}×${prSummary.bestReps}"
                        } else {
                            "—"
                        },
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentLime,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(text = "max reps", fontSize = 10.sp, color = TextMuted)
                }

                // Card 3: Total Completed Sets
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceHigh)
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = strings.totalCompleted,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            maxLines = 1
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${prSummary.totalSets}",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(text = "подходов", fontSize = 10.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Filter Tabs (All Sets / Set 1 / Set 2...)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    GymPillTab(
                        text = "${strings.allSetsTab} (${history.size})",
                        selected = selectedSetFilter == null,
                        onClick = { selectedSetFilter = null }
                    )
                }

                items(availableSetNumbers) { setNum ->
                    val count = history.count { it.setNumber == setNum }
                    GymPillTab(
                        text = "Сет #$setNum ($count)",
                        selected = selectedSetFilter == setNum,
                        onClick = { selectedSetFilter = setNum }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Sort Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayRecords.size} записей",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(GymWell)
                        .padding(3.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (sortBy == "best") AccentLime else Color.Transparent)
                            .clickable { sortBy = "best" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.sortByBest,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sortBy == "best") GymInkOnLight else TextSecondary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(if (sortBy == "date") AccentLime else Color.Transparent)
                            .clickable { sortBy = "date" }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = strings.sortByDate,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sortBy == "date") GymInkOnLight else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // List of Records
            if (displayRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceHigh),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noHistoryFound,
                        fontSize = 13.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayRecords) { record ->
                        val key = "${record.workoutId}-${record.setNumber}-${record.dateEpochMs}"
                        val isApplied = appliedKey == key
                        val rank = displayRecords.indexOf(record)
                        val isTopBest = sortBy == "best" && rank == 0
                        val medal = when {
                            sortBy == "best" && rank == 0 -> "🥇"
                            sortBy == "best" && rank == 1 -> "🥈"
                            sortBy == "best" && rank == 2 -> "🥉"
                            else -> "#${rank + 1}"
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (isTopBest) AccentLimeSubtle else DarkSurfaceHigh)
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left: Medal + Weight x Reps
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = medal,
                                    fontSize = if (medal.startsWith("#")) 12.sp else 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTopBest) AccentLime else TextSecondary,
                                    modifier = Modifier.width(30.dp),
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )

                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "${StrengthCalculator.formatWeight(record.weightKg, isKgUnit)} × ${record.reps}",
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Black,
                                            color = TextPrimary,
                                            style = TextStyle(fontFeatureSettings = "tnum")
                                        )
                                        if (record.e1rmKg > 0) {
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(CircleShape)
                                                    .background(GymWell)
                                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                            ) {
                                                Text(
                                                    text = "1RM ${StrengthCalculator.formatWeight(record.e1rmKg, isKgUnit)}",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = AccentLime,
                                                    style = TextStyle(fontFeatureSettings = "tnum")
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Сет #${record.setNumber}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextSecondary
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.CalendarToday,
                                            contentDescription = null,
                                            tint = TextMuted,
                                            modifier = Modifier.size(11.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = PlatformBridge.formatDate(record.dateEpochMs, "dd MMM yyyy"),
                                            fontSize = 11.sp,
                                            color = TextMuted
                                        )
                                        if (record.workoutName.isNotBlank()) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "· ${record.workoutName}",
                                                fontSize = 11.sp,
                                                color = TextMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(10.dp))

                            // Right: Apply button
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isApplied) AccentLime else GymWell)
                                    .clickable {
                                        appliedKey = key
                                        onApplyValues(record.weightKg, record.reps)
                                        onDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isApplied) Icons.Default.Check else Icons.Default.ArrowDownward,
                                        contentDescription = strings.applyRecord,
                                        tint = if (isApplied) GymInkOnLight else AccentLime,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (isApplied) strings.applied else strings.applyRecord,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isApplied) GymInkOnLight else TextPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            GymOutlineButton(
                text = strings.done,
                onClick = onDismiss,
                fillWidth = true
            )
        }
    }
}
