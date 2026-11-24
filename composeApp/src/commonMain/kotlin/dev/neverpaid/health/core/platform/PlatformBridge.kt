package dev.neverpaid.health.core.platform

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import kotlinx.serialization.Serializable

object TimerNotificationIds {
    const val REST = "duda_rest_timer"
    const val FOCUS = "duda_focus_timer"
}

@Serializable
data class LiveWorkoutWidgetState(
    val workoutId: String,
    val workoutTitle: String,
    val startTimeEpochMs: Long,
    val completedSets: Int,
    val totalSets: Int,
    val totalVolumeKg: Double,
    val currentExerciseName: String = "",
    val restEndEpochMs: Long = 0L,
    val restTotalSeconds: Int = 0,
    val defaultRestSeconds: Int = 90,
    val useKilograms: Boolean = true,
    val languageCode: String = "ru"
)

expect object PlatformBridge {
    fun vibrate()
    fun showToast(message: String)
    fun currentTimeMillis(): Long
    fun generateUuid(): String
    fun formatDate(epochMs: Long, pattern: String = "dd MMM yyyy"): String

    fun putString(key: String, value: String)
    fun getString(key: String): String?
    fun remove(key: String)
    fun clear()
    fun decodeBase64ToString(base64: String): String?
    fun decodeImageBitmap(bytes: ByteArray): ImageBitmap?
    fun openUrl(url: String)

    fun scheduleTimerNotification(
        timerId: String,
        triggerAtEpochMs: Long,
        title: String,
        body: String,
        showOngoingCountdown: Boolean = true,
        ongoingTitle: String = title,
        ongoingBody: String = ""
    )
    fun cancelTimerNotification(timerId: String)
    fun showTimerAlertNotification(timerId: String, title: String, body: String)
    fun playAlertSound()
    fun requestNotificationPermission()
    fun isAppInForeground(): Boolean
    fun updateLiveWorkoutWidget(state: LiveWorkoutWidgetState?)
    fun setRestTimerExternalListener(listener: (() -> Unit)?)
    fun consumeOpenActiveWorkoutPending(): Boolean
}

/**
 * Handle for triggering native Google Sign-In.
 */
interface NativeGoogleAuthHandler {
    fun signIn()
}

/**
 * Platform-native Google Sign-In launcher.
 * - Android: Android Credential Manager (Native Google One-Tap BottomSheet) + Chrome Custom Tabs fallback.
 * - iOS: System Safari Authentication Session with deep-link return.
 */
@Composable
expect fun rememberNativeGoogleAuthLauncher(
    onSuccess: (idToken: String) -> Unit,
    onError: (errorMessage: String) -> Unit
): NativeGoogleAuthHandler

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (base64: String, mimeType: String) -> Unit
): () -> Unit
