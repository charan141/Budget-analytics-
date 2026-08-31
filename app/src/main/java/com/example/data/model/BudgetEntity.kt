package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "budgets",
    indices = [
        Index(value = ["category", "monthYear"], unique = true)
    ]
)
data class BudgetEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val category: String, // Specific category name or "TOTAL" for overall budget
    val monthlyLimit: Double,
    val monthYear: String, // Format: "yyyy-MM" e.g., "2026-08"
    val alertThresholdPercent: Int = 80, // Trigger warning at 80%
    val lastNotifiedPercentage: Int = 0 // 0, 80, 90, 100 to avoid spamming
)
