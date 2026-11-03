package dev.neverpaid.health.features.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.HistoricalSetRecord
import dev.neverpaid.health.core.math.SetHistoryCalculator
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
        dragHandle = null
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
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FitnessCenter,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = exerciseName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = strings.historyModalTitle,
                            fontSize = 11.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // PR Summary Strip
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkBackground)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(14.dp))
                    .padding(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Card 1: All-Time PR 1RM
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFFFFB300), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = strings.allTimePR, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFFB300))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (prSummary.best1RMKg > 0) "${prSummary.best1RMKg.toInt()} кг" else "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFFFFD54F)
                    )
                    Text(text = "1RM", fontSize = 9.sp, color = TextMuted)
                }

                // Card 2: Best Weight x Reps
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.LocalFireDepartment, contentDescription = null, tint = EmeraldNeon, modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = strings.bestWeight, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (prSummary.bestWeightKg > 0) "${prSummary.bestWeightKg.toInt()}×${prSummary.bestReps}" else "—",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldLight
                    )
                    Text(text = "max reps", fontSize = 9.sp, color = TextMuted)
                }

                // Card 3: Total Completed Sets
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(vertical = 8.dp, horizontal = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color(0xFF64B5F6), modifier = Modifier.size(12.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(text = strings.totalCompleted, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64B5F6))
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${prSummary.totalSets}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF90CAF9)
                    )
                    Text(text = "подходов", fontSize = 9.sp, color = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Filter Tabs (All Sets / Set 1 / Set 2...)
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedSetFilter == null,
                        onClick = { selectedSetFilter = null },
                        label = { Text("${strings.allSetsTab} (${history.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceHigh,
                            labelColor = TextSecondary
                        )
                    )
                }

                items(availableSetNumbers) { setNum ->
                    val count = history.count { it.setNumber == setNum }
                    FilterChip(
                        selected = selectedSetFilter == setNum,
                        onClick = { selectedSetFilter = setNum },
                        label = { Text("Сет #$setNum ($count)", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceHigh,
                            labelColor = TextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort Selector Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${displayRecords.size} записей",
                    fontSize = 11.sp,
                    color = TextMuted
                )

                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(DarkBackground)
                        .padding(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sortBy == "best") DarkSurfaceHigh else Color.Transparent)
                            .clickable { sortBy = "best" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strings.sortByBest,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sortBy == "best") EmeraldLight else TextMuted
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (sortBy == "date") DarkSurfaceHigh else Color.Transparent)
                            .clickable { sortBy = "date" }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = strings.sortByDate,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (sortBy == "date") EmeraldLight else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // List of Records
            if (displayRecords.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(160.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = strings.noHistoryFound,
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(displayRecords) { record ->
                        val key = "${record.workoutId}-${record.setNumber}-${record.dateEpochMs}"
                        val isApplied = appliedKey == key
                        val rank = displayRecords.indexOf(record)
                        val medal = when {
                            sortBy == "best" && rank == 0 -> "🥇"
                            sortBy == "best" && rank == 1 -> "🥈"
                            sortBy == "best" && rank == 2 -> "🥉"
                            else -> "#${rank + 1}"
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = DarkSurfaceVariant,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (rank == 0 && sortBy == "best") Color(0xFFFFB300).copy(alpha = 0.4f) else BorderSubtle
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
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
                                        fontSize = if (medal.startsWith("#")) 11.sp else 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary,
                                        modifier = Modifier.width(28.dp)
                                    )

                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "${record.weightKg.toInt()} кг × ${record.reps}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Black,
                                                color = TextPrimary
                                            )
                                            if (record.e1rmKg > 0) {
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(4.dp))
                                                        .background(EmeraldNeon.copy(alpha = 0.12f))
                                                        .padding(horizontal = 4.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = "1RM: ${record.e1rmKg.toInt()} кг",
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = EmeraldLight
                                                    )
                                                }
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(2.dp))

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Text(
                                                text = "Сет #${record.setNumber}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextSecondary
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Text(text = "•", fontSize = 10.sp, color = TextMuted)
                                            Spacer(modifier = Modifier.width(4.dp))
                                            Icon(
                                                imageVector = Icons.Default.CalendarToday,
                                                contentDescription = null,
                                                tint = TextMuted,
                                                modifier = Modifier.size(10.dp)
                                            )
                                            Spacer(modifier = Modifier.width(3.dp))
                                            Text(
                                                text = PlatformBridge.formatDate(record.dateEpochMs, "dd MMM yyyy"),
                                                fontSize = 10.sp,
                                                color = TextMuted
                                            )
                                            if (record.workoutName.isNotBlank()) {
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(text = "•", fontSize = 10.sp, color = TextMuted)
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = record.workoutName,
                                                    fontSize = 10.sp,
                                                    color = TextMuted,
                                                    maxLines = 1
                                                )
                                            }
                                        }
                                    }
                                }

                                // Right: Apply button
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isApplied) EmeraldNeon else DarkSurfaceHigh)
                                        .clickable {
                                            appliedKey = key
                                            onApplyValues(record.weightKg, record.reps)
                                            onDismiss()
                                        }
                                        .padding(horizontal = 10.dp, vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = if (isApplied) Icons.Default.Check else Icons.Default.ArrowDownward,
                                            contentDescription = strings.applyRecord,
                                            tint = if (isApplied) DarkBackground else EmeraldNeon,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = if (isApplied) strings.applied else strings.applyRecord,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isApplied) DarkBackground else TextPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text(text = strings.done, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            }
        }
    }
}
