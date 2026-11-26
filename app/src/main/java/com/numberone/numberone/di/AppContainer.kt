package com.numberone.numberone.di

import android.content.Context
import com.numberone.numberone.data.local.AppPreferences
import com.numberone.numberone.data.remote.*
import com.numberone.numberone.data.repository.*
import com.numberone.numberone.domain.repository.*

object AppContainer {
    private lateinit var appContext: Context
    fun initialize(context: Context) { appContext = context.applicationContext }
    private fun context() = checkNotNull(if (::appContext.isInitialized) appContext else null) { "Application not initialized" }
    /** Application context for call sites without an Activity (e.g. right after sign-in). */
    fun applicationContext(): Context = context()
    val appPreferences by lazy { AppPreferences(context()) }
    private val authSource by lazy { SupabaseAuthDataSource() }
    private val rpcSource by lazy { SupabaseRpcDataSource() }
    private val walletSource: WalletDataSource by lazy { SupabaseWalletDataSource() }
    val userRepository: UserRepository by lazy { UserRepositoryImpl(authSource) }
    val subscriptionRepository: SubscriptionRepository by lazy { SubscriptionRepositoryImpl(rpcSource) }
    val purchaseRepository: PurchaseRepository by lazy { PurchaseRepositoryImpl() }
    val walletRepository: WalletRepository by lazy { WalletRepositoryImpl(walletSource) }
    val virtualNumberRepository: VirtualNumberRepository by lazy { VirtualNumberRepositoryImpl() }
    val adminRepository: AdminRepository by lazy { AdminRepositoryImpl() }
    val engagementRepository by lazy { EngagementRepository() }
}
