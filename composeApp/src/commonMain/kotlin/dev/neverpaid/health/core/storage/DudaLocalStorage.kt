package dev.neverpaid.health.core.storage

import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.FocusTimerState
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.TodoTask
import dev.neverpaid.health.domain.model.UserProfileDto
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

object DudaLocalStorage {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    private const val KEY_ACCESS_TOKEN = "np_access_token"
    private const val KEY_USER_PROFILE = "np_user_cache"
    private const val KEY_USER_PREFERENCES = "np_user_preferences_v2"
    private const val KEY_ACTIVE_WORKOUT = "neverpaid_active_workout_v2"
    private const val KEY_FOCUS_TIMER = "np_focus_timer_state_v4"
    private const val KEY_WATER_PREFIX = "np_water_"
    private const val KEY_WORKOUT_HISTORY = "np_workout_history_v2"
    private const val KEY_USER_ROUTINES = "np_user_routines_v2"
    private const val KEY_MEAL_LOGS = "np_meal_logs_v2"
    private const val KEY_TODOS_PREFIX = "np_todos_v2_"
    private const val KEY_BODY_MEASUREMENTS = "np_body_measurements_v2"

    fun getAccessToken(): String? {
        val raw = PlatformBridge.getString(KEY_ACCESS_TOKEN)
        if (raw.isNullOrBlank() || raw == "offline-token") {
            return null
        }
        return raw
    }

    fun setAccessToken(token: String?) {
        if (token.isNullOrBlank() || token == "offline-token") {
            PlatformBridge.remove(KEY_ACCESS_TOKEN)
        } else {
            PlatformBridge.putString(KEY_ACCESS_TOKEN, token)
        }
    }

    fun getUserProfile(): UserProfileDto? {
        val str = PlatformBridge.getString(KEY_USER_PROFILE) ?: return null
        return try {
            json.decodeFromString<UserProfileDto>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setUserProfile(profile: UserProfileDto?) {
        if (profile == null) {
            PlatformBridge.remove(KEY_USER_PROFILE)
        } else {
            PlatformBridge.putString(KEY_USER_PROFILE, json.encodeToString(profile))
        }
    }

    fun getUserPreferences(): UserPreferencesData {
        val str = PlatformBridge.getString(KEY_USER_PREFERENCES) ?: return UserPreferencesData()
        return try {
            json.decodeFromString<UserPreferencesData>(str)
        } catch (_: Exception) {
            UserPreferencesData()
        }
    }

    fun setUserPreferences(preferences: UserPreferencesData) {
        PlatformBridge.putString(KEY_USER_PREFERENCES, json.encodeToString(preferences))
    }

    fun getActiveWorkoutDraft(): Workout? {
        val str = PlatformBridge.getString(KEY_ACTIVE_WORKOUT) ?: return null
        return try {
            json.decodeFromString<Workout>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setActiveWorkoutDraft(workout: Workout?) {
        if (workout == null) {
            PlatformBridge.remove(KEY_ACTIVE_WORKOUT)
        } else {
            PlatformBridge.putString(KEY_ACTIVE_WORKOUT, json.encodeToString(workout))
        }
    }

    fun getFocusTimerState(): FocusTimerState {
        val str = PlatformBridge.getString(KEY_FOCUS_TIMER) ?: return FocusTimerState()
        return try {
            json.decodeFromString<FocusTimerState>(str)
        } catch (_: Exception) {
            FocusTimerState()
        }
    }

    fun setFocusTimerState(state: FocusTimerState?) {
        if (state == null || !state.isActive) {
            PlatformBridge.remove(KEY_FOCUS_TIMER)
        } else {
            PlatformBridge.putString(KEY_FOCUS_TIMER, json.encodeToString(state))
        }
    }

    fun getCachedWater(date: String): Int {
        val str = PlatformBridge.getString(KEY_WATER_PREFIX + date) ?: return 0
        return str.toIntOrNull() ?: 0
    }

    fun setCachedWater(date: String, ml: Int) {
        PlatformBridge.putString(KEY_WATER_PREFIX + date, ml.toString())
    }

    // --- Permanent Local Offline-First Persistence ---

    fun getWorkoutHistory(): List<Workout> {
        val str = PlatformBridge.getString(KEY_WORKOUT_HISTORY) ?: return emptyList()
        return try {
            json.decodeFromString<List<Workout>>(str)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setWorkoutHistory(list: List<Workout>) {
        PlatformBridge.putString(KEY_WORKOUT_HISTORY, json.encodeToString(list))
    }

    fun getUserRoutines(): List<Routine> {
        val str = PlatformBridge.getString(KEY_USER_ROUTINES) ?: return emptyList()
        return try {
            json.decodeFromString<List<Routine>>(str)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setUserRoutines(list: List<Routine>) {
        PlatformBridge.putString(KEY_USER_ROUTINES, json.encodeToString(list))
    }

    fun getMealLogs(): List<MealLog> {
        val str = PlatformBridge.getString(KEY_MEAL_LOGS) ?: return emptyList()
        return try {
            json.decodeFromString<List<MealLog>>(str)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setMealLogs(list: List<MealLog>) {
        PlatformBridge.putString(KEY_MEAL_LOGS, json.encodeToString(list))
    }

    fun getTodos(date: String): List<TodoTask>? {
        val str = PlatformBridge.getString(KEY_TODOS_PREFIX + date) ?: return null
        return try {
            json.decodeFromString<List<TodoTask>>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setTodos(date: String, list: List<TodoTask>) {
        PlatformBridge.putString(KEY_TODOS_PREFIX + date, json.encodeToString(list))
    }

    fun getBodyMeasurements(): List<BodyMeasurement> {
        val str = PlatformBridge.getString(KEY_BODY_MEASUREMENTS) ?: return emptyList()
        return try {
            json.decodeFromString<List<BodyMeasurement>>(str)
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setBodyMeasurements(list: List<BodyMeasurement>) {
        PlatformBridge.putString(KEY_BODY_MEASUREMENTS, json.encodeToString(list))
    }

    fun clearAuthSession() {
        setAccessToken(null)
        setUserProfile(null)
        setActiveWorkoutDraft(null)
        setFocusTimerState(null)
    }
}
