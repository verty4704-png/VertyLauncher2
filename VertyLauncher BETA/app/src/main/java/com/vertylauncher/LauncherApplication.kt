package com.vertylauncher

import android.app.Application
import com.vertylauncher.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class LauncherApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@LauncherApplication)
            modules(appModule)
        }
    }
}
