package com.numberone.numberone

import android.app.Application
import com.numberone.numberone.core.localization.AppStrings
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.notifications.PushTokenRegistrar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Number One Application
 * Initializes application-wide dependencies.
 */
class NumberOneApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        AppContainer.initialize(this)

        // Keep the app-wide language in sync with the saved preference (also used by background notifications).
        CoroutineScope(SupervisorJob() + Dispatchers.Main).launch {
            AppContainer.appPreferences.appLanguage.collect { AppStrings.language = it }
        }

        // Startup registration: fails quietly if no session has been restored yet.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            PushTokenRegistrar.registerCurrent()
        }
    }
}
