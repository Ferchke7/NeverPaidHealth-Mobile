package dev.neverpaid.health.core.designsystem

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.i18n.LocalDudaStrings
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
    val strings = LocalDudaStrings.current
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

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(10.dp, RoundedCornerShape(20.dp))
            .clickable { onExpand() },
        shape = RoundedCornerShape(20.dp),
        color = SurfaceWhite,
        border = BorderStroke(1.5.dp, BorderLilacStrong)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
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
                        .background(EmeraldSuccess)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = workout.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$formattedTime · ${workout.exercises.size} ${strings.dashboardExercisesShort} · $completedSets ${strings.dashboardSetsShort}",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }

            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(VioletButtonBrush)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = null,
                    tint = InkWhite,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = strings.commonToGym,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = InkWhite
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FloatingFocusTimerOverlay(
    state: FocusTimerState,
    isAlarmRinging: Boolean = false,
    onToggleRunning: () -> Unit,
    onReset: () -> Unit,
    onMinimize: (Boolean) -> Unit,
    onClose: () -> Unit,
    onSetMode: (String, String, Int) -> Unit,
    onSelectAlarmPreset: (String) -> Unit = {},
    onStopAlarm: () -> Unit = {},
    onCompleteSession: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (!state.isOpen) return

    val strings = LocalDudaStrings.current
    val displaySeconds = if (state.mode == "stopwatch") state.elapsedSeconds else state.secondsLeft
    val formatted = StrengthCalculator.formatTime(displaySeconds / 60, displaySeconds % 60)

    if (state.isMinimized) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp)
                .shadow(8.dp, RoundedCornerShape(18.dp))
                .clickable { onMinimize(false) },
            shape = RoundedCornerShape(18.dp),
            color = SurfaceWhite,
            border = BorderStroke(1.dp, BorderLilacStrong)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    GymIconBadge(icon = Icons.Default.Timer, size = 36.dp)
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.taskTitle.ifBlank { strings.focusSessionDefault },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = formatted,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Black,
                            color = VioletPrimary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onToggleRunning, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = VioletPrimary
                        )
                    }
                    if (isAlarmRinging) {
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CrimsonRed)
                                .clickable(onClick = onStopAlarm)
                                .padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = strings.focusStopAlarmBtn,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Black,
                                color = InkWhite
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    IconButton(onClick = onCompleteSession, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = strings.done,
                            tint = EmeraldSuccess
                        )
                    }
                    IconButton(onClick = { onMinimize(false) }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = strings.commonExpand,
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
        containerColor = SurfaceWhite,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 4.dp)
                    .size(width = 40.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(BorderLilacStrong)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    GymKicker(text = strings.todoPomodoroTitle)
                    Text(
                        text = state.taskTitle.ifBlank { strings.focusDeepWork },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    IconButton(onClick = { onMinimize(true) }) {
                        Icon(Icons.Default.KeyboardArrowDown, contentDescription = strings.commonMinimize, tint = TextSecondary)
                    }
                    IconButton(onClick = onClose) {
                        Icon(Icons.Default.Close, contentDescription = strings.commonClose, tint = TextSecondary)
                    }
                }
            }

            // Mode Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(RoyalWell)
                    .border(1.dp, BorderLilac, CircleShape)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(
                    Triple("pomodoro", strings.focusPomodoro, 25),
                    Triple("target_timer", strings.focusTimer, state.customMinutes),
                    Triple("stopwatch", strings.focusStopwatch, 0)
                ).forEach { (modeKey, label, mins) ->
                    val selected = state.mode == modeKey
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(CircleShape)
                            .background(if (selected) VioletPrimary else Color.Transparent)
                            .clickable { onSetMode(modeKey, "work", mins.coerceAtLeast(15)) }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (selected) InkWhite else TextSecondary
                        )
                    }
                }
            }

            if (state.mode == "pomodoro") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("work", strings.todoPomodoroFocus, 25),
                        Triple("short_break", strings.todoPomodoroShortBreak, 5),
                        Triple("long_break", "15m", 15)
                    ).forEach { (phaseKey, label, mins) ->
                        GymPillTab(
                            text = label,
                            selected = state.pomodoroPhase == phaseKey,
                            onClick = { onSetMode("pomodoro", phaseKey, mins) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else if (state.mode == "target_timer") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(15, 25, 45, 60, 90).forEach { mins ->
                        GymPillTab(
                            text = "${mins}m",
                            selected = state.customMinutes == mins,
                            onClick = { onSetMode("target_timer", "work", mins) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Circular Countdown Display
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.size(200.dp),
                    color = VioletPrimary,
                    trackColor = SurfaceLilac,
                    strokeWidth = 10.dp
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatted,
                        fontSize = 44.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = TextPrimary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "${strings.focusSessionsCount} ${state.pomodoroCount}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GymOutlineButton(
                    text = strings.todoPomodoroReset,
                    onClick = onReset,
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )

                Button(
                    onClick = onToggleRunning,
                    modifier = Modifier
                        .weight(1.4f)
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    elevation = ButtonDefaults.buttonElevation(2.dp, 0.dp, 0.dp, 0.dp, 0.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (state.isRunning) AmberWarm else VioletPrimary,
                        contentColor = InkWhite
                    )
                ) {
                    Icon(
                        imageVector = if (state.isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (state.isRunning) strings.todoPomodoroPause else strings.todoPomodoroStart,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp
                    )
                }
            }

            GymOutlineButton(
                text = strings.focusFinishClose,
                onClick = onCompleteSession,
                fillWidth = true,
                textColor = EmeraldDark,
                borderColor = EmeraldBorder
            )

            if (isAlarmRinging) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(CrimsonRed)
                        .clickable(onClick = onStopAlarm)
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.focusAlarmRingingLabel,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = InkWhite
                        )
                        Text(
                            text = strings.focusStopAlarmBtn,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Black,
                            color = InkWhite
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "radar" to strings.focusSoundRadar,
                    "bell" to strings.focusSoundBell,
                    "gong" to strings.focusSoundGong,
                    "siren" to strings.focusSoundSiren
                ).forEach { (presetId, presetLabel) ->
                    GymPillTab(
                        text = presetLabel,
                        selected = state.alarmPreset == presetId,
                        onClick = { onSelectAlarmPreset(presetId) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
