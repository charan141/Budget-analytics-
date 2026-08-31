package com.example.scheduler

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

object SmsScanScheduler {
    const val ACTION_SMS_SCAN_NIGHT = "com.example.expensetracker.ACTION_SMS_SCAN_NIGHT"
    const val ACTION_SMS_SCAN_MORNING = "com.example.expensetracker.ACTION_SMS_SCAN_MORNING"

    private const val PREFS_NAME = "sms_scheduler_prefs"
    private const val KEY_LAST_NIGHT_SCAN_DATE = "last_night_scan_date"
    private const val KEY_LAST_SCAN_TIMESTAMP = "last_scan_timestamp"

    fun scheduleDailyAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        // 1. Schedule 11:59 PM (23:59:00) Nightly Scan
        val nightCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val nightIntent = Intent(context, DailySmsAlarmReceiver::class.java).apply {
            action = ACTION_SMS_SCAN_NIGHT
        }
        val nightPendingIntent = PendingIntent.getBroadcast(
            context,
            1159,
            nightIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, nightCalendar.timeInMillis, nightPendingIntent)

        // 2. Schedule 6:00 AM (06:00:00) Morning Fallback Scan
        val morningCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        val morningIntent = Intent(context, DailySmsAlarmReceiver::class.java).apply {
            action = ACTION_SMS_SCAN_MORNING
        }
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            600,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        scheduleExactAlarm(alarmManager, morningCalendar.timeInMillis, morningPendingIntent)

        // 3. Register WorkManager Periodic Backup Worker
        scheduleWorkManagerFallback(context)
    }

    private fun scheduleExactAlarm(alarmManager: AlarmManager, triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            // If exact alarm permission is restricted on Android 12+, use setAndAllowWhileIdle
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
        }
    }

    private fun scheduleWorkManagerFallback(context: Context) {
        try {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val syncWorkRequest = PeriodicWorkRequestBuilder<DailySmsSyncWorker>(
                12, TimeUnit.HOURS, // Runs twice a day as reliable safety net
                1, TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "DailySmsSyncWorkerPeriodic",
                ExistingPeriodicWorkPolicy.KEEP,
                syncWorkRequest
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun recordNightScanSuccess(context: Context) {
        val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString(KEY_LAST_NIGHT_SCAN_DATE, todayStr)
            .putLong(KEY_LAST_SCAN_TIMESTAMP, System.currentTimeMillis())
            .apply()
    }

    fun wasNightScanMissedForYesterday(context: Context): Boolean {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        val yesterdayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(cal.time)

        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastDate = prefs.getString(KEY_LAST_NIGHT_SCAN_DATE, "") ?: ""
        return lastDate != yesterdayStr && lastDate != SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    }

    fun getLastScanTimestamp(context: Context): Long {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.getLong(KEY_LAST_SCAN_TIMESTAMP, 0L)
    }

    fun getNextScheduledInfo(): Pair<String, String> {
        val cal = Calendar.getInstance()
        val now = cal.timeInMillis

        val nightCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }

        val morningCal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= now) add(Calendar.DAY_OF_YEAR, 1)
        }

        val df = SimpleDateFormat("hh:mm a, MMM dd", Locale.getDefault())
        return Pair(df.format(nightCal.time), df.format(morningCal.time))
    }
}
