package com.shejan.financebuddy.ui.budget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.activity.compose.BackHandler
import com.shejan.financebuddy.ui.common.DiscardChangesDialog
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.window.Dialog
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.BudgetEntity
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.GradientEnd
import com.shejan.financebuddy.ui.theme.GradientStart
import android.content.Context
import androidx.compose.ui.platform.LocalContext
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import com.shejan.financebuddy.ui.theme.TransferYellow
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.foundation.border
import com.shejan.financebuddy.data.db.TransactionEntity
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────
// Budget Screen — per-category monthly spending limits
// ─────────────────────────────────────────────────────────────

private val expenseCategories = listOf(
    "Food", "Groceries", "Rent", "Utilities", "Travel",
    "Shopping", "Entertainment", "Medical", "Other"
)

private val categoryColors = mapOf(
    "Food"           to "#FF5C7C",
    "Groceries"      to "#00C897",
    "Rent"           to "#0096FF",
    "Utilities"      to "#FFBD2E",
    "Travel"         to "#7C5CFC",
    "Shopping"       to "#FF7A45",
    "Entertainment"  to "#00D4AA",
    "Medical"        to "#FF3B6F",
    "Banking"        to "#0096FF",
    "Mobile Banking" to "#00D4AA",
    "Salary"         to "#00C897",
    "Freelance"      to "#7C5CFC",
    "Investment"     to "#FFBD2E",
    "Other"          to "#8A94B2"
)

private val niceColors = listOf(
    "#FF5C7C", "#00C897", "#0096FF", "#FFBD2E", "#7C5CFC",
    "#FF7A45", "#00D4AA", "#FF3B6F", "#E040FB", "#00E5FF",
    "#FFB300", "#1B5E20", "#3F51B5", "#9C27B0", "#00BCD4"
)

private fun getCategoryColor(category: String): String {
    val existingColor = categoryColors[category]
    if (existingColor != null) return existingColor
    val index = Math.abs(category.hashCode()) % niceColors.size
    return niceColors[index]
}

data class MonthOption(
    val offset: Int,
    val label: String,
    val fullLabel: String,
    val monthYear: String,
    val startTimestamp: Long,
    val endTimestamp: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetScreen(
    budgets: List<BudgetEntity>,
    spentByCategory: Map<String, Double> = emptyMap(),
    allTransactions: List<TransactionEntity> = emptyList(),
    onAddBudget: (BudgetEntity) -> Unit,
    onDeleteBudget: (BudgetEntity) -> Unit,
    triggerAddSheet: Boolean = false,
    onResetTriggerAddSheet: () -> Unit = {}
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    val monthOptions = remember {
        val options = mutableListOf<MonthOption>()
        val monthFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        val shortMonthFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthYearFormat = SimpleDateFormat("yyyy-MM", Locale.US)

        for (offset in 0..23) {
            val startCal = Calendar.getInstance().apply {
                set(Calendar.DAY_OF_MONTH, 1)
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                add(Calendar.MONTH, -offset)
            }
            val startTs = startCal.timeInMillis

            val endCal = Calendar.getInstance().apply {
                timeInMillis = startTs
                set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH))
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
                set(Calendar.MILLISECOND, 999)
            }
            val endTs = endCal.timeInMillis

            val monthDate = Date(startTs)
            val isCurrent = offset == 0
            val label = if (isCurrent) "This Month" else shortMonthFormat.format(monthDate)
            val fullLabel = if (isCurrent) "${monthFormat.format(monthDate)} (This Month)" else monthFormat.format(monthDate)
            val monthYear = monthYearFormat.format(monthDate)

            options.add(MonthOption(offset, label, fullLabel, monthYear, startTs, endTs))
        }
        options
    }

    var selectedMonthOption by remember { mutableStateOf(monthOptions.first()) }
    var showMonthDropdown by remember { mutableStateOf(false) }

    val currentMonthBudgets = remember(budgets, selectedMonthOption) {
        val selectedMonthYear = selectedMonthOption.monthYear
        budgets.filter { b ->
            if (b.monthYear.isNotEmpty()) {
                b.monthYear == selectedMonthYear
            } else {
                selectedMonthOption.offset == 0
            }
        }
    }

    val currentSpentByCategory = remember(selectedMonthOption, allTransactions, spentByCategory) {
        if (allTransactions.isNotEmpty()) {
            val map = mutableMapOf<String, Double>()
            val filtered = allTransactions.filter { tx ->
                tx.type.equals("EXPENSE", ignoreCase = true) &&
                tx.timestamp >= selectedMonthOption.startTimestamp &&
                tx.timestamp <= selectedMonthOption.endTimestamp
            }
            for (tx in filtered) {
                val cat = tx.category.trim()
                map[cat] = (map[cat] ?: 0.0) + tx.amount
            }
            map
        } else if (selectedMonthOption.offset == 0) {
            spentByCategory
        } else {
            emptyMap()
        }
    }

    val totalBudgeted  = remember(currentMonthBudgets) { currentMonthBudgets.sumOf { it.limitAmount } }
    val totalSpent     = remember(currentMonthBudgets, currentSpentByCategory) {
        currentMonthBudgets.sumOf { b ->
            currentSpentByCategory.entries.find { it.key.equals(b.category.trim(), ignoreCase = true) }?.value ?: 0.0
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var deletingBudget by remember { mutableStateOf<BudgetEntity?>(null) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(triggerAddSheet) {
        if (triggerAddSheet) {
            editingBudget = null
            showAddSheet = true
            onResetTriggerAddSheet()
        }
    }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Budget Info Dialog
    if (showInfoDialog) {
        BudgetInfoDialog(onDismiss = { showInfoDialog = false })
    }

    // Delete Confirmation Dialog
    deletingBudget?.let { budget ->
        Dialog(onDismissRequest = { deletingBudget = null }) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = CardDark,
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Top Warning Icon Badge
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(CircleShape)
                            .background(ExpenseRed.copy(alpha = 0.15f))
                            .border(1.dp, ExpenseRed.copy(alpha = 0.35f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = null,
                            tint = ExpenseRed,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Delete Budget?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete the spending limit for ${budget.category}?",
                        fontSize = 13.5.sp,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 19.sp
                    )

                    Spacer(modifier = Modifier.height(22.dp))

                    // Centered Equal-Sized Buttons Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Cancel Button
                        Surface(
                            onClick = { deletingBudget = null },
                            shape = RoundedCornerShape(12.dp),
                            color = CardDarker,
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Cancel",
                                    color = TextPrimary,
                                    fontSize = 13.5.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Delete Button
                        Surface(
                            onClick = {
                                onDeleteBudget(budget)
                                deletingBudget = null
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = ExpenseRed,
                            border = BorderStroke(1.dp, ExpenseRed),
                            modifier = Modifier
                                .weight(1f)
                                .height(44.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Delete",
                                    color = Color.White,
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

    var isTopBarVisible by remember { mutableStateOf(true) }
    val nestedScrollConnection = remember {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                if (delta < -12f) {
                    isTopBarVisible = false
                } else if (delta > 12f) {
                    isTopBarVisible = true
                }
                return Offset.Zero
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
            .nestedScroll(nestedScrollConnection)
    ) {
        // Ambient top glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.07f), Color.Transparent)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {}
        ) { innerPadding ->
            LazyColumn(
                modifier            = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding()),
                contentPadding      = PaddingValues(bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                // Spacer matching top bar height (64.dp) + status bar padding
                item {
                    Spacer(modifier = Modifier.statusBarsPadding().height(64.dp))
                }
                // ── Monthly Overview Arc Card ──────────────────────
                item {
                    MonthlyOverviewCard(
                        totalBudgeted  = totalBudgeted,
                        totalSpent     = totalSpent,
                        currencyFormat = currencyFormat,
                        monthLabel     = selectedMonthOption.label,
                        onInfoClick    = { showInfoDialog = true }
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Section header ──────────────────────────────────
                item {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "Spending Limits",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        Text(
                            text  = if (currentMonthBudgets.size == 1) "1 category" else "${currentMonthBudgets.size} categories",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Empty state ─────────────────────────────────────
                if (currentMonthBudgets.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 8.dp)
                                .clip(RoundedCornerShape(20.dp))
                                .background(CardDark)
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = "📊", fontSize = 36.sp)
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text      = if (selectedMonthOption.offset == 0) "No budgets set yet" else "No budgets for ${selectedMonthOption.label}",
                                    color     = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize  = 16.sp
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text      = if (selectedMonthOption.offset == 0) "Tap + Budget above to set a spending limit\nfor each category" else "Tap + Budget above to set a spending limit\nfor ${selectedMonthOption.label}",
                                    color     = TextMuted,
                                    fontSize  = 13.sp,
                                    textAlign = TextAlign.Center,
                                    lineHeight = 20.sp
                                )
                            }
                        }
                    }
                }

                // ── Budget item cards ───────────────────────────────
                items(currentMonthBudgets, key = { it.id }) { budget ->
                    val spent = currentSpentByCategory.entries.find { it.key.equals(budget.category.trim(), ignoreCase = true) }?.value ?: 0.0
                    BudgetItemCard(
                        budget         = budget,
                        spent          = spent,
                        currencyFormat = currencyFormat,
                        onEdit         = {
                            editingBudget = budget
                            showAddSheet = true
                        },
                        onDelete       = { deletingBudget = budget }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // Add / Edit Budget Bottom Sheet
        if (showAddSheet) {
            AddBudgetSheet(
                sheetState         = sheetState,
                existingCategories = currentMonthBudgets.map { it.category },
                budgetToEdit       = editingBudget,
                targetMonthYear    = selectedMonthOption.monthYear,
                monthLabel         = selectedMonthOption.label,
                onDismiss          = {
                    showAddSheet = false
                    editingBudget = null
                },
                onSave             = { budget ->
                    onAddBudget(budget)
                    showAddSheet = false
                    editingBudget = null
                }
            )
        }

        // ── Top Bar Overlay (Translucent and Animated) ───────────────
        AnimatedVisibility(
            visible = isTopBarVisible,
            enter   = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit    = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Budget",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(CardDark)
                                .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                                .clickable { showMonthDropdown = true }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = "Select Month",
                                tint = AccentTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text  = selectedMonthOption.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMonthDropdown,
                            onDismissRequest = { showMonthDropdown = false },
                            modifier = Modifier
                                .background(CardDarker)
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        ) {
                            monthOptions.forEach { option ->
                                val isSelected = option.offset == selectedMonthOption.offset
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = option.fullLabel,
                                                color = if (isSelected) AccentTeal else TextPrimary,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = 13.sp
                                            )
                                            if (isSelected) {
                                                Spacer(Modifier.width(8.dp))
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = null,
                                                    tint = AccentTeal,
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    },
                                    onClick = {
                                        selectedMonthOption = option
                                        showMonthDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentTeal.copy(alpha = 0.15f))
                            .clickable { showAddSheet = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Budget",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Budget",
                                color = AccentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Monthly Overview Arc Card
// ─────────────────────────────────────────────────────────────

@Composable
fun MonthlyOverviewCard(
    totalBudgeted: Double,
    totalSpent: Double,
    currencyFormat: DecimalFormat,
    monthLabel: String = "This Month",
    onInfoClick: () -> Unit = {}
) {
    val progress = if (totalBudgeted > 0) (totalSpent / totalBudgeted).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "MonthlyOverviewProgress"
    )

    val arcColor = when {
        progress >= 1f  -> ExpenseRed
        progress >= 0.9f -> ExpenseRed.copy(alpha = 0.85f)
        progress >= 0.7f -> TransferYellow
        else            -> IncomeGreen
    }

    val remaining = (totalBudgeted - totalSpent).coerceAtLeast(0.0)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        shape  = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            IconButton(
                onClick = onInfoClick,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 10.dp, end = 10.dp)
                    .size(32.dp)
            ) {
                Icon(
                    imageVector        = Icons.Default.Info,
                    contentDescription = "Budget Information",
                    tint               = TextMuted,
                    modifier           = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
            // Arc chart
            Box(
                modifier         = Modifier.size(160.dp),
                contentAlignment = Alignment.Center
            ) {
                androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                    val sweepAngle = animatedProgress * 270f
                    val strokeW    = 18.dp.toPx()

                    // Background arc track
                    drawArc(
                        color      = CardDarker,
                        startAngle = 135f,
                        sweepAngle = 270f,
                        useCenter  = false,
                        style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                    )
                    // Filled arc
                    if (sweepAngle > 0f) {
                        drawArc(
                            brush      = Brush.sweepGradient(
                                listOf(arcColor.copy(alpha = 0.6f), arcColor)
                            ),
                            startAngle = 135f,
                            sweepAngle = sweepAngle,
                            useCenter  = false,
                            style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }
                }

                // Center content
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text       = "${(animatedProgress * 100).toInt()}%",
                        style      = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color      = arcColor
                    )
                    Text(
                        text  = "used",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats row
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                BudgetStatItem(label = "Budgeted",  value = "৳${currencyFormat.format(totalBudgeted)}", color = TextPrimary)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(DividerColor)
                )
                BudgetStatItem(label = "Spent",     value = "৳${currencyFormat.format(totalSpent)}",    color = arcColor)
                Box(
                    modifier = Modifier
                        .width(1.dp)
                        .height(40.dp)
                        .background(DividerColor)
                )
                BudgetStatItem(label = "Remaining", value = "৳${currencyFormat.format(remaining)}",     color = IncomeGreen)
            }
        }
    }
}
}

@Composable
fun BudgetInfoDialog(
    onDismiss: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            color = CardDark,
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "About Monthly Budgets",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Track and plan your category spending limits with precision.",
                    fontSize = 13.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Feature bullet points
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CardDarker)
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    BudgetInfoRow(
                        title = "Monthly Spending Caps",
                        description = "Set customized spending targets for each category to keep your monthly expenses in check."
                    )
                    BudgetInfoRow(
                        title = "Real-Time Tracking",
                        description = "Expenses automatically sync with your logged transactions for the selected month."
                    )
                    BudgetInfoRow(
                        title = "Smart Insights & Alerts",
                        description = "Visual indicators warn you as you approach 80% or exceed your allocated limit."
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action button
                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Got It",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BackgroundDark
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetInfoRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentTeal)
        )
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                color = TextSecondary,
                lineHeight = 16.sp
            )
        }
    }
}

@Composable
private fun BudgetStatItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color      = color
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

// ─────────────────────────────────────────────────────────────
// Budget Item Card — per category with animated progress bar
// ─────────────────────────────────────────────────────────────

@Composable
fun BudgetItemCard(
    budget: BudgetEntity,
    spent: Double,
    currencyFormat: DecimalFormat,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(600),
        label = "BudgetItemProgress"
    )

    val accentColor = remember {
        try { Color(android.graphics.Color.parseColor(budget.colorHex)) }
        catch (e: Exception) { AccentTeal }
    }

    val barColor = when {
        progress >= 1f   -> ExpenseRed
        progress >= 0.9f -> ExpenseRed.copy(alpha = 0.85f)
        progress >= 0.7f -> TransferYellow
        else             -> accentColor
    }

    val overBudget = spent > budget.limitAmount
    val remaining  = (budget.limitAmount - spent).coerceAtLeast(0.0)
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = if (overBudget)
            androidx.compose.foundation.BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.4f))
        else
            androidx.compose.foundation.BorderStroke(1.dp, DividerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier              = Modifier.fillMaxWidth(),
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Category color dot
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .clip(CircleShape)
                            .background(accentColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text       = budget.category,
                        style      = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color      = TextPrimary
                    )
                    if (overBudget) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(ExpenseRed.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text      = "Over Budget",
                                fontSize  = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color     = ExpenseRed
                            )
                        }
                    }
                }
                Box {
                    IconButton(
                        onClick  = { showMenu = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector        = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint               = TextMuted,
                            modifier           = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    ) {
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Edit Budget",
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = null,
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Delete Budget",
                                        color = ExpenseRed,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                onDelete()
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Progress bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(CardDarker)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(
                            Brush.horizontalGradient(
                                colors = if (overBudget)
                                    listOf(ExpenseRed.copy(alpha = 0.7f), ExpenseRed)
                                else
                                    listOf(accentColor.copy(alpha = 0.7f), barColor)
                            )
                        )
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text  = "৳${currencyFormat.format(spent)} spent",
                    style = MaterialTheme.typography.labelSmall,
                    color = barColor
                )
                Text(
                    text  = if (overBudget)
                        "৳${currencyFormat.format(spent - budget.limitAmount)} over"
                    else
                        "৳${currencyFormat.format(remaining)} left of ৳${currencyFormat.format(budget.limitAmount)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (overBudget) ExpenseRed else TextSecondary
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Add / Edit Budget Bottom Sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddBudgetSheet(
    sheetState: androidx.compose.material3.SheetState,
    existingCategories: List<String>,
    budgetToEdit: BudgetEntity? = null,
    targetMonthYear: String = "",
    monthLabel: String = "This Month",
    onDismiss: () -> Unit,
    onSave: (BudgetEntity) -> Unit
) {
    val context = LocalContext.current
    val sharedPreferences = remember { context.getSharedPreferences("finance_buddy_prefs", Context.MODE_PRIVATE) }
    val defaultExpenseCategories = listOf("Food", "Groceries", "Rent", "Utilities", "Travel", "Shopping", "Entertainment", "Medical", "Other")

    val allExpenseCategories = remember {
        val saved = sharedPreferences.getString("active_expense_categories", null)
        if (saved != null) {
            saved.split("|").filter { it.isNotEmpty() }
        } else {
            val custom = sharedPreferences.getString("custom_expense_categories", "")
                ?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
            (defaultExpenseCategories + custom).distinct()
        }
    }

    // Available categories = those not already budgeted, or the one being edited
    val available = remember(allExpenseCategories, existingCategories, budgetToEdit) {
        if (budgetToEdit != null) {
            allExpenseCategories.filter { it.equals(budgetToEdit.category, ignoreCase = true) || it !in existingCategories }
        } else {
            allExpenseCategories.filter { it !in existingCategories }
        }
    }

    val initialCategory = remember(budgetToEdit, available) {
        budgetToEdit?.category ?: (if (available.isNotEmpty()) available.first() else "")
    }
    val initialAmount = remember(budgetToEdit) {
        budgetToEdit?.limitAmount?.let {
            if (it % 1.0 == 0.0) it.toInt().toString() else it.toString()
        } ?: ""
    }

    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory) }
    var limitAmount      by remember(initialAmount) { mutableStateOf(initialAmount) }
    var error            by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(limitAmount, selectedCategory, initialAmount, initialCategory) {
        if (budgetToEdit != null) {
            limitAmount != initialAmount || selectedCategory != initialCategory
        } else {
            limitAmount.trim().isNotEmpty()
        }
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = "Discard Budget Changes?",
            message = "Are you sure you want to discard your changes? Entered amount will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    // If all categories are budgeted and not editing, close the sheet
    LaunchedEffect(available, budgetToEdit) {
        if (available.isEmpty() && budgetToEdit == null) onDismiss()
        else if (selectedCategory !in available && available.isNotEmpty()) selectedCategory = available.first()
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (isFormDirty) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        },
        sheetState       = sheetState,
        containerColor   = CardDarker,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text      = if (budgetToEdit != null) "Edit Budget Limit ($monthLabel)" else "Set Budget Limit ($monthLabel)",
                style     = MaterialTheme.typography.titleLarge,
                color     = TextPrimary,
                modifier  = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Category selector chips
            Text(
                text  = "Category",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))

            androidx.compose.foundation.layout.FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                available.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    val catColor = try {
                        Color(android.graphics.Color.parseColor(getCategoryColor(cat)))
                    } catch (e: Exception) { AccentTeal }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) catColor.copy(alpha = 0.2f) else CardDark
                            )
                            .then(
                                if (isSelected) Modifier.then(
                                    Modifier.clip(RoundedCornerShape(12.dp))
                                ) else Modifier
                            )
                            .clickable { selectedCategory = cat }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text       = cat,
                            fontSize   = 13.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color      = if (isSelected) catColor else TextSecondary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Amount input
            Text(
                text  = "Monthly Limit (৳)",
                style = MaterialTheme.typography.labelMedium,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value         = limitAmount,
                onValueChange = { limitAmount = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("e.g. 5000", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentTeal,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor     = TextPrimary,
                    unfocusedTextColor   = TextPrimary,
                    cursorColor          = AccentTeal
                )
            )

            if (error != null) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = error!!, color = ExpenseRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save button
            Button(
                onClick = {
                    val amount = limitAmount.toDoubleOrNull()
                    when {
                        amount == null || amount <= 0 -> error = "Please enter a valid amount"
                        else -> {
                            onSave(
                                BudgetEntity(
                                    id          = budgetToEdit?.id ?: 0,
                                    category    = selectedCategory,
                                    limitAmount = amount,
                                    colorHex    = getCategoryColor(selectedCategory),
                                    monthYear   = budgetToEdit?.monthYear?.ifEmpty { targetMonthYear } ?: targetMonthYear
                                )
                            )
                        }
                    }
                },
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape          = RoundedCornerShape(16.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = if (budgetToEdit != null) "Update Budget" else "Save Budget",
                        style = MaterialTheme.typography.titleMedium,
                        color = BackgroundDark
                    )
                }
            }
        }
    }
}
