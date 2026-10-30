package dev.neverpaid.health.features.coach

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.model.RoutineExercise
import dev.neverpaid.health.domain.model.SaveRoutineRequestDto
import dev.neverpaid.health.domain.model.SuggestedWorkoutPlan
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class CoachViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<CoachUiAction, CoachUiState, CoachUiEffect>(
    initialState = CoachUiState(isLoading = true)
) {

    init {
        observeCoachStreams()
        refreshCoachRemote()
    }

    override fun onAction(action: CoachUiAction) {
        when (action) {
            is CoachUiAction.OnInputMessageChanged -> {
                updateState { copy(inputMessage = action.text) }
            }
            is CoachUiAction.OnAttachPhoto -> {
                updateState { copy(attachedImageBase64 = action.base64Jpeg) }
            }
            CoachUiAction.OnClearAttachedPhoto -> {
                updateState { copy(attachedImageBase64 = null) }
            }
            CoachUiAction.OnSendMessage -> {
                val text = currentState.inputMessage.trim()
                val img = currentState.attachedImageBase64
                if (text.isNotEmpty() || !img.isNullOrBlank()) {
                    sendMessage(text.ifEmpty { "Проанализируй это фото" }, img)
                }
            }
            is CoachUiAction.OnQuickPromptClick -> {
                sendMessage(action.prompt, null)
            }
            CoachUiAction.OnClearChatHistory -> {
                launchGuarded {
                    repository.clearCoachChatHistory()
                    sendEffect(CoachUiEffect.ShowToast("История чата очищена"))
                }
            }
            is CoachUiAction.OnStartSuggestedWorkout -> {
                startSuggestedWorkout(action.plan)
            }
            is CoachUiAction.OnSaveSuggestedRoutine -> {
                saveSuggestedRoutine(action.plan)
            }
        }
    }

    private fun observeCoachStreams() {
        combine(
            repository.getCoachInsight(),
            repository.getCoachChat()
        ) { insight, chat ->
            updateState {
                copy(
                    insight = insight,
                    chatMessages = chat,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshCoachRemote() {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.refreshCoachInsights()
            repository.refreshCoachMessages()
            updateState { copy(isLoading = false) }
        }
    }

    private fun sendMessage(text: String, imageBase64: String?) {
        updateState { copy(inputMessage = "", attachedImageBase64 = null, isSending = true) }
        launchGuarded(
            onError = {
                updateState { copy(isSending = false) }
            }
        ) {
            repository.sendCoachChatMessage(text, imageBase64)
            updateState { copy(isSending = false) }
        }
    }

    private fun startSuggestedWorkout(plan: SuggestedWorkoutPlan) {
        val routine = Routine(
            id = "ai-plan",
            name = plan.title,
            notes = plan.rationale,
            exercises = plan.exercises.mapIndexed { idx, ex ->
                RoutineExercise(
                    exerciseId = ex.exerciseId.ifBlank { ex.exerciseName },
                    exerciseName = ex.exerciseName,
                    orderIndex = idx + 1,
                    targetSets = ex.sets,
                    targetRepsMin = ex.repsMin,
                    targetRepsMax = ex.repsMax
                )
            }
        )
        repository.startWorkout(plan.title, routine)
        sendEffect(CoachUiEffect.NavigateToActiveWorkout)
    }

    private fun saveSuggestedRoutine(plan: SuggestedWorkoutPlan) {
        launchGuarded {
            val req = SaveRoutineRequestDto(
                title = plan.title,
                description = plan.rationale,
                exercises = plan.exercises.mapIndexed { idx, ex ->
                    RoutineExercise(
                        exerciseId = ex.exerciseId.ifBlank { ex.exerciseName },
                        exerciseName = ex.exerciseName,
                        orderIndex = idx + 1,
                        targetSets = ex.sets,
                        targetRepsMin = ex.repsMin,
                        targetRepsMax = ex.repsMax
                    )
                }
            )
            repository.createRoutine(req)
            sendEffect(CoachUiEffect.ShowToast("Шаблон «${plan.title}» сохранён в Мои рутины!"))
        }
    }
}
