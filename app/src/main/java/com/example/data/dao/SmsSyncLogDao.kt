package com.example.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.SmsSyncLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsSyncLogDao {
    @Query("SELECT * FROM sms_sync_logs ORDER BY timestamp DESC LIMIT 50")
    fun getAllLogs(): Flow<List<SmsSyncLogEntity>>

    @Query("SELECT * FROM sms_sync_logs ORDER BY timestamp DESC LIMIT 1")
    fun getLatestLog(): Flow<SmsSyncLogEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: SmsSyncLogEntity): Long

    @Query("DELETE FROM sms_sync_logs")
    suspend fun clearLogs()
}
