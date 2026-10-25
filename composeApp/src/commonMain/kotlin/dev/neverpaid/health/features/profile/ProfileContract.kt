package dev.neverpaid.health.features.profile

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState

data class ProfileUiState(
    val userName: String = "Athlete",
    val userEmail: String = "athlete@duda.uz",
    val languageCode: String = "ru",
    val useKilograms: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val serverUrl: String = "https://duda.uz/api/v1",
    val isDevMode: Boolean = false
) : UiState

sealed interface ProfileUiAction : UiAction {
    data class OnSelectLanguage(val languageCode: String) : ProfileUiAction
    data class OnToggleUnits(val useKg: Boolean) : ProfileUiAction
    data class OnSelectDefaultRest(val seconds: Int) : ProfileUiAction
    data class OnToggleSound(val enabled: Boolean) : ProfileUiAction
    data class OnToggleVibration(val enabled: Boolean) : ProfileUiAction
    data class OnUpdateServerUrl(val url: String) : ProfileUiAction
    data object OnLogoutClick : ProfileUiAction
}

sealed interface ProfileUiEffect : UiEffect {
    data object NavigateToAuth : ProfileUiEffect
}
