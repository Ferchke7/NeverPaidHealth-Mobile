package dev.neverpaid.health.core.network

import dev.neverpaid.health.core.error.AppError
import dev.neverpaid.health.core.error.AppErrorType
import dev.neverpaid.health.core.storage.DudaLocalStorage
import dev.neverpaid.health.domain.model.ActiveProgramBundle
import dev.neverpaid.health.domain.model.ActivityLogItem
import dev.neverpaid.health.domain.model.AnalyzePhotoRequestDto
import dev.neverpaid.health.domain.model.AthleteProfile
import dev.neverpaid.health.domain.model.AuthResponseDto
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.BodyTrendResponseDto
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachChatHistoryItemDto
import dev.neverpaid.health.domain.model.CoachChatRequestDto
import dev.neverpaid.health.domain.model.CoachChatResponseDto
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.CommentUpvoteResponseDto
import dev.neverpaid.health.domain.model.CreateFeedbackRequestDto
import dev.neverpaid.health.domain.model.DailyNutritionSummaryDto
import dev.neverpaid.health.domain.model.DailyScheduleResponseDto
import dev.neverpaid.health.domain.model.EstimateTextFoodRequestDto
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ExerciseHistoryResponseDto
import dev.neverpaid.health.domain.model.ExerciseRecordGroupDto
import dev.neverpaid.health.domain.model.FeedbackReport
import dev.neverpaid.health.domain.model.FinishWorkoutRequestDto
import dev.neverpaid.health.domain.model.GoogleAuthRequestDto
import dev.neverpaid.health.domain.model.InstallProgramRequestDto
import dev.neverpaid.health.domain.model.LikeStatusResponseDto
import dev.neverpaid.health.domain.model.LogFocusSessionRequestDto
import dev.neverpaid.health.domain.model.MealAnalysisResult
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.PlatformStats
import dev.neverpaid.health.domain.model.PostProgramCommentRequestDto
import dev.neverpaid.health.domain.model.PreviousPerformanceResponseDto
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.PublishProgramRequestDto
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.SaveBodyLogRequestDto
import dev.neverpaid.health.domain.model.SaveExerciseRequestDto
import dev.neverpaid.health.domain.model.SaveMealRequestDto
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.model.SaveTodoRequestDto
import dev.neverpaid.health.domain.model.SetActiveProgramRequestDto
import dev.neverpaid.health.domain.model.StartWorkoutRequestDto
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UpdateUnitPreferenceRequestDto
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.UserProgram
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutsListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ProblemDetailsDto(
    @SerialName("type") val type: String? = null,
    @SerialName("title") val title: String? = null,
    @SerialName("status") val status: Int? = null,
    @SerialName("detail") val detail: String? = null,
    @SerialName("error") val error: String? = null,
    @SerialName("message") val message: String? = null
)

class DudaApiClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
        explicitNulls = false
    }
) {
    companion object {
        const val PRODUCTION_GOOGLE_CLIENT_ID =
            "1034181863686-fpvbt3vqdv82rmuo93ftep40c04t3tb6.apps.googleusercontent.com"
    }

    var onUnauthorized: (() -> Unit)? = null

    private val client = HttpClient {
        install(ContentNegotiation) {
            json(json)
        }
    }

    private fun baseUrl(): String {
        return DudaLocalStorage.getUserPreferences().serverUrl.trimEnd('/')
    }

    private fun language(): String {
        return DudaLocalStorage.getUserPreferences().languageCode
    }

    private fun token(): String? {
        return DudaLocalStorage.getAccessToken()
    }

    private suspend inline fun <reified T> handleResponse(response: HttpResponse): T {
        if (response.status == HttpStatusCode.Unauthorized) {
            DudaLocalStorage.clearAuthSession()
            onUnauthorized?.invoke()
            throw AppError(
                message = "Сессия истекла или требуется вход через Google (401)",
                type = AppErrorType.Network
            )
        }

        if (response.status.isSuccess()) {
            if (response.status == HttpStatusCode.NoContent) {
                return Unit as T
            }
            return response.body<T>()
        }

        val errorBody = response.bodyAsText()
        val problem = try {
            json.decodeFromString<ProblemDetailsDto>(errorBody)
        } catch (_: Exception) {
            null
        }

        val message = problem?.detail
            ?: problem?.error
            ?: problem?.message
            ?: problem?.title
            ?: "Ошибка сервера (${response.status.value})"
        throw AppError(message = message, type = AppErrorType.Server)
    }

    // ========================================================================
    // 1. AUTH & PROFILE (/auth/google, /auth/logout, /me, /profile/unit-preference)
    // ========================================================================

    suspend fun authenticateGoogle(idToken: String): AuthResponseDto {
        val response = client.post("${baseUrl()}/auth/google") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            setBody(GoogleAuthRequestDto(idToken = idToken))
        }
        val authResult = handleResponse<AuthResponseDto>(response)
        DudaLocalStorage.setAccessToken(authResult.accessToken)
        DudaLocalStorage.setUserProfile(authResult.user)
        return authResult
    }

    suspend fun getMe(): UserProfileDto {
        val response = client.get("${baseUrl()}/me") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        val remoteProfile = handleResponse<UserProfileDto>(response)
        val cached = DudaLocalStorage.getUserProfile()
        val merged = if (cached != null) {
            remoteProfile.copy(
                heightCm = cached.heightCm ?: remoteProfile.heightCm,
                weightKg = cached.weightKg ?: remoteProfile.weightKg,
                targetWeightKg = cached.targetWeightKg ?: remoteProfile.targetWeightKg,
                gender = cached.gender,
                dietGoal = cached.dietGoal,
                targetCalories = cached.targetCalories,
                targetProteinG = cached.targetProteinG,
                targetCarbsG = cached.targetCarbsG,
                targetFatG = cached.targetFatG
            )
        } else {
            remoteProfile
        }
        DudaLocalStorage.setUserProfile(merged)
        return merged
    }

    suspend fun updateUnitPreference(unitPreference: String): UserProfileDto {
        val response = client.put("${baseUrl()}/profile/unit-preference") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(UpdateUnitPreferenceRequestDto(unitPreference = unitPreference))
        }
        val updated = handleResponse<UserProfileDto>(response)
        val cached = DudaLocalStorage.getUserProfile()
        val merged = (cached ?: updated).copy(unitPreference = updated.unitPreference)
        DudaLocalStorage.setUserProfile(merged)
        return merged
    }

    suspend fun logout() {
        try {
            client.post("${baseUrl()}/auth/logout") {
                header("Accept-Language", language())
                token()?.let { header("Authorization", "Bearer $it") }
            }
        } catch (_: Exception) {
        }
        DudaLocalStorage.clearAuthSession()
    }

    // ========================================================================
    // 2. EXERCISES (/exercises)
    // ========================================================================

    suspend fun getExercises(
        search: String? = null,
        muscleGroup: String? = null,
        equipment: String? = null
    ): List<Exercise> {
        val response = client.get("${baseUrl()}/exercises") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
            muscleGroup?.takeIf { it.isNotBlank() && it.lowercase() != "all" }?.let { parameter("muscle_group", it.lowercase()) }
            equipment?.takeIf { it.isNotBlank() && it.lowercase() != "all" }?.let { parameter("equipment", it.lowercase()) }
        }
        return handleResponse(response)
    }

    suspend fun createExercise(request: SaveExerciseRequestDto): Exercise {
        val response = client.post("${baseUrl()}/exercises") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun updateExercise(exerciseId: String, request: SaveExerciseRequestDto): Exercise {
        val response = client.put("${baseUrl()}/exercises/$exerciseId") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun deleteExercise(exerciseId: String) {
        val response = client.delete("${baseUrl()}/exercises/$exerciseId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    // ========================================================================
    // 3. ROUTINES (/routines)
    // ========================================================================

    suspend fun getRoutines(): List<Routine> {
        val response = client.get("${baseUrl()}/routines") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun createRoutine(request: SaveRoutineRequestDto): Routine {
        val response = client.post("${baseUrl()}/routines") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun updateRoutine(routineId: String, request: SaveRoutineRequestDto): Routine {
        val response = client.put("${baseUrl()}/routines/$routineId") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun deleteRoutine(routineId: String) {
        val response = client.delete("${baseUrl()}/routines/$routineId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    // ========================================================================
    // 4. WORKOUTS (/workouts, /workouts/previous, /workouts/{id}/finish)
    // ========================================================================

    suspend fun getWorkouts(limit: Int = 50, offset: Int = 0): List<Workout> {
        val response = client.get("${baseUrl()}/workouts") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("limit", limit)
            parameter("offset", offset)
        }
        val wrapper = handleResponse<WorkoutsListResponseDto>(response)
        return wrapper.items
    }

    suspend fun startWorkoutRemote(request: StartWorkoutRequestDto): Workout {
        val response = client.post("${baseUrl()}/workouts") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun getPreviousPerformance(
        exerciseIds: List<String>,
        exerciseNames: List<String>
    ): PreviousPerformanceResponseDto {
        val response = client.get("${baseUrl()}/workouts/previous") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            if (exerciseIds.isNotEmpty()) {
                parameter("exercise_ids", exerciseIds.joinToString(","))
            }
            if (exerciseNames.isNotEmpty()) {
                parameter("exercise_names", exerciseNames.joinToString("||"))
            }
        }
        return handleResponse(response)
    }

    suspend fun finishWorkoutRemote(workoutId: String, request: FinishWorkoutRequestDto): Workout {
        val response = client.post("${baseUrl()}/workouts/$workoutId/finish") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun cancelWorkoutRemote(workoutId: String) {
        val response = client.post("${baseUrl()}/workouts/$workoutId/cancel") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun deleteWorkout(workoutId: String) {
        val response = client.delete("${baseUrl()}/workouts/$workoutId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    // ========================================================================
    // 5. PROGRAMS, SPLITS & COMMUNITY (/programs, /athletes, /feedback, /platform/stats)
    // ========================================================================

    suspend fun getPrograms(
        tab: String = "library",
        splitType: String? = null,
        search: String? = null
    ): List<TrainingProgram> {
        val response = client.get("${baseUrl()}/programs") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("tab", tab)
            splitType?.takeIf { it.isNotBlank() && it != "all" }?.let { parameter("split_type", it) }
            search?.takeIf { it.isNotBlank() }?.let { parameter("search", it) }
        }
        return handleResponse(response)
    }

    suspend fun createProgram(request: SaveProgramRequestDto): TrainingProgram {
        val response = client.post("${baseUrl()}/programs") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun updateProgram(programId: String, request: SaveProgramRequestDto): TrainingProgram {
        val response = client.put("${baseUrl()}/programs/$programId") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun deleteProgram(programId: String) {
        val response = client.delete("${baseUrl()}/programs/$programId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun publishProgram(programId: String, isPublic: Boolean) {
        val response = client.post("${baseUrl()}/programs/$programId/publish") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(PublishProgramRequestDto(isPublic = isPublic))
        }
        handleResponse<Unit>(response)
    }

    suspend fun installProgram(programId: String, setActive: Boolean = true): UserProgram {
        val response = client.post("${baseUrl()}/programs/$programId/install") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(InstallProgramRequestDto(setActive = setActive))
        }
        return handleResponse(response)
    }

    suspend fun getActiveProgram(): ActiveProgramBundle? {
        val response = client.get("${baseUrl()}/programs/user/active") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        if (response.status == HttpStatusCode.NoContent) return null
        return handleResponse<ActiveProgramBundle>(response)
    }

    suspend fun setActiveUserProgram(userProgramId: String) {
        val response = client.post("${baseUrl()}/programs/user/active") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(SetActiveProgramRequestDto(userProgramId = userProgramId))
        }
        handleResponse<Unit>(response)
    }

    suspend fun getInstalledPrograms(): List<ActiveProgramBundle> {
        val response = client.get("${baseUrl()}/programs/user/installed") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getProgramLikeStatus(programId: String): LikeStatusResponseDto {
        val response = client.get("${baseUrl()}/programs/$programId/like") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun toggleLikeProgram(programId: String): LikeStatusResponseDto {
        val response = client.post("${baseUrl()}/programs/$programId/like") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getProgramComments(programId: String): List<ProgramComment> {
        val response = client.get("${baseUrl()}/programs/$programId/comments") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun postProgramComment(
        programId: String,
        content: String,
        parentId: String? = null,
        authorName: String? = null,
        avatarUrl: String? = null
    ): ProgramComment {
        val response = client.post("${baseUrl()}/programs/$programId/comments") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(
                PostProgramCommentRequestDto(
                    content = content,
                    parentId = parentId,
                    authorName = authorName,
                    avatarUrl = avatarUrl
                )
            )
        }
        return handleResponse(response)
    }

    suspend fun upvoteProgramComment(programId: String, commentId: String): CommentUpvoteResponseDto {
        val response = client.post("${baseUrl()}/programs/$programId/comments/$commentId/upvote") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun deleteProgramComment(programId: String, commentId: String) {
        val response = client.delete("${baseUrl()}/programs/$programId/comments/$commentId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun getAthleteProfile(athleteId: String): AthleteProfile {
        val response = client.get("${baseUrl()}/athletes/$athleteId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getPlatformStats(): PlatformStats {
        val response = client.get("${baseUrl()}/platform/stats") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getFeedbackList(): List<FeedbackReport> {
        val response = client.get("${baseUrl()}/feedback") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun submitFeedback(request: CreateFeedbackRequestDto): FeedbackReport {
        val response = client.post("${baseUrl()}/feedback") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun upvoteFeedback(feedbackId: String): CommentUpvoteResponseDto {
        val response = client.post("${baseUrl()}/feedback/$feedbackId/upvote") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    // ========================================================================
    // 6. TODOS, SCHEDULE & FOCUS SESSIONS (/todos)
    // ========================================================================

    suspend fun getDailySchedule(date: String): DailyScheduleResponseDto {
        val response = client.get("${baseUrl()}/todos") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("date", date)
        }
        return handleResponse(response)
    }

    suspend fun createTodo(request: SaveTodoRequestDto): TodoTask {
        val response = client.post("${baseUrl()}/todos") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun updateTodo(todoId: String, request: SaveTodoRequestDto): TodoTask {
        val response = client.put("${baseUrl()}/todos/$todoId") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun toggleTodo(todoId: String, date: String): TodoTask {
        val response = client.post("${baseUrl()}/todos/$todoId/toggle") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("date", date)
        }
        return handleResponse(response)
    }

    suspend fun deleteTodo(todoId: String) {
        val response = client.delete("${baseUrl()}/todos/$todoId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun getActivityLogs(date: String): List<ActivityLogItem> {
        val response = client.get("${baseUrl()}/todos/activity-logs") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("date", date)
        }
        return handleResponse(response)
    }

    suspend fun deleteActivityLog(logId: String) {
        val response = client.delete("${baseUrl()}/todos/activity-logs/$logId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun logFocusSession(request: LogFocusSessionRequestDto): ActivityLogItem {
        val response = client.post("${baseUrl()}/todos/focus-sessions") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    // ========================================================================
    // 7. NUTRITION (/nutrition/today, /nutrition/analyze-photo, /nutrition/estimate-text, /nutrition/meals)
    // ========================================================================

    suspend fun getNutritionToday(): DailyNutritionSummaryDto {
        val response = client.get("${baseUrl()}/nutrition/today") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun analyzeFoodPhoto(
        imageBase64: String,
        mimeType: String = "image/jpeg",
        notes: String = ""
    ): MealAnalysisResult {
        val response = client.post("${baseUrl()}/nutrition/analyze-photo") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(
                AnalyzePhotoRequestDto(
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    notes = notes,
                    language = language()
                )
            )
        }
        return handleResponse(response)
    }

    suspend fun estimateFoodText(description: String): MealAnalysisResult {
        val response = client.post("${baseUrl()}/nutrition/estimate-text") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(
                EstimateTextFoodRequestDto(
                    description = description,
                    language = language()
                )
            )
        }
        return handleResponse(response)
    }

    suspend fun saveMealLog(request: SaveMealRequestDto): MealLog {
        val response = client.post("${baseUrl()}/nutrition/meals") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun deleteMealLog(mealId: String) {
        val response = client.delete("${baseUrl()}/nutrition/meals/$mealId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    // ========================================================================
    // 8. AI COACH (/coach/insights, /coach/messages, /coach/chat)
    // ========================================================================

    suspend fun getCoachInsights(): CoachInsight {
        val response = client.get("${baseUrl()}/coach/insights") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getCoachMessages(): List<ChatMessage> {
        val response = client.get("${baseUrl()}/coach/messages") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun sendCoachChat(
        message: String,
        imageBase64: String? = null,
        mimeType: String? = null,
        history: List<CoachChatHistoryItemDto> = emptyList()
    ): CoachChatResponseDto {
        val response = client.post("${baseUrl()}/coach/chat") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(
                CoachChatRequestDto(
                    message = message,
                    imageBase64 = imageBase64,
                    mimeType = mimeType,
                    history = history
                )
            )
        }
        return handleResponse(response)
    }

    suspend fun clearCoachMessages() {
        val response = client.delete("${baseUrl()}/coach/messages") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    // ========================================================================
    // 9. PROGRESS & RECORDS (/progress/records, /progress/history/{exerciseId})
    // ========================================================================

    suspend fun getProgressRecords(): List<ExerciseRecordGroupDto> {
        val response = client.get("${baseUrl()}/progress/records") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getExerciseHistory(exerciseId: String): ExerciseHistoryResponseDto {
        val response = client.get("${baseUrl()}/progress/history/$exerciseId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    // ========================================================================
    // 10. BODY LOGS & TREND (/body/logs, /body/trend)
    // ========================================================================

    suspend fun getBodyTrend(): BodyTrendResponseDto {
        val response = client.get("${baseUrl()}/body/trend") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun getBodyLogs(): List<BodyMeasurement> {
        val response = client.get("${baseUrl()}/body/logs") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun saveBodyLog(request: SaveBodyLogRequestDto): BodyMeasurement {
        val response = client.post("${baseUrl()}/body/logs") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(request)
        }
        return handleResponse(response)
    }

    suspend fun deleteBodyLog(date: String) {
        val response = client.delete("${baseUrl()}/body/logs/$date") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }
}
