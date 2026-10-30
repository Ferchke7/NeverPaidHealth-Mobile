package dev.neverpaid.health.features.programs

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.AthleteProfile
import dev.neverpaid.health.domain.model.Exercise
import dev.neverpaid.health.domain.model.ProgramComment
import dev.neverpaid.health.domain.model.ProgramDay
import dev.neverpaid.health.domain.model.SaveProgramRequestDto
import dev.neverpaid.health.domain.model.TrainingProgram

data class ProgramsUiState(
    val programs: List<TrainingProgram> = emptyList(),
    val activeTab: String = "official", // "official" | "community" | "mine"
    val selectedSplitFilter: String = "all",
    val searchQuery: String = "",
    val detailProgram: TrainingProgram? = null,
    val activeCommentsProgramId: String? = null,
    val comments: List<ProgramComment> = emptyList(),
    val newCommentText: String = "",
    val replyingToComment: ProgramComment? = null,
    val athleteProfile: AthleteProfile? = null,
    val isEditorOpen: Boolean = false,
    val editingProgram: TrainingProgram? = null,
    val allExercises: List<Exercise> = emptyList(),
    val currentUserId: String = "",
    val isLoading: Boolean = false
) : UiState

sealed interface ProgramsUiAction : UiAction {
    data class OnSelectTab(val tab: String) : ProgramsUiAction
    data class OnSelectSplitFilter(val split: String) : ProgramsUiAction
    data class OnSearchQueryChanged(val query: String) : ProgramsUiAction
    data class OnOpenProgramDetail(val program: TrainingProgram?) : ProgramsUiAction
    data class OnActivateProgram(val programId: String) : ProgramsUiAction
    data object OnDeactivateProgram : ProgramsUiAction
    data class OnToggleLike(val programId: String) : ProgramsUiAction
    data class OnOpenComments(val programId: String) : ProgramsUiAction
    data object OnCloseComments : ProgramsUiAction
    data class OnCommentTextChanged(val text: String) : ProgramsUiAction
    data class OnReplyClick(val comment: ProgramComment) : ProgramsUiAction
    data object OnCancelReply : ProgramsUiAction
    data object OnSendComment : ProgramsUiAction
    data class OnUpvoteComment(val commentId: String) : ProgramsUiAction
    data class OnDeleteComment(val commentId: String) : ProgramsUiAction
    data class OnOpenAthleteProfile(val userId: String) : ProgramsUiAction
    data object OnCloseAthleteProfile : ProgramsUiAction
    data class OnOpenEditor(val program: TrainingProgram? = null) : ProgramsUiAction
    data object OnCloseEditor : ProgramsUiAction
    data class OnSaveProgram(val request: SaveProgramRequestDto, val editingId: String? = null) : ProgramsUiAction
    data class OnDeleteProgram(val programId: String) : ProgramsUiAction
    data class OnStartProgramDay(val program: TrainingProgram, val day: ProgramDay) : ProgramsUiAction
    data object OnRefresh : ProgramsUiAction
}

sealed interface ProgramsUiEffect : UiEffect {
    data object NavigateToActiveWorkout : ProgramsUiEffect
    data class ShowToast(val message: String) : ProgramsUiEffect
}
