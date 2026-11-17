package dev.neverpaid.health.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.TextSoft
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.domain.model.FeedbackItem

@Composable
fun ProfileRoute(
    viewModel: ProfileViewModel,
    onNavigateToAuth: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            ProfileUiEffect.NavigateToAuth -> onNavigateToAuth()
        }
    }

    ProfileScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onAction: (ProfileUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = strings.languageCode
    val restOptions = listOf(60, 90, 120, 180)
    val languages = listOf("ru" to "RU", "uz" to "UZ", "en" to "EN")

    var isLogoutConfirmOpen by rememberSaveable { mutableStateOf(false) }
    var isCommunityListExpanded by rememberSaveable { mutableStateOf(false) }

    val filteredFeedback = remember(state.feedbackList, state.selectedFeedbackFilter) {
        state.feedbackList.filter { item ->
            state.selectedFeedbackFilter == "all" || item.category.equals(state.selectedFeedbackFilter, ignoreCase = true)
        }
    }

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
            // Header — Figma 12
            item(key = "header", contentType = "header") {
                GymPageHeader(
                    title = strings.profileTitle,
                    subtitle = when (lang) {
                        "uz" -> "Sozlamalar, ma'lumotlar va maqsadlar"
                        "en" -> "Preferences, data and settings"
                        else -> "Предпочтения, данные и настройки"
                    }
                )
            }

            if (state.isLoading && state.user == null) {
                item(key = "loading_card", contentType = "loading") {
                    GymLoadingCard(
                        title = strings.stateLoadingTitle,
                        hint = strings.stateLoadingHint
                    )
                }
            }

            // Status / Error Banner
            if (state.errorMessage != null || state.successMessage != null) {
                item(key = "status_banner", contentType = "banner") {
                    val isError = state.errorMessage != null
                    val accent = if (isError) CrimsonRed else AccentLime
                    GymCard(
                        backgroundColor = if (isError) DarkSurface else AccentLimeSubtle,
                        borderColor = if (isError) CrimsonRed.copy(alpha = 0.5f) else BorderHighlight,
                        contentPadding = PaddingValues(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = state.errorMessage ?: state.successMessage.orEmpty(),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = accent,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { onAction(ProfileUiAction.OnDismissMessages) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = null,
                                    tint = accent,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Card 1: Account & Language — Figma 12
            item(key = "account_card", contentType = "account") {
                GymCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        text = when (lang) { "uz" -> "Akkaunt"; "en" -> "Account"; else -> "Аккаунт" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    val displayName = if (state.isGuest) {
                        when (lang) { "uz" -> "Mehmon"; "en" -> "Guest"; else -> "Гость" }
                    } else {
                        state.userName
                    }

                    Text(
                        text = displayName,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    if (state.userEmail.isNotBlank()) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = state.userEmail,
                            fontSize = 13.sp,
                            color = TextMuted
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when (lang) { "uz" -> "Ism"; "en" -> "Name"; else -> "Имя" },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GymInnerWell {
                        Text(
                            text = displayName,
                            fontSize = 15.sp,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    if (state.isGuest) {
                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Google orqali kirish"
                                "en" -> "Sign in with Google"
                                else -> "Вход через Google"
                            },
                            onClick = { onAction(ProfileUiAction.OnLogoutClick) }
                        )
                    } else {
                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Ma'lumotlarni yangilash"
                                "en" -> "Sync account"
                                else -> "Обновить профиль"
                            },
                            onClick = { onAction(ProfileUiAction.OnRefresh) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = strings.profileLanguage,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        languages.forEach { (code, label) ->
                            GymPillTab(
                                text = label,
                                selected = state.languageCode.equals(code, ignoreCase = true),
                                onClick = { onAction(ProfileUiAction.OnSelectLanguage(code)) }
                            )
                        }
                    }
                }
            }

            // Card 2: Training Preferences — Figma 12
            item(key = "training_prefs_card", contentType = "training_prefs") {
                GymCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Mashg'ulotlar"
                            "en" -> "Workouts"
                            else -> "Тренировки"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = strings.profileUnits,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        GymPillTab(
                            text = "KG",
                            selected = state.useKilograms,
                            onClick = { onAction(ProfileUiAction.OnToggleUnits(true)) }
                        )
                        GymPillTab(
                            text = "LB",
                            selected = !state.useKilograms,
                            onClick = { onAction(ProfileUiAction.OnToggleUnits(false)) }
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = strings.profileRestTimer,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        restOptions.forEach { secs ->
                            GymPillTab(
                                text = "$secs ${when (lang) { "uz" -> "son"; "en" -> "s"; else -> "с" }}",
                                selected = state.defaultRestSeconds == secs,
                                onClick = { onAction(ProfileUiAction.OnSelectDefaultRest(secs)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    GymDivider()
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Taymer ovozi"
                                "en" -> "Timer sound"
                                else -> "Звук таймера"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = state.soundAlertsEnabled,
                            onCheckedChange = { onAction(ProfileUiAction.OnToggleSound(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = AccentLime,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceHigh
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.profileVibration,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Switch(
                            checked = state.vibrationEnabled,
                            onCheckedChange = { onAction(ProfileUiAction.OnToggleVibration(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = AccentLime,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceHigh
                            )
                        )
                    }
                }
            }

            // Card 3: Nutrition Targets & Parameters — Figma 12
            item(key = "macro_targets_card", contentType = "macros") {
                val pVal = state.proteinTargetInput.toIntOrNull() ?: 0
                val cVal = state.carbsTargetInput.toIntOrNull() ?: 0
                val fVal = state.fatsTargetInput.toIntOrNull() ?: 0
                val computedKcal = (pVal * 4) + (cVal * 4) + (fVal * 9)

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GymCard(contentPadding = PaddingValues(20.dp)) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Ovqatlanish maqsadlari va parametrlar"
                                "en" -> "Nutrition goals & targets"
                                else -> "Цели питания и параметры"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (lang) {
                                "uz" -> "Ovqatlanish va AI Murabbiy bo'limlari bilan sinxronlanadi."
                                "en" -> "Synced with Nutrition & AI Coach goals."
                                else -> "Синхронизируются с разделом Питание и AI Тренером."
                            },
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        GymLabeledField(
                            label = when (lang) { "uz" -> "Oqsillar, g"; "en" -> "Protein, g"; else -> "Белки, г" },
                            value = state.proteinTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateProteinTarget(it)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "180"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GymLabeledField(
                            label = when (lang) { "uz" -> "Uglevodlar, g"; "en" -> "Carbs, g"; else -> "Углеводы, г" },
                            value = state.carbsTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateCarbsTarget(it)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "280"
                        )

                        Spacer(modifier = Modifier.height(10.dp))

                        GymLabeledField(
                            label = when (lang) { "uz" -> "Yog'lar, g"; "en" -> "Fats, g"; else -> "Жиры, г" },
                            value = state.fatsTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateFatsTarget(it)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            placeholder = "80"
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "${if (computedKcal > 0) computedKcal else state.calorieTargetInput} ккал",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Text(
                            text = "${pVal}×4 + ${cVal}×4 + ${fVal}×9",
                            fontSize = 12.sp,
                            color = TextMuted
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GymLabeledField(
                                label = when (lang) {
                                    "uz" -> "Kaloriya / kun"
                                    "en" -> "Calories / day"
                                    else -> "Ккал / день"
                                },
                                value = state.calorieTargetInput,
                                onValueChange = { onAction(ProfileUiAction.OnUpdateCalorieTarget(it)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = when (lang) {
                                    "uz" -> "Suv maqsadi, ml"
                                    "en" -> "Water goal, ml"
                                    else -> "Вода, мл"
                                },
                                value = state.waterGoalInput,
                                onValueChange = { onAction(ProfileUiAction.OnUpdateWaterGoal(it)) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    GymPrimaryButton(
                        text = when (lang) {
                            "uz" -> "Sozlamalarni saqlash"
                            "en" -> "Save preferences"
                            else -> "Сохранить предпочтения"
                        },
                        onClick = { onAction(ProfileUiAction.OnSaveMacroTargets) },
                        fillWidth = true
                    )

                    Text(
                        text = when (lang) {
                            "uz" -> "O'zgarishlar joriy sozlamalar mantiqi orqali qo'llaniladi."
                            "en" -> "Changes apply via the active preferences store."
                            else -> "Изменения применяются через текущую логику настроек."
                        },
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            // Card 4: Data, App & Confirmed Logout — Figma 12
            item(key = "data_app_card", contentType = "data_app") {
                val stats = state.platformStats
                GymCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Ma'lumotlar va ilova"
                            "en" -> "Data & application"
                            else -> "Данные и приложение"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    GymInnerWell {
                        val tons = (stats.totalVolumeKg / 1000.0).toInt()
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            StatCol(
                                label = when (lang) { "uz" -> "Atletlar"; "en" -> "Athletes"; else -> "Атлетов" },
                                value = stats.totalUsers.toString()
                            )
                            StatCol(
                                label = when (lang) { "uz" -> "Mashg'ulot"; "en" -> "Workouts"; else -> "Тренировок" },
                                value = stats.totalWorkouts.toString()
                            )
                            StatCol(
                                label = when (lang) { "uz" -> "Tonnaj"; "en" -> "Tonnage"; else -> "Тоннаж" },
                                value = "${tons}т"
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "API base URL",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    GymInnerWell {
                        Text(
                            text = "https://duda.uz/api/v1",
                            fontSize = 14.sp,
                            color = TextSoft
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    GymDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    if (state.isGuest) {
                        GymPrimaryButton(
                            text = when (lang) {
                                "uz" -> "Akkauntga kirish"
                                "en" -> "Sign in"
                                else -> "Войти в аккаунт"
                            },
                            onClick = { onAction(ProfileUiAction.OnLogoutClick) },
                            fillWidth = true
                        )
                    } else if (!isLogoutConfirmOpen) {
                        GymOutlineButton(
                            text = strings.profileLogout,
                            onClick = { isLogoutConfirmOpen = true },
                            fillWidth = true,
                            textColor = CrimsonRed,
                            borderColor = CrimsonRed.copy(alpha = 0.45f)
                        )
                    } else {
                        Text(
                            text = when (lang) {
                                "uz" -> "Akkauntdan chiqasizmi?"
                                "en" -> "Log out of account?"
                                else -> "Выйти из аккаунта?"
                            },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = when (lang) {
                                "uz" -> "Avval chiqishni tasdiqlang. Bu yerdaligingizda sessiya faol qoladi."
                                "en" -> "Confirm sign-out first. Your session remains active until confirmed."
                                else -> "Сначала подтвердите выход. Пока вы здесь, сеанс остаётся активным."
                            },
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = { isLogoutConfirmOpen = false },
                                modifier = Modifier.weight(1f),
                                fillWidth = true
                            )
                            GymOutlineButton(
                                text = strings.profileLogout,
                                onClick = {
                                    isLogoutConfirmOpen = false
                                    onAction(ProfileUiAction.OnLogoutClick)
                                },
                                modifier = Modifier.weight(1f),
                                fillWidth = true,
                                textColor = CrimsonRed,
                                borderColor = CrimsonRed
                            )
                        }
                    }
                }
            }

            // Card 5: Community Feedback & Ideas — Figma 12
            item(key = "feedback_card", contentType = "feedback") {
                GymCard(contentPadding = PaddingValues(20.dp)) {
                    Text(
                        text = when (lang) {
                            "uz" -> "duda.uz ni yaxshilashga yordam bering"
                            "en" -> "Help improve duda.uz"
                            else -> "Помогите сделать duda.uz лучше"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Yaxshilash taklif qiling yoki mavjud g'oyani qo'llab-quvvatlang."
                            "en" -> "Suggest an improvement or upvote an existing idea."
                            else -> "Предложите улучшение или поддержите существующую идею."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when (lang) {
                            "uz" -> "Taklifingiz"
                            "en" -> "Your suggestion"
                            else -> "Ваше предложение"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    GymTextField(
                        value = state.feedbackMessage,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateFeedbackMessage(it)) },
                        placeholder = when (lang) {
                            "uz" -> "Nimani yaxshilash kerak?"
                            "en" -> "What should we improve?"
                            else -> "Что стоит улучшить?"
                        },
                        singleLine = false,
                        minLines = 2
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Taklif yuborish"
                                "en" -> "Send suggestion"
                                else -> "Отправить предложение"
                            },
                            onClick = { onAction(ProfileUiAction.OnSubmitFeedback) },
                            enabled = !state.isSubmittingFeedback && state.feedbackMessage.isNotBlank()
                        )
                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Kategoriya bilan yuborish"
                                "en" -> "Full form"
                                else -> "Подробный отзыв"
                            },
                            onClick = { onAction(ProfileUiAction.OnOpenFeedbackModal) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    GymDivider()
                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = when (lang) {
                            "uz" -> "Jamiyat g'oyalari (${state.feedbackList.size})"
                            "en" -> "Community ideas (${state.feedbackList.size})"
                            else -> "Идеи сообщества (${state.feedbackList.size})"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Mavjud takliflar va ovozlar"
                            "en" -> "Existing suggestions and votes"
                            else -> "Существующие предложения и голоса"
                        },
                        fontSize = 12.sp,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    GymOutlineButton(
                        text = if (isCommunityListExpanded) {
                            when (lang) { "uz" -> "Ro'yxatni yopish"; "en" -> "Hide ideas"; else -> "Скрыть идеи" }
                        } else {
                            when (lang) { "uz" -> "Ko'rish va ovoz berish"; "en" -> "View and vote"; else -> "Посмотреть и проголосовать" }
                        },
                        onClick = { isCommunityListExpanded = !isCommunityListExpanded },
                        fillWidth = true
                    )

                    if (isCommunityListExpanded) {
                        Spacer(modifier = Modifier.height(14.dp))

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf(
                                "all" to "${when (lang) { "uz" -> "Hammasi"; "en" -> "All"; else -> "Все" }} (${state.feedbackList.size})",
                                "idea" to when (lang) { "uz" -> "G'oyalar"; "en" -> "Ideas"; else -> "Идеи" },
                                "bug" to when (lang) { "uz" -> "Xatolar"; "en" -> "Bugs"; else -> "Баги" },
                                "praise" to when (lang) { "uz" -> "Fikrlar"; "en" -> "Praise"; else -> "Отзывы" }
                            ).forEach { (key, label) ->
                                GymPillTab(
                                    text = label,
                                    selected = state.selectedFeedbackFilter == key,
                                    onClick = { onAction(ProfileUiAction.OnSelectFeedbackFilter(key)) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        if (filteredFeedback.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Bu bo'limda hali yozuvlar yo'q"
                                        "en" -> "No entries in this category yet"
                                        else -> "Пока нет записей в этой категории"
                                    },
                                    fontSize = 13.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            filteredFeedback.forEachIndexed { idx, item ->
                                if (idx > 0) GymDivider()
                                FeedbackItemCleanRow(
                                    item = item,
                                    lang = lang,
                                    onUpvote = { onAction(ProfileUiAction.OnToggleFeedbackUpvote(item.id)) }
                                )
                            }
                        }
                    }
                }
            }

            item(key = "bottom_spacer", contentType = "spacer") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    if (state.isFeedbackModalOpen) {
        ModalBottomSheet(
            onDismissRequest = { onAction(ProfileUiAction.OnCloseFeedbackModal) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Fikr yoki taklif qoldirish"
                            "en" -> "Leave feedback or idea"
                            else -> "Оставить отзыв или предложение"
                        },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    IconButton(onClick = { onAction(ProfileUiAction.OnCloseFeedbackModal) }) {
                        Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                    }
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "idea" to when (lang) { "uz" -> "G'oya"; "en" -> "Idea"; else -> "Идея" },
                        "bug" to when (lang) { "uz" -> "Xato"; "en" -> "Bug"; else -> "Баг" },
                        "praise" to when (lang) { "uz" -> "Fikr"; "en" -> "Praise"; else -> "Отзыв" }
                    ).forEach { (catKey, catLabel) ->
                        GymPillTab(
                            text = catLabel,
                            selected = state.feedbackCategory == catKey,
                            onClick = { onAction(ProfileUiAction.OnSelectFeedbackCategory(catKey)) }
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = when (lang) { "uz" -> "Baho:"; "en" -> "Rating:"; else -> "Оценка:" },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { onAction(ProfileUiAction.OnSelectFeedbackRating(star)) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star",
                                tint = if (star <= state.feedbackRating) AmberWarm else TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                GymTextField(
                    value = state.feedbackMessage,
                    onValueChange = { onAction(ProfileUiAction.OnUpdateFeedbackMessage(it)) },
                    placeholder = when (lang) {
                        "uz" -> "G'oyangiz, xato yoki taassurotingizni yozing..."
                        "en" -> "Describe your idea, bug or feedback..."
                        else -> "Опишите вашу идею, баг или впечатление..."
                    },
                    singleLine = false,
                    minLines = 3
                )

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    GymOutlineButton(
                        text = strings.cancel,
                        onClick = { onAction(ProfileUiAction.OnCloseFeedbackModal) },
                        modifier = Modifier.weight(1f),
                        fillWidth = true
                    )
                    GymPrimaryButton(
                        text = if (state.isSubmittingFeedback) strings.loading else when (lang) {
                            "uz" -> "Yuborish"
                            "en" -> "Submit"
                            else -> "Отправить"
                        },
                        onClick = { onAction(ProfileUiAction.OnSubmitFeedback) },
                        enabled = !state.isSubmittingFeedback && state.feedbackMessage.isNotBlank(),
                        modifier = Modifier.weight(1f),
                        fillWidth = true
                    )
                }
            }
        }
    }
}

@Composable
private fun StatCol(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = AccentLime
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
    }
}

@Composable
private fun FeedbackItemCleanRow(
    item: FeedbackItem,
    lang: String,
    onUpvote: () -> Unit
) {
    val categoryLabel = when (item.category.lowercase()) {
        "bug" -> when (lang) { "uz" -> "Xato"; "en" -> "Bug"; else -> "Баг" }
        "praise" -> when (lang) { "uz" -> "Fikr"; "en" -> "Praise"; else -> "Отзыв" }
        else -> when (lang) { "uz" -> "G'oya"; "en" -> "Idea"; else -> "Идея" }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${item.userName.ifBlank { "Атлет" }} · $categoryLabel",
                fontSize = 12.sp,
                color = AccentLime,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = item.createdAt.take(10),
                fontSize = 11.sp,
                color = TextMuted
            )
        }

        Text(
            text = item.message,
            fontSize = 14.sp,
            color = TextPrimary,
            lineHeight = 20.sp
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            val shape = RoundedCornerShape(8.dp)
            Row(
                modifier = Modifier
                    .clip(shape)
                    .background(if (item.upvotedByMe) AccentLimeSubtle else DarkBackground)
                    .border(
                        1.dp,
                        if (item.upvotedByMe) BorderHighlight else BorderSubtle,
                        shape
                    )
                    .clickable(onClick = onUpvote)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ThumbUp,
                    contentDescription = null,
                    tint = if (item.upvotedByMe) AccentLime else TextSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = item.upvotes.toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (item.upvotedByMe) AccentLime else TextSecondary
                )
            }
        }
    }
}
