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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.NSUUID
import platform.Foundation.NSUserDefaults
import platform.Foundation.NSURL
import platform.Foundation.timeIntervalSince1970
import platform.UIKit.UIApplication
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

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
actual fun GoogleOAuthWebViewModal(
    isOpen: Boolean,
    authUrl: String,
    onDismiss: () -> Unit,
    onTokenCaptured: (accessToken: String, userJson: String?) -> Unit
) {
    if (!isOpen) return
    var pastedToken by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF121215)
        ) {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = "Вход через Google — duda.uz",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Нажмите «Открыть duda.uz в Safari», войдите через Google и вставьте полученный токен сессии (np_access_token):",
                    color = Color(0xFFA1A1AA),
                    fontSize = 12.sp
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        NSURL.URLWithString(authUrl)?.let { url ->
                            UIApplication.sharedApplication.openURL(url)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Открыть duda.uz в Safari", color = Color(0xFF4ADE80))
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pastedToken,
                    onValueChange = { pastedToken = it },
                    label = { Text("JWT Access Token или Google ID Token") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    OutlinedButton(onClick = onDismiss) {
                        Text("Отмена")
                    }
                    Spacer(modifier = Modifier.padding(4.dp))
                    Button(
                        onClick = {
                            if (pastedToken.isNotBlank()) {
                                onTokenCaptured(pastedToken.trim(), null)
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF22C55E),
                            contentColor = Color.Black
                        )
                    ) {
                        Text("Войти", fontWeight = FontWeight.Bold)
                    }
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
