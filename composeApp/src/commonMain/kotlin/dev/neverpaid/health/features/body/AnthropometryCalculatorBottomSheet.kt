package dev.neverpaid.health.features.body

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.neverpaid.health.core.designsystem.AmberGold
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CyanElectric
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceElevated
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.IndigoElectric
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
        containerColor = DarkBackground,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .navigationBarsPadding()
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Straighten,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = "Антропометрия и Золотое сечение",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = strings.cancel, tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth().height(420.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Gender Selection
                item {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            onClick = { isMale = true },
                            selected = isMale,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                                activeContentColor = EmeraldNeon,
                                inactiveContainerColor = DarkSurface,
                                inactiveContentColor = TextSecondary
                            )
                        ) {
                            Text("Мужчина", style = MaterialTheme.typography.labelSmall)
                        }
                        SegmentedButton(
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            onClick = { isMale = false },
                            selected = !isMale,
                            colors = SegmentedButtonDefaults.colors(
                                activeContainerColor = EmeraldNeon.copy(alpha = 0.2f),
                                activeContentColor = EmeraldNeon,
                                inactiveContainerColor = DarkSurface,
                                inactiveContentColor = TextSecondary
                            )
                        ) {
                            Text("Женщина", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Summary Cards
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Navy Body Fat % Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Жир (US Navy)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${(((navyBf?.bodyFatPercentage ?: 15.0) * 10).toInt() / 10.0)}%",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = EmeraldNeon
                                )
                                Text(
                                    text = "Сухая масса: ${navyBf?.leanMassKg?.toInt() ?: 0} кг",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }

                        // Adonis Ratio Card
                        Card(
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                            border = BorderStroke(1.dp, BorderSubtle)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "Индекс Адониса",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${((adonis.shoulderToWaistRatio * 100).toInt() / 100.0)}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = AmberGold
                                )
                                Text(
                                    text = "Цель: 1.618",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextMuted
                                )
                            }
                        }
                    }
                }

                // Casey Butt Natural Muscular Limit
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = DarkSurface),
                        border = BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Предел Кейси Батта (Natural Limit при 10% жира):",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                            Text(
                                text = "Максимальный вес: ${naturalLimit.maxWeightAt10PercentFatKg.toInt()} кг (сухой)",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = CyanElectric
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Бицепс: ${naturalLimit.maxBicepsCm.toInt()} см", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                Text("Грудь: ${naturalLimit.maxChestCm.toInt()} см", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                                Text("Бедро: ${naturalLimit.maxThighCm.toInt()} см", style = MaterialTheme.typography.labelSmall, color = TextPrimary)
                            }
                        }
                    }
                }

                // Inputs Row 1: Height & Weight
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = heightCmText,
                            onValueChange = { heightCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Рост (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = weightKgText,
                            onValueChange = { weightKgText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Вес (кг)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Inputs Row 2: Neck & Waist
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = neckCmText,
                            onValueChange = { neckCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Шея (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = waistCmText,
                            onValueChange = { waistCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Талия (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Inputs Row 3: Shoulder & Hip
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = shoulderCmText,
                            onValueChange = { shoulderCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Плечи (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = hipCmText,
                            onValueChange = { hipCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Бёдра (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Inputs Row 4: Wrist & Ankle (Bone Frame)
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = wristCmText,
                            onValueChange = { wristCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Запястье (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                        OutlinedTextField(
                            value = ankleCmText,
                            onValueChange = { ankleCmText = it },
                            modifier = Modifier.weight(1f),
                            label = { Text("Щиколотка (см)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            FilledTonalButton(
                onClick = {
                    onSaveMetrics(navyBf?.bodyFatPercentage ?: 15.0, adonis.shoulderToWaistRatio)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = EmeraldNeon,
                    contentColor = DarkBackground
                )
            ) {
                Text(
                    text = "Сохранить антропометрию",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
