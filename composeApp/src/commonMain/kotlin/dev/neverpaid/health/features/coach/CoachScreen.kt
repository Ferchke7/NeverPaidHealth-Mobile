package dev.neverpaid.health.features.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymKicker
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymStatTile
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.rememberBase64ImageBitmap
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.platform.rememberImagePickerLauncher
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.SuggestedWorkoutPlan
import kotlinx.serialization.json.Json
import kotlin.math.roundToInt

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

    CoachScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CoachScreen(
    state: CoachUiState,
    onAction: (CoachUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = strings.languageCode
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var isReadinessSurveyOpen by remember { mutableStateOf(false) }

    if (isReadinessSurveyOpen) {
        ReadinessSurveyModal(
            isOpen = true,
            onDismiss = { isReadinessSurveyOpen = false },
            onSubmitAssessment = { assessment ->
                val promptText = "Результаты утреннего опроса готовности ЦНС: Индекс ${assessment.score}/100 (${assessment.tier.name}), Сон: ${assessment.sleepHours}ч (качество ${assessment.sleepQuality}/5), Крепатура: ${assessment.muscleSoreness}/5, Стресс: ${assessment.stressLevel}/5. Рекомендованный объём: ${assessment.volumeModifierPercentage}%. Скорректируй сегодняшнюю тренировку."
                onAction(CoachUiAction.OnInputMessageChanged(promptText))
                onAction(CoachUiAction.OnSendMessage)
                isReadinessSurveyOpen = false
            }
        )
    }

    val pickPhoto = rememberImagePickerLauncher { base64, _ ->
        onAction(CoachUiAction.OnAttachPhoto(base64))
    }

    val quickPrompts = listOf(
        when (lang) {
            "uz" -> "Tiklanish"
            "en" -> "Recovery"
            else -> "Восстановление"
        } to when (lang) {
            "uz" -> "Tiklanish va MNS holatimni baholang"
            "en" -> "Assess my recovery and CNS readiness"
            else -> "Оцени моё восстановление и готовность ЦНС"
        },
        when (lang) {
            "uz" -> "Plato"
            "en" -> "Plateau"
            else -> "Пробить плато"
        } to when (lang) {
            "uz" -> "Shtanga bosishda platoni qanday yengish mumkin?"
            "en" -> "How do I break through a bench press plateau?"
            else -> "Как преодолеть плато в жиме штанги лёжа?"
        },
        when (lang) {
            "uz" -> "Bugungi reja"
            "en" -> "Today plan"
            else -> "Тренировка дня"
        } to when (lang) {
            "uz" -> "Bugungi tiklanish asosida mashg'ulot tuzing"
            "en" -> "Build today's workout based on my recovery"
            else -> "Составь тренировку на сегодня с учётом восстановления"
        },
        when (lang) {
            "uz" -> "Ratsion"
            "en" -> "Cut macros"
            else -> "Сушка без потерь"
        } to when (lang) {
            "uz" -> "Kuch yo'qotmasdan quritish uchun ratsion hisoblang"
            "en" -> "Calculate cutting macros without losing strength"
            else -> "Рассчитай рацион на сушку без потери силы"
        }
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header", contentType = "header") {
            GymPageHeader(title = strings.navCoach)
        }

        if (state.isLoading && state.chatMessages.isEmpty()) {
            item(key = "loading_card", contentType = "loading") {
                GymLoadingCard(
                    title = strings.stateLoadingTitle,
                    hint = strings.stateLoadingHint
                )
            }
        } else {
            // 1. CNS Readiness Hero + Weekly Load
            item(key = "readiness_card", contentType = "readiness") {
                val weeklyTons = ((state.insight.weeklyVolumeKg / 1000.0) * 10.0).roundToInt() / 10.0
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                GymKicker(text = state.insight.readinessLevel)
                                Text(
                                    text = when (lang) {
                                        "uz" -> "CNS tayyorgarlik indeksi"
                                        "en" -> "CNS Readiness"
                                        else -> "Готовность ЦНС"
                                    },
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                            }
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${state.insight.cnsScore}",
                                    fontSize = 40.sp,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = (-1).sp,
                                    color = AccentLime,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                                Text(
                                    text = "/100",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 6.dp, start = 2.dp)
                                )
                            }
                        }

                        GymProgressBar(
                            progress = (state.insight.cnsScore / 100f).coerceIn(0f, 1f),
                            heightDp = 8,
                            color = AccentLime
                        )

                        Text(
                            text = state.insight.statusMessage,
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            color = TextSecondary
                        )

                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Ertalabki MNS so'rovini o'tish"
                                "en" -> "Run CNS Readiness Check-in"
                                else -> "Пройти чек-ап готовности ЦНС"
                            },
                            onClick = { isReadinessSurveyOpen = true },
                            fillWidth = true
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymStatTile(
                            label = when (lang) { "uz" -> "Haftalik hajm"; "en" -> "Weekly volume"; else -> "За неделю" },
                            value = "$weeklyTons",
                            unit = "т (${state.insight.weeklyWorkoutsCount} тр.)",
                            valueColor = AccentLime,
                            modifier = Modifier.weight(1f)
                        )
                        GymStatTile(
                            label = when (lang) { "uz" -> "Dam olish"; "en" -> "Rest days"; else -> "Дней отдыха" },
                            value = "${state.insight.daysSinceLastTrain}",
                            unit = when (lang) { "uz" -> "kun"; "en" -> "d"; else -> "дн." },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // 2. Progressive Overload Target
            item(key = "overload_card", contentType = "overload") {
                val topTarget = state.insight.overloadTargets.firstOrNull()
                GymCard {
                    GymKicker(
                        text = when (lang) {
                            "uz" -> "KEYINGI REKORD"
                            "en" -> "OVERLOAD TARGET"
                            else -> "ЦЕЛЬ ПРОГРЕССИИ"
                        },
                        color = AmberWarm
                    )

                    if (topTarget != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = topTarget.exerciseName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${topTarget.lastWeightKg} → ${topTarget.targetWeightKg} ${strings.unitKg}",
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentLime,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                        Text(
                            text = topTarget.reason.ifBlank {
                                "${topTarget.lastReps} → ${topTarget.targetReps} повторений"
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    } else {
                        Text(
                            text = state.insight.progressiveOverloadTarget,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = AccentLime
                        )
                        Text(
                            text = when (lang) {
                                "uz" -> "Oxirgi yondashuvlar barqaror texnika bilan bajarilganda yuklamani oshiring."
                                "en" -> "Increase load when recent sets feel solid and controlled."
                                else -> "Добавляйте вес на штангу, когда закрываете верхнюю границу повторений."
                            },
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }
            }

            // 3. Suggested Workout Card (if present)
            state.insight.suggestedWorkout?.let { plan ->
                item(key = "insight_suggested_workout", contentType = "suggested_workout") {
                    CoachSuggestedWorkoutCard(
                        plan = plan,
                        unitLabel = strings.unitKg,
                        lang = lang,
                        onStartWorkout = { onAction(CoachUiAction.OnStartSuggestedWorkout(plan)) },
                        onSaveRoutine = { onAction(CoachUiAction.OnSaveSuggestedRoutine(plan)) }
                    )
                }
            }

            // 4. AI Coach Chat
            item(key = "coach_chat_card", contentType = "chat") {
                GymCard {
                    GymSectionTitle(
                        title = when (lang) {
                            "uz" -> "AI Murabbiy"
                            "en" -> "AI Coach Chat"
                            else -> "Чат с AI-тренером"
                        },
                        actionLabel = if (state.chatMessages.isNotEmpty()) {
                            when (lang) { "uz" -> "Tozalash"; "en" -> "Clear"; else -> "Очистить" }
                        } else null,
                        onAction = if (state.chatMessages.isNotEmpty()) {
                            { onAction(CoachUiAction.OnClearChatHistory) }
                        } else null
                    )

                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(quickPrompts, key = { it.first }) { (label, prompt) ->
                            GymPillTab(
                                text = label,
                                selected = false,
                                onClick = { onAction(CoachUiAction.OnQuickPromptClick(prompt)) }
                            )
                        }
                    }

                    if (state.chatMessages.isEmpty()) {
                        GymInnerWell {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Mashg'ulot, texnika yoki ovqatlanish haqida so'rang — yoki tahlil uchun rasm yuboring."
                                    "en" -> "Ask about programming, form, or macros — or attach a photo for instant analysis."
                                    else -> "Спросите про рабочий вес, технику, замену упражнения или прикрепите фото формы / еды."
                                },
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = TextSecondary
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            state.chatMessages.forEach { msg ->
                                ChatMessageBlock(
                                    message = msg,
                                    unitLabel = strings.unitKg,
                                    lang = lang,
                                    onCopy = { copyText ->
                                        clipboard.setText(AnnotatedString(copyText))
                                        PlatformBridge.showToast(
                                            when (lang) {
                                                "uz" -> "Nusxalandi"
                                                "en" -> "Copied"
                                                else -> "Ответ скопирован"
                                            }
                                        )
                                    },
                                    onStartSuggestedWorkout = { onAction(CoachUiAction.OnStartSuggestedWorkout(it)) },
                                    onSaveSuggestedRoutine = { onAction(CoachUiAction.OnSaveSuggestedRoutine(it)) }
                                )
                            }
                        }
                    }

                    if (state.isSending) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CircularProgressIndicator(
                                color = AccentLime,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.5.dp
                            )
                            Text(
                                text = when (lang) {
                                    "uz" -> "AI murabbiy javob yozmoqda..."
                                    "en" -> "Coach is thinking..."
                                    else -> "Тренер анализирует..."
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentLime
                            )
                        }
                    }

                    if (state.attachedImageBase64 != null) {
                        val attachedBitmap = rememberBase64ImageBitmap(state.attachedImageBase64)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(AccentLimeSubtle)
                                .padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                if (attachedBitmap != null) {
                                    Image(
                                        bitmap = attachedBitmap,
                                        contentDescription = null,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                    )
                                }
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Rasm biriktirildi"
                                        "en" -> "Photo attached"
                                        else -> "Фото прикреплено"
                                    },
                                    fontSize = 13.sp,
                                    color = AccentLime,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(
                                onClick = { onAction(CoachUiAction.OnClearAttachedPhoto) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    val canSend = (state.inputMessage.isNotBlank() || state.attachedImageBase64 != null) && !state.isSending
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceHigh)
                                .clickable(onClick = pickPhoto),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = "Прикрепить фото",
                                tint = AccentLime,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        GymTextField(
                            value = state.inputMessage,
                            onValueChange = { onAction(CoachUiAction.OnInputMessageChanged(it)) },
                            placeholder = when (lang) {
                                "uz" -> "Mashg'ulot haqida so'rang..."
                                "en" -> "Ask your coach..."
                                else -> "Сообщение тренеру..."
                            },
                            singleLine = false,
                            minLines = 1,
                            modifier = Modifier.weight(1f)
                        )

                        Box(
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(if (canSend) AccentLime else DarkSurfaceHigh)
                                .clickable(enabled = canSend) { onAction(CoachUiAction.OnSendMessage) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Отправить",
                                tint = if (canSend) GymInkOnLight else TextMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatMessageBlock(
    message: ChatMessage,
    unitLabel: String,
    lang: String,
    onCopy: (String) -> Unit,
    onStartSuggestedWorkout: (SuggestedWorkoutPlan) -> Unit,
    onSaveSuggestedRoutine: (SuggestedWorkoutPlan) -> Unit
) {
    val isUser = message.isUser
    val (cleanedText, embeddedWorkout) = remember(message.text) {
        if (isUser) message.text to null else extractEmbeddedWorkoutPlan(message.text)
    }

    if (isUser) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.86f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 20.dp, bottomEnd = 6.dp))
                    .background(AccentLime)
                    .padding(14.dp)
            ) {
                Text(
                    text = cleanedText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 21.sp,
                    color = GymInkOnLight
                )
            }
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (cleanedText.isNotBlank()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 6.dp))
                        .background(DarkSurfaceHigh)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (lang) { "uz" -> "AI MURABBIY"; "en" -> "AI COACH"; else -> "AI ТРЕНЕР" },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.8.sp,
                            color = AccentLime
                        )
                        Row(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(DarkSurfaceElevated)
                                .clickable { onCopy(cleanedText) }
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy",
                                tint = TextSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when (lang) { "uz" -> "Nusxa"; "en" -> "Copy"; else -> "Копировать" },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary
                            )
                        }
                    }
                    Text(
                        text = cleanedText,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = TextPrimary
                    )
                }
            }

            val attachedPlan = message.suggestedWorkout ?: embeddedWorkout
            if (attachedPlan != null) {
                CoachSuggestedWorkoutCard(
                    plan = attachedPlan,
                    unitLabel = unitLabel,
                    lang = lang,
                    onStartWorkout = { onStartSuggestedWorkout(attachedPlan) },
                    onSaveRoutine = { onSaveSuggestedRoutine(attachedPlan) }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CoachSuggestedWorkoutCard(
    plan: SuggestedWorkoutPlan,
    unitLabel: String,
    lang: String,
    onStartWorkout: () -> Unit,
    onSaveRoutine: () -> Unit
) {
    GymCard(backgroundColor = AccentLimeSubtle) {
        GymKicker(
            text = when (lang) {
                "uz" -> "AI MASHG'ULOT REJASI"
                "en" -> "AI WORKOUT PLAN"
                else -> "ПЕРСОНАЛЬНЫЙ СПЛИТ ОТ ТРЕНЕРА"
            }
        )

        Text(
            text = plan.title.ifBlank {
                when (lang) { "uz" -> "Shaxsiy mashg'ulot"; "en" -> "Suggested Session"; else -> "Персональная тренировка" }
            },
            fontSize = 20.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary
        )

        if (plan.rationale.isNotBlank()) {
            Text(
                text = plan.rationale,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (plan.exercises.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkSurface)
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                plan.exercises.forEachIndexed { idx, ex ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${idx + 1}. ${ex.exerciseName}",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val weightText = if (ex.targetWeightKg > 0.0) " · ${ex.targetWeightKg} $unitLabel" else ""
                        Text(
                            text = "${ex.sets}×${ex.repsMin}$weightText",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentLime
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            GymPrimaryButton(
                text = when (lang) { "uz" -> "Boshlash"; "en" -> "Start"; else -> "Начать сейчас" },
                onClick = onStartWorkout,
                modifier = Modifier.weight(1.2f),
                fillWidth = true
            )
            GymOutlineButton(
                text = when (lang) { "uz" -> "Saqlash"; "en" -> "Save"; else -> "В шаблоны" },
                onClick = onSaveRoutine,
                modifier = Modifier.weight(1f),
                fillWidth = true
            )
        }
    }
}
