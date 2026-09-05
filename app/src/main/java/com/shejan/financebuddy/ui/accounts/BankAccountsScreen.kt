package com.shejan.financebuddy.ui.accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.draw.rotate
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.activity.compose.BackHandler
import com.shejan.financebuddy.ui.common.DiscardChangesDialog
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat

private val PRESET_CASH = listOf("Hand Cash", "Petty Cash", "Wallet Cash")
private val PRESET_BANKS = listOf(
    "BRAC Bank PLC", "The City Bank PLC", "Eastern Bank PLC (EBL)",
    "Dutch-Bangla Bank PLC (DBBL)", "Prime Bank PLC", "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)", "Al-Arafah Islami Bank PLC",
    "Shahjalal Islami Bank PLC", "Sonali Bank PLC", "Janata Bank PLC",
    "Agrani Bank PLC", "Rupali Bank PLC", "Trust Bank PLC",
    "One Bank PLC", "Meghna Bank PLC", "NRB Bank PLC"
)
private val PRESET_MFS = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash"
)
private val ACCOUNT_SUBTYPES = listOf("Savings", "Current", "Salary", "Student", "Business", "Islamic", "Personal", "Merchant", "Agent", "In Hand", "Wallet", "Petty Cash", "Other")

private val BANK_COLOR_MAP = mapOf(
    "Hand Cash" to "#10B981",
    "Petty Cash" to "#059669",
    "Wallet Cash" to "#34D399",
    "BRAC Bank PLC" to "#0096FF",
    "The City Bank PLC" to "#007A33",
    "Eastern Bank PLC (EBL)" to "#003366",
    "Dutch-Bangla Bank PLC (DBBL)" to "#7C5CFC",
    "Prime Bank PLC" to "#FF5722",
    "Mutual Trust Bank PLC" to "#0C2340",
    "Islami Bank Bangladesh PLC (IBBL)" to "#1B5E20",
    "Al-Arafah Islami Bank PLC" to "#2E7D32",
    "Shahjalal Islami Bank PLC" to "#008080",
    "bKash" to "#FF5C7C",
    "Nagad" to "#FFBD2E",
    "Rocket" to "#00D4AA",
    "Upay" to "#FFB300",
    "CellFin (IBBL)" to "#4CAF50",
    "Ok Wallet" to "#FF5722",
    "MyCash" to "#3F51B5"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankAccountsScreen(
    accounts: List<AccountEntity>,
    onBack: () -> Unit,
    onAddAccount: (AccountEntity) -> Unit,
    onUpdateAccount: (AccountEntity) -> Unit,
    onDeleteAccount: (AccountEntity) -> Unit
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }
    val cash  = remember(accounts) { accounts.filter { it.type == "CASH" || it.name.contains("Cash", ignoreCase = true) } }
    val banks = remember(accounts) { accounts.filter { it.type == "BANK" && !it.name.contains("Cash", ignoreCase = true) } }
    val mfs   = remember(accounts) { accounts.filter { it.type == "MFS"  && !it.name.contains("Cash", ignoreCase = true) } }
    val totalBalance = remember(accounts) { accounts.sumOf { it.balance } }
    val totalCashBalance = remember(cash) { cash.sumOf { it.balance } }
    val totalBankBalance = remember(banks) { banks.sumOf { it.balance } }
    val totalMfsBalance = remember(mfs) { mfs.sumOf { it.balance } }

    val groupedCash = remember(cash) {
        cash.groupBy { it.name }.map { (name, accList) ->
            GroupedAccount(
                name = name,
                type = "CASH",
                colorHex = accList.firstOrNull()?.colorHex ?: BANK_COLOR_MAP[name] ?: "#10B981",
                accounts = accList
            )
        }
    }
    val groupedBanks = remember(banks) {
        banks.groupBy { it.name }.map { (name, accList) ->
            GroupedAccount(
                name = name,
                type = "BANK",
                colorHex = accList.firstOrNull()?.colorHex ?: BANK_COLOR_MAP[name] ?: "#0096FF",
                accounts = accList
            )
        }
    }
    val groupedMfs = remember(mfs) {
        mfs.groupBy { it.name }.map { (name, accList) ->
            GroupedAccount(
                name = name,
                type = "MFS",
                colorHex = accList.firstOrNull()?.colorHex ?: BANK_COLOR_MAP[name] ?: "#FF5C7C",
                accounts = accList
            )
        }
    }

    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<AccountEntity?>(null) }
    var deletingAccounts by remember { mutableStateOf<List<AccountEntity>?>(null) }
    var deleteDialogTitle by remember { mutableStateOf("Delete Account?") }
    var deleteDialogMessage by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    deletingAccounts?.let { targetAccounts ->
        Dialog(onDismissRequest = { deletingAccounts = null }) {
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
                        text = deleteDialogTitle,
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = deleteDialogMessage,
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
                            onClick = { deletingAccounts = null },
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
                                targetAccounts.forEach { onDeleteAccount(it) }
                                deletingAccounts = null
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

    Box(modifier = Modifier.fillMaxSize().background(BackgroundDark)) {
        // Ambient background glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentTeal.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // Clean Top Bar with Top Right '+' Square Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onBack() }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                            contentDescription = "Back",
                            tint = TextPrimary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "Bank Accounts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Manage your wallets & accounts",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }

                // Add Account '+' Button in a Compact Square Box (shifted slightly left)
                Box(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(28.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(CardDarker)
                        .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                        .clickable {
                            editingAccount = null
                            showAddSheet = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Account",
                        tint = AccentTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            if (accounts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(CardDarker)
                                .border(1.dp, DividerColor, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = AccentTeal.copy(alpha = 0.7f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No accounts linked yet",
                            color = TextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap + below to add your first Bank or MFS",
                            color = TextMuted,
                            fontSize = 13.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Hero Total Balance Summary Card
                    item {
                        AccountsHeroCard(
                            totalBalance = totalBalance,
                            cashBalance = totalCashBalance,
                            bankBalance = totalBankBalance,
                            mfsBalance = totalMfsBalance,
                            cashCount = cash.size,
                            bankCount = banks.size,
                            mfsCount = mfs.size,
                            currencyFormat = currencyFormat
                        )
                    }

                    if (cash.isNotEmpty()) {
                        item { SectionGroupHeader(title = "Hand Cash", showCount = false) }
                        items(groupedCash, key = { it.name }) { group ->
                            GroupedAccountManageCard(
                                group = group,
                                currencyFormat = currencyFormat,
                                onEdit = { account -> editingAccount = account; showAddSheet = true },
                                onDelete = { account ->
                                    deletingAccounts = listOf(account)
                                    val name = account.showAs.ifBlank { account.name }
                                    deleteDialogTitle = "Delete Account?"
                                    deleteDialogMessage = "Are you sure you want to remove \"$name\"? Existing transactions will remain intact."
                                },
                                onDeleteAll = { accountsToDelete ->
                                    deletingAccounts = accountsToDelete
                                    deleteDialogTitle = "Delete Bank Card?"
                                    deleteDialogMessage = "Are you sure you want to delete all ${accountsToDelete.size} accounts under \"${group.name}\"? Existing transactions will remain intact."
                                },
                                onAddAnother = { name, type ->
                                    editingAccount = AccountEntity(
                                        id = 0,
                                        name = name,
                                        type = type,
                                        balance = 0.0,
                                        colorHex = BANK_COLOR_MAP[name] ?: "#10B981"
                                    )
                                    showAddSheet = true
                                }
                            )
                        }
                    }

                    if (banks.isNotEmpty()) {
                        item {
                            if (cash.isNotEmpty()) Spacer(Modifier.height(4.dp))
                            SectionGroupHeader(title = "Banks", count = banks.size)
                        }
                        items(groupedBanks, key = { it.name }) { group ->
                            GroupedAccountManageCard(
                                group = group,
                                currencyFormat = currencyFormat,
                                onEdit = { account -> editingAccount = account; showAddSheet = true },
                                onDelete = { account ->
                                    deletingAccounts = listOf(account)
                                    val name = account.showAs.ifBlank { account.name }
                                    deleteDialogTitle = "Delete Account?"
                                    deleteDialogMessage = "Are you sure you want to remove \"$name\"? Existing transactions will remain intact."
                                },
                                onDeleteAll = { accountsToDelete ->
                                    deletingAccounts = accountsToDelete
                                    deleteDialogTitle = "Delete Bank Card?"
                                    deleteDialogMessage = "Are you sure you want to delete all ${accountsToDelete.size} accounts under \"${group.name}\"? Existing transactions will remain intact."
                                },
                                onAddAnother = { name, type ->
                                    editingAccount = AccountEntity(
                                        id = 0,
                                        name = name,
                                        type = type,
                                        balance = 0.0,
                                        colorHex = BANK_COLOR_MAP[name] ?: "#0096FF"
                                    )
                                    showAddSheet = true
                                }
                            )
                        }
                    }

                    if (mfs.isNotEmpty()) {
                        item {
                            Spacer(Modifier.height(4.dp))
                            SectionGroupHeader(title = "MFS", count = mfs.size)
                        }
                        items(groupedMfs, key = { it.name }) { group ->
                            GroupedAccountManageCard(
                                group = group,
                                currencyFormat = currencyFormat,
                                onEdit = { account -> editingAccount = account; showAddSheet = true },
                                onDelete = { account ->
                                    deletingAccounts = listOf(account)
                                    val name = account.showAs.ifBlank { account.name }
                                    deleteDialogTitle = "Delete Account?"
                                    deleteDialogMessage = "Are you sure you want to remove \"$name\"? Existing transactions will remain intact."
                                },
                                onDeleteAll = { accountsToDelete ->
                                    deletingAccounts = accountsToDelete
                                    deleteDialogTitle = "Delete Bank Card?"
                                    deleteDialogMessage = "Are you sure you want to delete all ${accountsToDelete.size} accounts under \"${group.name}\"? Existing transactions will remain intact."
                                },
                                onAddAnother = { name, type ->
                                    editingAccount = AccountEntity(
                                        id = 0,
                                        name = name,
                                        type = type,
                                        balance = 0.0,
                                        colorHex = BANK_COLOR_MAP[name] ?: "#FF5C7C"
                                    )
                                    showAddSheet = true
                                }
                            )
                        }
                    }

                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }

    if (showAddSheet) {
        AccountFormSheet(
            existingAccount = editingAccount,
            onDismiss = {
                showAddSheet = false
                editingAccount = null
            },
            onSave = { account ->
                if (editingAccount != null && editingAccount!!.id != 0) onUpdateAccount(account) else onAddAccount(account)
                showAddSheet = false
                editingAccount = null
            }
        )
    }
}

@Composable
private fun AccountsHeroCard(
    totalBalance: Double,
    cashBalance: Double,
    bankBalance: Double,
    mfsBalance: Double,
    cashCount: Int,
    bankCount: Int,
    mfsCount: Int,
    currencyFormat: DecimalFormat
) {
    var isExpanded by remember { mutableStateOf(false) }
    val arrowRotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "ArrowRotation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    stiffness = Spring.StiffnessMediumLow,
                    dampingRatio = Spring.DampingRatioNoBouncy
                )
            )
            .clickable { isExpanded = !isExpanded },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(
            1.dp,
            Brush.horizontalGradient(
                listOf(AccentTeal.copy(alpha = 0.35f), AccentBlue.copy(alpha = 0.15f))
            )
        )
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                AccentTeal.copy(alpha = 0.08f),
                                AccentPurple.copy(alpha = 0.04f),
                                Color.Transparent
                            )
                        )
                    )
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TOTAL NET BALANCE",
                            color = TextSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.8.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(20.dp))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${cashCount + bankCount + mfsCount} Linked",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                val context = LocalContext.current
                val preferencesManager = remember { com.shejan.financebuddy.data.PreferencesManager(context.applicationContext) }
                val hideTotalBalance by preferencesManager.hideTotalBalance.collectAsState(initial = false)
                var showTemporarily by remember { mutableStateOf(false) }

                LaunchedEffect(showTemporarily) {
                    if (showTemporarily) {
                        kotlinx.coroutines.delay(2000)
                        showTemporarily = false
                    }
                }

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
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )

                    if (hideTotalBalance) {
                        IconButton(
                            onClick = { showTemporarily = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (showTemporarily) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Show/Hide Balance",
                                tint = AccentTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DividerColor.copy(alpha = 0.6f))
                        Spacer(modifier = Modifier.height(14.dp))

                        // Single Column of separate boxes for Cash, Banks, MFS
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // 1. Cash Box
                            AccountCategorySummaryBox(
                                icon = Icons.Default.Payments,
                                iconColor = IncomeGreen,
                                title = "Hand Cash",
                                subtitle = "Physical Cash",
                                balanceStr = "৳${currencyFormat.format(cashBalance)}"
                            )

                            // 2. Banks Box
                            AccountCategorySummaryBox(
                                icon = Icons.Default.AccountBalance,
                                iconColor = AccentBlue,
                                title = "Bank Accounts",
                                subtitle = "$bankCount ${if (bankCount == 1) "account" else "accounts"}",
                                balanceStr = "৳${currencyFormat.format(bankBalance)}"
                            )

                            // 3. MFS Box
                            AccountCategorySummaryBox(
                                icon = Icons.Default.PhoneAndroid,
                                iconColor = ExpenseRed,
                                title = "MFS",
                                subtitle = "$mfsCount ${if (mfsCount == 1) "account" else "accounts"}",
                                balanceStr = "৳${currencyFormat.format(mfsBalance)}"
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Expand/Collapse Chevron Indicator in bottom-center
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = AccentTeal,
                        modifier = Modifier
                            .size(24.dp)
                            .rotate(arrowRotation)
                    )
                }
            }
        }
    }
}

@Composable
private fun AccountCategorySummaryBox(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    balanceStr: String
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDarker,
        border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = title,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = subtitle,
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Text(
                text = balanceStr,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}

@Composable
private fun SectionGroupHeader(title: String, count: Int = 0, showCount: Boolean = true) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(4.dp, 16.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(AccentTeal)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text = title,
            color = TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
        if (showCount) {
            Spacer(Modifier.width(8.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentTeal.copy(alpha = 0.12f))
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = count.toString(),
                    color = AccentTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

private data class GroupedAccount(
    val name: String,
    val type: String,
    val colorHex: String,
    val accounts: List<AccountEntity>
) {
    val totalBalance: Double get() = accounts.sumOf { it.balance }
    val count: Int get() = accounts.size
}

@Composable
private fun GroupedAccountManageCard(
    group: GroupedAccount,
    currencyFormat: DecimalFormat,
    onEdit: (AccountEntity) -> Unit,
    onDelete: (AccountEntity) -> Unit,
    onDeleteAll: (List<AccountEntity>) -> Unit,
    onAddAnother: (String, String) -> Unit
) {
    val cardColor = remember(group.colorHex, group.name) {
        try { Color(android.graphics.Color.parseColor(group.colorHex)) } catch (e: Exception) {
            BANK_COLOR_MAP[group.name]?.let { Color(android.graphics.Color.parseColor(it)) } ?: AccentTeal
        }
    }
    var showMenu by remember { mutableStateOf(false) }
    var showEditSelector by remember { mutableStateOf(false) }
    var showDeleteSelector by remember { mutableStateOf(false) }

    if (showEditSelector) {
        EditAccountSelectDialog(
            group = group,
            cardColor = cardColor,
            currencyFormat = currencyFormat,
            onDismiss = { showEditSelector = false },
            onSelectAccount = onEdit
        )
    }

    if (showDeleteSelector) {
        DeleteAccountSelectDialog(
            group = group,
            cardColor = cardColor,
            currencyFormat = currencyFormat,
            onDismiss = { showDeleteSelector = false },
            onDeleteSingle = onDelete,
            onDeleteEntireGroup = { onDeleteAll(group.accounts) }
        )
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, cardColor.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header: Icon + Name + Count Badge + Total Balance + Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Institution Icon Badge
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(cardColor.copy(alpha = 0.12f))
                        .border(1.dp, cardColor.copy(alpha = 0.25f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (group.type == "CASH" || group.name.contains("Cash", ignoreCase = true)) Icons.Default.Payments
                        else if (group.type == "MFS") Icons.Default.PhoneAndroid
                        else Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = cardColor,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(Modifier.width(12.dp))

                // Title & Count Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.name,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (group.count > 1) "Combined: ৳${currencyFormat.format(group.totalBalance)}"
                        else if (group.type == "CASH") "Physical Cash"
                        else if (group.type == "MFS") "Mobile Financial Service"
                        else "Bank Account",
                        color = TextSecondary,
                        fontSize = 11.5.sp
                    )
                }

                // 3-dots Menu
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = CardDark,
                        modifier = Modifier
                            .clip(RoundedCornerShape(14.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                    ) {
                        // 1. Edit Account
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Edit, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Edit Account", color = TextPrimary, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                if (group.count == 1) {
                                    onEdit(group.accounts.first())
                                } else {
                                    showEditSelector = true
                                }
                            }
                        )

                        // 2. Add Another Account
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Add, contentDescription = null, tint = cardColor, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Add Another at ${group.name}", color = cardColor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                                }
                            },
                            onClick = {
                                showMenu = false
                                onAddAnother(group.name, group.type)
                            }
                        )

                        // 3. Delete Account
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, contentDescription = null, tint = ExpenseRed, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Delete Account", color = ExpenseRed, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                showMenu = false
                                if (group.count == 1) {
                                    onDelete(group.accounts.first())
                                } else {
                                    showDeleteSelector = true
                                }
                            }
                        )
                    }
                }
            }

            // Single Account Card or Multi-Account Cards Inside (Always Expanded)
            if (group.count == 1) {
                val account = group.accounts.first()
                Spacer(Modifier.height(10.dp))
                SingleAccountInnerCard(
                    account = account,
                    cardColor = cardColor,
                    currencyFormat = currencyFormat,
                    accountIndex = 1,
                    isOnlyAccount = true
                )
            } else {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 10.dp))

                Text(
                    text = "INDIVIDUAL ACCOUNTS (${group.count})",
                    fontSize = 10.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    group.accounts.forEachIndexed { index, account ->
                        SingleAccountInnerCard(
                            account = account,
                            cardColor = cardColor,
                            currencyFormat = currencyFormat,
                            accountIndex = index + 1,
                            isOnlyAccount = false
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditAccountSelectDialog(
    group: GroupedAccount,
    cardColor: Color,
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onSelectAccount: (AccountEntity) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.15f))
                                .border(1.dp, AccentTeal.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Select Account to Edit",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = group.name,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(16.dp))

                Text(
                    text = "Choose the specific account you want to edit:",
                    fontSize = 12.5.sp,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 10.dp)
                )

                // List of accounts
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    group.accounts.forEachIndexed { index, acc ->
                        val displayName = when {
                            acc.showAs.isNotBlank() -> acc.showAs
                            else -> "${acc.accountSubtype.ifBlank { "Account" }} #${index + 1}"
                        }
                        val isCashAcc = acc.type == "CASH" || acc.name.contains("Cash", ignoreCase = true)
                        val accNum = if (acc.accountNumber.isNotBlank()) {
                            val raw = acc.accountNumber.trim()
                            if (raw.length > 4) "•••• ${raw.takeLast(4)}" else raw
                        } else ""

                        Surface(
                            onClick = {
                                onSelectAccount(acc)
                                onDismiss()
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CardDarker,
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!isCashAcc && accNum.isNotBlank()) {
                                            Text(
                                                text = accNum,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        if (acc.accountSubtype.isNotBlank()) {
                                            Text(
                                                text = if (!isCashAcc && accNum.isNotBlank()) "• ${acc.accountSubtype}" else acc.accountSubtype,
                                                fontSize = 11.sp,
                                                color = cardColor
                                            )
                                        } else if (isCashAcc) {
                                            Text(
                                                text = "Physical Cash",
                                                fontSize = 11.sp,
                                                color = cardColor
                                            )
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "৳${currencyFormat.format(acc.balance)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentTeal
                                    )
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                        contentDescription = null,
                                        tint = TextMuted,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Cancel button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = CardDarker,
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
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
            }
        }
    }
}

@Composable
private fun DeleteAccountSelectDialog(
    group: GroupedAccount,
    cardColor: Color,
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onDeleteSingle: (AccountEntity) -> Unit,
    onDeleteEntireGroup: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
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
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.15f))
                                .border(1.dp, ExpenseRed.copy(alpha = 0.35f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "Delete Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = group.name,
                                fontSize = 12.sp,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextMuted, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(14.dp))

                Text(
                    text = "Select an account to delete:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextMuted,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // List of individual accounts to delete
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    group.accounts.forEachIndexed { index, acc ->
                        val displayName = when {
                            acc.showAs.isNotBlank() -> acc.showAs
                            else -> "${acc.accountSubtype.ifBlank { "Account" }} #${index + 1}"
                        }
                        val isCashAcc = acc.type == "CASH" || acc.name.contains("Cash", ignoreCase = true)
                        val accNum = if (acc.accountNumber.isNotBlank()) {
                            val raw = acc.accountNumber.trim()
                            if (raw.length > 4) "•••• ${raw.takeLast(4)}" else raw
                        } else ""

                        Surface(
                            onClick = {
                                onDismiss()
                                onDeleteSingle(acc)
                            },
                            shape = RoundedCornerShape(12.dp),
                            color = CardDarker,
                            border = BorderStroke(1.dp, DividerColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = displayName,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextPrimary
                                    )
                                    Spacer(Modifier.height(2.dp))
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (!isCashAcc && accNum.isNotBlank()) {
                                            Text(
                                                text = accNum,
                                                fontSize = 11.sp,
                                                color = TextSecondary
                                            )
                                        }
                                        if (acc.accountSubtype.isNotBlank()) {
                                            Text(
                                                text = if (!isCashAcc && accNum.isNotBlank()) "• ${acc.accountSubtype}" else acc.accountSubtype,
                                                fontSize = 11.sp,
                                                color = cardColor
                                            )
                                        } else if (isCashAcc) {
                                            Text(
                                                text = "Physical Cash",
                                                fontSize = 11.sp,
                                                color = cardColor
                                            )
                                        }
                                    }
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "৳${currencyFormat.format(acc.balance)}",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(ExpenseRed.copy(alpha = 0.12f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteOutline,
                                            contentDescription = "Delete",
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
                HorizontalDivider(color = DividerColor.copy(alpha = 0.6f))
                Spacer(Modifier.height(12.dp))

                // Delete Entire Bank Card Option
                Surface(
                    onClick = {
                        onDismiss()
                        onDeleteEntireGroup()
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = ExpenseRed.copy(alpha = 0.10f),
                    border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.40f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(ExpenseRed.copy(alpha = 0.20f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = null,
                                tint = ExpenseRed,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Delete Entire Bank Card",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ExpenseRed
                            )
                            Text(
                                text = "Removes all ${group.count} accounts under ${group.name}",
                                fontSize = 11.sp,
                                color = ExpenseRed.copy(alpha = 0.75f)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // Cancel button
                Surface(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    color = CardDarker,
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(42.dp)
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
            }
        }
    }
}

@Composable
private fun SingleAccountInnerCard(
    account: AccountEntity,
    cardColor: Color,
    currencyFormat: DecimalFormat,
    accountIndex: Int = 1,
    isOnlyAccount: Boolean = false
) {
    val isCash = account.type == "CASH" || account.name.contains("Cash", ignoreCase = true)

    // Resolve display name / nickname
    val displayName = when {
        account.showAs.isNotBlank() -> account.showAs
        !isOnlyAccount -> "${account.accountSubtype.ifBlank { "Account" }} #$accountIndex"
        else -> account.name
    }

    // Resolve account number
    val accNumberDisplay = if (account.accountNumber.isNotBlank()) {
        val raw = account.accountNumber.trim()
        if (raw.length > 4) "•••• ${raw.takeLast(4)}" else raw
    } else {
        ""
    }

    // Resolve account type
    val accTypeDisplay = when {
        account.accountSubtype.isNotBlank() -> account.accountSubtype
        account.type == "BANK" -> "Bank Account"
        account.type == "MFS" -> "MFS Wallet"
        else -> "Physical Cash"
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = CardDarker,
        border = BorderStroke(1.dp, DividerColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Row 1: Account Name / Nickname + Balance
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Account Name / Nickname
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    Text(
                        text = displayName,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(Modifier.width(8.dp))

                // Balance
                Text(
                    text = "৳${currencyFormat.format(account.balance)}",
                    color = AccentTeal,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Row 2: Distinct Boxes for Account Number, Type of Account, and Holder
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. Account Number Box (Only for non-cash accounts that have an account number)
                if (!isCash && accNumberDisplay.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardDark)
                            .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = accNumberDisplay,
                                color = TextPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }

                // 2. Type of Account Box
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(cardColor.copy(alpha = 0.12f))
                        .border(1.dp, cardColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = accTypeDisplay,
                        color = cardColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // 3. Holder Name (if managed account)
                if (account.isManaged && account.holderName.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(AccentPurple.copy(alpha = 0.12f))
                            .border(1.dp, AccentPurple.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(11.dp)
                            )
                            Text(
                                text = account.holderName,
                                color = AccentPurple,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountFormSheet(
    existingAccount: AccountEntity?,
    onDismiss: () -> Unit,
    onSave: (AccountEntity) -> Unit
) {
    val isEditing = existingAccount != null && existingAccount.id != 0
    var accountType    by remember(existingAccount) { mutableStateOf(existingAccount?.type ?: "BANK") }
    var accountName    by remember(existingAccount) {
        mutableStateOf(
            TextFieldValue(
                text = existingAccount?.name ?: (if (accountType == "CASH") "Hand Cash" else ""),
                selection = TextRange((existingAccount?.name ?: (if (accountType == "CASH") "Hand Cash" else "")).length)
            )
        )
    }
    var accountSubtype by remember(existingAccount) { mutableStateOf(existingAccount?.accountSubtype ?: "") }
    var initialBalance by remember(existingAccount) { mutableStateOf(if (isEditing) existingAccount!!.balance.toString() else "") }
    var accountNumber  by remember(existingAccount) { mutableStateOf(existingAccount?.accountNumber ?: "") }
    var showAs         by remember(existingAccount) { mutableStateOf(existingAccount?.showAs ?: "") }
    val coroutineScope = rememberCoroutineScope()
    var nameExpanded    by remember { mutableStateOf(false) }
    var subtypeExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }
    var forceDismiss by remember { mutableStateOf(false) }

    val initialTypeVal = remember(existingAccount) { existingAccount?.type ?: "BANK" }
    val initialNameVal = remember(existingAccount) { existingAccount?.name ?: (if (initialTypeVal == "CASH") "Hand Cash" else "") }
    val initialSubtypeVal = remember(existingAccount) { existingAccount?.accountSubtype ?: "" }
    val initialBalanceVal = remember(existingAccount) { if (isEditing) (existingAccount?.balance?.toString() ?: "") else "" }
    val initialAccNumVal = remember(existingAccount) { existingAccount?.accountNumber ?: "" }
    val initialShowAsVal = remember(existingAccount) { existingAccount?.showAs ?: "" }

    val isFormDirty = remember(
        accountName.text, initialBalance, accountNumber, showAs, accountSubtype, accountType, isEditing
    ) {
        accountName.text.trim() != initialNameVal.trim() ||
        initialBalance.trim() != initialBalanceVal.trim() ||
        accountNumber.trim() != initialAccNumVal.trim() ||
        showAs.trim() != initialShowAsVal.trim() ||
        accountSubtype != initialSubtypeVal ||
        accountType != initialTypeVal
    }

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        confirmValueChange = { targetValue ->
            if (targetValue == SheetValue.Hidden && isFormDirty && !forceDismiss) {
                showDiscardDialog = true
                false
            } else {
                true
            }
        }
    )

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = if (isEditing) "Discard Account Changes?" else "Discard Account?",
            message = "Are you sure you want to discard? Any entered account details will be lost.",
            onDismissRequest = {
                showDiscardDialog = false
            },
            onConfirmDiscard = {
                showDiscardDialog = false
                forceDismiss = true
                coroutineScope.launch {
                    try {
                        sheetState.hide()
                    } finally {
                        onDismiss()
                    }
                }
            }
        )
    }

    val presetList = if (accountType == "BANK") PRESET_BANKS else PRESET_MFS
    val filteredPresets = if (accountName.text.isBlank()) presetList else presetList.filter { it.contains(accountName.text, ignoreCase = true) }

    val isNameValid = accountName.text.trim().isNotBlank()
    val isAccountTypeValid = accountType != "BANK" || accountSubtype.isNotBlank()
    val isInitialBalanceValid = isEditing || (initialBalance.trim().isNotBlank() && initialBalance.trim().toDoubleOrNull() != null)
    val isAccountNumberValid = accountType == "CASH" || accountNumber.trim().isNotBlank()

    val isValid = isNameValid && isAccountTypeValid && isInitialBalanceValid && isAccountNumberValid

    ModalBottomSheet(
        onDismissRequest = {
            if (isFormDirty && !forceDismiss) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        },
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = !isFormDirty),
        sheetState = sheetState,
        containerColor = CardDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(AccentTeal.copy(alpha = 0.15f))
                        .border(1.dp, AccentTeal.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddCard,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) (if (accountType == "CASH") "Edit Cash Record" else "Edit Account") else (if (accountType == "CASH") "Add Hand Cash" else "Add New Account"),
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditing) "Update account details" else if (accountType == "CASH") "Track physical cash in your hand or wallet" else "Link a bank or MFS to track your money",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Type toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDarker)
                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("CASH", "BANK", "MFS").forEach { t ->
                    val selected = accountType == t
                    val itemColor = if (selected) BackgroundDark else TextPrimary
                    val icon = when (t) {
                        "CASH" -> Icons.Default.Payments
                        "MFS"  -> Icons.Default.PhoneAndroid
                        else   -> Icons.Default.AccountBalance
                    }
                    val label = when (t) {
                        "CASH" -> "Cash"
                        "MFS"  -> "MFS"
                        else   -> "Bank"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccentTeal else Color.Transparent)
                            .clickable {
                                accountType = t
                                accountName = TextFieldValue(if (t == "CASH") "Hand Cash" else "")
                                nameExpanded = false
                            }
                            .padding(vertical = 11.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = itemColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = label,
                                color = itemColor,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            // Account name autocomplete (Banks and MFS only - hidden for Hand Cash)
            if (accountType != "CASH") {
                ExposedDropdownMenuBox(
                    expanded = nameExpanded,
                    onExpandedChange = { nameExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountName,
                        onValueChange = {
                            accountName = it
                            nameExpanded = true
                        },
                        label = { Text(if (accountType == "BANK") "Bank Name *" else "MFS Name *") },
                        placeholder = { Text("Type or select\u2026", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = if (accountType == "MFS") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = formTextFieldColors(accountName.text.isEmpty()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(
                                type = ExposedDropdownMenuAnchorType.PrimaryEditable,
                                enabled = true
                            )
                    )
                    if (filteredPresets.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = nameExpanded,
                            onDismissRequest = { nameExpanded = false },
                            modifier = Modifier
                                .background(CardDarker)
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        ) {
                            filteredPresets.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset, color = TextPrimary, fontSize = 13.sp) },
                                    onClick = {
                                        accountName = TextFieldValue(
                                            text = preset,
                                            selection = TextRange(preset.length)
                                        )
                                        nameExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            Spacer(Modifier.height(14.dp))

            // Account subtype (Banks only)
            if (accountType == "BANK") {
                ExposedDropdownMenuBox(
                    expanded = subtypeExpanded,
                    onExpandedChange = { subtypeExpanded = it }
                ) {
                    OutlinedTextField(
                        value = accountSubtype,
                        onValueChange = {},
                        label = { Text("Account Type *") },
                        placeholder = { Text("e.g. Savings, Current\u2026", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Tune,
                                contentDescription = null,
                                tint = AccentTeal,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subtypeExpanded) },
                        readOnly = true,
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        colors = formTextFieldColors(accountSubtype.isEmpty()),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                    )
                    ExposedDropdownMenu(
                        expanded = subtypeExpanded,
                        onDismissRequest = { subtypeExpanded = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    ) {
                        ACCOUNT_SUBTYPES.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub, color = TextPrimary, fontSize = 13.sp) },
                                onClick = { accountSubtype = sub; subtypeExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            // Initial balance (new accounts only)
            if (!isEditing) {
                OutlinedTextField(
                    value = initialBalance,
                    onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) initialBalance = it },
                    label = { Text("Initial Balance (\u09f3) *") },
                    placeholder = { Text("0.00", color = TextMuted) },
                    prefix = { Text("\u09f3 ", color = AccentTeal, fontWeight = FontWeight.Bold) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = formTextFieldColors(initialBalance.isEmpty()),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }

            // Account Number (Banks and MFS only - hidden for Hand Cash)
            if (accountType != "CASH") {
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { input ->
                        if (input.all { it.isDigit() }) {
                            accountNumber = input
                        }
                    },
                    label = { Text("Account Number *") },
                    placeholder = { Text("Digits only", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = AccentTeal,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = formTextFieldColors(accountNumber.isEmpty()),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(14.dp))
            }

            // Nickname / Location label (max 20 chars - optional)
            OutlinedTextField(
                value = showAs,
                onValueChange = { input ->
                    if (input.length <= 20) {
                        showAs = input
                    }
                },
                label = { Text(if (accountType == "CASH") "Location / Tag (Optional)" else "Nickname (Optional)") },
                placeholder = { Text(if (accountType == "CASH") "e.g. Wallet, Safe (Optional)" else "e.g. Salary, Personal (Optional)", color = TextMuted) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Badge,
                        contentDescription = null,
                        tint = AccentTeal,
                        modifier = Modifier.size(20.dp)
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(showAs.isEmpty()),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    val colorHex = BANK_COLOR_MAP[accountName.text] ?: if (accountType == "CASH") "#10B981" else if (accountType == "MFS") "#FF5C7C" else "#0096FF"
                    val saved = if (isEditing) {
                        existingAccount!!.copy(
                            name = accountName.text.trim(),
                            type = accountType,
                            accountSubtype = if (accountType == "BANK" || accountType == "CASH") accountSubtype else "",
                            isManaged = false,
                            holderName = "",
                            accountNumber = accountNumber.trim(),
                            showAs = showAs.trim(),
                            colorHex = colorHex
                        )
                    } else {
                        AccountEntity(
                            name = accountName.text.trim(),
                            type = accountType,
                            balance = initialBalance.toDoubleOrNull() ?: 0.0,
                            colorHex = colorHex,
                            accountSubtype = if (accountType == "BANK" || accountType == "CASH") accountSubtype else "",
                            isManaged = false,
                            holderName = "",
                            accountNumber = accountNumber.trim(),
                            showAs = showAs.trim()
                        )
                    }
                    forceDismiss = true
                    coroutineScope.launch {
                        try {
                            sheetState.hide()
                        } finally {
                            onSave(saved)
                        }
                    }
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = CardDarker
                ),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isValid) Brush.linearGradient(listOf(AccentTeal, AccentBlue))
                            else Brush.linearGradient(listOf(CardDarker, CardDarker))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Add,
                            contentDescription = null,
                            tint = if (isValid) BackgroundDark else TextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = if (isEditing) "Save Changes" else if (accountType == "CASH") "Add Cash Record" else "Add Account",
                            color = if (isValid) BackgroundDark else TextMuted,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun formTextFieldColors(isEmpty: Boolean) = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentTeal, unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentTeal, unfocusedLabelColor = if (isEmpty) TextMuted else TextSecondary,
    cursorColor = AccentTeal, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardDarker, unfocusedContainerColor = CardDarker,
    focusedPlaceholderColor = TextMuted, unfocusedPlaceholderColor = TextMuted
)
