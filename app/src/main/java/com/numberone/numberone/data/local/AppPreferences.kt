
package com.numberone.numberone.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.numberone.numberone.core.localization.AppLanguage
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.numberOneDataStore by preferencesDataStore(
    name = "number_one_preferences"
)

class AppPreferences(
    private val context: Context
) {

    companion object {
        private val LANGUAGE_KEY =
            stringPreferencesKey("app_language")

        private val THEME_KEY =
            stringPreferencesKey("app_theme")

        private val ORDER_NOTIFICATIONS_KEY =
            booleanPreferencesKey("order_notifications")

        private val ACTIVATION_NOTIFICATIONS_KEY =
            booleanPreferencesKey("activation_notifications")

        private val EXPIRY_NOTIFICATIONS_KEY =
            booleanPreferencesKey("expiry_notifications")
    }

    private val preferencesFlow =
        context.numberOneDataStore.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }

    val appLanguage: Flow<AppLanguage> =
        preferencesFlow.map { preferences ->
            AppLanguage.fromLanguageCode(
                preferences[LANGUAGE_KEY] ?: "en"
            )
        }

    val appTheme: Flow<String> =
        preferencesFlow.map { preferences ->
            preferences[THEME_KEY] ?: "dark"
        }

    val orderNotifications: Flow<Boolean> =
        preferencesFlow.map { preferences ->
            preferences[ORDER_NOTIFICATIONS_KEY] ?: true
        }

    val activationNotifications: Flow<Boolean> =
        preferencesFlow.map { preferences ->
            preferences[ACTIVATION_NOTIFICATIONS_KEY] ?: true
        }

    val expiryNotifications: Flow<Boolean> =
        preferencesFlow.map { preferences ->
            preferences[EXPIRY_NOTIFICATIONS_KEY] ?: true
        }

    suspend fun setLanguage(language: AppLanguage) {
        context.numberOneDataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = language.languageCode
        }
    }

    suspend fun setTheme(theme: String) {
        require(theme in listOf("system", "light", "dark")) {
            "Invalid theme value"
        }

        context.numberOneDataStore.edit { preferences ->
            preferences[THEME_KEY] = theme
        }
    }

    suspend fun setOrderNotifications(enabled: Boolean) {
        context.numberOneDataStore.edit { preferences ->
            preferences[ORDER_NOTIFICATIONS_KEY] = enabled
        }
    }

    suspend fun setActivationNotifications(enabled: Boolean) {
        context.numberOneDataStore.edit { preferences ->
            preferences[ACTIVATION_NOTIFICATIONS_KEY] = enabled
        }
    }

    suspend fun setExpiryNotifications(enabled: Boolean) {
        context.numberOneDataStore.edit { preferences ->
            preferences[EXPIRY_NOTIFICATIONS_KEY] = enabled
        }
    }
}
