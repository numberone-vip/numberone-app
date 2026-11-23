
package com.numberone.numberone.domain.usecase.wallet

import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.repository.WalletRepository

class GetWalletTransactionsUseCase(
    private val walletRepository: WalletRepository
) {
    suspend operator fun invoke(): Result<List<WalletTransaction>> {
        return walletRepository.getTransactions()
    }
}
