package dev.neverpaid.health.core.math

import dev.neverpaid.health.domain.model.Workout
import kotlin.math.round

data class HistoricalSetRecord(
    val workoutId: String,
    val workoutName: String,
    val dateEpochMs: Long,
    val setNumber: Int,
    val setType: String,
    val weightKg: Double,
    val reps: Int,
    val e1rmKg: Double,
    val volumeKg: Double
)

data class AllTimeExercisePR(
    val best1RMKg: Double,
    val bestWeightKg: Double,
    val bestReps: Int,
    val totalSets: Int,
    val bestRecord: HistoricalSetRecord?
)

object SetHistoryCalculator {

    fun normalizeExerciseName(name: String?): String {
        if (name == null) return ""
        return name
            .lowercase()
            .replace(Regex("""\s*\([^)]*\)"""), "")
            .replace(Regex("""[^a-z0-9а-яё]""", RegexOption.IGNORE_CASE), "")
            .trim()
    }

    fun extractExerciseHistory(
        workouts: List<Workout>,
        exerciseId: String?,
        exerciseName: String?
    ): List<HistoricalSetRecord> {
        val records = mutableListOf<HistoricalSetRecord>()
        val targetNorm = normalizeExerciseName(exerciseName)

        for (w in workouts) {
            if (!w.isFinished) continue

            for (ex in w.exercises) {
                val idMatch = exerciseId != null && ex.exerciseId == exerciseId
                val nameMatch = targetNorm.isNotEmpty() && normalizeExerciseName(ex.exerciseName) == targetNorm

                if (idMatch || nameMatch) {
                    for (s in ex.sets) {
                        if (!s.isCompleted && s.weightKg <= 0.0 && s.reps <= 0) continue

                        val weight = s.weightKg
                        val reps = s.reps
                        if (weight <= 0.0 && reps <= 0) continue

                        val e1rm = StrengthCalculator.calculateE1RM(weight, reps) ?: weight
                        val volume = weight * reps

                        records.add(
                            HistoricalSetRecord(
                                workoutId = w.id,
                                workoutName = w.name.ifBlank { "Workout" },
                                dateEpochMs = w.startTimeEpochMs,
                                setNumber = s.setNumber,
                                setType = s.setType,
                                weightKg = weight,
                                reps = reps,
                                e1rmKg = round(e1rm * 10.0) / 10.0,
                                volumeKg = round(volume * 10.0) / 10.0
                            )
                        )
                    }
                }
            }
        }
        return records
    }

    fun getTop3BestForSet(
        allRecords: List<HistoricalSetRecord>,
        setNumber: Int
    ): Pair<List<HistoricalSetRecord>, Boolean> {
        if (allRecords.isEmpty()) return Pair(emptyList(), false)

        val exact = allRecords.filter { it.setNumber == setNumber }

        val comparator = Comparator<HistoricalSetRecord> { a, b ->
            val scoreA = if (a.weightKg > 0) a.e1rmKg * 1000 + a.reps else a.reps.toDouble()
            val scoreB = if (b.weightKg > 0) b.e1rmKg * 1000 + b.reps else b.reps.toDouble()
            val diff = scoreB.compareTo(scoreA)
            if (diff != 0) return@Comparator diff
            if (b.weightKg != a.weightKg) return@Comparator b.weightKg.compareTo(a.weightKg)
            if (b.reps != a.reps) return@Comparator b.reps.compareTo(a.reps)
            b.dateEpochMs.compareTo(a.dateEpochMs)
        }

        val sortedExact = exact.sortedWith(comparator)
        val uniqueExact = mutableListOf<HistoricalSetRecord>()
        val seen = mutableSetOf<String>()

        for (r in sortedExact) {
            val key = "${r.workoutId}-${r.weightKg}-${r.reps}"
            if (!seen.contains(key)) {
                seen.add(key)
                uniqueExact.add(r)
                if (uniqueExact.size >= 3) break
            }
        }

        if (uniqueExact.isNotEmpty()) {
            return Pair(uniqueExact, false)
        }

        // Fallback to top records across all sets of this exercise
        val sortedAll = allRecords.sortedWith(comparator)
        val fallback = mutableListOf<HistoricalSetRecord>()
        val seenAll = mutableSetOf<String>()

        for (r in sortedAll) {
            val key = "${r.workoutId}-${r.weightKg}-${r.reps}"
            if (!seenAll.contains(key)) {
                seenAll.add(key)
                fallback.add(r)
                if (fallback.size >= 3) break
            }
        }

        return Pair(fallback, fallback.isNotEmpty())
    }

    fun getAllTimePR(records: List<HistoricalSetRecord>): AllTimeExercisePR {
        if (records.isEmpty()) return AllTimeExercisePR(0.0, 0.0, 0, 0, null)

        var best1RM = 0.0
        var bestWeight = 0.0
        var bestReps = 0
        var bestRecord: HistoricalSetRecord? = null

        for (r in records) {
            if (r.e1rmKg > best1RM) {
                best1RM = r.e1rmKg
                bestRecord = r
            }
            if (r.weightKg > bestWeight) {
                bestWeight = r.weightKg
            }
            if (r.reps > bestReps) {
                bestReps = r.reps
            }
        }

        return AllTimeExercisePR(
            best1RMKg = best1RM,
            bestWeightKg = bestWeight,
            bestReps = bestReps,
            totalSets = records.size,
            bestRecord = bestRecord
        )
    }

    fun formatPlateBreakdown(weightKg: Double): String? {
        if (weightKg < 25.0) return null
        val perSide = (weightKg - 20.0) / 2.0
        val perSideStr = if (perSide % 1.0 == 0.0) "${perSide.toInt()}" else "$perSide"
        return "(20+${perSideStr}кг)"
    }
}
