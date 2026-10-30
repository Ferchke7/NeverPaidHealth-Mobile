package dev.neverpaid.health.core.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Muscle group identifier and scientific hypertrophy guidelines.
 */
enum class MuscleZone(
    val id: String,
    val nameRu: String,
    val nameUz: String,
    val nameEn: String,
    val minOptimalWeeklySets: Int = 10,
    val maxOptimalWeeklySets: Int = 20,
    val recoveryHours: Int = 48,
    val isFront: Boolean = true
) {
    CHEST("chest", "Грудные мышцы (Пекторальные)", "Ko'krak mushaklari", "Chest / Pectorals", 12, 22, 48, true),
    FRONT_DELTS("shoulders_front", "Передние дельты", "Old deltasimon mushaklar", "Anterior Deltoids", 8, 16, 48, true),
    SIDE_DELTS("shoulders_side", "Средние (боковые) дельты", "Yon deltasimon mushaklar", "Lateral Deltoids", 12, 24, 36, true),
    BICEPS("biceps", "Двуглавая мышца (Бицепс)", "Biceps mushagi", "Biceps Brachii", 10, 18, 48, true),
    FOREARMS("forearms", "Предплечья и хват", "Bilak mushaklari", "Forearms & Grip", 8, 16, 36, true),
    ABS("abs", "Прямая мышца живота (Пресс)", "Qorin mushaklari (Press)", "Rectus Abdominis / Core", 10, 20, 36, true),
    OBLIQUES("obliques", "Косые мышцы живота", "Qiya qorin mushaklari", "Obliques", 8, 16, 36, true),
    QUADS("quads", "Четырёхглавая мышца (Квадрицепс)", "Sonning oldi (Kvadritseps)", "Quadriceps", 12, 22, 72, true),
    CALVES("calves", "Икроножные и камбаловидные", "Boldir mushaklari", "Gastrocnemius & Soleus", 10, 20, 36, true),

    // Back Zone
    TRAPS("traps", "Трапециевидные мышцы", "Trapeziya mushaklari", "Trapezius", 10, 18, 48, false),
    LATS("lats", "Широчайшие мышцы спины", "Keng orqa mushaklar (Qanot)", "Latissimus Dorsi", 12, 22, 48, false),
    REAR_DELTS("rear_delts", "Задние дельты", "Orqa deltasimon mushaklar", "Posterior Deltoids", 12, 22, 36, false),
    TRICEPS("triceps", "Трёхглавая мышца (Трицепс)", "Triceps mushagi", "Triceps Brachii", 10, 18, 48, false),
    LOWER_BACK("lower_back", "Разгибатели спины (Поясница)", "Bel cho'zuvchi mushaklari", "Erector Spinae / Lower Back", 6, 14, 72, false),
    GLUTES("glutes", "Ягодичные мышцы", "Dumba mushaklari", "Gluteus Maximus & Medius", 10, 20, 72, false),
    HAMSTRINGS("hamstrings", "Задняя поверхность бедра", "Sonning orqa mushaklari", "Hamstrings", 10, 18, 72, false)
}

/**
 * Weekly volume tracking model per muscle group.
 */
data class MuscleVolumeData(
    val zone: MuscleZone,
    val completedWeeklySets: Int = 0,
    val exercisesLogged: List<String> = emptyList(),
    val lastTrainedDate: String? = null
) {
    val status: VolumeStatus
        get() = when {
            completedWeeklySets == 0 -> VolumeStatus.UNTRAINED
            completedWeeklySets < zone.minOptimalWeeklySets -> VolumeStatus.UNDER_TRAINED
            completedWeeklySets <= zone.maxOptimalWeeklySets -> VolumeStatus.OPTIMAL
            else -> VolumeStatus.OVER_REACHED
        }

    val progressRatio: Float
        get() = (completedWeeklySets.toFloat() / zone.maxOptimalWeeklySets.toFloat()).coerceIn(0f, 1.25f)
}

enum class VolumeStatus(
    val titleRu: String,
    val titleUz: String,
    val titleEn: String,
    val color: Color
) {
    UNTRAINED("Не тренировалась", "Shug'ullanilmagan", "Untrained", Color(0xFF27272A)),
    UNDER_TRAINED("Ниже оптимального", "Optimaldan past", "Maintenance / Low", Color(0xFFF59E0B)),
    OPTIMAL("Оптимальный рост (Hypertrophy)", "Optimal o'sish", "Optimal Hypertrophy", Color(0xFF22C55E)),
    OVER_REACHED("Высокое утомление", "Yuqori charchoq", "Overreaching / High Fatigue", Color(0xFFEF4444))
}

/**
 * 2D Interactive Vector Muscle Anatomy Heatmap Component.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InteractiveAnatomyHeatmap(
    weeklyMuscleVolumes: Map<String, Int> = emptyMap(),
    onMuscleSelected: ((MuscleZone) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var isFrontView by remember { mutableStateOf(true) }
    var inspectedMuscle by remember { mutableStateOf<MuscleZone?>(null) }

    // Map raw muscle strings to MuscleVolumeData
    val fullData = remember(weeklyMuscleVolumes) {
        MuscleZone.entries.associateWith { zone ->
            val sets = weeklyMuscleVolumes[zone.id]
                ?: weeklyMuscleVolumes[zone.id.replace("_front", "").replace("_side", "")]
                ?: 0
            MuscleVolumeData(zone = zone, completedWeeklySets = sets)
        }
    }

    // Modal Sheet for Inspected Muscle
    inspectedMuscle?.let { zone ->
        val data = fullData[zone] ?: MuscleVolumeData(zone)
        MuscleDetailBottomSheet(
            data = data,
            onDismiss = { inspectedMuscle = null }
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header with View Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "МЫШЕЧНЫЙ ТЕПЛОВОЙ АТЛАС",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isFrontView) "Фронтальный вид (Спереди)" else "Дорсальный вид (Со спины)",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = isFrontView,
                        onClick = { isFrontView = true },
                        label = { Text("Спереди", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextPrimary
                        )
                    )
                    FilterChip(
                        selected = !isFrontView,
                        onClick = { isFrontView = false },
                        label = { Text("Сзади", fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldNeon,
                            selectedLabelColor = DarkBackground,
                            containerColor = DarkSurfaceVariant,
                            labelColor = TextPrimary
                        )
                    )
                }
            }

            // Vector Body Canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .background(DarkBackground, RoundedCornerShape(16.dp))
                    .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .pointerInput(isFrontView, fullData) {
                            detectTapGestures { offset ->
                                val tapped = detectMuscleTap(
                                    offset = offset,
                                    canvasWidth = size.width.toFloat(),
                                    canvasHeight = size.height.toFloat(),
                                    isFront = isFrontView
                                )
                                if (tapped != null) {
                                    inspectedMuscle = tapped
                                    onMuscleSelected?.invoke(tapped)
                                }
                            }
                        }
                ) {
                    val w = size.width
                    val h = size.height
                    val cx = w / 2f

                    if (isFrontView) {
                        drawFrontBody(cx = cx, h = h, data = fullData)
                    } else {
                        drawBackBody(cx = cx, h = h, data = fullData)
                    }
                }
            }

            // Legend Indicator Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendPill("0 подходов", VolumeStatus.UNTRAINED.color)
                LegendPill("1-9 подх.", VolumeStatus.UNDER_TRAINED.color)
                LegendPill("10-20 подх. (Опт.)", VolumeStatus.OPTIMAL.color)
                LegendPill("20+ подх.", VolumeStatus.OVER_REACHED.color)
            }
        }
    }
}

/**
 * Draws Front Musculature on Canvas with exact proportional bounding zones.
 */
private fun DrawScope.drawFrontBody(cx: Float, h: Float, data: Map<MuscleZone, MuscleVolumeData>) {
    val scale = h / 320f
    val headY = 22f * scale
    val neckY = 40f * scale
    val chestY = 62f * scale
    val absY = 105f * scale
    val hipsY = 155f * scale
    val kneesY = 225f * scale
    val calvesY = 275f * scale

    // Head Outline
    drawCircle(
        color = Color(0xFF3F3F46),
        radius = 16f * scale,
        center = Offset(cx, headY)
    )

    // Neck
    drawRoundRect(
        color = Color(0xFF3F3F46),
        topLeft = Offset(cx - 8f * scale, headY + 12f * scale),
        size = Size(16f * scale, 16f * scale),
        cornerRadius = CornerRadius(2f, 2f)
    )

    // Shoulders (Front & Side Delts)
    val deltColor = data[MuscleZone.FRONT_DELTS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Delt
    drawOval(
        color = deltColor,
        topLeft = Offset(cx - 58f * scale, neckY + 4f * scale),
        size = Size(24f * scale, 30f * scale)
    )
    // Right Delt
    drawOval(
        color = deltColor,
        topLeft = Offset(cx + 34f * scale, neckY + 4f * scale),
        size = Size(24f * scale, 30f * scale)
    )

    // Chest (Pectorals)
    val chestColor = data[MuscleZone.CHEST]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Pec
    drawRoundRect(
        color = chestColor,
        topLeft = Offset(cx - 36f * scale, chestY),
        size = Size(34f * scale, 36f * scale),
        cornerRadius = CornerRadius(6f * scale, 6f * scale)
    )
    // Right Pec
    drawRoundRect(
        color = chestColor,
        topLeft = Offset(cx + 2f * scale, chestY),
        size = Size(34f * scale, 36f * scale),
        cornerRadius = CornerRadius(6f * scale, 6f * scale)
    )

    // Biceps
    val bicepColor = data[MuscleZone.BICEPS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Bicep
    drawOval(
        color = bicepColor,
        topLeft = Offset(cx - 62f * scale, chestY + 16f * scale),
        size = Size(18f * scale, 34f * scale)
    )
    // Right Bicep
    drawOval(
        color = bicepColor,
        topLeft = Offset(cx + 44f * scale, chestY + 16f * scale),
        size = Size(18f * scale, 34f * scale)
    )

    // Forearms
    val forearmColor = data[MuscleZone.FOREARMS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Forearm
    drawOval(
        color = forearmColor,
        topLeft = Offset(cx - 72f * scale, absY + 2f * scale),
        size = Size(16f * scale, 44f * scale)
    )
    // Right Forearm
    drawOval(
        color = forearmColor,
        topLeft = Offset(cx + 56f * scale, absY + 2f * scale),
        size = Size(16f * scale, 44f * scale)
    )

    // Abs & Core
    val absColor = data[MuscleZone.ABS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    for (row in 0..2) {
        val rowY = absY + (row * 15f * scale)
        // Left pack
        drawRoundRect(
            color = absColor,
            topLeft = Offset(cx - 16f * scale, rowY),
            size = Size(14f * scale, 12f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale)
        )
        // Right pack
        drawRoundRect(
            color = absColor,
            topLeft = Offset(cx + 2f * scale, rowY),
            size = Size(14f * scale, 12f * scale),
            cornerRadius = CornerRadius(3f * scale, 3f * scale)
        )
    }

    // Obliques
    val obliquesColor = data[MuscleZone.OBLIQUES]?.status?.color ?: VolumeStatus.UNTRAINED.color
    drawRoundRect(
        color = obliquesColor,
        topLeft = Offset(cx - 30f * scale, absY + 4f * scale),
        size = Size(12f * scale, 38f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale)
    )
    drawRoundRect(
        color = obliquesColor,
        topLeft = Offset(cx + 18f * scale, absY + 4f * scale),
        size = Size(12f * scale, 38f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale)
    )

    // Quads (Quadriceps)
    val quadColor = data[MuscleZone.QUADS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Quad
    drawRoundRect(
        color = quadColor,
        topLeft = Offset(cx - 38f * scale, hipsY),
        size = Size(34f * scale, 70f * scale),
        cornerRadius = CornerRadius(10f * scale, 10f * scale)
    )
    // Right Quad
    drawRoundRect(
        color = quadColor,
        topLeft = Offset(cx + 4f * scale, hipsY),
        size = Size(34f * scale, 70f * scale),
        cornerRadius = CornerRadius(10f * scale, 10f * scale)
    )

    // Calves & Shins
    val calfColor = data[MuscleZone.CALVES]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Calf
    drawRoundRect(
        color = calfColor,
        topLeft = Offset(cx - 32f * scale, kneesY + 12f * scale),
        size = Size(24f * scale, 65f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
    // Right Calf
    drawRoundRect(
        color = calfColor,
        topLeft = Offset(cx + 8f * scale, kneesY + 12f * scale),
        size = Size(24f * scale, 65f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
}

/**
 * Draws Back Musculature on Canvas with exact proportional bounding zones.
 */
private fun DrawScope.drawBackBody(cx: Float, h: Float, data: Map<MuscleZone, MuscleVolumeData>) {
    val scale = h / 320f
    val headY = 22f * scale
    val trapsY = 38f * scale
    val latsY = 75f * scale
    val lowerBackY = 120f * scale
    val glutesY = 150f * scale
    val hamstringsY = 185f * scale
    val calvesY = 240f * scale

    // Head Outline
    drawCircle(
        color = Color(0xFF3F3F46),
        radius = 16f * scale,
        center = Offset(cx, headY)
    )

    // Traps (Diamond/V shape)
    val trapColor = data[MuscleZone.TRAPS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    val trapPath = Path().apply {
        moveTo(cx, trapsY)
        lineTo(cx - 32f * scale, trapsY + 22f * scale)
        lineTo(cx, trapsY + 54f * scale)
        lineTo(cx + 32f * scale, trapsY + 22f * scale)
        close()
    }
    drawPath(trapPath, color = trapColor)

    // Rear Delts
    val rearDeltColor = data[MuscleZone.REAR_DELTS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    drawOval(
        color = rearDeltColor,
        topLeft = Offset(cx - 56f * scale, trapsY + 10f * scale),
        size = Size(22f * scale, 28f * scale)
    )
    drawOval(
        color = rearDeltColor,
        topLeft = Offset(cx + 34f * scale, trapsY + 10f * scale),
        size = Size(22f * scale, 28f * scale)
    )

    // Triceps
    val tricepColor = data[MuscleZone.TRICEPS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    drawOval(
        color = tricepColor,
        topLeft = Offset(cx - 62f * scale, latsY - 10f * scale),
        size = Size(18f * scale, 38f * scale)
    )
    drawOval(
        color = tricepColor,
        topLeft = Offset(cx + 44f * scale, latsY - 10f * scale),
        size = Size(18f * scale, 38f * scale)
    )

    // Lats (Wings / V-shape)
    val latsColor = data[MuscleZone.LATS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Lat
    val leftLatPath = Path().apply {
        moveTo(cx - 24f * scale, latsY)
        lineTo(cx - 48f * scale, latsY + 10f * scale)
        lineTo(cx - 26f * scale, latsY + 50f * scale)
        lineTo(cx - 10f * scale, latsY + 35f * scale)
        close()
    }
    drawPath(leftLatPath, color = latsColor)

    // Right Lat
    val rightLatPath = Path().apply {
        moveTo(cx + 24f * scale, latsY)
        lineTo(cx + 48f * scale, latsY + 10f * scale)
        lineTo(cx + 26f * scale, latsY + 50f * scale)
        lineTo(cx + 10f * scale, latsY + 35f * scale)
        close()
    }
    drawPath(rightLatPath, color = latsColor)

    // Lower Back (Erectors)
    val lowerBackColor = data[MuscleZone.LOWER_BACK]?.status?.color ?: VolumeStatus.UNTRAINED.color
    drawRoundRect(
        color = lowerBackColor,
        topLeft = Offset(cx - 16f * scale, lowerBackY),
        size = Size(32f * scale, 26f * scale),
        cornerRadius = CornerRadius(4f * scale, 4f * scale)
    )

    // Glutes
    val gluteColor = data[MuscleZone.GLUTES]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Glute
    drawOval(
        color = gluteColor,
        topLeft = Offset(cx - 34f * scale, glutesY),
        size = Size(32f * scale, 38f * scale)
    )
    // Right Glute
    drawOval(
        color = gluteColor,
        topLeft = Offset(cx + 2f * scale, glutesY),
        size = Size(32f * scale, 38f * scale)
    )

    // Hamstrings
    val hamColor = data[MuscleZone.HAMSTRINGS]?.status?.color ?: VolumeStatus.UNTRAINED.color
    // Left Hamstring
    drawRoundRect(
        color = hamColor,
        topLeft = Offset(cx - 36f * scale, hamstringsY),
        size = Size(32f * scale, 60f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
    // Right Hamstring
    drawRoundRect(
        color = hamColor,
        topLeft = Offset(cx + 4f * scale, hamstringsY),
        size = Size(32f * scale, 60f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )

    // Calves (Back Gastrocnemius diamond heads)
    val calfColor = data[MuscleZone.CALVES]?.status?.color ?: VolumeStatus.UNTRAINED.color
    drawRoundRect(
        color = calfColor,
        topLeft = Offset(cx - 32f * scale, calvesY),
        size = Size(26f * scale, 65f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
    drawRoundRect(
        color = calfColor,
        topLeft = Offset(cx + 6f * scale, calvesY),
        size = Size(26f * scale, 65f * scale),
        cornerRadius = CornerRadius(8f * scale, 8f * scale)
    )
}

/**
 * Touch Detection on Canvas to map tap coordinate to exact MuscleZone.
 */
private fun detectMuscleTap(
    offset: Offset,
    canvasWidth: Float,
    canvasHeight: Float,
    isFront: Boolean
): MuscleZone? {
    val scale = canvasHeight / 320f
    val cx = canvasWidth / 2f
    val x = offset.x
    val y = offset.y

    if (isFront) {
        // Front Chest: Y between 60*scale and 100*scale, X within +- 40
        if (y in (55f * scale)..(100f * scale) && kotlin.math.abs(x - cx) < 42f * scale) return MuscleZone.CHEST
        // Front Delts: Y between 40 and 75, X outside +- 35
        if (y in (40f * scale)..(75f * scale) && kotlin.math.abs(x - cx) in (32f * scale)..(65f * scale)) return MuscleZone.FRONT_DELTS
        // Biceps: Y between 75 and 115, X outside +- 42
        if (y in (75f * scale)..(115f * scale) && kotlin.math.abs(x - cx) in (40f * scale)..(70f * scale)) return MuscleZone.BICEPS
        // Forearms: Y between 110 and 160, X outside +- 50
        if (y in (110f * scale)..(165f * scale) && kotlin.math.abs(x - cx) in (50f * scale)..(80f * scale)) return MuscleZone.FOREARMS
        // Abs: Y between 100 and 150, X within +- 22
        if (y in (100f * scale)..(150f * scale) && kotlin.math.abs(x - cx) < 22f * scale) return MuscleZone.ABS
        // Obliques: Y between 105 and 150, X within +- 38
        if (y in (105f * scale)..(150f * scale) && kotlin.math.abs(x - cx) in (20f * scale)..(38f * scale)) return MuscleZone.OBLIQUES
        // Quads: Y between 150 and 225
        if (y in (150f * scale)..(225f * scale) && kotlin.math.abs(x - cx) < 45f * scale) return MuscleZone.QUADS
        // Calves: Y between 230 and 310
        if (y in (230f * scale)..(310f * scale) && kotlin.math.abs(x - cx) < 40f * scale) return MuscleZone.CALVES
    } else {
        // Traps: Y between 35 and 90, X within +- 35
        if (y in (35f * scale)..(90f * scale) && kotlin.math.abs(x - cx) < 35f * scale) return MuscleZone.TRAPS
        // Rear Delts: Y between 45 and 80, X outside +- 32
        if (y in (45f * scale)..(80f * scale) && kotlin.math.abs(x - cx) in (32f * scale)..(65f * scale)) return MuscleZone.REAR_DELTS
        // Triceps: Y between 70 and 115, X outside +- 40
        if (y in (70f * scale)..(115f * scale) && kotlin.math.abs(x - cx) in (40f * scale)..(68f * scale)) return MuscleZone.TRICEPS
        // Lats: Y between 75 and 125, X within +- 50
        if (y in (75f * scale)..(130f * scale) && kotlin.math.abs(x - cx) in (15f * scale)..(50f * scale)) return MuscleZone.LATS
        // Lower Back: Y between 120 and 150
        if (y in (120f * scale)..(150f * scale) && kotlin.math.abs(x - cx) < 25f * scale) return MuscleZone.LOWER_BACK
        // Glutes: Y between 150 and 190
        if (y in (150f * scale)..(190f * scale) && kotlin.math.abs(x - cx) < 42f * scale) return MuscleZone.GLUTES
        // Hamstrings: Y between 185 and 245
        if (y in (185f * scale)..(245f * scale) && kotlin.math.abs(x - cx) < 42f * scale) return MuscleZone.HAMSTRINGS
        // Calves: Y between 245 and 315
        if (y in (245f * scale)..(315f * scale) && kotlin.math.abs(x - cx) < 40f * scale) return MuscleZone.CALVES
    }
    return null
}

@Composable
private fun LegendPill(label: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(text = label, fontSize = 10.sp, color = TextMuted)
    }
}

/**
 * Bottom Sheet displaying scientific volume targets and recovery for inspected muscle.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MuscleDetailBottomSheet(
    data: MuscleVolumeData,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val zone = data.zone

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        contentColor = TextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = zone.nameRu,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Text(
                        text = "${zone.nameEn} • ${zone.nameUz}",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Закрыть", tint = TextSecondary)
                }
            }

            // Status Indicator Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Нагрузка за последние 7 дней:",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .background(data.status.color, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${data.completedWeeklySets} подходов",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { data.progressRatio },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(8.dp),
                        color = data.status.color,
                        trackColor = DarkBorder,
                        drawStopIndicator = {}
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Мин. стимул: ${zone.minOptimalWeeklySets} подх.",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                        Text(
                            text = "Оптимум: ${zone.maxOptimalWeeklySets} подх.",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }
            }

            // Scientific Recommendations
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkBackground),
                border = androidx.compose.foundation.BorderStroke(1.dp, DarkBorder),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "ФИЗИОЛОГИЧЕСКИЙ ПРОТОКОЛ ВОССТАНОВЛЕНИЯ",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = EmeraldNeon,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "• Время полного восстановления миофибрилл: ~${zone.recoveryHours} часов.\n" +
                            "• Рекомендуемая частота: 2 раза в неделю по ${zone.minOptimalWeeklySets / 2}-${zone.maxOptimalWeeklySets / 2} рабочих сетов.\n" +
                            "• Статус сейчас: ${data.status.titleRu}.",
                        fontSize = 12.sp,
                        color = TextSecondary,
                        lineHeight = 18.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
