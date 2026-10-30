package dev.neverpaid.health.data.repository

import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
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
import dev.neverpaid.health.domain.model.ExerciseHistoryPointDto
import dev.neverpaid.health.domain.model.ExerciseHistoryResponseDto
import dev.neverpaid.health.domain.model.ExerciseRecordGroupDto
import dev.neverpaid.health.domain.model.FeedbackReport
import dev.neverpaid.health.domain.model.FocusTimerState
import dev.neverpaid.health.domain.model.MealAnalysisResult
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.PersonalRecord
import dev.neverpaid.health.domain.model.PlatformStats
import dev.neverpaid.health.domain.model.PreviousExerciseNote
import dev.neverpaid.health.domain.model.PreviousSetData
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.ProgramDayExercise
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.RecognizedFoodItem
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveBodyLogRequestDto
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class FakeNeverPaidRepository : NeverPaidRepository {

    private val authenticatedFlow = MutableStateFlow(true)
    private val currentUserFlow = MutableStateFlow<UserProfileDto?>(
        UserProfileDto(
            id = "user-1",
            email = "athlete@duda.uz",
            displayName = "Ferdavs",
            targetCalories = 2500,
            targetProteinG = 170,
            targetCarbsG = 280,
            targetFatG = 70
        )
    )
    private val preferencesFlow = MutableStateFlow(UserPreferencesData())
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(StandardExerciseCatalog.exercises)
    private val routinesFlow = MutableStateFlow<List<Routine>>(StandardExerciseCatalog.prebuiltRoutines)
    private val activeWorkoutFlow = MutableStateFlow<Workout?>(null)
    private val activeSheetOpenFlow = MutableStateFlow(false)
    private val lastFinishedWorkoutFlow = MutableStateFlow<Workout?>(null)
    private val workoutHistoryFlow = MutableStateFlow(
        listOf(
            Workout(
                id = "w-1",
                title = "Push Hypertrophy",
                startTimeEpochMs = 1730000000000L,
                endTimeEpochMs = 1730003600000L,
                durationSeconds = 3600,
                totalVolumeKg = 8400.0,
                isFinished = true,
                exercises = listOf(
                    WorkoutExercise(
                        id = "we-1",
                        exerciseId = "barbell-bench-press",
                        exercise = StandardExerciseCatalog.exercises.first(),
                        sets = listOf(
                            WorkoutSet(id = "s-1", setNumber = 1, weightKg = 100.0, reps = 8, isCompleted = true)
                        )
                    )
                )
            )
        )
    )
    private val recordGroupsFlow = MutableStateFlow(
        listOf(
            ExerciseRecordGroupDto(
                exerciseId = "barbell-bench-press",
                exerciseName = "Barbell Bench Press",
                bestWeightKg = 100.0,
                bestE1rmKg = 126.7,
                maxVolumeSetKg = 800.0,
                maxReps = 8
            )
        )
    )
    private val programsFlow = MutableStateFlow(
        listOf(
            TrainingProgram(
                id = "prog-1",
                name = "Classic Push Pull Legs",
                description = "6-day hypertrophy program",
                splitType = "ppl",
                difficulty = "intermediate",
                daysPerWeek = 6,
                isPublic = true,
                likesCount = 12,
                days = listOf(
                    ProgramDay(
                        id = "day-1",
                        dayNumber = 1,
                        name = "Push A",
                        exercises = listOf(
                            ProgramDayExercise(
                                id = "pde-1",
                                exerciseId = "barbell-bench-press",
                                exerciseName = "Barbell Bench Press",
                                targetSets = 4,
                                targetReps = "8-10"
                            )
                        )
                    )
                )
            )
        )
    )
    private val activeProgramBundleFlow = MutableStateFlow<ActiveProgramBundle?>(null)
    private val installedProgramsFlow = MutableStateFlow<List<ActiveProgramBundle>>(emptyList())
    private val commentsMap = mutableMapOf(
        "prog-1" to MutableStateFlow(
            listOf(
                ProgramComment(
                    id = "c-1",
                    programId = "prog-1",
                    authorName = "Aziz",
                    content = "Solid PPL routine!",
                    upvotes = 3,
                    createdAt = "2026-10-25"
                )
            )
        )
    )
    private val todosByDate = mutableMapOf<String, List<TodoTask>>()
    private val scheduleFlow = MutableStateFlow<DailyScheduleResponseDto?>(null)
    private val focusTimerFlow = MutableStateFlow(FocusTimerState())
    private val mealsByDate = mutableMapOf<String, MutableStateFlow<List<MealLog>>>()
    private val nutritionSummaryFlow = MutableStateFlow<DailyNutritionSummaryDto?>(
        DailyNutritionSummaryDto(
            date = "2026-10-30",
            totalCalories = 0,
            targetCalories = 2500,
            targetProteinG = 170.0,
            targetCarbsG = 280.0,
            targetFatG = 70.0,
            meals = emptyList()
        )
    )
    private val waterByDate = mutableMapOf<String, MutableStateFlow<Int>>()
    private val coachInsightFlow = MutableStateFlow(CoachInsight())
    private val coachChatFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val coachSuggestionsFlow = MutableStateFlow(listOf("Analyze my bench press"))
    private val bodyMeasurementsFlow = MutableStateFlow<List<BodyMeasurement>>(emptyList())
    private val bodyTrendFlow = MutableStateFlow<BodyTrendResponseDto?>(null)

    private fun ensureTodosForDate(date: String): List<TodoTask> {
        return todosByDate.getOrPut(date) {
            listOf(
                TodoTask(
                    id = "todo-1",
                    title = "Morning Mobility",
                    category = "workout",
                    priority = "high",
                    status = "pending",
                    scheduledDate = date,
                    isRecurring = true,
                    durationMinutes = 20
                )
            )
        }
    }

    override fun isUserAuthenticated(): Flow<Boolean> = authenticatedFlow.asStateFlow()
    override fun getCurrentUser(): Flow<UserProfileDto?> = currentUserFlow.asStateFlow()

    override suspend fun authenticateWithGoogleIdToken(idToken: String): AuthResponseDto {
        val user = currentUserFlow.value!!
        authenticatedFlow.value = true
        return AuthResponseDto(accessToken = "token-123", user = user)
    }

    override suspend fun authenticateWithAccessToken(accessToken: String, userJson: String?): UserProfileDto {
        authenticatedFlow.value = true
        return currentUserFlow.value!!
    }

    override suspend fun refreshCurrentUser(): UserProfileDto = currentUserFlow.value!!

    override suspend fun updateUserStatsLocal(updater: (UserProfileDto) -> UserProfileDto) {
        currentUserFlow.value = currentUserFlow.value?.let(updater)
    }

    override suspend fun updateUnitPreference(unit: String): UserProfileDto {
        val updated = currentUserFlow.value!!.copy(unitPreference = unit)
        currentUserFlow.value = updated
        return updated
    }

    override suspend fun login(email: String, password: String): AuthResponseDto {
        authenticatedFlow.value = true
        return AuthResponseDto(accessToken = "token-123", user = currentUserFlow.value!!)
    }

    override suspend fun register(email: String, password: String, name: String): AuthResponseDto {
        authenticatedFlow.value = true
        return AuthResponseDto(accessToken = "token-123", user = currentUserFlow.value!!)
    }

    override suspend fun logout() {
        authenticatedFlow.value = false
    }

    override fun getExercises(): Flow<List<Exercise>> = exercisesFlow.asStateFlow()
    override fun getExerciseById(id: String): Exercise? = exercisesFlow.value.find { it.id == id }

    override fun searchExercises(query: String, muscle: String?, equipment: String?): Flow<List<Exercise>> {
        return exercisesFlow.map { list ->
            list.filter { ex ->
                (query.isBlank() || ex.name.contains(query, ignoreCase = true)) &&
                    (muscle.isNullOrBlank() || muscle == "All" || ex.effectiveMuscle.equals(muscle, ignoreCase = true)) &&
                    (equipment.isNullOrBlank() || equipment == "All" || ex.equipment.equals(equipment, ignoreCase = true))
            }
        }
    }

    override suspend fun refreshExercises(search: String?, muscle: String?, equipment: String?): List<Exercise> =
        exercisesFlow.value

    override suspend fun createCustomExercise(
        name: String,
        primaryMuscleGroup: String,
        equipment: String,
        measurementType: String
    ): Exercise {
        val ex = Exercise(
            id = "custom-${exercisesFlow.value.size + 1}",
            name = name,
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType,
            isCustom = true
        )
        exercisesFlow.value = exercisesFlow.value + ex
        return ex
    }

    override suspend fun updateCustomExercise(
        exerciseId: String,
        name: String,
        primaryMuscleGroup: String,
        equipment: String,
        measurementType: String
    ): Exercise {
        val updated = Exercise(
            id = exerciseId,
            name = name,
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType,
            isCustom = true
        )
        exercisesFlow.value = exercisesFlow.value.map { if (it.id == exerciseId) updated else it }
        return updated
    }

    override suspend fun deleteCustomExercise(exerciseId: String) {
        exercisesFlow.value = exercisesFlow.value.filterNot { it.id == exerciseId }
    }

    override fun getRoutines(): List<Routine> = routinesFlow.value
    override fun getUserRoutinesFlow(): Flow<List<Routine>> = routinesFlow.asStateFlow()
    override fun getPrebuiltRoutines(): List<Routine> = StandardExerciseCatalog.prebuiltRoutines
    override suspend fun refreshRoutines(): List<Routine> = routinesFlow.value

    override suspend fun createRoutine(title: String, description: String, exercises: List<Exercise>): Routine {
        val routine = Routine(id = "r-${routinesFlow.value.size + 1}", name = title, notes = description)
        routinesFlow.value = routinesFlow.value + routine
        return routine
    }

    override suspend fun saveRoutineFull(
        routineId: String?,
        name: String,
        notes: String?,
        exercises: List<RoutineExercise>
    ): Routine {
        val routine = Routine(id = routineId ?: "r-new", name = name, notes = notes, exercises = exercises)
        routinesFlow.value = routinesFlow.value.filterNot { it.id == routine.id } + routine
        return routine
    }

    override suspend fun deleteRoutine(routineId: String) {
        routinesFlow.value = routinesFlow.value.filterNot { it.id == routineId }
    }

    override fun getActiveWorkout(): Flow<Workout?> = activeWorkoutFlow.asStateFlow()
    override fun isActiveWorkoutSheetOpen(): Flow<Boolean> = activeSheetOpenFlow.asStateFlow()
    override fun setActiveWorkoutSheetOpen(isOpen: Boolean) {
        activeSheetOpenFlow.value = isOpen
    }

    override fun getLastFinishedWorkoutSummary(): Flow<Workout?> = lastFinishedWorkoutFlow.asStateFlow()
    override fun dismissWorkoutSummary() {
        lastFinishedWorkoutFlow.value = null
    }

    override fun getPreviousSetsMap(): Flow<Map<String, List<PreviousSetData>>> = MutableStateFlow(emptyMap())
    override fun getPreviousNotesMap(): Flow<Map<String, PreviousExerciseNote>> = MutableStateFlow(emptyMap())

    override fun startWorkout(title: String, routine: Routine?): Workout {
        val workoutExercises = routine?.exercises?.mapIndexed { idx, re ->
            val catEx = getExerciseById(re.exerciseId) ?: Exercise(id = re.exerciseId, name = re.exerciseName)
            WorkoutExercise(
                id = "we-$idx",
                exerciseId = catEx.id,
                exercise = catEx,
                sets = (1..re.targetSets.coerceAtLeast(1)).map { sIdx ->
                    WorkoutSet(id = "set-$idx-$sIdx", setNumber = sIdx, weightKg = 60.0, reps = 10)
                }
            )
        } ?: emptyList()
        val workout = Workout(
            id = "active-1",
            title = title,
            startTimeEpochMs = 1730000000000L,
            exercises = workoutExercises
        )
        activeWorkoutFlow.value = workout
        return workout
    }

    override fun startWorkoutWithExercises(
        title: String,
        exercises: List<WorkoutExercise>,
        routineId: String?
    ): Workout {
        val workout = Workout(
            id = "active-1",
            title = title,
            startTimeEpochMs = 1730000000000L,
            exercises = exercises,
            routineId = routineId
        )
        activeWorkoutFlow.value = workout
        return workout
    }

    override fun updateActiveWorkout(workout: Workout) {
        activeWorkoutFlow.value = workout
    }

    override fun finishActiveWorkout(): Workout? {
        val current = activeWorkoutFlow.value ?: return null
        val finished = current.copy(isFinished = true, durationSeconds = 2400)
        activeWorkoutFlow.value = null
        lastFinishedWorkoutFlow.value = finished
        workoutHistoryFlow.value = listOf(finished) + workoutHistoryFlow.value
        return finished
    }

    override fun discardActiveWorkout() {
        activeWorkoutFlow.value = null
    }

    override fun getWorkoutHistory(): Flow<List<Workout>> = workoutHistoryFlow.asStateFlow()
    override suspend fun refreshWorkoutHistory(): List<Workout> = workoutHistoryFlow.value
    override suspend fun deleteWorkoutFromHistory(workoutId: String) {
        workoutHistoryFlow.value = workoutHistoryFlow.value.filterNot { it.id == workoutId }
    }

    override fun getProgressSummary(): Flow<ProgressSummary> {
        return workoutHistoryFlow.map { workouts ->
            ProgressSummary(
                totalWorkouts = workouts.size,
                totalVolumeKg = workouts.sumOf { it.totalVolume },
                totalReps = 40,
                currentStreakDays = 3,
                personalRecords = listOf(
                    PersonalRecord(
                        exerciseId = "barbell-bench-press",
                        exerciseName = "Barbell Bench Press",
                        weightKg = 100.0,
                        reps = 8,
                        e1rmKg = 126.7,
                        maxVolumeSetKg = 800.0
                    )
                )
            )
        }
    }

    override fun getRecordGroups(): Flow<List<ExerciseRecordGroupDto>> = recordGroupsFlow.asStateFlow()
    override suspend fun refreshProgressRecords(): List<ExerciseRecordGroupDto> = recordGroupsFlow.value
    override suspend fun getExerciseHistoryPoints(exerciseId: String): ExerciseHistoryResponseDto {
        return ExerciseHistoryResponseDto(
            exerciseId = exerciseId,
            exerciseName = "Barbell Bench Press",
            dataPoints = listOf(
                ExerciseHistoryPointDto(
                    workoutId = "w-1",
                    date = "2026-10-25",
                    bestWeightKg = 100.0,
                    bestE1rmKg = 126.7,
                    totalExerciseVolumeKg = 2400.0
                )
            )
        )
    }

    override fun getPrograms(): Flow<List<TrainingProgram>> = programsFlow.asStateFlow()
    override fun getActiveProgramBundle(): Flow<ActiveProgramBundle?> = activeProgramBundleFlow.asStateFlow()
    override fun getInstalledProgramsFlow(): Flow<List<ActiveProgramBundle>> = installedProgramsFlow.asStateFlow()
    override suspend fun refreshPrograms(tab: String, splitType: String?, search: String?): List<TrainingProgram> =
        programsFlow.value

    override suspend fun refreshActiveProgram(): ActiveProgramBundle? = activeProgramBundleFlow.value
    override suspend fun refreshInstalledPrograms(): List<ActiveProgramBundle> = installedProgramsFlow.value

    override suspend fun activateProgram(programId: String) {
        programsFlow.value = programsFlow.value.map { it.copy(isActive = it.id == programId) }
        val target = programsFlow.value.find { it.id == programId } ?: return
        val bundle = ActiveProgramBundle(
            userProgramId = "up-$programId",
            currentDayNumber = 1,
            isActive = true,
            program = target
        )
        activeProgramBundleFlow.value = bundle
        installedProgramsFlow.value = listOf(bundle)
    }

    override suspend fun setActiveUserProgram(userProgramId: String) {}

    override suspend fun saveProgram(programId: String?, request: SaveProgramRequestDto): TrainingProgram {
        val prog = TrainingProgram(
            id = programId ?: "prog-new",
            name = request.name,
            description = request.description,
            splitType = request.splitType,
            difficulty = request.difficulty,
            daysPerWeek = request.daysPerWeek,
            isPublic = request.isPublic
        )
        programsFlow.value = programsFlow.value.filterNot { it.id == prog.id } + prog
        return prog
    }

    override suspend fun deleteProgram(programId: String) {
        programsFlow.value = programsFlow.value.filterNot { it.id == programId }
    }

    override suspend fun publishProgram(programId: String, isPublic: Boolean) {}
    override suspend fun toggleLikeProgram(programId: String) {}

    override fun getProgramComments(programId: String): Flow<List<ProgramComment>> {
        return commentsMap.getOrPut(programId) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override suspend fun refreshProgramComments(programId: String): List<ProgramComment> {
        return commentsMap.getOrPut(programId) { MutableStateFlow(emptyList()) }.value
    }

    override suspend fun addProgramComment(
        programId: String,
        content: String,
        parentId: String?
    ): ProgramComment {
        val comment = ProgramComment(
            id = "c-${kotlin.random.Random.nextInt(1000)}",
            programId = programId,
            parentId = parentId,
            authorName = "Ferdavs",
            content = content,
            createdAt = "2026-10-30"
        )
        val flow = commentsMap.getOrPut(programId) { MutableStateFlow(emptyList()) }
        flow.value = flow.value + comment
        return comment
    }

    override suspend fun upvoteProgramComment(programId: String, commentId: String) {}
    override suspend fun deleteProgramComment(programId: String, commentId: String) {}
    override suspend fun getAthleteProfile(athleteId: String): AthleteProfile =
        AthleteProfile(id = athleteId, displayName = "Aziz")

    override suspend fun getPlatformStats(): PlatformStats = PlatformStats(totalWorkouts = 42, activeAthletes30d = 12)
    override suspend fun getFeedbackList(): List<FeedbackReport> = emptyList()
    override suspend fun submitFeedback(category: String, title: String, description: String): FeedbackReport =
        FeedbackReport(id = "fb-1", category = category, title = title, description = description)

    override suspend fun upvoteFeedback(feedbackId: String) {}

    override fun getTodos(date: String): Flow<List<TodoTask>> {
        val list = ensureTodosForDate(date)
        return MutableStateFlow(list)
    }

    override fun getDailyScheduleFlow(): Flow<DailyScheduleResponseDto?> = scheduleFlow.asStateFlow()

    override suspend fun fetchDailySchedule(date: String): DailyScheduleResponseDto {
        val tasks = ensureTodosForDate(date)
        val completed = tasks.count { it.isCompleted }
        val dto = DailyScheduleResponseDto(
            date = date,
            totalTasks = tasks.size,
            completedTasks = completed,
            totalPlannedMinutes = tasks.sumOf { it.durationMinutes },
            totalSpentMinutes = tasks.sumOf { it.spentMinutes },
            todos = tasks
        )
        scheduleFlow.value = dto
        return dto
    }

    override suspend fun fetchActivityLogs(date: String): List<ActivityLogItem> = emptyList()
    override suspend fun deleteActivityLog(logId: String, currentDate: String) {}

    override suspend fun addTodo(task: TodoTask) {
        val date = task.scheduledDate.ifBlank { "2026-10-30" }
        val existing = ensureTodosForDate(date)
        todosByDate[date] = existing + task
        fetchDailySchedule(date)
    }

    override suspend fun saveTodoFull(
        todoId: String?,
        request: SaveTodoRequestDto,
        refreshDate: String
    ): TodoTask {
        val task = TodoTask(
            id = todoId ?: "todo-${kotlin.random.Random.nextInt(10000)}",
            title = request.title,
            description = request.description,
            category = request.category,
            priority = request.priority,
            scheduledDate = request.scheduledDate,
            startTime = request.startTime,
            endTime = request.endTime,
            durationMinutes = request.durationMinutes,
            isRecurring = request.isRecurring,
            recurrencePattern = request.recurrencePattern
        )
        val existing = ensureTodosForDate(refreshDate).filterNot { it.id == task.id }
        todosByDate[refreshDate] = existing + task
        fetchDailySchedule(refreshDate)
        return task
    }

    override suspend fun toggleTodo(taskId: String, date: String) {
        val list = ensureTodosForDate(date).map { task ->
            if (task.id == taskId) {
                val willBeCompleted = !task.isCompletedOn(date)
                val nextDates = if (willBeCompleted) task.completedDates + date else task.completedDates - date
                val nextStatus = if (willBeCompleted) "completed" else "pending"
                task.copy(status = nextStatus, completedDates = nextDates)
            } else task
        }
        todosByDate[date] = list
        scheduleFlow.value = DailyScheduleResponseDto(date = date, todos = list)
    }

    override suspend fun deleteTodo(taskId: String) {
        todosByDate.keys.forEach { date ->
            todosByDate[date] = todosByDate[date].orEmpty().filterNot { it.id == taskId }
        }
    }

    override fun getFocusTimerState(): Flow<FocusTimerState> = focusTimerFlow.asStateFlow()

    override fun startFocusForTodo(todo: TodoTask, strategy: String, mode: String) {
        focusTimerFlow.value = FocusTimerState(
            isActive = true,
            isRunning = false,
            todoId = todo.id,
            taskTitle = todo.title,
            category = todo.category,
            mode = mode,
            splitStrategy = strategy,
            targetTotalMinutes = todo.durationMinutes,
            isModalOpen = true
        )
    }

    override fun startQuickFocusSession(
        title: String,
        category: String,
        targetMinutes: Int,
        strategy: String,
        mode: String
    ) {
        focusTimerFlow.value = FocusTimerState(
            isActive = true,
            isRunning = false,
            taskTitle = title,
            category = category,
            targetTotalMinutes = targetMinutes,
            splitStrategy = strategy,
            mode = mode,
            isModalOpen = true
        )
    }

    override fun pauseFocusTimer() {
        focusTimerFlow.value = focusTimerFlow.value.copy(isRunning = false)
    }

    override fun resumeFocusTimer() {
        focusTimerFlow.value = focusTimerFlow.value.copy(isRunning = true)
    }

    override fun tickFocusTimer() {}
    override fun skipFocusPhase() {}
    override fun resetFocusPhase() {}
    override fun setFocusMode(mode: String) {
        focusTimerFlow.value = focusTimerFlow.value.copy(mode = mode)
    }

    override fun setFocusSplitStrategy(strategy: String) {
        focusTimerFlow.value = focusTimerFlow.value.copy(splitStrategy = strategy)
    }

    override fun setFocusBreakMinutes(minutes: Int) {}
    override fun setFocusModalOpen(isOpen: Boolean) {
        focusTimerFlow.value = focusTimerFlow.value.copy(isModalOpen = isOpen)
    }

    override suspend fun finishAndLogFocusSession(markCompleted: Boolean, notes: String) {
        focusTimerFlow.value = FocusTimerState()
    }

    override fun discardFocusTimer() {
        focusTimerFlow.value = FocusTimerState()
    }

    override fun getMealLogs(date: String): Flow<List<MealLog>> {
        return mealsByDate.getOrPut(date) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    override fun getNutritionTodaySummary(): Flow<DailyNutritionSummaryDto?> = nutritionSummaryFlow.asStateFlow()

    override suspend fun refreshNutritionToday(): DailyNutritionSummaryDto {
        return nutritionSummaryFlow.value!!
    }

    override suspend fun addMealLog(meal: MealLog) {
        val dateFlow = mealsByDate.getOrPut(meal.date) { MutableStateFlow(emptyList()) }
        dateFlow.value = dateFlow.value + meal
        val current = nutritionSummaryFlow.value ?: DailyNutritionSummaryDto(date = meal.date)
        val updatedMeals = current.meals + meal
        nutritionSummaryFlow.value = current.copy(
            meals = updatedMeals,
            totalCalories = updatedMeals.sumOf { it.calories },
            totalProteinG = updatedMeals.sumOf { it.proteinG },
            totalCarbsG = updatedMeals.sumOf { it.carbsG },
            totalFatG = updatedMeals.sumOf { it.fatG }
        )
    }

    override suspend fun deleteMealLog(mealId: String) {
        mealsByDate.values.forEach { flow ->
            flow.value = flow.value.filterNot { it.id == mealId }
        }
        val current = nutritionSummaryFlow.value ?: return
        val updatedMeals = current.meals.filterNot { it.id == mealId }
        nutritionSummaryFlow.value = current.copy(
            meals = updatedMeals,
            totalCalories = updatedMeals.sumOf { it.calories },
            totalProteinG = updatedMeals.sumOf { it.proteinG },
            totalCarbsG = updatedMeals.sumOf { it.carbsG },
            totalFatG = updatedMeals.sumOf { it.fatG }
        )
    }

    override suspend fun analyzeFood(description: String): MealLog {
        return MealLog(
            id = "ai-1",
            name = description,
            calories = 420,
            proteinG = 32.0,
            carbsG = 40.0,
            fatG = 14.0
        )
    }

    override suspend fun estimateFoodTextFull(description: String): MealAnalysisResult {
        return MealAnalysisResult(
            mealName = description,
            totalCalories = 420,
            proteinG = 32.0,
            carbsG = 40.0,
            fatG = 14.0,
            items = listOf(
                RecognizedFoodItem(
                    name = description,
                    portion = "1 порция",
                    calories = 420,
                    proteinG = 32.0,
                    carbsG = 40.0,
                    fatG = 14.0
                )
            ),
            coachTip = "Balanced high-protein meal."
        )
    }

    override suspend fun analyzeFoodPhotoFull(
        imageBase64: String,
        mimeType: String,
        notes: String
    ): MealAnalysisResult {
        return estimateFoodTextFull(notes.ifBlank { "Photo Meal" })
    }

    override fun getWaterMl(date: String): Flow<Int> {
        return waterByDate.getOrPut(date) { MutableStateFlow(0) }.asStateFlow()
    }

    override suspend fun addWater(date: String, amountMl: Int) {
        val flow = waterByDate.getOrPut(date) { MutableStateFlow(0) }
        flow.value = (flow.value + amountMl).coerceAtLeast(0)
    }

    override fun getCoachInsight(): Flow<CoachInsight> = coachInsightFlow.asStateFlow()
    override fun getCoachChat(): Flow<List<ChatMessage>> = coachChatFlow.asStateFlow()
    override fun getCoachSuggestions(): Flow<List<String>> = coachSuggestionsFlow.asStateFlow()
    override suspend fun refreshCoachData() {}

    override suspend fun sendCoachChatMessage(text: String): ChatMessage {
        val reply = ChatMessage(
            id = "msg-2",
            role = "assistant",
            content = "Your Bench Press progressive overload looks strong!"
        )
        coachChatFlow.value = coachChatFlow.value + reply
        return reply
    }

    override suspend fun sendCoachChatWithImage(
        text: String,
        imageBase64: String?,
        mimeType: String?
    ): ChatMessage = sendCoachChatMessage(text)

    override suspend fun clearCoachHistory() {
        coachChatFlow.value = emptyList()
    }

    override fun getBodyMeasurements(): Flow<List<BodyMeasurement>> = bodyMeasurementsFlow.asStateFlow()
    override fun getBodyTrendFlow(): Flow<BodyTrendResponseDto?> = bodyTrendFlow.asStateFlow()
    override suspend fun refreshBodyData() {}
    override suspend fun addBodyMeasurement(weightKg: Double, heightCm: Double, bodyFatPercent: Double?) {}
    override suspend fun saveBodyLogFull(request: SaveBodyLogRequestDto): BodyMeasurement {
        return BodyMeasurement(logDate = request.logDate, weightKg = request.weightKg)
    }

    override suspend fun deleteBodyLog(date: String) {}

    override fun getUserPreferences(): Flow<UserPreferencesData> = preferencesFlow.asStateFlow()
    override suspend fun updatePreferences(preferences: UserPreferencesData) {
        preferencesFlow.value = preferences
    }
}
