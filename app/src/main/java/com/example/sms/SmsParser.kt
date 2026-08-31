package com.example.sms

import com.example.data.model.TransactionEntity
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSmsResult(
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, INVESTMENT
    val category: String,
    val paymentMethod: String,
    val accountNumberLast4: String?,
    val timestamp: Long,
    val rawBody: String,
    val smsId: String
)

object SmsParser {

    // Regex for amounts: supports $12.34, Rs. 1,234.50, INR 500, USD 45.00, etc.
    private val AMOUNT_PATTERN = Pattern.compile(
        """(?:rs\.?|inr|usd|\$|€|£|cad|aud)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)\s*(?:rs\.?|inr|usd|\$|€|£)?""",
        Pattern.CASE_INSENSITIVE
    )

    private val DEBIT_KEYWORDS = listOf(
        "debited", "spent", "paid", "payment", "purchase", "sent", "deducted",
        "withdrawn", "charged", "dr", "txn of", "transferred to", "vpa", "pos", "atm wdl"
    )

    private val CREDIT_KEYWORDS = listOf(
        "credited", "received", "deposited", "refund", "cashback", "cr", "added to",
        "salary", "payroll", "dividend"
    )

    private val INVESTMENT_KEYWORDS = listOf(
        "mutual fund", "sip", "zerodha", "groww", "robinhood", "vanguard",
        "etf", "stock", "shares", "crypto", "coinswitch", "binance", "coinbase", "fd booked"
    )

    private val ACCOUNT_PATTERN = Pattern.compile(
        """(?:a\/c|acct|acc|account|card|ending|ending with|xx|x)\s*(?:no\.?)?\s*([0-9xX]*[0-9]{4})""",
        Pattern.CASE_INSENSITIVE
    )

    private val MERCHANT_PATTERN = Pattern.compile(
        """(?:at|to|info|merchant|towards|vpa|paid to|sent to)\s+([A-Za-z0-9\s\.\-_&@]+?)(?:\s+on|\s+ref|\s+using|\s+via|\s+avl|\s+bal|\s+upi|\.|\n|$)""",
        Pattern.CASE_INSENSITIVE
    )

    fun parse(smsBody: String, smsDate: Long, smsAddress: String? = null, messageId: String? = null): ParsedSmsResult? {
        val lower = smsBody.lowercase(Locale.ROOT)

        // Ignore OTP messages, OTP verifications, spam that don't involve financial balance changes
        if (isOtpOrNonFinancial(lower)) {
            return null
        }

        // 1. Detect Type
        val isInvestment = INVESTMENT_KEYWORDS.any { lower.contains(it) }
        val isDebit = DEBIT_KEYWORDS.any { lower.contains(it) }
        val isCredit = CREDIT_KEYWORDS.any { lower.contains(it) }

        if (!isInvestment && !isDebit && !isCredit) {
            return null
        }

        val type = when {
            isInvestment -> "INVESTMENT"
            isDebit -> "EXPENSE"
            isCredit -> "INCOME"
            else -> "EXPENSE"
        }

        // 2. Extract Amount
        val amount = extractAmount(smsBody) ?: return null
        if (amount <= 0.0) return null

        // 3. Extract Account Info
        val accountLast4 = extractAccount(smsBody)

        // 4. Extract Merchant or Title
        val merchant = extractMerchant(smsBody)
        val title = cleanTitle(merchant, smsAddress, type, isInvestment)

        // 5. Categorize based on merchant and keywords
        val category = categorize(lower, merchant, type)

        // 6. Payment Method
        val paymentMethod = detectPaymentMethod(lower)

        val id = messageId ?: "${smsDate}_${amount}_${title.hashCode()}"

        return ParsedSmsResult(
            title = title,
            amount = amount,
            type = type,
            category = category,
            paymentMethod = paymentMethod,
            accountNumberLast4 = accountLast4,
            timestamp = smsDate,
            rawBody = smsBody,
            smsId = id
        )
    }

    private fun isOtpOrNonFinancial(text: String): Boolean {
        if ((text.contains("otp") || text.contains("verification code") || text.contains("one time password") || text.contains("secret code"))
            && !text.contains("debited") && !text.contains("spent") && !text.contains("credited")) {
            return true
        }
        if (text.contains("click here to claim") || text.contains("won a lottery") || text.contains("apply for loan")) {
            return true
        }
        return false
    }

    private fun extractAmount(text: String): Double? {
        // Look for patterns like "debited for USD 45.20", "paid Rs 1,200.00", "Spent $35", etc.
        val specificPatterns = listOf(
            Pattern.compile("""(?:debited|spent|paid|purchase|credited|received|sent|deducted|amount|for|rs\.?|inr|usd|\$|€|£)\s*(?:by|of|for|with|is)?\s*(?:rs\.?|inr|usd|\$|€|£)?\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE),
            Pattern.compile("""(?:rs\.?|inr|usd|\$|€|£)\s*([0-9]{1,3}(?:,[0-9]{3})*(?:\.[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", Pattern.CASE_INSENSITIVE)
        )

        for (p in specificPatterns) {
            val m = p.matcher(text)
            if (m.find()) {
                val match = m.group(1)?.replace(",", "")
                val parsed = match?.toDoubleOrNull()
                if (parsed != null && parsed > 0.0) {
                    return parsed
                }
            }
        }

        val generalMatcher = AMOUNT_PATTERN.matcher(text)
        while (generalMatcher.find()) {
            val str = generalMatcher.group(1)?.replace(",", "")
            val parsed = str?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0 && parsed < 10000000.0) {
                return parsed
            }
        }
        return null
    }

    private fun extractAccount(text: String): String? {
        val m = ACCOUNT_PATTERN.matcher(text)
        if (m.find()) {
            val raw = m.group(1)?.trim() ?: return null
            return if (raw.length >= 4) raw.takeLast(4) else raw
        }
        return null
    }

    private fun extractMerchant(text: String): String? {
        val m = MERCHANT_PATTERN.matcher(text)
        if (m.find()) {
            val merchant = m.group(1)?.trim()
            if (!merchant.isNullOrBlank() && merchant.length in 3..40) {
                // Filter out non-merchant stop words
                val lower = merchant.lowercase(Locale.ROOT)
                if (!lower.startsWith("your a/c") && !lower.startsWith("account") && !lower.startsWith("balance")) {
                    return merchant
                }
            }
        }
        return null
    }

    private fun cleanTitle(merchant: String?, sender: String?, type: String, isInvestment: Boolean): String {
        if (!merchant.isNullOrBlank()) {
            return merchant.replace(Regex("""(?i)\s*(vpa|upi|ref|txn|a\/c|bank|ltd|pvt).*"""), "").trim()
                .ifBlank { merchant }
                .capitalizeWords()
        }
        if (isInvestment) return "Investment Asset Purchase"
        if (type == "INCOME") return "Income Deposit / Credit"
        if (!sender.isNullOrBlank() && sender.length > 2) {
            return "Payment via " + sender.takeLast(6).uppercase(Locale.ROOT)
        }
        return "Bank Expense"
    }

    private fun categorize(text: String, merchant: String?, type: String): String {
        val combined = "$text ${merchant ?: ""}".lowercase(Locale.ROOT)

        if (type == "INCOME") {
            return when {
                combined.contains("salary") || combined.contains("payroll") || combined.contains("wages") -> "Salary"
                combined.contains("dividend") || combined.contains("interest") -> "Dividends / Interest"
                combined.contains("refund") || combined.contains("cashback") || combined.contains("reversal") -> "Refund / Cashback"
                combined.contains("rent") -> "Rental Income"
                combined.contains("freelance") || combined.contains("upwork") || combined.contains("fiverr") || combined.contains("client") -> "Freelance / Business"
                else -> "Other Income"
            }
        }

        if (type == "INVESTMENT") {
            return when {
                combined.contains("crypto") || combined.contains("bitcoin") || combined.contains("btc") || combined.contains("eth") || combined.contains("binance") || combined.contains("coinbase") -> "Crypto"
                combined.contains("gold") || combined.contains("sovereign") -> "Gold / Commodities"
                combined.contains("sip") || combined.contains("mutual fund") || combined.contains("etf") || combined.contains("vanguard") -> "Mutual Funds / SIP"
                combined.contains("stock") || combined.contains("share") || combined.contains("zerodha") || combined.contains("groww") || combined.contains("robinhood") -> "Stocks / Equities"
                combined.contains("fd") || combined.contains("deposit") || combined.contains("bond") -> "Fixed Deposit / Bonds"
                else -> "Stocks / Equities"
            }
        }

        // EXPENSE categorization
        return when {
            // Food & Dining
            combined.contains("starbucks") || combined.contains("mcdonald") || combined.contains("burger") ||
            combined.contains("pizza") || combined.contains("swiggy") || combined.contains("zomato") ||
            combined.contains("restaurant") || combined.contains("cafe") || combined.contains("dining") ||
            combined.contains("bakery") || combined.contains("dunkin") || combined.contains("subway") ||
            combined.contains("food") || combined.contains("coffee") || combined.contains("tea") -> "Food & Dining"

            // Groceries
            combined.contains("grocery") || combined.contains("supermarket") || combined.contains("whole foods") ||
            combined.contains("trader joe") || combined.contains("costco") || combined.contains("walmart grocery") ||
            combined.contains("blinkit") || combined.contains("instacart") || combined.contains("mart") ||
            combined.contains("fresh") || combined.contains("provisions") -> "Groceries"

            // Shopping
            combined.contains("amazon") || combined.contains("flipkart") || combined.contains("ebay") ||
            combined.contains("target") || combined.contains("walmart") || combined.contains("zara") ||
            combined.contains("h&m") || combined.contains("myntra") || combined.contains("apple store") ||
            combined.contains("best buy") || combined.contains("mall") || combined.contains("clothing") ||
            combined.contains("shoes") || combined.contains("nike") || combined.contains("adidas") -> "Shopping"

            // Transport & Fuel
            combined.contains("uber") || combined.contains("lyft") || combined.contains("ola") ||
            combined.contains("petrol") || combined.contains("fuel") || combined.contains("shell") ||
            combined.contains("chevron") || combined.contains("gas station") || combined.contains("parking") ||
            combined.contains("toll") || combined.contains("fastag") || combined.contains("subway transit") ||
            combined.contains("metro") || combined.contains("train") || combined.contains("railway") -> "Transport & Fuel"

            // Bills & Utilities
            combined.contains("electricity") || combined.contains("power") || combined.contains("electric") ||
            combined.contains("water") || combined.contains("utility") || combined.contains("wifi") ||
            combined.contains("broadband") || combined.contains("internet") || combined.contains("airtel") ||
            combined.contains("jio") || combined.contains("verizon") || combined.contains("at&t") ||
            combined.contains("gas bill") || combined.contains("recharge") || combined.contains("postpaid") -> "Bills & Utilities"

            // Entertainment
            combined.contains("netflix") || combined.contains("spotify") || combined.contains("disney") ||
            combined.contains("hulu") || combined.contains("prime video") || combined.contains("cinema") ||
            combined.contains("theater") || combined.contains("movie") || combined.contains("bookmyshow") ||
            combined.contains("steam") || combined.contains("playstation") || combined.contains("xbox") ||
            combined.contains("concert") || combined.contains("gaming") -> "Entertainment"

            // Health & Medical
            combined.contains("pharmacy") || combined.contains("hospital") || combined.contains("clinic") ||
            combined.contains("medical") || combined.contains("doctor") || combined.contains("dentist") ||
            combined.contains("cvs") || combined.contains("walgreens") || combined.contains("apollo") ||
            combined.contains("medplus") || combined.contains("health") || combined.contains("diagnostics") -> "Health & Medical"

            // Travel
            combined.contains("airline") || combined.contains("flight") || combined.contains("airways") ||
            combined.contains("hotel") || combined.contains("airbnb") || combined.contains("booking.com") ||
            combined.contains("expedia") || combined.contains("makemytrip") || combined.contains("resort") -> "Travel"

            // Education
            combined.contains("school") || combined.contains("college") || combined.contains("university") ||
            combined.contains("course") || combined.contains("udemy") || combined.contains("coursera") ||
            combined.contains("tuition") || combined.contains("books") -> "Education"

            // Personal Care
            combined.contains("salon") || combined.contains("spa") || combined.contains("haircut") ||
            combined.contains("gym") || combined.contains("fitness") || combined.contains("cosmetics") ||
            combined.contains("sephora") -> "Personal Care"

            // Subscriptions
            combined.contains("subscription") || combined.contains("membership") || combined.contains("patreon") ||
            combined.contains("apple.com/bill") || combined.contains("google play") || combined.contains("cloud") -> "Subscriptions"

            else -> "Other Expense"
        }
    }

    private fun detectPaymentMethod(text: String): String {
        return when {
            text.contains("upi") || text.contains("gpay") || text.contains("phonepe") || text.contains("paytm") || text.contains("@okhdfc") || text.contains("@okaxis") -> "UPI"
            text.contains("credit card") || text.contains("visa card") || text.contains("mastercard") || text.contains("amex") -> "Credit Card"
            text.contains("debit card") || text.contains("atm") -> "Debit Card"
            text.contains("net banking") || text.contains("neft") || text.contains("rtgs") || text.contains("imps") || text.contains("bank transfer") -> "Bank Transfer"
            text.contains("wallet") -> "Wallet"
            text.contains("crypto") || text.contains("btc") -> "Crypto / Wallet"
            else -> "Bank"
        }
    }

    private fun String.capitalizeWords(): String {
        return this.split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
    }
}
