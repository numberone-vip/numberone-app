package com.numberone.numberone.notifications

import android.content.Context
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.numberone.numberone.di.AppContainer
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/**
 * Sends this device's FCM token to the backend for the signed-in user.
 *
 * Safe to call at any time: it does nothing when Firebase is not configured or not yet
 * initialized, bounds the wait for a token, and never throws into the UI.
 */
object PushTokenRegistrar {

    private const val TOKEN_TIMEOUT_MS = 10_000L

    /** Variant for call sites without an Activity context (sign-in, app startup). */
    suspend fun registerCurrent(): Unit = registerCurrent(AppContainer.applicationContext())

    suspend fun registerCurrent(context: Context) {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) return
            val token = fetchToken() ?: return
            // Result is intentionally ignored: registration is retried on the next Home visit.
            AppContainer.virtualNumberRepository.registerDeviceToken(token)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            // Firebase unavailable or delayed: stay silent, never crash.
        }
    }

    private suspend fun fetchToken(): String? = withTimeoutOrNull(TOKEN_TIMEOUT_MS) {
        suspendCancellableCoroutine { continuation ->
            try {
                FirebaseMessaging.getInstance().token
                    .addOnSuccessListener { token -> if (continuation.isActive) continuation.resume(token) }
                    .addOnFailureListener { if (continuation.isActive) continuation.resume(null) }
            } catch (e: Throwable) {
                if (continuation.isActive) continuation.resume(null)
            }
        }
    }
}
