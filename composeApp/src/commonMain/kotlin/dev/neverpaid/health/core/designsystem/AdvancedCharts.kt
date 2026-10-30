package dev.neverpaid.health.core.designsystem

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import kotlin.math.PI
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

// ============================================================================
// 1. DATA MODELS FOR ADVANCED CHARTS
// ============================================================================

/**
 * Data point for the Interactive Volume Chart.
 * @param date ISO date or short label (e.g. "10/24" or "2026-10-24")
 * @param volumeKg Total tonnage in kilograms
 * @param sessionTitle Optional workout name (e.g. "Push Heavy Strength")
 * @param isPersonalRecord True if this session set an all-time volume record
 */
data class VolumeDataPoint(
    val date: String,
    val volumeKg: Double,
    val sessionTitle: String = "",
    val isPersonalRecord: Boolean = false
)

/**
 * Single day entry in the 84-day (12 weeks) consistency heatmap.
 * @param date ISO date "YYYY-MM-DD"
 * @param tier Workout intensity level from 0 to 4
 * @param volumeKg Total session tonnage
 * @param workoutCount Number of workouts completed on this date
 */
data class HeatmapDayEntry(
    val date: String,
    val tier: Int, // 0 = rest, 1 = light/cardio, 2 = standard, 3 = heavy, 4 = elite volume
    val volumeKg: Double = 0.0,
    val workoutCount: Int = 0
)

/**
 * Macro target and progress container for concentric arc gauges.
 */
data class MacroTargetProgress(
    val currentCalories: Double,
    val targetCalories: Double,
    val currentProteinGrams: Double,
    val targetProteinGrams: Double,
    val currentCarbsGrams: Double,
    val targetCarbsGrams: Double,
    val currentFatsGrams: Double,
    val targetFatsGrams: Double
)

// ============================================================================
// 2. INTERACTIVE VOLUME CHART (SMOOTH BEZIER + SCRUBBER)
// ============================================================================

/**
 * High-performance Compose Canvas chart showing volume progression.
 * Features:
 * - Cubic Bezier spline smoothing for organic curves
 * - Neon gradient area fill
 * - Dynamic Y-axis gridlines and tonnage labels
 * - Interactive horizontal touch drag scrubber with snapped data badge
 * - Glowing milestone markers for PR sessions
 */
@Composable
fun InteractiveVolumeChart(
    dataPoints: List<VolumeDataPoint>,
    modifier: Modifier = Modifier,
    lineColor: Color = EmeraldNeon,
    gradientStartColor: Color = EmeraldGlow,
    gradientEndColor: Color = Color.Transparent,
    gridColor: Color = BorderSubtle,
    textColor: Color = TextSecondary
) {
    val strings = LocalDudaStrings.current
    val textMeasurer = rememberTextMeasurer()

    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    if (dataPoints.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(DarkSurfaceVariant, RoundedCornerShape(16.dp))
                .border(1.dp, BorderSubtle, RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = strings.noData,
                style = MaterialTheme.typography.bodyMedium,
                color = TextMuted
            )
        }
        return
    }

    val maxVolume = remember(dataPoints) {
        val maxVal = dataPoints.maxOfOrNull { it.volumeKg } ?: 1000.0
        // Round up to nice readable ceiling
        if (maxVal <= 0.0) 1000.0 else maxVal * 1.15
    }
    val minVolume = 0.0

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Selected Scrubber Header readout
            val activePoint = selectedIndex?.let { dataPoints.getOrNull(it) } ?: dataPoints.lastOrNull()
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = activePoint?.sessionTitle?.ifBlank { strings.volumeTonnage } ?: strings.volumeTonnage,
                        style = MaterialTheme.typography.labelMedium,
                        color = TextSecondary
                    )
                    Text(
                        text = "${((activePoint?.volumeKg ?: 0.0)).toInt()} ${strings.unitKg}",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (activePoint?.isPersonalRecord == true) AmberGold else EmeraldNeon
                    )
                }

                if (activePoint != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurfaceElevated,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
                    ) {
                        Text(
                            text = activePoint.date,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextPrimary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Main Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(dataPoints) {
                            detectDragGestures(
                                onDragStart = { offset ->
                                    val pointSpacing = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                    val index = ((offset.x - 30.dp.toPx()) / pointSpacing)
                                        .toInt()
                                        .coerceIn(0, dataPoints.size - 1)
                                    selectedIndex = index
                                },
                                onDrag = { change, _ ->
                                    change.consume()
                                    val pointSpacing = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                    val index = ((change.position.x - 30.dp.toPx()) / pointSpacing)
                                        .toInt()
                                        .coerceIn(0, dataPoints.size - 1)
                                    selectedIndex = index
                                },
                                onDragEnd = {
                                    // Keep selected point visible
                                },
                                onDragCancel = {
                                    // Retain or reset
                                }
                            )
                        }
                        .pointerInput(dataPoints) {
                            detectTapGestures { offset ->
                                val pointSpacing = size.width / (dataPoints.size - 1).coerceAtLeast(1)
                                val index = ((offset.x - 30.dp.toPx()) / pointSpacing)
                                    .toInt()
                                    .coerceIn(0, dataPoints.size - 1)
                                selectedIndex = index
                            }
                        }
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height
                    val leftPadding = 40.dp.toPx()
                    val rightPadding = 16.dp.toPx()
                    val topPadding = 16.dp.toPx()
                    val bottomPadding = 24.dp.toPx()

                    val chartWidth = canvasWidth - leftPadding - rightPadding
                    val chartHeight = canvasHeight - topPadding - bottomPadding

                    // Draw Horizontal Gridlines (4 levels: 0%, 33%, 66%, 100%)
                    val gridLevels = 4
                    for (i in 0..gridLevels) {
                        val fraction = i.toFloat() / gridLevels
                        val y = topPadding + chartHeight * (1f - fraction)
                        val levelVolume = minVolume + (maxVolume - minVolume) * fraction

                        // Grid line
                        drawLine(
                            color = gridColor,
                            start = Offset(leftPadding, y),
                            end = Offset(canvasWidth - rightPadding, y),
                            strokeWidth = 1f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                        )

                        // Y-Axis Label
                        val label = if (levelVolume >= 1000.0) {
                            "${(levelVolume / 1000.0).toInt()}k"
                        } else {
                            "${levelVolume.toInt()}"
                        }
                        drawText(
                            textMeasurer = textMeasurer,
                            text = label,
                            topLeft = Offset(4.dp.toPx(), y - 8.dp.toPx()),
                            style = TextStyle(
                                color = textColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }

                    if (dataPoints.size < 2) {
                        // Single point representation
                        val x = leftPadding + chartWidth / 2f
                        val y = topPadding + chartHeight * (1f - ((dataPoints[0].volumeKg - minVolume) / (maxVolume - minVolume)).toFloat())
                        drawCircle(
                            color = lineColor,
                            radius = 6.dp.toPx(),
                            center = Offset(x, y)
                        )
                        return@Canvas
                    }

                    // Calculate Coordinates for points
                    val points = dataPoints.mapIndexed { index, dp ->
                        val x = leftPadding + (index.toFloat() / (dataPoints.size - 1)) * chartWidth
                        val normalizedY = ((dp.volumeKg - minVolume) / (maxVolume - minVolume)).coerceIn(0.0, 1.0).toFloat()
                        val y = topPadding + chartHeight * (1f - normalizedY)
                        Offset(x, y)
                    }

                    // Construct Smooth Cubic Bezier Line and Fill Paths
                    val strokePath = Path().apply {
                        moveTo(points.first().x, points.first().y)
                        for (i in 0 until points.size - 1) {
                            val p0 = points[i]
                            val p1 = points[i + 1]
                            val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                            val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)
                            cubicTo(controlPoint1.x, controlPoint1.y, controlPoint2.x, controlPoint2.y, p1.x, p1.y)
                        }
                    }

                    val fillPath = Path().apply {
                        addPath(strokePath)
                        lineTo(points.last().x, topPadding + chartHeight)
                        lineTo(points.first().x, topPadding + chartHeight)
                        close()
                    }

                    // Draw Gradient Area Under Curve
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(gradientStartColor, gradientEndColor),
                            startY = topPadding,
                            endY = topPadding + chartHeight
                        ),
                        style = Fill
                    )

                    // Draw Main Glowing Line
                    drawPath(
                        path = strokePath,
                        color = lineColor,
                        style = Stroke(
                            width = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    )

                    // Draw Milestone PR Badges / Points
                    points.forEachIndexed { index, point ->
                        val dp = dataPoints[index]
                        if (dp.isPersonalRecord) {
                            // Gold star / outer halo
                            drawCircle(
                                color = AmberGold.copy(alpha = 0.35f),
                                radius = 9.dp.toPx(),
                                center = point
                            )
                            drawCircle(
                                color = AmberGold,
                                radius = 4.5.dp.toPx(),
                                center = point
                            )
                        } else {
                            drawCircle(
                                color = lineColor,
                                radius = 3.dp.toPx(),
                                center = point
                            )
                        }
                    }

                    // Active Scrubber Crosshair Line & Beacon
                    val activeIdx = selectedIndex
                    if (activeIdx != null && activeIdx in points.indices) {
                        val activePointCoord = points[activeIdx]

                        // Vertical dashed indicator
                        drawLine(
                            color = lineColor.copy(alpha = 0.8f),
                            start = Offset(activePointCoord.x, topPadding),
                            end = Offset(activePointCoord.x, topPadding + chartHeight),
                            strokeWidth = 1.5.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 6f), 0f)
                        )

                        // Glowing Beacon on Curve
                        drawCircle(
                            color = lineColor.copy(alpha = 0.25f),
                            radius = 12.dp.toPx(),
                            center = activePointCoord
                        )
                        drawCircle(
                            color = DarkBackground,
                            radius = 6.dp.toPx(),
                            center = activePointCoord
                        )
                        drawCircle(
                            color = lineColor,
                            radius = 4.dp.toPx(),
                            center = activePointCoord
                        )
                    }

                    // Draw Date X-Axis Markers for first, middle, last
                    val xIndices = listOf(0, dataPoints.size / 2, dataPoints.size - 1).distinct()
                    xIndices.forEach { idx ->
                        val pt = points[idx]
                        val dateText = dataPoints[idx].date.takeLast(5)
                        drawText(
                            textMeasurer = textMeasurer,
                            text = dateText,
                            topLeft = Offset(pt.x - 14.dp.toPx(), topPadding + chartHeight + 6.dp.toPx()),
                            style = TextStyle(
                                color = textColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 3. CONSISTENCY HEATMAP (84-DAY / 12-WEEK ATHLETIC MATRIX)
// ============================================================================

/**
 * 84-day (12 weeks × 7 days) athletic discipline heatmap inspired by GitHub & Strava.
 * Color intensity scales automatically by session volume.
 * Provides interactive tap inspection for any day in the quarter.
 */
@Composable
fun ConsistencyHeatmap84Day(
    days: List<HeatmapDayEntry>,
    modifier: Modifier = Modifier,
    onDayClick: (HeatmapDayEntry) -> Unit = {}
) {
    val strings = LocalDudaStrings.current
    var selectedDay by remember { mutableStateOf<HeatmapDayEntry?>(null) }

    // Precompute 12 weeks of 7 days
    val weeksCount = 12
    val daysInWeek = 7
    val totalSlots = weeksCount * daysInWeek

    // Pad or trim input list to strictly 84 slots
    val paddedDays = remember(days) {
        val list = days.takeLast(totalSlots).toMutableList()
        while (list.size < totalSlots) {
            list.add(0, HeatmapDayEntry(date = "", tier = 0, volumeKg = 0.0, workoutCount = 0))
        }
        list
    }

    val totalWorkouts = remember(paddedDays) {
        paddedDays.sumOf { it.workoutCount }
    }

    val activeStreak = remember(paddedDays) {
        var streak = 0
        for (i in paddedDays.indices.reversed()) {
            if (paddedDays[i].workoutCount > 0) {
                streak++
            } else if (paddedDays[i].date.isNotEmpty()) {
                break
            }
        }
        streak
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Streak & Total Workout count
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = strings.consistencyHeatmap,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "$totalWorkouts ${strings.workoutsDone} (12 ${strings.weeksUnit})",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AmberWarm.copy(alpha = 0.15f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, AmberWarm.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = AmberGold,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "$activeStreak ${strings.daysStreak}",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = AmberGold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Canvas Grid
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(paddedDays) {
                            detectTapGestures { offset ->
                                val cellSize = 11.dp.toPx()
                                val gap = 3.dp.toPx()
                                val weekWidth = cellSize + gap
                                val dayHeight = cellSize + gap

                                val weekIndex = (offset.x / weekWidth).toInt().coerceIn(0, weeksCount - 1)
                                val dayIndex = (offset.y / dayHeight).toInt().coerceIn(0, daysInWeek - 1)

                                val flatIndex = weekIndex * daysInWeek + dayIndex
                                if (flatIndex in paddedDays.indices) {
                                    val entry = paddedDays[flatIndex]
                                    if (entry.date.isNotEmpty()) {
                                        selectedDay = entry
                                        onDayClick(entry)
                                    }
                                }
                            }
                        }
                ) {
                    val cellSize = 11.dp.toPx()
                    val gap = 3.dp.toPx()

                    for (w in 0 until weeksCount) {
                        for (d in 0 until daysInWeek) {
                            val flatIdx = w * daysInWeek + d
                            val entry = paddedDays.getOrElse(flatIdx) { HeatmapDayEntry("", 0) }

                            val x = w * (cellSize + gap)
                            val y = d * (cellSize + gap)

                            val color = when (entry.tier) {
                                1 -> EmeraldDark.copy(alpha = 0.45f)
                                2 -> EmeraldNeon.copy(alpha = 0.70f)
                                3 -> EmeraldNeon
                                4 -> AmberGold
                                else -> DarkSurfaceElevated
                            }

                            val isSelected = selectedDay?.date == entry.date && entry.date.isNotEmpty()

                            drawRoundRect(
                                color = color,
                                topLeft = Offset(x, y),
                                size = Size(cellSize, cellSize),
                                cornerRadius = CornerRadius(2.5.dp.toPx())
                            )

                            if (isSelected) {
                                drawRoundRect(
                                    color = Color.White,
                                    topLeft = Offset(x - 1.dp.toPx(), y - 1.dp.toPx()),
                                    size = Size(cellSize + 2.dp.toPx(), cellSize + 2.dp.toPx()),
                                    cornerRadius = CornerRadius(3.5.dp.toPx()),
                                    style = Stroke(width = 1.5.dp.toPx())
                                )
                            }
                        }
                    }
                }
            }

            // Legend Row & Micro Tooltip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selected Day Micro Badge
                if (selectedDay != null && selectedDay?.date?.isNotEmpty() == true) {
                    Text(
                        text = "${selectedDay?.date}: ${selectedDay?.workoutCount} ${strings.workoutsDone} (${selectedDay?.volumeKg?.toInt()} kg)",
                        style = MaterialTheme.typography.labelSmall,
                        color = EmeraldNeon,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = strings.lessMoreLegend,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }

                // Legend squares
                Row(
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(
                        DarkSurfaceElevated,
                        EmeraldDark.copy(alpha = 0.45f),
                        EmeraldNeon.copy(alpha = 0.70f),
                        EmeraldNeon,
                        AmberGold
                    ).forEach { col ->
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(col)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. CONCENTRIC MACRONUTRIENT DONUT GAUGE
// ============================================================================

/**
 * High-precision multi-ring concentric donut gauge for daily nutrition.
 * Outer Ring: Calories (Total Daily Burn vs Target)
 * Middle Ring: Protein (Muscle Synthesis Target)
 * Inner-1 Ring: Carbs (Glycogen Replenishment)
 * Inner-2 Ring: Healthy Fats (Endocrine Target)
 */
@Composable
fun MacronutrientDonutGauge(
    progress: MacroTargetProgress,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current

    val animCalorieProgress by animateFloatAsState(
        targetValue = (progress.currentCalories / progress.targetCalories.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.25f),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
    )
    val animProteinProgress by animateFloatAsState(
        targetValue = (progress.currentProteinGrams / progress.targetProteinGrams.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.25f),
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
    )
    val animCarbsProgress by animateFloatAsState(
        targetValue = (progress.currentCarbsGrams / progress.targetCarbsGrams.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.25f),
        animationSpec = tween(durationMillis = 1000, easing = FastOutSlowInEasing)
    )
    val animFatsProgress by animateFloatAsState(
        targetValue = (progress.currentFatsGrams / progress.targetFatsGrams.coerceAtLeast(1.0)).toFloat().coerceIn(0f, 1.25f),
        animationSpec = tween(durationMillis = 1100, easing = FastOutSlowInEasing)
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Concentric Canvas Donut
            Box(
                modifier = Modifier.size(150.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 8.dp.toPx()
                    val gap = 3.dp.toPx()

                    val radius0 = (size.minDimension / 2f) - (strokeWidth / 2f)
                    val radius1 = radius0 - strokeWidth - gap
                    val radius2 = radius1 - strokeWidth - gap
                    val radius3 = radius2 - strokeWidth - gap

                    // Draw Tracks (background track rings)
                    listOf(radius0, radius1, radius2, radius3).forEach { r ->
                        drawCircle(
                            color = DarkSurfaceElevated,
                            radius = r,
                            style = Stroke(width = strokeWidth)
                        )
                    }

                    // 1. Outer Ring: Calories (Emerald Neon)
                    val calSweep = animCalorieProgress * 360f
                    drawArc(
                        color = if (animCalorieProgress > 1.0f) AmberWarm else EmeraldNeon,
                        startAngle = -90f,
                        sweepAngle = calSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius0, center.y - radius0),
                        size = Size(radius0 * 2, radius0 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Protein Ring: Indigo Electric
                    val protSweep = animProteinProgress * 360f
                    drawArc(
                        color = IndigoElectric,
                        startAngle = -90f,
                        sweepAngle = protSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius1, center.y - radius1),
                        size = Size(radius1 * 2, radius1 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 3. Carbs Ring: Cyan Electric
                    val carbsSweep = animCarbsProgress * 360f
                    drawArc(
                        color = CyanElectric,
                        startAngle = -90f,
                        sweepAngle = carbsSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius2, center.y - radius2),
                        size = Size(radius2 * 2, radius2 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 4. Fats Ring: Purple Accent
                    val fatsSweep = animFatsProgress * 360f
                    drawArc(
                        color = PurpleAccent,
                        startAngle = -90f,
                        sweepAngle = fatsSweep,
                        useCenter = false,
                        topLeft = Offset(center.x - radius3, center.y - radius3),
                        size = Size(radius3 * 2, radius3 * 2),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                // Center Readout
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "${progress.currentCalories.toInt()}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "/ ${progress.targetCalories.toInt()} ${strings.unitKcal}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Legend Readouts
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MacroLegendRow(
                    label = strings.protein,
                    current = progress.currentProteinGrams,
                    target = progress.targetProteinGrams,
                    color = IndigoElectric
                )
                MacroLegendRow(
                    label = strings.carbs,
                    current = progress.currentCarbsGrams,
                    target = progress.targetCarbsGrams,
                    color = CyanElectric
                )
                MacroLegendRow(
                    label = strings.fats,
                    current = progress.currentFatsGrams,
                    target = progress.targetFatsGrams,
                    color = PurpleAccent
                )
            }
        }
    }
}

@Composable
private fun MacroLegendRow(
    label: String,
    current: Double,
    target: Double,
    color: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
        }
        Text(
            text = "${current.toInt()} / ${target.toInt()} g",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
    }
}

// ============================================================================
// 5. ANIMATED WATER HYDRATION WAVE VESSEL
// ============================================================================

/**
 * Animated Canvas water vessel with realistic sinusoidal wave movement,
 * bubbly particles, and instant +250ml / +500ml hydration logging buttons.
 */
@Composable
fun WaterHydrationWave(
    currentMl: Int,
    targetMl: Int,
    onAddWater: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current

    // Infinite wave phase animation
    val infiniteTransition = rememberInfiniteTransition(label = "waveTransition")
    val phase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2400, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    val progress = (currentMl.toFloat() / targetMl.coerceAtLeast(1).toFloat()).coerceIn(0f, 1f)
    val animFillHeight by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "fillHeight"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderSubtle)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
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
                    Icon(
                        imageVector = Icons.Default.WaterDrop,
                        contentDescription = null,
                        tint = CyanElectric,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = strings.waterHydration,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "$currentMl / $targetMl ml (${(progress * 100).toInt()}%)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = CyanElectric
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Wave Vessel Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceElevated)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(12.dp))
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val waterLevelY = h * (1f - animFillHeight)

                    val waveAmplitude = 4.dp.toPx()
                    val waveFrequency = (2f * PI.toFloat()) / w

                    // Primary Wave Path
                    val wavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, waterLevelY)

                        var x = 0f
                        while (x <= w) {
                            val y = waterLevelY + sin(x * waveFrequency + phase) * waveAmplitude
                            lineTo(x, y)
                            x += 4f
                        }

                        lineTo(w, h)
                        close()
                    }

                    // Secondary Sub-Wave (shifted phase for depth)
                    val subWavePath = Path().apply {
                        moveTo(0f, h)
                        lineTo(0f, waterLevelY)

                        var x = 0f
                        while (x <= w) {
                            val y = waterLevelY + sin(x * waveFrequency + phase + 1.2f) * (waveAmplitude * 0.7f)
                            lineTo(x, y)
                            x += 4f
                        }

                        lineTo(w, h)
                        close()
                    }

                    // Draw sub-wave
                    drawPath(
                        path = subWavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanElectric.copy(alpha = 0.35f), IndigoElectric.copy(alpha = 0.25f)),
                            startY = waterLevelY,
                            endY = h
                        )
                    )

                    // Draw primary wave
                    drawPath(
                        path = wavePath,
                        brush = Brush.verticalGradient(
                            colors = listOf(CyanElectric.copy(alpha = 0.85f), IndigoDark.copy(alpha = 0.75f)),
                            startY = waterLevelY,
                            endY = h
                        )
                    )

                    // Target Line Indicator at 100%
                    drawLine(
                        color = EmeraldNeon.copy(alpha = 0.5f),
                        start = Offset(0f, 6.dp.toPx()),
                        end = Offset(w, 6.dp.toPx()),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f), 0f)
                    )
                }

                // Hydration Quick Action Buttons inside/over the card
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = { onAddWater(250) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DarkSurface.copy(alpha = 0.75f),
                            contentColor = CyanElectric
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+250 ml", style = MaterialTheme.typography.labelSmall)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    FilledTonalButton(
                        onClick = { onAddWater(500) },
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.filledTonalButtonColors(
                            containerColor = DarkSurface.copy(alpha = 0.75f),
                            contentColor = CyanElectric
                        ),
                        modifier = Modifier.height(34.dp)
                    ) {
                        Text("+500 ml", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
