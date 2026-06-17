package com.livana.app.core.model

import java.math.BigDecimal
import java.math.BigInteger
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale

/**
 * USDC amount as raw atomic units (6 decimals). 1_000_000 == $1.00.
 * Never use floating point for on-chain amounts.
 */
@JvmInline
value class Usdc(val atomic: BigInteger) {
    fun toDecimal(): BigDecimal = atomic.toBigDecimal().divide(SCALE, 2, RoundingMode.DOWN)

    /** Display as "$1,234.56". */
    fun format(): String = "$" + "%,.2f".format(toDecimal())

    /** Display as "$1,234" (whole dollars, no cents). */
    fun formatWhole(): String {
        val formatter = NumberFormat.getCurrencyInstance(Locale.US).apply {
            maximumFractionDigits = 0
            minimumFractionDigits = 0
        }
        return formatter.format(toDecimal())
    }

    companion object {
        private val SCALE = BigDecimal(1_000_000)
        fun ofDollars(dollars: BigDecimal): Usdc =
            Usdc(dollars.multiply(SCALE).toBigInteger())
    }
}

