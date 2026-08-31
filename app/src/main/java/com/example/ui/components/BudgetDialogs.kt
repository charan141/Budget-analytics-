package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AppCurrency
import com.example.data.model.BudgetEntity
import com.example.data.model.DefaultCategories
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetEditDialog(
    budget: BudgetEntity?,
    onDismiss: () -> Unit,
    onSave: (category: String, monthlyLimit: Double, alertThreshold: Int) -> Unit,
    currency: AppCurrency = AppCurrency.USD,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = budget != null

    var selectedCategory by remember { mutableStateOf(budget?.category ?: "TOTAL") }
    var limitText by remember { mutableStateOf(budget?.let { String.format(Locale.getDefault(), "%.2f", it.monthlyLimit) } ?: "") }
    var alertThreshold by remember { mutableFloatStateOf(budget?.alertThresholdPercent?.toFloat() ?: 80f) }

    var categoryDropdownExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val categoryOptions = listOf("TOTAL (Overall Budget)") + DefaultCategories.EXPENSE_CATEGORIES.map { it.name }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Budget" else "Set Monthly Budget",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Category Selection Dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryDropdownExpanded,
                    onExpandedChange = { if (!isEditing) categoryDropdownExpanded = !categoryDropdownExpanded }
                ) {
                    OutlinedTextField(
                        value = if (selectedCategory == "TOTAL") "TOTAL (Overall Monthly Budget)" else selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isEditing, // lock category when editing
                        label = { Text("Budget Category") },
                        trailingIcon = { if (!isEditing) ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = categoryDropdownExpanded,
                        onDismissRequest = { categoryDropdownExpanded = false }
                    ) {
                        categoryOptions.forEach { opt ->
                            val catKey = if (opt.startsWith("TOTAL")) "TOTAL" else opt
                            DropdownMenuItem(
                                text = { Text(opt) },
                                onClick = {
                                    selectedCategory = catKey
                                    categoryDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Budget Limit Field
                OutlinedTextField(
                    value = limitText,
                    onValueChange = {
                        limitText = it
                        showError = false
                    },
                    label = { Text("Monthly Spending Limit (${currency.symbol})") },
                    placeholder = { Text("e.g., 500.00") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("budget_limit_input"),
                    singleLine = true,
                    isError = showError && limitText.toDoubleOrNull() == null
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Push Notification Warning Threshold
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.NotificationsActive,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Push Alert Threshold",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        text = "${alertThreshold.toInt()}%",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Text(
                    text = "You will receive a notification when spending reaches this percentage of your monthly limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
                )

                Slider(
                    value = alertThreshold,
                    onValueChange = { alertThreshold = it },
                    valueRange = 50f..100f,
                    steps = 9 // 50, 55, 60, 65, 70, 75, 80, 85, 90, 95, 100
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter a valid monthly limit amount.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (isEditing && onDelete != null) {
                        OutlinedButton(
                            onClick = onDelete,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            )
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
                            val parsed = limitText.toDoubleOrNull()
                            if (parsed == null || parsed <= 0.0) {
                                showError = true
                            } else {
                                onSave(selectedCategory, parsed, alertThreshold.toInt())
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_budget_button")
                    ) {
                        Text("Save Budget")
                    }
                }
            }
        }
    }
}
