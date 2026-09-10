package com.shejan.financebuddy.ui.common

import android.content.Context
import androidx.core.content.edit
import com.shejan.financebuddy.data.db.FinanceDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object CategoryManager {

    suspend fun renameCategory(
        context: Context,
        oldCategory: String,
        newCategory: String,
        isExpense: Boolean = true
    ) = withContext(Dispatchers.IO) {
        val trimmedOld = oldCategory.trim()
        val trimmedNew = newCategory.trim()
        if (trimmedOld.isEmpty() || trimmedNew.isEmpty() || trimmedOld.equals(trimmedNew, ignoreCase = true)) {
            return@withContext
        }

        val prefs = context.getSharedPreferences("finance_buddy_prefs", Context.MODE_PRIVATE)
        val activeKey = if (isExpense) "active_expense_categories" else "active_income_categories"
        val customKey = if (isExpense) "custom_expense_categories" else "custom_income_categories"

        val activeSaved = prefs.getString(activeKey, null)
        if (activeSaved != null) {
            val updated = activeSaved.split("|").map {
                if (it.equals(trimmedOld, ignoreCase = true)) trimmedNew else it
            }.distinct()
            prefs.edit { putString(activeKey, updated.joinToString("|")) }
        }

        val customSaved = prefs.getString(customKey, "")
        if (!customSaved.isNullOrEmpty()) {
            val updated = customSaved.split("|").map {
                if (it.equals(trimmedOld, ignoreCase = true)) trimmedNew else it
            }.distinct()
            prefs.edit { putString(customKey, updated.joinToString("|")) }
        }

        val db = FinanceDatabase.getDatabase(context)
        db.budgetDao().updateCategoryName(trimmedOld, trimmedNew)
        db.transactionDao().updateCategoryName(trimmedOld, trimmedNew)
        db.pendingSmsDao().updateCategoryName(trimmedOld, trimmedNew)
    }
}
