package dev.neverpaid.health.domain.repository

import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferencesData(
    val useKilograms: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val serverUrl: String = "http://10.0.2.2:8080/api/v1",
    val isDevMode: Boolean = true
)

interface NeverPaidRepository {
    fun getExercises(): Flow<List<Exercise>>
    fun getExerciseById(id: String): Exercise?
    fun searchExercises(query: String, muscle: String?, equipment: String?): Flow<List<Exercise>>

    fun getActiveWorkout(): Flow<Workout?>
    fun startWorkout(title: String, routine: Routine? = null): Workout
    fun updateActiveWorkout(workout: Workout)
    fun finishActiveWorkout(): Workout?
    fun discardActiveWorkout()

    fun getWorkoutHistory(): Flow<List<Workout>>
    fun getRoutines(): List<Routine>
    fun getProgressSummary(): Flow<ProgressSummary>

    fun getCoachInsight(): Flow<CoachInsight>
    fun getCoachChat(): Flow<List<ChatMessage>>
    suspend fun sendCoachChatMessage(text: String): ChatMessage

    fun getBodyMeasurements(): Flow<List<BodyMeasurement>>
    suspend fun addBodyMeasurement(weightKg: Double, heightCm: Double, bodyFatPercent: Double?)

    fun getUserPreferences(): Flow<UserPreferencesData>
    suspend fun updatePreferences(preferences: UserPreferencesData)
}
