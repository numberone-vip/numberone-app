
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

    /** Numbers owned by the signed-in account. */
    suspend fun getMyAccountSubscriptions(): List<Subscription>

    /** Messages for the signed-in account's active numbers (no access code). */
    suspend fun getMyAccountSms(): List<SmsMessage>

    suspend fun getMySms(
        accessCode: String
    ): List<SmsMessage>
}
