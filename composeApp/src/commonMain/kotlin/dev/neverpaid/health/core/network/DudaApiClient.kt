package dev.neverpaid.health.core.network

import dev.neverpaid.health.core.error.AppError
import dev.neverpaid.health.core.error.AppErrorType
import dev.neverpaid.health.core.storage.DudaLocalStorage
import dev.neverpaid.health.domain.model.AuthResponseDto
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.Workout
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class ProblemDetailsDto(
    val type: String? = null,
    val title: String? = null,
    val status: Int? = null,
    val detail: String? = null
)

@Serializable
data class LoginRequest(val email: String, val password: String)

@Serializable
data class RegisterRequest(val email: String, val password: String, val name: String)

@Serializable
data class ToggleTodoRequest(val date: String, val completed: Boolean)

@Serializable
data class AnalyzeFoodRequest(val description: String)

@Serializable
data class CoachChatRequest(val message: String, val context: String? = null)

@Serializable
data class CoachChatResponse(val reply: String)

@Serializable
data class PostCommentRequest(val content: String, val parentId: String? = null)

class DudaApiClient(
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }
) {
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
            throw AppError(
                message = "Session expired. Please log in again.",
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

        val message = problem?.detail ?: problem?.title ?: "Server returned error: ${response.status.value}"
        throw AppError(message = message, type = AppErrorType.Server)
    }

    // --- AUTH ---
    suspend fun login(email: String, password: String): AuthResponseDto {
        val response = client.post("${baseUrl()}/auth/login") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            setBody(LoginRequest(email, password))
        }
        val authResult = handleResponse<AuthResponseDto>(response)
        DudaLocalStorage.setAccessToken(authResult.accessToken)
        DudaLocalStorage.setUserProfile(authResult.user)
        return authResult
    }

    suspend fun register(email: String, password: String, name: String): AuthResponseDto {
        val response = client.post("${baseUrl()}/auth/register") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            setBody(RegisterRequest(email, password, name))
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
        val profile = handleResponse<UserProfileDto>(response)
        DudaLocalStorage.setUserProfile(profile)
        return profile
    }

    suspend fun logout() {
        try {
            client.post("${baseUrl()}/auth/logout") {
                token()?.let { header("Authorization", "Bearer $it") }
            }
        } catch (_: Exception) {}
        DudaLocalStorage.clearAuthSession()
    }

    // --- EXERCISES ---
    suspend fun getExercises(query: String? = null, muscle: String? = null, equipment: String? = null): List<Exercise> {
        val response = client.get("${baseUrl()}/exercises") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            query?.takeIf { it.isNotBlank() }?.let { parameter("query", it) }
            muscle?.takeIf { it.isNotBlank() && it != "All" }?.let { parameter("muscle", it) }
            equipment?.takeIf { it.isNotBlank() && it != "All" }?.let { parameter("equipment", it) }
        }
        return handleResponse(response)
    }

    // --- WORKOUTS ---
    suspend fun getWorkouts(): List<Workout> {
        val response = client.get("${baseUrl()}/workouts") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun saveWorkout(workout: Workout): Workout {
        val response = client.post("${baseUrl()}/workouts") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(workout)
        }
        return handleResponse(response)
    }

    // --- ROUTINES ---
    suspend fun getRoutines(): List<Routine> {
        val response = client.get("${baseUrl()}/routines") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun createRoutine(routine: Routine): Routine {
        val response = client.post("${baseUrl()}/routines") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(routine)
        }
        return handleResponse(response)
    }

    // --- PROGRAMS & COMMUNITY ---
    suspend fun getPrograms(): List<TrainingProgram> {
        val response = client.get("${baseUrl()}/programs") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun activateProgram(programId: String) {
        val response = client.post("${baseUrl()}/programs/$programId/activate") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun likeProgram(programId: String) {
        val response = client.post("${baseUrl()}/programs/$programId/like") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    suspend fun getProgramComments(programId: String): List<ProgramComment> {
        val response = client.get("${baseUrl()}/programs/$programId/comments") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun postProgramComment(programId: String, content: String, parentId: String? = null): ProgramComment {
        val response = client.post("${baseUrl()}/programs/$programId/comments") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(PostCommentRequest(content, parentId))
        }
        return handleResponse(response)
    }

    // --- TODOS / DAILY PLAN ---
    suspend fun getTodos(date: String): List<TodoTask> {
        val response = client.get("${baseUrl()}/todos") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("date", date)
        }
        return handleResponse(response)
    }

    suspend fun saveTodo(task: TodoTask): TodoTask {
        val response = client.post("${baseUrl()}/todos") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(task)
        }
        return handleResponse(response)
    }

    suspend fun toggleTodo(taskId: String, date: String, completed: Boolean) {
        val response = client.post("${baseUrl()}/todos/$taskId/toggle") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(ToggleTodoRequest(date, completed))
        }
        handleResponse<Unit>(response)
    }

    suspend fun deleteTodo(taskId: String) {
        val response = client.delete("${baseUrl()}/todos/$taskId") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        handleResponse<Unit>(response)
    }

    // --- NUTRITION ---
    suspend fun getMealLogs(date: String): List<MealLog> {
        val response = client.get("${baseUrl()}/nutrition/logs") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            parameter("date", date)
        }
        return handleResponse(response)
    }

    suspend fun saveMealLog(meal: MealLog): MealLog {
        val response = client.post("${baseUrl()}/nutrition/logs") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(meal)
        }
        return handleResponse(response)
    }

    suspend fun analyzeFood(description: String): MealLog {
        val response = client.post("${baseUrl()}/nutrition/analyze") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(AnalyzeFoodRequest(description))
        }
        return handleResponse(response)
    }

    // --- BODY ---
    suspend fun getBodyMeasurements(): List<BodyMeasurement> {
        val response = client.get("${baseUrl()}/body/measurements") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun saveBodyMeasurement(measurement: BodyMeasurement): BodyMeasurement {
        val response = client.post("${baseUrl()}/body/measurements") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(measurement)
        }
        return handleResponse(response)
    }

    // --- COACH ---
    suspend fun getCoachInsight(): CoachInsight {
        val response = client.get("${baseUrl()}/coach/insights") {
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
        }
        return handleResponse(response)
    }

    suspend fun sendCoachChatMessage(message: String, context: String? = null): String {
        val response = client.post("${baseUrl()}/coach/chat") {
            contentType(ContentType.Application.Json)
            header("Accept-Language", language())
            token()?.let { header("Authorization", "Bearer $it") }
            setBody(CoachChatRequest(message, context))
        }
        val result = handleResponse<CoachChatResponse>(response)
        return result.reply
    }
}
