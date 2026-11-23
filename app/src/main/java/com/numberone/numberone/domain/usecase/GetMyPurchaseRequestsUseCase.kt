
package com.numberone.numberone.domain.usecase

import com.numberone.numberone.domain.model.PurchaseRequest
import com.numberone.numberone.domain.repository.PurchaseRepository

/**
 * Number One - Get My Purchase Requests Use Case
 * Retrieves the signed-in customer's purchase requests.
 */
class GetMyPurchaseRequestsUseCase(
    private val purchaseRepository: PurchaseRepository
) {

    suspend operator fun invoke(): Result<List<PurchaseRequest>> {
        return purchaseRepository.getMyPurchaseRequests()
    }
}
