package com.numberone.numberone.core.util

/**
 * Transfer details shown to customers on the Sham Cash deposit flow.
 *
 * TODO(owner): fill these before release. While a value is blank the matching copy row is simply
 * hidden, so customers are never shown an empty or wrong account.
 */
object PaymentConfig {
    /** Sham Cash account / wallet code that customers send money to. */
    val SHAM_CASH_ACCOUNT: String = ""

    /** Optional account holder name shown above the account code. */
    val SHAM_CASH_ACCOUNT_NAME: String = ""
}
