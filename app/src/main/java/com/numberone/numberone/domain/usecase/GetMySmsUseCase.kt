
package com.numberone.numberone.domain.usecase

import com.numberone.numberone.domain.model.SmsMessage
import com.numberone.numberone.domain.repository.SubscriptionRepository

/**
 * Number One - Get My SMS Use Case
 * Retrieves SMS messages using the subscription access code.
 */
class GetMySmsUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {

    suspend operator fun invoke(
        accessCode: String
    ): Result<List<SmsMessage>> {

        val cleanedAccessCode = accessCode.trim()

        if (cleanedAccessCode.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Access code cannot be empty.")
            )
        }

        return runCatching {
            subscriptionRepository.getMySms(cleanedAccessCode)
        }
    }
}
