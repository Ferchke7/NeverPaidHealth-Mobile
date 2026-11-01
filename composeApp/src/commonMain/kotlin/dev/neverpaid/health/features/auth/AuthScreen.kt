package dev.neverpaid.health.features.auth

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
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.DudaBrandLogo
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
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
            color = Color(0xFF4285F4),
            start = androidx.compose.ui.geometry.Offset(cx, cy),
            end = androidx.compose.ui.geometry.Offset(w, cy),
            strokeWidth = stroke,
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )

        val strokeStyle = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke)
        val arcRect = androidx.compose.ui.geometry.Rect(stroke / 2f, stroke / 2f, w - stroke / 2f, h - stroke / 2f)

        // Red top arc
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 205f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )

        // Yellow bottom-left arc
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 125f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )

        // Green bottom arc
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 40f,
            sweepAngle = 85f,
            useCenter = false,
            topLeft = arcRect.topLeft,
            size = arcRect.size,
            style = strokeStyle
        )

        // Blue upper-right hook
        drawArc(
            color = Color(0xFF4285F4),
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
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Bar: Duda Brand Logo + Language Switcher
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            DudaBrandLogo()

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, BorderSubtle, RoundedCornerShape(10.dp))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                listOf("ru" to "RU", "uz" to "UZ", "en" to "EN").forEach { (code, label) ->
                    val isSelected = lang == code
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(7.dp))
                            .background(if (isSelected) EmeraldNeon else Color.Transparent)
                            .clickable { onAction(AuthUiAction.OnSelectLanguage(code)) }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.Black else TextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(26.dp))

        // Hero Sparkle Badge
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(EmeraldSubtle)
                .border(1.dp, BorderHighlight, RoundedCornerShape(20.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "duda.uz • AI Strength & Nutrition",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = EmeraldLight
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = when (lang) {
                "uz" -> "Kuch mashg'ulotlari va ovqatlanishning aqlli kundaligi"
                "en" -> "Your Smart Strength & Nutrition Training Log"
                else -> "Твой умный дневник силовых тренировок и питания"
            },
            fontSize = 24.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 30.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = when (lang) {
                "uz" -> "Mashg'ulotlarni yozib boring, 1RM rekordlarni kuzating, AI yordamida ovqatlanishni tahlil qiling."
                "en" -> "Log workouts with auto rest timers, track 1RM & tonnage, and analyze meals with AI."
                else -> "Логируй подходы и веса, отслеживай 1ПМ и тоннаж, считай КБЖУ с ИИ."
            },
            fontSize = 13.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Permanent Authentication Card
        AthleticCard(
            backgroundColor = DarkSurface,
            borderColor = BorderHighlight
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Section 1: ALWAYS VISIBLE Credentials / Token Input
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(EmeraldSubtle)
                            .border(1.dp, BorderHighlight, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Key,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                    Column {
                        Text(
                            text = when (lang) {
                                "uz" -> "Kirish uchun token / Credentials"
                                "en" -> "Session Token / Credentials"
                                else -> "Вход по токену сессии / Credentials"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = when (lang) {
                                "uz" -> "duda.uz saytidagi np_access_token yoki Google ID token"
                                "en" -> "np_access_token from duda.uz or Google ID token"
                                else -> "np_access_token из duda.uz или Google ID токен"
                            },
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Error Notice
                if (state.errorMessage != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(CrimsonSubtle)
                            .border(1.dp, CrimsonRed.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = state.errorMessage,
                            color = CrimsonRed,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                OutlinedTextField(
                    value = state.tokenInput,
                    onValueChange = { onAction(AuthUiAction.OnTokenInputChanged(it)) },
                    placeholder = {
                        Text(
                            text = "np_access_token или токен Google...",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = DarkSurfaceVariant,
                        unfocusedContainerColor = DarkSurfaceVariant,
                        focusedBorderColor = EmeraldNeon,
                        unfocusedBorderColor = BorderSubtle,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val clip = clipboardManager.getText()?.text.orEmpty()
                            if (clip.isNotBlank()) {
                                onAction(AuthUiAction.OnPasteTokenFromClipboard(clip))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = DarkSurfaceVariant,
                            contentColor = EmeraldLight
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentPaste,
                            contentDescription = null,
                            tint = EmeraldNeon,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (lang) {
                                "uz" -> "Qo'yish"
                                "en" -> "Paste"
                                else -> "Из буфера"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }

                    Button(
                        onClick = { onAction(AuthUiAction.OnSubmitTokenClick) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = EmeraldNeon,
                            contentColor = Color.Black
                        ),
                        modifier = Modifier
                            .weight(1.2f)
                            .height(48.dp)
                    ) {
                        if (state.isLoading) {
                            CircularProgressIndicator(
                                color = Color.Black,
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Login,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = when (lang) {
                                    "uz" -> "Token orqali kirish"
                                    "en" -> "Sign in"
                                    else -> "Войти по токену"
                                },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.Black
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                    Text(
                        text = when (lang) {
                            "uz" -> "YOKI GOOGLE ORQALI"
                            "en" -> "OR VIA GOOGLE"
                            else -> "ИЛИ ЧЕРЕЗ GOOGLE"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Google One-Tap Native Button
                Button(
                    onClick = onTriggerGoogleSignIn,
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF18181B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    GoogleLogoIcon()
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Google orqali kirish"
                            "en" -> "Continue with Google"
                            else -> "Войти через Google"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF18181B)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 2b. Browser Sign-In (duda.uz)
                OutlinedButton(
                    onClick = { onAction(AuthUiAction.OnOpenBrowserLogin) },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Brauzerda kirish (duda.uz)"
                            "en" -> "Sign in via Browser (duda.uz)"
                            else -> "Войти через браузер (duda.uz)"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Divider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                    Text(
                        text = when (lang) {
                            "uz" -> "YOKI OFLAYN REJIMDA"
                            "en" -> "OR OFFLINE MODE"
                            else -> "ИЛИ БЕЗ РЕГИСТРАЦИИ"
                        },
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextMuted,
                        modifier = Modifier.padding(horizontal = 10.dp)
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f), color = BorderSubtle)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Instant Guest Mode (Emerald Neon)
                Button(
                    onClick = { onAction(AuthUiAction.OnContinueAsGuest) },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkSurfaceVariant,
                        contentColor = TextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .border(1.dp, BorderHighlight, RoundedCornerShape(12.dp))
                ) {
                    Icon(
                        imageVector = Icons.Default.ElectricBolt,
                        contentDescription = null,
                        tint = EmeraldNeon,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Mehmon sifatida davom etish (100% Oflayn)"
                            "en" -> "Continue as Guest (100% Offline)"
                            else -> "Продолжить как гость (100% Оффлайн)"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = when (lang) {
                        "uz" -> "Barcha funksiyalar ochiq • Ma'lumotlar qurilmada saqlanadi"
                        "en" -> "All features unlocked • 100% offline-ready"
                        else -> "Все функции активны • Локальное хранилище без ограничений"
                    },
                    fontSize = 11.sp,
                    color = EmeraldLight.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Feature Highlights Matrix (2x2 athletic cards)
        val features = listOf(
            Triple(
                Icons.Default.FitnessCenter,
                when (lang) {
                    "uz" -> "Aqlli mashg'ulot"
                    "en" -> "Smart Workout Log"
                    else -> "Умный лог тренировок"
                },
                when (lang) {
                    "uz" -> "Dam olish taymeri, 1RM va o'tgan og'irliklar"
                    "en" -> "Rest timer, 1RM calculator & past sets"
                    else -> "Авто-таймер отдыха, 1ПМ и прошлые веса"
                }
            ),
            Triple(
                Icons.Default.ShowChart,
                when (lang) {
                    "uz" -> "Haqiqiy tahlil"
                    "en" -> "Real Analytics"
                    else -> "Аналитика и тоннаж"
                },
                when (lang) {
                    "uz" -> "Haftalik tonnaj va 84 kunlik faollik xaritasi"
                    "en" -> "Weekly tonnage & 84-day consistency matrix"
                    else -> "Недельный тоннаж и 84-дневный стрик"
                }
            ),
            Triple(
                Icons.Default.Restaurant,
                when (lang) {
                    "uz" -> "AI Ovqatlanish"
                    "en" -> "AI Nutrition"
                    else -> "ИИ-Дневник питания"
                },
                when (lang) {
                    "uz" -> "Taom rasmidan KBJU hisoblash va maqsadlar"
                    "en" -> "Photo macro scan & daily target balance"
                    else -> "Оценка КБЖУ по фото и баланс макросов"
                }
            ),
            Triple(
                Icons.Default.ElectricBolt,
                when (lang) {
                    "uz" -> "Oflayn rejim"
                    "en" -> "Offline-Ready"
                    else -> "Оффлайн-доступ"
                },
                when (lang) {
                    "uz" -> "Internet bo'lmasa ham to'liq ishlaydi"
                    "en" -> "Works 100% locally with instant sync"
                    else -> "Работает без интернета с авто-сохранением"
                }
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            for (i in features.indices step 2) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val f1 = features[i]
                    Box(modifier = Modifier.weight(1f)) {
                        FeatureHighlightTile(icon = f1.first, title = f1.second, description = f1.third)
                    }
                    if (i + 1 < features.size) {
                        val f2 = features[i + 1]
                        Box(modifier = Modifier.weight(1f)) {
                            FeatureHighlightTile(icon = f2.first, title = f2.second, description = f2.third)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FeatureHighlightTile(
    icon: ImageVector,
    title: String,
    description: String
) {
    AthleticCard(
        backgroundColor = DarkSurfaceVariant,
        borderColor = BorderSubtle
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(EmeraldSubtle)
                    .border(1.dp, BorderHighlight, RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(17.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 11.sp,
                color = TextSecondary,
                lineHeight = 14.sp,
                maxLines = 2
            )
        }
    }
}
