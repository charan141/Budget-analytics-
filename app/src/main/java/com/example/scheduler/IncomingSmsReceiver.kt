package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.example.data.AppDatabase
import com.example.data.model.TransactionEntity
import com.example.notification.NotificationHelper
import com.example.sms.SmsParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class IncomingSmsReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isNullOrEmpty()) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = AppDatabase.getDatabase(context)

                for (sms in messages) {
                    val body = sms.messageBody ?: continue
                    val sender = sms.originatingAddress ?: "SMS"
                    val timestamp = sms.timestampMillis

                    val parsed = SmsParser.parse(
                        smsBody = body,
                        smsDate = timestamp,
                        smsAddress = sender,
                        messageId = "live_${timestamp}_${body.hashCode()}"
                    )

                    if (parsed != null) {
                        val count = database.transactionDao().countBySmsId(parsed.smsId)
                        if (count == 0) {
                            database.transactionDao().insert(
                                TransactionEntity(
                                    title = parsed.title,
                                    amount = parsed.amount,
                                    type = parsed.type,
                                    category = parsed.category,
                                    timestamp = parsed.timestamp,
                                    paymentMethod = parsed.paymentMethod,
                                    notes = "Instant live auto-detection from SMS (${sender.takeLast(8)})",
                                    smsId = parsed.smsId,
                                    isSmsAutoDetected = true,
                                    accountNumberLast4 = parsed.accountNumberLast4
                                )
                            )

                            // Check and trigger budget warnings if applicable
                            NotificationHelper.checkAndTriggerBudgetAlerts(context, database)

                            // Post notification
                            NotificationHelper.sendSmsSyncNotification(
                                context = context,
                                newCount = 1,
                                totalScanned = 1,
                                totalAmount = parsed.amount,
                                scheduledNote = "Live SMS Auto-Detect"
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
