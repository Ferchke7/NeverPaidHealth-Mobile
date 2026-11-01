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
                updateState { copy(errorMessage = null) }
            }
            AuthUiAction.OnContinueAsGuest -> {
                repository.continueAsGuest()
                sendEffect(AuthUiEffect.NavigateToHome)
            }
            is AuthUiAction.OnPasteTokenFromClipboard -> {
                val clean = action.text.trim().trim('"', '\'')
                if (clean.isNotBlank()) {
                    updateState { copy(tokenInput = clean, errorMessage = null) }
                    submitManualToken(clean)
                }
            }
            is AuthUiAction.OnGoogleTokenCaptured -> {
                handleGoogleIdToken(action.idToken)
            }
            is AuthUiAction.OnGoogleAuthError -> {
                updateState { copy(isLoading = false, errorMessage = action.message) }
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

    private fun handleGoogleIdToken(idToken: String) {
        updateState { copy(isLoading = true, errorMessage = null) }
        launchGuarded(
            onError = { error ->
                updateState {
                    copy(
                        isLoading = false,
                        errorMessage = error.message ?: "Ошибка авторизации через Google"
                    )
                }
            }
        ) {
            repository.authenticateWithGoogleIdToken(idToken)
            updateState { copy(isLoading = false) }
            sendEffect(AuthUiEffect.NavigateToHome)
        }
    }

    private fun submitManualToken(overrideToken: String? = null) {
        val raw = (overrideToken ?: currentState.tokenInput).trim().trim('"', '\'')
        if (raw.isEmpty()) {
            updateState { copy(errorMessage = "Введите токен или учетные данные") }
            return
        }
        updateState { copy(isLoading = true, errorMessage = null) }
        launchGuarded(
            onError = { error ->
                updateState { copy(isLoading = false, errorMessage = error.message ?: "Ошибка авторизации по токену") }
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
                            errorMessage = err.message ?: "Неверный токен. Скопируйте np_access_token из duda.uz или войдите через Google."
                        )
                    }
                }
            )
        }
    }
}
