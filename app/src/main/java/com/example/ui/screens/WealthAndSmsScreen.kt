package com.example.ui.screens

import android.Manifest
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AppCurrency
import com.example.data.model.InvestmentEntity
import com.example.data.model.InvestmentType
import com.example.scheduler.SmsScanScheduler
import com.example.sms.ParsedSmsResult
import com.example.sms.SampleSmsData
import com.example.sms.SmsParser
import com.example.sms.SmsReader
import com.example.ui.components.DonutPieChart
import com.example.ui.components.HeroNetWorthCard
import com.example.ui.components.InvestmentEditDialog
import com.example.viewmodel.CategorySpending
import com.example.viewmodel.ExpenseViewModel

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WealthAndSmsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Investments & Wealth", "SMS Auto-Scheduler")

    val netWorth by viewModel.netWorthSummary.collectAsStateWithLifecycle()
    val investments by viewModel.allInvestments.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val syncLogs by viewModel.syncLogs.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by viewModel.syncMessage.collectAsStateWithLifecycle()

    var selectedInvestmentForEdit by remember { mutableStateOf<InvestmentEntity?>(null) }
    var isAddInvestmentOpen by remember { mutableStateOf(false) }

    // SMS Simulator state
    var simulatorInputText by remember { mutableStateOf(SampleSmsData.SAMPLES.first().message) }
    var parsedTestResult by remember { mutableStateOf<ParsedSmsResult?>(null) }

    var hasSmsPerm by remember { mutableStateOf(SmsReader.hasSmsPermission(context)) }
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { perms ->
        hasSmsPerm = perms[Manifest.permission.READ_SMS] == true || SmsReader.hasSmsPermission(context)
        if (hasSmsPerm) {
            Toast.makeText(context, "SMS Read Permission Granted!", Toast.LENGTH_SHORT).show()
        }
    }

    val (nextNight, nextMorning) = remember { SmsScanScheduler.getNextScheduledInfo() }

    Scaffold(
        floatingActionButton = {
            if (selectedTabIndex == 0) {
                FloatingActionButton(
                    onClick = { isAddInvestmentOpen = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White,
                    modifier = Modifier.testTag("add_investment_fab")
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add Investment")
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Tab Selector
            TabRow(
                selectedTabIndex = selectedTabIndex,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTabIndex == index,
                        onClick = { selectedTabIndex = index },
                        text = {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                                )
                            )
                        }
                    )
                }
            }

            if (selectedTabIndex == 0) {
                // ==================== TAB 1: WEALTH & INVESTMENTS ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HeroNetWorthCard(
                            netWorth = netWorth,
                            currency = selectedCurrency
                        )
                    }

                    // Asset Allocation Donut Chart
                    item {
                        val assetAllocation = investments.groupBy { it.assetType }.map { (type: String, list: List<InvestmentEntity>) ->
                            val sum = list.sumOf { it.currentValue }
                            val typeDisplay = InvestmentType.values().firstOrNull { it.name == type }?.displayName ?: type
                            val totalVal = investments.sumOf { it.currentValue }
                            val pct = if (totalVal > 0) (sum / totalVal) * 100.0 else 0.0
                            CategorySpending(
                                category = typeDisplay,
                                amount = sum,
                                percentage = pct,
                                count = list.size
                            )
                        }


                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Asset Class Allocation",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                DonutPieChart(
                                    categorySpending = assetAllocation,
                                    centerTitle = "Invested Assets",
                                    currency = selectedCurrency
                                )
                            }
                        }
                    }

                    // Portfolio Holdings List Header
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Portfolio Holdings (${investments.size})",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "Total: ${selectedCurrency.format(netWorth.totalInvestmentsValue)}",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    if (investments.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Box(modifier = Modifier.padding(24.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = "No investments tracked yet. Tap + to add stocks, mutual funds, crypto, or real estate.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    } else {
                        items(investments, key = { it.id }) { item ->
                            InvestmentCardItem(
                                investment = item,
                                currency = selectedCurrency,
                                onClick = { selectedInvestmentForEdit = item }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            } else {
                // ==================== TAB 2: SMS AUTO-SCHEDULER & SETTINGS ====================
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // 1. Scheduler Status Card (11:59 PM & 6:00 AM Fallback)
                    item {
                        Card(
                            shape = RoundedCornerShape(22.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(2.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(18.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f), CircleShape),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Schedule,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(24.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column {
                                        Text(
                                            text = "Automated Message Box Scanner",
                                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "Daily Automated SMS Parsing",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "🌙 Primary Nightly Schedule:",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "11:59 PM",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        Text(
                                            text = "Next: $nextNight",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )

                                        Spacer(modifier = Modifier.height(8.dp))

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = "☀️ Morning Fallback Schedule:",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold)
                                            )
                                            Text(
                                                text = "6:00 AM",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                                                color = Color(0xFFF59E0B)
                                            )
                                        }
                                        Text(
                                            text = "Runs if phone was turned off or missed at 11:59 PM (Next: $nextMorning)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))

                                // Permission Check / Request
                                if (!hasSmsPerm) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "SMS Permission required to read messages",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color(0xFFEF4444),
                                            modifier = Modifier.weight(1f)
                                        )
                                        Button(
                                            onClick = {
                                                permissionLauncher.launch(
                                                    arrayOf(
                                                        Manifest.permission.READ_SMS,
                                                        Manifest.permission.RECEIVE_SMS
                                                    )
                                                )
                                            },
                                            modifier = Modifier.testTag("request_sms_permission_button")
                                        ) {
                                            Text("Grant Access")
                                        }
                                    }
                                } else {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = Color(0xFF10B981),
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "SMS Access Active",
                                                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                                                color = Color(0xFF10B981)
                                            )
                                        }

                                        Button(
                                            onClick = { viewModel.triggerManualSmsScan() },
                                            enabled = !isSyncing,
                                            modifier = Modifier.testTag("manual_sync_sms_button")
                                        ) {
                                            if (isSyncing) {
                                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                            }
                                            Text(if (isSyncing) "Scanning..." else "Sync SMS Now")
                                        }
                                    }
                                }

                                if (syncMessage != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = syncMessage!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    // 2. Interactive Bank SMS Parser Simulator
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(1.dp),
                            modifier = Modifier.fillMaxWidth().testTag("sms_simulator_card")
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Bank SMS Parser Simulator",
                                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Select a bank sample or paste SMS text to test the regex extraction engine:",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                // Sample Chips
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SampleSmsData.SAMPLES.take(6).forEach { sample ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                                            modifier = Modifier.clickable {
                                                simulatorInputText = sample.message
                                                parsedTestResult = SmsParser.parse(sample.message, System.currentTimeMillis(), sample.sender)
                                            }
                                        ) {
                                            Text(
                                                text = sample.bankName,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.height(10.dp))

                                OutlinedTextField(
                                    value = simulatorInputText,
                                    onValueChange = {
                                        simulatorInputText = it
                                        parsedTestResult = null
                                    },
                                    label = { Text("SMS Message Text") },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .testTag("sms_simulator_input"),
                                    maxLines = 3
                                )

                                Spacer(modifier = Modifier.height(10.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedButton(
                                        onClick = {
                                            parsedTestResult = SmsParser.parse(simulatorInputText, System.currentTimeMillis(), "SIMULATOR")
                                        },
                                        modifier = Modifier.weight(1f).testTag("test_parse_sms_button")
                                    ) {
                                        Text("Parse SMS")
                                    }

                                    Button(
                                        onClick = {
                                            val res = viewModel.parseAndSimulateSms(simulatorInputText)
                                            if (res != null) {
                                                Toast.makeText(context, "Added '${res.title}' ($${res.amount}) to transactions!", Toast.LENGTH_SHORT).show()
                                            } else {
                                                Toast.makeText(context, "Could not extract financial transaction from this SMS.", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f).testTag("import_simulated_sms_button")
                                    ) {
                                        Text("Import as Txn")
                                    }
                                }

                                // Parsed Result Output
                                if (parsedTestResult != null) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Surface(
                                        shape = RoundedCornerShape(12.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = "Parsed Result:",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(text = "• Title: ${parsedTestResult!!.title}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "• Amount: $${String.format(Locale.getDefault(), "%.2f", parsedTestResult!!.amount)}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "• Type: ${parsedTestResult!!.type}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "• Category: ${parsedTestResult!!.category}", style = MaterialTheme.typography.bodySmall)
                                            Text(text = "• Method: ${parsedTestResult!!.paymentMethod}", style = MaterialTheme.typography.bodySmall)
                                            if (parsedTestResult!!.accountNumberLast4 != null) {
                                                Text(text = "• Account: **${parsedTestResult!!.accountNumberLast4}", style = MaterialTheme.typography.bodySmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // 3. SMS Sync History Logs
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Sync History Logs",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (syncLogs.isNotEmpty()) {
                                TextButton(onClick = { viewModel.clearSyncHistory() }) {
                                    Text("Clear Logs")
                                }
                            }
                        }
                    }

                    if (syncLogs.isEmpty()) {
                        item {
                            Text(
                                text = "No sync events recorded yet.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    } else {
                        items(syncLogs, key = { it.id }) { log ->
                            Card(
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = log.status,
                                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                            color = if (log.status == "SUCCESS") Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                                        )
                                        Text(
                                            text = SimpleDateFormat("MMM dd, hh:mm a", Locale.getDefault()).format(Date(log.timestamp)),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = log.summary,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Investment Dialog
    if (isAddInvestmentOpen) {
        InvestmentEditDialog(
            investment = null,
            currency = selectedCurrency,
            onDismiss = { isAddInvestmentOpen = false },
            onSave = { name, assetType, invested, currentVal, qty, notes ->
                viewModel.saveInvestment(
                    name = name,
                    assetType = assetType,
                    investedAmount = invested,
                    currentValue = currentVal,
                    quantity = qty,
                    notes = notes
                )
            }
        )
    }

    if (selectedInvestmentForEdit != null) {
        InvestmentEditDialog(
            investment = selectedInvestmentForEdit,
            currency = selectedCurrency,
            onDismiss = { selectedInvestmentForEdit = null },
            onSave = { name, assetType, invested, currentVal, qty, notes ->
                viewModel.saveInvestment(
                    id = selectedInvestmentForEdit!!.id,
                    name = name,
                    assetType = assetType,
                    investedAmount = invested,
                    currentValue = currentVal,
                    quantity = qty,
                    notes = notes
                )
            },
            onDelete = {
                viewModel.deleteInvestment(selectedInvestmentForEdit!!)
                selectedInvestmentForEdit = null
            }
        )
    }
}

@Composable
fun InvestmentCardItem(
    investment: InvestmentEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    currency: AppCurrency = AppCurrency.USD
) {
    val isProfit = investment.gainLoss >= 0
    val profitColor = if (isProfit) Color(0xFF10B981) else Color(0xFFEF4444)
    val assetDisplay = InvestmentType.values().firstOrNull { it.name == investment.assetType }?.displayName ?: investment.assetType

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = investment.name,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = assetDisplay,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "Invested: ${currency.format(investment.investedAmount)} • Qty: ${investment.quantity}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = currency.format(investment.currentValue),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = profitColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "${if (isProfit) "+" else ""}${String.format(Locale.getDefault(), "%.1f%%", investment.returnPercentage)} (${currency.formatSigned(investment.gainLoss)})",
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                        color = profitColor,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}
