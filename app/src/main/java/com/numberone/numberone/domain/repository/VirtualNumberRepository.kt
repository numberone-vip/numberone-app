package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote

interface VirtualNumberRepository {
    suspend fun catalog(): Result<VirtualNumberCatalog>
    suspend fun quote(countryCode: String, serviceCode: String): Result<VirtualNumberQuote>
    suspend fun purchase(countryCode: String, serviceCode: String, operator: String, requestId: String): Result<VirtualNumberOrder>
    suspend fun poll(activationId: String): Result<VirtualNumberOrder>
    suspend fun requestNextSms(activationId: String): Result<VirtualNumberOrder>
    suspend fun latestActivation(): Result<VirtualNumberOrder?>
    suspend fun hideActivation(activationId: String): Result<Unit>
    suspend fun cancel(activationId: String): Result<VirtualNumberOrder>
    suspend fun registerDeviceToken(token: String): Result<Unit>
}
