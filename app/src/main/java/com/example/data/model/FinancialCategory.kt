package com.example.data.model

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.CategoryColors

enum class TransactionType {
    EXPENSE,
    INCOME,
    INVESTMENT
}

enum class InvestmentType(val displayName: String) {
    STOCKS("Stocks & Equities"),
    MUTUAL_FUNDS("Mutual Funds / ETFs"),
    CRYPTO("Cryptocurrency"),
    REAL_ESTATE("Real Estate / REITs"),
    GOLD("Gold & Precious Metals"),
    SAVINGS_BONDS("Bonds & Term Deposits")
}

data class CategoryItem(
    val name: String,
    val type: TransactionType,
    val iconName: String,
    val colorHex: Long
)

object DefaultCategories {
    val EXPENSE_CATEGORIES = listOf(
        CategoryItem("Food & Dining", TransactionType.EXPENSE, "restaurant", 0xFF10B981),
        CategoryItem("Shopping", TransactionType.EXPENSE, "shopping_bag", 0xFF3B82F6),
        CategoryItem("Bills & Utilities", TransactionType.EXPENSE, "receipt_long", 0xFFF59E0B),
        CategoryItem("Transport & Fuel", TransactionType.EXPENSE, "directions_car", 0xFFEF4444),
        CategoryItem("Entertainment", TransactionType.EXPENSE, "movie", 0xFF8B5CF6),
        CategoryItem("Health & Medical", TransactionType.EXPENSE, "local_hospital", 0xFFEC4899),
        CategoryItem("Groceries", TransactionType.EXPENSE, "local_grocery_store", 0xFF14B8A6),
        CategoryItem("Travel", TransactionType.EXPENSE, "flight", 0xFF6366F1),
        CategoryItem("Education", TransactionType.EXPENSE, "school", 0xFF84CC16),
        CategoryItem("Personal Care", TransactionType.EXPENSE, "spa", 0xFFF97316),
        CategoryItem("Subscriptions", TransactionType.EXPENSE, "subscriptions", 0xFF06B6D4),
        CategoryItem("Other Expense", TransactionType.EXPENSE, "more_horiz", 0xFF64748B)
    )

    val INCOME_CATEGORIES = listOf(
        CategoryItem("Salary", TransactionType.INCOME, "account_balance_wallet", 0xFF10B981),
        CategoryItem("Freelance / Business", TransactionType.INCOME, "work", 0xFF3B82F6),
        CategoryItem("Dividends / Interest", TransactionType.INCOME, "trending_up", 0xFF8B5CF6),
        CategoryItem("Rental Income", TransactionType.INCOME, "home", 0xFFF59E0B),
        CategoryItem("Refund / Cashback", TransactionType.INCOME, "currency_exchange", 0xFF14B8A6),
        CategoryItem("Other Income", TransactionType.INCOME, "savings", 0xFF64748B)
    )

    val INVESTMENT_CATEGORIES = listOf(
        CategoryItem("Stocks / Equities", TransactionType.INVESTMENT, "show_chart", 0xFF3B82F6),
        CategoryItem("Mutual Funds / SIP", TransactionType.INVESTMENT, "pie_chart", 0xFF8B5CF6),
        CategoryItem("Crypto", TransactionType.INVESTMENT, "currency_bitcoin", 0xFFF59E0B),
        CategoryItem("Real Estate", TransactionType.INVESTMENT, "apartment", 0xFF10B981),
        CategoryItem("Gold / Commodities", TransactionType.INVESTMENT, "monetization_on", 0xFFEAB308),
        CategoryItem("Fixed Deposit / Bonds", TransactionType.INVESTMENT, "account_balance", 0xFF06B6D4)
    )

    fun getAllCategoryNames(): List<String> {
        return (EXPENSE_CATEGORIES + INCOME_CATEGORIES + INVESTMENT_CATEGORIES).map { it.name }
    }

    fun getColorForCategory(name: String): Color {
        val match = (EXPENSE_CATEGORIES + INCOME_CATEGORIES + INVESTMENT_CATEGORIES).firstOrNull {
            it.name.equals(name, ignoreCase = true)
        }
        if (match != null) {
            return Color(match.colorHex)
        }
        val hash = kotlin.math.abs(name.hashCode())
        return CategoryColors[hash % CategoryColors.size]
    }
}
