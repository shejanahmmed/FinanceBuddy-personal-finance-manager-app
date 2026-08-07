package com.shejan.financebuddy.ui.pending

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.PendingSmsTransactionEntity
import com.shejan.financebuddy.ui.home.AddTransactionSheet
import com.shejan.financebuddy.ui.theme.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

// ─── Categories ──────────────────────────────────────────────────────────────

private val CATEGORIES = listOf(
    "Mobile Banking", "Banking", "Food & Dining", "Shopping", "Transport",
    "Utilities", "Healthcare", "Education", "Entertainment", "Salary",
    "Freelance", "Business", "Investment", "Transfer", "Other"
)

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PendingTransactionsScreen(
    pendingList: List<PendingSmsTransactionEntity>,
    confirmedList: List<PendingSmsTransactionEntity> = emptyList(),
    dismissedList: List<PendingSmsTransactionEntity> = emptyList(),
    accounts: List<AccountEntity>,
    database: com.shejan.financebuddy.data.db.FinanceDatabase,
    mappingsList: List<com.shejan.financebuddy.data.db.SmsSenderMappingEntity>,
    potentialSenders: List<com.shejan.financebuddy.sms.PotentialSender>,
    onAddMapping: (String, Int) -> Unit,
    onDeleteMapping: (com.shejan.financebuddy.data.db.SmsSenderMappingEntity) -> Unit,
    onLoadPotentialSenders: () -> Unit,
    onSyncSenderHistory: (String, Int, (Int) -> Unit) -> Unit,
    onConfirm: (PendingSmsTransactionEntity, PendingSmsTransactionEntity) -> Unit,
    onDismiss: (PendingSmsTransactionEntity) -> Unit,
    onRestore: (PendingSmsTransactionEntity) -> Unit = {},
    onDeletePermanently: (PendingSmsTransactionEntity) -> Unit = {},
    onUpdate: (PendingSmsTransactionEntity) -> Unit,
    onDismissAll: () -> Unit,
    onConfirmAll: () -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isScanning by remember { mutableStateOf(false) }

    var showSyncOptionsDialog by remember { mutableStateOf(false) }

    fun startSyncProcess(daysLimit: Int?) {
        isScanning = true
        scope.launch {
            val count = com.shejan.financebuddy.sms.SmsSyncHelper.syncPreviousSms(context, database, daysLimit)
            isScanning = false
            android.widget.Toast.makeText(
                context,
                if (count > 0) "Imported $count transaction messages!" else "No new transaction messages found.",
                android.widget.Toast.LENGTH_LONG
            ).show()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showSyncOptionsDialog = true
        } else {
            android.widget.Toast.makeText(
                context,
                "Permission denied. Cannot scan SMS history.",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun checkPermissionAndShowSyncDialog() {
        val hasReadPermission = androidx.core.content.ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_SMS
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        if (hasReadPermission) {
            showSyncOptionsDialog = true
        } else {
            permissionLauncher.launch(android.Manifest.permission.READ_SMS)
        }
    }

    var showMoreMenu by remember { mutableStateOf(false) }
    var editTarget by remember { mutableStateOf<PendingSmsTransactionEntity?>(null) }
    var showDismissAllDialog by remember { mutableStateOf(false) }
    var showConfirmAllDialog by remember { mutableStateOf(false) }
    var showMappingConfigSheet by remember { mutableStateOf(false) }
    val configSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter tab selection: "PENDING", "CONFIRMED", "DISMISSED"
    var selectedFilterTab by remember { mutableStateOf("PENDING") }

    val activeList = when (selectedFilterTab) {
        "CONFIRMED" -> confirmedList
        "DISMISSED" -> dismissedList
        else        -> pendingList
    }

    // Edit bottom sheet state
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .background(Brush.verticalGradient(colors = listOf(AccentTeal.copy(alpha = 0.07f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // ── Screen Header (Matching Bank Accounts Page Design) ──────
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
                        text = "Transaction Inbox",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (pendingList.isEmpty()) "No unreviewed transactions" else "${pendingList.size} unreviewed transactions",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }

                // 3-dot More Options Dropdown
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        if (isScanning) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = AccentTeal
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "More Options",
                                tint = TextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                    ) {
                        if (pendingList.isNotEmpty()) {
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.DoneAll,
                                            contentDescription = null,
                                            tint = IncomeGreen,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Accept All Pending (${pendingList.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showConfirmAllDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = null,
                                            tint = ExpenseRed,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Text("Dismiss All Pending (${pendingList.size})", color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    }
                                },
                                onClick = {
                                    showMoreMenu = false
                                    showDismissAllDialog = true
                                }
                            )
                            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(vertical = 4.dp))
                        }

                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("Sync SMS History", color = TextPrimary, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                checkPermissionAndShowSyncDialog()
                            }
                        )
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Tune,
                                        contentDescription = null,
                                        tint = AccentTeal,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text("SMS Sender Settings", color = TextPrimary, fontSize = 14.sp)
                                }
                            },
                            onClick = {
                                showMoreMenu = false
                                onLoadPotentialSenders()
                                showMappingConfigSheet = true
                            }
                        )
                    }
                }
            }

            // ── Filter Selector Tabs (Pending / Confirmed / Dismissed) ────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDarker)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tabs = listOf(
                    Triple("PENDING", "Pending", pendingList.size),
                    Triple("CONFIRMED", "Confirmed", confirmedList.size),
                    Triple("DISMISSED", "Dismissed", dismissedList.size)
                )
                tabs.forEach { (tabKey, label, count) ->
                    val isSelected = selectedFilterTab == tabKey
                    val tabColor = when (tabKey) {
                        "CONFIRMED" -> IncomeGreen
                        "DISMISSED" -> ExpenseRed
                        else        -> TransferYellow
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isSelected) tabColor.copy(alpha = 0.18f) else Color.Transparent)
                            .border(
                                width = if (isSelected) 1.dp else 0.dp,
                                color = if (isSelected) tabColor.copy(alpha = 0.5f) else Color.Transparent,
                                shape = RoundedCornerShape(10.dp)
                            )
                            .clickable { selectedFilterTab = tabKey }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) tabColor else TextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (count > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (isSelected) tabColor else DividerColor)
                                        .padding(horizontal = 6.dp, vertical = 2.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = count.toString(),
                                        color = if (isSelected) BackgroundDark else TextPrimary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Scrollable list with entrance animations ──────────────────────
            if (activeList.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = when (selectedFilterTab) {
                                "CONFIRMED" -> Icons.Default.CheckCircle
                                "DISMISSED" -> Icons.Default.Close
                                else        -> Icons.Default.Sms
                            },
                            contentDescription = null,
                            tint = TextMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = when (selectedFilterTab) {
                                "CONFIRMED" -> "No confirmed SMS transactions yet."
                                "DISMISSED" -> "No dismissed SMS transactions."
                                else        -> "No pending SMS transactions to review!"
                            },
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(activeList, key = { it.id }) { pending ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            PendingTransactionCard(
                                pending = pending,
                                accounts = accounts,
                                onConfirm = { onConfirm(pending, it) },
                                onDismiss = { onDismiss(pending) },
                                onRestore = { onRestore(pending) },
                                onDeletePermanently = { onDeletePermanently(pending) },
                                onEdit = { editTarget = pending }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }
        }

        // ── Edit Bottom Sheet (Using Unified AddTransactionSheet) ─────────────
        editTarget?.let { target ->
            AddTransactionSheet(
                accounts                  = accounts,
                onDismiss                 = { editTarget = null },
                initialType               = target.type,
                initialAmount             = target.amount,
                initialFromAccountId      = target.fromAccountId,
                initialToAccountId        = target.toAccountId,
                initialCategory           = target.category,
                initialNote               = target.note,
                onSaveTransaction         = { tx, newFromAcc, newToAcc ->
                    scope.launch(Dispatchers.IO) {
                        var fromId = tx.fromAccountId
                        var toId = tx.toAccountId
                        if (newFromAcc != null) {
                            fromId = database.accountDao().insertAccount(newFromAcc).toInt()
                        }
                        if (newToAcc != null) {
                            toId = database.accountDao().insertAccount(newToAcc).toInt()
                        }

                        val updatedPending = target.copy(
                            amount        = tx.amount,
                            type          = tx.type,
                            category      = tx.category,
                            note          = tx.note,
                            fromAccountId = fromId,
                            toAccountId   = toId
                        )

                        if (selectedFilterTab == "PENDING") {
                            onConfirm(target, updatedPending)
                        } else {
                            onUpdate(updatedPending)
                        }
                    }
                    editTarget = null
                }
            )
        }

        // ── Confirm All Dialog ─────────────────────────────────────────────
        if (showConfirmAllDialog) {
            AlertDialog(
                onDismissRequest = { showConfirmAllDialog = false },
                containerColor   = CardDarker,
                title = {
                    Text(
                        "Accept All Pending Transactions?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        "All ${pendingList.size} unconfirmed SMS detections will be accepted and added to your financial accounts.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onConfirmAll()
                            showConfirmAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = IncomeGreen),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Accept All", color = OnAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showConfirmAllDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        // ── Dismiss All Dialog ─────────────────────────────────────────────
        if (showDismissAllDialog) {
            AlertDialog(
                onDismissRequest = { showDismissAllDialog = false },
                containerColor   = CardDarker,
                title = {
                    Text(
                        "Dismiss All transactions?",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Text(
                        "All ${pendingList.size} unconfirmed SMS detections will be moved to the Dismissed tab.",
                        color = TextSecondary,
                        fontSize = 14.sp
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            onDismissAll()
                            showDismissAllDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Dismiss All", color = OnAccent, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showDismissAllDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showSyncOptionsDialog) {
            AlertDialog(
                onDismissRequest = { showSyncOptionsDialog = false },
                containerColor   = CardDarker,
                title = {
                    Text(
                        "Sync SMS History",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "Select the timeframe to scan for bank and MFS transaction messages:",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        val options = listOf(
                            Pair("1 Month (30 Days)", 30),
                            Pair("3 Months", 90),
                            Pair("6 Months", 180),
                            Pair("1 Year", 365),
                            Pair("All SMS History", null as Int?)
                        )
                        options.forEach { (label, days) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(CardDark)
                                    .clickable {
                                        showSyncOptionsDialog = false
                                        startSyncProcess(days)
                                    }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = null,
                                    tint = AccentTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = label,
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSyncOptionsDialog = false }) {
                        Text("Cancel", color = TextSecondary)
                    }
                }
            )
        }

        if (showMappingConfigSheet) {
            ModalBottomSheet(
                onDismissRequest = { showMappingConfigSheet = false },
                sheetState       = configSheetState,
                containerColor   = CardDarker,
                shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                tonalElevation   = 8.dp,
                dragHandle = { BottomSheetDefaults.DragHandle(color = DividerColor) }
            ) {
                SmsSenderMappingsConfigSheet(
                    accounts = accounts,
                    mappingsList = mappingsList,
                    potentialSenders = potentialSenders,
                    onAddMapping = onAddMapping,
                    onDeleteMapping = onDeleteMapping,
                    onSyncSenderHistory = onSyncSenderHistory,
                    onDismiss = { showMappingConfigSheet = false }
                )
            }
        }
    }
}

// ─── Pending Card Composable ──────────────────────────────────────────────────

@Composable
private fun PendingTransactionCard(
    pending: PendingSmsTransactionEntity,
    accounts: List<AccountEntity>,
    onConfirm: (PendingSmsTransactionEntity) -> Unit,
    onDismiss: () -> Unit,
    onRestore: () -> Unit = {},
    onDeletePermanently: () -> Unit = {},
    onEdit: () -> Unit
) {
    val matchedAccount = accounts.firstOrNull { it.id == pending.fromAccountId }
    val typeColor = if (pending.type == "INCOME") IncomeGreen else ExpenseRed
    val typeLabel = when (pending.type) {
        "INCOME"   -> "Income"
        "EXPENSE"  -> "Expense"
        "TRANSFER" -> "Transfer"
        else       -> pending.type
    }
    val typeIcon = when (pending.type) {
        "INCOME"   -> Icons.Default.KeyboardArrowDown
        "EXPENSE"  -> Icons.Default.KeyboardArrowUp
        else       -> Icons.Default.SwapHoriz
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(CardDark)
            .border(
                border = BorderStroke(1.dp, DividerColor.copy(alpha = 0.7f)),
                shape = RoundedCornerShape(20.dp)
            )
    ) {
        // Left side type indicator bar
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(4.dp)
                .background(typeColor)
        )

        Column(
            modifier = Modifier
                .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            // Header Row (Origin & Amount)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Circle Badge for Institution Icon
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(typeColor.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = typeIcon,
                        contentDescription = null,
                        tint = typeColor,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pending.detectedAccountName,
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = formatTimestamp(pending.receivedAt),
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "৳${formatAmount(pending.amount)}",
                        color = typeColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(typeColor.copy(alpha = 0.12f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = typeLabel,
                                color = typeColor,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        if (pending.status != "PENDING") {
                            val badgeColor = if (pending.status == "CONFIRMED") IncomeGreen else ExpenseRed
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(badgeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = pending.status,
                                    color = badgeColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.ExtraBold
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Unmatched Warning Banner
            if (pending.fromAccountId == -1 && pending.status == "PENDING") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(TransferYellow.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, TransferYellow.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = TransferYellow,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Unknown bank/MFS source. Please link an account.",
                        color = TransferYellow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Insufficient Balance Warning Banner
            val sourceAccount = accounts.find { it.id == pending.fromAccountId }
            val isInsufficient = (pending.type == "EXPENSE" || pending.type == "TRANSFER") &&
                    sourceAccount != null && pending.amount > sourceAccount.balance

            if (pending.fromAccountId != -1 && isInsufficient && pending.status == "PENDING") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ExpenseRed.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .border(1.dp, ExpenseRed.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                        .padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = ExpenseRed,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Insufficient balance in ${sourceAccount?.name} (Available: ৳${String.format("%,.2f", sourceAccount?.balance ?: 0.0)})",
                        color = ExpenseRed,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Metadata card section
            if (pending.note.isNotBlank() || matchedAccount != null || pending.category.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarker.copy(alpha = 0.6f), RoundedCornerShape(12.dp))
                        .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (pending.note.isNotBlank()) {
                            DetailRowItem(
                                icon = Icons.AutoMirrored.Filled.Notes,
                                label = when (pending.type) {
                                    "EXPENSE" -> "Recipient / Note"
                                    "INCOME"  -> "Source / Note"
                                    else      -> "Note"
                                },
                                value = pending.note
                            )
                        }
                        if (pending.category.isNotBlank()) {
                            DetailRowItem(
                                icon = Icons.Default.Category,
                                label = "Category",
                                value = pending.category
                            )
                        }
                        DetailRowItem(
                            icon = Icons.Default.CreditCard,
                            label = "Account",
                            value = matchedAccount?.name ?: "Tap Edit to choose account",
                            valueColor = if (matchedAccount != null) TextSecondary else TransferYellow
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Raw SMS Message bubble
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker, RoundedCornerShape(12.dp))
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sms,
                            contentDescription = null,
                            tint = AccentTeal.copy(alpha = 0.7f),
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "SMS SENDER: ${pending.senderAddress}",
                            color = TextMuted,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = pending.rawSmsBody,
                        color = TextSecondary,
                        fontSize = 11.sp,
                        lineHeight = 16.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons based on status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (pending.status) {
                    "DISMISSED" -> {
                        // Delete permanently button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ExpenseRed.copy(alpha = 0.08f))
                                    .border(1.dp, ExpenseRed.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .clickable { onDeletePermanently() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Permanently",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Delete",
                                color = ExpenseRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Restore to Pending button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TransferYellow.copy(alpha = 0.12f))
                                    .border(1.dp, TransferYellow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .clickable { onRestore() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Restore to Pending",
                                    tint = TransferYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Restore",
                                color = TransferYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Confirm button
                        val canConfirm = pending.fromAccountId != -1 && !isInsufficient
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (canConfirm) IncomeGreen else IncomeGreen.copy(alpha = 0.12f))
                                    .border(
                                        width = 1.dp,
                                        color = if (canConfirm) IncomeGreen else DividerColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = canConfirm) { onConfirm(pending) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm",
                                    tint = if (canConfirm) BackgroundDark else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Confirm",
                                color = if (canConfirm) IncomeGreen else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    "CONFIRMED" -> {
                        // Restore to Pending button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(TransferYellow.copy(alpha = 0.12f))
                                    .border(1.dp, TransferYellow.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                                    .clickable { onRestore() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.History,
                                    contentDescription = "Restore to Pending",
                                    tint = TransferYellow,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Move to Pending",
                                color = TransferYellow,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Edit Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CardDarker)
                                    .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
                                    .clickable { onEdit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Edit",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    else -> {
                        // PENDING status buttons
                        // Dismiss Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(ExpenseRed.copy(alpha = 0.08f))
                                    .border(1.dp, ExpenseRed.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                    .clickable { onDismiss() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = ExpenseRed,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Dismiss",
                                color = ExpenseRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Edit Button
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CardDarker)
                                    .border(1.dp, DividerColor, RoundedCornerShape(6.dp))
                                    .clickable { onEdit() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = TextSecondary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Edit",
                                color = TextSecondary,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Confirm Button
                        val canConfirm = pending.fromAccountId != -1 && !isInsufficient
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(if (canConfirm) IncomeGreen else IncomeGreen.copy(alpha = 0.12f))
                                    .border(
                                        width = 1.dp,
                                        color = if (canConfirm) IncomeGreen else DividerColor.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(6.dp)
                                    )
                                    .clickable(enabled = canConfirm) { onConfirm(pending) },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Confirm",
                                    tint = if (canConfirm) BackgroundDark else TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(5.dp))
                            Text(
                                text = "Confirm",
                                color = if (canConfirm) IncomeGreen else TextMuted,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

// ─── Empty State Composable ──────────────────────────────────────────────────

@Composable
private fun EmptyState(
    isScanning: Boolean,
    onScanHistory: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(CardDark)
                    .border(1.dp, DividerColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = IncomeGreen,
                    modifier = Modifier.size(46.dp)
                )
            }
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = "Perfect Sync!",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "All detected bank and MFS SMS messages have been processed. New notifications will appear here instantly.",
                color = TextSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(
                onClick = onScanHistory,
                enabled = !isScanning,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentTeal),
                border = BorderStroke(1.dp, AccentTeal),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = AccentTeal
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = AccentTeal
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan SMS History", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─── Helper Custom Row Composable ─────────────────────────────────────────────

@Composable
private fun DetailRowItem(
    icon: ImageVector,
    label: String,
    value: String,
    valueColor: Color = TextSecondary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = TextMuted,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.width(72.dp)
        )
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Helper Styles ────────────────────────────────────────────────────────────

@Composable
private fun SheetLabel(text: String) {
    Text(
        text = text,
        color = TextSecondary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.height(6.dp))
}

@Composable
private fun outlinedTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor   = AccentTeal,
    unfocusedBorderColor = DividerColor,
    focusedTextColor     = TextPrimary,
    unfocusedTextColor   = TextPrimary,
    cursorColor          = AccentTeal,
    focusedContainerColor   = CardDarker,
    unfocusedContainerColor = CardDarker
)

// ─── Format Helpers ───────────────────────────────────────────────────────────

private fun formatAmount(amount: Double): String {
    val nf = NumberFormat.getNumberInstance()
    nf.maximumFractionDigits = 2
    nf.minimumFractionDigits = 0
    return nf.format(amount)
}

private fun formatTimestamp(ms: Long): String {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    return sdf.format(Date(ms))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SmsSenderMappingsConfigSheet(
    accounts: List<AccountEntity>,
    mappingsList: List<com.shejan.financebuddy.data.db.SmsSenderMappingEntity>,
    potentialSenders: List<com.shejan.financebuddy.sms.PotentialSender>,
    onAddMapping: (String, Int) -> Unit,
    onDeleteMapping: (com.shejan.financebuddy.data.db.SmsSenderMappingEntity) -> Unit,
    onSyncSenderHistory: (String, Int, (Int) -> Unit) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(0) } // 0 = Active, 1 = Link New

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.85f)
            .padding(horizontal = 20.dp)
    ) {
        // Sheet Title
        Text(
            text = "SMS Sender Configurations",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Map custom numbers/sender IDs to your bank accounts",
            color = TextMuted,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Custom TabRow
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = Color.Transparent,
            contentColor = AccentTeal,
            divider = { HorizontalDivider(color = DividerColor) }
        ) {
            Tab(
                selected = selectedTab == 0,
                onClick = { selectedTab = 0 },
                text = { Text("Active Mappings (${mappingsList.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = AccentTeal,
                unselectedContentColor = TextMuted
            )
            Tab(
                selected = selectedTab == 1,
                onClick = { selectedTab = 1 },
                text = { Text("Link New Sender (${potentialSenders.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                selectedContentColor = AccentTeal,
                unselectedContentColor = TextMuted
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
            if (selectedTab == 0) {
                // Active Mappings Tab
                if (mappingsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Tune, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No custom mappings yet", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Go to 'Link New Sender' tab to link a number", color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(mappingsList) { mapping ->
                            val account = accounts.find { it.id == mapping.accountId }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardDark)
                                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mapping.senderAddress,
                                        color = TextPrimary,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Mapped to: ${account?.name ?: "Unknown Account"}",
                                        color = AccentTeal,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                                IconButton(onClick = { onDeleteMapping(mapping) }) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Mapping",
                                        tint = ExpenseRed,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Link New Tab
                if (potentialSenders.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(horizontal = 32.dp)) {
                            Icon(Icons.Default.Sms, null, tint = TextMuted, modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No unmapped senders found", color = TextMuted, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("SMS inbox scanned, but no unmapped transaction-like messages were detected.", color = TextMuted.copy(alpha = 0.6f), fontSize = 12.sp, textAlign = TextAlign.Center)
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(potentialSenders) { sender ->
                            var showAccountMenu by remember { mutableStateOf(false) }
                            
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CardDark)
                                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                                    .padding(14.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = sender.senderAddress,
                                            color = TextPrimary,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                        Text(
                                            text = formatTimestamp(sender.timestamp),
                                            color = TextMuted,
                                            fontSize = 10.sp
                                        )
                                    }
                                    
                                    Box {
                                        Button(
                                            onClick = { showAccountMenu = true },
                                            colors = ButtonDefaults.buttonColors(containerColor = AccentTeal),
                                            shape = RoundedCornerShape(10.dp),
                                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                                            modifier = Modifier.height(34.dp)
                                        ) {
                                            Text("Link Account", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BackgroundDark)
                                        }
                                        
                                        DropdownMenu(
                                            expanded = showAccountMenu,
                                            onDismissRequest = { showAccountMenu = false },
                                            containerColor = CardDarker
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("Select Target Account:", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp) },
                                                onClick = {},
                                                enabled = false
                                            )
                                            accounts.forEach { account ->
                                                DropdownMenuItem(
                                                    text = { Text(account.name, color = TextPrimary, fontSize = 13.sp) },
                                                    onClick = {
                                                        showAccountMenu = false
                                                        onAddMapping(sender.senderAddress, account.id)
                                                        onSyncSenderHistory(sender.senderAddress, account.id) { count ->
                                                            android.widget.Toast.makeText(
                                                                context,
                                                                "Success! Mapped ${sender.senderAddress} and imported $count transactions.",
                                                                android.widget.Toast.LENGTH_LONG
                                                            ).show()
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                // Preview Message Bubble
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(CardDarker, RoundedCornerShape(8.dp))
                                        .padding(10.dp)
                                ) {
                                    Text(
                                        text = sender.latestMessage,
                                        color = TextSecondary,
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        OutlinedButton(
            onClick = onDismiss,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            border = BorderStroke(1.dp, DividerColor)
        ) {
            Text("Close Settings")
        }
        Spacer(modifier = Modifier.height(32.dp))
    }
}

