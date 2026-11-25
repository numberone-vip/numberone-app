package com.numberone.numberone.data.repository

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.error.AppException
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.log.AppLog
import com.numberone.numberone.data.remote.FiveSimDataSource
import com.numberone.numberone.data.remote.FiveSimGateway
import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote
import com.numberone.numberone.domain.repository.VirtualNumberRepository
import kotlinx.coroutines.CancellationException

class VirtualNumberRepositoryImpl(
    private val source: FiveSimGateway = FiveSimDataSource()
) : VirtualNumberRepository {

    override suspend fun catalog(): Result<VirtualNumberCatalog> = guard { source.catalog() }

    override suspend fun quote(countryCode: String, serviceCode: String): Result<VirtualNumberQuote> = guard {
        require(countryCode.isNotBlank() && serviceCode.isNotBlank()) { tr("اختر الدولة والخدمة") }
        source.quote(countryCode, serviceCode)
    }

    override suspend fun purchase(
        countryCode: String,
        serviceCode: String,
        operator: String,
        requestId: String
    ): Result<VirtualNumberOrder> = guard {
        require(countryCode.isNotBlank() && serviceCode.isNotBlank() && operator.isNotBlank()) {
            tr("اختر الدولة والخدمة والمشغّل")
        }
        require(UUID_REGEX.matches(requestId)) { tr("معرّف طلب غير صالح") }
        source.purchase(countryCode, serviceCode, operator, requestId)
    }

    override suspend fun poll(activationId: String): Result<VirtualNumberOrder> = guard {
        require(activationId.isNotBlank()) { tr("معرّف التفعيل مطلوب") }
        source.poll(activationId)
    }

    override suspend fun latestActivation(): Result<VirtualNumberOrder?> = guard {
        source.latestActivation()
    }

    override suspend fun requestNextSms(activationId: String): Result<VirtualNumberOrder> = guard {
        require(activationId.isNotBlank()) { tr("معرّف التفعيل مطلوب") }
        source.requestNextSms(activationId)
    }

    override suspend fun cancel(activationId: String): Result<VirtualNumberOrder> = guard {
        require(activationId.isNotBlank()) { tr("معرّف التفعيل مطلوب") }
        source.cancel(activationId)
    }

    override suspend fun registerDeviceToken(token: String): Result<Unit> = guard {
        require(token.isNotBlank()) { tr("رمز الجهاز غير صالح") }
        source.registerDeviceToken(token)
    }

    /**
     * Runs one backend call and wraps any failure as a typed [AppException].
     * The UI turns it into text via ErrorMapper. Cancellation is rethrown so that a
     * leaving screen stops its polling immediately.
     */
    private suspend fun <T> guard(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        AppLog.warn(TAG, "virtual number call failed", e)
        Result.failure(AppException(ErrorMapper.classify(e), e))
    }

    private companion object {
        const val TAG = "VirtualNumber"
        val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
