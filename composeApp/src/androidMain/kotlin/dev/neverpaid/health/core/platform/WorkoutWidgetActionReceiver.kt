package dev.neverpaid.health.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Handles quick gym & rest-timer actions tapped on the lock-screen notification
 * or home-screen workout widget without forcing the user to unlock and open the app.
 */
class WorkoutWidgetActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        PlatformBridge.handleWorkoutWidgetAction(context.applicationContext ?: context, action)
    }

    companion object {
        const val ACTION_REST_START = "dev.neverpaid.health.WIDGET_REST_START"
        const val ACTION_REST_ADD_15 = "dev.neverpaid.health.WIDGET_REST_ADD_15"
        const val ACTION_REST_SKIP = "dev.neverpaid.health.WIDGET_REST_SKIP"
    }
}
