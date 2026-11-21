package com.numberone.numberone.notifications

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.localization.trf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.numberone.numberone.di.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Receives backend-pushed SMS-arrival events while the app is backgrounded.
 *
 * The title and body are written here in the app's language rather than taken from the
 * server payload. The OTP is never included in the push; the user opens the app to read it.
 */
class VirtualNumberMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Fails quietly when no user is signed in; PushTokenRegistrar re-sends on next Home visit.
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            AppContainer.virtualNumberRepository.registerDeviceToken(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        val activationId = message.data["activation_id"]
        val phone = message.data["phone"]
        val title = tr("وصل رمز تفعيل جديد")
        val body = if (phone.isNullOrBlank()) {
            tr("افتح Number One لعرض الرسالة الجديدة.")
        } else {
            trf("وصلت رسالة جديدة إلى الرقم {0}. افتح التطبيق لعرضها.", phone)
        }
        val id = if (activationId != null) {
            SmsNotifications.notifyId(activationId)
        } else {
            System.currentTimeMillis().toInt()
        }
        SmsNotifications.show(this, id, title, body)
    }
}
