package com.numberone.numberone.domain.usecase.wallet

import com.numberone.numberone.core.localization.tr
import com.numberone.numberone.domain.model.CreateWalletTopUp
import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.repository.WalletRepository

class CreateWalletTopUpUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(request: CreateWalletTopUp): Result<WalletTopUp> {
        if (!request.amount.isFinite() || request.amount <= 0.0 || request.amount > 100000.0) {
            return Result.failure(IllegalArgumentException(tr("المبلغ يجب أن يكون أكبر من صفر وأقل من 100000 USDT")))
        }
        return walletRepository.createTopUpRequest(request)
    }
}
