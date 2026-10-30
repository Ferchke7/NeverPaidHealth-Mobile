package dev.neverpaid.health.features.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AmberGold
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.CyanElectric
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldDark
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.PurpleAccent
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        ProfileScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    state: ProfileUiState,
    onAction: (ProfileUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val restOptions = listOf(60, 90, 120, 180)
    val languages = listOf("ru" to "Русский", "uz" to "O'zbek", "en" to "English")

    val filteredFeedback = state.feedbackList.filter { item ->
        state.selectedFeedbackFilter == "all" || item.category.equals(state.selectedFeedbackFilter, ignoreCase = true)
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // User Identity Card
        item(key = "user_identity_card") {
            AthleticCard(backgroundColor = DarkSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(CircleShape)
                                .background(EmeraldSubtle)
                                .border(1.dp, EmeraldNeon.copy(alpha = 0.4f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            val initials = state.userName.trim().take(2).uppercase()
                            if (initials.isNotEmpty()) {
                                Text(
                                    text = initials,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = EmeraldNeon
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = state.userName,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(EmeraldSubtle)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "PRO",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Black,
                                        color = EmeraldNeon
                                    )
                                }
                            }
                            if (state.userEmail.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = state.userEmail,
                                    fontSize = 12.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    OutlinedButton(
                        onClick = { onAction(ProfileUiAction.OnLogoutClick) },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = CrimsonRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = strings.profileLogout,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = CrimsonRed
                        )
                    }
                }
            }
        }

        // Status / Error Banner
        if (state.errorMessage != null || state.successMessage != null) {
            item(key = "status_banner") {
                val isError = state.errorMessage != null
                val bannerColor = if (isError) CrimsonRed else EmeraldNeon
                AthleticCard(backgroundColor = bannerColor.copy(alpha = 0.14f)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.errorMessage ?: state.successMessage.orEmpty(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = bannerColor,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { onAction(ProfileUiAction.OnDismissMessages) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = null,
                                tint = bannerColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // Live duda.uz Platform Stats Card
        item(key = "platform_stats_card") {
            val stats = state.platformStats
            AthleticCard(backgroundColor = DarkSurface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Public,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Экосистема duda.uz",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "Живая статистика сообщества атлетов",
                                    fontSize = 11.sp,
                                    color = TextMuted
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlatformStatPill(
                            label = "Атлетов",
                            value = stats.totalUsers.toString(),
                            color = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                        PlatformStatPill(
                            label = "Тренировок",
                            value = stats.totalWorkouts.toString(),
                            color = CyanElectric,
                            modifier = Modifier.weight(1f)
                        )
                        PlatformStatPill(
                            label = "Программ",
                            value = stats.totalPrograms.toString(),
                            color = AmberGold,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        PlatformStatPill(
                            label = "Блюд в логе",
                            value = stats.totalMeals.toString(),
                            color = PurpleAccent,
                            modifier = Modifier.weight(1f)
                        )
                        PlatformStatPill(
                            label = "AI разборов",
                            value = stats.totalAiQueries.toString(),
                            color = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                        val tons = (stats.totalVolumeKg / 1000.0).toInt()
                        PlatformStatPill(
                            label = "Тоннаж",
                            value = "${tons}т",
                            color = CyanElectric,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Daily Calorie, Macro & Water Targets Card
        item(key = "macro_targets_card") {
            AthleticCard(backgroundColor = DarkSurface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Restaurant,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Дневные цели КБЖУ и воды",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Синхронизируются с разделом Питание и AI Тренером",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.calorieTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateCalorieTarget(it)) },
                            label = { Text("Ккал / день", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors()
                        )
                        OutlinedTextField(
                            value = state.waterGoalInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateWaterGoal(it)) },
                            label = { Text("Вода (мл)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors()
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = state.proteinTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateProteinTarget(it)) },
                            label = { Text("Белки (г)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors()
                        )
                        OutlinedTextField(
                            value = state.carbsTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateCarbsTarget(it)) },
                            label = { Text("Углеводы (г)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors()
                        )
                        OutlinedTextField(
                            value = state.fatsTargetInput,
                            onValueChange = { onAction(ProfileUiAction.OnUpdateFatsTarget(it)) },
                            label = { Text("Жиры (г)", fontSize = 11.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            colors = profileTextFieldColors()
                        )
                    }

                    Button(
                        onClick = { onAction(ProfileUiAction.OnSaveMacroTargets) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon,
                            contentColor = DarkBackground
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Сохранить цели питания",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // Language Selection (RU / UZ / EN)
        item(key = "language_section") {
            AthleticCard(backgroundColor = DarkSurface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Language,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.profileLanguage,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        languages.forEachIndexed { index, (code, label) ->
                            val isSelected = state.languageCode.equals(code, ignoreCase = true)
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { onAction(ProfileUiAction.OnSelectLanguage(code)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = languages.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = EmeraldNeon,
                                    activeContentColor = DarkBackground,
                                    inactiveContainerColor = DarkSurfaceHigh,
                                    inactiveContentColor = TextSecondary
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }

        // Units System (KG / LB) & Rest Timer
        item(key = "units_and_timer_section") {
            AthleticCard(backgroundColor = DarkSurface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Scale,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.profileUnits,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    val units = listOf(true to "KG (Килограммы)", false to "LB (Фунты)")
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        units.forEachIndexed { index, (useKg, label) ->
                            val isSelected = state.useKilograms == useKg
                            SegmentedButton(
                                selected = isSelected,
                                onClick = { onAction(ProfileUiAction.OnToggleUnits(useKg)) },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = units.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = EmeraldNeon,
                                    activeContentColor = DarkBackground,
                                    inactiveContainerColor = DarkSurfaceHigh,
                                    inactiveContentColor = TextSecondary
                                )
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = strings.profileRestTimer,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        restOptions.forEach { secs ->
                            val isSelected = state.defaultRestSeconds == secs
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) EmeraldDark else DarkSurfaceHigh)
                                    .clickable { onAction(ProfileUiAction.OnSelectDefaultRest(secs)) }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${secs}s",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) TextPrimary else TextSecondary
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Vibration,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = strings.profileVibration,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }

                        Switch(
                            checked = state.vibrationEnabled,
                            onCheckedChange = { onAction(ProfileUiAction.OnToggleVibration(it)) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBackground,
                                checkedTrackColor = EmeraldNeon,
                                uncheckedThumbColor = TextMuted,
                                uncheckedTrackColor = DarkSurfaceHigh
                            )
                        )
                    }
                }
            }
        }

        // Community Feedback & Feature Request Board Header
        item(key = "feedback_board_header") {
            AthleticCard(backgroundColor = DarkSurface) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Отзывы и Идеи сообщества",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "Предлагайте функции, голосуйте и сообщайте о багах",
                                fontSize = 11.sp,
                                color = TextMuted
                            )
                        }
                        Button(
                            onClick = { onAction(ProfileUiAction.OnOpenFeedbackModal) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = EmeraldNeon,
                                contentColor = DarkBackground
                            ),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Оставить отзыв",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    val filters = listOf(
                        "all" to "Все (${state.feedbackList.size})",
                        "idea" to "Идеи",
                        "bug" to "Баги",
                        "praise" to "Отзывы"
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filters.forEach { (key, label) ->
                            val selected = state.selectedFeedbackFilter == key
                            FilterChip(
                                selected = selected,
                                onClick = { onAction(ProfileUiAction.OnSelectFeedbackFilter(key)) },
                                label = {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSubtle,
                                    selectedLabelColor = EmeraldNeon,
                                    containerColor = DarkSurfaceHigh,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }
                }
            }
        }

        if (filteredFeedback.isEmpty()) {
            item(key = "feedback_empty") {
                AthleticCard(backgroundColor = DarkSurface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Пока нет записей в этой категории",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextSecondary
                        )
                    }
                }
            }
        } else {
            items(filteredFeedback, key = { it.id }) { item ->
                FeedbackItemCard(
                    item = item,
                    onUpvote = { onAction(ProfileUiAction.OnToggleFeedbackUpvote(item.id)) }
                )
            }
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }

    // Feedback Submission ModalBottomSheet
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
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Оставить отзыв или предложение",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )

                val categories = listOf(
                    Triple("idea", "Идея", Icons.Default.Lightbulb),
                    Triple("bug", "Баг", Icons.Default.BugReport),
                    Triple("praise", "Отзыв", Icons.Default.Favorite)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { (catKey, catLabel, icon) ->
                        val selected = state.feedbackCategory == catKey
                        FilterChip(
                            selected = selected,
                            onClick = { onAction(ProfileUiAction.OnSelectFeedbackCategory(catKey)) },
                            leadingIcon = {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = catLabel,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldSubtle,
                                selectedLabelColor = EmeraldNeon,
                                selectedLeadingIconColor = EmeraldNeon,
                                containerColor = DarkSurfaceHigh,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }

                // Star Rating Row
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "Оценка:",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
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
                                tint = if (star <= state.feedbackRating) AmberGold else TextMuted.copy(alpha = 0.35f),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = state.feedbackMessage,
                    onValueChange = { onAction(ProfileUiAction.OnUpdateFeedbackMessage(it)) },
                    label = { Text("Опишите вашу идею, баг или впечатление...") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = profileTextFieldColors()
                )

                Button(
                    onClick = { onAction(ProfileUiAction.OnSubmitFeedback) },
                    enabled = !state.isSubmittingFeedback && state.feedbackMessage.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = EmeraldNeon,
                        contentColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text(
                        text = if (state.isSubmittingFeedback) "Отправка..." else "Отправить в duda.uz",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun PlatformStatPill(
    label: String,
    value: String,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurfaceHigh)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Black,
            color = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun FeedbackItemCard(
    item: FeedbackItem,
    onUpvote: () -> Unit
) {
    val categoryColor = when (item.category.lowercase()) {
        "bug" -> CrimsonRed
        "praise" -> AmberGold
        else -> EmeraldNeon
    }
    val categoryLabel = when (item.category.lowercase()) {
        "bug" -> "БАГ"
        "praise" -> "ОТЗЫВ"
        else -> "ИДЕЯ"
    }

    AthleticCard(backgroundColor = DarkSurface) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(EmeraldSubtle),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.userName.take(1).uppercase(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldNeon
                        )
                    }
                    Column {
                        Text(
                            text = item.userName.ifBlank { "Атлет" },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = item.createdAt.take(10),
                            fontSize = 10.sp,
                            color = TextMuted
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(categoryColor.copy(alpha = 0.16f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = categoryLabel,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = categoryColor
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Text(
                            text = item.rating.toString(),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }
            }

            Text(
                text = item.message,
                fontSize = 13.sp,
                color = TextPrimary,
                lineHeight = 18.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (item.upvotedByMe) EmeraldSubtle else DarkSurfaceHigh)
                        .clickable(onClick = onUpvote)
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ThumbUp,
                        contentDescription = null,
                        tint = if (item.upvotedByMe) EmeraldNeon else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = item.upvotes.toString(),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.upvotedByMe) EmeraldNeon else TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
private fun profileTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedContainerColor = DarkSurfaceVariant,
    unfocusedContainerColor = DarkSurfaceVariant,
    focusedBorderColor = EmeraldNeon,
    unfocusedBorderColor = BorderSubtle,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    focusedLabelColor = EmeraldNeon,
    unfocusedLabelColor = TextMuted
)
