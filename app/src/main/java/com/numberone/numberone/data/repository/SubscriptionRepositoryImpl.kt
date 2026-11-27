package com.numberone.numberone.data.repository

import com.numberone.numberone.data.remote.SupabaseRpcDataSource
import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.model.Subscription
import com.numberone.numberone.domain.repository.SubscriptionRepository

class SubscriptionRepositoryImpl(
    private val rpcDataSource: SupabaseRpcDataSource
) : SubscriptionRepository {

    override suspend fun getSubscriptionById(subscriptionId: String): Subscription? =
        rpcDataSource.getSubscriptionById(subscriptionId = subscriptionId)

    override suspend fun getSubscription(accessCode: String): Subscription? =
        rpcDataSource.getSubscription(accessCode = accessCode)

    override suspend fun getMySms(accessCode: String): List<SmsMessage> =
        rpcDataSource.getMySms(accessCode = accessCode)
}
