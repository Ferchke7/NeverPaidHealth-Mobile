package dev.neverpaid.health.features.progress

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.PersonalRecord

data class ProgressUiState(
    val totalWorkouts: Int = 0,
    val totalVolumeKg: Double = 0.0,
    val totalReps: Int = 0,
    val currentStreakDays: Int = 0,
    val personalRecords: List<PersonalRecord> = emptyList(),
    val weeklyVolumeBars: List<Pair<String, Double>> = emptyList(),
    val muscleHypertrophySets: Map<String, Int> = emptyMap(),
    val isLoading: Boolean = false
) : UiState

sealed interface ProgressUiAction : UiAction {
    data object OnRefreshClick : ProgressUiAction
}

sealed interface ProgressUiEffect : UiEffect
