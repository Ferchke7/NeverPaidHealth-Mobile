package dev.neverpaid.health.features.nutrition

import dev.neverpaid.health.core.mvi.UiAction
import dev.neverpaid.health.core.mvi.UiEffect
import dev.neverpaid.health.core.mvi.UiState
import dev.neverpaid.health.domain.model.MealLog

data class NutritionUiState(
    val selectedDate: String = "",
    val meals: List<MealLog> = emptyList(),
    val totalCalories: Int = 0,
    val targetCalories: Int = 2450,
    val totalProtein: Int = 0,
    val targetProtein: Int = 180,
    val totalFats: Int = 0,
    val targetFats: Int = 70,
    val totalCarbs: Int = 0,
    val targetCarbs: Int = 280,
    val waterMl: Int = 0,
    val targetWaterMl: Int = 3000,
    val streakDays: Int = 4,

    // Add Meal Sheet
    val isAddMealSheetOpen: Boolean = false,
    val newMealType: String = "lunch",
    val newMealName: String = "",
    val newMealCalories: String = "",
    val newMealProtein: String = "",
    val newMealFats: String = "",
    val newMealCarbs: String = "",

    // AI Analysis
    val aiDescriptionInput: String = "",
    val isAiAnalyzing: Boolean = false,

    val isLoading: Boolean = false
) : UiState

sealed interface NutritionUiAction : UiAction {
    data class OnSelectDate(val date: String) : NutritionUiAction
    data class OnAddWater(val amountMl: Int) : NutritionUiAction
    data object OnOpenAddMealSheet : NutritionUiAction
    data object OnCloseAddMealSheet : NutritionUiAction
    data class OnMealTypeChanged(val type: String) : NutritionUiAction
    data class OnMealNameChanged(val name: String) : NutritionUiAction
    data class OnMealCaloriesChanged(val calories: String) : NutritionUiAction
    data class OnMealProteinChanged(val protein: String) : NutritionUiAction
    data class OnMealFatsChanged(val fats: String) : NutritionUiAction
    data class OnMealCarbsChanged(val carbs: String) : NutritionUiAction
    data class OnAiDescriptionChanged(val desc: String) : NutritionUiAction
    data object OnRunAiAnalysis : NutritionUiAction
    data object OnSaveMeal : NutritionUiAction
    data class OnDeleteMeal(val mealId: String) : NutritionUiAction
}

sealed interface NutritionUiEffect : UiEffect {
    data class ShowToast(val message: String) : NutritionUiEffect
}
