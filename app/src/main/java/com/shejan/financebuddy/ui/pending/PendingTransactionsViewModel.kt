package com.shejan.financebuddy.ui.pending

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.shejan.financebuddy.data.db.FinanceDatabase
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

import com.shejan.financebuddy.data.db.SmsSenderMappingEntity
import com.shejan.financebuddy.sms.SmsParser
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.MutableStateFlow

data class ActiveSenderMapping(
    val id: Int = 0, // 0 if default, > 0 if DB entity
    val senderAddress: String, // e.g., "bKash", "+8801700000000"
    val accountId: Int,
    val accountName: String,
    val isDefault: Boolean = false,
    val isCustomOverride: Boolean = false
)

class PendingTransactionsViewModel(private val database: FinanceDatabase) : ViewModel() {

    private val pendingSmsDao  = database.pendingSmsDao()
    private val transactionDao = database.transactionDao()

    /** All pending SMS-detected transactions (status = PENDING), ordered newest first. */
    val pendingList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getAllPending()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Confirmed SMS transactions (status = CONFIRMED). */
    val confirmedList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getConfirmedList()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Dismissed SMS transactions (status = DISMISSED). */
    val dismissedList: StateFlow<List<PendingSmsTransactionEntity>> =
        pendingSmsDao.getDismissedList()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** Badge count for pending items. */
    val pendingCount: StateFlow<Int> =
        pendingSmsDao.getPendingCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Count for confirmed items. */
    val confirmedCount: StateFlow<Int> =
        pendingSmsDao.getConfirmedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Count for dismissed items. */
    val dismissedCount: StateFlow<Int> =
        pendingSmsDao.getDismissedCount()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** Active SMS Sender Mappings (merging custom DB mappings, default pre-synced mappings, and active inbox senders). */
    val activeMappings: StateFlow<List<ActiveSenderMapping>> = combine(
        database.accountDao().getAllAccounts(),
        database.smsSenderMappingDao().getAllMappingsFlow(),
        pendingSmsDao.getAllPending()
    ) { accounts, dbMappings, pendingList ->
        val result = mutableListOf<ActiveSenderMapping>()
        val dbMappingBySender = dbMappings.associateBy { it.senderAddress.lowercase().trim() }
        val addedSenders = mutableSetOf<String>()
        val addedAccountKeys = mutableSetOf<String>()

        // 1. Add all custom DB mappings where accountId > 0 and the account still exists
        for (mapping in dbMappings) {
            if (mapping.accountId > 0) {
                val senderLower = mapping.senderAddress.lowercase().trim()
                val account = accounts.find { it.id == mapping.accountId }
                if (account != null) {
                    val displaySender = SmsParser.formatDisplaySender(mapping.senderAddress)
                    val key = "${displaySender.lowercase()}_${account.id}"
                    if (!addedAccountKeys.contains(key)) {
                        val isDefault = SmsParser.resolveAccount(mapping.senderAddress) != null
                        result.add(
                            ActiveSenderMapping(
                                id = mapping.id,
                                senderAddress = displaySender,
                                accountId = mapping.accountId,
                                accountName = account.name,
                                isDefault = isDefault,
                                isCustomOverride = isDefault
                            )
                        )
                        addedAccountKeys.add(key)
                    }
                    val aliases = SmsParser.getAliasesForSender(mapping.senderAddress)
                    for (a in aliases) {
                        addedSenders.add(a.lowercase().trim())
                    }
                    addedSenders.add(senderLower)
                }
            }
        }

        // 2. Add default active mappings for user's existing accounts (if not explicitly unlinked or overridden in DB)
        for (account in accounts) {
            val knownSenders = SmsParser.getKnownSendersForAccount(account.name)
            if (knownSenders.isEmpty()) continue

            // If any known sender for this account was explicitly unlinked in DB, skip
            val hasUnlinked = knownSenders.any { s ->
                val m = dbMappingBySender[s.lowercase().trim()]
                m != null && m.accountId == -1
            }
            if (hasUnlinked) continue

            // Check if any alias is already added via custom DB mapping (accountId > 0)
            if (knownSenders.any { addedSenders.contains(it.lowercase().trim()) }) {
                continue
            }

            // Find if user received any SMS from any known alias for this account in inbox
            val matchingPending = pendingList.firstOrNull { p ->
                knownSenders.any { k -> k.equals(p.senderAddress.trim(), ignoreCase = true) }
            }

            val defaultSenderCode = SmsParser.getDefaultSenderHeader(account.name)
            val senderHeader = matchingPending?.senderAddress ?: defaultSenderCode
            val formattedSenderHeader = SmsParser.formatDisplaySender(senderHeader)

            val key = "${formattedSenderHeader.lowercase()}_${account.id}"
            if (!addedAccountKeys.contains(key)) {
                result.add(
                    ActiveSenderMapping(
                        id = 0,
                        senderAddress = formattedSenderHeader,
                        accountId = account.id,
                        accountName = account.name,
                        isDefault = true,
                        isCustomOverride = false
                    )
                )
                addedAccountKeys.add(key)
            }

            for (s in knownSenders) {
                addedSenders.add(s.lowercase().trim())
            }
        }

        // 3. Fallback: Any pending SMS with fromAccountId > 0 that has a valid account, not yet listed and not unlinked
        for (pending in pendingList) {
            if (pending.fromAccountId > 0) {
                val sLower = pending.senderAddress.lowercase().trim()
                val m = dbMappingBySender[sLower]
                if (m != null && m.accountId == -1) continue // explicitly unlinked

                if (!addedSenders.contains(sLower)) {
                    val account = accounts.find { it.id == pending.fromAccountId }
                    if (account != null) {
                        val displaySender = SmsParser.formatDisplaySender(pending.senderAddress)
                        val key = "${displaySender.lowercase()}_${account.id}"
                        if (!addedAccountKeys.contains(key)) {
                            val isDefault = SmsParser.resolveAccount(pending.senderAddress) != null
                            result.add(
                                ActiveSenderMapping(
                                    id = 0,
                                    senderAddress = displaySender,
                                    accountId = account.id,
                                    accountName = account.name,
                                    isDefault = isDefault,
                                    isCustomOverride = false
                                )
                            )
                            addedAccountKeys.add(key)
                        }
                        val aliases = SmsParser.getAliasesForSender(pending.senderAddress)
                        for (a in aliases) {
                            addedSenders.add(a.lowercase().trim())
                        }
                        addedSenders.add(sLower)
                    }
                }
            }
        }

        result
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** SMS Sender Mappings flow */
    val mappingsList: StateFlow<List<SmsSenderMappingEntity>> =
        database.smsSenderMappingDao().getAllMappingsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _potentialSenders = MutableStateFlow<List<com.shejan.financebuddy.sms.PotentialSender>>(emptyList())
    val potentialSenders: StateFlow<List<com.shejan.financebuddy.sms.PotentialSender>> = _potentialSenders

    init {
        viewModelScope.launch(Dispatchers.IO) {
            autoHealAndLinkAccounts()
        }
    }

    suspend fun autoHealAndLinkAccounts() {
        val accounts = database.accountDao().getAllAccountsOnce().toMutableList()
        val pendingItems = pendingSmsDao.getAllPendingOnce()
        val mappings = database.smsSenderMappingDao().getAllMappingsOnce()
        val unlinkedSenders = mappings.filter { it.accountId == -1 }.map { it.senderAddress.lowercase().trim() }.toSet()

        for (pending in pendingItems) {
            val sLower = pending.senderAddress.lowercase().trim()
            if (unlinkedSenders.contains(sLower)) continue // explicitly unlinked

            if (pending.fromAccountId == -1 && pending.detectedAccountName.isNotBlank()) {
                var matched = accounts.firstOrNull { a ->
                    a.name.equals(pending.detectedAccountName, ignoreCase = true) ||
                    a.name.contains(pending.detectedAccountName, ignoreCase = true) ||
                    pending.detectedAccountName.contains(a.name, ignoreCase = true)
                }

                if (matched == null) {
                    val isMfs = SmsParser.isMfsAccount(pending.detectedAccountName)
                    val newAcc = com.shejan.financebuddy.data.db.AccountEntity(
                        id = 0,
                        name = pending.detectedAccountName,
                        type = if (isMfs) "MFS" else "BANK",
                        balance = 0.0,
                        colorHex = SmsParser.getDefaultColorForAccount(pending.detectedAccountName)
                    )
                    val insertedId = database.accountDao().insertAccount(newAcc).toInt()
                    matched = newAcc.copy(id = insertedId)
                    accounts.add(matched)
                }

                pendingSmsDao.updatePending(pending.copy(fromAccountId = matched.id))
            }
        }
    }

    fun loadPotentialSenders(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            autoHealAndLinkAccounts()
            val senders = com.shejan.financebuddy.sms.SmsSyncHelper.findPotentialUnknownSenders(context, database)
            _potentialSenders.value = senders
        }
    }

    fun addMapping(senderAddress: String, accountId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val senderLower = senderAddress.lowercase().trim()
            val sendersToMap = SmsParser.getAliasesForSender(senderAddress).ifEmpty { listOf(senderLower) }

            for (s in sendersToMap) {
                database.smsSenderMappingDao().deleteBySender(s)
                database.smsSenderMappingDao().insertMapping(
                    SmsSenderMappingEntity(
                        senderAddress = s,
                        accountId = accountId
                    )
                )
                database.pendingSmsDao().updateAccountIdForSender(s, accountId)
            }
        }
    }

    fun unlinkMapping(mapping: ActiveSenderMapping) {
        viewModelScope.launch(Dispatchers.IO) {
            val senderLower = mapping.senderAddress.lowercase().trim()
            val sendersToUnlink = SmsParser.getAliasesForSender(mapping.senderAddress).ifEmpty { listOf(senderLower) }

            for (s in sendersToUnlink) {
                database.smsSenderMappingDao().deleteBySender(s)
                if (mapping.isDefault || mapping.id == 0) {
                    database.smsSenderMappingDao().insertMapping(
                        SmsSenderMappingEntity(
                            senderAddress = s,
                            accountId = -1
                        )
                    )
                }
                database.pendingSmsDao().unassignAccountIdForSender(s)
            }
        }
    }

    fun deleteMapping(mapping: SmsSenderMappingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val isDefault = SmsParser.resolveAccount(mapping.senderAddress) != null
            val sendersToHandle = SmsParser.getAliasesForSender(mapping.senderAddress).ifEmpty { listOf(mapping.senderAddress) }
            for (s in sendersToHandle) {
                database.smsSenderMappingDao().deleteBySender(s)
                if (isDefault) {
                    database.smsSenderMappingDao().insertMapping(
                        SmsSenderMappingEntity(
                            senderAddress = s,
                            accountId = -1
                        )
                    )
                }
                database.pendingSmsDao().unassignAccountIdForSender(s)
            }
        }
    }

    fun syncSenderHistory(context: android.content.Context, senderAddress: String, accountId: Int, onComplete: (Int) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val count = com.shejan.financebuddy.sms.SmsSyncHelper.syncPreviousSmsForSender(context, database, senderAddress, accountId)
            launch(Dispatchers.Main) {
                onComplete(count)
            }
        }
    }

    /**
     * Confirms a pending entry: inserts it as a real transaction (updating balances)
     * and updates its status to "CONFIRMED".
     */
    fun confirm(pending: PendingSmsTransactionEntity, edited: PendingSmsTransactionEntity = pending) {
        viewModelScope.launch(Dispatchers.IO) {
            val updatedPending = edited.copy(status = "CONFIRMED")
            val transaction = TransactionEntity(
                amount        = updatedPending.amount,
                type          = updatedPending.type,
                category      = updatedPending.category,
                timestamp     = updatedPending.timestamp,
                fromAccountId = updatedPending.fromAccountId,
                toAccountId   = updatedPending.toAccountId,
                note          = updatedPending.note
            )
            transactionDao.insertTransaction(transaction)  // also adjusts account balances
            pendingSmsDao.updatePending(updatedPending)
        }
    }

    /**
     * Dismisses a pending entry by marking status = "DISMISSED".
     */
    fun dismiss(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updateStatus(pending.id, "DISMISSED")
        }
    }

    /**
     * Restores a DISMISSED entry back to "PENDING".
     * DISMISSED entries never had a real transaction inserted, so no balance reversal is needed.
     */
    fun restore(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updateStatus(pending.id, "PENDING")
        }
    }

    /**
     * Restores a CONFIRMED entry back to "PENDING" AND reverses its balance impact.
     *
     * When a pending entry was confirmed, a real TransactionEntity was inserted and
     * account balances were adjusted. Moving it back to Pending must undo that:
     * 1. Find the linked real transaction (by timestamp + fromAccountId + amount).
     * 2. Delete it → reverses the balance effect on the account.
     * 3. Reset the pending record status to "PENDING" so the user can re-review it.
     *
     * If no matching transaction is found (e.g. it was already manually deleted from
     * the history), we still reset the pending status to avoid it being stuck on CONFIRMED.
     */
    fun restoreConfirmed(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            val existing = transactionDao.findByTimestampAndAccount(
                timestamp     = pending.timestamp,
                fromAccountId = pending.fromAccountId,
                amount        = pending.amount
            )
            if (existing != null) {
                transactionDao.deleteTransaction(existing) // reverses account balance
            }
            pendingSmsDao.updateStatus(pending.id, "PENDING")
        }
    }

    /**
     * Permanently deletes a pending entry.
     */
    fun deletePermanently(pending: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.deletePending(pending)
        }
    }

    /**
     * Saves edits the user made to a pending entry.
     * Only for PENDING/DISMISSED status — does NOT touch the transactions table.
     */
    fun update(updated: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.updatePending(updated)
        }
    }

    /**
     * Updates a CONFIRMED pending entry AND its linked real transaction.
     *
     * Strategy:
     * 1. Find the original transaction by (timestamp + fromAccountId + amount).
     * 2. Delete it → reverses the old balance effect on the account.
     * 3. Insert an updated transaction → applies new balance effects.
     * 4. Save the updated pending record so the card reflects the new values.
     *
     * If no matching transaction is found (e.g. the user manually deleted it),
     * we fall back to a plain insert so the balance is at least corrected.
     */
    fun updateConfirmed(old: PendingSmsTransactionEntity, updated: PendingSmsTransactionEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            // Step 1 – find and delete the old transaction to reverse its balance impact
            val existing = transactionDao.findByTimestampAndAccount(
                timestamp     = old.timestamp,
                fromAccountId = old.fromAccountId,
                amount        = old.amount
            )
            if (existing != null) {
                transactionDao.deleteTransaction(existing) // also reverses account balance
            }

            // Step 2 – insert a fresh transaction with the corrected values
            val newTx = TransactionEntity(
                amount        = updated.amount,
                type          = updated.type,
                category      = updated.category,
                timestamp     = updated.timestamp,
                fromAccountId = updated.fromAccountId,
                toAccountId   = updated.toAccountId,
                note          = updated.note
            )
            transactionDao.insertTransaction(newTx) // also adjusts account balance

            // Step 3 – persist the updated pending record
            pendingSmsDao.updatePending(updated.copy(status = "CONFIRMED"))
        }
    }

    /**
     * Marks all currently pending entries as DISMISSED.
     */
    fun dismissAll() {
        viewModelScope.launch(Dispatchers.IO) {
            pendingSmsDao.dismissAllPending()
        }
    }

    /**
     * Confirms only pending entries that:
     * 1. Have an assigned account (fromAccountId != -1), AND
     * 2. The account still exists in the DB (not orphaned/deleted).
     * fromAccountId is a non-nullable Int; -1 is the sentinel for "no account mapped".
     * Returns counts via callback (acceptedCount, skippedCount).
     */
    fun confirmAll(onComplete: (acceptedCount: Int, skippedCount: Int) -> Unit = { _, _ -> }) {
        viewModelScope.launch(Dispatchers.IO) {
            val items = pendingList.value
            val existingAccountIds = database.accountDao().getAllAccountsOnce().map { it.id }.toSet()

            // Skip unassigned (-1) AND orphaned (stale ID pointing to deleted account)
            val readyItems = items.filter { it.fromAccountId != -1 && it.fromAccountId in existingAccountIds }
            val skippedCount = items.size - readyItems.size

            readyItems.forEach { item ->
                val updated = item.copy(status = "CONFIRMED")
                val transaction = TransactionEntity(
                    amount        = updated.amount,
                    type          = updated.type,
                    category      = updated.category,
                    timestamp     = updated.timestamp,
                    fromAccountId = updated.fromAccountId,
                    toAccountId   = updated.toAccountId,
                    note          = updated.note
                )
                transactionDao.insertTransaction(transaction)
                pendingSmsDao.updatePending(updated)
            }

            launch(Dispatchers.Main) {
                onComplete(readyItems.size, skippedCount)
            }
        }
    }
}
