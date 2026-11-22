
package com.numberone.numberone.data.repository

import com.numberone.numberone.data.remote.SupabaseRpcDataSource
import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.model.Subscription
import com.numberone.numberone.domain.repository.SubscriptionRepository

/**
 * Number One - Subscription Repository Implementation
 * Connects subscription operations to Supabase RPC.
 */
class SubscriptionRepositoryImpl(
    private val rpcDataSource: SupabaseRpcDataSource
) : SubscriptionRepository {

    override suspend fun getSubscription(
        accessCode: String
    ): Subscription? {
        return rpcDataSource.getSubscription(
            accessCode = accessCode
        )
    }

    override suspend fun getMyAccountSubscriptions(): List<Subscription> = rpcDataSource.getMyAccountSubscriptions()

    override suspend fun getMyAccountSms(): List<SmsMessage> = rpcDataSource.getMyAccountSms()

    override suspend fun getMySms(
        accessCode: String
    ): List<SmsMessage> {
        return rpcDataSource.getMySms(
            accessCode = accessCode
        )
    }
}
