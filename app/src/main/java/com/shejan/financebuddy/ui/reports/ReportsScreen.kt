package com.shejan.financebuddy.ui.reports

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ReportsScreen(
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val context = LocalContext.current
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val accountsMap = remember(accounts) { accounts.associateBy { it.id } }

    // Report view mode state: "MONTHLY" or "YEARLY"
    var selectedReportType by remember { mutableStateOf("MONTHLY") }

    // Fullscreen table dialog states
    var showFullscreenTable by remember { mutableStateOf(false) }
    var showFullscreenYearlyTable by remember { mutableStateOf(false) }

    // Calendar state for month/year selection
    val selectedCalendar = remember {
        mutableStateOf(Calendar.getInstance().apply {
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        })
    }

    val monthName = remember(selectedCalendar.value.timeInMillis) {
        SimpleDateFormat("MMMM", Locale.getDefault()).format(selectedCalendar.value.time)
    }

    val year = remember(selectedCalendar.value.timeInMillis) {
        selectedCalendar.value.get(Calendar.YEAR)
    }

    // ─── Monthly View Calculations ─────────────────────────────────
    val startOfMonthMillis = remember(selectedCalendar.value.timeInMillis) {
        selectedCalendar.value.timeInMillis
    }

    val endOfMonthMillis = remember(selectedCalendar.value.timeInMillis) {
        val cal = selectedCalendar.value.clone() as Calendar
        cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        cal.timeInMillis
    }

    val monthTransactions = remember(allTransactions, startOfMonthMillis, endOfMonthMillis) {
        allTransactions.filter { it.timestamp in startOfMonthMillis..endOfMonthMillis }
            .sortedByDescending { it.timestamp }
    }

    val totalIncome = remember(monthTransactions) {
        monthTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }

    val totalExpense = remember(monthTransactions) {
        monthTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }

    val totalCurrentBalance = remember(accounts) {
        accounts.sumOf { it.balance }
    }

    val netChangesAfterStart = remember(allTransactions, startOfMonthMillis) {
        allTransactions.filter { it.timestamp >= startOfMonthMillis }.sumOf { tx ->
            when (tx.type) {
                "INCOME" -> tx.amount
                "EXPENSE" -> -tx.amount
                else -> 0.0
            }
        }
    }

    val startingBalance = remember(totalCurrentBalance, netChangesAfterStart) {
        totalCurrentBalance - netChangesAfterStart
    }

    val remainingBalance = remember(startingBalance, totalIncome, totalExpense) {
        startingBalance + totalIncome - totalExpense
    }

    // ─── Yearly View Calculations (All 12 Months for Selected Year) ──
    val yearlyMonthlySummaries = remember(allTransactions, totalCurrentBalance, year) {
        val monthNames = arrayOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        val list = mutableListOf<MonthSummaryData>()

        for (m in 0..11) {
            val cal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, m)
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            val mStart = cal.timeInMillis
            cal.set(Calendar.DAY_OF_MONTH, cal.getActualMaximum(Calendar.DAY_OF_MONTH))
            cal.set(Calendar.HOUR_OF_DAY, 23)
            cal.set(Calendar.MINUTE, 59)
            cal.set(Calendar.SECOND, 59)
            cal.set(Calendar.MILLISECOND, 999)
            val mEnd = cal.timeInMillis

            val mTxs = allTransactions.filter { it.timestamp in mStart..mEnd }
            val mInc = mTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
            val mExp = mTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }

            val mNetAfter = allTransactions.filter { it.timestamp >= mStart }.sumOf { tx ->
                when (tx.type) {
                    "INCOME" -> tx.amount
                    "EXPENSE" -> -tx.amount
                    else -> 0.0
                }
            }
            val mStartBal = totalCurrentBalance - mNetAfter
            val mRemBal = mStartBal + mInc - mExp

            list.add(
                MonthSummaryData(
                    monthName = monthNames[m],
                    startingBalance = mStartBal,
                    totalIncome = mInc,
                    totalExpense = mExp,
                    remainingBalance = mRemBal
                )
            )
        }
        list
    }

    val totalAnnualIncome = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.sumOf { it.totalIncome }
    }

    val totalAnnualExpense = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.sumOf { it.totalExpense }
    }

    val annualStartingBalance = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.firstOrNull()?.startingBalance ?: 0.0
    }

    val annualRemainingBalance = remember(yearlyMonthlySummaries) {
        yearlyMonthlySummaries.lastOrNull()?.remainingBalance ?: 0.0
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient Header Glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ─── Header Top Bar (Matching Bank Accounts Page Design) ─────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
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
                        text = "Financial Report",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Monthly & annual statement overview",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            // ─── Segmented Tab Switch (Monthly vs Yearly) ─────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(CardDarker)
                    .padding(4.dp)
            ) {
                val isMonthly = selectedReportType == "MONTHLY"
                val isYearly = selectedReportType == "YEARLY"

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isMonthly) AccentTeal else Color.Transparent)
                        .clickable { selectedReportType = "MONTHLY" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Monthly Breakdown",
                        color = if (isMonthly) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isMonthly) FontWeight.Bold else FontWeight.Medium
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isYearly) AccentTeal else Color.Transparent)
                        .clickable { selectedReportType = "YEARLY" }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Yearly Summary",
                        color = if (isYearly) Color.White else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = if (isYearly) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }

            // ─── Selector Bar (Month/Year or Year) ───────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = CardDark),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    IconButton(
                        onClick = {
                            val newCal = selectedCalendar.value.clone() as Calendar
                            if (selectedReportType == "MONTHLY") {
                                newCal.add(Calendar.MONTH, -1)
                            } else {
                                newCal.add(Calendar.YEAR, -1)
                            }
                            selectedCalendar.value = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronLeft,
                            contentDescription = "Previous Period",
                            tint = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (selectedReportType == "MONTHLY") "$monthName $year" else "Calendar Year $year",
                            color = AccentTeal,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (selectedReportType == "MONTHLY") "${monthTransactions.size} transactions recorded" else "12 Months Financial History",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            val newCal = selectedCalendar.value.clone() as Calendar
                            if (selectedReportType == "MONTHLY") {
                                newCal.add(Calendar.MONTH, 1)
                            } else {
                                newCal.add(Calendar.YEAR, 1)
                            }
                            selectedCalendar.value = newCal
                        },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = "Next Period",
                            tint = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ─── Content List ─────────────────────────────────────────────
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (selectedReportType == "MONTHLY") {
                    // ───────────────────────────────────────────────────────
                    // MONTHLY VIEW DETAILED TABLE
                    // ───────────────────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp, start = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monthly Transactions Breakdown",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = { showFullscreenTable = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Table to Fullscreen",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardDark)
                                .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier.width(560.dp)
                            ) {
                                // Monthly Table Headers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDarker)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(75.dp))
                                    Text("Category", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(130.dp))
                                    Text("Type", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(85.dp))
                                    Text("Account", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(130.dp))
                                    Text("Amount", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.width(140.dp))
                                }

                                HorizontalDivider(color = DividerColor)

                                if (monthTransactions.isEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(32.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "No transactions found for $monthName $year",
                                            color = TextMuted,
                                            fontSize = 13.sp
                                        )
                                    }
                                } else {
                                    monthTransactions.forEachIndexed { index, tx ->
                                        val isLast = index == monthTransactions.size - 1
                                        val bgColor = if (index % 2 == 0) CardDark else CardDarker.copy(alpha = 0.5f)
                                        val formattedDate = remember(tx.timestamp) { SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.timestamp)) }
                                        val accName = accountsMap[tx.fromAccountId]?.name ?: "Account #${tx.fromAccountId}"

                                        val (typeLabel, typeColor) = when (tx.type) {
                                            "INCOME" -> "Income" to IncomeGreen
                                            "EXPENSE" -> "Expense" to ExpenseRed
                                            else -> "Transfer" to TransferYellow
                                        }

                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(bgColor)
                                                .padding(horizontal = 14.dp, vertical = 11.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(text = formattedDate, color = TextPrimary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, modifier = Modifier.width(75.dp))
                                            Text(text = if (tx.category.isNotBlank()) tx.category else tx.type, color = TextPrimary, fontSize = 11.5.sp, maxLines = 1, softWrap = false, modifier = Modifier.width(130.dp))
                                            Box(modifier = Modifier.width(85.dp)) {
                                                Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.15f)) {
                                                    Text(text = typeLabel, color = typeColor, fontSize = 9.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
                                                }
                                            }
                                            Text(text = accName, color = TextSecondary, fontSize = 11.5.sp, maxLines = 1, softWrap = false, modifier = Modifier.width(130.dp))
                                            val prefix = if (tx.type == "INCOME") "+৳" else if (tx.type == "EXPENSE") "-৳" else "৳"
                                            Text(text = "$prefix${currencyFormat.format(tx.amount)}", color = typeColor, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.width(140.dp))
                                        }
                                        if (!isLast) HorizontalDivider(color = DividerColor.copy(alpha = 0.4f))
                                    }
                                }
                            }
                        }
                    }

                    // Summary Box (Monthly)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Monthly Balance & Statement Summary", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Closing calculation for $monthName $year", color = TextMuted, fontSize = 11.sp)

                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SummaryMetricBox(
                                        title = "🏦 Starting Bank / Accounts Balance",
                                        value = "৳${currencyFormat.format(startingBalance)}",
                                        valueColor = TextPrimary
                                    )

                                    SummaryMetricBox(
                                        title = "📈 Total Income (+)",
                                        value = "+৳${currencyFormat.format(totalIncome)}",
                                        valueColor = IncomeGreen
                                    )

                                    SummaryMetricBox(
                                        title = "📉 Total Expense (-)",
                                        value = "-৳${currencyFormat.format(totalExpense)}",
                                        valueColor = ExpenseRed
                                    )

                                    SummaryMetricBox(
                                        title = "💳 Remaining Balance (Closing)",
                                        value = "৳${currencyFormat.format(remainingBalance)}",
                                        valueColor = AccentTeal,
                                        isHighlight = true
                                    )
                                }
                            }
                        }
                    }

                } else {
                    // ───────────────────────────────────────────────────────
                    // YEARLY VIEW (12 MONTHS SUMMARY TABLE)
                    // ───────────────────────────────────────────────────────
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp, start = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Annual Monthly History ($year)",
                                color = TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            IconButton(
                                onClick = { showFullscreenYearlyTable = true },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Fullscreen,
                                    contentDescription = "Expand Yearly Table to Fullscreen",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(CardDark)
                                .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Column(
                                modifier = Modifier.width(660.dp)
                            ) {
                                // Yearly Table Headers
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDarker)
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Month", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(100.dp))
                                    Text("Starting Bal", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(135.dp))
                                    Text("Income (+)", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(135.dp))
                                    Text("Expense (-)", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, modifier = Modifier.width(135.dp))
                                    Text("Closing Bal", color = TextMuted, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.width(155.dp))
                                }

                                HorizontalDivider(color = DividerColor)

                                yearlyMonthlySummaries.forEachIndexed { index, mData ->
                                    val isLast = index == yearlyMonthlySummaries.size - 1
                                    val bgColor = if (index % 2 == 0) CardDark else CardDarker.copy(alpha = 0.5f)

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor)
                                            .padding(horizontal = 14.dp, vertical = 11.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = mData.monthName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, softWrap = false, modifier = Modifier.width(100.dp))
                                        Text(text = "৳${currencyFormat.format(mData.startingBalance)}", color = TextSecondary, fontSize = 11.5.sp, maxLines = 1, softWrap = false, modifier = Modifier.width(135.dp))
                                        Text(text = "+৳${currencyFormat.format(mData.totalIncome)}", color = IncomeGreen, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, modifier = Modifier.width(135.dp))
                                        Text(text = "-৳${currencyFormat.format(mData.totalExpense)}", color = ExpenseRed, fontSize = 11.5.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false, modifier = Modifier.width(135.dp))
                                        Text(text = "৳${currencyFormat.format(mData.remainingBalance)}", color = AccentTeal, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1, softWrap = false, textAlign = TextAlign.End, modifier = Modifier.width(155.dp))
                                    }
                                    if (!isLast) HorizontalDivider(color = DividerColor.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }

                    // Summary Box (Yearly)
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = CardDark),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Text("Annual Financial Summary ($year)", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Full 12-month consolidated overview", color = TextMuted, fontSize = 11.sp)

                                HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 12.dp))

                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    SummaryMetricBox(
                                        title = "🏦 Year-Start Opening Balance",
                                        value = "৳${currencyFormat.format(annualStartingBalance)}",
                                        valueColor = TextPrimary
                                    )

                                    SummaryMetricBox(
                                        title = "📈 Total Annual Income (+)",
                                        value = "+৳${currencyFormat.format(totalAnnualIncome)}",
                                        valueColor = IncomeGreen
                                    )

                                    SummaryMetricBox(
                                        title = "📉 Total Annual Expenses (-)",
                                        value = "-৳${currencyFormat.format(totalAnnualExpense)}",
                                        valueColor = ExpenseRed
                                    )

                                    SummaryMetricBox(
                                        title = "💳 Year-End Closing Balance",
                                        value = "৳${currencyFormat.format(annualRemainingBalance)}",
                                        valueColor = AccentTeal,
                                        isHighlight = true
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ─── Export as PDF Button (Bottom Action Bar) ────────────────
            Surface(
                color = CardDarker,
                tonalElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Button(
                        onClick = {
                            if (selectedReportType == "MONTHLY") {
                                PdfReportGenerator.exportMonthlyReportPdf(
                                    context = context,
                                    monthName = monthName,
                                    year = year,
                                    transactions = monthTransactions,
                                    accountsMap = accountsMap,
                                    startingBalance = startingBalance,
                                    totalIncome = totalIncome,
                                    totalExpense = totalExpense,
                                    remainingBalance = remainingBalance
                                )
                            } else {
                                PdfReportGenerator.exportYearlyReportPdf(
                                    context = context,
                                    year = year,
                                    monthlySummaries = yearlyMonthlySummaries,
                                    annualStartingBalance = annualStartingBalance,
                                    totalAnnualIncome = totalAnnualIncome,
                                    totalAnnualExpense = totalAnnualExpense,
                                    annualRemainingBalance = annualRemainingBalance
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PictureAsPdf,
                            contentDescription = "Export PDF Icon",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (selectedReportType == "MONTHLY") "Export Monthly Statement as PDF" else "Export Annual Report as PDF",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Fullscreen Monthly Transactions Table Dialog
    if (showFullscreenTable) {
        Dialog(
            onDismissRequest = { showFullscreenTable = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Fullscreen Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Monthly Transactions Breakdown",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "$monthName $year • ${monthTransactions.size} transactions recorded",
                                color = AccentTeal,
                                fontSize = 11.5.sp
                            )
                        }

                        IconButton(
                            onClick = { showFullscreenTable = false },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CardDarker)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DividerColor)

                    // Fullscreen Scrollable Table
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        LazyColumn(
                            modifier = Modifier.width(620.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // Table Headers
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDarker)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Date", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(85.dp))
                                    Text("Category", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                                    Text("Type", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(90.dp))
                                    Text("Account", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(145.dp))
                                    Text("Amount", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(150.dp))
                                }
                                HorizontalDivider(color = DividerColor)
                            }

                            if (monthTransactions.isEmpty()) {
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(48.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("No transactions found for $monthName $year", color = TextMuted, fontSize = 14.sp)
                                    }
                                }
                            } else {
                                itemsIndexed(monthTransactions) { index, tx ->
                                    val isLast = index == monthTransactions.size - 1
                                    val bgColor = if (index % 2 == 0) CardDark else CardDarker.copy(alpha = 0.5f)
                                    val formattedDate = remember(tx.timestamp) { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(tx.timestamp)) }
                                    val accName = accountsMap[tx.fromAccountId]?.name ?: "Account #${tx.fromAccountId}"
                                    val (typeLabel, typeColor) = when (tx.type) {
                                        "INCOME" -> "Income" to IncomeGreen
                                        "EXPENSE" -> "Expense" to ExpenseRed
                                        else -> "Transfer" to TransferYellow
                                    }

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .background(bgColor)
                                            .padding(horizontal = 16.dp, vertical = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = formattedDate, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Medium, modifier = Modifier.width(85.dp))
                                        Text(text = if (tx.category.isNotBlank()) tx.category else tx.type, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(140.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Box(modifier = Modifier.width(90.dp)) {
                                            Surface(shape = RoundedCornerShape(4.dp), color = typeColor.copy(alpha = 0.15f)) {
                                                Text(text = typeLabel, color = typeColor, fontSize = 10.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp))
                                            }
                                        }
                                        Text(text = accName, color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(145.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        val prefix = if (tx.type == "INCOME") "+৳" else if (tx.type == "EXPENSE") "-৳" else "৳"
                                        Text(text = "$prefix${currencyFormat.format(tx.amount)}", color = typeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(150.dp))
                                    }
                                    if (!isLast) HorizontalDivider(color = DividerColor.copy(alpha = 0.4f))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Fullscreen Yearly Monthly History Table Dialog
    if (showFullscreenYearlyTable) {
        Dialog(
            onDismissRequest = { showFullscreenYearlyTable = false },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = BackgroundDark
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                ) {
                    // Fullscreen Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "Annual Monthly History",
                                color = TextPrimary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Calendar Year $year • 12 Months Statement",
                                color = AccentTeal,
                                fontSize = 11.5.sp
                            )
                        }

                        IconButton(
                            onClick = { showFullscreenYearlyTable = false },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(CardDarker)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    HorizontalDivider(color = DividerColor)

                    // Fullscreen Scrollable Table
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .horizontalScroll(rememberScrollState())
                    ) {
                        LazyColumn(
                            modifier = Modifier.width(700.dp),
                            contentPadding = PaddingValues(bottom = 32.dp)
                        ) {
                            // Table Headers
                            item {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDarker)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Month", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
                                    Text("Starting Bal", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                                    Text("Income (+)", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                                    Text("Expense (-)", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                                    Text("Closing Bal", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(170.dp))
                                }
                                HorizontalDivider(color = DividerColor)
                            }

                            itemsIndexed(yearlyMonthlySummaries) { index, mData ->
                                val isLast = index == yearlyMonthlySummaries.size - 1
                                val bgColor = if (index % 2 == 0) CardDark else CardDarker.copy(alpha = 0.5f)

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(bgColor)
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = mData.monthName, color = TextPrimary, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(110.dp))
                                    Text(text = "৳${currencyFormat.format(mData.startingBalance)}", color = TextSecondary, fontSize = 12.sp, modifier = Modifier.width(140.dp))
                                    Text(text = "+৳${currencyFormat.format(mData.totalIncome)}", color = IncomeGreen, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                                    Text(text = "-৳${currencyFormat.format(mData.totalExpense)}", color = ExpenseRed, fontSize = 12.5.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(140.dp))
                                    Text(text = "৳${currencyFormat.format(mData.remainingBalance)}", color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(170.dp))
                                }
                                if (!isLast) HorizontalDivider(color = DividerColor.copy(alpha = 0.4f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryMetricBox(
    title: String,
    value: String,
    valueColor: Color,
    isHighlight: Boolean = false
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isHighlight) CardDarker else CardDarker.copy(alpha = 0.5f))
            .border(
                1.dp,
                if (isHighlight) AccentTeal.copy(alpha = 0.4f) else DividerColor.copy(alpha = 0.4f),
                RoundedCornerShape(12.dp)
            )
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = title,
                color = if (isHighlight) TextPrimary else TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (isHighlight) FontWeight.SemiBold else FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                color = valueColor,
                fontSize = if (isHighlight) 18.sp else 15.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
