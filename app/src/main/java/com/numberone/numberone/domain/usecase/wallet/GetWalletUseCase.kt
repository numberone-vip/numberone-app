package com.numberone.numberone.domain.usecase.wallet

import com.numberone.numberone.domain.model.Wallet
import com.numberone.numberone.domain.repository.WalletRepository

class GetWalletUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<Wallet> {
        return walletRepository.getWallet()
    }
}