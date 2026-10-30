package dev.neverpaid.health.features.nutrition

import androidx.lifecycle.viewModelScope
import dev.neverpaid.health.core.mvi.BaseViewModel
import dev.neverpaid.health.core.platform.PlatformBridge
import dev.neverpaid.health.domain.model.MealLog
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDate
import kotlinx.datetime.plus
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NutritionViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<NutritionUiAction, NutritionUiState, NutritionUiEffect>(
    initialState = NutritionUiState(isLoading = true)
) {

    init {
        val today = PlatformBridge.formatDate(PlatformBridge.currentTimeMillis(), "yyyy-MM-dd")
        updateState { copy(selectedDate = today) }
        repository.getUserPreferences()
            .onEach { prefs ->
                updateState {
                    copy(
                        targetCalories = prefs.calorieTarget,
                        targetProtein = prefs.proteinTargetG,
                        targetCarbs = prefs.carbsTargetG,
                        targetFats = prefs.fatsTargetG
                    )
                }
            }
            .launchIn(viewModelScope)
        loadNutritionData(today)
    }

    override fun onAction(action: NutritionUiAction) {
        when (action) {
            is NutritionUiAction.OnSelectDate -> {
                updateState { copy(selectedDate = action.date) }
                loadNutritionData(action.date)
            }
            is NutritionUiAction.OnShiftDateDays -> {
                // Shift current date by deltaDays
                val todayMs = PlatformBridge.currentTimeMillis()
                val currentStr = currentState.selectedDate
                val targetDateStr = shiftDateString(currentStr, action.deltaDays, todayMs)
                updateState { copy(selectedDate = targetDateStr) }
                loadNutritionData(targetDateStr)
            }
            is NutritionUiAction.OnAddWater -> addWater(action.amountMl)
            is NutritionUiAction.OnSelectAnalyzerMealType -> {
                updateState { copy(selectedMealType = action.type) }
            }
            is NutritionUiAction.OnAiDescriptionChanged -> {
                updateState { copy(aiDescriptionInput = action.desc) }
            }
            is NutritionUiAction.OnPhotoPicked -> {
                updateState { copy(selectedPhotoBase64 = action.base64Jpeg) }
            }
            NutritionUiAction.OnClearPhoto -> {
                updateState { copy(selectedPhotoBase64 = null) }
            }
            NutritionUiAction.OnRunAiAnalysis -> runAiFoodAnalysis()
            NutritionUiAction.OnConfirmAiPreview -> confirmAiPreviewMeal()
            NutritionUiAction.OnDismissAiPreview -> {
                updateState { copy(aiAnalysisPreview = null) }
            }
            NutritionUiAction.OnOpenAddMealSheet -> {
                updateState { copy(isAddMealSheetOpen = true, newMealType = currentState.selectedMealType) }
            }
            NutritionUiAction.OnCloseAddMealSheet -> {
                updateState { copy(isAddMealSheetOpen = false) }
            }
            is NutritionUiAction.OnMealTypeChanged -> updateState { copy(newMealType = action.type) }
            is NutritionUiAction.OnMealNameChanged -> updateState { copy(newMealName = action.name) }
            is NutritionUiAction.OnMealCaloriesChanged -> updateState { copy(newMealCalories = action.calories) }
            is NutritionUiAction.OnMealProteinChanged -> updateState { copy(newMealProtein = action.protein) }
            is NutritionUiAction.OnMealFatsChanged -> updateState { copy(newMealFats = action.fats) }
            is NutritionUiAction.OnMealCarbsChanged -> updateState { copy(newMealCarbs = action.carbs) }
            NutritionUiAction.OnSaveMeal -> saveMeal()
            is NutritionUiAction.OnDeleteMeal -> deleteMeal(action.mealId)
        }
    }

    private fun loadNutritionData(date: String) {
        launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.refreshDailyNutrition(date)
        }

        launchGuarded {
            repository.getMealLogs(date).collectLatest { logs ->
                val cals = logs.sumOf { it.calories }
                val prot = logs.sumOf { it.proteinG }.toInt()
                val fats = logs.sumOf { it.fatsG }
                val carbs = logs.sumOf { it.carbsG }.toInt()

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
        }
    }

    private fun runAiFoodAnalysis() {
        val desc = currentState.aiDescriptionInput.trim()
        val photo = currentState.selectedPhotoBase64
        if (desc.isEmpty() && photo.isNullOrBlank()) {
            sendEffect(NutritionUiEffect.ShowToast("Добавьте фото блюда или опишите его текстом"))
            return
        }

        updateState { copy(isAiAnalyzing = true, aiAnalysisPreview = null) }

        launchGuarded(
            onError = { err ->
                updateState { copy(isAiAnalyzing = false) }
                sendEffect(NutritionUiEffect.ShowToast(err.message ?: "Ошибка ИИ-анализа"))
            }
        ) {
            val result = if (!photo.isNullOrBlank()) {
                repository.analyzeFoodPhotoFull(
                    imageBase64 = photo,
                    notes = desc
                )
            } else {
                repository.estimateFoodTextFull(desc)
            }
            updateState {
                copy(
                    isAiAnalyzing = false,
                    aiAnalysisPreview = result,
                    newMealName = result.mealName,
                    newMealCalories = result.totalCalories.toString(),
                    newMealProtein = result.proteinG.toInt().toString(),
                    newMealFats = result.fatG.toInt().toString(),
                    newMealCarbs = result.carbsG.toInt().toString()
                )
            }
        }
    }

    private fun confirmAiPreviewMeal() {
        val preview = currentState.aiAnalysisPreview ?: return
        val newMeal = MealLog(
            id = PlatformBridge.generateUuid(),
            date = currentState.selectedDate,
            mealType = currentState.selectedMealType,
            name = preview.title.ifBlank { preview.name },
            calories = preview.calories,
            proteinG = preview.proteinG,
            fatG = preview.fatsG,
            carbsG = preview.carbsG,
            items = preview.foodItems
        )

        updateState {
            copy(
                aiAnalysisPreview = null,
                aiDescriptionInput = "",
                selectedPhotoBase64 = null
            )
        }

        launchGuarded {
            repository.addMealLog(newMeal)
            sendEffect(NutritionUiEffect.ShowToast("Приём пищи добавлен в дневник"))
        }
    }

    private fun saveMeal() {
        val name = currentState.newMealName.trim()
        val cals = currentState.newMealCalories.toIntOrNull() ?: 0
        val prot = currentState.newMealProtein.toDoubleOrNull() ?: 0.0
        val fats = currentState.newMealFats.toDoubleOrNull() ?: 0.0
        val carbs = currentState.newMealCarbs.toDoubleOrNull() ?: 0.0

        if (name.isEmpty() || cals <= 0) return

        val newMeal = MealLog(
            id = PlatformBridge.generateUuid(),
            date = currentState.selectedDate,
            mealType = currentState.newMealType,
            name = name,
            calories = cals,
            proteinG = prot,
            fatG = fats,
            carbsG = carbs
        )

        updateState {
            copy(
                isAddMealSheetOpen = false,
                newMealName = "",
                newMealCalories = "",
                newMealProtein = "",
                newMealFats = "",
                newMealCarbs = ""
            )
        }

        launchGuarded {
            repository.addMealLog(newMeal)
            sendEffect(NutritionUiEffect.ShowToast("Запись сохранена"))
        }
    }

    private fun deleteMeal(mealId: String) {
        launchGuarded {
            repository.deleteMealLog(mealId, currentState.selectedDate)
        }
    }

    private fun shiftDateString(currentDate: String, deltaDays: Int, fallbackMs: Long): String {
        return runCatching {
            val parts = currentDate.split("-").map { it.toInt() }
            val date = LocalDate(parts[0], parts[1], parts[2])
            val shifted = date.plus(deltaDays, DateTimeUnit.DAY)
            shifted.toString()
        }.getOrElse {
            PlatformBridge.formatDate(fallbackMs + deltaDays * 86400000L, "yyyy-MM-dd")
        }
    }
}
