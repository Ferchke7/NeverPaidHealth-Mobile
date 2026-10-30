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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dev.neverpaid.health.core.network.DudaApiClient
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
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
                        // Fallback to Chrome Custom Tabs
                        launchCustomTabFallback(activity, currentOnError)
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
                    .setToolbarColor(0xFF09090B.toInt())
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

@SuppressLint("SetJavaScriptEnabled")
@Composable
actual fun GoogleOAuthWebViewModal(
    isOpen: Boolean,
    authUrl: String,
    onDismiss: () -> Unit,
    onTokenCaptured: (accessToken: String, userJson: String?) -> Unit
) {
    if (!isOpen) return

    val currentOnTokenCaptured by rememberUpdatedState(onTokenCaptured)
    var hasCaptured by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF09090B)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF121215))
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Вход через Google — duda.uz",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Закрыть",
                            tint = Color(0xFFA1A1AA)
                        )
                    }
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    var webViewRef by remember { mutableStateOf<WebView?>(null) }

                    DisposableEffect(Unit) {
                        val handler = Handler(Looper.getMainLooper())
                        val runnable = object : Runnable {
                            override fun run() {
                                if (hasCaptured) return
                                val wv = webViewRef
                                if (wv != null) {
                                    wv.evaluateJavascript(
                                        "(function() { return JSON.stringify({ token: window.localStorage.getItem('np_access_token'), user: window.localStorage.getItem('np_user_cache') }); })();"
                                    ) { rawResult ->
                                        if (!hasCaptured && !rawResult.isNullOrBlank() && rawResult != "null") {
                                            try {
                                                val unescaped = if (rawResult.startsWith("\"") && rawResult.endsWith("\"")) {
                                                    org.json.JSONTokener(rawResult).nextValue().toString()
                                                } else {
                                                    rawResult
                                                }
                                                val obj = JSONObject(unescaped)
                                                val token = obj.optString("token", "")
                                                val userJson = obj.optString("user", "")
                                                if (token.isNotBlank() && token != "null" && token != "offline-token") {
                                                    hasCaptured = true
                                                    currentOnTokenCaptured(
                                                        token,
                                                        userJson.takeIf { it.isNotBlank() && it != "null" }
                                                    )
                                                }
                                            } catch (_: Exception) {
                                            }
                                        }
                                    }

                                    // Also check np_access_token cookie on https://duda.uz
                                    if (!hasCaptured) {
                                        val cookies = CookieManager.getInstance().getCookie("https://duda.uz").orEmpty()
                                        val match = Regex("np_access_token=([^;]+)").find(cookies)
                                        val cookieToken = match?.groupValues?.getOrNull(1)?.trim()
                                        if (!cookieToken.isNullOrBlank() && cookieToken != "offline-token") {
                                            hasCaptured = true
                                            currentOnTokenCaptured(cookieToken, null)
                                        }
                                    }
                                }
                                if (!hasCaptured) {
                                    handler.postDelayed(this, 600L)
                                }
                            }
                        }
                        handler.postDelayed(runnable, 600L)
                        onDispose {
                            handler.removeCallbacks(runnable)
                        }
                    }

                    AndroidView(
                        modifier = Modifier.fillMaxSize(),
                        factory = { context ->
                            WebView(context).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT
                                )
                                settings.javaScriptEnabled = true
                                settings.domStorageEnabled = true
                                settings.databaseEnabled = true
                                settings.javaScriptCanOpenWindowsAutomatically = true
                                settings.setSupportMultipleWindows(false)
                                settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW

                                // Strip "; wv" so Google OAuth / GIS allows authentication inside WebView
                                val defaultUa = settings.userAgentString ?: ""
                                settings.userAgentString = defaultUa
                                    .replace("; wv", "")
                                    .replace("Version/4.0 ", "")

                                CookieManager.getInstance().setAcceptCookie(true)
                                CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)

                                webChromeClient = WebChromeClient()
                                webViewClient = object : WebViewClient() {
                                    override fun shouldOverrideUrlLoading(
                                        view: WebView?,
                                        request: WebResourceRequest?
                                    ): Boolean {
                                        return false
                                    }
                                }

                                loadUrl(authUrl)
                                webViewRef = this
                            }
                        }
                    )
                }
            }
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
