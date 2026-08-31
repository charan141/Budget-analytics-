package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sms_sync_logs")
data class SmsSyncLogEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val messagesScanned: Int,
    val transactionsFound: Int,
    val status: String, // "SUCCESS", "NO_NEW_DATA", "FAILED", "MISSED_TRIGGERED_MORNING"
    val summary: String
)
