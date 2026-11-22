package com.numberone.numberone.domain.model

/** Input for wallet_create_deposit_request(p_amount). The server fixes currency/network to USDT/BEP20. */
data class CreateWalletTopUp(
    val amount: Double,
    val transactionReference: String? = null,
    val currency: String = "USDT",
    val network: String = "BEP20"
)
