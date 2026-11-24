package dev.neverpaid.health.core.platform

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlarmManager
import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.RingtoneManager
import android.media.ToneGenerator
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.neverpaid.health.core.designsystem.RoyalVioletToolbarHex
import dev.neverpaid.health.core.network.DudaApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.lang.ref.WeakReference
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val CHANNEL_TIMER_ALERTS = "duda_timer_alerts_v1"
private const val CHANNEL_TIMER_ONGOING = "duda_timer_ongoing_v1"
private const val PREFS_TIMER_ALARMS = "duda_timer_alarms"
private const val KEY_NOTIF_PROMPTED = "notif_prompted_v1"
private const val REQUEST_CODE_POST_NOTIFICATIONS = 9401

private class AndroidPlatformHolder {
    var appContext: Context? = null
    var lifecycleRegistered = false
    var resumedActivityCount = 0
    var currentActivityRef: WeakReference<Activity>? = null
    val inMemoryFallback = HashMap<String, String>()
}

actual object PlatformBridge {
    private val runtime = AndroidPlatformHolder()

    fun init(context: Context) {
        val appCtx = context.applicationContext
        runtime.appContext = appCtx
        if (!runtime.lifecycleRegistered && appCtx is Application) {
            runtime.lifecycleRegistered = true
            appCtx.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
                override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
                    runtime.currentActivityRef = WeakReference(activity)
                }

                override fun onActivityStarted(activity: Activity) {
                    runtime.currentActivityRef = WeakReference(activity)
                }

                override fun onActivityResumed(activity: Activity) {
                    runtime.resumedActivityCount += 1
                    runtime.currentActivityRef = WeakReference(activity)
                    requestNotificationPermission()
                }

                override fun onActivityPaused(activity: Activity) {
                    runtime.resumedActivityCount = (runtime.resumedActivityCount - 1).coerceAtLeast(0)
                }

                override fun onActivityStopped(activity: Activity) {}
                override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
                override fun onActivityDestroyed(activity: Activity) {
                    if (runtime.currentActivityRef?.get() === activity) {
                        runtime.currentActivityRef = null
                    }
                }
            })
        }
    }

    actual fun vibrate() {
        try {
            val ctx = runtime.appContext ?: return
            val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(50)
            }
        } catch (_: Throwable) {
        }
    }

    actual fun showToast(message: String) {
        val ctx = runtime.appContext ?: return
        val showAction = Runnable {
            try {
                Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
            } catch (_: Throwable) {
            }
        }
        if (Looper.myLooper() == Looper.getMainLooper()) {
            showAction.run()
        } else {
            Handler(Looper.getMainLooper()).post(showAction)
        }
    }

    actual fun currentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    actual fun generateUuid(): String {
        return UUID.randomUUID().toString()
    }

    actual fun formatDate(epochMs: Long, pattern: String): String {
        return try {
            SimpleDateFormat(pattern, Locale.getDefault()).format(Date(epochMs))
        } catch (_: Exception) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(epochMs))
        }
    }

    private fun prefs() = runtime.appContext?.getSharedPreferences("duda_storage", Context.MODE_PRIVATE)

    actual fun putString(key: String, value: String) {
        val p = prefs()
        if (p != null) {
            p.edit().putString(key, value).apply()
        } else {
            runtime.inMemoryFallback[key] = value
        }
    }

    actual fun getString(key: String): String? {
        val p = prefs()
        return if (p != null) p.getString(key, null) else runtime.inMemoryFallback[key]
    }

    actual fun remove(key: String) {
        val p = prefs()
        if (p != null) {
            p.edit().remove(key).apply()
        } else {
            runtime.inMemoryFallback.remove(key)
        }
    }

    actual fun clear() {
        val p = prefs()
        if (p != null) {
            p.edit().clear().apply()
        } else {
            runtime.inMemoryFallback.clear()
        }
    }

    actual fun decodeBase64ToString(base64: String): String? {
        return try {
            val bytes = android.util.Base64.decode(base64, android.util.Base64.URL_SAFE or android.util.Base64.NO_PADDING or android.util.Base64.NO_WRAP)
            String(bytes, Charsets.UTF_8)
        } catch (_: Exception) {
            null
        }
    }

    actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? {
        if (bytes.isEmpty()) return null
        return try {
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
        } catch (_: Throwable) {
            null
        }
    }

    actual fun openUrl(url: String) {
        val ctx = runtime.appContext ?: return
        try {
            val intent = CustomTabsIntent.Builder().build()
            val activity = ctx.findActivity()
            if (activity != null) {
                intent.launchUrl(activity, Uri.parse(url))
            } else {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                ctx.startActivity(browserIntent)
            }
        } catch (_: Exception) {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            ctx.startActivity(browserIntent)
        }
    }

    private data class TimerNotificationSlots(
        val alarmRequestCode: Int,
        val ongoingNotificationId: Int,
        val alertNotificationId: Int
    )

    private fun slotsForTimer(timerId: String): TimerNotificationSlots = when (timerId) {
        TimerNotificationIds.REST -> TimerNotificationSlots(1001, 2001, 3001)
        TimerNotificationIds.FOCUS -> TimerNotificationSlots(1002, 2002, 3002)
        else -> {
            val base = (timerId.hashCode() and 0x0FFF) + 4000
            TimerNotificationSlots(base, base + 1000, base + 2000)
        }
    }

    private fun alarmPrefs(ctx: Context) = ctx.getSharedPreferences(PREFS_TIMER_ALARMS, Context.MODE_PRIVATE)

    private fun ensureNotificationChannels(ctx: Context, nm: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (nm.getNotificationChannel(CHANNEL_TIMER_ALERTS) == null) {
            val soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttrs = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
            val alertChannel = NotificationChannel(
                CHANNEL_TIMER_ALERTS,
                "Таймеры и отдых",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Оповещения об окончании отдыха и фокус-сессий"
                enableVibration(true)
                vibrationPattern = longArrayOf(0L, 280L, 140L, 380L)
                setSound(soundUri, audioAttrs)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(alertChannel)
        }
        if (nm.getNotificationChannel(CHANNEL_TIMER_ONGOING) == null) {
            val ongoingChannel = NotificationChannel(
                CHANNEL_TIMER_ONGOING,
                "Активный таймер",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Отображение обратного отсчёта в шторке уведомлений"
                setSound(null, null)
                enableVibration(false)
                setShowBadge(false)
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
            }
            nm.createNotificationChannel(ongoingChannel)
        }
    }

    private fun hasPostNotificationsPermission(ctx: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun createAppLaunchIntent(ctx: Context): Intent {
        val launchIntent = ctx.packageManager.getLaunchIntentForPackage(ctx.packageName)
            ?: Intent(ctx, dev.neverpaid.health.MainActivity::class.java)
        return launchIntent.apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
    }

    private fun createAlarmBroadcastIntent(
        ctx: Context,
        timerId: String,
        triggerAtEpochMs: Long,
        title: String,
        body: String
    ): Intent {
        return Intent(ctx, TimerAlarmReceiver::class.java).apply {
            action = "dev.neverpaid.health.ACTION_TIMER_ALARM_$timerId"
            putExtra(TimerAlarmReceiver.EXTRA_TIMER_ID, timerId)
            putExtra(TimerAlarmReceiver.EXTRA_TRIGGER_AT, triggerAtEpochMs)
            putExtra(TimerAlarmReceiver.EXTRA_TITLE, title)
            putExtra(TimerAlarmReceiver.EXTRA_BODY, body)
        }
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
        val ctx = runtime.appContext ?: return
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val slots = slotsForTimer(timerId)
            if (nm != null) {
                ensureNotificationChannels(ctx, nm)
                nm.cancel(slots.alertNotificationId)
            }

            alarmPrefs(ctx).edit()
                .putLong("trigger_$timerId", triggerAtEpochMs)
                .putString("title_$timerId", title)
                .putString("body_$timerId", body)
                .apply()

            val broadcastIntent = createAlarmBroadcastIntent(ctx, timerId, triggerAtEpochMs, title, body)
            val alarmPendingIntent = PendingIntent.getBroadcast(
                ctx,
                slots.alarmRequestCode,
                broadcastIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val showPendingIntent = PendingIntent.getActivity(
                ctx,
                slots.alarmRequestCode + 100,
                createAppLaunchIntent(ctx),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            if (alarmManager != null && triggerAtEpochMs > System.currentTimeMillis()) {
                val canExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    alarmManager.canScheduleExactAlarms()
                } else {
                    true
                }
                if (canExact) {
                    try {
                        val clockInfo = AlarmManager.AlarmClockInfo(triggerAtEpochMs, showPendingIntent)
                        alarmManager.setAlarmClock(clockInfo, alarmPendingIntent)
                    } catch (_: SecurityException) {
                        alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, alarmPendingIntent)
                    }
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtEpochMs, alarmPendingIntent)
                }
            }

            if (showOngoingCountdown && nm != null && hasPostNotificationsPermission(ctx)) {
                val ongoingNotification = NotificationCompat.Builder(ctx, CHANNEL_TIMER_ONGOING)
                    .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                    .setContentTitle(ongoingTitle.ifBlank { title })
                    .apply {
                        if (ongoingBody.isNotBlank()) setContentText(ongoingBody)
                    }
                    .setWhen(triggerAtEpochMs)
                    .setShowWhen(true)
                    .setUsesChronometer(true)
                    .setChronometerCountDown(true)
                    .setOngoing(true)
                    .setOnlyAlertOnce(true)
                    .setSilent(true)
                    .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                    .setContentIntent(showPendingIntent)
                    .build()
                nm.notify(slots.ongoingNotificationId, ongoingNotification)
            }
        } catch (_: Throwable) {
        }
    }

    actual fun cancelTimerNotification(timerId: String) {
        val ctx = runtime.appContext ?: return
        try {
            alarmPrefs(ctx).edit()
                .remove("trigger_$timerId")
                .remove("title_$timerId")
                .remove("body_$timerId")
                .apply()

            val slots = slotsForTimer(timerId)
            val alarmManager = ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
            val existing = PendingIntent.getBroadcast(
                ctx,
                slots.alarmRequestCode,
                createAlarmBroadcastIntent(ctx, timerId, 0L, "", ""),
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (existing != null) {
                alarmManager?.cancel(existing)
                existing.cancel()
            }
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.cancel(slots.ongoingNotificationId)
            nm?.cancel(slots.alertNotificationId)
        } catch (_: Throwable) {
        }
    }

    internal fun handleAlarmBroadcast(
        context: Context,
        timerId: String,
        triggerAtEpochMs: Long,
        title: String,
        body: String
    ) {
        if (runtime.appContext == null) {
            runtime.appContext = context.applicationContext
        }
        val prefs = alarmPrefs(context)
        val scheduledTriggerAt = prefs.getLong("trigger_$timerId", -1L)
        if (scheduledTriggerAt <= 0L || (triggerAtEpochMs > 0L && scheduledTriggerAt != triggerAtEpochMs)) {
            return
        }
        val storedTitle = title.ifBlank { prefs.getString("title_$timerId", "").orEmpty() }
        val storedBody = body.ifBlank { prefs.getString("body_$timerId", "").orEmpty() }
        prefs.edit()
            .remove("trigger_$timerId")
            .remove("title_$timerId")
            .remove("body_$timerId")
            .apply()
        showTimerAlertNotification(
            timerId = timerId,
            title = storedTitle.ifBlank { "duda.uz" },
            body = storedBody.ifBlank { "Таймер завершён" }
        )
    }

    actual fun showTimerAlertNotification(timerId: String, title: String, body: String) {
        val ctx = runtime.appContext ?: return
        try {
            val nm = ctx.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            ensureNotificationChannels(ctx, nm)
            val slots = slotsForTimer(timerId)
            nm.cancel(slots.ongoingNotificationId)

            if (!hasPostNotificationsPermission(ctx)) return
            val showPendingIntent = PendingIntent.getActivity(
                ctx,
                slots.alarmRequestCode + 200,
                createAppLaunchIntent(ctx),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val notification = NotificationCompat.Builder(ctx, CHANNEL_TIMER_ALERTS)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setContentIntent(showPendingIntent)
                .build()
            nm.notify(slots.alertNotificationId, notification)
        } catch (_: Throwable) {
        }
    }

    actual fun playAlertSound() {
        val ctx = runtime.appContext ?: return
        try {
            val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            val ringtone = if (uri != null) RingtoneManager.getRingtone(ctx, uri) else null
            if (ringtone != null) {
                ringtone.play()
                return
            }
        } catch (_: Throwable) {
        }
        try {
            val tg = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 85)
            tg.startTone(ToneGenerator.TONE_PROP_BEEP2, 280)
            Handler(Looper.getMainLooper()).postDelayed({
                try {
                    tg.release()
                } catch (_: Throwable) {
                }
            }, 400L)
        } catch (_: Throwable) {
        }
    }

    actual fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val ctx = runtime.appContext ?: return
        if (hasPostNotificationsPermission(ctx)) return
        val prefs = alarmPrefs(ctx)
        if (prefs.getBoolean(KEY_NOTIF_PROMPTED, false)) return
        val activity = runtime.currentActivityRef?.get() ?: return
        prefs.edit().putBoolean(KEY_NOTIF_PROMPTED, true).apply()
        activity.runOnUiThread {
            try {
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            } catch (_: Throwable) {
            }
        }
    }

    actual fun isAppInForeground(): Boolean = runtime.resumedActivityCount > 0
}

internal fun Context.findActivity(): Activity? {
    var current = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}

@Composable
actual fun rememberNativeGoogleAuthLauncher(
    onSuccess: (idToken: String) -> Unit,
    onError: (errorMessage: String) -> Unit
): NativeGoogleAuthHandler {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val currentOnSuccess by rememberUpdatedState(onSuccess)
    val currentOnError by rememberUpdatedState(onError)

    return remember(context, coroutineScope) {
        object : NativeGoogleAuthHandler {
            override fun signIn() {
                val activity = context.findActivity()
                if (activity == null) {
                    currentOnError("Контекст Activity недоступен")
                    return
                }

                coroutineScope.launch {
                    try {
                        val credentialManager = CredentialManager.create(activity)
                        val googleIdOption = GetGoogleIdOption.Builder()
                            .setFilterByAuthorizedAccounts(false)
                            .setServerClientId(DudaApiClient.PRODUCTION_GOOGLE_CLIENT_ID)
                            .setAutoSelectEnabled(false)
                            .build()

                        val request = GetCredentialRequest.Builder()
                            .addCredentialOption(googleIdOption)
                            .build()

                        val result = credentialManager.getCredential(
                            request = request,
                            context = activity
                        )

                        val credential = result.credential
                        val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                        val idToken = googleIdTokenCredential.idToken
                        if (idToken.isNotBlank()) {
                            currentOnSuccess(idToken)
                        } else {
                            currentOnError("Не удалось получить Google ID Token")
                        }
                    } catch (e: GetCredentialCancellationException) {
                        // User cancelled the prompt, do not show error
                    } catch (e: Exception) {
                        val msg = e.message.orEmpty()
                        if (msg.contains("No credentials", ignoreCase = true) ||
                            msg.contains("16", ignoreCase = true) ||
                            e::class.simpleName?.contains("NoCredential", ignoreCase = true) == true
                        ) {
                            launchCustomTabFallback(activity, currentOnError)
                        } else {
                            currentOnError(msg.ifBlank { "Ошибка Google Sign-In" })
                        }
                    }
                }
            }
        }
    }
}

private fun launchCustomTabFallback(activity: Activity, onError: (String) -> Unit) {
    try {
        val authUrl = "https://duda.uz/login?mobile=1"
        val customTabsIntent = CustomTabsIntent.Builder()
            .setShowTitle(true)
            .setDefaultColorSchemeParams(
                CustomTabColorSchemeParams.Builder()
                    .setToolbarColor(android.graphics.Color.parseColor(RoyalVioletToolbarHex))
                    .build()
            )
            .build()
        customTabsIntent.launchUrl(activity, Uri.parse(authUrl))
    } catch (e: Exception) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://duda.uz/login?mobile=1"))
            activity.startActivity(intent)
        } catch (e2: Exception) {
            onError("Не удалось открыть браузер для авторизации")
        }
    }
}

@Composable
actual fun rememberImagePickerLauncher(
    onImageSelected: (base64: String, mimeType: String) -> Unit
): () -> Unit {
    val context = LocalContext.current
    val currentCallback by rememberUpdatedState(onImageSelected)

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            try {
                val inputStream = context.contentResolver.openInputStream(uri)
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                inputStream?.close()
                if (originalBitmap != null) {
                    val maxDim = 1024
                    val ratio = minOf(
                        maxDim.toFloat() / originalBitmap.width.toFloat(),
                        maxDim.toFloat() / originalBitmap.height.toFloat(),
                        1.0f
                    )
                    val targetW = (originalBitmap.width * ratio).toInt().coerceAtLeast(1)
                    val targetH = (originalBitmap.height * ratio).toInt().coerceAtLeast(1)
                    val scaled = Bitmap.createScaledBitmap(originalBitmap, targetW, targetH, true)
                    val out = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.JPEG, 82, out)
                    val base64 = Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
                    currentCallback(base64, "image/jpeg")
                }
            } catch (e: Exception) {
                PlatformBridge.showToast("Ошибка чтения фото: ${e.message}")
            }
        }
    }

    return remember(launcher) {
        { launcher.launch("image/*") }
    }
}
