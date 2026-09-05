package com.shejan.financebuddy.ui.payees

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.blur
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.activity.compose.BackHandler
import com.shejan.financebuddy.ui.common.DiscardChangesDialog
import com.shejan.financebuddy.ui.common.rememberBitmapFromUri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.ui.theme.*
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val PRESET_BANKS = listOf(
    "BRAC Bank PLC", "The City Bank PLC", "Eastern Bank PLC (EBL)",
    "Dutch-Bangla Bank PLC (DBBL)", "Prime Bank PLC", "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)", "Al-Arafah Islami Bank PLC",
    "Shahjalal Islami Bank PLC", "Sonali Bank PLC", "Janata Bank PLC",
    "Agrani Bank PLC", "Rupali Bank PLC", "Trust Bank PLC"
)
private val PRESET_MFS = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash"
)

private val BANK_COLOR_MAP = mapOf(
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
fun PayeeDetailScreen(
    payee: PayeeEntity?,
    accounts: List<PayeeAccountEntity>,
    allTransactions: List<TransactionEntity> = emptyList(),
    allAccounts: List<AccountEntity> = emptyList(),
    onBack: () -> Unit,
    onUpdatePayee: (PayeeEntity) -> Unit,
    onDeletePayee: () -> Unit,
    onAddAccount: (PayeeAccountEntity) -> Unit,
    onUpdateAccount: (PayeeAccountEntity) -> Unit,
    onDeleteAccount: (PayeeAccountEntity) -> Unit
) {
    if (payee == null) {
        Box(modifier = Modifier.fillMaxSize().background(BackgroundDark), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentBlue)
        }
        return
    }

    val context = LocalContext.current
    var showAddSheet by remember { mutableStateOf(false) }
    var editingAccount by remember { mutableStateOf<PayeeAccountEntity?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<PayeeAccountEntity?>(null) }
    var showMenu by remember { mutableStateOf(false) }
    var showHistory by remember { mutableStateOf(false) }

    val recipientTransactions = remember(allTransactions, payee, accounts) {
        val accNums = accounts.map { it.accountNumber.trim() }.filter { it.isNotBlank() }
        allTransactions.filter { tx ->
            tx.note.contains(payee.name, ignoreCase = true) ||
            tx.note.contains(payee.uniqueId, ignoreCase = true) ||
            tx.category.contains(payee.name, ignoreCase = true) ||
            accNums.any { num -> tx.note.contains(num, ignoreCase = true) }
        }.sortedByDescending { it.timestamp }
    }
    val currencyFormat = remember { DecimalFormat("#,##0.00") }
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy • hh:mm a", Locale.getDefault()) }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }
            onUpdatePayee(payee.copy(imageUri = uri.toString()))
        }
    }

    val profileBitmap = rememberBitmapFromUri(payee.imageUri)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()

    val avatarBg = remember(payee.name) {
        val hash = payee.name.hashCode()
        val colors = listOf(AccentTeal, AccentBlue, TransferYellow, IncomeGreen, Color(0xFF9C27B0), Color(0xFFE91E63))
        colors[Math.abs(hash) % colors.size]
    }

    // Delete Payee Confirmation Dialog
    if (showDeleteConfirm) {
        Dialog(onDismissRequest = { showDeleteConfirm = false }) {
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
                    // Top Warning Trash Icon Badge
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
                        text = "Delete Recipient Profile?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to permanently delete \"${payee.name}\" and all their saved accounts?",
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
                            onClick = { showDeleteConfirm = false },
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
                                onDeletePayee()
                                showDeleteConfirm = false
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

    // Delete Account Confirmation Dialog
    accountToDelete?.let { acc ->
        Dialog(onDismissRequest = { accountToDelete = null }) {
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
                    // Top Warning Trash Icon Badge
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
                        text = "Remove Saved Account?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to remove this ${acc.bankName} account?",
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
                            onClick = { accountToDelete = null },
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

                        // Remove Button
                        Surface(
                            onClick = {
                                onDeleteAccount(acc)
                                accountToDelete = null
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
                                    text = "Remove",
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
                .height(300.dp)
                .background(Brush.verticalGradient(colors = listOf(avatarBg.copy(alpha = 0.08f), Color.Transparent)))
        )

        Column(modifier = Modifier.fillMaxSize()) {
            // -- Top Action Bar ------------------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
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
                Spacer(modifier = Modifier.weight(1f))

                // Plain 3-Dots Options Button (Not inside circle)
                Box {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = TextPrimary,
                            modifier = Modifier.size(22.dp)
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
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.AddAPhoto, null, tint = AccentTeal, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text(
                                        text = if (!payee.imageUri.isNullOrEmpty()) "Change Photo" else "Upload Photo",
                                        color = TextPrimary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = {
                                showMenu = false
                                photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }
                        )

                        if (!payee.imageUri.isNullOrEmpty()) {
                            HorizontalDivider(color = DividerColor)
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.NoPhotography, null, tint = TransferYellow, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(10.dp))
                                        Text("Remove Photo", color = TransferYellow, fontSize = 13.sp)
                                    }
                                },
                                onClick = {
                                    showMenu = false
                                    onUpdatePayee(payee.copy(imageUri = null))
                                }
                            )
                        }

                        HorizontalDivider(color = DividerColor)
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Delete, null, tint = ExpenseRed, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(10.dp))
                                    Text("Delete Recipient", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            },
                            onClick = {
                                showMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            // -- Header Profile Card ------------------------------
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Clean Profile Avatar Circle
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(avatarBg.copy(alpha = 0.14f))
                        .border(2.dp, avatarBg.copy(alpha = 0.35f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    if (profileBitmap != null) {
                        Image(
                            bitmap = profileBitmap,
                            contentDescription = payee.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Text(
                            text = payee.name.take(1).uppercase(Locale.ROOT),
                            color = avatarBg,
                            fontSize = 34.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(payee.name, color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Unique ID Badge (High-contrast, proper padding/border order)
                    Box(
                        modifier = Modifier
                            .background(CardDarker, RoundedCornerShape(8.dp))
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Text(
                            text = payee.uniqueId,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Saved Accounts Count Badge in a Separate Box
                    val accCount = accounts.size
                    val countLabel = if (accCount == 1) "1 Account" else "$accCount Accounts"
                    Box(
                        modifier = Modifier
                            .background(AccentBlue.copy(alpha = 0.12f), RoundedCornerShape(8.dp))
                            .border(1.dp, AccentBlue.copy(alpha = 0.30f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = AccentBlue,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = countLabel,
                                color = AccentBlue,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // View History / View Accounts Toggle Badge
                    val toggleBg = if (showHistory) AccentBlue.copy(alpha = 0.12f) else AccentTeal.copy(alpha = 0.12f)
                    val toggleBorder = if (showHistory) AccentBlue.copy(alpha = 0.30f) else AccentTeal.copy(alpha = 0.30f)
                    val toggleColor = if (showHistory) AccentBlue else AccentTeal

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(toggleBg)
                            .border(1.dp, toggleBorder, RoundedCornerShape(8.dp))
                            .clickable { showHistory = !showHistory }
                            .padding(horizontal = 10.dp, vertical = 5.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (showHistory) Icons.Default.AccountBalanceWallet else Icons.Default.History,
                                contentDescription = null,
                                tint = toggleColor,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = if (showHistory) "View Accounts" else "View History",
                                color = toggleColor,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 20.dp))

            // -- Accounts / History Header ------------------------
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (showHistory) "Transaction History" else "Saved Accounts",
                    color = TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                if (!showHistory) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentBlue.copy(alpha = 0.12f))
                            .clickable { editingAccount = null; showAddSheet = true }
                            .padding(horizontal = 10.dp, vertical = 5.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Add, null, tint = AccentBlue, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add Account", color = AccentBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // -- List Content (History vs Accounts) ---------------
            if (showHistory) {
                if (recipientTransactions.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CardDarker)
                                    .border(1.dp, DividerColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.History, null, tint = TextMuted.copy(alpha = 0.6f), modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No transaction history found for ${payee.name}", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(recipientTransactions, key = { it.id }) { tx ->
                            val accName = remember(allAccounts, tx.fromAccountId) {
                                allAccounts.firstOrNull { it.id == tx.fromAccountId }?.name ?: "Account"
                            }
                            RecipientHistoryCard(
                                transaction = tx,
                                accountName = accName,
                                currencyFormat = currencyFormat,
                                dateFormat = dateFormat
                            )
                        }
                    }
                }
            } else {
                if (accounts.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(CardDarker)
                                    .border(1.dp, DividerColor, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalanceWallet, null, tint = TextMuted.copy(alpha = 0.6f), modifier = Modifier.size(30.dp))
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text("No saved accounts for this recipient", color = TextMuted, fontSize = 13.sp)
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(accounts, key = { it.id }) { acc ->
                            PayeeAccountCard(
                                account = acc,
                                onEdit = { editingAccount = acc; showAddSheet = true },
                                onDelete = { accountToDelete = acc }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddSheet) {
        PayeeAccountFormSheet(
            sheetState = sheetState,
            payeeName = payee.name,
            existingAccount = editingAccount,
            onDismiss = { scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false; editingAccount = null } },
            onSave = { acc ->
                if (editingAccount != null) {
                    onUpdateAccount(acc.copy(id = editingAccount!!.id, payeeId = payee.id))
                } else {
                    onAddAccount(acc.copy(payeeId = payee.id))
                }
                scope.launch { sheetState.hide() }.invokeOnCompletion { showAddSheet = false; editingAccount = null }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PayeeAccountCard(
    account: PayeeAccountEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    var showActions by remember { mutableStateOf(false) }

    val cardColor = remember(account.bankName) {
        try { Color(android.graphics.Color.parseColor(BANK_COLOR_MAP[account.bankName] ?: "#0096FF")) }
        catch (e: Exception) { AccentTeal }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, cardColor.copy(alpha = 0.20f), RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = { if (showActions) showActions = false },
                onLongClick = { showActions = true }
            )
    ) {
        val blurRadius = if (showActions) 8.dp else 0.dp
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .blur(blurRadius)
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Bank / MFS Icon Badge
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(cardColor.copy(alpha = 0.12f))
                    .border(1.dp, cardColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (account.type == "MFS") Icons.Default.PhoneAndroid else Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = cardColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            // Account Details Column with uniform 4.dp spacing
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = account.bankName,
                    color = TextPrimary,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                val displayNum = if (account.accountNumber.length > 4) {
                    "•••• ${account.accountNumber.takeLast(4)}"
                } else {
                    account.accountNumber
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (account.accountNumber.isNotBlank()) {
                        Text(
                            text = "Acc: $displayNum",
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                    val secondaryLabel = when {
                        account.nickname.isNotBlank() -> "Nickname: ${account.nickname}"
                        account.recipientName.isNotBlank() -> "Name: ${account.recipientName}"
                        else -> ""
                    }
                    if (account.accountNumber.isNotBlank() && secondaryLabel.isNotBlank()) {
                        Text(
                            text = "•",
                            color = TextMuted,
                            fontSize = 11.5.sp
                        )
                    }
                    if (secondaryLabel.isNotBlank()) {
                        Text(
                            text = secondaryLabel,
                            color = TextSecondary,
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Normal
                        )
                    }
                }
            }

            // 3-Dots Options Menu Icon (⋮)
            IconButton(
                onClick = { showActions = true },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Account Options",
                    tint = TextPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Animated action overlay for Edit & Delete
        AnimatedVisibility(
            visible = showActions,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(BackgroundDark.copy(alpha = 0.90f))
                    .clickable { showActions = false },
                contentAlignment = Alignment.Center
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Edit Button (Teal Theme, Equal Size)
                    Surface(
                        onClick = {
                            showActions = false
                            onEdit()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = AccentTeal.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, AccentTeal.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .width(115.dp)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = AccentTeal,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Edit", color = AccentTeal, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    // Delete Button (Red Theme, Equal Size)
                    Surface(
                        onClick = {
                            showActions = false
                            onDelete()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = ExpenseRed.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, ExpenseRed.copy(alpha = 0.6f)),
                        modifier = Modifier
                            .width(115.dp)
                            .height(42.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = ExpenseRed,
                                modifier = Modifier.size(17.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Delete", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PayeeAccountFormSheet(
    sheetState: SheetState,
    payeeName: String,
    existingAccount: PayeeAccountEntity?,
    onDismiss: () -> Unit,
    onSave: (PayeeAccountEntity) -> Unit
) {
    val isEditing = existingAccount != null

    var type by remember(existingAccount) { mutableStateOf(existingAccount?.type ?: "BANK") }
    var bankName by remember(existingAccount) { mutableStateOf(existingAccount?.bankName ?: "") }
    var accountNumber by remember(existingAccount) { mutableStateOf(existingAccount?.accountNumber ?: "") }
    var recipientName by remember(existingAccount) { mutableStateOf(existingAccount?.recipientName ?: payeeName) }
    var nickname by remember(existingAccount) { mutableStateOf(existingAccount?.nickname ?: "") }

    var nameExpanded by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(bankName, accountNumber, nickname, isEditing) {
        if (isEditing) {
            bankName.trim() != (existingAccount?.bankName ?: "") ||
            accountNumber.trim() != (existingAccount?.accountNumber ?: "") ||
            nickname.trim() != (existingAccount?.nickname ?: "")
        } else {
            bankName.trim().isNotEmpty() || accountNumber.trim().isNotEmpty() || nickname.trim().isNotEmpty()
        }
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = if (isEditing) "Discard Account Changes?" else "Discard Account?",
            message = "Are you sure you want to discard? Any entered account details will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    val presetList = if (type == "BANK") PRESET_BANKS else PRESET_MFS
    val filteredPresets = if (bankName.isBlank()) presetList else presetList.filter { it.contains(bankName, ignoreCase = true) }

    val isValid = bankName.trim().isNotBlank() && accountNumber.trim().isNotBlank()

    ModalBottomSheet(
        onDismissRequest = {
            if (isFormDirty) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        },
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
                        .background(AccentBlue.copy(alpha = 0.15f))
                        .border(1.dp, AccentBlue.copy(alpha = 0.3f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Edit else Icons.Default.AddCard,
                        contentDescription = null,
                        tint = AccentBlue,
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isEditing) "Edit Account" else "Add Saved Account",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (isEditing) "Update bank details" else "Link banking details to this profile",
                        color = TextMuted,
                        fontSize = 12.sp
                    )
                }
            }

            Spacer(Modifier.height(18.dp))

            // Type Toggle (Bank / MFS with vector icons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(CardDarker)
                    .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("BANK", "MFS").forEach { t ->
                    val selected = type == t
                    val itemColor = if (selected) BackgroundDark else TextPrimary
                    val icon = when (t) {
                        "MFS"  -> Icons.Default.PhoneAndroid
                        else   -> Icons.Default.AccountBalance
                    }
                    val label = when (t) {
                        "MFS"  -> "MFS"
                        else   -> "Bank"
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (selected) AccentBlue else Color.Transparent)
                            .clickable { type = t; bankName = ""; nameExpanded = false }
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
                            Spacer(modifier = Modifier.width(8.dp))
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

            // Bank Name Autocomplete
            ExposedDropdownMenuBox(
                expanded = nameExpanded,
                onExpandedChange = { nameExpanded = it }
            ) {
                OutlinedTextField(
                    value = bankName,
                    onValueChange = { bankName = it; nameExpanded = true },
                    label = { Text(if (type == "BANK") "Bank Name" else "MFS Name", color = TextSecondary) },
                    placeholder = { Text("Type or select\u2026", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = if (type == "BANK") Icons.Default.AccountBalance else Icons.Default.PhoneAndroid,
                            contentDescription = null,
                            tint = AccentBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = nameExpanded) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = formTextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
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
                                onClick = { bankName = preset; nameExpanded = false }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Account Number / Wallet Number
            OutlinedTextField(
                value = accountNumber,
                onValueChange = { accountNumber = it },
                label = { Text(if (type == "BANK") "Account Number" else "Mobile Number", color = TextSecondary) },
                placeholder = { Text(if (type == "BANK") "e.g. 12040921..." else "e.g. 01712...", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.CreditCard, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Recipient Name on the account
            OutlinedTextField(
                value = recipientName,
                onValueChange = { recipientName = it },
                label = { Text("Account Holder Name", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.PersonOutline, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(14.dp))

            // Nickname / Alias
            OutlinedTextField(
                value = nickname,
                onValueChange = { if (it.length <= 20) nickname = it },
                label = { Text("Nickname (Optional)", color = TextSecondary) },
                placeholder = { Text("e.g. Personal, Business", color = TextMuted) },
                leadingIcon = {
                    Icon(Icons.Default.Badge, null, tint = AccentBlue, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = formTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(24.dp))

            Button(
                onClick = {
                    onSave(
                        PayeeAccountEntity(
                            payeeId = 0,
                            bankName = bankName.trim(),
                            accountNumber = accountNumber.trim(),
                            recipientName = recipientName.trim(),
                            type = type,
                            nickname = nickname.trim()
                        )
                    )
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
                            if (isValid) Brush.linearGradient(listOf(AccentBlue, AccentTeal))
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
                            text = if (isEditing) "Save Changes" else "Add Account",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun formTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = AccentBlue, unfocusedBorderColor = DividerColor,
    focusedLabelColor = AccentBlue, unfocusedLabelColor = TextSecondary,
    cursorColor = AccentBlue, focusedTextColor = TextPrimary, unfocusedTextColor = TextPrimary,
    focusedContainerColor = CardDarker, unfocusedContainerColor = CardDarker
)

// rememberBitmapFromUri is provided by com.shejan.financebuddy.ui.common.PayeeAvatar

@Composable
private fun RecipientHistoryCard(
    transaction: TransactionEntity,
    accountName: String,
    currencyFormat: DecimalFormat,
    dateFormat: SimpleDateFormat
) {
    val isIncome = transaction.type == "INCOME"
    val amountColor = if (isIncome) IncomeGreen else ExpenseRed
    val sign = if (isIncome) "+" else "-"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(amountColor.copy(alpha = 0.12f))
                    .border(1.dp, amountColor.copy(alpha = 0.25f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isIncome) Icons.Default.ArrowDownward else Icons.Default.ArrowUpward,
                    contentDescription = null,
                    tint = amountColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = accountName,
                    color = TextPrimary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = dateFormat.format(Date(transaction.timestamp)),
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    if (transaction.note.isNotBlank()) {
                        Text("•", color = TextMuted, fontSize = 11.sp)
                        Text(
                            text = transaction.note,
                            color = TextMuted,
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(Modifier.width(10.dp))

            Text(
                text = "$sign৳${currencyFormat.format(transaction.amount)}",
                color = amountColor,
                fontSize = 14.5.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
