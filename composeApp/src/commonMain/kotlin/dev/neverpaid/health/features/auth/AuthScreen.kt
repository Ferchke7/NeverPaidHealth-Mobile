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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.neverpaid.health.core.designsystem.AthleticCard
import dev.neverpaid.health.core.designsystem.BorderHighlight
import dev.neverpaid.health.core.designsystem.BorderSubtle
import dev.neverpaid.health.core.designsystem.CrimsonRed
import dev.neverpaid.health.core.designsystem. CrimsonSubtle
import dev.neverpaid.health.core.designsystem.DarkBackground
import dev.neverpaid.health.core.designsystem.DarkSurface
import dev.neverpaid.health.core.designsystem.DarkSurfaceHigh
import dev.neverpaid.health.core.designsystem.DarkSurfaceVariant
import dev.neverpaid.health.core.designsystem.DudaBrandLogo
import dev.neverpaid.health.core.designsystem.EmeraldLight
import dev.neverpaid.health.core.designsystem.EmeraldNeon
import dev.neverpaid.health.core.designsystem.EmeraldSubtle
import dev.neverpaid.health.core.designsystem.TextMuted
import dev.neverpaid.health.core.designsystem.TextPrimary
import dev.neverpaid.health.core.designsystem.TextSecondary
import dev.neverpaid.health.core.i18n.LocalDudaStrings
import dev.neverpaid.health.core.mvi.CollectEffect
import dev.neverpaid.health.core.platform.GoogleOAuthWebViewModal

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

    if (state.isWebViewOpen) {
        GoogleOAuthWebViewModal(
            isOpen = state.isWebViewOpen,
            authUrl = "https://duda.uz/login",
            onDismiss = {
                viewModel.onAction(AuthUiAction.OnDismissGoogleWebView)
            },
            onTokenCaptured = { accessToken, userJson ->
                viewModel.onAction(AuthUiAction.OnGoogleWebViewSuccess(accessToken, userJson))
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = DarkBackground
    ) { innerPadding ->
        AuthScreen(
            state = state,
            onAction = viewModel::onAction,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
fun AuthScreen(
    state: AuthUiState,
    onAction: (AuthUiAction) -> Unit,
    modifier: Modifier = Modifier
) {
    val strings = LocalDudaStrings.current
    val scrollState = rememberScrollState()
    val lang = state.selectedLanguage.lowercase()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header: Brand Logo + Inline Language Switcher (RU / UZ / EN) matching LoginPage.tsx
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

        Spacer(modifier = Modifier.height(28.dp))

        // Badge pill
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

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = when (lang) {
                "uz" -> "Kuch mashg'ulotlari va ovqatlanishning aqlli kundaligi"
                "en" -> "Your Smart Strength & Nutrition Training Log"
                else -> "Твой умный дневник силовых тренировок"
            },
            fontSize = 26.sp,
            fontWeight = FontWeight.Black,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 32.sp
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = when (lang) {
                "uz" -> "Mashg'ulotlarni yozib boring, 1RM rekordlarni kuzating, AI yordamida kaloriyalarni hisoblang va duda.uz bilan sinxronlang."
                "en" -> "Log workouts with automatic rest timers, track 1RM & tonnage, analyze meals with AI, and sync seamlessly with duda.uz."
                else -> "Логируй подходы и веса, отслеживай прогресс 1ПМ и тоннаж, считай КБЖУ по фото с ИИ и синхронизируй все данные с duda.uz."
            },
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Main Login Card (1:1 with LoginPage.tsx right-hand auth card)
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
                Text(
                    text = when (lang) {
                        "uz" -> "duda.uz hisobiga kirish"
                        "en" -> "Sign in to duda.uz"
                        else -> "Вход в аккаунт duda.uz"
                    },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = when (lang) {
                        "uz" -> "Barcha mashg'ulotlaringiz va dasturlaringiz bulutda saqlanadi"
                        "en" -> "All your workouts, routines, and AI logs sync with your cloud profile"
                        else -> "Все ваши тренировки, программы и дневник питания синхронизируются с облаком"
                    },
                    fontSize = 12.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

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
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Primary Google Sign-In Button
                Button(
                    onClick = { onAction(AuthUiAction.OnOpenGoogleSignIn) },
                    enabled = !state.isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF18181B)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(
                            color = Color(0xFF18181B),
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF4285F4)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "G",
                                color = Color.White,
                                fontWeight = FontWeight.Black,
                                fontSize = 14.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = when (lang) {
                                "uz" -> "Google orqali kirish"
                                "en" -> "Continue with Google"
                                else -> "Войти через Google"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = { onAction(AuthUiAction.OnToggleTokenFallback) }
                ) {
                    Icon(
                        imageVector = Icons.Default.Key,
                        contentDescription = null,
                        tint = TextSecondary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (lang) {
                            "uz" -> "Token orqali kirish (ixtiyoriy)"
                            "en" -> "Sign in with Session / Google Token"
                            else -> "Войти по токену сессии (если браузер закрыт)"
                        },
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                if (state.showTokenFallback) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = state.tokenInput,
                        onValueChange = { onAction(AuthUiAction.OnTokenInputChanged(it)) },
                        label = {
                            Text(
                                when (lang) {
                                    "uz" -> "duda.uz Access Token yoki Google ID Token"
                                    "en" -> "duda.uz Access Token or Google ID Token"
                                    else -> "Токен сессии (np_access_token) или Google ID Token"
                                },
                                fontSize = 12.sp
                            )
                        },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp),
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
                    OutlinedButton(
                        onClick = { onAction(AuthUiAction.OnSubmitTokenClick) },
                        enabled = !state.isLoading,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = when (lang) {
                                "uz" -> "Tokenni tasdiqlash"
                                "en" -> "Verify & Sign In"
                                else -> "Подтвердить и войти"
                            },
                            fontWeight = FontWeight.Bold,
                            color = EmeraldLight
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4 Feature Cards matching LoginPage.tsx
        val features = listOf(
            Triple(
                Icons.Default.FitnessCenter,
                when (lang) {
                    "uz" -> "Aqlli mashg'ulot kundaligi"
                    "en" -> "Smart Workout Logger"
                    else -> "Умный лог тренировок"
                },
                when (lang) {
                    "uz" -> "Oldingi natijalar, dam olish taymeri va 1RM hisobi"
                    "en" -> "Previous set autofill, rest timer & live 1RM calculation"
                    else -> "Автоподстановка прошлых весов, таймер отдыха и расчёт 1ПМ"
                }
            ),
            Triple(
                Icons.Default.Layers,
                when (lang) {
                    "uz" -> "Mashg'ulot dasturlari va splitlar"
                    "en" -> "Training Programs & Splits"
                    else -> "Готовые программы и сплиты"
                },
                when (lang) {
                    "uz" -> "PPL, Upper/Lower, Full Body va hamjamiyat dasturlari"
                    "en" -> "PPL, Upper/Lower, Full Body & community templates"
                    else -> "PPL, Upper/Lower, Full Body, отзывы атлетов и свои шаблоны"
                }
            ),
            Triple(
                Icons.Default.AutoAwesome,
                when (lang) {
                    "uz" -> "AI Murabbiy va Foto-tahlil"
                    "en" -> "AI Coach & Photo Macro Scan"
                    else -> "ИИ-Тренер и анализ еды по фото"
                },
                when (lang) {
                    "uz" -> "Taom rasmidan kaloriyalarni aniqlash va shaxsiy AI murabbiy"
                    "en" -> "Instant meal photo calorie estimation & personal AI coach"
                    else -> "Оценка КБЖУ блюда по фотографии и персональный ИИ-коуч"
                }
            ),
            Triple(
                Icons.Default.CalendarToday,
                when (lang) {
                    "uz" -> "Kunlik reja va Fokus taymeri"
                    "en" -> "Daily Plan & Focus Timer"
                    else -> "План дня и Фокус-таймер"
                },
                when (lang) {
                    "uz" -> "Kunlik vazifalar, Pomodoro taymeri va faollik jurnali"
                    "en" -> "Athlete checklist, Pomodoro focus timer & activity log"
                    else -> "Чек-лист атлета, Помодоро таймер и календарь активности"
                }
            )
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            features.forEach { (icon, title, desc) ->
                FeatureShowcaseCard(icon = icon, title = title, description = desc)
            }
        }

        Spacer(modifier = Modifier.height(28.dp))
    }
}

@Composable
private fun FeatureShowcaseCard(
    icon: ImageVector,
    title: String,
    description: String
) {
    AthleticCard(
        backgroundColor = DarkSurfaceVariant,
        borderColor = BorderSubtle
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(EmeraldSubtle)
                    .border(1.dp, BorderHighlight, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = EmeraldLight,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = TextSecondary,
                    lineHeight = 16.sp
                )
            }
        }
    }
}
