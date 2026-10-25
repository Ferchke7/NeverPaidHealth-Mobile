package dev.neverpaid.health.domain.repository

import dev.neverpaid.health.domain.model.AuthResponseDto
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.Workout
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferencesData(
    val useKilograms: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val serverUrl: String = "https://duda.uz/api/v1",
    val isDevMode: Boolean = false,
    val languageCode: String = "ru"
)

typealias DudaRepository = NeverPaidRepository

interface NeverPaidRepository {
    // --- AUTH & USER ---
    fun isUserAuthenticated(): Flow<Boolean>
    fun getCurrentUser(): Flow<UserProfileDto?>
    suspend fun login(email: String, password: String): AuthResponseDto
    suspend fun register(email: String, password: String, name: String): AuthResponseDto
    suspend fun logout()

    // --- EXERCISES ---
    fun getExercises(): Flow<List<Exercise>>
    fun getExerciseById(id: String): Exercise?
    fun searchExercises(query: String, muscle: String?, equipment: String?): Flow<List<Exercise>>

    // --- ACTIVE WORKOUT ---
    fun getActiveWorkout(): Flow<Workout?>
    fun startWorkout(title: String, routine: Routine? = null): Workout
    fun updateActiveWorkout(workout: Workout)
    fun finishActiveWorkout(): Workout?
    fun discardActiveWorkout()

    // --- HISTORY & ROUTINES ---
    fun getWorkoutHistory(): Flow<List<Workout>>
    fun getRoutines(): List<Routine>
    suspend fun createRoutine(title: String, description: String, exercises: List<Exercise>): Routine
    fun getProgressSummary(): Flow<ProgressSummary>

    // --- PROGRAMS & COMMUNITY ---
    fun getPrograms(): Flow<List<TrainingProgram>>
    suspend fun activateProgram(programId: String)
    suspend fun toggleLikeProgram(programId: String)
    fun getProgramComments(programId: String): Flow<List<ProgramComment>>
    suspend fun addProgramComment(programId: String, content: String, parentId: String? = null): ProgramComment

    // --- TODOS (DATE-ISOLATED) ---
    fun getTodos(date: String): Flow<List<TodoTask>>
    suspend fun addTodo(task: TodoTask)
    suspend fun toggleTodo(taskId: String, date: String)
    suspend fun deleteTodo(taskId: String)

    // --- NUTRITION ---
    fun getMealLogs(date: String): Flow<List<MealLog>>
    suspend fun addMealLog(meal: MealLog)
    suspend fun deleteMealLog(mealId: String)
    suspend fun analyzeFood(description: String): MealLog
    fun getWaterMl(date: String): Flow<Int>
    suspend fun addWater(date: String, amountMl: Int)

    // --- COACH ---
    fun getCoachInsight(): Flow<CoachInsight>
    fun getCoachChat(): Flow<List<ChatMessage>>
    suspend fun sendCoachChatMessage(text: String): ChatMessage

    // --- BODY ---
    fun getBodyMeasurements(): Flow<List<BodyMeasurement>>
    suspend fun addBodyMeasurement(weightKg: Double, heightCm: Double, bodyFatPercent: Double?)

    // --- PREFERENCES ---
    fun getUserPreferences(): Flow<UserPreferencesData>
    suspend fun updatePreferences(preferences: UserPreferencesData)
}
