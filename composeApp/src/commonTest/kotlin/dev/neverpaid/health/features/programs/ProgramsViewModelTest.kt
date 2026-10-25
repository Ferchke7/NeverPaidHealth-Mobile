package dev.neverpaid.health.features.programs

import dev.neverpaid.health.data.repository.NeverPaidRepositoryImpl
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ProgramsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: NeverPaidRepositoryImpl
    private lateinit var viewModel: ProgramsViewModel

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        repository = NeverPaidRepositoryImpl()
        viewModel = ProgramsViewModel(repository)
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testInitialProgramsLoaded() = runTest {
        val state = viewModel.state.value
        assertFalse(state.programs.isEmpty(), "Programs list should be loaded")
        assertNotNull(state.selectedProgram)
    }

    @Test
    fun testActivateProgram() = runTest {
        val firstProg = viewModel.state.value.programs.first()
        viewModel.onAction(ProgramsUiAction.OnActivateProgram(firstProg.id))
        testDispatcher.scheduler.advanceUntilIdle()

        val updatedFirst = viewModel.state.value.programs.first { it.id == firstProg.id }
        assertTrue(updatedFirst.isActive)
    }

    @Test
    fun testCommentsSheetAndReplies() = runTest {
        val prog = viewModel.state.value.programs.first()

        viewModel.onAction(ProgramsUiAction.OnOpenComments(prog.id))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(prog.id, viewModel.state.value.activeCommentsProgramId)
        val initialComments = viewModel.state.value.comments
        assertTrue(initialComments.isNotEmpty())

        val parentComment = initialComments.first()
        viewModel.onAction(ProgramsUiAction.OnReplyClick(parentComment))
        assertEquals(parentComment.id, viewModel.state.value.replyingToComment?.id)

        viewModel.onAction(ProgramsUiAction.OnCommentTextChanged("Great program!"))
        assertEquals("Great program!", viewModel.state.value.newCommentText)

        viewModel.onAction(ProgramsUiAction.OnSendComment)
        testDispatcher.scheduler.advanceUntilIdle()

        assertNull(viewModel.state.value.replyingToComment)
        assertEquals("", viewModel.state.value.newCommentText)

        viewModel.onAction(ProgramsUiAction.OnCloseComments)
        assertNull(viewModel.state.value.activeCommentsProgramId)
    }
}
