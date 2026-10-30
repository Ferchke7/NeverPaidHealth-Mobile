package dev.neverpaid.health.features.progress

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
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProgressViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeNeverPaidRepository
    private lateinit var viewModel: ProgressViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = FakeNeverPaidRepository()
        viewModel = ProgressViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialProgressState() = runTest {
        val state = viewModel.state.value
        assertFalse(state.isLoading)
        assertTrue(state.totalWorkouts > 0)
        assertTrue(state.totalVolumeKg > 0)
        assertTrue(state.weeklyVolumeBars.isNotEmpty(), "Weekly volume bars must be calculated")
        assertEquals(5, state.weeklyVolumeBars.size)
    }

    @Test
    fun testTabSwitching() = runTest {
        assertEquals(0, viewModel.state.value.selectedTab)

        viewModel.onAction(ProgressUiAction.OnSelectTab(1))
        assertEquals(1, viewModel.state.value.selectedTab)

        viewModel.onAction(ProgressUiAction.OnSelectTab(2))
        assertEquals(2, viewModel.state.value.selectedTab)
    }
}
