package dev.neverpaid.health.features.coach

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine

class CoachViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<CoachUiAction, CoachUiState, CoachUiEffect>(
    initialState = CoachUiState(isLoading = true)
) {

    init {
        loadCoachData()
    }

    override fun onAction(action: CoachUiAction) {
        when (action) {
            is CoachUiAction.OnInputMessageChanged -> {
                updateState { copy(inputMessage = action.text) }
            }
            CoachUiAction.OnSendMessage -> {
                val text = currentState.inputMessage.trim()
                if (text.isNotEmpty()) {
                    sendMessage(text)
                }
            }
            is CoachUiAction.OnQuickPromptClick -> {
                sendMessage(action.prompt)
            }
        }
    }

    private fun loadCoachData() {
        launchGuarded {
            combine(
                repository.getCoachInsight(),
                repository.getCoachChat()
            ) { insight, chat ->
                Pair(insight, chat)
            }.collect { (insight, chat) ->
                updateState {
                    copy(
                        insight = insight,
                        chatMessages = chat,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun sendMessage(text: String) {
        updateState { copy(inputMessage = "", isSending = true) }
        launchGuarded {
            repository.sendCoachChatMessage(text)
            updateState { copy(isSending = false) }
        }
    }
}
