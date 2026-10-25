package dev.neverpaid.health.features.profile

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class ProfileViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ProfileUiAction, ProfileUiState, ProfileUiEffect>(
    initialState = ProfileUiState()
) {

    init {
        loadProfileAndPreferences()
    }

    override fun onAction(action: ProfileUiAction) {
        when (action) {
            is ProfileUiAction.OnSelectLanguage -> {
                updatePrefs { copy(languageCode = action.languageCode) }
            }
            is ProfileUiAction.OnToggleUnits -> {
                updatePrefs { copy(useKilograms = action.useKg) }
            }
            is ProfileUiAction.OnSelectDefaultRest -> {
                updatePrefs { copy(defaultRestSeconds = action.seconds) }
            }
            is ProfileUiAction.OnToggleSound -> {
                updatePrefs { copy(soundAlertsEnabled = action.enabled) }
            }
            is ProfileUiAction.OnToggleVibration -> {
                updatePrefs { copy(vibrationEnabled = action.enabled) }
            }
            is ProfileUiAction.OnUpdateServerUrl -> {
                updatePrefs { copy(serverUrl = action.url) }
            }
            ProfileUiAction.OnLogoutClick -> logout()
        }
    }

    private fun loadProfileAndPreferences() {
        launchGuarded {
            repository.getUserPreferences().collectLatest { prefs ->
                updateState {
                    copy(
                        languageCode = prefs.languageCode,
                        useKilograms = prefs.useKilograms,
                        defaultRestSeconds = prefs.defaultRestSeconds,
                        soundAlertsEnabled = prefs.soundAlertsEnabled,
                        vibrationEnabled = prefs.vibrationEnabled,
                        serverUrl = prefs.serverUrl,
                        isDevMode = prefs.isDevMode
                    )
                }
            }
        }

        launchGuarded {
            repository.getCurrentUser().collectLatest { user ->
                if (user != null) {
                    updateState {
                        copy(
                            userName = user.name,
                            userEmail = user.email
                        )
                    }
                }
            }
        }
    }

    private fun logout() {
        launchGuarded {
            repository.logout()
            sendEffect(ProfileUiEffect.NavigateToAuth)
        }
    }

    private fun updatePrefs(modify: ProfileUiState.() -> ProfileUiState) {
        val updated = modify(currentState)
        updateState { updated }
        launchGuarded {
            repository.updatePreferences(
                dev.neverpaid.health.domain.repository.UserPreferencesData(
                    useKilograms = updated.useKilograms,
                    defaultRestSeconds = updated.defaultRestSeconds,
                    soundAlertsEnabled = updated.soundAlertsEnabled,
                    vibrationEnabled = updated.vibrationEnabled,
                    serverUrl = updated.serverUrl,
                    isDevMode = updated.isDevMode,
                    languageCode = updated.languageCode
                )
            )
        }
    }
}
