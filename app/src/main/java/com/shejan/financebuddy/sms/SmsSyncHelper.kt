package com.shejan.financebuddy.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.util.Log
import androidx.core.content.ContextCompat
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SmsSyncHelper {
    private const val TAG = "SmsSyncHelper"

    /**
     * Scans the system SMS inbox for transaction messages from the last 30 days.
     * Filters, parses, and inserts them into the pending database.
     * 
     * @param daysLimit Optional number of days to look back.
     * @param fromTimestampMillis Optional start timestamp in millis (e.g. from custom date picker). Takes priority over daysLimit.
     * @return The number of newly imported transactions.
     */
    suspend fun syncPreviousSms(
        context: Context,
        database: FinanceDatabase,
        daysLimit: Int? = 30,
        fromTimestampMillis: Long? = null
    ): Int = withContext(Dispatchers.IO) {
        if (!isReadSmsPermissionGranted(context)) {
            Log.w(TAG, "READ_SMS permission not granted. Aborting sync.")
            return@withContext 0
        }

        val pendingSmsDao = database.pendingSmsDao()
        val accountDao = database.accountDao()
        val senderMappingDao = database.smsSenderMappingDao()

        val accounts = accountDao.getAllAccountsOnce()
        val mappings = senderMappingDao.getAllMappingsOnce()
        val mappingMap = mappings.associateBy { it.senderAddress.lowercase().trim() }

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        // Build selection dynamically based on fromTimestampMillis or daysLimit
        val minTimestamp: Long? = when {
            fromTimestampMillis != null && fromTimestampMillis > 0 -> fromTimestampMillis
            daysLimit != null && daysLimit > 0 -> System.currentTimeMillis() - (daysLimit.toLong() * 24 * 60 * 60 * 1000)
            else -> null
        }

        val selection: String?
        val selectionArgs: Array<String>?
        if (minTimestamp != null) {
            selection = "date >= ?"
            selectionArgs = arrayOf(minTimestamp.toString())
        } else {
            selection = null
            selectionArgs = null
        }

        var importedCount = 0

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val body = cursor.getString(bodyIdx) ?: continue
                    val smsDate = cursor.getLong(dateIdx)

                    val senderLower = sender.lowercase().trim()
                    val mapping = mappingMap[senderLower]

                    if (mapping != null && mapping.accountId == -1) {
                        // User explicitly unlinked this sender — do not auto-map to any account
                        val parsed = SmsParser.parse(sender, body) ?: continue
                        val existsInPending = pendingSmsDao.isSmsExists(body)
                        if (existsInPending) continue

                        val pending = PendingSmsTransactionEntity(
                            rawSmsBody          = body,
                            senderAddress       = sender,
                            amount              = parsed.amount,
                            type                = parsed.type,
                            category            = parsed.category,
                            note                = parsed.note,
                            detectedAccountName = parsed.detectedAccountName,
                            fromAccountId       = -1, // Unlinked
                            toAccountId         = null,
                            timestamp           = parsed.timestamp ?: smsDate,
                            receivedAt          = smsDate
                        )
                        pendingSmsDao.insertPending(pending)
                        importedCount++
                        continue
                    }

                    val parsed = if (mapping != null && mapping.accountId > 0) {
                        val matchedAccount = accounts.find { it.id == mapping.accountId }
                        if (matchedAccount != null) {
                            SmsParser.parse(
                                sender = sender,
                                body = body,
                                resolvedAccountName = matchedAccount.name,
                                bankIndicator = matchedAccount.name
                            )
                        } else {
                            SmsParser.parse(sender, body)
                        }
                    } else {
                        SmsParser.parse(sender, body)
                    }

                    if (parsed == null) continue

                    // Check for duplicates in pending table
                    val existsInPending = pendingSmsDao.isSmsExists(body)
                    if (existsInPending) continue

                    // Match account name
                    val matchedAccount = if (mapping != null && mapping.accountId > 0) {
                        accounts.find { it.id == mapping.accountId }
                    } else {
                        accounts.firstOrNull { acc ->
                            acc.name.equals(parsed.detectedAccountName, ignoreCase = true)
                        }
                    }

                    val pending = PendingSmsTransactionEntity(
                        rawSmsBody          = body,
                        senderAddress       = sender,
                        amount              = parsed.amount,
                        type                = parsed.type,
                        category            = parsed.category,
                        note                = parsed.note,
                        detectedAccountName = parsed.detectedAccountName,
                        fromAccountId       = matchedAccount?.id ?: -1,
                        toAccountId         = null,
                        timestamp           = parsed.timestamp ?: smsDate,
                        receivedAt          = smsDate
                    )

                    pendingSmsDao.insertPending(pending)
                    importedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying system SMS provider: ${e.message}", e)
        }

        return@withContext importedCount
    }

    /**
     * Scans the system SMS inbox for potential transaction messages from unknown, unlinked,
     * or unmapped senders.
     */
    suspend fun findPotentialUnknownSenders(context: Context, database: FinanceDatabase): List<PotentialSender> = withContext(Dispatchers.IO) {
        val result = mutableListOf<PotentialSender>()
        if (!isReadSmsPermissionGranted(context)) return@withContext result

        val mappings = database.smsSenderMappingDao().getAllMappingsOnce()
        val accounts = database.accountDao().getAllAccountsOnce()
        val mappingMap = mappings.associateBy { it.senderAddress.lowercase().trim() }

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        // Search for keywords that suggest transaction messages
        val selection = "body LIKE '%Tk%' OR body LIKE '%BDT%' OR body LIKE '%৳%' OR body LIKE '%received%' OR body LIKE '%sent%' OR body LIKE '%paid%' OR body LIKE '%Cash In%' OR body LIKE '%Cash Out%' OR body LIKE '%Payment%'"
        
        val seenSenders = mutableSetOf<String>()

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                null,
                "date DESC"
            )?.use { cursor ->
                val addressIdx = cursor.getColumnIndexOrThrow("address")
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val sender = cursor.getString(addressIdx) ?: continue
                    val senderLower = sender.lowercase().trim()
                    if (seenSenders.contains(senderLower)) continue

                    val mapping = mappingMap[senderLower]

                    // 1. If actively mapped to an account in DB -> skip from Link New
                    if (mapping != null && mapping.accountId > 0) continue

                    // 2. If mapping is not in DB:
                    if (mapping == null) {
                        val resolvedAccountName = SmsParser.resolveAccount(sender)
                        if (resolvedAccountName != null) {
                            val matchingAccount = accounts.firstOrNull { acc ->
                                acc.name.equals(resolvedAccountName, ignoreCase = true) ||
                                resolvedAccountName.contains(acc.name, ignoreCase = true) ||
                                acc.name.contains(resolvedAccountName, ignoreCase = true)
                            }
                            // If an active account matches by default -> skip from Link New
                            if (matchingAccount != null) continue
                        }
                    }

                    // 3. Either explicitly unlinked (accountId == -1), unknown number, or unmatched default sender
                    val body = cursor.getString(bodyIdx) ?: continue
                    val date = cursor.getLong(dateIdx)

                    seenSenders.add(senderLower)
                    result.add(PotentialSender(
                        senderAddress = sender,
                        latestMessage = body,
                        timestamp = date
                    ))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding potential senders: ${e.message}", e)
        }

        // Also add any explicitly unlinked senders (accountId == -1) from DB if not already seen in inbox
        val unlinkedDbMappings = mappings.filter { it.accountId == -1 }
        for (unlinked in unlinkedDbMappings) {
            val sLower = unlinked.senderAddress.lowercase().trim()
            if (!seenSenders.contains(sLower)) {
                seenSenders.add(sLower)
                val canonical = SmsParser.resolveAccount(unlinked.senderAddress) ?: unlinked.senderAddress
                result.add(
                    PotentialSender(
                        senderAddress = canonical,
                        latestMessage = "Unlinked sender. Tap 'Link Account' to attach to a bank or wallet.",
                        timestamp = System.currentTimeMillis()
                    )
                )
            }
        }

        return@withContext result
    }

    /**
     * Sycns past SMS history from a specific sender address using the mapped account's context.
     */
    suspend fun syncPreviousSmsForSender(context: Context, database: FinanceDatabase, senderAddress: String, accountId: Int): Int = withContext(Dispatchers.IO) {
        if (!isReadSmsPermissionGranted(context)) return@withContext 0

        val pendingSmsDao = database.pendingSmsDao()
        val accountDao = database.accountDao()
        val accounts = accountDao.getAllAccountsOnce()
        val matchedAccount = accounts.find { it.id == accountId } ?: return@withContext 0

        val uri = Uri.parse("content://sms/inbox")
        val projection = arrayOf("address", "body", "date")

        val selection = "address = ?"
        val selectionArgs = arrayOf(senderAddress)

        var importedCount = 0

        try {
            context.contentResolver.query(
                uri,
                projection,
                selection,
                selectionArgs,
                "date DESC"
            )?.use { cursor ->
                val bodyIdx = cursor.getColumnIndexOrThrow("body")
                val dateIdx = cursor.getColumnIndexOrThrow("date")

                while (cursor.moveToNext()) {
                    val body = cursor.getString(bodyIdx) ?: continue
                    val smsDate = cursor.getLong(dateIdx)

                    // Attempt to parse SMS with mapping context
                    val parsed = SmsParser.parse(
                        sender = senderAddress,
                        body = body,
                        resolvedAccountName = matchedAccount.name,
                        bankIndicator = matchedAccount.name
                    ) ?: continue

                    // Check for duplicates in pending table
                    val existsInPending = pendingSmsDao.isSmsExists(body)
                    if (existsInPending) continue

                    val pending = PendingSmsTransactionEntity(
                        rawSmsBody          = body,
                        senderAddress       = senderAddress,
                        amount              = parsed.amount,
                        type                = parsed.type,
                        category            = parsed.category,
                        note                = parsed.note,
                        detectedAccountName = parsed.detectedAccountName,
                        fromAccountId       = matchedAccount.id,
                        toAccountId         = null,
                        timestamp           = parsed.timestamp ?: smsDate,
                        receivedAt          = smsDate
                    )

                    pendingSmsDao.insertPending(pending)
                    importedCount++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing SMS for specific sender: ${e.message}", e)
        }

        return@withContext importedCount
    }

    /** Returns true if READ_SMS permission is currently granted. */
    fun isReadSmsPermissionGranted(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }
}

data class PotentialSender(
    val senderAddress: String,
    val latestMessage: String,
    val timestamp: Long
)
