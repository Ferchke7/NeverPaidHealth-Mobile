package dev.neverpaid.health.features.todo

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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.DudaTopHeader
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary

data class DayItem(
    val dayName: String,
    val dayNumber: String,
    val hasWorkout: Boolean,
    val splitTitle: String
)

data class AthleteCheckItem(
    val id: String,
    val title: String,
    val target: String,
    var isDone: Boolean
)

@Composable
fun TodoScreen(
    onStartWorkout: (String) -> Unit,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val days = remember {
        listOf(
            DayItem("ПН", "26", true, "Push Day (Hypertrophy)"),
            DayItem("ВТ", "27", false, "Отдых и восстановление"),
            DayItem("СР", "28", true, "Pull Day (Hypertrophy)"),
            DayItem("ЧТ", "29", false, "Кардио и мобильность"),
            DayItem("ПТ", "30", true, "Leg Day (Squat Focus)"),
            DayItem("СБ", "31", false, "Сауна и растяжка"),
            DayItem("ВС", "1", true, "Full Body Power")
        )
    }

    var selectedDayIndex by remember { mutableStateOf(0) }
    val currentDay = days[selectedDayIndex]

    val tasks = remember {
        mutableStateListOf(
            AthleteCheckItem("1", "Силовая тренировка по сплиту", "Push Day", true),
            AthleteCheckItem("2", "Дневная норма белка", "180 грамм", true),
            AthleteCheckItem("3", "Водный баланс", "3.0 литра", false),
            AthleteCheckItem("4", "Качественный сон", "8 часов", false),
            AthleteCheckItem("5", "Дневная активность", "10 000 шагов", true)
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            DudaTopHeader(
                streakDays = 4,
                onProfileClick = onNavigateToProfile,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Text(
                text = "ПЛАН И КАЛЕНДАРЬ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight,
                letterSpacing = 1.5.sp
            )
            Text(
                text = "Расписание тренировок",
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
        }

        // 7-day interactive strip
        item(key = "calendar_strip") {
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(days) { index, day ->
                    val isSelected = index == selectedDayIndex
                    val bg = if (isSelected) EmeraldSubtle else DarkSurfaceVariant
                    val border = if (isSelected) BorderHighlight else BorderSubtle

                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .background(bg)
                            .border(width = 1.dp, color = border, shape = RoundedCornerShape(14.dp))
                            .clickable { selectedDayIndex = index }
                            .padding(vertical = 10.dp, horizontal = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = day.dayName,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) EmeraldLight else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = day.dayNumber,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Black,
                            color = if (isSelected) TextPrimary else TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(if (day.hasWorkout) EmeraldNeon else DarkSurfaceHigh)
                        )
                    }
                }
            }
        }

        // Active day workout card
        item(key = "day_card") {
            AthleticCard(
                backgroundColor = DarkSurfaceVariant,
                borderColor = if (currentDay.hasWorkout) BorderHighlight else BorderSubtle
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (currentDay.hasWorkout) EmeraldSubtle else DarkSurfaceHigh),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (currentDay.hasWorkout) Icons.Default.FitnessCenter else Icons.Default.CalendarToday,
                                    contentDescription = null,
                                    tint = if (currentDay.hasWorkout) EmeraldLight else TextMuted,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (currentDay.hasWorkout) "СИЛОВАЯ ТРЕНИРОВКА" else "ДЕНЬ ВОССТАНОВЛЕНИЯ",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (currentDay.hasWorkout) EmeraldLight else TextMuted,
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = currentDay.splitTitle,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                            }
                        }
                    }

                    if (currentDay.hasWorkout) {
                        Spacer(modifier = Modifier.height(14.dp))
                        PrimaryAthleticButton(
                            text = "Начать тренировку дня",
                            onClick = { onStartWorkout(currentDay.splitTitle) },
                            icon = Icons.Default.PlayArrow
                        )
                    }
                }
            }
        }

        // Daily Checklist Section
        item(key = "checklist_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ЕЖЕДНЕВНЫЙ ЧЕК-ЛИСТ АТЛЕТА",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                val completed = tasks.count { it.isDone }
                Text(
                    text = "$completed из ${tasks.size} выполнено",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = EmeraldLight
                )
            }
        }

        items(tasks.size, key = { tasks[it].id }) { index ->
            val task = tasks[index]
            AthleticCard(
                backgroundColor = DarkSurface,
                borderColor = if (task.isDone) BorderHighlight else BorderSubtle
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            tasks[index] = task.copy(isDone = !task.isDone)
                        }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (task.isDone) EmeraldNeon else DarkSurfaceHigh)
                                .border(
                                    width = 1.dp,
                                    color = if (task.isDone) EmeraldNeon else BorderSubtle,
                                    shape = RoundedCornerShape(6.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (task.isDone) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Done",
                                    tint = androidx.compose.ui.graphics.Color.Black,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = task.title,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (task.isDone) TextPrimary else TextSecondary
                            )
                            Text(
                                text = task.target,
                                fontSize = 12.sp,
                                color = TextMuted
                            )
                        }
                    }
                }
            }
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
