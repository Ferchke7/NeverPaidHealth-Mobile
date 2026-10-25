package dev.neverpaid.health.features.programs

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.TrainingProgram

data class ProgramsUiState(
    val programs: List<TrainingProgram> = emptyList(),
    val selectedProgram: TrainingProgram? = null,
    val activeCommentsProgramId: String? = null,
    val comments: List<ProgramComment> = emptyList(),
    val newCommentText: String = "",
    val replyingToComment: ProgramComment? = null,
    val isLoading: Boolean = false
) : UiState

sealed interface ProgramsUiAction : UiAction {
    data class OnSelectProgram(val program: TrainingProgram) : ProgramsUiAction
    data class OnActivateProgram(val programId: String) : ProgramsUiAction
    data class OnToggleLike(val programId: String) : ProgramsUiAction
    data class OnOpenComments(val programId: String) : ProgramsUiAction
    data object OnCloseComments : ProgramsUiAction
    data class OnCommentTextChanged(val text: String) : ProgramsUiAction
    data class OnReplyClick(val comment: ProgramComment) : ProgramsUiAction
    data object OnCancelReply : ProgramsUiAction
    data object OnSendComment : ProgramsUiAction
    data class OnStartProgramDay(val day: ProgramDay) : ProgramsUiAction
}

sealed interface ProgramsUiEffect : UiEffect {
    data object NavigateToActiveWorkout : ProgramsUiEffect
    data class ShowToast(val message: String) : ProgramsUiEffect
}
