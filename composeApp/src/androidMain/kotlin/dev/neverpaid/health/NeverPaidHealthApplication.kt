package dev.neverpaid.health

import android.app.Application
import android.os.Build
import dev.neverpaid.health.core.di.appModule
import dev.neverpaid.health.core.platform.PlatformBridge
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import java.io.File

class NeverPaidHealthApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        if (isCrashReporterProcess()) return

        DudaCrashHandler.install(this)
        PlatformBridge.init(this)

        if (org.koin.core.context.GlobalContext.getOrNull() == null) {
            startKoin {
                androidLogger()
                androidContext(this@NeverPaidHealthApplication)
                modules(appModule)
            }
        }
    }

    private fun isCrashReporterProcess(): Boolean {
        val processName = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getProcessName()
        } else {
            try {
                File("/proc/self/cmdline").readText().trim('\u0000', ' ', '\n', '\r')
            } catch (_: Throwable) {
                ""
            }
        }
        return processName.endsWith(":crash")
    }
}
