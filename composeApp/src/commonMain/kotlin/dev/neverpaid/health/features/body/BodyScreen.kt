package dev.neverpaid.health.features.body

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.IndigoElectric
import dev.neverpaid.health.core.designsystem.PrimaryAthleticButton
import dev.neverpaid.health.core.designsystem.StatCounter
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.BodyMeasurement

@Composable
fun BodyRoute(
    viewModel: BodyViewModel,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

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
    if (state.isLoading) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = EmeraldNeon)
        }
        return
    }

    if (state.isAddDialogOpen) {
        AddMeasurementDialog(
            defaultWeight = state.latestWeightKg,
            defaultHeight = state.heightCm,
            onSave = { w, h, bf -> onAction(BodyUiAction.OnSaveMeasurement(w, h, bf)) },
            onDismiss = { onAction(BodyUiAction.OnDismissAddDialog) }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "header") {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Body & Health Metrics",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "Weight tracking, 7-day smoothed average & TDEE",
                fontSize = 13.sp,
                color = TextSecondary
            )
        }

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
                                text = "CURRENT WEIGHT",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextMuted,
                                letterSpacing = 1.sp
                            )
                        }

                        Text(
                            text = "SMOOTHED",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldNeon
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatCounter(
                            title = "Latest",
                            value = "${state.latestWeightKg}",
                            unit = "kg",
                            accentColor = TextPrimary
                        )
                        StatCounter(
                            title = "7-Day Moving Avg",
                            value = "${state.movingAverage7DayKg ?: state.latestWeightKg}",
                            unit = "kg",
                            accentColor = EmeraldNeon
                        )
                        StatCounter(
                            title = "Height",
                            value = "${state.heightCm.toInt()}",
                            unit = "cm",
                            accentColor = IndigoElectric
                        )
                    }
                }
            }
        }

        item(key = "bmi_tdee_row") {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                AthleticCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "BODY MASS INDEX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "${state.bmi}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = EmeraldNeon)
                        Text(text = "Healthy Athletic", fontSize = 11.sp, color = TextSecondary)
                    }
                }

                AthleticCard(
                    modifier = Modifier.weight(1f),
                    backgroundColor = DarkSurfaceVariant
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(text = "ESTIMATED TDEE", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(text = "${state.tdeeCalories}", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = IndigoElectric)
                        Text(text = "kcal / day", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        item(key = "log_button") {
            PrimaryAthleticButton(
                text = "+ Log Weigh-In",
                onClick = { onAction(BodyUiAction.OnOpenAddDialog) },
                icon = Icons.Default.Add
            )
        }

        item(key = "history_header") {
            Text(
                text = "LOGGED MEASUREMENTS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = TextMuted,
                letterSpacing = 1.sp
            )
        }

        items(
            items = state.measurements,
            key = { it.id }
        ) { m ->
            BodyMeasurementRow(measurement = m)
        }

        item(key = "spacer_bottom") {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BodyMeasurementRow(measurement: BodyMeasurement) {
    val dateStr = PlatformBridge.formatDate(measurement.dateEpochMs, "dd MMM yyyy")

    AthleticCard(backgroundColor = DarkSurfaceVariant) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = dateStr, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text(text = "BMI: ${measurement.bmi}", fontSize = 12.sp, color = TextMuted)
            }

            Text(
                text = "${measurement.weightKg} kg",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = EmeraldNeon
            )
        }
    }
}

@Composable
private fun AddMeasurementDialog(
    defaultWeight: Double,
    defaultHeight: Double,
    onSave: (Double, Double, Double?) -> Unit,
    onDismiss: () -> Unit
) {
    var weightInput by remember { mutableStateOf(defaultWeight.toString()) }
    var heightInput by remember { mutableStateOf(defaultHeight.toInt().toString()) }
    var bodyFatInput by remember { mutableStateOf("14.0") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Log Bodyweight", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = weightInput,
                    onValueChange = { weightInput = it },
                    label = { Text("Weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                OutlinedTextField(
                    value = heightInput,
                    onValueChange = { heightInput = it },
                    label = { Text("Height (cm)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSubtle
                    )
                )

                OutlinedTextField(
                    value = bodyFatInput,
                    onValueChange = { bodyFatInput = it },
                    label = { Text("Body Fat % (Optional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSubtle
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val w = weightInput.toDoubleOrNull() ?: defaultWeight
                    val h = heightInput.toDoubleOrNull() ?: defaultHeight
                    val bf = bodyFatInput.toDoubleOrNull()
                    onSave(w, h, bf)
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldNeon, contentColor = DarkBackground)
            ) {
                Text(text = "Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Cancel", color = TextSecondary)
            }
        },
        containerColor = DarkSurface
    )
}
