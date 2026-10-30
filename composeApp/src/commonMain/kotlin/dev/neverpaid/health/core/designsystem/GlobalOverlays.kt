package dev.neverpaid.health.core.designsystem

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.domain.model.FocusTimerState
import dev.neverpaid.health.domain.model.Workout
import kotlinx.coroutines.delay
import kotlinx.datetime.Clock

@Composable
fun FloatingActiveWorkoutPill(
    workout: Workout,
    onExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    var nowMs by remember { mutableLongStateOf(Clock.System.now().toEpochMilliseconds()) }
    LaunchedEffect(workout.id) {
        while (true) {
            delay(1000)
            nowMs = Clock.System.now().toEpochMilliseconds()
        }
    }

    val elapsedSeconds = ((nowMs - workout.startTimeEpochMs) / 1000L).coerceAtLeast(0L).toInt()
    val formattedTime = StrengthCalculator.formatTime(elapsedSeconds / 60, elapsedSeconds % 60)
    val completedSets = workout.exercises.sumOf { ex -> ex.sets.count { it.isCompleted } }

    AthleticCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
            .clickable { onExpand() },
        backgroundColor = DarkSurfaceVariant,
        borderColor = EmeraldNeon
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(EmeraldNeon)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$formattedTime • ${workout.exercises.size} упр. • $completedSets сет.",
                        fontSize = 11.sp,
                        color = EmeraldLight,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(EmeraldNeon)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            tint = Color.Black,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Открыть",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Black
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingFocusTimerOverlay(
    state: FocusTimerState,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onMinimize: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSetMode: (String, String, Int) -> Unit,
    onCompleteSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) return

    val displaySeconds = if (state.mode == "stopwatch") state.elapsedSeconds else state.secondsLeft
    val formatted = StrengthCalculator.formatTime(displaySeconds / 60, displaySeconds % 60)

    if (state.isMinimized) {
        AthleticCard(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 4.dp),
            backgroundColor = DarkSurfaceVariant,
            borderColor = IndigoElectric
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onMinimize(false) }
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = IndigoElectric,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.taskTitle.ifBlank { "Фокус-сессия" },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatted,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            color = EmeraldLight
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleRunning, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = EmeraldNeon
                        )
                    }
                    IconButton(onClick = onCompleteSession, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Завершить",
                            tint = EmeraldLight
                        )
                    }
                    IconButton(onClick = { onMinimize(false) }, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Развернуть",
                            tint = TextSecondary
                        )
                    }
                }
            }
        }
        return
    }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val progress = if (state.mode == "stopwatch") {
        1f
    } else {
        (state.secondsLeft.toFloat() / state.totalSeconds.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    }

    ModalBottomSheet(
        onDismissRequest = { onMinimize(true) },
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top bar: Minimize & Close
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "ФОКУС ТАЙМЕР",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = state.taskTitle.ifBlank { "Глубокая концентрация" },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = { onMinimize(true) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Свернуть", tint = TextSecondary)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Mode Tabs: Помидор / Таймер / Секундомер
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("pomodoro", "Помидор", 25),
                    Triple("timer", "Таймер", state.customMinutes),
                    Triple("stopwatch", "Секундомер", 0)
                ).forEach { (modeKey, label, mins) ->
                    val selected = state.mode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selected) EmeraldNeon else Color.Transparent)
                            .clickable { onSetMode(modeKey, "focus", mins.coerceAtLeast(15)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (selected) Color.Black else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (state.mode == "pomodoro") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("focus", "Фокус 25м", 25),
                        Triple("short_break", "Перерыв 5м", 5),
                        Triple("long_break", "Длинный 15м", 15)
                    ).forEach { (phaseKey, label, mins) ->
                        val selected = state.pomodoroPhase == phaseKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) EmeraldSubtle else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (selected) BorderHighlight else BorderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetMode("pomodoro", phaseKey, mins) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) EmeraldLight else TextSecondary
                            )
                        }
                    }
                }
            } else if (state.mode == "timer") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 25, 45, 60, 90).forEach { mins ->
                        val selected = state.customMinutes == mins
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (selected) EmeraldSubtle else DarkSurfaceVariant)
                                .border(
                                    1.dp,
                                    if (selected) BorderHighlight else BorderSubtle,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { onSetMode("timer", "focus", mins) }
                                .padding(vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${mins}м",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) EmeraldLight else TextSecondary
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            // Circular Countdown Display
            Box(
                modifier = Modifier.size(180.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(180.dp),
                    color = EmeraldNeon,
                    trackColor = DarkSurfaceHigh,
                    strokeWidth = 8.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatted,
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Помидоров: ${state.pomodoroCount}",
                        fontSize = 11.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onReset,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Сброс", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = onToggleRunning,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) AmberWarm else EmeraldNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isRunning) "Пауза" else "Старт",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedButton(
                onClick = onCompleteSession,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = EmeraldLight)
            ) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Завершить сессию и отметить задачу выполненной", fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
