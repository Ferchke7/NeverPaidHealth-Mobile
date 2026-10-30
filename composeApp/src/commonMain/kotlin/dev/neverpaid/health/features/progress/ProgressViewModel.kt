package dev.neverpaid.health.features.progress

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine

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
            is ProgressUiAction.OnSelectTab -> updateState { copy(selectedTab = action.tabIndex) }
            ProgressUiAction.OnRefreshClick -> loadProgress()
        }
    }

    private fun loadProgress() {
        launchGuarded {
            combine(
                repository.getProgressSummary(),
                repository.getWorkoutHistory()
            ) { summary, history ->
                Pair(summary, history)
            }.collect { (summary, history) ->
                val (volumeBars, hypertrophy) = computeRealMetrics(history)
                updateState {
                    copy(
                        totalWorkouts = summary.totalWorkouts,
                        totalVolumeKg = summary.totalVolumeKg,
                        totalReps = summary.totalReps,
                        currentStreakDays = summary.currentStreakDays,
                        personalRecords = summary.personalRecords,
                        weeklyVolumeBars = volumeBars,
                        muscleHypertrophySets = hypertrophy,
                        workoutHistory = history,
                        isLoading = false
                    )
                }
            }
        }
    }

    private fun computeRealMetrics(history: List<Workout>): Pair<List<Pair<String, Double>>, Map<String, Int>> {
        val now = PlatformBridge.currentTimeMillis()
        val weekMs = 7 * 86_400_000L

        // Real 5-week volume progression
        val volumeBars = mutableListOf<Pair<String, Double>>()
        for (w in 4 downTo 0) {
            val weekStart = now - ((w + 1) * weekMs)
            val weekEnd = now - (w * weekMs)
            val label = if (w == 0) "Сейчас" else "Н-$w"
            val weekVolume = history
                .filter { it.startTimeEpochMs in weekStart..weekEnd }
                .sumOf { it.totalVolumeKg }
            volumeBars.add(label to weekVolume)
        }

        // Real 7-day muscle hypertrophy sets
        val sevenDaysAgo = now - weekMs
        val recentWorkouts = history.filter { it.startTimeEpochMs >= sevenDaysAgo }
        val muscleMap = mutableMapOf(
            "Грудь" to 0,
            "Спина" to 0,
            "Квадрицепсы" to 0,
            "Бицепс бедра" to 0,
            "Плечи" to 0,
            "Руки" to 0
        )

        for (workout in recentWorkouts) {
            for (ex in workout.exercises) {
                val completedSets = ex.sets.count { it.isCompleted && it.type != "warmup" }
                val muscleKey = when (ex.primaryMuscleGroup.lowercase()) {
                    "chest", "грудь" -> "Грудь"
                    "back", "спина" -> "Спина"
                    "quads", "квадрицепсы", "ноги" -> "Квадрицепсы"
                    "hamstrings", "бицепс бедра" -> "Бицепс бедра"
                    "shoulders", "плечи" -> "Плечи"
                    "biceps", "triceps", "arms", "руки" -> "Руки"
                    else -> null
                }
                if (muscleKey != null) {
                    muscleMap[muscleKey] = (muscleMap[muscleKey] ?: 0) + completedSets
                }
            }
        }

        return Pair(volumeBars, muscleMap)
    }
}
