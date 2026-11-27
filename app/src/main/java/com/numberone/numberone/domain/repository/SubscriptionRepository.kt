package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.model.Subscription

interface SubscriptionRepository {
    suspend fun getSubscriptionById(subscriptionId: String): Subscription?
    suspend fun getSubscription(accessCode: String): Subscription?
    suspend fun getMySms(accessCode: String): List<SmsMessage>
}
