package dev.neverpaid.health.domain.repository

import dev.neverpaid.health.domain.model.ActiveProgramBundle
import dev.neverpaid.health.domain.model.ActivityLogItem
import dev.neverpaid.health.domain.model.AthleteProfile
import dev.neverpaid.health.domain.model.AuthResponseDto
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.BodyTrendResponseDto
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.DailyNutritionSummaryDto
import dev.neverpaid.health.domain.model.DailyScheduleResponseDto
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ExerciseHistoryResponseDto
import dev.neverpaid.health.domain.model.ExerciseRecordGroupDto
import dev.neverpaid.health.domain.model.FeedbackReport
import dev.neverpaid.health.domain.model.FocusTimerState
import dev.neverpaid.health.domain.model.MealAnalysisResult
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.PlatformStats
import dev.neverpaid.health.domain.model.PreviousExerciseNote
import dev.neverpaid.health.domain.model.PreviousSetData
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveBodyLogRequestDto
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

@Serializable
data class UserPreferencesData(
    val useKilograms: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val serverUrl: String = "https://duda.uz/api/v1",
    val isDevMode: Boolean = false,
    val languageCode: String = "ru",
    val calorieTarget: Int = 2500,
    val proteinTargetG: Int = 175,
    val carbsTargetG: Int = 250,
    val fatsTargetG: Int = 75,
    val waterGoalMl: Int = 2500,
    val targetWeightKg: Double = 75.0,
    val heightCm: Double = 178.0,
    val age: Int = 26,
    val gender: String = "male",
    val activityLevel: String = "moderate",
    val goalType: String = "maintain"
) {
    val proteinTarget: Int get() = proteinTargetG
    val carbsTarget: Int get() = carbsTargetG
    val fatsTarget: Int get() = fatsTargetG
}

typealias DudaRepository = NeverPaidRepository

interface NeverPaidRepository {
    // --- 1. AUTH & USER ---
    fun isUserAuthenticated(): Flow<Boolean>
    fun getCurrentUser(): Flow<UserProfileDto?>
    suspend fun authenticateWithGoogleIdToken(idToken: String): AuthResponseDto
    suspend fun authenticateWithAccessToken(accessToken: String, userJson: String? = null): UserProfileDto
    suspend fun refreshCurrentUser(): UserProfileDto
    suspend fun updateUserStatsLocal(updater: (UserProfileDto) -> UserProfileDto)
    suspend fun updateUnitPreference(unit: String): UserProfileDto
    suspend fun login(email: String, password: String): AuthResponseDto
    suspend fun register(email: String, password: String, name: String): AuthResponseDto
    suspend fun logout()

    // --- 2. EXERCISES ---
    fun getExercises(): Flow<List<Exercise>>
    fun getExerciseById(id: String): Exercise?
    fun searchExercises(query: String, muscle: String?, equipment: String?): Flow<List<Exercise>>
    suspend fun refreshExercises(search: String? = null, muscle: String? = null, equipment: String? = null): List<Exercise>
    suspend fun createCustomExercise(
        name: String,
        primaryMuscleGroup: String,
        equipment: String,
        measurementType: String = "weight_reps"
    ): Exercise
    suspend fun updateCustomExercise(
        exerciseId: String,
        name: String,
        primaryMuscleGroup: String,
        equipment: String,
        measurementType: String = "weight_reps"
    ): Exercise
    suspend fun deleteCustomExercise(exerciseId: String)

    // --- 3. ROUTINES ---
    fun getRoutines(): List<Routine>
    fun getUserRoutinesFlow(): Flow<List<Routine>>
    fun getPrebuiltRoutines(): List<Routine>
    suspend fun refreshRoutines(): List<Routine>
    suspend fun createRoutine(title: String, description: String, exercises: List<Exercise>): Routine
    suspend fun saveRoutineFull(
        routineId: String? = null,
        name: String,
        notes: String?,
        exercises: List<RoutineExercise>
    ): Routine
    suspend fun deleteRoutine(routineId: String)

    // --- 4. ACTIVE WORKOUT & PREVIOUS SETS ---
    fun getActiveWorkout(): Flow<Workout?>
    fun isActiveWorkoutSheetOpen(): Flow<Boolean>
    fun setActiveWorkoutSheetOpen(isOpen: Boolean)
    fun getLastFinishedWorkoutSummary(): Flow<Workout?>
    fun dismissWorkoutSummary()
    fun getPreviousSetsMap(): Flow<Map<String, List<PreviousSetData>>>
    fun getPreviousNotesMap(): Flow<Map<String, PreviousExerciseNote>>
    fun startWorkout(title: String, routine: Routine? = null): Workout
    fun startWorkoutWithExercises(title: String, exercises: List<WorkoutExercise>, routineId: String? = null): Workout
    fun updateActiveWorkout(workout: Workout)
    fun finishActiveWorkout(): Workout?
    fun discardActiveWorkout()

    // --- 5. WORKOUT HISTORY & PROGRESS ---
    fun getWorkoutHistory(): Flow<List<Workout>>
    suspend fun refreshWorkoutHistory(): List<Workout>
    suspend fun deleteWorkoutFromHistory(workoutId: String)
    fun getProgressSummary(): Flow<ProgressSummary>
    fun getRecordGroups(): Flow<List<ExerciseRecordGroupDto>>
    suspend fun refreshProgressRecords(): List<ExerciseRecordGroupDto>
    suspend fun getExerciseHistoryPoints(exerciseId: String): ExerciseHistoryResponseDto

    // --- 6. PROGRAMS, SPLITS & COMMUNITY ---
    fun getPrograms(): Flow<List<TrainingProgram>>
    fun getActiveProgramBundle(): Flow<ActiveProgramBundle?>
    fun getInstalledProgramsFlow(): Flow<List<ActiveProgramBundle>>
    suspend fun refreshPrograms(tab: String = "library", splitType: String? = null, search: String? = null): List<TrainingProgram>
    suspend fun refreshActiveProgram(): ActiveProgramBundle?
    suspend fun refreshInstalledPrograms(): List<ActiveProgramBundle>
    suspend fun activateProgram(programId: String)
    suspend fun setActiveUserProgram(userProgramId: String)
    suspend fun saveProgram(programId: String? = null, request: SaveProgramRequestDto): TrainingProgram
    suspend fun deleteProgram(programId: String)
    suspend fun publishProgram(programId: String, isPublic: Boolean)
    suspend fun toggleLikeProgram(programId: String)
    fun getProgramComments(programId: String): Flow<List<ProgramComment>>
    suspend fun refreshProgramComments(programId: String): List<ProgramComment>
    suspend fun addProgramComment(programId: String, content: String, parentId: String? = null): ProgramComment
    suspend fun upvoteProgramComment(programId: String, commentId: String)
    suspend fun deleteProgramComment(programId: String, commentId: String)
    suspend fun getAthleteProfile(athleteId: String): AthleteProfile
    suspend fun getPlatformStats(): PlatformStats
    suspend fun getFeedbackList(): List<FeedbackReport>
    suspend fun submitFeedback(category: String, title: String, description: String): FeedbackReport
    suspend fun upvoteFeedback(feedbackId: String)

    // --- 7. TODOS, CALENDAR & FOCUS TIMER ---
    fun getTodos(date: String): Flow<List<TodoTask>>
    fun getDailyScheduleFlow(): Flow<DailyScheduleResponseDto?>
    suspend fun fetchDailySchedule(date: String): DailyScheduleResponseDto
    suspend fun fetchActivityLogs(date: String): List<ActivityLogItem>
    suspend fun deleteActivityLog(logId: String, currentDate: String)
    suspend fun addTodo(task: TodoTask)
    suspend fun saveTodoFull(todoId: String? = null, request: SaveTodoRequestDto, refreshDate: String): TodoTask
    suspend fun toggleTodo(taskId: String, date: String)
    suspend fun deleteTodo(taskId: String)

    // Focus Timer Global Store
    fun getFocusTimerState(): Flow<FocusTimerState>
    fun startFocusForTodo(todo: TodoTask, strategy: String = "25m", mode: String = "pomodoro")
    fun startQuickFocusSession(title: String, category: String = "work", targetMinutes: Int = 25, strategy: String = "25m", mode: String = "pomodoro")
    fun pauseFocusTimer()
    fun resumeFocusTimer()
    fun tickFocusTimer()
    fun skipFocusPhase()
    fun resetFocusPhase()
    fun setFocusMode(mode: String)
    fun setFocusSplitStrategy(strategy: String)
    fun setFocusBreakMinutes(minutes: Int)
    fun setFocusModalOpen(isOpen: Boolean)
    suspend fun finishAndLogFocusSession(markCompleted: Boolean = true, notes: String = "")
    fun discardFocusTimer()

    // --- 8. NUTRITION & AI FOOD ANALYZER ---
    fun getMealLogs(date: String): Flow<List<MealLog>>
    fun getNutritionTodaySummary(): Flow<DailyNutritionSummaryDto?>
    suspend fun refreshNutritionToday(): DailyNutritionSummaryDto
    suspend fun addMealLog(meal: MealLog)
    suspend fun deleteMealLog(mealId: String)
    suspend fun analyzeFood(description: String): MealLog
    suspend fun estimateFoodTextFull(description: String): MealAnalysisResult
    suspend fun analyzeFoodPhotoFull(imageBase64: String, mimeType: String = "image/jpeg", notes: String = ""): MealAnalysisResult
    fun getWaterMl(date: String): Flow<Int>
    suspend fun addWater(date: String, amountMl: Int)

    // --- 9. AI COACH ---
    fun getCoachInsight(): Flow<CoachInsight>
    fun getCoachChat(): Flow<List<ChatMessage>>
    fun getCoachSuggestions(): Flow<List<String>>
    suspend fun refreshCoachData()
    suspend fun sendCoachChatMessage(text: String): ChatMessage
    suspend fun sendCoachChatWithImage(text: String, imageBase64: String?, mimeType: String?): ChatMessage
    suspend fun clearCoachHistory()

    // --- 10. BODY & BMI ---
    fun getBodyMeasurements(): Flow<List<BodyMeasurement>>
    fun getBodyTrendFlow(): Flow<BodyTrendResponseDto?>
    suspend fun refreshBodyData()
    suspend fun addBodyMeasurement(weightKg: Double, heightCm: Double, bodyFatPercent: Double?)
    suspend fun saveBodyLogFull(request: SaveBodyLogRequestDto): BodyMeasurement
    suspend fun deleteBodyLog(date: String)

    // --- 11. PREFERENCES ---
    fun getUserPreferences(): Flow<UserPreferencesData>
    suspend fun updatePreferences(preferences: UserPreferencesData)

    // --- 12. CONVENIENCE DELEGATES FOR VIEWMODELS ---
    fun observeRoutines(): Flow<List<Routine>> = getUserRoutinesFlow()
    fun observeActiveProgram(): Flow<ActiveProgramBundle?> = getActiveProgramBundle()
    suspend fun getActiveProgram(): ActiveProgramBundle? = refreshActiveProgram()
    suspend fun refreshWorkouts(): List<Workout> = refreshWorkoutHistory()
    suspend fun createRoutine(request: SaveRoutineRequestDto): Routine =
        saveRoutineFull(null, request.name, request.notes, request.exercises)
    suspend fun updateRoutine(routineId: String, request: SaveRoutineRequestDto): Routine =
        saveRoutineFull(routineId, request.name, request.notes, request.exercises)
    fun startWorkoutFromProgramDay(program: TrainingProgram, day: ProgramDay): Workout {
        val exercises = day.exercises.mapIndexed { idx, ex ->
            val catalogEx = getExerciseById(ex.exerciseId)
            WorkoutExercise(
                instanceId = "prog-${day.dayNumber}-$idx",
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseName,
                measurementType = catalogEx?.measurementType ?: "weight_reps",
                primaryMuscleGroup = catalogEx?.effectiveMuscle ?: "chest",
                equipment = catalogEx?.equipment ?: "barbell",
                orderIndex = idx,
                sets = (1..ex.targetSets.coerceAtLeast(1)).map { sIdx ->
                    WorkoutSet(
                        id = "set-$idx-$sIdx",
                        setNumber = sIdx,
                        weightKg = 0.0,
                        reps = ex.targetRepsMax.coerceAtLeast(8)
                    )
                }
            )
        }
        return startWorkoutWithExercises(
            title = "${program.name} — ${day.name}",
            exercises = exercises,
            routineId = program.id
        )
    }
    suspend fun advanceActiveProgramDay(): ActiveProgramBundle? = refreshActiveProgram()
    suspend fun deactivateProgram() {}
    suspend fun toggleUpvoteProgramComment(programId: String, commentId: String) =
        upvoteProgramComment(programId, commentId)
    suspend fun updateProgram(programId: String, request: SaveProgramRequestDto): TrainingProgram =
        saveProgram(programId, request)
    suspend fun createProgram(request: SaveProgramRequestDto): TrainingProgram =
        saveProgram(null, request)
    suspend fun getActivityLogs(date: String = ""): List<ActivityLogItem> = fetchActivityLogs(date)
    fun startFocusTimerForTask(task: TodoTask) = startFocusForTodo(task)
    fun observeAllTodos(): Flow<List<TodoTask>> = getDailyScheduleFlow().map { it?.todos.orEmpty() }
    fun observeActivityLogs(): Flow<List<ActivityLogItem>> = getDailyScheduleFlow().map { it?.recentLogs.orEmpty() }
    suspend fun refreshTodos(date: String = ""): DailyScheduleResponseDto = fetchDailySchedule(date)
    suspend fun updateTodo(todoId: String, request: SaveTodoRequestDto): TodoTask =
        saveTodoFull(todoId, request, request.scheduledDate)
    suspend fun refreshDailyNutrition(date: String): DailyNutritionSummaryDto = refreshNutritionToday()
    suspend fun analyzeFoodPhoto(imageBase64: String, notes: String = ""): MealAnalysisResult =
        analyzeFoodPhotoFull(imageBase64, "image/jpeg", notes)
    suspend fun deleteMealLog(mealId: String, date: String) = deleteMealLog(mealId)
    suspend fun clearCoachChatHistory() = clearCoachHistory()
    suspend fun refreshCoachInsights() = refreshCoachData()
    suspend fun refreshCoachMessages() = refreshCoachData()
    suspend fun sendCoachChatMessage(text: String, imageBase64: String?): ChatMessage =
        if (imageBase64.isNullOrBlank()) sendCoachChatMessage(text)
        else sendCoachChatWithImage(text, imageBase64, "image/jpeg")
    suspend fun saveBodyMeasurementFull(request: SaveBodyLogRequestDto): BodyMeasurement =
        saveBodyLogFull(request)
    suspend fun deleteBodyMeasurement(date: String) = deleteBodyLog(date)
    suspend fun updateProfilePreferences(preferences: UserPreferencesData) = updatePreferences(preferences)
    suspend fun refreshBodyMeasurements() = refreshBodyData()
    suspend fun getBodyTrend(days: Int = 30): BodyTrendResponseDto? {
        refreshBodyData()
        return getBodyTrendFlow().first()
    }
    suspend fun getPreviousPerformance(exerciseId: String): PreviousPerformanceItem? {
        val sets = getPreviousSetsMap().first()[exerciseId].orEmpty()
        val note = getPreviousNotesMap().first()[exerciseId]?.note.orEmpty()
        if (sets.isEmpty() && note.isBlank()) return null
        return PreviousPerformanceItem(sets = sets, notes = note)
    }
    suspend fun getPreviousPerformance(exerciseIds: List<String>): Map<String, List<PreviousSetData>> =
        getPreviousSetsMap().first().filterKeys { it in exerciseIds }
}

data class PreviousPerformanceItem(
    val sets: List<PreviousSetData> = emptyList(),
    val notes: String = ""
)
