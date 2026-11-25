package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote

/** Boundary between the repository and the Edge Function client. Fakeable in tests. */
interface FiveSimGateway {
    suspend fun catalog(): VirtualNumberCatalog
    suspend fun quote(countryCode: String, serviceCode: String): VirtualNumberQuote
    suspend fun purchase(countryCode: String, serviceCode: String, operator: String, requestId: String): VirtualNumberOrder
    suspend fun poll(activationId: String): VirtualNumberOrder
    suspend fun latestActivation(): VirtualNumberOrder?
    suspend fun requestNextSms(activationId: String): VirtualNumberOrder
    suspend fun cancel(activationId: String): VirtualNumberOrder
    suspend fun registerDeviceToken(token: String)
}
