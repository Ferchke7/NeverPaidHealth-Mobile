package dev.neverpaid.health.features.auth

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import dev.neverpaid.health.core.designsystem.BorderDivider
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymDivider
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.TextSoft

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.CrimsonSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DudaBrandLogo
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.GoogleBrandBlue
import dev.neverpaid.health.core.designsystem.GoogleBrandGreen
import dev.neverpaid.health.core.designsystem.GoogleBrandRed
import dev.neverpaid.health.core.designsystem.GoogleBrandYellow
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymWell
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.rememberNativeGoogleAuthLauncher

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    androidx.compose.foundation.Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = w * 0.18f

        // Blue horizontal crossbar
        drawLine(
            color = GoogleBrandBlue,
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(w, cy),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        val strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        val arcRect = androidx.compose.ui.geometry.Rect(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f)

        // Red top arc
        drawArc(
            color = GoogleBrandRed,
            startAngle = 205f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )

        // Yellow bottom-left arc
        drawArc(
            color = GoogleBrandYellow,
            startAngle = 125f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )

        // Green bottom arc
        drawArc(
            color = GoogleBrandGreen,
            startAngle = 40f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )

        // Blue upper-right hook
        drawArc(
            color = GoogleBrandBlue,
            startAngle = 315f,
            sweepAngle = 55f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )
    }
}

@Composable
fun AuthRoute(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    CollectEffect(viewModel.effect) { effect ->
        when (effect) {
            AuthUiEffect.NavigateToHome -> onNavigateToHome()
            is AuthUiEffect.ShowToast -> {}
        }
    }

    val googleAuthLauncher = rememberNativeGoogleAuthLauncher(
        onSuccess = { idToken ->
            viewModel.onAction(AuthUiAction.OnGoogleTokenCaptured(idToken))
        },
        onError = { error ->
            viewModel.onAction(AuthUiAction.OnGoogleAuthError(error))
        }
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        AuthScreen(
            state = state,
            onAction = viewModel::onAction,
            onTriggerGoogleSignIn = { googleAuthLauncher.signIn() },
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthUiAction) -> Unit,
    onTriggerGoogleSignIn: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val clipboardManager = LocalClipboardManager.current
    val lang = state.selectedLanguage.lowercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Minimal Obsidian Auth Header (duda.uz + RU/UZ/EN language switcher)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "duda.uz",
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = (-0.3).sp,
                color = TextPrimary
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ru" to "RU", "uz" to "UZ", "en" to "EN").forEach { (code, label) ->
                    GymPillTab(
                        text = label,
                        selected = lang == code,
                        onClick = { onAction(AuthUiAction.OnSelectLanguage(code)) }
                    )
                }
            }
        }

        HorizontalDivider(thickness = 1.dp, color = BorderDivider)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Card 1: Primary Native Login Card
            GymCard(
                backgroundColor = DarkSurface,
                borderColor = BorderSubtle
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "NATIVE · ILOVA"
                        "en" -> "NATIVE · APP"
                        else -> "NATIVE · ПРИЛОЖЕНИЕ"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.8.sp,
                    color = TextSecondary
                )

                Text(
                    text = when (lang) {
                        "uz" -> "Senga moslashadigan mashg'ulotlar."
                        "en" -> "Workouts that adapt to you."
                        else -> "Тренировки, которые подстраиваются под тебя."
                    },
                    fontSize = 28.sp,
                    fontWeight = FontWeight.SemiBold,
                    lineHeight = 34.sp,
                    letterSpacing = (-0.5).sp,
                    color = TextPrimary
                )

                Text(
                    text = when (lang) {
                        "uz" -> "Davom etish uchun kiring"
                        "en" -> "Sign in to continue"
                        else -> "Войдите, чтобы продолжить"
                    },
                    fontSize = 14.sp,
                    color = TextSecondary
                )

                androidx.compose.material3.Surface(
                    onClick = onTriggerGoogleSignIn,
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(10.dp),
                    color = AccentLime,
                    contentColor = GymInkOnLight
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = GymInkOnLight,
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            GoogleLogoIcon(modifier = Modifier.size(16.dp))
                        }
                        Text(
                            text = when (lang) {
                                "uz" -> "Google orqali davom etish"
                                "en" -> "Continue with Google"
                                else -> "Продолжить с Google"
                            },
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = GymInkOnLight
                        )
                    }
                }

                if (state.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(CrimsonSubtle)
                            .border(1.dp, CrimsonRed.copy(alpha = 0.45f), RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = CrimsonRed,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }

                GymDivider(color = BorderSubtle)

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    WelcomeFeatureLine(
                        title = when (lang) {
                            "uz" -> "Mashg'ulotlar"
                            "en" -> "Workouts"
                            else -> "Тренировки"
                        },
                        subtitle = when (lang) {
                            "uz" -> "Shablonlar, dasturlar va mashqlar"
                            "en" -> "Templates, programs and exercises"
                            else -> "Шаблоны, программы и упражнения"
                        }
                    )
                    WelcomeFeatureLine(
                        title = when (lang) {
                            "uz" -> "Tarix"
                            "en" -> "History"
                            else -> "История"
                        },
                        subtitle = when (lang) {
                            "uz" -> "O'tgan mashg'ulotlar bir joyda"
                            "en" -> "Past workouts in one place"
                            else -> "Прошедшие тренировки в одном месте"
                        }
                    )
                    WelcomeFeatureLine(
                        title = when (lang) {
                            "uz" -> "Progress"
                            "en" -> "Progress"
                            else -> "Прогресс"
                        },
                        subtitle = when (lang) {
                            "uz" -> "Mashg'ulot ko'rsatkichlari va tana metrikalari"
                            "en" -> "Training metrics and body measurements"
                            else -> "Показатели тренировок и метрики тела"
                        }
                    )
                }
            }

            // Card 2: Guest Mode (Native Offline)
            GymCard(
                backgroundColor = DarkSurface,
                borderColor = BorderSubtle
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "Akkauntsiz"
                        "en" -> "Without an account"
                        else -> "Без аккаунта"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = when (lang) {
                        "uz" -> "Mehmon rejimi faqat native ilovada mavjud."
                        "en" -> "Guest mode is available only in the native app."
                        else -> "Гостевой режим доступен только в нативном приложении."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )
                GymOutlineButton(
                    text = when (lang) {
                        "uz" -> "Mehmon sifatida davom etish"
                        "en" -> "Continue as guest"
                        else -> "Продолжить как гость"
                    },
                    enabled = !state.isLoading,
                    onClick = { onAction(AuthUiAction.OnContinueAsGuest) }
                )
            }

            // Card 3: Token Login (Native Only) + Browser OAuth Fallback
            GymCard(
                backgroundColor = DarkSurface,
                borderColor = BorderSubtle
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "Token orqali kirish"
                        "en" -> "Sign in with token"
                        else -> "Вход по токену"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = when (lang) {
                        "uz" -> "Faqat native ilova · mavjud kirish usuli."
                        "en" -> "Native app only · existing sign-in method."
                        else -> "Только нативное приложение · существующий способ входа."
                    },
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    color = TextSecondary
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = when (lang) {
                            "uz" -> "Kirish tokeni"
                            "en" -> "Access token"
                            else -> "Токен доступа"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextSecondary
                    )
                    GymTextField(
                        value = state.tokenInput,
                        onValueChange = { onAction(AuthUiAction.OnTokenInputChanged(it)) },
                        placeholder = when (lang) {
                            "uz" -> "Tokenni kiriting"
                            "en" -> "Paste token"
                            else -> "Вставьте токен"
                        },
                        singleLine = true,
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    val clip = clipboardManager.getText()?.text.orEmpty()
                                    if (clip.isNotBlank()) {
                                        onAction(AuthUiAction.OnPasteTokenFromClipboard(clip))
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Paste token",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )
                }

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    GymOutlineButton(
                        text = when (lang) {
                            "uz" -> "Token bilan kirish"
                            "en" -> "Sign in with token"
                            else -> "Войти по токену"
                        },
                        enabled = !state.isLoading,
                        onClick = { onAction(AuthUiAction.OnSubmitTokenClick) }
                    )
                    GymOutlineButton(
                        text = when (lang) {
                            "uz" -> "Brauzerda kirish"
                            "en" -> "Browser login"
                            else -> "Войти через браузер"
                        },
                        enabled = !state.isLoading,
                        textColor = TextSecondary,
                        onClick = { onAction(AuthUiAction.OnOpenBrowserLogin) }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeFeatureLine(
    title: String,
    subtitle: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = title,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = TextPrimary
        )
        Text(
            text = subtitle,
            fontSize = 13.sp,
            color = TextSecondary
        )
    }
}
