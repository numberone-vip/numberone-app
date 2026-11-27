package com.numberone.numberone

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.numberone.numberone.core.localization.AppLanguage
import com.numberone.numberone.core.localization.AppStrings
import com.numberone.numberone.core.theme.NumberOneTheme
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.notifications.AppAlertMonitor
import com.numberone.numberone.notifications.SmsNotifications
import com.numberone.numberone.presentation.navigation.AppNavGraph
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : FragmentActivity() {

    private val requestNotifPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            lifecycleScope.launch { AppAlertMonitor.checkAndNotify(this@MainActivity) }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        SmsNotifications.ensureChannel(this)

        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                while (true) {
                    AppAlertMonitor.checkAndNotify(this@MainActivity)
                    delay(20_000L)
                }
            }
        }

        setContent {
            val language by AppContainer.appPreferences.appLanguage.collectAsState(initial = AppLanguage.ENGLISH)
            val theme by AppContainer.appPreferences.appTheme.collectAsState(initial = "system")
            val dark = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            SideEffect { AppStrings.language = language }
            LaunchedEffect(Unit) { AppAlertMonitor.checkAndNotify(this@MainActivity) }
            NumberOneTheme(darkTheme = dark, isArabic = language.isArabic) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                    com.numberone.numberone.core.util.InAppUpdateDialog()
                }
            }
        }
    }
}
