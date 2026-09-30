package com.shejan.financebuddy.ui.home

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.common.AppBackButton
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionListScreen(
    type: String, // "INCOME" or "EXPENSE"
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    // Standard back handler to support system back gestures
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    // Filter states: "ALL", "WEEK", "MONTH", "YEAR", "CUSTOM"
    var selectedFilter by remember { mutableStateOf("ALL") }
    var customDateMillis by remember { mutableStateOf<Long?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    // Date Picker launcher for Custom Date selection
    if (showDatePicker) {
        val cal = Calendar.getInstance()
        if (customDateMillis != null) {
            cal.timeInMillis = customDateMillis!!
        }
        DisposableEffect(Unit) {
            val dialog = DatePickerDialog(
                context,
                { _, year, month, dayOfMonth ->
                    val selectedCal = Calendar.getInstance().apply {
                        set(year, month, dayOfMonth, 0, 0, 0)
                        set(Calendar.MILLISECOND, 0)
                    }
                    customDateMillis = selectedCal.timeInMillis
                    selectedFilter = "CUSTOM"
                    showDatePicker = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.datePicker.maxDate = System.currentTimeMillis()
            dialog.setOnCancelListener { showDatePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }

    // Filtered transactions based on selected filter
    val filteredTransactions = remember(transactions, selectedFilter, customDateMillis) {
        when (selectedFilter) {
            "WEEK" -> {
                val startOfWeek = Calendar.getInstance().apply {
                    firstDayOfWeek = Calendar.MONDAY
                    set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                transactions.filter { it.timestamp >= startOfWeek }
            }
            "MONTH" -> {
                val startOfMonth = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_MONTH, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                transactions.filter { it.timestamp >= startOfMonth }
            }
            "YEAR" -> {
                val startOfYear = Calendar.getInstance().apply {
                    set(Calendar.DAY_OF_YEAR, 1)
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.timeInMillis
                transactions.filter { it.timestamp >= startOfYear }
            }
            "CUSTOM" -> {
                if (customDateMillis == null) transactions
                else {
                    val startOfDay = customDateMillis!!
                    val endOfDay = Calendar.getInstance().apply {
                        timeInMillis = customDateMillis!!
                        set(Calendar.HOUR_OF_DAY, 23)
                        set(Calendar.MINUTE, 59)
                        set(Calendar.SECOND, 59)
                        set(Calendar.MILLISECOND, 999)
                    }.timeInMillis
                    transactions.filter { it.timestamp in startOfDay..endOfDay }
                }
            }
            else -> transactions
        }
    }

    val totalAmount = remember(filteredTransactions) { filteredTransactions.sumOf { it.amount } }

    // Group transactions by date string
    val groupedTransactions = remember(filteredTransactions) {
        val dateFormat = SimpleDateFormat("d MMMM yyyy", Locale.ENGLISH)
        filteredTransactions.groupBy { tx ->
            dateFormat.format(Date(tx.timestamp))
        }
    }

    val filterOptions = listOf(
        "ALL" to "All Time",
        "WEEK" to "This Week",
        "MONTH" to "This Month",
        "YEAR" to "This Year",
        "CUSTOM" to if (selectedFilter == "CUSTOM" && customDateMillis != null) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(customDateMillis!!))
        } else "Select Date"
    )

    var showFilterMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient background glow
        val glowColor = if (type == "INCOME") IncomeGreen else ExpenseRed
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(glowColor.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Header Top Bar ──────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 18.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left side: Back Button & Page Title
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AppBackButton(
                        onClick = { onBack() },
                        size = 40.dp,
                        cornerRadius = 12.dp
                    )

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (type == "INCOME") "Income Ledger" else "Expense Ledger",
                            color = TextPrimary,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.3).sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Historical transaction records",
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }

                // Right side: Filter Button & Dropdown Menu
                Box {
                    val isFiltered = selectedFilter != "ALL"
                    val activeLabel = filterOptions.firstOrNull { it.first == selectedFilter }?.second ?: "Filter"

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = if (isFiltered) glowColor.copy(alpha = 0.16f) else CardDark,
                        border = BorderStroke(1.dp, if (isFiltered) glowColor.copy(alpha = 0.5f) else DividerColor),
                        modifier = Modifier.clickable { showFilterMenu = true }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = "Filter",
                                tint = if (isFiltered) glowColor else TextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = if (isFiltered) activeLabel else "Filter",
                                color = if (isFiltered) glowColor else TextPrimary,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showFilterMenu,
                        onDismissRequest = { showFilterMenu = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                    ) {
                        filterOptions.forEach { (key, label) ->
                            val isSelected = selectedFilter == key
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        if (key == "CUSTOM") {
                                            Icon(
                                                imageVector = Icons.Default.CalendarMonth,
                                                contentDescription = null,
                                                tint = if (isSelected) glowColor else TextSecondary,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = label,
                                            color = if (isSelected) glowColor else TextPrimary,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            fontSize = 13.sp
                                        )
                                    }
                                },
                                trailingIcon = {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Selected",
                                            tint = glowColor,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                },
                                onClick = {
                                    showFilterMenu = false
                                    if (key == "CUSTOM") {
                                        showDatePicker = true
                                    } else {
                                        selectedFilter = key
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // ─── Content List ─────────────────────────────────────────────
            if (filteredTransactions.isEmpty()) {
                EmptyTransactionsState(type = type)
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Summary Aggregates Card
                    item {
                        SummaryAggregateCard(
                            type = type,
                            totalAmount = totalAmount,
                            count = filteredTransactions.size,
                            formatter = currencyFormat
                        )
                    }

                    // Transaction list grouped by day
                    groupedTransactions.forEach { (dateHeader, txs) ->
                        // Date section header
                        item {
                            Text(
                                text = dateHeader,
                                color = TextSecondary,
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)
                            )
                        }

                        items(txs, key = { it.id }) { tx ->
                            TransactionDetailsRow(
                                tx = tx,
                                accounts = accounts,
                                formatter = currencyFormat
                            )
                            HorizontalDivider(
                                color = DividerColor.copy(alpha = 0.35f),
                                modifier = Modifier.padding(horizontal = 20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryAggregateCard(
    type: String,
    totalAmount: Double,
    count: Int,
    formatter: DecimalFormat
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Total ${if (type == "INCOME") "Inflow" else "Outflow"}",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "৳${formatter.format(totalAmount)}",
                    color = if (type == "INCOME") IncomeGreen else ExpenseRed,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = (if (type == "INCOME") IncomeGreen else ExpenseRed).copy(alpha = 0.12f),
                border = BorderStroke(1.dp, (if (type == "INCOME") IncomeGreen else ExpenseRed).copy(alpha = 0.35f))
            ) {
                Text(
                    text = "$count Trans.",
                    color = if (type == "INCOME") IncomeGreen else ExpenseRed,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun TransactionDetailsRow(
    tx: TransactionEntity,
    accounts: List<AccountEntity>,
    formatter: DecimalFormat
) {
    val timeStr = remember(tx.timestamp) {
        SimpleDateFormat("hh:mm a", Locale.ENGLISH).format(Date(tx.timestamp))
    }
    val account = remember(tx.fromAccountId, accounts) {
        accounts.find { it.id == tx.fromAccountId }
    }
    val accountName = account?.name ?: "Unknown Account"
    val subDetail = remember(tx.note, account?.showAs) {
        if (tx.note.isNotBlank()) tx.note else account?.showAs?.takeIf { it.isNotBlank() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Styled indicator badge (Squircle)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(CardDark)
                .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (tx.type == "INCOME") Icons.AutoMirrored.Filled.TrendingUp else Icons.Default.TrendingDown,
                contentDescription = null,
                tint = if (tx.type == "INCOME") IncomeGreen else ExpenseRed,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        // Transaction metadata
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = tx.category.ifBlank { if (tx.type == "INCOME") "Income" else "Expense" },
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "$accountName • $timeStr",
                color = TextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Normal
            )
            if (!subDetail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subDetail,
                    color = TextMuted,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Amount Display
        Text(
            text = "${if (tx.type == "INCOME") "+" else "-"}৳${formatter.format(tx.amount)}",
            color = if (tx.type == "INCOME") IncomeGreen else ExpenseRed,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun EmptyTransactionsState(type: String) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 48.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(36.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "No Records Found",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "You haven't recorded any ${if (type == "INCOME") "income" else "expenses"} yet.",
                color = TextSecondary,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
