package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.Formatters
import com.example.ui.components.StatusBadge
import com.example.ui.theme.DeficitRed
import com.example.ui.theme.SurplusGreen
import com.example.ui.viewmodel.VillaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeExpenseScreen(
    currentUser: User,
    viewModel: VillaViewModel
) {
    var selectedTab by remember { mutableStateOf(0) } // 0: Expenses, 1: Incomes
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showAddIncomeDialog by remember { mutableStateOf(false) }

    val expenses by viewModel.allExpenses.collectAsState()
    val incomes by viewModel.allIncomes.collectAsState()

    val canEdit = currentUser.role == UserRole.SYSTEM_ADMIN ||
            currentUser.role == UserRole.GOVERNANCE_MEMBER ||
            currentUser.role == UserRole.DELEGATED_ADMIN

    Scaffold(
        floatingActionButton = {
            if (canEdit) {
                FloatingActionButton(
                    onClick = {
                        if (selectedTab == 0) showAddExpenseDialog = true else showAddIncomeDialog = true
                    },
                    modifier = Modifier.testTag("add_transaction_fab"),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = if (selectedTab == 0) "Add Expense" else "Add Income",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Tab Selector
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = DeficitRed, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Expenses (${expenses.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_expenses")
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SurplusGreen, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Incomes / Deposits (${incomes.size})")
                        }
                    },
                    modifier = Modifier.testTag("tab_incomes")
                )
            }

            if (selectedTab == 0) {
                // EXPENSES LIST
                val totalExpense = expenses.sumOf { it.amount }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Expenses: ${Formatters.formatBdt(totalExpense)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeficitRed
                        )
                        Text(
                            text = "Common ÷ 14 & Specific",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (expenses.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No expense records found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(expenses, key = { it.id }) { expense ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("expense_item_${expense.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = expense.subcategory.ifBlank { expense.category },
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${expense.date} • ${expense.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "- ${Formatters.formatBdt(expense.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = DeficitRed
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = when (expense.allocationType) {
                                                    "COMMON" -> MaterialTheme.colorScheme.primaryContainer
                                                    "BUILDING_RESERVE" -> MaterialTheme.colorScheme.secondaryContainer
                                                    else -> MaterialTheme.colorScheme.tertiaryContainer
                                                }
                                            ) {
                                                Text(
                                                    text = when (expense.allocationType) {
                                                        "COMMON" -> "Common (÷ 14)"
                                                        "INDIVIDUAL" -> "Flat ${expense.assignedFlatId ?: "Assigned"}"
                                                        else -> expense.allocationType
                                                    },
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            if (expense.vendorPayee.isNotBlank()) {
                                                Text(
                                                    text = "Payee: ${expense.vendorPayee}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.outline,
                                                    modifier = Modifier.align(Alignment.CenterVertically)
                                                )
                                            }
                                        }

                                        if (canEdit) {
                                            IconButton(
                                                onClick = { viewModel.deleteExpense(expense.id, currentUser.userId) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (expense.remarks.isNotBlank() || expense.receiptRef.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Ref: ${expense.receiptRef} ${if (expense.remarks.isNotBlank()) "• ${expense.remarks}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // INCOMES LIST
                val totalIncome = incomes.sumOf { it.amount }
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total Incomes/Deposits: ${Formatters.formatBdt(totalIncome)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SurplusGreen
                        )
                        Text(
                            text = "Member Deposits & Other",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                if (incomes.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No income records found", color = MaterialTheme.colorScheme.outline)
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(incomes, key = { it.id }) { income ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("income_item_${income.id}"),
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = income.payerName,
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = "${income.date} • ${income.category}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Text(
                                            text = "+ ${Formatters.formatBdt(income.amount)}",
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = SurplusGreen
                                        )
                                    }

                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = if (income.incomeType == "MEMBER_DEPOSIT")
                                                    SurplusGreen.copy(alpha = 0.15f)
                                                else MaterialTheme.colorScheme.secondaryContainer
                                            ) {
                                                Text(
                                                    text = if (income.flatId != null) "Flat ${income.flatId}" else "Building",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = if (income.incomeType == "MEMBER_DEPOSIT") SurplusGreen else MaterialTheme.colorScheme.onSecondaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }

                                            Text(
                                                text = "Via ${income.paymentMethod}",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.outline,
                                                modifier = Modifier.align(Alignment.CenterVertically)
                                            )
                                        }

                                        if (canEdit) {
                                            IconButton(
                                                onClick = { viewModel.deleteIncome(income.id, currentUser.userId) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                    }

                                    if (income.referenceNo.isNotBlank() || income.remarks.isNotBlank()) {
                                        Spacer(Modifier.height(4.dp))
                                        Text(
                                            text = "Ref: ${income.referenceNo} ${if (income.remarks.isNotBlank()) "• ${income.remarks}" else ""}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.outline
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val customCategories by viewModel.customCategories.collectAsState()

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            currentUser = currentUser,
            customCategories = customCategories,
            onDismiss = { showAddExpenseDialog = false },
            onAdd = { amount, cat, subcat, expType, allocType, flatId, vendor, method, ref, rem ->
                viewModel.addExpense(amount, cat, subcat, expType, allocType, flatId, vendor, method, ref, rem, currentUser.userId)
                showAddExpenseDialog = false
            }
        )
    }

    if (showAddIncomeDialog) {
        AddIncomeDialog(
            currentUser = currentUser,
            customCategories = customCategories,
            onDismiss = { showAddIncomeDialog = false },
            onAdd = { amount, incType, flatId, payerName, cat, subcat, method, ref, rem ->
                viewModel.addIncome(amount, incType, null, payerName, flatId, cat, subcat, method, ref, rem, currentUser.userId)
                showAddIncomeDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseDialog(
    currentUser: User,
    customCategories: List<CustomCategory> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (Double, String, String, String, String, String?, String, String, String, String) -> Unit
) {
    val dbExpenseCats = customCategories.filter { it.type == "EXPENSE" }
    val categories = if (dbExpenseCats.isNotEmpty()) dbExpenseCats.map { it.categoryName } else MasterCategories.ExpenseCategories.keys.toList()

    var amountText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(categories.firstOrNull() ?: "Maintenance & Operations") }
    val subcategories = if (dbExpenseCats.isNotEmpty()) {
        dbExpenseCats.find { it.categoryName == selectedCategory }?.subcategoriesCsv?.split(",")?.map { it.trim() }?.filter { it.isNotBlank() } ?: listOf("General")
    } else {
        MasterCategories.ExpenseCategories[selectedCategory] ?: emptyList()
    }
    var selectedSubcategory by remember { mutableStateOf(subcategories.firstOrNull() ?: "General Repairs") }
    var allocationType by remember { mutableStateOf("COMMON") }
    var selectedFlatId by remember { mutableStateOf("1A") }
    var vendor by remember { mutableStateOf("") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }

    val flats = listOf("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B", "6A", "6B", "7A", "7B")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingDown, contentDescription = null, tint = DeficitRed)
                Spacer(Modifier.width(8.dp))
                Text("Record Building Expense", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (BDT ৳)") },
                    placeholder = { Text("e.g. 4500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Category selector
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat) },
                                onClick = {
                                    selectedCategory = cat
                                    selectedSubcategory = MasterCategories.ExpenseCategories[cat]?.firstOrNull() ?: ""
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Subcategory
                Text("Subcategory:", style = MaterialTheme.typography.labelMedium)
                var subcatExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = subcatExpanded,
                    onExpandedChange = { subcatExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedSubcategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subcatExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = subcatExpanded,
                        onDismissRequest = { subcatExpanded = false }
                    ) {
                        subcategories.forEach { sub ->
                            DropdownMenuItem(
                                text = { Text(sub) },
                                onClick = {
                                    selectedSubcategory = sub
                                    subcatExpanded = false
                                }
                            )
                        }
                    }
                }

                // Allocation Type (CRITICAL for financial formula!)
                Text("Expense Allocation:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = allocationType == "COMMON",
                        onClick = { allocationType = "COMMON" },
                        label = { Text("Common (÷ 14)") },
                        modifier = Modifier.testTag("alloc_common")
                    )
                    FilterChip(
                        selected = allocationType == "INDIVIDUAL",
                        onClick = { allocationType = "INDIVIDUAL" },
                        label = { Text("Individual Flat") },
                        modifier = Modifier.testTag("alloc_individual")
                    )
                    FilterChip(
                        selected = allocationType == "BUILDING_RESERVE",
                        onClick = { allocationType = "BUILDING_RESERVE" },
                        label = { Text("Reserve Fund") }
                    )
                }

                if (allocationType == "INDIVIDUAL") {
                    Text("Select Flat to Assign:", style = MaterialTheme.typography.labelMedium)
                    var flatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = flatExpanded,
                        onExpandedChange = { flatExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Flat $selectedFlatId",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = flatExpanded,
                            onDismissRequest = { flatExpanded = false }
                        ) {
                            flats.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text("Flat $f") },
                                    onClick = {
                                        selectedFlatId = f
                                        flatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = vendor,
                    onValueChange = { vendor = it },
                    label = { Text("Vendor / Payee") },
                    placeholder = { Text("e.g. Otis, Technician, Cleaner") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Receipt / Voucher Ref") },
                    placeholder = { Text("e.g. VOUCH-0129") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onAdd(
                            amount,
                            selectedCategory,
                            selectedSubcategory,
                            "Operating",
                            allocationType,
                            if (allocationType == "INDIVIDUAL") selectedFlatId else null,
                            vendor,
                            paymentMethod,
                            reference,
                            remarks
                        )
                    }
                },
                enabled = amountText.toDoubleOrNull() != null,
                modifier = Modifier.testTag("submit_expense_button")
            ) {
                Text("Save Expense")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIncomeDialog(
    currentUser: User,
    customCategories: List<CustomCategory> = emptyList(),
    onDismiss: () -> Unit,
    onAdd: (Double, String, String?, String, String, String, String, String, String) -> Unit
) {
    val dbIncomeCats = customCategories.filter { it.type == "INCOME" }
    val incomeCategories = if (dbIncomeCats.isNotEmpty()) dbIncomeCats.map { it.categoryName } else MasterCategories.IncomeCategories

    var amountText by remember { mutableStateOf("3500") }
    var incomeType by remember { mutableStateOf("MEMBER_DEPOSIT") }
    var selectedCategory by remember { mutableStateOf(incomeCategories.firstOrNull() ?: "Monthly Service Charge") }
    var selectedFlatId by remember { mutableStateOf("1A") }
    var payerName by remember { mutableStateOf("Flat 1A Owner") }
    var paymentMethod by remember { mutableStateOf("Cash") }
    var reference by remember { mutableStateOf("") }
    var remarks by remember { mutableStateOf("") }
    val flats = listOf("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B", "6A", "6B", "7A", "7B")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.TrendingUp, contentDescription = null, tint = SurplusGreen)
                Spacer(Modifier.width(8.dp))
                Text("Record Income / Deposit", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Amount (BDT ৳)") },
                    placeholder = { Text("e.g. 3500") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("income_amount_input")
                )

                Text("Deposit Type:", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = incomeType == "MEMBER_DEPOSIT",
                        onClick = { incomeType = "MEMBER_DEPOSIT" },
                        label = { Text("Member Deposit (Flat)") }
                    )
                    FilterChip(
                        selected = incomeType == "BUILDING_INCOME",
                        onClick = { incomeType = "BUILDING_INCOME" },
                        label = { Text("General Building Income") }
                    )
                }

                if (incomeType == "MEMBER_DEPOSIT") {
                    Text("Select Flat:", style = MaterialTheme.typography.labelMedium)
                    var flatExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = flatExpanded,
                        onExpandedChange = { flatExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = "Flat $selectedFlatId",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = flatExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = flatExpanded,
                            onDismissRequest = { flatExpanded = false }
                        ) {
                            flats.forEach { f ->
                                DropdownMenuItem(
                                    text = { Text("Flat $f") },
                                    onClick = {
                                        selectedFlatId = f
                                        payerName = "Flat $f Deposit"
                                        flatExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = payerName,
                    onValueChange = { payerName = it },
                    label = { Text("Payer Name / Depositor") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Category
                Text("Category:", style = MaterialTheme.typography.labelMedium)
                var catExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = catExpanded,
                    onExpandedChange = { catExpanded = it }
                ) {
                    OutlinedTextField(
                        value = selectedCategory,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = catExpanded,
                        onDismissRequest = { catExpanded = false }
                    ) {
                        incomeCategories.forEach { c ->
                            DropdownMenuItem(
                                text = { Text(c) },
                                onClick = {
                                    selectedCategory = c
                                    catExpanded = false
                                }
                            )
                        }
                    }
                }

                // Payment Method
                Text("Payment Method:", style = MaterialTheme.typography.labelMedium)
                var methodExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = methodExpanded,
                    onExpandedChange = { methodExpanded = it }
                ) {
                    OutlinedTextField(
                        value = paymentMethod,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = methodExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                    )
                    ExposedDropdownMenu(
                        expanded = methodExpanded,
                        onDismissRequest = { methodExpanded = false }
                    ) {
                        MasterCategories.PaymentMethods.forEach { m ->
                            DropdownMenuItem(
                                text = { Text(m) },
                                onClick = {
                                    paymentMethod = m
                                    methodExpanded = false
                                }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = reference,
                    onValueChange = { reference = it },
                    label = { Text("Reference / Receipt No") },
                    placeholder = { Text("e.g. BKASH-10294 or REC-002") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = remarks,
                    onValueChange = { remarks = it },
                    label = { Text("Remarks (Optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount > 0) {
                        onAdd(
                            amount,
                            incomeType,
                            if (incomeType == "MEMBER_DEPOSIT") selectedFlatId else null,
                            payerName,
                            selectedCategory,
                            "$selectedCategory deposit",
                            paymentMethod,
                            reference,
                            remarks
                        )
                    }
                },
                enabled = amountText.toDoubleOrNull() != null,
                modifier = Modifier.testTag("submit_income_button")
            ) {
                Text("Save Income")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
