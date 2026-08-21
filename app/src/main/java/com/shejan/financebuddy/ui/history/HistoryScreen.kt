package com.shejan.financebuddy.ui.history

import android.app.DatePickerDialog
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    transactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    // Type filter state: "ALL", "INCOME", "EXPENSE", "TRANSFER", "LOAN_REPAYMENT"
    var selectedTypeFilter by remember { mutableStateOf("ALL") }

    // Period filter state: "ALL", "WEEK", "MONTH", "YEAR", "CUSTOM"
    var selectedPeriodFilter by remember { mutableStateOf("ALL") }
    var customDateMillis by remember { mutableStateOf<Long?>(null) }

    // Account filter state: 0 for All, or account ID
    var selectedAccountIdFilter by remember { mutableIntStateOf(0) }

    // Single-card accordion expansion state (only one card expanded at a time)
    var expandedTxId by remember { mutableStateOf<Int?>(null) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showFilterSheet by remember { mutableStateOf(false) }

    // Accounts map for quick lookup
    val accountMap = remember(accounts) { accounts.associateBy { it.id } }

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
                    selectedPeriodFilter = "CUSTOM"
                    showDatePicker = false
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
            dialog.setOnCancelListener { showDatePicker = false }
            dialog.show()
            onDispose { dialog.dismiss() }
        }
    }

    // Filtered transaction list
    val filteredTransactions = remember(transactions, selectedTypeFilter, selectedPeriodFilter, selectedAccountIdFilter, customDateMillis) {
        transactions.filter { tx ->
            // 1. Type Filter
            val isLoanRepayment = tx.category.contains("Loan Repayment", ignoreCase = true) || tx.note.contains("Repayment to", ignoreCase = true)
            val typeMatch = when (selectedTypeFilter) {
                "INCOME" -> tx.type == "INCOME"
                "EXPENSE" -> tx.type == "EXPENSE" && !isLoanRepayment
                "TRANSFER" -> tx.type == "TRANSFER"
                "LOAN_REPAYMENT" -> isLoanRepayment
                else -> true
            }

            // 2. Period Filter
            val periodMatch = when (selectedPeriodFilter) {
                "WEEK" -> {
                    val startOfWeek = Calendar.getInstance().apply {
                        firstDayOfWeek = Calendar.MONDAY
                        set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tx.timestamp >= startOfWeek
                }
                "MONTH" -> {
                    val startOfMonth = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_MONTH, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tx.timestamp >= startOfMonth
                }
                "YEAR" -> {
                    val startOfYear = Calendar.getInstance().apply {
                        set(Calendar.DAY_OF_YEAR, 1)
                        set(Calendar.HOUR_OF_DAY, 0)
                        set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    tx.timestamp >= startOfYear
                }
                "CUSTOM" -> {
                    if (customDateMillis == null) true
                    else {
                        val startOfDay = customDateMillis!!
                        val endOfDay = Calendar.getInstance().apply {
                            timeInMillis = customDateMillis!!
                            set(Calendar.HOUR_OF_DAY, 23)
                            set(Calendar.MINUTE, 59)
                            set(Calendar.SECOND, 59)
                            set(Calendar.MILLISECOND, 999)
                        }.timeInMillis
                        tx.timestamp in startOfDay..endOfDay
                    }
                }
                else -> true
            }

            // 3. Account Filter
            val accountMatch = if (selectedAccountIdFilter == 0) true else {
                tx.fromAccountId == selectedAccountIdFilter || tx.toAccountId == selectedAccountIdFilter
            }

            typeMatch && periodMatch && accountMatch
        }.sortedByDescending { it.timestamp }
    }

    // Totals calculations
    val totalInflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalOutflow = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val totalTransfers = remember(filteredTransactions) {
        filteredTransactions.filter { it.type == "TRANSFER" }.sumOf { it.amount }
    }

    // Grouping by date
    val groupedTransactions = remember(filteredTransactions) {
        val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        val todayStr = dateFormat.format(Date())
        val cal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = dateFormat.format(cal.time)

        filteredTransactions.groupBy { tx ->
            val dateStr = dateFormat.format(Date(tx.timestamp))
            when (dateStr) {
                todayStr -> "Today"
                yesterdayStr -> "Yesterday"
                else -> dateStr
            }
        }
    }

    val typeFilters = listOf(
        "ALL" to "All",
        "INCOME" to "Income",
        "EXPENSE" to "Expenses",
        "TRANSFER" to "Transfers",
        "LOAN_REPAYMENT" to "Loan Repay"
    )

    val periodFilters = listOf(
        "ALL" to "All Time",
        "WEEK" to "This Week",
        "MONTH" to "This Month",
        "YEAR" to "This Year",
        "CUSTOM" to if (selectedPeriodFilter == "CUSTOM" && customDateMillis != null) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(customDateMillis!!))
        } else "Select Date"
    )

    val hasActiveFilters = selectedTypeFilter != "ALL" || selectedPeriodFilter != "ALL" || selectedAccountIdFilter != 0

    if (showFilterSheet) {
        HistoryFilterSheet(
            accounts = accounts,
            selectedType = selectedTypeFilter,
            selectedPeriod = selectedPeriodFilter,
            selectedAccountId = selectedAccountIdFilter,
            customDateMillis = customDateMillis,
            typeFilters = typeFilters,
            periodFilters = periodFilters,
            onSelectType = { selectedTypeFilter = it },
            onSelectPeriod = { selectedPeriodFilter = it },
            onSelectAccount = { selectedAccountIdFilter = it },
            onPickCustomDate = { showDatePicker = true },
            onReset = {
                selectedTypeFilter = "ALL"
                selectedPeriodFilter = "ALL"
                selectedAccountIdFilter = 0
                customDateMillis = null
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.05f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Header Top Bar ─────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(CardDarker)
                        .border(1.dp, DividerColor, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Transaction History",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Complete record of all financial activity",
                        color = TextSecondary,
                        fontSize = 12.sp
                    )
                }

                // Header Filter Action Button (Matching StatisticsScreen)
                IconButton(
                    onClick = { showFilterSheet = true }
                ) {
                    Icon(
                        imageVector = Icons.Default.FilterList,
                        contentDescription = "Filter",
                        tint = if (hasActiveFilters) AccentTeal else TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            // ─── Active Filter Badges (Shown when filters are enabled) ─────
            if (hasActiveFilters) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selectedTypeFilter != "ALL") {
                        item {
                            ActiveFilterBadge(
                                label = typeFilters.firstOrNull { it.first == selectedTypeFilter }?.second ?: selectedTypeFilter,
                                onClear = { selectedTypeFilter = "ALL" }
                            )
                        }
                    }
                    if (selectedPeriodFilter != "ALL") {
                        item {
                            val periodLabel = if (selectedPeriodFilter == "CUSTOM" && customDateMillis != null) {
                                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(customDateMillis!!))
                            } else periodFilters.firstOrNull { it.first == selectedPeriodFilter }?.second ?: selectedPeriodFilter

                            ActiveFilterBadge(
                                label = periodLabel,
                                onClear = {
                                    selectedPeriodFilter = "ALL"
                                    customDateMillis = null
                                }
                            )
                        }
                    }
                    if (selectedAccountIdFilter != 0) {
                        item {
                            val accName = accountMap[selectedAccountIdFilter]?.name ?: "Account"
                            ActiveFilterBadge(
                                label = accName,
                                onClear = { selectedAccountIdFilter = 0 }
                            )
                        }
                    }
                    item {
                        Text(
                            text = "Reset All",
                            color = AccentTeal,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable {
                                    selectedTypeFilter = "ALL"
                                    selectedPeriodFilter = "ALL"
                                    selectedAccountIdFilter = 0
                                    customDateMillis = null
                                }
                                .padding(horizontal = 4.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            // ─── Content List ─────────────────────────────────────────────
            if (filteredTransactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = TextSecondary.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No matching transactions", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                        Spacer(Modifier.height(4.dp))
                        Text("Try adjusting your search query or filter options", color = TextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    // Summary Aggregates Header (Single Card with 3-color Segmented Bar + 3 stacked rows)
                    item {
                        val totalVolume = totalInflow + totalOutflow + totalTransfers
                        val inflowWeight = if (totalVolume > 0 && totalInflow > 0) (totalInflow / totalVolume).toFloat() else 0f
                        val outflowWeight = if (totalVolume > 0 && totalOutflow > 0) (totalOutflow / totalVolume).toFloat() else 0f
                        val transferWeight = if (totalVolume > 0 && totalTransfers > 0) (totalTransfers / totalVolume).toFloat() else 0f

                        val inflowPct = if (totalVolume > 0) ((totalInflow / totalVolume) * 100).toInt() else 0
                        val outflowPct = if (totalVolume > 0) ((totalOutflow / totalVolume) * 100).toInt() else 0
                        val transferPct = if (totalVolume > 0) (100 - inflowPct - outflowPct).coerceAtLeast(0) else 0

                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 6.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // 1. Inflow Row (Title + Percentage on Single Row)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(IncomeGreen.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingUp,
                                                contentDescription = null,
                                                tint = IncomeGreen,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Inflow", fontSize = 13.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            if (totalVolume > 0) {
                                                Text("•", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.4f))
                                                Text("$inflowPct%", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "+৳${currencyFormat.format(totalInflow)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IncomeGreen
                                    )
                                }

                                // 2. Outflow Row (Title + Percentage on Single Row)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(ExpenseRed.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.TrendingDown,
                                                contentDescription = null,
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Outflow", fontSize = 13.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            if (totalVolume > 0) {
                                                Text("•", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.4f))
                                                Text("$outflowPct%", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "-৳${currencyFormat.format(totalOutflow)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ExpenseRed
                                    )
                                }

                                // 3. Transfers Row (Title + Percentage on Single Row)
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(26.dp)
                                                .clip(CircleShape)
                                                .background(TransferYellow.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.SwapHoriz,
                                                contentDescription = null,
                                                tint = TransferYellow,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text("Transfers", fontSize = 13.5.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                            if (totalVolume > 0) {
                                                Text("•", fontSize = 11.sp, color = TextSecondary.copy(alpha = 0.4f))
                                                Text("$transferPct%", fontSize = 12.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    Text(
                                        text = "⇄৳${currencyFormat.format(totalTransfers)}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TransferYellow
                                    )
                                }

                                Spacer(Modifier.height(2.dp))

                                // Multi-Segment Horizontal Ratio Bar inside a Smooth Track (No border)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(10.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(CardDarker)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxSize(),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (totalVolume > 0) {
                                            if (inflowWeight > 0.001f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(inflowWeight)
                                                        .fillMaxHeight()
                                                        .background(IncomeGreen)
                                                )
                                            }
                                            if (outflowWeight > 0.001f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(outflowWeight)
                                                        .fillMaxHeight()
                                                        .background(ExpenseRed)
                                                )
                                            }
                                            if (transferWeight > 0.001f) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(transferWeight)
                                                        .fillMaxHeight()
                                                        .background(TransferYellow)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Grouped Transactions
                    groupedTransactions.forEach { (dateHeader, txs) ->
                        item {
                            Text(
                                text = dateHeader,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 24.dp, top = 16.dp, bottom = 8.dp)
                            )
                        }

                        items(txs, key = { it.id }) { tx ->
                            HistoryTransactionCard(
                                tx = tx,
                                accountsMap = accountMap,
                                formatter = currencyFormat,
                                isExpanded = expandedTxId == tx.id,
                                onToggleExpand = {
                                    expandedTxId = if (expandedTxId == tx.id) null else tx.id
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}



@Composable
private fun HistoryTransactionCard(
    tx: TransactionEntity,
    accountsMap: Map<Int, AccountEntity>,
    formatter: DecimalFormat,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {

    val isLoanRepayment = tx.category.contains("Loan Repayment", ignoreCase = true) || tx.note.contains("Repayment to", ignoreCase = true)

    val (badgeText, badgeColor, icon) = when {
        isLoanRepayment -> Triple("LOAN REPAY", AccentTeal, Icons.Default.CreditCard)
        tx.type == "INCOME" -> Triple("INCOME", IncomeGreen, Icons.Default.TrendingUp)
        tx.type == "EXPENSE" -> Triple("EXPENSE", ExpenseRed, Icons.Default.TrendingDown)
        else -> Triple("TRANSFER", TransferYellow, Icons.Default.SwapHoriz)
    }

    val fromAccount = accountsMap[tx.fromAccountId]
    val toAccount = tx.toAccountId?.let { accountsMap[it] }

    val formattedDateTime = remember(tx.timestamp) {
        SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
    }

    fun formatAccNum(num: String): String {
        return if (num.length > 4) "•••• ${num.takeLast(4)}" else num
    }

    val accountSubtext = when {
        tx.type == "TRANSFER" && fromAccount != null && toAccount != null ->
            "${fromAccount.name} (${formatAccNum(fromAccount.accountNumber)}) ➔ ${toAccount.name} (${formatAccNum(toAccount.accountNumber)})"
        tx.type == "TRANSFER" && fromAccount != null ->
            "From: ${fromAccount.name} (${formatAccNum(fromAccount.accountNumber)})"
        fromAccount != null ->
            "${fromAccount.name} (${formatAccNum(fromAccount.accountNumber)})"
        else -> "General Account"
    }

    val amountPrefix = when {
        tx.type == "INCOME" -> "+"
        tx.type == "EXPENSE" -> "-"
        tx.type == "TRANSFER" -> "⇄"
        else -> "↺"
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, badgeColor.copy(alpha = 0.2f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggleExpand)
            .animateContentSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header Row (Default visible: Category Icon & Name on left, Amount & Down/Up Arrow on right)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Icon Box
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(badgeColor.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = badgeColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Category Name / Title
                    Text(
                        text = if (tx.category.isNotBlank()) tx.category else tx.type,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Amount
                    Text(
                        text = "$amountPrefix৳${formatter.format(tx.amount)}",
                        color = badgeColor,
                        fontSize = 14.5.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Expand / Collapse Circular Arrow Badge
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                            .border(1.dp, DividerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription = if (isExpanded) "Collapse" else "Expand",
                            tint = TextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Expanded Details Section (Enclosed in a sleek dark inner box)
            if (isExpanded) {
                Spacer(modifier = Modifier.height(10.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Type Label
                        Text(
                            text = badgeText,
                            color = badgeColor,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.ExtraBold
                        )

                        // Bank Name & Account Number
                        Text(
                            text = accountSubtext,
                            color = TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )

                        // Date & Time
                        Text(
                            text = formattedDateTime,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Medium
                        )

                        // Optional Note
                        if (tx.note.isNotBlank() && tx.note != tx.category) {
                            Text(
                                text = tx.note,
                                color = TextSecondary,
                                fontSize = 11.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActiveFilterBadge(
    label: String,
    onClear: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = AccentTeal.copy(alpha = 0.15f),
        border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = label, color = AccentTeal, fontSize = 11.5.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear filter",
                tint = AccentTeal,
                modifier = Modifier
                    .size(14.dp)
                    .clickable(onClick = onClear)
            )
        }
    }
}

// ─── Filter Modal Bottom Sheet ─────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun HistoryFilterSheet(
    accounts: List<AccountEntity>,
    selectedType: String,
    selectedPeriod: String,
    selectedAccountId: Int,
    customDateMillis: Long?,
    typeFilters: List<Pair<String, String>>,
    periodFilters: List<Pair<String, String>>,
    onSelectType: (String) -> Unit,
    onSelectPeriod: (String) -> Unit,
    onSelectAccount: (Int) -> Unit,
    onPickCustomDate: () -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Filter Transactions",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Select filters to refine history view",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }

                TextButton(onClick = onReset) {
                    Text("Reset All", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            // Section 1: Transaction Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Transaction Type", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    typeFilters.forEach { (key, label) ->
                        val isSelected = selectedType == key
                        val chipBg = if (isSelected) AccentTeal.copy(alpha = 0.2f) else CardDarker
                        val chipBorder = if (isSelected) AccentTeal else DividerColor
                        val chipTextColor = if (isSelected) AccentTeal else TextSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable { onSelectType(key) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = label,
                                color = chipTextColor,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Section 2: Time Period
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Time Period", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    periodFilters.forEach { (key, label) ->
                        val isSelected = selectedPeriod == key
                        val chipBg = if (isSelected) AccentBlue.copy(alpha = 0.2f) else CardDarker
                        val chipBorder = if (isSelected) AccentBlue else DividerColor
                        val chipTextColor = if (isSelected) AccentBlue else TextSecondary

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(chipBg)
                                .border(1.dp, chipBorder, RoundedCornerShape(20.dp))
                                .clickable {
                                    if (key == "CUSTOM") {
                                        onPickCustomDate()
                                    } else {
                                        onSelectPeriod(key)
                                    }
                                }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (key == "CUSTOM") {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = chipTextColor, modifier = Modifier.size(14.dp))
                                }
                                Text(
                                    text = label,
                                    color = chipTextColor,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Section 3: Account Filter
            if (accounts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Account / Wallet", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val isAllSelected = selectedAccountId == 0
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isAllSelected) AccentTeal.copy(alpha = 0.2f) else CardDarker)
                                .border(1.dp, if (isAllSelected) AccentTeal else DividerColor, RoundedCornerShape(20.dp))
                                .clickable { onSelectAccount(0) }
                                .padding(horizontal = 14.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text = "All Accounts",
                                color = if (isAllSelected) AccentTeal else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isAllSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }

                        accounts.forEach { acc ->
                            val isSelected = selectedAccountId == acc.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) AccentTeal.copy(alpha = 0.2f) else CardDarker)
                                    .border(1.dp, if (isSelected) AccentTeal else DividerColor, RoundedCornerShape(20.dp))
                                    .clickable { onSelectAccount(acc.id) }
                                    .padding(horizontal = 14.dp, vertical = 7.dp)
                            ) {
                                Text(
                                    text = acc.name,
                                    color = if (isSelected) AccentTeal else TextSecondary,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Apply Button
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = Color.White)
            ) {
                Text("Apply Filters", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}
