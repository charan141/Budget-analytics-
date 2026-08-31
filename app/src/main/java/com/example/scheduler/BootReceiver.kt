package com.example.scheduler

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.data.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED) {

            // Reschedule alarms immediately upon boot
            SmsScanScheduler.scheduleDailyAlarms(context)

            // Check if phone was off during 11:59 PM scan
            val cal = Calendar.getInstance()
            val hour = cal.get(Calendar.HOUR_OF_DAY)

            if (SmsScanScheduler.wasNightScanMissedForYesterday(context)) {
                // If boot happened between 6:00 AM and 11:58 PM, run the missed scan immediately!
                if (hour >= 6) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val database = AppDatabase.getDatabase(context)
                            DailySmsAlarmReceiver.performSmsScan(
                                context = context,
                                database = database,
                                triggerType = "Reboot Catch-Up Scan (Device was off at 11:59 PM, caught up post-boot)"
                            )
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }
        }
    }
}
