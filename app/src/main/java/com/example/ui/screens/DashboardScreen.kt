package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.User
import com.example.data.model.UserRole
import com.example.ui.components.*
import com.example.ui.theme.DeficitRed
import com.example.ui.theme.SurplusGreen
import com.example.ui.viewmodel.VillaViewModel

@Composable
fun DashboardScreen(
    currentUser: User,
    viewModel: VillaViewModel,
    onNavigateToAccounts: () -> Unit,
    onNavigateToStatements: () -> Unit,
    onNavigateToCommunity: () -> Unit
) {
    val flatStatements by viewModel.flatStatements.collectAsState()
    val allIncomes by viewModel.allIncomes.collectAsState()
    val allExpenses by viewModel.allExpenses.collectAsState()
    val gasBills by viewModel.gasBills.collectAsState()
    val announcements by viewModel.announcements.collectAsState()
    val pendingApprovals by viewModel.pendingChangeRequests.collectAsState()

    // Blinking animation for unpaid bills past 10th
    val infiniteTransition = rememberInfiniteTransition(label = "due_blink_transition")
    val blinkAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "due_blink_alpha"
    )

    // Calculations
    val totalIncome = allIncomes.sumOf { it.amount }
    val totalExpense = allExpenses.sumOf { it.amount }
    val buildingReserve = totalIncome - totalExpense

    val outstandingGas = gasBills.filter { it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }.sumOf { it.amount }
    val outstandingService = flatStatements.sumOf { it.serviceChargeDue }

    // User's own statement if Flat Owner, Renter, Guard, or Governance Member with flat
    val myStatement = flatStatements.find { it.flatId == currentUser.flatId }
    val isGuard = currentUser.role == UserRole.GUARD
    val guardUnpaidGas = gasBills.filter { (it.userId == currentUser.userId || (isGuard && it.flatId == "Guard Room")) && it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
    val guardGasDue = guardUnpaidGas.sumOf { it.amount }

    val userUnpaidGas = gasBills.filter { 
        (it.flatId == currentUser.flatId || it.userId == currentUser.userId || (isGuard && it.flatId == "Guard Room")) && 
        it.paymentStatus != "Paid" && it.paymentStatus != "Waived" 
    }
    val myServiceChargeDue = if (isGuard) 0.0 else (myStatement?.serviceChargeDue ?: 0.0)
    val myGasDue = if (isGuard) guardGasDue else (if (userUnpaidGas.isNotEmpty()) userUnpaidGas.sumOf { it.amount } else (myStatement?.gasBillDue ?: 0.0))
    val hasUnpaidDue = myServiceChargeDue > 0.0 || myGasDue > 0.0

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(4.dp))
            // Welcome Header
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Welcome, ${currentUser.fullName}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = when (currentUser.role) {
                                UserRole.SYSTEM_ADMIN -> "System Administrator • SAS-Siddik Villa"
                                UserRole.DELEGATED_ADMIN -> "Delegated Administrator • Flat ${currentUser.flatId ?: "Management"}"
                                UserRole.GOVERNANCE_MEMBER -> "Managing Committee Member • Flat ${currentUser.flatId ?: ""}"
                                UserRole.FLAT_OWNER -> "Flat Owner • Flat ${currentUser.flatId}"
                                UserRole.RENTER -> "Tenant / Renter • Flat ${currentUser.flatId}"
                                UserRole.GUARD -> "Building Security • Day Shift"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = currentUser.flatId?.let { "Unit $it" } ?: "HQ",
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }

        // BLINKING DUE STATUS ALERT ON OWN DASHBOARD (for all residents/guards with unpaid dues)
        if (hasUnpaidDue && currentUser.role != UserRole.SYSTEM_ADMIN) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = 2.dp,
                            color = DeficitRed.copy(alpha = blinkAlpha),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .testTag("blinking_due_alert"),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DeficitRed.copy(alpha = 0.15f * blinkAlpha)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(DeficitRed.copy(alpha = blinkAlpha), RoundedCornerShape(20.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PriorityHigh,
                                contentDescription = "Due Alert",
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "OVERDUE PAYMENT NOTICE",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.labelMedium,
                                color = DeficitRed
                            )
                            Text(
                                text = "Monthly bills must be cleared by the 10th of each month. Your account has pending dues:",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (myServiceChargeDue > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DeficitRed.copy(alpha = blinkAlpha)
                                    ) {
                                        Text(
                                            text = "Service: ${Formatters.formatBdt(myServiceChargeDue)} DUE (by 10th)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (myGasDue > 0) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = DeficitRed.copy(alpha = blinkAlpha)
                                    ) {
                                        Text(
                                            text = "Gas: ${Formatters.formatBdt(myGasDue)} DUE (by 10th)",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                if (isGuard) {
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.tertiaryContainer
                                    ) {
                                        Text(
                                            text = "Service Charge: ৳0 (Guard Exempt)",
                                            color = MaterialTheme.colorScheme.onTertiaryContainer,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // ROLE SPECIFIC CONTENT
        when (currentUser.role) {
            UserRole.SYSTEM_ADMIN, UserRole.DELEGATED_ADMIN, UserRole.GOVERNANCE_MEMBER -> {
                // Admin / Committee Financial Summary Cards
                item {
                    SectionHeader(
                        title = "Building Financial Overview",
                        subtitle = "Real-time Reserve & Operating Fund",
                        actionText = "All Statements",
                        onActionClick = onNavigateToStatements
                    )
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Reserve Fund",
                            value = Formatters.formatBdt(buildingReserve),
                            subtitle = "Cash In Hand & Bank",
                            icon = Icons.Default.AccountBalance,
                            accentColor = if (buildingReserve >= 0) SurplusGreen else DeficitRed,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Total Collected",
                            value = Formatters.formatBdt(totalIncome),
                            subtitle = "All Receipts",
                            icon = Icons.Default.TrendingUp,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Total Expenses",
                            value = Formatters.formatBdt(totalExpense),
                            subtitle = "Common + Individual",
                            icon = Icons.Default.TrendingDown,
                            accentColor = MaterialTheme.colorScheme.error,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Due Receivables",
                            value = Formatters.formatBdt(outstandingService + outstandingGas),
                            subtitle = "Service + Gas",
                            icon = Icons.Default.PendingActions,
                            accentColor = Color(0xFFE67E22),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Pending Approvals Alert
                if (pendingApprovals.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.NotificationImportant,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${pendingApprovals.size} Profile Change Request(s) Pending Authorisation",
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        text = "Review and authorise submitted profile changes under More > Profiles",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            UserRole.FLAT_OWNER -> {
                // Flat Owner Dashboard
                item {
                    SectionHeader(title = "My Flat Financial Position (Flat ${currentUser.flatId})")
                }

                item {
                    val balance = myStatement?.netBalance ?: 0.0
                    val isSurplus = balance >= 0
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSurplus) SurplusGreen.copy(alpha = 0.12f) else DeficitRed.copy(alpha = 0.12f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NET FINANCIAL BALANCE",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSurplus) SurplusGreen else DeficitRed
                                )
                                BalancePill(balance = balance)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = Formatters.formatBdt(balance),
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (isSurplus) SurplusGreen else DeficitRed
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Formula: Own Deposits (৳${myStatement?.totalDeposits?.toInt() ?: 0}) - Common Share ÷ 14 (৳${myStatement?.shareOfCommonExpense?.toInt() ?: 0}) - Assigned (৳${myStatement?.individualAssignedExpense?.toInt() ?: 0})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "My Total Deposits",
                            value = Formatters.formatBdt(myStatement?.totalDeposits ?: 0.0),
                            subtitle = "Confirmed Payments",
                            icon = Icons.Default.Savings,
                            accentColor = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "My Common Share",
                            value = Formatters.formatBdt(myStatement?.shareOfCommonExpense ?: 0.0),
                            subtitle = "Building Expense ÷ 14",
                            icon = Icons.Default.PieChart,
                            accentColor = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Service Charge Due",
                            value = Formatters.formatBdt(myStatement?.serviceChargeDue ?: 0.0),
                            subtitle = if ((myStatement?.serviceChargeDue ?: 0.0) == 0.0) "Cleared" else "Unpaid past 10th",
                            icon = Icons.Default.Receipt,
                            accentColor = if ((myStatement?.serviceChargeDue ?: 0.0) == 0.0) SurplusGreen else DeficitRed,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if ((myStatement?.serviceChargeDue ?: 0.0) > 0)
                                        Modifier.border(1.dp, DeficitRed.copy(alpha = blinkAlpha), RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                        )
                        StatCard(
                            title = "Gas Bill Due",
                            value = Formatters.formatBdt(myStatement?.gasBillDue ?: 0.0),
                            subtitle = if ((myStatement?.gasBillDue ?: 0.0) == 0.0) "Paid" else "Unpaid past 10th",
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = if ((myStatement?.gasBillDue ?: 0.0) == 0.0) SurplusGreen else DeficitRed,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if ((myStatement?.gasBillDue ?: 0.0) > 0)
                                        Modifier.border(1.dp, DeficitRed.copy(alpha = blinkAlpha), RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                        )
                    }
                }
            }

            UserRole.RENTER -> {
                // Renter Dashboard
                item {
                    SectionHeader(title = "Tenant Financial & Utilities (Flat ${currentUser.flatId})")
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatCard(
                            title = "Monthly Service",
                            value = Formatters.formatBdt(myStatement?.serviceChargeDue ?: 0.0),
                            subtitle = if ((myStatement?.serviceChargeDue ?: 0.0) == 0.0) "Cleared" else "Due past 10th",
                            icon = Icons.Default.ReceiptLong,
                            accentColor = if ((myStatement?.serviceChargeDue ?: 0.0) == 0.0) SurplusGreen else DeficitRed,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if ((myStatement?.serviceChargeDue ?: 0.0) > 0)
                                        Modifier.border(1.dp, DeficitRed.copy(alpha = blinkAlpha), RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                        )
                        StatCard(
                            title = "Gas Bill (Individual)",
                            value = Formatters.formatBdt(myStatement?.gasBillDue ?: 0.0),
                            subtitle = if ((myStatement?.gasBillDue ?: 0.0) == 0.0) "Paid" else "Due past 10th",
                            icon = Icons.Default.LocalFireDepartment,
                            accentColor = if ((myStatement?.gasBillDue ?: 0.0) == 0.0) SurplusGreen else DeficitRed,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if ((myStatement?.gasBillDue ?: 0.0) > 0)
                                        Modifier.border(1.dp, DeficitRed.copy(alpha = blinkAlpha), RoundedCornerShape(16.dp))
                                    else Modifier
                                )
                        )
                    }
                }
            }

            UserRole.GUARD -> {
                // Guard Dashboard
                item {
                    SectionHeader(title = "Security & Maintenance Portal")
                }
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(Modifier.width(8.dp))
                                Text("Guard Duty Status", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Text("• Duty Hours: Day Shift (8:00 AM - 8:00 PM)", style = MaterialTheme.typography.bodySmall)
                            Text("• Standby Generator Fuel: 50 Liters Available", style = MaterialTheme.typography.bodySmall)
                            Text("• Emergency Contacts: Secretary (Flat 6A - Mr. Saif Ahmed Sakil), President (Flat 3A - Mr. Abu Bakkar Siddiki)", style = MaterialTheme.typography.bodySmall)
                            val lastGuardGasBill = gasBills.filter { it.userId == currentUser.userId || it.flatId == "Guard Room" }.maxByOrNull { it.id }
                            if (lastGuardGasBill != null && lastGuardGasBill.paymentStatus != "Paid" && lastGuardGasBill.paymentStatus != "Waived") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("• Guard Room Gas Bill: ", style = MaterialTheme.typography.bodySmall)
                                    Surface(shape = RoundedCornerShape(4.dp), color = DeficitRed.copy(alpha = blinkAlpha)) {
                                        Text(
                                            text = "${Formatters.formatBdt(lastGuardGasBill.amount)} DUE by 10th",
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.labelSmall,
                                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                        )
                                    }
                                }
                            } else {
                                Text("• Guard Room Gas Bill: ${Formatters.formatBdt(lastGuardGasBill?.amount ?: 540.0)} (Paid)", style = MaterialTheme.typography.bodySmall)
                            }
                            Text("• Monthly Service Charge: ৳0 (Guard ID is exempted from service charges per policy)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }

        // Common Section: Pinned & Latest Announcements
        item {
            SectionHeader(
                title = "Community Notices",
                actionText = "View All",
                onActionClick = onNavigateToCommunity
            )
        }

        items(announcements.take(2)) { announcement ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (announcement.isPinned)
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (announcement.isPinned) {
                                Icon(
                                    Icons.Default.PushPin,
                                    contentDescription = "Pinned",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                            }
                            Text(
                                text = announcement.title,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        StatusBadge(status = announcement.category)
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = announcement.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Published by ${announcement.publishedBy}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }
        }

        // Recent Financial Transactions Snippet (for Admin / Owners)
        if (currentUser.role != UserRole.GUARD) {
            item {
                SectionHeader(
                    title = "Recent Transactions",
                    actionText = "Transactions",
                    onActionClick = onNavigateToAccounts
                )
            }

            val recentExpenses = allExpenses.take(3)
            items(recentExpenses) { expense ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(DeficitRed.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.ArrowOutward, contentDescription = null, tint = DeficitRed, modifier = Modifier.size(18.dp))
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(expense.subcategory.ifBlank { expense.category }, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text("${expense.date} • ${expense.allocationType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Text(
                            text = "- ${Formatters.formatBdt(expense.amount)}",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DeficitRed
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
