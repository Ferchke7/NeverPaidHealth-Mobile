package dev.neverpaid.health.features.progress

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class ProgressViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<ProgressUiAction, ProgressUiState, ProgressUiEffect>(
    initialState = ProgressUiState(isLoading = true)
) {

    init {
        loadProgress()
    }

    override fun onAction(action: ProgressUiAction) {
        when (action) {
            ProgressUiAction.OnRefreshClick -> loadProgress()
        }
    }

    private fun loadProgress() {
        launchGuarded {
            repository.getProgressSummary().collectLatest { summary ->
                val volumeBars = listOf(
                    "W-4" to 8200.0,
                    "W-3" to 9400.0,
                    "W-2" to 11000.0,
                    "W-1" to 12500.0,
                    "Current" to 14200.0
                )

                val hypertrophy = mapOf(
                    "Chest" to 14,
                    "Back" to 16,
                    "Quads" to 12,
                    "Hamstrings" to 10,
                    "Shoulders" to 12,
                    "Arms" to 14
                )

                updateState {
                    copy(
                        totalWorkouts = summary.totalWorkouts,
                        totalVolumeKg = summary.totalVolumeKg,
                        totalReps = summary.totalReps,
                        currentStreakDays = summary.currentStreakDays,
                        personalRecords = summary.personalRecords,
                        weeklyVolumeBars = volumeBars,
                        muscleHypertrophySets = hypertrophy,
                        isLoading = false
                    )
                }
            }
        }
    }
}
