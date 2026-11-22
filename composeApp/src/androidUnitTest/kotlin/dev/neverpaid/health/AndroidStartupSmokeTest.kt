package dev.neverpaid.health

import android.content.Context
import android.content.Intent
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import dev.neverpaid.health.core.i18n.RuDudaStrings
import dev.neverpaid.health.domain.repository.NeverPaidRepository
import dev.neverpaid.health.domain.repository.UserPreferencesData
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.koin.core.context.stopKoin
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = NeverPaidHealthApplication::class)
class AndroidStartupSmokeTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @After
    fun tearDown() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun coldStart_authToGuestDashboard_darkThemeAndNavigation_doesNotCrash() {
        composeRule.waitForIdle()

        // Fresh install lands on AuthScreen cleanly.
        composeRule.onNodeWithText("Продолжить без аккаунта").assertIsDisplayed()

        // Enter Guest mode -> renders DashboardScreen inside DudaRoyalTheme + Scaffold + NavBar.
        composeRule.onNodeWithText("Продолжить без аккаунта").performClick()
        composeRule.waitForIdle()

        // Switch dark theme on cleanly.
        val repository = GlobalContext.get().get<NeverPaidRepository>()
        runBlocking {
            repository.updateProfilePreferences(UserPreferencesData(languageCode = "ru", darkTheme = true))
        }
        composeRule.waitForIdle()

        // Navigate across bottom nav tabs to catch any composition/measurement crash.
        listOf(
            RuDudaStrings.navTodo,
            RuDudaStrings.navNutrition,
            RuDudaStrings.navCoach,
            RuDudaStrings.navProgress,
            RuDudaStrings.navWorkouts
        ).forEach { label ->
            composeRule.onAllNodesWithText(label).onFirst().performClick()
            composeRule.waitForIdle()
        }

        // Open menu sheet and navigate across secondary routes (Programs, Exercises, History, Body, Profile).
        listOf(
            RuDudaStrings.navPrograms,
            RuDudaStrings.navExercises,
            RuDudaStrings.navHistory,
            RuDudaStrings.navBody,
            RuDudaStrings.navProfile
        ).forEach { label ->
            composeRule.onNodeWithContentDescription(RuDudaStrings.menuButton).performClick()
            composeRule.waitForIdle()
            composeRule.onAllNodesWithText(label).onLast().performClick()
            composeRule.waitForIdle()
        }

        // Return to Workouts hub and start an empty workout to verify WorkoutScreen renders cleanly.
        composeRule.onAllNodesWithText(RuDudaStrings.navWorkouts).onFirst().performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText("+ Пустая тренировка").performClick()
        composeRule.waitForIdle()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35], application = NeverPaidHealthApplication::class)
class AndroidUpgradeStartupSmokeTest {

    @After
    fun tearDown() {
        try {
            stopKoin()
        } catch (_: Exception) {
        }
    }

    @Test
    fun upgradeFromV002_withPersistedPrefsAndActiveWorkout_doesNotCrash() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val prefs = context.getSharedPreferences("duda_storage", Context.MODE_PRIVATE)
        prefs.edit()
            .putString("np_access_token", "header.eyJzdWIiOiJ1c2VyLTEifQ.sig")
            .putString(
                "np_user_cache",
                """{"id":"user-1","email":"athlete@duda.uz","display_name":"Athlete","unit_preference":"kg"}"""
            )
            .putString(
                "np_user_preferences_v2",
                """{"language_code":"ru","use_kilograms":true,"default_rest_seconds":90}"""
            )
            .putString(
                "np_active_program_bundle_v2",
                """{"user_program":{"id":"up-1","program_id":"p-1","current_day_index":0,"is_active":true},"program":{"id":"p-1","name":"PPL","days":[{"day_number":1,"name":"Push","exercises":[{"exercise_id":"10000000-0000-0000-0000-000000000001","exercise_name":"Barbell Bench Press"}]},{"day_number":1,"name":"Push Copy","exercises":[]}]}}"""
            )
            .putString(
                "neverpaid_active_workout_v2",
                """{"id":"w-old-1","name":"Old Workout","status":"in_progress","started_at":"2026-11-15T10:00:00Z","exercises":[{"instance_id":"ex-1","exercise_id":"10000000-0000-0000-0000-000000000001","exercise_name":"Barbell Bench Press","sets":[{"id":"s-1","set_number":1,"weight_kg":80.0,"reps":8,"completed":true}]}]}"""
            )
            .commit()

        val controller = Robolectric.buildActivity(MainActivity::class.java)
            .create()
            .start()
            .resume()
            .visible()
        controller.get()
        controller.pause().stop().destroy()
    }

    @Test
    fun crashReportActivity_rendersTraceCleanly() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val intent = Intent(context, CrashReportActivity::class.java).apply {
            putExtra(CrashReportActivity.EXTRA_TRACE, "Sample crash trace for diagnostics")
        }
        val controller = Robolectric.buildActivity(CrashReportActivity::class.java, intent)
            .create()
            .start()
            .resume()
        controller.get()
        controller.pause().stop().destroy()
    }
}
