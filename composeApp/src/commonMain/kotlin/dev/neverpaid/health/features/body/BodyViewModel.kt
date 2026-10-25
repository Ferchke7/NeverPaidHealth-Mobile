package dev.neverpaid.health.features.body

import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class BodyViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<BodyUiAction, BodyUiState, BodyUiEffect>(
    initialState = BodyUiState(isLoading = true)
) {

    init {
        loadMeasurements()
    }

    override fun onAction(action: BodyUiAction) {
        when (action) {
            BodyUiAction.OnOpenAddDialog -> updateState { copy(isAddDialogOpen = true) }
            BodyUiAction.OnDismissAddDialog -> updateState { copy(isAddDialogOpen = false) }
            is BodyUiAction.OnSaveMeasurement -> {
                launchGuarded {
                    repository.addBodyMeasurement(action.weight, action.height, action.bodyFat)
                    updateState { copy(isAddDialogOpen = false) }
                }
            }
        }
    }

    private fun loadMeasurements() {
        launchGuarded {
            repository.getBodyMeasurements().collectLatest { list ->
                val latest = list.lastOrNull()
                val latestWeight = latest?.weightKg ?: 80.0
                val height = latest?.heightCm ?: 180.0
                val weights = list.map { it.weightKg }
                val movingAvg = StrengthCalculator.calculate7DayMovingAverage(weights)
                val bmi = StrengthCalculator.calculateBMI(latestWeight, height) ?: 24.7
                val tdee = StrengthCalculator.calculateTDEE(latestWeight, height, age = 26)

                updateState {
                    copy(
                        measurements = list.reversed(),
                        latestWeightKg = latestWeight,
                        movingAverage7DayKg = movingAvg,
                        heightCm = height,
                        bmi = bmi,
                        tdeeCalories = tdee,
                        isLoading = false
                    )
                }
            }
        }
    }
}
