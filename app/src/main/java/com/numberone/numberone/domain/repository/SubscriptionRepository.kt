
package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.model.Subscription

/**
 * Number One - Subscription Repository
 * Defines subscription and SMS operations.
 */
interface SubscriptionRepository {

    suspend fun getSubscription(
        accessCode: String
    ): Subscription?

    suspend fun getMySms(
        accessCode: String
    ): List<SmsMessage>
}
