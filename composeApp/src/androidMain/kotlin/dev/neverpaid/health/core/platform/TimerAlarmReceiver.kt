package dev.neverpaid.health.core.platform

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * BroadcastReceiver triggered by AlarmManager when a background rest or focus countdown
 * reaches its wall-clock target timestamp.
 */
class TimerAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val timerId = intent.getStringExtra(EXTRA_TIMER_ID) ?: return
        val triggerAtEpochMs = intent.getLongExtra(EXTRA_TRIGGER_AT, -1L)
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_BODY).orEmpty()
        PlatformBridge.handleAlarmBroadcast(
            context = context.applicationContext ?: context,
            timerId = timerId,
            triggerAtEpochMs = triggerAtEpochMs,
            title = title,
            body = body
        )
    }

    companion object {
        const val EXTRA_TIMER_ID = "extra_timer_id"
        const val EXTRA_TRIGGER_AT = "extra_trigger_at"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
    }
}
