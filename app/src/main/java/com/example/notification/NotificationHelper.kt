package com.example.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.AppDatabase
import com.example.data.model.BudgetEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object NotificationHelper {
    const val CHANNEL_BUDGET = "budget_alerts_channel"
    const val CHANNEL_SMS_SYNC = "sms_sync_channel"

    fun createNotificationChannels(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val budgetChannel = NotificationChannel(
                CHANNEL_BUDGET,
                "Budget & Spending Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies when approaching or exceeding monthly spending budgets."
                enableVibration(true)
            }

            val smsSyncChannel = NotificationChannel(
                CHANNEL_SMS_SYNC,
                "SMS Automated Sync",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Updates on automated 11:59 PM and 6:00 AM SMS transaction scans."
            }

            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(budgetChannel)
            manager.createNotificationChannel(smsSyncChannel)
        }
    }

    suspend fun checkAndTriggerBudgetAlerts(
        context: Context,
        database: AppDatabase,
        monthYear: String? = null
    ) = withContext(Dispatchers.IO) {
        val targetMonth = monthYear ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val budgets = database.budgetDao().getBudgetsForMonthSync(targetMonth)
        if (budgets.isEmpty()) return@withContext

        // Get time range for the target month
        val cal = Calendar.getInstance()
        val parsed = SimpleDateFormat("yyyy-MM", Locale.getDefault()).parse(targetMonth) ?: Date()
        cal.time = parsed
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val startTime = cal.timeInMillis

        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endTime = cal.timeInMillis

        val transactions = database.transactionDao().getTransactionsInRangeSync(startTime, endTime)
        val expenseTransactions = transactions.filter { it.type == "EXPENSE" }
        val totalSpent = expenseTransactions.sumOf { it.amount }

        for (budget in budgets) {
            val spentForCategory = if (budget.category == "TOTAL") {
                totalSpent
            } else {
                expenseTransactions.filter { it.category.equals(budget.category, ignoreCase = true) }
                    .sumOf { it.amount }
            }

            if (budget.monthlyLimit <= 0) continue

            val percentage = (spentForCategory / budget.monthlyLimit) * 100.0

            // Determine notification tier
            val notificationTier = when {
                percentage >= 100.0 -> 100
                percentage >= 90.0 -> 90
                percentage >= budget.alertThresholdPercent.toDouble() -> budget.alertThresholdPercent
                else -> 0
            }

            if (notificationTier > 0 && notificationTier > budget.lastNotifiedPercentage) {
                // Send Notification
                sendBudgetNotification(
                    context = context,
                    budget = budget,
                    spent = spentForCategory,
                    percentage = percentage,
                    tier = notificationTier
                )

                // Update last notified percentage in DB
                database.budgetDao().update(
                    budget.copy(lastNotifiedPercentage = notificationTier)
                )
            }
        }
    }

    fun sendBudgetNotification(
        context: Context,
        budget: BudgetEntity,
        spent: Double,
        percentage: Double,
        tier: Int
    ) {
        val title = when {
            percentage >= 100.0 -> "🚨 Budget Exceeded: ${budget.category}"
            percentage >= 90.0 -> "⚠️ Critical Budget Alert: ${budget.category}"
            else -> "📊 Budget Warning: ${budget.category}"
        }

        val formattedSpent = String.format(Locale.getDefault(), "$%.2f", spent)
        val formattedLimit = String.format(Locale.getDefault(), "$%.2f", budget.monthlyLimit)
        val formattedPct = String.format(Locale.getDefault(), "%.1f%%", percentage)

        val message = when {
            percentage >= 100.0 -> "You have exceeded your ${budget.category} budget! Spent $formattedSpent of $formattedLimit ($formattedPct)."
            else -> "You have reached $formattedPct of your ${budget.category} monthly budget ($formattedSpent / $formattedLimit)."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            budget.id.toInt() + 1000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_BUDGET)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(budget.id.toInt() + 2000, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun sendSmsSyncNotification(
        context: Context,
        newCount: Int,
        totalScanned: Int,
        totalAmount: Double,
        scheduledNote: String = "Nightly 11:59 PM Scan"
    ) {
        val title = if (newCount > 0) "💳 SMS Sync: $newCount New Transactions" else "💳 SMS Sync Complete"
        val message = if (newCount > 0) {
            "Added $newCount new transactions totaling $${String.format(Locale.getDefault(), "%.2f", totalAmount)} via $scheduledNote."
        } else {
            "Scanned $totalScanned SMS messages. No new financial transactions found."
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            3001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SMS_SYNC)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val manager = NotificationManagerCompat.from(context)
            manager.notify(4001, builder.build())
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
