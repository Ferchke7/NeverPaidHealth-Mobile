package dev.neverpaid.health.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// ============================================================================
// 1. IDENTITY & AUTHENTICATION
// ============================================================================

@Serializable
data class UserProfileDto(
    @SerialName("id") val id: String = "",
    @SerialName("email") val email: String = "",
    @SerialName("display_name") val displayName: String = "Athlete",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("unit_preference") val unitPreference: String = "kg",
    @SerialName("created_at") val createdAt: String? = null,
    // Extended local athlete stats (matching web authStore.ts)
    @SerialName("height_cm") val heightCm: Double? = 178.0,
    @SerialName("weight_kg") val weightKg: Double? = null,
    @SerialName("target_weight_kg") val targetWeightKg: Double? = 75.0,
    @SerialName("gender") val gender: String = "male",
    @SerialName("diet_goal") val dietGoal: String = "maintain",
    @SerialName("target_calories") val targetCalories: Int = 2500,
    @SerialName("target_protein_g") val targetProteinG: Int = 170,
    @SerialName("target_carbs_g") val targetCarbsG: Int = 280,
    @SerialName("target_fat_g") val targetFatG: Int = 70
) {
    val name: String
        get() = displayName.ifBlank { email.substringBefore("@").ifBlank { "Athlete" } }
}

@Serializable
data class AuthResponseDto(
    @SerialName("access_token") val accessToken: String,
    @SerialName("expires_in") val expiresIn: Int = 2592000,
    @SerialName("user") val user: UserProfileDto
)

@Serializable
data class GoogleAuthRequestDto(
    @SerialName("id_token") val idToken: String
)

@Serializable
data class UpdateUnitPreferenceRequestDto(
    @SerialName("unit_preference") val unitPreference: String
)

// ============================================================================
// 2. EXERCISES CATALOG
// ============================================================================

@Serializable
data class Exercise(
    @SerialName("id") val id: String,
    @SerialName("name") val name: String,
    @SerialName("primary_muscle_group") val primaryMuscleGroup: String = "chest",
    @SerialName("primary_muscle") val primaryMuscleRaw: String? = null,
    @SerialName("secondary_muscle_groups") val secondaryMuscleGroups: List<String> = emptyList(),
    @SerialName("equipment") val equipment: String = "barbell",
    @SerialName("measurement_type") val measurementType: String = "weight_reps",
    @SerialName("is_custom") val isCustom: Boolean = false,
    @SerialName("created_by_user_id") val createdByUserId: String? = null,
    @SerialName("source_id") val sourceId: String? = null,
    @SerialName("category") val category: String = "compound",
    @SerialName("level") val level: String? = null,
    @SerialName("instructions") val instructionsList: List<String> = emptyList()
) {
    val primaryMuscle: String
        get() = (primaryMuscleRaw ?: primaryMuscleGroup).ifBlank { "chest" }

    val effectiveMuscle: String
        get() = primaryMuscle

    val instructions: String
        get() = instructionsList.joinToString("\n")
}

@Serializable
data class SaveExerciseRequestDto(
    @SerialName("name") val name: String,
    @SerialName("primary_muscle_group") val primaryMuscleGroup: String = "chest",
    @SerialName("secondary_muscle_groups") val secondaryMuscleGroups: List<String> = emptyList(),
    @SerialName("equipment") val equipment: String = "barbell",
    @SerialName("measurement_type") val measurementType: String = "weight_reps"
) {
    constructor(
        name: String,
        primaryMuscle: String,
        equipment: String = "barbell",
        category: String = "compound",
        instructions: String = ""
    ) : this(
        name = name,
        primaryMuscleGroup = primaryMuscle,
        equipment = equipment
    )
}

// ============================================================================
// 3. ROUTINES
// ============================================================================

@Serializable
data class RoutineExercise(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("target_sets") val targetSets: Int = 3,
    @SerialName("target_reps_min") val targetRepsMin: Int = 8,
    @SerialName("target_reps_max") val targetRepsMax: Int = 12
)

@Serializable
data class Routine(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("exercises") val exercises: List<RoutineExercise> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_prebuilt") val isPrebuilt: Boolean = false,
    @SerialName("category") val category: String? = null
) {
    val title: String get() = name
    val description: String get() = notes ?: ""
}

@Serializable
data class SaveRoutineRequestDto(
    @SerialName("name") val name: String,
    @SerialName("notes") val notes: String? = null,
    @SerialName("exercises") val exercises: List<RoutineExercise> = emptyList()
) {
    constructor(
        title: String,
        description: String = "",
        exercises: List<RoutineExercise> = emptyList()
    ) : this(name = title, notes = description, exercises = exercises)

    val title: String get() = name
    val description: String get() = notes ?: ""
}

// ============================================================================
// 4. WORKOUTS & ACTIVE SESSION
// ============================================================================

@Serializable
data class WorkoutSet(
    @SerialName("id") val id: String = "",
    @SerialName("set_number") val setNumber: Int = 1,
    @SerialName("set_type") val setType: String = "normal", // "warmup", "normal", "dropset", "failure"
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    @SerialName("reps") val reps: Int = 10,
    @SerialName("rpe") val rpe: Double? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("distance_meters") val distanceMeters: Double? = null,
    @SerialName("is_pr") val isPr: Boolean = false,
    @SerialName("completed") val isCompleted: Boolean = false,
    @SerialName("previous_weight_kg") val previousWeightKg: Double? = null,
    @SerialName("previous_reps") val previousReps: Int? = null
) {
    constructor(
        id: String = "",
        setNumber: Int = 1,
        type: String,
        weightKg: Double = 0.0,
        reps: Int = 10,
        rpe: Double? = null,
        isCompleted: Boolean = false,
        previousWeightKg: Double? = null,
        previousReps: Int? = null,
        isPr: Boolean = false
    ) : this(
        id = id,
        setNumber = setNumber,
        setType = type,
        weightKg = weightKg,
        reps = reps,
        rpe = rpe,
        isCompleted = isCompleted,
        previousWeightKg = previousWeightKg,
        previousReps = previousReps,
        isPr = isPr
    )

    val type: String get() = setType
    val isWarmup: Boolean get() = setType == "warmup"
}

@Serializable
data class WorkoutExercise(
    @SerialName("instance_id") val instanceId: String = "",
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String = "",
    @SerialName("measurement_type") val measurementType: String = "weight_reps",
    @SerialName("primary_muscle_group") val primaryMuscleGroup: String = "chest",
    @SerialName("equipment") val equipment: String = "barbell",
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("notes") val notes: String = "",
    @SerialName("sets") val sets: List<WorkoutSet> = emptyList()
) {
    constructor(
        id: String,
        exerciseId: String,
        exercise: Exercise,
        notes: String = "",
        sets: List<WorkoutSet> = emptyList()
    ) : this(
        instanceId = id,
        exerciseId = exerciseId,
        exerciseName = exercise.name,
        measurementType = exercise.measurementType,
        primaryMuscleGroup = exercise.effectiveMuscle,
        equipment = exercise.equipment,
        notes = notes,
        sets = sets
    )

    val id: String get() = instanceId.ifBlank { exerciseId }
    val exercise: Exercise
        get() = Exercise(
            id = exerciseId,
            name = exerciseName.ifBlank { exerciseId },
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType
        )
}

@Serializable
data class Workout(
    @SerialName("id") val id: String,
    @SerialName("routine_id") val routineId: String? = null,
    @SerialName("name") val name: String = "Workout",
    @SerialName("status") val status: String = "completed",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("finished_at") val finishedAt: String? = null,
    @SerialName("duration_seconds") val durationSeconds: Long = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("completed_sets_count") val completedSetsCount: Int = 0,
    @SerialName("exercises") val exercises: List<WorkoutExercise> = emptyList(),
    @SerialName("notes") val notes: String = "",
    @SerialName("start_time_epoch_ms") val startTimeEpochMs: Long = 0L,
    @SerialName("end_time_epoch_ms") val endTimeEpochMs: Long? = null,
    @SerialName("is_finished") val isFinished: Boolean = false
) {
    constructor(
        id: String,
        title: String,
        startTimeEpochMs: Long = 0L,
        endTimeEpochMs: Long? = null,
        durationSeconds: Long = 0L,
        totalVolumeKg: Double = 0.0,
        isFinished: Boolean = false,
        exercises: List<WorkoutExercise> = emptyList(),
        routineId: String? = null,
        notes: String = ""
    ) : this(
        id = id,
        routineId = routineId,
        name = title,
        durationSeconds = durationSeconds,
        totalVolumeKg = totalVolumeKg,
        exercises = exercises,
        notes = notes,
        startTimeEpochMs = startTimeEpochMs,
        endTimeEpochMs = endTimeEpochMs,
        isFinished = isFinished
    )

    val title: String get() = name
    val totalVolume: Double
        get() = if (totalVolumeKg > 0.0) totalVolumeKg else exercises.sumOf { ex ->
            ex.sets.filter { it.isCompleted }.sumOf { it.weightKg * it.reps }
        }
}

@Serializable
data class WorkoutsListResponseDto(
    @SerialName("items") val items: List<Workout> = emptyList()
)

@Serializable
data class StartWorkoutRequestDto(
    @SerialName("id") val id: String,
    @SerialName("routine_id") val routineId: String? = null,
    @SerialName("name") val name: String,
    @SerialName("replace_active") val replaceActive: Boolean = true
)

@Serializable
data class FinishSetPayloadDto(
    @SerialName("id") val id: String,
    @SerialName("set_number") val setNumber: Int,
    @SerialName("set_type") val setType: String = "normal",
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("reps") val reps: Int,
    @SerialName("rpe") val rpe: Double? = null,
    @SerialName("duration_seconds") val durationSeconds: Int? = null,
    @SerialName("distance_meters") val distanceMeters: Double? = null,
    @SerialName("completed") val completed: Boolean = true
)

@Serializable
data class FinishExercisePayloadDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("measurement_type") val measurementType: String = "weight_reps",
    @SerialName("notes") val notes: String? = null,
    @SerialName("sets") val sets: List<FinishSetPayloadDto>
)

@Serializable
data class FinishWorkoutRequestDto(
    @SerialName("duration_seconds") val durationSeconds: Long,
    @SerialName("exercises") val exercises: List<FinishExercisePayloadDto>
)

@Serializable
data class PreviousSetData(
    @SerialName("set_number") val setNumber: Int = 1,
    @SerialName("weight_kg") val weightKg: Double = 0.0,
    @SerialName("reps") val reps: Int = 0,
    @SerialName("rpe") val rpe: Double? = null,
    @SerialName("date") val date: String? = null
)

@Serializable
data class PreviousExerciseNote(
    @SerialName("note") val note: String = "",
    @SerialName("workout_name") val workoutName: String = "",
    @SerialName("date") val date: String = ""
)

@Serializable
data class PreviousPerformanceResponseDto(
    @SerialName("items") val items: Map<String, List<PreviousSetData>> = emptyMap(),
    @SerialName("notes") val notes: Map<String, PreviousExerciseNote> = emptyMap()
)

// ============================================================================
// 5. PROGRAMS, SPLITS & COMMUNITY
// ============================================================================

@Serializable
data class ProgramDayExercise(
    @SerialName("id") val id: String = "",
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("order_index") val orderIndex: Int = 0,
    @SerialName("target_sets") val targetSets: Int = 3,
    @SerialName("target_reps_min") val targetRepsMin: Int = 8,
    @SerialName("target_reps_max") val targetRepsMax: Int = 12,
    @SerialName("target_reps") val targetReps: String = "$targetRepsMin-$targetRepsMax"
)

@Serializable
data class ProgramDay(
    @SerialName("id") val id: String = "",
    @SerialName("day_number") val dayNumber: Int,
    @SerialName("name") val name: String,
    @SerialName("is_rest_day") val isRestDay: Boolean = false,
    @SerialName("notes") val notes: String? = null,
    @SerialName("exercises") val exercises: List<ProgramDayExercise> = emptyList()
) {
    val title: String get() = name
}

@Serializable
data class TrainingProgram(
    @SerialName("id") val id: String,
    @SerialName("author_id") val authorId: String? = null,
    @SerialName("author_name") val authorName: String = "Атлет",
    @SerialName("author_avatar") val authorAvatar: String? = null,
    @SerialName("name") val name: String,
    @SerialName("description") val description: String = "",
    @SerialName("split_type") val splitType: String = "ppl",
    @SerialName("days_per_week") val daysPerWeek: Int = 3,
    @SerialName("level") val level: String = "intermediate",
    @SerialName("duration_weeks") val durationWeeks: Int = 8,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("is_prebuilt") val isPrebuilt: Boolean = false,
    @SerialName("likes_count") val likesCount: Int = 0,
    @SerialName("installs_count") val installsCount: Int = 0,
    @SerialName("comments_count") val commentsCount: Int = 0,
    @SerialName("days") val days: List<ProgramDay> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("is_liked") val isLiked: Boolean = false,
    @SerialName("is_active") val isActive: Boolean = false
) {
    constructor(
        id: String,
        name: String,
        description: String = "",
        splitType: String = "ppl",
        difficulty: String,
        daysPerWeek: Int = 3,
        durationWeeks: Int = 8,
        isPublic: Boolean = false,
        likesCount: Int = 0,
        days: List<ProgramDay> = emptyList()
    ) : this(
        id = id,
        name = name,
        description = description,
        splitType = splitType,
        daysPerWeek = daysPerWeek,
        level = difficulty,
        durationWeeks = durationWeeks,
        isPublic = isPublic,
        likesCount = likesCount,
        days = days
    )

    val title: String get() = name
    val difficulty: String get() = level
    val authorUserId: String get() = authorId ?: ""
}

@Serializable
data class SaveProgramRequestDto(
    @SerialName("name") val name: String,
    @SerialName("description") val description: String = "",
    @SerialName("split_type") val splitType: String = "ppl",
    @SerialName("days_per_week") val daysPerWeek: Int = 3,
    @SerialName("level") val level: String = "intermediate",
    @SerialName("duration_weeks") val durationWeeks: Int = 8,
    @SerialName("is_public") val isPublic: Boolean = false,
    @SerialName("days") val days: List<ProgramDay> = emptyList()
) {
    constructor(
        title: String,
        description: String = "",
        splitType: String = "ppl",
        difficulty: String = "intermediate",
        daysPerWeek: Int = 3,
        durationWeeks: Int = 8,
        isPublic: Boolean = false,
        days: List<ProgramDay> = emptyList()
    ) : this(
        name = title,
        description = description,
        splitType = splitType,
        daysPerWeek = daysPerWeek,
        level = difficulty,
        durationWeeks = durationWeeks,
        isPublic = isPublic,
        days = days
    )

    val title: String get() = name
    val difficulty: String get() = level
}

@Serializable
data class PublishProgramRequestDto(
    @SerialName("is_public") val isPublic: Boolean
)

@Serializable
data class InstallProgramRequestDto(
    @SerialName("set_active") val setActive: Boolean = true
)

@Serializable
data class SetActiveProgramRequestDto(
    @SerialName("user_program_id") val userProgramId: String
)

@Serializable
data class UserProgram(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String = "",
    @SerialName("program_id") val programId: String,
    @SerialName("program_name") val programName: String = "",
    @SerialName("is_active") val isActive: Boolean = false,
    @SerialName("current_day_index") val currentDayIndex: Int = 0,
    @SerialName("installed_at") val installedAt: String? = null
) {
    val currentDayNumber: Int get() = currentDayIndex + 1
}

@Serializable
data class ActiveProgramBundle(
    @SerialName("user_program") val userProgram: UserProgram? = null,
    @SerialName("program") val program: TrainingProgram? = null
) {
    constructor(
        userProgramId: String,
        currentDayNumber: Int = 1,
        isActive: Boolean = true,
        program: TrainingProgram
    ) : this(
        userProgram = UserProgram(
            id = userProgramId,
            programId = program.id,
            programName = program.name,
            isActive = isActive,
            currentDayIndex = (currentDayNumber - 1).coerceAtLeast(0)
        ),
        program = program
    )

    val currentDayNumber: Int
        get() = (userProgram?.currentDayIndex ?: 0) + 1
}

@Serializable
data class LikeStatusResponseDto(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("likes_count") val likesCount: Int? = null
)

@Serializable
data class ProgramComment(
    @SerialName("id") val id: String,
    @SerialName("program_id") val programId: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("author_name") val authorName: String = "Атлет",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("content") val content: String,
    @SerialName("upvotes") val upvotes: Int = 0,
    @SerialName("has_upvoted") val hasUpvoted: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
    val replies: List<ProgramComment> = emptyList()
) {
    val userName: String get() = authorName
    val userAvatarUrl: String? get() = avatarUrl
    val likesCount: Int get() = upvotes
    val isLiked: Boolean get() = hasUpvoted
    val isUpvoted: Boolean get() = hasUpvoted
    val authorUserId: String get() = userId
}

@Serializable
data class PostProgramCommentRequestDto(
    @SerialName("content") val content: String,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("author_name") val authorName: String? = null,
    @SerialName("avatar_url") val avatarUrl: String? = null
)

@Serializable
data class CommentUpvoteResponseDto(
    @SerialName("upvotes") val upvotes: Int = 0,
    @SerialName("has_upvoted") val hasUpvoted: Boolean = false
)

@Serializable
data class AthleteProfile(
    @SerialName("id") val id: String,
    @SerialName("display_name") val displayName: String = "Атлет",
    @SerialName("avatar_url") val avatarUrl: String? = null,
    @SerialName("workouts_done") val workoutsDone: Int = 0,
    @SerialName("total_volume_kg") val totalVolumeKg: Double = 0.0,
    @SerialName("member_since") val memberSince: String? = null,
    @SerialName("public_programs") val publicPrograms: List<TrainingProgram> = emptyList()
) {
    val name: String get() = displayName
    val totalWorkouts: Int get() = workoutsDone
    val totalLikesReceived: Int get() = publicPrograms.sumOf { it.likesCount }
}

@Serializable
data class PlatformStats(
    @SerialName("total_workouts") val totalWorkouts: Int = 0,
    @SerialName("total_tonnage_kg") val totalTonnageKg: Double = 0.0,
    @SerialName("total_programs") val totalPrograms: Int = 0,
    @SerialName("total_installs") val totalInstalls: Int = 0,
    @SerialName("total_feedbacks") val totalFeedbacks: Int = 0,
    @SerialName("total_reviews") val totalReviews: Int = 0,
    @SerialName("active_athletes_30d") val activeAthletes30d: Int = 1
) {
    val totalUsers: Int get() = activeAthletes30d
    val totalVolumeKg: Double get() = totalTonnageKg
    val totalMeals: Int get() = totalInstalls
    val totalAiQueries: Int get() = totalReviews
}

@Serializable
data class FeedbackReport(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("author_name") val authorName: String = "Атлет",
    @SerialName("category") val category: String = "feature",
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("status") val status: String = "open",
    @SerialName("upvotes_count") val upvotesCount: Int = 0,
    @SerialName("has_upvoted") val hasUpvoted: Boolean = false,
    @SerialName("created_at") val createdAt: String = ""
) {
    val userName: String get() = authorName
    val message: String get() = if (description.isNotBlank()) "$title — $description" else title
    val upvotes: Int get() = upvotesCount
    val upvotedByMe: Boolean get() = hasUpvoted
    val rating: Int get() = 5
}

typealias FeedbackItem = FeedbackReport
typealias User = UserProfileDto

@Serializable
data class CreateFeedbackRequestDto(
    @SerialName("category") val category: String,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String,
    @SerialName("author_name") val authorName: String
)

// ============================================================================
// 6. TODOS, DAILY SCHEDULE & FOCUS TIMER
// ============================================================================

@Serializable
data class TodoTask(
    @SerialName("id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String = "work", // "work", "study", "workout", "nutrition", "routine", "other"
    @SerialName("priority") val priority: String = "medium", // "high", "medium", "low"
    @SerialName("status") val status: String = "pending", // "pending", "in_progress", "completed", "skipped"
    @SerialName("scheduled_date") val scheduledDate: String = "",
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    @SerialName("spent_minutes") val spentMinutes: Int = 0,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("recurrence_pattern") val recurrencePattern: String = "none", // "none", "daily", "weekdays", "weekends", "weekly"
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String? = null
) {
    val isCompleted: Boolean get() = status == "completed"
    val notes: String get() = description
    val scheduledTime: String get() = startTime ?: ""
    val repeatDaily: Boolean get() = isRecurring || recurrencePattern == "daily"
    val target: String
        get() = when {
            !startTime.isNullOrBlank() && !endTime.isNullOrBlank() -> "$startTime – $endTime ($durationMinutes мин)"
            !startTime.isNullOrBlank() -> "$startTime ($durationMinutes мин)"
            description.isNotBlank() -> description
            else -> "$durationMinutes мин"
        }
    val pomodoroMinutes: Int get() = durationMinutes.coerceAtLeast(5)
    fun isCompletedOn(date: String): Boolean = status == "completed"
}

@Serializable
data class SaveTodoRequestDto(
    @SerialName("title") val title: String,
    @SerialName("description") val description: String = "",
    @SerialName("category") val category: String = "work",
    @SerialName("priority") val priority: String = "medium",
    @SerialName("scheduled_date") val scheduledDate: String = "",
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Int = 30,
    @SerialName("is_recurring") val isRecurring: Boolean = false,
    @SerialName("recurrence_pattern") val recurrencePattern: String = "none"
) {
    constructor(
        title: String,
        notes: String = "",
        category: String = "work",
        priority: String = "medium",
        scheduledDate: String = "",
        scheduledTime: String? = null,
        repeatDaily: Boolean = false,
        durationMinutes: Int = 30
    ) : this(
        title = title,
        description = notes,
        category = category,
        priority = priority,
        scheduledDate = scheduledDate,
        startTime = scheduledTime,
        durationMinutes = durationMinutes,
        isRecurring = repeatDaily,
        recurrencePattern = if (repeatDaily) "daily" else "none"
    )

    val notes: String get() = description
    val scheduledTime: String get() = startTime ?: ""
    val repeatDaily: Boolean get() = isRecurring
}

@Serializable
data class ActivityLogItem(
    @SerialName("id") val id: String,
    @SerialName("todo_id") val todoId: String? = null,
    @SerialName("task_title") val taskTitle: String,
    @SerialName("category") val category: String = "work",
    @SerialName("duration_minutes") val durationMinutes: Int = 0,
    @SerialName("session_type") val sessionType: String = "pomodoro",
    @SerialName("notes") val notes: String = "",
    @SerialName("started_at") val startedAt: String = "",
    @SerialName("ended_at") val endedAt: String = ""
) {
    val actionType: String get() = sessionType
    val durationSeconds: Int get() = durationMinutes * 60
    val loggedDate: String get() = startedAt.take(10)
}

@Serializable
data class LogFocusSessionRequestDto(
    @SerialName("todo_id") val todoId: String? = null,
    @SerialName("task_title") val taskTitle: String,
    @SerialName("category") val category: String = "work",
    @SerialName("duration_minutes") val durationMinutes: Int,
    @SerialName("session_type") val sessionType: String = "pomodoro",
    @SerialName("notes") val notes: String = "",
    @SerialName("mark_completed") val markCompleted: Boolean = true,
    @SerialName("date") val date: String
)

@Serializable
data class DayProgressStats(
    @SerialName("total_tasks") val totalTasks: Int = 0,
    @SerialName("completed_tasks") val completedTasks: Int = 0,
    @SerialName("completion_percent") val completionPercent: Int = 0
)

@Serializable
data class DailyScheduleResponseDto(
    @SerialName("date") val date: String = "",
    @SerialName("total_tasks") val totalTasks: Int = 0,
    @SerialName("completed_tasks") val completedTasks: Int = 0,
    @SerialName("total_planned_minutes") val totalPlannedMinutes: Int = 0,
    @SerialName("total_spent_minutes") val totalSpentMinutes: Int = 0,
    @SerialName("todos") val todos: List<TodoTask> = emptyList(),
    @SerialName("recent_logs") val recentLogs: List<ActivityLogItem> = emptyList(),
    @SerialName("day_progress") val dayProgress: Map<String, DayProgressStats> = emptyMap()
)

@Serializable
data class FocusTimerState(
    val isActive: Boolean = false,
    val isRunning: Boolean = false,
    val todoId: String? = null,
    val taskTitle: String = "",
    val category: String = "work",
    val mode: String = "pomodoro", // "pomodoro", "target_timer", "stopwatch"
    val pomodoroPhase: String = "work", // "work", "short_break", "long_break"
    val pomodoroRound: Int = 1,
    val totalPomodoroRounds: Int = 1,
    val targetTotalMinutes: Int = 25,
    val splitStrategy: String = "25m", // "25m", "30m", "50m", "single"
    val roundDurationsSec: List<Int> = listOf(25 * 60),
    val workDurationSec: Int = 25 * 60,
    val shortBreakDurationSec: Int = 5 * 60,
    val longBreakDurationSec: Int = 15 * 60,
    val secondsRemaining: Int = 25 * 60,
    val secondsElapsedTotal: Int = 0,
    val isModalOpen: Boolean = false
) {
    val isOpen: Boolean get() = isActive
    val isMinimized: Boolean get() = isActive && !isModalOpen
    val elapsedSeconds: Int get() = secondsElapsedTotal
    val secondsLeft: Int get() = secondsRemaining
    val totalSeconds: Int get() = workDurationSec.coerceAtLeast(1)
    val customMinutes: Int get() = targetTotalMinutes
    val pomodoroCount: Int get() = pomodoroRound
}

// ============================================================================
// 7. NUTRITION & AI FOOD ANALYZER
// ============================================================================

@Serializable
data class RecognizedFoodItem(
    @SerialName("name") val name: String,
    @SerialName("portion") val portion: String = "",
    @SerialName("calories") val calories: Int = 0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0
)

@Serializable
data class MealAnalysisResult(
    @SerialName("meal_name") val mealName: String = "",
    @SerialName("total_calories") val totalCalories: Int = 0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("fiber_g") val fiberG: Double = 0.0,
    @SerialName("confidence") val confidence: String = "high",
    @SerialName("health_score") val healthScore: Int = 8,
    @SerialName("items") val items: List<RecognizedFoodItem> = emptyList(),
    @SerialName("coach_tip") val coachTip: String = ""
) {
    val title: String get() = mealName
    val name: String get() = mealName
    val calories: Int get() = totalCalories
    val fatsG: Double get() = fatG
    val foodItems: List<RecognizedFoodItem> get() = items
    val aiNotes: String get() = coachTip
}

@Serializable
data class MealLog(
    @SerialName("id") val id: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("meal_type") val mealType: String = "lunch", // "breakfast", "lunch", "dinner", "snack"
    @SerialName("name") val name: String = "",
    @SerialName("calories") val calories: Int = 0,
    @SerialName("protein_g") val proteinG: Double = 0.0,
    @SerialName("carbs_g") val carbsG: Double = 0.0,
    @SerialName("fat_g") val fatG: Double = 0.0,
    @SerialName("photo_url") val photoUrl: String? = null,
    @SerialName("items") val items: List<RecognizedFoodItem> = emptyList(),
    @SerialName("logged_at") val loggedAt: String = ""
) {
    constructor(
        id: String = "",
        mealType: String = "lunch",
        title: String,
        calories: Int = 0,
        proteinGrams: Double = 0.0,
        fatsGrams: Double = 0.0,
        carbsGrams: Double = 0.0,
        loggedDate: String = "",
        foodItems: List<RecognizedFoodItem> = emptyList(),
        aiConfidence: String? = null,
        notes: String = ""
    ) : this(
        id = id,
        date = loggedDate,
        mealType = mealType,
        name = title,
        calories = calories,
        proteinG = proteinGrams,
        carbsG = carbsGrams,
        fatG = fatsGrams,
        items = foodItems
    )

    val title: String get() = name
    val fatsG: Int get() = fatG.toInt()
    val proteinGrams: Double get() = proteinG
    val fatsGrams: Double get() = fatG
    val carbsGrams: Double get() = carbsG
    val loggedDate: String get() = date
    val foodItems: List<RecognizedFoodItem> get() = items
    val aiNotes: String get() = ""
}

@Serializable
data class SaveMealRequestDto(
    @SerialName("meal_type") val mealType: String,
    @SerialName("name") val name: String,
    @SerialName("calories") val calories: Int,
    @SerialName("protein_g") val proteinG: Double,
    @SerialName("carbs_g") val carbsG: Double,
    @SerialName("fat_g") val fatG: Double,
    @SerialName("items") val items: List<RecognizedFoodItem> = emptyList()
)

@Serializable
data class AnalyzePhotoRequestDto(
    @SerialName("image_base64") val imageBase64: String,
    @SerialName("mime_type") val mimeType: String = "image/jpeg",
    @SerialName("notes") val notes: String = "",
    @SerialName("language") val language: String = "ru"
)

@Serializable
data class EstimateTextFoodRequestDto(
    @SerialName("description") val description: String,
    @SerialName("language") val language: String = "ru"
)

@Serializable
data class DailyNutritionSummaryDto(
    @SerialName("date") val date: String = "",
    @SerialName("total_calories") val totalCalories: Int = 0,
    @SerialName("total_protein_g") val totalProteinG: Double = 0.0,
    @SerialName("total_carbs_g") val totalCarbsG: Double = 0.0,
    @SerialName("total_fat_g") val totalFatG: Double = 0.0,
    @SerialName("target_calories") val targetCalories: Int = 2500,
    @SerialName("target_protein_g") val targetProteinG: Double = 170.0,
    @SerialName("target_carbs_g") val targetCarbsG: Double = 280.0,
    @SerialName("target_fat_g") val targetFatG: Double = 70.0,
    @SerialName("meals") val meals: List<MealLog> = emptyList()
)

@Serializable
data class NutritionSummary(
    val date: String,
    val totalCalories: Int = 0,
    val targetCalories: Int = 2500,
    val totalProtein: Int = 0,
    val targetProtein: Int = 170,
    val totalFats: Int = 0,
    val targetFats: Int = 70,
    val totalCarbs: Int = 0,
    val targetCarbs: Int = 280,
    val waterMl: Int = 0,
    val targetWaterMl: Int = 2000
)

// ============================================================================
// 8. AI COACH
// ============================================================================

@Serializable
data class OverloadTarget(
    @SerialName("exercise_id") val exerciseId: String = "",
    @SerialName("exercise_name") val exerciseName: String = "",
    @SerialName("last_weight_kg") val lastWeightKg: Double = 0.0,
    @SerialName("last_reps") val lastReps: Int = 0,
    @SerialName("target_weight_kg") val targetWeightKg: Double = 0.0,
    @SerialName("target_reps") val targetReps: Int = 0,
    @SerialName("reason") val reason: String = ""
)

@Serializable
data class SuggestedExercise(
    @SerialName("exercise_id") val exerciseId: String = "",
    @SerialName("exercise_name") val exerciseName: String = "",
    @SerialName("target_sets") val targetSets: Int = 3,
    @SerialName("target_reps") val targetReps: Int = 10,
    @SerialName("target_weight_kg") val targetWeightKg: Double = 0.0,
    @SerialName("rest_seconds") val restSeconds: Int = 90,
    @SerialName("notes") val notes: String = ""
) {
    val sets: Int get() = targetSets
    val repsMin: Int get() = targetReps
    val repsMax: Int get() = targetReps
}

@Serializable
data class SuggestedWorkoutPlan(
    @SerialName("name") val name: String = "",
    @SerialName("split_type") val splitType: String = "",
    @SerialName("estimated_duration_min") val estimatedDurationMin: Int = 45,
    @SerialName("focus") val focus: String = "",
    @SerialName("exercises") val exercises: List<SuggestedExercise> = emptyList()
) {
    val title: String get() = name
    val rationale: String get() = focus
}

@Serializable
data class CoachInsight(
    @SerialName("readiness_score") val readinessScore: Int = 88,
    @SerialName("recovery_status") val recoveryStatus: String = "optimal",
    @SerialName("weekly_workouts_count") val weeklyWorkoutsCount: Int = 0,
    @SerialName("weekly_volume_kg") val weeklyVolumeKg: Double = 0.0,
    @SerialName("days_since_last_train") val daysSinceLastTrain: Int = 0,
    @SerialName("suggested_split") val suggestedSplit: String = "",
    @SerialName("overload_targets") val overloadTargets: List<OverloadTarget> = emptyList(),
    @SerialName("recommended_workout") val recommendedWorkout: SuggestedWorkoutPlan? = null
) {
    val suggestedWorkout: SuggestedWorkoutPlan? get() = recommendedWorkout
    val cnsScore: Int get() = readinessScore
    val readinessLevel: String get() = recoveryStatus.replaceFirstChar { it.uppercase() }
    val statusMessage: String
        get() = suggestedSplit.ifBlank { "Recovery status: $recoveryStatus" }
    val progressiveOverloadTarget: String
        get() = overloadTargets.firstOrNull()?.let {
            "${it.exerciseName}: ${it.targetWeightKg} kg × ${it.targetReps}"
        } ?: "Progressive overload ready"
    val recoveryMetrics: Map<String, Int>
        get() = mapOf(
            "Readiness" to readinessScore,
            "Weekly Sessions" to weeklyWorkoutsCount,
            "Days Rest" to daysSinceLastTrain
        )
}

@Serializable
data class ChatMessage(
    @SerialName("id") val id: String = "",
    @SerialName("role") val role: String = "assistant",
    @SerialName("content") val content: String = "",
    @SerialName("has_image") val hasImage: Boolean = false,
    @SerialName("suggested_workout") val suggestedWorkout: SuggestedWorkoutPlan? = null,
    @SerialName("created_at") val createdAt: String = "",
    val timestampEpochMs: Long = 0L
) {
    val isUser: Boolean get() = role == "user"
    val text: String get() = content
}

@Serializable
data class CoachChatHistoryItemDto(
    @SerialName("role") val role: String,
    @SerialName("content") val content: String
)

@Serializable
data class CoachChatRequestDto(
    @SerialName("message") val message: String,
    @SerialName("image_base64") val imageBase64: String? = null,
    @SerialName("mime_type") val mimeType: String? = null,
    @SerialName("history") val history: List<CoachChatHistoryItemDto> = emptyList()
)

@Serializable
data class CoachChatResponseDto(
    @SerialName("reply") val reply: String = "",
    @SerialName("suggestions") val suggestions: List<String> = emptyList(),
    @SerialName("suggested_workout") val suggestedWorkout: SuggestedWorkoutPlan? = null
)

// ============================================================================
// 9. PROGRESS & RECORDS
// ============================================================================

@Serializable
data class PersonalRecordItemDto(
    @SerialName("pr_type") val prType: String = "best_e1rm",
    @SerialName("value") val value: Double = 0.0,
    @SerialName("achieved_at") val achievedAt: String = "",
    @SerialName("workout_id") val workoutId: String = ""
)

@Serializable
data class ExerciseRecordGroupDto(
    @SerialName("exercise_id") val exerciseId: String,
    @SerialName("exercise_name") val exerciseName: String,
    @SerialName("best_weight_kg") val bestWeightKg: Double? = null,
    @SerialName("best_e1rm_kg") val bestE1rmKg: Double? = null,
    @SerialName("max_volume_set_kg") val maxVolumeSetKg: Double? = null,
    @SerialName("max_reps") val maxReps: Int? = null,
    @SerialName("records") val records: List<PersonalRecordItemDto> = emptyList()
)

@Serializable
data class ExerciseHistoryPointDto(
    @SerialName("workout_id") val workoutId: String = "",
    @SerialName("date") val date: String = "",
    @SerialName("best_weight_kg") val bestWeightKg: Double = 0.0,
    @SerialName("best_e1rm_kg") val bestE1rmKg: Double = 0.0,
    @SerialName("total_exercise_volume_kg") val totalExerciseVolumeKg: Double = 0.0
)

@Serializable
data class ExerciseHistoryResponseDto(
    @SerialName("exercise_id") val exerciseId: String = "",
    @SerialName("exercise_name") val exerciseName: String = "",
    @SerialName("data_points") val dataPoints: List<ExerciseHistoryPointDto> = emptyList()
)

@Serializable
data class PersonalRecord(
    val exerciseId: String,
    val exerciseName: String,
    val weightKg: Double,
    val reps: Int,
    val e1rmKg: Double,
    val maxVolumeSetKg: Double = 0.0,
    val achievedAt: String = "",
    val achievedAtEpochMs: Long = 0L
)

@Serializable
data class ProgressSummary(
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val currentStreakDays: Int = 0,
    val personalRecords: List<PersonalRecord> = emptyList()
)

// ============================================================================
// 10. BODY LOGS & TRENDS
// ============================================================================

@Serializable
data class BodyMeasurement(
    @SerialName("id") val id: String = "",
    @SerialName("log_date") val logDate: String = "",
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("height_cm") val heightCm: Double = 178.0,
    @SerialName("body_fat_percentage") val bodyFatPercent: Double? = null,
    @SerialName("waist_cm") val waistCm: Double? = null,
    @SerialName("chest_cm") val chestCm: Double? = null,
    @SerialName("arms_cm") val armsCm: Double? = null,
    @SerialName("thighs_cm") val thighsCm: Double? = null,
    @SerialName("calves_cm") val calvesCm: Double? = null,
    @SerialName("neck_cm") val neckCm: Double? = null,
    @SerialName("calculated_bmi") val calculatedBmi: Double? = null,
    val dateEpochMs: Long = 0L
) {
    val loggedDate: String get() = logDate
    val bodyFatPercentage: Double? get() = bodyFatPercent
    val bicepsCm: Double? get() = armsCm
    val bmi: Double
        get() = calculatedBmi ?: if (heightCm > 0) {
            val hm = heightCm / 100.0
            kotlin.math.round((weightKg / (hm * hm)) * 10.0) / 10.0
        } else 22.0
}

@Serializable
data class SaveBodyLogRequestDto(
    @SerialName("log_date") val logDate: String,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("height_cm") val heightCm: Double? = null,
    @SerialName("body_fat_percentage") val bodyFatPercentage: Double? = null,
    @SerialName("waist_cm") val waistCm: Double? = null,
    @SerialName("chest_cm") val chestCm: Double? = null,
    @SerialName("arms_cm") val armsCm: Double? = null,
    @SerialName("neck_cm") val neckCm: Double? = null
) {
    constructor(
        loggedDate: String,
        weightKg: Double,
        heightCm: Double? = null,
        bodyFatPercentage: Double? = null,
        waistCm: Double? = null,
        chestCm: Double? = null,
        bicepsCm: Double? = null,
        bmi: Double? = null,
        notes: String? = null
    ) : this(
        logDate = loggedDate,
        weightKg = weightKg,
        heightCm = heightCm,
        bodyFatPercentage = bodyFatPercentage,
        waistCm = waistCm,
        chestCm = chestCm,
        armsCm = bicepsCm
    )
}

@Serializable
data class BodyTrendPointDto(
    @SerialName("date") val date: String,
    @SerialName("weight_kg") val weightKg: Double,
    @SerialName("sma_7day_kg") val sma7DayKg: Double
)

@Serializable
data class BodyTrendResponseDto(
    @SerialName("current_weight_kg") val currentWeightKg: Double = 0.0,
    @SerialName("current_7day_sma_kg") val current7DaySmaKg: Double = 0.0,
    @SerialName("delta_7day_kg") val delta7DayKg: Double? = null,
    @SerialName("delta_30day_kg") val delta30DayKg: Double? = null,
    @SerialName("delta_90day_kg") val delta90DayKg: Double? = null,
    @SerialName("trend_points") val trendPoints: List<BodyTrendPointDto> = emptyList()
)
