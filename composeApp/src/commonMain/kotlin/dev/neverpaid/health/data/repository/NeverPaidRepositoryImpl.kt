package dev.neverpaid.health.data.repository

import dev.neverpaid.health.core.math.SetCalculationInput
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.PersonalRecord
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class NeverPaidRepositoryImpl : NeverPaidRepository {

    private val exercisesFlow = MutableStateFlow<List<Exercise>>(StandardExerciseCatalog.standardExercises)
    private val activeWorkoutFlow = MutableStateFlow<Workout?>(null)
    private val workoutHistoryFlow = MutableStateFlow<List<Workout>>(createInitialHistory())
    private val bodyMeasurementsFlow = MutableStateFlow<List<BodyMeasurement>>(createInitialBodyMeasurements())
    private val preferencesFlow = MutableStateFlow(UserPreferencesData())

    private val coachInsightFlow = MutableStateFlow(
        CoachInsight(
            cnsScore = 88,
            readinessLevel = "Full Throttle",
            statusMessage = "CNS recovery is optimal. High power output expected on Barbell Bench Press.",
            progressiveOverloadTarget = "+2.5 kg on Bench Press (Target: 102.5 kg × 5)",
            recoveryMetrics = mapOf(
                "Sleep Score" to 92,
                "Rest Heart Rate" to 54,
                "Hypertrophy Strain" to 78,
                "Hydration Balance" to 96
            )
        )
    )

    private val chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(
        listOf(
            ChatMessage(
                id = "m-1",
                isUser = false,
                text = "Welcome to NeverPaidHealth AI Strength Coach. Your 7-day volume load is balanced and CNS readiness is 88/100 (Full Throttle). What are we hitting today?",
                timestampEpochMs = PlatformBridge.currentTimeMillis() - 3600000
            )
        )
    )

    override fun getExercises(): Flow<List<Exercise>> = exercisesFlow.asStateFlow()

    override fun getExerciseById(id: String): Exercise? {
        return exercisesFlow.value.find { it.id == id }
    }

    override fun searchExercises(query: String, muscle: String?, equipment: String?): Flow<List<Exercise>> {
        return exercisesFlow.map { list ->
            list.filter { item ->
                val matchesQuery = query.isBlank() || item.name.contains(query, ignoreCase = true)
                val matchesMuscle = muscle.isNullOrBlank() || muscle == "All" || item.primaryMuscleGroup.equals(muscle, ignoreCase = true)
                val matchesEquipment = equipment.isNullOrBlank() || equipment == "All" || item.equipment.equals(equipment, ignoreCase = true)
                matchesQuery && matchesMuscle && matchesEquipment
            }
        }
    }

    override fun getActiveWorkout(): Flow<Workout?> = activeWorkoutFlow.asStateFlow()

    override fun startWorkout(title: String, routine: Routine?): Workout {
        val initialExercises = if (routine != null) {
            routine.exercises.map { ex ->
                WorkoutExercise(
                    exerciseId = ex.id,
                    exerciseName = ex.name,
                    primaryMuscleGroup = ex.primaryMuscleGroup,
                    equipment = ex.equipment,
                    sets = listOf(
                        WorkoutSet(
                            id = PlatformBridge.generateUuid(),
                            setNumber = 1,
                            weightKg = 60.0,
                            reps = 10,
                            type = "warmup",
                            isCompleted = false,
                            previousWeightKg = 60.0,
                            previousReps = 10
                        ),
                        WorkoutSet(
                            id = PlatformBridge.generateUuid(),
                            setNumber = 2,
                            weightKg = 100.0,
                            reps = 5,
                            type = "normal",
                            isCompleted = false,
                            previousWeightKg = 100.0,
                            previousReps = 5
                        )
                    )
                )
            }
        } else {
            emptyList()
        }

        val newWorkout = Workout(
            id = PlatformBridge.generateUuid(),
            title = if (title.isNotBlank()) title else "Evening Workout",
            startTimeEpochMs = PlatformBridge.currentTimeMillis(),
            exercises = initialExercises,
            totalVolumeKg = 0.0,
            isFinished = false
        )

        activeWorkoutFlow.value = newWorkout
        return newWorkout
    }

    override fun updateActiveWorkout(workout: Workout) {
        val allSets = workout.exercises.flatMap { it.sets }
        val calcInputs = allSets.map {
            SetCalculationInput(
                weightKg = it.weightKg,
                reps = it.reps,
                type = it.type,
                completed = it.isCompleted
            )
        }
        val computedVolume = StrengthCalculator.calculateVolume(calcInputs)
        val updated = workout.copy(totalVolumeKg = computedVolume)
        activeWorkoutFlow.value = updated
    }

    override fun finishActiveWorkout(): Workout? {
        val current = activeWorkoutFlow.value ?: return null
        val now = PlatformBridge.currentTimeMillis()
        val duration = ((now - current.startTimeEpochMs) / 1000).coerceAtLeast(60)

        val finishedWorkout = current.copy(
            endTimeEpochMs = now,
            durationSeconds = duration,
            isFinished = true
        )

        val updatedHistory = listOf(finishedWorkout) + workoutHistoryFlow.value
        workoutHistoryFlow.value = updatedHistory
        activeWorkoutFlow.value = null
        return finishedWorkout
    }

    override fun discardActiveWorkout() {
        activeWorkoutFlow.value = null
    }

    override fun getWorkoutHistory(): Flow<List<Workout>> = workoutHistoryFlow.asStateFlow()

    override fun getRoutines(): List<Routine> = StandardExerciseCatalog.prebuiltRoutines

    override fun getProgressSummary(): Flow<ProgressSummary> {
        return workoutHistoryFlow.map { history ->
            val totalWorkouts = history.size
            val totalVolume = history.sumOf { it.totalVolumeKg }
            val totalReps = history.sumOf { w ->
                w.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.reps } }
            }

            // Extract best PRs
            val prMap = mutableMapOf<String, PersonalRecord>()
            for (w in history) {
                for (ex in w.exercises) {
                    for (s in ex.sets.filter { it.isCompleted }) {
                        val e1rm = StrengthCalculator.calculateE1RM(s.weightKg, s.reps) ?: s.weightKg
                        val existing = prMap[ex.exerciseName]
                        if (existing == null || e1rm > existing.e1rmKg) {
                            prMap[ex.exerciseName] = PersonalRecord(
                                exerciseId = ex.exerciseId,
                                exerciseName = ex.exerciseName,
                                weightKg = s.weightKg,
                                reps = s.reps,
                                e1rmKg = e1rm,
                                achievedAtEpochMs = w.startTimeEpochMs
                            )
                        }
                    }
                }
            }

            ProgressSummary(
                totalWorkouts = totalWorkouts,
                totalVolumeKg = totalVolume,
                totalReps = totalReps,
                currentStreakDays = 4,
                personalRecords = prMap.values.toList()
            )
        }
    }

    override fun getCoachInsight(): Flow<CoachInsight> = coachInsightFlow.asStateFlow()

    override fun getCoachChat(): Flow<List<ChatMessage>> = chatMessagesFlow.asStateFlow()

    override suspend fun sendCoachChatMessage(text: String): ChatMessage {
        val userMsg = ChatMessage(
            id = PlatformBridge.generateUuid(),
            isUser = true,
            text = text,
            timestampEpochMs = PlatformBridge.currentTimeMillis()
        )
        val current = chatMessagesFlow.value
        chatMessagesFlow.value = current + userMsg

        // AI Response simulation
        val replyText = generateAiCoachResponse(text)
        val aiMsg = ChatMessage(
            id = PlatformBridge.generateUuid(),
            isUser = false,
            text = replyText,
            timestampEpochMs = PlatformBridge.currentTimeMillis() + 500
        )
        chatMessagesFlow.value = chatMessagesFlow.value + aiMsg
        return aiMsg
    }

    override fun getBodyMeasurements(): Flow<List<BodyMeasurement>> = bodyMeasurementsFlow.asStateFlow()

    override suspend fun addBodyMeasurement(weightKg: Double, heightCm: Double, bodyFatPercent: Double?) {
        val bmi = StrengthCalculator.calculateBMI(weightKg, heightCm) ?: 22.0
        val measurement = BodyMeasurement(
            id = PlatformBridge.generateUuid(),
            dateEpochMs = PlatformBridge.currentTimeMillis(),
            weightKg = weightKg,
            heightCm = heightCm,
            bmi = bmi,
            bodyFatPercent = bodyFatPercent
        )
        bodyMeasurementsFlow.value = bodyMeasurementsFlow.value + measurement
    }

    override fun getUserPreferences(): Flow<UserPreferencesData> = preferencesFlow.asStateFlow()

    override suspend fun updatePreferences(preferences: UserPreferencesData) {
        preferencesFlow.value = preferences
    }

    private fun generateAiCoachResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("bench") || lower.contains("chest") ->
                "For Barbell Bench Press, your estimated 1RM is 116.7 kg. Today's recommendation: 3 sets of 5 reps at 100 kg, focusing on controlled eccentric (2 sec down) and leg drive."
            lower.contains("squat") || lower.contains("leg") ->
                "Squat mechanics analysis: ensure your core is braced with diaphragmatic breathing before descent. Aim for 4 sets of 6 reps with 2 min rest."
            lower.contains("deadlift") || lower.contains("back") ->
                "Deadlifts heavily fatigue the spinal erectors and CNS. Maintain neutral cervical spine and engage lats before breaking off the floor."
            lower.contains("pain") || lower.contains("sore") ->
                "If experiencing joint discomfort rather than muscular fatigue, reduce intensity by 20% or switch to dumbbells for a more natural scapular plane."
            else ->
                "Keep consistency high! Progressive overload requires steady weekly volume increments. Rest 90-120 seconds between heavy compound sets."
        }
    }

    private fun createInitialHistory(): List<Workout> {
        val now = PlatformBridge.currentTimeMillis()
        val dayMs = 86400000L

        return listOf(
            Workout(
                id = "hist-1",
                title = "Push Day (Chest & Triceps)",
                startTimeEpochMs = now - (dayMs * 1),
                endTimeEpochMs = now - (dayMs * 1) + 3600000,
                durationSeconds = 3600,
                totalVolumeKg = 8450.0,
                isFinished = true,
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "10000000-0000-0000-0000-000000000001",
                        exerciseName = "Barbell Bench Press",
                        primaryMuscleGroup = "chest",
                        equipment = "barbell",
                        sets = listOf(
                            WorkoutSet("s1", 1, 60.0, 10, "warmup", true),
                            WorkoutSet("s2", 2, 90.0, 8, "normal", true),
                            WorkoutSet("s3", 3, 100.0, 5, "normal", true),
                            WorkoutSet("s4", 4, 100.0, 5, "normal", true)
                        )
                    ),
                    WorkoutExercise(
                        exerciseId = "50000000-0000-0000-0000-000000000003",
                        exerciseName = "Dumbbell Lateral Raise",
                        primaryMuscleGroup = "shoulders",
                        equipment = "dumbbell",
                        sets = listOf(
                            WorkoutSet("s5", 1, 14.0, 12, "normal", true),
                            WorkoutSet("s6", 2, 14.0, 12, "normal", true),
                            WorkoutSet("s7", 3, 16.0, 10, "drop", true)
                        )
                    )
                )
            ),
            Workout(
                id = "hist-2",
                title = "Pull Day (Deadlift & Lats)",
                startTimeEpochMs = now - (dayMs * 3),
                endTimeEpochMs = now - (dayMs * 3) + 4200000,
                durationSeconds = 4200,
                totalVolumeKg = 11200.0,
                isFinished = true,
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "20000000-0000-0000-0000-000000000001",
                        exerciseName = "Deadlift",
                        primaryMuscleGroup = "back",
                        equipment = "barbell",
                        sets = listOf(
                            WorkoutSet("s8", 1, 100.0, 5, "warmup", true),
                            WorkoutSet("s9", 2, 140.0, 5, "normal", true),
                            WorkoutSet("s10", 3, 160.0, 4, "normal", true)
                        )
                    )
                )
            ),
            Workout(
                id = "hist-3",
                title = "Leg Day (Heavy Squats)",
                startTimeEpochMs = now - (dayMs * 5),
                endTimeEpochMs = now - (dayMs * 5) + 3900000,
                durationSeconds = 3900,
                totalVolumeKg = 9800.0,
                isFinished = true,
                exercises = listOf(
                    WorkoutExercise(
                        exerciseId = "30000000-0000-0000-0000-000000000001",
                        exerciseName = "Barbell Squat",
                        primaryMuscleGroup = "quads",
                        equipment = "barbell",
                        sets = listOf(
                            WorkoutSet("s11", 1, 80.0, 8, "warmup", true),
                            WorkoutSet("s12", 2, 120.0, 6, "normal", true),
                            WorkoutSet("s13", 3, 130.0, 5, "normal", true)
                        )
                    )
                )
            )
        )
    }

    private fun createInitialBodyMeasurements(): List<BodyMeasurement> {
        val now = PlatformBridge.currentTimeMillis()
        val dayMs = 86400000L
        return listOf(
            BodyMeasurement("b-1", now - (dayMs * 6), 80.5, 180.0, 24.8, 14.5),
            BodyMeasurement("b-2", now - (dayMs * 5), 80.2, 180.0, 24.7, 14.4),
            BodyMeasurement("b-3", now - (dayMs * 4), 80.0, 180.0, 24.7, 14.3),
            BodyMeasurement("b-4", now - (dayMs * 3), 79.8, 180.0, 24.6, 14.3),
            BodyMeasurement("b-5", now - (dayMs * 2), 80.1, 180.0, 24.7, 14.2),
            BodyMeasurement("b-6", now - (dayMs * 1), 79.9, 180.0, 24.7, 14.2),
            BodyMeasurement("b-7", now, 80.0, 180.0, 24.7, 14.1)
        )
    }
}
