package com.shejan.financebuddy.ui.loans

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.activity.compose.BackHandler
import com.shejan.financebuddy.ui.common.DiscardChangesDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.lazy.LazyRow
import com.shejan.financebuddy.data.db.LoanEntity
import com.shejan.financebuddy.data.db.AccountEntity
import com.shejan.financebuddy.data.db.PayeeEntity
import com.shejan.financebuddy.ui.theme.*
import java.text.DecimalFormat
import java.util.Locale

// ─── Supported Bangladeshi Banks Preset List ─────────────────
private val BANK_PRESETS = listOf(
    "BRAC Bank PLC",
    "Dutch-Bangla Bank PLC (DBBL)",
    "The City Bank PLC",
    "Eastern Bank PLC (EBL)",
    "Prime Bank PLC",
    "Mutual Trust Bank PLC",
    "Islami Bank Bangladesh PLC (IBBL)",
    "Al-Arafah Islami Bank PLC",
    "Shahjalal Islami Bank PLC",
    "Other Bank"
)

// Colors based on banks to make it look premium
private val BANK_COLORS = mapOf(
    "BRAC Bank PLC" to "#0096FF",
    "The City Bank PLC" to "#007A33",
    "Eastern Bank PLC (EBL)" to "#003366",
    "Dutch-Bangla Bank PLC (DBBL)" to "#7C5CFC",
    "Prime Bank PLC" to "#FF5722",
    "Mutual Trust Bank PLC" to "#0C2340",
    "Islami Bank Bangladesh PLC (IBBL)" to "#1B5E20",
    "Al-Arafah Islami Bank PLC" to "#2E7D32",
    "Shahjalal Islami Bank PLC" to "#008080",
    "Other Bank" to "#7C5CFC"
)

private fun getBankColor(bankName: String): Color {
    val hex = BANK_COLORS[bankName] ?: "#00D4AA"
    return try {
        Color(android.graphics.Color.parseColor(hex))
    } catch (e: Exception) {
        AccentTeal
    }
}

@Composable
private fun loanTextFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor       = TextPrimary,
    unfocusedTextColor     = TextPrimary,
    focusedBorderColor     = AccentTeal,
    unfocusedBorderColor   = DividerColor,
    focusedContainerColor  = CardDarker,
    unfocusedContainerColor = CardDarker,
    focusedLabelColor      = AccentTeal,
    unfocusedLabelColor    = TextSecondary
)

// ─── Helper for EMI & Repayment Calculations ───────────────
private fun calculateEmi(principal: Double, annualRate: Double, months: Int): Double {
    if (months <= 0) return 0.0
    if (annualRate <= 0.0) return principal / months
    val monthlyRate = annualRate / 12.0 / 100.0
    val emi = (principal * monthlyRate * Math.pow(1.0 + monthlyRate, months.toDouble())) /
            (Math.pow(1.0 + monthlyRate, months.toDouble()) - 1.0)
    return if (emi.isNaN() || emi.isInfinite()) 0.0 else emi
}

// ─── Grouped Loan Models ─────────────────────────────────────
data class GroupedPersonalLoan(
    val lenderName: String,
    val isLent: Boolean,
    val loans: List<LoanEntity>
) {
    val totalPrincipal: Double get() = loans.sumOf { it.loanAmount }
    val totalRepaid: Double get() = loans.sumOf { it.repaidAmount }
    val totalRemaining: Double get() = loans.sumOf { (it.loanAmount - it.repaidAmount).coerceAtLeast(0.0) }
    val isFullyRepaid: Boolean get() = totalRemaining <= 0.0
    val progressPercent: Float get() = if (totalPrincipal > 0) ((totalRepaid / totalPrincipal) * 100).toFloat().coerceIn(0f, 100f) else 0f
    val loanCount: Int get() = loans.size
    val primaryUnpaidLoan: LoanEntity? get() = loans.firstOrNull { (it.loanAmount - it.repaidAmount) > 0.0 } ?: loans.firstOrNull()
}

data class GroupedBankLoan(
    val bankName: String,
    val loans: List<LoanEntity>
) {
    val totalPrincipal: Double get() = loans.sumOf { it.loanAmount }
    val totalRepaid: Double get() = loans.sumOf { it.repaidAmount }

    val totalEmi: Double get() = loans.sumOf { calculateEmi(it.loanAmount, it.interestRate, it.durationMonths) }

    val totalOriginalRepayable: Double get() = loans.sumOf {
        val emi = calculateEmi(it.loanAmount, it.interestRate, it.durationMonths)
        emi * it.durationMonths
    }

    val totalRemainingRepayable: Double get() = loans.sumOf {
        val emi = calculateEmi(it.loanAmount, it.interestRate, it.durationMonths)
        val origRepayable = emi * it.durationMonths
        (origRepayable - it.repaidAmount).coerceAtLeast(0.0)
    }

    val totalRemainingPrincipal: Double get() = loans.sumOf {
        val emi = calculateEmi(it.loanAmount, it.interestRate, it.durationMonths)
        val origRepayable = emi * it.durationMonths
        val remRepayable = (origRepayable - it.repaidAmount).coerceAtLeast(0.0)
        val ratio = if (origRepayable > 0) it.loanAmount / origRepayable else 1.0
        remRepayable * ratio
    }

    val totalRemainingInterest: Double get() = (totalRemainingRepayable - totalRemainingPrincipal).coerceAtLeast(0.0)

    val isFullyRepaid: Boolean get() = totalRemainingRepayable <= 0.0
    val progressPercent: Float get() = if (totalOriginalRepayable > 0) ((totalRepaid / totalOriginalRepayable) * 100).toFloat().coerceIn(0f, 100f) else 0f
    val loanCount: Int get() = loans.size
    val primaryUnpaidLoan: LoanEntity? get() = loans.firstOrNull {
        val emi = calculateEmi(it.loanAmount, it.interestRate, it.durationMonths)
        val origRepayable = emi * it.durationMonths
        (origRepayable - it.repaidAmount) > 0.0
    } ?: loans.firstOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoansScreen(
    loans: List<LoanEntity>,
    accounts: List<AccountEntity>,
    payees: List<PayeeEntity> = emptyList(),
    onBack: () -> Unit,
    onAddLoan: (LoanEntity, accountId: Int) -> Unit,
    onDeleteLoan: (LoanEntity) -> Unit,
    onRepayLoan: (LoanEntity, Double, Int) -> Unit,
    onNavigateToAccounts: () -> Unit
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    // Group loans by lender / bank name
    val groupedBankLoans = remember(loans) {
        loans.filter { it.loanType != "PERSONAL" }
            .groupBy { it.bankName.trim().lowercase(Locale.ROOT) }
            .map { (_, group) ->
                GroupedBankLoan(
                    bankName = group.first().bankName.trim(),
                    loans = group
                )
            }
    }

    val groupedPersonalBorrowedLoans = remember(loans) {
        loans.filter { it.loanType == "PERSONAL" && !it.isLent }
            .groupBy { it.lenderName.trim().lowercase(Locale.ROOT) }
            .map { (_, group) ->
                GroupedPersonalLoan(
                    lenderName = group.first().lenderName.trim(),
                    isLent = false,
                    loans = group
                )
            }
    }

    val groupedPersonalLentLoans = remember(loans) {
        loans.filter { it.loanType == "PERSONAL" && it.isLent }
            .groupBy { it.lenderName.trim().lowercase(Locale.ROOT) }
            .map { (_, group) ->
                GroupedPersonalLoan(
                    lenderName = group.first().lenderName.trim(),
                    isLent = true,
                    loans = group
                )
            }
    }

    // --- Liability side: money we OWE (bank + borrowed from friends) ---
    val totalRemainingPrincipal = remember(loans) {
        loans.filter { !it.isLent }.sumOf { loan ->
            if (loan.loanType == "PERSONAL") {
                (loan.loanAmount - loan.repaidAmount).coerceAtLeast(0.0)
            } else {
                val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
                val originalRepayable = emi * loan.durationMonths
                val remainingRepayable = (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0)
                val principalRatio = if (originalRepayable > 0) loan.loanAmount / originalRepayable else 1.0
                remainingRepayable * principalRatio
            }
        }
    }

    val totalRemainingRepayable = remember(loans) {
        loans.filter { !it.isLent }.sumOf { loan ->
            if (loan.loanType == "PERSONAL") {
                (loan.loanAmount - loan.repaidAmount).coerceAtLeast(0.0)
            } else {
                val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
                val originalRepayable = emi * loan.durationMonths
                (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0)
            }
        }
    }

    // --- Asset side: money OWED TO US (lent to friends) ---
    val totalRemainingLent = remember(loans) {
        loans.filter { it.loanType == "PERSONAL" && it.isLent }.sumOf { loan ->
            (loan.loanAmount - loan.repaidAmount).coerceAtLeast(0.0)
        }
    }

    val totalRemainingInterest = remember(loans, totalRemainingRepayable, totalRemainingPrincipal) {
        (totalRemainingRepayable - totalRemainingPrincipal).coerceAtLeast(0.0)
    }

    val totalRepaid = remember(loans) { loans.filter { !it.isLent }.sumOf { it.repaidAmount } }

    var showAddTypeChooser by remember { mutableStateOf(false) }
    var showAddBankLoanSheet by remember { mutableStateOf(false) }
    var showAddPersonalLoanSheet by remember { mutableStateOf(false) }
    var isAddingPersonalLoanLent by remember { mutableStateOf(false) }
    var prefillLenderName by remember { mutableStateOf<String?>(null) }
    var prefillBankName by remember { mutableStateOf<String?>(null) }
    var deletingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var editingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    var repayingLoan by remember { mutableStateOf<LoanEntity?>(null) }
    
    val typeChooserSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val bankLoanSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val personalLoanSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Delete loan confirmation dialog
    deletingLoan?.let { loan ->
        val lenderOrBank = if (loan.loanType == "PERSONAL") loan.lenderName else loan.bankName
        Dialog(onDismissRequest = { deletingLoan = null }) {
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
                        text = "Delete Loan Entry?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to remove the loan record of ৳${currencyFormat.format(loan.loanAmount)} with \"$lenderOrBank\"?",
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
                            onClick = { deletingLoan = null },
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
                                onDeleteLoan(loan)
                                deletingLoan = null
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // Ambient background gradient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentBlue.copy(alpha = 0.08f), Color.Transparent)
                    )
                )
        )

        Scaffold(
            containerColor = Color.Transparent,
            topBar = {}
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = innerPadding.calculateBottomPadding())
            ) {
                // ─── Screen Header (Matching Bank Accounts Page Design) ──
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
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Loans & Debts",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Track money lent, borrowed & EMI details",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }

                    // Add Loan / Lent '+' Button in a Compact Square Box
                    Box(
                        modifier = Modifier
                            .padding(end = 6.dp)
                            .size(28.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(CardDarker)
                            .border(1.dp, AccentTeal.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            .clickable {
                                prefillBankName = null
                                prefillLenderName = null
                                showAddTypeChooser = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Loan or Lent",
                            tint = AccentTeal,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // ─── Dashboard Overview Card ────────────────────────
                    item {
                        LoanSummaryOverview(
                            totalPrincipal = totalRemainingPrincipal,
                            totalRepayable = totalRemainingRepayable,
                            totalInterest = totalRemainingInterest,
                            totalRepaid = totalRepaid,
                            totalLent = totalRemainingLent,
                            currencyFormat = currencyFormat
                        )
                    }

                    // Section Title
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Active Loans",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${loans.size} total",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }

                    // Empty State
                    if (loans.isEmpty()) {
                        item {
                            LoansEmptyState()
                        }
                    }

                    // Bank Loans Section (Grouped by Bank Name)
                    if (groupedBankLoans.isNotEmpty()) {
                        item {
                            Text(
                                text = "Bank Loans",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }

                        items(groupedBankLoans, key = { "bank_group_${it.bankName.lowercase(Locale.ROOT)}" }) { group ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 4 }
                            ) {
                                GroupedBankLoanCardItem(
                                    group = group,
                                    accounts = accounts,
                                    currencyFormat = currencyFormat,
                                    onDeleteClick = { deletingLoan = it },
                                    onEditClick = { editingLoan = it },
                                    onRepayClick = { repayingLoan = it },
                                    onAddAnotherClick = { bank ->
                                        prefillBankName = bank
                                        showAddBankLoanSheet = true
                                    }
                                )
                            }
                        }
                    }

                    // Borrowed from Friend/Family Section (Grouped by Person)
                    if (groupedPersonalBorrowedLoans.isNotEmpty()) {
                        item {
                            Text(
                                text = "Borrowed from Friend / Family",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentBlue,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        items(groupedPersonalBorrowedLoans, key = { "pers_borrow_group_${it.lenderName.lowercase(Locale.ROOT)}" }) { group ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 4 }
                            ) {
                                GroupedPersonalLoanCardItem(
                                    group = group,
                                    accounts = accounts,
                                    currencyFormat = currencyFormat,
                                    onDeleteClick = { deletingLoan = it },
                                    onEditClick = { editingLoan = it },
                                    onRepayClick = { repayingLoan = it },
                                    onAddAnotherClick = { lender, lent ->
                                        prefillLenderName = lender
                                        isAddingPersonalLoanLent = lent
                                        showAddPersonalLoanSheet = true
                                    },
                                    isLent = false
                                )
                            }
                        }
                    }

                    // Lent to Friend/Family Section (Grouped by Person)
                    if (groupedPersonalLentLoans.isNotEmpty()) {
                        item {
                            Text(
                                text = "Lent to Friend / Family",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = AccentPurple,
                                modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
                            )
                        }

                        items(groupedPersonalLentLoans, key = { "pers_lent_group_${it.lenderName.lowercase(Locale.ROOT)}" }) { group ->
                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 },
                                exit = fadeOut(tween(250)) + slideOutVertically(tween(250)) { it / 4 }
                            ) {
                                GroupedPersonalLoanCardItem(
                                    group = group,
                                    accounts = accounts,
                                    currencyFormat = currencyFormat,
                                    onDeleteClick = { deletingLoan = it },
                                    onEditClick = { editingLoan = it },
                                    onRepayClick = { repayingLoan = it },
                                    onAddAnotherClick = { lender, lent ->
                                        prefillLenderName = lender
                                        isAddingPersonalLoanLent = lent
                                        showAddPersonalLoanSheet = true
                                    },
                                    isLent = true
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // ─── Add Loan Type Chooser Bottom Sheet ───────────────────────
    if (showAddTypeChooser) {
        ModalBottomSheet(
            onDismissRequest = { showAddTypeChooser = false },
            sheetState = typeChooserSheetState,
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Choose Loan Action",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                // Bank Loan Option Card
                Card(
                    onClick = {
                        prefillBankName = null
                        showAddBankLoanSheet = true
                        showAddTypeChooser = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentTeal.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.AccountBalance, contentDescription = null, tint = AccentTeal)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🏦 Bank Loan (Borrowed)", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text("Formal EMI-based bank loan with interest rates & tenure.", color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }

                // Personal Loan Option Card (Borrowed)
                Card(
                    onClick = {
                        prefillLenderName = null
                        isAddingPersonalLoanLent = false
                        showAddPersonalLoanSheet = true
                        showAddTypeChooser = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentBlue.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.Default.People, contentDescription = null, tint = AccentBlue)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("🤝 Borrow from Friend / Family", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text("Informal loan from individuals with no interest.", color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }

                // Personal Loan Option Card (Lent)
                Card(
                    onClick = {
                        prefillLenderName = null
                        isAddingPersonalLoanLent = true
                        showAddPersonalLoanSheet = true
                        showAddTypeChooser = false
                    },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDark),
                    modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(16.dp))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(AccentPurple.copy(alpha = 0.1f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = null, tint = AccentPurple)
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text("📥 Lend to Friend / Family", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 15.sp)
                            Text("Track money you lend to others and their repayments.", color = TextSecondary, fontSize = 12.sp, lineHeight = 16.sp)
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    // ─── Add Bank Loan Bottom Sheet ──────────────────────────────
    if (showAddBankLoanSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddBankLoanSheet = false
                prefillBankName = null
            },
            sheetState = bankLoanSheetState,
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
        ) {
            AddLoanFormSheet(
                accounts = accounts,
                initialBankName = prefillBankName,
                onDismiss = {
                    showAddBankLoanSheet = false
                    prefillBankName = null
                },
                onAddLoan = { bank, amount, months, rate, accountId ->
                    onAddLoan(
                        LoanEntity(
                            bankName = bank,
                            loanAmount = amount,
                            durationMonths = months,
                            interestRate = rate,
                            accountId = accountId,
                            loanType = "BANK"
                        ),
                        accountId
                    )
                    showAddBankLoanSheet = false
                    prefillBankName = null
                },
                onNavigateToAccounts = onNavigateToAccounts,
                currencyFormat = currencyFormat
            )
        }
    }

    // ─── Add Personal Loan Bottom Sheet ──────────────────────────
    if (showAddPersonalLoanSheet) {
        ModalBottomSheet(
            onDismissRequest = {
                showAddPersonalLoanSheet = false
                prefillLenderName = null
            },
            sheetState = personalLoanSheetState,
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
        ) {
            AddPersonalLoanFormSheet(
                accounts = accounts,
                payees = payees,
                isLent = isAddingPersonalLoanLent,
                initialLenderName = prefillLenderName,
                onDismiss = {
                    showAddPersonalLoanSheet = false
                    prefillLenderName = null
                },
                onAddLoan = { lender, amount, accountId ->
                    onAddLoan(
                        LoanEntity(
                            bankName = "Personal Loan",
                            loanAmount = amount,
                            durationMonths = 1,
                            interestRate = 0.0,
                            accountId = accountId,
                            loanType = "PERSONAL",
                            lenderName = lender,
                            isLent = isAddingPersonalLoanLent
                        ),
                        accountId
                    )
                    showAddPersonalLoanSheet = false
                    prefillLenderName = null
                },
                onNavigateToAccounts = onNavigateToAccounts,
                currencyFormat = currencyFormat
            )
        }
    }

    // ─── Edit Loan Bottom Sheets ──────────────────────────────────
    if (editingLoan != null && editingLoan!!.loanType != "PERSONAL") {
        ModalBottomSheet(
            onDismissRequest = { editingLoan = null },
            sheetState = bankLoanSheetState,
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
        ) {
            val loanToEdit = editingLoan!!
            AddLoanFormSheet(
                accounts = accounts,
                initialLoan = loanToEdit,
                onDismiss = { editingLoan = null },
                onAddLoan = { bank, amount, months, rate, accountId ->
                    onAddLoan(
                        loanToEdit.copy(
                            bankName = bank,
                            loanAmount = amount,
                            durationMonths = months,
                            interestRate = rate,
                            accountId = accountId
                        ),
                        accountId
                    )
                    editingLoan = null
                },
                onNavigateToAccounts = onNavigateToAccounts,
                currencyFormat = currencyFormat
            )
        }
    }

    if (editingLoan != null && editingLoan!!.loanType == "PERSONAL") {
        ModalBottomSheet(
            onDismissRequest = { editingLoan = null },
            sheetState = personalLoanSheetState,
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
        ) {
            val loanToEdit = editingLoan!!
            AddPersonalLoanFormSheet(
                accounts = accounts,
                payees = payees,
                isLent = loanToEdit.isLent,
                initialLoan = loanToEdit,
                onDismiss = { editingLoan = null },
                onAddLoan = { lender, amount, accountId ->
                    onAddLoan(
                        loanToEdit.copy(
                            lenderName = lender,
                            loanAmount = amount,
                            accountId = accountId
                        ),
                        accountId
                    )
                    editingLoan = null
                },
                onNavigateToAccounts = onNavigateToAccounts,
                currencyFormat = currencyFormat
            )
        }
    }

    // ─── Repay Loan Bottom Sheet ────────────────────────────────
    if (repayingLoan != null) {
        ModalBottomSheet(
            onDismissRequest = { repayingLoan = null },
            containerColor = SurfaceDark,
            dragHandle = { BottomSheetDefaults.DragHandle(color = TextSecondary.copy(alpha = 0.75f)) }
        ) {
            val loan = repayingLoan!!
            val linkedAccount = remember(accounts, loan.accountId) {
                accounts.find { it.id == loan.accountId }
            }
            RepayLoanFormSheet(
                loan = loan,
                account = linkedAccount,
                accounts = accounts,
                currencyFormat = currencyFormat,
                onDismiss = { repayingLoan = null },
                onRepay = { amount, accountId ->
                    onRepayLoan(loan, amount, accountId)
                    repayingLoan = null
                }
            )
        }
    }
}

@Composable
fun LoanSummaryOverview(
    totalPrincipal: Double,
    totalRepayable: Double,
    totalInterest: Double,
    totalRepaid: Double,
    totalLent: Double,
    currencyFormat: DecimalFormat
) {
    var isExpanded by remember { mutableStateOf(false) }

    val oweStr = "৳${currencyFormat.format(totalPrincipal)}"
    val lentStr = "৳${currencyFormat.format(totalLent)}"
    val debtRemainingStr = "৳${currencyFormat.format(totalRepayable)}"
    val lentRemainingStr = "৳${currencyFormat.format(totalLent)}"
    val repaidStr = "৳${currencyFormat.format(totalRepaid)}"

    val isTopLarge = oweStr.length > 11 || lentStr.length > 11

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, AccentBlue.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded }
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            AccentBlue.copy(alpha = 0.12f),
                            AccentPurple.copy(alpha = 0.08f),
                            CardDark
                        )
                    )
                )
                .padding(18.dp)
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                // Header Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(AccentBlue)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "TOTAL LIABILITIES & ASSETS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentBlue,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(CardDarker)
                            .border(1.dp, DividerColor, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "BDT ৳",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Top values: I Owe vs Owed to Me (Adaptive Row or Column)
                if (isTopLarge) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // What I Owe
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "I Owe (Borrowed)",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = oweStr,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Owed to Me
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Owed to Me (Lent)",
                                fontSize = 12.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = lentStr,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left — What I owe
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "I Owe (Borrowed)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = oweStr,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        // Right — What's owed to me
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.End
                        ) {
                            Text(
                                text = "Owed to Me (Lent)",
                                fontSize = 11.sp,
                                color = TextSecondary,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = lentStr,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Bold,
                                color = AccentTeal,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Expandable Section (3 Stat Boxes)
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = fadeIn(tween(250)) + slideInVertically(tween(250)) { -it / 4 },
                    exit = fadeOut(tween(200)) + slideOutVertically(tween(200)) { -it / 4 }
                ) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DividerColor, modifier = Modifier.fillMaxWidth())
                        Spacer(modifier = Modifier.height(14.dp))

                        // Bottom 3 Stat Boxes (Stacked in a single column)
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Stat 1: Debt Remaining
                            StatPillBox(
                                title = "Debt Remaining",
                                value = debtRemainingStr,
                                valueColor = ExpenseRed
                            )
                            // Stat 2: Lent Remaining
                            StatPillBox(
                                title = "Lent Remaining",
                                value = lentRemainingStr,
                                valueColor = AccentTeal
                            )
                            // Stat 3: Total Repaid
                            StatPillBox(
                                title = "Total Repaid",
                                value = repaidStr,
                                valueColor = IncomeGreen
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Centered Expand / Collapse Arrow
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
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
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPillBox(
    title: String,
    value: String,
    valueColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(CardDarker)
            .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 12.sp,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// ─── Composable: Loans Empty State ──────────────────────────
@Composable
fun LoansEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(AccentTeal.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.MonetizationOn,
                    contentDescription = null,
                    tint = AccentTeal,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No active loans added",
                color = TextPrimary,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap the + button to calculate and store your bank loan liabilities.",
                color = TextMuted,
                fontSize = 12.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ─── Composable: Grouped Bank Loan Card Item ─────────────────────────
@Composable
fun GroupedBankLoanCardItem(
    group: GroupedBankLoan,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat,
    onDeleteClick: (LoanEntity) -> Unit,
    onEditClick: (LoanEntity) -> Unit,
    onRepayClick: (LoanEntity) -> Unit,
    onAddAnotherClick: ((String) -> Unit)? = null
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val bankColor = getBankColor(group.bankName)
    val singleLoan = group.loans.firstOrNull()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, bankColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            // Clickable upper part (Header & metrics)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                // Card Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Bank Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(bankColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = group.bankName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (group.loanCount > 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(bankColor.copy(alpha = 0.15f))
                                        .border(1.dp, bankColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${group.loanCount} Loans",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = bankColor
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (group.loanCount > 1) {
                                "${group.loanCount} Bank Loans · Consolidated"
                            } else {
                                "${singleLoan?.durationMonths ?: 0} Months | ${singleLoan?.interestRate ?: 0.0}% APR"
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // 3-Dots Menu Button (⋮)
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Loan Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = CardDarker,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                        ) {
                            if (group.loanCount == 1 && singleLoan != null) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Loan",
                                                tint = AccentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Edit Loan",
                                                color = TextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEditClick(singleLoan)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Loan",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Delete Loan",
                                                color = ExpenseRed,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick(singleLoan)
                                    }
                                )
                            }
                            if (onAddAnotherClick != null) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Loan",
                                                tint = AccentTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Add Another Loan at ${group.bankName}",
                                                color = AccentTeal,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onAddAnotherClick(group.bankName)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Info: Amount & EMI Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Remaining Principal",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${currencyFormat.format(group.totalRemainingPrincipal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (group.loanCount > 1) "Total Monthly EMI" else "Monthly EMI",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${currencyFormat.format(group.totalEmi)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentTeal
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Repayment Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repaid: ${String.format(Locale.US, "%.1f%%", group.progressPercent)}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${currencyFormat.format(group.totalRepaid)} / ৳${currencyFormat.format(group.totalOriginalRepayable)}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DividerColor.copy(alpha = 0.3f))
                    ) {
                        if (group.progressPercent > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (group.progressPercent / 100f).coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentTeal)
                            )
                        }
                    }
                }

                // Expand Arrow Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                            .border(1.dp, DividerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationState)
                        )
                    }
                }
            }

            // Expanded Details Block
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

                    if (group.loanCount > 1) {
                        // Multi-loan individual sub-cards
                        Text(
                            text = "INDIVIDUAL BANK LOANS (${group.loanCount})",
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
                            group.loans.forEachIndexed { index, loan ->
                                val emi = calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths)
                                val origRepayable = emi * loan.durationMonths
                                val remRepayable = (origRepayable - loan.repaidAmount).coerceAtLeast(0.0)
                                val ratio = if (origRepayable > 0) loan.loanAmount / origRepayable else 1.0
                                val remPrincipal = remRepayable * ratio
                                val loanPct = if (origRepayable > 0) ((loan.repaidAmount / origRepayable) * 100).toFloat().coerceIn(0f, 100f) else 0f
                                val linkedAcc = remember(accounts, loan.accountId) { accounts.find { it.id == loan.accountId } }

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
                                        // Row 1: Header + Action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Loan #${index + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp
                                                )
                                                Text(
                                                    text = "• ${loan.durationMonths}m @ ${loan.interestRate}%",
                                                    color = TextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            // Edit & Delete Action Icons
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { onEditClick(loan) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = AccentBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteClick(loan) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = ExpenseRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Row 2: Metrics
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Principal", fontSize = 10.sp, color = TextMuted)
                                                Text("৳${currencyFormat.format(loan.loanAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            }
                                            Column {
                                                Text("Monthly EMI", fontSize = 10.sp, color = TextMuted)
                                                Text("৳${currencyFormat.format(emi)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = AccentTeal)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Remaining", fontSize = 10.sp, color = TextMuted)
                                                Text("৳${currencyFormat.format(remPrincipal)}", fontSize = 12.5.sp, fontWeight = FontWeight.Bold, color = if (remRepayable <= 0.0) AccentTeal else TextPrimary)
                                            }
                                        }

                                        if (linkedAcc != null) {
                                            Text(
                                                text = "Linked Account: ${linkedAcc.name}",
                                                color = TextSecondary,
                                                fontSize = 11.sp
                                            )
                                        }

                                        // Row 3: Mini Progress Bar + Individual Repay Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(DividerColor.copy(alpha = 0.3f))
                                            ) {
                                                if (loanPct > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction = (loanPct / 100f).coerceIn(0f, 1f))
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(AccentTeal)
                                                    )
                                                }
                                            }

                                            if (remRepayable > 0) {
                                                Button(
                                                    onClick = { onRepayClick(loan) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = AccentTeal,
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = "Pay EMI",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Settled", color = AccentTeal, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    val cardPagerState = rememberPagerState(pageCount = { 2 })

                    HorizontalPager(
                        state = cardPagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        if (page == 0) {
                            // Page 1 (Default): Full-Width Numbers Breakdown
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DetailTextRow(label = "Total Principal", value = "৳${currencyFormat.format(group.totalPrincipal)}", valueColor = TextPrimary)
                                DetailTextRow(label = "Remaining Principal", value = "৳${currencyFormat.format(group.totalRemainingPrincipal)}", valueColor = AccentTeal)
                                DetailTextRow(label = "Remaining Interest", value = "৳${currencyFormat.format(group.totalRemainingInterest)}", valueColor = ExpenseRed)
                                DetailTextRow(label = "Total Remaining Payable", value = "৳${currencyFormat.format(group.totalRemainingRepayable)}", valueColor = TextPrimary)
                                if (group.loanCount == 1 && singleLoan != null) {
                                    val linkedAccount = accounts.find { it.id == singleLoan.accountId }
                                    if (linkedAccount != null) {
                                        DetailTextRow(label = "Account Linked", value = linkedAccount.name, valueColor = bankColor)
                                    }
                                } else {
                                    DetailTextRow(label = "Active Bank Loans", value = "${group.loanCount} Loans", valueColor = bankColor)
                                }
                            }
                        } else {
                            // Page 2: Centered Doughnut Chart & Color Legend
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LoanDoughnutChart(
                                    principalRemaining = group.totalRemainingPrincipal,
                                    interestRemaining = group.totalRemainingInterest,
                                    repaid = group.totalRepaid,
                                    modifier = Modifier.size(105.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LegendBadgeItem(color = AccentBlue, label = "Principal", value = "৳${currencyFormat.format(group.totalRemainingPrincipal)}")
                                    LegendBadgeItem(color = ExpenseRed, label = "Interest", value = "৳${currencyFormat.format(group.totalRemainingInterest)}")
                                    LegendBadgeItem(color = AccentTeal, label = "Repaid", value = "৳${currencyFormat.format(group.totalRepaid)}")
                                }
                            }
                        }
                    }

                    // Page Indicator Dots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { index ->
                            val active = cardPagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (active) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (active) AccentTeal else TextMuted.copy(alpha = 0.4f))
                            )
                        }
                    }

                    // Repay Loan Button or Fully Repaid Indicator
                    if (group.isFullyRepaid) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(AccentTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (group.loanCount > 1) "All Bank Loans Fully Repaid" else "Fully Repaid",
                                    color = AccentTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        val loanToRepay = group.primaryUnpaidLoan ?: group.loans.first()
                        Button(
                            onClick = { onRepayClick(loanToRepay) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Repay Loan", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GroupedPersonalLoanCardItem(
    group: GroupedPersonalLoan,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat,
    onDeleteClick: (LoanEntity) -> Unit,
    onEditClick: (LoanEntity) -> Unit,
    onRepayClick: (LoanEntity) -> Unit,
    onAddAnotherClick: ((String, Boolean) -> Unit)? = null,
    isLent: Boolean = false
) {
    var expanded by remember { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f)

    val personalColor = if (isLent) AccentPurple else AccentBlue
    val singleLoan = group.loans.firstOrNull()

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = BorderStroke(1.dp, personalColor.copy(alpha = 0.25f)),
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Column {
            // Clickable upper part
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp)
            ) {
                // Card Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Personal Indicator Dot
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(personalColor)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = group.lenderName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (group.loanCount > 1) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(personalColor.copy(alpha = 0.15f))
                                        .border(1.dp, personalColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "${group.loanCount} Loans",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = personalColor
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (group.loanCount > 1) {
                                if (isLent) "${group.loanCount} Active Records · Lent" else "${group.loanCount} Active Records · Borrowed"
                            } else {
                                if (isLent) "Lent to Friend / Family" else "Friend / Family Loan"
                            },
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }

                    // Top-right Menu or Action
                    Box {
                        IconButton(
                            onClick = { showMenu = true },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Loan Options",
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            shape = RoundedCornerShape(14.dp),
                            containerColor = CardDarker,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .border(1.dp, DividerColor, RoundedCornerShape(14.dp))
                        ) {
                            if (group.loanCount == 1 && singleLoan != null) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Edit Loan",
                                                tint = AccentBlue,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Edit Loan",
                                                color = TextPrimary,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onEditClick(singleLoan)
                                    }
                                )
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Loan",
                                                tint = ExpenseRed,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = "Delete Loan",
                                                color = ExpenseRed,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onDeleteClick(singleLoan)
                                    }
                                )
                            }
                            if (onAddAnotherClick != null) {
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.Add,
                                                contentDescription = "Add Loan",
                                                tint = AccentTeal,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(
                                                text = if (isLent) "Lend Again to ${group.lenderName}" else "Borrow Again from ${group.lenderName}",
                                                color = AccentTeal,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        }
                                    },
                                    onClick = {
                                        showMenu = false
                                        onAddAnotherClick(group.lenderName, isLent)
                                    }
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Main Info: Amount Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isLent) "Remaining Receivable" else "Remaining Borrowed",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${currencyFormat.format(group.totalRemaining)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }

                    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (isLent) "Total Lent" else "Total Borrowed",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${currencyFormat.format(group.totalPrincipal)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = personalColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Repayment Progress Bar
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Repaid: ${String.format(Locale.US, "%.1f%%", group.progressPercent)}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "৳${currencyFormat.format(group.totalRepaid)} / ৳${currencyFormat.format(group.totalPrincipal)}",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(DividerColor.copy(alpha = 0.3f))
                    ) {
                        if (group.progressPercent > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(fraction = (group.progressPercent / 100f).coerceIn(0f, 1f))
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(AccentTeal)
                            )
                        }
                    }
                }

                // Expand Arrow
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(CardDarker)
                            .border(1.dp, DividerColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = if (expanded) "Collapse" else "Expand",
                            tint = TextPrimary,
                            modifier = Modifier
                                .size(18.dp)
                                .rotate(rotationState)
                        )
                    }
                }
            }

            // Expanded Details Block
            if (expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                ) {
                    HorizontalDivider(color = DividerColor.copy(alpha = 0.5f), modifier = Modifier.padding(bottom = 12.dp))

                    if (group.loanCount > 1) {
                        // Multi-loan itemized breakdown list
                        Text(
                            text = "INDIVIDUAL LOANS (${group.loanCount})",
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
                            group.loans.forEachIndexed { index, loan ->
                                val loanRem = (loan.loanAmount - loan.repaidAmount).coerceAtLeast(0.0)
                                val loanPct = if (loan.loanAmount > 0) ((loan.repaidAmount / loan.loanAmount) * 100).toFloat().coerceIn(0f, 100f) else 0f
                                val linkedAcc = remember(accounts, loan.accountId) { accounts.find { it.id == loan.accountId } }

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
                                        // Row 1: Header + Action buttons
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "Loan #${index + 1}",
                                                    fontWeight = FontWeight.Bold,
                                                    color = TextPrimary,
                                                    fontSize = 13.sp
                                                )
                                                if (linkedAcc != null) {
                                                    Text(
                                                        text = "• ${linkedAcc.name}",
                                                        color = TextMuted,
                                                        fontSize = 11.sp,
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                }
                                            }

                                            // Edit & Delete Action Icons
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                IconButton(
                                                    onClick = { onEditClick(loan) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit",
                                                        tint = AccentBlue,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { onDeleteClick(loan) },
                                                    modifier = Modifier.size(28.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete",
                                                        tint = ExpenseRed,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }

                                        // Row 2: Metrics
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("Principal", fontSize = 10.sp, color = TextMuted)
                                                Text("৳${currencyFormat.format(loan.loanAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                                            }
                                            Column {
                                                Text(if (isLent) "Received" else "Repaid", fontSize = 10.sp, color = TextMuted)
                                                Text("৳${currencyFormat.format(loan.repaidAmount)}", fontSize = 12.5.sp, fontWeight = FontWeight.SemiBold, color = AccentTeal)
                                            }
                                            Column(horizontalAlignment = Alignment.End) {
                                                Text("Remaining", fontSize = 10.sp, color = TextMuted)
                                                Text(
                                                    text = "৳${currencyFormat.format(loanRem)}",
                                                    fontSize = 12.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (loanRem <= 0.0) AccentTeal else personalColor
                                                )
                                            }
                                        }

                                        // Row 3: Mini Progress Bar + Individual Repay Button
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .height(4.dp)
                                                    .clip(RoundedCornerShape(2.dp))
                                                    .background(DividerColor.copy(alpha = 0.3f))
                                            ) {
                                                if (loanPct > 0f) {
                                                    Box(
                                                        modifier = Modifier
                                                            .fillMaxWidth(fraction = (loanPct / 100f).coerceIn(0f, 1f))
                                                            .height(4.dp)
                                                            .clip(RoundedCornerShape(2.dp))
                                                            .background(AccentTeal)
                                                    )
                                                }
                                            }

                                            if (loanRem > 0) {
                                                Button(
                                                    onClick = { onRepayClick(loan) },
                                                    colors = ButtonDefaults.buttonColors(
                                                        containerColor = if (isLent) AccentPurple else AccentTeal,
                                                        contentColor = Color.White
                                                    ),
                                                    shape = RoundedCornerShape(8.dp),
                                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                                                    modifier = Modifier.height(28.dp)
                                                ) {
                                                    Text(
                                                        text = if (isLent) "Receive" else "Repay",
                                                        fontSize = 11.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                }
                                            } else {
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Icon(Icons.Default.CheckCircle, null, tint = AccentTeal, modifier = Modifier.size(13.dp))
                                                    Spacer(modifier = Modifier.width(3.dp))
                                                    Text("Settled", color = AccentTeal, fontSize = 10.5.sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                    }

                    // Visual Pager / Chart Breakdown
                    val cardPagerState = rememberPagerState(pageCount = { 2 })

                    HorizontalPager(
                        state = cardPagerState,
                        modifier = Modifier.fillMaxWidth()
                    ) { page ->
                        if (page == 0) {
                            // Page 1: Consolidated Numbers Breakdown
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DetailTextRow(label = if (isLent) "Total Lent" else "Total Borrowed", value = "৳${currencyFormat.format(group.totalPrincipal)}", valueColor = TextPrimary)
                                DetailTextRow(label = if (isLent) "Total Returned" else "Total Repaid", value = "৳${currencyFormat.format(group.totalRepaid)}", valueColor = AccentTeal)
                                DetailTextRow(label = if (isLent) "Total Still Owed" else "Total Remaining", value = "৳${currencyFormat.format(group.totalRemaining)}", valueColor = personalColor)
                                if (group.loanCount == 1 && singleLoan != null) {
                                    val linkedAccount = accounts.find { it.id == singleLoan.accountId }
                                    if (linkedAccount != null) {
                                        DetailTextRow(label = "Account Linked", value = linkedAccount.name, valueColor = personalColor)
                                    }
                                } else {
                                    DetailTextRow(label = "Total Active Loans", value = "${group.loanCount} Records", valueColor = TextSecondary)
                                }
                            }
                        } else {
                            // Page 2: Centered Doughnut Chart & Color Legend
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LoanDoughnutChart(
                                    principalRemaining = group.totalRemaining,
                                    interestRemaining = 0.0,
                                    repaid = group.totalRepaid,
                                    modifier = Modifier.size(105.dp)
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    LegendBadgeItem(color = personalColor, label = if (isLent) "Remaining" else "Borrowed", value = "৳${currencyFormat.format(group.totalRemaining)}")
                                    LegendBadgeItem(color = AccentTeal, label = if (isLent) "Returned" else "Repaid", value = "৳${currencyFormat.format(group.totalRepaid)}")
                                }
                            }
                        }
                    }

                    // Page Indicator Dots
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        repeat(2) { index ->
                            val active = cardPagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .padding(horizontal = 3.dp)
                                    .size(if (active) 7.dp else 5.dp)
                                    .clip(CircleShape)
                                    .background(if (active) AccentTeal else TextMuted.copy(alpha = 0.4f))
                            )
                        }
                    }

                    // Repay Button or Fully Repaid Indicator
                    if (group.isFullyRepaid) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                                .background(AccentTeal.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                                .border(1.dp, AccentTeal.copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                                .padding(vertical = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = AccentTeal, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (group.loanCount > 1) "All Loans Fully Settled" else "Fully Repaid",
                                    color = AccentTeal,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                            }
                        }
                    } else {
                        val loanToRepay = group.primaryUnpaidLoan ?: group.loans.first()
                        Button(
                            onClick = { onRepayClick(loanToRepay) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isLent) AccentPurple else AccentTeal,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Payment,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                if (isLent) "Record Repayment Received" else "Repay Person",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DetailTextRow(
    label: String,
    value: String,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = TextSecondary, fontSize = 11.5.sp, fontWeight = FontWeight.Medium)
        Text(text = value, color = valueColor, fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun LegendBadgeItem(
    color: Color,
    label: String,
    value: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = TextSecondary,
                lineHeight = 12.sp
            )
            Text(
                text = value,
                fontSize = 11.5.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                lineHeight = 14.sp
            )
        }
    }
}

// ─── Composable: Custom Canvas Doughnut Chart ───────────────
@Composable
fun LoanDoughnutChart(
    principalRemaining: Double,
    interestRemaining: Double,
    repaid: Double,
    modifier: Modifier = Modifier
) {
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(principalRemaining, interestRemaining, repaid) {
        animProgress.animateTo(1f, animationSpec = tween(800, easing = FastOutSlowInEasing))
    }

    val total = principalRemaining + interestRemaining + repaid
    val repaidPct = if (total > 0) (repaid / total).toFloat() else 0f
    val principalPct = if (total > 0) (principalRemaining / total).toFloat() else 0f
    val interestPct = if (total > 0) (interestRemaining / total).toFloat() else 0f

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeW = 8.dp.toPx()
            val canvasSize = size.minDimension
            val radiusSize = canvasSize - strokeW
            val rectOffset = Offset(
                (size.width - radiusSize) / 2f,
                (size.height - radiusSize) / 2f
            )

            // Gray background ring
            drawArc(
                color = DividerColor.copy(alpha = 0.5f),
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = rectOffset,
                size = Size(radiusSize, radiusSize),
                style = Stroke(width = strokeW)
            )

            val rSweep = repaidPct * 360f * animProgress.value
            val pSweep = principalPct * 360f * animProgress.value
            val iSweep = interestPct * 360f * animProgress.value

            // Repaid Sector (AccentTeal)
            if (rSweep > 0f) {
                drawArc(
                    color = AccentTeal,
                    startAngle = -90f,
                    sweepAngle = rSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            }

            // Remaining Principal Sector (AccentBlue)
            if (pSweep > 0f) {
                drawArc(
                    color = AccentBlue,
                    startAngle = -90f + rSweep,
                    sweepAngle = pSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            }

            // Remaining Interest Sector (ExpenseRed)
            if (iSweep > 0f) {
                drawArc(
                    color = ExpenseRed,
                    startAngle = -90f + rSweep + pSweep,
                    sweepAngle = iSweep,
                    useCenter = false,
                    topLeft = rectOffset,
                    size = Size(radiusSize, radiusSize),
                    style = Stroke(width = strokeW, cap = StrokeCap.Butt)
                )
            }
        }

        // Percentage in center
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = String.format(Locale.US, "%.0f%%", repaidPct * 100),
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "Repaid",
                color = TextMuted,
                fontSize = 8.sp
            )
        }
    }
}

// ─── Composable: Add Loan Form Sheet ─────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddLoanFormSheet(
    accounts: List<AccountEntity>,
    initialLoan: LoanEntity? = null,
    initialBankName: String? = null,
    onDismiss: () -> Unit,
    onAddLoan: (bank: String, amount: Double, months: Int, rate: Double, accountId: Int) -> Unit,
    onNavigateToAccounts: () -> Unit,
    currencyFormat: DecimalFormat
) {
    var selectedAccount by remember(accounts, initialLoan, initialBankName) {
        mutableStateOf(
            accounts.find { it.id == initialLoan?.accountId }
                ?: (if (initialBankName != null) accounts.find { it.name.contains(initialBankName, ignoreCase = true) } else null)
                ?: accounts.firstOrNull()
        )
    }
    var amountInput by remember(initialLoan) {
        mutableStateOf(initialLoan?.let { if (it.loanAmount > 0) it.loanAmount.toLong().toString() else "" } ?: "")
    }
    var monthsInput by remember(initialLoan) {
        mutableStateOf(initialLoan?.let { if (it.durationMonths > 0) it.durationMonths.toString() else "" } ?: "")
    }
    var rateInput by remember(initialLoan) {
        mutableStateOf(initialLoan?.let { if (it.interestRate > 0) it.interestRate.toString() else "" } ?: "")
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(amountInput, monthsInput, rateInput) {
        amountInput.trim().isNotEmpty() || monthsInput.trim().isNotEmpty() || rateInput.trim().isNotEmpty()
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = "Discard Loan Changes?",
            message = "Are you sure you want to discard this loan entry? Any entered loan details will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    // Parsed states for calculations
    val parsedAmount = remember(amountInput) { amountInput.toDoubleOrNull() ?: 0.0 }
    val parsedMonths = remember(monthsInput) { monthsInput.toIntOrNull() ?: 0 }
    val parsedRate = remember(rateInput) { rateInput.toDoubleOrNull() ?: 0.0 }

    // Live Calculations
    val liveEmi = remember(parsedAmount, parsedRate, parsedMonths) {
        calculateEmi(parsedAmount, parsedRate, parsedMonths)
    }
    val liveRepayable = remember(liveEmi, parsedMonths) { liveEmi * parsedMonths }
    val liveInterest = remember(liveRepayable, parsedAmount) { (liveRepayable - parsedAmount).coerceAtLeast(0.0) }

    val isFormValid = remember(selectedAccount, parsedAmount, parsedMonths, parsedRate) {
        selectedAccount != null && parsedAmount > 0.0 && parsedMonths > 0 && parsedRate >= 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Bank Accounts Found",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "You must link a bank/MFS account to receive the loan funds and pay EMI.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAccounts()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Bank Account", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(
                text = if (initialLoan != null) "Edit Bank Loan" else "Calculate & Add Loan",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Account Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccount?.let { "${it.name} [${it.accountSubtype}]" } ?: "Select Bank Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Select Account to Deposit Funds", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalance, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = loanTextFieldColors(),
                    trailingIcon = {
                        IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Options", tint = TextPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedDropdown = !expandedDropdown }
                )

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(account.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("৳${currencyFormat.format(account.balance)}", color = AccentTeal, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                selectedAccount = account
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Amount Input (৳)
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Loan Amount (৳)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.MonetizationOn, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Month Repayment Period
            OutlinedTextField(
                value = monthsInput,
                onValueChange = { monthsInput = it.filter { char -> char.isDigit() } },
                label = { Text("Repayment Period (Months)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.CalendarMonth, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Interest Rate (%)
            OutlinedTextField(
                value = rateInput,
                onValueChange = { rateInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Interest Rate (% per year)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Percent, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Calculations Card (Premium View)
            if (parsedAmount > 0 || parsedMonths > 0 || parsedRate > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDarker),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "LIVE ESTIMATIONS", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentTeal)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Monthly EMI:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(liveEmi)}", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Interest:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(liveInterest)}", color = ExpenseRed, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total Repayable:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(liveRepayable)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isFormDirty) {
                            showDiscardDialog = true
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isFormValid && selectedAccount != null) {
                            onAddLoan(
                                selectedAccount!!.name,
                                parsedAmount,
                                parsedMonths,
                                parsedRate,
                                selectedAccount!!.id
                            )
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        contentColor = BackgroundDark,
                        disabledContainerColor = CardDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (initialLoan != null) "Update Loan" else "Add Loan")
                }
            }
        }
    }
}

// ─── Composable: Repay Loan Form Sheet ───────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepayLoanFormSheet(
    loan: LoanEntity,
    account: AccountEntity?,
    accounts: List<AccountEntity>,
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onRepay: (amount: Double, accountId: Int) -> Unit
) {
    val emi = remember(loan) { calculateEmi(loan.loanAmount, loan.interestRate, loan.durationMonths) }
    val originalRepayable = remember(loan, emi) { emi * loan.durationMonths }
    val remainingRepayable = remember(loan, originalRepayable) { (originalRepayable - loan.repaidAmount).coerceAtLeast(0.0) }

    val initialAmount = remember(emi, remainingRepayable) {
        val amount = if (remainingRepayable < emi) remainingRepayable else emi
        String.format(Locale.US, "%.2f", amount)
    }

    var repayAmountInput by remember {
        mutableStateOf(
            TextFieldValue(
                text = initialAmount,
                selection = TextRange(initialAmount.length)
            )
        )
    }
    var selectedAccount by remember { mutableStateOf(account ?: accounts.firstOrNull()) }
    var expandedDropdown by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(repayAmountInput.text, initialAmount) {
        repayAmountInput.text.trim() != initialAmount.trim() && repayAmountInput.text.trim().isNotEmpty()
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = "Discard Repayment?",
            message = "Are you sure you want to discard this repayment? Entered repayment amount will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    val parsedAmount = remember(repayAmountInput.text) { repayAmountInput.text.toDoubleOrNull() ?: 0.0 }
    val accountBalance = remember(selectedAccount) { selectedAccount?.balance ?: 0.0 }

    // Validation
    val isInsufficientBalance = parsedAmount > accountBalance
    val isExceedingRemaining = parsedAmount > remainingRepayable
    val isFormValid = parsedAmount > 0.0 && !isInsufficientBalance && !isExceedingRemaining && selectedAccount != null

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val titleText = if (loan.loanType == "PERSONAL") "Repay Loan - ${loan.lenderName}" else "Repay Loan - ${loan.bankName}"
        Text(
            text = titleText,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        // Payment Account Information
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = selectedAccount?.let { "${it.name} [Bal: ৳${currencyFormat.format(it.balance)}]" } ?: "Select Payment Account",
                onValueChange = {},
                readOnly = true,
                label = { Text("Repay From Account", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.AccountBalance, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                shape = RoundedCornerShape(14.dp),
                colors = loanTextFieldColors(),
                trailingIcon = {
                    IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                        Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Options", tint = TextPrimary)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expandedDropdown = !expandedDropdown }
            )

            DropdownMenu(
                expanded = expandedDropdown,
                onDismissRequest = { expandedDropdown = false },
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardDarker)
                    .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
            ) {
                accounts.forEach { acc ->
                    DropdownMenuItem(
                        text = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(acc.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                Text("৳${currencyFormat.format(acc.balance)}", color = AccentTeal, fontSize = 13.sp)
                            }
                        },
                        onClick = {
                            selectedAccount = acc
                            expandedDropdown = false
                        }
                    )
                }
            }
        }

        // Display current loan details
        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardDarker),
            modifier = Modifier.fillMaxWidth().border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(text = "LOAN SUMMARY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentTeal)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Remaining Payable:", color = TextSecondary, fontSize = 13.sp)
                    Text(text = "৳${currencyFormat.format(remainingRepayable)}", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }

                if (loan.loanType != "PERSONAL") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Monthly EMI Amount:", color = TextSecondary, fontSize = 13.sp)
                        Text(text = "৳${currencyFormat.format(emi)}", color = TextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // Repayment Amount Input
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            OutlinedTextField(
                value = repayAmountInput,
                onValueChange = { newValue ->
                    val filteredText = newValue.text.filter { char -> char.isDigit() || char == '.' }
                    if (filteredText == newValue.text) {
                        repayAmountInput = newValue
                    } else {
                        repayAmountInput = TextFieldValue(
                            text = filteredText,
                            selection = TextRange(filteredText.length)
                        )
                    }
                },
                label = { Text("Repayment Amount (৳)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.MonetizationOn, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor       = TextPrimary,
                    unfocusedTextColor     = TextPrimary,
                    focusedBorderColor     = if (isInsufficientBalance || isExceedingRemaining) ExpenseRed else AccentTeal,
                    unfocusedBorderColor   = if (isInsufficientBalance || isExceedingRemaining) ExpenseRed else DividerColor,
                    focusedContainerColor  = CardDarker,
                    unfocusedContainerColor = CardDarker,
                    focusedLabelColor      = if (isInsufficientBalance || isExceedingRemaining) ExpenseRed else AccentTeal,
                    unfocusedLabelColor    = TextSecondary
                ),
                modifier = Modifier.fillMaxWidth(),
                trailingIcon = {
                    TextButton(
                        onClick = {
                            val payAllText = String.format(Locale.US, "%.2f", remainingRepayable)
                            repayAmountInput = TextFieldValue(text = payAllText, selection = TextRange(payAllText.length))
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("PAY ALL", color = AccentTeal, fontWeight = FontWeight.Bold, fontSize = 11.sp)
                    }
                }
            )

            if (isInsufficientBalance) {
                Text(
                    text = "Insufficient balance in selected account (৳${currencyFormat.format(accountBalance)})",
                    color = ExpenseRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            } else if (isExceedingRemaining) {
                Text(
                    text = "Amount exceeds the remaining loan balance (৳${currencyFormat.format(remainingRepayable)})",
                    color = ExpenseRed,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = {
                    if (isFormDirty) {
                        showDiscardDialog = true
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                border = BorderStroke(1.dp, DividerColor),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Cancel")
            }

            Button(
                onClick = {
                    if (isFormValid && selectedAccount != null) {
                        onRepay(parsedAmount, selectedAccount!!.id)
                    }
                },
                enabled = isFormValid,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentTeal,
                    contentColor = BackgroundDark,
                    disabledContainerColor = CardDark,
                    disabledContentColor = TextMuted
                ),
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Confirm Repay", fontWeight = FontWeight.Bold)
            }
        }
    }
}

// ─── Composable: Add Personal Loan Form Sheet ────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddPersonalLoanFormSheet(
    accounts: List<AccountEntity>,
    payees: List<PayeeEntity> = emptyList(),
    isLent: Boolean = false,
    initialLoan: LoanEntity? = null,
    initialLenderName: String? = null,
    onDismiss: () -> Unit,
    onAddLoan: (lender: String, amount: Double, accountId: Int) -> Unit,
    onNavigateToAccounts: () -> Unit,
    currencyFormat: DecimalFormat
) {
    var selectedAccount by remember(accounts, initialLoan) {
        mutableStateOf(accounts.find { it.id == initialLoan?.accountId } ?: accounts.firstOrNull())
    }
    var lenderInput by remember(initialLoan, initialLenderName) {
        mutableStateOf(initialLoan?.lenderName ?: initialLenderName ?: "")
    }
    var amountInput by remember(initialLoan) {
        mutableStateOf(initialLoan?.let { if (it.loanAmount > 0) it.loanAmount.toLong().toString() else "" } ?: "")
    }

    var expandedDropdown by remember { mutableStateOf(false) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(lenderInput, amountInput) {
        lenderInput.trim().isNotEmpty() || amountInput.trim().isNotEmpty()
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = if (isLent) "Discard Lent Entry?" else "Discard Borrowed Entry?",
            message = "Are you sure you want to discard? Any entered personal loan details will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    // Parsed states for calculations
    val parsedAmount = remember(amountInput) { amountInput.toDoubleOrNull() ?: 0.0 }

    val isFormValid = remember(selectedAccount, lenderInput, parsedAmount) {
        selectedAccount != null && lenderInput.trim().isNotEmpty() && parsedAmount > 0.0
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(androidx.compose.foundation.rememberScrollState())
            .padding(20.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (accounts.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.AccountBalance,
                    contentDescription = null,
                    tint = TextMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No Accounts Found",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isLent) "You must link a bank/MFS account to deduct the lent funds from." else "You must link a bank/MFS account to receive the loan funds.",
                    color = TextMuted,
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onDismiss()
                        onNavigateToAccounts()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AccentTeal, contentColor = BackgroundDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Account", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Text(
                text = if (initialLoan != null) (if (isLent) "Edit Lent Money" else "Edit Borrowed Money") else (if (isLent) "Lend Money to Friend / Family" else "Borrow from Friend / Family"),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            // Account Selection Dropdown
            Box(modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = selectedAccount?.let { "${it.name} [${it.accountSubtype}]" } ?: "Select Account",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(if (isLent) "Select Account to Deduct Funds From" else "Select Account to Deposit Funds", color = TextSecondary) },
                    leadingIcon = {
                        Icon(Icons.Default.AccountBalance, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                    },
                    shape = RoundedCornerShape(14.dp),
                    colors = loanTextFieldColors(),
                    trailingIcon = {
                        IconButton(onClick = { expandedDropdown = !expandedDropdown }) {
                            Icon(imageVector = Icons.Default.ArrowDropDown, contentDescription = "Dropdown Options", tint = TextPrimary)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expandedDropdown = !expandedDropdown }
                )

                DropdownMenu(
                    expanded = expandedDropdown,
                    onDismissRequest = { expandedDropdown = false },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(CardDarker)
                        .border(1.dp, DividerColor, RoundedCornerShape(12.dp))
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(account.name, color = TextPrimary, fontWeight = FontWeight.Medium)
                                    Text("৳${currencyFormat.format(account.balance)}", color = AccentTeal, fontSize = 13.sp)
                                }
                            },
                            onClick = {
                                selectedAccount = account
                                expandedDropdown = false
                            }
                        )
                    }
                }
            }

            // Person Name Input
            OutlinedTextField(
                value = lenderInput,
                onValueChange = { lenderInput = it },
                label = { Text(if (isLent) "Borrower Name (Friend / Family)" else "Lender Name (Friend / Family)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.Person, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Saved Recipient / Friend Suggestions
            if (payees.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "SAVED RECIPIENTS / FRIENDS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextMuted
                    )
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(payees) { payee ->
                            val isSelected = lenderInput.equals(payee.name, ignoreCase = true)
                            FilterChip(
                                selected = isSelected,
                                onClick = { lenderInput = payee.name },
                                label = { Text(payee.name, fontSize = 12.sp) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Default.Person,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = if (isSelected) AccentTeal else TextSecondary
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AccentTeal.copy(alpha = 0.2f),
                                    selectedLabelColor = AccentTeal,
                                    containerColor = CardDarker,
                                    labelColor = TextPrimary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    borderColor = if (isSelected) AccentTeal else DividerColor,
                                    selectedBorderColor = AccentTeal,
                                    enabled = true,
                                    selected = isSelected
                                ),
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }

            // Amount Input (৳)
            OutlinedTextField(
                value = amountInput,
                onValueChange = { amountInput = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Loan Amount (৳)", color = TextSecondary) },
                leadingIcon = {
                    Icon(Icons.Default.MonetizationOn, null, tint = AccentTeal, modifier = Modifier.size(20.dp))
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = loanTextFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // Dynamic Info / Confirmation Preview Card
            if (lenderInput.trim().isNotEmpty() && parsedAmount > 0) {
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = CardDarker),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(BorderStroke(1.dp, DividerColor), RoundedCornerShape(12.dp))
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "SUMMARY PREVIEW", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentBlue)

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = if (isLent) "Lending to:" else "Borrowing from:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = lenderInput, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Principal Amount:", color = TextSecondary, fontSize = 13.sp)
                            Text(text = "৳${currencyFormat.format(parsedAmount)}", color = AccentTeal, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Action Buttons Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        if (isFormDirty) {
                            showDiscardDialog = true
                        } else {
                            onDismiss()
                        }
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                    border = BorderStroke(1.dp, DividerColor),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        if (isFormValid && selectedAccount != null) {
                            onAddLoan(
                                lenderInput.trim(),
                                parsedAmount,
                                selectedAccount!!.id
                            )
                        }
                    },
                    enabled = isFormValid,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AccentTeal,
                        contentColor = BackgroundDark,
                        disabledContainerColor = CardDark,
                        disabledContentColor = TextMuted
                    ),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(if (initialLoan != null) "Update Loan" else if (isLent) "Lend Money" else "Add Personal Loan")
                }
            }
        }
    }
}
