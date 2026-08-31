package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "investments")
data class InvestmentEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val assetType: String, // STOCKS, MUTUAL_FUNDS, CRYPTO, REAL_ESTATE, GOLD, SAVINGS_BONDS
    val investedAmount: Double,
    val currentValue: Double,
    val quantity: Double = 1.0,
    val purchaseDate: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val gainLoss: Double
        get() = currentValue - investedAmount

    val returnPercentage: Double
        get() = if (investedAmount > 0) ((currentValue - investedAmount) / investedAmount) * 100.0 else 0.0
}
