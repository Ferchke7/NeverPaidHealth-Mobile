package dev.neverpaid.health

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.os.Process
import android.util.Log
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import kotlin.system.exitProcess

internal object DudaCrashHandler {
    private const val PREFS_CRASH = "duda_crash_prefs"
    private const val KEY_LAST_TRACE = "last_crash_trace"

    fun install(context: Context) {
        if (Build.FINGERPRINT.contains("robolectric", ignoreCase = true)) return
        val appCtx = context.applicationContext
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val trace = buildReport(appCtx, thread, throwable)
            try {
                appCtx.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
                    .edit()
                    .putString(KEY_LAST_TRACE, trace)
                    .commit()
            } catch (_: Throwable) {
            }
            try {
                val intent = Intent(appCtx, CrashReportActivity::class.java).apply {
                    addFlags(
                        Intent.FLAG_ACTIVITY_NEW_TASK or
                            Intent.FLAG_ACTIVITY_CLEAR_TASK or
                            Intent.FLAG_ACTIVITY_NO_ANIMATION
                    )
                    putExtra(CrashReportActivity.EXTRA_TRACE, trace)
                }
                appCtx.startActivity(intent)
            } catch (_: Throwable) {
            }
            Process.killProcess(Process.myPid())
            exitProcess(10)
        }
    }

    private fun buildReport(context: Context, thread: Thread, throwable: Throwable): String {
        val versionName = try {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
        } catch (_: Throwable) {
            "unknown"
        }
        return buildString {
            appendLine("App: duda.uz $versionName")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE}, SDK ${Build.VERSION.SDK_INT})")
            appendLine("Thread: ${thread.name}")
            appendLine("Exception: ${throwable::class.java.name}: ${throwable.message}")
            appendLine()
            append(Log.getStackTraceString(throwable))
        }
    }

    internal fun loadLastReport(context: Context): String {
        return try {
            context.getSharedPreferences(PREFS_CRASH, Context.MODE_PRIVATE)
                .getString(KEY_LAST_TRACE, null)
                ?: "No crash trace recorded."
        } catch (_: Throwable) {
            "Unable to read crash trace."
        }
    }
}

class CrashReportActivity : Activity() {

    companion object {
        const val EXTRA_TRACE = "extra_trace"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val trace = intent?.getStringExtra(EXTRA_TRACE)
            ?: DudaCrashHandler.loadLastReport(this)

        val density = resources.displayMetrics.density
        val pad = (18 * density).toInt()
        val gap = (10 * density).toInt()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(Color.rgb(15, 13, 28))
            setPadding(pad, (36 * density).toInt(), pad, pad)
        }

        val title = TextView(this).apply {
            text = "duda.uz — Отчёт об ошибке"
            setTextColor(Color.WHITE)
            textSize = 20f
            typeface = Typeface.DEFAULT_BOLD
        }
        root.addView(title)

        val subtitle = TextView(this).apply {
            text = "Скопируйте текст ниже или очистите локальный кэш и перезапустите приложение."
            setTextColor(Color.rgb(203, 198, 232))
            textSize = 13f
            setPadding(0, gap / 2, 0, gap)
        }
        root.addView(subtitle)

        val actionsRow = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        val copyBtn = Button(this).apply {
            text = "Копировать"
            setBackgroundColor(Color.rgb(109, 40, 217))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                val cm = getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                cm?.setPrimaryClip(ClipData.newPlainText("Duda Crash", trace))
                Toast.makeText(this@CrashReportActivity, "Скопировано", Toast.LENGTH_SHORT).show()
            }
        }
        actionsRow.addView(copyBtn)

        val spacer = TextView(this).apply {
            layoutParams = LinearLayout.LayoutParams(gap, 1)
        }
        actionsRow.addView(spacer)

        val clearBtn = Button(this).apply {
            text = "Сбросить кэш"
            setBackgroundColor(Color.rgb(55, 48, 86))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f)
            setOnClickListener {
                getSharedPreferences("duda_storage", Context.MODE_PRIVATE).edit().clear().commit()
                relaunchMain()
            }
        }
        actionsRow.addView(clearBtn)

        root.addView(actionsRow)

        val restartBtn = Button(this).apply {
            text = "Перезапустить приложение"
            setBackgroundColor(Color.rgb(36, 29, 68))
            setTextColor(Color.WHITE)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply { topMargin = gap; bottomMargin = gap }
            setOnClickListener { relaunchMain() }
        }
        root.addView(restartBtn)

        val scroll = ScrollView(this).apply {
            setBackgroundColor(Color.rgb(9, 8, 18))
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                0,
                1f
            )
        }

        val traceView = TextView(this).apply {
            text = trace
            setTextColor(Color.rgb(245, 243, 255))
            textSize = 11.5f
            typeface = Typeface.MONOSPACE
            setTextIsSelectable(true)
            setPadding(gap, gap, gap, gap)
        }
        scroll.addView(traceView)
        root.addView(scroll)

        setContentView(root)
    }

    private fun relaunchMain() {
        val restartIntent = Intent(this, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        startActivity(restartIntent)
        finish()
        Process.killProcess(Process.myPid())
    }
}
