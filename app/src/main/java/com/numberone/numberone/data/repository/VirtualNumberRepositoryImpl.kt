package com.numberone.numberone.data.repository

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.core.error.ErrorMapper
import com.numberone.numberone.core.error.UserFacingException
import com.numberone.numberone.data.remote.FiveSimDataSource
import com.numberone.numberone.domain.model.VirtualNumberCatalog
import com.numberone.numberone.domain.model.VirtualNumberOrder
import com.numberone.numberone.domain.model.VirtualNumberQuote
import com.numberone.numberone.domain.repository.VirtualNumberRepository
import kotlinx.coroutines.CancellationException

class VirtualNumberRepositoryImpl(
    private val source: FiveSimDataSource = FiveSimDataSource()
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
     * Runs one backend call and converts any failure into a short, user-facing message.
     * Cancellation is rethrown so that a leaving screen stops its polling immediately.
     */
    private suspend fun <T> guard(block: suspend () -> T): Result<T> = try {
        Result.success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: Throwable) {
        Result.failure(UserFacingException(userMessage(e)))
    }

    private fun userMessage(error: Throwable): String {
        if (error is UserFacingException) return error.message.orEmpty()
        val raw = error.message.orEmpty()
        val lower = raw.lowercase()
        return when {
            // Messages written in Arabic by this app (validation) are already user-facing.
            raw.any { it in '\u0600'..'\u06FF' } -> raw
            "no stock" in lower ->
                tr("لا يوجد مخزون لهذا الاختيار حالياً. جرّب مشغلاً أو دولة أخرى.")
            "insufficient" in lower ->
                tr("الرصيد غير كافٍ لإتمام الشراء.")
            "not confirmed" in lower || "reconcil" in lower ||
                "persistence is pending" in lower || "retained" in lower ->
                tr("عملية الشراء قيد المراجعة. لن يُخصم المبلغ مرتين، حاول لاحقاً أو تواصل مع الدعم.")
            "already used with different" in lower ->
                tr("معرّف الطلب مستخدم بتفاصيل مختلفة. أعد المحاولة من جديد.")
            "still pending" in lower ->
                tr("لديك طلب سابق قيد المراجعة. انتظر اكتماله قبل طلب رقم جديد.")
            "not found" in lower ->
                tr("لم يتم العثور على هذا التفعيل.")
            "session" in lower || "authentication required" in lower ->
                tr("انتهت الجلسة. يرجى تسجيل الدخول مجدداً.")
            "failed (4" in lower ->
                tr("لا توجد أرقام متاحة لهذا الاختيار الآن. جرّب لاحقاً.")
            else -> ErrorMapper.toUserMessage(error)
        }
    }

    private companion object {
        val UUID_REGEX = Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")
    }
}
