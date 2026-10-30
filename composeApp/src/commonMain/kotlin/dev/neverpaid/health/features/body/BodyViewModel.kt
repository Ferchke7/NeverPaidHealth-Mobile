package dev.neverpaid.health.features.body

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.math.StrengthCalculator
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.SaveBodyLogRequestDto
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlin.math.roundToInt

class BodyViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<BodyUiAction, BodyUiState, BodyUiEffect>(
    initialState = BodyUiState(isLoading = true)
) {

    init {
        observeBodyStreams()
        refreshRemoteBodyLogs()
    }

    override fun onAction(action: BodyUiAction) {
        when (action) {
            BodyUiAction.OnOpenAddDialog -> updateState { copy(isAddDialogOpen = true) }
            BodyUiAction.OnDismissAddDialog -> updateState { copy(isAddDialogOpen = false) }
            is BodyUiAction.OnSaveFullMeasurement -> {
                updateState { copy(isAddDialogOpen = false) }
                launchGuarded {
                    val today = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
                    repository.saveBodyMeasurementFull(
                        SaveBodyLogRequestDto(
                            logDate = today,
                            weightKg = action.weightKg,
                            heightCm = action.heightCm,
                            bodyFatPercentage = action.bodyFatPct,
                            waistCm = action.waistCm,
                            chestCm = action.chestCm,
                            armsCm = action.bicepsCm
                        )
                    )
                    sendEffect(BodyUiEffect.ShowToast("Замер сохранён"))
                }
            }
            is BodyUiAction.OnDeleteMeasurement -> {
                launchGuarded {
                    repository.deleteBodyMeasurement(action.loggedDate)
                    sendEffect(BodyUiEffect.ShowToast("Запись удалена"))
                }
            }
            BodyUiAction.OnToggleBmiCalculator -> {
                updateState { copy(showBmiCalculator = !showBmiCalculator) }
            }
            is BodyUiAction.OnUpdateBmiParams -> {
                recalculateBmrTdee(
                    gender = action.gender,
                    age = action.age,
                    heightCm = action.heightCm,
                    weightKg = action.weightKg,
                    activity = action.activityMultiplier
                )
            }
            BodyUiAction.OnOpenTargetWeightModal -> {
                updateState { copy(isTargetWeightModalOpen = true) }
            }
            BodyUiAction.OnCloseTargetWeightModal -> {
                updateState { copy(isTargetWeightModalOpen = false) }
            }
            is BodyUiAction.OnSaveTargetWeight -> {
                updateState { copy(isTargetWeightModalOpen = false, targetWeightKg = action.targetKg) }
                launchGuarded {
                    val prefs = repository.getUserPreferences().first()
                    repository.updateProfilePreferences(prefs.copy(targetWeightKg = action.targetKg ?: 75.0))
                    sendEffect(BodyUiEffect.ShowToast("Целевой вес обновлён"))
                }
            }
        }
    }

    private fun observeBodyStreams() {
        combine(
            repository.getBodyMeasurements(),
            repository.getUserPreferences()
        ) { list, prefs ->
            val sorted = list.sortedBy { it.loggedDate }
            val firstWeight = sorted.firstOrNull()?.weightKg ?: 80.0
            val latest = sorted.lastOrNull()
            val latestWeight = latest?.weightKg ?: currentState.latestWeightKg
            val height = latest?.heightCm?.takeIf { it > 0 } ?: currentState.heightCm
            val weights = sorted.map { it.weightKg }
            val movingAvg = StrengthCalculator.calculate7DayMovingAverage(weights)
            val bmi = StrengthCalculator.calculateBMI(latestWeight, height) ?: 24.5

            val bmr = computeMifflinBmr(currentState.gender, latestWeight, height, currentState.age)
            val tdee = (bmr * currentState.activityMultiplier).roundToInt()

            updateState {
                copy(
                    measurements = sorted.reversed(),
                    startWeightKg = firstWeight,
                    latestWeightKg = latestWeight,
                    targetWeightKg = prefs.targetWeightKg,
                    movingAverage7DayKg = movingAvg,
                    heightCm = height,
                    bmi = bmi,
                    bmrCalories = bmr,
                    tdeeCalories = tdee,
                    isLoading = false
                )
            }
        }.launchIn(viewModelScope)
    }

    private fun refreshRemoteBodyLogs() {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.refreshBodyMeasurements()
            repository.getBodyTrend(30)
            updateState { copy(isLoading = false) }
        }
    }

    private fun recalculateBmrTdee(
        gender: String,
        age: Int,
        heightCm: Double,
        weightKg: Double,
        activity: Double
    ) {
        val bmi = StrengthCalculator.calculateBMI(weightKg, heightCm) ?: 24.5
        val bmr = computeMifflinBmr(gender, weightKg, heightCm, age)
        val tdee = (bmr * activity).roundToInt()
        updateState {
            copy(
                gender = gender,
                age = age,
                heightCm = heightCm,
                latestWeightKg = weightKg,
                activityMultiplier = activity,
                bmi = bmi,
                bmrCalories = bmr,
                tdeeCalories = tdee
            )
        }
    }

    private fun computeMifflinBmr(gender: String, weightKg: Double, heightCm: Double, age: Int): Int {
        val base = (10.0 * weightKg) + (6.25 * heightCm) - (5.0 * age)
        return (if (gender == "female") base - 161.0 else base + 5.0).roundToInt().coerceAtLeast(1000)
    }
}
