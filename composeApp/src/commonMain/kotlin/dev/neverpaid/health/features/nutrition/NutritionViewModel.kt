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
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class NutritionViewModel(
    private val repository: NeverPaidRepository
) : BaseViewModel<NutritionUiAction, NutritionUiState, NutritionUiEffect>(
    initialState = NutritionUiState(isLoading = true)
) {

    private var mealsJob: kotlinx.coroutines.Job? = null
    private var waterJob: kotlinx.coroutines.Job? = null
    private var refreshJob: kotlinx.coroutines.Job? = null

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
        mealsJob?.cancel()
        waterJob?.cancel()
        refreshJob?.cancel()
        updateState { copy(isLoading = true, meals = emptyList(), totalCalories = 0, totalProtein = 0, totalFats = 0, totalCarbs = 0, waterMl = 0) }
        refreshJob = launchGuarded(
            onError = { updateState { copy(isLoading = false) } }
        ) {
            repository.refreshDailyNutrition(date)
        }

        mealsJob = launchGuarded {
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

        waterJob = launchGuarded {
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
                    newMealProtein = result.proteinG.toString(),
                    newMealFats = result.fatG.toString(),
                    newMealCarbs = result.carbsG.toString()
                )
            }
        }
    }

    private fun readNutritionNumber(input: String, fallback: Double = 0.0): Double {
        val value = if (input.isBlank()) fallback else input.trim().replace(',', '.').toDoubleOrNull() ?: 0.0
        return if (value.isFinite()) value.coerceAtLeast(0.0) else 0.0
    }

    private fun confirmAiPreviewMeal() {
        if (currentState.isSavingMeal) return
        val preview = currentState.aiAnalysisPreview ?: return
        val newMeal = MealLog(
            id = PlatformBridge.generateUuid(),
            date = currentState.selectedDate,
            mealType = currentState.selectedMealType,
            name = currentState.newMealName.trim().ifBlank { preview.mealName },
            calories = readNutritionNumber(currentState.newMealCalories, preview.calories.toDouble()).roundToInt(),
            proteinG = readNutritionNumber(currentState.newMealProtein, preview.proteinG),
            fatG = readNutritionNumber(currentState.newMealFats, preview.fatG),
            carbsG = readNutritionNumber(currentState.newMealCarbs, preview.carbsG),
            items = preview.foodItems
        )

        persistMeal(newMeal, fromAi = true)
    }

    private fun saveMeal() {
        if (currentState.isSavingMeal) return
        val name = currentState.newMealName.trim()
        val cals = readNutritionNumber(currentState.newMealCalories).roundToInt()
        val prot = readNutritionNumber(currentState.newMealProtein)
        val fats = readNutritionNumber(currentState.newMealFats)
        val carbs = readNutritionNumber(currentState.newMealCarbs)

        if (name.isEmpty() || cals < 0 || listOf(prot, fats, carbs).any { !it.isFinite() || it < 0 }) return

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

        persistMeal(newMeal, fromAi = false)
    }

    private fun persistMeal(meal: MealLog, fromAi: Boolean) {
        updateState { copy(isSavingMeal = true) }
        launchGuarded(onError = { error ->
            updateState { copy(isSavingMeal = false) }
            sendError(error)
        }) {
            repository.addMealLog(meal)
            updateState {
                copy(
                    isSavingMeal = false,
                    isAddMealSheetOpen = false,
                    aiAnalysisPreview = if (fromAi) null else aiAnalysisPreview,
                    aiDescriptionInput = if (fromAi) "" else aiDescriptionInput,
                    selectedPhotoBase64 = if (fromAi) null else selectedPhotoBase64,
                    newMealName = "", newMealCalories = "", newMealProtein = "",
                    newMealFats = "", newMealCarbs = ""
                )
            }
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
