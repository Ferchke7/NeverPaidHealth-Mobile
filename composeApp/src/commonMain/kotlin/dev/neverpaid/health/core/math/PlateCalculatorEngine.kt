package dev.neverpaid.health.core.math

import kotlin.math.roundToInt

/**
 * Standard Olympic and Powerlifting Barbell Types.
 */
enum class BarbellType(
    val id: String,
    val defaultWeightKg: Double,
    val defaultWeightLbs: Double,
    val displayNameRu: String,
    val displayNameUz: String,
    val displayNameEn: String,
    val sleeveLengthMm: Int = 415
) {
    OLYMPIC_MENS("mens_olympic", 20.0, 45.0, "Олимпийский гриф (20 кг)", "Erkaklar olimpiya shtangasi (20 kg)", "Men's Olympic Bar (20 kg / 44 lbs)", 415),
    OLYMPIC_WOMENS("womens_olympic", 15.0, 35.0, "Женский гриф (15 кг)", "Ayollar olimpiya shtangasi (15 kg)", "Women's Olympic Bar (15 kg / 33 lbs)", 320),
    TECHNIQUE("technique_bar", 10.0, 22.0, "Технический гриф (10 кг)", "Texnik shtanga (10 kg)", "Technique Bar (10 kg / 22 lbs)", 250),
    TRAP_HEX("trap_bar", 25.0, 55.0, "Трэп-гриф (Hex Bar, 25 кг)", "Trep shtanga (Hex Bar, 25 kg)", "Trap / Hex Bar (25 kg / 55 lbs)", 400),
    SAFETY_SQUAT("safety_squat", 30.0, 65.0, "Safety Squat Bar (30 кг)", "Safety Squat Bar (30 kg)", "Safety Squat Bar (30 kg / 66 lbs)", 400),
    EZ_CURL("ez_curl", 10.0, 22.0, "EZ-гриф (10 кг)", "EZ-shtanga (10 kg)", "EZ Curl Bar (10 kg / 22 lbs)", 175),
    SMITH_MACHINE("smith", 15.0, 33.0, "Каретка тренажёра Смита (15 кг)", "Smit trenajori karetkasi (15 kg)", "Smith Machine Carriage (15 kg)", 450)
}

/**
 * Plate Specification with Competition Colors, Dimensions and Inventory.
 */
data class PlateSpec(
    val weightKg: Double,
    val weightLbs: Double,
    val colorHex: Long, // 0xAARRGGBB
    val diameterMm: Int, // standard full diameter is 450mm for bumper/comp plates
    val thicknessMm: Int,
    val labelText: String,
    val countPerGym: Int = 10
) {
    val isBumperFullDiameter: Boolean get() = diameterMm >= 440
}

/**
 * Result of Plate Calculation per side of the barbell.
 */
data class PlateCalculationResult(
    val targetWeightKg: Double,
    val barbellWeightKg: Double,
    val collarWeightKg: Double,
    val weightPerSideKg: Double,
    val achievedTotalKg: Double,
    val platesPerSide: List<PlateItem>,
    val remainingWeightKg: Double,
    val isExactMatch: Boolean,
    val isOverLimit: Boolean,
    val percentageOf1RM: Double? = null
)

/**
 * A specific plate placed on the sleeve.
 */
data class PlateItem(
    val spec: PlateSpec,
    val count: Int
) {
    val totalWeightKg: Double get() = spec.weightKg * count
}

/**
 * High-performance plate calculator for powerlifters, bodybuilders and crossfitters.
 */
object PlateCalculatorEngine {

    // Metric Plates (KG)
    val STANDARD_METRIC_PLATES: List<PlateSpec> = listOf(
        PlateSpec(weightKg = 25.0, weightLbs = 55.0, colorHex = 0xFFEF4444, diameterMm = 450, thicknessMm = 65, labelText = "25"),
        PlateSpec(weightKg = 20.0, weightLbs = 44.0, colorHex = 0xFF3B82F6, diameterMm = 450, thicknessMm = 54, labelText = "20"),
        PlateSpec(weightKg = 15.0, weightLbs = 33.0, colorHex = 0xFFEAB308, diameterMm = 400, thicknessMm = 45, labelText = "15"),
        PlateSpec(weightKg = 10.0, weightLbs = 22.0, colorHex = 0xFF22C55E, diameterMm = 350, thicknessMm = 38, labelText = "10"),
        PlateSpec(weightKg = 5.0, weightLbs = 11.0, colorHex = 0xFFF8FAFC, diameterMm = 250, thicknessMm = 30, labelText = "5"),
        PlateSpec(weightKg = 2.5, weightLbs = 5.5, colorHex = 0xFF1E293B, diameterMm = 210, thicknessMm = 24, labelText = "2.5"),
        PlateSpec(weightKg = 1.25, weightLbs = 2.75, colorHex = 0xFF94A3B8, diameterMm = 170, thicknessMm = 18, labelText = "1.25"),
        PlateSpec(weightKg = 0.5, weightLbs = 1.1, colorHex = 0xFFCBD5E1, diameterMm = 135, thicknessMm = 14, labelText = "0.5"),
        PlateSpec(weightKg = 0.25, weightLbs = 0.55, colorHex = 0xFFE2E8F0, diameterMm = 110, thicknessMm = 12, labelText = "0.25")
    )

    // Imperial Plates (LBS)
    val STANDARD_IMPERIAL_PLATES: List<PlateSpec> = listOf(
        PlateSpec(weightKg = 20.41, weightLbs = 45.0, colorHex = 0xFF3B82F6, diameterMm = 450, thicknessMm = 55, labelText = "45"),
        PlateSpec(weightKg = 15.88, weightLbs = 35.0, colorHex = 0xFFEAB308, diameterMm = 400, thicknessMm = 45, labelText = "35"),
        PlateSpec(weightKg = 11.34, weightLbs = 25.0, colorHex = 0xFF22C55E, diameterMm = 350, thicknessMm = 38, labelText = "25"),
        PlateSpec(weightKg = 4.54, weightLbs = 10.0, colorHex = 0xFFF8FAFC, diameterMm = 250, thicknessMm = 30, labelText = "10"),
        PlateSpec(weightKg = 2.27, weightLbs = 5.0, colorHex = 0xFF1E293B, diameterMm = 200, thicknessMm = 24, labelText = "5"),
        PlateSpec(weightKg = 1.13, weightLbs = 2.5, colorHex = 0xFF94A3B8, diameterMm = 160, thicknessMm = 18, labelText = "2.5")
    )

    /**
     * Calculates the exact plate combination needed per side of the barbell.
     */
    fun calculatePlates(
        targetWeightKg: Double,
        barbell: BarbellType = BarbellType.OLYMPIC_MENS,
        useCollars: Boolean = false,
        collarPairWeightKg: Double = 2.5,
        availablePlates: List<PlateSpec> = STANDARD_METRIC_PLATES,
        oneRepMaxKg: Double? = null
    ): PlateCalculationResult {
        val barWeight = barbell.defaultWeightKg
        val collarWeight = if (useCollars) collarPairWeightKg else 0.0
        val baseWeight = barWeight + collarWeight

        if (targetWeightKg <= baseWeight) {
            val percentage = if (oneRepMaxKg != null && oneRepMaxKg > 0) {
                ((baseWeight / oneRepMaxKg) * 100.0).roundToOneDecimal()
            } else null

            return PlateCalculationResult(
                targetWeightKg = targetWeightKg,
                barbellWeightKg = barWeight,
                collarWeightKg = collarWeight,
                weightPerSideKg = 0.0,
                achievedTotalKg = baseWeight,
                platesPerSide = emptyList(),
                remainingWeightKg = 0.0,
                isExactMatch = targetWeightKg == baseWeight,
                isOverLimit = false,
                percentageOf1RM = percentage
            )
        }

        val neededWeightTotal = targetWeightKg - baseWeight
        val neededPerSide = neededWeightTotal / 2.0

        var currentRemainder = neededPerSide
        val resultPlates = mutableListOf<PlateItem>()

        val sortedPlates = availablePlates.sortedByDescending { it.weightKg }

        for (plate in sortedPlates) {
            if (currentRemainder >= plate.weightKg - 0.001) {
                val maxPossible = (currentRemainder / plate.weightKg).toInt()
                val countToTake = maxPossible.coerceAtMost(plate.countPerGym / 2)
                if (countToTake > 0) {
                    resultPlates.add(PlateItem(plate, countToTake))
                    currentRemainder -= (countToTake * plate.weightKg)
                }
            }
        }

        val achievedSideWeight = resultPlates.sumOf { it.totalWeightKg }
        val achievedTotal = baseWeight + (achievedSideWeight * 2.0)
        val remainingDifference = targetWeightKg - achievedTotal
        val isExact = kotlin.math.abs(remainingDifference) < 0.1

        val percentage = if (oneRepMaxKg != null && oneRepMaxKg > 0) {
            ((achievedTotal / oneRepMaxKg) * 100.0).roundToOneDecimal()
        } else null

        // Check if total thickness fits standard sleeve
        val totalThicknessMm = resultPlates.sumOf { it.spec.thicknessMm * it.count }
        val isOverLimit = totalThicknessMm > (barbell.sleeveLengthMm - 50)

        return PlateCalculationResult(
            targetWeightKg = targetWeightKg,
            barbellWeightKg = barWeight,
            collarWeightKg = collarWeight,
            weightPerSideKg = achievedSideWeight,
            achievedTotalKg = achievedTotal.roundToOneDecimal(),
            platesPerSide = resultPlates,
            remainingWeightKg = remainingDifference.roundToOneDecimal(),
            isExactMatch = isExact,
            isOverLimit = isOverLimit,
            percentageOf1RM = percentage
        )
    }

    /**
     * Generates standard quick-select weights based on percentage of athlete's 1RM.
     */
    fun generateQuickPercentages(oneRepMaxKg: Double): List<Pair<Int, Double>> {
        val percentages = listOf(50, 60, 70, 75, 80, 85, 90, 95)
        return percentages.map { pct ->
            val raw = oneRepMaxKg * (pct / 100.0)
            // Round to nearest 2.5kg step
            val rounded = (raw / 2.5).roundToInt() * 2.5
            pct to rounded
        }
    }

    /**
     * Formats plate count breakdown as readable string, e.g. "2×20kg, 1×10kg, 1×2.5kg".
     */
    fun formatPlateBreakdown(plates: List<PlateItem>): String {
        if (plates.isEmpty()) return "Пустой гриф"
        return plates.joinToString(", ") { item ->
            "${item.count}×${item.spec.labelText} кг"
        }
    }

    private fun Double.roundToOneDecimal(): Double {
        return (this * 10.0).roundToInt() / 10.0
    }
}
