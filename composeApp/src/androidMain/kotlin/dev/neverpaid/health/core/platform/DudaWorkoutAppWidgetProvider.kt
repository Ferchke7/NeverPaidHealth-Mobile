package dev.neverpaid.health.core.platform

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context

/**
 * Android Home Screen widget displaying the currently active workout timer,
 * set progress, lifted volume, and instant rest timer controls.
 */
class DudaWorkoutAppWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        PlatformBridge.refreshHomeWorkoutWidgets(context.applicationContext ?: context, appWidgetIds)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        PlatformBridge.refreshHomeWorkoutWidgets(context.applicationContext ?: context)
    }
}
