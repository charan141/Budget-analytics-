package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.TransactionEntity
import com.example.scheduler.SmsScanScheduler
import com.example.ui.components.CurrencySelectionDialog
import com.example.ui.components.DailySpendingBarChart
import com.example.ui.components.DonutPieChart
import com.example.ui.components.HeroNetWorthCard
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.MonthlyCashflowCard
import com.example.ui.components.TransactionEditDialog
import com.example.ui.components.TransactionListItem
import com.example.viewmodel.ExpenseViewModel
import java.util.Locale

@Composable
fun DashboardScreen(
    viewModel: ExpenseViewModel,
    onNavigateToTransactions: () -> Unit,
    onNavigateToBudgets: () -> Unit,
    onNavigateToWealth: () -> Unit,
    onNavigateToSms: () -> Unit,
    modifier: Modifier = Modifier
) {
    val netWorth by viewModel.netWorthSummary.collectAsStateWithLifecycle()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val currentMonthTxs by viewModel.currentMonthTransactions.collectAsStateWithLifecycle()
    val categoryBreakdown by viewModel.categorySpendingBreakdown.collectAsStateWithLifecycle()
    val dailySpending by viewModel.dailySpendingTrend.collectAsStateWithLifecycle()
    val budgetProgressList by viewModel.budgetProgressList.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var selectedTransactionForEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var isCurrencyDialogOpen by remember { mutableStateOf(false) }

    // Check if any budget is near or over limit
    val warningBudgets = budgetProgressList.filter { it.percentage >= 80.0 }
    val (nightSchedule, morningSchedule) = remember { SmsScanScheduler.getNextScheduledInfo() }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_transaction_fab")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Controls Bar (Currency Selector & Offline Security Status)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Currency Selection Pill Button
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                        modifier = Modifier
                            .clickable { isCurrencyDialogOpen = true }
                            .testTag("currency_selector_button")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = selectedCurrency.flag,
                                fontSize = 16.sp
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${selectedCurrency.code} (${selectedCurrency.symbol})",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // 100% Offline Badge
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = "Offline Storage",
                                tint = Color(0xFF059669),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "100% Offline • On-Device",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold, fontSize = 10.sp),
                                color = Color(0xFF059669)
                            )
                        }
                    }
                }
            }

            item {
                // Month Header
                MonthSelectorHeader(
                    selectedMonthYear = selectedMonthYear,
                    onPrevious = { viewModel.previousMonth() },
                    onNext = { viewModel.nextMonth() }
                )
            }

            // 1. Hero Net Worth Card
            item {
                HeroNetWorthCard(
                    netWorth = netWorth,
                    currency = selectedCurrency,
                    onViewInvestments = onNavigateToWealth
                )
            }

            // 2. Monthly Cashflow Overview (Income, Expense, Invested)
            item {
                MonthlyCashflowCard(
                    income = netWorth.monthlyIncome,
                    expense = netWorth.monthlyExpenses,
                    investments = netWorth.monthlyInvestments,
                    currency = selectedCurrency
                )
            }

            // 3. Automated SMS Scheduler Status Pill
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateToSms() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Schedule,
                                    contentDescription = "Scheduler",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Automated SMS Scanner",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Scheduled 11:59 PM • 6:00 AM Fallback",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isSyncing) {
                            CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
                        } else {
                            IconButton(
                                onClick = { viewModel.triggerManualSmsScan() },
                                modifier = Modifier.testTag("quick_sms_sync_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Sync SMS",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            // 4. Budget Alert Banner (if any budget >= 80%)
            if (warningBudgets.isNotEmpty()) {
                item {
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFFEF3C7)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigateToBudgets() }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Warning",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${warningBudgets.size} Budget Alert${if (warningBudgets.size > 1) "s" else ""}",
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF92400E)
                                )
                                val topAlert = warningBudgets.first()
                                Text(
                                    text = "${topAlert.budget.category}: ${String.format(Locale.getDefault(), "%.0f%%", topAlert.percentage)} used (${selectedCurrency.format(topAlert.spentAmount, false)} / ${selectedCurrency.format(topAlert.budget.monthlyLimit, false)})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFB45309)
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.ArrowForward,
                                contentDescription = "View",
                                tint = Color(0xFFD97706),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // 5. Daily Spending Bar Chart
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DailySpendingBarChart(
                            dailySpending = dailySpending,
                            currency = selectedCurrency
                        )
                    }
                }
            }

            // 6. Category Breakdown Donut Chart
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Monthly Spending Classification",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        DonutPieChart(
                            categorySpending = categoryBreakdown,
                            currency = selectedCurrency
                        )
                    }
                }
            }

            // 7. Recent Transactions Header & List
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent Transactions",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    TextButton(onClick = onNavigateToTransactions) {
                        Text("View All")
                    }
                }
            }

            val recentTxs = currentMonthTxs.take(5)
            if (recentTxs.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No transactions recorded yet. Tap + to add or Sync SMS.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                items(recentTxs, key = { it.id }) { tx ->
                    TransactionListItem(
                        transaction = tx,
                        currency = selectedCurrency,
                        onClick = { selectedTransactionForEdit = tx },
                        onDelete = { viewModel.deleteTransaction(tx) }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp)) // Padding for FAB & Nav
            }
        }
    }

    // Currency Selector Dialog
    if (isCurrencyDialogOpen) {
        CurrencySelectionDialog(
            selectedCurrency = selectedCurrency,
            onCurrencySelected = { viewModel.setCurrency(it) },
            onDismiss = { isCurrencyDialogOpen = false }
        )
    }

    // Add / Edit Transaction Dialog
    if (isAddDialogOpen) {
        TransactionEditDialog(
            transaction = null,
            currency = selectedCurrency,
            onDismiss = { isAddDialogOpen = false },
            onSave = { title, amount, type, category, paymentMethod, notes, timestamp ->
                viewModel.addTransaction(title, amount, type, category, paymentMethod, notes, timestamp)
            }
        )
    }

    if (selectedTransactionForEdit != null) {
        TransactionEditDialog(
            transaction = selectedTransactionForEdit,
            currency = selectedCurrency,
            onDismiss = { selectedTransactionForEdit = null },
            onSave = { title, amount, type, category, paymentMethod, notes, timestamp ->
                viewModel.updateTransaction(
                    selectedTransactionForEdit!!.copy(
                        title = title,
                        amount = amount,
                        type = type,
                        category = category,
                        paymentMethod = paymentMethod,
                        notes = notes,
                        timestamp = timestamp
                    )
                )
            },
            onDelete = {
                viewModel.deleteTransaction(selectedTransactionForEdit!!)
                selectedTransactionForEdit = null
            }
        )
    }
}
