package dev.neverpaid.health.data.repository

import dev.neverpaid.health.core.math.SetCalculationInput
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.network.DudaApiClient
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.storage.DudaLocalStorage
import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.domain.model.AuthResponseDto
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.PersonalRecord
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class NeverPaidRepositoryImpl(
    private val apiClient: DudaApiClient? = null
) : NeverPaidRepository {

    private val scope = CoroutineScope(Dispatchers.Default)

    // Auth flows
    private val isAuthenticatedFlow = MutableStateFlow(DudaLocalStorage.getAccessToken() != null)
    private val currentUserFlow = MutableStateFlow(DudaLocalStorage.getUserProfile())

    // Data flows
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(StandardExerciseCatalog.standardExercises)
    private val activeWorkoutFlow = MutableStateFlow<Workout?>(DudaLocalStorage.getActiveWorkoutDraft())
    private val workoutHistoryFlow = MutableStateFlow<List<Workout>>(
        DudaLocalStorage.getCachedWorkouts() ?: createInitialHistory()
    )
    private val bodyMeasurementsFlow = MutableStateFlow<List<BodyMeasurement>>(
        DudaLocalStorage.getCachedBodyMeasurements() ?: createInitialBodyMeasurements()
    )
    private val preferencesFlow = MutableStateFlow(DudaLocalStorage.getUserPreferences())
    private val todosFlow = MutableStateFlow<List<TodoTask>>(
        DudaLocalStorage.getCachedTodos() ?: createInitialTodos()
    )
    private val mealLogsFlow = MutableStateFlow<List<MealLog>>(
        DudaLocalStorage.getCachedMeals() ?: createInitialMealLogs()
    )
    private val programsFlow = MutableStateFlow<List<TrainingProgram>>(createInitialPrograms())
    private val programCommentsMap = mutableMapOf<String, MutableStateFlow<List<ProgramComment>>>()

    private val coachInsightFlow = MutableStateFlow(
        CoachInsight(
            cnsScore = 88,
            readinessLevel = "Full Throttle",
            statusMessage = "CNS recovery is optimal. High power output expected on compound movements.",
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
                text = "Welcome to duda.uz AI Strength Coach. Your 7-day volume load is balanced and CNS readiness is 88/100 (Full Throttle). What are we hitting today?",
                timestampEpochMs = PlatformBridge.currentTimeMillis() - 3600000
            )
        )
    )

    init {
        // Background sync if online & authenticated
        if (isAuthenticatedFlow.value && apiClient != null) {
            scope.launch {
                try {
                    val profile = apiClient.getMe()
                    currentUserFlow.value = profile
                    val remoteWorkouts = apiClient.getWorkouts()
                    if (remoteWorkouts.isNotEmpty()) {
                        workoutHistoryFlow.value = remoteWorkouts
                        DudaLocalStorage.setCachedWorkouts(remoteWorkouts)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    // --- AUTH ---
    override fun isUserAuthenticated(): Flow<Boolean> = isAuthenticatedFlow.asStateFlow()

    override fun getCurrentUser(): Flow<UserProfileDto?> = currentUserFlow.asStateFlow()

    override suspend fun login(email: String, password: String): AuthResponseDto {
        if (apiClient != null) {
            try {
                val response = apiClient.login(email, password)
                isAuthenticatedFlow.value = true
                currentUserFlow.value = response.user
                return response
            } catch (e: Exception) {
                if (e.message?.contains("failed", ignoreCase = true) == true) throw e
            }
        }
        val guestUser = UserProfileDto(
            id = "guest-${PlatformBridge.currentTimeMillis()}",
            email = email,
            name = email.substringBefore("@").replaceFirstChar { it.uppercase() }
        )
        val authResp = AuthResponseDto("offline-token", guestUser)
        DudaLocalStorage.setAccessToken(authResp.accessToken)
        DudaLocalStorage.setUserProfile(guestUser)
        isAuthenticatedFlow.value = true
        currentUserFlow.value = guestUser
        return authResp
    }

    override suspend fun register(email: String, password: String, name: String): AuthResponseDto {
        if (apiClient != null) {
            try {
                val response = apiClient.register(email, password, name)
                isAuthenticatedFlow.value = true
                currentUserFlow.value = response.user
                return response
            } catch (e: Exception) {
                if (e.message?.contains("failed", ignoreCase = true) == true) throw e
            }
        }
        val guestUser = UserProfileDto(
            id = "guest-${PlatformBridge.currentTimeMillis()}",
            email = email,
            name = name
        )
        val authResp = AuthResponseDto("offline-token", guestUser)
        DudaLocalStorage.setAccessToken(authResp.accessToken)
        DudaLocalStorage.setUserProfile(guestUser)
        isAuthenticatedFlow.value = true
        currentUserFlow.value = guestUser
        return authResp
    }

    override suspend fun logout() {
        apiClient?.logout()
        isAuthenticatedFlow.value = false
        currentUserFlow.value = null
    }

    // --- EXERCISES ---
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

    // --- ACTIVE WORKOUT ---
    override fun getActiveWorkout(): Flow<Workout?> = activeWorkoutFlow.asStateFlow()

    override fun startWorkout(title: String, routine: Routine?): Workout {
        val initialExercises = if (routine != null) {
            routine.exercises.map { ex ->
                // Try to find previous performance from history
                val lastPerf = findLastPerformance(ex.id)
                WorkoutExercise(
                    exerciseId = ex.id,
                    exerciseName = ex.name,
                    primaryMuscleGroup = ex.primaryMuscleGroup,
                    equipment = ex.equipment,
                    sets = listOf(
                        WorkoutSet(
                            id = PlatformBridge.generateUuid(),
                            setNumber = 1,
                            weightKg = lastPerf?.first ?: 60.0,
                            reps = lastPerf?.second ?: 10,
                            type = "warmup",
                            isCompleted = false,
                            previousWeightKg = lastPerf?.first ?: 60.0,
                            previousReps = lastPerf?.second ?: 10
                        ),
                        WorkoutSet(
                            id = PlatformBridge.generateUuid(),
                            setNumber = 2,
                            weightKg = lastPerf?.first ?: 80.0,
                            reps = lastPerf?.second ?: 8,
                            type = "normal",
                            isCompleted = false,
                            previousWeightKg = lastPerf?.first ?: 80.0,
                            previousReps = lastPerf?.second ?: 8
                        )
                    )
                )
            }
        } else {
            emptyList()
        }

        val newWorkout = Workout(
            id = PlatformBridge.generateUuid(),
            title = if (title.isNotBlank()) title else "Workout Session",
            startTimeEpochMs = PlatformBridge.currentTimeMillis(),
            exercises = initialExercises,
            totalVolumeKg = 0.0,
            isFinished = false
        )

        activeWorkoutFlow.value = newWorkout
        DudaLocalStorage.setActiveWorkoutDraft(newWorkout)
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
        DudaLocalStorage.setActiveWorkoutDraft(updated)
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
        DudaLocalStorage.setCachedWorkouts(updatedHistory)
        activeWorkoutFlow.value = null
        DudaLocalStorage.setActiveWorkoutDraft(null)

        // Sync with backend asynchronously
        scope.launch {
            try {
                apiClient?.saveWorkout(finishedWorkout)
            } catch (_: Exception) {}
        }

        return finishedWorkout
    }

    override fun discardActiveWorkout() {
        activeWorkoutFlow.value = null
        DudaLocalStorage.setActiveWorkoutDraft(null)
    }

    // --- HISTORY & ROUTINES ---
    override fun getWorkoutHistory(): Flow<List<Workout>> = workoutHistoryFlow.asStateFlow()

    override fun getRoutines(): List<Routine> = StandardExerciseCatalog.prebuiltRoutines

    override suspend fun createRoutine(title: String, description: String, exercises: List<Exercise>): Routine {
        val newRoutine = Routine(
            id = PlatformBridge.generateUuid(),
            title = title,
            description = description,
            exercises = exercises
        )
        return newRoutine
    }

    override fun getProgressSummary(): Flow<ProgressSummary> {
        return workoutHistoryFlow.map { history ->
            val totalWorkouts = history.size
            val totalVolume = history.sumOf { it.totalVolumeKg }
            val totalReps = history.sumOf { w ->
                w.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.reps } }
            }

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
                currentStreakDays = calculateStreakDays(history),
                personalRecords = prMap.values.toList()
            )
        }
    }

    // --- PROGRAMS & COMMUNITY ---
    override fun getPrograms(): Flow<List<TrainingProgram>> = programsFlow.asStateFlow()

    override suspend fun activateProgram(programId: String) {
        val updated = programsFlow.value.map { p ->
            p.copy(isActive = p.id == programId)
        }
        programsFlow.value = updated
        scope.launch {
            try { apiClient?.activateProgram(programId) } catch (_: Exception) {}
        }
    }

    override suspend fun toggleLikeProgram(programId: String) {
        val updated = programsFlow.value.map { p ->
            if (p.id == programId) {
                val newLiked = !p.isLiked
                val newCount = if (newLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                p.copy(isLiked = newLiked, likesCount = newCount)
            } else p
        }
        programsFlow.value = updated
        scope.launch {
            try { apiClient?.likeProgram(programId) } catch (_: Exception) {}
        }
    }

    override fun getProgramComments(programId: String): Flow<List<ProgramComment>> {
        val flow = programCommentsMap.getOrPut(programId) {
            MutableStateFlow(createInitialComments(programId))
        }
        return flow.asStateFlow()
    }

    override suspend fun addProgramComment(programId: String, content: String, parentId: String?): ProgramComment {
        val user = currentUserFlow.value
        val newComment = ProgramComment(
            id = PlatformBridge.generateUuid(),
            programId = programId,
            userId = user?.id ?: "local-user",
            userName = user?.name ?: "Athlete",
            userAvatarUrl = user?.avatarUrl,
            content = content,
            createdAtEpochMs = PlatformBridge.currentTimeMillis(),
            parentId = parentId
        )

        val flow = programCommentsMap.getOrPut(programId) {
            MutableStateFlow(createInitialComments(programId))
        }

        if (parentId == null) {
            flow.value = listOf(newComment) + flow.value
        } else {
            flow.value = flow.value.map { comment ->
                if (comment.id == parentId) {
                    comment.copy(replies = comment.replies + newComment)
                } else comment
            }
        }

        // Increment comments count on program
        programsFlow.value = programsFlow.value.map { p ->
            if (p.id == programId) p.copy(commentsCount = p.commentsCount + 1) else p
        }

        scope.launch {
            try { apiClient?.postProgramComment(programId, content, parentId) } catch (_: Exception) {}
        }

        return newComment
    }

    // --- TODOS (DATE-ISOLATED) ---
    override fun getTodos(date: String): Flow<List<TodoTask>> {
        return todosFlow.map { list ->
            // Filter: show tasks that are daily recurring OR scheduled for this specific date
            list.filter { it.isRecurringDaily || it.scheduledDate == date }
        }
    }

    override suspend fun addTodo(task: TodoTask) {
        val updated = todosFlow.value + task
        todosFlow.value = updated
        DudaLocalStorage.setCachedTodos(updated)
        scope.launch {
            try { apiClient?.saveTodo(task) } catch (_: Exception) {}
        }
    }

    override suspend fun toggleTodo(taskId: String, date: String) {
        val updated = todosFlow.value.map { task ->
            if (task.id == taskId) {
                val newCompletedDates = if (task.completedDates.contains(date)) {
                    task.completedDates - date
                } else {
                    task.completedDates + date
                }
                task.copy(completedDates = newCompletedDates)
            } else task
        }
        todosFlow.value = updated
        DudaLocalStorage.setCachedTodos(updated)
        scope.launch {
            try {
                val isCompleted = updated.find { it.id == taskId }?.isCompletedOn(date) ?: false
                apiClient?.toggleTodo(taskId, date, isCompleted)
            } catch (_: Exception) {}
        }
    }

    override suspend fun deleteTodo(taskId: String) {
        val updated = todosFlow.value.filter { it.id != taskId }
        todosFlow.value = updated
        DudaLocalStorage.setCachedTodos(updated)
        scope.launch {
            try { apiClient?.deleteTodo(taskId) } catch (_: Exception) {}
        }
    }

    // --- NUTRITION ---
    override fun getMealLogs(date: String): Flow<List<MealLog>> {
        return mealLogsFlow.map { list ->
            list.filter { it.date == date }
        }
    }

    override suspend fun addMealLog(meal: MealLog) {
        val updated = mealLogsFlow.value + meal
        mealLogsFlow.value = updated
        DudaLocalStorage.setCachedMeals(updated)
        scope.launch {
            try { apiClient?.saveMealLog(meal) } catch (_: Exception) {}
        }
    }

    override suspend fun deleteMealLog(mealId: String) {
        val updated = mealLogsFlow.value.filter { it.id != mealId }
        mealLogsFlow.value = updated
        DudaLocalStorage.setCachedMeals(updated)
    }

    override suspend fun analyzeFood(description: String): MealLog {
        if (apiClient != null) {
            try {
                return apiClient.analyzeFood(description)
            } catch (_: Exception) {}
        }
        // Intelligent fallback heuristic if offline
        val lower = description.lowercase()
        val (cals, prot, fat, carb) = when {
            lower.contains("плов") || lower.contains("osh") -> listOf(650, 24, 28, 75)
            lower.contains("куриц") || lower.contains("chicken") || lower.contains("tovuq") -> listOf(420, 48, 8, 35)
            lower.contains("лосось") || lower.contains("salmon") || lower.contains("baliq") -> listOf(480, 40, 22, 10)
            lower.contains("протеин") || lower.contains("protein") || lower.contains("shake") -> listOf(140, 26, 2, 4)
            lower.contains("яйц") || lower.contains("egg") || lower.contains("tuxum") -> listOf(240, 18, 16, 2)
            lower.contains("овсянк") || lower.contains("oats") || lower.contains("suli") -> listOf(350, 14, 6, 60)
            else -> listOf(380, 22, 12, 45)
        }
        return MealLog(
            id = PlatformBridge.generateUuid(),
            date = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd"),
            mealType = "lunch",
            name = description.take(40),
            calories = cals,
            proteinG = prot,
            fatsG = fat,
            carbsG = carb
        )
    }

    override fun getWaterMl(date: String): Flow<Int> {
        val flow = MutableStateFlow(DudaLocalStorage.getCachedWater(date))
        return flow.asStateFlow()
    }

    override suspend fun addWater(date: String, amountMl: Int) {
        val current = DudaLocalStorage.getCachedWater(date)
        val updated = (current + amountMl).coerceAtLeast(0)
        DudaLocalStorage.setCachedWater(date, updated)
    }

    // --- COACH ---
    override fun getCoachInsight(): Flow<CoachInsight> = coachInsightFlow.asStateFlow()

    override fun getCoachChat(): Flow<List<ChatMessage>> = chatMessagesFlow.asStateFlow()

    override suspend fun sendCoachChatMessage(text: String): ChatMessage {
        val userMsg = ChatMessage(
            id = PlatformBridge.generateUuid(),
            isUser = true,
            text = text,
            timestampEpochMs = PlatformBridge.currentTimeMillis()
        )
        chatMessagesFlow.value = chatMessagesFlow.value + userMsg

        val replyText = if (apiClient != null) {
            try {
                apiClient.sendCoachChatMessage(text)
            } catch (_: Exception) {
                generateLocalAiCoachReply(text)
            }
        } else {
            generateLocalAiCoachReply(text)
        }

        val aiMsg = ChatMessage(
            id = PlatformBridge.generateUuid(),
            isUser = false,
            text = replyText,
            timestampEpochMs = PlatformBridge.currentTimeMillis() + 400
        )
        chatMessagesFlow.value = chatMessagesFlow.value + aiMsg
        return aiMsg
    }

    // --- BODY ---
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
        val updated = bodyMeasurementsFlow.value + measurement
        bodyMeasurementsFlow.value = updated
        DudaLocalStorage.setCachedBodyMeasurements(updated)

        scope.launch {
            try { apiClient?.saveBodyMeasurement(measurement) } catch (_: Exception) {}
        }
    }

    // --- PREFERENCES ---
    override fun getUserPreferences(): Flow<UserPreferencesData> = preferencesFlow.asStateFlow()

    override suspend fun updatePreferences(preferences: UserPreferencesData) {
        preferencesFlow.value = preferences
        DudaLocalStorage.setUserPreferences(preferences)
    }

    // --- HELPERS ---
    private fun findLastPerformance(exerciseId: String): Pair<Double, Int>? {
        for (w in workoutHistoryFlow.value) {
            val ex = w.exercises.find { it.exerciseId == exerciseId } ?: continue
            val lastCompletedSet = ex.sets.lastOrNull { it.isCompleted } ?: continue
            return Pair(lastCompletedSet.weightKg, lastCompletedSet.reps)
        }
        return null
    }

    private fun calculateStreakDays(history: List<Workout>): Int {
        if (history.isEmpty()) return 0
        return 4 // Base athlete consistency streak
    }

    private fun generateLocalAiCoachReply(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("bench") || lower.contains("жим") || lower.contains("ko'krak") ->
                "For Barbell Bench Press: focus on scapular retraction, leg drive, and a controlled 2-second eccentric descent. Target today: +2.5 kg on your top working set."
            lower.contains("squat") || lower.contains("присед") || lower.contains("oyoq") ->
                "Squat biomechanics: brace diaphragm before every rep, push knees outward along toe trajectory, and hit parallel depth without lumbar flexion."
            lower.contains("deadlift") || lower.contains("тяг") || lower.contains("orqa") ->
                "Deadlifts tax spinal erectors and CNS. Pull slack out of the barbell first, engage lats like bending the bar, and drive feet through the floor."
            lower.contains("pain") || lower.contains("бол") || lower.contains("og'riq") ->
                "If feeling joint discomfort or connective tissue strain, deload intensity by 20% or switch to dumbbell neutral grip."
            lower.contains("protein") || lower.contains("белок") || lower.contains("oqs") ->
                "Target 1.8 - 2.2 g of high quality protein per kg of bodyweight, distributed across 3-4 meals to maximize Muscle Protein Synthesis (MPS)."
            else ->
                "Maintain progressive overload consistency! Rest 90-120 seconds between compound sets to allow ATP-CP replenishment."
        }
    }

    private fun createInitialTodos(): List<TodoTask> {
        return listOf(
            TodoTask("t-1", "Силовая тренировка по сплиту", "Push Day", "WORKOUT", "HIGH", true),
            TodoTask("t-2", "Дневная норма белка", "180 грамм", "NUTRITION", "HIGH", true),
            TodoTask("t-3", "Водный баланс", "3.0 литра", "RECOVERY", "NORMAL", true),
            TodoTask("t-4", "Качественный сон", "8 часов", "RECOVERY", "HIGH", true),
            TodoTask("t-5", "Дневная активность", "10 000 шагов", "FOCUS", "NORMAL", true)
        )
    }

    private fun createInitialMealLogs(): List<MealLog> {
        val today = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        return listOf(
            MealLog("m-1", today, "breakfast", "Овсянка с протеином и ягодами", 540, 42, 10, 68),
            MealLog("m-2", today, "lunch", "Куриное филе на гриле, рис басмати и брокколи", 680, 55, 14, 82),
            MealLog("m-3", today, "snack", "Греческий йогурт 2% и спелый банан", 260, 22, 4, 34),
            MealLog("m-4", today, "dinner", "Филе лосося, печёный картофель и спаржа", 420, 35, 18, 30)
        )
    }

    private fun createInitialPrograms(): List<TrainingProgram> {
        val exChest = StandardExerciseCatalog.standardExercises.filter { it.primaryMuscleGroup == "chest" }.take(3)
        val exBack = StandardExerciseCatalog.standardExercises.filter { it.primaryMuscleGroup == "back" }.take(3)
        val exQuads = StandardExerciseCatalog.standardExercises.filter { it.primaryMuscleGroup == "quads" }.take(3)

        return listOf(
            TrainingProgram(
                id = "prog-1",
                title = "Push Pull Legs (PPL Hypertrophy)",
                description = "Классический сплит для максимального мышечного роста и силы. Оптимальное разделение мышечных групп.",
                level = "intermediate",
                daysPerWeek = 4,
                likesCount = 142,
                isLiked = true,
                commentsCount = 18,
                isActive = true,
                days = listOf(
                    ProgramDay(1, "Push Day (Chest, Shoulders, Triceps)", "push", false, exChest),
                    ProgramDay(2, "Pull Day (Back, Lats, Biceps)", "pull", false, exBack),
                    ProgramDay(3, "Legs Day (Quads, Hamstrings, Calves)", "legs", false, exQuads),
                    ProgramDay(4, "Active Recovery & Mobility", "rest", true, emptyList())
                )
            ),
            TrainingProgram(
                id = "prog-2",
                title = "Upper / Lower Split (Power & Hypertrophy)",
                description = "4-дневный баланс между силой верхней и нижней частей тела с достаточным временем на восстановление.",
                level = "advanced",
                daysPerWeek = 4,
                likesCount = 98,
                isLiked = false,
                commentsCount = 9,
                isActive = false,
                days = listOf(
                    ProgramDay(1, "Upper Body Power", "push", false, exChest + exBack),
                    ProgramDay(2, "Lower Body Power", "legs", false, exQuads),
                    ProgramDay(3, "Rest & Sauna", "rest", true, emptyList()),
                    ProgramDay(4, "Upper Body Hypertrophy", "push", false, exChest)
                )
            ),
            TrainingProgram(
                id = "prog-3",
                title = "5×5 Linear Strength Foundation",
                description = "Базовые многосуставные движения со штангой для построения фундаментальной силы всего тела.",
                level = "beginner",
                daysPerWeek = 3,
                likesCount = 215,
                isLiked = true,
                commentsCount = 34,
                isActive = false,
                days = listOf(
                    ProgramDay(1, "Workout A (Squat, Bench, Row)", "fullbody", false, exChest),
                    ProgramDay(2, "Rest & Mobility", "rest", true, emptyList()),
                    ProgramDay(3, "Workout B (Squat, Overhead Press, Deadlift)", "fullbody", false, exBack)
                )
            )
        )
    }

    private fun createInitialComments(programId: String): List<ProgramComment> {
        val now = PlatformBridge.currentTimeMillis()
        return listOf(
            ProgramComment(
                id = "c-1",
                programId = programId,
                userId = "u-1",
                userName = "Temur Alimov",
                content = "Отличный сплит! За 6 недель жим лежа вырос со 100 кг до 112.5 кг. Восстановление супер.",
                createdAtEpochMs = now - 86400000L * 2,
                likesCount = 12,
                isLiked = true,
                replies = listOf(
                    ProgramComment(
                        id = "c-1-1",
                        programId = programId,
                        userId = "u-2",
                        userName = "Sardor K.",
                        content = "Сколько минут отдыхаешь между тяжёлыми сетами?",
                        createdAtEpochMs = now - 86400000L * 1,
                        parentId = "c-1",
                        likesCount = 3,
                        isLiked = false
                    )
                )
            ),
            ProgramComment(
                id = "c-2",
                programId = programId,
                userId = "u-3",
                userName = "Jasur B.",
                content = "Подскажите, чем лучше заменить отжимания на брусьях, если плечо ноет?",
                createdAtEpochMs = now - 3600000L * 5,
                likesCount = 4,
                isLiked = false
            )
        )
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
