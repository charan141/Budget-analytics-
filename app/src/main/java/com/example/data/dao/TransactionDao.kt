package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id")
    suspend fun getTransactionById(id: Long): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime ORDER BY timestamp DESC")
    fun getTransactionsInRange(startTime: Long, endTime: Long): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getTransactionsInRangeSync(startTime: Long, endTime: Long): List<TransactionEntity>

    @Query("SELECT * FROM transactions WHERE type = :type ORDER BY timestamp DESC")
    fun getTransactionsByType(type: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE category = :category ORDER BY timestamp DESC")
    fun getTransactionsByCategory(category: String): Flow<List<TransactionEntity>>

    @Query("SELECT COUNT(*) FROM transactions WHERE smsId = :smsId")
    suspend fun countBySmsId(smsId: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(transactions: List<TransactionEntity>): List<Long>

    @Update
    suspend fun update(transaction: TransactionEntity)

    @Delete
    suspend fun delete(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'EXPENSE' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalExpenseInRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INCOME' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalIncomeInRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE type = 'INVESTMENT' AND timestamp >= :startTime AND timestamp <= :endTime")
    fun getTotalInvestmentInRange(startTime: Long, endTime: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM transactions WHERE category = :category AND type = 'EXPENSE' AND timestamp >= :startTime AND timestamp <= :endTime")
    suspend fun getCategoryExpenseSum(category: String, startTime: Long, endTime: Long): Double?
}
