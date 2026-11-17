package dev.neverpaid.health.features.body

import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeGlow
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.BorderDivider
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymLoadingCard
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPageHeader
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymProgressBar
import dev.neverpaid.health.core.designsystem.GymSectionTitle
import dev.neverpaid.health.core.designsystem.GymStatTile
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.BodyMeasurement
import kotlin.math.abs
import kotlin.math.roundToInt

private enum class BodyChartMetric {
    WEIGHT,
    WAIST,
    BODY_FAT
}

private enum class DietMode {
    CUT,
    MAINTAIN,
    BULK
}

@Composable
fun BodyRoute(
    viewModel: BodyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            is BodyUiEffect.ShowToast -> PlatformBridge.showToast(effect.message)
        }
    }

    BodyScreen(
        state = state,
        onAction = viewModel::onAction,
        modifier = modifier
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BodyScreen(
    state: BodyUiState,
    onAction: (BodyUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val lang = strings.languageCode

    var isAnthropometrySheetOpen by remember { mutableStateOf(false) }
    var chartMetric by rememberSaveable { mutableStateOf(BodyChartMetric.WEIGHT) }
    var dietMode by rememberSaveable { mutableStateOf(DietMode.MAINTAIN) }

    // Inline Add Measurement fields
    var weightInput by remember(state.latestWeightKg) { mutableStateOf(formatOneDecimal(state.latestWeightKg)) }
    var waistInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var bicepsInput by remember { mutableStateOf("") }
    var bodyFatInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    // Inline Calculator fields
    var calcHeightInput by remember(state.heightCm) { mutableStateOf(state.heightCm.roundToInt().toString()) }
    var calcWeightInput by remember(state.latestWeightKg) { mutableStateOf(formatOneDecimal(state.latestWeightKg)) }
    var calcAgeInput by remember(state.age) { mutableStateOf(state.age.toString()) }
    var calcGender by remember(state.gender) { mutableStateOf(state.gender) }
    var calcActivity by remember(state.activityMultiplier) { mutableStateOf(state.activityMultiplier) }

    // Inline Target weight field
    var targetWeightInput by remember(state.targetWeightKg, state.latestWeightKg) {
        mutableStateOf(
            state.targetWeightKg?.let { formatOneDecimal(it) } ?: formatOneDecimal(state.latestWeightKg)
        )
    }

    if (isAnthropometrySheetOpen) {
        AnthropometryCalculatorBottomSheet(
            isOpen = true,
            currentHeightCm = state.heightCm,
            currentWeightKg = state.latestWeightKg,
            onDismiss = { isAnthropometrySheetOpen = false },
            onSaveMetrics = { bf, adonis ->
                onAction(
                    BodyUiAction.OnSaveFullMeasurement(
                        weightKg = state.latestWeightKg,
                        heightCm = state.heightCm,
                        bodyFatPct = bf,
                        waistCm = null,
                        chestCm = null,
                        bicepsCm = null,
                        notes = "Adonis Index: ${((adonis * 100).toInt() / 100.0)}"
                    )
                )
                isAnthropometrySheetOpen = false
            }
        )
    }

    if (state.isAddDialogOpen) {
        AddMeasurementBottomSheet(
            defaultWeight = state.latestWeightKg,
            defaultHeight = state.heightCm,
            unitLabel = strings.unitKg,
            lang = lang,
            onSave = { w, h, bf, waist, chest, biceps, notes ->
                onAction(BodyUiAction.OnSaveFullMeasurement(w, h, bf, waist, chest, biceps, notes))
            },
            onDismiss = { onAction(BodyUiAction.OnDismissAddDialog) }
        )
    }

    if (state.isTargetWeightModalOpen) {
        TargetWeightDialog(
            currentTarget = state.targetWeightKg,
            latestWeight = state.latestWeightKg,
            unitLabel = strings.unitKg,
            lang = lang,
            onSave = { onAction(BodyUiAction.OnSaveTargetWeight(it)) },
            onDismiss = { onAction(BodyUiAction.OnCloseTargetWeightModal) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 20.dp,
                end = 20.dp,
                top = 8.dp,
                bottom = 110.dp
            ),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            item(key = "header", contentType = "header") {
                GymPageHeader(
                    title = strings.bodyTitle,
                    subtitle = strings.bodySubtitle
                )
            }

            if (state.isLoading && state.measurements.isEmpty()) {
                item(key = "loading_card", contentType = "loading") {
                    GymLoadingCard(
                        title = strings.stateLoadingTitle,
                        hint = strings.stateLoadingHint
                    )
                }
            } else {
                // Hero Card: Current Weight + Delta Pill + Target Progress
                item(key = "weight_hero_card", contentType = "hero") {
                    val weightDelta = state.latestWeightKg - state.startWeightKg
                    val deltaSign = if (weightDelta > 0.0) "+" else ""
                    val deltaText = "$deltaSign${formatOneDecimal(weightDelta)} ${strings.unitKg}"

                    GymCard {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = strings.bodyCurrentWeight,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        text = formatOneDecimal(state.latestWeightKg),
                                        fontSize = 44.sp,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = (-1.2).sp,
                                        color = TextPrimary,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = strings.unitKg,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentLime,
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(AccentLimeSubtle)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = deltaText,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Black,
                                    color = AccentLime,
                                    style = TextStyle(fontFeatureSettings = "tnum")
                                )
                            }
                        }

                        val target = state.targetWeightKg
                        if (target != null && target > 0.0) {
                            val totalDiff = abs(target - state.startWeightKg).coerceAtLeast(0.5)
                            val remaining = abs(target - state.latestWeightKg)
                            val progress = (1.0 - (remaining / totalDiff)).toFloat().coerceIn(0.05f, 1f)

                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(DarkSurfaceHigh)
                                    .clickable { onAction(BodyUiAction.OnOpenTargetWeightModal) }
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = when (lang) {
                                            "uz" -> "Maqsad: ${formatOneDecimal(target)} ${strings.unitKg}"
                                            "en" -> "Target: ${formatOneDecimal(target)} ${strings.unitKg}"
                                            else -> "Цель: ${formatOneDecimal(target)} ${strings.unitKg}"
                                        },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary,
                                        style = TextStyle(fontFeatureSettings = "tnum")
                                    )
                                    Text(
                                        text = when (lang) {
                                            "uz" -> "O'zgartirish"
                                            "en" -> "Edit"
                                            else -> "Изменить"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentLime
                                    )
                                }
                                GymProgressBar(progress = progress, heightDp = 6, color = AccentLime)
                            }
                        } else {
                            GymOutlineButton(
                                text = when (lang) {
                                    "uz" -> "Maqsadli vaznni belgilash"
                                    "en" -> "Set target weight"
                                    else -> "Задать целевой вес"
                                },
                                onClick = { onAction(BodyUiAction.OnOpenTargetWeightModal) },
                                fillWidth = true
                            )
                        }
                    }
                }

                // 2-Column Secondary KPI Row: Target & 7-Day SMA
                item(key = "kpi_secondary_row", contentType = "kpi") {
                    val sma = state.movingAverage7DayKg ?: state.latestWeightKg
                    val targetVal = state.targetWeightKg?.let { formatOneDecimal(it) } ?: "—"
                    val targetUnit = if (state.targetWeightKg != null) strings.unitKg else ""

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GymStatTile(
                            label = when (lang) {
                                "uz" -> "Maqsadli vazn"
                                "en" -> "Target weight"
                                else -> "Целевой вес"
                            },
                            value = targetVal,
                            unit = targetUnit,
                            valueColor = AccentLime,
                            modifier = Modifier
                                .weight(1f)
                                .clickable { onAction(BodyUiAction.OnOpenTargetWeightModal) }
                        )
                        GymStatTile(
                            label = "${strings.bodyMovingAverage} (7д)",
                            value = formatOneDecimal(sma),
                            unit = strings.unitKg,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Weight & Body Composition Dynamics Chart Card
                item(key = "dynamics_chart_card", contentType = "chart") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) {
                                "uz" -> "Vazn dinamikasi"
                                "en" -> "Weight & body trend"
                                else -> "Динамика замеров"
                            }
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            GymPillTab(
                                text = when (lang) { "uz" -> "Vazn"; "en" -> "Weight"; else -> "Вес" },
                                selected = chartMetric == BodyChartMetric.WEIGHT,
                                onClick = { chartMetric = BodyChartMetric.WEIGHT }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Bel"; "en" -> "Waist"; else -> "Обхваты" },
                                selected = chartMetric == BodyChartMetric.WAIST,
                                onClick = { chartMetric = BodyChartMetric.WAIST }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Yog', %"; "en" -> "Body fat %"; else -> "Жир, %" },
                                selected = chartMetric == BodyChartMetric.BODY_FAT,
                                onClick = { chartMetric = BodyChartMetric.BODY_FAT }
                            )
                        }

                        val series = remember(state.measurements, chartMetric) {
                            state.measurements
                                .sortedBy { it.loggedDate.ifBlank { it.id } }
                                .mapNotNull { m ->
                                    val value = when (chartMetric) {
                                        BodyChartMetric.WEIGHT -> m.weightKg
                                        BodyChartMetric.WAIST -> m.waistCm
                                        BodyChartMetric.BODY_FAT -> m.bodyFatPercentage
                                    }
                                    value?.let { v ->
                                        val dateStr = m.loggedDate.ifBlank {
                                            PlatformBridge.formatDate(m.dateEpochMs, "dd.MM")
                                        }
                                        val shortDate = if (dateStr.length >= 10 && dateStr[4] == '-') {
                                            "${dateStr.substring(8, 10)}.${dateStr.substring(5, 7)}"
                                        } else {
                                            dateStr.takeLast(5)
                                        }
                                        shortDate to v
                                    }
                                }
                                .takeLast(8)
                        }

                        if (series.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "Grafik uchun o'lchovlar yo'q"
                                        "en" -> "No measurements recorded for this metric yet"
                                        else -> "Нет замеров для отображения графика"
                                    },
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            BodyTrendLineChart(
                                points = series,
                                unitLabel = when (chartMetric) {
                                    BodyChartMetric.WEIGHT -> strings.unitKg
                                    BodyChartMetric.WAIST -> "см"
                                    BodyChartMetric.BODY_FAT -> "%"
                                }
                            )
                        }
                    }
                }

                // Inline Add Measurement Card (Clean 2-Column Grid)
                item(key = "add_measurement_card", contentType = "add_measurement") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) {
                                "uz" -> "O'lchov qo'shish"
                                "en" -> "Add measurement"
                                else -> "Добавить замер"
                            },
                            actionLabel = when (lang) { "uz" -> "Tozalash"; "en" -> "Reset"; else -> "Сбросить" },
                            onAction = {
                                weightInput = formatOneDecimal(state.latestWeightKg)
                                waistInput = ""
                                chestInput = ""
                                bicepsInput = ""
                                bodyFatInput = ""
                                notesInput = ""
                            }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymLabeledField(
                                label = "${when (lang) { "uz" -> "Vazn"; "en" -> "Weight"; else -> "Вес" }}, ${strings.unitKg}",
                                value = weightInput,
                                onValueChange = { weightInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = "78",
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = when (lang) { "uz" -> "Yog', %"; "en" -> "Body fat %"; else -> "Жир, %" },
                                value = bodyFatInput,
                                onValueChange = { bodyFatInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = "—",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymLabeledField(
                                label = when (lang) { "uz" -> "Bel, sm"; "en" -> "Waist, cm"; else -> "Талия, см" },
                                value = waistInput,
                                onValueChange = { waistInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = "—",
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = when (lang) { "uz" -> "Ko'krak, sm"; "en" -> "Chest, cm"; else -> "Грудь, см" },
                                value = chestInput,
                                onValueChange = { chestInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = "—",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymLabeledField(
                                label = when (lang) { "uz" -> "Qo'l, sm"; "en" -> "Arms, cm"; else -> "Бицепс, см" },
                                value = bicepsInput,
                                onValueChange = { bicepsInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = "—",
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = when (lang) { "uz" -> "Eslatma"; "en" -> "Note"; else -> "Заметка" },
                                value = notesInput,
                                onValueChange = { notesInput = it },
                                placeholder = when (lang) {
                                    "uz" -> "Ertalab"
                                    "en" -> "Morning"
                                    else -> "Натощак"
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        GymPrimaryButton(
                            text = when (lang) {
                                "uz" -> "O'lchovni saqlash"
                                "en" -> "Save measurement"
                                else -> "Сохранить замер"
                            },
                            onClick = {
                                val w = weightInput.toDoubleOrNull() ?: state.latestWeightKg
                                val h = calcHeightInput.toDoubleOrNull() ?: state.heightCm
                                onAction(
                                    BodyUiAction.OnSaveFullMeasurement(
                                        weightKg = w,
                                        heightCm = h,
                                        bodyFatPct = bodyFatInput.toDoubleOrNull(),
                                        waistCm = waistInput.toDoubleOrNull(),
                                        chestCm = chestInput.toDoubleOrNull(),
                                        bicepsCm = bicepsInput.toDoubleOrNull(),
                                        notes = notesInput.trim()
                                    )
                                )
                                waistInput = ""
                                chestInput = ""
                                bicepsInput = ""
                                bodyFatInput = ""
                                notesInput = ""
                            },
                            fillWidth = true
                        )
                    }
                }

                // Recent Measurements List Card
                item(key = "recent_measurements_card", contentType = "measurements") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) {
                                "uz" -> "Oxirgi o'lchovlar"
                                "en" -> "Recent measurements"
                                else -> "Последние замеры"
                            }
                        )

                        if (state.measurements.isEmpty()) {
                            GymInnerWell {
                                Text(
                                    text = when (lang) {
                                        "uz" -> "O'lchovlar tarixi bo'sh"
                                        "en" -> "No measurements logged yet"
                                        else -> "История замеров пока пуста"
                                    },
                                    fontSize = 14.sp,
                                    color = TextSecondary
                                )
                            }
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                state.measurements.take(10).forEach { m ->
                                    BodyMeasurementCleanRow(
                                        measurement = m,
                                        unitLabel = strings.unitKg,
                                        lang = lang,
                                        onDelete = {
                                            val dateKey = m.loggedDate.ifBlank {
                                                PlatformBridge.formatDate(m.dateEpochMs, "yyyy-MM-dd")
                                            }
                                            onAction(BodyUiAction.OnDeleteMeasurement(dateKey))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                // Calculator Card (BMI / BMR / TDEE + Navy BF% & Adonis Sheet)
                item(key = "calculator_card", contentType = "calculator") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) {
                                "uz" -> "Kalkulyator"
                                "en" -> "Metabolic calculator"
                                else -> "Калькулятор обмена"
                            },
                            actionLabel = strings.navyBodyFatTitle,
                            onAction = { isAnthropometrySheetOpen = true }
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            GymLabeledField(
                                label = strings.bodyHeight,
                                value = calcHeightInput,
                                onValueChange = { calcHeightInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = "178",
                                modifier = Modifier.weight(1f)
                            )
                            GymLabeledField(
                                label = "${when (lang) { "uz" -> "Vazn"; "en" -> "Weight"; else -> "Вес" }}, ${strings.unitKg}",
                                value = calcWeightInput,
                                onValueChange = { calcWeightInput = it },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                placeholder = "78",
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Bottom
                        ) {
                            GymLabeledField(
                                label = when (lang) { "uz" -> "Yosh, yil"; "en" -> "Age, yrs"; else -> "Возраст, лет" },
                                value = calcAgeInput,
                                onValueChange = { calcAgeInput = it.filter { c -> c.isDigit() } },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                placeholder = "28",
                                modifier = Modifier.weight(1f)
                            )
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = when (lang) { "uz" -> "Jins"; "en" -> "Sex"; else -> "Пол" },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    GymPillTab(
                                        text = when (lang) { "uz" -> "Erkak"; "en" -> "M"; else -> "Муж" },
                                        selected = calcGender == "male",
                                        onClick = {
                                            calcGender = "male"
                                            onAction(
                                                BodyUiAction.OnUpdateBmiParams(
                                                    gender = "male",
                                                    age = calcAgeInput.toIntOrNull() ?: state.age,
                                                    heightCm = calcHeightInput.toDoubleOrNull() ?: state.heightCm,
                                                    weightKg = calcWeightInput.toDoubleOrNull() ?: state.latestWeightKg,
                                                    activityMultiplier = calcActivity
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                    GymPillTab(
                                        text = when (lang) { "uz" -> "Ayol"; "en" -> "F"; else -> "Жен" },
                                        selected = calcGender == "female",
                                        onClick = {
                                            calcGender = "female"
                                            onAction(
                                                BodyUiAction.OnUpdateBmiParams(
                                                    gender = "female",
                                                    age = calcAgeInput.toIntOrNull() ?: state.age,
                                                    heightCm = calcHeightInput.toDoubleOrNull() ?: state.heightCm,
                                                    weightKg = calcWeightInput.toDoubleOrNull() ?: state.latestWeightKg,
                                                    activityMultiplier = calcActivity
                                                )
                                            )
                                        },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = when (lang) {
                                    "uz" -> "Faollik koeffitsiyenti"
                                    "en" -> "Activity factor"
                                    else -> "Коэффициент активности"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(
                                    1.2 to "1.2",
                                    1.375 to "1.375",
                                    1.55 to "1.55",
                                    1.725 to "1.725"
                                ).forEach { (mult, label) ->
                                    GymPillTab(
                                        text = label,
                                        selected = calcActivity == mult,
                                        onClick = { calcActivity = mult }
                                    )
                                }
                            }
                        }

                        // 3-Column Heavy Numeral Readout: BMI, BMR, TDEE
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            CalcResultTile(
                                label = strings.bodyBmi,
                                value = formatOneDecimal(state.bmi),
                                unit = "кг/м²",
                                valueColor = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            CalcResultTile(
                                label = "BMR",
                                value = "${state.bmrCalories}",
                                unit = "ккал",
                                valueColor = TextPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            CalcResultTile(
                                label = strings.bodyTdee,
                                value = "${state.tdeeCalories}",
                                unit = "ккал",
                                valueColor = AccentLime,
                                modifier = Modifier.weight(1f)
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            GymPrimaryButton(
                                text = when (lang) { "uz" -> "Hisoblash"; "en" -> "Calculate"; else -> "Рассчитать" },
                                onClick = {
                                    onAction(
                                        BodyUiAction.OnUpdateBmiParams(
                                            gender = calcGender,
                                            age = calcAgeInput.toIntOrNull() ?: state.age,
                                            heightCm = calcHeightInput.toDoubleOrNull() ?: state.heightCm,
                                            weightKg = calcWeightInput.toDoubleOrNull() ?: state.latestWeightKg,
                                            activityMultiplier = calcActivity
                                        )
                                    )
                                },
                                modifier = Modifier.weight(1f)
                            )
                            GymOutlineButton(
                                text = strings.navyBodyFatTitle,
                                onClick = { isAnthropometrySheetOpen = true },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Targets & Calorie Mode Card
                item(key = "goals_card", contentType = "goals") {
                    GymCard {
                        GymSectionTitle(
                            title = when (lang) { "uz" -> "Maqsadlar"; "en" -> "Goals"; else -> "Цели и калории" }
                        )

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            GymPillTab(
                                text = when (lang) { "uz" -> "Pasaytirish"; "en" -> "Cut (-20%)"; else -> "Снижение" },
                                selected = dietMode == DietMode.CUT,
                                onClick = { dietMode = DietMode.CUT }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Saqlash"; "en" -> "Maintain"; else -> "Поддержание" },
                                selected = dietMode == DietMode.MAINTAIN,
                                onClick = { dietMode = DietMode.MAINTAIN }
                            )
                            GymPillTab(
                                text = when (lang) { "uz" -> "Oshirish"; "en" -> "Bulk (+12%)"; else -> "Набор" },
                                selected = dietMode == DietMode.BULK,
                                onClick = { dietMode = DietMode.BULK }
                            )
                        }

                        val targetKcal = when (dietMode) {
                            DietMode.CUT -> (state.tdeeCalories * 0.80).roundToInt()
                            DietMode.MAINTAIN -> state.tdeeCalories
                            DietMode.BULK -> (state.tdeeCalories * 1.12).roundToInt()
                        }
                        val modeLabel = when (dietMode) {
                            DietMode.CUT -> when (lang) { "uz" -> "Pasaytirish"; "en" -> "Fat loss"; else -> "Дефицит калорий" }
                            DietMode.MAINTAIN -> when (lang) { "uz" -> "Saqlash"; "en" -> "Maintenance"; else -> "Поддержание веса" }
                            DietMode.BULK -> when (lang) { "uz" -> "Oshirish"; "en" -> "Surplus"; else -> "Профицит для роста" }
                        }

                        GymInnerWell {
                            Text(
                                text = "$modeLabel · ${when (lang) { "uz" -> "kuniga"; "en" -> "per day"; else -> "ориентир на день" }}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$targetKcal ккал",
                                fontSize = 34.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-0.8).sp,
                                color = AccentLime,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        GymLabeledField(
                            label = "${when (lang) { "uz" -> "Maqsadli vazn"; "en" -> "Target weight"; else -> "Целевой вес" }}, ${strings.unitKg}",
                            value = targetWeightInput,
                            onValueChange = { targetWeightInput = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            placeholder = "75"
                        )

                        GymPrimaryButton(
                            text = when (lang) {
                                "uz" -> "Maqsadni saqlash"
                                "en" -> "Save target"
                                else -> "Сохранить цели"
                            },
                            onClick = {
                                onAction(BodyUiAction.OnSaveTargetWeight(targetWeightInput.toDoubleOrNull()))
                            },
                            fillWidth = true
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CalcResultTile(
    label: String,
    value: String,
    unit: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurfaceHigh)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Black,
            color = valueColor,
            style = TextStyle(fontFeatureSettings = "tnum"),
            maxLines = 1
        )
        Text(
            text = unit,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = TextMuted,
            maxLines = 1
        )
    }
}

@Composable
private fun BodyTrendLineChart(
    points: List<Pair<String, Double>>,
    unitLabel: String
) {
    val minVal = points.minOf { it.second }
    val maxVal = points.maxOf { it.second }
    val padding = ((maxVal - minVal) * 0.25).coerceAtLeast(0.5)
    val low = minVal - padding
    val high = maxVal + padding
    val range = (high - low).coerceAtLeast(1.0)

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = unitLabel, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = TextMuted)
            Text(
                text = "${formatOneDecimal(points.first().second)} → ${formatOneDecimal(points.last().second)} $unitLabel",
                fontSize = 13.sp,
                color = AccentLime,
                fontWeight = FontWeight.Black,
                style = TextStyle(fontFeatureSettings = "tnum")
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier
                    .width(42.dp)
                    .height(130.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = formatOneDecimal(high),
                    fontSize = 10.sp,
                    color = TextMuted,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = formatOneDecimal((high + low) / 2.0),
                    fontSize = 10.sp,
                    color = TextMuted,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = formatOneDecimal(low),
                    fontSize = 10.sp,
                    color = TextMuted,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(130.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    listOf(0f, 0.5f, 1f).forEach { frac ->
                        val y = frac * h
                        drawLine(
                            color = BorderDivider,
                            start = Offset(0f, y),
                            end = Offset(w, y),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                        )
                    }

                    val stepX = if (points.size > 1) w / (points.size - 1).toFloat() else w / 2f
                    val coords = points.mapIndexed { idx, (_, value) ->
                        val x = if (points.size == 1) w / 2f else idx * stepX
                        val normY = ((value - low) / range).toFloat().coerceIn(0f, 1f)
                        val y = h - (normY * (h - 16.dp.toPx())) - 8.dp.toPx()
                        Offset(x, y)
                    }

                    if (coords.size > 1) {
                        val strokePath = Path().apply {
                            moveTo(coords.first().x, coords.first().y)
                            for (i in 0 until coords.size - 1) {
                                val p0 = coords[i]
                                val p1 = coords[i + 1]
                                val cp1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                                val cp2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                                cubicTo(cp1.x, cp1.y, cp2.x, cp2.y, p1.x, p1.y)
                            }
                        }
                        val fillPath = Path().apply {
                            addPath(strokePath)
                            lineTo(coords.last().x, h)
                            lineTo(coords.first().x, h)
                            close()
                        }
                        drawPath(
                            path = fillPath,
                            brush = Brush.verticalGradient(
                                colors = listOf(AccentLimeGlow, Color.Transparent),
                                startY = 0f,
                                endY = h
                            ),
                            style = Fill
                        )
                        drawPath(
                            path = strokePath,
                            color = AccentLime,
                            style = Stroke(
                                width = 3.dp.toPx(),
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }

                    coords.forEach { pt ->
                        drawCircle(
                            color = DarkBackground,
                            radius = 5.5.dp.toPx(),
                            center = pt
                        )
                        drawCircle(
                            color = AccentLime,
                            radius = 3.8.dp.toPx(),
                            center = pt
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 42.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { (dateLabel, value) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = formatOneDecimal(value),
                        fontSize = 10.sp,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                    Text(
                        text = dateLabel,
                        fontSize = 10.sp,
                        color = TextMuted,
                        style = TextStyle(fontFeatureSettings = "tnum")
                    )
                }
            }
        }
    }
}

@Composable
private fun BodyMeasurementCleanRow(
    measurement: BodyMeasurement,
    unitLabel: String,
    lang: String,
    onDelete: () -> Unit
) {
    val dateStr = measurement.loggedDate.ifBlank {
        PlatformBridge.formatDate(measurement.dateEpochMs, "dd MMM yyyy")
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkSurfaceHigh)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${formatOneDecimal(measurement.weightKg)} $unitLabel",
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
                Text(
                    text = dateStr,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    modifier = Modifier.padding(bottom = 2.dp)
                )
            }
            val extras = buildList {
                measurement.bodyFatPercentage?.let { add("${when (lang) { "uz" -> "Yog'"; "en" -> "BF"; else -> "Жир" }}: $it%") }
                measurement.waistCm?.let { add("${when (lang) { "uz" -> "Bel"; "en" -> "Waist"; else -> "Талия" }}: ${it.toInt()} см") }
                measurement.chestCm?.let { add("${when (lang) { "uz" -> "Ko'krak"; "en" -> "Chest"; else -> "Грудь" }}: ${it.toInt()} см") }
                measurement.bicepsCm?.let { add("${when (lang) { "uz" -> "Qo'l"; "en" -> "Arm"; else -> "Бицепс" }}: ${it.toInt()} см") }
            }
            if (extras.isNotEmpty()) {
                Text(
                    text = extras.joinToString(" · "),
                    fontSize = 12.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(fontFeatureSettings = "tnum")
                )
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurface)
                .clickable(onClick = onDelete)
                .padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Text(
                text = when (lang) { "uz" -> "O'chirish"; "en" -> "Delete"; else -> "Удалить" },
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = CrimsonRed
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMeasurementBottomSheet(
    defaultWeight: Double,
    defaultHeight: Double,
    unitLabel: String,
    lang: String,
    onSave: (Double, Double, Double?, Double?, Double?, Double?, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var weightInput by remember { mutableStateOf(formatOneDecimal(defaultWeight)) }
    var heightInput by remember { mutableStateOf(defaultHeight.roundToInt().toString()) }
    var bodyFatInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var bicepsInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "Vazn va tana o'lchamlari"
                        "en" -> "Log Weight & Measurements"
                        else -> "Записать вес и замеры"
                    },
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextPrimary
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GymLabeledField(
                    label = "${when (lang) { "uz" -> "Vazn"; "en" -> "Weight"; else -> "Вес" }} ($unitLabel)",
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f)
                )
                GymLabeledField(
                    label = when (lang) { "uz" -> "Yog' %"; "en" -> "Body fat %"; else -> "% жира" },
                    value = bodyFatInput,
                    onValueChange = { bodyFatInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GymLabeledField(
                    label = when (lang) { "uz" -> "Bel, sm"; "en" -> "Waist, cm"; else -> "Талия, см" },
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )
                GymLabeledField(
                    label = when (lang) { "uz" -> "Ko'krak, sm"; "en" -> "Chest, cm"; else -> "Грудь, см" },
                    value = chestInput,
                    onValueChange = { chestInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GymLabeledField(
                    label = when (lang) { "uz" -> "Biceps, sm"; "en" -> "Biceps, cm"; else -> "Бицепс, см" },
                    value = bicepsInput,
                    onValueChange = { bicepsInput = it },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    placeholder = "—",
                    modifier = Modifier.weight(1f)
                )
                GymLabeledField(
                    label = when (lang) { "uz" -> "Eslatma"; "en" -> "Note"; else -> "Заметка" },
                    value = notesInput,
                    onValueChange = { notesInput = it },
                    placeholder = when (lang) {
                        "uz" -> "Ertalab"
                        "en" -> "Morning"
                        else -> "Натощак"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                GymOutlineButton(
                    text = when (lang) { "uz" -> "Bekor"; "en" -> "Cancel"; else -> "Отмена" },
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
                GymPrimaryButton(
                    text = when (lang) { "uz" -> "Saqlash"; "en" -> "Save"; else -> "Сохранить" },
                    onClick = {
                        onSave(
                            weightInput.toDoubleOrNull() ?: defaultWeight,
                            heightInput.toDoubleOrNull() ?: defaultHeight,
                            bodyFatInput.toDoubleOrNull(),
                            waistInput.toDoubleOrNull(),
                            chestInput.toDoubleOrNull(),
                            bicepsInput.toDoubleOrNull(),
                            notesInput.trim()
                        )
                    },
                    modifier = Modifier.weight(1f),
                    fillWidth = true
                )
            }
        }
    }
}

@Composable
private fun TargetWeightDialog(
    currentTarget: Double?,
    latestWeight: Double,
    unitLabel: String,
    lang: String,
    onSave: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf(formatOneDecimal(currentTarget ?: latestWeight)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurface,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = when (lang) {
                    "uz" -> "Maqsadli vazn ($unitLabel)"
                    "en" -> "Target weight ($unitLabel)"
                    else -> "Целевой вес ($unitLabel)"
                },
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TextPrimary
            )
        },
        text = {
            GymLabeledField(
                label = unitLabel,
                value = input,
                onValueChange = { input = it },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                placeholder = "75"
            )
        },
        confirmButton = {
            GymPrimaryButton(
                text = when (lang) { "uz" -> "Saqlash"; "en" -> "Save"; else -> "Сохранить" },
                onClick = { onSave(input.toDoubleOrNull()) }
            )
        },
        dismissButton = {
            GymOutlineButton(
                text = when (lang) { "uz" -> "Bekor"; "en" -> "Cancel"; else -> "Отмена" },
                onClick = onDismiss
            )
        }
    )
}

private fun formatOneDecimal(value: Double): String {
    val scaled = (value * 10.0).roundToInt()
    val intPart = scaled / 10
    val frac = abs(scaled % 10)
    return if (frac == 0) "$intPart" else "$intPart.$frac"
}
