package com.numberone.numberone.domain.usecase

import com.numberone.numberone.domain.model.Subscription
import com.numberone.numberone.domain.repository.SubscriptionRepository

/**
 * Number One - Get Subscription Use Case
 * Validates and retrieves a subscription using its access code.
 */
class GetSubscriptionUseCase(
    private val subscriptionRepository: SubscriptionRepository
) {

    suspend operator fun invoke(
        accessCode: String
    ): Result<Subscription?> {

        val cleanedAccessCode = accessCode.trim()

        if (cleanedAccessCode.isBlank()) {
            return Result.failure(
                IllegalArgumentException("Access code cannot be empty.")
            )
        }

        return runCatching {
            subscriptionRepository.getSubscription(cleanedAccessCode)
        }
    }
}
