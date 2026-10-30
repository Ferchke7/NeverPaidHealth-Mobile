package dev.neverpaid.health.domain.model

import kotlinx.serialization.Serializable

// ============================================================================
// 1. SUPERSETS & INTENSITY TECHNIQUES
// ============================================================================

/**
 * Defines a superset, tri-set, or circuit group linking multiple exercises together.
 * For example: Superset A (A1: Bench Press, A2: Barbell Row).
 */
@Serializable
data class SupersetGroup(
    val id: String,
    val groupLabel: String, // e.g. "A", "B", "C"
    val exerciseIds: List<String>,
    val intraSetRestSeconds: Int = 0, // Rest between exercises inside the superset (typically 0 or 15s)
    val roundRestSeconds: Int = 120 // Rest between full rounds of the superset
)

/**
 * Classification of a training set.
 */
@Serializable
enum class SetTypeTag {
    NORMAL,
    WARMUP,
    DROP_SET,
    REST_PAUSE,
    MYO_REPS,
    FAILURE
}

/**
 * Athletic set rating capturing RPE (Rate of Perceived Exertion) and RIR (Reps in Reserve).
 */
@Serializable
data class SetRating(
    val rpe: Double? = null, // 6.0 to 10.0
    val rir: Int? = null, // 0 to 5+
    val typeTag: SetTypeTag = SetTypeTag.NORMAL,
    val tempo: String? = null // e.g. "3-0-1-0" (3s eccentric, 0s pause, 1s concentric, 0s top)
)

// ============================================================================
// 2. CNS & READINESS ASSESSMENT
// ============================================================================

/**
 * Classification of the athlete's neuromuscular readiness.
 */
enum class ReadinessTier {
    PEAK_READINESS,     // 85 - 100: Ready for PRs, high volume / heavy load
    NORMAL_READINESS,   // 65 - 84: Standard scheduled workout volume
    MILD_FATIGUE,       // 45 - 64: Reduce volume by 15-20% or keep RPE <= 8
    CRITICAL_RECOVERY   // < 45: Active recovery, mobility, or complete rest
}

/**
 * Morning or pre-workout CNS Readiness survey.
 * Computes an evidence-based neuromuscular fatigue score (0 to 100).
 */
@Serializable
data class ReadinessAssessment(
    val id: String,
    val date: String, // "YYYY-MM-DD"
    val sleepHours: Double, // e.g. 7.5
    val sleepQuality: Int, // 1 (terrible) to 5 (optimal deep rest)
    val muscleSoreness: Int, // 1 (fresh) to 5 (debilitating DOMS)
    val cnsFatigue: Int, // 1 (energized) to 5 (drained / foggy)
    val stressLevel: Int, // 1 (calm) to 5 (overwhelmed)
    val restingHeartRateBpm: Int? = null,
    val hrvScoreMs: Double? = null,
    val notes: String = ""
) {
    /**
     * Calculates deterministic readiness score (0 to 100).
     */
    val score: Int
        get() {
            // Baseline 100 points
            var points = 100.0

            // Sleep duration weighting (Target: 8.0 hours)
            val sleepDeficit = (8.0 - sleepHours).coerceAtLeast(0.0)
            points -= sleepDeficit * 6.0 // -6 points per hour under 8h

            // Sleep quality: 1 -> -16, 2 -> -10, 3 -> -4, 4 -> 0, 5 -> +4
            points += (sleepQuality - 4) * 5.0

            // Soreness penalty: 1 -> 0, 2 -> -4, 3 -> -10, 4 -> -18, 5 -> -28
            points -= when (muscleSoreness) {
                1 -> 0.0
                2 -> 4.0
                3 -> 10.0
                4 -> 18.0
                else -> 28.0
            }

            // CNS fatigue penalty: 1 -> 0, 2 -> -5, 3 -> -12, 4 -> -22, 5 -> -35
            points -= when (cnsFatigue) {
                1 -> 0.0
                2 -> 5.0
                3 -> 12.0
                4 -> 22.0
                else -> 35.0
            }

            // Stress penalty: 1 -> 0, 2 -> -3, 3 -> -8, 4 -> -15, 5 -> -25
            points -= when (stressLevel) {
                1 -> 0.0
                2 -> 3.0
                3 -> 8.0
                4 -> 15.0
                else -> 25.0
            }

            return points.toInt().coerceIn(0, 100)
        }

    val tier: ReadinessTier
        get() = when {
            score >= 85 -> ReadinessTier.PEAK_READINESS
            score >= 65 -> ReadinessTier.NORMAL_READINESS
            score >= 45 -> ReadinessTier.MILD_FATIGUE
            else -> ReadinessTier.CRITICAL_RECOVERY
        }

    /**
     * Recommended volume modifier for today's session.
     */
    val volumeModifierPercentage: Int
        get() = when (tier) {
            ReadinessTier.PEAK_READINESS -> 105 // +5% volume / push for PR
            ReadinessTier.NORMAL_READINESS -> 100 // 100% standard prescribed volume
            ReadinessTier.MILD_FATIGUE -> 80 // -20% volume (drop 1 set per exercise)
            ReadinessTier.CRITICAL_RECOVERY -> 50 // Deload or active recovery only
        }
}

// ============================================================================
// 3. ATHLETE HABITS & STREAK TRACKING
// ============================================================================

enum class HabitCategory {
    NUTRITION,
    TRAINING,
    RECOVERY,
    HYDRATION,
    MINDSET
}

enum class HabitFrequency {
    DAILY,
    WEEKDAYS,
    WEEKENDS,
    THREE_TIMES_A_WEEK
}

/**
 * Habit item tracked with daily completion dates for strict historical streak preservation.
 */
@Serializable
data class AthleteHabit(
    val id: String,
    val title: String,
    val description: String = "",
    val category: HabitCategory,
    val targetValue: String, // e.g. "2500 ml", "8 hrs", "5 g"
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val iconName: String = "Check",
    val completedDates: Set<String> = emptySet(), // Set of "YYYY-MM-DD"
    val createdAtIso: String = ""
) {
    /**
     * Checks if habit was completed on a specific ISO date ("YYYY-MM-DD").
     */
    fun isCompletedOn(date: String): Boolean = completedDates.contains(date)

    /**
     * Computes the current active uninterrupted streak up to today or yesterday.
     */
    fun calculateCurrentStreak(todayDate: String): Int {
        if (completedDates.isEmpty()) return 0
        val sortedDates = completedDates.sortedDescending()
        var streak = 0
        var checkDate = todayDate

        // If not completed today, check if yesterday was completed
        if (!sortedDates.contains(todayDate)) {
            // Allow 1 day grace if today is not over yet
            val yesterday = decrementIsoDate(todayDate)
            if (!sortedDates.contains(yesterday)) {
                return 0
            }
            checkDate = yesterday
        }

        while (sortedDates.contains(checkDate)) {
            streak++
            checkDate = decrementIsoDate(checkDate)
        }
        return streak
    }

    /**
     * 30-day completion rate as a percentage (0% to 100%).
     */
    fun completionRate30Days(todayDate: String): Int {
        var completedCount = 0
        var cursor = todayDate
        for (i in 0 until 30) {
            if (completedDates.contains(cursor)) {
                completedCount++
            }
            cursor = decrementIsoDate(cursor)
        }
        return ((completedCount / 30.0) * 100).toInt()
    }

    private fun decrementIsoDate(isoDate: String): String {
        val parts = isoDate.split("-")
        if (parts.size != 3) return isoDate
        val y = parts[0].toIntOrNull() ?: 2026
        val m = parts[1].toIntOrNull() ?: 10
        val d = parts[2].toIntOrNull() ?: 1

        if (d > 1) {
            val prevDay = d - 1
            return "$y-${m.toString().padStart(2, '0')}-${prevDay.toString().padStart(2, '0')}"
        } else {
            val prevMonth = if (m > 1) m - 1 else 12
            val prevYear = if (m > 1) y else y - 1
            val daysInPrevMonth = when (prevMonth) {
                2 -> 28
                4, 6, 9, 11 -> 30
                else -> 31
            }
            return "$prevYear-${prevMonth.toString().padStart(2, '0')}-$daysInPrevMonth"
        }
    }
}

// ============================================================================
// 4. INTERMITTENT FASTING TRACKER
// ============================================================================

enum class FastingProtocol(val fastHours: Int, val eatingHours: Int, val title: String) {
    LEAN_GAINS_16_8(16, 8, "16:8 LeanGains"),
    WARRIOR_18_6(18, 6, "18:6 Warrior"),
    OMAD_20_4(20, 4, "20:4 OMAD"),
    CIRCADIAN_13_11(13, 11, "13:11 Circadian"),
    PROLONGED_24(24, 0, "24h Monk Fast")
}

enum class FastingStage(val minimumHours: Double, val titleRu: String, val titleUz: String, val titleEn: String) {
    ANABOLIC_DIGESTION(0.0, "Пищеварение и усвоение", "Hazm qilish va so'rilish", "Digestion & Absorption"),
    BLOOD_SUGAR_NORMALIZATION(4.0, "Нормализация уровня сахара", "Qand miqdori normallashishi", "Blood Sugar Normalization"),
    FAT_BURNING_START(12.0, "Старт сжигания жира", "Yog' erishining boshlanishi", "Fat Burning Inception"),
    KETOSIS(14.0, "Кетоз и окисление жиров", "Ketoz va yog' kislotalari oksidlanishi", "Ketosis State"),
    AUTOPHAGY(16.0, "Клеточная аутофагия", "Hujayra autofagiyasi", "Cellular Autophagy"),
    GROWTH_HORMONE_PEAK(18.0, "Пик гормона роста (HGH)", "O'sish gormoni (HGH) cho'qqisi", "Growth Hormone Peak")
}

@Serializable
data class FastingSession(
    val id: String,
    val protocol: FastingProtocol,
    val startTimeEpochMs: Long,
    val targetDurationHours: Double,
    val endTimeEpochMs: Long? = null,
    val isCompleted: Boolean = false
) {
    fun elapsedHours(currentEpochMs: Long): Double {
        val end = endTimeEpochMs ?: currentEpochMs
        return ((end - startTimeEpochMs) / (1000.0 * 60.0 * 60.0)).coerceAtLeast(0.0)
    }

    fun progressFraction(currentEpochMs: Long): Float {
        val elapsed = elapsedHours(currentEpochMs)
        return (elapsed / targetDurationHours).toFloat().coerceIn(0f, 1f)
    }

    fun remainingHours(currentEpochMs: Long): Double {
        val elapsed = elapsedHours(currentEpochMs)
        return (targetDurationHours - elapsed).coerceAtLeast(0.0)
    }

    fun currentStage(currentEpochMs: Long): FastingStage {
        val elapsed = elapsedHours(currentEpochMs)
        return FastingStage.values().reversed().firstOrNull { elapsed >= it.minimumHours }
            ?: FastingStage.ANABOLIC_DIGESTION
    }
}

// ============================================================================
// 5. DETAILED ATHLETIC FOOD ITEM & SERVING UNITS
// ============================================================================

enum class FoodCategory {
    PROTEIN_SOURCE,
    COMPLEX_CARBS,
    HEALTHY_FATS,
    FRUITS_BERRIES,
    VEGETABLES_GREENS,
    DAIRY_EGGS,
    SPORTS_SUPPLEMENTS,
    PRE_WORKOUT_SNACKS
}

@Serializable
data class ServingPortion(
    val label: String, // e.g. "1 мерная ложка (30г)", "1 среднее яблоко (150г)"
    val grams: Double
)

@Serializable
data class AthleticFoodItem(
    val id: String,
    val nameRu: String,
    val nameUz: String,
    val nameEn: String,
    val category: FoodCategory,
    val caloriesPer100g: Double,
    val proteinPer100g: Double,
    val carbsPer100g: Double,
    val fatsPer100g: Double,
    val fiberPer100g: Double = 0.0,
    val sodiumMgPer100g: Double = 0.0,
    val servingPortions: List<ServingPortion> = emptyList()
) {
    fun localizedName(languageCode: String): String = when (languageCode.lowercase()) {
        "uz" -> nameUz
        "en" -> nameEn
        else -> nameRu
    }

    /**
     * Calculates nutrition for a given portion in grams.
     */
    fun calculateNutrients(grams: Double): CalculatedNutrients {
        val factor = grams / 100.0
        return CalculatedNutrients(
            grams = grams,
            calories = caloriesPer100g * factor,
            protein = proteinPer100g * factor,
            carbs = carbsPer100g * factor,
            fats = fatsPer100g * factor,
            fiber = fiberPer100g * factor
        )
    }
}

@Serializable
data class CalculatedNutrients(
    val grams: Double,
    val calories: Double,
    val protein: Double,
    val carbs: Double,
    val fats: Double,
    val fiber: Double
)
