package dev.neverpaid.health.features.nutrition

import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest

class NutritionViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<NutritionUiAction, NutritionUiState, NutritionUiEffect>(
    initialState = NutritionUiState()
) {

    init {
        val today = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        updateState { copy(selectedDate = today) }
        loadNutritionData(today)
    }

    override fun onAction(action: NutritionUiAction) {
        when (action) {
            is NutritionUiAction.OnSelectDate -> {
                updateState { copy(selectedDate = action.date) }
                loadNutritionData(action.date)
            }
            is NutritionUiAction.OnAddWater -> addWater(action.amountMl)
            NutritionUiAction.OnOpenAddMealSheet -> updateState { copy(isAddMealSheetOpen = true) }
            NutritionUiAction.OnCloseAddMealSheet -> updateState { copy(isAddMealSheetOpen = false) }
            is NutritionUiAction.OnMealTypeChanged -> updateState { copy(newMealType = action.type) }
            is NutritionUiAction.OnMealNameChanged -> updateState { copy(newMealName = action.name) }
            is NutritionUiAction.OnMealCaloriesChanged -> updateState { copy(newMealCalories = action.calories) }
            is NutritionUiAction.OnMealProteinChanged -> updateState { copy(newMealProtein = action.protein) }
            is NutritionUiAction.OnMealFatsChanged -> updateState { copy(newMealFats = action.fats) }
            is NutritionUiAction.OnMealCarbsChanged -> updateState { copy(newMealCarbs = action.carbs) }
            is NutritionUiAction.OnAiDescriptionChanged -> updateState { copy(aiDescriptionInput = action.desc) }
            NutritionUiAction.OnRunAiAnalysis -> runAiFoodAnalysis()
            NutritionUiAction.OnSaveMeal -> saveMeal()
            is NutritionUiAction.OnDeleteMeal -> deleteMeal(action.mealId)
        }
    }

    private fun loadNutritionData(date: String) {
        launchGuarded {
            repository.getMealLogs(date).collectLatest { logs ->
                val cals = logs.sumOf { it.calories }
                val prot = logs.sumOf { it.proteinG }
                val fats = logs.sumOf { it.fatsG }
                val carbs = logs.sumOf { it.carbsG }

                updateState {
                    copy(
                        meals = logs,
                        totalCalories = cals,
                        totalProtein = prot,
                        totalFats = fats,
                        totalCarbs = carbs,
                        isLoading = false
                    )
                }
            }
        }

        launchGuarded {
            repository.getWaterMl(date).collectLatest { ml ->
                updateState { copy(waterMl = ml) }
            }
        }
    }

    private fun addWater(amountMl: Int) {
        val date = currentState.selectedDate
        launchGuarded {
            repository.addWater(date, amountMl)
            val updated = (currentState.waterMl + amountMl).coerceAtLeast(0)
            updateState { copy(waterMl = updated) }
        }
    }

    private fun runAiFoodAnalysis() {
        val desc = currentState.aiDescriptionInput.trim()
        if (desc.isEmpty()) return

        updateState { copy(isAiAnalyzing = true) }

        launchGuarded(
            onError = {
                updateState { copy(isAiAnalyzing = false) }
            }
        ) {
            val result = repository.analyzeFood(desc)
            updateState {
                copy(
                    isAiAnalyzing = false,
                    newMealName = result.name,
                    newMealCalories = result.calories.toString(),
                    newMealProtein = result.proteinG.toString(),
                    newMealFats = result.fatsG.toString(),
                    newMealCarbs = result.carbsG.toString()
                )
            }
        }
    }

    private fun saveMeal() {
        val name = currentState.newMealName.trim()
        val cals = currentState.newMealCalories.toIntOrNull() ?: 0
        val prot = currentState.newMealProtein.toIntOrNull() ?: 0
        val fats = currentState.newMealFats.toIntOrNull() ?: 0
        val carbs = currentState.newMealCarbs.toIntOrNull() ?: 0

        if (name.isEmpty() || cals <= 0) return

        val newMeal = MealLog(
            id = PlatformBridge.generateUuid(),
            date = currentState.selectedDate,
            mealType = currentState.newMealType,
            name = name,
            calories = cals,
            proteinG = prot,
            fatsG = fats,
            carbsG = carbs
        )

        updateState {
            copy(
                isAddMealSheetOpen = false,
                newMealName = "",
                newMealCalories = "",
                newMealProtein = "",
                newMealFats = "",
                newMealCarbs = "",
                aiDescriptionInput = ""
            )
        }

        launchGuarded {
            repository.addMealLog(newMeal)
        }
    }

    private fun deleteMeal(mealId: String) {
        launchGuarded {
            repository.deleteMealLog(mealId)
        }
    }
}
