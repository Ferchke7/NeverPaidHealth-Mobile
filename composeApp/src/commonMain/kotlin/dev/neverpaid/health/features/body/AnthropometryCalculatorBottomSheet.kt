package dev.neverpaid.health.features.body

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AmberWarm
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.GymIconBadge
import dev.neverpaid.health.core.designsystem.GymInnerWell
import dev.neverpaid.health.core.designsystem.GymLabeledField
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.math.AnthropometryEngine

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnthropometryCalculatorBottomSheet(
    isOpen: Boolean,
    currentHeightCm: Double,
    currentWeightKg: Double,
    onDismiss: () -> Unit,
    onSaveMetrics: (bodyFatPct: Double, adonisRatio: Double) -> Unit
) {
    if (!isOpen) return

    val strings = LocalDudaStrings.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var isMale by remember { mutableStateOf(true) }
    var heightCmText by remember(currentHeightCm) { mutableStateOf(if (currentHeightCm > 0) currentHeightCm.toInt().toString() else "180") }
    var weightKgText by remember(currentWeightKg) { mutableStateOf(if (currentWeightKg > 0) currentWeightKg.toInt().toString() else "80") }
    var neckCmText by remember { mutableStateOf("40") }
    var waistCmText by remember { mutableStateOf("82") }
    var hipCmText by remember { mutableStateOf("96") }
    var shoulderCmText by remember { mutableStateOf("125") }
    var wristCmText by remember { mutableStateOf("18") }
    var ankleCmText by remember { mutableStateOf("22") }

    val height = heightCmText.toDoubleOrNull() ?: 180.0
    val weight = weightKgText.toDoubleOrNull() ?: 80.0
    val neck = neckCmText.toDoubleOrNull() ?: 40.0
    val waist = waistCmText.toDoubleOrNull() ?: 82.0
    val hip = hipCmText.toDoubleOrNull() ?: 96.0
    val shoulder = shoulderCmText.toDoubleOrNull() ?: 125.0
    val wrist = wristCmText.toDoubleOrNull() ?: 18.0
    val ankle = ankleCmText.toDoubleOrNull() ?: 22.0

    val navyBf = remember(isMale, height, neck, waist, hip, weight) {
        AnthropometryEngine.calculateNavyBodyFat(
            isMale = isMale,
            heightCm = height,
            neckCm = neck,
            waistCm = waist,
            hipCm = hip,
            weightKg = weight
        )
    }

    val adonis = remember(height, waist, shoulder) {
        AnthropometryEngine.calculateAdonisIndex(
            heightCm = height,
            waistCm = waist,
            shouldersCm = shoulder,
            chestCm = waist * 1.33
        )
    }

    val naturalLimit = remember(height, wrist, ankle, weight) {
        AnthropometryEngine.calculateMaxMuscularPotential(
            heightCm = height,
            wristCm = wrist,
            ankleCm = ankle,
            currentWeightKg = weight,
            currentBodyFatPct = navyBf?.bodyFatPercentage ?: 15.0
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
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
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    GymIconBadge(
                        icon = Icons.Default.Straighten,
                        size = 42.dp
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Антропометрия",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = (-0.4).sp,
                            color = TextPrimary
                        )
                        Text(
                            text = "US Navy BF% · Индекс Адониса",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = TextMuted)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gender Selection
                item(key = "gender_tabs") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        GymPillTab(
                            text = "Мужчина",
                            selected = isMale,
                            onClick = { isMale = true },
                            modifier = Modifier.weight(1f)
                        )
                        GymPillTab(
                            text = "Женщина",
                            selected = !isMale,
                            onClick = { isMale = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Summary Hero Tiles
                item(key = "summary_tiles") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceHigh)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Жир (US Navy)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "${(((navyBf?.bodyFatPercentage ?: 15.0) * 10).toInt() / 10.0)}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentLime,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "Сухая: ${navyBf?.leanMassKg?.toInt() ?: 0} кг",
                                fontSize = 12.sp,
                                color = TextMuted,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .background(DarkSurfaceHigh)
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Индекс Адониса",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "${((adonis.shoulderToWaistRatio * 100).toInt() / 100.0)}",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Black,
                                color = AmberWarm,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                            Text(
                                text = "Цель: 1.618",
                                fontSize = 12.sp,
                                color = TextMuted,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                    }
                }

                // Casey Butt Natural Muscular Limit
                item(key = "natural_limit") {
                    GymInnerWell {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Натуральный потенциал (10% жира)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                            Text(
                                text = "до ${naturalLimit.maxWeightAt10PercentFatKg.toInt()} кг",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black,
                                color = AccentLime,
                                style = TextStyle(fontFeatureSettings = "tnum")
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            LimitStatPill(
                                label = "Бицепс",
                                value = "${naturalLimit.maxBicepsCm.toInt()} см",
                                modifier = Modifier.weight(1f)
                            )
                            LimitStatPill(
                                label = "Грудь",
                                value = "${naturalLimit.maxChestCm.toInt()} см",
                                modifier = Modifier.weight(1f)
                            )
                            LimitStatPill(
                                label = "Бедро",
                                value = "${naturalLimit.maxThighCm.toInt()} см",
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Inputs Row 1: Height & Weight
                item(key = "inputs_1") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymLabeledField(
                            label = "Рост, см",
                            value = heightCmText,
                            onValueChange = { heightCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymLabeledField(
                            label = "Вес, кг",
                            value = weightKgText,
                            onValueChange = { weightKgText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Inputs Row 2: Neck & Waist
                item(key = "inputs_2") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymLabeledField(
                            label = "Шея, см",
                            value = neckCmText,
                            onValueChange = { neckCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymLabeledField(
                            label = "Талия, см",
                            value = waistCmText,
                            onValueChange = { waistCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Inputs Row 3: Shoulder & Hip
                item(key = "inputs_3") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymLabeledField(
                            label = "Плечи, см",
                            value = shoulderCmText,
                            onValueChange = { shoulderCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymLabeledField(
                            label = "Бёдра, см",
                            value = hipCmText,
                            onValueChange = { hipCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Inputs Row 4: Wrist & Ankle
                item(key = "inputs_4") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymLabeledField(
                            label = "Запястье, см",
                            value = wristCmText,
                            onValueChange = { wristCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        GymLabeledField(
                            label = "Щиколотка, см",
                            value = ankleCmText,
                            onValueChange = { ankleCmText = it },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            GymPrimaryButton(
                text = "Сохранить антропометрию",
                onClick = {
                    onSaveMetrics(navyBf?.bodyFatPercentage ?: 15.0, adonis.shoulderToWaistRatio)
                    onDismiss()
                },
                fillWidth = true
            )
        }
    }
}

@Composable
private fun LimitStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = label,
            fontSize = 11.sp,
            color = TextMuted
        )
        Text(
            text = value,
            fontSize = 14.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            style = TextStyle(fontFeatureSettings = "tnum")
        )
    }
}
