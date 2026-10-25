package dev.neverpaid.health.features.auth

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository

class AuthViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<AuthUiAction, AuthUiState, AuthUiEffect>(
    initialState = AuthUiState()
) {

    override fun onAction(action: AuthUiAction) {
        when (action) {
            is AuthUiAction.OnToggleTab -> {
                updateState { copy(isLoginTab = action.isLogin, errorMessage = null) }
            }
            is AuthUiAction.OnEmailChanged -> {
                updateState { copy(email = action.email, errorMessage = null) }
            }
            is AuthUiAction.OnPasswordChanged -> {
                updateState { copy(password = action.password, errorMessage = null) }
            }
            is AuthUiAction.OnNameChanged -> {
                updateState { copy(name = action.name, errorMessage = null) }
            }
            AuthUiAction.OnSubmitClick -> submitAuth()
            AuthUiAction.OnGuestModeClick -> continueGuestMode()
        }
    }

    private fun submitAuth() {
        val email = currentState.email.trim()
        val password = currentState.password.trim()
        val name = currentState.name.trim()

        if (email.isEmpty() || password.isEmpty()) {
            updateState { copy(errorMessage = "Please enter email and password") }
            return
        }

        updateState { copy(isLoading = true, errorMessage = null) }

        launchGuarded(
            onError = { error ->
                updateState { copy(isLoading = false, errorMessage = error.message) }
            }
        ) {
            if (currentState.isLoginTab) {
                repository.login(email, password)
            } else {
                repository.register(email, password, name.ifEmpty { "Athlete" })
            }
            updateState { copy(isLoading = false) }
            sendEffect(AuthUiEffect.NavigateToHome)
        }
    }

    private fun continueGuestMode() {
        launchGuarded(
            onError = { error ->
                updateState { copy(errorMessage = error.message) }
            }
        ) {
            repository.login("athlete@duda.uz", "offline-athlete")
            sendEffect(AuthUiEffect.NavigateToHome)
        }
    }
}
