package com.example.sms

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Telephony
import androidx.core.content.ContextCompat
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsReader {

    fun hasSmsPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    suspend fun readAndParseRecentSms(
        context: Context,
        sinceTimestamp: Long = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L) // default last 7 days
    ): List<TransactionEntity> = withContext(Dispatchers.IO) {
        if (!hasSmsPermission(context)) {
            return@withContext emptyList()
        }

        val transactions = mutableListOf<TransactionEntity>()
        val uri: Uri = Telephony.Sms.Inbox.CONTENT_URI
        val projection = arrayOf(
            Telephony.Sms._ID,
            Telephony.Sms.ADDRESS,
            Telephony.Sms.BODY,
            Telephony.Sms.DATE
        )
        val selection = "${Telephony.Sms.DATE} >= ?"
        val selectionArgs = arrayOf(sinceTimestamp.toString())
        val sortOrder = "${Telephony.Sms.DATE} DESC"

        try {
            val cursor = context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )

            cursor?.use {
                val idCol = it.getColumnIndexOrThrow(Telephony.Sms._ID)
                val addressCol = it.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                val bodyCol = it.getColumnIndexOrThrow(Telephony.Sms.BODY)
                val dateCol = it.getColumnIndexOrThrow(Telephony.Sms.DATE)

                while (it.moveToNext()) {
                    val msgId = it.getString(idCol)
                    val address = it.getString(addressCol) ?: ""
                    val body = it.getString(bodyCol) ?: ""
                    val date = it.getLong(dateCol)

                    val parsed = SmsParser.parse(
                        smsBody = body,
                        smsDate = date,
                        smsAddress = address,
                        messageId = "sms_$msgId"
                    )

                    if (parsed != null) {
                        transactions.add(
                            TransactionEntity(
                                title = parsed.title,
                                amount = parsed.amount,
                                type = parsed.type,
                                category = parsed.category,
                                timestamp = parsed.timestamp,
                                paymentMethod = parsed.paymentMethod,
                                notes = "Auto-synced from SMS (${address.takeLast(8)})",
                                smsId = parsed.smsId,
                                isSmsAutoDetected = true,
                                accountNumberLast4 = parsed.accountNumberLast4
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        transactions
    }
}
