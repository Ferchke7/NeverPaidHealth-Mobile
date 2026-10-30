package dev.neverpaid.health.features.coach

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight
import dev.neverpaid.health.domain.model.SuggestedWorkoutPlan

data class CoachUiState(
    val insight: CoachInsight = CoachInsight(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val inputMessage: String = "",
    val attachedImageBase64: String? = null,
    val isSending: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed interface CoachUiAction : UiAction {
    data class OnInputMessageChanged(val text: String) : CoachUiAction
    data class OnAttachPhoto(val base64Jpeg: String) : CoachUiAction
    data object OnClearAttachedPhoto : CoachUiAction
    data object OnSendMessage : CoachUiAction
    data class OnQuickPromptClick(val prompt: String) : CoachUiAction
    data object OnClearChatHistory : CoachUiAction
    data class OnStartSuggestedWorkout(val plan: SuggestedWorkoutPlan) : CoachUiAction
    data class OnSaveSuggestedRoutine(val plan: SuggestedWorkoutPlan) : CoachUiAction
}

sealed interface CoachUiEffect : UiEffect {
    data object NavigateToActiveWorkout : CoachUiEffect
    data class ShowToast(val message: String) : CoachUiEffect
}
