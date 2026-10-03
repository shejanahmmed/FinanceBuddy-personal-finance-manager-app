package com.shejan.financebuddy.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LaptopMac
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Surface
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.core.graphics.toColorInt
import com.shejan.financebuddy.ui.notifications.AppNotification
import com.shejan.financebuddy.ui.notifications.NotificationsBottomSheet
import com.shejan.financebuddy.ui.accounts.getShortBankName
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.milliseconds
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.LoanEntity
import com.shejan.financebuddy.data.db.BudgetEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.ui.home.components.BalanceTrendLineChart
import com.shejan.financebuddy.ui.home.components.ExpenseBarChart
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accounts: List<AccountEntity>,
    allTransactions: List<TransactionEntity>,
    monthlyIncome: Double,
    monthlyExpenses: Double,
    onSaveTransaction: (TransactionEntity, AccountEntity?, AccountEntity?) -> Unit,
    onOpenDrawer: () -> Unit,
    onIncomeClick: () -> Unit,
    onExpenseClick: () -> Unit,
    payees: List<PayeeEntity> = emptyList(),
    payeeAccounts: List<PayeeAccountEntity> = emptyList(),
    onSavePayee: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    hideBalancesPref: Boolean = false,
    loans: List<LoanEntity> = emptyList(),
    budgets: List<BudgetEntity> = emptyList(),
    spentByCategory: Map<String, Double> = emptyMap(),
    onNavigateToLoans: () -> Unit = {},
    onNavigateToHistory: () -> Unit = {},
    onNavigateToBudget: () -> Unit = {},
    notifications: List<AppNotification> = emptyList(),
    onNotificationAction: (String) -> Unit = {},
    onMarkAllNotificationsRead: () -> Unit = {},
    onDismissNotification: (String) -> Unit = {}
) {
    var showAddSheet by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val preferencesManager = remember { com.shejan.financebuddy.data.PreferencesManager(context.applicationContext) }
    val pinnedAccountId by preferencesManager.pinnedAccountId.collectAsState(initial = null)
    val scope = rememberCoroutineScope()

    var showNotificationsSheet by remember { mutableStateOf(false) }
    val notificationsSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val unreadNotificationsCount = remember(notifications) { notifications.count { !it.isRead } }

    val recentTransactions = remember(allTransactions) { allTransactions.take(5) }
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val totalBalance   = accounts.sumOf { it.balance }

    val sortedAccounts = remember(accounts, allTransactions) {
        accounts.sortedWith(
            compareByDescending<AccountEntity> { account ->
                allTransactions
                    .filter { it.fromAccountId == account.id || it.toAccountId == account.id }
                    .maxOfOrNull { it.timestamp } ?: 0L
            }.thenBy { it.name }
        )
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
        Scaffold(
            containerColor = Color.Transparent,
            // Empty topBar so we can overlay the floating top bar smoothly
            topBar = {}
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
            ) {
                // Spacer matching top bar height + status bar padding
                Spacer(modifier = Modifier.statusBarsPadding().height(54.dp))

                val context = LocalContext.current
                val preferencesManager = remember { com.shejan.financebuddy.data.PreferencesManager(context.applicationContext) }
                val hideTotalBalance by preferencesManager.hideTotalBalance.collectAsState(initial = false)
                var showTemporarily by remember { mutableStateOf(false) }

                LaunchedEffect(showTemporarily) {
                    if (showTemporarily) {
                        delay(2000.milliseconds)
                        showTemporarily = false
                    }
                }

                // ── 1. Hero Total Balance Card (Merged with Income & Expenses) ───────
                val currentDateText = remember {
                    SimpleDateFormat("MMM d", Locale.getDefault()).format(Date())
                }

                val isLight = currentThemeModeState == "LIGHT"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 0.dp, bottom = 8.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp)
                    ) {
                        // Header Row: "Total Balance" + Date Chip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total Balance",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )

                            Box(
                                modifier = Modifier
                                    .border(
                                        width = 1.dp,
                                        color = DividerColor,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .background(
                                        CardDarker,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = currentDateText,
                                    color = TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Amount Display Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            val balanceStr = "৳${currencyFormat.format(totalBalance)}"
                            val displayText = if (hideTotalBalance && !showTemporarily) {
                                "৳" + balanceStr.substring(1).filter { it != ',' && it != '.' }.map { '*' }.joinToString("")
                            } else {
                                balanceStr
                            }

                            Text(
                                text = displayText,
                                fontSize = 27.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )

                            if (hideTotalBalance) {
                                IconButton(
                                    onClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        showTemporarily = true
                                    },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = if (showTemporarily) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Show/Hide Balance",
                                        tint = TextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Merged Income & Expense Sub-Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Income Sub-Card
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(66.dp)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { onIncomeClick() },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isLight) Color(0xFFECFDF5) else CardDarker,
                                border = BorderStroke(
                                    1.dp,
                                    if (isLight) Color(0xFFA7F3D0) else DividerColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isLight) Color(0xFFD1FAE5)
                                                else IncomeGreen.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowDownward,
                                            contentDescription = null,
                                            tint = if (isLight) Color(0xFF059669) else IncomeGreen,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Income",
                                            color = if (isLight) Color(0xFF047857) else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "৳${currencyFormat.format(monthlyIncome)}",
                                            color = if (isLight) Color(0xFF065F46) else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }

                            // Expenses Sub-Card
                            Surface(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(66.dp)
                                    .clickable(
                                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                                        indication = null
                                    ) { onExpenseClick() },
                                shape = RoundedCornerShape(16.dp),
                                color = if (isLight) Color(0xFFFFF1F2) else CardDarker,
                                border = BorderStroke(
                                    1.dp,
                                    if (isLight) Color(0xFFFECDD3) else DividerColor
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(
                                                if (isLight) Color(0xFFFFE4E6)
                                                else ExpenseRed.copy(alpha = 0.15f)
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowUpward,
                                            contentDescription = null,
                                            tint = if (isLight) Color(0xFFE11D48) else ExpenseRed,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column(
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        Text(
                                            text = "Expenses",
                                            color = if (isLight) Color(0xFFBE123C) else TextSecondary,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Spacer(modifier = Modifier.height(1.dp))
                                        Text(
                                            text = "৳${currencyFormat.format(monthlyExpenses)}",
                                            color = if (isLight) Color(0xFF9F1239) else TextPrimary,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── 2. My Wallets & Accounts Section ──────────────────
                val activeAccounts = remember(sortedAccounts, pinnedAccountId) {
                    if (pinnedAccountId != null) {
                        val pinned = sortedAccounts.filter { it.id == pinnedAccountId }
                        val rest = sortedAccounts.filter { it.id != pinnedAccountId }
                        pinned + rest
                    } else {
                        sortedAccounts
                    }
                }

                // Section Header: Title + Pill Badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 14.dp, end = 14.dp, top = 14.dp, bottom = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Wallets & Accounts",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Box(
                        modifier = Modifier
                            .background(CardDarker, shape = RoundedCornerShape(12.dp))
                            .border(1.dp, DividerColor, shape = RoundedCornerShape(12.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${accounts.size} Accounts",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                if (activeAccounts.isNotEmpty()) {
                    BoxWithConstraints(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val containerWidth = maxWidth
                        val horizontalMargin = 14.dp
                        val spacing = 14.dp

                        val cardWidth = if (activeAccounts.size == 1) {
                            containerWidth - (horizontalMargin * 2)
                        } else {
                            (containerWidth - (horizontalMargin * 2) - spacing) / 2
                        }

                        LazyRow(
                            contentPadding = PaddingValues(horizontal = horizontalMargin, vertical = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(spacing)
                        ) {
                            items(activeAccounts, key = { it.id }) { account ->
                                AccountCardChip(
                                    account = account,
                                    currencyFormat = currencyFormat,
                                    hideBalancesPref = hideBalancesPref,
                                    isPinned = account.id == pinnedAccountId,
                                    onPinToggle = {
                                        scope.launch {
                                            if (account.id == pinnedAccountId) {
                                                preferencesManager.setPinnedAccountId(null)
                                            } else {
                                                preferencesManager.setPinnedAccountId(account.id)
                                            }
                                        }
                                    },
                                    modifier = Modifier.width(cardWidth)
                                )
                            }
                        }
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CardDark)
                            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                            .padding(vertical = 18.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active accounts — add funds via Bank Accounts",
                            color = TextMuted,
                            fontSize = 12.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // ── 3. Expense Graph (Weekly Chart) ───────────────────
                SectionHeader(title = "Weekly Spending", onViewAllClick = onNavigateToHistory)
                Card(
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(205.dp)
                ) {
                    val weeklyExpenses = remember(allTransactions) { getActualWeeklyExpenses(allTransactions) }
                    val hasWeeklyExpenses = remember(weeklyExpenses) { weeklyExpenses.any { it > 0.0 } }
                    if (hasWeeklyExpenses) {
                        ExpenseBarChart(
                            days     = getLast7DayNames(),
                            amounts  = weeklyExpenses,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Dot Grid and Sinusoidal Dashed Curve Background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val spacing = 22.dp.toPx()
                                val dotRadius = 1.1.dp.toPx()
                                val dotColor = if (isDarkModeGlobal) Color.White.copy(alpha = 0.05f) else Color.Black.copy(alpha = 0.04f)

                                var x = spacing / 2f
                                while (x < size.width) {
                                    var y = spacing / 2f
                                    while (y < size.height) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = dotRadius,
                                            center = Offset(x, y)
                                        )
                                        y += spacing
                                    }
                                    x += spacing
                                }

                                // Wavy sinusoidal dashed curve
                                val path = Path().apply {
                                    moveTo(-10f, size.height * 0.65f)
                                    cubicTo(
                                        size.width * 0.22f, size.height * 0.65f,
                                        size.width * 0.35f, size.height * 0.36f,
                                        size.width * 0.50f, size.height * 0.36f
                                    )
                                    cubicTo(
                                        size.width * 0.65f, size.height * 0.36f,
                                        size.width * 0.78f, size.height * 0.65f,
                                        size.width + 10f, size.height * 0.58f
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = AccentTeal.copy(alpha = 0.35f),
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 6.dp.toPx()), 0f)
                                    )
                                )
                            }

                            // Foreground Content: Mini squircle bar icon and descriptive texts
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(54.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(CardDarker)
                                        .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalAlignment = Alignment.Bottom,
                                        modifier = Modifier.height(20.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(10.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(TextMuted)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(20.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(TextMuted)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .width(3.dp)
                                                .height(14.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(TextMuted)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Text(
                                    text = "No spending data for this week.",
                                    fontSize = 15.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Transactions you make will appear here",
                                    fontSize = 13.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 4. Last Recorded Overview ──────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Recent Transactions",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        if (recentTransactions.isNotEmpty()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = CardDarker,
                                border = BorderStroke(1.dp, DividerColor)
                            ) {
                                Text(
                                    text = "Latest ${recentTransactions.take(5).size}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextSecondary,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "View All →",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        modifier = Modifier.clickable { onNavigateToHistory() }
                    )
                }

                if (recentTransactions.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .height(130.dp)
                            .clickable { onNavigateToHistory() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Dot Grid Canvas Background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val spacing = 20.dp.toPx()
                                val dotRadius = 1.dp.toPx()
                                val dotColor = if (isDarkModeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)

                                var x = spacing / 2f
                                while (x < size.width) {
                                    var y = spacing / 2f
                                    while (y < size.height) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = dotRadius,
                                            center = Offset(x, y)
                                        )
                                        y += spacing
                                    }
                                    x += spacing
                                }

                                // Subtle curved decorative dashed accent
                                val path = Path().apply {
                                    moveTo(-10f, size.height * 0.7f)
                                    cubicTo(
                                        size.width * 0.25f, size.height * 0.7f,
                                        size.width * 0.45f, size.height * 0.35f,
                                        size.width * 0.65f, size.height * 0.35f
                                    )
                                    cubicTo(
                                        size.width * 0.80f, size.height * 0.35f,
                                        size.width * 0.90f, size.height * 0.6f,
                                        size.width + 10f, size.height * 0.55f
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = AccentTeal.copy(alpha = 0.25f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
                                    )
                                )
                            }

                            // Foreground Content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardDarker)
                                        .border(1.dp, DividerColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.SwapHoriz,
                                        contentDescription = "Transactions",
                                        tint = AccentTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "No transactions recorded yet",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "Tap to view full transaction history →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentTeal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        recentTransactions.take(5).forEach { tx ->
                            TransactionRowCard(
                                tx             = tx,
                                accounts       = accounts,
                                currencyFormat = currencyFormat,
                                onClick        = onNavigateToHistory
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 5. Balance Trend (Line Chart) ─────────────────────
                val trendBalances = remember(totalBalance, allTransactions) {
                    getActualBalanceTrend(totalBalance, allTransactions)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Balance Trend",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = CardDarker,
                        border = BorderStroke(1.dp, DividerColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(IncomeGreen)
                            )
                            Text(
                                text = "Last 7 Days",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = TextSecondary
                            )
                        }
                    }
                }

                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .height(205.dp)
                ) {
                    val hasTrendData = remember(allTransactions) { allTransactions.isNotEmpty() }
                    if (hasTrendData) {
                        BalanceTrendLineChart(
                            balances = trendBalances,
                            dates    = getLast7DayNames(),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(top = 10.dp, bottom = 12.dp, start = 6.dp, end = 6.dp)
                        )
                    } else {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Dot Grid Canvas Background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val spacing = 22.dp.toPx()
                                val dotRadius = 1.1.dp.toPx()
                                val dotColor = if (isDarkModeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)

                                var x = spacing / 2f
                                while (x < size.width) {
                                    var y = spacing / 2f
                                    while (y < size.height) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = dotRadius,
                                            center = Offset(x, y)
                                        )
                                        y += spacing
                                    }
                                    x += spacing
                                }

                                // Subtle curved decorative dashed accent
                                val path = Path().apply {
                                    moveTo(-10f, size.height * 0.7f)
                                    cubicTo(
                                        size.width * 0.25f, size.height * 0.7f,
                                        size.width * 0.45f, size.height * 0.35f,
                                        size.width * 0.65f, size.height * 0.35f
                                    )
                                    cubicTo(
                                        size.width * 0.80f, size.height * 0.35f,
                                        size.width * 0.90f, size.height * 0.6f,
                                        size.width + 10f, size.height * 0.55f
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = AccentTeal.copy(alpha = 0.25f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
                                    )
                                )
                            }

                            // Foreground Content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CardDarker)
                                        .border(1.dp, DividerColor.copy(alpha = 0.6f), RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.TrendingUp,
                                        contentDescription = "Balance Trend",
                                        tint = AccentTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))

                                Text(
                                    text = "No balance trend data yet",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(4.dp))

                                Text(
                                    text = "Add transactions to see your 7-day balance curve",
                                    fontSize = 12.5.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 6. Monthly Budgets (Spending Limits) ──────────────
                val currentMonthKey = remember { SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) }
                val currentMonthBudgets = remember(budgets, currentMonthKey) {
                    budgets.filter { it.monthYear.isEmpty() || it.monthYear == currentMonthKey }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Monthly Budgets",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "View All →",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        modifier = Modifier.clickable { onNavigateToBudget() }
                    )
                }

                if (currentMonthBudgets.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .height(130.dp)
                            .clickable { onNavigateToBudget() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Dot Grid Canvas Background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val spacing = 20.dp.toPx()
                                val dotRadius = 1.dp.toPx()
                                val dotColor = if (isDarkModeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)

                                var x = spacing / 2f
                                while (x < size.width) {
                                    var y = spacing / 2f
                                    while (y < size.height) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = dotRadius,
                                            center = Offset(x, y)
                                        )
                                        y += spacing
                                    }
                                    x += spacing
                                }

                                // Subtle curved decorative dashed accent
                                val path = Path().apply {
                                    moveTo(-10f, size.height * 0.7f)
                                    cubicTo(
                                        size.width * 0.25f, size.height * 0.7f,
                                        size.width * 0.45f, size.height * 0.35f,
                                        size.width * 0.65f, size.height * 0.35f
                                    )
                                    cubicTo(
                                        size.width * 0.80f, size.height * 0.35f,
                                        size.width * 0.90f, size.height * 0.6f,
                                        size.width + 10f, size.height * 0.55f
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = AccentTeal.copy(alpha = 0.25f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
                                    )
                                )
                            }

                            // Foreground Content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardDarker)
                                        .border(1.dp, DividerColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(3.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .weight(0.55f)
                                                .height(4.5.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(AccentTeal)
                                        )
                                        Box(
                                            modifier = Modifier
                                                .weight(0.45f)
                                                .height(4.5.dp)
                                                .clip(RoundedCornerShape(2.dp))
                                                .background(TextMuted.copy(alpha = 0.35f))
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "No budget limits set for this month",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "Tap to set category spending limits →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentTeal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        currentMonthBudgets.take(3).forEach { budget ->
                            val spent = spentByCategory[budget.category] ?: 0.0
                            val progress = if (budget.limitAmount > 0) (spent / budget.limitAmount).toFloat().coerceIn(0f, 1f) else 0f
                            val overBudget = spent > budget.limitAmount
                            val remaining = (budget.limitAmount - spent).coerceAtLeast(0.0)

                            val accentColor = remember(budget.colorHex) {
                                try { Color(budget.colorHex.toColorInt()) } catch (_: Exception) { AccentTeal }
                            }

                            val barColor = when {
                                progress >= 1f   -> ExpenseRed
                                progress >= 0.9f -> ExpenseRed.copy(alpha = 0.85f)
                                progress >= 0.7f -> TransferYellow
                                else             -> accentColor
                            }

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onNavigateToBudget() },
                                shape = RoundedCornerShape(18.dp),
                                colors = CardDefaults.cardColors(containerColor = CardDark),
                                border = BorderStroke(
                                    1.dp,
                                    if (overBudget) ExpenseRed.copy(alpha = 0.5f) else DividerColor
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(9.dp)
                                                    .clip(CircleShape)
                                                    .background(barColor)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = budget.category,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = TextPrimary
                                            )
                                        }

                                        Text(
                                            text = if (overBudget) "⚠️ Over Budget" else "${(progress * 100).toInt()}%",
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = if (overBudget) ExpenseRed else TextSecondary
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    // Progress bar
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(CardDarker)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(fraction = progress)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(barColor)
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "৳${currencyFormat.format(spent)} / ৳${currencyFormat.format(budget.limitAmount)}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = TextPrimary
                                        )
                                        Text(
                                            text = if (overBudget) {
                                                "Exceeded by ৳${currencyFormat.format(spent - budget.limitAmount)}"
                                            } else {
                                                "৳${currencyFormat.format(remaining)} remaining"
                                            },
                                            fontSize = 12.5.sp,
                                            fontWeight = FontWeight.Normal,
                                            color = if (overBudget) ExpenseRed else TextSecondary
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // ── 7. Loans Overview ──────────────────────────────────
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Loans Overview",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "View All →",
                        fontSize = 12.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = AccentTeal,
                        modifier = Modifier.clickable { onNavigateToLoans() }
                    )
                }

                val totalPrincipal = remember(loans) { loans.sumOf { it.loanAmount } }
                val totalRepaid = remember(loans) { loans.sumOf { it.repaidAmount } }
                val remainingBalance = remember(totalPrincipal, totalRepaid) { (totalPrincipal - totalRepaid).coerceAtLeast(0.0) }
                val overallProgress = remember(totalPrincipal, totalRepaid) {
                    if (totalPrincipal > 0) (totalRepaid / totalPrincipal).toFloat().coerceIn(0f, 1f) else 0f
                }
                val activeLoansCount = remember(loans) {
                    loans.count { (it.loanAmount - it.repaidAmount) > 0.0 }
                }

                if (loans.isEmpty()) {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .height(130.dp)
                            .clickable { onNavigateToLoans() }
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            // Dot Grid Canvas Background
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val spacing = 20.dp.toPx()
                                val dotRadius = 1.dp.toPx()
                                val dotColor = if (isDarkModeGlobal) Color.White.copy(alpha = 0.04f) else Color.Black.copy(alpha = 0.03f)

                                var x = spacing / 2f
                                while (x < size.width) {
                                    var y = spacing / 2f
                                    while (y < size.height) {
                                        drawCircle(
                                            color = dotColor,
                                            radius = dotRadius,
                                            center = Offset(x, y)
                                        )
                                        y += spacing
                                    }
                                    x += spacing
                                }

                                // Subtle curved decorative dashed accent
                                val path = Path().apply {
                                    moveTo(-10f, size.height * 0.7f)
                                    cubicTo(
                                        size.width * 0.25f, size.height * 0.7f,
                                        size.width * 0.45f, size.height * 0.35f,
                                        size.width * 0.65f, size.height * 0.35f
                                    )
                                    cubicTo(
                                        size.width * 0.80f, size.height * 0.35f,
                                        size.width * 0.90f, size.height * 0.6f,
                                        size.width + 10f, size.height * 0.55f
                                    )
                                }

                                drawPath(
                                    path = path,
                                    color = AccentTeal.copy(alpha = 0.25f),
                                    style = Stroke(
                                        width = 1.5.dp.toPx(),
                                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 5.dp.toPx()), 0f)
                                    )
                                )
                            }

                            // Foreground Content
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(horizontal = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardDarker)
                                        .border(1.dp, DividerColor.copy(alpha = 0.6f), RoundedCornerShape(12.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CreditCard,
                                        contentDescription = "Loans",
                                        tint = AccentTeal,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                Text(
                                    text = "No active loans or borrowings",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(3.dp))

                                Text(
                                    text = "Tap to record and manage loans →",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AccentTeal,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                } else {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardDark),
                        border = BorderStroke(1.dp, DividerColor),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { onNavigateToLoans() }
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            // Top: Remaining Payable + Active Loans Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(
                                        text = "Remaining Payable",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        text = "৳${currencyFormat.format(remainingBalance)}",
                                        fontSize = 21.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = AccentTeal.copy(alpha = 0.12f),
                                    border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.35f))
                                ) {
                                    Text(
                                        text = "$activeLoansCount ${if (activeLoansCount == 1) "Active Loan" else "Active Loans"}",
                                        color = AccentTeal,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                                    )
                                }
                            }

                            Spacer(Modifier.height(14.dp))

                            // Middle: Repayment Progress
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Repayment Progress",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = TextSecondary
                                )
                                Text(
                                    text = "${(overallProgress * 100).toInt()}% Repaid",
                                    fontSize = 12.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = AccentTeal
                                )
                            }
                            Spacer(Modifier.height(6.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(CardDarker)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(overallProgress)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(AccentTeal)
                                )
                            }

                            Spacer(Modifier.height(14.dp))

                            HorizontalDivider(color = DividerColor, modifier = Modifier.fillMaxWidth())

                            Spacer(Modifier.height(12.dp))

                            // Bottom: Total Borrowed and Total Repaid
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Total Borrowed",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "৳${currencyFormat.format(totalPrincipal)}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Total Repaid",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = TextSecondary,
                                        textAlign = TextAlign.End
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Text(
                                        text = "৳${currencyFormat.format(totalRepaid)}",
                                        fontSize = 14.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentTeal,
                                        textAlign = TextAlign.End
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(120.dp))
            }
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
                    .background(BackgroundDark)
                    .statusBarsPadding()
                    .padding(horizontal = 14.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Drawer Menu Button (Squircle container)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { onOpenDrawer() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = "Menu",
                        tint = TextPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Title + Green Indicator Dot
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "FinanceBuddy",
                        fontSize = 19.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981))
                    )
                }

                // Right Notification Bell Button (Squircle container)
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(CardDark)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .clickable(
                            interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                            indication = null
                        ) { showNotificationsSheet = true },
                    contentAlignment = Alignment.Center
                ) {
                    if (unreadNotificationsCount > 0) {
                        BadgedBox(
                            badge = {
                                Badge(
                                    containerColor = ExpenseRed,
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotificationsCount > 9) "9+" else unreadNotificationsCount.toString(),
                                        color = Color.White,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    } else {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = TextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Bottom sheet for notifications
        if (showNotificationsSheet) {
            NotificationsBottomSheet(
                notifications = notifications,
                sheetState = notificationsSheetState,
                onDismiss = { showNotificationsSheet = false },
                onNotificationAction = onNotificationAction,
                onMarkAllAsRead = onMarkAllNotificationsRead,
                onDismissNotification = onDismissNotification
            )
        }

        // Bottom sheet for transaction additions
        if (showAddSheet) {
            AddTransactionSheet(
                accounts          = accounts,
                onDismiss         = { showAddSheet = false },
                onSaveTransaction = onSaveTransaction,
                payees            = payees,
                payeeAccounts     = payeeAccounts,
                onSavePayee       = onSavePayee
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Components & Stubs
// ─────────────────────────────────────────────────────────────

@Composable
fun ContactlessWaveGraphic(
    modifier: Modifier = Modifier,
    tint: Color = TextSecondary
) {
    Canvas(modifier = modifier.size(width = 14.dp, height = 14.dp)) {
        val strokeWidth = 1.3.dp.toPx()
        for (i in 1..3) {
            val r = size.width * (0.35f + i * 0.28f)
            drawArc(
                color = tint.copy(alpha = 0.35f + (i * 0.2f)),
                startAngle = -40f,
                sweepAngle = 80f,
                useCenter = false,
                topLeft = Offset(size.width * 0.05f - r / 2, size.height * 0.5f - r / 2),
                size = Size(r, r),
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )
        }
    }
}

@Composable
fun EmvChipGraphic(
    modifier: Modifier = Modifier,
    tint: Color = AccentTeal
) {
    Box(
        modifier = modifier
            .size(width = 20.dp, height = 14.dp)
            .border(1.dp, tint.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .background(tint.copy(alpha = 0.08f), RoundedCornerShape(3.dp)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(1.5.dp)) {
            val stroke = 0.8.dp.toPx()
            val lineColor = tint.copy(alpha = 0.6f)
            drawLine(
                color = lineColor,
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                strokeWidth = stroke
            )
            drawLine(
                color = lineColor,
                start = Offset(size.width * 0.35f, 0f),
                end = Offset(size.width * 0.35f, size.height),
                strokeWidth = stroke
            )
            drawLine(
                color = lineColor,
                start = Offset(size.width * 0.65f, 0f),
                end = Offset(size.width * 0.65f, size.height),
                strokeWidth = stroke
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCardChip(
    account: AccountEntity,
    currencyFormat: DecimalFormat,
    modifier: Modifier = Modifier,
    hideBalancesPref: Boolean = false,
    isPinned: Boolean = false,
    onPinToggle: () -> Unit = {}
) {
    val cardColor = remember(account.colorHex) {
        try { Color(account.colorHex.toColorInt()) } catch (_: Exception) { AccentTeal }
    }
    var isBalanceVisible by remember(hideBalancesPref) { mutableStateOf(!hideBalancesPref) }
    var showMenu by remember { mutableStateOf(false) }

    // Card flip animation state
    var isFlipped by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(
        targetValue = if (isFlipped) 180f else 0f,
        animationSpec = tween(durationMillis = 400, easing = FastOutSlowInEasing),
        label = "CardFlipAnimation"
    )

    LaunchedEffect(isBalanceVisible, hideBalancesPref) {
        if (isBalanceVisible && hideBalancesPref) {
            delay(3000.milliseconds)
            isBalanceVisible = false
        }
    }

    val typeIcon = remember(account.type, account.name) {
        when {
            account.type.equals("CASH", ignoreCase = true) || account.name.contains("Cash", ignoreCase = true) -> Icons.Default.Payments
            account.type.equals("MFS", ignoreCase = true) -> Icons.Default.Payments
            else -> Icons.Default.CreditCard
        }
    }

    val rawSubtype = if (account.name.contains("Cash", ignoreCase = true)) "CASH" else account.accountSubtype.ifBlank { account.type }

    // Solid opaque card gradient based on the account's theme color
    val solidCardBrush = remember(cardColor) {
        val baseDark = Color(0xFF11141E)
        val startColor = Color(
            red = (cardColor.red * 0.45f + baseDark.red * 0.55f).coerceIn(0f, 1f),
            green = (cardColor.green * 0.45f + baseDark.green * 0.55f).coerceIn(0f, 1f),
            blue = (cardColor.blue * 0.45f + baseDark.blue * 0.55f).coerceIn(0f, 1f),
            alpha = 1.0f
        )
        val endColor = Color(
            red = (cardColor.red * 0.16f + baseDark.red * 0.84f).coerceIn(0f, 1f),
            green = (cardColor.green * 0.16f + baseDark.green * 0.84f).coerceIn(0f, 1f),
            blue = (cardColor.blue * 0.16f + baseDark.blue * 0.84f).coerceIn(0f, 1f),
            alpha = 1.0f
        )
        Brush.linearGradient(
            colors = listOf(startColor, endColor),
            start = Offset(0f, 0f),
            end = Offset(350f, 350f)
        )
    }

    Box {
        Card(
            shape   = RoundedCornerShape(12.dp),
            colors  = CardDefaults.cardColors(containerColor = Color.Transparent),
            border  = BorderStroke(1.dp, cardColor.copy(alpha = 0.45f)),
            modifier = modifier
                .height(115.dp)
                .graphicsLayer {
                    rotationY = rotation
                    cameraDistance = 12f * density
                }
                .clip(RoundedCornerShape(12.dp))
                .background(solidCardBrush)
                .drawBehind {
                    // 1. Subtle radial light glow in top-right
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                cardColor.copy(alpha = 0.32f),
                                Color.Transparent
                            ),
                            center = Offset(size.width * 0.88f, size.height * 0.12f),
                            radius = size.width * 0.55f
                        ),
                        center = Offset(size.width * 0.88f, size.height * 0.12f),
                        radius = size.width * 0.55f
                    )

                    // 2. Faded geometric concentric watermark arcs in bottom-right corner
                    val arcCenter = Offset(size.width * 0.95f, size.height * 0.90f)
                    drawCircle(
                        color = Color.White.copy(alpha = 0.05f),
                        radius = size.width * 0.52f,
                        center = arcCenter,
                        style = Stroke(width = 1.2.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.038f),
                        radius = size.width * 0.36f,
                        center = arcCenter,
                        style = Stroke(width = 1.0.dp.toPx())
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.025f),
                        radius = size.width * 0.20f,
                        center = arcCenter,
                        style = Stroke(width = 0.8.dp.toPx())
                    )

                    // 3. Faded curved abstract contour wave lines across card body
                    val wavePath1 = Path().apply {
                        moveTo(-10f, size.height * 0.40f)
                        cubicTo(
                            size.width * 0.30f, size.height * 0.15f,
                            size.width * 0.68f, size.height * 0.80f,
                            size.width + 10f, size.height * 0.48f
                        )
                    }
                    drawPath(
                        path = wavePath1,
                        color = Color.White.copy(alpha = 0.055f),
                        style = Stroke(width = 1.2.dp.toPx())
                    )

                    val wavePath2 = Path().apply {
                        moveTo(-10f, size.height * 0.65f)
                        cubicTo(
                            size.width * 0.35f, size.height * 0.38f,
                            size.width * 0.65f, size.height * 1.02f,
                            size.width + 10f, size.height * 0.72f
                        )
                    }
                    drawPath(
                        path = wavePath2,
                        color = cardColor.copy(alpha = 0.18f),
                        style = Stroke(width = 1.0.dp.toPx())
                    )
                }
                .combinedClickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = { isFlipped = !isFlipped },
                    onLongClick = { showMenu = true }
                )
        ) {
            if (rotation <= 90f) {
                // Front side of card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top Row: Account Icon + Name (left) & Contactless wave / Pin (right)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f, fill = false).padding(end = 4.dp)
                        ) {
                            Icon(
                                imageVector = typeIcon,
                                contentDescription = null,
                                tint = cardColor,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = getShortBankName(account.name, account.type),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isPinned) {
                                Icon(
                                    imageVector = Icons.Default.PushPin,
                                    contentDescription = "Pinned Card",
                                    tint = AccentTeal,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                            ContactlessWaveGraphic(tint = cardColor.copy(alpha = 0.65f))
                        }
                    }

                    // Middle Row: EMV Chip Graphic (Right-aligned)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        EmvChipGraphic(tint = cardColor)
                    }

                    // Bottom Area: Balance + Nickname & Account Number
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        val displayText = if (!hideBalancesPref || isBalanceVisible) {
                            "৳${currencyFormat.format(account.balance)}"
                        } else {
                            "৳••••••"
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = displayText,
                                fontSize = 14.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )

                            if (hideBalancesPref) {
                                IconButton(
                                    onClick = { isBalanceVisible = !isBalanceVisible },
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isBalanceVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = "Toggle Balance Visibility",
                                        tint = TextPrimary.copy(alpha = 0.6f),
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }

                        // Bottom Detail Row: Dot + Nickname / Account Number
                        val last4 = if (account.accountNumber.isNotBlank()) account.accountNumber.takeLast(4) else ""
                        val bottomDetail = when {
                            account.showAs.isNotBlank() && last4.isNotBlank() -> "${account.showAs.uppercase()}  •••• •••• •••• $last4"
                            account.showAs.isNotBlank() -> account.showAs.uppercase()
                            last4.isNotBlank() -> "•••• •••• •••• $last4"
                            else -> (if (account.name.contains("Cash", ignoreCase = true)) "HAND CASH" else getShortBankName(account.name, account.type)).uppercase()
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(cardColor)
                            )
                            Spacer(modifier = Modifier.width(5.dp))
                            Text(
                                text = bottomDetail,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary.copy(alpha = 0.85f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            } else {
                // Back side of card
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { rotationY = 180f }
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    // Top header: Bank name
                    Text(
                        text = getShortBankName(account.name, account.type),
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Back Side details (Acc Type & Acc No)
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = TextSecondary, fontWeight = FontWeight.Medium)) {
                                    append("Type: ")
                                }
                                withStyle(style = SpanStyle(color = cardColor, fontWeight = FontWeight.Bold)) {
                                    append(rawSubtype)
                                }
                            },
                            fontSize = 10.sp
                        )

                        val displayAcc = if (account.accountNumber.isNotBlank()) {
                            val last4 = account.accountNumber.takeLast(4)
                            "**** **** **** $last4"
                        } else {
                            "unknown"
                        }

                        Text(
                            text = buildAnnotatedString {
                                withStyle(style = SpanStyle(color = TextSecondary, fontWeight = FontWeight.Medium)) {
                                    append("Acc No: ")
                                }
                                withStyle(style = SpanStyle(color = TextPrimary, fontWeight = FontWeight.SemiBold)) {
                                    append(displayAcc)
                                }
                            },
                            fontSize = 10.sp
                        )
                    }

                    // Small flip back helper
                    Text(
                        text = "Tap to flip back",
                        fontSize = 8.sp,
                        color = TextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
            shape = RoundedCornerShape(12.dp),
            containerColor = CardDarker,
            modifier = Modifier.border(1.dp, DividerColor, RoundedCornerShape(12.dp))
        ) {
            DropdownMenuItem(
                text = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) ExpenseRed else AccentTeal,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = if (isPinned) "Unpin" else "Pin to First",
                            color = TextPrimary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                onClick = {
                    showMenu = false
                    onPinToggle()
                },
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
            )
        }
    }
}

@Composable
fun TransactionRowCard(
    tx: TransactionEntity,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat,
    onClick: () -> Unit
) {
    val dateOnlyString = remember(tx.timestamp) {
        SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(tx.timestamp))
    }
    val sourceAccount = remember(tx.fromAccountId) {
        accounts.find { it.id == tx.fromAccountId }?.name ?: "Unknown"
    }
    val isLight = currentThemeModeState == "LIGHT"
    val visuals = getTransactionVisuals(tx = tx, accounts = accounts)

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category / Transaction Icon in Squircle
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(visuals.color.copy(alpha = if (isLight) 0.12f else 0.16f))
                    .border(
                        1.dp,
                        visuals.color.copy(alpha = if (isLight) 0.28f else 0.24f),
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = visuals.icon,
                    contentDescription = visuals.title,
                    tint = visuals.color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Details (Title & Subtitle with Date only)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = visuals.title,
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$sourceAccount • $dateOnlyString",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Normal,
                    color = TextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount
            Text(
                text = "${visuals.amountPrefix}৳${currencyFormat.format(tx.amount)}",
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = visuals.amountColor
            )
        }
    }
}

private data class TransactionVisuals(
    val title: String,
    val icon: ImageVector,
    val color: Color,
    val amountPrefix: String,
    val amountColor: Color
)

@Composable
private fun getTransactionVisuals(
    tx: TransactionEntity,
    accounts: List<AccountEntity>
): TransactionVisuals {
    val destAccountName = remember(tx.toAccountId, tx.note) {
        if (tx.toAccountId != null) {
            accounts.find { it.id == tx.toAccountId }?.name ?: "Account"
        } else if (tx.note.startsWith("To: ")) {
            tx.note.removePrefix("To: ").substringBefore(" - ").ifBlank { "Other Person" }
        } else {
            "Other Person"
        }
    }

    val catLower = tx.category.lowercase(Locale.ROOT).trim()

    return when (tx.type) {
        "INCOME" -> {
            val (icon, color) = when {
                catLower.contains("salary") || catLower.contains("paycheck") ->
                    Icons.Default.Payments to IncomeGreen
                catLower.contains("invest") || catLower.contains("dividend") || catLower.contains("profit") || catLower.contains("stock") ->
                    Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF818CF8)
                catLower.contains("bonus") || catLower.contains("award") ->
                    Icons.Default.CardGiftcard to Color(0xFFF59E0B)
                catLower.contains("business") || catLower.contains("sales") ->
                    Icons.Default.Storefront to IncomeGreen
                catLower.contains("freelance") || catLower.contains("gig") ->
                    Icons.Default.LaptopMac to IncomeGreen
                else ->
                    Icons.AutoMirrored.Filled.TrendingUp to IncomeGreen
            }
            TransactionVisuals(
                title = tx.category.ifBlank { "Income" },
                icon = icon,
                color = color,
                amountPrefix = "+",
                amountColor = IncomeGreen
            )
        }
        "EXPENSE" -> {
            val (icon, color) = when {
                catLower.contains("invest") || catLower.contains("stock") || catLower.contains("trading") || catLower.contains("crypto") || catLower.contains("share") ->
                    Icons.AutoMirrored.Filled.TrendingUp to Color(0xFF818CF8)
                catLower.contains("food") || catLower.contains("dining") || catLower.contains("restaurant") || catLower.contains("cafe") || catLower.contains("lunch") || catLower.contains("dinner") || catLower.contains("snack") || catLower.contains("coffee") || catLower.contains("burger") || catLower.contains("pizza") ->
                    Icons.Default.Restaurant to Color(0xFFF43F5E)
                catLower.contains("shop") || catLower.contains("cloth") || catLower.contains("shoe") || catLower.contains("dress") || catLower.contains("market") || catLower.contains("grocery") || catLower.contains("groceries") || catLower.contains("mall") || catLower.contains("heho") ->
                    Icons.Default.ShoppingBag to Color(0xFFF59E0B)
                catLower.contains("tag") || catLower.contains("offer") || catLower.contains("deal") || catLower.contains("discount") || catLower.contains("buy") ->
                    Icons.Default.LocalOffer to Color(0xFFF59E0B)
                catLower.contains("bill") || catLower.contains("utilit") || catLower.contains("electric") || catLower.contains("water") || catLower.contains("gas") || catLower.contains("internet") || catLower.contains("wifi") || catLower.contains("recharge") || catLower.contains("mobile") ->
                    Icons.Default.Bolt to AccentTeal
                catLower.contains("transp") || catLower.contains("car") || catLower.contains("fuel") || catLower.contains("petrol") || catLower.contains("gasoline") || catLower.contains("cng") || catLower.contains("uber") || catLower.contains("pathao") || catLower.contains("bus") || catLower.contains("train") || catLower.contains("flight") ->
                    Icons.Default.DirectionsCar to AccentBlue
                catLower.contains("health") || catLower.contains("medic") || catLower.contains("doctor") || catLower.contains("hospital") || catLower.contains("pharma") ->
                    Icons.Default.MedicalServices to Color(0xFF10B981)
                catLower.contains("entertain") || catLower.contains("movie") || catLower.contains("game") || catLower.contains("cinema") || catLower.contains("theatre") || catLower.contains("netflix") || catLower.contains("stream") ->
                    Icons.Default.Movie to Color(0xFFA855F7)
                catLower.contains("home") || catLower.contains("rent") || catLower.contains("housing") || catLower.contains("flat") || catLower.contains("repair") ->
                    Icons.Default.Home to Color(0xFFEC4899)
                catLower.contains("educat") || catLower.contains("school") || catLower.contains("college") || catLower.contains("university") || catLower.contains("tuition") || catLower.contains("course") || catLower.contains("book") ->
                    Icons.Default.School to Color(0xFF6366F1)
                catLower.contains("loan") || catLower.contains("repay") || catLower.contains("debt") || catLower.contains("emi") ->
                    Icons.Default.CreditCard to AccentTeal
                catLower.contains("travel") || catLower.contains("hotel") || catLower.contains("trip") || catLower.contains("vacation") ->
                    Icons.Default.Flight to Color(0xFF0EA5E9)
                catLower.contains("gift") || catLower.contains("charity") || catLower.contains("donation") || catLower.contains("zakat") ->
                    Icons.Default.CardGiftcard to Color(0xFFEC4899)
                else ->
                    Icons.Default.LocalOffer to Color(0xFFF59E0B)
            }
            TransactionVisuals(
                title = tx.category.ifBlank { "Expense" },
                icon = icon,
                color = color,
                amountPrefix = "-",
                amountColor = ExpenseRed
            )
        }
        else -> {
            TransactionVisuals(
                title = "Transfer to $destAccountName",
                icon = Icons.Default.SwapHoriz,
                color = TransferYellow,
                amountPrefix = "",
                amountColor = TransferYellow
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    onViewAllClick: (() -> Unit)? = null
) {
    if (onViewAllClick != null) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Text(
                text = "View All →",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = AccentTeal,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }
    } else {
        Text(
            text       = title,
            fontSize   = 15.sp,
            fontWeight = FontWeight.Bold,
            color      = TextPrimary,
            modifier   = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )
    }
}

// ── Actual Data Math Helpers ───────────────────────────────

private fun getActualWeeklyExpenses(transactions: List<TransactionEntity>): List<Double> {
    val calendar = Calendar.getInstance()
    val dailySums = DoubleArray(7)

    // Set calendar to end of today
    calendar.set(Calendar.HOUR_OF_DAY, 23)
    calendar.set(Calendar.MINUTE, 59)
    calendar.set(Calendar.SECOND, 59)
    calendar.set(Calendar.MILLISECOND, 999)

    val endOfDays = LongArray(7)
    val startOfDays = LongArray(7)

    for (i in 0..6) {
        val idx = 6 - i // today is index 6, yesterday is 5, etc.
        endOfDays[idx] = calendar.timeInMillis

        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        startOfDays[idx] = calendar.timeInMillis

        // Go to previous day
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        // Reset to end of day for next iteration
        calendar.set(Calendar.HOUR_OF_DAY, 23)
        calendar.set(Calendar.MINUTE, 59)
        calendar.set(Calendar.SECOND, 59)
        calendar.set(Calendar.MILLISECOND, 999)
    }

    for (tx in transactions) {
        if (tx.type == "EXPENSE") {
            for (i in 0..6) {
                if (tx.timestamp in startOfDays[i]..endOfDays[i]) {
                    dailySums[i] += tx.amount
                    break
                }
            }
        }
    }

    return dailySums.toList()
}

private fun getActualBalanceTrend(currentTotalBalance: Double, transactions: List<TransactionEntity>): List<Double> {
    val calendar = Calendar.getInstance()

    // Start of today
    calendar.set(Calendar.HOUR_OF_DAY, 0)
    calendar.set(Calendar.MINUTE, 0)
    calendar.set(Calendar.SECOND, 0)
    calendar.set(Calendar.MILLISECOND, 0)

    val dayStarts = LongArray(7)
    for (i in 0..6) {
        val idx = 6 - i
        dayStarts[idx] = calendar.timeInMillis
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }

    val trend = DoubleArray(7)
    trend[6] = currentTotalBalance

    for (i in 5 downTo 0) {
        val dayDStart = dayStarts[i + 1]
        val dayDEnd = if (i + 2 < 7) dayStarts[i + 2] else Long.MAX_VALUE

        var netChange = 0.0
        for (tx in transactions) {
            if (tx.timestamp in dayDStart until dayDEnd) {
                when (tx.type) {
                    "INCOME" -> netChange += tx.amount
                    "EXPENSE" -> netChange -= tx.amount
                }
            }
        }
        trend[i] = trend[i + 1] - netChange
    }

    return trend.toList()
}

private fun getLast7DayNames(): List<String> {
    val sdf = SimpleDateFormat("EEE", Locale.getDefault())
    val calendar = Calendar.getInstance()
    val names = mutableListOf<String>()
    repeat(7) {
        names.add(sdf.format(calendar.time))
        calendar.add(Calendar.DAY_OF_YEAR, -1)
    }
    return names.reversed()
}
