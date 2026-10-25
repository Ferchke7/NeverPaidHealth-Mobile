package dev.neverpaid.health

import android.app.Application
import dev.neverpaid.health.core.di.appModule
import dev.neverpaid.health.core.platform.PlatformBridge
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin

class NeverPaidHealthApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        PlatformBridge.init(this)

        startKoin {
            androidLogger()
            androidContext(this@NeverPaidHealthApplication)
            modules(appModule)
        }
    }
}
