package com.numberone.numberone.domain.usecase.wallet

import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.repository.WalletRepository

class GetMyWalletTopUpsUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<List<WalletTopUp>> {
        return walletRepository.getMyTopUpRequests()
    }
}