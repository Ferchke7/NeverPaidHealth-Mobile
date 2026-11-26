package dev.neverpaid.health.data.repository

import kotlinx.datetime.Instant
import kotlinx.datetime.atTime
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import dev.neverpaid.health.core.audio.AthleteSoundManager
import dev.neverpaid.health.core.i18n.getDudaStrings
import dev.neverpaid.health.core.math.SetCalculationInput
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.network.DudaApiClient
import dev.neverpaid.health.core.platform.LiveWorkoutWidgetState
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.core.platform.TimerNotificationIds
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
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.ProgramDayExercise
import dev.neverpaid.health.domain.model.ProgressSummary
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveBodyLogRequestDto
import dev.neverpaid.health.domain.model.SaveExerciseRequestDto
import dev.neverpaid.health.domain.model.SaveMealRequestDto
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.SaveWeeklyPlanRequestDto
import dev.neverpaid.health.domain.model.StartWorkoutRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UpdateWorkoutRequestDto
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.WeeklyPlanItem
import dev.neverpaid.health.domain.model.WeeklyPlanUpsertItemDto
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutExercise
import dev.neverpaid.health.domain.model.WorkoutSet
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.roundToInt

class NeverPaidRepositoryImpl(
    private val apiClient: DudaApiClient? = null,
    private val nowProvider: () -> Long = { PlatformBridge.currentTimeMillis() }
) : NeverPaidRepository {

    private val scope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, _ -> }
    )
    private val templateSyncMutex = Mutex()
    private var activeWorkoutSyncJob: Job? = null
    private var pendingLocalWorkoutId: String? = null
    private val routineForkAliases = mutableMapOf<String, String>()
    private val programForkAliases = mutableMapOf<String, String>()
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // --- Auth & User ---
    private val isAuthenticatedFlow = MutableStateFlow(DudaLocalStorage.getAccessToken() != null || DudaLocalStorage.getUserProfile() != null)
    private val currentUserFlow = MutableStateFlow(DudaLocalStorage.getUserProfile())
    private val preferencesFlow = MutableStateFlow(DudaLocalStorage.getUserPreferences())

    // --- Live API State Flows (Offline-first persistent storage guarantees zero data loss) ---
    private val exercisesFlow = MutableStateFlow<List<Exercise>>(StandardExerciseCatalog.standardExercises)
    private val userRoutinesFlow = MutableStateFlow<List<Routine>>(DudaLocalStorage.getUserRoutines())
    private val weeklyPlanFlow = MutableStateFlow<List<WeeklyPlanItem>>(DudaLocalStorage.getWeeklyPlan())
    private val activeWorkoutFlow = MutableStateFlow<Workout?>(DudaLocalStorage.getActiveWorkoutDraft())
    private val isActiveWorkoutSheetOpenFlow = MutableStateFlow(false)
    private val lastFinishedWorkoutSummaryFlow = MutableStateFlow<Workout?>(null)
    private val previousSetsMapFlow = MutableStateFlow<Map<String, List<PreviousSetData>>>(emptyMap())
    private val previousNotesMapFlow = MutableStateFlow<Map<String, PreviousExerciseNote>>(emptyMap())

    private val workoutHistoryFlow = MutableStateFlow<List<Workout>>(DudaLocalStorage.getWorkoutHistory())
    private val recordGroupsFlow = MutableStateFlow<List<ExerciseRecordGroupDto>>(emptyList())

    private val programsFlow = MutableStateFlow<List<TrainingProgram>>(DudaLocalStorage.getCustomPrograms())
    private val activeProgramBundleFlow = MutableStateFlow<ActiveProgramBundle?>(DudaLocalStorage.getActiveProgramBundle())
    private val installedProgramsFlow = MutableStateFlow<List<ActiveProgramBundle>>(emptyList())
    private val programCommentsMap = mutableMapOf<String, MutableStateFlow<List<ProgramComment>>>()

    private val dailyScheduleFlow = MutableStateFlow<DailyScheduleResponseDto?>(null)
    private val todosMapByDate = mutableMapOf<String, MutableStateFlow<List<TodoTask>>>()
    private val focusTimerFlow = MutableStateFlow(DudaLocalStorage.getFocusTimerState())
    private val focusAlarmRingingFlow = MutableStateFlow(PlatformBridge.isFocusAlarmRinging())

    private val nutritionSummaryFlow = MutableStateFlow<DailyNutritionSummaryDto?>(null)
    private val mealLogsFlow = MutableStateFlow<List<MealLog>>(DudaLocalStorage.getMealLogs())
    private val waterFlowsMap = mutableMapOf<String, MutableStateFlow<Int>>()

    private val coachInsightFlow = MutableStateFlow(CoachInsight())
    private val chatMessagesFlow = MutableStateFlow<List<ChatMessage>>(emptyList())
    private val coachSuggestionsFlow = MutableStateFlow<List<String>>(emptyList())

    private val bodyMeasurementsFlow = MutableStateFlow<List<BodyMeasurement>>(DudaLocalStorage.getBodyMeasurements())
    private val bodyTrendFlow = MutableStateFlow<BodyTrendResponseDto?>(null)

    init {
        syncFocusTimerFromClock()
        publishLiveWorkoutWidget(activeWorkoutFlow.value)
        PlatformBridge.setFocusAlarmRingingListener { ringing ->
            focusAlarmRingingFlow.value = ringing
        }
        apiClient?.onUnauthorized = {
            if (!isGuestUser()) {
                isAuthenticatedFlow.value = false
                currentUserFlow.value = null
            }
        }

        if (isAuthenticatedFlow.value && apiClient != null && !isGuestUser()) {
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
                } catch (_: Throwable) {
                    if (DudaLocalStorage.getAccessToken() == null && !isGuestUser()) {
                        isAuthenticatedFlow.value = false
                        currentUserFlow.value = null
                    }
                }
            }
        }
    }

    private suspend fun refreshInitialAuthenticatedData() {
        syncWithServer()
    }

    override suspend fun syncWithServer() {
        syncFocusTimerFromClock()
        if (apiClient == null || !isAuthenticatedFlow.value || isGuestUser()) return
        try { refreshExercises() } catch (_: Throwable) {}
        try { refreshRoutines() } catch (_: Throwable) {}
        try { refreshActiveProgram() } catch (_: Throwable) {}
        try { refreshInstalledPrograms() } catch (_: Throwable) {}
        try { refreshWeeklyPlan() } catch (_: Throwable) {}
        try { refreshWorkoutHistory() } catch (_: Throwable) {}
        try { refreshActiveWorkout() } catch (_: Throwable) {}
        try { refreshProgressRecords() } catch (_: Throwable) {}
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

    private fun tryParseJwtClaims(token: String): UserProfileDto? {
        return try {
            val parts = token.trim().split(".")
            if (parts.size < 2) return null
            var normalized = parts[1].replace('-', '+').replace('_', '/')
            while (normalized.length % 4 != 0) {
                normalized += "="
            }
            val jsonString = PlatformBridge.decodeBase64ToString(normalized) ?: return null
            val element = json.parseToJsonElement(jsonString)
            val obj = element as? kotlinx.serialization.json.JsonObject ?: return null

            val email = obj["email"]?.toString()?.trim('"')
                ?: obj["sub"]?.toString()?.trim('"')
                ?: "athlete@duda.uz"
            val name = obj["name"]?.toString()?.trim('"')
                ?: obj["displayName"]?.toString()?.trim('"')
                ?: email.substringBefore("@").replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            val picture = obj["picture"]?.toString()?.trim('"')
            val id = obj["user_id"]?.toString()?.trim('"')
                ?: obj["sub"]?.toString()?.trim('"')
                ?: PlatformBridge.generateUuid()

            UserProfileDto(
                id = id,
                email = email,
                displayName = name,
                avatarUrl = picture
            )
        } catch (_: Exception) {
            null
        }
    }

    override suspend fun authenticateWithAccessToken(accessToken: String, userJson: String?): UserProfileDto {
        val cleanToken = accessToken.trim().trim('"', '\'')
        DudaLocalStorage.setAccessToken(cleanToken)
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
                DudaLocalStorage.setUserProfile(profile)
                isAuthenticatedFlow.value = true
                currentUserFlow.value = profile
                scope.launch { refreshInitialAuthenticatedData() }
                profile
            } catch (eMe: Exception) {
                // If /me rejected the token, try exchanging it via POST /auth/google in case it is a Google id_token
                try {
                    val authResp = client.authenticateGoogle(cleanToken)
                    DudaLocalStorage.setUserProfile(authResp.user)
                    isAuthenticatedFlow.value = true
                    currentUserFlow.value = authResp.user
                    scope.launch { refreshInitialAuthenticatedData() }
                    authResp.user
                } catch (_: Exception) {
                    // Fall back to offline JWT claim decoding so the athlete is never locked out
                    val jwtProfile = tryParseJwtClaims(cleanToken)
                    if (jwtProfile != null) {
                        DudaLocalStorage.setUserProfile(jwtProfile)
                        currentUserFlow.value = jwtProfile
                        isAuthenticatedFlow.value = true
                        jwtProfile
                    } else {
                        throw eMe
                    }
                }
            }
        } else {
            val jwtProfile = tryParseJwtClaims(cleanToken)
            val fallback = jwtProfile ?: currentUserFlow.value ?: UserProfileDto(
                id = "user-1",
                email = "athlete@duda.uz",
                displayName = "Athlete"
            )
            DudaLocalStorage.setUserProfile(fallback)
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

    override fun continueAsGuest() {
        val guest = UserProfileDto(
            id = "guest_athlete",
            email = "athlete@duda.uz",
            displayName = "Гость",
            unitPreference = if (preferencesFlow.value.useKilograms) "kg" else "lb"
        )
        DudaLocalStorage.setUserProfile(guest)
        currentUserFlow.value = guest
        isAuthenticatedFlow.value = true
    }

    override fun isGuestUser(): Boolean {
        return currentUserFlow.value?.id == "guest_athlete" || DudaLocalStorage.getAccessToken() == null
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
        activeWorkoutSyncJob?.cancel()
        pendingLocalWorkoutId = null
        apiClient?.logout()
        DudaLocalStorage.clearAuthSession()
        isAuthenticatedFlow.value = false
        currentUserFlow.value = null
        activeWorkoutFlow.value = null
        isActiveWorkoutSheetOpenFlow.value = false
        userRoutinesFlow.value = emptyList()
        weeklyPlanFlow.value = emptyList()
        activeProgramBundleFlow.value = null
        installedProgramsFlow.value = emptyList()
        programsFlow.value = emptyList()
        workoutHistoryFlow.value = emptyList()
        routineForkAliases.clear()
        programForkAliases.clear()
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.FOCUS)
        PlatformBridge.updateLiveWorkoutWidget(null)
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

    private fun deduplicatePreferringPersonal(list: List<Routine>): List<Routine> {
        val personalBases = list
            .filter { it.userId != "00000000-0000-0000-0000-000000000000" }
            .map { normalizeRoutineBaseName(it.name) }
            .toSet()
        return list.filter { rot ->
            rot.userId != "00000000-0000-0000-0000-000000000000" ||
                normalizeRoutineBaseName(rot.name) !in personalBases
        }
    }

    override suspend fun refreshRoutines(): List<Routine> {
        val client = apiClient ?: return userRoutinesFlow.value
        val list = deduplicatePreferringPersonal(client.getRoutines())
        userRoutinesFlow.value = list
        DudaLocalStorage.setUserRoutines(list)
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

    private fun normalizeRoutineBaseName(raw: String): String {
        var s = raw.trim().lowercase()
        val suffixes = listOf("(custom)", "(своя)", "(shaxsiy)")
        var changed = true
        while (changed) {
            changed = false
            for (suffix in suffixes) {
                if (s.endsWith(suffix)) {
                    s = s.dropLast(suffix.length).trim()
                    changed = true
                }
            }
        }
        return s
    }

    override suspend fun saveRoutineFull(
        routineId: String?,
        name: String,
        notes: String?,
        exercises: List<RoutineExercise>
    ): Routine {
        val cleanName = name.trim()
        val isTemplateLikePrebuilt = !routineId.isNullOrBlank() && routineId.startsWith("prebuilt-")
        val resolvedTargetId = routineId?.let { routineForkAliases[it] ?: it }
        val existingOwnMatch = if (isTemplateLikePrebuilt) {
            val base = normalizeRoutineBaseName(cleanName)
            userRoutinesFlow.value.firstOrNull {
                it.userId != "00000000-0000-0000-0000-000000000000" &&
                    normalizeRoutineBaseName(it.name) == base
            }
        } else null

        val effectiveId = existingOwnMatch?.id ?: resolvedTargetId?.takeIf { !it.startsWith("prebuilt-") }
        val req = SaveRoutineRequestDto(
            name = cleanName,
            notes = notes?.trim(),
            exercises = exercises.mapIndexed { idx, ex -> ex.copy(orderIndex = idx) }
        )
        val saved = if (apiClient != null) {
            if (!effectiveId.isNullOrBlank()) {
                apiClient.updateRoutine(effectiveId, req)
            } else {
                apiClient.createRoutine(req)
            }
        } else {
            Routine(
                id = effectiveId ?: PlatformBridge.generateUuid(),
                name = req.name,
                notes = req.notes ?: existingOwnMatch?.notes,
                exercises = req.exercises
            )
        }
        if (!routineId.isNullOrBlank() && saved.id != routineId) {
            routineForkAliases[routineId] = saved.id
        }
        val exists = userRoutinesFlow.value.any { it.id == saved.id }
        val rawUpdated = if (exists) {
            userRoutinesFlow.value.map { if (it.id == saved.id) saved else it }
        } else {
            listOf(saved) + userRoutinesFlow.value
        }
        val updated = deduplicatePreferringPersonal(rawUpdated)
        userRoutinesFlow.value = updated
        DudaLocalStorage.setUserRoutines(updated)
        return saved
    }

    override suspend fun deleteRoutine(routineId: String) {
        apiClient?.deleteRoutine(routineId)
        val filtered = userRoutinesFlow.value.filter { it.id != routineId }
        userRoutinesFlow.value = filtered
        DudaLocalStorage.setUserRoutines(filtered)
        val filteredPlan = normalizeWeeklyPlanOrder(weeklyPlanFlow.value.filterNot { it.routineId == routineId })
        weeklyPlanFlow.value = filteredPlan
        DudaLocalStorage.setWeeklyPlan(filteredPlan)
    }

    private fun normalizeWeeklyPlanOrder(items: List<WeeklyPlanItem>): List<WeeklyPlanItem> {
        val sorted = items
            .filter { it.dayOfWeek in 1..7 }
            .sortedWith(compareBy<WeeklyPlanItem> { it.dayOfWeek }.thenBy { it.orderIndex })
        val counters = mutableMapOf<Int, Int>()
        return sorted.map { item ->
            val idx = counters[item.dayOfWeek] ?: 0
            counters[item.dayOfWeek] = idx + 1
            item.copy(orderIndex = idx)
        }
    }

    override fun observeWeeklyPlan(): Flow<List<WeeklyPlanItem>> = weeklyPlanFlow.asStateFlow()

    override suspend fun refreshWeeklyPlan(): List<WeeklyPlanItem> {
        val client = apiClient
        if (client != null && !isGuestUser()) {
            val remote = normalizeWeeklyPlanOrder(client.getWeeklyPlan().items)
            weeklyPlanFlow.value = remote
            DudaLocalStorage.setWeeklyPlan(remote)
            return remote
        }
        return weeklyPlanFlow.value
    }

    override suspend fun saveWeeklyPlan(items: List<WeeklyPlanItem>): List<WeeklyPlanItem> {
        val normalized = normalizeWeeklyPlanOrder(items)
        weeklyPlanFlow.value = normalized
        DudaLocalStorage.setWeeklyPlan(normalized)
        val client = apiClient
        if (client != null && !isGuestUser()) {
            val req = SaveWeeklyPlanRequestDto(
                items = normalized.map { it ->
                    WeeklyPlanUpsertItemDto(
                        id = it.id.takeIf { id -> id.isNotBlank() && !id.startsWith("local-") },
                        dayOfWeek = it.dayOfWeek,
                        orderIndex = it.orderIndex,
                        itemType = it.itemType,
                        routineId = it.routineId?.takeIf { id -> !id.startsWith("prebuilt-") },
                        programId = it.programId,
                        programDayNumber = it.programDayNumber,
                        title = it.title,
                        notes = it.notes
                    )
                }
            )
            val saved = normalizeWeeklyPlanOrder(client.saveWeeklyPlan(req).items)
            weeklyPlanFlow.value = saved
            DudaLocalStorage.setWeeklyPlan(saved)
            return saved
        }
        return normalized
    }

    override suspend fun setRoutineWeekdays(routineId: String, daysOfWeek: List<Int>): List<WeeklyPlanItem> {
        val cleanDays = daysOfWeek.filter { it in 1..7 }.distinct().sorted()
        val client = apiClient
        if (client != null && !isGuestUser() && !routineId.startsWith("prebuilt-")) {
            val saved = normalizeWeeklyPlanOrder(client.setRoutineWeekdays(routineId, cleanDays).items)
            weeklyPlanFlow.value = saved
            DudaLocalStorage.setWeeklyPlan(saved)
            return saved
        }
        val routine = (userRoutinesFlow.value + StandardExerciseCatalog.prebuiltRoutines).find { it.id == routineId }
        val kept = weeklyPlanFlow.value.filter { it.routineId != routineId || it.dayOfWeek in cleanDays }
        val existingDays = kept.filter { it.routineId == routineId }.map { it.dayOfWeek }.toSet()
        val added = cleanDays.filterNot { it in existingDays }.map { day ->
            WeeklyPlanItem(
                id = PlatformBridge.generateUuid(),
                dayOfWeek = day,
                orderIndex = 999,
                itemType = "routine",
                routineId = routineId,
                title = routine?.name ?: "Тренировка",
                subtitle = routine?.notes.orEmpty(),
                exercises = routine?.exercises.orEmpty()
            )
        }
        val next = normalizeWeeklyPlanOrder(kept + added)
        weeklyPlanFlow.value = next
        DudaLocalStorage.setWeeklyPlan(next)
        return next
    }

    override suspend fun toggleWeeklyPlanItemComplete(itemId: String, completed: Boolean): List<WeeklyPlanItem> {
        val optimistic = weeklyPlanFlow.value.map {
            if (it.id == itemId) it.copy(isCompletedThisWeek = completed) else it
        }
        weeklyPlanFlow.value = optimistic
        DudaLocalStorage.setWeeklyPlan(optimistic)
        val client = apiClient
        if (client != null && !isGuestUser()) {
            val saved = normalizeWeeklyPlanOrder(client.toggleWeeklyPlanComplete(itemId, completed).items)
            weeklyPlanFlow.value = saved
            DudaLocalStorage.setWeeklyPlan(saved)
            return saved
        }
        return optimistic
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
            routineId = routine?.id?.let { routineForkAliases[it] ?: it }
        )
    }

    override fun startWorkoutWithExercises(
        title: String,
        exercises: List<WorkoutExercise>,
        routineId: String?,
        programId: String?,
        programDayNumber: Int?
    ): Workout {
        val workoutId = PlatformBridge.generateUuid()
        val nowMs = PlatformBridge.currentTimeMillis()
        val newWorkout = Workout(
            id = workoutId,
            routineId = routineId,
            programId = programId,
            programDayNumber = programDayNumber,
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
        publishLiveWorkoutWidget(newWorkout)

        // Sync start with backend & fetch previous sets/notes (matching useStartWorkout.ts)
        if (apiClient != null && !isGuestUser()) {
            pendingLocalWorkoutId = workoutId
            scope.launch {
                try {
                    apiClient.startWorkoutRemote(
                        StartWorkoutRequestDto(
                            id = workoutId,
                            routineId = routineId?.takeIf { !it.startsWith("prebuilt-") },
                            name = newWorkout.name,
                            replaceActive = true,
                            exercises = buildWorkoutExercisePayloads(newWorkout.exercises, forceCompleted = false)
                        )
                    )
                } catch (_: Exception) {
                } finally {
                    if (pendingLocalWorkoutId == workoutId) {
                        pendingLocalWorkoutId = null
                    }
                }

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

    private fun buildWorkoutExercisePayloads(
        exercises: List<WorkoutExercise>,
        forceCompleted: Boolean
    ): List<FinishExercisePayloadDto> {
        return exercises.map { ex ->
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
                        completed = if (forceCompleted) true else s.isCompleted
                    )
                }
            )
        }
    }

    private fun normalizeRemoteActiveWorkout(remote: Workout): Workout {
        val parsedStartMs = if (remote.startTimeEpochMs > 0L) {
            remote.startTimeEpochMs
        } else {
            runCatching { Instant.parse(remote.startedAt).toEpochMilliseconds() }
                .getOrElse { PlatformBridge.currentTimeMillis() }
        }
        val normalizedExercises = remote.exercises.mapIndexed { exIdx, ex ->
            ex.copy(
                instanceId = ex.instanceId.ifBlank { PlatformBridge.generateUuid() },
                orderIndex = ex.orderIndex.takeIf { it >= 0 } ?: exIdx,
                sets = ex.sets.mapIndexed { sIdx, s ->
                    s.copy(
                        id = s.id.ifBlank { PlatformBridge.generateUuid() },
                        setNumber = if (s.setNumber > 0) s.setNumber else sIdx + 1
                    )
                }
            )
        }
        return remote.copy(
            startTimeEpochMs = parsedStartMs,
            exercises = normalizedExercises,
            isFinished = false
        )
    }

    override suspend fun refreshActiveWorkout(): Workout? {
        val client = apiClient ?: return activeWorkoutFlow.value
        if (isGuestUser()) return activeWorkoutFlow.value
        val remote = try {
            client.getActiveWorkoutRemote()
        } catch (_: Exception) {
            return activeWorkoutFlow.value
        }

        if (remote != null) {
            val normalized = normalizeRemoteActiveWorkout(remote)
            val cur = activeWorkoutFlow.value
            if (cur == null || cur.id != normalized.id || (pendingLocalWorkoutId != normalized.id && activeWorkoutSyncJob?.isActive != true)) {
                activeWorkoutFlow.value = normalized
                DudaLocalStorage.setActiveWorkoutDraft(normalized)
                publishLiveWorkoutWidget(normalized)
                fetchPreviousForActiveWorkout(normalized)
            }
            return normalized
        } else {
            val cur = activeWorkoutFlow.value
            if (cur != null && pendingLocalWorkoutId != cur.id) {
                activeWorkoutFlow.value = null
                isActiveWorkoutSheetOpenFlow.value = false
                DudaLocalStorage.setActiveWorkoutDraft(null)
                DudaLocalStorage.setRestTimerState(null)
                PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
                publishLiveWorkoutWidget(null)
            }
            return null
        }
    }

    private fun publishLiveWorkoutWidget(workout: Workout?) {
        if (workout == null) {
            PlatformBridge.updateLiveWorkoutWidget(null)
            return
        }
        val allSets = workout.exercises.flatMap { it.sets }
        val currentEx = workout.exercises.firstOrNull { ex -> ex.sets.any { !it.isCompleted } }
            ?: workout.exercises.lastOrNull()
        val now = nowProvider()
        val savedRest = DudaLocalStorage.getRestTimerState()?.takeIf {
            (it.workoutId.isBlank() || it.workoutId == workout.id) && it.endEpochMs > now
        }
        val prefs = preferencesFlow.value
        PlatformBridge.updateLiveWorkoutWidget(
            LiveWorkoutWidgetState(
                workoutId = workout.id,
                workoutTitle = workout.title,
                startTimeEpochMs = workout.startTimeEpochMs.takeIf { it > 0L } ?: now,
                completedSets = allSets.count { it.isCompleted },
                totalSets = allSets.size,
                totalVolumeKg = workout.totalVolume,
                currentExerciseName = currentEx?.exerciseName.orEmpty(),
                restEndEpochMs = savedRest?.endEpochMs ?: 0L,
                restTotalSeconds = savedRest?.totalSeconds ?: 0,
                defaultRestSeconds = prefs.defaultRestSeconds,
                useKilograms = prefs.useKilograms,
                languageCode = prefs.languageCode
            )
        )
    }

    private fun workoutStructureSignature(w: Workout?): String {
        if (w == null) return ""
        return w.exercises.joinToString("|") { ex ->
            "${ex.exerciseId}:${ex.exerciseName}:${ex.sets.count { it.setType != "warmup" }}:${ex.sets.size}"
        }
    }

    override fun updateActiveWorkout(workout: Workout) {
        val prevSignature = workoutStructureSignature(activeWorkoutFlow.value)
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
        publishLiveWorkoutWidget(updated)

        val nextSignature = workoutStructureSignature(updated)
        if (prevSignature != nextSignature && updated.exercises.isNotEmpty()) {
            syncRoutineFromWorkout(updated)
            syncProgramFromWorkout(updated, syncRemote = true)
        }

        if (apiClient != null && !isGuestUser()) {
            activeWorkoutSyncJob?.cancel()
            activeWorkoutSyncJob = scope.launch {
                delay(450)
                val latest = activeWorkoutFlow.value ?: return@launch
                if (latest.id != updated.id) return@launch
                try {
                    apiClient.updateActiveWorkoutRemote(
                        workoutId = latest.id,
                        request = UpdateWorkoutRequestDto(
                            routineId = latest.routineId?.takeIf { !it.startsWith("prebuilt-") },
                            name = latest.name,
                            exercises = buildWorkoutExercisePayloads(latest.exercises, forceCompleted = false)
                        )
                    )
                } catch (_: Exception) {}
            }
        }
    }

    override fun finishActiveWorkout(): Workout? {
        activeWorkoutSyncJob?.cancel()
        pendingLocalWorkoutId = null
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

        if (current.exercises.isNotEmpty()) {
            syncProgramFromWorkout(current, syncRemote = true)
        }

        val updatedHistory = listOf(finishedWorkout) + workoutHistoryFlow.value
        workoutHistoryFlow.value = updatedHistory
        DudaLocalStorage.setWorkoutHistory(updatedHistory)
        lastFinishedWorkoutSummaryFlow.value = finishedWorkout
        activeWorkoutFlow.value = null
        isActiveWorkoutSheetOpenFlow.value = false
        DudaLocalStorage.setActiveWorkoutDraft(null)
        DudaLocalStorage.setRestTimerState(null)
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
        publishLiveWorkoutWidget(null)

        if (apiClient != null) {
            scope.launch {
                try {
                    val finishReq = FinishWorkoutRequestDto(
                        durationSeconds = durationSec,
                        exercises = buildWorkoutExercisePayloads(validExercises, forceCompleted = true)
                    )
                    try {
                        apiClient.startWorkoutRemote(
                            StartWorkoutRequestDto(
                                id = finishedWorkout.id,
                                routineId = finishedWorkout.routineId?.takeIf { !it.startsWith("prebuilt-") },
                                name = finishedWorkout.name,
                                replaceActive = true,
                                exercises = buildWorkoutExercisePayloads(validExercises, forceCompleted = false)
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
        activeWorkoutSyncJob?.cancel()
        pendingLocalWorkoutId = null
        val current = activeWorkoutFlow.value
        activeWorkoutFlow.value = null
        isActiveWorkoutSheetOpenFlow.value = false
        DudaLocalStorage.setActiveWorkoutDraft(null)
        DudaLocalStorage.setRestTimerState(null)
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.REST)
        publishLiveWorkoutWidget(null)
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
        DudaLocalStorage.setWorkoutHistory(list)
        return list
    }

    override suspend fun deleteWorkoutFromHistory(workoutId: String) {
        apiClient?.deleteWorkout(workoutId)
        val filtered = workoutHistoryFlow.value.filter { it.id != workoutId }
        workoutHistoryFlow.value = filtered
        DudaLocalStorage.setWorkoutHistory(filtered)
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

    private fun applyCustomProgramOverride(program: TrainingProgram): TrainingProgram {
        val customList = DudaLocalStorage.getCustomPrograms()
        val activeProg = activeProgramBundleFlow.value?.program
        val match = customList.find { it.id == program.id }
            ?: (if (activeProg != null && (activeProg.id == program.id || activeProg.name.equals(program.name, ignoreCase = true))) activeProg else null)
            ?: customList.find { it.name.equals(program.name, ignoreCase = true) }
        return if (match != null && match.days.isNotEmpty()) {
            program.copy(days = match.days)
        } else {
            program
        }
    }

    private fun persistCustomProgramOverride(program: TrainingProgram) {
        val current = DudaLocalStorage.getCustomPrograms()
        val updated = current.filterNot { it.id == program.id || it.name.equals(program.name, ignoreCase = true) } + program
        DudaLocalStorage.setCustomPrograms(updated)
    }

    private fun syncRoutineFromWorkout(workout: Workout) {
        val rawRoutineId = workout.routineId ?: return
        if (workout.exercises.isEmpty()) return

        val resolvedExercises = workout.exercises.mapIndexed { idx, ex ->
            val workingSets = ex.sets.filter { it.setType != "warmup" }
            val targetSets = (if (workingSets.isNotEmpty()) workingSets.size else ex.sets.size).coerceAtLeast(1)
            val repVals = (if (workingSets.isNotEmpty()) workingSets else ex.sets).map { it.reps }.filter { it > 0 }
            val minReps = repVals.minOrNull() ?: 8
            val maxReps = (repVals.maxOrNull() ?: 12).coerceAtLeast(minReps)
            RoutineExercise(
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseName,
                orderIndex = idx,
                targetSets = targetSets,
                targetRepsMin = minReps,
                targetRepsMax = maxReps
            )
        }

        if (apiClient == null || isGuestUser()) {
            val base = normalizeRoutineBaseName(workout.name)
            val existing = userRoutinesFlow.value.firstOrNull {
                it.id == (routineForkAliases[rawRoutineId] ?: rawRoutineId) ||
                    (it.userId != "00000000-0000-0000-0000-000000000000" &&
                        normalizeRoutineBaseName(it.name) == base)
            }
            val saved = if (existing != null) {
                existing.copy(name = existing.name, exercises = resolvedExercises)
            } else {
                Routine(
                    id = PlatformBridge.generateUuid(),
                    name = workout.name.ifBlank { "My Routine" },
                    exercises = resolvedExercises
                )
            }
            if (saved.id != rawRoutineId) {
                routineForkAliases[rawRoutineId] = saved.id
                val cur = activeWorkoutFlow.value
                if (cur != null && cur.id == workout.id && cur.routineId != saved.id) {
                    val rebound = cur.copy(routineId = saved.id)
                    activeWorkoutFlow.value = rebound
                    DudaLocalStorage.setActiveWorkoutDraft(rebound)
                }
            }
            val nextList = deduplicatePreferringPersonal(
                if (userRoutinesFlow.value.any { it.id == saved.id }) {
                    userRoutinesFlow.value.map { if (it.id == saved.id) saved else it }
                } else {
                    listOf(saved) + userRoutinesFlow.value
                }
            )
            userRoutinesFlow.value = nextList
            DudaLocalStorage.setUserRoutines(nextList)
            return
        }

        scope.launch {
            templateSyncMutex.withLock {
                try {
                    val effectiveId = routineForkAliases[rawRoutineId] ?: rawRoutineId
                    val saved = saveRoutineFull(
                        routineId = effectiveId,
                        name = workout.name,
                        notes = null,
                        exercises = resolvedExercises
                    )
                    if (saved.id != rawRoutineId) {
                        routineForkAliases[rawRoutineId] = saved.id
                    }
                    val cur = activeWorkoutFlow.value
                    if (cur != null && cur.id == workout.id && cur.routineId != saved.id) {
                        val rebound = cur.copy(routineId = saved.id)
                        activeWorkoutFlow.value = rebound
                        DudaLocalStorage.setActiveWorkoutDraft(rebound)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    private fun syncProgramFromWorkout(workout: Workout, syncRemote: Boolean) {
        if (workout.exercises.isEmpty()) return

        val activeBundle = activeProgramBundleFlow.value
        val activeProg = activeBundle?.program
        val candidateProgId = workout.programId ?: workout.routineId

        val targetProgram: TrainingProgram = when {
            candidateProgId != null && activeProg?.id == candidateProgId -> activeProg
            candidateProgId != null -> {
                programsFlow.value.find { it.id == candidateProgId }
                    ?: installedProgramsFlow.value.mapNotNull { it.program }.find { it.id == candidateProgId }
                    ?: DudaLocalStorage.getCustomPrograms().find { it.id == candidateProgId }
            }
            activeProg != null && workout.name.startsWith(activeProg.name, ignoreCase = true) -> activeProg
            else -> null
        } ?: return

        if (targetProgram.days.isEmpty()) return

        val dayIndex = when {
            workout.programDayNumber != null -> {
                val byNum = targetProgram.days.indexOfFirst { it.dayNumber == workout.programDayNumber }
                if (byNum >= 0) byNum else (workout.programDayNumber - 1).coerceIn(0, targetProgram.days.lastIndex)
            }
            else -> {
                val byTitle = targetProgram.days.indexOfFirst { d ->
                    d.name.isNotBlank() && workout.name.endsWith(d.name, ignoreCase = true)
                }
                if (byTitle >= 0) byTitle else 0
            }
        }

        val existingDay = targetProgram.days[dayIndex]
        val updatedDayExercises = workout.exercises.mapIndexed { idx, ex ->
            val prevMatch = existingDay.exercises.find {
                it.exerciseId == ex.exerciseId || it.exerciseName.equals(ex.exerciseName, ignoreCase = true)
            }
            val workingSets = ex.sets.filter { it.setType != "warmup" }
            val targetSets = (if (workingSets.isNotEmpty()) workingSets.size else ex.sets.size).coerceAtLeast(1)
            val repVals = (if (workingSets.isNotEmpty()) workingSets else ex.sets).map { it.reps }.filter { it > 0 }
            val minReps = repVals.minOrNull() ?: prevMatch?.targetRepsMin ?: 8
            val maxReps = (repVals.maxOrNull() ?: prevMatch?.targetRepsMax ?: 12).coerceAtLeast(minReps)
            ProgramDayExercise(
                id = prevMatch?.id.orEmpty(),
                exerciseId = ex.exerciseId,
                exerciseName = ex.exerciseName,
                orderIndex = idx,
                targetSets = targetSets,
                targetRepsMin = minReps,
                targetRepsMax = maxReps,
                targetReps = "$minReps-$maxReps"
            )
        }

        val updatedDays = targetProgram.days.mapIndexed { idx, d ->
            if (idx == dayIndex) d.copy(exercises = updatedDayExercises) else d
        }
        val updatedProgram = targetProgram.copy(days = updatedDays)

        persistCustomProgramOverride(updatedProgram)

        if (activeProg != null && (activeProg.id == targetProgram.id || activeProg.name.equals(targetProgram.name, ignoreCase = true))) {
            val newBundle = activeBundle.copy(program = updatedProgram)
            activeProgramBundleFlow.value = newBundle
            DudaLocalStorage.setActiveProgramBundle(newBundle)
        }

        installedProgramsFlow.value = installedProgramsFlow.value.map { b ->
            val p = b.program
            if (p != null && (p.id == targetProgram.id || p.name.equals(targetProgram.name, ignoreCase = true))) {
                b.copy(program = updatedProgram)
            } else b
        }

        programsFlow.value = programsFlow.value.map { p ->
            if (p.id == targetProgram.id || p.name.equals(targetProgram.name, ignoreCase = true)) {
                p.copy(days = updatedDays)
            } else p
        }

        if (syncRemote && apiClient != null && !isGuestUser()) {
            scope.launch {
                templateSyncMutex.withLock {
                    try {
                        val resolvedId = programForkAliases[targetProgram.id] ?: targetProgram.id
                        val saved = apiClient.updateProgram(
                            programId = resolvedId,
                            request = SaveProgramRequestDto(
                                name = updatedProgram.name,
                                description = updatedProgram.description,
                                splitType = updatedProgram.splitType,
                                daysPerWeek = updatedProgram.daysPerWeek.coerceAtLeast(updatedDays.size),
                                level = updatedProgram.level,
                                durationWeeks = updatedProgram.durationWeeks,
                                isPublic = updatedProgram.isPublic,
                                days = updatedDays
                            )
                        )
                        if (saved.id != resolvedId) {
                            programForkAliases[targetProgram.id] = saved.id
                            programForkAliases[resolvedId] = saved.id
                        }
                        persistCustomProgramOverride(saved)
                        val curActive = activeProgramBundleFlow.value
                        if (curActive?.program != null &&
                            (curActive.program.id == targetProgram.id || curActive.program.name.equals(saved.name, ignoreCase = true))
                        ) {
                            val updatedBundle = curActive.copy(
                                program = saved.copy(isActive = true),
                                userProgram = curActive.userProgram?.copy(programId = saved.id)
                            )
                            activeProgramBundleFlow.value = updatedBundle
                            DudaLocalStorage.setActiveProgramBundle(updatedBundle)
                        }
                        val curWorkout = activeWorkoutFlow.value
                        if (curWorkout != null && (curWorkout.programId == targetProgram.id || curWorkout.programId == resolvedId) && saved.id != curWorkout.programId) {
                            val rebound = curWorkout.copy(programId = saved.id)
                            activeWorkoutFlow.value = rebound
                            DudaLocalStorage.setActiveWorkoutDraft(rebound)
                        }
                    } catch (_: Exception) {}
                }
            }
        }
    }

    override fun getPrograms(): Flow<List<TrainingProgram>> = programsFlow.asStateFlow()

    override fun getActiveProgramBundle(): Flow<ActiveProgramBundle?> = activeProgramBundleFlow.asStateFlow()

    override fun getInstalledProgramsFlow(): Flow<List<ActiveProgramBundle>> = installedProgramsFlow.asStateFlow()

    override suspend fun refreshPrograms(tab: String, splitType: String?, search: String?): List<TrainingProgram> {
        val client = apiClient ?: return programsFlow.value
        val activeProgId = activeProgramBundleFlow.value?.program?.id
        val list = try {
            client.getPrograms(tab = tab, splitType = splitType, search = search).map { p ->
                val isAct = activeProgId != null && p.id == activeProgId
                p.copy(isActive = isAct)
            }
        } catch (_: Exception) {
            programsFlow.value.ifEmpty { DudaLocalStorage.getCustomPrograms() }
        }
        programsFlow.value = list
        return list
    }

    override suspend fun refreshActiveProgram(): ActiveProgramBundle? {
        val client = apiClient ?: return activeProgramBundleFlow.value
        var fromServer = false
        val active = try {
            fromServer = true
            client.getActiveProgram()
        } catch (_: Exception) {
            fromServer = false
            activeProgramBundleFlow.value ?: DudaLocalStorage.getActiveProgramBundle()
        }
        val effectiveActive = active?.let { bundle ->
            val prog = bundle.program?.let { p ->
                if (fromServer) p.copy(isActive = true) else applyCustomProgramOverride(p).copy(isActive = true)
            }
            bundle.copy(program = prog)
        }
        activeProgramBundleFlow.value = effectiveActive
        if (effectiveActive != null) {
            DudaLocalStorage.setActiveProgramBundle(effectiveActive)
            effectiveActive.program?.let { persistCustomProgramOverride(it) }
        } else if (fromServer) {
            DudaLocalStorage.setActiveProgramBundle(null)
        }
        val activeId = effectiveActive?.program?.id
        programsFlow.value = programsFlow.value.map {
            val isAct = it.id == activeId
            val updated = if (isAct && effectiveActive?.program != null) effectiveActive.program else it
            updated.copy(isActive = isAct)
        }
        return effectiveActive
    }

    override suspend fun refreshInstalledPrograms(): List<ActiveProgramBundle> {
        val client = apiClient ?: return installedProgramsFlow.value
        val installed = try {
            val remote = client.getInstalledPrograms()
            remote.forEach { it.program?.let(::persistCustomProgramOverride) }
            remote
        } catch (_: Exception) {
            installedProgramsFlow.value
        }
        installedProgramsFlow.value = installed
        return installed
    }

    override suspend fun activateProgram(programId: String) {
        val localTarget = programsFlow.value.find { it.id == programId }
            ?: DudaLocalStorage.getCustomPrograms().find { it.id == programId }

        if (localTarget != null) {
            val activeCopy = applyCustomProgramOverride(localTarget).copy(isActive = true)
            val bundle = ActiveProgramBundle(
                userProgramId = "up-${activeCopy.id}",
                currentDayNumber = 1,
                isActive = true,
                program = activeCopy
            )
            activeProgramBundleFlow.value = bundle
            DudaLocalStorage.setActiveProgramBundle(bundle)
            programsFlow.value = programsFlow.value.map { it.copy(isActive = it.id == programId) }
        }

        if (apiClient != null && !isGuestUser()) {
            try {
                apiClient.installProgram(programId, setActive = true)
                refreshActiveProgram()
                refreshInstalledPrograms()
            } catch (_: Exception) {}
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
        val applyLocalToggle = {
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
        if (apiClient != null && !isGuestUser()) {
            try {
                val res = apiClient.toggleLikeProgram(programId)
                programsFlow.value = programsFlow.value.map { p ->
                    if (p.id == programId) {
                        p.copy(
                            isLiked = res.hasLiked,
                            likesCount = res.likesCount ?: if (res.hasLiked) p.likesCount + 1 else (p.likesCount - 1).coerceAtLeast(0)
                        )
                    } else p
                }
            } catch (_: Exception) {
                applyLocalToggle()
            }
        } else {
            applyLocalToggle()
        }
    }

    override fun getProgramComments(programId: String): Flow<List<ProgramComment>> {
        val flow = programCommentsMap.getOrPut(programId) {
            MutableStateFlow(DudaLocalStorage.getProgramComments(programId).orEmpty())
        }
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
        val flow = programCommentsMap.getOrPut(programId) {
            MutableStateFlow(DudaLocalStorage.getProgramComments(programId).orEmpty())
        }
        val client = apiClient ?: return flow.value
        return try {
            val rawComments = client.getProgramComments(programId)
            val rootComments = rawComments.filter { it.parentId.isNullOrBlank() }.map { root ->
                val replies = rawComments.filter { it.parentId == root.id }
                root.copy(replies = replies)
            }
            val localCached = DudaLocalStorage.getProgramComments(programId).orEmpty()
            val remoteIds = rootComments.map { it.id }.toSet()
            val merged = rootComments + localCached.filter { it.id !in remoteIds }
            flow.value = merged
            DudaLocalStorage.setProgramComments(programId, merged)
            merged
        } catch (_: Exception) {
            flow.value
        }
    }

    override suspend fun addProgramComment(programId: String, content: String, parentId: String?): ProgramComment {
        val user = currentUserFlow.value
        val flow = programCommentsMap.getOrPut(programId) {
            MutableStateFlow(DudaLocalStorage.getProgramComments(programId).orEmpty())
        }
        val addLocalFallback = {
            val newComment = ProgramComment(
                id = PlatformBridge.generateUuid(),
                programId = programId,
                userId = user?.id ?: "u-1",
                authorName = user?.name ?: "Атлет",
                avatarUrl = user?.avatarUrl,
                content = content.trim(),
                parentId = parentId
            )
            val updated = if (parentId == null) {
                listOf(newComment) + flow.value
            } else {
                flow.value.map { c ->
                    if (c.id == parentId) c.copy(replies = c.replies + newComment) else c
                }
            }
            flow.value = updated
            DudaLocalStorage.setProgramComments(programId, updated)
            programsFlow.value = programsFlow.value.map { p ->
                if (p.id == programId) p.copy(commentsCount = p.commentsCount + 1) else p
            }
            activeProgramBundleFlow.value?.let { b ->
                if (b.program?.id == programId) {
                    activeProgramBundleFlow.value = b.copy(
                        program = b.program.copy(commentsCount = b.program.commentsCount + 1)
                    )
                }
            }
            newComment
        }

        return if (apiClient != null && !isGuestUser()) {
            try {
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
                activeProgramBundleFlow.value?.let { b ->
                    if (b.program?.id == programId) {
                        activeProgramBundleFlow.value = b.copy(
                            program = b.program.copy(commentsCount = b.program.commentsCount + 1)
                        )
                    }
                }
                created
            } catch (_: Exception) {
                addLocalFallback()
            }
        } else {
            addLocalFallback()
        }
    }

    override suspend fun upvoteProgramComment(programId: String, commentId: String) {
        try {
            apiClient?.upvoteProgramComment(programId, commentId)
            refreshProgramComments(programId)
        } catch (_: Exception) {
            val flow = programCommentsMap.getOrPut(programId) {
                MutableStateFlow(DudaLocalStorage.getProgramComments(programId).orEmpty())
            }
            val updated = flow.value.map { c ->
                if (c.id == commentId) {
                    val next = !c.hasUpvoted
                    c.copy(
                        hasUpvoted = next,
                        upvotes = if (next) c.upvotes + 1 else (c.upvotes - 1).coerceAtLeast(0)
                    )
                } else {
                    c.copy(
                        replies = c.replies.map { r ->
                            if (r.id == commentId) {
                                val next = !r.hasUpvoted
                                r.copy(
                                    hasUpvoted = next,
                                    upvotes = if (next) r.upvotes + 1 else (r.upvotes - 1).coerceAtLeast(0)
                                )
                            } else r
                        }
                    )
                }
            }
            flow.value = updated
            DudaLocalStorage.setProgramComments(programId, updated)
        }
    }

    override suspend fun deleteProgramComment(programId: String, commentId: String) {
        try {
            apiClient?.deleteProgramComment(programId, commentId)
            refreshProgramComments(programId)
        } catch (_: Exception) {
            val flow = programCommentsMap.getOrPut(programId) {
                MutableStateFlow(DudaLocalStorage.getProgramComments(programId).orEmpty())
            }
            val updated = flow.value.filterNot { it.id == commentId }.map { c ->
                c.copy(replies = c.replies.filterNot { it.id == commentId })
            }
            flow.value = updated
            DudaLocalStorage.setProgramComments(programId, updated)
        }
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
        val flow = todosMapByDate.getOrPut(date) {
            MutableStateFlow(DudaLocalStorage.getTodos(date).orEmpty())
        }
        if (apiClient != null && !isGuestUser()) {
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
        if (client != null && !isGuestUser()) {
            val schedule = client.getDailySchedule(date)
            dailyScheduleFlow.value = schedule
            val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(emptyList()) }
            flow.value = schedule.todos
            DudaLocalStorage.setTodos(date, schedule.todos)
            return schedule
        } else {
            val tasks = todosMapByDate[date]?.value ?: DudaLocalStorage.getTodos(date).orEmpty()
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
        if (apiClient != null && !isGuestUser()) {
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
            val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(DudaLocalStorage.getTodos(date).orEmpty()) }
            val updated = flow.value + task.copy(scheduledDate = date)
            flow.value = updated
            DudaLocalStorage.setTodos(date, updated)
        }
    }

    override suspend fun saveTodoFull(
        todoId: String?,
        request: SaveTodoRequestDto,
        refreshDate: String
    ): TodoTask {
        val client = apiClient
        if (client != null && !isGuestUser()) {
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
            val flow = todosMapByDate.getOrPut(refreshDate) { MutableStateFlow(DudaLocalStorage.getTodos(refreshDate).orEmpty()) }
            val updated = flow.value.filter { it.id != item.id } + item
            flow.value = updated
            DudaLocalStorage.setTodos(refreshDate, updated)
            return item
        }
    }

    override suspend fun toggleTodo(taskId: String, date: String) {
        if (apiClient != null && !isGuestUser()) {
            apiClient.toggleTodo(taskId, date)
            fetchDailySchedule(date)
        } else {
            val flow = todosMapByDate.getOrPut(date) { MutableStateFlow(DudaLocalStorage.getTodos(date).orEmpty()) }
            val updated = flow.value.map { t ->
                if (t.id == taskId) {
                    val nextStatus = if (t.status == "completed") "pending" else "completed"
                    t.copy(status = nextStatus)
                } else t
            }
            flow.value = updated
            DudaLocalStorage.setTodos(date, updated)
        }
    }

    override suspend fun deleteTodo(taskId: String) {
        val currentDate = dailyScheduleFlow.value?.date
            ?: PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        if (apiClient != null && !isGuestUser()) {
            apiClient.deleteTodo(taskId)
            fetchDailySchedule(currentDate)
        } else {
            todosMapByDate.forEach { (d, flow) ->
                val filtered = flow.value.filter { it.id != taskId }
                flow.value = filtered
                DudaLocalStorage.setTodos(d, filtered)
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

    private fun normalizeFocusMode(mode: String): String = when (mode.lowercase()) {
        "timer", "target_timer" -> "target_timer"
        "stopwatch" -> "stopwatch"
        else -> "pomodoro"
    }

    private fun normalizeFocusPhase(phase: String): String = when (phase.lowercase()) {
        "short_break" -> "short_break"
        "long_break" -> "long_break"
        else -> "work"
    }

    private fun durationForCurrentPhase(state: FocusTimerState): Int = when (normalizeFocusMode(state.mode)) {
        "stopwatch" -> 0
        "target_timer" -> (state.targetTotalMinutes * 60).coerceAtLeast(60)
        else -> when (normalizeFocusPhase(state.pomodoroPhase)) {
            "short_break" -> state.shortBreakDurationSec.coerceAtLeast(60)
            "long_break" -> state.longBreakDurationSec.coerceAtLeast(60)
            else -> state.roundDurationsSec.getOrElse(state.pomodoroRound - 1) { state.workDurationSec }.coerceAtLeast(60)
        }
    }

    private fun currentFocusStrings() = getDudaStrings(preferencesFlow.value.languageCode)

    private fun scheduleOrCancelFocusNotification(state: FocusTimerState) {
        val endMs = state.phaseEndEpochMs
        if (!state.isActive || !state.isRunning || normalizeFocusMode(state.mode) == "stopwatch" || endMs == null || endMs <= nowProvider()) {
            PlatformBridge.cancelTimerNotification(TimerNotificationIds.FOCUS)
            return
        }
        val strings = currentFocusStrings()
        val isWork = normalizeFocusMode(state.mode) != "pomodoro" || normalizeFocusPhase(state.pomodoroPhase) == "work"
        val alertTitle = if (isWork) strings.focusWorkDoneTitle else strings.focusBreakDoneTitle
        val prefix = state.taskTitle.trim().takeIf { it.isNotEmpty() }?.let { "$it — " } ?: ""
        val alertBody = prefix + if (isWork) strings.focusWorkDoneBody else strings.focusBreakDoneBody
        PlatformBridge.requestNotificationPermission()
        PlatformBridge.scheduleTimerNotification(
            timerId = TimerNotificationIds.FOCUS,
            triggerAtEpochMs = endMs,
            title = alertTitle,
            body = alertBody,
            showOngoingCountdown = true,
            ongoingTitle = strings.focusOngoingTitle,
            ongoingBody = state.taskTitle.ifBlank { strings.focusSessionDefault }
        )
    }

    private fun applyAndPersistFocusState(next: FocusTimerState) {
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
        scheduleOrCancelFocusNotification(next)
    }

    override fun startFocusForTodo(todo: TodoTask, strategy: String, mode: String) {
        val totalMin = max(1, todo.durationMinutes)
        val normalizedMode = normalizeFocusMode(mode)
        val rounds = calculatePomodoroRounds(totalMin, strategy)
        val firstRoundSec = rounds.firstOrNull() ?: (25 * 60)
        val initialRem = when (normalizedMode) {
            "stopwatch" -> 0
            "target_timer" -> totalMin * 60
            else -> firstRoundSec
        }
        val now = nowProvider()
        val phaseEnd = if (normalizedMode == "stopwatch" || initialRem <= 0) null else now + initialRem * 1000L
        val next = FocusTimerState(
            isActive = true,
            isRunning = true,
            todoId = todo.id,
            taskTitle = todo.title,
            category = todo.category,
            mode = normalizedMode,
            pomodoroPhase = "work",
            pomodoroRound = 1,
            totalPomodoroRounds = rounds.size,
            targetTotalMinutes = totalMin,
            splitStrategy = strategy,
            roundDurationsSec = rounds,
            workDurationSec = firstRoundSec,
            secondsRemaining = initialRem,
            secondsElapsedTotal = 0,
            isModalOpen = true,
            phaseEndEpochMs = phaseEnd,
            runningSinceEpochMs = now,
            elapsedBeforePauseSec = 0,
            soundEnabled = focusTimerFlow.value.soundEnabled,
            alarmPreset = DudaLocalStorage.getFocusAlarmPreset()
        )
        applyAndPersistFocusState(next)
    }

    override fun startQuickFocusSession(
        title: String,
        category: String,
        targetMinutes: Int,
        strategy: String,
        mode: String
    ) {
        val totalMin = max(1, targetMinutes)
        val normalizedMode = normalizeFocusMode(mode)
        val rounds = calculatePomodoroRounds(totalMin, strategy)
        val firstRoundSec = rounds.firstOrNull() ?: (25 * 60)
        val initialRem = when (normalizedMode) {
            "stopwatch" -> 0
            "target_timer" -> totalMin * 60
            else -> firstRoundSec
        }
        val now = nowProvider()
        val phaseEnd = if (normalizedMode == "stopwatch" || initialRem <= 0) null else now + initialRem * 1000L
        val next = FocusTimerState(
            isActive = true,
            isRunning = true,
            todoId = null,
            taskTitle = title.ifBlank { currentFocusStrings().focusSessionDefault },
            category = category,
            mode = normalizedMode,
            pomodoroPhase = "work",
            pomodoroRound = 1,
            totalPomodoroRounds = rounds.size,
            targetTotalMinutes = totalMin,
            splitStrategy = strategy,
            roundDurationsSec = rounds,
            workDurationSec = firstRoundSec,
            secondsRemaining = initialRem,
            secondsElapsedTotal = 0,
            isModalOpen = true,
            phaseEndEpochMs = phaseEnd,
            runningSinceEpochMs = now,
            elapsedBeforePauseSec = 0,
            soundEnabled = focusTimerFlow.value.soundEnabled,
            alarmPreset = DudaLocalStorage.getFocusAlarmPreset()
        )
        applyAndPersistFocusState(next)
    }

    override fun pauseFocusTimer() {
        PlatformBridge.stopFocusAlarm()
        syncFocusTimerFromClock()
        val st = focusTimerFlow.value
        val next = st.copy(
            isRunning = false,
            phaseEndEpochMs = null,
            runningSinceEpochMs = null,
            elapsedBeforePauseSec = st.secondsElapsedTotal
        )
        applyAndPersistFocusState(next)
    }

    override fun resumeFocusTimer() {
        val st = focusTimerFlow.value
        val mode = normalizeFocusMode(st.mode)
        val rem = if (mode != "stopwatch" && st.secondsRemaining <= 0) durationForCurrentPhase(st) else st.secondsRemaining
        val now = nowProvider()
        val phaseEnd = if (mode == "stopwatch" || rem <= 0) null else now + rem * 1000L
        val next = st.copy(
            isActive = true,
            isRunning = true,
            mode = mode,
            secondsRemaining = rem,
            phaseEndEpochMs = phaseEnd,
            runningSinceEpochMs = now,
            elapsedBeforePauseSec = st.secondsElapsedTotal
        )
        applyAndPersistFocusState(next)
    }

    override fun syncFocusTimerFromClock() {
        tickFocusTimer()
    }

    override fun tickFocusTimer() {
        val st = focusTimerFlow.value
        if (!st.isActive || !st.isRunning) return

        val now = nowProvider()
        val mode = normalizeFocusMode(st.mode)

        if (mode == "stopwatch") {
            val since = st.runningSinceEpochMs ?: now
            val elapsedDelta = ((now - since) / 1000L).toInt().coerceAtLeast(0)
            val total = st.elapsedBeforePauseSec + elapsedDelta
            if (total != st.secondsElapsedTotal || st.runningSinceEpochMs == null || st.mode != mode) {
                val updated = st.copy(
                    mode = mode,
                    secondsElapsedTotal = total,
                    runningSinceEpochMs = since,
                    phaseEndEpochMs = null
                )
                focusTimerFlow.value = updated
                if (st.runningSinceEpochMs == null) {
                    DudaLocalStorage.setFocusTimerState(updated)
                }
            }
            return
        }

        var current = st.copy(mode = mode, pomodoroPhase = normalizeFocusPhase(st.pomodoroPhase))
        var since = current.runningSinceEpochMs ?: now
        var phaseEnd = current.phaseEndEpochMs ?: (now + current.secondsRemaining.coerceAtLeast(0) * 1000L)
        var crossedPhase = false

        repeat(64) {
            val nextRem = ceil((phaseEnd - now) / 1000.0).toInt().coerceAtLeast(0)
            val isWork = current.mode != "pomodoro" || current.pomodoroPhase == "work"

            if (nextRem > 0) {
                val elapsedSinceStart = ((now - since) / 1000L).toInt().coerceAtLeast(0)
                val nextElapsed = if (isWork) current.elapsedBeforePauseSec + elapsedSinceStart else current.secondsElapsedTotal
                val updated = current.copy(
                    secondsRemaining = nextRem,
                    secondsElapsedTotal = nextElapsed,
                    phaseEndEpochMs = phaseEnd,
                    runningSinceEpochMs = since
                )
                if (updated != focusTimerFlow.value) {
                    focusTimerFlow.value = updated
                    if (crossedPhase || st.phaseEndEpochMs == null || st.runningSinceEpochMs == null) {
                        DudaLocalStorage.setFocusTimerState(updated)
                        scheduleOrCancelFocusNotification(updated)
                    }
                }
                return
            }

            // Phase finished at `phaseEnd`
            val fullPhaseSec = ((phaseEnd - since) / 1000L).toInt().coerceAtLeast(0)
            val completedElapsed = if (isWork) current.elapsedBeforePauseSec + fullPhaseSec else current.secondsElapsedTotal
            val justCompletedInForeground = (now - phaseEnd) <= 3000L

            PlatformBridge.cancelTimerNotification(TimerNotificationIds.FOCUS)
            if (justCompletedInForeground) {
                if (current.soundEnabled) {
                    AthleteSoundManager.playFocusAlarm(current.alarmPreset)
                } else {
                    PlatformBridge.vibrate()
                }
                val strings = currentFocusStrings()
                PlatformBridge.showToast(if (isWork) strings.focusWorkDoneTitle else strings.focusBreakDoneTitle)
            }

            if (current.mode == "target_timer" || (current.pomodoroPhase == "work" && current.pomodoroRound >= current.totalPomodoroRounds)) {
                val finished = current.copy(
                    secondsRemaining = 0,
                    secondsElapsedTotal = completedElapsed,
                    elapsedBeforePauseSec = completedElapsed,
                    isRunning = false,
                    phaseEndEpochMs = null,
                    runningSinceEpochMs = null
                )
                applyAndPersistFocusState(finished)
                return
            }

            crossedPhase = true
            if (current.pomodoroPhase == "work") {
                val isLong = current.pomodoroRound % 4 == 0
                val nextPhase = if (isLong) "long_break" else "short_break"
                val breakDur = if (isLong) current.longBreakDurationSec else current.shortBreakDurationSec
                since = phaseEnd
                phaseEnd = phaseEnd + breakDur * 1000L
                current = current.copy(
                    pomodoroPhase = nextPhase,
                    secondsRemaining = breakDur,
                    secondsElapsedTotal = completedElapsed,
                    elapsedBeforePauseSec = completedElapsed,
                    phaseEndEpochMs = phaseEnd,
                    runningSinceEpochMs = since
                )
            } else {
                val nextRound = (current.pomodoroRound + 1).coerceAtMost(current.totalPomodoroRounds)
                val roundDur = current.roundDurationsSec.getOrElse(nextRound - 1) { current.workDurationSec }
                since = phaseEnd
                phaseEnd = phaseEnd + roundDur * 1000L
                current = current.copy(
                    pomodoroPhase = "work",
                    pomodoroRound = nextRound,
                    workDurationSec = roundDur,
                    secondsRemaining = roundDur,
                    secondsElapsedTotal = completedElapsed,
                    elapsedBeforePauseSec = completedElapsed,
                    phaseEndEpochMs = phaseEnd,
                    runningSinceEpochMs = since
                )
            }
        }

        val cappedFinished = current.copy(
            secondsRemaining = 0,
            isRunning = false,
            phaseEndEpochMs = null,
            runningSinceEpochMs = null,
            elapsedBeforePauseSec = current.secondsElapsedTotal
        )
        applyAndPersistFocusState(cappedFinished)
    }

    override fun skipFocusPhase() {
        syncFocusTimerFromClock()
        val st = focusTimerFlow.value
        if (normalizeFocusMode(st.mode) != "pomodoro") return
        val now = nowProvider()
        if (normalizeFocusPhase(st.pomodoroPhase) == "work") {
            if (st.pomodoroRound >= st.totalPomodoroRounds) {
                val next = st.copy(
                    secondsRemaining = 0,
                    isRunning = false,
                    phaseEndEpochMs = null,
                    runningSinceEpochMs = null,
                    elapsedBeforePauseSec = st.secondsElapsedTotal
                )
                applyAndPersistFocusState(next)
            } else {
                val isLong = st.pomodoroRound % 4 == 0
                val nextPhase = if (isLong) "long_break" else "short_break"
                val breakDur = if (isLong) st.longBreakDurationSec else st.shortBreakDurationSec
                val next = st.copy(
                    pomodoroPhase = nextPhase,
                    secondsRemaining = breakDur,
                    elapsedBeforePauseSec = st.secondsElapsedTotal,
                    runningSinceEpochMs = if (st.isRunning) now else null,
                    phaseEndEpochMs = if (st.isRunning) now + breakDur * 1000L else null
                )
                applyAndPersistFocusState(next)
            }
        } else {
            val nextRound = (st.pomodoroRound + 1).coerceAtMost(st.totalPomodoroRounds)
            val roundDur = st.roundDurationsSec.getOrElse(nextRound - 1) { st.workDurationSec }
            val next = st.copy(
                pomodoroPhase = "work",
                pomodoroRound = nextRound,
                workDurationSec = roundDur,
                secondsRemaining = roundDur,
                elapsedBeforePauseSec = st.secondsElapsedTotal,
                runningSinceEpochMs = if (st.isRunning) now else null,
                phaseEndEpochMs = if (st.isRunning) now + roundDur * 1000L else null
            )
            applyAndPersistFocusState(next)
        }
    }

    override fun resetFocusPhase() {
        val st = focusTimerFlow.value
        val dur = durationForCurrentPhase(st)
        val next = st.copy(
            isRunning = false,
            secondsRemaining = dur,
            secondsElapsedTotal = if (normalizeFocusMode(st.mode) == "stopwatch") 0 else st.secondsElapsedTotal,
            phaseEndEpochMs = null,
            runningSinceEpochMs = null,
            elapsedBeforePauseSec = if (normalizeFocusMode(st.mode) == "stopwatch") 0 else st.secondsElapsedTotal
        )
        applyAndPersistFocusState(next)
    }

    override fun setFocusMode(mode: String) {
        val st = focusTimerFlow.value
        val normalizedMode = normalizeFocusMode(mode)
        val rem = when (normalizedMode) {
            "stopwatch" -> 0
            "target_timer" -> st.targetTotalMinutes * 60
            else -> st.workDurationSec
        }
        val next = st.copy(
            mode = normalizedMode,
            pomodoroPhase = "work",
            secondsRemaining = rem,
            isRunning = false,
            phaseEndEpochMs = null,
            runningSinceEpochMs = null,
            secondsElapsedTotal = if (normalizedMode == "stopwatch") 0 else st.secondsElapsedTotal,
            elapsedBeforePauseSec = if (normalizedMode == "stopwatch") 0 else st.secondsElapsedTotal
        )
        applyAndPersistFocusState(next)
    }

    override fun setFocusPhase(phase: String, durationMinutes: Int?) {
        val st = focusTimerFlow.value
        val normalizedPhase = normalizeFocusPhase(phase)
        val explicitSec = durationMinutes?.let { max(1, it) * 60 }
        val nextWorkSec = if (normalizedPhase == "work" && explicitSec != null) explicitSec else st.workDurationSec
        val nextShortSec = if (normalizedPhase == "short_break" && explicitSec != null) explicitSec else st.shortBreakDurationSec
        val nextLongSec = if (normalizedPhase == "long_break" && explicitSec != null) explicitSec else st.longBreakDurationSec
        val rem = explicitSec ?: when (normalizedPhase) {
            "short_break" -> nextShortSec
            "long_break" -> nextLongSec
            else -> st.roundDurationsSec.getOrElse(st.pomodoroRound - 1) { nextWorkSec }
        }
        val next = st.copy(
            mode = "pomodoro",
            pomodoroPhase = normalizedPhase,
            workDurationSec = nextWorkSec,
            shortBreakDurationSec = nextShortSec,
            longBreakDurationSec = nextLongSec,
            secondsRemaining = rem,
            isRunning = false,
            phaseEndEpochMs = null,
            runningSinceEpochMs = null
        )
        applyAndPersistFocusState(next)
    }

    override fun setFocusTargetMinutes(minutes: Int) {
        val safeMin = max(1, minutes)
        val st = focusTimerFlow.value
        val next = st.copy(
            targetTotalMinutes = safeMin,
            secondsRemaining = if (normalizeFocusMode(st.mode) == "target_timer") safeMin * 60 else st.secondsRemaining,
            isRunning = false,
            phaseEndEpochMs = null,
            runningSinceEpochMs = null
        )
        applyAndPersistFocusState(next)
    }

    override fun setFocusSoundEnabled(enabled: Boolean) {
        val next = focusTimerFlow.value.copy(soundEnabled = enabled)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
        if (!enabled) {
            PlatformBridge.stopFocusAlarm()
        }
    }

    override fun setFocusAlarmPreset(preset: String) {
        DudaLocalStorage.setFocusAlarmPreset(preset)
        val normalized = DudaLocalStorage.getFocusAlarmPreset()
        val next = focusTimerFlow.value.copy(alarmPreset = normalized)
        focusTimerFlow.value = next
        if (next.isActive) {
            DudaLocalStorage.setFocusTimerState(next)
        }
    }

    override fun previewFocusAlarm(preset: String) {
        setFocusAlarmPreset(preset)
        AthleteSoundManager.playFocusAlarm(DudaLocalStorage.getFocusAlarmPreset(), preview = true)
    }

    override fun stopFocusAlarm() {
        PlatformBridge.stopFocusAlarm()
    }

    override fun observeFocusAlarmRinging(): Flow<Boolean> = focusAlarmRingingFlow.asStateFlow()

    override fun setFocusSplitStrategy(strategy: String) {
        val st = focusTimerFlow.value
        val rounds = calculatePomodoroRounds(st.targetTotalMinutes, strategy)
        val roundIdx = st.pomodoroRound.coerceAtMost(rounds.size)
        val roundSec = rounds.getOrElse(roundIdx - 1) { rounds.first() }
        val isWork = normalizeFocusPhase(st.pomodoroPhase) == "work"
        val rem = if (isWork) roundSec else st.secondsRemaining
        val now = nowProvider()
        val next = st.copy(
            splitStrategy = strategy,
            roundDurationsSec = rounds,
            totalPomodoroRounds = rounds.size,
            pomodoroRound = roundIdx,
            workDurationSec = roundSec,
            secondsRemaining = rem,
            runningSinceEpochMs = if (st.isRunning && isWork) now else st.runningSinceEpochMs,
            phaseEndEpochMs = if (st.isRunning && isWork) now + rem * 1000L else st.phaseEndEpochMs
        )
        applyAndPersistFocusState(next)
    }

    override fun setFocusBreakMinutes(minutes: Int) {
        val st = focusTimerFlow.value
        val sec = max(1, minutes) * 60
        val isBreak = normalizeFocusPhase(st.pomodoroPhase) == "short_break"
        val now = nowProvider()
        val next = st.copy(
            shortBreakDurationSec = sec,
            secondsRemaining = if (isBreak) sec else st.secondsRemaining,
            runningSinceEpochMs = if (st.isRunning && isBreak) now else st.runningSinceEpochMs,
            phaseEndEpochMs = if (st.isRunning && isBreak) now + sec * 1000L else st.phaseEndEpochMs
        )
        applyAndPersistFocusState(next)
    }

    override fun setFocusModalOpen(isOpen: Boolean) {
        val next = focusTimerFlow.value.copy(isModalOpen = isOpen)
        focusTimerFlow.value = next
        DudaLocalStorage.setFocusTimerState(next)
    }

    override suspend fun finishAndLogFocusSession(markCompleted: Boolean, notes: String) {
        syncFocusTimerFromClock()
        val st = focusTimerFlow.value
        if (!st.isActive) return
        val durationMins = max(1, (st.secondsElapsedTotal / 60.0).roundToInt())
        val today = dailyScheduleFlow.value?.date
            ?: PlatformBridge.formatDate(nowProvider(), "yyyy-MM-dd")

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
        PlatformBridge.stopFocusAlarm()
        PlatformBridge.cancelTimerNotification(TimerNotificationIds.FOCUS)
        val cleared = FocusTimerState(
            isActive = false,
            soundEnabled = focusTimerFlow.value.soundEnabled,
            alarmPreset = DudaLocalStorage.getFocusAlarmPreset()
        )
        focusTimerFlow.value = cleared
        DudaLocalStorage.setFocusTimerState(null)
    }

    // ========================================================================
    // 8. NUTRITION & AI FOOD ANALYZER
    // ========================================================================

    override fun getNutritionTodaySummary(): Flow<DailyNutritionSummaryDto?> = nutritionSummaryFlow.asStateFlow()

    private fun mealDate(meal: MealLog): String = meal.date.ifBlank {
        runCatching {
            kotlinx.datetime.Instant.parse(meal.loggedAt)
                .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault()).date.toString()
        }.getOrDefault("")
    }

    override fun getMealLogs(date: String): Flow<List<MealLog>> =
        mealLogsFlow.map { meals -> meals.filter { mealDate(it) == date } }

    private fun cachedNutritionSummary(date: String): DailyNutritionSummaryDto {
        val meals = mealLogsFlow.value.filter { mealDate(it) == date }
        val user = currentUserFlow.value
        return DailyNutritionSummaryDto(
            date = date,
            totalCalories = meals.sumOf { it.calories },
            totalProteinG = meals.sumOf { it.proteinG },
            totalCarbsG = meals.sumOf { it.carbsG },
            totalFatG = meals.sumOf { it.fatG },
            meals = meals
        ).let { summary ->
            summary.copy(
                targetCalories = user?.targetCalories ?: summary.targetCalories,
                targetProteinG = user?.targetProteinG?.toDouble() ?: summary.targetProteinG,
                targetCarbsG = user?.targetCarbsG?.toDouble() ?: summary.targetCarbsG,
                targetFatG = user?.targetFatG?.toDouble() ?: summary.targetFatG
            )
        }
    }

    override suspend fun refreshNutritionToday(): DailyNutritionSummaryDto =
        refreshDailyNutrition(PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd"))

    override suspend fun refreshDailyNutrition(date: String): DailyNutritionSummaryDto {
        val client = apiClient
        val user = currentUserFlow.value
        val summary = if (client != null && !isGuestUser()) {
            val remote = client.getNutritionToday(date)
            val datedMeals = remote.meals.map { it.copy(date = remote.date) }
            val receivedIds = datedMeals.map { it.id }.toSet()
            val updated = mealLogsFlow.value.filter { mealDate(it) != date && it.id !in receivedIds } + datedMeals
            mealLogsFlow.value = updated
            DudaLocalStorage.setMealLogs(updated)
            remote.copy(
                meals = datedMeals,
                targetCalories = user?.targetCalories ?: remote.targetCalories,
                targetProteinG = (user?.targetProteinG ?: remote.targetProteinG.toInt()).toDouble(),
                targetCarbsG = (user?.targetCarbsG ?: remote.targetCarbsG.toInt()).toDouble(),
                targetFatG = (user?.targetFatG ?: remote.targetFatG.toInt()).toDouble()
            )
        } else {
            cachedNutritionSummary(date)
        }
        nutritionSummaryFlow.value = summary
        return summary
    }

    override suspend fun addMealLog(meal: MealLog) {
        val savedMeal = if (apiClient != null && !isGuestUser()) {
            apiClient.saveMealLog(
                SaveMealRequestDto(
                    loggedAt = if (meal.date == PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")) {
                        kotlinx.datetime.Instant.fromEpochMilliseconds(PlatformBridge.currentTimeMillis()).toString()
                    } else {
                        kotlinx.datetime.LocalDate.parse(meal.date).atTime(12, 0)
                            .toInstant(kotlinx.datetime.TimeZone.currentSystemDefault()).toString()
                    },
                    mealType = meal.mealType,
                    name = meal.name,
                    calories = meal.calories,
                    proteinG = meal.proteinG,
                    carbsG = meal.carbsG,
                    fatG = meal.fatG,
                    items = meal.items
                )
            ).copy(date = meal.date)
        } else {
            meal
        }
        // The POST response is authoritative. A failed follow-up GET must not
        // make a successful save look failed and invite a duplicate submission.
        val updated = mealLogsFlow.value.filterNot { it.id == savedMeal.id } + savedMeal
        mealLogsFlow.value = updated
        DudaLocalStorage.setMealLogs(updated)
        nutritionSummaryFlow.value = cachedNutritionSummary(meal.date)
    }

    override suspend fun deleteMealLog(mealId: String) {
        val date = mealLogsFlow.value.find { it.id == mealId }?.let { mealDate(it) }
            ?: PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        if (apiClient != null && !isGuestUser()) {
            apiClient.deleteMealLog(mealId)
        }
        val filtered = mealLogsFlow.value.filter { it.id != mealId }
        mealLogsFlow.value = filtered
        DudaLocalStorage.setMealLogs(filtered)
        nutritionSummaryFlow.value = cachedNutritionSummary(date)
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
            val logs = client.getBodyLogs()
            bodyMeasurementsFlow.value = logs
            DudaLocalStorage.setBodyMeasurements(logs)
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
        return if (client != null && !isGuestUser()) {
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
            val updated = bodyMeasurementsFlow.value + m
            bodyMeasurementsFlow.value = updated
            DudaLocalStorage.setBodyMeasurements(updated)
            m
        }
    }

    override suspend fun deleteBodyLog(date: String) {
        apiClient?.deleteBodyLog(date)
        val filtered = bodyMeasurementsFlow.value.filter { it.logDate != date }
        bodyMeasurementsFlow.value = filtered
        DudaLocalStorage.setBodyMeasurements(filtered)
        if (apiClient != null && !isGuestUser()) {
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
        activeWorkoutFlow.value?.let { publishLiveWorkoutWidget(it) }
    }
}
