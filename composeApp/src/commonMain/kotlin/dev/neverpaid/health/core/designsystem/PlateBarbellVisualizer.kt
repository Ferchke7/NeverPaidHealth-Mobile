package dev.neverpaid.health.core.designsystem

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.math.BarbellType
import dev.neverpaid.health.core.math.PlateCalculationResult
import dev.neverpaid.health.core.math.PlateCalculatorEngine
import dev.neverpaid.health.core.math.PlateItem
import dev.neverpaid.health.core.math.PlateSpec

/**
 * Interactive Barbell Sleeve Canvas Component.
 * Visually renders standard Olympic/Powerlifting barbell sleeve with loaded color-coded plates.
 */
@Composable
fun BarbellSleeveCanvas(
    result: PlateCalculationResult,
    barbell: BarbellType,
    modifier: Modifier = Modifier,
    heightDp: Int = 180
) {
    val textMeasurer = rememberTextMeasurer()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(heightDp.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxWidth().height((heightDp - 20).dp).padding(horizontal = 16.dp)) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val centerY = canvasHeight / 2f

                // 1. Draw Shaft (Left side of the bar)
                val shaftWidth = 50f
                val shaftHeight = 16f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF64748B), Color(0xFF334155), Color(0xFF1E293B))
                    ),
                    topLeft = Offset(0f, centerY - (shaftHeight / 2f)),
                    size = Size(shaftWidth, shaftHeight),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // 2. Draw Inside Collar / Stopper
                val stopperWidth = 24f
                val stopperHeight = 70f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF334155))
                    ),
                    topLeft = Offset(shaftWidth, centerY - (stopperHeight / 2f)),
                    size = Size(stopperWidth, stopperHeight),
                    cornerRadius = CornerRadius(4f, 4f)
                )

                // 3. Draw Sleeve (Steel cylinder)
                val sleeveStartX = shaftWidth + stopperWidth
                val sleeveWidth = (canvasWidth - sleeveStartX - 20f).coerceAtLeast(180f)
                val sleeveHeight = 26f
                drawRoundRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFCBD5E1), Color(0xFF94A3B8), Color(0xFF64748B), Color(0xFF334155))
                    ),
                    topLeft = Offset(sleeveStartX, centerY - (sleeveHeight / 2f)),
                    size = Size(sleeveWidth, sleeveHeight),
                    cornerRadius = CornerRadius(3f, 3f)
                )

                // 4. Draw Loaded Plates from Inside to Outside
                var currentX = sleeveStartX + 4f
                val maxPlateDiameter = canvasHeight * 0.88f // scale max 450mm plate to canvas

                for (plateItem in result.platesPerSide) {
                    for (i in 0 until plateItem.count) {
                        val spec = plateItem.spec
                        // Scale plate height based on diameter (450mm = 100%)
                        val plateHeight = (spec.diameterMm / 450f) * maxPlateDiameter
                        // Scale plate width visually based on thickness
                        val plateWidth = (spec.thicknessMm / 65f * 24f).coerceIn(10f, 28f)

                        val plateColor = Color(spec.colorHex)
                        val topY = centerY - (plateHeight / 2f)

                        // Plate body gradient
                        drawRoundRect(
                            brush = Brush.horizontalGradient(
                                colors = listOf(
                                    plateColor.copy(alpha = 0.85f),
                                    plateColor,
                                    plateColor.copy(alpha = 0.7f)
                                ),
                                startX = currentX,
                                endX = currentX + plateWidth
                            ),
                            topLeft = Offset(currentX, topY),
                            size = Size(plateWidth, plateHeight),
                            cornerRadius = CornerRadius(3f, 3f)
                        )

                        // Outer border for bumper visibility
                        drawRoundRect(
                            color = Color(0x66000000),
                            topLeft = Offset(currentX, topY),
                            size = Size(plateWidth, plateHeight),
                            cornerRadius = CornerRadius(3f, 3f),
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1f)
                        )

                        // Inner Hub Ring for large plates
                        if (spec.diameterMm >= 350) {
                            val hubHeight = 32f
                            drawRoundRect(
                                color = Color(0x55000000),
                                topLeft = Offset(currentX + 2f, centerY - (hubHeight / 2f)),
                                size = Size((plateWidth - 4f).coerceAtLeast(2f), hubHeight),
                                cornerRadius = CornerRadius(2f, 2f)
                            )
                        }

                        // Plate Text Label (if plate width allows text)
                        if (plateWidth >= 14f && plateHeight >= 50f) {
                            val textLayout = textMeasurer.measure(
                                text = spec.labelText,
                                style = TextStyle(
                                    fontSize = if (plateHeight > 90f) 10.sp else 8.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (spec.colorHex == 0xFFF8FAFC) Color(0xFF0F172A) else Color.White
                                )
                            )
                            val textX = currentX + (plateWidth - textLayout.size.width) / 2f
                            val textY = topY + 8f
                            drawText(textLayout, topLeft = Offset(textX, textY))
                        }

                        currentX += plateWidth + 2f
                    }
                }

                // 5. Draw Lock Collar if used
                if (result.collarWeightKg > 0) {
                    val collarWidth = 14f
                    val collarHeight = 44f
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color(0xFFE2E8F0), Color(0xFF94A3B8), Color(0xFF475569))
                        ),
                        topLeft = Offset(currentX + 3f, centerY - (collarHeight / 2f)),
                        size = Size(collarWidth, collarHeight),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }
        }
    }
}

/**
 * Full Interactive Plate Calculator Widget.
 */
@Composable
fun InteractivePlateCalculator(
    initialWeightKg: Double = 100.0,
    oneRepMaxKg: Double? = null,
    onWeightConfirmed: ((Double) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var targetWeight by remember { mutableDoubleStateOf(initialWeightKg.coerceAtLeast(20.0)) }
    var selectedBarbell by remember { mutableStateOf(BarbellType.OLYMPIC_MENS) }
    var useCollars by remember { mutableStateOf(false) }

    val calcResult = remember(targetWeight, selectedBarbell, useCollars, oneRepMaxKg) {
        PlateCalculatorEngine.calculatePlates(
            targetWeightKg = targetWeight,
            barbell = selectedBarbell,
            useCollars = useCollars,
            oneRepMaxKg = oneRepMaxKg
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Target Weight & Quick Adjust
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "ЦЕЛЕВОЙ ВЕС НА ШТАНГЕ",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 1.sp
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${calcResult.targetWeightKg}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = EmeraldNeon
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "КГ",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    if (calcResult.percentageOf1RM != null) {
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(
                            modifier = Modifier
                                .padding(bottom = 6.dp)
                                .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
                                .border(1.dp, EmeraldNeon.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${calcResult.percentageOf1RM}% 1ПМ",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = EmeraldLight
                            )
                        }
                    }
                }
            }

            // Quick increment buttons
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                QuickWeightAdjustButton("-5") { targetWeight = (targetWeight - 5.0).coerceAtLeast(selectedBarbell.defaultWeightKg) }
                QuickWeightAdjustButton("-2.5") { targetWeight = (targetWeight - 2.5).coerceAtLeast(selectedBarbell.defaultWeightKg) }
                QuickWeightAdjustButton("+2.5") { targetWeight = targetWeight + 2.5 }
                QuickWeightAdjustButton("+5") { targetWeight = targetWeight + 5.0 }
                QuickWeightAdjustButton("+10") { targetWeight = targetWeight + 10.0 }
            }
        }

        // Barbell Sleeve Graphical Representation
        BarbellSleeveCanvas(
            result = calcResult,
            barbell = selectedBarbell
        )

        // Summary Breakdown Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "НА КАЖДУЮ СТОРОНУ:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary
                    )
                    Text(
                        text = "${calcResult.weightPerSideKg} кг",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                // Plate chips summary
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (calcResult.platesPerSide.isEmpty()) {
                        Text("Только пустой гриф", fontSize = 13.sp, color = TextMuted)
                    } else {
                        calcResult.platesPerSide.forEach { item ->
                            PlateBadgeChip(item)
                        }
                    }
                }

                if (!calcResult.isExactMatch && calcResult.remainingWeightKg > 0) {
                    Text(
                        text = "⚠️ Нехватка блинов для точного шага: осталось ${calcResult.remainingWeightKg} кг",
                        fontSize = 11.sp,
                        color = AmberWarning
                    )
                }
            }
        }

        // Barbell Selection Chips
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("ТИП ГРИФА", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BarbellType.entries.take(4).forEach { bType ->
                    FilterChip(
                        selected = selectedBarbell == bType,
                        onClick = {
                            selectedBarbell = bType
                            targetWeight = targetWeight.coerceAtLeast(bType.defaultWeightKg)
                        },
                        label = { Text("${bType.defaultWeightKg.toInt()} кг (${bType.displayNameRu.take(12)})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextPrimary
                        )
                    )
                }
            }
        }

        // Percentage Quick Fill (if 1RM is provided)
        if (oneRepMaxKg != null && oneRepMaxKg > 0) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("ПРОЦЕНТ ОТ 1ПМ (${oneRepMaxKg} кг)", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                val percentages = PlateCalculatorEngine.generateQuickPercentages(oneRepMaxKg)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    percentages.forEach { (pct, weight) ->
                        FilterChip(
                            selected = kotlin.math.abs(targetWeight - weight) < 0.1,
                            onClick = { targetWeight = weight },
                            label = { Text("$pct% ($weight кг)", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = BlueAccent,
                                selectedLabelColor = TextPrimary,
                                containerColor = DarkSurface,
                                labelColor = TextSecondary
                            )
                        )
                    }
                }
            }
        }

        // Confirm Button
        if (onWeightConfirmed != null) {
            PrimaryAthleticButton(
                text = "Установить ${calcResult.achievedTotalKg} кг в подход",
                onClick = { onWeightConfirmed(calcResult.achievedTotalKg) }
            )
        }
    }
}

@Composable
private fun QuickWeightAdjustButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(8.dp))
            .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, fontSize = 11.sp, fontWeight = FontWeight.Black, color = TextPrimary)
    }
}

@Composable
private fun PlateBadgeChip(item: PlateItem) {
    Row(
        modifier = Modifier
            .background(DarkSurfaceVariant, RoundedCornerShape(6.dp))
            .border(1.dp, Color(item.spec.colorHex).copy(alpha = 0.8f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(Color(item.spec.colorHex), CircleShape)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "${item.count}×${item.spec.labelText} кг",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}
