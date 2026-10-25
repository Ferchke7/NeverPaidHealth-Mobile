package dev.neverpaid.health.features.auth

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState

data class AuthUiState(
    val isLoginTab: Boolean = true,
    val email: String = "",
    val password: String = "",
    val name: String = "",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState

sealed interface AuthUiAction : UiAction {
    data class OnToggleTab(val isLogin: Boolean) : AuthUiAction
    data class OnEmailChanged(val email: String) : AuthUiAction
    data class OnPasswordChanged(val password: String) : AuthUiAction
    data class OnNameChanged(val name: String) : AuthUiAction
    data object OnSubmitClick : AuthUiAction
    data object OnGuestModeClick : AuthUiAction
}

sealed interface AuthUiEffect : UiEffect {
    data object NavigateToHome : AuthUiEffect
    data class ShowToast(val message: String) : AuthUiEffect
}
