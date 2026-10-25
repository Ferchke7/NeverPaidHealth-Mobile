package dev.neverpaid.health.features.body

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.BodyMeasurement

data class BodyUiState(
    val measurements: List<BodyMeasurement> = emptyList(),
    val latestWeightKg: Double = 80.0,
    val movingAverage7DayKg: Double? = 80.1,
    val heightCm: Double = 180.0,
    val bmi: Double = 24.7,
    val tdeeCalories: Int = 2650,
    val isAddDialogOpen: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed interface BodyUiAction : UiAction {
    data object OnOpenAddDialog : BodyUiAction
    data object OnDismissAddDialog : BodyUiAction
    data class OnSaveMeasurement(val weight: Double, val height: Double, val bodyFat: Double?) : BodyUiAction
}

sealed interface BodyUiEffect : UiEffect
