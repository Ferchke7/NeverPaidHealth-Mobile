package dev.neverpaid.health.features.profile

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.FeedbackItem
import dev.neverpaid.health.domain.model.PlatformStats
import dev.neverpaid.health.domain.model.User

data class ProfileUiState(
    val user: User? = null,
    val userName: String = "Атлет",
    val userEmail: String = "",
    val languageCode: String = "ru",
    val useKilograms: Boolean = true,
    val defaultRestSeconds: Int = 90,
    val soundAlertsEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val calorieTargetInput: String = "2400",
    val proteinTargetInput: String = "175",
    val carbsTargetInput: String = "250",
    val fatsTargetInput: String = "75",
    val waterGoalInput: String = "2500",
    val platformStats: PlatformStats = PlatformStats(),
    val feedbackList: List<FeedbackItem> = emptyList(),
    val selectedFeedbackFilter: String = "all",
    val isFeedbackModalOpen: Boolean = false,
    val feedbackCategory: String = "idea",
    val feedbackRating: Int = 5,
    val feedbackMessage: String = "",
    val isSubmittingFeedback: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
) : UiState

sealed interface ProfileUiAction : UiAction {
    data object OnRefresh : ProfileUiAction
    data class OnSelectLanguage(val languageCode: String) : ProfileUiAction
    data class OnToggleUnits(val useKg: Boolean) : ProfileUiAction
    data class OnSelectDefaultRest(val seconds: Int) : ProfileUiAction
    data class OnToggleSound(val enabled: Boolean) : ProfileUiAction
    data class OnToggleVibration(val enabled: Boolean) : ProfileUiAction
    data class OnUpdateCalorieTarget(val value: String) : ProfileUiAction
    data class OnUpdateProteinTarget(val value: String) : ProfileUiAction
    data class OnUpdateCarbsTarget(val value: String) : ProfileUiAction
    data class OnUpdateFatsTarget(val value: String) : ProfileUiAction
    data class OnUpdateWaterGoal(val value: String) : ProfileUiAction
    data object OnSaveMacroTargets : ProfileUiAction
    data class OnSelectFeedbackFilter(val filter: String) : ProfileUiAction
    data object OnOpenFeedbackModal : ProfileUiAction
    data object OnCloseFeedbackModal : ProfileUiAction
    data class OnSelectFeedbackCategory(val category: String) : ProfileUiAction
    data class OnSelectFeedbackRating(val rating: Int) : ProfileUiAction
    data class OnUpdateFeedbackMessage(val message: String) : ProfileUiAction
    data object OnSubmitFeedback : ProfileUiAction
    data class OnToggleFeedbackUpvote(val feedbackId: String) : ProfileUiAction
    data object OnLogoutClick : ProfileUiAction
    data object OnDismissMessages : ProfileUiAction
}

sealed interface ProfileUiEffect : UiEffect {
    data object NavigateToAuth : ProfileUiEffect
}
