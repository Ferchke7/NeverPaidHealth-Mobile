package dev.neverpaid.health.core.platform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import org.jetbrains.skia.Image as SkiaImage
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationState
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNTimeIntervalNotificationTrigger
import platform.UserNotifications.UNUserNotificationCenter

actual object PlatformBridge {

    actual fun vibrate() {
        val generator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleMedium)
        generator.prepare()
        generator.impactOccurred()
    }

    actual fun showToast(message: String) {
        println("[duda.uz iOS Notification]: $message")
    }

    actual fun currentTimeMillis(): Long {
        return (NSDate().timeIntervalSince1970 * 1000).toLong()
    }

    actual fun generateUuid(): String {
        return NSUUID().UUIDString()
    }

    actual fun formatDate(epochMs: Long, pattern: String): String {
        val date = NSDate(timeIntervalSinceReferenceDate = (epochMs / 1000.0) - 978307200.0)
        val formatter = NSDateFormatter().apply {
            dateFormat = pattern
        }
        return formatter.stringFromDate(date)
    }

    actual fun putString(key: String, value: String) {
        NSUserDefaults.standardUserDefaults.setObject(value, forKey = key)
    }

    actual fun getString(key: String): String? {
        return NSUserDefaults.standardUserDefaults.stringForKey(key)
    }

    actual fun remove(key: String) {
        NSUserDefaults.standardUserDefaults.removeObjectForKey(key)
    }

    actual fun clear() {
        val defaults = NSUserDefaults.standardUserDefaults
        defaults.dictionaryRepresentation().keys.forEach {
            if (it is String) defaults.removeObjectForKey(it)
        }
    }

    @OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)
    actual fun decodeBase64ToString(base64: String): String? {
        return try {
            val bytes = kotlin.io.encoding.Base64.UrlSafe.decode(base64)
            bytes.decodeToString()
        } catch (_: Exception) {
            try {
                val bytes = kotlin.io.encoding.Base64.Default.decode(base64)
                bytes.decodeToString()
            } catch (_: Exception) {
                null
            }
        }
    }

    actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
        if (bytes.isEmpty()) return null
        return try {
            SkiaImage.makeFromEncoded(bytes).toComposeImageBitmap()
        } catch (_: Exception) {
            null
        }
    }

    actual fun openUrl(url: String) {
        val nsUrl = platform.Foundation.NSURL.URLWithString(url) ?: return
        platform.UIKit.UIApplication.sharedApplication.openURL(nsUrl)
    }

    actual fun scheduleTimerNotification(
        timerId: String,
        triggerAtEpochMs: Long,
        title: String,
        body: String,
        showOngoingCountdown: Boolean,
        ongoingTitle: String,
        ongoingBody: String
    ) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(timerId))
        val delaySec = ((triggerAtEpochMs - currentTimeMillis()) / 1000.0).coerceAtLeast(1.0)
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = delaySec,
            repeats = false
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = timerId,
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(request) { _ -> }
    }

    actual fun cancelTimerNotification(timerId: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        center.removePendingNotificationRequestsWithIdentifiers(listOf(timerId))
        center.removeDeliveredNotificationsWithIdentifiers(listOf(timerId))
    }

    actual fun showTimerAlertNotification(timerId: String, title: String, body: String) {
        val center = UNUserNotificationCenter.currentNotificationCenter()
        val content = UNMutableNotificationContent().apply {
            setTitle(title)
            setBody(body)
            setSound(UNNotificationSound.defaultSound)
        }
        val trigger = UNTimeIntervalNotificationTrigger.triggerWithTimeInterval(
            timeInterval = 0.2,
            repeats = false
        )
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = timerId,
            content = content,
            trigger = trigger
        )
        center.addNotificationRequest(request) { _ -> }
    }

    actual fun playAlertSound() {
        AudioServicesPlaySystemSound(1007u)
    }

    actual fun requestNotificationPermission() {
        val options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(options) { _, _ -> }
    }

    actual fun isAppInForeground(): Boolean {
        return UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive
    }

    actual fun updateLiveWorkoutWidget(state: LiveWorkoutWidgetState?) {
        // Store state snapshot cleanly for potential WidgetKit extension without requiring entitlements
        val defaults = NSUserDefaults.standardUserDefaults
        if (state == null) {
            defaults.removeObjectForKey("np_live_workout_widget_id")
        } else {
            defaults.setObject(state.workoutId, forKey = "np_live_workout_widget_id")
        }
    }

    actual fun setRestTimerExternalListener(listener: (() -> Unit)?) {
        // Android notification/appwidget quick actions trigger this hook; no-op on iOS.
    }

    actual fun consumeOpenActiveWorkoutPending(): Boolean = false
}

@Composable
actual fun rememberNativeGoogleAuthLauncher(
    onSuccess: (idToken: String) -> Unit,
    onError: (errorMessage: String) -> Unit
): NativeGoogleAuthHandler {
    return remember {
        object : NativeGoogleAuthHandler {
            override fun signIn() {
                val url = NSURL.URLWithString("https://duda.uz/login?mobile=1")
                if (url != null) {
                    UIApplication.sharedApplication.openURL(url)
                } else {
                    onError("Не удалось открыть браузер для входа")
                }
            }
        }
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (base64: String, mimeType: String) -> Unit
): () -> Unit {
    return remember {
        {
            PlatformBridge.showToast("Используйте текстовое описание блюда на iOS или прикрепите фото")
        }
    }
}
