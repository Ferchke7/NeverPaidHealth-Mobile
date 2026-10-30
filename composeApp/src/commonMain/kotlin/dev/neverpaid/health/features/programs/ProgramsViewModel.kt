package dev.neverpaid.health.features.programs

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class ProgramsViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ProgramsUiAction, ProgramsUiState, ProgramsUiEffect>(
    initialState = ProgramsUiState(isLoading = true)
) {

    init {
        observeProgramsState()
        refreshPrograms()
    }

    override fun onAction(action: ProgramsUiAction) {
        when (action) {
            is ProgramsUiAction.OnSelectTab -> {
                updateState { copy(activeTab = action.tab) }
            }
            is ProgramsUiAction.OnSelectSplitFilter -> {
                updateState { copy(selectedSplitFilter = action.split) }
            }
            is ProgramsUiAction.OnSearchQueryChanged -> {
                updateState { copy(searchQuery = action.query) }
            }
            is ProgramsUiAction.OnOpenProgramDetail -> {
                updateState { copy(detailProgram = action.program) }
            }
            is ProgramsUiAction.OnActivateProgram -> activateProgram(action.programId)
            ProgramsUiAction.OnDeactivateProgram -> deactivateProgram()
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
            is ProgramsUiAction.OnUpvoteComment -> upvoteComment(action.commentId)
            is ProgramsUiAction.OnDeleteComment -> deleteComment(action.commentId)
            is ProgramsUiAction.OnOpenAthleteProfile -> openAthleteProfile(action.userId)
            ProgramsUiAction.OnCloseAthleteProfile -> {
                updateState { copy(athleteProfile = null) }
            }
            is ProgramsUiAction.OnOpenEditor -> {
                updateState {
                    copy(
                        isEditorOpen = true,
                        editingProgram = action.program,
                        detailProgram = null
                    )
                }
            }
            ProgramsUiAction.OnCloseEditor -> {
                updateState { copy(isEditorOpen = false, editingProgram = null) }
            }
            is ProgramsUiAction.OnSaveProgram -> saveProgram(action.request, action.editingId)
            is ProgramsUiAction.OnDeleteProgram -> deleteProgram(action.programId)
            is ProgramsUiAction.OnStartProgramDay -> startDayWorkout(action.program, action.day)
            ProgramsUiAction.OnRefresh -> refreshPrograms()
        }
    }

    private fun observeProgramsState() {
        combine(
            repository.getPrograms(),
            repository.getExercises(),
            repository.getCurrentUser()
        ) { list, exercises, user ->
            val updatedDetail = currentState.detailProgram?.let { dp ->
                list.find { it.id == dp.id } ?: dp
            }
            updateState {
                copy(
                    programs = list,
                    detailProgram = updatedDetail,
                    allExercises = exercises,
                    currentUserId = user?.id ?: "",
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshPrograms() {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.refreshPrograms()
            repository.getActiveProgram()
            updateState { copy(isLoading = false) }
        }
    }

    private fun activateProgram(programId: String) {
        launchGuarded {
            repository.activateProgram(programId)
            sendEffect(ProgramsUiEffect.ShowToast("Программа установлена как активная"))
        }
    }

    private fun deactivateProgram() {
        launchGuarded {
            repository.deactivateProgram()
            sendEffect(ProgramsUiEffect.ShowToast("Активная программа отключена"))
        }
    }

    private fun toggleLike(programId: String) {
        launchGuarded {
            repository.toggleLikeProgram(programId)
        }
    }

    private fun openComments(programId: String) {
        updateState { copy(activeCommentsProgramId = programId, comments = emptyList()) }
        launchGuarded {
            repository.refreshProgramComments(programId)
            repository.getProgramComments(programId).collect { commentList ->
                if (currentState.activeCommentsProgramId == programId) {
                    updateState { copy(comments = commentList) }
                }
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

    private fun upvoteComment(commentId: String) {
        val programId = currentState.activeCommentsProgramId ?: return
        launchGuarded {
            repository.toggleUpvoteProgramComment(programId, commentId)
        }
    }

    private fun deleteComment(commentId: String) {
        val programId = currentState.activeCommentsProgramId ?: return
        launchGuarded {
            repository.deleteProgramComment(programId, commentId)
        }
    }

    private fun openAthleteProfile(userId: String) {
        if (userId.isBlank()) return
        launchGuarded {
            val profile = repository.getAthleteProfile(userId)
            updateState { copy(athleteProfile = profile) }
        }
    }

    private fun saveProgram(request: SaveProgramRequestDto, editingId: String?) {
        launchGuarded {
            if (!editingId.isNullOrBlank()) {
                repository.updateProgram(editingId, request)
                sendEffect(ProgramsUiEffect.ShowToast("Программа обновлена"))
            } else {
                repository.createProgram(request)
                sendEffect(ProgramsUiEffect.ShowToast("Программа создана"))
            }
            updateState { copy(isEditorOpen = false, editingProgram = null) }
        }
    }

    private fun deleteProgram(programId: String) {
        launchGuarded {
            repository.deleteProgram(programId)
            updateState { copy(detailProgram = null) }
            sendEffect(ProgramsUiEffect.ShowToast("Программа удалена"))
        }
    }

    private fun startDayWorkout(program: TrainingProgram, day: ProgramDay) {
        updateState { copy(detailProgram = null) }
        repository.startWorkoutFromProgramDay(program, day)
        sendEffect(ProgramsUiEffect.NavigateToActiveWorkout)
    }
}
