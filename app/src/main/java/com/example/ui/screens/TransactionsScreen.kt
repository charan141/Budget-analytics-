package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.DefaultCategories
import com.example.data.model.TransactionEntity
import com.example.ui.components.CurrencySelectionDialog
import com.example.ui.components.MonthSelectorHeader
import com.example.ui.components.TransactionEditDialog
import com.example.ui.components.TransactionListItem
import com.example.viewmodel.ExpenseViewModel
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionsScreen(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsStateWithLifecycle()
    val selectedCurrency by viewModel.selectedCurrency.collectAsStateWithLifecycle()
    val transactions by viewModel.filteredTransactions.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategoryFilter.collectAsStateWithLifecycle()
    val selectedType by viewModel.selectedTypeFilter.collectAsStateWithLifecycle()

    var selectedTransactionForEdit by remember { mutableStateOf<TransactionEntity?>(null) }
    var isAddDialogOpen by remember { mutableStateOf(false) }
    var isCurrencyDialogOpen by remember { mutableStateOf(false) }

    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    val allCategoriesList = listOf("ALL") + DefaultCategories.getAllCategoryNames()

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { isAddDialogOpen = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White,
                modifier = Modifier.testTag("add_transaction_fab_tx_screen")
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Transaction")
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Month Header
            MonthSelectorHeader(
                selectedMonthYear = selectedMonthYear,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() }
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Search Field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.searchQuery.value = it },
                placeholder = { Text("Search description, merchant, card...") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                },
                trailingIcon = if (searchQuery.isNotEmpty()) {
                    {
                        IconButton(onClick = { viewModel.searchQuery.value = "" }) {
                            Icon(imageVector = Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                } else null,
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("transaction_search_bar")
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Type Filter Chips (All / Expense / Income / Investment)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf("ALL" to "All Types", "EXPENSE" to "Expenses", "INCOME" to "Incomes", "INVESTMENT" to "Investments").forEach { (typeKey, label) ->
                    val isSelected = selectedType == typeKey
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectedTypeFilter.value = typeKey },
                        label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                        modifier = Modifier.testTag("filter_type_$typeKey")
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Category Filter Dropdown Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${transactions.size} Transactions",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.menuAnchor()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.FilterList, contentDescription = "Category Filter", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (selectedCategory == "ALL") "All Categories" else selectedCategory,
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium)
                            )
                        }
                    }

                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        allCategoriesList.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(if (cat == "ALL") "All Categories" else cat) },
                                onClick = {
                                    viewModel.selectedCategoryFilter.value = cat
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Transactions List
            if (transactions.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "No transactions found",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Try adjusting filters or tap + to log a transaction.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(transactions, key = { it.id }) { tx ->
                        TransactionListItem(
                            transaction = tx,
                            currency = selectedCurrency,
                            onClick = { selectedTransactionForEdit = tx },
                            onDelete = {
                                val deletedItem = tx
                                viewModel.deleteTransaction(tx)
                                coroutineScope.launch {
                                    val result = snackbarHostState.showSnackbar(
                                        message = "Deleted ${deletedItem.title}",
                                        actionLabel = "Undo",
                                        duration = SnackbarDuration.Short
                                    )
                                    if (result == SnackbarResult.ActionPerformed) {
                                        viewModel.addTransaction(
                                            title = deletedItem.title,
                                            amount = deletedItem.amount,
                                            type = deletedItem.type,
                                            category = deletedItem.category,
                                            paymentMethod = deletedItem.paymentMethod,
                                            notes = deletedItem.notes,
                                            timestamp = deletedItem.timestamp
                                        )
                                    }
                                }
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Add / Edit Dialogs
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
                val deletedItem = selectedTransactionForEdit!!
                viewModel.deleteTransaction(deletedItem)
                selectedTransactionForEdit = null
                coroutineScope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${deletedItem.title}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.addTransaction(
                            title = deletedItem.title,
                            amount = deletedItem.amount,
                            type = deletedItem.type,
                            category = deletedItem.category,
                            paymentMethod = deletedItem.paymentMethod,
                            notes = deletedItem.notes,
                            timestamp = deletedItem.timestamp
                        )
                    }
                }
            }
        )
    }
}
