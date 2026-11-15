package dev.neverpaid.health.data.catalog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExerciseVisualResolverTest {

    @Test
    fun resolvesByExplicitSourceId() {
        val meta = ExerciseVisualResolver.resolve(
            nameOrId = "Custom Localized Name",
            sourceId = "Chin-Up"
        )
        assertEquals("Chin-Up", meta.resolvedId)
        assertEquals(
            listOf(
                "https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@main/exercises/Chin-Up/0.jpg",
                "https://cdn.jsdelivr.net/gh/yuhonas/free-exercise-db@main/exercises/Chin-Up/1.jpg"
            ),
            meta.frameUrls
        )
    }

    @Test
    fun resolvesAllStandardCatalogExercises() {
        val missing = StandardExerciseCatalog.exercises.filter { ex ->
            !ExerciseVisualResolver.resolve(
                nameOrId = ex.name,
                sourceId = ex.sourceId,
                exerciseId = ex.id
            ).hasAnimatedFrames
        }
        assertTrue(
            missing.isEmpty(),
            "Expected all standard catalog exercises to resolve visuals, missing: ${missing.map { it.name }}"
        )
    }

    @Test
    fun resolvesRussianAliasesAndNormalizedNames() {
        val bench = ExerciseVisualResolver.resolve("Жим лёжа")
        assertEquals("Barbell_Bench_Press_-_Medium_Grip", bench.resolvedId)
        assertTrue(bench.hasAnimatedFrames)

        val squat = ExerciseVisualResolver.resolve("Приседания со штангой")
        assertEquals("Barbell_Squat", squat.resolvedId)

        val parens = ExerciseVisualResolver.resolve("Bench Press (Barbell)")
        assertTrue(parens.hasAnimatedFrames)
    }

    @Test
    fun returnsEmptyFramesForUnknownCustomExercise() {
        val custom = ExerciseVisualResolver.resolve("Авторское супер упражнение X99")
        assertFalse(custom.hasImages)
        assertEquals(emptyList(), custom.frameUrls)
    }
}
