package dev.neverpaid.health.features.coach

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
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
            else -> "Плато"
        } to when (lang) {
            "uz" -> "Shtanga bosishda platoni qanday yengish mumkin?"
            "en" -> "How do I break through a bench press plateau?"
            else -> "Как преодолеть плато в жиме штанги лёжа?"
        },
        when (lang) {
            "uz" -> "Bugungi reja"
            "en" -> "Today plan"
            else -> "План на сегодня"
        } to when (lang) {
            "uz" -> "Bugungi tiklanish asosida mashg'ulot tuzing"
            "en" -> "Build today's workout based on my recovery"
            else -> "Составь тренировку на сегодня с учётом восстановления"
        },
        when (lang) {
            "uz" -> "Ratsion"
            "en" -> "Cut macros"
            else -> "Сушка"
        } to when (lang) {
            "uz" -> "Kuch yo'qotmasdan quritish uchun ratsion hisoblang"
            "en" -> "Calculate cutting macros without losing strength"
            else -> "Рассчитай рацион на сушку без потери силы"
        }
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header — Figma 10
            item(key = "header", contentType = "header") {
                GymPageHeader(
                    title = strings.navCoach,
                    subtitle = when (lang) {
                        "uz" -> "Mashg'ulotlar va tiklanishda yordamchingiz"
                        "en" -> "Your assistant for training and recovery"
                        else -> "Ваш помощник в тренировках и восстановлении"
                    }
                )
            }

            // Advisory Subheader — Figma 10
            item(key = "advisory_note", contentType = "note") {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Tahlil va tavsiyalar"
                            "en" -> "Training & recovery context"
                            else -> "Оценка состояния и нагрузка"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentLime
                    )
                    Text(
                        text = when (lang) {
                            "uz" -> "Tavsiyalar mashg'ulot tarixiga asoslanadi va shifokor ko'rigini almashtirmaydi."
                            "en" -> "Guidance reflects your training history and does not replace self-assessment or medical advice."
                            else -> "Подсказки основаны на ваших данных тренировок, не заменяют оценку самочувствия и не являются медицинской рекомендацией."
                        },
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        color = TextMuted
                    )
                }
            }

            if (state.isLoading && state.chatMessages.isEmpty()) {
                item(key = "loading_card", contentType = "loading") {
                    GymLoadingCard(
                        title = strings.stateLoadingTitle,
                        hint = strings.stateLoadingHint
                    )
                }
            } else {
                // Card 1: Readiness & ACWR Card — Figma 10
                item(key = "readiness_card", contentType = "readiness") {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = state.insight.readinessLevel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentLime
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = when (lang) {
                                "uz" -> "Bugungi tayyorgarlik"
                                "en" -> "Readiness today"
                                else -> "Готовность сегодня"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = "${state.insight.cnsScore} / 100",
                            fontSize = 36.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = when (lang) {
                                "uz" -> "CNS · tiklanish indeksi"
                                "en" -> "CNS · recovery index"
                                else -> "CNS · индекс готовности"
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSoft
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        GymProgressBar(
                            progress = (state.insight.cnsScore / 100f).coerceIn(0f, 1f),
                            heightDp = 6,
                            color = AccentLime
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = state.insight.statusMessage,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val weeklyTons = ((state.insight.weeklyVolumeKg / 1000.0) * 10.0).roundToInt() / 10.0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Haftalik yuklama"
                                    "en" -> "Weekly load"
                                    else -> "Нагрузка за неделю"
                                },
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${state.insight.weeklyWorkoutsCount} · $weeklyTons т",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AccentLime
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (lang) {
                                "uz" -> "Oxirgi mashg'ulotdan beri: ${state.insight.daysSinceLastTrain} kun"
                                "en" -> "Days since last session: ${state.insight.daysSinceLastTrain}"
                                else -> "Дней с прошлой тренировки: ${state.insight.daysSinceLastTrain}"
                            },
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPillTab(
                                text = when (lang) {
                                    "uz" -> "MNS so'rovi"
                                    "en" -> "CNS Check-in"
                                    else -> "Опрос ЦНС"
                                },
                                selected = true,
                                onClick = { isReadinessSurveyOpen = true }
                            )
                            quickPrompts.take(2).forEach { (label, prompt) ->
                                GymPillTab(
                                    text = label,
                                    selected = false,
                                    onClick = { onAction(CoachUiAction.OnQuickPromptClick(prompt)) }
                                )
                            }
                        }
                    }
                }

                // Card 2: Progressive Overload Recommendation — Figma 10
                val topTarget = state.insight.overloadTargets.firstOrNull()
                item(key = "overload_card", contentType = "overload") {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Tavsiya · yuklama"
                                "en" -> "Coach · overload target"
                                else -> "Рекомендация · прогрессия"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = AccentLime
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        if (topTarget != null) {
                            Text(
                                text = topTarget.exerciseName,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${topTarget.lastWeightKg} → ${topTarget.targetWeightKg} ${strings.unitKg}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = topTarget.reason.ifBlank {
                                    "${topTarget.lastReps} → ${topTarget.targetReps} повторений"
                                },
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = TextSecondary
                            )
                        } else {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Bosqichma-bosqich og'irlik oshirish"
                                    "en" -> "Next progressive overload step"
                                    else -> "Следующий шаг в прогрессии"
                                },
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.insight.progressiveOverloadTarget,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                                color = AccentLime
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = when (lang) {
                                    "uz" -> "Oxirgi yondashuvlar barqaror texnika va og'riqsiz bajarilganda yuklamani oshiring."
                                    "en" -> "Increase load only when recent sets are clean, controlled, and pain-free."
                                    else -> "Повышайте вес только если последние подходы выполнены с устойчивой техникой и без дискомфорта."
                                },
                                fontSize = 13.sp,
                                lineHeight = 19.sp,
                                color = TextSecondary
                            )
                        }
                    }
                }

                // Card 3: Suggested Workout Preview Card — Figma 10
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

                // Card 4: Discussion & Chat Composer Card — Figma 10
                item(key = "coach_chat_card", contentType = "chat") {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Mashg'ulotingizni muhokama qilamiz"
                                "en" -> "Discuss your training"
                                else -> "Обсудим вашу тренировку"
                            },
                            fontSize = 20.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = when (lang) {
                                "uz" -> "AI murabbiy · ${state.chatMessages.size} xabar"
                                "en" -> "AI Coach · ${state.chatMessages.size} messages"
                                else -> "AI Тренер duda.uz · ${state.chatMessages.size} сообщений"
                            },
                            fontSize = 12.sp,
                            color = AccentLime
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        if (state.chatMessages.isNotEmpty()) {
                            GymOutlineButton(
                                text = when (lang) {
                                    "uz" -> "Tarixni tozalash"
                                    "en" -> "Clear history"
                                    else -> "Очистить историю"
                                },
                                onClick = { onAction(CoachUiAction.OnClearChatHistory) }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }

                        // Quick prompts bar
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(quickPrompts, key = { it.first }) { (label, prompt) ->
                                GymPillTab(
                                    text = label,
                                    selected = false,
                                    onClick = { onAction(CoachUiAction.OnQuickPromptClick(prompt)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (state.chatMessages.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "AI murabbiyga savol bering"
                                        "en" -> "Ask the AI coach anything"
                                        else -> "Задайте вопрос тренеру"
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextPrimary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Tezkor savol tugmasini bosing yoki texnika tahlili uchun foto biriktiring."
                                        "en" -> "Tap a quick prompt above or attach a form/physique photo for analysis."
                                        else -> "Выберите быстрый вопрос выше, спросите о прогрессии или прикрепите фото для разбора."
                                    },
                                    fontSize = 12.sp,
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
                                                    "uz" -> "Javob nusxalandi"
                                                    "en" -> "Reply copied"
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
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    color = AccentLime,
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = when (lang) {
                                        "uz" -> "AI murabbiy ma'lumotlarni tahlil qilmoqda..."
                                        "en" -> "AI coach is analyzing..."
                                        else -> "ИИ-Тренер анализирует ваши данные..."
                                    },
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        GymDivider()
                        Spacer(modifier = Modifier.height(14.dp))

                        if (state.attachedImageBase64 != null) {
                            val attachedBitmap = rememberBase64ImageBitmap(state.attachedImageBase64)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(AccentLimeSubtle)
                                    .border(1.dp, BorderHighlight, RoundedCornerShape(10.dp))
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                                .size(40.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .border(1.dp, BorderSubtle, RoundedCornerShape(6.dp))
                                        )
                                    }
                                    Text(
                                        text = when (lang) {
                                            "uz" -> "Rasm biriktirildi"
                                            "en" -> "Photo attached"
                                            else -> "Фото прикреплено к сообщению"
                                        },
                                        fontSize = 12.sp,
                                        color = AccentLime,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(
                                    onClick = { onAction(CoachUiAction.OnClearAttachedPhoto) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(10.dp))
                        }

                        Text(
                            text = when (lang) {
                                "uz" -> "Savolingiz"
                                "en" -> "Your question"
                                else -> "Ваш вопрос"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        GymTextField(
                            value = state.inputMessage,
                            onValueChange = { onAction(CoachUiAction.OnInputMessageChanged(it)) },
                            placeholder = when (lang) {
                                "uz" -> "Mashg'ulot haqida so'rang"
                                "en" -> "Ask about your workout"
                                else -> "Спросите о тренировке"
                            },
                            singleLine = false,
                            minLines = 1
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val canSend = state.inputMessage.isNotBlank() || state.attachedImageBase64 != null
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymOutlineButton(
                                text = when (lang) {
                                    "uz" -> "Foto biriktirish"
                                    "en" -> "Attach photo"
                                    else -> "Прикрепить фото"
                                },
                                onClick = pickPhoto
                            )
                            GymPrimaryButton(
                                text = when (lang) {
                                    "uz" -> "Yuborish"
                                    "en" -> "Send"
                                    else -> "Отправить"
                                },
                                onClick = { onAction(CoachUiAction.OnSendMessage) },
                                enabled = canSend && !state.isSending
                            )
                        }
                    }
                }

                item(key = "bottom_spacer", contentType = "spacer") {
                    Spacer(modifier = Modifier.height(24.dp))
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
        GymInnerWell {
            Text(
                text = when (lang) { "uz" -> "Siz"; "en" -> "You"; else -> "Вы" },
                fontSize = 12.sp,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = cleanedText,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = TextPrimary
            )
        }
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (cleanedText.isNotBlank()) {
                val shape = RoundedCornerShape(10.dp)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(shape)
                        .background(DarkBackground)
                        .border(1.dp, BorderSubtle, shape)
                        .padding(14.dp)
                ) {
                    Text(
                        text = when (lang) { "uz" -> "AI Murabbiy"; "en" -> "AI Coach"; else -> "AI Тренер" },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentLime
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = cleanedText,
                        fontSize = 14.sp,
                        lineHeight = 21.sp,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    GymOutlineButton(
                        text = when (lang) {
                            "uz" -> "Javobni nusxalash"
                            "en" -> "Copy reply"
                            else -> "Копировать ответ"
                        },
                        onClick = { onCopy(cleanedText) }
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
    GymCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle,
        contentPadding = PaddingValues(20.dp)
    ) {
        Text(
            text = when (lang) {
                "uz" -> "Tavsiya etilgan mashg'ulot"
                "en" -> "Coach · workout option"
                else -> "Вариант тренировки"
            },
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = AccentLime
        )

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = plan.title.ifBlank {
                when (lang) { "uz" -> "Shaxsiy mashg'ulot"; "en" -> "Suggested Session"; else -> "Персональная тренировка" }
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )

        if (plan.rationale.isNotBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = plan.rationale,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

        if (plan.exercises.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
            plan.exercises.forEachIndexed { idx, ex ->
                if (idx > 0) GymDivider()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${idx + 1}. ${ex.exerciseName}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    val weightText = if (ex.targetWeightKg > 0.0) " · ${ex.targetWeightKg} $unitLabel" else ""
                    Text(
                        text = "${ex.sets} × ${ex.repsMin}$weightText",
                        fontSize = 13.sp,
                        color = AccentLime
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymPrimaryButton(
                text = when (lang) { "uz" -> "Boshlash"; "en" -> "Start"; else -> "Начать" },
                onClick = onStartWorkout
            )
            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "Reja sifatida saqlash"
                    "en" -> "Save as routine"
                    else -> "Сохранить как план"
                },
                onClick = onSaveRoutine
            )
        }
    }
}
