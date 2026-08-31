package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppCurrency
import com.example.data.model.DefaultCategories
import com.example.data.model.TransactionEntity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TransactionEditDialog(
    transaction: TransactionEntity?, // null if adding new
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, type: String, category: String, paymentMethod: String, notes: String, timestamp: Long) -> Unit,
    currency: AppCurrency = AppCurrency.USD,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = transaction != null

    var title by remember { mutableStateOf(transaction?.title ?: "") }
    var amountText by remember { mutableStateOf(transaction?.let { String.format(Locale.getDefault(), "%.2f", it.amount) } ?: "") }
    var selectedType by remember { mutableStateOf(transaction?.type ?: "EXPENSE") }
    var selectedCategory by remember {
        mutableStateOf(
            transaction?.category ?: when (selectedType) {
                "INCOME" -> "Salary"
                "INVESTMENT" -> "Stocks / Equities"
                else -> "Food & Dining"
            }
        )
    }
    var paymentMethod by remember { mutableStateOf(transaction?.paymentMethod ?: "UPI") }
    var notes by remember { mutableStateOf(transaction?.notes ?: "") }
    var timestamp by remember { mutableStateOf(transaction?.timestamp ?: System.currentTimeMillis()) }

    var paymentDropdownExpanded by remember { mutableStateOf(false) }
    val paymentOptions = listOf("UPI", "Credit Card", "Debit Card", "Bank Transfer", "Cash", "Wallet", "Crypto")

    val categoriesForType = when (selectedType) {
        "INCOME" -> DefaultCategories.INCOME_CATEGORIES
        "INVESTMENT" -> DefaultCategories.INVESTMENT_CATEGORIES
        else -> DefaultCategories.EXPENSE_CATEGORIES
    }

    var showError by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Transaction" else "Add Transaction",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Type Toggle (Expense / Income / Investment)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        Triple("EXPENSE", "Expense", Color(0xFFEF4444)),
                        Triple("INCOME", "Income", Color(0xFF10B981)),
                        Triple("INVESTMENT", "Investment", Color(0xFF8B5CF6))
                    ).forEach { (typeKey, label, color) ->
                        val isSelected = selectedType == typeKey
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedType = typeKey
                                    // Reset category to first available in new type
                                    val newCats = when (typeKey) {
                                        "INCOME" -> DefaultCategories.INCOME_CATEGORIES
                                        "INVESTMENT" -> DefaultCategories.INVESTMENT_CATEGORIES
                                        else -> DefaultCategories.EXPENSE_CATEGORIES
                                    }
                                    selectedCategory = newCats.first().name
                                }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium),
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        showError = false
                    },
                    label = { Text("Amount (${currency.symbol})") },
                    placeholder = { Text("0.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_amount_input"),
                    singleLine = true,
                    isError = showError && amountText.toDoubleOrNull() == null
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        showError = false
                    },
                    label = { Text("Description / Merchant") },
                    placeholder = { Text("e.g., Starbucks, Whole Foods, Salary") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("transaction_title_input"),
                    singleLine = true,
                    isError = showError && title.isBlank()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Category Chips Selector
                Text(
                    text = "Category Label",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    categoriesForType.forEach { cat ->
                        val isSelected = selectedCategory.equals(cat.name, ignoreCase = true)
                        val catColor = Color(cat.colorHex)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat.name },
                            label = {
                                Text(
                                    text = cat.name,
                                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                                )
                            },
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Done,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null,
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = catColor.copy(alpha = 0.2f),
                                selectedLabelColor = catColor
                            )
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment Method Dropdown
                ExposedDropdownMenuBox(
                    expanded = paymentDropdownExpanded,
                    onExpandedChange = { paymentDropdownExpanded = !paymentDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Method") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = paymentDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = paymentDropdownExpanded,
                        onDismissRequest = { paymentDropdownExpanded = false }
                    ) {
                        paymentOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    paymentMethod = option
                                    paymentDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("Add any remarks or tags...") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter a valid amount and description.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isEditing && onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            modifier = Modifier.testTag("delete_transaction_button")
                        ) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                        }
                    }

                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }

                    Button(
                        onClick = {
                            val parsedAmount = amountText.toDoubleOrNull()
                            if (parsedAmount == null || parsedAmount <= 0.0 || title.isBlank()) {
                                showError = true
                            } else {
                                onSave(
                                    title.trim(),
                                    parsedAmount,
                                    selectedType,
                                    selectedCategory,
                                    paymentMethod,
                                    notes.trim(),
                                    timestamp
                                )
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_transaction_button")
                    ) {
                        Text(if (isEditing) "Save Changes" else "Add")
                    }
                }
            }
        }
    }
}
