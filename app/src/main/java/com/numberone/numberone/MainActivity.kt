package com.numberone.numberone

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.numberone.numberone.core.localization.AppStrings
import com.numberone.numberone.core.localization.AppLanguage
import com.numberone.numberone.core.theme.NumberOneTheme
import com.numberone.numberone.di.AppContainer
import com.numberone.numberone.presentation.navigation.AppNavGraph

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Draw behind the status and navigation bars; screens pad with safe-area insets.
        enableEdgeToEdge()
        setContent {
            val language by AppContainer.appPreferences.appLanguage.collectAsState(initial = AppLanguage.ENGLISH)
            val theme by AppContainer.appPreferences.appTheme.collectAsState(initial = "system")
            val dark = when (theme) {
                "dark" -> true
                "light" -> false
                else -> isSystemInDarkTheme()
            }
            // Keep error text in the same language the user picked.
            SideEffect { AppStrings.language = language }
            NumberOneTheme(darkTheme = dark, isArabic = language.isArabic) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavGraph()
                }
            }
        }
    }
}
