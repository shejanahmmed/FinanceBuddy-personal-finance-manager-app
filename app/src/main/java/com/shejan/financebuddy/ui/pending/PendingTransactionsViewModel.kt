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

    /** SMS Sender Mappings flow */
    val mappingsList: StateFlow<List<com.shejan.financebuddy.data.db.SmsSenderMappingEntity>> =
        database.smsSenderMappingDao().getAllMappingsFlow()
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _potentialSenders = kotlinx.coroutines.flow.MutableStateFlow<List<com.shejan.financebuddy.sms.PotentialSender>>(emptyList())
    val potentialSenders: StateFlow<List<com.shejan.financebuddy.sms.PotentialSender>> = _potentialSenders

    fun loadPotentialSenders(context: android.content.Context) {
        viewModelScope.launch(Dispatchers.IO) {
            val senders = com.shejan.financebuddy.sms.SmsSyncHelper.findPotentialUnknownSenders(context, database)
            _potentialSenders.value = senders
        }
    }

    fun addMapping(senderAddress: String, accountId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            database.smsSenderMappingDao().insertMapping(
                com.shejan.financebuddy.data.db.SmsSenderMappingEntity(
                    senderAddress = senderAddress.lowercase().trim(),
                    accountId = accountId
                )
            )
        }
    }

    fun deleteMapping(mapping: com.shejan.financebuddy.data.db.SmsSenderMappingEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            database.smsSenderMappingDao().deleteMapping(mapping)
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
