package dev.neverpaid.health.features.auth

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState

data class AuthUiState(
    val isWebViewOpen: Boolean = false,
    val showTokenFallback: Boolean = false,
    val tokenInput: String = "",
    val selectedLanguage: String = "ru",
    val isLoading: Boolean = false,
    val errorMessage: String? = null
) : UiState

sealed interface AuthUiAction : UiAction {
    data object OnOpenGoogleSignIn : AuthUiAction
    data class OnGoogleTokenCaptured(val idToken: String) : AuthUiAction
    data class OnGoogleAuthError(val message: String) : AuthUiAction
    data object OnDismissGoogleWebView : AuthUiAction
    data class OnGoogleWebViewSuccess(val accessToken: String, val userJson: String?) : AuthUiAction
    data object OnToggleTokenFallback : AuthUiAction
    data class OnTokenInputChanged(val value: String) : AuthUiAction
    data object OnSubmitTokenClick : AuthUiAction
    data class OnSelectLanguage(val languageCode: String) : AuthUiAction
}

sealed interface AuthUiEffect : UiEffect {
    data object NavigateToHome : AuthUiEffect
    data class ShowToast(val message: String) : AuthUiEffect
}
