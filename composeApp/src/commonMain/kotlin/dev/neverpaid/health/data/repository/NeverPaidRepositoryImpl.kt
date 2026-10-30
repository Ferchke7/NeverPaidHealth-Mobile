package dev.neverpaid.health.data.repository

import dev.neverpaid.health.core.math.SetCalculationInput
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.network.DudaApiClient
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.storage.DudaLocalStorage
import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.domain.model.ActiveProgramBundle
import dev.neverpaid.health.domain.model.ActivityLogItem
import dev.neverpaid.health.domain.model.AthleteProfile
import dev.neverpaid.health.domain.model.AuthResponseDto
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.BodyTrendResponseDto
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachChatHistoryItemDto
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.CreateFeedbackRequestDto
import dev.neverpaid.health.domain.model.DailyNutritionSummaryDto
import dev.neverpaid.health.domain.model.DailyScheduleResponseDto
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ExerciseHistoryResponseDto
import dev.neverpaid.health.domain.model.ExerciseRecordGroupDto
import dev.neverpaid.health.domain.model.FeedbackReport
import dev.neverpaid.health.domain.model.FinishExercisePayloadDto
import dev.neverpaid.health.domain.model.FinishSetPayloadDto
import dev.neverpaid.health.domain.model.FinishWorkoutRequestDto
import dev.neverpaid.health.domain.model.FocusTimerState
import dev.neverpaid.health.domain.model.LogFocusSessionRequestDto
import dev.neverpaid.health.domain.model.MealAnalysisResult
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.PersonalRecord
import dev.neverpaid.health.domain.model.PlatformStats
import dev.neverpaid.health.domain.model.PreviousExerciseNote
import dev.neverpaid.health.domain.model.PreviousSetData
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveBodyLogRequestDto
import dev.neverpaid.health.domain.model.SaveExerciseRequestDto
import dev.neverpaid.health.domain.model.SaveMealRequestDto
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.StartWorkoutRequestDto
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class NeverPaidRepositoryImpl(
    private val apiClient: DudaApiClient? = null
) : NeverPaidRepository {

    private val scope = CoroutineScope(Dispatchers.Default)
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // --- Auth & User ---
    private val isAuthenticatedFlow = MutableStateFlow(DudaLocalStorage.getAccessToken() != null)
    private val currentUserFlow = MutableStateFlow(DudaLocalStorage.getUserProfile())
    private val preferencesFlow = MutableStateFlow(DudaLocalStorage.getUserPreferences())

    // --- Live API State Flows (No fake data in production!) ---
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(
        if (apiClient == null) StandardExerciseCatalog.standardExercises else emptyList()
    )
    private val userRoutinesFlow = MutableStateFlow<List<Routine>>(emptyList())
    private val activeWorkoutFlow = MutableStateFlow<Workout?>(DudaLocalStorage.getActiveWorkoutDraft())
    private val isActiveWorkoutSheetOpenFlow = MutableStateFlow(false)
    private val lastFinishedWorkoutSummaryFlow = MutableStateFlow<Workout?>(null)
    private val previousSetsMapFlow = MutableStateFlow<Map<String, List<PreviousSetData>>>(emptyMap())
    private val previousNotesMapFlow = MutableStateFlow<Map<String, PreviousExerciseNote>>(emptyMap())

    private val workoutHistoryFlow = MutableStateFlow<List<Workout>>(emptyList())
    private val recordGroupsFlow = MutableStateFlow<List<ExerciseRecordGroupDto>>(emptyList())

    private val programsFlow = MutableStateFlow<List<TrainingProgram>>(emptyList())
    private val activeProgramBundleFlow = MutableStateFlow<ActiveProgramBundle?>(null)
    private val installedProgramsFlow = MutableStateFlow<List<ActiveProgramBundle>>(emptyList())
    private val programCommentsMap = mutableMapOf<String, MutableStateFlow<List<ProgramComment>>>()

    private val dailyScheduleFlow = MutableStateFlow<DailyScheduleResponseDto?>(null)
    private val todosMapByDate = mutableMapOf<String, MutableStateFlow<List<TodoTask>>>()
    private val focusTimerFlow = MutableStateFlow(DudaLocalStorage.getFocusTimerState())

    private val nutritionSummaryFlow = MutableStateFlow<DailyNutritionSummaryDto?>(null)
    private val mealLogsFlow = MutableStateFlow<List<MealLog>>(emptyList())
    private val waterFlowsMap = mutableMapOf<String, MutableStateFlow<Int>>()

    private val coachInsightFlow = MutableStateFlow(CoachInsight())
    private val chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val coachSuggestionsFlow = MutableStateFlow<List<String>>(emptyList())

    private val bodyMeasurementsFlow = MutableStateFlow<List<BodyMeasurement>>(emptyList())
    private val bodyTrendFlow = MutableStateFlow<BodyTrendResponseDto?>(null)

    init {
        apiClient?.onUnauthorized = {
            isAuthenticatedFlow.value = false
            currentUserFlow.value = null
        }

        if (isAuthenticatedFlow.value && apiClient != null) {
            scope.launch {
                try {
                    val me = apiClient.getMe()
                    currentUserFlow.value = me
                    val currentPrefs = preferencesFlow.value
                    val useKg = me.unitPreference.lowercase() != "lb"
                    if (currentPrefs.useKilograms != useKg) {
                        updatePreferences(currentPrefs.copy(useKilograms = useKg))
                    }
                    refreshInitialAuthenticatedData()
                } catch (_: Exception) {
                    if (DudaLocalStorage.getAccessToken() == null) {
                        isAuthenticatedFlow.value = false
                        currentUserFlow.value = null
                    }
                }
            }
        }
    }

    private suspend fun refreshInitialAuthenticatedData() {
        val client = apiClient ?: return
        try {
            exercisesFlow.value = client.getExercises()
        } catch (_: Exception) {}
        try {
            userRoutinesFlow.value = client.getRoutines()
        } catch (_: Exception) {}
        try {
            activeProgramBundleFlow.value = client.getActiveProgram()
        } catch (_: Exception) {}
        try {
            workoutHistoryFlow.value = client.getWorkouts(limit = 50)
        } catch (_: Exception) {}
        try {
            recordGroupsFlow.value = client.getProgressRecords()
        } catch (_: Exception) {}
    }

    // ========================================================================
    // 1. AUTH & USER
    // ========================================================================

    override fun isUserAuthenticated(): Flow<Boolean> = isAuthenticatedFlow.asStateFlow()

    override fun getCurrentUser(): Flow<UserProfileDto?> = currentUserFlow.asStateFlow()

    override suspend fun authenticateWithGoogleIdToken(idToken: String): AuthResponseDto {
        val client = apiClient ?: throw IllegalStateException("API client not available")
        val response = client.authenticateGoogle(idToken)
        isAuthenticatedFlow.value = true
        currentUserFlow.value = response.user
        scope.launch { refreshInitialAuthenticatedData() }
        return response
    }

    override suspend fun authenticateWithAccessToken(accessToken: String, userJson: String?): UserProfileDto {
        // Check if user pasted a Google ID Token (3-part JWT with accounts.google.com or Google OAuth) vs np_access_token
        DudaLocalStorage.setAccessToken(accessToken)
        if (!userJson.isNullOrBlank()) {
            try {
                val parsed = json.decodeFromString<UserProfileDto>(userJson)
                DudaLocalStorage.setUserProfile(parsed)
                currentUserFlow.value = parsed
            } catch (_: Exception) {}
        }

        val client = apiClient
        if (client != null) {
            return try {
                val profile = client.getMe()
                isAuthenticatedFlow.value = true
                currentUserFlow.value = profile
                scope.launch { refreshInitialAuthenticatedData() }
                profile
            } catch (_: Exception) {
                // If /me rejected the token, try exchanging it via POST /auth/google in case it is a Google id_token
                val authResp = client.authenticateGoogle(accessToken)
                isAuthenticatedFlow.value = true
                currentUserFlow.value = authResp.user
                scope.launch { refreshInitialAuthenticatedData() }
                authResp.user
            }
        } else {
            val fallback = currentUserFlow.value ?: UserProfileDto(id = "user-1", email = "athlete@duda.uz", displayName = "Athlete")
            isAuthenticatedFlow.value = true
            currentUserFlow.value = fallback
            return fallback
        }
    }

    override suspend fun refreshCurrentUser(): UserProfileDto {
        val client = apiClient ?: return currentUserFlow.value ?: UserProfileDto()
        val me = client.getMe()
        currentUserFlow.value = me
        isAuthenticatedFlow.value = true
        return me
    }

    override suspend fun updateUserStatsLocal(updater: (UserProfileDto) -> UserProfileDto) {
        val existing = currentUserFlow.value ?: UserProfileDto()
        val updated = updater(existing)
        currentUserFlow.value = updated
        DudaLocalStorage.setUserProfile(updated)
    }

    override suspend fun updateUnitPreference(unit: String): UserProfileDto {
        val client = apiClient
        val updated = if (client != null) {
            client.updateUnitPreference(unit)
        } else {
            (currentUserFlow.value ?: UserProfileDto()).copy(unitPreference = unit)
        }
        currentUserFlow.value = updated
        val prefs = preferencesFlow.value.copy(useKilograms = unit.lowercase() != "lb")
        updatePreferences(prefs)
        return updated
    }

    override suspend fun login(email: String, password: String): AuthResponseDto {
        // Used if token is passed in password/email or for unit tests
        if (apiClient != null) {
            val user = authenticateWithAccessToken(password.ifBlank { email })
            return AuthResponseDto(accessToken = DudaLocalStorage.getAccessToken().orEmpty(), user = user)
        }
        val user = UserProfileDto(id = "u-1", email = email, displayName = email.substringBefore("@"))
        val resp = AuthResponseDto("test-token", 2592000, user)
        isAuthenticatedFlow.value = true
        currentUserFlow.value = user
        return resp
    }

    override suspend fun register(email: String, password: String, name: String): AuthResponseDto {
        return login(email, password)
    }

    override suspend fun logout() {
        apiClient?.logout()
        DudaLocalStorage.clearAuthSession()
        isAuthenticatedFlow.value = false
        currentUserFlow.value = null
        activeWorkoutFlow.value = null
        isActiveWorkoutSheetOpenFlow.value = false
    }

    // ========================================================================
    // 2. EXERCISES
    // ========================================================================

    override fun getExercises(): Flow<List<Exercise>> = exercisesFlow.asStateFlow()

    override fun getExerciseById(id: String): Exercise? {
        return exercisesFlow.value.find { it.id == id }
            ?: StandardExerciseCatalog.standardExercises.find { it.id == id }
    }

    override fun searchExercises(query: String, muscle: String?, equipment: String?): Flow<List<Exercise>> {
        return exercisesFlow.map { list ->
            list.filter { item ->
                val itemMuscle = item.effectiveMuscle.lowercase()
                val itemEquip = item.equipment.lowercase()
                val q = query.trim().lowercase()
                val matchesQuery = q.isEmpty() ||
                    item.name.lowercase().contains(q) ||
                    itemMuscle.contains(q) ||
                    itemEquip.contains(q)
                val targetMuscle = muscle?.lowercase()
                val matchesMuscle = targetMuscle.isNullOrBlank() || targetMuscle == "all" ||
                    itemMuscle == targetMuscle ||
                    (targetMuscle == "quads" && (itemMuscle == "quads" || itemMuscle == "legs")) ||
                    (targetMuscle == "hamstrings" && (itemMuscle == "hamstrings" || itemMuscle == "glutes"))
                val targetEquip = equipment?.lowercase()
                val matchesEquip = targetEquip.isNullOrBlank() || targetEquip == "all" || itemEquip == targetEquip
                matchesQuery && matchesMuscle && matchesEquip
            }
        }
    }

    override suspend fun refreshExercises(search: String?, muscle: String?, equipment: String?): List<Exercise> {
        val client = apiClient ?: return exercisesFlow.value
        val remote = client.getExercises(search = search, muscleGroup = muscle, equipment = equipment)
        if (search.isNullOrBlank() && (muscle == null || muscle == "all") && (equipment == null || equipment == "all")) {
            exercisesFlow.value = remote
        }
        return remote
    }

    override suspend fun createCustomExercise(
        name: String,
        primaryMuscleGroup: String,
        equipment: String,
        measurementType: String
    ): Exercise {
        val req = SaveExerciseRequestDto(
            name = name.trim(),
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType
        )
        val created = apiClient?.createExercise(req) ?: Exercise(
            id = PlatformBridge.generateUuid(),
            name = name.trim(),
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType,
            isCustom = true
        )
        exercisesFlow.value = listOf(created) + exercisesFlow.value
        return created
    }

    override suspend fun updateCustomExercise(
        exerciseId: String,
        name: String,
        primaryMuscleGroup: String,
        equipment: String,
        measurementType: String
    ): Exercise {
        val req = SaveExerciseRequestDto(
            name = name.trim(),
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType
        )
        val updated = apiClient?.updateExercise(exerciseId, req) ?: Exercise(
            id = exerciseId,
            name = name.trim(),
            primaryMuscleGroup = primaryMuscleGroup,
            equipment = equipment,
            measurementType = measurementType,
            isCustom = true
        )
        exercisesFlow.value = exercisesFlow.value.map { if (it.id == exerciseId) updated else it }
        return updated
    }

    override suspend fun deleteCustomExercise(exerciseId: String) {
        apiClient?.deleteExercise(exerciseId)
        exercisesFlow.value = exercisesFlow.value.filter { it.id != exerciseId }
    }

    // ========================================================================
    // 3. ROUTINES
    // ========================================================================

    override fun getRoutines(): List<Routine> {
        return userRoutinesFlow.value + StandardExerciseCatalog.prebuiltRoutines
    }

    override fun getUserRoutinesFlow(): Flow<List<Routine>> = userRoutinesFlow.asStateFlow()

    override fun getPrebuiltRoutines(): List<Routine> = StandardExerciseCatalog.prebuiltRoutines

    override suspend fun refreshRoutines(): List<Routine> {
        val client = apiClient ?: return userRoutinesFlow.value
        val list = client.getRoutines()
        userRoutinesFlow.value = list
        return list
    }

    override suspend fun createRoutine(title: String, description: String, exercises: List<Exercise>): Routine {
        val routineExercises = exercises.mapIndexed { index, ex ->
            RoutineExercise(
                exerciseId = ex.id,
                exerciseName = ex.name,
                orderIndex = index,
                targetSets = 3,
                targetRepsMin = 8,
                targetRepsMax = 12
            )
        }
        return saveRoutineFull(routineId = null, name = title, notes = description, exercises = routineExercises)
    }

    override suspend fun saveRoutineFull(
        routineId: String?,
        name: String,
        notes: String?,
        exercises: List<RoutineExercise>
    ): Routine {
        val req = SaveRoutineRequestDto(
            name = name.trim(),
            notes = notes?.trim()?.takeIf { it.isNotEmpty() },
            exercises = exercises.mapIndexed { idx, ex -> ex.copy(orderIndex = idx) }
        )
        val saved = if (apiClient != null) {
            if (!routineId.isNullOrBlank() && !routineId.startsWith("prebuilt-")) {
                apiClient.updateRoutine(routineId, req)
            } else {
                apiClient.createRoutine(req)
            }
        } else {
            Routine(
                id = routineId ?: PlatformBridge.generateUuid(),
                name = req.name,
                notes = req.notes,
                exercises = req.exercises
            )
        }
        val exists = userRoutinesFlow.value.any { it.id == saved.id }
        userRoutinesFlow.value = if (exists) {
            userRoutinesFlow.value.map { if (it.id == saved.id) saved else it }
        } else {
            listOf(saved) + userRoutinesFlow.value
        }
        return saved
    }

    override suspend fun deleteRoutine(routineId: String) {
        apiClient?.deleteRoutine(routineId)
        userRoutinesFlow.value = userRoutinesFlow.value.filter { it.id != routineId }
    }

    // ========================================================================
    // 4. ACTIVE WORKOUT & PREVIOUS SETS
    // ========================================================================

    override fun getActiveWorkout(): Flow<Workout?> = activeWorkoutFlow.asStateFlow()

    override fun isActiveWorkoutSheetOpen(): Flow<Boolean> = isActiveWorkoutSheetOpenFlow.asStateFlow()

    override fun setActiveWorkoutSheetOpen(isOpen: Boolean) {
        isActiveWorkoutSheetOpenFlow.value = isOpen
    }

    override fun getLastFinishedWorkoutSummary(): Flow<Workout?> = lastFinishedWorkoutSummaryFlow.asStateFlow()

    override fun dismissWorkoutSummary() {
        lastFinishedWorkoutSummaryFlow.value = null
    }

    override fun getPreviousSetsMap(): Flow<Map<String, List<PreviousSetData>>> = previousSetsMapFlow.asStateFlow()

    override fun getPreviousNotesMap(): Flow<Map<String, PreviousExerciseNote>> = previousNotesMapFlow.asStateFlow()

    override fun startWorkout(title: String, routine: Routine?): Workout {
        val exercises = routine?.exercises?.mapIndexed { idx, rEx ->
            val setsCount = rEx.targetSets.coerceAtLeast(1)
            val defaultReps = rEx.targetRepsMax ?: rEx.targetRepsMin ?: 10
            WorkoutExercise(
                instanceId = PlatformBridge.generateUuid(),
                exerciseId = rEx.exerciseId,
                exerciseName = rEx.exerciseName,
                orderIndex = idx,
                sets = (1..setsCount).map { sNum ->
                    WorkoutSet(
                        id = PlatformBridge.generateUuid(),
                        setNumber = sNum,
                        setType = "normal",
                        weightKg = 0.0,
                        reps = defaultReps,
                        isCompleted = false
                    )
                }
            )
        } ?: emptyList()

        return startWorkoutWithExercises(
            title = title.ifBlank { routine?.name ?: "Силовая тренировка" },
            exercises = exercises,
            routineId = routine?.id?.takeIf { !it.startsWith("prebuilt-") }
        )
    }

    override fun startWorkoutWithExercises(
        title: String,
        exercises: List<WorkoutExercise>,
        routineId: String?
    ): Workout {
        val workoutId = PlatformBridge.generateUuid()
        val nowMs = PlatformBridge.currentTimeMillis()
        val newWorkout = Workout(
            id = workoutId,
            routineId = routineId,
            name = title.ifBlank { "Силовая тренировка" },
            status = "in_progress",
            startedAt = PlatformBridge.formatDate(nowMs, "yyyy-MM-dd'T'HH:mm:ss'Z'"),
            startTimeEpochMs = nowMs,
            exercises = exercises.mapIndexed { idx, ex ->
                ex.copy(
                    instanceId = ex.instanceId.ifBlank { PlatformBridge.generateUuid() },
                    orderIndex = idx
                )
            },
            totalVolumeKg = 0.0,
            isFinished = false
        )

        activeWorkoutFlow.value = newWorkout
        isActiveWorkoutSheetOpenFlow.value = true
        DudaLocalStorage.setActiveWorkoutDraft(newWorkout)

        // Sync start with backend & fetch previous sets/notes (matching useStartWorkout.ts)
        if (apiClient != null) {
            scope.launch {
                try {
                    apiClient.startWorkoutRemote(
                        StartWorkoutRequestDto(
                            id = workoutId,
                            routineId = routineId,
                            name = newWorkout.name,
                            replaceActive = true
                        )
                    )
                } catch (_: Exception) {}

                fetchPreviousForActiveWorkout(newWorkout)
            }
        }

        return newWorkout
    }

    private suspend fun fetchPreviousForActiveWorkout(workout: Workout) {
        val client = apiClient ?: return
        if (workout.exercises.isEmpty()) return
        try {
            val ids = workout.exercises.map { it.exerciseId }.distinct()
            val names = workout.exercises.map { it.exerciseName }.distinct()
            val prevResp = client.getPreviousPerformance(ids, names)
            previousSetsMapFlow.value = prevResp.items
            previousNotesMapFlow.value = prevResp.notes

            val current = activeWorkoutFlow.value ?: return
            if (current.id != workout.id) return

            val enrichedExercises = current.exercises.map { ex ->
                val prevSets = prevResp.items[ex.exerciseId]
                    ?: prevResp.items[ex.exerciseName]
                    ?: prevResp.items[ex.exerciseName.lowercase()]
                    ?: emptyList()
                val updatedSets = ex.sets.mapIndexed { idx, set ->
                    val matchPrev = prevSets.find { it.setNumber == set.setNumber }
                        ?: prevSets.getOrNull(idx)
                        ?: prevSets.lastOrNull()
                    if (matchPrev != null) {
                        set.copy(
                            weightKg = if (set.weightKg == 0.0 && !set.isCompleted) matchPrev.weightKg else set.weightKg,
                            reps = if (!set.isCompleted && matchPrev.reps > 0) matchPrev.reps else set.reps,
                            previousWeightKg = matchPrev.weightKg,
                            previousReps = matchPrev.reps
                        )
                    } else {
                        set
                    }
                }
                ex.copy(sets = updatedSets)
            }
            val updatedWorkout = current.copy(exercises = enrichedExercises)
            activeWorkoutFlow.value = updatedWorkout
            DudaLocalStorage.setActiveWorkoutDraft(updatedWorkout)
        } catch (_: Exception) {}
    }

    override fun updateActiveWorkout(workout: Workout) {
        val allSets = workout.exercises.flatMap { it.sets }
        val calcInputs = allSets.map {
            SetCalculationInput(
                weightKg = it.weightKg,
                reps = it.reps,
                type = it.setType,
                completed = it.isCompleted && it.setType != "warmup"
            )
        }
        val computedVolume = StrengthCalculator.calculateVolume(calcInputs)
        val completedCount = allSets.count { it.isCompleted }
        val updated = workout.copy(
            totalVolumeKg = computedVolume,
            completedSetsCount = completedCount
        )
        activeWorkoutFlow.value = updated
        DudaLocalStorage.setActiveWorkoutDraft(updated)
    }

    override fun finishActiveWorkout(): Workout? {
        val current = activeWorkoutFlow.value ?: return null
        val nowMs = PlatformBridge.currentTimeMillis()
        val startMs = if (current.startTimeEpochMs > 0) current.startTimeEpochMs else (nowMs - 1800_000L)
        val durationSec = ((nowMs - startMs) / 1000L).coerceAtLeast(60L)

        // Filter exercises that have at least 1 completed set (or mark non-zero sets completed, matching ActiveWorkoutSheet.tsx)
        val validExercises = current.exercises.mapNotNull { ex ->
            val completedSets = ex.sets.filter { it.isCompleted || it.weightKg > 0.0 || it.reps > 0 }
                .mapIndexed { idx, s ->
                    s.copy(setNumber = idx + 1, isCompleted = true)
                }
            if (completedSets.isEmpty()) null else ex.copy(sets = completedSets)
        }

        val totalVolume = validExercises.flatMap { it.sets }
            .filter { it.setType != "warmup" }
            .sumOf { it.weightKg * it.reps }
        val totalSets = validExercises.sumOf { it.sets.size }

        val finishedWorkout = current.copy(
            status = "completed",
            endTimeEpochMs = nowMs,
            durationSeconds = durationSec,
            totalVolumeKg = totalVolume,
            completedSetsCount = totalSets,
            exercises = validExercises,
            isFinished = true
        )

        workoutHistoryFlow.value = listOf(finishedWorkout) + workoutHistoryFlow.value
        lastFinishedWorkoutSummaryFlow.value = finishedWorkout
        activeWorkoutFlow.value = null
        isActiveWorkoutSheetOpenFlow.value = false
        DudaLocalStorage.setActiveWorkoutDraft(null)

        if (apiClient != null) {
            scope.launch {
                try {
                    val finishReq = FinishWorkoutRequestDto(
                        durationSeconds = durationSec,
                        exercises = validExercises.map { ex ->
                            FinishExercisePayloadDto(
                                exerciseId = ex.exerciseId,
                                exerciseName = ex.exerciseName,
                                measurementType = ex.measurementType,
                                notes = ex.notes.trim().takeIf { it.isNotEmpty() },
                                sets = ex.sets.mapIndexed { idx, s ->
                                    FinishSetPayloadDto(
                                        id = s.id.ifBlank { PlatformBridge.generateUuid() },
                                        setNumber = idx + 1,
                                        setType = s.setType,
                                        weightKg = s.weightKg,
                                        reps = s.reps,
                                        rpe = s.rpe,
                                        durationSeconds = s.durationSeconds,
                                        distanceMeters = s.distanceMeters,
                                        completed = true
                                    )
                                }
                            )
                        }
                    )
                    try {
                        apiClient.startWorkoutRemote(
                            StartWorkoutRequestDto(
                                id = finishedWorkout.id,
                                routineId = finishedWorkout.routineId,
                                name = finishedWorkout.name,
                                replaceActive = true
                            )
                        )
                    } catch (_: Exception) {}
                    apiClient.finishWorkoutRemote(finishedWorkout.id, finishReq)
                    refreshWorkoutHistory()
                    refreshProgressRecords()
                } catch (_: Exception) {}
            }
        }

        return finishedWorkout
    }

    override fun discardActiveWorkout() {
        val current = activeWorkoutFlow.value
        activeWorkoutFlow.value = null
        isActiveWorkoutSheetOpenFlow.value = false
        DudaLocalStorage.setActiveWorkoutDraft(null)
        if (current != null && apiClient != null) {
            scope.launch {
                try {
                    apiClient.cancelWorkoutRemote(current.id)
                } catch (_: Exception) {}
            }
        }
    }

    // ========================================================================
    // 5. WORKOUT HISTORY & PROGRESS
    // ========================================================================

    override fun getWorkoutHistory(): Flow<List<Workout>> = workoutHistoryFlow.asStateFlow()

    override suspend fun refreshWorkoutHistory(): List<Workout> {
        val client = apiClient ?: return workoutHistoryFlow.value
        val list = client.getWorkouts(limit = 50)
        workoutHistoryFlow.value = list
        return list
    }

    override suspend fun deleteWorkoutFromHistory(workoutId: String) {
        apiClient?.deleteWorkout(workoutId)
        workoutHistoryFlow.value = workoutHistoryFlow.value.filter { it.id != workoutId }
    }

    override fun getRecordGroups(): Flow<List<ExerciseRecordGroupDto>> = recordGroupsFlow.asStateFlow()

    override suspend fun refreshProgressRecords(): List<ExerciseRecordGroupDto> {
        val client = apiClient ?: return recordGroupsFlow.value
        val records = client.getProgressRecords()
        recordGroupsFlow.value = records
        return records
    }

    override suspend fun getExerciseHistoryPoints(exerciseId: String): ExerciseHistoryResponseDto {
        val client = apiClient ?: return ExerciseHistoryResponseDto(exerciseId = exerciseId)
        return client.getExerciseHistory(exerciseId)
    }

    override fun getProgressSummary(): Flow<ProgressSummary> {
        return combine(workoutHistoryFlow, recordGroupsFlow) { history, recordGroups ->
            val totalWorkouts = history.size
            val totalVolume = history.sumOf { it.totalVolumeKg }
            val totalReps = history.sumOf { w ->
                w.exercises.sumOf { ex -> ex.sets.filter { it.isCompleted }.sumOf { it.reps } }
            }

            val prs = if (recordGroups.isNotEmpty()) {
                recordGroups.map { g ->
                    val latestDate = g.records.firstOrNull()?.achievedAt.orEmpty()
                    PersonalRecord(
                        exerciseId = g.exerciseId,
                        exerciseName = g.exerciseName,
                        weightKg = g.bestWeightKg ?: 0.0,
                        reps = g.maxReps ?: 1,
                        e1rmKg = g.bestE1rmKg ?: g.bestWeightKg ?: 0.0,
                        maxVolumeSetKg = g.maxVolumeSetKg ?: 0.0,
                        achievedAt = latestDate
                    )
                }
            } else {
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
                                    achievedAt = w.startedAt,
                                    achievedAtEpochMs = w.startTimeEpochMs
                                )
                            }
                        }
                    }
                }
                prMap.values.toList()
            }

            ProgressSummary(
                totalWorkouts = totalWorkouts,
                totalVolumeKg = totalVolume,
                totalReps = totalReps,
                currentStreakDays = calculateStreakDays(history),
                personalRecords = prs
            )
        }
    }

    private fun calculateStreakDays(history: List<Workout>): Int {
        if (history.isEmpty()) return 0
        val dates = history.mapNotNull { w ->
            if (w.startedAt.length >= 10) w.startedAt.substring(0, 10)
            else if (w.startTimeEpochMs > 0) PlatformBridge.formatDate(w.startTimeEpochMs, "yyyy-MM-dd")
            else null
        }.distinct().sortedDescending()
        return dates.size.coerceAtMost(30)
    }

    // ========================================================================
    // 6. PROGRAMS, SPLITS & COMMUNITY
    // ========================================================================

    override fun getPrograms(): Flow<List<TrainingProgram>> = programsFlow.asStateFlow()

    override fun getActiveProgramBundle(): Flow<ActiveProgramBundle?> = activeProgramBundleFlow.asStateFlow()

    override fun getInstalledProgramsFlow(): Flow<List<ActiveProgramBundle>> = installedProgramsFlow.asStateFlow()

    override suspend fun refreshPrograms(tab: String, splitType: String?, search: String?): List<TrainingProgram> {
        val client = apiClient ?: return programsFlow.value
        val activeProgId = activeProgramBundleFlow.value?.program?.id
        val list = client.getPrograms(tab = tab, splitType = splitType, search = search).map { p ->
            p.copy(isActive = p.id == activeProgId)
        }
        programsFlow.value = list
        return list
    }

    override suspend fun refreshActiveProgram(): ActiveProgramBundle? {
        val client = apiClient ?: return activeProgramBundleFlow.value
        val active = try {
            client.getActiveProgram()
        } catch (_: Exception) {
            null
        }
        activeProgramBundleFlow.value = active
        val activeId = active?.program?.id
        programsFlow.value = programsFlow.value.map { it.copy(isActive = it.id == activeId) }
        return active
    }

    override suspend fun refreshInstalledPrograms(): List<ActiveProgramBundle> {
        val client = apiClient ?: return installedProgramsFlow.value
        val installed = client.getInstalledPrograms()
        installedProgramsFlow.value = installed
        return installed
    }

    override suspend fun activateProgram(programId: String) {
        if (apiClient != null) {
            apiClient.installProgram(programId, setActive = true)
            refreshActiveProgram()
            refreshInstalledPrograms()
        } else {
            programsFlow.value = programsFlow.value.map { it.copy(isActive = it.id == programId) }
        }
    }

    override suspend fun setActiveUserProgram(userProgramId: String) {
        apiClient?.setActiveUserProgram(userProgramId)
        refreshActiveProgram()
        refreshInstalledPrograms()
    }

    override suspend fun saveProgram(programId: String?, request: SaveProgramRequestDto): TrainingProgram {
        val client = apiClient ?: throw IllegalStateException("API client required")
        val saved = if (!programId.isNullOrBlank()) {
            client.updateProgram(programId, request)
        } else {
            client.createProgram(request)
        }
        refreshPrograms(tab = "my_splits")
        return saved
    }

    override suspend fun deleteProgram(programId: String) {
        apiClient?.deleteProgram(programId)
        programsFlow.value = programsFlow.value.filter { it.id != programId }
    }

    override suspend fun publishProgram(programId: String, isPublic: Boolean) {
        apiClient?.publishProgram(programId, isPublic)
        programsFlow.value = programsFlow.value.map {
            if (it.id == programId) it.copy(isPublic = isPublic) else it
        }
    }

    override suspend fun toggleLikeProgram(programId: String) {
        if (apiClient != null) {
            val res = apiClient.toggleLikeProgram(programId)
            programsFlow.value = programsFlow.value.map { p ->
                if (p.id == programId) {
                    p.copy(
                        isLiked = res.hasLiked,
                        likesCount = res.likesCount ?: if (res.hasLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                    )
                } else p
            }
        } else {
            programsFlow.value = programsFlow.value.map { p ->
                if (p.id == programId) {
                    val nextLiked = !p.isLiked
                    p.copy(
                        isLiked = nextLiked,
                        likesCount = if (nextLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                    )
                } else p
            }
        }
    }

    override fun getProgramComments(programId: String): Flow<List<ProgramComment>> {
        val flow = programCommentsMap.getOrPut(programId) { MutableStateFlow(emptyList()) }
        if (apiClient != null) {
            scope.launch {
                try {
                    refreshProgramComments(programId)
                } catch (_: Exception) {}
            }
        }
        return flow.asStateFlow()
    }

    override suspend fun refreshProgramComments(programId: String): List<ProgramComment> {
        val client = apiClient ?: return programCommentsMap[programId]?.value ?: emptyList()
        val rawComments = client.getProgramComments(programId)
        val rootComments = rawComments.filter { it.parentId.isNullOrBlank() }.map { root ->
            val replies = rawComments.filter { it.parentId == root.id }
            root.copy(replies = replies)
        }
        val flow = programCommentsMap.getOrPut(programId) { MutableStateFlow(emptyList()) }
        flow.value = rootComments
        return rootComments
    }

    override suspend fun addProgramComment(programId: String, content: String, parentId: String?): ProgramComment {
        val user = currentUserFlow.value
        if (apiClient != null) {
            val created = apiClient.postProgramComment(
                programId = programId,
                content = content.trim(),
                parentId = parentId,
                authorName = user?.name ?: "Атлет",
                avatarUrl = user?.avatarUrl
            )
            refreshProgramComments(programId)
            programsFlow.value = programsFlow.value.map { p ->
                if (p.id == programId) p.copy(commentsCount = p.commentsCount + 1) else p
            }
            return created
        } else {
            val newComment = ProgramComment(
                id = PlatformBridge.generateUuid(),
                programId = programId,
                userId = user?.id ?: "u-1",
                authorName = user?.name ?: "Атлет",
                content = content.trim(),
                parentId = parentId
            )
            val flow = programCommentsMap.getOrPut(programId) { MutableStateFlow(emptyList()) }
            if (parentId == null) {
                flow.value = listOf(newComment) + flow.value
            } else {
                flow.value = flow.value.map { c ->
                    if (c.id == parentId) c.copy(replies = c.replies + newComment) else c
                }
            }
            return newComment
        }
    }

    override suspend fun upvoteProgramComment(programId: String, commentId: String) {
        apiClient?.upvoteProgramComment(programId, commentId)
        refreshProgramComments(programId)
    }

    override suspend fun deleteProgramComment(programId: String, commentId: String) {
        apiClient?.deleteProgramComment(programId, commentId)
        refreshProgramComments(programId)
    }

    override suspend fun getAthleteProfile(athleteId: String): AthleteProfile {
        val client = apiClient ?: return AthleteProfile(id = athleteId)
        return client.getAthleteProfile(athleteId)
    }

    override suspend fun getPlatformStats(): PlatformStats {
        val client = apiClient ?: return PlatformStats()
        return client.getPlatformStats()
    }

    override suspend fun getFeedbackList(): List<FeedbackReport> {
        val client = apiClient ?: return emptyList()
        return client.getFeedbackList()
    }

    override suspend fun submitFeedback(category: String, title: String, description: String): FeedbackReport {
        val client = apiClient ?: throw IllegalStateException("API client required")
        val author = currentUserFlow.value?.name ?: "Атлет"
        return client.submitFeedback(
            CreateFeedbackRequestDto(
                category = category,
                title = title.trim(),
                description = description.trim(),
                authorName = author
            )
        )
    }

    override suspend fun upvoteFeedback(feedbackId: String) {
        apiClient?.upvoteFeedback(feedbackId)
    }

    // ========================================================================
    // 7. TODOS, DAILY SCHEDULE & FOCUS TIMER
    // ========================================================================

    override fun getDailyScheduleFlow(): Flow<DailyScheduleResponseDto?> = dailyScheduleFlow.asStateFlow()

    override fun getTodos(date: String): Flow<List<TodoTask>> {
        val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(emptyList()) }
        if (apiClient != null) {
            scope.launch {
                try {
                    fetchDailySchedule(date)
                } catch (_: Exception) {}
            }
        }
        return flow.asStateFlow()
    }

    override suspend fun fetchDailySchedule(date: String): DailyScheduleResponseDto {
        val client = apiClient
        if (client != null) {
            val schedule = client.getDailySchedule(date)
            dailyScheduleFlow.value = schedule
            val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(emptyList()) }
            flow.value = schedule.todos
            return schedule
        } else {
            val tasks = todosMapByDate[date]?.value ?: emptyList()
            val completed = tasks.count { it.isCompleted }
            return DailyScheduleResponseDto(
                date = date,
                totalTasks = tasks.size,
                completedTasks = completed,
                todos = tasks
            )
        }
    }

    override suspend fun fetchActivityLogs(date: String): List<ActivityLogItem> {
        val client = apiClient ?: return emptyList()
        return client.getActivityLogs(date)
    }

    override suspend fun deleteActivityLog(logId: String, currentDate: String) {
        apiClient?.deleteActivityLog(logId)
        fetchDailySchedule(currentDate)
    }

    override suspend fun addTodo(task: TodoTask) {
        val date = task.scheduledDate.ifBlank {
            PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        }
        if (apiClient != null) {
            apiClient.createTodo(
                SaveTodoRequestDto(
                    title = task.title,
                    description = task.description,
                    category = task.category.lowercase(),
                    priority = task.priority.lowercase(),
                    scheduledDate = date,
                    startTime = task.startTime,
                    endTime = task.endTime,
                    durationMinutes = task.durationMinutes,
                    isRecurring = task.isRecurring,
                    recurrencePattern = if (task.isRecurring) "daily" else "none"
                )
            )
            fetchDailySchedule(date)
        } else {
            val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(emptyList()) }
            flow.value = flow.value + task.copy(scheduledDate = date)
        }
    }

    override suspend fun saveTodoFull(
        todoId: String?,
        request: SaveTodoRequestDto,
        refreshDate: String
    ): TodoTask {
        val client = apiClient
        if (client != null) {
            val saved = if (!todoId.isNullOrBlank()) {
                client.updateTodo(todoId, request)
            } else {
                client.createTodo(request)
            }
            fetchDailySchedule(refreshDate)
            return saved
        } else {
            val item = TodoTask(
                id = todoId ?: PlatformBridge.generateUuid(),
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
            val flow = todosMapByDate.getOrPut(refreshDate) { MutableStateFlow(emptyList()) }
            flow.value = flow.value.filter { it.id != item.id } + item
            return item
        }
    }

    override suspend fun toggleTodo(taskId: String, date: String) {
        if (apiClient != null) {
            apiClient.toggleTodo(taskId, date)
            fetchDailySchedule(date)
        } else {
            val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(emptyList()) }
            flow.value = flow.value.map { t ->
                if (t.id == taskId) {
                    val nextStatus = if (t.status == "completed") "pending" else "completed"
                    t.copy(status = nextStatus)
                } else t
            }
        }
    }

    override suspend fun deleteTodo(taskId: String) {
        val currentDate = dailyScheduleFlow.value?.date
            ?: PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        if (apiClient != null) {
            apiClient.deleteTodo(taskId)
            fetchDailySchedule(currentDate)
        } else {
            todosMapByDate.values.forEach { flow ->
                flow.value = flow.value.filter { it.id != taskId }
            }
        }
    }

    // --- Focus Timer Global State Machine (matching focusTimerStore.ts) ---

    private fun calculatePomodoroRounds(targetMinutes: Int, strategy: String): List<Int> {
        val totalMin = max(1, targetMinutes)
        if (strategy == "single") return listOf(totalMin * 60)
        val blockSize = when (strategy) {
            "30m" -> 30
            "50m" -> 50
            else -> 25
        }
        if (totalMin <= blockSize) return listOf(totalMin * 60)
        val rounds = mutableListOf<Int>()
        var rem = totalMin
        while (rem > 0) {
            if (rem <= blockSize) {
                rounds.add(rem * 60)
                rem = 0
            } else {
                rounds.add(blockSize * 60)
                rem -= blockSize
            }
        }
        return rounds
    }

    override fun getFocusTimerState(): Flow<FocusTimerState> = focusTimerFlow.asStateFlow()

    override fun startFocusForTodo(todo: TodoTask, strategy: String, mode: String) {
        val totalMin = max(1, todo.durationMinutes)
        val rounds = calculatePomodoroRounds(totalMin, strategy)
        val firstRoundSec = rounds.firstOrNull() ?: (25 * 60)
        val initialRem = when (mode) {
            "stopwatch" -> 0
            "target_timer" -> totalMin * 60
            else -> firstRoundSec
        }
        val next = FocusTimerState(
            isActive = true,
            isRunning = true,
            todoId = todo.id,
            taskTitle = todo.title,
            category = todo.category,
            mode = mode,
            pomodoroPhase = "work",
            pomodoroRound = 1,
            totalPomodoroRounds = rounds.size,
            targetTotalMinutes = totalMin,
            splitStrategy = strategy,
            roundDurationsSec = rounds,
            workDurationSec = firstRoundSec,
            secondsRemaining = initialRem,
            secondsElapsedTotal = 0,
            isModalOpen = true
        )
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun startQuickFocusSession(
        title: String,
        category: String,
        targetMinutes: Int,
        strategy: String,
        mode: String
    ) {
        val totalMin = max(1, targetMinutes)
        val rounds = calculatePomodoroRounds(totalMin, strategy)
        val firstRoundSec = rounds.firstOrNull() ?: (25 * 60)
        val initialRem = when (mode) {
            "stopwatch" -> 0
            "target_timer" -> totalMin * 60
            else -> firstRoundSec
        }
        val next = FocusTimerState(
            isActive = true,
            isRunning = true,
            todoId = null,
            taskTitle = title.ifBlank { "Фокус-сессия" },
            category = category,
            mode = mode,
            pomodoroPhase = "work",
            pomodoroRound = 1,
            totalPomodoroRounds = rounds.size,
            targetTotalMinutes = totalMin,
            splitStrategy = strategy,
            roundDurationsSec = rounds,
            workDurationSec = firstRoundSec,
            secondsRemaining = initialRem,
            secondsElapsedTotal = 0,
            isModalOpen = true
        )
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun pauseFocusTimer() {
        val next = focusTimerFlow.value.copy(isRunning = false)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun resumeFocusTimer() {
        val next = focusTimerFlow.value.copy(isRunning = true)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun tickFocusTimer() {
        val st = focusTimerFlow.value
        if (!st.isActive || !st.isRunning) return
        val isWork = st.mode != "pomodoro" || st.pomodoroPhase == "work"
        val nextElapsed = if (isWork) st.secondsElapsedTotal + 1 else st.secondsElapsedTotal

        if (st.mode == "stopwatch") {
            val next = st.copy(secondsElapsedTotal = nextElapsed)
            focusTimerFlow.value = next
            return
        }

        val nextRem = (st.secondsRemaining - 1).coerceAtLeast(0)
        if (nextRem > 0) {
            focusTimerFlow.value = st.copy(
                secondsRemaining = nextRem,
                secondsElapsedTotal = nextElapsed
            )
            return
        }

        // Phase finished
        PlatformBridge.vibrate()
        if (st.mode == "target_timer") {
            val next = st.copy(secondsRemaining = 0, secondsElapsedTotal = nextElapsed, isRunning = false)
            focusTimerFlow.value = next
            DudaLocalStorage.setFocusTimerState(next)
            return
        }

        if (st.pomodoroPhase == "work") {
            if (st.pomodoroRound >= st.totalPomodoroRounds) {
                val next = st.copy(secondsRemaining = 0, secondsElapsedTotal = nextElapsed, isRunning = false)
                focusTimerFlow.value = next
                DudaLocalStorage.setFocusTimerState(next)
            } else {
                val isLong = st.pomodoroRound % 4 == 0
                val nextPhase = if (isLong) "long_break" else "short_break"
                val breakDur = if (isLong) st.longBreakDurationSec else st.shortBreakDurationSec
                val next = st.copy(
                    pomodoroPhase = nextPhase,
                    secondsRemaining = breakDur,
                    secondsElapsedTotal = nextElapsed
                )
                focusTimerFlow.value = next
                DudaLocalStorage.setFocusTimerState(next)
            }
        } else {
            val nextRound = (st.pomodoroRound + 1).coerceAtMost(st.totalPomodoroRounds)
            val roundDur = st.roundDurationsSec.getOrElse(nextRound - 1) { st.workDurationSec }
            val next = st.copy(
                pomodoroPhase = "work",
                pomodoroRound = nextRound,
                workDurationSec = roundDur,
                secondsRemaining = roundDur,
                secondsElapsedTotal = nextElapsed
            )
            focusTimerFlow.value = next
            DudaLocalStorage.setFocusTimerState(next)
        }
    }

    override fun skipFocusPhase() {
        val st = focusTimerFlow.value
        if (st.mode != "pomodoro") return
        if (st.pomodoroPhase == "work") {
            if (st.pomodoroRound >= st.totalPomodoroRounds) {
                val next = st.copy(secondsRemaining = 0, isRunning = false)
                focusTimerFlow.value = next
                DudaLocalStorage.setFocusTimerState(next)
            } else {
                val isLong = st.pomodoroRound % 4 == 0
                val nextPhase = if (isLong) "long_break" else "short_break"
                val breakDur = if (isLong) st.longBreakDurationSec else st.shortBreakDurationSec
                val next = st.copy(pomodoroPhase = nextPhase, secondsRemaining = breakDur)
                focusTimerFlow.value = next
                DudaLocalStorage.setFocusTimerState(next)
            }
        } else {
            val nextRound = (st.pomodoroRound + 1).coerceAtMost(st.totalPomodoroRounds)
            val roundDur = st.roundDurationsSec.getOrElse(nextRound - 1) { st.workDurationSec }
            val next = st.copy(
                pomodoroPhase = "work",
                pomodoroRound = nextRound,
                workDurationSec = roundDur,
                secondsRemaining = roundDur
            )
            focusTimerFlow.value = next
            DudaLocalStorage.setFocusTimerState(next)
        }
    }

    override fun resetFocusPhase() {
        val st = focusTimerFlow.value
        val dur = when (st.mode) {
            "stopwatch" -> 0
            "target_timer" -> st.targetTotalMinutes * 60
            else -> when (st.pomodoroPhase) {
                "short_break" -> st.shortBreakDurationSec
                "long_break" -> st.longBreakDurationSec
                else -> st.roundDurationsSec.getOrElse(st.pomodoroRound - 1) { st.workDurationSec }
            }
        }
        val next = st.copy(secondsRemaining = dur)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun setFocusMode(mode: String) {
        val st = focusTimerFlow.value
        val rem = when (mode) {
            "stopwatch" -> 0
            "target_timer" -> st.targetTotalMinutes * 60
            else -> st.workDurationSec
        }
        val next = st.copy(mode = mode, pomodoroPhase = "work", secondsRemaining = rem)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun setFocusSplitStrategy(strategy: String) {
        val st = focusTimerFlow.value
        val rounds = calculatePomodoroRounds(st.targetTotalMinutes, strategy)
        val roundIdx = st.pomodoroRound.coerceAtMost(rounds.size)
        val roundSec = rounds.getOrElse(roundIdx - 1) { rounds.first() }
        val next = st.copy(
            splitStrategy = strategy,
            roundDurationsSec = rounds,
            totalPomodoroRounds = rounds.size,
            pomodoroRound = roundIdx,
            workDurationSec = roundSec,
            secondsRemaining = if (st.pomodoroPhase == "work") roundSec else st.secondsRemaining
        )
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun setFocusBreakMinutes(minutes: Int) {
        val st = focusTimerFlow.value
        val sec = max(1, minutes) * 60
        val next = st.copy(
            shortBreakDurationSec = sec,
            secondsRemaining = if (st.pomodoroPhase == "short_break") sec else st.secondsRemaining
        )
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override fun setFocusModalOpen(isOpen: Boolean) {
        val next = focusTimerFlow.value.copy(isModalOpen = isOpen)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override suspend fun finishAndLogFocusSession(markCompleted: Boolean, notes: String) {
        val st = focusTimerFlow.value
        if (!st.isActive) return
        val durationMins = max(1, (st.secondsElapsedTotal / 60.0).roundToInt())
        val today = dailyScheduleFlow.value?.date
            ?: PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")

        if (apiClient != null) {
            apiClient.logFocusSession(
                LogFocusSessionRequestDto(
                    todoId = st.todoId,
                    taskTitle = st.taskTitle,
                    category = st.category,
                    durationMinutes = durationMins,
                    sessionType = st.mode,
                    notes = notes,
                    markCompleted = markCompleted,
                    date = today
                )
            )
            fetchDailySchedule(today)
        }
        discardFocusTimer()
    }

    override fun discardFocusTimer() {
        val cleared = FocusTimerState(isActive = false)
        focusTimerFlow.value = cleared
        DudaLocalStorage.setFocusTimerState(null)
    }

    // ========================================================================
    // 8. NUTRITION & AI FOOD ANALYZER
    // ========================================================================

    override fun getNutritionTodaySummary(): Flow<DailyNutritionSummaryDto?> = nutritionSummaryFlow.asStateFlow()

    override fun getMealLogs(date: String): Flow<List<MealLog>> {
        if (apiClient != null) {
            scope.launch {
                try {
                    refreshNutritionToday()
                } catch (_: Exception) {}
            }
        }
        return mealLogsFlow.asStateFlow()
    }

    override suspend fun refreshNutritionToday(): DailyNutritionSummaryDto {
        val client = apiClient
        val user = currentUserFlow.value
        if (client != null) {
            val remote = client.getNutritionToday()
            val merged = remote.copy(
                targetCalories = user?.targetCalories ?: remote.targetCalories,
                targetProteinG = (user?.targetProteinG ?: remote.targetProteinG.toInt()).toDouble(),
                targetCarbsG = (user?.targetCarbsG ?: remote.targetCarbsG.toInt()).toDouble(),
                targetFatG = (user?.targetFatG ?: remote.targetFatG.toInt()).toDouble()
            )
            nutritionSummaryFlow.value = merged
            mealLogsFlow.value = merged.meals
            return merged
        } else {
            val meals = mealLogsFlow.value
            val summary = DailyNutritionSummaryDto(
                date = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd"),
                totalCalories = meals.sumOf { it.calories },
                totalProteinG = meals.sumOf { it.proteinG },
                totalCarbsG = meals.sumOf { it.carbsG },
                totalFatG = meals.sumOf { it.fatG },
                meals = meals
            )
            nutritionSummaryFlow.value = summary
            return summary
        }
    }

    override suspend fun addMealLog(meal: MealLog) {
        if (apiClient != null) {
            apiClient.saveMealLog(
                SaveMealRequestDto(
                    mealType = meal.mealType,
                    name = meal.name,
                    calories = meal.calories,
                    proteinG = meal.proteinG,
                    carbsG = meal.carbsG,
                    fatG = meal.fatG,
                    items = meal.items
                )
            )
            refreshNutritionToday()
        } else {
            mealLogsFlow.value = mealLogsFlow.value + meal
        }
    }

    override suspend fun deleteMealLog(mealId: String) {
        if (apiClient != null) {
            apiClient.deleteMealLog(mealId)
            refreshNutritionToday()
        } else {
            mealLogsFlow.value = mealLogsFlow.value.filter { it.id != mealId }
        }
    }

    override suspend fun estimateFoodTextFull(description: String): MealAnalysisResult {
        val client = apiClient ?: return MealAnalysisResult(
            mealName = description.take(40),
            totalCalories = 420,
            proteinG = 32.0,
            carbsG = 45.0,
            fatG = 12.0,
            coachTip = "Отличный баланс БЖУ для восстановления."
        )
        return client.estimateFoodText(description)
    }

    override suspend fun analyzeFoodPhotoFull(
        imageBase64: String,
        mimeType: String,
        notes: String
    ): MealAnalysisResult {
        val client = apiClient ?: throw IllegalStateException("API client required")
        return client.analyzeFoodPhoto(imageBase64 = imageBase64, mimeType = mimeType, notes = notes)
    }

    override suspend fun analyzeFood(description: String): MealLog {
        val result = estimateFoodTextFull(description)
        return MealLog(
            id = PlatformBridge.generateUuid(),
            date = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd"),
            mealType = "lunch",
            name = result.mealName.ifBlank { description.take(40) },
            calories = result.totalCalories,
            proteinG = result.proteinG,
            carbsG = result.carbsG,
            fatG = result.fatG,
            items = result.items
        )
    }

    override fun getWaterMl(date: String): Flow<Int> {
        val flow = waterFlowsMap.getOrPut(date) {
            MutableStateFlow(DudaLocalStorage.getCachedWater(date))
        }
        return flow.asStateFlow()
    }

    override suspend fun addWater(date: String, amountMl: Int) {
        val current = DudaLocalStorage.getCachedWater(date)
        val updated = (current + amountMl).coerceAtLeast(0)
        DudaLocalStorage.setCachedWater(date, updated)
        val flow = waterFlowsMap.getOrPut(date) { MutableStateFlow(updated) }
        flow.value = updated
    }

    // ========================================================================
    // 9. AI COACH
    // ========================================================================

    override fun getCoachInsight(): Flow<CoachInsight> = coachInsightFlow.asStateFlow()

    override fun getCoachChat(): Flow<List<ChatMessage>> = chatMessagesFlow.asStateFlow()

    override fun getCoachSuggestions(): Flow<List<String>> = coachSuggestionsFlow.asStateFlow()

    override suspend fun refreshCoachData() {
        val client = apiClient ?: return
        try {
            coachInsightFlow.value = client.getCoachInsights()
        } catch (_: Exception) {}
        try {
            chatMessagesFlow.value = client.getCoachMessages()
        } catch (_: Exception) {}
    }

    override suspend fun sendCoachChatMessage(text: String): ChatMessage {
        return sendCoachChatWithImage(text = text, imageBase64 = null, mimeType = null)
    }

    override suspend fun sendCoachChatWithImage(
        text: String,
        imageBase64: String?,
        mimeType: String?
    ): ChatMessage {
        val userMsg = ChatMessage(
            id = PlatformBridge.generateUuid(),
            role = "user",
            content = text,
            hasImage = !imageBase64.isNullOrBlank(),
            timestampEpochMs = PlatformBridge.currentTimeMillis()
        )
        val previousHistory = chatMessagesFlow.value.takeLast(8).map {
            CoachChatHistoryItemDto(role = it.role, content = it.content)
        }
        chatMessagesFlow.value = chatMessagesFlow.value + userMsg

        val client = apiClient
        val aiMsg = if (client != null) {
            val resp = client.sendCoachChat(
                message = text,
                imageBase64 = imageBase64,
                mimeType = mimeType,
                history = previousHistory
            )
            if (resp.suggestions.isNotEmpty()) {
                coachSuggestionsFlow.value = resp.suggestions
            }
            ChatMessage(
                id = PlatformBridge.generateUuid(),
                role = "assistant",
                content = resp.reply,
                suggestedWorkout = resp.suggestedWorkout,
                timestampEpochMs = PlatformBridge.currentTimeMillis() + 200
            )
        } else {
            ChatMessage(
                id = PlatformBridge.generateUuid(),
                role = "assistant",
                content = "Придерживайтесь прогрессивной перегрузки и отдыхайте 90–120 секунд между рабочими подходами.",
                timestampEpochMs = PlatformBridge.currentTimeMillis() + 200
            )
        }

        chatMessagesFlow.value = chatMessagesFlow.value + aiMsg
        return aiMsg
    }

    override suspend fun clearCoachHistory() {
        apiClient?.clearCoachMessages()
        chatMessagesFlow.value = emptyList()
    }

    // ========================================================================
    // 10. BODY & BMI
    // ========================================================================

    override fun getBodyMeasurements(): Flow<List<BodyMeasurement>> = bodyMeasurementsFlow.asStateFlow()

    override fun getBodyTrendFlow(): Flow<BodyTrendResponseDto?> = bodyTrendFlow.asStateFlow()

    override suspend fun refreshBodyData() {
        val client = apiClient ?: return
        try {
            bodyTrendFlow.value = client.getBodyTrend()
        } catch (_: Exception) {}
        try {
            bodyMeasurementsFlow.value = client.getBodyLogs()
        } catch (_: Exception) {}
    }

    override suspend fun addBodyMeasurement(weightKg: Double, heightCm: Double, bodyFatPercent: Double?) {
        val today = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        saveBodyLogFull(
            SaveBodyLogRequestDto(
                logDate = today,
                weightKg = weightKg,
                heightCm = heightCm,
                bodyFatPercentage = bodyFatPercent
            )
        )
    }

    override suspend fun saveBodyLogFull(request: SaveBodyLogRequestDto): BodyMeasurement {
        updateUserStatsLocal { u ->
            u.copy(
                weightKg = request.weightKg,
                heightCm = request.heightCm ?: u.heightCm
            )
        }
        val client = apiClient
        return if (client != null) {
            val saved = client.saveBodyLog(request)
            refreshBodyData()
            saved
        } else {
            val m = BodyMeasurement(
                id = PlatformBridge.generateUuid(),
                logDate = request.logDate,
                weightKg = request.weightKg,
                heightCm = request.heightCm ?: 178.0,
                bodyFatPercent = request.bodyFatPercentage,
                waistCm = request.waistCm,
                chestCm = request.chestCm,
                armsCm = request.armsCm,
                neckCm = request.neckCm,
                dateEpochMs = PlatformBridge.currentTimeMillis()
            )
            bodyMeasurementsFlow.value = bodyMeasurementsFlow.value + m
            m
        }
    }

    override suspend fun deleteBodyLog(date: String) {
        apiClient?.deleteBodyLog(date)
        bodyMeasurementsFlow.value = bodyMeasurementsFlow.value.filter { it.logDate != date }
        if (apiClient != null) {
            refreshBodyData()
        }
    }

    // ========================================================================
    // 11. PREFERENCES
    // ========================================================================

    override fun getUserPreferences(): Flow<UserPreferencesData> = preferencesFlow.asStateFlow()

    override suspend fun updatePreferences(preferences: UserPreferencesData) {
        preferencesFlow.value = preferences
        DudaLocalStorage.setUserPreferences(preferences)
    }
}
