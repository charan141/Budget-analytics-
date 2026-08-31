package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
    indices = [
        Index(value = ["smsId"], unique = true),
        Index(value = ["timestamp"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val amount: Double,
    val type: String, // EXPENSE, INCOME, INVESTMENT
    val category: String,
    val timestamp: Long = System.currentTimeMillis(),
    val paymentMethod: String = "Bank", // UPI, Card, Bank, Cash, etc.
    val notes: String = "",
    val smsId: String? = null, // To identify SMS duplicates
    val isSmsAutoDetected: Boolean = false,
    val accountNumberLast4: String? = null
)
