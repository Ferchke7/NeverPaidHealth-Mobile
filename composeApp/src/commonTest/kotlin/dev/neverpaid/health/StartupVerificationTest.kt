package dev.neverpaid.health

import dev.neverpaid.health.core.i18n.EnDudaStrings
import dev.neverpaid.health.core.i18n.RuDudaStrings
import dev.neverpaid.health.core.i18n.UzDudaStrings
import dev.neverpaid.health.core.i18n.getDudaStrings
import dev.neverpaid.health.core.network.DudaApiClient
import dev.neverpaid.health.data.catalog.ExerciseVisualResolver
import dev.neverpaid.health.data.catalog.StandardExerciseCatalog
import dev.neverpaid.health.data.repository.NeverPaidRepositoryImpl
import dev.neverpaid.health.domain.model.TrainingProgram
import dev.neverpaid.health.domain.model.Workout
import dev.neverpaid.health.domain.model.WorkoutSet
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
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
    fun localizedStrings_instantiateWithoutClassFormatLimit() {
        assertEquals("ru", getDudaStrings("ru").languageCode)
        assertEquals("uz", getDudaStrings("uz").languageCode)
        assertEquals("en", getDudaStrings("en").languageCode)
        assertEquals(RuDudaStrings.appName, UzDudaStrings.appName)
        assertEquals(RuDudaStrings.appName, EnDudaStrings.appName)
        assertFalse(RuDudaStrings.profileThemeLabel.isBlank())
        assertFalse(UzDudaStrings.profileThemeLabel.isBlank())
        assertFalse(EnDudaStrings.profileThemeLabel.isBlank())
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

    @Test
    fun syncRoutineFromWorkout_createsSinglePersonalCopyAndUpdatesInPlace() = runTest {
        val repo = NeverPaidRepositoryImpl(apiClient = null)
        val routine = StandardExerciseCatalog.prebuiltRoutines.first()
        val beforeCount = repo.getUserRoutinesFlow().first().size

        val started = repo.startWorkout(routine.name, routine)
        val extraExercise = StandardExerciseCatalog.standardExercises.last()

        val withAdded = started.copy(
            exercises = started.exercises + dev.neverpaid.health.domain.model.WorkoutExercise(
                instanceId = "extra-1",
                exerciseId = extraExercise.id,
                exerciseName = extraExercise.name,
                sets = listOf(WorkoutSet(id = "set-x1", setNumber = 1, weightKg = 20.0, reps = 12))
            )
        )
        repo.updateActiveWorkout(withAdded)

        val reboundId = repo.getActiveWorkout().first()?.routineId
        assertNotNull(reboundId)
        val afterFirstEdit = repo.getUserRoutinesFlow().first()
        assertEquals(beforeCount + 1, afterFirstEdit.size)

        // Second structural tweak must update that SAME personal copy instead of creating another one
        val withSecondChange = withAdded.copy(
            exercises = withAdded.exercises.dropLast(1)
        )
        repo.updateActiveWorkout(withSecondChange)
        val afterSecondEdit = repo.getUserRoutinesFlow().first()
        assertEquals(beforeCount + 1, afterSecondEdit.size)
        assertEquals(reboundId, repo.getActiveWorkout().first()?.routineId)

        // Logout clears user-scoped caches cleanly
        repo.logout()
        assertEquals(0, repo.getUserRoutinesFlow().first().size)
    }

    @Test
    fun backendJsonWithNullableFields_decodesCleanlyWithCoerceInputValues() {
        val codec = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
            explicitNulls = false
            coerceInputValues = true
        }
        val activeWorkoutJson = """
            {
              "id": "w-1",
              "user_id": "u-1",
              "name": "Push 1",
              "routine_id": null,
              "status": "in_progress",
              "started_at": "2026-11-24T08:00:00Z",
              "finished_at": null,
              "total_volume_kg": null,
              "completed_sets_count": null,
              "duration_seconds": null,
              "exercises": []
            }
        """.trimIndent()
        val parsedWorkout = codec.decodeFromString<Workout>(activeWorkoutJson)
        assertEquals("w-1", parsedWorkout.id)
        assertEquals(0.0, parsedWorkout.totalVolumeKg)
        assertEquals(0L, parsedWorkout.durationSeconds)

        val programJson = """
            {
              "id": "p-1",
              "user_id": "u-1",
              "name": "Jeff Nippard PPL",
              "description": null,
              "split_type": "ppl",
              "days_per_week": 6,
              "level": "advanced",
              "is_public": false,
              "days": [],
              "updated_at": "2026-11-24T08:00:00Z"
            }
        """.trimIndent()
        val parsedProg = codec.decodeFromString<TrainingProgram>(programJson)
        assertEquals("u-1", parsedProg.authorUserId)
        assertEquals("2026-11-24T08:00:00Z", parsedProg.updatedAt)
    }
}
