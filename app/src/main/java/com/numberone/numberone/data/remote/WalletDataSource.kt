package com.numberone.numberone.data.remote

import com.numberone.numberone.domain.model.CreateWalletTopUp
import com.numberone.numberone.domain.model.Wallet
import com.numberone.numberone.domain.model.WalletTopUp
import com.numberone.numberone.domain.model.WalletTransaction
import com.numberone.numberone.domain.model.Service
import com.numberone.numberone.domain.model.WalletOrderResult
import com.numberone.numberone.domain.model.PaymentOrderCreation
import com.numberone.numberone.domain.model.Order
import com.numberone.numberone.domain.model.ShamCashTopUp
import com.numberone.numberone.domain.model.AppPaymentConfig

interface WalletDataSource {
    suspend fun getWallet(userId: String): Wallet
    suspend fun getTransactions(userId: String): List<WalletTransaction>
    suspend fun createTopUpRequest(userId: String, request: CreateWalletTopUp): WalletTopUp
    suspend fun getMyTopUpRequests(userId: String): List<WalletTopUp>
    suspend fun submitDepositTransaction(requestId: String, transactionHash: String): Boolean
    suspend fun verifyDeposit(requestId: String, transactionHash: String): String
    suspend fun getServices(): List<Service>
    suspend fun getMyOrders(userId: String): List<Order>
    suspend fun createOrder(serviceId: String): PaymentOrderCreation
    suspend fun uploadReceipt(orderId: String, bytes: ByteArray, mimeType: String, extension: String): String
    suspend fun submitOrderPayment(orderId: String, transactionHash: String, receiptPath: String): Boolean
    suspend fun purchaseServiceWithWallet(serviceId: String, requestId: String): WalletOrderResult
    suspend fun createShamCashTopUp(paymentMethod: String, paidAmount: Double): String
    suspend fun submitShamCashReceipt(requestId: String, bytes: ByteArray, mimeType: String, extension: String): Boolean
    suspend fun getMyShamCashTopUps(): List<ShamCashTopUp>
    suspend fun getPaymentConfig(): AppPaymentConfig
}
