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
        loadPreferences()
    }

    override fun onAction(action: ProfileUiAction) {
        when (action) {
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
        }
    }

    private fun loadPreferences() {
        launchGuarded {
            repository.getUserPreferences().collectLatest { prefs ->
                updateState {
                    copy(
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
    }

    private fun updatePrefs(modify: ProfileUiState.() -> ProfileUiState) {
        val updated = modify(currentState)
        updateState { updated }
        launchGuarded {
            val currentPrefs = repository.getUserPreferences()
            repository.updatePreferences(
                dev.neverpaid.health.domain.repository.UserPreferencesData(
                    useKilograms = updated.useKilograms,
                    defaultRestSeconds = updated.defaultRestSeconds,
                    soundAlertsEnabled = updated.soundAlertsEnabled,
                    vibrationEnabled = updated.vibrationEnabled,
                    serverUrl = updated.serverUrl,
                    isDevMode = updated.isDevMode
                )
            )
        }
    }
}
