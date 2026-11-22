package dev.neverpaid.health.features.profile

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.CrimsonSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymStatTile
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.SurfaceWhite
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.VioletPrimary
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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 36.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "header", contentType = "header") {
            GymPageHeader(title = strings.profileTitle)
        }

        if (state.isLoading && state.user == null) {
            item(key = "loading_card", contentType = "loading") {
                GymLoadingCard(
                    title = strings.stateLoadingTitle,
                    hint = strings.stateLoadingHint
                )
            }
        }

        if (state.errorMessage != null || state.successMessage != null) {
            item(key = "status_banner", contentType = "banner") {
                val isError = state.errorMessage != null
                val accent = if (isError) CrimsonRed else AccentLime
                GymCard(
                    backgroundColor = if (isError) CrimsonSubtle else AccentLimeSubtle,
                    contentPadding = PaddingValues(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.errorMessage ?: state.successMessage.orEmpty(),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onAction(ProfileUiAction.OnDismissMessages) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = accent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }

        // 1. Royal Violet Athlete Profile Hero + Language Preference Card (Figma Screen 10)
        item(key = "account_card", contentType = "account") {
            val displayName = if (state.isGuest) {
                when (lang) { "uz" -> "Mehmon"; "en" -> "Guest Athlete"; else -> "Гость" }
            } else {
                state.userName.ifBlank { "Атлет" }
            }
            val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "D"

            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                RoyalHeroCard(bottomExtraPadding = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .clip(CircleShape)
                                .background(InkWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initial,
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Black,
                                color = VioletPrimary
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = displayName,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Black,
                                color = InkWhite
                            )
                            if (state.userEmail.isNotBlank()) {
                                Text(
                                    text = state.userEmail,
                                    fontSize = 13.sp,
                                    color = HeroMuted
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(SurfaceWhite)
                                .clickable {
                                    if (state.isGuest) {
                                        onAction(ProfileUiAction.OnLogoutClick)
                                    } else {
                                        onAction(ProfileUiAction.OnRefresh)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (state.isGuest) {
                                    when (lang) { "uz" -> "Kirish"; "en" -> "Sign in"; else -> "Войти" }
                                } else {
                                    when (lang) { "uz" -> "Yangilash"; "en" -> "Sync"; else -> "Синхр." }
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = VioletPrimary
                            )
                        }
                    }
                }

                GymCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = strings.profileLanguage,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
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
            }
        }

        // 2. Gym & Timer Settings
        item(key = "training_prefs_card", contentType = "training_prefs") {
            GymCard {
                GymSectionTitle(
                    title = when (lang) {
                        "uz" -> "Mashg'ulot sozlamalari"
                        "en" -> "Workout settings"
                        else -> "Настройки зала"
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.profileUnits,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
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
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = strings.profileRestTimer,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        restOptions.forEach { secs ->
                            GymPillTab(
                                text = "$secs ${when (lang) { "uz" -> "s"; "en" -> "s"; else -> "с" }}",
                                selected = state.defaultRestSeconds == secs,
                                onClick = { onAction(ProfileUiAction.OnSelectDefaultRest(secs)) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                GymDivider()

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
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Switch(
                        checked = state.soundAlertsEnabled,
                        onCheckedChange = { onAction(ProfileUiAction.OnToggleSound(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GymInkOnLight,
                            checkedTrackColor = AccentLime,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceHigh
                        )
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = strings.profileVibration,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Switch(
                        checked = state.vibrationEnabled,
                        onCheckedChange = { onAction(ProfileUiAction.OnToggleVibration(it)) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = GymInkOnLight,
                            checkedTrackColor = AccentLime,
                            uncheckedThumbColor = TextSecondary,
                            uncheckedTrackColor = DarkSurfaceHigh
                        )
                    )
                }
            }
        }

        // 3. Daily Nutrition & Hydration Targets
        item(key = "macro_targets_card", contentType = "macros") {
            val pVal = state.proteinTargetInput.toIntOrNull() ?: 0
            val cVal = state.carbsTargetInput.toIntOrNull() ?: 0
            val fVal = state.fatsTargetInput.toIntOrNull() ?: 0
            val computedKcal = (pVal * 4) + (cVal * 4) + (fVal * 9)

            GymCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (lang) {
                            "uz" -> "KBJU va suv maqsadi"
                            "en" -> "Daily macros & water"
                            else -> "Норма КБЖУ и воды"
                        },
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Text(
                        text = "${if (computedKcal > 0) computedKcal else state.calorieTargetInput} ккал",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = AccentLime,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GymLabeledField(
                        label = when (lang) { "uz" -> "Oqsil, g"; "en" -> "Protein, g"; else -> "Белки, г" },
                        value = state.proteinTargetInput,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateProteinTarget(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = "180",
                        modifier = Modifier.weight(1f)
                    )
                    GymLabeledField(
                        label = when (lang) { "uz" -> "Uglevod, g"; "en" -> "Carbs, g"; else -> "Углеводы, г" },
                        value = state.carbsTargetInput,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateCarbsTarget(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = "280",
                        modifier = Modifier.weight(1f)
                    )
                    GymLabeledField(
                        label = when (lang) { "uz" -> "Yog', g"; "en" -> "Fat, g"; else -> "Жиры, г" },
                        value = state.fatsTargetInput,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateFatsTarget(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        placeholder = "80",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GymLabeledField(
                        label = when (lang) { "uz" -> "Kaloriya / kun"; "en" -> "Calories / day"; else -> "Ккал / день" },
                        value = state.calorieTargetInput,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateCalorieTarget(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                    GymLabeledField(
                        label = when (lang) { "uz" -> "Suv, ml"; "en" -> "Water, ml"; else -> "Вода, мл" },
                        value = state.waterGoalInput,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateWaterGoal(it)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f)
                    )
                }

                GymPrimaryButton(
                    text = when (lang) {
                        "uz" -> "Saqlash"
                        "en" -> "Save targets"
                        else -> "Сохранить цели"
                    },
                    onClick = { onAction(ProfileUiAction.OnSaveMacroTargets) },
                    fillWidth = true
                )
            }
        }

        // 4. Community Stats & Feedback
        item(key = "community_card", contentType = "feedback") {
            val stats = state.platformStats
            val tons = (stats.totalVolumeKg / 1000.0).toInt()

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GymStatTile(
                        label = when (lang) { "uz" -> "Atletlar"; "en" -> "Athletes"; else -> "Атлетов" },
                        value = stats.totalUsers.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    GymStatTile(
                        label = when (lang) { "uz" -> "Mashg'ulot"; "en" -> "Workouts"; else -> "Тренировок" },
                        value = stats.totalWorkouts.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    GymStatTile(
                        label = when (lang) { "uz" -> "Tonnaj"; "en" -> "Tonnage"; else -> "Тоннаж" },
                        value = "$tons",
                        unit = "т",
                        valueColor = AccentLime,
                        modifier = Modifier.weight(1f)
                    )
                }

                GymCard {
                    GymSectionTitle(
                        title = when (lang) {
                            "uz" -> "Fikr va g'oyalar"
                            "en" -> "Ideas & feedback"
                            else -> "Идеи и предложения"
                        },
                        actionLabel = when (lang) {
                            "uz" -> "Batafsil"
                            "en" -> "Full form"
                            else -> "Подробно"
                        },
                        onAction = { onAction(ProfileUiAction.OnOpenFeedbackModal) }
                    )

                    GymTextField(
                        value = state.feedbackMessage,
                        onValueChange = { onAction(ProfileUiAction.OnUpdateFeedbackMessage(it)) },
                        placeholder = when (lang) {
                            "uz" -> "Nimani yaxshilash kerak?"
                            "en" -> "What should we improve?"
                            else -> "Что добавить или улучшить в приложении?"
                        },
                        singleLine = false,
                        minLines = 2
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymPrimaryButton(
                            text = when (lang) {
                                "uz" -> "Yuborish"
                                "en" -> "Send"
                                else -> "Отправить"
                            },
                            onClick = { onAction(ProfileUiAction.OnSubmitFeedback) },
                            enabled = !state.isSubmittingFeedback && state.feedbackMessage.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        )
                        GymOutlineButton(
                            text = if (isCommunityListExpanded) {
                                when (lang) { "uz" -> "Yopish"; "en" -> "Hide"; else -> "Скрыть (${state.feedbackList.size})" }
                            } else {
                                when (lang) { "uz" -> "G'oyalar (${state.feedbackList.size})"; "en" -> "Ideas (${state.feedbackList.size})"; else -> "Идеи (${state.feedbackList.size})" }
                            },
                            onClick = { isCommunityListExpanded = !isCommunityListExpanded },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (isCommunityListExpanded) {
                        GymDivider()

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
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                filteredFeedback.forEach { item ->
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
            }
        }

        // 5. Sign Out
        if (!state.isGuest) {
            item(key = "logout_card", contentType = "logout") {
                if (!isLogoutConfirmOpen) {
                    GymOutlineButton(
                        text = strings.profileLogout,
                        onClick = { isLogoutConfirmOpen = true },
                        fillWidth = true,
                        textColor = CrimsonRed
                    )
                } else {
                    GymCard(backgroundColor = CrimsonSubtle) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Akkauntdan chiqasizmi?"
                                "en" -> "Log out of account?"
                                else -> "Выйти из аккаунта?"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary
                        )
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
                                textColor = CrimsonRed
                            )
                        }
                    }
                }
            }
        }
    }

    if (state.isFeedbackModalOpen) {
        ModalBottomSheet(
            onDismissRequest = { onAction(ProfileUiAction.OnCloseFeedbackModal) },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .size(width = 40.dp, height = 4.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Fikr yoki taklif"
                            "en" -> "Leave feedback"
                            else -> "Отзыв или идея"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
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
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary
                    )
                    (1..5).forEach { star ->
                        IconButton(
                            onClick = { onAction(ProfileUiAction.OnSelectFeedbackRating(star)) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "$star",
                                tint = if (star <= state.feedbackRating) AmberWarm else TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
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

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceHigh)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "${item.userName.ifBlank { "Атлет" }} · $categoryLabel · ${item.createdAt.take(10)}",
                fontSize = 12.sp,
                color = AccentLime,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.message,
                fontSize = 14.sp,
                color = TextPrimary,
                lineHeight = 20.sp
            )
        }

        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(if (item.upvotedByMe) AccentLime else DarkSurfaceElevated)
                .clickable(onClick = onUpvote)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ThumbUp,
                contentDescription = null,
                tint = if (item.upvotedByMe) GymInkOnLight else TextSecondary,
                modifier = Modifier.size(14.dp)
            )
            Text(
                text = item.upvotes.toString(),
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (item.upvotedByMe) GymInkOnLight else TextPrimary
            )
        }
    }
}
