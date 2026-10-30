package dev.neverpaid.health.features.nutrition

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CyanElectric
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.IndigoSubtle
import dev.neverpaid.health.core.designsystem.SecondaryAthleticButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.designsystem.MacronutrientDonutGauge
import dev.neverpaid.health.core.designsystem.MacroTargetProgress
import dev.neverpaid.health.core.designsystem.WaterHydrationWave
import dev.neverpaid.health.domain.model.FastingProtocol
import dev.neverpaid.health.domain.model.FastingSession
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.platform.rememberImagePickerLauncher

private val mealTypeOptions = listOf(
    "breakfast" to "Завтрак",
    "lunch" to "Обед",
    "dinner" to "Ужин",
    "snack" to "Перекус"
)

private val quickFoodPrompts = listOf(
    "Куриная грудка 200г с рисом 150г и овощами",
    "Овсянка 80г на молоке с бананом и скуп протеина",
    "Плов с говядиной 300г и салат ачучук",
    "3 жареных яйца, 2 тоста и авокадо",
    "Творог 5% 200г с мёдом и орехами",
    "Протеиновый коктейль на воде + банан"
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

@Composable
fun NutritionScreen(
    state: NutritionUiState,
    onAction: (NutritionUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val pickPhoto = rememberImagePickerLauncher { base64, _ ->
        onAction(NutritionUiAction.OnPhotoPicked(base64))
    }

    var isFoodCatalogSheetOpen by remember { mutableStateOf(false) }
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

    if (state.isAddMealSheetOpen) {
        AddMealBottomSheet(
            mealType = state.newMealType,
            mealName = state.newMealName,
            calories = state.newMealCalories,
            protein = state.newMealProtein,
            fats = state.newMealFats,
            carbs = state.newMealCarbs,
            onMealTypeChanged = { onAction(NutritionUiAction.OnMealTypeChanged(it)) },
            onMealNameChanged = { onAction(NutritionUiAction.OnMealNameChanged(it)) },
            onCaloriesChanged = { onAction(NutritionUiAction.OnMealCaloriesChanged(it)) },
            onProteinChanged = { onAction(NutritionUiAction.OnMealProteinChanged(it)) },
            onFatsChanged = { onAction(NutritionUiAction.OnMealFatsChanged(it)) },
            onCarbsChanged = { onAction(NutritionUiAction.OnMealCarbsChanged(it)) },
            onSave = { onAction(NutritionUiAction.OnSaveMeal) },
            onDismiss = { onAction(NutritionUiAction.OnCloseAddMealSheet) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Питание и ИИ-Анализ КБЖУ",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Сфотографируйте блюдо или опишите текстом для мгновенного расчёта макросов",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }

        // Date Switcher Bar
        item(key = "date_switcher") {
            AthleticCard(backgroundColor = DarkSurface) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { onAction(NutritionUiAction.OnShiftDateDays(-1)) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Пред. день",
                            tint = TextPrimary
                        )
                    }
                    Text(
                        text = "📅 ${state.selectedDate}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                    IconButton(onClick = { onAction(NutritionUiAction.OnShiftDateDays(1)) }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = "След. день",
                            tint = TextPrimary
                        )
                    }
                }
            }
        }

        // Concentric Macronutrient Donut Gauge
        item(key = "energy_donut_gauge") {
            MacronutrientDonutGauge(
                progress = MacroTargetProgress(
                    currentCalories = state.totalCalories.toDouble(),
                    targetCalories = state.targetCalories.toDouble(),
                    currentProteinGrams = state.totalProtein.toDouble(),
                    targetProteinGrams = state.targetProtein.toDouble(),
                    currentCarbsGrams = state.totalCarbs.toDouble(),
                    targetCarbsGrams = state.targetCarbs.toDouble(),
                    currentFatsGrams = state.totalFats.toDouble(),
                    targetFatsGrams = state.targetFats.toDouble()
                )
            )
        }

        // Energy & Macros Card
        item(key = "energy_hero") {
            AthleticCard(
                backgroundColor = DarkSurfaceVariant,
                borderColor = BorderHighlight
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "КАЛОРИЙНОСТЬ РАЦИОНА",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "${state.totalCalories}",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = TextPrimary
                                )
                                Text(
                                    text = " / ${state.targetCalories} ккал",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(bottom = 3.dp, start = 4.dp)
                                )
                            }
                        }

                        val remaining = (state.targetCalories - state.totalCalories).coerceAtLeast(0)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(EmeraldSubtle)
                                .border(width = 1.dp, color = BorderHighlight, shape = RoundedCornerShape(12.dp))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "Осталось $remaining ккал",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    LinearProgressIndicator(
                        progress = { caloriesProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = EmeraldNeon,
                        trackColor = DarkSurfaceHigh,
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroCard(
                            title = "Белки",
                            current = "${state.totalProtein} г",
                            target = "${state.targetProtein} г",
                            color = EmeraldNeon,
                            modifier = Modifier.weight(1f)
                        )
                        MacroCard(
                            title = "Жиры",
                            current = "${state.totalFats} г",
                            target = "${state.targetFats} г",
                            color = AmberWarm,
                            modifier = Modifier.weight(1f)
                        )
                        MacroCard(
                            title = "Углеводы",
                            current = "${state.totalCarbs} г",
                            target = "${state.targetCarbs} г",
                            color = IndigoElectric,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // AI Photo & Text Food Scanner Card (1:1 with NutritionPage.tsx)
        item(key = "ai_scanner_card") {
            AthleticCard(
                backgroundColor = DarkSurface,
                borderColor = BorderHighlight
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = EmeraldNeon,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ИИ-Сканер блюда (Фото / Текст)",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Meal type selector chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(mealTypeOptions, key = { it.first }) { (key, label) ->
                            FilterChip(
                                selected = state.selectedMealType == key,
                                onClick = { onAction(NutritionUiAction.OnSelectAnalyzerMealType(key)) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = EmeraldSubtle,
                                    selectedLabelColor = EmeraldLight,
                                    containerColor = DarkSurfaceVariant,
                                    labelColor = TextSecondary
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Photo picker button
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = pickPhoto,
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CameraAlt,
                                contentDescription = null,
                                tint = EmeraldLight,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (state.selectedPhotoBase64 != null) "Фото прикреплено ✓" else "Загрузить фото еды",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }

                        if (state.selectedPhotoBase64 != null) {
                            IconButton(onClick = { onAction(NutritionUiAction.OnClearPhoto) }) {
                                Icon(Icons.Default.Close, contentDescription = "Убрать фото", tint = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = state.aiDescriptionInput,
                        onValueChange = { onAction(NutritionUiAction.OnAiDescriptionChanged(it)) },
                        placeholder = {
                            Text(
                                "Опишите блюдо (напр. «250г плова и салат») или комментарий к фото...",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = DarkSurfaceVariant,
                            unfocusedContainerColor = DarkSurfaceVariant,
                            focusedBorderColor = EmeraldNeon,
                            unfocusedBorderColor = BorderSubtle,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Quick Food Chips
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(quickFoodPrompts) { prompt ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, BorderSubtle, RoundedCornerShape(8.dp))
                                    .clickable { onAction(NutritionUiAction.OnAiDescriptionChanged(prompt)) }
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(prompt, fontSize = 11.sp, color = TextSecondary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = { onAction(NutritionUiAction.OnRunAiAnalysis) },
                        enabled = !state.isAiAnalyzing && (state.aiDescriptionInput.isNotBlank() || state.selectedPhotoBase64 != null),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon,
                            contentColor = Color.Black
                        )
                    ) {
                        if (state.isAiAnalyzing) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ИИ рассчитывает КБЖУ...", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        } else {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Рассчитать КБЖУ с помощью ИИ", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        }
                    }

                    // AI Analysis Result Preview Card
                    if (state.aiAnalysisPreview != null) {
                        val preview = state.aiAnalysisPreview!!
                        Spacer(modifier = Modifier.height(12.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .border(1.dp, EmeraldNeon, RoundedCornerShape(12.dp))
                                .padding(14.dp)
                        ) {
                            Column {
                                Text(
                                    text = "РЕЗУЛЬТАТ ИИ-АНАЛИЗА",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldLight
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = preview.title.ifBlank { preview.name },
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "${preview.calories} ккал • Б: ${preview.proteinG.toInt()}г • Ж: ${preview.fatsG.toInt()}г • У: ${preview.carbsG.toInt()}г",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldNeon
                                )
                                if (preview.foodItems.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    preview.foodItems.forEach { item ->
                                        Text(
                                            text = "• ${item.name} (${item.portion}): ${item.calories} ккал",
                                            fontSize = 11.sp,
                                            color = TextSecondary
                                        )
                                    }
                                }
                                if (preview.aiNotes.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = preview.aiNotes,
                                        fontSize = 11.sp,
                                        color = TextMuted
                                    )
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedButton(
                                        onClick = { onAction(NutritionUiAction.OnDismissAiPreview) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Отмена", fontSize = 12.sp)
                                    }
                                    Button(
                                        onClick = { onAction(NutritionUiAction.OnConfirmAiPreview) },
                                        modifier = Modifier.weight(1.4f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = EmeraldNeon,
                                            contentColor = Color.Black
                                        )
                                    ) {
                                        Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Записать в рацион", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Intermittent Fasting Tracker
        item(key = "fasting_tracker") {
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
                onEndFast = {
                    activeFastingSession = null
                }
            )
        }

        // Hydration Animated Wave Vessel
        item(key = "water_wave_card") {
            WaterHydrationWave(
                currentMl = state.waterMl,
                targetMl = state.targetWaterMl,
                onAddWater = { ml -> onAction(NutritionUiAction.OnAddWater(ml)) }
            )
        }

        // Meals Header
        item(key = "meals_header") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "ЗАПИСИ ПИТАНИЯ (${state.meals.size})",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "+ База продуктов (150+)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyanElectric,
                        modifier = Modifier.clickable { isFoodCatalogSheetOpen = true }
                    )
                    Text(
                        text = "+ Ввести вручную",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight,
                        modifier = Modifier.clickable { onAction(NutritionUiAction.OnOpenAddMealSheet) }
                    )
                }
            }
        }

        if (state.meals.isEmpty()) {
            item(key = "empty_meals") {
                AthleticCard(backgroundColor = DarkSurface) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Записей за ${state.selectedDate} пока нет", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Используйте ИИ-сканер выше или добавьте приём пищи вручную", fontSize = 12.sp, color = TextSecondary)
                    }
                }
            }
        } else {
            items(state.meals, key = { it.id }) { meal ->
                AthleticCard(
                    backgroundColor = DarkSurface,
                    borderColor = BorderSubtle
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Restaurant,
                                    contentDescription = null,
                                    tint = EmeraldLight,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = meal.name,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                val mealTypeRu = mealTypeOptions.find { it.first == meal.mealType.lowercase() }?.second ?: meal.mealType
                                Text(
                                    text = "$mealTypeRu • Б: ${meal.proteinG}г • Ж: ${meal.fatsG}г • У: ${meal.carbsG}г",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${meal.calories} ккал",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldNeon
                            )

                            IconButton(
                                onClick = { onAction(NutritionUiAction.OnDeleteMeal(meal.id)) },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        item(key = "add_meal_button") {
            SecondaryAthleticButton(
                text = "+ Добавить приём пищи вручную",
                onClick = { onAction(NutritionUiAction.OnOpenAddMealSheet) },
                icon = Icons.Default.Add
            )
        }

        item(key = "bottom_spacer") {
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun MacroCard(
    title: String,
    current: String,
    target: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderSubtle,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = current,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = color
            )
            Text(
                text = "из $target",
                fontSize = 10.sp,
                color = TextMuted
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMealBottomSheet(
    mealType: String,
    mealName: String,
    calories: String,
    protein: String,
    fats: String,
    carbs: String,
    onMealTypeChanged: (String) -> Unit,
    onMealNameChanged: (String) -> Unit,
    onCaloriesChanged: (String) -> Unit,
    onProteinChanged: (String) -> Unit,
    onFatsChanged: (String) -> Unit,
    onCarbsChanged: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            Text(
                text = "Добавить приём пищи вручную",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(mealTypeOptions) { (type, label) ->
                    val isSelected = type == mealType
                    FilterChip(
                        selected = isSelected,
                        onClick = { onMealTypeChanged(type) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            OutlinedTextField(
                value = mealName,
                onValueChange = onMealNameChanged,
                label = { Text("Название продукта или блюда") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = DarkSurfaceVariant,
                    unfocusedContainerColor = DarkSurfaceVariant,
                    focusedBorderColor = EmeraldNeon,
                    unfocusedBorderColor = BorderSubtle,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = calories,
                    onValueChange = onCaloriesChanged,
                    label = { Text("Ккал") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = protein,
                    onValueChange = onProteinChanged,
                    label = { Text("Белки (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = fats,
                    onValueChange = onFatsChanged,
                    label = { Text("Жиры (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )

                OutlinedTextField(
                    value = carbs,
                    onValueChange = onCarbsChanged,
                    label = { Text("Углеводы (г)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(10.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            Button(
                onClick = onSave,
                enabled = mealName.isNotBlank() && calories.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldNeon,
                    contentColor = DarkBackground
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(text = "Сохранить", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
