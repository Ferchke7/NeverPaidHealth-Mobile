package dev.neverpaid.health.core.platform

import androidx.compose.runtime.Composable

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
expect fun GoogleOAuthWebViewModal(
    isOpen: Boolean,
    authUrl: String = "https://duda.uz/login",
    onDismiss: () -> Unit,
    onTokenCaptured: (accessToken: String, userJson: String?) -> Unit
)

@Composable
expect fun rememberImagePickerLauncher(
    onImageSelected: (base64: String, mimeType: String) -> Unit
): () -> Unit
