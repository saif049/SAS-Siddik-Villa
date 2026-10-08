package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.SystemInfoDialog
import com.example.ui.viewmodel.AuthViewModel
import com.example.ui.viewmodel.VillaViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScreen(
    authViewModel: AuthViewModel = viewModel(),
    villaViewModel: VillaViewModel = viewModel()
) {
    val currentUser by authViewModel.currentUser.collectAsState()
    val toastMessage by villaViewModel.toastMessage.collectAsState()
    val syncItems by villaViewModel.syncItems.collectAsState()
    val pendingSyncCount = syncItems.count { it.syncStatus == "PENDING" }

    val snackbarHostState = remember { SnackbarHostState() }
    var currentTab by remember { mutableStateOf(0) }
    var showInfoDialog by remember { mutableStateOf(false) }

    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            snackbarHostState.showSnackbar(it)
            villaViewModel.clearToast()
        }
    }

    if (showInfoDialog) {
        SystemInfoDialog(onDismiss = { showInfoDialog = false })
    }

    if (currentUser == null) {
        LoginScreen(
            authViewModel = authViewModel,
            onLoginSuccess = { /* Automatically updates currentUser StateFlow */ }
        )
    } else {
        val user = currentUser!!
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = "SAS-Siddik Villa",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${user.fullName} • ${user.flatId?.let { "Flat $it" } ?: user.role.name.replace("_", " ")}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { villaViewModel.triggerSync() },
                            modifier = Modifier.testTag("topbar_sync_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (pendingSyncCount > 0) {
                                        Badge { Text(pendingSyncCount.toString()) }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Sync,
                                    contentDescription = "Sync Queue"
                                )
                            }
                        }

                        IconButton(
                            onClick = { showInfoDialog = true },
                            modifier = Modifier.testTag("topbar_info_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "About App"
                            )
                        }

                        IconButton(
                            onClick = { authViewModel.logout() },
                            modifier = Modifier.testTag("topbar_logout_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Logout,
                                contentDescription = "Sign Out"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                NavigationBar(
                    modifier = Modifier.testTag("bottom_nav_bar")
                ) {
                    NavigationBarItem(
                        selected = currentTab == 0,
                        onClick = { currentTab = 0 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Dashboard") },
                        label = { Text("Dashboard") },
                        modifier = Modifier.testTag("nav_item_dashboard")
                    )
                    NavigationBarItem(
                        selected = currentTab == 1,
                        onClick = { currentTab = 1 },
                        icon = { Icon(Icons.Default.AccountBalanceWallet, contentDescription = "Accounts") },
                        label = { Text("Accounts") },
                        modifier = Modifier.testTag("nav_item_accounts")
                    )
                    NavigationBarItem(
                        selected = currentTab == 2,
                        onClick = { currentTab = 2 },
                        icon = { Icon(Icons.Default.Assessment, contentDescription = "Statements") },
                        label = { Text("Statements") },
                        modifier = Modifier.testTag("nav_item_statements")
                    )
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Forum, contentDescription = "Community") },
                        label = { Text("Community") },
                        modifier = Modifier.testTag("nav_item_community")
                    )
                    NavigationBarItem(
                        selected = currentTab == 4,
                        onClick = { currentTab = 4 },
                        icon = { Icon(Icons.Default.Tune, contentDescription = "More") },
                        label = { Text("More") },
                        modifier = Modifier.testTag("nav_item_more")
                    )
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (currentTab) {
                    0 -> DashboardScreen(
                        currentUser = user,
                        viewModel = villaViewModel,
                        onNavigateToAccounts = { currentTab = 1 },
                        onNavigateToStatements = { currentTab = 2 },
                        onNavigateToCommunity = { currentTab = 3 }
                    )
                    1 -> IncomeExpenseScreen(
                        currentUser = user,
                        viewModel = villaViewModel
                    )
                    2 -> FinancialStatementsScreen(
                        currentUser = user,
                        viewModel = villaViewModel
                    )
                    3 -> CommunityScreen(
                        currentUser = user,
                        viewModel = villaViewModel
                    )
                    4 -> AdminAndMoreScreen(
                        currentUser = user,
                        viewModel = villaViewModel,
                        onLogout = { authViewModel.logout() }
                    )
                }
            }
        }
    }
}
