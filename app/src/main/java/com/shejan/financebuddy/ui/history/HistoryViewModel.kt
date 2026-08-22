package com.shejan.financebuddy.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class HistoryViewModel(
    private val repository: FinanceRepository
) : ViewModel() {

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTypeFilter = MutableStateFlow("ALL")
    val selectedTypeFilter: StateFlow<String> = _selectedTypeFilter.asStateFlow()

    private val _selectedPeriodFilter = MutableStateFlow("ALL")
    val selectedPeriodFilter: StateFlow<String> = _selectedPeriodFilter.asStateFlow()

    private val _customDateMillis = MutableStateFlow<Long?>(null)
    val customDateMillis: StateFlow<Long?> = _customDateMillis.asStateFlow()

    private val _selectedAccountIdFilter = MutableStateFlow(0)
    val selectedAccountIdFilter: StateFlow<Int> = _selectedAccountIdFilter.asStateFlow()

    fun setTypeFilter(type: String) {
        _selectedTypeFilter.value = type
    }

    fun setPeriodFilter(period: String) {
        _selectedPeriodFilter.value = period
    }

    fun setCustomDate(millis: Long?) {
        _customDateMillis.value = millis
        if (millis != null) {
            _selectedPeriodFilter.value = "CUSTOM"
        }
    }

    fun setAccountFilter(accountId: Int) {
        _selectedAccountIdFilter.value = accountId
    }

    fun resetFilters() {
        _selectedTypeFilter.value = "ALL"
        _selectedPeriodFilter.value = "ALL"
        _selectedAccountIdFilter.value = 0
        _customDateMillis.value = null
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(oldTx, newTx)
        }
    }

    class Factory(private val repository: FinanceRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HistoryViewModel::class.java)) {
                return HistoryViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
