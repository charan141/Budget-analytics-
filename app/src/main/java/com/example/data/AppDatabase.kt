package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.dao.BudgetDao
import com.example.data.dao.InvestmentDao
import com.example.data.dao.SmsSyncLogDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.InvestmentEntity
import com.example.data.model.SmsSyncLogEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Database(
    entities = [
        TransactionEntity::class,
        BudgetEntity::class,
        InvestmentEntity::class,
        SmsSyncLogEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun budgetDao(): BudgetDao
    abstract fun investmentDao(): InvestmentDao
    abstract fun smsSyncLogDao(): SmsSyncLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "expense_wealth_database"
                )
                    .addCallback(DatabaseCallback())
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    CoroutineScope(Dispatchers.IO).launch {
                        populateInitialData(database)
                    }
                }
            }
        }

        private suspend fun populateInitialData(database: AppDatabase) {
            val now = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = monthFormat.format(Date(now))

            // 1. Initial Sample Transactions
            val dayMillis = 24 * 60 * 60 * 1000L
            val sampleTransactions = listOf(
                TransactionEntity(
                    title = "Tech Corp Monthly Salary",
                    amount = 5400.00,
                    type = "INCOME",
                    category = "Salary",
                    timestamp = now - 2 * dayMillis,
                    paymentMethod = "Bank Transfer",
                    notes = "Monthly net pay deposit"
                ),
                TransactionEntity(
                    title = "Freelance Mobile App UI Design",
                    amount = 850.00,
                    type = "INCOME",
                    category = "Freelance / Business",
                    timestamp = now - 6 * dayMillis,
                    paymentMethod = "UPI",
                    notes = "Milestone payment"
                ),
                TransactionEntity(
                    title = "Whole Foods Market",
                    amount = 142.50,
                    type = "EXPENSE",
                    category = "Groceries",
                    timestamp = now - 1 * dayMillis,
                    paymentMethod = "Credit Card",
                    notes = "Weekly grocery restock"
                ),
                TransactionEntity(
                    title = "Starbucks Reserve Cafe",
                    amount = 16.75,
                    type = "EXPENSE",
                    category = "Food & Dining",
                    timestamp = now - 4 * 3600 * 1000L,
                    paymentMethod = "UPI",
                    notes = "Coffee & croissant with teammate"
                ),
                TransactionEntity(
                    title = "Electric & Power Utility",
                    amount = 85.20,
                    type = "EXPENSE",
                    category = "Bills & Utilities",
                    timestamp = now - 4 * dayMillis,
                    paymentMethod = "Bank Transfer",
                    notes = "Monthly electricity bill"
                ),
                TransactionEntity(
                    title = "Uber Ride Downtown",
                    amount = 28.40,
                    type = "EXPENSE",
                    category = "Transport & Fuel",
                    timestamp = now - 12 * 3600 * 1000L,
                    paymentMethod = "UPI",
                    notes = "Commute to tech conference"
                ),
                TransactionEntity(
                    title = "Amazon Electronics - Noise Cancelling Headphones",
                    amount = 199.99,
                    type = "EXPENSE",
                    category = "Shopping",
                    timestamp = now - 5 * dayMillis,
                    paymentMethod = "Credit Card",
                    notes = "Productivity headset"
                ),
                TransactionEntity(
                    title = "Netflix 4K Subscription",
                    amount = 19.99,
                    type = "EXPENSE",
                    category = "Entertainment",
                    timestamp = now - 8 * dayMillis,
                    paymentMethod = "Credit Card",
                    notes = "Monthly recurring streaming"
                ),
                TransactionEntity(
                    title = "S&P 500 ETF Index SIP",
                    amount = 500.00,
                    type = "INVESTMENT",
                    category = "Mutual Funds / SIP",
                    timestamp = now - 3 * dayMillis,
                    paymentMethod = "Bank Transfer",
                    notes = "Monthly automated dollar-cost averaging"
                )
            )
            database.transactionDao().insertAll(sampleTransactions)

            // 2. Initial Sample Budgets
            val sampleBudgets = listOf(
                BudgetEntity(category = "TOTAL", monthlyLimit = 2200.00, monthYear = currentMonth, alertThresholdPercent = 80),
                BudgetEntity(category = "Food & Dining", monthlyLimit = 400.00, monthYear = currentMonth, alertThresholdPercent = 80),
                BudgetEntity(category = "Groceries", monthlyLimit = 500.00, monthYear = currentMonth, alertThresholdPercent = 80),
                BudgetEntity(category = "Shopping", monthlyLimit = 350.00, monthYear = currentMonth, alertThresholdPercent = 80),
                BudgetEntity(category = "Bills & Utilities", monthlyLimit = 250.00, monthYear = currentMonth, alertThresholdPercent = 80),
                BudgetEntity(category = "Transport & Fuel", monthlyLimit = 200.00, monthYear = currentMonth, alertThresholdPercent = 80),
                BudgetEntity(category = "Entertainment", monthlyLimit = 150.00, monthYear = currentMonth, alertThresholdPercent = 80)
            )
            database.budgetDao().insertAll(sampleBudgets)

            // 3. Initial Sample Investments for Net Worth
            val sampleInvestments = listOf(
                InvestmentEntity(
                    name = "Vanguard S&P 500 ETF (VOO)",
                    assetType = "MUTUAL_FUNDS",
                    investedAmount = 12500.00,
                    currentValue = 15820.00,
                    quantity = 32.5,
                    notes = "Long-term index fund"
                ),
                InvestmentEntity(
                    name = "Apple Inc. (AAPL)",
                    assetType = "STOCKS",
                    investedAmount = 4200.00,
                    currentValue = 5650.00,
                    quantity = 24.0,
                    notes = "Tech blue-chip holding"
                ),
                InvestmentEntity(
                    name = "Bitcoin (BTC)",
                    assetType = "CRYPTO",
                    investedAmount = 3000.00,
                    currentValue = 4850.00,
                    quantity = 0.075,
                    notes = "Hardware wallet cold storage"
                ),
                InvestmentEntity(
                    name = "Physical Gold Sovereign",
                    assetType = "GOLD",
                    investedAmount = 2400.00,
                    currentValue = 2920.00,
                    quantity = 1.0,
                    notes = "Inflation hedge"
                ),
                InvestmentEntity(
                    name = "High Yield Emergency Fund",
                    assetType = "SAVINGS_BONDS",
                    investedAmount = 10000.00,
                    currentValue = 10450.00,
                    quantity = 1.0,
                    notes = "4.5% APY liquid savings"
                )
            )
            database.investmentDao().insertAll(sampleInvestments)

            // 4. Initial Sync Log
            database.smsSyncLogDao().insert(
                SmsSyncLogEntity(
                    messagesScanned = 14,
                    transactionsFound = 3,
                    status = "SUCCESS",
                    summary = "Scheduler configured: Runs daily at 11:59 PM with 6:00 AM fallback."
                )
            )
        }
    }
}
