package dev.neverpaid.health.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Exercise(
    val id: String,
    val name: String,
    val primaryMuscleGroup: String, // chest, back, quads, hamstrings, shoulders, biceps, triceps, core
    val secondaryMuscleGroups: List<String> = emptyList(),
    val equipment: String, // barbell, dumbbell, cable, machine, bodyweight, smith_machine
    val measurementType: String = "weight_reps",
    val isCustom: Boolean = false,
    val instructions: String = ""
)

@Serializable
data class WorkoutSet(
    val id: String,
    val setNumber: Int,
    val weightKg: Double,
    val reps: Int,
    val type: String = "normal", // "normal", "warmup", "drop", "failure"
    val isCompleted: Boolean = false,
    val rpe: Double? = null,
    val previousWeightKg: Double? = null,
    val previousReps: Int? = null
)

@Serializable
data class WorkoutExercise(
    val exerciseId: String,
    val exerciseName: String,
    val primaryMuscleGroup: String,
    val equipment: String,
    val sets: List<WorkoutSet> = emptyList(),
    val notes: String = ""
)

@Serializable
data class Workout(
    val id: String,
    val title: String,
    val startTimeEpochMs: Long,
    val endTimeEpochMs: Long? = null,
    val durationSeconds: Long = 0,
    val exercises: List<WorkoutExercise> = emptyList(),
    val totalVolumeKg: Double = 0.0,
    val isFinished: Boolean = false,
    val notes: String = ""
)

@Serializable
data class Routine(
    val id: String,
    val title: String,
    val description: String = "",
    val exercises: List<Exercise> = emptyList()
)

@Serializable
data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val e1rmKg: Double,
    val achievedAtEpochMs: Long
)

@Serializable
data class ProgressSummary(
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val currentStreakDays: Int = 0,
    val personalRecords: List<PersonalRecord> = emptyList()
)

@Serializable
data class CoachInsight(
    val cnsScore: Int = 85, // 0 - 100
    val readinessLevel: String = "Full Throttle", // "Full Throttle", "Moderate", "Deload Recommended"
    val statusMessage: String = "Recovery is optimal. Progressive overload suggested on compound lifts.",
    val progressiveOverloadTarget: String = "+2.5 kg on Barbell Bench Press",
    val recoveryMetrics: Map<String, Int> = mapOf(
        "Sleep Quality" to 88,
        "Volume Strain" to 72,
        "Hydration" to 95
    )
)

@Serializable
data class ChatMessage(
    val id: String,
    val isUser: Boolean,
    val text: String,
    val timestampEpochMs: Long
)

@Serializable
data class BodyMeasurement(
    val id: String,
    val dateEpochMs: Long,
    val weightKg: Double,
    val heightCm: Double,
    val bmi: Double,
    val bodyFatPercent: Double? = null
)
