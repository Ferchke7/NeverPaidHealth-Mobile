package dev.neverpaid.health.features.body

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.BodyMeasurement
import kotlin.math.abs
import kotlin.math.roundToInt

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

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        BodyScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun BodyScreen(
    state: BodyUiState,
    onAction: (BodyUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    if (state.isLoading && state.measurements.isEmpty()) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    if (state.isAddDialogOpen) {
        AddMeasurementBottomSheet(
            defaultWeight = state.latestWeightKg,
            defaultHeight = state.heightCm,
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
            onSave = { onAction(BodyUiAction.OnSaveTargetWeight(it)) },
            onDismiss = { onAction(BodyUiAction.OnCloseTargetWeightModal) }
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
                        text = "Вес, Замеры и ИМТ",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "Трекинг веса, скользящая средняя за 7 дней и калькулятор TDEE",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                OutlinedButton(
                    onClick = { onAction(BodyUiAction.OnToggleBmiCalculator) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.height(38.dp)
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Калькулятор", fontSize = 12.sp, color = EmeraldLight, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Body Target Progress Card (BodyTargetProgressCard.tsx)
        item(key = "target_progress_card") {
            BodyTargetProgressCard(
                startWeight = state.startWeightKg,
                currentWeight = state.latestWeightKg,
                targetWeight = state.targetWeightKg,
                onEditTarget = { onAction(BodyUiAction.OnOpenTargetWeightModal) }
            )
        }

        // Toggleable BMI & TDEE Calculator Card (BMICalculatorCard.tsx)
        item(key = "bmi_calculator_card") {
            AnimatedVisibility(visible = state.showBmiCalculator) {
                BMICalculatorInteractiveCard(
                    state = state,
                    onUpdateParams = { g, a, h, w, act ->
                        onAction(BodyUiAction.OnUpdateBmiParams(g, a, h, w, act))
                    }
                )
            }
        }

        // Current Weight & 7-Day SMA Card
        item(key = "weight_card") {
            AthleticCard(backgroundColor = DarkSurface) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSubtle),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MonitorWeight,
                                    contentDescription = null,
                                    tint = EmeraldNeon,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ТЕКУЩИЕ ПОКАЗАТЕЛИ ТЕЛА",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCounter(
                            title = "Текущий вес",
                            value = "${state.latestWeightKg}",
                            unit = "кг",
                            accentColor = TextPrimary
                        )
                        StatCounter(
                            title = "Средняя (7 дн.)",
                            value = "${state.movingAverage7DayKg ?: state.latestWeightKg}",
                            unit = "кг",
                            accentColor = EmeraldNeon
                        )
                        StatCounter(
                            title = "ИМТ (BMI)",
                            value = "${state.bmi}",
                            unit = "",
                            accentColor = IndigoElectric
                        )
                        StatCounter(
                            title = "Норма TDEE",
                            value = "${state.tdeeCalories}",
                            unit = "ккал",
                            accentColor = AmberWarm
                        )
                    }
                }
            }
        }

        item(key = "log_button") {
            PrimaryAthleticButton(
                text = "+ Записать взвешивание и замеры",
                onClick = { onAction(BodyUiAction.OnOpenAddDialog) },
                icon = Icons.Default.Add
            )
        }

        item(key = "history_header") {
            Text(
                text = "ИСТОРИЯ ВЗВЕШИВАНИЙ (${state.measurements.size})",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        items(
            items = state.measurements,
            key = { it.id.ifBlank { it.loggedDate } }
        ) { m ->
            BodyMeasurementRow(
                measurement = m,
                onDelete = {
                    val dateKey = m.loggedDate.ifBlank {
                        PlatformBridge.formatDate(m.dateEpochMs, "yyyy-MM-dd")
                    }
                    onAction(BodyUiAction.OnDeleteMeasurement(dateKey))
                }
            )
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BodyTargetProgressCard(
    startWeight: Double,
    currentWeight: Double,
    targetWeight: Double?,
    onEditTarget: () -> Unit
) {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Flag, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "ЦЕЛЕВОЙ ВЕС АТЛЕТА",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onEditTarget() }
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldLight, modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (targetWeight != null) "Цель: $targetWeight кг" else "Задать цель",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldLight
                    )
                }
            }

            if (targetWeight != null && targetWeight > 0) {
                val totalDiff = abs(targetWeight - startWeight).coerceAtLeast(0.5)
                val remaining = abs(targetWeight - currentWeight)
                val progress = (1.0 - (remaining / totalDiff)).toFloat().coerceIn(0.05f, 1.0f)

                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Старт: $startWeight кг", fontSize = 12.sp, color = TextSecondary)
                    Text("Сейчас: $currentWeight кг", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    Text("Цель: $targetWeight кг", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
                }
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = EmeraldNeon,
                    trackColor = DarkSurfaceHigh
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Установите целевой вес для сушки или набора мышечной массы, чтобы отслеживать прогресс.",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }
        }
    }
}

@Composable
private fun BMICalculatorInteractiveCard(
    state: BodyUiState,
    onUpdateParams: (String, Int, Double, Double, Double) -> Unit
) {
    var ageText by remember(state.age) { mutableStateOf(state.age.toString()) }
    var heightText by remember(state.heightCm) { mutableStateOf(state.heightCm.toInt().toString()) }
    var weightText by remember(state.latestWeightKg) { mutableStateOf(state.latestWeightKg.toString()) }

    val cutKcal = (state.tdeeCalories * 0.80).roundToInt()
    val bulkKcal = (state.tdeeCalories * 1.12).roundToInt()

    AthleticCard(
        backgroundColor = DarkSurface,
        borderColor = BorderHighlight
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "КАЛЬКУЛЯТОР ИМТ, BMR И НОРМЫ КБЖУ (MIFFLIN-ST JEOR)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldLight
            )

            Spacer(modifier = Modifier.height(10.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("male" to "Мужчина", "female" to "Женщина").forEach { (gKey, gLabel) ->
                    FilterChip(
                        selected = state.gender == gKey,
                        onClick = {
                            onUpdateParams(gKey, state.age, state.heightCm, state.latestWeightKg, state.activityMultiplier)
                        },
                        label = { Text(gLabel, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = ageText,
                    onValueChange = { v ->
                        ageText = v
                        v.toIntOrNull()?.let { a ->
                            onUpdateParams(state.gender, a, state.heightCm, state.latestWeightKg, state.activityMultiplier)
                        }
                    },
                    label = { Text("Возраст") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = heightText,
                    onValueChange = { v ->
                        heightText = v
                        v.toDoubleOrNull()?.let { h ->
                            onUpdateParams(state.gender, state.age, h, state.latestWeightKg, state.activityMultiplier)
                        }
                    },
                    label = { Text("Рост (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = weightText,
                    onValueChange = { v ->
                        weightText = v
                        v.toDoubleOrNull()?.let { w ->
                            onUpdateParams(state.gender, state.age, state.heightCm, w, state.activityMultiplier)
                        }
                    },
                    label = { Text("Вес (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                val activities = listOf(
                    1.2 to "Сидячий (1.2)",
                    1.375 to "1-3 трен. (1.375)",
                    1.55 to "3-5 трен. (1.55)",
                    1.725 to "6-7 трен. (1.725)"
                )
                items(activities) { (mult, label) ->
                    FilterChip(
                        selected = state.activityMultiplier == mult,
                        onClick = {
                            onUpdateParams(state.gender, state.age, state.heightCm, state.latestWeightKg, mult)
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldSubtle,
                            selectedLabelColor = EmeraldNeon
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Сушка (-20%)", fontSize = 10.sp, color = TextSecondary)
                        Text("$cutKcal ккал", fontSize = 14.sp, fontWeight = FontWeight.Black, color = AmberWarm)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Поддержка", fontSize = 10.sp, color = TextSecondary)
                        Text("${state.tdeeCalories} ккал", fontSize = 14.sp, fontWeight = FontWeight.Black, color = EmeraldNeon)
                    }
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkSurfaceVariant)
                        .padding(10.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                        Text("Масса (+12%)", fontSize = 10.sp, color = TextSecondary)
                        Text("$bulkKcal ккал", fontSize = 14.sp, fontWeight = FontWeight.Black, color = IndigoElectric)
                    }
                }
            }
        }
    }
}

@Composable
private fun BodyMeasurementRow(
    measurement: BodyMeasurement,
    onDelete: () -> Unit
) {
    val dateStr = measurement.loggedDate.ifBlank {
        PlatformBridge.formatDate(measurement.dateEpochMs, "dd MMM yyyy")
    }

    AthleticCard(backgroundColor = DarkSurfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = dateStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                val extras = buildList {
                    measurement.bodyFatPercentage?.let { add("Жир: $it%") }
                    measurement.waistCm?.let { add("Талия: ${it.toInt()}см") }
                    measurement.chestCm?.let { add("Грудь: ${it.toInt()}см") }
                    measurement.bicepsCm?.let { add("Бицепс: ${it.toInt()}см") }
                }
                Text(
                    text = if (extras.isNotEmpty()) extras.joinToString(" • ") else "ИМТ: ${measurement.bmi}",
                    fontSize = 12.sp,
                    color = TextSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "${measurement.weightKg} кг",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldNeon
                )
                IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = TextMuted, modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddMeasurementBottomSheet(
    defaultWeight: Double,
    defaultHeight: Double,
    onSave: (Double, Double, Double?, Double?, Double?, Double?, String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var weightInput by remember { mutableStateOf(defaultWeight.toString()) }
    var heightInput by remember { mutableStateOf(defaultHeight.toInt().toString()) }
    var bodyFatInput by remember { mutableStateOf("") }
    var waistInput by remember { mutableStateOf("") }
    var chestInput by remember { mutableStateOf("") }
    var bicepsInput by remember { mutableStateOf("") }
    var notesInput by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Записать вес и замеры тела", fontSize = 18.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = null, tint = TextSecondary)
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Вес (кг)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bodyFatInput,
                    onValueChange = { bodyFatInput = it },
                    label = { Text("% жира (опц.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = waistInput,
                    onValueChange = { waistInput = it },
                    label = { Text("Талия (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = chestInput,
                    onValueChange = { chestInput = it },
                    label = { Text("Грудь (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = bicepsInput,
                    onValueChange = { bicepsInput = it },
                    label = { Text("Бицепс (см)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }

            OutlinedTextField(
                value = notesInput,
                onValueChange = { notesInput = it },
                label = { Text("Заметка (напр. утром натощак)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Button(
                onClick = {
                    val w = weightInput.toDoubleOrNull() ?: defaultWeight
                    val h = heightInput.toDoubleOrNull() ?: defaultHeight
                    onSave(
                        w,
                        h,
                        bodyFatInput.toDoubleOrNull(),
                        waistInput.toDoubleOrNull(),
                        chestInput.toDoubleOrNull(),
                        bicepsInput.toDoubleOrNull(),
                        notesInput.trim()
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black)
            ) {
                Text("Сохранить замер", fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@Composable
private fun TargetWeightDialog(
    currentTarget: Double?,
    latestWeight: Double,
    onSave: (Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var input by remember { mutableStateOf((currentTarget ?: latestWeight).toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Целевой вес (кг)", color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text("Целевой вес в кг") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true
            )
        },
        confirmButton = {
            Button(
                onClick = { onSave(input.toDoubleOrNull()) },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = Color.Black)
            ) {
                Text("Сохранить", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
