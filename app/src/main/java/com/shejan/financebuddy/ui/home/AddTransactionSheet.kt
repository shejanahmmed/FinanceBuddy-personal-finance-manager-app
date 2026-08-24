package com.shejan.financebuddy.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import android.content.Context
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.material.icons.filled.Tune
import com.shejan.financebuddy.ui.theme.*
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.TransactionEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.data.db.PayeeAccountEntity
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentTeal
import com.shejan.financebuddy.ui.theme.BackgroundDark
import com.shejan.financebuddy.ui.theme.CardDark
import com.shejan.financebuddy.ui.theme.CardDarker
import com.shejan.financebuddy.ui.theme.DividerColor
import com.shejan.financebuddy.ui.theme.ExpenseRed
import com.shejan.financebuddy.ui.theme.GradientEnd
import com.shejan.financebuddy.ui.theme.GradientStart
import com.shejan.financebuddy.ui.theme.IncomeGreen
import com.shejan.financebuddy.ui.theme.TextMuted
import com.shejan.financebuddy.ui.theme.TextPrimary
import com.shejan.financebuddy.ui.theme.TextSecondary
import com.shejan.financebuddy.ui.theme.TransferYellow

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun AddTransactionSheet(
    accounts: List<AccountEntity>,
    sheetState: SheetState? = null,
    onDismiss: () -> Unit,
    onSaveTransaction: (TransactionEntity, AccountEntity?, AccountEntity?) -> Unit,
    payees: List<PayeeEntity> = emptyList(),
    payeeAccounts: List<PayeeAccountEntity> = emptyList(),
    onSavePayee: (String, String, String, String) -> Unit = { _, _, _, _ -> },
    initialType: String? = null,
    initialAmount: Double? = null,
    initialFromAccountId: Int? = null,
    initialToAccountId: Int? = null,
    initialCategory: String? = null,
    initialNote: String? = null
) {

    var amount by remember(initialAmount) {
        mutableStateOf(
            if (initialAmount != null && initialAmount > 0.0) {
                if (initialAmount % 1.0 == 0.0) initialAmount.toLong().toString() else initialAmount.toString()
            } else ""
        )
    }
    var selectedType by remember(initialType) { mutableStateOf(initialType ?: "EXPENSE") } // "INCOME", "EXPENSE", "TRANSFER"
    var selectedCategory by remember(initialCategory) { mutableStateOf(initialCategory ?: "") }
    var selectedFromAccount by remember(initialFromAccountId, accounts) {
        val initialAcc = accounts.firstOrNull { it.id == initialFromAccountId }
        val defaultCashAcc = accounts.firstOrNull { it.type == "CASH" || it.name.contains("Cash", ignoreCase = true) || it.name.contains("Hand", ignoreCase = true) }
        mutableStateOf(
            if (initialType == "INCOME" || initialType == "TRANSFER") {
                initialAcc
            } else {
                initialAcc ?: defaultCashAcc ?: accounts.firstOrNull()
            }
        )
    }
    var selectedToAccount by remember(initialToAccountId, accounts) {
        mutableStateOf(accounts.firstOrNull { it.id == initialToAccountId })
    }
    var note by remember(initialNote) { mutableStateOf(initialNote ?: "") }

    var fromAccountExpanded by remember { mutableStateOf(false) }
    var toAccountExpanded by remember { mutableStateOf(false) }

    var fromAccountSearchText by remember(selectedFromAccount) {
        mutableStateOf(
            TextFieldValue(
                text = selectedFromAccount?.name ?: "",
                selection = TextRange((selectedFromAccount?.name ?: "").length)
            )
        )
    }
    var toAccountSearchText by remember(selectedToAccount) {
        mutableStateOf(
            TextFieldValue(
                text = selectedToAccount?.name ?: "",
                selection = TextRange((selectedToAccount?.name ?: "").length)
            )
        )
    }

    // Optional details state for new From Account
    var fromNewAccType by remember { mutableStateOf("BANK") }
    var fromNewAccSubtype by remember { mutableStateOf("Savings") }
    var fromNewAccInitialBalance by remember { mutableStateOf("") }
    var fromNewAccNumber by remember { mutableStateOf("") }
    var fromNewAccNickname by remember { mutableStateOf("") }

    // Optional details state for new To Account (Transfer mode)
    var toNewAccType by remember { mutableStateOf("BANK") }
    var toNewAccSubtype by remember { mutableStateOf("Savings") }
    var toNewAccInitialBalance by remember { mutableStateOf("") }
    var toNewAccNumber by remember { mutableStateOf("") }
    var toNewAccNickname by remember { mutableStateOf("") }

    LaunchedEffect(fromAccountSearchText.text) {
        val text = fromAccountSearchText.text.trim()
        if (text.isNotEmpty()) {
            val isCash = text.contains("cash", ignoreCase = true)
            val isMfs = PRESET_MFS.any { text.contains(it, ignoreCase = true) }
            fromNewAccType = when {
                isCash -> "CASH"
                isMfs  -> "MFS"
                else   -> "BANK"
            }
            fromNewAccSubtype = when (fromNewAccType) {
                "CASH" -> "In Hand"
                "MFS"  -> "Personal"
                else   -> "Savings"
            }
        }
    }

    LaunchedEffect(toAccountSearchText.text) {
        val text = toAccountSearchText.text.trim()
        if (text.isNotEmpty()) {
            val isCash = text.contains("cash", ignoreCase = true)
            val isMfs = PRESET_MFS.any { text.contains(it, ignoreCase = true) }
            toNewAccType = when {
                isCash -> "CASH"
                isMfs  -> "MFS"
                else   -> "BANK"
            }
            toNewAccSubtype = when (toNewAccType) {
                "CASH" -> "In Hand"
                "MFS"  -> "Personal"
                else   -> "Savings"
            }
        }
    }

    var isOwnAccount by remember { mutableStateOf(true) }
    var recipientName by remember { mutableStateOf(TextFieldValue("")) }
    var recipientAccountNumber by remember { mutableStateOf(TextFieldValue("")) }
    var saveToPayees by remember { mutableStateOf(false) }

    var selectedPayee by remember { mutableStateOf<PayeeEntity?>(null) }
    var selectedPayeeAccount by remember { mutableStateOf<PayeeAccountEntity?>(null) }

    var payeeExpanded by remember { mutableStateOf(false) }
    var payeeAccountExpanded by remember { mutableStateOf(false) }

    val isFromAccountNew = remember(selectedFromAccount, fromAccountSearchText, accounts) {
        selectedFromAccount == null && fromAccountSearchText.text.trim().isNotEmpty() &&
                accounts.none { it.name.equals(fromAccountSearchText.text.trim(), ignoreCase = true) }
    }
    val isToAccountNew = remember(selectedToAccount, toAccountSearchText, accounts) {
        selectedToAccount == null && toAccountSearchText.text.trim().isNotEmpty() &&
                accounts.none { it.name.equals(toAccountSearchText.text.trim(), ignoreCase = true) }
    }

    val selectedBalance = selectedFromAccount?.balance ?: 0.0
    val parsedAmount = amount.toDoubleOrNull() ?: 0.0
    val isInsufficient = (selectedType == "EXPENSE" || selectedType == "TRANSFER") &&
            ((selectedFromAccount != null && parsedAmount > selectedBalance) || (isFromAccountNew && parsedAmount > 0.0))

    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current
    val sharedPreferences = remember { context.getSharedPreferences("finance_buddy_prefs", Context.MODE_PRIVATE) }

    val defaultIncomeCategories = listOf("Salary", "Freelance", "Investment", "Pocket Money", "Other")
    val defaultExpenseCategories = listOf("Food", "Groceries", "Rent", "Utilities", "Travel", "Shopping", "Entertainment", "Medical", "Other")

    var incomeCategories by remember {
        val saved = sharedPreferences.getString("active_income_categories", null)
        mutableStateOf(
            if (saved != null) {
                saved.split("|").filter { it.isNotEmpty() }
            } else {
                val custom = sharedPreferences.getString("custom_income_categories", "")
                    ?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                (defaultIncomeCategories + custom).distinct()
            }
        )
    }

    var expenseCategories by remember {
        val saved = sharedPreferences.getString("active_expense_categories", null)
        mutableStateOf(
            if (saved != null) {
                saved.split("|").filter { it.isNotEmpty() }
            } else {
                val custom = sharedPreferences.getString("custom_expense_categories", "")
                    ?.split("|")?.filter { it.isNotEmpty() } ?: emptyList()
                (defaultExpenseCategories + custom).distinct()
            }
        )
    }

    val activeCategories = remember(selectedType, incomeCategories, expenseCategories) {
        if (selectedType == "INCOME") {
            incomeCategories
        } else if (selectedType == "EXPENSE") {
            expenseCategories
        } else {
            listOf("Transfer")
        }
    }

    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf("") }
    var newCategoryName by remember { mutableStateOf("") }
    var showCancelConfirmation by remember { mutableStateOf(false) }

    // Reset default category if type changes and current category is invalid
    androidx.compose.runtime.LaunchedEffect(selectedType) {
        if (selectedType == "TRANSFER") {
            selectedCategory = "Transfer"
        } else if (selectedCategory.isEmpty() || !activeCategories.contains(selectedCategory)) {
            selectedCategory = activeCategories.first()
        }

        if (selectedType == "EXPENSE") {
            if (selectedFromAccount == null) {
                val cashAcc = accounts.firstOrNull { it.type == "CASH" || it.name.contains("Cash", ignoreCase = true) || it.name.contains("Hand", ignoreCase = true) }
                selectedFromAccount = cashAcc ?: accounts.firstOrNull()
            }
        } else if (selectedType == "INCOME" || selectedType == "TRANSFER") {
            if (selectedFromAccount?.type == "CASH" || selectedFromAccount?.name?.contains("Cash", ignoreCase = true) == true) {
                selectedFromAccount = null
            }
        }
    }

    val view = androidx.compose.ui.platform.LocalView.current
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val keyboardController = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current

    val dismissKeyboard = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        (context as? android.app.Activity)?.currentFocus?.clearFocus()
        (context as? android.app.Activity)?.window?.let { window ->
            androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                .hide(androidx.core.view.WindowInsetsCompat.Type.ime())
        }
        val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? android.view.inputmethod.InputMethodManager
        imm?.hideSoftInputFromWindow(view.windowToken, 0)
        imm?.hideSoftInputFromWindow(view.applicationWindowToken, 0)
    }

    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()

    var fromAccountAbsoluteY by remember { mutableStateOf(0f) }
    var toAccountAbsoluteY by remember { mutableStateOf(0f) }
    var payeeAbsoluteY by remember { mutableStateOf(0f) }
    var noteAbsoluteY by remember { mutableStateOf(0f) }

    val dynamicBottomSpacer by animateDpAsState(
        targetValue = if (fromAccountExpanded || toAccountExpanded || payeeExpanded) 350.dp else 24.dp,
        label = "dynamicBottomSpacer"
    )

    LaunchedEffect(fromAccountExpanded) {
        if (fromAccountExpanded) {
            delay(60)
            if (fromAccountAbsoluteY > 0f) {
                scrollState.animateScrollTo((fromAccountAbsoluteY - 16f).coerceAtLeast(0f).toInt())
            }
        }
    }

    LaunchedEffect(toAccountExpanded) {
        if (toAccountExpanded) {
            delay(60)
            if (toAccountAbsoluteY > 0f) {
                scrollState.animateScrollTo((toAccountAbsoluteY - 16f).coerceAtLeast(0f).toInt())
            }
        }
    }

    LaunchedEffect(payeeExpanded) {
        if (payeeExpanded) {
            delay(60)
            if (payeeAbsoluteY > 0f) {
                scrollState.animateScrollTo((payeeAbsoluteY - 16f).coerceAtLeast(0f).toInt())
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = BackgroundDark
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .imePadding()
            ) {
                // ── Full Screen Top Header Bar ──────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Add Transaction",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                // ── Scrollable Body ────────────────────────────────────
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(scrollState)
                        .padding(horizontal = 24.dp, vertical = 16.dp)
                        .padding(bottom = 24.dp)
                ) {

            // ── Tab Selector ────────────────────────────────────
            // ── Minimal Dark Segmented Control (Expense / Income / Transfer) ───────
            val types = remember { listOf("EXPENSE" to "Expense", "INCOME" to "Income", "TRANSFER" to "Transfer") }
            val selectedTabIndex = remember(selectedType) {
                types.indexOfFirst { it.first == selectedType }.coerceAtLeast(0)
            }
            val indicatorColor = when (selectedType) {
                "INCOME" -> IncomeGreen
                "EXPENSE" -> ExpenseRed
                else -> TransferYellow
            }

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(CircleShape)
                    .background(Color.Black)
                    .border(1.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .padding(4.dp)
            ) {
                val segmentWidth = maxWidth / 3
                val indicatorOffset by animateDpAsState(
                    targetValue = segmentWidth * selectedTabIndex,
                    animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
                    label = "SegmentSlideAnimation"
                )

                // White sliding pill for selected segment
                Box(
                    modifier = Modifier
                        .offset(x = indicatorOffset)
                        .width(segmentWidth)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(Color.White)
                )

                // Segment Labels Row
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    types.forEach { (typeKey, label) ->
                        val isSelected = selectedType == typeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(CircleShape)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) { selectedType = typeKey },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Amount Input (Typographic ৳ Field) ──────────────
            OutlinedTextField(
                value         = amount,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) amount = it },
                label         = { Text("Amount in BDT (৳)", color = TextSecondary) },
                prefix        = { Text("৳ ", color = indicatorColor, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                textStyle     = TextStyleForAmount(indicatorColor),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier.fillMaxWidth(),
                colors        = TextFieldColors()
            )

            if (isInsufficient) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Warning: Insufficient balance in ${selectedFromAccount?.name ?: fromAccountSearchText} (Available: ৳${String.format("%,.2f", selectedBalance)})",
                    color = ExpenseRed,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Category Selector (Category Chips Grid) ─────────
            if (selectedType != "TRANSFER") {
                Text(text = "Category", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(modifier = Modifier.height(8.dp))

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    activeCategories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isSelected) indicatorColor else CardDark)
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .combinedClickable(
                                    onClick = { selectedCategory = cat },
                                    onLongClick = {
                                        categoryToDelete = cat
                                        showDeleteDialog = true
                                    }
                                )
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text       = cat,
                                color      = if (isSelected) BackgroundDark else TextPrimary,
                                fontSize   = 12.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }

                    // Add Custom Category Chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(CardDark.copy(alpha = 0.6f))
                            .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
                            .clickable { showAddCategoryDialog = true }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Category",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "New",
                                color = AccentTeal,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))
            }

            // ── Account Selector(s) ──────────────────────────────
            if (selectedType == "TRANSFER") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                ) {
                    Text(text = "Transfer to", style = androidx.compose.material3.MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("Own Account", "Other's Account").forEach { opt ->
                            val selected = (opt == "Own Account" && isOwnAccount) || (opt == "Other's Account" && !isOwnAccount)
                            val color = TransferYellow
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (selected) color.copy(alpha = 0.15f) else CardDarker)
                                    .border(
                                        width = 1.dp,
                                        color = if (selected) color else DividerColor,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { isOwnAccount = (opt == "Own Account") }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = opt,
                                    color = if (selected) color else TextPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }

            val isFromAccountCash = remember(selectedFromAccount, fromAccountSearchText.text) {
                val name = selectedFromAccount?.name ?: fromAccountSearchText.text
                selectedFromAccount?.type == "CASH" || name.contains("Cash", ignoreCase = true)
            }

            androidx.compose.runtime.LaunchedEffect(isFromAccountCash) {
                if (isFromAccountCash && (selectedToAccount?.type == "CASH" || selectedToAccount?.name?.contains("Cash", ignoreCase = true) == true)) {
                    selectedToAccount = null
                    toAccountSearchText = TextFieldValue("")
                }
            }

            // Source / From Account
            ExposedDropdownMenuBox(
                expanded = fromAccountExpanded,
                onExpandedChange = { isExpanded ->
                    fromAccountExpanded = isExpanded
                    if (isExpanded) {
                        fromAccountSearchText = TextFieldValue("")
                        coroutineScope.launch {
                            delay(60)
                            if (fromAccountAbsoluteY > 0f) {
                                scrollState.animateScrollTo((fromAccountAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        fromAccountAbsoluteY = coordinates.positionInParent().y + scrollState.value
                    }
            ) {
                OutlinedTextField(
                    value = fromAccountSearchText,
                    onValueChange = {
                        fromAccountSearchText = it
                        fromAccountExpanded = true
                    },
                    readOnly = selectedFromAccount != null,
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    label = { Text(if (selectedType == "TRANSFER") "From Account" else "Account", color = TextSecondary) },
                    placeholder = { Text("Select account", color = TextMuted) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = if (fromAccountExpanded) AccentTeal else TextMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    trailingIcon = {
                        val hasContent = selectedFromAccount != null || fromAccountSearchText.text.isNotEmpty()
                        if (hasContent && !fromAccountExpanded) {
                            IconButton(
                                onClick = {
                                    selectedFromAccount = null
                                    fromAccountSearchText = TextFieldValue("")
                                    fromAccountExpanded = true
                                },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear account",
                                    tint = TextMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        } else {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromAccountExpanded)
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                        .onFocusChanged { focusState ->
                            if (focusState.isFocused) {
                                fromAccountExpanded = true
                                coroutineScope.launch {
                                    delay(60)
                                    if (fromAccountAbsoluteY > 0f) {
                                        scrollState.animateScrollTo((fromAccountAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                                    }
                                }
                            }
                        }
                )

                ExposedDropdownMenu(
                    expanded = fromAccountExpanded,
                    onDismissRequest = {
                        fromAccountExpanded = false
                        fromAccountSearchText = TextFieldValue(
                            text = selectedFromAccount?.name ?: "",
                            selection = TextRange((selectedFromAccount?.name ?: "").length)
                        )
                    },
                    modifier = Modifier
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                        .heightIn(max = 250.dp)
                ) {
                    AccountDropdownItems(
                        searchText = fromAccountSearchText.text,
                        accountsList = accounts,
                        selectedAccount = selectedFromAccount,
                        allowPresetLinking = selectedType != "EXPENSE",
                        allowCashOption = true,
                        cashTagText = if (selectedType == "TRANSFER" && isOwnAccount) "Deposit" else "In Hand",
                        onSelectExisting = { account ->
                            selectedFromAccount = account
                            fromAccountSearchText = TextFieldValue(
                                text = account.name,
                                selection = TextRange(account.name.length)
                            )
                            fromAccountExpanded = false
                            dismissKeyboard()
                            coroutineScope.launch {
                                delay(50)
                                dismissKeyboard()
                                delay(100)
                                dismissKeyboard()
                                scrollState.animateScrollTo(0)
                            }
                        },
                        onSelectNew = { name ->
                            selectedFromAccount = null
                            fromAccountSearchText = TextFieldValue(
                                text = name,
                                selection = TextRange(name.length)
                            )
                            fromAccountExpanded = false
                            dismissKeyboard()
                            coroutineScope.launch {
                                delay(50)
                                dismissKeyboard()
                                delay(100)
                                dismissKeyboard()
                                scrollState.animateScrollTo(0)
                            }
                        }
                    )
                }
            }

            AnimatedVisibility(visible = isFromAccountNew) {
                OptionalNewAccountSection(
                    accountName = fromAccountSearchText.text,
                    accountType = fromNewAccType,
                    onTypeChange = { newType ->
                        fromNewAccType = newType
                        fromNewAccSubtype = when (newType) {
                            "CASH" -> "In Hand"
                            "MFS"  -> "Personal"
                            else   -> "Savings"
                        }
                    },
                    accountSubtype = fromNewAccSubtype,
                    onSubtypeChange = { fromNewAccSubtype = it },
                    initialBalance = fromNewAccInitialBalance,
                    onInitialBalanceChange = { fromNewAccInitialBalance = it },
                    accountNumber = fromNewAccNumber,
                    onAccountNumberChange = { fromNewAccNumber = it },
                    nickname = fromNewAccNickname,
                    onNicknameChange = { fromNewAccNickname = it }
                )
            }

            // Destination / To Account (Visible only for TRANSFER and isOwnAccount)
            if (selectedType == "TRANSFER" && isOwnAccount) {
                Spacer(modifier = Modifier.height(12.dp))
                val destAccounts = remember(accounts, selectedFromAccount, isFromAccountCash) {
                    accounts.filter { account ->
                        account.id != (selectedFromAccount?.id ?: -1) &&
                        (!isFromAccountCash || (account.type != "CASH" && !account.name.contains("Cash", ignoreCase = true)))
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = toAccountExpanded,
                    onExpandedChange = { isExpanded ->
                        toAccountExpanded = isExpanded
                        if (isExpanded) {
                            coroutineScope.launch {
                                delay(60)
                                if (toAccountAbsoluteY > 0f) {
                                    scrollState.animateScrollTo((toAccountAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            toAccountAbsoluteY = coordinates.positionInParent().y + scrollState.value
                        }
                ) {
                    OutlinedTextField(
                        value = toAccountSearchText,
                        onValueChange = {
                            toAccountSearchText = it
                            toAccountExpanded = true
                        },
                        readOnly = selectedToAccount != null,
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        label = { Text(if (isOwnAccount) "To Account" else "To Bank/MFS", color = TextSecondary) },
                        placeholder = { Text(if (isOwnAccount) "Select destination" else "Select bank", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (toAccountExpanded) AccentTeal else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        trailingIcon = {
                            val hasContent = selectedToAccount != null || toAccountSearchText.text.isNotEmpty()
                            if (hasContent && !toAccountExpanded) {
                                IconButton(
                                    onClick = {
                                        selectedToAccount = null
                                        toAccountSearchText = TextFieldValue("")
                                        toAccountExpanded = true
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Clear account",
                                        tint = TextMuted,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = toAccountExpanded)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    toAccountExpanded = true
                                    coroutineScope.launch {
                                        delay(60)
                                        if (toAccountAbsoluteY > 0f) {
                                            scrollState.animateScrollTo((toAccountAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                                        }
                                    }
                                }
                            }
                    )

                    ExposedDropdownMenu(
                        expanded = toAccountExpanded,
                        onDismissRequest = {
                            toAccountExpanded = false
                            toAccountSearchText = TextFieldValue(
                                text = selectedToAccount?.name ?: "",
                                selection = TextRange((selectedToAccount?.name ?: "").length)
                            )
                        },
                        modifier = Modifier
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                            .heightIn(max = 250.dp)
                    ) {
                        AccountDropdownItems(
                            searchText = toAccountSearchText.text,
                            accountsList = destAccounts,
                            selectedAccount = selectedToAccount,
                            allowPresetLinking = true,
                            allowCashOption = !isFromAccountCash,
                            cashTagText = "Withdrawal",
                            onSelectExisting = { account ->
                                selectedToAccount = account
                                toAccountSearchText = TextFieldValue(
                                    text = account.name,
                                    selection = TextRange(account.name.length)
                                )
                                toAccountExpanded = false
                                dismissKeyboard()
                                coroutineScope.launch {
                                    delay(50)
                                    dismissKeyboard()
                                    delay(100)
                                    dismissKeyboard()
                                    scrollState.animateScrollTo(0)
                                }
                            },
                            onSelectNew = { name ->
                                selectedToAccount = null
                                toAccountSearchText = TextFieldValue(
                                    text = name,
                                    selection = TextRange(name.length)
                                )
                                toAccountExpanded = false
                                dismissKeyboard()
                                coroutineScope.launch {
                                    delay(50)
                                    dismissKeyboard()
                                    delay(100)
                                    dismissKeyboard()
                                    scrollState.animateScrollTo(0)
                                }
                            }
                        )
                    }
                }

                AnimatedVisibility(visible = isToAccountNew) {
                    OptionalNewAccountSection(
                        accountName = toAccountSearchText.text,
                        accountType = toNewAccType,
                        onTypeChange = { newType ->
                            toNewAccType = newType
                            toNewAccSubtype = when (newType) {
                                "CASH" -> "In Hand"
                                "MFS"  -> "Personal"
                                else   -> "Savings"
                            }
                        },
                        accountSubtype = toNewAccSubtype,
                        onSubtypeChange = { toNewAccSubtype = it },
                        initialBalance = toNewAccInitialBalance,
                        onInitialBalanceChange = { toNewAccInitialBalance = it },
                        accountNumber = toNewAccNumber,
                        onAccountNumberChange = { toNewAccNumber = it },
                        nickname = toNewAccNickname,
                        onNicknameChange = { toNewAccNickname = it }
                    )
                }
            }

            if (selectedType == "TRANSFER" && !isOwnAccount) {
                Spacer(modifier = Modifier.height(14.dp))

                val filteredPayeeAccounts = remember(recipientName.text, payees, payeeAccounts) {
                    val nameText = recipientName.text
                    if (nameText.trim().isEmpty()) {
                        payeeAccounts
                    } else {
                        val matchingPayeeIds = payees
                            .filter { it.name.contains(nameText, ignoreCase = true) }
                            .map { it.id }
                            .toSet()
                        payeeAccounts.filter {
                            it.payeeId in matchingPayeeIds ||
                            it.recipientName.contains(nameText, ignoreCase = true) ||
                            it.accountNumber.contains(nameText) ||
                            it.bankName.contains(nameText, ignoreCase = true)
                        }
                    }
                }

                // ExposedDropdownMenuBox for Recipient Name Autocomplete
                ExposedDropdownMenuBox(
                    expanded = payeeExpanded,
                    onExpandedChange = { isExpanded ->
                        payeeExpanded = isExpanded
                        if (isExpanded) {
                            coroutineScope.launch {
                                delay(60)
                                if (payeeAbsoluteY > 0f) {
                                    scrollState.animateScrollTo((payeeAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                                }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned { coordinates ->
                            payeeAbsoluteY = coordinates.positionInParent().y + scrollState.value
                        }
                ) {
                    OutlinedTextField(
                        value = recipientName,
                        onValueChange = {
                            recipientName = it
                            payeeExpanded = true
                        },
                        textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                        label = { Text("Recipient Name *", color = TextSecondary) },
                        placeholder = { Text("Enter or select recipient name", color = TextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = if (payeeExpanded) AccentTeal else TextMuted,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = TextFieldColors(),
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                            .onFocusChanged { focusState ->
                                if (focusState.isFocused) {
                                    payeeExpanded = true
                                    coroutineScope.launch {
                                        delay(60)
                                        if (payeeAbsoluteY > 0f) {
                                            scrollState.animateScrollTo((payeeAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                                        }
                                    }
                                }
                            }
                    )

                    if (filteredPayeeAccounts.isNotEmpty()) {
                        ExposedDropdownMenu(
                            expanded = payeeExpanded,
                            onDismissRequest = { payeeExpanded = false },
                            modifier = Modifier
                                .background(CardDarker)
                                .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                                .heightIn(max = 240.dp)
                        ) {
                            filteredPayeeAccounts.forEach { acc ->
                                val parentPayee = payees.firstOrNull { it.id == acc.payeeId }
                                val nameToShow = parentPayee?.name ?: acc.recipientName
                                DropdownMenuItem(
                                    text = {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(nameToShow, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                                                val accLast4 = if (acc.accountNumber.length > 4) {
                                                    val last4 = acc.accountNumber.takeLast(4)
                                                    if (acc.accountNumber.length == 16) "•••• •••• •••• $last4" else "•".repeat(acc.accountNumber.length - 4) + " $last4"
                                                } else acc.accountNumber
                                                Text(
                                                    text = "${acc.bankName} • $accLast4",
                                                    color = TextSecondary,
                                                    fontSize = 12.sp
                                                )
                                            }
                                            if (acc.nickname.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(AccentTeal.copy(alpha = 0.12f))
                                                        .border(1.dp, AccentTeal.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = acc.nickname,
                                                        color = AccentTeal,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            }
                                        }
                                    },
                                    onClick = {
                                        recipientName = TextFieldValue(
                                            text = nameToShow,
                                            selection = TextRange(nameToShow.length)
                                        )
                                        recipientAccountNumber = TextFieldValue(
                                            text = acc.accountNumber,
                                            selection = TextRange(acc.accountNumber.length)
                                        )
                                        saveToPayees = false
                                        payeeExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                val isAccountAlreadySaved = remember(recipientAccountNumber.text, payeeAccounts) {
                    val cleanNum = recipientAccountNumber.text.trim()
                    cleanNum.isNotEmpty() && payeeAccounts.any { it.accountNumber.trim() == cleanNum }
                }

                // Editable Recipient Account Number Field
                OutlinedTextField(
                    value = recipientAccountNumber,
                    onValueChange = {
                        recipientAccountNumber = it
                        val cleanNum = it.text.trim()
                        if (cleanNum.isNotEmpty() && payeeAccounts.any { acc -> acc.accountNumber.trim() == cleanNum }) {
                            saveToPayees = false
                        }
                    },
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                    label = { Text("Recipient Account/Mobile Number *", color = TextSecondary) },
                    placeholder = { Text("Enter account or phone number", color = TextMuted) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldColors(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Switch to toggle saving to profiles
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(enabled = !isAccountAlreadySaved) { saveToPayees = !saveToPayees }
                        .padding(vertical = 4.dp)
                ) {
                    Switch(
                        checked = saveToPayees,
                        onCheckedChange = { saveToPayees = it },
                        enabled = !isAccountAlreadySaved,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = OnAccent,
                            checkedTrackColor = AccentTeal,
                            checkedBorderColor = Color.Transparent,
                            uncheckedThumbColor = SwitchThumbUnchecked,
                            uncheckedTrackColor = SwitchTrackUnchecked,
                            uncheckedBorderColor = SwitchBorderUnchecked
                        )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isAccountAlreadySaved) "Save to Recipient Profiles (Already Saved)" else "Save to Recipient Profiles",
                        color = if (isAccountAlreadySaved) TextMuted else TextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // ── Notes ──────────────────────────────────────────
            OutlinedTextField(
                value         = note,
                onValueChange = { note = it },
                textStyle     = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                label         = { Text("Add Note (Optional)", color = TextSecondary) },
                singleLine    = true,
                shape         = RoundedCornerShape(12.dp),
                modifier      = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned { coordinates ->
                        noteAbsoluteY = coordinates.positionInParent().y + scrollState.value
                    }
                    .onFocusChanged { focusState ->
                        if (focusState.isFocused) {
                            coroutineScope.launch {
                                delay(60)
                                if (noteAbsoluteY > 0f) {
                                    scrollState.animateScrollTo((noteAbsoluteY - 16f).coerceAtLeast(0f).toInt())
                                }
                            }
                        }
                    },
                colors        = TextFieldColors()
            )

            Spacer(modifier = Modifier.height(28.dp))

            // ── Save Button ────────────────────────────────────
            val isValid = amount.isNotEmpty() && amount.toDoubleOrNull() != null && amount.toDouble() > 0 &&
                    (selectedFromAccount != null || isFromAccountNew) && !isInsufficient &&
                    (selectedType != "TRANSFER" || 
                        ((isOwnAccount && (selectedToAccount != null || isToAccountNew) && 
                          (selectedFromAccount?.id != selectedToAccount?.id || fromAccountSearchText.text.trim().lowercase() != toAccountSearchText.text.trim().lowercase())) ||
                          (!isOwnAccount && recipientName.text.trim().isNotEmpty() && recipientAccountNumber.text.trim().isNotEmpty())))

            Button(
                onClick = {
                    if (isValid) {
                        val finalNote = if (selectedType == "TRANSFER" && !isOwnAccount) {
                            "To: ${recipientName.text.trim()} (${recipientAccountNumber.text.trim()})" + (if (note.trim().isNotEmpty()) " - ${note.trim()}" else "")
                        } else {
                            note
                        }
                        if (selectedType == "TRANSFER" && !isOwnAccount && saveToPayees) {
                            val cleanNumber = recipientAccountNumber.text.trim()
                            val guessedType = if (cleanNumber.length == 11 && cleanNumber.startsWith("01")) "MFS" else "BANK"
                            val guessedBank = if (guessedType == "MFS") "Mobile Wallet" else "Bank Account"
                            onSavePayee(
                                recipientName.text.trim(),
                                guessedBank,
                                cleanNumber,
                                guessedType
                            )
                        }
                        
                        val newFromAcc = if (isFromAccountNew) {
                            createNewAccountEntity(
                                name = fromAccountSearchText.text.trim(),
                                customType = fromNewAccType,
                                customSubtype = fromNewAccSubtype,
                                initialBalance = fromNewAccInitialBalance.toDoubleOrNull() ?: 0.0,
                                accountNumber = fromNewAccNumber,
                                nickname = fromNewAccNickname
                            )
                        } else null

                        val newToAcc = if (selectedType == "TRANSFER" && isOwnAccount && isToAccountNew) {
                            createNewAccountEntity(
                                name = toAccountSearchText.text.trim(),
                                customType = toNewAccType,
                                customSubtype = toNewAccSubtype,
                                initialBalance = toNewAccInitialBalance.toDoubleOrNull() ?: 0.0,
                                accountNumber = toNewAccNumber,
                                nickname = toNewAccNickname
                            )
                        } else null

                        onSaveTransaction(
                            TransactionEntity(
                                amount        = amount.toDouble(),
                                type          = selectedType,
                                category      = if (selectedType == "TRANSFER") "Transfer" else selectedCategory,
                                timestamp     = System.currentTimeMillis(),
                                fromAccountId = selectedFromAccount?.id ?: 0,
                                toAccountId   = if (selectedType == "TRANSFER" && isOwnAccount) (selectedToAccount?.id ?: 0) else null,
                                note          = finalNote
                            ),
                            newFromAcc,
                            newToAcc
                        )
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onDismiss()
                    }
                },
                enabled  = isValid,
                shape    = RoundedCornerShape(12.dp),
                border   = BorderStroke(1.dp, if (isValid) AccentTeal.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)),
                colors   = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    disabledContainerColor = CardDarker
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            if (isValid) {
                                Brush.horizontalGradient(colors = listOf(GradientStart, GradientEnd))
                            } else {
                                Brush.horizontalGradient(colors = listOf(CardDark, CardDark))
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Save Transaction",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = if (isValid) BackgroundDark else TextMuted
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // ── Cancel Button ──────────────────────────────────
            Button(
                onClick = {
                    if (amount.isNotEmpty() || note.isNotEmpty()) {
                        showCancelConfirmation = true
                    } else {
                        onDismiss()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                colors = ButtonDefaults.buttonColors(
                    containerColor = ExpenseRed
                ),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(ExpenseRed),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Cancel",
                        style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(dynamicBottomSpacer))
        }

        // ── Cancel Confirmation Dialog ──────────────────────────
        if (showCancelConfirmation) {
            androidx.compose.ui.window.Dialog(onDismissRequest = { showCancelConfirmation = false }) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .padding(20.dp),
                    shape = RoundedCornerShape(24.dp),
                    color = CardDark
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = "⚠️", fontSize = 36.sp)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Discard Changes?",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Are you sure you want to cancel? Any entered transaction details will be lost.",
                            style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            TextButton(
                                onClick = { showCancelConfirmation = false },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp)
                                    .background(CardDarker, RoundedCornerShape(12.dp))
                            ) {
                                Text("Keep Editing", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                            }
                            Button(
                                onClick = {
                                    showCancelConfirmation = false
                                    onDismiss()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ExpenseRed),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(44.dp),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Text("Discard", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }

        // ── Custom Category Dialogs ──────────────────────────────
        if (showAddCategoryDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showAddCategoryDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Add Custom Category",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedTextField(
                            value = newCategoryName,
                            onValueChange = { newCategoryName = it },
                            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium),
                            label = { Text("Category Name", color = TextSecondary) },
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = {
                                    showAddCategoryDialog = false
                                    newCategoryName = ""
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ExpenseRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = {
                                    val trimmed = newCategoryName.trim()
                                    if (trimmed.isNotEmpty()) {
                                        if (selectedType == "INCOME") {
                                            if (!incomeCategories.contains(trimmed)) {
                                                val updated = incomeCategories + trimmed
                                                incomeCategories = updated
                                                sharedPreferences.edit().putString("active_income_categories", updated.joinToString("|")).apply()
                                                selectedCategory = trimmed
                                            }
                                        } else if (selectedType == "EXPENSE") {
                                            if (!expenseCategories.contains(trimmed)) {
                                                val updated = expenseCategories + trimmed
                                                expenseCategories = updated
                                                sharedPreferences.edit().putString("active_expense_categories", updated.joinToString("|")).apply()
                                                selectedCategory = trimmed
                                            }
                                        }
                                    }
                                    showAddCategoryDialog = false
                                    newCategoryName = ""
                                },
                                enabled = newCategoryName.trim().isNotEmpty(),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    1.dp,
                                    if (newCategoryName.trim().isNotEmpty()) AccentTeal.copy(alpha = 0.6f) else Color.White.copy(alpha = 0.15f)
                                ),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = AccentTeal,
                                    contentColor = BackgroundDark,
                                    disabledContainerColor = CardDarker,
                                    disabledContentColor = TextMuted
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Add",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showDeleteDialog) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { showDeleteDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(20.dp),
                    color = CardDark,
                    border = BorderStroke(1.dp, DividerColor)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Delete Category?",
                            style = androidx.compose.material3.MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Are you sure you want to delete the category \"$categoryToDelete\"?",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { showDeleteDialog = false },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.25f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CardDarker,
                                    contentColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                            Button(
                                onClick = {
                                    if (selectedType == "INCOME") {
                                        val updated = incomeCategories.filter { it != categoryToDelete }
                                        incomeCategories = updated
                                        sharedPreferences.edit().putString("active_income_categories", updated.joinToString("|")).apply()
                                        if (selectedCategory == categoryToDelete) {
                                            selectedCategory = updated.firstOrNull() ?: "Other"
                                        }
                                    } else if (selectedType == "EXPENSE") {
                                        val updated = expenseCategories.filter { it != categoryToDelete }
                                        expenseCategories = updated
                                        sharedPreferences.edit().putString("active_expense_categories", updated.joinToString("|")).apply()
                                        if (selectedCategory == categoryToDelete) {
                                            selectedCategory = updated.firstOrNull() ?: "Other"
                                        }
                                    }
                                    showDeleteDialog = false
                                },
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = ExpenseRed,
                                    contentColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            ) {
                                Text(
                                    text = "Delete",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
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

// ─────────────────────────────────────────────────────────────
// Styles Helpers
// ─────────────────────────────────────────────────────────────

@Composable
private fun TextStyleForAmount(color: Color) = androidx.compose.ui.text.TextStyle(
    color      = color,
    fontSize   = 22.sp,
    fontWeight = FontWeight.Bold
)

@Composable
private fun TextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor        = TextPrimary,
    unfocusedTextColor      = TextPrimary,
    disabledTextColor       = TextPrimary,
    focusedBorderColor      = AccentTeal,
    unfocusedBorderColor    = Color.White.copy(alpha = 0.35f),
    focusedContainerColor   = CardDark,
    unfocusedContainerColor = CardDark,
    disabledContainerColor  = CardDark,
    focusedLabelColor       = AccentTeal,
    unfocusedLabelColor     = TextSecondary,
    focusedPlaceholderColor = TextMuted,
    unfocusedPlaceholderColor = TextMuted
)

@Composable
fun LaunchedEffectForType(type: String, block: suspend () -> Unit) {
    androidx.compose.runtime.LaunchedEffect(key1 = type) {
        block()
    }
}

private val PRESET_CASH = listOf("Hand Cash")
private val PRESET_BANKS = listOf(
    "BRAC Bank PLC", "The City Bank PLC", "Eastern Bank PLC (EBL)",
    "Dutch-Bangla Bank PLC (DBBL)", "Prime Bank PLC", "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)", "Al-Arafah Islami Bank PLC", "Shahjalal Islami Bank PLC"
)
private val PRESET_MFS = listOf(
    "bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash"
)

private fun createNewAccountEntity(
    name: String,
    customType: String? = null,
    customSubtype: String? = null,
    initialBalance: Double = 0.0,
    accountNumber: String = "",
    nickname: String = ""
): AccountEntity {
    val cleanName = name
        .replace(" (Deposit)", "")
        .replace(" (Withdrawal)", "")
        .replace(" (In Hand)", "")
        .trim()
    val presetMfs = listOf("bKash", "Nagad", "Rocket", "Upay", "CellFin (IBBL)", "Ok Wallet", "MyCash")
    val isCash = cleanName.contains("cash", ignoreCase = true)
    val isMfs = presetMfs.any { cleanName.contains(it, ignoreCase = true) }

    val type = customType ?: when {
        isCash -> "CASH"
        isMfs  -> "MFS"
        else   -> "BANK"
    }
    val subtype = customSubtype?.takeIf { it.isNotBlank() } ?: when (type) {
        "CASH" -> "In Hand"
        "MFS"  -> "Personal"
        else   -> "Savings"
    }
    val colorHex = when {
        type == "CASH" -> "#10B981"
        cleanName.contains("BRAC", ignoreCase = true) -> "#0096FF"
        cleanName.contains("City", ignoreCase = true) -> "#1A365D"
        cleanName.contains("Eastern", ignoreCase = true) -> "#004B87"
        cleanName.contains("Dutch-Bangla", ignoreCase = true) || cleanName.contains("DBBL", ignoreCase = true) -> "#00875A"
        cleanName.contains("Prime", ignoreCase = true) -> "#1E3A8A"
        cleanName.contains("Mutual Trust", ignoreCase = true) || cleanName.contains("MTB", ignoreCase = true) -> "#A21CAF"
        cleanName.contains("Islami", ignoreCase = true) || cleanName.contains("IBBL", ignoreCase = true) -> "#15803D"
        cleanName.contains("Al-Arafah", ignoreCase = true) -> "#0F766E"
        cleanName.contains("Shahjalal", ignoreCase = true) -> "#0369A1"
        cleanName.contains("bKash", ignoreCase = true) -> "#E2136E"
        cleanName.contains("Nagad", ignoreCase = true) -> "#F04A24"
        cleanName.contains("Rocket", ignoreCase = true) -> "#8C2D19"
        cleanName.contains("Upay", ignoreCase = true) -> "#0052CC"
        cleanName.contains("CellFin", ignoreCase = true) -> "#15803D"
        isMfs -> "#FF5C7C"
        else -> "#0096FF"
    }
    return AccountEntity(
        name = if (type == "CASH") "Hand Cash" else cleanName,
        type = type,
        balance = initialBalance,
        colorHex = colorHex,
        accountSubtype = subtype,
        accountNumber = accountNumber.trim(),
        showAs = nickname.trim()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionalNewAccountSection(
    accountName: String,
    accountType: String,
    onTypeChange: (String) -> Unit,
    accountSubtype: String,
    onSubtypeChange: (String) -> Unit,
    initialBalance: String,
    onInitialBalanceChange: (String) -> Unit,
    accountNumber: String,
    onAccountNumberChange: (String) -> Unit,
    nickname: String,
    onNicknameChange: (String) -> Unit
) {
    var subtypeExpanded by remember { mutableStateOf(false) }

    val subtypes = when (accountType) {
        "CASH" -> listOf("In Hand", "Wallet", "Emergency Cash")
        "MFS"  -> listOf("Personal", "Agent", "Merchant")
        else   -> listOf("Savings", "Current", "Salary", "Student", "Credit Card", "Fixed Deposit", "DPS")
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(CardDark)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Tune,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Optional Account Details",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(AccentTeal.copy(alpha = 0.15f))
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = "New Account",
                    color = AccentTeal,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Account Type Toggle (Cash / Bank / MFS)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(CardDarker)
                .border(1.dp, DividerColor, RoundedCornerShape(10.dp))
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            listOf("CASH" to "Cash", "BANK" to "Bank", "MFS" to "MFS").forEach { (catKey, catLabel) ->
                val selected = accountType == catKey
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (selected) AccentTeal else Color.Transparent)
                        .clickable { onTypeChange(catKey) }
                        .padding(vertical = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = catLabel,
                        color = if (selected) BackgroundDark else TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Account Subtype Dropdown
        ExposedDropdownMenuBox(
            expanded = subtypeExpanded,
            onExpandedChange = { subtypeExpanded = it },
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = accountSubtype,
                onValueChange = {},
                readOnly = true,
                label = { Text("Account Type", color = TextSecondary, fontSize = 12.sp) },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subtypeExpanded) },
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldColors(),
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
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
                    .heightIn(max = 200.dp)
            ) {
                subtypes.forEach { st ->
                    DropdownMenuItem(
                        text = { Text(st, color = TextPrimary, fontSize = 13.sp) },
                        onClick = {
                            onSubtypeChange(st)
                            subtypeExpanded = false
                        }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Initial Balance & Account Number Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = initialBalance,
                onValueChange = { if (it.isEmpty() || it.toDoubleOrNull() != null) onInitialBalanceChange(it) },
                label = { Text("Initial Balance (৳)", color = TextSecondary, fontSize = 11.sp) },
                placeholder = { Text("0.00", color = TextMuted, fontSize = 12.sp) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = TextFieldColors(),
                textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
                modifier = Modifier.weight(1f)
            )

            if (accountType != "CASH") {
                OutlinedTextField(
                    value = accountNumber,
                    onValueChange = { input -> if (input.all { it.isDigit() }) onAccountNumberChange(input) },
                    label = { Text("Acc/Phone No.", color = TextSecondary, fontSize = 11.sp) },
                    placeholder = { Text("Optional", color = TextMuted, fontSize = 12.sp) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = TextFieldColors(),
                    textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Nickname / Custom Alias
        OutlinedTextField(
            value = nickname,
            onValueChange = onNicknameChange,
            label = { Text("Nickname / Alias (Optional)", color = TextSecondary, fontSize = 12.sp) },
            placeholder = { Text("e.g. Salary Acc, Main bKash", color = TextMuted, fontSize = 12.sp) },
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            colors = TextFieldColors(),
            textStyle = androidx.compose.ui.text.TextStyle(color = TextPrimary, fontSize = 13.sp),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.AccountDropdownItems(
    searchText: String,
    accountsList: List<AccountEntity>,
    selectedAccount: AccountEntity? = null,
    allowPresetLinking: Boolean = true,
    allowCashOption: Boolean = true,
    cashTagText: String = "Deposit",
    onSelectExisting: (AccountEntity) -> Unit,
    onSelectNew: (String) -> Unit
) {
    val matchingExistingCash = if (allowCashOption) {
        accountsList.filter { (it.type == "CASH" || it.name.contains("Cash", ignoreCase = true)) && it.name.contains(searchText, ignoreCase = true) }
    } else emptyList()

    val matchingExistingBanks = accountsList.filter { it.type == "BANK" && !it.name.contains("Cash", ignoreCase = true) && it.name.contains(searchText, ignoreCase = true) }
    val matchingExistingMfs = accountsList.filter { it.type == "MFS" && !it.name.contains("Cash", ignoreCase = true) && it.name.contains(searchText, ignoreCase = true) }
    val existingNames = accountsList.map { it.name.lowercase() }

    val matchingPresetCash = if (allowPresetLinking && allowCashOption) {
        PRESET_CASH.filter {
            !existingNames.contains(it.lowercase()) && it.contains(searchText, ignoreCase = true)
        }
    } else emptyList()
    val matchingPresetBanks = if (allowPresetLinking) {
        PRESET_BANKS.filter {
            !existingNames.contains(it.lowercase()) && it.contains(searchText, ignoreCase = true)
        }
    } else emptyList()
    val matchingPresetMfs = if (allowPresetLinking) {
        PRESET_MFS.filter {
            !existingNames.contains(it.lowercase()) && it.contains(searchText, ignoreCase = true)
        }
    } else emptyList()

    if (!allowPresetLinking && matchingExistingCash.isEmpty() && matchingExistingBanks.isEmpty() && matchingExistingMfs.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text("No created accounts match search", color = TextMuted, fontSize = 12.sp)
        }
    }

    // 1. Cash Section
    if (matchingExistingCash.isNotEmpty() || matchingPresetCash.isNotEmpty()) {
        val headerTitle = when (cashTagText) {
            "Deposit" -> "Hand Cash (Deposit)"
            "Withdrawal" -> "Hand Cash (Withdrawal)"
            else -> "Hand Cash"
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = headerTitle.uppercase(),
                color = IncomeGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.5.sp
            )
        }
        matchingExistingCash.forEach { account ->
            val isSelected = selectedAccount?.id == account.id
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) AccentTeal else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (cashTagText == "Deposit" || cashTagText == "Withdrawal") {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(IncomeGreen.copy(alpha = 0.12f))
                                        .border(1.dp, IncomeGreen.copy(alpha = 0.25f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = cashTagText,
                                        color = IncomeGreen,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = { onSelectExisting(account) }
            )
        }
        matchingPresetCash.forEach { preset ->
            val linkText = when (cashTagText) {
                "Deposit" -> "+ Link $preset (Deposit)"
                "Withdrawal" -> "+ Link $preset (Withdrawal)"
                else -> "+ Link $preset"
            }
            DropdownMenuItem(
                text = { Text(linkText, color = TextSecondary, fontSize = 13.sp) },
                onClick = {
                    val newName = when (cashTagText) {
                        "Deposit" -> "$preset (Deposit)"
                        "Withdrawal" -> "$preset (Withdrawal)"
                        else -> preset
                    }
                    onSelectNew(newName)
                }
            )
        }
    }

    // 2. Banks Section
    if (matchingExistingBanks.isNotEmpty() || matchingPresetBanks.isNotEmpty()) {
        if (matchingExistingCash.isNotEmpty() || matchingPresetCash.isNotEmpty()) {
            HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "YOUR BANKS",
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.5.sp
            )
        }
        matchingExistingBanks.forEach { account ->
            val isSelected = selectedAccount?.id == account.id
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) AccentTeal else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (account.accountNumber.isNotBlank()) {
                                val accLast4 = account.accountNumber.takeLast(4)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DividerColor.copy(alpha = 0.3f))
                                        .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "*******$accLast4",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = { onSelectExisting(account) }
            )
        }
        matchingPresetBanks.forEach { preset ->
            DropdownMenuItem(
                text = { Text("+ Link $preset", color = TextSecondary, fontSize = 13.sp) },
                onClick = { onSelectNew(preset) }
            )
        }
    }

    // 3. MFS Section
    if (matchingExistingMfs.isNotEmpty() || matchingPresetMfs.isNotEmpty()) {
        if (matchingExistingCash.isNotEmpty() || matchingPresetCash.isNotEmpty() || matchingExistingBanks.isNotEmpty() || matchingPresetBanks.isNotEmpty()) {
            HorizontalDivider(color = DividerColor.copy(alpha = 0.5f))
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(CardDark.copy(alpha = 0.6f))
                .padding(horizontal = 14.dp, vertical = 6.dp)
        ) {
            Text(
                text = "YOUR MOBILE WALLETS",
                color = AccentTeal,
                fontWeight = FontWeight.Bold,
                fontSize = 10.5.sp,
                letterSpacing = 0.5.sp
            )
        }
        matchingExistingMfs.forEach { account ->
            val isSelected = selectedAccount?.id == account.id
            DropdownMenuItem(
                text = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = account.name,
                                color = if (isSelected) AccentTeal else TextPrimary,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                            if (account.accountNumber.isNotBlank()) {
                                val accLast4 = account.accountNumber.takeLast(4)
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(DividerColor.copy(alpha = 0.3f))
                                        .border(1.dp, DividerColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "*******$accLast4",
                                        color = TextSecondary,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "Selected",
                                tint = AccentTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                },
                onClick = { onSelectExisting(account) }
            )
        }
        matchingPresetMfs.forEach { preset ->
            DropdownMenuItem(
                text = { Text("+ Link $preset", color = TextSecondary, fontSize = 13.sp) },
                onClick = { onSelectNew(preset) }
            )
        }
    }

    val typedTrimmed = searchText.trim()
    if (allowPresetLinking &&
        typedTrimmed.isNotEmpty() &&
        !accountsList.any { it.name.equals(typedTrimmed, ignoreCase = true) } &&
        !PRESET_CASH.any { it.equals(typedTrimmed, ignoreCase = true) } &&
        !PRESET_BANKS.any { it.equals(typedTrimmed, ignoreCase = true) } &&
        !PRESET_MFS.any { it.equals(typedTrimmed, ignoreCase = true) }
    ) {
        DropdownMenuItem(
            text = { Text("+ Create custom account: \"$typedTrimmed\"", color = TextPrimary) },
            onClick = { onSelectNew(typedTrimmed) }
        )
    }
}
