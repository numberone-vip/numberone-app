package com.numberone.numberone

import android.app.Application
import android.content.Intent
import android.os.Process
import android.util.Log
import com.numberone.numberone.core.localization.AppStrings
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.notifications.PushTokenRegistrar
import java.io.File
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NumberOneApplication : Application() {
    override fun onCreate() {
        Thread.setDefaultUncaughtExceptionHandler { _, e ->
            val trace = Log.getStackTraceString(e)
            runCatching {
                File(getExternalFilesDir(null), "crash_log.txt").writeText(trace)
            }
            runCatching {
                val intent = Intent(this, CrashActivity::class.java).apply {
                    putExtra("err", trace)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                startActivity(intent)
            }
            runCatching { Thread.sleep(500) }
            Process.killProcess(Process.myPid())
        }
        super.onCreate()
        AppContainer.initialize(this)
        val safe = CoroutineExceptionHandler { _, _ -> }
        CoroutineScope(SupervisorJob() + Dispatchers.Main + safe).launch {
            runCatching { AppContainer.appPreferences.appLanguage.collect { AppStrings.language = it } }
        }
        CoroutineScope(SupervisorJob() + Dispatchers.IO + safe).launch {
            runCatching { PushTokenRegistrar.registerCurrent() }
        }
    }
}
