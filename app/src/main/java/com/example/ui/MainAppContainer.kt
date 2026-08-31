package com.example.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PieChart
import androidx.compose.material.icons.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.ui.screens.AnalyticsScreen
import com.example.ui.screens.BudgetsScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.TransactionsScreen
import com.example.ui.screens.WealthAndSmsScreen
import com.example.viewmodel.ExpenseViewModel

sealed class NavTab(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Dashboard : NavTab("dashboard", "Home", Icons.Filled.Home, Icons.Outlined.Home)
    object Transactions : NavTab("transactions", "Transactions", Icons.Filled.ReceiptLong, Icons.Outlined.ReceiptLong)
    object Analytics : NavTab("analytics", "Analytics", Icons.Filled.PieChart, Icons.Outlined.PieChart)
    object Budgets : NavTab("budgets", "Budgets", Icons.Filled.Savings, Icons.Outlined.Savings)
    object Wealth : NavTab("wealth", "Wealth & SMS", Icons.Filled.AccountBalanceWallet, Icons.Outlined.AccountBalanceWallet)
}

@Composable
fun MainAppContainer(
    viewModel: ExpenseViewModel,
    modifier: Modifier = Modifier
) {
    val navItems = remember {
        listOf(
            NavTab.Dashboard,
            NavTab.Transactions,
            NavTab.Analytics,
            NavTab.Budgets,
            NavTab.Wealth
        )
    }

    var selectedTabIndex by remember { mutableIntStateOf(0) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 6.dp
            ) {
                navItems.forEachIndexed { index, item ->
                    val isSelected = selectedTabIndex == index
                    NavigationBarItem(
                        selected = isSelected,
                        onClick = { selectedTabIndex = index },
                        icon = {
                            Icon(
                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                contentDescription = item.title,
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        label = {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        ),
                        modifier = Modifier.testTag("nav_${item.route}")
                    )
                }
            }
        },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        when (selectedTabIndex) {
            0 -> DashboardScreen(
                viewModel = viewModel,
                onNavigateToTransactions = { selectedTabIndex = 1 },
                onNavigateToBudgets = { selectedTabIndex = 3 },
                onNavigateToWealth = { selectedTabIndex = 4 },
                onNavigateToSms = { selectedTabIndex = 4 },
                modifier = Modifier.padding(innerPadding)
            )
            1 -> TransactionsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            2 -> AnalyticsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            3 -> BudgetsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
            4 -> WealthAndSmsScreen(
                viewModel = viewModel,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
