package dev.neverpaid.health.core.storage

import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.BodyMeasurement
import dev.neverpaid.health.domain.model.MealLog
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

    private const val KEY_ACCESS_TOKEN = "duda_access_token_v1"
    private const val KEY_USER_PROFILE = "duda_user_profile_v1"
    private const val KEY_USER_PREFERENCES = "duda_user_preferences_v1"
    private const val KEY_ACTIVE_WORKOUT = "duda_active_workout_v1"
    private const val KEY_CACHED_WORKOUTS = "duda_cached_workouts_v1"
    private const val KEY_CACHED_TODOS = "duda_cached_todos_v1"
    private const val KEY_CACHED_MEALS = "duda_cached_meals_v1"
    private const val KEY_WATER_PREFIX = "duda_water_v1_"
    private const val KEY_CACHED_MEASUREMENTS = "duda_cached_measurements_v1"

    fun getAccessToken(): String? {
        return PlatformBridge.getString(KEY_ACCESS_TOKEN)
    }

    fun setAccessToken(token: String?) {
        if (token.isNullOrBlank()) {
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

    fun getCachedWorkouts(): List<Workout>? {
        val str = PlatformBridge.getString(KEY_CACHED_WORKOUTS) ?: return null
        return try {
            json.decodeFromString<List<Workout>>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setCachedWorkouts(workouts: List<Workout>) {
        PlatformBridge.putString(KEY_CACHED_WORKOUTS, json.encodeToString(workouts))
    }

    fun getCachedTodos(): List<TodoTask>? {
        val str = PlatformBridge.getString(KEY_CACHED_TODOS) ?: return null
        return try {
            json.decodeFromString<List<TodoTask>>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setCachedTodos(todos: List<TodoTask>) {
        PlatformBridge.putString(KEY_CACHED_TODOS, json.encodeToString(todos))
    }

    fun getCachedMeals(): List<MealLog>? {
        val str = PlatformBridge.getString(KEY_CACHED_MEALS) ?: return null
        return try {
            json.decodeFromString<List<MealLog>>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setCachedMeals(meals: List<MealLog>) {
        PlatformBridge.putString(KEY_CACHED_MEALS, json.encodeToString(meals))
    }

    fun getCachedWater(date: String): Int {
        val str = PlatformBridge.getString(KEY_WATER_PREFIX + date) ?: return 0
        return str.toIntOrNull() ?: 0
    }

    fun setCachedWater(date: String, ml: Int) {
        PlatformBridge.putString(KEY_WATER_PREFIX + date, ml.toString())
    }

    fun getCachedBodyMeasurements(): List<BodyMeasurement>? {
        val str = PlatformBridge.getString(KEY_CACHED_MEASUREMENTS) ?: return null
        return try {
            json.decodeFromString<List<BodyMeasurement>>(str)
        } catch (_: Exception) {
            null
        }
    }

    fun setCachedBodyMeasurements(list: List<BodyMeasurement>) {
        PlatformBridge.putString(KEY_CACHED_MEASUREMENTS, json.encodeToString(list))
    }

    fun clearAuthSession() {
        setAccessToken(null)
        setUserProfile(null)
        setActiveWorkoutDraft(null)
    }
}
