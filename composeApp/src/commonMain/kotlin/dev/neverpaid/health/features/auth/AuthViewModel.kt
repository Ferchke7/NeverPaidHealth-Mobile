package dev.neverpaid.health.features.auth

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class AuthViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<AuthUiAction, AuthUiState, AuthUiEffect>(
    initialState = AuthUiState()
) {

    init {
        repository.getUserPreferences()
            .onEach { prefs ->
                updateState { copy(selectedLanguage = prefs.languageCode) }
            }
            .launchIn(viewModelScope)
    }

    override fun onAction(action: AuthUiAction) {
        when (action) {
            AuthUiAction.OnOpenGoogleSignIn -> {
                updateState { copy(isWebViewOpen = true, errorMessage = null) }
            }
            AuthUiAction.OnDismissGoogleWebView -> {
                updateState { copy(isWebViewOpen = false) }
            }
            is AuthUiAction.OnGoogleWebViewSuccess -> {
                handleWebViewToken(action.accessToken, action.userJson)
            }
            AuthUiAction.OnToggleTokenFallback -> {
                updateState { copy(showTokenFallback = !showTokenFallback, errorMessage = null) }
            }
            is AuthUiAction.OnTokenInputChanged -> {
                updateState { copy(tokenInput = action.value, errorMessage = null) }
            }
            AuthUiAction.OnSubmitTokenClick -> {
                submitManualToken()
            }
            is AuthUiAction.OnSelectLanguage -> {
                launchGuarded {
                    val current = repository.getUserPreferences().first()
                    repository.updatePreferences(current.copy(languageCode = action.languageCode))
                }
            }
        }
    }

    private fun handleWebViewToken(accessToken: String, userJson: String?) {
        updateState { copy(isWebViewOpen = false, isLoading = true, errorMessage = null) }
        launchGuarded(
            onError = { error ->
                updateState { copy(isLoading = false, errorMessage = error.message ?: "Ошибка авторизации через Google") }
            }
        ) {
            repository.authenticateWithAccessToken(accessToken, userJson)
            updateState { copy(isLoading = false) }
            sendEffect(AuthUiEffect.NavigateToHome)
        }
    }

    private fun submitManualToken() {
        val raw = currentState.tokenInput.trim()
        if (raw.isEmpty()) {
            updateState { copy(errorMessage = "Введите токен авторизации") }
            return
        }
        updateState { copy(isLoading = true, errorMessage = null) }
        launchGuarded(
            onError = { error ->
                updateState { copy(isLoading = false, errorMessage = error.message ?: "Ошибка проверки токена") }
            }
        ) {
            val accessResult = runCatching { repository.authenticateWithAccessToken(raw) }
            if (accessResult.isSuccess) {
                updateState { copy(isLoading = false, tokenInput = "") }
                sendEffect(AuthUiEffect.NavigateToHome)
                return@launchGuarded
            }

            val googleResult = runCatching { repository.authenticateWithGoogleIdToken(raw) }
            googleResult.fold(
                onSuccess = {
                    updateState { copy(isLoading = false, tokenInput = "") }
                    sendEffect(AuthUiEffect.NavigateToHome)
                },
                onFailure = { err ->
                    updateState {
                        copy(
                            isLoading = false,
                            errorMessage = err.message ?: "Неверный токен. Войдите через кнопку Google выше."
                        )
                    }
                }
            )
        }
    }
}
