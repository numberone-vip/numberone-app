package com.numberone.numberone.notifications

import com.numberone.numberone.core.localization.tr
import android.Manifest
import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.numberone.numberone.MainActivity

/**
 * Single place that builds SMS-arrival notifications.
 *
 * Privacy: the OTP itself is never placed in the notification (lock-screen visibility is
 * private). The user opens the app to read the code.
 *
 * De-duplication: foreground polling and FCM both use [notifyId] for the same activation,
 * so the second notification replaces the first instead of stacking.
 */
object SmsNotifications {
    const val CHANNEL_ID = "virtual_number_sms"

    fun notifyId(activationId: String): Int = activationId.hashCode()

    fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            tr("رسائل الأرقام المؤقتة"),
            NotificationManager.IMPORTANCE_HIGH
        )
        channel.description = tr("تنبيهات وصول رسائل التحقق")
        manager.createNotificationChannel(channel)
    }

    fun dismiss(context: Context, id: Int) {
        NotificationManagerCompat.from(context).cancel(id)
    }

    fun canPost(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    fun show(context: Context, id: Int, title: String, body: String) {
        ensureChannel(context)
        if (!canPost(context)) return

        val openApp = Intent(context, MainActivity::class.java)
            .addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TOP
            )
        val contentIntent = PendingIntent.getActivity(
            context,
            id,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.sym_action_email)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setColor(0xFFD9B26A.toInt())
            .setSubText("Number One")
            .setWhen(System.currentTimeMillis())
            .setShowWhen(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(id, notification)
    }
}
