package dev.neverpaid.health.features.profile

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.flow.collectLatest

class ProfileViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ProfileUiAction, ProfileUiState, ProfileUiEffect>(
    initialState = ProfileUiState()
) {

    init {
        observeProfileAndPreferences()
        refreshData()
    }

    override fun onAction(action: ProfileUiAction) {
        when (action) {
            ProfileUiAction.OnRefresh -> refreshData()
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
            is ProfileUiAction.OnUpdateCalorieTarget -> {
                updateState { copy(calorieTargetInput = action.value) }
            }
            is ProfileUiAction.OnUpdateProteinTarget -> {
                updateState { copy(proteinTargetInput = action.value) }
            }
            is ProfileUiAction.OnUpdateCarbsTarget -> {
                updateState { copy(carbsTargetInput = action.value) }
            }
            is ProfileUiAction.OnUpdateFatsTarget -> {
                updateState { copy(fatsTargetInput = action.value) }
            }
            is ProfileUiAction.OnUpdateWaterGoal -> {
                updateState { copy(waterGoalInput = action.value) }
            }
            ProfileUiAction.OnSaveMacroTargets -> saveMacroTargets()
            is ProfileUiAction.OnSelectFeedbackFilter -> {
                updateState { copy(selectedFeedbackFilter = action.filter) }
            }
            ProfileUiAction.OnOpenFeedbackModal -> {
                updateState {
                    copy(
                        isFeedbackModalOpen = true,
                        feedbackCategory = "feature",
                        feedbackRating = 5,
                        feedbackMessage = "",
                        errorMessage = null
                    )
                }
            }
            ProfileUiAction.OnCloseFeedbackModal -> {
                updateState { copy(isFeedbackModalOpen = false) }
            }
            is ProfileUiAction.OnSelectFeedbackCategory -> {
                updateState { copy(feedbackCategory = action.category) }
            }
            is ProfileUiAction.OnSelectFeedbackRating -> {
                updateState { copy(feedbackRating = action.rating.coerceIn(1, 5)) }
            }
            is ProfileUiAction.OnUpdateFeedbackMessage -> {
                updateState { copy(feedbackMessage = action.message) }
            }
            ProfileUiAction.OnSubmitFeedback -> submitFeedback()
            is ProfileUiAction.OnToggleFeedbackUpvote -> toggleUpvote(action.feedbackId)
            ProfileUiAction.OnLogoutClick -> logout()
            ProfileUiAction.OnDismissMessages -> {
                updateState { copy(errorMessage = null, successMessage = null) }
            }
        }
    }

    private fun observeProfileAndPreferences() {
        launchGuarded {
            repository.getUserPreferences().collectLatest { prefs ->
                updateState {
                    copy(
                        languageCode = prefs.languageCode,
                        useKilograms = prefs.useKilograms,
                        defaultRestSeconds = prefs.defaultRestSeconds,
                        soundAlertsEnabled = prefs.soundAlertsEnabled,
                        vibrationEnabled = prefs.vibrationEnabled
                    )
                }
            }
        }

        launchGuarded {
            repository.getCurrentUser().collectLatest { user ->
                if (user != null) {
                    updateState {
                        copy(
                            user = user,
                            userName = user.name.ifBlank { "Атлет" },
                            userEmail = user.email,
                            calorieTargetInput = user.targetCalories.toString(),
                            proteinTargetInput = user.targetProteinG.toString(),
                            carbsTargetInput = user.targetCarbsG.toString(),
                            fatsTargetInput = user.targetFatG.toString()
                        )
                    }
                }
            }
        }
    }

    private fun refreshData() {
        launchGuarded(
            onError = { err ->
                updateState { copy(isLoading = false, errorMessage = err.message) }
            }
        ) {
            updateState { copy(isLoading = true, errorMessage = null) }
            val stats = runCatching { repository.getPlatformStats() }.getOrDefault(currentState.platformStats)
            val feedbacks = runCatching { repository.getFeedbackList() }.getOrDefault(currentState.feedbackList)
            updateState {
                copy(
                    isLoading = false,
                    platformStats = stats,
                    feedbackList = feedbacks
                )
            }
        }
    }

    private fun saveMacroTargets() {
        val state = currentState
        val cal = state.calorieTargetInput.toIntOrNull()?.coerceIn(800, 10000) ?: 2400
        val p = state.proteinTargetInput.toIntOrNull()?.coerceIn(20, 600) ?: 175
        val c = state.carbsTargetInput.toIntOrNull()?.coerceIn(20, 1000) ?: 250
        val f = state.fatsTargetInput.toIntOrNull()?.coerceIn(10, 400) ?: 75

        launchGuarded(
            onError = { err ->
                updateState { copy(errorMessage = err.message) }
            }
        ) {
            repository.updateUserStatsLocal {
                it.copy(
                    targetCalories = cal,
                    targetProteinG = p,
                    targetCarbsG = c,
                    targetFatG = f
                )
            }
            updateState { copy(successMessage = "Цели КБЖУ сохранены") }
        }
    }

    private fun submitFeedback() {
        val state = currentState
        val msg = state.feedbackMessage.trim()
        if (msg.isBlank()) {
            updateState { copy(errorMessage = "Напишите ваш отзыв или предложение") }
            return
        }

        launchGuarded(
            onError = { err ->
                updateState {
                    copy(
                        isSubmittingFeedback = false,
                        errorMessage = err.message ?: "Ошибка отправки отзыва"
                    )
                }
            }
        ) {
            updateState { copy(isSubmittingFeedback = true, errorMessage = null) }
            val created = repository.submitFeedback(
                category = state.feedbackCategory,
                title = msg.take(60),
                description = msg
            )
            updateState {
                copy(
                    isSubmittingFeedback = false,
                    isFeedbackModalOpen = false,
                    feedbackMessage = "",
                    feedbackList = listOf(created) + feedbackList,
                    successMessage = "Спасибо за ваш отзыв!"
                )
            }
        }
    }

    private fun toggleUpvote(feedbackId: String) {
        launchGuarded(
            onError = { err ->
                updateState { copy(errorMessage = err.message) }
            }
        ) {
            repository.upvoteFeedback(feedbackId)
            val updated = runCatching { repository.getFeedbackList() }.getOrDefault(currentState.feedbackList)
            updateState { copy(feedbackList = updated) }
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
                UserPreferencesData(
                    useKilograms = updated.useKilograms,
                    defaultRestSeconds = updated.defaultRestSeconds,
                    soundAlertsEnabled = updated.soundAlertsEnabled,
                    vibrationEnabled = updated.vibrationEnabled,
                    languageCode = updated.languageCode
                )
            )
        }
    }
}
