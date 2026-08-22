package com.shejan.financebuddy.data.repository

import com.shejan.financebuddy.data.db.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class FinanceRepository(
    private val database: FinanceDatabase,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val accountDao = database.accountDao()
    private val transactionDao = database.transactionDao()
    private val budgetDao = database.budgetDao()
    private val goalDao = database.goalDao()
    private val loanDao = database.loanDao()
    private val investmentDao = database.investmentDao()
    private val payeeDao = database.payeeDao()

    // ─── Accounts ──────────────────────────────────────────────────────────
    val allAccounts: Flow<List<AccountEntity>> = accountDao.getAllAccounts()

    suspend fun getAccountById(id: Int): AccountEntity? = withContext(ioDispatcher) {
        accountDao.getAccountById(id)
    }

    suspend fun insertAccount(account: AccountEntity): Long = withContext(ioDispatcher) {
        accountDao.insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity): Unit = withContext(ioDispatcher) {
        accountDao.updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity): Unit = withContext(ioDispatcher) {
        accountDao.deleteAccount(account)
    }

    // ─── Transactions ──────────────────────────────────────────────────────
    val allTransactions: Flow<List<TransactionEntity>> = transactionDao.getAllTransactions()

    fun getRecentTransactions(limit: Int): Flow<List<TransactionEntity>> =
        transactionDao.getRecentTransactions(limit)

    fun getTransactionsByPeriod(from: Long, to: Long): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByPeriod(from, to)

    fun getMonthlyIncome(start: Long): Flow<Double?> =
        transactionDao.getMonthlyIncome(start)

    fun getMonthlyExpenses(start: Long): Flow<Double?> =
        transactionDao.getMonthlyExpenses(start)

    fun getIncomeSumByPeriod(from: Long, to: Long): Flow<Double?> =
        transactionDao.getIncomeSumByPeriod(from, to)

    fun getExpenseSumByPeriod(from: Long, to: Long): Flow<Double?> =
        transactionDao.getExpenseSumByPeriod(from, to)

    fun getExpensesByCategoryFromDate(start: Long): Flow<List<CategoryExpenseSum>> =
        transactionDao.getExpensesByCategoryFromDate(start)

    suspend fun insertTransaction(transaction: TransactionEntity): Long = withContext(ioDispatcher) {
        transactionDao.insertTransaction(transaction)
    }

    suspend fun updateTransaction(oldTx: TransactionEntity, newTx: TransactionEntity): Unit = withContext(ioDispatcher) {
        transactionDao.updateTransaction(oldTx, newTx)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity): Unit = withContext(ioDispatcher) {
        transactionDao.deleteTransaction(transaction)
    }

    // ─── Budgets ───────────────────────────────────────────────────────────
    val allBudgets: Flow<List<BudgetEntity>> = budgetDao.getAllBudgets()

    suspend fun insertBudget(budget: BudgetEntity): Long = withContext(ioDispatcher) {
        budgetDao.insertBudget(budget)
    }

    suspend fun updateBudget(budget: BudgetEntity): Unit = withContext(ioDispatcher) {
        budgetDao.updateBudget(budget)
    }

    suspend fun deleteBudget(budget: BudgetEntity): Unit = withContext(ioDispatcher) {
        budgetDao.deleteBudget(budget)
    }

    // ─── Goals ─────────────────────────────────────────────────────────────
    val allGoals: Flow<List<GoalEntity>> = goalDao.getAllGoals()

    suspend fun insertGoal(goal: GoalEntity): Long = withContext(ioDispatcher) {
        goalDao.insertGoal(goal)
    }

    suspend fun updateGoal(goal: GoalEntity): Unit = withContext(ioDispatcher) {
        goalDao.updateGoal(goal)
    }

    suspend fun deleteGoal(goal: GoalEntity): Unit = withContext(ioDispatcher) {
        goalDao.deleteGoal(goal)
    }

    // ─── Loans ─────────────────────────────────────────────────────────────
    val allLoans: Flow<List<LoanEntity>> = loanDao.getAllLoans()

    suspend fun insertLoan(loan: LoanEntity): Long = withContext(ioDispatcher) {
        loanDao.insertLoan(loan)
    }

    suspend fun updateLoan(loan: LoanEntity): Unit = withContext(ioDispatcher) {
        loanDao.updateLoan(loan)
    }

    suspend fun deleteLoan(loan: LoanEntity): Unit = withContext(ioDispatcher) {
        loanDao.deleteLoan(loan)
    }

    // ─── Investments ───────────────────────────────────────────────────────
    val allInvestments: Flow<List<InvestmentEntity>> = investmentDao.getAllInvestments()

    suspend fun insertInvestment(investment: InvestmentEntity): Long = withContext(ioDispatcher) {
        investmentDao.insertInvestment(investment)
    }

    suspend fun updateInvestment(investment: InvestmentEntity): Unit = withContext(ioDispatcher) {
        investmentDao.updateInvestment(investment)
    }

    suspend fun deleteInvestment(investment: InvestmentEntity): Unit = withContext(ioDispatcher) {
        investmentDao.deleteInvestment(investment)
    }

    // ─── Payees ────────────────────────────────────────────────────────────
    val allPayees: Flow<List<PayeeEntity>> = payeeDao.getAllPayees()
    val allPayeeAccounts: Flow<List<PayeeAccountEntity>> = payeeDao.getAllPayeeAccounts()

    suspend fun insertPayee(payee: PayeeEntity): Long = withContext(ioDispatcher) {
        payeeDao.insertPayee(payee)
    }

    suspend fun updatePayee(payee: PayeeEntity): Unit = withContext(ioDispatcher) {
        payeeDao.updatePayee(payee)
    }

    suspend fun deletePayee(payee: PayeeEntity): Unit = withContext(ioDispatcher) {
        payeeDao.deletePayee(payee)
    }

    suspend fun insertPayeeAccount(account: PayeeAccountEntity): Long = withContext(ioDispatcher) {
        payeeDao.insertPayeeAccount(account)
    }

    suspend fun updatePayeeAccount(account: PayeeAccountEntity): Unit = withContext(ioDispatcher) {
        payeeDao.updatePayeeAccount(account)
    }

    suspend fun deletePayeeAccount(account: PayeeAccountEntity): Unit = withContext(ioDispatcher) {
        payeeDao.deletePayeeAccount(account)
    }
}
