package dev.neverpaid.health.core.math

import kotlin.math.pow
import kotlin.math.round

object StrengthCalculator {

    /**
     * Calculates Estimated 1 Rep Max (E1RM) using the Epley formula.
     * Matches golden calculation vectors from NeverPaidHealth contracts.
     *
     * Reps > 12 are considered unreliable for 1RM estimation and return null.
     */
    fun calculateE1RM(weightKg: Double, reps: Int): Double? {
        if (weightKg <= 0.0 || reps <= 0) return null
        if (reps == 1) return weightKg
        if (reps > 12) return null

        val rawE1rm = weightKg * (1.0 + reps / 30.0)
        return roundToOneDecimal(rawE1rm)
    }

    /**
     * Calculates total training volume in KG.
     * Only completed sets contribute to volume.
     */
    fun calculateVolume(sets: List<SetCalculationInput>): Double {
        val total = sets.filter { it.completed }
            .sumOf { it.weightKg * it.reps }
        return roundToOneDecimal(total)
    }

    /**
     * Body Mass Index (BMI) = weight (kg) / (height (m))^2
     */
    fun calculateBMI(weightKg: Double, heightCm: Double): Double? {
        if (weightKg <= 0.0 || heightCm <= 0.0) return null
        val heightMeters = heightCm / 100.0
        val rawBmi = weightKg / heightMeters.pow(2)
        return roundToOneDecimal(rawBmi)
    }

    /**
     * 7-day rolling moving average of bodyweights.
     */
    fun calculate7DayMovingAverage(weightsKg: List<Double>): Double? {
        if (weightsKg.isEmpty()) return null
        val slice = weightsKg.takeLast(7)
        val avg = slice.average()
        return roundToOneDecimal(avg)
    }

    /**
     * Total Daily Energy Expenditure (TDEE) via Mifflin-St Jeor formula.
     */
    fun calculateTDEE(
        weightKg: Double,
        heightCm: Double,
        age: Int,
        isMale: Boolean = true,
        activityMultiplier: Double = 1.55 // Moderate activity (3-5 workouts/wk)
    ): Int {
        if (weightKg <= 0 || heightCm <= 0 || age <= 0) return 2000
        val bmrBase = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        val bmr = if (isMale) bmrBase + 5 else bmrBase - 161
        return round(bmr * activityMultiplier).toInt()
    }

    fun kgToLbs(kg: Double): Double {
        return roundToOneDecimal(kg * 2.20462)
    }

    fun lbsToKg(lbs: Double): Double {
        return roundToOneDecimal(lbs / 2.20462)
    }

    private fun roundToOneDecimal(value: Double): Double {
        return round(value * 10.0) / 10.0
    }
}

data class SetCalculationInput(
    val weightKg: Double,
    val reps: Int,
    val type: String = "normal",
    val completed: Boolean = true
)
