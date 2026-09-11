package com.shejan.financebuddy.ui.goals

import com.shejan.financebuddy.data.db.AccountEntity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.activity.compose.BackHandler
import com.shejan.financebuddy.ui.common.DiscardChangesDialog
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.ui.window.Dialog
import com.shejan.financebuddy.ui.theme.*
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shejan.financebuddy.data.db.GoalEntity
import com.shejan.financebuddy.ui.theme.AccentBlue
import com.shejan.financebuddy.ui.theme.AccentPurple
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
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

// ─────────────────────────────────────────────────────────────
// Curated Emoji & Color options for goal creation
// ─────────────────────────────────────────────────────────────

private val goalEmojis = listOf(
    "🎯", "🏠", "✈️", "💻", "🚗", "📱", "🎓", "💍",
    "🏖️", "🎮", "💪", "🏦", "🛒", "🎁", "⚕️", "🌟"
)

// ─────────────────────────────────────────────────────────────
// Goals Screen Root
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen(
    goals: List<GoalEntity>,
    accounts: List<AccountEntity> = emptyList(),
    onAddGoal: (GoalEntity) -> Unit,
    onUpdateGoal: (GoalEntity) -> Unit = onAddGoal,
    onDeposit: (goalId: Int, amount: Double, fromAccountId: Int?) -> Unit,
    onDeleteGoal: (GoalEntity) -> Unit,
    triggerAddSheet: Boolean = false,
    onResetTriggerAddSheet: () -> Unit = {}
) {
    val currencyFormat = remember { DecimalFormat("##,##,##0.00") }

    val totalTarget = goals.sumOf { it.targetAmount }
    val totalSaved  = goals.sumOf { it.savedAmount }
    val completed   = goals.count { it.savedAmount >= it.targetAmount }

    var showAddSheet     by remember { mutableStateOf(false) }
    var editingGoal      by remember { mutableStateOf<GoalEntity?>(null) }
    var deletingGoal     by remember { mutableStateOf<GoalEntity?>(null) }
    var depositGoal      by remember { mutableStateOf<GoalEntity?>(null) }
    var showInfoDialog   by remember { mutableStateOf(false) }

    LaunchedEffect(triggerAddSheet) {
        if (triggerAddSheet) {
            editingGoal = null
            showAddSheet = true
            onResetTriggerAddSheet()
        }
    }

    val addSheetState     = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val depositSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Goal Info Dialog
    if (showInfoDialog) {
        GoalInfoDialog(onDismiss = { showInfoDialog = false })
    }

    // Delete Confirmation Dialog
    deletingGoal?.let { goal ->
        Dialog(onDismissRequest = { deletingGoal = null }) {
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
                        text = "Delete Goal?",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Are you sure you want to delete your savings goal \"${goal.title}\"?",
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
                            onClick = { deletingGoal = null },
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
                                onDeleteGoal(goal)
                                deletingGoal = null
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
                                    color = androidx.compose.ui.graphics.Color.White,
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
        // Ambient glow
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(AccentPurple.copy(alpha = 0.06f), Color.Transparent)
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
                // ── Summary overview card ────────────────────────
                item {
                    GoalsSummaryCard(
                        totalTarget    = totalTarget,
                        totalSaved     = totalSaved,
                        activeCount    = goals.size - completed,
                        completedCount = completed,
                        currencyFormat = currencyFormat,
                        onInfoClick    = { showInfoDialog = true }
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }

                // ── Section header ───────────────────────────────
                item {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        Text(
                            text       = "My Goals",
                            style      = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        Text(
                            text  = "${goals.size} total",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }

                // ── Empty state ──────────────────────────────────
                if (goals.isEmpty()) {
                    item { GoalsEmptyState() }
                }

                // ── Goal cards ───────────────────────────────────
                items(goals, key = { it.id }) { goal ->
                    AnimatedVisibility(
                        visible = true,
                        enter   = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 4 }
                    ) {
                        GoalCard(
                            goal           = goal,
                            currencyFormat = currencyFormat,
                            onDeposit      = { depositGoal = goal },
                            onEdit         = {
                                editingGoal = goal
                                showAddSheet = true
                            },
                            onDelete       = { deletingGoal = goal }
                        )
                    }
                }
                item {
                    Spacer(modifier = Modifier.height(120.dp))
                }
            }
        }

        // Add / Edit Goal sheet
        if (showAddSheet) {
            AddGoalSheet(
                sheetState = addSheetState,
                goalToEdit = editingGoal,
                onDismiss  = {
                    showAddSheet = false
                    editingGoal = null
                },
                onSave     = { goal ->
                    if (editingGoal != null) {
                        onUpdateGoal(goal)
                    } else {
                        onAddGoal(goal)
                    }
                    showAddSheet = false
                    editingGoal = null
                }
            )
        }

        // Deposit sheet
        depositGoal?.let { goal ->
            DepositSheet(
                sheetState     = depositSheetState,
                goal           = goal,
                accounts       = accounts,
                currencyFormat = currencyFormat,
                onDismiss      = { depositGoal = null },
                onConfirm      = { amount, fromAccountId ->
                    onDeposit(goal.id, amount, fromAccountId)
                    depositGoal = null
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
                verticalAlignment     = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text       = "Goals",
                    style      = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color      = TextPrimary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (completed > 0) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(IncomeGreen.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text      = "🎉 $completed Completed",
                                fontSize  = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color     = IncomeGreen
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(AccentPurple.copy(alpha = 0.15f))
                            .clickable { showAddSheet = true }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Goal",
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Goal",
                                color = AccentPurple,
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
// Summary Arc Card (totals across all goals)
// ─────────────────────────────────────────────────────────────

@Composable
private fun GoalsSummaryCard(
    totalTarget: Double,
    totalSaved: Double,
    activeCount: Int,
    completedCount: Int,
    currencyFormat: DecimalFormat,
    onInfoClick: () -> Unit = {}
) {
    val progress = if (totalTarget > 0) (totalSaved / totalTarget).toFloat().coerceIn(0f, 1f) else 0f
    val animProgress = remember { Animatable(0f) }
    LaunchedEffect(progress) { animProgress.animateTo(progress, animationSpec = tween(1000)) }

    val arcColor = when {
        progress >= 1f   -> IncomeGreen
        progress >= 0.7f -> AccentTeal
        else             -> AccentPurple
    }

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
                    contentDescription = "Goals Information",
                    tint               = TextMuted,
                    modifier           = Modifier.size(18.dp)
                )
            }

            Column(
                modifier            = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier          = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Circular arc
                    Box(
                        modifier         = Modifier.size(110.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeW    = 14.dp.toPx()
                            val inset      = strokeW / 2
                            val arcRect    = Size(size.width - inset * 2, size.height - inset * 2)
                            val arcOffset  = Offset(inset, inset)

                            // Track
                            drawArc(
                                color      = CardDarker,
                                startAngle = 135f,
                                sweepAngle = 270f,
                                useCenter  = false,
                                topLeft    = arcOffset,
                                size       = arcRect,
                                style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                            // Fill
                            if (animProgress.value > 0f) {
                                drawArc(
                                    color      = arcColor,
                                    startAngle = 135f,
                                    sweepAngle = animProgress.value * 270f,
                                    useCenter  = false,
                                    topLeft    = arcOffset,
                                    size       = arcRect,
                                    style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text       = "${(animProgress.value * 100).toInt()}%",
                                style      = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color      = arcColor,
                                fontSize   = 20.sp
                            )
                            Text(text = "saved", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        }
                    }

                    Spacer(modifier = Modifier.width(20.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text       = "Total Progress",
                            style      = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color      = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        StatRow(label = "Saved",  value = "৳${currencyFormat.format(totalSaved)}",  color = arcColor)
                        Spacer(modifier = Modifier.height(4.dp))
                        StatRow(label = "Target", value = "৳${currencyFormat.format(totalTarget)}", color = TextSecondary)
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = DividerColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            MiniStat(label = "Active",    value = "$activeCount",    color = AccentTeal)
                            MiniStat(label = "Completed", value = "$completedCount", color = IncomeGreen)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GoalInfoDialog(
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
                    text = "About Savings Goals",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Set targets, build savings, and track your financial milestones.",
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
                    GoalInfoRow(
                        title = "Target Milestones",
                        description = "Create customized savings targets with personalized emojis, deadlines, and visual themes."
                    )
                    GoalInfoRow(
                        title = "Progress Tracking",
                        description = "Monitor real-time saved percentages and remaining amounts toward completing each goal."
                    )
                    GoalInfoRow(
                        title = "Seamless Deposits",
                        description = "Deposit funds directly from your linked accounts with automated balance and transaction logging."
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
                            .background(Brush.horizontalGradient(colors = listOf(AccentPurple, GradientEnd))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Got It",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = OnAccent
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalInfoRow(title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(AccentPurple)
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
private fun StatRow(label: String, value: String, color: Color) {
    Row(
        modifier              = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        Text(text = value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, color = color)
    }
}

@Composable
private fun MiniStat(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = color)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, fontSize = 10.sp)
    }
}

// ─────────────────────────────────────────────────────────────
// Individual Goal Card
// ─────────────────────────────────────────────────────────────

@Composable
fun GoalCard(
    goal: GoalEntity,
    currencyFormat: DecimalFormat,
    onDeposit: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val progress = if (goal.targetAmount > 0)
        (goal.savedAmount / goal.targetAmount).toFloat().coerceIn(0f, 1f)
    else 0f

    val animProgress = remember(goal.savedAmount) { Animatable(0f) }
    LaunchedEffect(progress) { animProgress.animateTo(progress, animationSpec = tween(900)) }

    val isCompleted = goal.savedAmount >= goal.targetAmount
    val accentColor = remember(isCompleted) {
        if (isCompleted) IncomeGreen else AccentPurple
    }
    val remaining   = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
    var showMenu    by remember { mutableStateOf(false) }

    // Deadline calculation
    val deadlineText = goal.deadline?.let { dl ->
        val now  = System.currentTimeMillis()
        val diff = dl - now
        when {
            diff < 0                          -> "⚠️ Overdue"
            diff < TimeUnit.DAYS.toMillis(1)  -> "Due today"
            diff < TimeUnit.DAYS.toMillis(30) -> "${TimeUnit.MILLISECONDS.toDays(diff)}d left"
            else -> {
                val months = TimeUnit.MILLISECONDS.toDays(diff) / 30
                "${months}mo left"
            }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp),
        shape  = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (isCompleted) IncomeGreen.copy(alpha = 0.35f) else DividerColor
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            // Header row
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Emoji icon container with square border
                Box(
                    modifier         = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.12f))
                        .border(1.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = goal.emoji, fontSize = 18.sp)
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text          = goal.title,
                        style         = MaterialTheme.typography.titleSmall,
                        fontSize      = 13.5.sp,
                        fontWeight    = FontWeight.SemiBold,
                        color         = TextPrimary,
                        maxLines      = 1,
                        overflow      = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment     = Alignment.CenterVertically
                    ) {
                        if (isCompleted) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(IncomeGreen.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("🎉 Completed!", fontSize = 10.sp, color = IncomeGreen, fontWeight = FontWeight.Bold)
                            }
                        }
                        deadlineText?.let {
                            val dlColor = if (it.startsWith("⚠️")) ExpenseRed else TextSecondary
                            Text(text = it, style = MaterialTheme.typography.labelSmall, color = dlColor)
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
                            contentDescription = "Goal Options",
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
                                        tint = AccentPurple,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "Edit Goal",
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
                                        text = "Delete Goal",
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

            Spacer(modifier = Modifier.height(18.dp))

            // Progress ring + stats
            Row(
                modifier          = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular progress ring
                Box(
                    modifier         = Modifier.size(72.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeW   = 9.dp.toPx()
                        val inset     = strokeW / 2
                        val arcRect   = Size(size.width - inset * 2, size.height - inset * 2)
                        val arcOffset = Offset(inset, inset)

                        drawArc(
                            color      = CardDarker,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter  = false,
                            topLeft    = arcOffset,
                            size       = arcRect,
                            style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                        if (animProgress.value > 0f) {
                            drawArc(
                                color      = accentColor,
                                startAngle = -90f,
                                sweepAngle = animProgress.value * 360f,
                                useCenter  = false,
                                topLeft    = arcOffset,
                                size       = arcRect,
                                style      = Stroke(width = strokeW, cap = StrokeCap.Round)
                            )
                        }
                    }
                    Text(
                        text       = "${(animProgress.value * 100).toInt()}%",
                        fontSize   = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color      = if (isCompleted) IncomeGreen else accentColor
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text  = "৳${currencyFormat.format(goal.savedAmount)}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isCompleted) IncomeGreen else accentColor
                    )
                    Text(
                        text  = "of ৳${currencyFormat.format(goal.targetAmount)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (!isCompleted) {
                        Text(
                            text  = "৳${currencyFormat.format(remaining)} to go",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }

                // Deposit button (hidden when goal is completed)
                if (!isCompleted) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(Brush.linearGradient(colors = listOf(accentColor.copy(alpha = 0.2f), accentColor.copy(alpha = 0.1f))))
                            .clickable { onDeposit() }
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text       = "+ Add",
                            fontSize   = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Empty State
// ─────────────────────────────────────────────────────────────

@Composable
private fun GoalsEmptyState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(CardDark)
            .padding(vertical = 52.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "🎯", fontSize = 42.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text       = "No savings goals yet",
                color      = TextPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize   = 16.sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text       = "Tap + to set your first goal\nand start saving today",
                color      = TextMuted,
                fontSize   = 13.sp,
                textAlign  = TextAlign.Center,
                lineHeight = 20.sp
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Add / Edit Goal Bottom Sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGoalSheet(
    sheetState: androidx.compose.material3.SheetState,
    goalToEdit: GoalEntity? = null,
    onDismiss: () -> Unit,
    onSave: (GoalEntity) -> Unit
) {
    val initialTitle = remember(goalToEdit) { goalToEdit?.title ?: "" }
    val initialTarget = remember(goalToEdit) {
        goalToEdit?.targetAmount?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: ""
    }
    val initialEmoji = remember(goalToEdit) { goalToEdit?.emoji ?: goalEmojis.first() }

    var title          by remember(initialTitle) { mutableStateOf(initialTitle) }
    var targetAmount   by remember(initialTarget) { mutableStateOf(initialTarget) }
    var selectedEmoji  by remember(initialEmoji) { mutableStateOf(initialEmoji) }
    var error          by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(title, targetAmount, selectedEmoji, initialTitle, initialTarget, initialEmoji) {
        if (goalToEdit != null) {
            title != initialTitle || targetAmount != initialTarget || selectedEmoji != initialEmoji
        } else {
            title.trim().isNotEmpty() || targetAmount.trim().isNotEmpty()
        }
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = if (goalToEdit != null) "Discard Goal Changes?" else "Discard Goal?",
            message = "Are you sure you want to discard your changes? Any entered goal details will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
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
        containerColor   = DrawerBackground,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp)
        ) {
            Text(
                text       = if (goalToEdit != null) "Edit Savings Goal" else "New Savings Goal",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                modifier   = Modifier.fillMaxWidth(),
                textAlign  = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(22.dp))

            // Goal name
            Text(
                text       = "Goal Name",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value         = title,
                onValueChange = { title = it; error = null },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("e.g. Emergency Fund", color = TextMuted) },
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentPurple,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor     = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor   = androidx.compose.ui.graphics.Color.White,
                    cursorColor          = AccentPurple
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Target amount
            Text(
                text       = "Target Amount (৳)",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = TextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value         = targetAmount,
                onValueChange = { targetAmount = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("e.g. 50000", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = AccentPurple,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor     = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor   = androidx.compose.ui.graphics.Color.White,
                    cursorColor          = AccentPurple
                )
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Emoji picker
            Text(
                text       = "Pick an Emoji",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = TextSecondary
            )
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement   = Arrangement.spacedBy(8.dp)
            ) {
                goalEmojis.forEach { emoji ->
                    val isSelected = selectedEmoji == emoji
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) AccentPurple.copy(alpha = 0.25f) else Color(0xFF263045)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) AccentPurple else DividerColor,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { selectedEmoji = emoji },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = emoji, fontSize = 20.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error!!, color = ExpenseRed, fontSize = 12.sp)
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Save button
            Button(
                onClick = {
                    val amount = targetAmount.toDoubleOrNull()
                    when {
                        title.isBlank()                -> error = "Please enter a goal name"
                        amount == null || amount <= 0  -> error = "Please enter a valid target amount"
                        else -> {
                            onSave(
                                if (goalToEdit != null) {
                                    goalToEdit.copy(
                                        title        = title.trim(),
                                        targetAmount = amount,
                                        emoji        = selectedEmoji
                                    )
                                } else {
                                    GoalEntity(
                                        title        = title.trim(),
                                        targetAmount = amount,
                                        savedAmount  = 0.0,
                                        colorHex     = "#7C5CFC",
                                        emoji        = selectedEmoji,
                                        deadline     = null,
                                        createdAt    = System.currentTimeMillis()
                                    )
                                }
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
                        .background(Brush.horizontalGradient(colors = listOf(AccentPurple, GradientEnd))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = if (goalToEdit != null) "Update Goal" else "Create Goal",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnAccent
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────
// Deposit Bottom Sheet
// ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepositSheet(
    sheetState: androidx.compose.material3.SheetState,
    goal: GoalEntity,
    accounts: List<AccountEntity> = emptyList(),
    currencyFormat: DecimalFormat,
    onDismiss: () -> Unit,
    onConfirm: (Double, Int?) -> Unit
) {
    var amount by remember { mutableStateOf("") }
    var selectedAccountId by remember { mutableStateOf<Int?>(accounts.firstOrNull()?.id) }
    var error  by remember { mutableStateOf<String?>(null) }
    var showDiscardDialog by remember { mutableStateOf(false) }

    val isFormDirty = remember(amount) {
        amount.trim().isNotEmpty()
    }

    BackHandler(enabled = isFormDirty) {
        showDiscardDialog = true
    }

    if (showDiscardDialog) {
        DiscardChangesDialog(
            title = "Discard Deposit?",
            message = "Are you sure you want to discard this deposit? Entered amount will be lost.",
            onDismissRequest = { showDiscardDialog = false },
            onConfirmDiscard = {
                showDiscardDialog = false
                onDismiss()
            }
        )
    }

    val accentColor = remember(goal.savedAmount >= goal.targetAmount) {
        if (goal.savedAmount >= goal.targetAmount) IncomeGreen else AccentPurple
    }

    val currentProgress = if (goal.targetAmount > 0)
        (goal.savedAmount / goal.targetAmount * 100).toInt()
    else 0

    ModalBottomSheet(
        onDismissRequest = {
            if (isFormDirty) {
                showDiscardDialog = true
            } else {
                onDismiss()
            }
        },
        sheetState       = sheetState,
        containerColor   = DrawerBackground,
        shape            = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = goal.emoji, fontSize = 36.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text       = "Add Savings",
                style      = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color      = TextPrimary,
                textAlign  = TextAlign.Center
            )
            Text(
                text      = goal.title,
                style     = MaterialTheme.typography.bodyMedium,
                color     = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(18.dp))

            // Progress summary
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF263045))
                    .border(1.dp, DividerColor, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Saved", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text       = "৳${currencyFormat.format(goal.savedAmount)}",
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Progress", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text       = "$currentProgress%",
                            fontWeight = FontWeight.Bold,
                            color      = accentColor
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Target", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
                        Text(
                            text       = "৳${currencyFormat.format(goal.targetAmount)}",
                            fontWeight = FontWeight.Bold,
                            color      = androidx.compose.ui.graphics.Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Amount input
            Text(
                "Deposit Amount (৳)",
                style      = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color      = TextSecondary,
                modifier   = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value         = amount,
                onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' }; error = null },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = { Text("e.g. 5000", color = TextMuted) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine    = true,
                shape         = RoundedCornerShape(16.dp),
                colors        = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor   = accentColor,
                    unfocusedBorderColor = DividerColor,
                    focusedTextColor     = androidx.compose.ui.graphics.Color.White,
                    unfocusedTextColor   = androidx.compose.ui.graphics.Color.White,
                    cursorColor          = accentColor
                )
            )

            // Optional Account Selector
            if (accounts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    "Deduct From Account",
                    style      = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color      = TextSecondary,
                    modifier   = Modifier.align(Alignment.Start)
                )
                Spacer(modifier = Modifier.height(6.dp))
                androidx.compose.foundation.layout.FlowRow(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp)
                ) {
                    // "None / Record Only" Chip
                    val isNoneSelected = selectedAccountId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isNoneSelected) AccentTeal.copy(alpha = 0.25f) else Color(0xFF263045))
                            .border(1.dp, if (isNoneSelected) AccentTeal else DividerColor, RoundedCornerShape(10.dp))
                            .clickable { selectedAccountId = null; error = null }
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text       = "Manual (No Account)",
                            color      = if (isNoneSelected) AccentTeal else androidx.compose.ui.graphics.Color.White,
                            fontSize   = 11.sp,
                            fontWeight = if (isNoneSelected) FontWeight.Bold else FontWeight.SemiBold
                        )
                    }

                    accounts.forEach { acc ->
                        val isSelected = selectedAccountId == acc.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) AccentTeal.copy(alpha = 0.25f) else Color(0xFF263045))
                                .border(1.dp, if (isSelected) AccentTeal else DividerColor, RoundedCornerShape(10.dp))
                                .clickable { selectedAccountId = acc.id; error = null }
                                .padding(horizontal = 12.dp, vertical = 7.dp)
                        ) {
                            Text(
                                text       = "${acc.name} (৳${currencyFormat.format(acc.balance)})",
                                color      = if (isSelected) AccentTeal else androidx.compose.ui.graphics.Color.White,
                                fontSize   = 11.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            if (error != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(text = error!!, color = ExpenseRed, fontSize = 12.sp, modifier = Modifier.align(Alignment.Start))
            }

            Spacer(modifier = Modifier.height(22.dp))

            Button(
                onClick = {
                    val dep = amount.toDoubleOrNull()
                    if (dep == null || dep <= 0) {
                        error = "Please enter a valid amount"
                        return@Button
                    }
                    val chosenAcc = accounts.find { it.id == selectedAccountId }
                    if (chosenAcc != null && dep > chosenAcc.balance) {
                        error = "Insufficient balance in ${chosenAcc.name} (Available: ৳${currencyFormat.format(chosenAcc.balance)})"
                        return@Button
                    }
                    onConfirm(dep, selectedAccountId)
                },
                modifier       = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape          = RoundedCornerShape(16.dp),
                colors         = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Brush.horizontalGradient(colors = listOf(accentColor, accentColor.copy(alpha = 0.7f)))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text  = "Add Savings",
                        style = MaterialTheme.typography.titleMedium,
                        color = OnAccent
                    )
                }
            }
        }
    }
}
