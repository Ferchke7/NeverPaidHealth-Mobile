package dev.neverpaid.health.features.auth

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AccentLime
import dev.neverpaid.health.core.designsystem.AccentLimeSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem.CrimsonSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DudaBrandLogo
import dev.neverpaid.health.core.designsystem.GoogleBrandBlue
import dev.neverpaid.health.core.designsystem.GoogleBrandGreen
import dev.neverpaid.health.core.designsystem.GoogleBrandRed
import dev.neverpaid.health.core.designsystem.GoogleBrandYellow
import dev.neverpaid.health.core.designsystem.GymCard
import dev.neverpaid.health.core.designsystem.GymIconBadge
import dev.neverpaid.health.core.designsystem.GymInkOnLight
import dev.neverpaid.health.core.designsystem.GymOutlineButton
import dev.neverpaid.health.core.designsystem.GymPillTab
import dev.neverpaid.health.core.designsystem.GymPrimaryButton
import dev.neverpaid.health.core.designsystem.GymTextField
import dev.neverpaid.health.core.designsystem.HeroMuted
import dev.neverpaid.health.core.designsystem.InkWhite
import dev.neverpaid.health.core.designsystem.RoyalHeroCard
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.rememberNativeGoogleAuthLauncher

@Composable
fun GoogleLogoIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(22.dp)) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f
        val stroke = w * 0.18f

        drawLine(
            color = GoogleBrandBlue,
            start = Offset(cx, cy),
            end = Offset(w, cy),
            strokeWidth = stroke,
            cap = StrokeCap.Round
        )

        val strokeStyle = Stroke(width = stroke)
        val arcRect = Rect(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f)

        drawArc(
            color = GoogleBrandRed,
            startAngle = 205f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )
        drawArc(
            color = GoogleBrandYellow,
            startAngle = 125f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )
        drawArc(
            color = GoogleBrandGreen,
            startAngle = 40f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )
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
    var showTokenSection by remember { mutableStateOf(state.tokenInput.isNotBlank()) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Top bar: Brand + language pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DudaBrandLogo()

            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
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

        Spacer(modifier = Modifier.height(8.dp))

        // Royal Violet Hero Card (Figma Screen 11 Login)
        RoyalHeroCard(bottomExtraPadding = 24.dp) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .background(InkWhite.copy(alpha = 0.16f))
                    .padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                Text(
                    text = when (lang) {
                        "uz" -> "KUCH VA PROGRESS"
                        "en" -> "STRENGTH & PERFORMANCE"
                        else -> "СИЛА И ПРОГРЕСС"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = InkWhite
                )
            }

            Text(
                text = when (lang) {
                    "uz" -> "Senga moslashadigan mashg'ulotlar."
                    "en" -> "Workouts that adapt to you."
                    else -> "Тренировки, которые подстраиваются под тебя."
                },
                fontSize = 30.sp,
                fontWeight = FontWeight.Black,
                lineHeight = 35.sp,
                letterSpacing = (-0.9).sp,
                color = InkWhite
            )

            Text(
                text = when (lang) {
                    "uz" -> "Dasturlar, og'irliklar kundaligi, ovqatlanish va tana ko'rsatkichlari bitta ilovada."
                    "en" -> "Programs, set logging, nutrition and body metrics in one focused training log."
                    else -> "Программы, дневник подходов, питание и метрики тела без лишнего шума."
                },
                fontSize = 14.sp,
                lineHeight = 20.sp,
                color = HeroMuted
            )
        }

        // 3 gym pillars
        GymCard(backgroundColor = DarkSurface) {
            WelcomeFeatureRow(
                icon = Icons.Default.FitnessCenter,
                title = when (lang) {
                    "uz" -> "Mashg'ulot kundaligi"
                    "en" -> "Workout tracker"
                    else -> "Дневник тренировок"
                },
                subtitle = when (lang) {
                    "uz" -> "800+ mashqlar, taymer va shaxsiy rekordlar"
                    "en" -> "800+ exercises, rest timer and personal records"
                    else -> "800+ упражнений, таймер отдыха и личные рекорды"
                }
            )
            WelcomeFeatureRow(
                icon = Icons.AutoMirrored.Filled.ShowChart,
                title = when (lang) {
                    "uz" -> "Aniq progress"
                    "en" -> "Real progression"
                    else -> "Честная аналитика"
                },
                subtitle = when (lang) {
                    "uz" -> "Tonnaj, 1RM va tana o'lchovlari dinamikasi"
                    "en" -> "Volume, 1RM estimates and body measurements"
                    else -> "Тоннаж, расчёт 1ПМ и замеры тела по неделям"
                }
            )
            WelcomeFeatureRow(
                icon = Icons.Default.Restaurant,
                title = when (lang) {
                    "uz" -> "KBJU va AI murabbiy"
                    "en" -> "Macros & AI coach"
                    else -> "КБЖУ и AI-тренер"
                },
                subtitle = when (lang) {
                    "uz" -> "Ovqat tahlili va shaxsiy tavsiyalar"
                    "en" -> "Meal analysis and adaptive training advice"
                    else -> "Разбор рациона по фото и корректировка плана"
                }
            )
        }

        // Primary CTAs: Google Sign-In + Guest Mode
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                onClick = onTriggerGoogleSignIn,
                enabled = !state.isLoading,
                shape = RoundedCornerShape(16.dp),
                color = AccentLime,
                contentColor = GymInkOnLight,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = GymInkOnLight,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(30.dp)
                                .clip(CircleShape)
                                .background(InkWhite),
                            contentAlignment = Alignment.Center
                        ) {
                            GoogleLogoIcon(modifier = Modifier.size(18.dp))
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Google orqali kirish"
                            "en" -> "Continue with Google"
                            else -> "Продолжить с Google"
                        },
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = GymInkOnLight
                    )
                }
            }

            GymOutlineButton(
                text = when (lang) {
                    "uz" -> "Mehmon sifatida boshlash"
                    "en" -> "Continue as guest"
                    else -> "Продолжить без аккаунта"
                },
                enabled = !state.isLoading,
                fillWidth = true,
                onClick = { onAction(AuthUiAction.OnContinueAsGuest) }
            )
        }

        if (state.errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(CrimsonSubtle)
                    .padding(16.dp)
            ) {
                Text(
                    text = state.errorMessage,
                    color = CrimsonRed,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 18.sp
                )
            }
        }

        // Collapsible fallback: Browser / Token sign-in
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showTokenSection = !showTokenSection }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = when (lang) {
                        "uz" -> "Boshqa kirish usullari (brauzer / token)"
                        "en" -> "Other sign-in options (browser / token)"
                        else -> "Другие способы входа (браузер / токен)"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            AnimatedVisibility(visible = showTokenSection) {
                GymCard(
                    modifier = Modifier.padding(top = 8.dp),
                    backgroundColor = DarkSurface
                ) {
                    GymTextField(
                        value = state.tokenInput,
                        onValueChange = { onAction(AuthUiAction.OnTokenInputChanged(it)) },
                        placeholder = when (lang) {
                            "uz" -> "Kirish tokenini kiriting"
                            "en" -> "Paste access token"
                            else -> "Вставьте токен доступа"
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
                                    tint = AccentLime,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GymPrimaryButton(
                            text = when (lang) {
                                "uz" -> "Token bilan kirish"
                                "en" -> "Sign in"
                                else -> "Войти по токену"
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.weight(1f),
                            onClick = { onAction(AuthUiAction.OnSubmitTokenClick) }
                        )
                        GymOutlineButton(
                            text = when (lang) {
                                "uz" -> "Brauzer"
                                "en" -> "Browser"
                                else -> "Через браузер"
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.weight(1f),
                            onClick = { onAction(AuthUiAction.OnOpenBrowserLogin) }
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun WelcomeFeatureRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        GymIconBadge(
            icon = icon,
            tint = AccentLime,
            background = DarkSurfaceHigh,
            size = 44.dp
        )
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = TextSecondary
            )
        }
    }
}
