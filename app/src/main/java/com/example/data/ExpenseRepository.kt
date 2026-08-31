package com.example.data

import com.example.data.dao.BudgetDao
import com.example.data.dao.InvestmentDao
import com.example.data.dao.SmsSyncLogDao
import com.example.data.dao.TransactionDao
import com.example.data.model.BudgetEntity
import com.example.data.model.InvestmentEntity
import com.example.data.model.SmsSyncLogEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val transactionDao: TransactionDao,
    private val budgetDao: BudgetDao,
    private val investmentDao: InvestmentDao,
    private val smsSyncLogDao: SmsSyncLogDao
) {
    // Transactions
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>> {
        return transactionDao.getTransactionsInRange(startTime, endTime)
    }

    suspend fun getTransactionsInRangeSync(startTime: Long, endTime: Long): List<TransactionEntity> {
        return transactionDao.getTransactionsInRangeSync(startTime, endTime)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        return transactionDao.insert(transaction)
    }

    suspend fun insertTransactions(transactions: List<TransactionEntity>): List<Long> {
        return transactionDao.insertAll(transactions)
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        transactionDao.update(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.delete(transaction)
    }

    suspend fun deleteTransactionById(id: Long) {
        transactionDao.deleteById(id)
    }

    suspend fun countBySmsId(smsId: String): Int {
        return transactionDao.countBySmsId(smsId)
    }

    // Budgets
    fun getBudgetsForMonth(monthYear: String): Flow<List<BudgetEntity>> {
        return budgetDao.getBudgetsForMonth(monthYear)
    }

    suspend fun getBudgetsForMonthSync(monthYear: String): List<BudgetEntity> {
        return budgetDao.getBudgetsForMonthSync(monthYear)
    }

    suspend fun insertOrUpdateBudget(budget: BudgetEntity): Long {
        return budgetDao.insertOrUpdate(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity) {
        budgetDao.delete(budget)
    }

    suspend fun deleteBudgetById(id: Long) {
        budgetDao.deleteById(id)
    }

    // Investments
    val allInvestments: Flow<List<InvestmentEntity>> = investmentDao.getAllInvestments()
    val totalInvestedAmount: Flow<Double?> = investmentDao.getTotalInvestedAmount()
    val totalCurrentValue: Flow<Double?> = investmentDao.getTotalCurrentValue()

    suspend fun insertInvestment(investment: InvestmentEntity): Long {
        return investmentDao.insert(investment)
    }

    suspend fun updateInvestment(investment: InvestmentEntity) {
        investmentDao.update(investment)
    }

    suspend fun deleteInvestment(investment: InvestmentEntity) {
        investmentDao.delete(investment)
    }

    suspend fun deleteInvestmentById(id: Long) {
        investmentDao.deleteById(id)
    }

    // SMS Sync Logs
    val syncLogs: Flow<List<SmsSyncLogEntity>> = smsSyncLogDao.getAllLogs()
    val latestSyncLog: Flow<SmsSyncLogEntity?> = smsSyncLogDao.getLatestLog()

    suspend fun insertSyncLog(log: SmsSyncLogEntity): Long {
        return smsSyncLogDao.insert(log)
    }

    suspend fun clearSyncLogs() {
        smsSyncLogDao.clearLogs()
    }
}
