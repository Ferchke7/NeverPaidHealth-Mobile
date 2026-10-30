package dev.neverpaid.health.features.coach

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
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
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.platform.rememberImagePickerLauncher
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.SuggestedWorkoutPlan
import kotlinx.serialization.json.Json

private val workoutJsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

private fun extractEmbeddedWorkoutPlan(text: String): Pair<String, SuggestedWorkoutPlan?> {
    val startMarker = "```workout-json"
    val startIndex = text.indexOf(startMarker)
    if (startIndex == -1) return text to null
    val contentStart = startIndex + startMarker.length
    val endIndex = text.indexOf("```", contentStart)
    if (endIndex == -1) return text to null

    val jsonString = text.substring(contentStart, endIndex).trim()
    val cleanedText = (text.substring(0, startIndex) + text.substring(endIndex + 3)).trim()
    val parsed = runCatching {
        workoutJsonParser.decodeFromString<SuggestedWorkoutPlan>(jsonString)
    }.getOrNull()
    return cleanedText to parsed
}

@Composable
fun CoachRoute(
    viewModel: CoachViewModel,
    onNavigateToActiveWorkout: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            CoachUiEffect.NavigateToActiveWorkout -> onNavigateToActiveWorkout()
            is CoachUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        CoachScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun CoachScreen(
    state: CoachUiState,
    onAction: (CoachUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickPhoto = rememberImagePickerLauncher { base64, _ ->
        onAction(CoachUiAction.OnAttachPhoto(base64))
    }

    val quickPrompts = listOf(
        "Составь тренировку на сегодня с учётом восстановления",
        "Как пробить плато в жиме штанги лёжа?",
        "Рассчитай рацион на сушку без потери силы",
        "Оцени мою готовность ЦНС и перегрузку"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "AI Тренер duda.uz",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = "Индекс готовности ЦНС • Прогрессивная перегрузка • Генератор тренировок",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            IconButton(
                onClick = { onAction(CoachUiAction.OnClearChatHistory) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Очистить чат",
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // CNS & Progressive Overload Card
        AthleticCard(
            backgroundColor = DarkSurfaceVariant,
            borderColor = EmeraldDark
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(EmeraldSubtle),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "ГОТОВНОСТЬ ЦНС: ${state.insight.cnsScore}/100 (${state.insight.readinessLevel})",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldNeon
                            )
                            Text(
                                text = state.insight.progressiveOverloadTarget,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Quick action chips
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(quickPrompts) { prompt ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp))
                        .clickable { onAction(CoachUiAction.OnQuickPromptClick(prompt)) }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(text = prompt, fontSize = 11.sp, color = EmeraldLight, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Chat Message Feed
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (state.insight.suggestedWorkout != null) {
                item(key = "insight_suggested_workout") {
                    CoachSuggestedWorkoutCard(
                        plan = state.insight.suggestedWorkout!!,
                        onStartWorkout = { onAction(CoachUiAction.OnStartSuggestedWorkout(it)) },
                        onSaveRoutine = { onAction(CoachUiAction.OnSaveSuggestedRoutine(it)) }
                    )
                }
            }

            items(
                items = state.chatMessages,
                key = { it.id }
            ) { msg ->
                ChatMessageBubble(
                    message = msg,
                    onStartSuggestedWorkout = { onAction(CoachUiAction.OnStartSuggestedWorkout(it)) },
                    onSaveSuggestedRoutine = { onAction(CoachUiAction.OnSaveSuggestedRoutine(it)) }
                )
            }

            if (state.isSending) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = EmeraldNeon,
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "ИИ-Тренер анализирует ваши данные...", fontSize = 12.sp, color = TextMuted)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (state.attachedImageBase64 != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldSubtle)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📸 Фото прикреплено к сообщению", fontSize = 12.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                IconButton(onClick = { onAction(CoachUiAction.OnClearAttachedPhoto) }, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(6.dp))
        }

        // Input bar with Photo Attachment + Text + Send
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = pickPhoto,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DarkSurfaceVariant)
            ) {
                Icon(
                    imageVector = Icons.Default.CameraAlt,
                    contentDescription = "Прикрепить фото",
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = state.inputMessage,
                onValueChange = { onAction(CoachUiAction.OnInputMessageChanged(it)) },
                placeholder = { Text(text = "Спросите тренера или попросите сплит...", color = TextMuted, fontSize = 13.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(24.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                maxLines = 3
            )

            Spacer(modifier = Modifier.width(8.dp))

            val canSend = state.inputMessage.isNotBlank() || state.attachedImageBase64 != null
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (canSend) EmeraldNeon else DarkSurfaceHigh)
                    .clickable(enabled = canSend && !state.isSending) {
                        onAction(CoachUiAction.OnSendMessage)
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "Send",
                    tint = if (canSend) DarkBackground else TextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun ChatMessageBubble(
    message: ChatMessage,
    onStartSuggestedWorkout: (SuggestedWorkoutPlan) -> Unit,
    onSaveSuggestedRoutine: (SuggestedWorkoutPlan) -> Unit
) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bg = if (isUser) EmeraldDark else DarkSurfaceVariant

    val (cleanedText, embeddedWorkout) = remember(message.text) {
        if (isUser) message.text to null else extractEmbeddedWorkoutPlan(message.text)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        if (!isUser) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 4.dp, start = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ElectricBolt,
                    contentDescription = null,
                    tint = EmeraldNeon,
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "AI Тренер duda.uz",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
            }
        }

        if (cleanedText.isNotBlank()) {
            Box(
                modifier = Modifier
                    .clip(
                        RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = if (isUser) 16.dp else 4.dp,
                            bottomEnd = if (isUser) 4.dp else 16.dp
                        )
                    )
                    .background(bg)
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = cleanedText,
                    fontSize = 14.sp,
                    color = TextPrimary,
                    lineHeight = 20.sp
                )
            }
        }

        if (embeddedWorkout != null) {
            Spacer(modifier = Modifier.height(8.dp))
            CoachSuggestedWorkoutCard(
                plan = embeddedWorkout,
                onStartWorkout = onStartSuggestedWorkout,
                onSaveRoutine = onSaveSuggestedRoutine
            )
        }
    }
}

@Composable
private fun CoachSuggestedWorkoutCard(
    plan: SuggestedWorkoutPlan,
    onStartWorkout: (SuggestedWorkoutPlan) -> Unit,
    onSaveRoutine: (SuggestedWorkoutPlan) -> Unit
) {
    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderHighlight
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "ПЕРСОНАЛЬНЫЙ ПЛАН ОТ ИИ-ТРЕНЕРА",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = plan.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Black,
                color = TextPrimary
            )
            if (plan.rationale.isNotBlank()) {
                Text(
                    text = plan.rationale,
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            plan.exercises.forEachIndexed { idx, ex ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${idx + 1}. ${ex.exerciseName}",
                        fontSize = 12.sp,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${ex.sets} × ${ex.repsMin}–${ex.repsMax}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onSaveRoutine(plan) },
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("В шаблон", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = { onStartWorkout(plan) },
                    modifier = Modifier
                        .weight(1.3f)
                        .height(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = Color.Black
                    )
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Начать сейчас", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
