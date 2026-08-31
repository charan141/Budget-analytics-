package com.example.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.InvestmentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InvestmentDao {
    @Query("SELECT * FROM investments ORDER BY currentValue DESC")
    fun getAllInvestments(): Flow<List<InvestmentEntity>>

    @Query("SELECT * FROM investments WHERE id = :id")
    suspend fun getInvestmentById(id: Long): InvestmentEntity?

    @Query("SELECT SUM(investedAmount) FROM investments")
    fun getTotalInvestedAmount(): Flow<Double?>

    @Query("SELECT SUM(currentValue) FROM investments")
    fun getTotalCurrentValue(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(investment: InvestmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(investments: List<InvestmentEntity>)

    @Update
    suspend fun update(investment: InvestmentEntity)

    @Delete
    suspend fun delete(investment: InvestmentEntity)

    @Query("DELETE FROM investments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
