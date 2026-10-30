package dev.neverpaid.health.core.storage

import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.FocusTimerState
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

    fun clearAuthSession() {
        setAccessToken(null)
        setUserProfile(null)
        setActiveWorkoutDraft(null)
        setFocusTimerState(null)
    }
}
