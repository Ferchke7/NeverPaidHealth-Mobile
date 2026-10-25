package dev.neverpaid.health.features.profile

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState

data class ProfileUiState(
    val useKilograms: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val serverUrl: String = "http://10.0.2.2:8080/api/v1",
    val isDevMode: Boolean = true
) : UiState

sealed interface ProfileUiAction : UiAction {
    data class OnToggleUnits(val useKg: Boolean) : ProfileUiAction
    data class OnSelectDefaultRest(val seconds: Int) : ProfileUiAction
    data class OnToggleSound(val enabled: Boolean) : ProfileUiAction
    data class OnToggleVibration(val enabled: Boolean) : ProfileUiAction
    data class OnUpdateServerUrl(val url: String) : ProfileUiAction
}

sealed interface ProfileUiEffect : UiEffect
