package dev.neverpaid.health.features.body

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.BodyMeasurement

data class BodyUiState(
    val measurements: List<BodyMeasurement> = emptyList(),
    val latestWeightKg: Double = 80.0,
    val startWeightKg: Double = 80.0,
    val targetWeightKg: Double? = null,
    val movingAverage7DayKg: Double? = null,
    val heightCm: Double = 178.0,
    val age: Int = 26,
    val gender: String = "male",
    val activityMultiplier: Double = 1.55,
    val bmi: Double = 25.2,
    val bmrCalories: Int = 1800,
    val tdeeCalories: Int = 2790,
    val showBmiCalculator: Boolean = false,
    val isAddDialogOpen: Boolean = false,
    val isTargetWeightModalOpen: Boolean = false,
    val isLoading: Boolean = false
) : UiState

sealed interface BodyUiAction : UiAction {
    data object OnOpenAddDialog : BodyUiAction
    data object OnDismissAddDialog : BodyUiAction
    data class OnSaveFullMeasurement(
        val weightKg: Double,
        val heightCm: Double,
        val bodyFatPct: Double?,
        val waistCm: Double?,
        val chestCm: Double?,
        val bicepsCm: Double?,
        val notes: String
    ) : BodyUiAction
    data class OnDeleteMeasurement(val loggedDate: String) : BodyUiAction
    data object OnToggleBmiCalculator : BodyUiAction
    data class OnUpdateBmiParams(
        val gender: String,
        val age: Int,
        val heightCm: Double,
        val weightKg: Double,
        val activityMultiplier: Double
    ) : BodyUiAction
    data object OnOpenTargetWeightModal : BodyUiAction
    data object OnCloseTargetWeightModal : BodyUiAction
    data class OnSaveTargetWeight(val targetKg: Double?) : BodyUiAction
}

sealed interface BodyUiEffect : UiEffect {
    data class ShowToast(val message: String) : BodyUiEffect
}
