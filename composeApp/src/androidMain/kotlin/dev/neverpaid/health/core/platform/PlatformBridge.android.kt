package dev.neverpaid.health.core.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
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
import dev.neverpaid.health.core.network.DudaApiClient
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

actual object PlatformBridge {
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    actual fun vibrate() {
        val ctx = appContext ?: return
        val vibrator = ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator?.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator?.vibrate(50)
        }
    }

    actual fun showToast(message: String) {
        val ctx = appContext ?: return
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show()
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

    private val inMemoryFallback = mutableMapOf<String, String>()
    private fun prefs() = appContext?.getSharedPreferences("duda_storage", Context.MODE_PRIVATE)

    actual fun putString(key: String, value: String) {
        val p = prefs()
        if (p != null) {
            p.edit().putString(key, value).apply()
        } else {
            inMemoryFallback[key] = value
        }
    }

    actual fun getString(key: String): String? {
        val p = prefs()
        return if (p != null) p.getString(key, null) else inMemoryFallback[key]
    }

    actual fun remove(key: String) {
        val p = prefs()
        if (p != null) {
            p.edit().remove(key).apply()
        } else {
            inMemoryFallback.remove(key)
        }
    }

    actual fun clear() {
        val p = prefs()
        if (p != null) {
            p.edit().clear().apply()
        } else {
            inMemoryFallback.clear()
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

    actual fun openUrl(url: String) {
        val ctx = appContext ?: return
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
                    .setToolbarColor(0xFF0A0A0A.toInt())
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
