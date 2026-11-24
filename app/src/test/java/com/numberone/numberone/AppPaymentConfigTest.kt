package com.numberone.numberone

import com.numberone.numberone.domain.model.AppPaymentConfig
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class AppPaymentConfigTest {
    @Test
    fun decodesLivePaymentConfigRpcShape() {
        val response = """
            [{
              "binance_wallet_address": "0x1111111111111111111111111111111111111111",
              "binance_network": "BEP20",
              "sham_cash_account": "SC-123456",
              "sham_cash_name": "Number One",
              "syp_usd_rate": 12500.0,
              "promo_banner_text": "Welcome"
            }]
        """.trimIndent()

        val config = Json.decodeFromString<List<AppPaymentConfig>>(response).single()

        assertEquals("0x1111111111111111111111111111111111111111", config.binanceWalletAddress)
        assertEquals("BEP20", config.binanceNetwork)
        assertEquals("SC-123456", config.shamCashAccount)
        assertEquals("Number One", config.shamCashName)
        assertEquals(12500.0, config.sypUsdRate, 0.0)
        assertEquals("Welcome", config.promoBannerText)
    }
}
