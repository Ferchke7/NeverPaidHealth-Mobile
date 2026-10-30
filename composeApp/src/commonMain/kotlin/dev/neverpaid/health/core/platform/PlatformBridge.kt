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
