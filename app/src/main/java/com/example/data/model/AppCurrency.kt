package com.example.data.model

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

data class AppCurrency(
    val code: String,
    val symbol: String,
    val name: String,
    val flag: String,
    val isSymbolPrefix: Boolean = true
) {
    fun format(amount: Double, includeDecimals: Boolean = true): String {
        val symbols = DecimalFormatSymbols(Locale.US).apply {
            groupingSeparator = ','
            decimalSeparator = '.'
        }
        val pattern = if (includeDecimals) "#,##0.00" else "#,##0"
        val df = DecimalFormat(pattern, symbols)
        val formattedNum = df.format(amount)

        return if (isSymbolPrefix) {
            "$symbol$formattedNum"
        } else {
            "$formattedNum $symbol"
        }
    }

    fun formatCompact(amount: Double): String {
        return when {
            amount >= 1_000_000_000 -> "${format(amount / 1_000_000_000, true)}B"
            amount >= 1_000_000 -> "${format(amount / 1_000_000, true)}M"
            amount >= 1_000 -> "${format(amount / 1_000, true)}k"
            else -> format(amount, true)
        }
    }

    fun formatSigned(amount: Double, type: String): String {
        val formatted = format(Math.abs(amount))
        return when (type) {
            "INCOME" -> "+$formatted"
            "EXPENSE" -> "-$formatted"
            "INVESTMENT" -> "⚡$formatted"
            else -> formatted
        }
    }

    fun formatGainLoss(amount: Double): String {
        val formatted = format(Math.abs(amount), includeDecimals = false)
        return if (amount >= 0) "+$formatted" else "-$formatted"
    }

    fun formatSigned(amount: Double): String {
        return formatGainLoss(amount)
    }

    companion object {
        val USD = AppCurrency("USD", "$", "US Dollar", "🇺🇸")
        val EUR = AppCurrency("EUR", "€", "Euro", "🇪🇺")
        val GBP = AppCurrency("GBP", "£", "British Pound", "🇬🇧")
        val INR = AppCurrency("INR", "₹", "Indian Rupee", "🇮🇳")
        val JPY = AppCurrency("JPY", "¥", "Japanese Yen", "🇯🇵", isSymbolPrefix = true)
        val CAD = AppCurrency("CAD", "CA$", "Canadian Dollar", "🇨🇦")
        val AUD = AppCurrency("AUD", "A$", "Australian Dollar", "🇦🇺")
        val SGD = AppCurrency("SGD", "S$", "Singapore Dollar", "🇸🇬")
        val AED = AppCurrency("AED", "AED", "UAE Dirham", "🇦🇪")
        val CHF = AppCurrency("CHF", "CHF", "Swiss Franc", "🇨🇭")
        val CNY = AppCurrency("CNY", "¥", "Chinese Yuan", "🇨🇳")
        val BRL = AppCurrency("BRL", "R$", "Brazilian Real", "🇧🇷")
        val MXN = AppCurrency("MXN", "Mex$", "Mexican Peso", "🇲🇽")
        val KRW = AppCurrency("KRW", "₩", "South Korean Won", "🇰🇷")
        val SAR = AppCurrency("SAR", "SAR", "Saudi Riyal", "🇸🇦")
        val ZAR = AppCurrency("ZAR", "R", "South African Rand", "🇿🇦")
        val NZD = AppCurrency("NZD", "NZ$", "New Zealand Dollar", "🇳🇿")
        val SEK = AppCurrency("SEK", "kr", "Swedish Krona", "🇸🇪", isSymbolPrefix = false)
        val NOK = AppCurrency("NOK", "kr", "Norwegian Krone", "🇳🇴", isSymbolPrefix = false)
        val THB = AppCurrency("THB", "฿", "Thai Baht", "🇹🇭")
        val RUB = AppCurrency("RUB", "₽", "Russian Ruble", "🇷🇺")
        val TRY = AppCurrency("TRY", "₺", "Turkish Lira", "🇹🇷")
        val IDR = AppCurrency("IDR", "Rp", "Indonesian Rupiah", "🇮🇩")
        val MYR = AppCurrency("MYR", "RM", "Malaysian Ringgit", "🇲🇾")
        val PHP = AppCurrency("PHP", "₱", "Philippine Peso", "🇵🇭")
        val VND = AppCurrency("VND", "₫", "Vietnamese Dong", "🇻🇳", isSymbolPrefix = false)
        val PLN = AppCurrency("PLN", "zł", "Polish Zloty", "🇵🇱", isSymbolPrefix = false)
        val NGN = AppCurrency("NGN", "₦", "Nigerian Naira", "🇳🇬")
        val EGP = AppCurrency("EGP", "E£", "Egyptian Pound", "🇪🇬")
        val PKR = AppCurrency("PKR", "Rs", "Pakistani Rupee", "🇵🇰")
        val BDT = AppCurrency("BDT", "৳", "Bangladeshi Taka", "🇧🇩")

        val SUPPORTED_CURRENCIES = listOf(
            USD, EUR, GBP, INR, JPY, CAD, AUD, SGD, AED, CHF,
            CNY, BRL, MXN, KRW, SAR, ZAR, NZD, SEK, NOK, THB,
            RUB, TRY, IDR, MYR, PHP, VND, PLN, NGN, EGP, PKR, BDT
        )

        fun fromCode(code: String): AppCurrency {
            return SUPPORTED_CURRENCIES.find { it.code.equals(code, ignoreCase = true) } ?: USD
        }
    }
}
