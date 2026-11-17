package dev.neverpaid.health.features.nutrition

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.rememberBase64ImageBitmap
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymWhiteButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "header") {
                GymPageHeader(
                    title = "Питание",
                    subtitle = "Приёмы пищи, макросы и вода"
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
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "page_header", contentType = "header") {
            GymPageHeader(
                title = "Питание",
                subtitle = "Приёмы пищи, макросы и вода"
            )
        }

        // Date Header & Nav
        item(key = "date_nav_block", contentType = "date_nav") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = formatFullDateWithWeekday(state.selectedDate),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymOutlineButton(
                        text = "← Назад",
                        onClick = { onAction(NutritionUiAction.OnShiftDateDays(-1)) }
                    )
                    GymOutlineButton(
                        text = "Сегодня",
                        onClick = { onAction(NutritionUiAction.OnSelectDate(todayStr)) }
                    )
                    GymOutlineButton(
                        text = "Вперёд →",
                        onClick = { onAction(NutritionUiAction.OnShiftDateDays(1)) }
                    )
                }

                Text(
                    text = "Часовой пояс профиля · ${tz.id}",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        // Card 1: Daily Balance
        item(key = "daily_balance_card", contentType = "balance") {
            GymCard {
                Text(
                    text = "Дневной баланс",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                BalanceMetricRow(
                    headline = "${state.totalCalories} / ${state.targetCalories} ккал",
                    label = "Энергия",
                    progress = caloriesProgress
                )
                Spacer(modifier = Modifier.height(14.dp))
                BalanceMetricRow(
                    headline = "${state.totalProtein} / ${state.targetProtein} г",
                    label = "Белки",
                    progress = proteinProgress
                )
                Spacer(modifier = Modifier.height(14.dp))
                BalanceMetricRow(
                    headline = "${state.totalCarbs} / ${state.targetCarbs} г",
                    label = "Углеводы",
                    progress = carbsProgress
                )
                Spacer(modifier = Modifier.height(14.dp))
                BalanceMetricRow(
                    headline = "${state.totalFats} / ${state.targetFats} г",
                    label = "Жиры",
                    progress = fatsProgress
                )
            }
        }

        // Card 2: Meals Log (Divided List)
        item(key = "meals_card", contentType = "meals") {
            GymCard {
                Text(
                    text = "Приёмы пищи",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${state.meals.size} записи · ${formatSpacedInt(state.totalCalories)} ккал",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(16.dp))

                if (state.meals.isEmpty()) {
                    GymInnerWell {
                        Text(
                            text = "Записей за ${state.selectedDate} пока нет",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Выберите способ добавления еды ниже — по фото, текстом или вручную.",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                } else {
                    state.meals.forEachIndexed { idx, meal ->
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
                        if (idx < state.meals.lastIndex) {
                            Spacer(modifier = Modifier.height(16.dp))
                            GymDivider()
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        // Card 3: Hydration
        item(key = "water_card", contentType = "water") {
            val portions = state.waterMl / 250
            GymCard {
                Text(
                    text = "Вода",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "${formatLiters(state.waterMl)} / ${formatLiters(state.targetWaterMl)} л",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$portions порций по 250 мл",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                GymProgressBar(
                    progress = waterProgress,
                    color = AccentLime
                )
                Spacer(modifier = Modifier.height(16.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymPrimaryButton(
                        text = "+ 250 мл",
                        onClick = { onAction(NutritionUiAction.OnAddWater(250)) }
                    )
                    GymOutlineButton(
                        text = "− 250 мл",
                        enabled = state.waterMl >= 250,
                        onClick = { onAction(NutritionUiAction.OnAddWater(-250)) }
                    )
                    GymOutlineButton(
                        text = "Сбросить",
                        enabled = state.waterMl > 0,
                        onClick = { showWaterResetConfirm = true }
                    )
                }
            }
        }

        // Card 4: Add Food Mode Picker
        item(key = "add_food_card", contentType = "add_food") {
            GymCard {
                Text(
                    text = "Добавить еду",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Выберите удобный способ ввода",
                    fontSize = 13.sp,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymPillTab(
                        text = "По фото",
                        selected = inputMode == FoodInputMode.PHOTO,
                        onClick = {
                            inputMode = if (inputMode == FoodInputMode.PHOTO) FoodInputMode.NONE else FoodInputMode.PHOTO
                        }
                    )
                    GymPillTab(
                        text = "Текстом",
                        selected = inputMode == FoodInputMode.TEXT,
                        onClick = {
                            inputMode = if (inputMode == FoodInputMode.TEXT) FoodInputMode.NONE else FoodInputMode.TEXT
                        }
                    )
                    GymPillTab(
                        text = "Вручную",
                        selected = inputMode == FoodInputMode.MANUAL,
                        onClick = {
                            editingMealId = null
                            inputMode = if (inputMode == FoodInputMode.MANUAL) FoodInputMode.NONE else FoodInputMode.MANUAL
                        }
                    )
                    GymOutlineButton(
                        text = "База продуктов",
                        onClick = { isFoodCatalogSheetOpen = true }
                    )
                }
            }
        }

        // Card 5: Draft / AI Recognition Card (1:1 Figma 08 Bottom Card)
        if (inputMode != FoodInputMode.NONE || state.aiAnalysisPreview != null) {
            item(key = "draft_card", contentType = "draft") {
                val preview = state.aiAnalysisPreview
                val showFormFields = preview != null || inputMode == FoodInputMode.MANUAL || editingMealId != null

                GymCard {
                    Text(
                        text = if (preview != null) "ИИ-распознавание" else "Черновик",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = AccentLime,
                        letterSpacing = 0.8.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
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
                        fontSize = 20.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (preview != null && preview.aiNotes.isNotBlank()) {
                            preview.aiNotes
                        } else if (showFormFields) {
                            "Проверьте продукты, порцию и КБЖУ перед добавлением."
                        } else {
                            "Прикрепите фото или опишите блюдо — ИИ рассчитает калории и макросы."
                        },
                        fontSize = 13.sp,
                        color = TextSecondary
                    )

                    Spacer(modifier = Modifier.height(14.dp))

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
                        Spacer(modifier = Modifier.height(14.dp))
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
                                Spacer(modifier = Modifier.height(10.dp))
                                Image(
                                    bitmap = selectedBitmap,
                                    contentDescription = "Выбранное фото блюда",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(160.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(GymWell)
                                        .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        GymLabeledField(
                            label = "Комментарий (необязательно)",
                            value = state.aiDescriptionInput,
                            onValueChange = { onAction(NutritionUiAction.OnAiDescriptionChanged(it)) },
                            placeholder = "Например: 200г курицы без кожи и рис"
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPrimaryButton(
                                text = if (state.isAiAnalyzing) "Анализ..." else "Рассчитать КБЖУ",
                                enabled = !state.isAiAnalyzing && (state.selectedPhotoBase64 != null || state.aiDescriptionInput.isNotBlank()),
                                onClick = { onAction(NutritionUiAction.OnRunAiAnalysis) }
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = { inputMode = FoodInputMode.NONE }
                            )
                        }
                    } else if (!showFormFields && inputMode == FoodInputMode.TEXT) {
                        Spacer(modifier = Modifier.height(14.dp))
                        GymLabeledField(
                            label = "Опишите блюдо",
                            value = state.aiDescriptionInput,
                            onValueChange = { onAction(NutritionUiAction.OnAiDescriptionChanged(it)) },
                            placeholder = "Овсянка 80г на молоке с бананом",
                            singleLine = false
                        )
                        Spacer(modifier = Modifier.height(10.dp))
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
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPrimaryButton(
                                text = if (state.isAiAnalyzing) "Анализ..." else "Рассчитать КБЖУ",
                                enabled = !state.isAiAnalyzing && state.aiDescriptionInput.isNotBlank(),
                                onClick = { onAction(NutritionUiAction.OnRunAiAnalysis) }
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = { inputMode = FoodInputMode.NONE }
                            )
                        }
                    } else if (showFormFields) {
                        Spacer(modifier = Modifier.height(14.dp))
                        GymLabeledField(
                            label = "Название",
                            value = state.newMealName,
                            onValueChange = { onAction(NutritionUiAction.OnMealNameChanged(it)) },
                            placeholder = "Йогурт с ягодами"
                        )
                        Spacer(modifier = Modifier.height(12.dp))
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
                        Spacer(modifier = Modifier.height(12.dp))
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
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            GymLabeledField(
                                label = "Жиры, г",
                                value = state.newMealFats,
                                onValueChange = { onAction(NutritionUiAction.OnMealFatsChanged(it.filter { c -> c.isDigit() || c == '.' })) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.weight(1f))
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = "Черновик ещё не включён в дневной итог.",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
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
                                }
                            )
                            GymOutlineButton(
                                text = strings.cancel,
                                onClick = {
                                    editingMealId = null
                                    if (preview != null) onAction(NutritionUiAction.OnDismissAiPreview)
                                    inputMode = FoodInputMode.NONE
                                }
                            )
                        }
                    }
                }
            }
        }

        // Toggleable Fasting Tracker Card (preserves full fasting workflow)
        item(key = "fasting_section", contentType = "fasting") {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                GymOutlineButton(
                    text = if (showFastingWidget) "Скрыть интервальное голодание" else "Интервальное голодание 16/8",
                    onClick = { showFastingWidget = !showFastingWidget }
                )
                AnimatedVisibility(visible = showFastingWidget) {
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
        }
    }

    if (showWaterResetConfirm) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ModalBottomSheet(
            onDismissRequest = { showWaterResetConfirm = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            contentColor = TextPrimary
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "СБРОС ВОДЫ",
                    fontSize = 11.sp,
                    color = TextMuted,
                    letterSpacing = 0.8.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Сбросить счётчик воды за ${state.selectedDate}?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(14.dp))
                GymDivider()
                Spacer(modifier = Modifier.height(14.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymOutlineButton(
                        text = strings.cancel,
                        onClick = { showWaterResetConfirm = false }
                    )
                    GymWhiteButton(
                        text = "Сбросить",
                        onClick = {
                            if (state.waterMl > 0) {
                                onAction(NutritionUiAction.OnAddWater(-state.waterMl))
                            }
                            showWaterResetConfirm = false
                        }
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun BalanceMetricRow(
    headline: String,
    label: String,
    progress: Float
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = headline,
            fontSize = 24.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = label,
            fontSize = 13.sp,
            color = TextSecondary
        )
        GymProgressBar(
            progress = progress,
            color = AccentLime
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
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

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = metaLine,
            fontSize = 12.sp,
            color = TextMuted
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = meal.name,
            fontSize = 17.sp,
            fontWeight = FontWeight.Medium,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${meal.calories} ккал",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Б ${meal.proteinG.toInt()} г · У ${meal.carbsG.toInt()} г · Ж ${meal.fatsG.toInt()} г",
            fontSize = 13.sp,
            color = TextSecondary
        )
        Spacer(modifier = Modifier.height(12.dp))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            GymOutlineButton(text = "Изменить", onClick = onEdit)
            GymOutlineButton(text = "Повторить", onClick = onRepeat)
            GymOutlineButton(text = "Удалить", onClick = onDelete)
        }
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
