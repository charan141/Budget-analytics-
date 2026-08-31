package com.example

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.ExpenseRepository
import com.example.notification.NotificationHelper
import com.example.scheduler.SmsScanScheduler

class ExpenseApplication : Application() {

    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy {
        ExpenseRepository(
            transactionDao = database.transactionDao(),
            budgetDao = database.budgetDao(),
            investmentDao = database.investmentDao(),
            smsSyncLogDao = database.smsSyncLogDao()
        )
    }

    override fun onCreate() {
        super.onCreate()

        // Create notification channels for budget alerts and SMS updates
        NotificationHelper.createNotificationChannels(this)

        // Initialize 11:59 PM and 6:00 AM fallback daily SMS scan alarms
        SmsScanScheduler.scheduleDailyAlarms(this)
    }
}
