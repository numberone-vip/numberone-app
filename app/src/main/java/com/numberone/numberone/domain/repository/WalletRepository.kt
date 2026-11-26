package com.numberone.numberone.domain.repository

import com.numberone.numberone.domain.model.CreateWalletTopUp
import com.numberone.numberone.domain.model.Wallet
import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.model.Service
import com.numberone.numberone.domain.model.WalletOrderResult
import com.numberone.numberone.domain.model.PaymentOrderCreation
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.ShamCashTopUp

interface WalletRepository {
    suspend fun getWallet(): Result<Wallet>
    suspend fun getTransactions(): Result<List<WalletTransaction>>
    suspend fun createTopUpRequest(request: CreateWalletTopUp): Result<WalletTopUp>
    suspend fun getMyTopUpRequests(): Result<List<WalletTopUp>>
    suspend fun submitDepositTransaction(requestId: String, transactionHash: String): Result<Boolean>
    suspend fun verifyDeposit(requestId: String, transactionHash: String): Result<String>
    suspend fun getServices(): Result<List<Service>>
    suspend fun getMyOrders(): Result<List<Order>>
    suspend fun createOrder(serviceId: String): Result<PaymentOrderCreation>
    suspend fun createOrderWithMonths(serviceId: String, months: Int): Result<PaymentOrderCreation> = createOrder(serviceId)
    suspend fun uploadReceipt(orderId: String, bytes: ByteArray, mimeType: String, extension: String): Result<String>
    suspend fun submitOrderPayment(orderId: String, transactionHash: String, receiptPath: String): Result<Boolean>
    suspend fun purchaseServiceWithWallet(serviceId: String, requestId: String): Result<WalletOrderResult>
    suspend fun purchaseServiceWithWalletMonths(serviceId: String, requestId: String, months: Int): Result<WalletOrderResult> = purchaseServiceWithWallet(serviceId, requestId)
    suspend fun createShamCashTopUp(paymentMethod: String, paidAmount: Double): Result<String>
    suspend fun submitShamCashReceipt(requestId: String, bytes: ByteArray, mimeType: String, extension: String): Result<Boolean>
    suspend fun getMyShamCashTopUps(): Result<List<ShamCashTopUp>>
}
