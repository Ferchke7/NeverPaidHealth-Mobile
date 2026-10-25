package dev.neverpaid.health.features.programs

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.Routine
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class ProgramsViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ProgramsUiAction, ProgramsUiState, ProgramsUiEffect>(
    initialState = ProgramsUiState(isLoading = true)
) {

    init {
        loadPrograms()
    }

    override fun onAction(action: ProgramsUiAction) {
        when (action) {
            is ProgramsUiAction.OnSelectProgram -> {
                updateState { copy(selectedProgram = action.program) }
            }
            is ProgramsUiAction.OnActivateProgram -> activateProgram(action.programId)
            is ProgramsUiAction.OnToggleLike -> toggleLike(action.programId)
            is ProgramsUiAction.OnOpenComments -> openComments(action.programId)
            ProgramsUiAction.OnCloseComments -> closeComments()
            is ProgramsUiAction.OnCommentTextChanged -> {
                updateState { copy(newCommentText = action.text) }
            }
            is ProgramsUiAction.OnReplyClick -> {
                updateState { copy(replyingToComment = action.comment) }
            }
            ProgramsUiAction.OnCancelReply -> {
                updateState { copy(replyingToComment = null) }
            }
            ProgramsUiAction.OnSendComment -> sendComment()
            is ProgramsUiAction.OnStartProgramDay -> startDayWorkout(action.day)
        }
    }

    private fun loadPrograms() {
        launchGuarded {
            repository.getPrograms().collectLatest { list ->
                updateState {
                    copy(
                        programs = list,
                        selectedProgram = selectedProgram ?: list.find { it.isActive } ?: list.firstOrNull(),
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun activateProgram(programId: String) {
        launchGuarded {
            repository.activateProgram(programId)
            sendEffect(ProgramsUiEffect.ShowToast("Program activated"))
        }
    }

    private fun toggleLike(programId: String) {
        launchGuarded {
            repository.toggleLikeProgram(programId)
        }
    }

    private fun openComments(programId: String) {
        updateState { copy(activeCommentsProgramId = programId) }
        launchGuarded {
            repository.getProgramComments(programId).collectLatest { commentList ->
                updateState { copy(comments = commentList) }
            }
        }
    }

    private fun closeComments() {
        updateState {
            copy(
                activeCommentsProgramId = null,
                newCommentText = "",
                replyingToComment = null
            )
        }
    }

    private fun sendComment() {
        val programId = currentState.activeCommentsProgramId ?: return
        val text = currentState.newCommentText.trim()
        if (text.isEmpty()) return

        val parentId = currentState.replyingToComment?.id
        updateState { copy(newCommentText = "", replyingToComment = null) }

        launchGuarded {
            repository.addProgramComment(programId, text, parentId)
        }
    }

    private fun startDayWorkout(day: ProgramDay) {
        val routine = Routine(
            id = "prog-day-${day.dayNumber}",
            title = day.title,
            description = day.splitType,
            exercises = day.exercises
        )
        repository.startWorkout(day.title, routine)
        sendEffect(ProgramsUiEffect.NavigateToActiveWorkout)
    }
}
