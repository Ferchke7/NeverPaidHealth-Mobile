package dev.neverpaid.health.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileDto(
    val id: String,
    val email: String,
    val name: String,
    val avatarUrl: String? = null,
    val unitPreference: String = "kg"
)

@Serializable
data class AuthResponseDto(
    val accessToken: String,
    val user: UserProfileDto
)

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
data class ProgramDay(
    val dayNumber: Int,
    val title: String,
    val splitType: String, // "push", "pull", "legs", "rest", "cardio", "fullbody"
    val isRestDay: Boolean = false,
    val exercises: List<Exercise> = emptyList()
)

@Serializable
data class TrainingProgram(
    val id: String,
    val title: String,
    val description: String,
    val level: String = "intermediate", // "beginner", "intermediate", "advanced"
    val daysPerWeek: Int = 4,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val commentsCount: Int = 0,
    val isActive: Boolean = false,
    val days: List<ProgramDay> = emptyList()
)

@Serializable
data class ProgramComment(
    val id: String,
    val programId: String,
    val userId: String,
    val userName: String,
    val userAvatarUrl: String? = null,
    val content: String,
    val createdAtEpochMs: Long,
    val parentId: String? = null,
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val replies: List<ProgramComment> = emptyList()
)

@Serializable
data class TodoTask(
    val id: String,
    val title: String,
    val target: String,
    val category: String = "WORKOUT", // "WORKOUT", "NUTRITION", "RECOVERY", "FOCUS"
    val priority: String = "NORMAL", // "HIGH", "NORMAL", "LOW"
    val isRecurringDaily: Boolean = true,
    val scheduledDate: String? = null, // "YYYY-MM-DD" for single-day tasks
    val completedDates: Set<String> = emptySet(), // Set of "YYYY-MM-DD"
    val pomodoroMinutes: Int = 25
) {
    fun isCompletedOn(date: String): Boolean = completedDates.contains(date)
}

@Serializable
data class MealLog(
    val id: String,
    val date: String, // "YYYY-MM-DD"
    val mealType: String, // "breakfast", "lunch", "dinner", "snack"
    val name: String,
    val calories: Int,
    val proteinG: Int,
    val fatsG: Int,
    val carbsG: Int
)

@Serializable
data class NutritionSummary(
    val date: String,
    val totalCalories: Int = 0,
    val targetCalories: Int = 2450,
    val totalProtein: Int = 0,
    val targetProtein: Int = 180,
    val totalFats: Int = 0,
    val targetFats: Int = 70,
    val totalCarbs: Int = 0,
    val targetCarbs: Int = 280,
    val waterMl: Int = 0,
    val targetWaterMl: Int = 3000
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
