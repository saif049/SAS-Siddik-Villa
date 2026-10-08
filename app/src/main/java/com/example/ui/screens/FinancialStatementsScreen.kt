package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.FlatFinancialStatement
import com.example.data.model.User
import com.example.ui.components.BalancePill
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DeficitRed
import com.example.ui.theme.SurplusGreen
import com.example.ui.viewmodel.VillaViewModel

@Composable
fun FinancialStatementsScreen(
    currentUser: User,
    viewModel: VillaViewModel
) {
    val statements by viewModel.flatStatements.collectAsState()
    val expenses by viewModel.allExpenses.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var filterType by remember { mutableStateOf("ALL") } // ALL, SURPLUS, DEFICIT

    val totalCommon = expenses.filter { it.allocationType == "COMMON" }.sumOf { it.amount }
    val commonSharePerFlat = if (statements.isNotEmpty()) totalCommon / 14.0 else 0.0

    val filteredStatements = statements.filter { item ->
        val matchesSearch = item.flatId.contains(searchQuery, ignoreCase = true) ||
                item.ownerName.contains(searchQuery, ignoreCase = true)
        val matchesFilter = when (filterType) {
            "SURPLUS" -> item.netBalance >= 0
            "DEFICIT" -> item.netBalance < 0
            else -> true
        }
        matchesSearch && matchesFilter
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Common Expense Calculation Formula Header Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Common Building Expenses",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = Formatters.formatBdt(totalCommon),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "Per Flat (÷ 14)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Text(
                                text = Formatters.formatBdt(commonSharePerFlat),
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Accounting Rule: Net Balance = Own Eligible Deposits − (Total Common ÷ 14) − Individual Assigned Expenses",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search & Filter Row
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search flat (e.g. 1A, 5B) or owner name...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("search_statements_input"),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = filterType == "ALL",
                onClick = { filterType = "ALL" },
                label = { Text("All 14 Flats (${statements.size})") }
            )
            FilterChip(
                selected = filterType == "SURPLUS",
                onClick = { filterType = "SURPLUS" },
                label = { Text("Surplus (${statements.count { it.netBalance >= 0 }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = SurplusGreen.copy(alpha = 0.15f),
                    selectedLabelColor = SurplusGreen
                )
            )
            FilterChip(
                selected = filterType == "DEFICIT",
                onClick = { filterType = "DEFICIT" },
                label = { Text("Deficit (${statements.count { it.netBalance < 0 }})") },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = DeficitRed.copy(alpha = 0.15f),
                    selectedLabelColor = DeficitRed
                )
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Statements List
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(filteredStatements, key = { it.flatId }) { item ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("flat_statement_${item.flatId}"),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (item.flatId == currentUser.flatId)
                            MaterialTheme.colorScheme.surfaceVariant
                        else MaterialTheme.colorScheme.surface
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Text(
                                        text = "Flat ${item.flatId}",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = item.ownerName,
                                        fontWeight = FontWeight.SemiBold,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    if (item.flatId == currentUser.flatId) {
                                        Text(
                                            text = "(Your Account)",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }
                            BalancePill(balance = item.netBalance)
                        }

                        Divider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                        )

                        // Breakdown Details Grid
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Own Deposits", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(Formatters.formatBdt(item.totalDeposits), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = SurplusGreen)
                            }
                            Column {
                                Text("Common Share", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(Formatters.formatBdt(item.shareOfCommonExpense), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                            }
                            Column {
                                Text("Assigned Exp.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                Text(Formatters.formatBdt(item.individualAssignedExpense), fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = if (item.individualAssignedExpense > 0) DeficitRed else MaterialTheme.colorScheme.onSurface)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Utility status chips
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.serviceChargeDue == 0.0) SurplusGreen.copy(alpha = 0.12f) else DeficitRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (item.serviceChargeDue == 0.0) "Service: Cleared" else "Service: Due ৳${item.serviceChargeDue.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.serviceChargeDue == 0.0) SurplusGreen else DeficitRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = if (item.gasBillDue == 0.0) SurplusGreen.copy(alpha = 0.12f) else DeficitRed.copy(alpha = 0.12f)
                                ) {
                                    Text(
                                        text = if (item.gasBillDue == 0.0) "Gas: Paid" else "Gas: Due ৳${item.gasBillDue.toInt()}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (item.gasBillDue == 0.0) SurplusGreen else DeficitRed,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Text(
                                text = if (item.netBalance >= 0) "Surplus in Fund" else "Deficit to Settle",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.netBalance >= 0) SurplusGreen else DeficitRed,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}
