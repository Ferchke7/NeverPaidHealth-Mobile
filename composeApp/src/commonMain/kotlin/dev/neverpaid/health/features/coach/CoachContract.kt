package dev.neverpaid.health.features.coach

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.ChatMessage
import dev.neverpaid.health.domain.model.CoachInsight

data class CoachUiState(
    val insight: CoachInsight = CoachInsight(),
    val chatMessages: List<ChatMessage> = emptyList(),
    val inputMessage: String = "",
    val isSending: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed interface CoachUiAction : UiAction {
    data class OnInputMessageChanged(val text: String) : CoachUiAction
    data object OnSendMessage : CoachUiAction
    data class OnQuickPromptClick(val prompt: String) : CoachUiAction
}

sealed interface CoachUiEffect : UiEffect
