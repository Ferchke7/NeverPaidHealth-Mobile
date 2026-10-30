package dev.neverpaid.health.core.math

import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Result of US Navy Body Fat % Calculation.
 */
data class NavyBodyFatResult(
    val bodyFatPercentage: Double,
    val leanMassKg: Double,
    val fatMassKg: Double,
    val category: BodyFatCategory,
    val idealWeightKg: Double
)

enum class BodyFatCategory(
    val titleRu: String,
    val titleUz: String,
    val titleEn: String,
    val colorHex: Long
) {
    ESSENTIAL_FAT("Минимальный жир (Соревновательный)", "Minimal yog' (Musobaqa)", "Essential Fat", 0xFFEF4444),
    ATHLETES("Атлетическое телосложение", "Atletik tana tuzilishi", "Athletes", 0xFF22C55E),
    FITNESS("Фитнес форма", "Fitnes shakli", "Fitness", 0xFF3B82F6),
    AVERAGE("Средний уровень", "O'rtacha daraja", "Average", 0xFFEAB308),
    OBESE("Избыточный вес", "Ortiqcha vazn", "Obese", 0xFFF97316)
}

/**
 * Adonis Index / Body Proportion Analysis.
 */
data class AdonisIndexResult(
    val shoulderToWaistRatio: Double,
    val chestToWaistRatio: Double,
    val idealWaistCm: Double,
    val idealShouldersCm: Double,
    val idealChestCm: Double,
    val adonisScorePercentage: Int, // 100% = exact 1.618 ratio
    val assessmentRu: String,
    val assessmentUz: String,
    val assessmentEn: String
)

/**
 * Maximum Muscular Potential Calculation (Natural Limit Model).
 */
data class MaximumMuscularPotentialResult(
    val maxLeanMassKg: Double,
    val maxWeightAt10PercentFatKg: Double,
    val currentLeanMassKg: Double,
    val percentageOfGeneticLimit: Int,
    val maxChestCm: Double,
    val maxBicepsCm: Double,
    val maxForearmCm: Double,
    val maxThighCm: Double,
    val maxCalfCm: Double
)

/**
 * Advanced body composition and anthropometric analytics engine.
 */
object AnthropometryEngine {

    const val GOLDEN_RATIO: Double = 1.618

    /**
     * Calculates Body Fat Percentage using the US Navy Circumference Method.
     * All measurements in Centimeters.
     */
    fun calculateNavyBodyFat(
        isMale: Boolean,
        heightCm: Double,
        neckCm: Double,
        waistCm: Double,
        hipCm: Double? = null,
        weightKg: Double
    ): NavyBodyFatResult? {
        if (heightCm <= 0 || neckCm <= 0 || waistCm <= 0 || weightKg <= 0) return null

        val bfp = if (isMale) {
            val diff = waistCm - neckCm
            if (diff <= 0) return null
            495.0 / (1.0324 - (0.19077 * log10(diff)) + (0.15456 * log10(heightCm))) - 450.0
        } else {
            val hips = hipCm ?: (waistCm * 1.25)
            val sum = waistCm + hips - neckCm
            if (sum <= 0) return null
            495.0 / (1.29579 - (0.35004 * log10(sum)) + (0.22100 * log10(heightCm))) - 450.0
        }

        val clampedBfp = bfp.coerceIn(3.0, 60.0).roundToOneDecimal()
        val fatMass = ((clampedBfp / 100.0) * weightKg).roundToOneDecimal()
        val leanMass = (weightKg - fatMass).roundToOneDecimal()

        val category = determineCategory(clampedBfp, isMale)

        // Ideal bodyweight at 12% for men or 20% for women
        val targetFatPct = if (isMale) 0.12 else 0.20
        val idealWeight = (leanMass / (1.0 - targetFatPct)).roundToOneDecimal()

        return NavyBodyFatResult(
            bodyFatPercentage = clampedBfp,
            leanMassKg = leanMass,
            fatMassKg = fatMass,
            category = category,
            idealWeightKg = idealWeight
        )
    }

    /**
     * Calculates Adonis Index (Golden Ratio) proportions.
     */
    fun calculateAdonisIndex(
        heightCm: Double,
        waistCm: Double,
        shouldersCm: Double,
        chestCm: Double
    ): AdonisIndexResult {
        val s2w = if (waistCm > 0) (shouldersCm / waistCm).roundToOneDecimal(2) else 1.0
        val c2w = if (waistCm > 0) (chestCm / waistCm).roundToOneDecimal(2) else 1.0

        // Ideal aesthetic waist is approx 45-47% of height
        val idealWaist = (heightCm * 0.45).roundToOneDecimal()
        val idealShoulders = (idealWaist * GOLDEN_RATIO).roundToOneDecimal()
        val idealChest = (idealWaist * 1.33).roundToOneDecimal()

        // Adonis symmetry score: closeness of s2w to 1.618 (0 to 100%)
        val deviation = kotlin.math.abs(s2w - GOLDEN_RATIO)
        val score = ((1.0 - (deviation / 0.5)).coerceIn(0.2, 1.0) * 100).toInt()

        val assessment = when {
            s2w >= 1.60 && s2w <= 1.64 -> Triple(
                "Идеальная классическая V-образная фигура (Золотое сечение)!",
                "Ideal klassik V-simon qomat (Oltin nisbat)!",
                "Ideal classic V-Taper aesthetic (Golden Ratio achieved)!"
            )
            s2w > 1.64 -> Triple(
                "Экстремальный V-конус! Мощный плечевой пояс.",
                "Ekstremal V-konus! Kuchli yelka kamari.",
                "Extreme V-Taper with dominant shoulder girdle width."
            )
            s2w >= 1.45 -> Triple(
                "Отличный атлетический V-конус. Фокус на ширину дельт и широчайших.",
                "Ajoyib atletik V-konus. Deltoid va qanot kengligiga e'tibor qarating.",
                "Solid athletic taper. Continue progressive overload on lateral delts and lats."
            )
            else -> Triple(
                "Широкая талия относительно плеч. Рекомендуется снижение % жира и развитие дельт.",
                "Yelkaga nisbatan keng bel. Yog' % kamaytirish va yelkani o'stirish tavsiya etiladi.",
                "Focus on waist reduction via caloric deficit while developing shoulder breadth."
            )
        }

        return AdonisIndexResult(
            shoulderToWaistRatio = s2w,
            chestToWaistRatio = c2w,
            idealWaistCm = idealWaist,
            idealShouldersCm = idealShoulders,
            idealChestCm = idealChest,
            adonisScorePercentage = score,
            assessmentRu = assessment.first,
            assessmentUz = assessment.second,
            assessmentEn = assessment.third
        )
    }

    /**
     * Calculates Natural Maximum Muscular Potential based on Casey Butt's formula.
     */
    fun calculateMaxMuscularPotential(
        heightCm: Double,
        wristCm: Double = 18.0,
        ankleCm: Double = 22.5,
        currentWeightKg: Double,
        currentBodyFatPct: Double = 15.0
    ): MaximumMuscularPotentialResult {
        val hInches = heightCm / 2.54
        val wInches = wristCm / 2.54
        val aInches = ankleCm / 2.54

        // Casey Butt formula for max lean mass in lbs
        val maxLeanLbs = (hInches.pow(1.5) * (kotlin.math.sqrt(wInches) / 22.6670 + kotlin.math.sqrt(aInches) / 17.0104) * (1.0 / 224.0 + 1.0))
        val maxLeanKg = (maxLeanLbs * 0.453592).roundToOneDecimal()
        val maxWeightAt10Pct = (maxLeanKg / 0.90).roundToOneDecimal()

        val currentLeanKg = (currentWeightKg * (1.0 - (currentBodyFatPct / 100.0))).roundToOneDecimal()
        val limitPct = ((currentLeanKg / maxLeanKg) * 100).toInt().coerceIn(40, 100)

        // Limb limit estimates
        val maxChest = (wristCm * 6.5).roundToOneDecimal()
        val maxBiceps = (wristCm * 2.3).roundToOneDecimal()
        val maxForearm = (wristCm * 1.8).roundToOneDecimal()
        val maxThigh = (ankleCm * 2.7).roundToOneDecimal()
        val maxCalf = (ankleCm * 1.8).roundToOneDecimal()

        return MaximumMuscularPotentialResult(
            maxLeanMassKg = maxLeanKg,
            maxWeightAt10PercentFatKg = maxWeightAt10Pct,
            currentLeanMassKg = currentLeanKg,
            percentageOfGeneticLimit = limitPct,
            maxChestCm = maxChest,
            maxBicepsCm = maxBiceps,
            maxForearmCm = maxForearm,
            maxThighCm = maxThigh,
            maxCalfCm = maxCalf
        )
    }

    private fun determineCategory(bfp: Double, isMale: Boolean): BodyFatCategory {
        return if (isMale) {
            when {
                bfp < 6.0 -> BodyFatCategory.ESSENTIAL_FAT
                bfp < 14.0 -> BodyFatCategory.ATHLETES
                bfp < 18.0 -> BodyFatCategory.FITNESS
                bfp < 25.0 -> BodyFatCategory.AVERAGE
                else -> BodyFatCategory.OBESE
            }
        } else {
            when {
                bfp < 14.0 -> BodyFatCategory.ESSENTIAL_FAT
                bfp < 21.0 -> BodyFatCategory.ATHLETES
                bfp < 25.0 -> BodyFatCategory.FITNESS
                bfp < 32.0 -> BodyFatCategory.AVERAGE
                else -> BodyFatCategory.OBESE
            }
        }
    }

    private fun Double.roundToOneDecimal(decimals: Int = 1): Double {
        val factor = 10.0.pow(decimals)
        return (this * factor).roundToInt() / factor
    }
}
