package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.data.model.InvestmentEntity
import com.example.data.model.InvestmentType
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InvestmentEditDialog(
    investment: InvestmentEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, assetType: String, invested: Double, currentVal: Double, quantity: Double, notes: String) -> Unit,
    currency: AppCurrency = AppCurrency.USD,
    onDelete: (() -> Unit)? = null
) {
    val isEditing = investment != null

    var name by remember { mutableStateOf(investment?.name ?: "") }
    var assetType by remember { mutableStateOf(investment?.assetType ?: "STOCKS") }
    var investedText by remember { mutableStateOf(investment?.let { String.format(Locale.getDefault(), "%.2f", it.investedAmount) } ?: "") }
    var currentValText by remember { mutableStateOf(investment?.let { String.format(Locale.getDefault(), "%.2f", it.currentValue) } ?: "") }
    var quantityText by remember { mutableStateOf(investment?.let { String.format(Locale.getDefault(), "%.2f", it.quantity) } ?: "1.0") }
    var notes by remember { mutableStateOf(investment?.notes ?: "") }

    var assetDropdownExpanded by remember { mutableStateOf(false) }
    var showError by remember { mutableStateOf(false) }

    val assetOptions = InvestmentType.values().map { it.name to it.displayName }

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
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEditing) "Edit Investment" else "Add Investment Asset",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Asset Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        showError = false
                    },
                    label = { Text("Asset / Holding Name") },
                    placeholder = { Text("e.g. Apple (AAPL), S&P 500 Index, Bitcoin") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("investment_name_input"),
                    singleLine = true,
                    isError = showError && name.isBlank()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Asset Class Dropdown
                ExposedDropdownMenuBox(
                    expanded = assetDropdownExpanded,
                    onExpandedChange = { assetDropdownExpanded = !assetDropdownExpanded }
                ) {
                    val currentDisplay = assetOptions.firstOrNull { it.first == assetType }?.second ?: assetType
                    OutlinedTextField(
                        value = currentDisplay,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Asset Class") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = assetDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = assetDropdownExpanded,
                        onDismissRequest = { assetDropdownExpanded = false }
                    ) {
                        assetOptions.forEach { (typeKey, displayName) ->
                            DropdownMenuItem(
                                text = { Text(displayName) },
                                onClick = {
                                    assetType = typeKey
                                    assetDropdownExpanded = false
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Invested & Current Value row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = investedText,
                        onValueChange = {
                            investedText = it
                            showError = false
                        },
                        label = { Text("Invested (${currency.symbol})") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("invested_amount_input"),
                        singleLine = true
                    )

                    OutlinedTextField(
                        value = currentValText,
                        onValueChange = {
                            currentValText = it
                            showError = false
                        },
                        label = { Text("Current Value (${currency.symbol})") },
                        placeholder = { Text("0.00") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("current_value_input"),
                        singleLine = true
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Quantity
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = { quantityText = it },
                    label = { Text("Quantity / Units") },
                    placeholder = { Text("1.0") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Notes
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Brokerage account, purchase price") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2
                )

                if (showError) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Please enter holding name and valid numerical amounts.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
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
                            val invested = investedText.toDoubleOrNull()
                            val current = currentValText.toDoubleOrNull()
                            val qty = quantityText.toDoubleOrNull() ?: 1.0

                            if (name.isBlank() || invested == null || current == null) {
                                showError = true
                            } else {
                                onSave(name, assetType, invested, current, qty, notes)
                                onDismiss()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("save_investment_button")
                    ) {
                        Text("Save Asset")
                    }
                }
            }
        }
    }
}
