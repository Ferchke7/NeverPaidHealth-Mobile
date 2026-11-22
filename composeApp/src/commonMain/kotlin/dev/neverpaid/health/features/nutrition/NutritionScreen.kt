package dev.neverpaid.health.features.nutrition

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.FloatingQuickDock
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymKicker
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InfoAccent
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.designsystem.rememberBase64ImageBitmap
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.platform.rememberImagePickerLauncher
import dev.neverpaid.health.domain.model.FastingProtocol
import dev.neverpaid.health.domain.model.FastingSession
import dev.neverpaid.health.domain.model.MealLog
import kotlinx.datetime.Clock
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.round

private enum class FoodInputMode {
    NONE,
    PHOTO,
    TEXT,
    MANUAL
}

private val mealTypeOptions = listOf(
    "breakfast" to "Завтрак",
    "lunch" to "Обед",
    "dinner" to "Ужин",
    "snack" to "Перекус"
)

private val quickFoodPrompts = listOf(
    "Куриная грудка 200г с рисом 150г",
    "Овсянка 80г с бананом и протеином",
    "Плов с говядиной 300г и салат",
    "Творог 5% 200г с орехами"
)

@Composable
fun NutritionRoute(
    viewModel: NutritionViewModel,
    onNavigateToProfile: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is NutritionUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        NutritionScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun NutritionScreen(
    state: NutritionUiState,
    onAction: (NutritionUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val tz = remember { TimeZone.currentSystemDefault() }
    val todayStr = remember { Clock.System.now().toLocalDateTime(tz).date.toString() }

    val pickPhoto = rememberImagePickerLauncher { base64, _ ->
        onAction(NutritionUiAction.OnPhotoPicked(base64))
    }

    var inputMode by remember { mutableStateOf(FoodInputMode.NONE) }
    var editingMealId by remember { mutableStateOf<String?>(null) }
    var draftPortionGrams by remember { mutableStateOf("200") }
    var isFoodCatalogSheetOpen by remember { mutableStateOf(false) }
    var showWaterResetConfirm by remember { mutableStateOf(false) }
    var showFastingWidget by remember { mutableStateOf(false) }

    var activeFastingSession by remember {
        mutableStateOf<FastingSession?>(
            FastingSession(
                id = "fasting_active",
                protocol = FastingProtocol.LEAN_GAINS_16_8,
                startTimeEpochMs = PlatformBridge.currentTimeMillis() - (14 * 3600 * 1000L),
                targetDurationHours = 16.0
            )
        )
    }

    val caloriesProgress = (state.totalCalories.toFloat() / state.targetCalories.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val proteinProgress = (state.totalProtein.toFloat() / state.targetProtein.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val carbsProgress = (state.totalCarbs.toFloat() / state.targetCarbs.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val fatsProgress = (state.totalFats.toFloat() / state.targetFats.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val waterProgress = (state.waterMl.toFloat() / state.targetWaterMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)

    if (isFoodCatalogSheetOpen) {
        FoodDatabaseBottomSheet(
            isOpen = true,
            onDismiss = { isFoodCatalogSheetOpen = false },
            onFoodSelected = { name, cal, prot, carb, fat ->
                onAction(NutritionUiAction.OnMealTypeChanged(state.selectedMealType.ifBlank { "lunch" }))
                onAction(NutritionUiAction.OnMealNameChanged(name))
                onAction(NutritionUiAction.OnMealCaloriesChanged(cal.toInt().toString()))
                onAction(NutritionUiAction.OnMealProteinChanged(prot.toInt().toString()))
                onAction(NutritionUiAction.OnMealFatsChanged(fat.toInt().toString()))
                onAction(NutritionUiAction.OnMealCarbsChanged(carb.toInt().toString()))
                onAction(NutritionUiAction.OnSaveMeal)
                isFoodCatalogSheetOpen = false
            }
        )
    }

    if (state.isLoading && state.meals.isEmpty() && state.totalCalories == 0) {
        LazyColumn(
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "header") {
                GymPageHeader(
                    title = "Питание",
                    subtitle = "Калории, макросы и гидратация"
                )
            }
            item(key = "loading") {
                GymLoadingCard(
                    title = "Питание",
                    hint = strings.stateLoadingHint
                )
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 20.dp,
            end = 20.dp,
            top = 8.dp,
            bottom = 110.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        item(key = "page_header", contentType = "header") {
            GymPageHeader(
                title = "Питание",
                subtitle = formatFullDateWithWeekday(state.selectedDate)
            )
        }

        // Date & Fasting Strip
        item(key = "date_nav_block", contentType = "date_nav") {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item(key = "prev_day") {
                    GymPillTab(
                        text = "← Назад",
                        selected = false,
                        onClick = { onAction(NutritionUiAction.OnShiftDateDays(-1)) }
                    )
                }
                item(key = "today") {
                    GymPillTab(
                        text = "Сегодня",
                        selected = state.selectedDate == todayStr || state.selectedDate.isBlank(),
                        onClick = { onAction(NutritionUiAction.OnSelectDate(todayStr)) }
                    )
                }
                item(key = "next_day") {
                    GymPillTab(
                        text = "Вперёд →",
                        selected = false,
                        onClick = { onAction(NutritionUiAction.OnShiftDateDays(1)) }
                    )
                }
                item(key = "fasting_toggle") {
                    GymPillTab(
                        text = "Голодание 16/8",
                        selected = showFastingWidget,
                        onClick = { showFastingWidget = !showFastingWidget }
                    )
                }
            }
        }

        // Hero Card: Royal Violet Daily Calorie & Macros Summary (Figma Screen 06)
        item(key = "daily_balance_card", contentType = "balance") {
            val remainingKcal = (state.targetCalories - state.totalCalories).coerceAtLeast(0)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RoyalHeroCard(bottomExtraPadding = 22.dp) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "DUDA · NUTRITION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp,
                            color = HeroMuted
                        )
                        Box(
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(InkWhite.copy(alpha = 0.18f))
                                .padding(horizontal = 12.dp, vertical = 5.dp)
                        ) {
                            Text(
                                text = "Осталось $remainingKcal ккал",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = InkWhite,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = formatSpacedInt(state.totalCalories),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-1.5).sp,
                            color = InkWhite,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                        Text(
                            text = "/ ${formatSpacedInt(state.targetCalories)} ккал",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HeroMuted,
                            style = TextStyle(fontFeatureSettings = "tnum"),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    GymProgressBar(
                        progress = caloriesProgress,
                        heightDp = 9,
                        color = InkWhite,
                        trackColor = InkWhite.copy(alpha = 0.22f)
                    )
                }

                FloatingQuickDock {
                    MacroSummaryTile(
                        label = "Белки",
                        current = state.totalProtein,
                        target = state.targetProtein,
                        progress = proteinProgress,
                        barColor = AccentLime,
                        modifier = Modifier.weight(1f)
                    )
                    MacroSummaryTile(
                        label = "Жиры",
                        current = state.totalFats,
                        target = state.targetFats,
                        progress = fatsProgress,
                        barColor = AmberWarm,
                        modifier = Modifier.weight(1f)
                    )
                    MacroSummaryTile(
                        label = "Углеводы",
                        current = state.totalCarbs,
                        target = state.targetCarbs,
                        progress = carbsProgress,
                        barColor = InfoAccent,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Toggleable Fasting Tracker Card
        if (showFastingWidget) {
            item(key = "fasting_section", contentType = "fasting") {
                FastingTrackerView(
                    activeSession = activeFastingSession,
                    onStartFast = { protocol ->
                        activeFastingSession = FastingSession(
                            id = PlatformBridge.generateUuid(),
                            protocol = protocol,
                            startTimeEpochMs = PlatformBridge.currentTimeMillis(),
                            targetDurationHours = protocol.fastHours.toDouble()
                        )
                    },
                    onEndFast = { activeFastingSession = null }
                )
            }
        }

        // Card 2: Clean 2x2 Selector for Adding Food
        item(key = "add_food_card", contentType = "add_food") {
            GymCard {
                GymSectionTitle(title = "Добавить еду")

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AddFoodModeTile(
                            title = "По фото",
                            subtitle = "ИИ-анализ блюда",
                            selected = inputMode == FoodInputMode.PHOTO,
                            onClick = {
                                inputMode = if (inputMode == FoodInputMode.PHOTO) FoodInputMode.NONE else FoodInputMode.PHOTO
                            },
                            modifier = Modifier.weight(1f)
                        )
                        AddFoodModeTile(
                            title = "Текстом",
                            subtitle = "Описать словами",
                            selected = inputMode == FoodInputMode.TEXT,
                            onClick = {
                                inputMode = if (inputMode == FoodInputMode.TEXT) FoodInputMode.NONE else FoodInputMode.TEXT
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        AddFoodModeTile(
                            title = "База продуктов",
                            subtitle = "Каталог 150+ блюд",
                            selected = isFoodCatalogSheetOpen,
                            onClick = { isFoodCatalogSheetOpen = true },
                            modifier = Modifier.weight(1f)
                        )
                        AddFoodModeTile(
                            title = "Вручную",
                            subtitle = "Точный ввод КБЖУ",
                            selected = inputMode == FoodInputMode.MANUAL,
                            onClick = {
                                editingMealId = null
                                inputMode = if (inputMode == FoodInputMode.MANUAL) FoodInputMode.NONE else FoodInputMode.MANUAL
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Card 3: Draft / AI Recognition Card
        if (inputMode != FoodInputMode.NONE || state.aiAnalysisPreview != null) {
            item(key = "draft_card", contentType = "draft") {
                val preview = state.aiAnalysisPreview
                val showFormFields = preview != null || inputMode == FoodInputMode.MANUAL || editingMealId != null

                GymCard(borderColor = BorderHighlight) {
                    GymKicker(
                        text = if (preview != null) "ИИ-распознавание" else "Черновик"
                    )

                    Text(
                        text = if (editingMealId != null) {
                            "Редактирование записи"
                        } else if (showFormFields) {
                            "Проверьте черновик"
                        } else if (inputMode == FoodInputMode.PHOTO) {
                            "Распознать по фото"
                        } else {
                            "Распознать по описанию"
                        },
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp,
                        color = TextPrimary
                    )

                    if (preview != null && preview.aiNotes.isNotBlank()) {
                        Text(
                            text = preview.aiNotes,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }

                    // Meal type selector
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val activeType = if (showFormFields && preview == null) state.newMealType else state.selectedMealType
                        mealTypeOptions.forEach { (typeKey, typeLabel) ->
                            GymPillTab(
                                text = typeLabel,
                                selected = activeType == typeKey,
                                onClick = {
                                    onAction(NutritionUiAction.OnSelectAnalyzerMealType(typeKey))
                                    onAction(NutritionUiAction.OnMealTypeChanged(typeKey))
                                }
                            )
                        }
                    }

                    if (!showFormFields && inputMode == FoodInputMode.PHOTO) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymOutlineButton(
                                text = if (state.selectedPhotoBase64 != null) "Фото выбрано ✓" else "Выбрать фото",
                                onClick = pickPhoto
                            )
                            if (state.selectedPhotoBase64 != null) {
                                GymOutlineButton(
                                    text = "Убрать фото",
                                    onClick = { onAction(NutritionUiAction.OnClearPhoto) }
                                )
                            }
                        }
                        if (state.selectedPhotoBase64 != null) {
                            val selectedBitmap = rememberBase64ImageBitmap(state.selectedPhotoBase64)
                            if (selectedBitmap != null) {
                                Image(
                                    bitmap = selectedBitmap,
                                    contentDescription = "Выбранное фото блюда",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(180.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(GymWell)
                                )
                            }
                        }
                        GymLabeledField(
                            label = "Комментарий (необязательно)",
                            value = state.aiDescriptionInput,
                            onValueChange = { onAction(NutritionUiAction.OnAiDescriptionChanged(it)) },
                            placeholder = "Например: 200г курицы без кожи и рис"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GymPrimaryButton(
                                text = if (state.isAiAnalyzing) "Анализ..." else "Рассчитать КБЖУ",
                                enabled = !state.isAiAnalyzing && (state.selectedPhotoBase64 != null || state.aiDescriptionInput.isNotBlank()),
                                onClick = { onAction(NutritionUiAction.OnRunAiAnalysis) },
                                modifier = Modifier.weight(1.3f)
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = { inputMode = FoodInputMode.NONE },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (!showFormFields && inputMode == FoodInputMode.TEXT) {
                        GymLabeledField(
                            label = "Опишите блюдо",
                            value = state.aiDescriptionInput,
                            onValueChange = { onAction(NutritionUiAction.OnAiDescriptionChanged(it)) },
                            placeholder = "Овсянка 80г на молоке с бананом",
                            singleLine = false
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            quickFoodPrompts.forEach { prompt ->
                                GymPillTab(
                                    text = prompt,
                                    selected = state.aiDescriptionInput == prompt,
                                    onClick = { onAction(NutritionUiAction.OnAiDescriptionChanged(prompt)) }
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GymPrimaryButton(
                                text = if (state.isAiAnalyzing) "Анализ..." else "Рассчитать КБЖУ",
                                enabled = !state.isAiAnalyzing && state.aiDescriptionInput.isNotBlank(),
                                onClick = { onAction(NutritionUiAction.OnRunAiAnalysis) },
                                modifier = Modifier.weight(1.3f)
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = { inputMode = FoodInputMode.NONE },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    } else if (showFormFields) {
                        GymLabeledField(
                            label = "Название",
                            value = state.newMealName,
                            onValueChange = { onAction(NutritionUiAction.OnMealNameChanged(it)) },
                            placeholder = "Йогурт с ягодами"
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymLabeledField(
                                label = "Порция, г",
                                value = draftPortionGrams,
                                onValueChange = { draftPortionGrams = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = "Калории, ккал",
                                value = state.newMealCalories,
                                onValueChange = { onAction(NutritionUiAction.OnMealCaloriesChanged(it.filter { c -> c.isDigit() })) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymLabeledField(
                                label = "Белки, г",
                                value = state.newMealProtein,
                                onValueChange = { onAction(NutritionUiAction.OnMealProteinChanged(it.filter { c -> c.isDigit() || c == '.' })) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = "Углеводы, г",
                                value = state.newMealCarbs,
                                onValueChange = { onAction(NutritionUiAction.OnMealCarbsChanged(it.filter { c -> c.isDigit() || c == '.' })) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = "Жиры, г",
                                value = state.newMealFats,
                                onValueChange = { onAction(NutritionUiAction.OnMealFatsChanged(it.filter { c -> c.isDigit() || c == '.' })) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GymPrimaryButton(
                                text = if (state.isSavingMeal) "Сохранение..." else "Подтвердить",
                                enabled = !state.isSavingMeal && state.newMealName.isNotBlank() && state.newMealCalories.isNotBlank(),
                                onClick = {
                                    val oldId = editingMealId
                                    if (preview != null) {
                                        onAction(NutritionUiAction.OnConfirmAiPreview)
                                    } else {
                                        onAction(NutritionUiAction.OnSaveMeal)
                                    }
                                    if (!oldId.isNullOrBlank()) {
                                        onAction(NutritionUiAction.OnDeleteMeal(oldId))
                                    }
                                    editingMealId = null
                                    inputMode = FoodInputMode.NONE
                                },
                                modifier = Modifier.weight(1.3f)
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = {
                                    editingMealId = null
                                    if (preview != null) onAction(NutritionUiAction.OnDismissAiPreview)
                                    inputMode = FoodInputMode.NONE
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }

        // Card 4: Meals Log
        item(key = "meals_card", contentType = "meals") {
            GymCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Приёмы пищи",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        color = TextPrimary
                    )
                    Text(
                        text = "${state.meals.size} · ${formatSpacedInt(state.totalCalories)} ккал",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }

                if (state.meals.isEmpty()) {
                    GymInnerWell {
                        Text(
                            text = "Записей за этот день пока нет",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Добавьте блюдо по фото, текстом, из базы или вручную.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        state.meals.forEach { meal ->
                            MealLogRowItem(
                                meal = meal,
                                tz = tz,
                                onEdit = {
                                    editingMealId = meal.id
                                    onAction(NutritionUiAction.OnMealTypeChanged(meal.mealType))
                                    onAction(NutritionUiAction.OnMealNameChanged(meal.name))
                                    onAction(NutritionUiAction.OnMealCaloriesChanged(meal.calories.toString()))
                                    onAction(NutritionUiAction.OnMealProteinChanged(meal.proteinG.toInt().toString()))
                                    onAction(NutritionUiAction.OnMealCarbsChanged(meal.carbsG.toInt().toString()))
                                    onAction(NutritionUiAction.OnMealFatsChanged(meal.fatsG.toInt().toString()))
                                    inputMode = FoodInputMode.MANUAL
                                },
                                onRepeat = {
                                    onAction(NutritionUiAction.OnMealTypeChanged(meal.mealType))
                                    onAction(NutritionUiAction.OnMealNameChanged(meal.name))
                                    onAction(NutritionUiAction.OnMealCaloriesChanged(meal.calories.toString()))
                                    onAction(NutritionUiAction.OnMealProteinChanged(meal.proteinG.toInt().toString()))
                                    onAction(NutritionUiAction.OnMealCarbsChanged(meal.carbsG.toInt().toString()))
                                    onAction(NutritionUiAction.OnMealFatsChanged(meal.fatsG.toInt().toString()))
                                    onAction(NutritionUiAction.OnSaveMeal)
                                },
                                onDelete = { onAction(NutritionUiAction.OnDeleteMeal(meal.id)) }
                            )
                        }
                    }
                }
            }
        }

        // Card 5: Hydration
        item(key = "water_card", contentType = "water") {
            val portions = state.waterMl / 250
            GymCard {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Вода",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.4).sp,
                        color = TextPrimary
                    )
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(DarkSurfaceHigh)
                            .padding(horizontal = 12.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = "$portions × 250 мл",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextSecondary,
                            style = TextStyle(fontFeatureSettings = "tnum")
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = formatLiters(state.waterMl),
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = (-1).sp,
                        color = TextPrimary,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = "/ ${formatLiters(state.targetWaterMl)} л",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        style = TextStyle(fontFeatureSettings = "tnum"),
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                GymProgressBar(
                    progress = waterProgress,
                    heightDp = 8,
                    color = InfoAccent
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymPrimaryButton(
                        text = "+ 250 мл",
                        onClick = { onAction(NutritionUiAction.OnAddWater(250)) },
                        modifier = Modifier.weight(1.3f)
                    )
                    GymOutlineButton(
                        text = "− 250",
                        enabled = state.waterMl >= 250,
                        onClick = { onAction(NutritionUiAction.OnAddWater(-250)) },
                        modifier = Modifier.weight(1f)
                    )
                    GymOutlineButton(
                        text = "Сброс",
                        enabled = state.waterMl > 0,
                        onClick = { showWaterResetConfirm = true },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (showWaterResetConfirm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showWaterResetConfirm = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 4.dp)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(CircleShape)
                        .background(DarkSurfaceElevated)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                GymKicker(text = "Гидратация")
                Text(
                    text = "Сбросить счётчик воды за ${state.selectedDate}?",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GymOutlineButton(
                        text = strings.cancel,
                        onClick = { showWaterResetConfirm = false },
                        modifier = Modifier.weight(1f)
                    )
                    GymPrimaryButton(
                        text = "Сбросить",
                        onClick = {
                            if (state.waterMl > 0) {
                                onAction(NutritionUiAction.OnAddWater(-state.waterMl))
                            }
                            showWaterResetConfirm = false
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun MacroSummaryTile(
    label: String,
    current: Int,
    target: Int,
    progress: Float,
    barColor: Color,
    modifier: Modifier = Modifier
) {
    val clamped = progress.coerceIn(0f, 1f)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceHigh)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "$current",
                fontSize = 20.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-0.5).sp,
                color = TextPrimary,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
            Text(
                text = " / $target г",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                style = TextStyle(fontFeatureSettings = "tnum"),
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(DarkSurface)
        ) {
            if (clamped > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clamped)
                        .height(6.dp)
                        .clip(CircleShape)
                        .background(barColor)
                )
            }
        }
    }
}

@Composable
private fun AddFoodModeTile(
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (selected) AccentLime else DarkSurfaceHigh)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.ExtraBold,
            color = if (selected) GymInkOnLight else TextPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = if (selected) GymInkOnLight.copy(alpha = 0.75f) else TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun MealLogRowItem(
    meal: MealLog,
    tz: TimeZone,
    onEdit: () -> Unit,
    onRepeat: () -> Unit,
    onDelete: () -> Unit
) {
    val mealTypeRu = mealTypeOptions.find { it.first.equals(meal.mealType, ignoreCase = true) }?.second ?: meal.mealType
    val timeStr = remember(meal.loggedAt) {
        runCatching {
            val ldt = Instant.parse(meal.loggedAt).toLocalDateTime(tz)
            "${ldt.hour.toString().padStart(2, '0')}:${ldt.minute.toString().padStart(2, '0')}"
        }.getOrNull() ?: ""
    }
    val metaLine = if (timeStr.isNotBlank()) "$mealTypeRu · $timeStr" else mealTypeRu

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceHigh)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = metaLine,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = meal.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = "${meal.calories}",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.5).sp,
                    color = AccentLime,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "ккал",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 3.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealMacroChip(text = "Б ${meal.proteinG.toInt()} г")
            MealMacroChip(text = "У ${meal.carbsG.toInt()} г")
            MealMacroChip(text = "Ж ${meal.fatsG.toInt()} г")
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            MealActionChip(text = "Изменить", onClick = onEdit)
            MealActionChip(text = "Повторить", onClick = onRepeat)
            MealActionChip(text = "Удалить", onClick = onDelete, isDestructive = true)
        }
    }
}

@Composable
private fun MealMacroChip(text: String) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 4.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextSecondary,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}

@Composable
private fun MealActionChip(
    text: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false
) {
    Box(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (isDestructive) CrimsonRed.copy(alpha = 0.14f) else DarkSurface)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 7.dp)
    ) {
        Text(
            text = text,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = if (isDestructive) CrimsonRed else TextSecondary
        )
    }
}

private fun formatFullDateWithWeekday(isoDate: String): String {
    return runCatching {
        val d = LocalDate.parse(isoDate)
        val weekday = when (d.dayOfWeek) {
            DayOfWeek.MONDAY -> "Понедельник"
            DayOfWeek.TUESDAY -> "Вторник"
            DayOfWeek.WEDNESDAY -> "Среда"
            DayOfWeek.THURSDAY -> "Четверг"
            DayOfWeek.FRIDAY -> "Пятница"
            DayOfWeek.SATURDAY -> "Суббота"
            DayOfWeek.SUNDAY -> "Воскресенье"
            else -> ""
        }
        val months = listOf(
            "января", "февраля", "марта", "апреля", "мая", "июня",
            "июля", "августа", "сентября", "октября", "ноября", "декабря"
        )
        val month = months.getOrElse(d.monthNumber - 1) { "" }
        "$weekday, ${d.dayOfMonth} $month ${d.year}"
    }.getOrElse { isoDate }
}

private fun formatLiters(ml: Int): String {
    val l = round((ml / 1000.0) * 10.0) / 10.0
    return l.toString().replace('.', ',')
}

private fun formatSpacedInt(value: Int): String {
    val raw = value.toString()
    if (raw.length <= 3) return raw
    val sb = StringBuilder()
    var count = 0
    for (i in raw.length - 1 downTo 0) {
        sb.append(raw[i])
        count++
        if (count % 3 == 0 && i > 0) sb.append(' ')
    }
    return sb.reverse().toString()
}
