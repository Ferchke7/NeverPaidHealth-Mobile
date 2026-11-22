package dev.neverpaid.health

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
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
@Config(sdk = [34], application = NeverPaidHealthApplication::class)
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

        // Open menu sheet and navigate to Workouts/Programs/Exercises/History/Body/Profile.
        composeRule.onNodeWithContentDescription(RuDudaStrings.menuButton).performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithText(RuDudaStrings.navPrograms).performClick()
        composeRule.waitForIdle()
    }
}

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], application = NeverPaidHealthApplication::class)
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
            .putString(
                "np_user_cache",
                """{"id":"guest_athlete","email":"athlete@duda.uz","display_name":"Guest","unit_preference":"kg"}"""
            )
            .putString(
                "np_user_preferences_v2",
                """{"language_code":"ru","use_kilograms":true,"default_rest_seconds":90}"""
            )
            .putString(
                "neverpaid_active_workout_v2",
                """{"id":"w-old-1","name":"Old Workout","status":"in_progress","started_at":"2026-11-15T10:00:00Z","exercises":[]}"""
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
}
