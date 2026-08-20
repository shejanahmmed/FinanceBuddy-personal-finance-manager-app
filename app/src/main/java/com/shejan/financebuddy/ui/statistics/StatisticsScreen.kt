package com.shejan.financebuddy.ui.statistics

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.roundToInt
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.cos
import kotlin.math.sin

// ─── Color palette for charts ───────────────────────────────────────────
private val chartPalette = listOf(
    Color(0xFF00D4AA), Color(0xFF0096FF), Color(0xFF7C5CFC),
    Color(0xFFFF5C7C), Color(0xFFFFBD2E), Color(0xFF00C897),
    Color(0xFFFF8C42), Color(0xFF44B4FF), Color(0xFFC97AFF),
    Color(0xFFFF6B6B)
)

@Composable
fun StatisticsScreen(
    allTransactions: List<TransactionEntity>,
    accounts: List<AccountEntity>,
    onBack: () -> Unit
) {
    BackHandler(onBack = onBack)

    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    var selectedPeriod by remember { mutableStateOf("MONTH") }

    val periodTransactions = remember(allTransactions, selectedPeriod) {
        val now = Calendar.getInstance()
        val startTime = when (selectedPeriod) {
            "WEEK" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "MONTH" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_MONTH, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            "YEAR" -> {
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_YEAR, 1)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)
                cal.timeInMillis
            }
            else -> 0L
        }
        allTransactions.filter { it.timestamp >= startTime }
    }

    val totalIncome = remember(periodTransactions) {
        periodTransactions.filter { it.type == "INCOME" }.sumOf { it.amount }
    }
    val totalExpense = remember(periodTransactions) {
        periodTransactions.filter { it.type == "EXPENSE" }.sumOf { it.amount }
    }
    val netSavings = totalIncome - totalExpense
    val savingsRate = if (totalIncome > 0) ((netSavings / totalIncome) * 100).coerceAtLeast(0.0) else 0.0

    val daysInPeriod = remember(selectedPeriod) {
        val cal = Calendar.getInstance()
        when (selectedPeriod) {
            "WEEK" -> 7
            "MONTH" -> cal.get(Calendar.DAY_OF_MONTH)
            "YEAR" -> cal.get(Calendar.DAY_OF_YEAR)
            else -> 30
        }.coerceAtLeast(1)
    }

    val categoryExpenses = remember(periodTransactions) {
        periodTransactions.filter { it.type == "EXPENSE" }
            .groupBy { it.category.ifBlank { "Other" } }
            .mapValues { (_, v) -> v.sumOf { it.amount } }
            .toList().sortedByDescending { it.second }
    }

    // Dynamic Income vs Expense Comparison Bar Data based on selectedPeriod
    val barChartTitle = remember(selectedPeriod) {
        when (selectedPeriod) {
            "WEEK"  -> "Daily Income vs Expense (This Week)"
            "MONTH" -> "Weekly Income vs Expense (This Month)"
            "YEAR"  -> "Monthly Income vs Expense (This Year)"
            else    -> "6-Month Income vs Expense (All Time)"
        }
    }

    val barChartData = remember(allTransactions, selectedPeriod) {
        val now = Calendar.getInstance()
        val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
        val dayNames = listOf("Sun","Mon","Tue","Wed","Thu","Fri","Sat")

        when (selectedPeriod) {
            "WEEK" -> {
                // 7 Days of current week (Mon -> Sun)
                val cal = now.clone() as Calendar
                cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
                cal.set(Calendar.HOUR_OF_DAY, 0); cal.set(Calendar.MINUTE, 0)
                cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0)

                (0..6).map { dayOffset ->
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, dayOffset)
                    val start = c.timeInMillis
                    val cEnd = c.clone() as Calendar
                    cEnd.set(Calendar.HOUR_OF_DAY, 23); cEnd.set(Calendar.MINUTE, 59)
                    cEnd.set(Calendar.SECOND, 59); cEnd.set(Calendar.MILLISECOND, 999)
                    val end = cEnd.timeInMillis

                    val txs = allTransactions.filter { it.timestamp in start..end }
                    val inc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val exp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val dayOfWeek = c.get(Calendar.DAY_OF_WEEK) - 1
                    Triple(dayNames[dayOfWeek], inc, exp)
                }
            }
            "MONTH" -> {
                // 4 Weeks of current month
                val cal = now.clone() as Calendar
                val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
                listOf(
                    "Wk 1" to (1..7),
                    "Wk 2" to (8..14),
                    "Wk 3" to (15..21),
                    "Wk 4" to (22..maxDays)
                ).map { (label, dayRange) ->
                    val cStart = cal.clone() as Calendar
                    cStart.set(Calendar.DAY_OF_MONTH, dayRange.first)
                    cStart.set(Calendar.HOUR_OF_DAY, 0); cStart.set(Calendar.MINUTE, 0)
                    cStart.set(Calendar.SECOND, 0); cStart.set(Calendar.MILLISECOND, 0)
                    val start = cStart.timeInMillis

                    val cEnd = cal.clone() as Calendar
                    cEnd.set(Calendar.DAY_OF_MONTH, dayRange.last)
                    cEnd.set(Calendar.HOUR_OF_DAY, 23); cEnd.set(Calendar.MINUTE, 59)
                    cEnd.set(Calendar.SECOND, 59); cEnd.set(Calendar.MILLISECOND, 999)
                    val end = cEnd.timeInMillis

                    val txs = allTransactions.filter { it.timestamp in start..end }
                    val inc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val exp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    Triple(label, inc, exp)
                }
            }
            "YEAR" -> {
                // 12 Months of current year (Jan..Dec)
                val cal = now.clone() as Calendar
                (0..11).map { month ->
                    val c = cal.clone() as Calendar
                    c.set(Calendar.MONTH, month)
                    c.set(Calendar.DAY_OF_MONTH, 1)
                    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                    val start = c.timeInMillis

                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                    c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
                    c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
                    val end = c.timeInMillis

                    val txs = allTransactions.filter { it.timestamp in start..end }
                    val inc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val exp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    Triple(monthNames[month], inc, exp)
                }
            }
            else -> {
                // ALL: Last 6 months
                val cal = now.clone() as Calendar
                (5 downTo 0).map { offset ->
                    val c = cal.clone() as Calendar
                    c.add(Calendar.MONTH, -offset)
                    c.set(Calendar.DAY_OF_MONTH, 1)
                    c.set(Calendar.HOUR_OF_DAY, 0); c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0); c.set(Calendar.MILLISECOND, 0)
                    val start = c.timeInMillis

                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                    c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
                    c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
                    val end = c.timeInMillis

                    val txs = allTransactions.filter { it.timestamp in start..end }
                    val inc = txs.filter { it.type == "INCOME" }.sumOf { it.amount }
                    val exp = txs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                    val m = c.get(Calendar.MONTH)
                    Triple(monthNames[m], inc, exp)
                }
            }
        }
    }

    // Dynamic Balance Trend Line Chart Header based on selectedPeriod
    val balanceTrendTitle = remember(selectedPeriod) {
        when (selectedPeriod) {
            "WEEK"  -> "Weekly Balance Trend"
            "MONTH" -> "Monthly Balance Trend"
            "YEAR"  -> "Yearly Balance Trend"
            else    -> "All-Time Balance Trend"
        }
    }

    val balanceTrendSubtitle = remember(selectedPeriod) {
        when (selectedPeriod) {
            "WEEK"  -> "Running total balance day-by-day this week"
            "MONTH" -> "Running total balance day-by-day this month"
            "YEAR"  -> "Running total balance month-by-month this year"
            else    -> "Historical running balance across full account activity"
        }
    }

    val balanceTrendXLabels = remember(selectedPeriod, allTransactions) {
        val now = Calendar.getInstance()
        when (selectedPeriod) {
            "WEEK"  -> listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            "MONTH" -> {
                val maxDays = now.getActualMaximum(Calendar.DAY_OF_MONTH)
                listOf("1", "${maxDays / 4}", "${maxDays / 2}", "${(maxDays * 3) / 4}", "$maxDays")
            }
            "YEAR"  -> listOf("Jan", "Mar", "May", "Jul", "Sep", "Nov")
            else    -> {
                val monthNames = listOf("Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec")
                (5 downTo 0).map { offset ->
                    val c = now.clone() as Calendar
                    c.add(Calendar.MONTH, -offset)
                    monthNames[c.get(Calendar.MONTH)]
                }
            }
        }
    }

    val balanceTrendData = remember(allTransactions, accounts, selectedPeriod) {
        val totalCurrentBalance = accounts.sumOf { it.balance }
        val today = Calendar.getInstance()

        when (selectedPeriod) {
            "WEEK" -> {
                // 7 Days of current week (Mon -> Sun)
                val cal = today.clone() as Calendar
                cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
                cal.set(Calendar.HOUR_OF_DAY, 23); cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59); cal.set(Calendar.MILLISECOND, 999)
                val weekEnd = cal.timeInMillis

                val netAfterWeek = allTransactions.filter { it.timestamp > weekEnd }.sumOf { tx ->
                    when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                }
                val balanceAtWeekEnd = totalCurrentBalance - netAfterWeek

                (6 downTo 0).map { dayOffset ->
                    val c = cal.clone() as Calendar
                    c.add(Calendar.DAY_OF_YEAR, -dayOffset)
                    val cutoff = c.timeInMillis
                    val netAfter = allTransactions.filter { it.timestamp > cutoff && it.timestamp <= weekEnd }.sumOf { tx ->
                        when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                    }
                    balanceAtWeekEnd - netAfter
                }
            }
            "MONTH" -> {
                // Days of current month (1 to maxDays of month)
                val maxDays = today.getActualMaximum(Calendar.DAY_OF_MONTH)
                val calMonthEnd = today.clone() as Calendar
                calMonthEnd.set(Calendar.DAY_OF_MONTH, maxDays)
                calMonthEnd.set(Calendar.HOUR_OF_DAY, 23); calMonthEnd.set(Calendar.MINUTE, 59)
                calMonthEnd.set(Calendar.SECOND, 59); calMonthEnd.set(Calendar.MILLISECOND, 999)
                val monthEnd = calMonthEnd.timeInMillis

                val netAfterMonth = allTransactions.filter { it.timestamp > monthEnd }.sumOf { tx ->
                    when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                }
                val balanceAtMonthEnd = totalCurrentBalance - netAfterMonth

                (1..maxDays).map { dayNum ->
                    val c = today.clone() as Calendar
                    c.set(Calendar.DAY_OF_MONTH, dayNum)
                    c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
                    c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
                    val cutoff = c.timeInMillis
                    val netAfter = allTransactions.filter { it.timestamp > cutoff && it.timestamp <= monthEnd }.sumOf { tx ->
                        when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                    }
                    balanceAtMonthEnd - netAfter
                }
            }
            "YEAR" -> {
                // 12 Months of current year (Jan..Dec)
                val yearEnd = today.clone() as Calendar
                yearEnd.set(Calendar.MONTH, 11)
                yearEnd.set(Calendar.DAY_OF_MONTH, 31)
                yearEnd.set(Calendar.HOUR_OF_DAY, 23); yearEnd.set(Calendar.MINUTE, 59)
                yearEnd.set(Calendar.SECOND, 59); yearEnd.set(Calendar.MILLISECOND, 999)
                val yearEndTime = yearEnd.timeInMillis

                val netAfterYear = allTransactions.filter { it.timestamp > yearEndTime }.sumOf { tx ->
                    when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                }
                val balanceAtYearEnd = totalCurrentBalance - netAfterYear

                (0..11).map { month ->
                    val c = today.clone() as Calendar
                    c.set(Calendar.MONTH, month)
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                    c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
                    c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
                    val cutoff = c.timeInMillis
                    val netAfter = allTransactions.filter { it.timestamp > cutoff && it.timestamp <= yearEndTime }.sumOf { tx ->
                        when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                    }
                    balanceAtYearEnd - netAfter
                }
            }
            else -> {
                // ALL TIME: Last 6 months historical trajectory
                (5 downTo 0).map { monthOffset ->
                    val c = today.clone() as Calendar
                    c.add(Calendar.MONTH, -monthOffset)
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH))
                    c.set(Calendar.HOUR_OF_DAY, 23); c.set(Calendar.MINUTE, 59)
                    c.set(Calendar.SECOND, 59); c.set(Calendar.MILLISECOND, 999)
                    val cutoff = c.timeInMillis
                    val netAfter = allTransactions.filter { it.timestamp > cutoff }.sumOf { tx ->
                        when (tx.type) { "INCOME" -> tx.amount; "EXPENSE" -> -tx.amount; else -> 0.0 }
                    }
                    totalCurrentBalance - netAfter
                }
            }
        }
    }

    val topTransactions = remember(periodTransactions) {
        periodTransactions.sortedByDescending { it.amount }.take(5)
    }

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        Box(
            modifier = Modifier.fillMaxWidth().height(240.dp).background(
                Brush.verticalGradient(listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent))
            )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Header ───────────────────────────────────────────────────
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
                        Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = "Back",
                        tint = TextPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Financial Statistics", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Visual analytics & spending insights", color = TextMuted, fontSize = 12.sp)
                }

                // Top-Right Filter Icon Button with Dropdown Menu
                var filterMenuExpanded by remember { mutableStateOf(false) }
                val periodOptions = listOf(
                    "WEEK" to "This Week",
                    "MONTH" to "This Month",
                    "YEAR" to "This Year",
                    "ALL" to "All Time"
                )

                Box {
                    IconButton(
                        onClick = { filterMenuExpanded = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.FilterList,
                            contentDescription = "Filter Timeframe",
                            tint = if (selectedPeriod != "ALL") AccentTeal else TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    ) {
                        periodOptions.forEach { (key, label) ->
                            val isSelected = selectedPeriod == key
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = label,
                                            color = if (isSelected) AccentTeal else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                        )
                                        if (isSelected) {
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected",
                                                tint = AccentTeal,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {
                                    selectedPeriod = key
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            // ── Scrollable Content ─────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION 1: Summary Stack Container (Outer dark ash card containing 4 full-width inner black cards)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = "Overview",
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Financial summary for selected period",
                                color = TextMuted,
                                fontSize = 11.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MetricTile(
                                    title = "Total Income",
                                    value = "৳${currencyFormat.format(totalIncome)}",
                                    icon = Icons.Default.TrendingUp,
                                    color = IncomeGreen,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MetricTile(
                                    title = "Total Expense",
                                    value = "৳${currencyFormat.format(totalExpense)}",
                                    icon = Icons.Default.TrendingDown,
                                    color = ExpenseRed,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MetricTile(
                                    title = "Net Savings",
                                    value = "${if (netSavings >= 0) "+" else ""}৳${currencyFormat.format(netSavings)}",
                                    icon = Icons.Default.AccountBalance,
                                    color = if (netSavings >= 0) AccentTeal else ExpenseRed,
                                    modifier = Modifier.fillMaxWidth()
                                )
                                MetricTile(
                                    title = "Savings Rate",
                                    value = "${String.format(Locale.US, "%.1f", savingsRate)}%",
                                    icon = Icons.Default.Star,
                                    color = AccentBlue,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // SECTION 2: Income vs Expense Breakdown List (Per reference image design)
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Text(
                                text = barChartTitle,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center
                            )

                            Spacer(modifier = Modifier.height(14.dp))

                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                barChartData.forEach { item ->
                                    IncomeExpenseComparisonRow(
                                        label = item.first,
                                        income = item.second,
                                        expense = item.third
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Legend Row at the bottom of the section box
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                LegendDot(IncomeGreen, "Income")
                                Spacer(modifier = Modifier.width(24.dp))
                                LegendDot(ExpenseRed, "Expense")
                            }
                        }
                    }
                }

                // SECTION 3: Balance Trend Line Chart
                item {
                    StatCard(title = balanceTrendTitle, subtitle = balanceTrendSubtitle) {
                        if (balanceTrendData.distinct().size > 1) {
                            BalanceTrendChart(
                                balances = balanceTrendData,
                                xLabels = balanceTrendXLabels,
                                selectedPeriod = selectedPeriod,
                                modifier = Modifier.fillMaxWidth().height(190.dp).padding(top = 8.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier.fillMaxWidth().height(100.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No balance movement detected in this period.", color = TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // SECTION 4: Category Donut Chart
                if (categoryExpenses.isNotEmpty()) {
                    item {
                        StatCard(title = "Expense by Category", subtitle = "Distribution of spending across categories") {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Centered Donut Chart
                                DonutChart(
                                    data = categoryExpenses,
                                    modifier = Modifier.fillMaxWidth().height(210.dp)
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                // Category boxes stacked underneath the chart inside the section card
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    categoryExpenses.take(6).forEachIndexed { index, (cat, amt) ->
                                        val pct = if (totalExpense > 0) (amt / totalExpense * 100) else 0.0
                                        val color = chartPalette[index % chartPalette.size]
                                        Surface(
                                            shape = RoundedCornerShape(12.dp),
                                            color = CardDarker,
                                            border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.5f)),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .clip(CircleShape)
                                                        .background(color)
                                                )
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Text(
                                                    text = cat,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = "${String.format(Locale.US, "%.1f", pct)}%",
                                                    color = AccentTeal,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Spacer(modifier = Modifier.width(12.dp))
                                                Text(
                                                    text = "৳${currencyFormat.format(amt)}",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp,
                                                    fontWeight = FontWeight.Medium
                                                )
                                            }
                                        }
                                    }
                                    if (categoryExpenses.size > 6) {
                                        Text(
                                            "+${categoryExpenses.size - 6} more categories",
                                            color = TextMuted,
                                            fontSize = 11.sp,
                                            modifier = Modifier.padding(start = 4.dp, top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // SECTION 5: Largest Transactions
                if (topTransactions.isNotEmpty()) {
                    item {
                        StatCard(title = "Largest Transactions", subtitle = "Top 5 by amount in selected period") {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                topTransactions.forEach { tx ->
                                    val isIncome = tx.type == "INCOME"
                                    val txColor = if (isIncome) IncomeGreen else if (tx.type == "TRANSFER") TransferYellow else ExpenseRed
                                    val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(tx.timestamp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = CardDarker,
                                        border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.5f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 12.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                modifier = Modifier.weight(1f),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(txColor.copy(alpha = 0.15f)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Icon(
                                                        imageVector = if (isIncome) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                                        contentDescription = null,
                                                        tint = txColor,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(10.dp))
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = tx.category.ifBlank { tx.type },
                                                        color = TextPrimary,
                                                        fontSize = 13.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = dateStr,
                                                        color = TextSecondary,
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                            Text(
                                                text = "${if (isIncome) "+" else "-"}৳${DecimalFormat("##,##,##0.00").format(tx.amount)}",
                                                color = txColor,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Stat Card wrapper ─────────────────────────────────────────────────
@Composable
private fun StatCard(title: String, subtitle: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
            Text(title, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(subtitle, color = TextMuted, fontSize = 11.sp)
            Spacer(modifier = Modifier.height(4.dp))
            content()
        }
    }
}

// ─── KPI Metric tile (Inner Pitch Black Card) ───────────────────────
@Composable
private fun MetricTile(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = CardDarker),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = value,
                    fontSize = 14.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = title,
                    fontSize = 11.sp,
                    lineHeight = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ─── Legend Dot ───────────────────────────────────────────────────────
@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, color = TextSecondary, fontSize = 12.sp, fontWeight = FontWeight.Medium)
    }
}

// ─── Income vs Expense Comparison Row (Matching reference design) ───
@Composable
private fun IncomeExpenseComparisonRow(
    label: String,
    income: Double,
    expense: Double
) {
    val total = income + expense
    val incomeRatio = if (total > 0) (income / total).toFloat() else 0.5f
    val expenseRatio = if (total > 0) (expense / total).toFloat() else 0.5f

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Left Square Box (Label e.g. WK 1, SUN, JAN)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarker),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .width(64.dp)
                .fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label.uppercase(),
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Right Rectangular Box (Progress Bar & Values)
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarker),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)),
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.SpaceEvenly,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Dual Segment Progress Bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E293B))
                ) {
                    if (total > 0) {
                        Row(modifier = Modifier.fillMaxSize()) {
                            if (incomeRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(incomeRatio)
                                        .background(IncomeGreen)
                                )
                            }
                            if (expenseRatio > 0f) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .weight(expenseRatio)
                                        .background(ExpenseRed)
                                )
                            }
                        }
                    }
                }

                // Compact Values Row underneath: ● ৳1000.00 | ● ৳1000.00
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(IncomeGreen)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "৳${String.format(Locale.US, "%.2f", income)}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = "   |   ",
                        color = TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Light
                    )

                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "৳${String.format(Locale.US, "%.2f", expense)}",
                        color = TextPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

// ─── Balance Trend Glow-Gradient Area Chart with Peak/Dip Markers & Touch Scrubbing ───
@Composable
private fun BalanceTrendChart(
    balances: List<Double>,
    xLabels: List<String> = emptyList(),
    selectedPeriod: String = "ALL",
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(balances) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(1100, easing = FastOutSlowInEasing))
    }
    val textMeasurer = rememberTextMeasurer()
    val rawMin = remember(balances) { balances.minOrNull() ?: 0.0 }
    val rawMax = remember(balances) { balances.maxOrNull() ?: 0.0 }
    val peakIdx = remember(balances) { balances.indexOf(rawMax) }
    val dipIdx = remember(balances) { balances.indexOf(rawMin) }

    val minVal = remember(rawMin) {
        if (rawMin >= 0.0) 0.0 else rawMin * 1.15
    }
    val maxVal = remember(rawMax, minVal) {
        (rawMax * 1.15).coerceAtLeast(minVal + 1000.0)
    }
    val range = maxVal - minVal

    var activeTouchIdx by remember { mutableStateOf<Int?>(null) }

    Canvas(
        modifier = modifier
            .pointerInput(balances) {
                detectTapGestures(
                    onPress = { offset ->
                        activeTouchIdx = getClosestIndex(offset.x, size.width.toFloat(), balances.size)
                    }
                )
            }
            .pointerInput(balances) {
                detectDragGestures(
                    onDragStart = { offset ->
                        activeTouchIdx = getClosestIndex(offset.x, size.width.toFloat(), balances.size)
                    },
                    onDragEnd = { activeTouchIdx = null },
                    onDragCancel = { activeTouchIdx = null },
                    onDrag = { change, _ ->
                        activeTouchIdx = getClosestIndex(change.position.x, size.width.toFloat(), balances.size)
                    }
                )
            }
    ) {
        val w = size.width
        val h = size.height
        val leftPad = 52.dp.toPx()
        val rightPad = 16.dp.toPx()
        val topPad = 24.dp.toPx()
        val botPad = 26.dp.toPx()
        val chartW = w - leftPad - rightPad
        val chartH = h - topPad - botPad
        val n = balances.size
        if (n < 2) return@Canvas

        // Grid lines + Y-axis labels
        for (i in 0..4) {
            val y = topPad + chartH * (i / 4f)
            val v = maxVal - range * (i / 4f)
            drawLine(
                color = DividerColor.copy(alpha = 0.35f),
                start = Offset(leftPad, y), end = Offset(w - rightPad, y),
                strokeWidth = 1.dp.toPx()
            )
            val lbl = when {
                v >= 100_000 -> {
                    val lakhs = v / 100_000.0
                    if (lakhs % 1.0 == 0.0) "${lakhs.toInt()}L" else String.format(Locale.US, "%.1fL", lakhs)
                }
                v >= 1_000 -> {
                    val k = v / 1000.0
                    if (k % 1.0 == 0.0) "${k.toInt()}K" else String.format(Locale.US, "%.1fK", k)
                }
                else -> String.format(Locale.US, "%.0f", v)
            }
            drawText(
                textMeasurer, lbl,
                topLeft = Offset(10.dp.toPx(), y - 7.dp.toPx()),
                style = TextStyle(color = ChartLabel, fontSize = 9.sp)
            )
        }

        val xStep = chartW / (n - 1)
        val animN = (animProgress.value * (n - 1)).toInt().coerceIn(0, n - 2) + 1
        val visibleBalances = balances.take(animN + 1)

        fun toPoint(idx: Int, bal: Double): Offset {
            val nx = ((bal - minVal) / range).toFloat()
            return Offset(
                leftPad + idx * xStep,
                topPad + chartH - nx * chartH
            )
        }

        val visPoints = visibleBalances.mapIndexed { i, b -> toPoint(i, b) }

        // Glow gradient fill area underneath curve
        val fillPath = Path()
        if (visPoints.isNotEmpty()) {
            fillPath.moveTo(visPoints[0].x, topPad + chartH)
            fillPath.lineTo(visPoints[0].x, visPoints[0].y)
            for (i in 1 until visPoints.size) {
                val p = visPoints[i - 1]; val c = visPoints[i]
                val cx1 = p.x + (c.x - p.x) / 2f
                fillPath.cubicTo(cx1, p.y, cx1, c.y, c.x, c.y)
            }
            fillPath.lineTo(visPoints.last().x, topPad + chartH)
            fillPath.close()
            drawPath(
                fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(AccentTeal.copy(alpha = 0.35f), Color.Transparent),
                    startY = topPad, endY = topPad + chartH
                )
            )
        }

        // Smooth Bezier line
        val strokePath = Path()
        if (visPoints.isNotEmpty()) {
            strokePath.moveTo(visPoints[0].x, visPoints[0].y)
            for (i in 1 until visPoints.size) {
                val p = visPoints[i - 1]; val c = visPoints[i]
                val cx1 = p.x + (c.x - p.x) / 2f
                strokePath.cubicTo(cx1, p.y, cx1, c.y, c.x, c.y)
            }
        }
        drawPath(
            strokePath,
            brush = Brush.horizontalGradient(
                colors = listOf(AccentTeal, AccentBlue),
                startX = leftPad, endX = w - rightPad
            ),
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        // Draw X-axis bottom labels & baseline
        if (xLabels.isNotEmpty()) {
            val xLabelCount = xLabels.size
            val step = chartW / (xLabelCount - 1).coerceAtLeast(1)
            xLabels.forEachIndexed { i, label ->
                val lx = leftPad + i * step
                val mResult = textMeasurer.measure(
                    text = label,
                    style = TextStyle(color = ChartLabel, fontSize = 9.sp)
                )
                drawText(
                    textMeasurer = textMeasurer,
                    text = label,
                    topLeft = Offset(
                        (lx - mResult.size.width / 2f).coerceIn(leftPad - 5f, w - rightPad - mResult.size.width.toFloat()),
                        topPad + chartH + 6.dp.toPx()
                    ),
                    style = TextStyle(color = ChartLabel, fontSize = 9.sp)
                )
            }
        }

        // X-axis baseline
        drawLine(
            color = DividerColor.copy(alpha = 0.35f),
            start = Offset(leftPad, topPad + chartH),
            end = Offset(w - rightPad, topPad + chartH),
            strokeWidth = 1.dp.toPx()
        )

        // Draw Peak Highlight Marker (Highest Balance)
        if (peakIdx in visPoints.indices && peakIdx != dipIdx) {
            val peakPt = visPoints[peakIdx]
            drawCircle(color = IncomeGreen.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = peakPt)
            drawCircle(color = IncomeGreen, radius = 4.dp.toPx(), center = peakPt)
            drawText(
                textMeasurer,
                text = "Peak: ৳${String.format(Locale.US, "%.0f", rawMax)}",
                topLeft = Offset((peakPt.x - 24.dp.toPx()).coerceIn(leftPad, w - rightPad - 60.dp.toPx()), peakPt.y - 18.dp.toPx()),
                style = TextStyle(color = IncomeGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }

        // Draw Dip Highlight Marker (Lowest Balance)
        if (dipIdx in visPoints.indices && dipIdx != peakIdx) {
            val dipPt = visPoints[dipIdx]
            drawCircle(color = ExpenseRed.copy(alpha = 0.25f), radius = 9.dp.toPx(), center = dipPt)
            drawCircle(color = ExpenseRed, radius = 4.dp.toPx(), center = dipPt)
            drawText(
                textMeasurer,
                text = "Low: ৳${String.format(Locale.US, "%.0f", rawMin)}",
                topLeft = Offset((dipPt.x - 24.dp.toPx()).coerceIn(leftPad, w - rightPad - 60.dp.toPx()), dipPt.y + 4.dp.toPx()),
                style = TextStyle(color = ExpenseRed, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            )
        }

        // Touch Scrubbing Guideline & Centered Multi-Line Tooltip Box
        activeTouchIdx?.let { idx ->
            if (idx in visPoints.indices) {
                val touchPt = visPoints[idx]
                val balValue = balances[idx]
                val dateText = run {
                    val today = Calendar.getInstance()
                    when (selectedPeriod) {
                        "WEEK" -> {
                            val days = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
                            if (idx in days.indices) days[idx] else ""
                        }
                        "MONTH" -> {
                            val c = today.clone() as Calendar
                            val dayNum = idx + 1
                            if (dayNum <= c.getActualMaximum(Calendar.DAY_OF_MONTH)) {
                                c.set(Calendar.DAY_OF_MONTH, dayNum)
                                SimpleDateFormat("MMM d", Locale.getDefault()).format(c.time)
                            } else ""
                        }
                        "YEAR" -> {
                            val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                            if (idx in monthNames.indices) monthNames[idx] else ""
                        }
                        else -> {
                            val c = today.clone() as Calendar
                            c.add(Calendar.MONTH, -(balances.size - 1 - idx))
                            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(c.time)
                        }
                    }
                }
                val amountText = "৳${DecimalFormat("##,##,##0.00").format(balValue)}"

                // Vertical indicator line
                drawLine(
                    color = AccentTeal.copy(alpha = 0.6f),
                    start = Offset(touchPt.x, topPad),
                    end = Offset(touchPt.x, topPad + chartH),
                    strokeWidth = 1.dp.toPx()
                )

                // Outer pulsing ring & center dot
                drawCircle(color = AccentTeal.copy(alpha = 0.35f), radius = 10.dp.toPx(), center = touchPt)
                drawCircle(color = Color.White, radius = 4.dp.toPx(), center = touchPt)

                // Measure text for exact centering inside box
                val mDate = textMeasurer.measure(
                    text = dateText,
                    style = TextStyle(color = AccentTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )
                val mAmount = textMeasurer.measure(
                    text = amountText,
                    style = TextStyle(color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )

                val paddingH = 10.dp.toPx()
                val boxW = maxOf(mDate.size.width, mAmount.size.width) + paddingH * 2f
                val boxH = 30.dp.toPx()

                val tipX = (touchPt.x - boxW / 2f).coerceIn(leftPad, w - rightPad - boxW)
                val tipY = (topPad - boxH - 2.dp.toPx()).coerceAtLeast(2.dp.toPx())

                // Tooltip box background container
                drawRoundRect(
                    color = CardDarker,
                    topLeft = Offset(tipX, tipY),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
                // Glowing border frame
                drawRoundRect(
                    color = AccentTeal.copy(alpha = 0.6f),
                    topLeft = Offset(tipX, tipY),
                    size = Size(boxW, boxH),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx()),
                    style = Stroke(1.dp.toPx())
                )

                // Draw centered Date/Period text (Line 1)
                drawText(
                    textMeasurer = textMeasurer,
                    text = dateText,
                    topLeft = Offset(
                        tipX + (boxW - mDate.size.width) / 2f,
                        tipY + 2.dp.toPx()
                    ),
                    style = TextStyle(color = AccentTeal, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                )

                // Draw centered Amount text (Line 2)
                drawText(
                    textMeasurer = textMeasurer,
                    text = amountText,
                    topLeft = Offset(
                        tipX + (boxW - mAmount.size.width) / 2f,
                        tipY + 14.dp.toPx()
                    ),
                    style = TextStyle(color = TextPrimary, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                )
            }
        }
    }
}

private fun getClosestIndex(touchX: Float, width: Float, count: Int): Int {
    val leftPad = 52.0f
    val rightPad = 16.0f
    val chartW = width - leftPad - rightPad
    if (chartW <= 0 || count < 2) return 0
    val xStep = chartW / (count - 1)
    val idx = ((touchX - leftPad) / xStep).roundToInt()
    return idx.coerceIn(0, count - 1)
}

private data class CalloutCandidate(
    val category: String,
    val color: Color,
    val rimX: Float,
    val rimY: Float,
    val rawY: Float,
    val isRightSide: Boolean
)

// ─── Donut / Pie Chart with Smart Non-Overlapping Leader Line Callouts ─
@Composable
private fun DonutChart(
    data: List<Pair<String, Double>>,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animProgress.snapTo(0f)
        animProgress.animateTo(1f, animationSpec = tween(900, easing = FastOutSlowInEasing))
    }
    val textMeasurer = rememberTextMeasurer()
    val total = data.sumOf { it.second }.coerceAtLeast(1.0)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w / 2f
        val cy = h / 2f

        // Large prominent donut radius leaving room for compact callout text labels
        val availableR = (minOf(w, h) / 2f - 52.dp.toPx()).coerceAtLeast(45.dp.toPx())
        val outerR = availableR
        val innerR = outerR * 0.58f
        val strokeW = outerR - innerR
        val arcR = innerR + strokeW / 2f

        var startAngle = -90f
        val candidates = mutableListOf<CalloutCandidate>()

        data.forEachIndexed { i, (category, value) ->
            val sweep = (value / total).toFloat() * 360f * animProgress.value
            val color = chartPalette[i % chartPalette.size]

            // 1. Draw segment arc
            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = false,
                topLeft = Offset(cx - arcR, cy - arcR),
                size = Size(arcR * 2f, arcR * 2f),
                style = Stroke(width = strokeW, cap = StrokeCap.Butt)
            )

            // Collect callout candidates
            if (sweep >= 3f && animProgress.value > 0.75f) {
                val midAngleRad = Math.toRadians((startAngle + sweep / 2f).toDouble())
                val cosA = Math.cos(midAngleRad).toFloat()
                val sinA = Math.sin(midAngleRad).toFloat()

                val ptRimX = cx + outerR * cosA
                val ptRimY = cy + outerR * sinA
                val isRightSide = cosA >= 0

                candidates.add(CalloutCandidate(category, color, ptRimX, ptRimY, ptRimY, isRightSide))
            }

            startAngle += sweep
        }

        // 2. Smart Vertical Collision Avoidance (guarantees labels never overlap)
        val minGap = 14.dp.toPx()

        fun resolveSideCollisions(items: List<CalloutCandidate>): List<Pair<CalloutCandidate, Float>> {
            if (items.isEmpty()) return emptyList()
            val sorted = items.sortedBy { it.rawY }
            val finalY = sorted.map { it.rawY }.toFloatArray()

            // Pass 1: Push down overlapping items
            for (idx in 1 until sorted.size) {
                if (finalY[idx] < finalY[idx - 1] + minGap) {
                    finalY[idx] = finalY[idx - 1] + minGap
                }
            }

            // Pass 2: Push back up if bottommost extends past canvas bounds
            val maxY = cy + outerR + 10.dp.toPx()
            if (finalY.last() > maxY) {
                finalY[finalY.lastIndex] = maxY
                for (idx in sorted.size - 2 downTo 0) {
                    if (finalY[idx] > finalY[idx + 1] - minGap) {
                        finalY[idx] = finalY[idx + 1] - minGap
                    }
                }
            }

            return sorted.mapIndexed { idx, item -> item to finalY[idx] }
        }

        val rightSide = resolveSideCollisions(candidates.filter { it.isRightSide })
        val leftSide = resolveSideCollisions(candidates.filter { !it.isRightSide })

        // 3. Draw Leader Lines & Text Labels with resolved non-overlapping Y coordinates and smart edge padding
        (rightSide + leftSide).forEach { (candidate, adjustedY) ->
            val isRightSide = candidate.isRightSide
            val kneeLength = 6.dp.toPx()

            val ptOutX = if (isRightSide) cx + outerR + 6.dp.toPx() else cx - outerR - 6.dp.toPx()

            // Compact Callout Text (9.5sp specifically for the graph)
            val mResult = textMeasurer.measure(
                text = candidate.category,
                style = TextStyle(color = TextPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            )

            // Clamp text position so it strictly respects outer card edge padding (min 10dp inside)
            val edgePadding = 10.dp.toPx()
            val textX = if (isRightSide) {
                (ptOutX + kneeLength + 3.dp.toPx()).coerceAtMost(w - edgePadding - mResult.size.width)
            } else {
                (ptOutX - kneeLength - mResult.size.width - 3.dp.toPx()).coerceAtLeast(edgePadding)
            }
            val textY = adjustedY - mResult.size.height / 2f

            // Leader Line starting directly at outer rim edge touching donut segment
            val linePath = Path().apply {
                moveTo(candidate.rimX, candidate.rimY)
                lineTo(ptOutX, adjustedY)
                lineTo(if (isRightSide) textX - 3.dp.toPx() else textX + mResult.size.width + 3.dp.toPx(), adjustedY)
            }
            drawPath(
                path = linePath,
                color = candidate.color,
                style = Stroke(width = 1.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            )

            drawText(
                textMeasurer = textMeasurer,
                text = candidate.category,
                topLeft = Offset(textX, textY),
                style = TextStyle(color = TextPrimary, fontSize = 9.5.sp, fontWeight = FontWeight.Bold)
            )
        }

        // Center hole fill
        drawCircle(color = CardDark, radius = innerR, center = Offset(cx, cy))
    }
}
