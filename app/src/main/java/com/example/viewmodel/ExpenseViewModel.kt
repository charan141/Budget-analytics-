package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.data.model.AppCurrency
import com.example.data.model.BudgetEntity
import com.example.data.model.InvestmentEntity
import com.example.data.model.SmsSyncLogEntity
import com.example.data.model.TransactionEntity
import com.example.notification.NotificationHelper
import com.example.scheduler.DailySmsAlarmReceiver
import com.example.scheduler.SmsScanScheduler
import com.example.sms.ParsedSmsResult
import com.example.sms.SmsParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val count: Int
)

data class DaySpending(
    val dayOfMonth: Int,
    val dateLabel: String,
    val amount: Double
)

data class MonthSpendingTrend(
    val monthYear: String,
    val monthLabel: String,
    val totalExpense: Double,
    val totalIncome: Double
)

data class BudgetProgress(
    val budget: BudgetEntity,
    val spentAmount: Double,
    val remainingAmount: Double,
    val percentage: Double,
    val isOverBudget: Boolean
)

data class NetWorthSummary(
    val totalNetWorth: Double,
    val liquidCashBalance: Double,
    val totalInvestmentsValue: Double,
    val totalInvestedPrincipal: Double,
    val totalInvestmentGainLoss: Double,
    val investmentReturnPercentage: Double,
    val monthlyIncome: Double,
    val monthlyExpenses: Double,
    val monthlyInvestments: Double,
    val monthlySavingsRate: Double
)

class ExpenseViewModel(
    application: Application,
    private val repository: ExpenseRepository,
    private val database: AppDatabase
) : AndroidViewModel(application) {

    private val prefs = application.getSharedPreferences("expense_tracker_prefs", Context.MODE_PRIVATE)

    // Selected Currency Flow
    private val _selectedCurrency = MutableStateFlow(
        AppCurrency.fromCode(prefs.getString("selected_currency_code", "USD") ?: "USD")
    )
    val selectedCurrency: StateFlow<AppCurrency> = _selectedCurrency

    fun setCurrency(currency: AppCurrency) {
        _selectedCurrency.value = currency
        prefs.edit().putString("selected_currency_code", currency.code).apply()
    }

    // Current Selected Month (Format: "yyyy-MM", e.g. "2026-08")
    private val _selectedMonthYear = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )
    val selectedMonthYear: StateFlow<String> = _selectedMonthYear

    // Filters
    val searchQuery = MutableStateFlow("")
    val selectedCategoryFilter = MutableStateFlow("ALL")
    val selectedTypeFilter = MutableStateFlow("ALL") // ALL, EXPENSE, INCOME, INVESTMENT

    // UI States
    val isSyncing = MutableStateFlow(false)
    val syncMessage = MutableStateFlow<String?>(null)

    // All Transactions Flow
    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Filtered Transactions Flow
    val filteredTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        selectedMonthYear,
        searchQuery,
        selectedCategoryFilter,
        selectedTypeFilter
    ) { txs, monthYear, query, category, type ->
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        txs.filter { tx ->
            val txMonth = monthFormat.format(Date(tx.timestamp))
            val matchesMonth = txMonth == monthYear
            val matchesQuery = query.isBlank() || tx.title.contains(query, ignoreCase = true) ||
                    tx.notes.contains(query, ignoreCase = true) ||
                    tx.paymentMethod.contains(query, ignoreCase = true) ||
                    (tx.accountNumberLast4?.contains(query) == true)
            val matchesCategory = category == "ALL" || tx.category.equals(category, ignoreCase = true)
            val matchesType = type == "ALL" || tx.type.equals(type, ignoreCase = true)

            matchesMonth && matchesQuery && matchesCategory && matchesType
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month's Transactions Flow
    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = combine(
        allTransactions,
        selectedMonthYear
    ) { txs, monthYear ->
        val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        txs.filter { monthFormat.format(Date(it.timestamp)) == monthYear }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budgets Flow
    val budgets: StateFlow<List<BudgetEntity>> = combine(
        _selectedMonthYear
    ) { month ->
        month[0]
    }.combine(repository.allTransactions) { monthYear, _ ->
        repository.getBudgetsForMonthSync(monthYear)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Budget Progress List
    val budgetProgressList: StateFlow<List<BudgetProgress>> = combine(
        currentMonthTransactions,
        _selectedMonthYear
    ) { txs, monthYear ->
        val expenseTxs = txs.filter { it.type == "EXPENSE" }
        val budgetsList = repository.getBudgetsForMonthSync(monthYear)
        val totalSpent = expenseTxs.sumOf { it.amount }

        budgetsList.map { budget ->
            val spent = if (budget.category == "TOTAL") {
                totalSpent
            } else {
                expenseTxs.filter { it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
            }
            val remaining = (budget.monthlyLimit - spent).coerceAtLeast(0.0)
            val pct = if (budget.monthlyLimit > 0) (spent / budget.monthlyLimit) * 100.0 else 0.0
            BudgetProgress(
                budget = budget,
                spentAmount = spent,
                remainingAmount = remaining,
                percentage = pct,
                isOverBudget = spent > budget.monthlyLimit
            )
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Category Breakdown (For Donut / Pie Chart)
    val categorySpendingBreakdown: StateFlow<List<CategorySpending>> = currentMonthTransactions
        .map { txs ->
            val expenseTxs = txs.filter { it.type == "EXPENSE" }
            val totalExpense = expenseTxs.sumOf { it.amount }
            if (totalExpense <= 0.0) return@map emptyList<CategorySpending>()

            val grouped = expenseTxs.groupBy { it.category }
            grouped.map { entry ->
                val cat = entry.key
                val list = entry.value
                val sum = list.sumOf { it.amount }
                CategorySpending(
                    category = cat,
                    amount = sum,
                    percentage = (sum / totalExpense) * 100.0,
                    count = list.size
                )
            }.sortedByDescending { it.amount }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    // Daily Spending Trend for Selected Month (For Bar / Line Chart)
    val dailySpendingTrend: StateFlow<List<DaySpending>> = combine(
        currentMonthTransactions,
        selectedMonthYear
    ) { txs, monthYear ->
        val cal = Calendar.getInstance()
        val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(monthYear) ?: Date()
        cal.time = parsed
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dayExpenses = mutableMapOf<Int, Double>()
        for (day in 1..maxDays) {
            dayExpenses[day] = 0.0
        }

        val expenseTxs = txs.filter { it.type == "EXPENSE" }
        val dayFormat = SimpleDateFormat("d", Locale.getDefault())

        for (tx in expenseTxs) {
            val day = dayFormat.format(Date(tx.timestamp)).toIntOrNull() ?: 1
            dayExpenses[day] = (dayExpenses[day] ?: 0.0) + tx.amount
        }

        dayExpenses.map { (day, amount) ->
            DaySpending(
                dayOfMonth = day,
                dateLabel = "$day",
                amount = amount
            )
        }.sortedBy { it.dayOfMonth }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Multi-Month Trends (Last 6 months comparison)
    val monthlyHistoryTrends: StateFlow<List<MonthSpendingTrend>> = allTransactions
        .map { txs ->
            val result = mutableListOf<MonthSpendingTrend>()

            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val labelFormat = SimpleDateFormat("MMM", Locale.getDefault())

            for (i in 5 downTo 0) {
                val iterCal = Calendar.getInstance().apply {
                    time = Date()
                    add(Calendar.MONTH, -i)
                }
                val mYear = monthFormat.format(iterCal.time)
                val label = labelFormat.format(iterCal.time)

                val monthTxs = txs.filter { monthFormat.format(Date(it.timestamp)) == mYear }
                val expense = monthTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val income = monthTxs.filter { it.type == "INCOME" }.sumOf { it.amount }

                result.add(
                    MonthSpendingTrend(
                        monthYear = mYear,
                        monthLabel = label,
                        totalExpense = expense,
                        totalIncome = income
                    )
                )
            }
            result
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())



    // Investments Flow
    val allInvestments: StateFlow<List<InvestmentEntity>> = repository.allInvestments
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Net Worth & Financial Health Summary
    val netWorthSummary: StateFlow<NetWorthSummary> = combine(
        allTransactions,
        currentMonthTransactions,
        allInvestments
    ) { allTxs, currentTxs, investments ->
        val totalAllTimeIncome = allTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val totalAllTimeExpense = allTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val totalAllTimeInvestmentTransfers = allTxs.filter { it.type == "INVESTMENT" }.sumOf { it.amount }

        val liquidCash = (totalAllTimeIncome - totalAllTimeExpense - totalAllTimeInvestmentTransfers).coerceAtLeast(0.0)

        val totalInvestedPrincipal = investments.sumOf { it.investedAmount }
        val totalCurrentValuation = investments.sumOf { it.currentValue }
        val gainLoss = totalCurrentValuation - totalInvestedPrincipal
        val roiPct = if (totalInvestedPrincipal > 0) (gainLoss / totalInvestedPrincipal) * 100.0 else 0.0

        val totalNetWorth = liquidCash + totalCurrentValuation

        val monthlyInc = currentTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
        val monthlyExp = currentTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
        val monthlyInv = currentTxs.filter { it.type == "INVESTMENT" }.sumOf { it.amount }
        val savingsRate = if (monthlyInc > 0) ((monthlyInc - monthlyExp) / monthlyInc) * 100.0 else 0.0

        NetWorthSummary(
            totalNetWorth = totalNetWorth,
            liquidCashBalance = liquidCash,
            totalInvestmentsValue = totalCurrentValuation,
            totalInvestedPrincipal = totalInvestedPrincipal,
            totalInvestmentGainLoss = gainLoss,
            investmentReturnPercentage = roiPct,
            monthlyIncome = monthlyInc,
            monthlyExpenses = monthlyExp,
            monthlyInvestments = monthlyInv,
            monthlySavingsRate = savingsRate
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        NetWorthSummary(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
    )

    // SMS Sync Logs
    val syncLogs: StateFlow<List<SmsSyncLogEntity>> = repository.syncLogs
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Selector Actions
    fun setSelectedMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    fun previousMonth() {
        val cal = Calendar.getInstance()
        val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(_selectedMonthYear.value) ?: Date()
        cal.time = parsed
        cal.add(Calendar.MONTH, -1)
        _selectedMonthYear.value = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    fun nextMonth() {
        val cal = Calendar.getInstance()
        val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(_selectedMonthYear.value) ?: Date()
        cal.time = parsed
        cal.add(Calendar.MONTH, 1)
        _selectedMonthYear.value = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(cal.time)
    }

    // Transaction CRUD
    fun addTransaction(
        title: String,
        amount: Double,
        type: String,
        category: String,
        paymentMethod: String,
        notes: String = "",
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val entity = TransactionEntity(
                title = title.trim(),
                amount = amount,
                type = type,
                category = category,
                paymentMethod = paymentMethod,
                notes = notes.trim(),
                timestamp = timestamp
            )
            repository.insertTransaction(entity)

            // Check budget notifications
            NotificationHelper.checkAndTriggerBudgetAlerts(getApplication(), database, _selectedMonthYear.value)
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
            NotificationHelper.checkAndTriggerBudgetAlerts(getApplication(), database, _selectedMonthYear.value)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    // Budget CRUD
    fun saveBudget(category: String, monthlyLimit: Double, alertThresholdPercent: Int = 80) {
        viewModelScope.launch {
            val currentMonth = _selectedMonthYear.value
            val existing = database.budgetDao().getBudget(category, currentMonth)
            val budget = existing?.copy(
                monthlyLimit = monthlyLimit,
                alertThresholdPercent = alertThresholdPercent,
                lastNotifiedPercentage = 0 // reset alert tier
            ) ?: BudgetEntity(
                category = category,
                monthlyLimit = monthlyLimit,
                monthYear = currentMonth,
                alertThresholdPercent = alertThresholdPercent,
                lastNotifiedPercentage = 0
            )
            repository.insertOrUpdateBudget(budget)
            NotificationHelper.checkAndTriggerBudgetAlerts(getApplication(), database, currentMonth)
        }
    }

    fun deleteBudget(budget: BudgetEntity) {
        viewModelScope.launch {
            repository.deleteBudget(budget)
        }
    }

    // Investment CRUD
    fun saveInvestment(
        id: Long = 0,
        name: String,
        assetType: String,
        investedAmount: Double,
        currentValue: Double,
        quantity: Double = 1.0,
        notes: String = ""
    ) {
        viewModelScope.launch {
            val entity = InvestmentEntity(
                id = id,
                name = name.trim(),
                assetType = assetType,
                investedAmount = investedAmount,
                currentValue = currentValue,
                quantity = quantity,
                notes = notes.trim()
            )
            if (id == 0L) {
                repository.insertInvestment(entity)
            } else {
                repository.updateInvestment(entity)
            }
        }
    }

    fun deleteInvestment(investment: InvestmentEntity) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    // SMS Trigger & Simulation
    fun triggerManualSmsScan() {
        viewModelScope.launch {
            isSyncing.value = true
            syncMessage.value = "Scanning SMS message box for financial transactions..."
            val (count, totalAmount) = DailySmsAlarmReceiver.performSmsScan(
                context = getApplication(),
                database = database,
                triggerType = "Manual On-Demand Scan"
            )
            isSyncing.value = false
            syncMessage.value = if (count > 0) {
                "Successfully added $count new transactions totaling $${String.format(Locale.getDefault(), "%.2f", totalAmount)}!"
            } else {
                "Scan complete. All transactions are already up to date."
            }
        }
    }

    fun parseAndSimulateSms(smsBody: String, sender: String = "SIMULATOR"): ParsedSmsResult? {
        val now = System.currentTimeMillis()
        val parsed = SmsParser.parse(
            smsBody = smsBody,
            smsDate = now,
            smsAddress = sender,
            messageId = "sim_${now}_${smsBody.hashCode()}"
        )
        if (parsed != null) {
            viewModelScope.launch {
                val entity = TransactionEntity(
                    title = parsed.title,
                    amount = parsed.amount,
                    type = parsed.type,
                    category = parsed.category,
                    timestamp = parsed.timestamp,
                    paymentMethod = parsed.paymentMethod,
                    notes = "Simulated SMS Parser test (${parsed.rawBody.take(40)}...)",
                    smsId = parsed.smsId,
                    isSmsAutoDetected = true,
                    accountNumberLast4 = parsed.accountNumberLast4
                )
                repository.insertTransaction(entity)
                NotificationHelper.checkAndTriggerBudgetAlerts(getApplication(), database, _selectedMonthYear.value)
            }
        }
        return parsed
    }

    fun testBudgetAlertNotification(category: String = "Food & Dining", spent: Double = 510.0, limit: Double = 600.0) {
        val pct = (spent / limit) * 100.0
        val sampleBudget = BudgetEntity(
            category = category,
            monthlyLimit = limit,
            monthYear = _selectedMonthYear.value
        )
        NotificationHelper.sendBudgetNotification(
            context = getApplication(),
            budget = sampleBudget,
            spent = spent,
            percentage = pct,
            tier = pct.toInt()
        )
    }

    fun clearSyncHistory() {
        viewModelScope.launch {
            repository.clearSyncLogs()
        }
    }
}
