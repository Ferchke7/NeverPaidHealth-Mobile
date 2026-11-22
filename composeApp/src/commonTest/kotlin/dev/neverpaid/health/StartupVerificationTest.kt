package dev.neverpaid.health

import dev.neverpaid.health.core.network.DudaApiClient
import dev.neverpaid.health.data.catalog.ExerciseVisualResolver
import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.data.repository.NeverPaidRepositoryImpl
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

class StartupVerificationTest {

    @Test
    fun realRepositoryAndApiClient_initializeCleanly() = runTest {
        val repo = NeverPaidRepositoryImpl(apiClient = DudaApiClient())
        assertFalse(repo.isUserAuthenticated().first())
        val prefs = repo.getUserPreferences().first()
        assertFalse(prefs.darkTheme)
        assertEquals(repo.getPrebuiltRoutines().size, StandardExerciseCatalog.prebuiltRoutines.size)
    }

    @Test
    fun catalogsHaveUniqueIds_andVisualResolverHandlesEveryEntry() {
        val exerciseIds = StandardExerciseCatalog.standardExercises.map { it.id }
        assertEquals(exerciseIds.size, exerciseIds.toSet().size)

        val routineIds = StandardExerciseCatalog.prebuiltRoutines.map { it.id }
        assertEquals(routineIds.size, routineIds.toSet().size)

        StandardExerciseCatalog.standardExercises.forEach { ex ->
            assertNotNull(ExerciseVisualResolver.resolve(ex.name, exerciseId = ex.id))
        }
        StandardExerciseCatalog.prebuiltRoutines.forEach { r ->
            r.exercises.forEach { ex ->
                assertNotNull(ExerciseVisualResolver.resolve(ex.exerciseName, exerciseId = ex.exerciseId))
            }
        }
    }
}
