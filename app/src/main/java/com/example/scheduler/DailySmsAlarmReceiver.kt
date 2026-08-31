package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import com.example.data.model.SmsSyncLogEntity
import com.example.notification.NotificationHelper
import com.example.sms.SmsReader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class DailySmsAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)

                when (action) {
                    SmsScanScheduler.ACTION_SMS_SCAN_NIGHT -> {
                        // Regular 11:59 PM Scan
                        performSmsScan(
                            context = context,
                            database = database,
                            triggerType = "11:59 PM Nightly Scan"
                        )
                        SmsScanScheduler.recordNightScanSuccess(context)
                    }
                    SmsScanScheduler.ACTION_SMS_SCAN_MORNING -> {
                        // 6:00 AM Fallback Scan
                        if (SmsScanScheduler.wasNightScanMissedForYesterday(context)) {
                            performSmsScan(
                                context = context,
                                database = database,
                                triggerType = "6:00 AM Fallback Scan (Phone was off at 11:59 PM)"
                            )
                        } else {
                            // Log normal check
                            database.smsSyncLogDao().insert(
                                SmsSyncLogEntity(
                                    messagesScanned = 0,
                                    transactionsFound = 0,
                                    status = "CHECKED_OK",
                                    summary = "6:00 AM Check: 11:59 PM scan completed previously. No backlog."
                                )
                            )
                        }
                    }
                }

                // Reschedule for next cycles
                SmsScanScheduler.scheduleDailyAlarms(context)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        suspend fun performSmsScan(
            context: Context,
            database: AppDatabase,
            triggerType: String
        ): Pair<Int, Double> {
            if (!SmsReader.hasSmsPermission(context)) {
                database.smsSyncLogDao().insert(
                    SmsSyncLogEntity(
                        messagesScanned = 0,
                        transactionsFound = 0,
                        status = "PERMISSION_MISSING",
                        summary = "$triggerType: SMS Read permission not granted by user."
                    )
                )
                return Pair(0, 0.0)
            }

            val sinceTimestamp = System.currentTimeMillis() - (48 * 60 * 60 * 1000L) // last 48h
            val detectedTransactions = SmsReader.readAndParseRecentSms(context, sinceTimestamp)

            var newAddedCount = 0
            var totalAmountAdded = 0.0

            for (tx in detectedTransactions) {
                val smsId = tx.smsId
                if (smsId != null) {
                    val count = database.transactionDao().countBySmsId(smsId)
                    if (count == 0) {
                        database.transactionDao().insert(tx)
                        newAddedCount++
                        if (tx.type == "EXPENSE") {
                            totalAmountAdded += tx.amount
                        }
                    }
                }
            }

            val summary = "$triggerType: Scanned ${detectedTransactions.size} messages, added $newAddedCount new transactions."
            database.smsSyncLogDao().insert(
                SmsSyncLogEntity(
                    messagesScanned = detectedTransactions.size,
                    transactionsFound = newAddedCount,
                    status = if (newAddedCount > 0) "SUCCESS" else "NO_NEW_DATA",
                    summary = summary
                )
            )

            // Trigger Budget notifications if threshold reached
            NotificationHelper.checkAndTriggerBudgetAlerts(context, database)

            // Send notification for sync status
            NotificationHelper.sendSmsSyncNotification(
                context = context,
                newCount = newAddedCount,
                totalScanned = detectedTransactions.size,
                totalAmount = totalAmountAdded,
                scheduledNote = triggerType
            )

            return Pair(newAddedCount, totalAmountAdded)
        }
    }
}
