package dev.neverpaid.health.features.nutrition

import dev.neverpaid.health.data.repository.FakeNeverPaidRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class NutritionViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNeverPaidRepository
    private lateinit var viewModel: NutritionViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeNeverPaidRepository()
        viewModel = NutritionViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialNutritionLoaded() = runTest {
        val state = viewModel.state.value
        assertFalse(state.selectedDate.isEmpty())
        assertTrue(state.targetCalories > 0)
        assertTrue(state.targetProtein > 0)
    }

    @Test
    fun testAddWaterIncrementsVolume() = runTest {
        val initialWater = viewModel.state.value.waterMl

        viewModel.onAction(NutritionUiAction.OnAddWater(250))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialWater + 250, viewModel.state.value.waterMl)

        viewModel.onAction(NutritionUiAction.OnAddWater(500))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(initialWater + 750, viewModel.state.value.waterMl)
    }

    @Test
    fun testAddMealCalculatesMacros() = runTest {
        val initialMeals = viewModel.state.value.meals.size
        val initialCals = viewModel.state.value.totalCalories

        viewModel.onAction(NutritionUiAction.OnOpenAddMealSheet)
        assertTrue(viewModel.state.value.isAddMealSheetOpen)

        viewModel.onAction(NutritionUiAction.OnMealNameChanged("Grilled Chicken & Rice"))
        viewModel.onAction(NutritionUiAction.OnMealCaloriesChanged("550"))
        viewModel.onAction(NutritionUiAction.OnMealProteinChanged("45"))
        viewModel.onAction(NutritionUiAction.OnMealFatsChanged("12"))
        viewModel.onAction(NutritionUiAction.OnMealCarbsChanged("65"))
        viewModel.onAction(NutritionUiAction.OnSaveMeal)
        testDispatcher.scheduler.advanceUntilIdle()

        assertFalse(viewModel.state.value.isAddMealSheetOpen)
        val updatedState = viewModel.state.value
        assertEquals(initialMeals + 1, updatedState.meals.size)
        assertEquals(initialCals + 550, updatedState.totalCalories)
        assertTrue(updatedState.totalProtein >= 45)
    }

    @Test
    fun testAiFoodAnalysisFillsPreview() = runTest {
        viewModel.onAction(NutritionUiAction.OnAiDescriptionChanged("3 eggs and 2 toasts"))
        viewModel.onAction(NutritionUiAction.OnRunAiAnalysis)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isAiAnalyzing)
        assertNotNull(state.aiAnalysisPreview)
        assertTrue(state.aiAnalysisPreview!!.totalCalories > 0)
    }
    @Test
    fun switchingDatesStopsUpdatesFromThePreviousDay() = runTest {
        val firstDay = viewModel.state.value.selectedDate
        viewModel.onAction(NutritionUiAction.OnSelectDate("2026-01-01"))
        testDispatcher.scheduler.advanceUntilIdle()
        repository.addMealLog(dev.neverpaid.health.domain.model.MealLog(
            id = "old-day", date = firstDay, name = "Previous day", calories = 500
        ))
        repository.addWater(firstDay, 500)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("2026-01-01", viewModel.state.value.selectedDate)
        assertTrue(viewModel.state.value.meals.isEmpty())
        assertEquals(0, viewModel.state.value.waterMl)
    }

    @Test
    fun correctedZeroMacrosAreSavedInsteadOfAiEstimates() = runTest {
        viewModel.onAction(NutritionUiAction.OnAiDescriptionChanged("Eggs"))
        viewModel.onAction(NutritionUiAction.OnRunAiAnalysis)
        testDispatcher.scheduler.advanceUntilIdle()
        viewModel.onAction(NutritionUiAction.OnMealProteinChanged("0"))
        viewModel.onAction(NutritionUiAction.OnMealFatsChanged("0"))
        viewModel.onAction(NutritionUiAction.OnMealCarbsChanged("12,5"))
        viewModel.onAction(NutritionUiAction.OnConfirmAiPreview)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(0.0, viewModel.state.value.meals.last().proteinG)
        assertEquals(0.0, viewModel.state.value.meals.last().fatG)
        assertEquals(12.5, viewModel.state.value.meals.last().carbsG)
    }

    @Test
    fun repeatedSaveActionsCreateOneMeal() = runTest {
        viewModel.onAction(NutritionUiAction.OnMealNameChanged("Lunch"))
        viewModel.onAction(NutritionUiAction.OnMealCaloriesChanged("500"))
        viewModel.onAction(NutritionUiAction.OnSaveMeal)
        viewModel.onAction(NutritionUiAction.OnSaveMeal)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(1, viewModel.state.value.meals.count { it.name == "Lunch" })
        assertFalse(viewModel.state.value.isSavingMeal)
    }

}
