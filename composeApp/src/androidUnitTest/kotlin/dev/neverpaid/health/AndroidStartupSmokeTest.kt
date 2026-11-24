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

    @Test
    fun timerNotificationScheduleBroadcastAndCancel_postsAndClearsNotificationCleanly() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(context).grantPermissions(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        dev.neverpaid.health.core.platform.PlatformBridge.updateLiveWorkoutWidget(null)
        shadowNm.cancelAll()

        val triggerAt = System.currentTimeMillis() + 60_000L
        dev.neverpaid.health.core.platform.PlatformBridge.scheduleTimerNotification(
            timerId = dev.neverpaid.health.core.platform.TimerNotificationIds.REST,
            triggerAtEpochMs = triggerAt,
            title = "Отдых завершён",
            body = "Время на следующий подход!",
            showOngoingCountdown = true,
            ongoingTitle = "Отдых",
            ongoingBody = "Push Day"
        )
        org.junit.Assert.assertTrue(
            "Expected ongoing chronometer notification after scheduling",
            shadowNm.allNotifications.isNotEmpty()
        )

        val receiver = dev.neverpaid.health.core.platform.TimerAlarmReceiver()
        val alarmIntent = Intent(context, dev.neverpaid.health.core.platform.TimerAlarmReceiver::class.java).apply {
            action = "dev.neverpaid.health.ACTION_TIMER_ALARM_${dev.neverpaid.health.core.platform.TimerNotificationIds.REST}"
            putExtra(dev.neverpaid.health.core.platform.TimerAlarmReceiver.EXTRA_TIMER_ID, dev.neverpaid.health.core.platform.TimerNotificationIds.REST)
            putExtra(dev.neverpaid.health.core.platform.TimerAlarmReceiver.EXTRA_TRIGGER_AT, triggerAt)
            putExtra(dev.neverpaid.health.core.platform.TimerAlarmReceiver.EXTRA_TITLE, "Отдых завершён")
            putExtra(dev.neverpaid.health.core.platform.TimerAlarmReceiver.EXTRA_BODY, "Время на следующий подход!")
        }
        receiver.onReceive(context, alarmIntent)

        org.junit.Assert.assertTrue(
            "Expected alert notification after alarm broadcast fires",
            shadowNm.allNotifications.any { notif ->
                val title = notif.extras.getString(android.app.Notification.EXTRA_TITLE)
                title == "Отдых завершён"
            }
        )

        dev.neverpaid.health.core.platform.PlatformBridge.cancelTimerNotification(
            dev.neverpaid.health.core.platform.TimerNotificationIds.REST
        )
        org.junit.Assert.assertTrue(
            "Expected notifications cleared after cancel",
            shadowNm.allNotifications.isEmpty()
        )
    }

    @Test
    fun liveWorkoutNotificationAndHomeWidgetActions_updateRestAndClearCleanly() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        org.robolectric.Shadows.shadowOf(context).grantPermissions(
            android.Manifest.permission.POST_NOTIFICATIONS
        )
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
        val shadowNm = org.robolectric.Shadows.shadowOf(nm)
        shadowNm.cancelAll()
        dev.neverpaid.health.core.storage.DudaLocalStorage.setRestTimerState(null)

        dev.neverpaid.health.core.platform.PlatformBridge.updateLiveWorkoutWidget(
            dev.neverpaid.health.core.platform.LiveWorkoutWidgetState(
                workoutId = "w-live-1",
                workoutTitle = "Push Hypertrophy",
                startTimeEpochMs = System.currentTimeMillis() - 180_000L,
                completedSets = 4,
                totalSets = 12,
                totalVolumeKg = 1480.0,
                currentExerciseName = "Bench Press",
                restEndEpochMs = 0L,
                restTotalSeconds = 0,
                defaultRestSeconds = 90,
                useKilograms = true,
                languageCode = "ru"
            )
        )

        org.junit.Assert.assertTrue(
            "Expected live workout notification after starting gym session",
            shadowNm.allNotifications.any { notif ->
                notif.extras.getString(android.app.Notification.EXTRA_TITLE) == "Push Hypertrophy"
            }
        )

        val actionReceiver = dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver()
        actionReceiver.onReceive(
            context,
            Intent(context, dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver::class.java).apply {
                action = dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver.ACTION_REST_START
            }
        )
        val startedRest = dev.neverpaid.health.core.storage.DudaLocalStorage.getRestTimerState()
        org.junit.Assert.assertNotNull("Expected ACTION_REST_START to persist rest timer", startedRest)
        org.junit.Assert.assertTrue(
            "Expected live notification title to reflect active rest",
            shadowNm.allNotifications.any { notif ->
                notif.extras.getString(android.app.Notification.EXTRA_TITLE)?.contains("Push Hypertrophy") == true &&
                    notif.extras.getString(android.app.Notification.EXTRA_TITLE)?.contains("ОТДЫХ") == true
            }
        )

        actionReceiver.onReceive(
            context,
            Intent(context, dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver::class.java).apply {
                action = dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver.ACTION_REST_ADD_15
            }
        )
        val extendedRest = dev.neverpaid.health.core.storage.DudaLocalStorage.getRestTimerState()
        org.junit.Assert.assertNotNull(extendedRest)
        org.junit.Assert.assertTrue(
            "Expected +15s action to extend endEpochMs",
            extendedRest!!.endEpochMs >= startedRest!!.endEpochMs + 14_000L
        )

        actionReceiver.onReceive(
            context,
            Intent(context, dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver::class.java).apply {
                action = dev.neverpaid.health.core.platform.WorkoutWidgetActionReceiver.ACTION_REST_SKIP
            }
        )
        org.junit.Assert.assertNull(
            "Expected ACTION_REST_SKIP to clear persisted rest timer",
            dev.neverpaid.health.core.storage.DudaLocalStorage.getRestTimerState()
        )

        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(context)
        dev.neverpaid.health.core.platform.DudaWorkoutAppWidgetProvider().onUpdate(
            context,
            appWidgetManager,
            intArrayOf(1)
        )

        dev.neverpaid.health.core.platform.PlatformBridge.updateLiveWorkoutWidget(null)
        org.junit.Assert.assertTrue(
            "Expected live workout notification cleared when workout ends",
            shadowNm.allNotifications.isEmpty()
        )
    }
}
