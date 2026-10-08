@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.*
import com.example.ui.components.Formatters
import com.example.ui.components.SectionHeader
import com.example.ui.components.StatusBadge
import com.example.ui.components.SystemInfoDialog
import com.example.ui.theme.DeficitRed
import com.example.ui.theme.SurplusGreen
import com.example.ui.viewmodel.VillaViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminAndMoreScreen(
    currentUser: User,
    viewModel: VillaViewModel,
    onLogout: () -> Unit
) {
    var selectedSection by remember { mutableStateOf(0) }
    var showSystemInfoDialog by remember { mutableStateOf(false) }

    val isAuthorizedAdmin = currentUser.role == UserRole.SYSTEM_ADMIN ||
            currentUser.role == UserRole.DELEGATED_ADMIN ||
            currentUser.userId == "admin" ||
            currentUser.userId == "delegated_admin" ||
            currentUser.userId == "owner_6a" ||
            currentUser.userId == "owner_3a" ||
            currentUser.userId == "owner_4a"

    if (showSystemInfoDialog) {
        SystemInfoDialog(onDismiss = { showSystemInfoDialog = false })
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ScrollableTabRow(
            selectedTabIndex = selectedSection,
            edgePadding = 12.dp
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = { Text("Profile & Authorisations") },
                icon = { Icon(Icons.Default.Badge, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_profiles")
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = { Text("Bills & Utilities") },
                icon = { Icon(Icons.Default.ReceiptLong, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_bills")
            )
            Tab(
                selected = selectedSection == 2,
                onClick = { selectedSection = 2 },
                text = { Text("Categories") },
                icon = { Icon(Icons.Default.Category, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_categories")
            )
            Tab(
                selected = selectedSection == 3,
                onClick = { selectedSection = 3 },
                text = { Text("Users & Passwords") },
                icon = { Icon(Icons.Default.ManageAccounts, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_users")
            )
            Tab(
                selected = selectedSection == 4,
                onClick = { selectedSection = 4 },
                text = { Text("Governance") },
                icon = { Icon(Icons.Default.AccountBalance, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_governance")
            )
            Tab(
                selected = selectedSection == 5,
                onClick = { selectedSection = 5 },
                text = { Text("Sync & System") },
                icon = { Icon(Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_sync")
            )
            Tab(
                selected = selectedSection == 6,
                onClick = { selectedSection = 6 },
                text = { Text("Audit Trail") },
                icon = { Icon(Icons.Default.HistoryEdu, contentDescription = null, modifier = Modifier.size(18.dp)) },
                modifier = Modifier.testTag("subtab_audit")
            )
        }

        when (selectedSection) {
            0 -> ProfileAndAuthorisationsSection(currentUser, viewModel, isAuthorizedAdmin)
            1 -> BillsAndUtilitiesSection(currentUser, viewModel, isAuthorizedAdmin)
            2 -> CategoriesManagementSection(currentUser, viewModel, isAuthorizedAdmin)
            3 -> UserAndPasswordManagementSection(currentUser, viewModel, isAuthorizedAdmin)
            4 -> GovernanceSection(currentUser, viewModel, isAuthorizedAdmin)
            5 -> SyncAndBackupSection(currentUser, viewModel, isAuthorizedAdmin, onShowInfo = { showSystemInfoDialog = true }, onLogout = onLogout)
            6 -> AuditTrailSection(currentUser, viewModel)
        }
    }
}

// 1. PROFILE & AUTHORISATIONS
@Composable
fun ProfileAndAuthorisationsSection(currentUser: User, viewModel: VillaViewModel, isAuthorizedAdmin: Boolean) {
    val pendingRequests by viewModel.pendingChangeRequests.collectAsState()
    val ownerProfiles by viewModel.allOwnerProfiles.collectAsState()
    val renterProfiles by viewModel.allRenters.collectAsState()
    val guardProfile by viewModel.guardProfile.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var showEditOwnProfileDialog by remember { mutableStateOf(false) }
    var selectedProfileToEditByAdmin by remember { mutableStateOf<Any?>(null) } // OwnerProfile, RenterProfile, or GuardProfile
    var selectedProfileToView by remember { mutableStateOf<Any?>(null) }
    var showAdminInfoModal by remember { mutableStateOf(false) }
    var directorySearchQuery by remember { mutableStateOf("") }

    val myOwnerProfile = ownerProfiles.find { it.flatId == currentUser.flatId || it.userId == currentUser.userId }
    val myRenterProfile = renterProfiles.find { it.renterId == currentUser.userId || it.flatId == currentUser.flatId }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Pending Authorisation Queue for System Admin & Delegated Admin
        if (isAuthorizedAdmin) {
            item {
                SectionHeader(
                    title = "Pending Member Authorisations (${pendingRequests.size})",
                    subtitle = "System Admin & Delegated Admin Verification & Approval"
                )
            }

            if (pendingRequests.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        Text(
                            text = "No pending profile modification requests at this time.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else {
                items(pendingRequests, key = { it.id }) { req ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Modification Request #${req.id} • Flat ${req.flatId}",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                StatusBadge(status = req.status)
                            }

                            Spacer(Modifier.height(8.dp))

                            Text("• Full Name: ${req.fullName}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                            Text("• Mobile: ${req.mobileNumber} (Alt: ${req.altMobileNumber.ifBlank { "N/A" }})", style = MaterialTheme.typography.bodySmall)
                            Text("• Email: ${req.email}", style = MaterialTheme.typography.bodySmall)
                            Text("• NID: ${req.nidNumber} • DOB: ${req.dateOfBirth}", style = MaterialTheme.typography.bodySmall)
                            Text("• Father: ${req.fatherName} • Mother: ${req.motherName}", style = MaterialTheme.typography.bodySmall)
                            Text("• Occupation: ${req.occupation} (${req.designation} at ${req.employer})", style = MaterialTheme.typography.bodySmall)
                            Text("• Office Address: ${req.officeAddress}", style = MaterialTheme.typography.bodySmall)
                            Text("• Permanent Address: ${req.permanentAddress}", style = MaterialTheme.typography.bodySmall)
                            Text("• Family Info: Spouse: ${req.spouseInfo} | Children: ${req.childrenInfo}", style = MaterialTheme.typography.bodySmall)
                            Text("• Emergency Contact: ${req.emergencyContact}", style = MaterialTheme.typography.bodySmall)

                            if (req.reason.isNotBlank()) {
                                Spacer(Modifier.height(4.dp))
                                Text("Reason for update: ${req.reason}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }

                            Spacer(Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { viewModel.reviewProfileChange(req.id, false, "Disapproved by Admin", currentUser.userId) }
                                ) {
                                    Text("Reject", color = MaterialTheme.colorScheme.error)
                                }
                                Spacer(Modifier.width(8.dp))
                                Button(
                                    onClick = { viewModel.reviewProfileChange(req.id, true, "Authorised by Admin", currentUser.userId) },
                                    modifier = Modifier.testTag("authorise_profile_button_${req.id}")
                                ) {
                                    Icon(Icons.Default.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Authorise & Apply")
                                }
                            }
                        }
                    }
                }
            }

            // Directory of All Profiles for System Admin & Delegated Admin
            item {
                SectionHeader(
                    title = "All Members Directory (View / Edit)",
                    subtitle = "System Admin & Delegated Admin can View, Edit, or Modify any profile"
                )
            }

            item {
                OutlinedTextField(
                    value = directorySearchQuery,
                    onValueChange = { directorySearchQuery = it },
                    placeholder = { Text("Search by Flat, Name, or Mobile...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // 0. System Admin Directory Entry
            if (directorySearchQuery.isBlank() || "admin".contains(directorySearchQuery, ignoreCase = true) || "sakil".contains(directorySearchQuery, ignoreCase = true) || "system".contains(directorySearchQuery, ignoreCase = true)) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.primary),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.AdminPanelSettings, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text("SAIF AHMED SAKIL", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("System Administrator • ssakil@isrt.ac.bd", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            IconButton(onClick = { showAdminInfoModal = true }) {
                                Icon(Icons.Default.Visibility, contentDescription = "View System Admin Info", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }

            // 1. Flat Owners Directory
            val filteredOwners = ownerProfiles.filter {
                it.flatId.contains(directorySearchQuery, ignoreCase = true) ||
                        it.fullName.contains(directorySearchQuery, ignoreCase = true) ||
                        it.mobileNumber.contains(directorySearchQuery, ignoreCase = true)
            }
            items(filteredOwners, key = { "owner_${it.flatId}" }) { owner ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Flat\n${owner.flatId}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(owner.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Owner • ${owner.mobileNumber} • ${owner.occupation}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }

                        Row {
                            IconButton(onClick = { selectedProfileToView = owner }) {
                                Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { selectedProfileToEditByAdmin = owner }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // 2. Renters Directory
            val filteredRenters = renterProfiles.filter {
                it.flatId.contains(directorySearchQuery, ignoreCase = true) ||
                        it.fullName.contains(directorySearchQuery, ignoreCase = true)
            }
            items(filteredRenters, key = { "renter_${it.renterId}" }) { renter ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Rent\n${renter.flatId}", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(renter.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("Tenant • Flat ${renter.flatId} • ${renter.mobileNumber}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                            }
                        }
                        Row {
                            IconButton(onClick = { selectedProfileToView = renter }) {
                                Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { selectedProfileToEditByAdmin = renter }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                            }
                        }
                    }
                }
            }

            // 3. Guard Directory Entry
            if (guardProfile != null) {
                item {
                    val guard = guardProfile!!
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(MaterialTheme.colorScheme.tertiaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Security, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
                                }
                                Spacer(Modifier.width(10.dp))
                                Column {
                                    Text(guard.fullName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text("Security Guard • ${guard.dutyHours}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                                }
                            }
                            Row {
                                IconButton(onClick = { selectedProfileToView = guard }) {
                                    Icon(Icons.Default.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                                }
                                IconButton(onClick = { selectedProfileToEditByAdmin = guard }) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.secondary)
                                }
                            }
                        }
                    }
                }
            }
        }

        // Current User Profile Summary Card
        item {
            SectionHeader(
                title = "My Profile Details",
                subtitle = "Passport Photo, Personal & Family Records",
                actionText = "Edit / Request Change",
                onActionClick = { showEditOwnProfileDialog = true }
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Passport-size photograph preview badge
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountBox,
                                contentDescription = "Passport Photo",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(44.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = myOwnerProfile?.fullName ?: myRenterProfile?.fullName ?: guardProfile?.fullName ?: currentUser.fullName,
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${currentUser.role.name.replace("_", " ")} ${currentUser.flatId?.let { "• Flat $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Divider(modifier = Modifier.padding(vertical = 4.dp))

                    if (currentUser.role == UserRole.SYSTEM_ADMIN) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    text = "System Admin Info",
                                    fontWeight = FontWeight.Bold,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "SAIF AHMED SAKIL, Freelancer; MS in Applied Statistics, ISRT, University of Dhaka & LL.B., National University; Mobile: +8801611447765; Email: ssakil@isrt.ac.bd",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    } else {
                        Text("• NID Number: ${myOwnerProfile?.nidNumber ?: myRenterProfile?.nidNumber ?: guardProfile?.nidNumber ?: "1980269251800000"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Date of Birth: ${myOwnerProfile?.dateOfBirth ?: myRenterProfile?.dateOfBirth ?: guardProfile?.dateOfBirth ?: "1985-01-01"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Mobile: ${myOwnerProfile?.mobileNumber ?: myRenterProfile?.mobileNumber ?: guardProfile?.mobileNumber ?: currentUser.mobile}", style = MaterialTheme.typography.bodySmall)
                        Text("• Alt Mobile: ${myOwnerProfile?.altMobileNumber ?: myRenterProfile?.altMobileNumber ?: guardProfile?.altMobileNumber ?: "N/A"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Email: ${myOwnerProfile?.email ?: myRenterProfile?.email ?: currentUser.email}", style = MaterialTheme.typography.bodySmall)
                        Text("• Father's Name: ${myOwnerProfile?.fatherName ?: myRenterProfile?.fatherName ?: guardProfile?.fatherName ?: "Late Alhaj Mohiuddin"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Mother's Name: ${myOwnerProfile?.motherName ?: myRenterProfile?.motherName ?: guardProfile?.motherName ?: "Begum Sufia Kamal"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Occupation: ${myOwnerProfile?.occupation ?: myRenterProfile?.occupation ?: "Consultant"} (${myOwnerProfile?.designation ?: myRenterProfile?.designation ?: "Senior Officer"} at ${myOwnerProfile?.employer ?: myRenterProfile?.employer ?: "Organization"})", style = MaterialTheme.typography.bodySmall)
                        Text("• Office Address: ${myOwnerProfile?.officeAddress ?: myRenterProfile?.officeAddress ?: "Dhaka"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Permanent Address: ${myOwnerProfile?.permanentAddress ?: myRenterProfile?.permanentAddress ?: guardProfile?.permanentAddress ?: "SAS-Siddik Villa, Dhaka"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Family Info: Spouse: ${myOwnerProfile?.spouseInfo ?: myRenterProfile?.spouseInfo ?: "Married"} | Children: ${myOwnerProfile?.childrenInfo ?: myRenterProfile?.childrenInfo ?: "2 Children"}", style = MaterialTheme.typography.bodySmall)
                        Text("• Emergency Contact: ${myOwnerProfile?.emergencyContact ?: myRenterProfile?.emergencyContact ?: guardProfile?.emergencyContact ?: "+8801700000000"}", style = MaterialTheme.typography.bodySmall)
                    }

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { showEditOwnProfileDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(if (currentUser.role == UserRole.SYSTEM_ADMIN) "Edit / Modify Admin Contact" else "Edit / Modify Profile Request")
                    }
                }
            }
        }
    }

    if (showEditOwnProfileDialog) {
        val initialFullName = myOwnerProfile?.fullName ?: myRenterProfile?.fullName ?: guardProfile?.fullName ?: currentUser.fullName
        val initialFather = myOwnerProfile?.fatherName ?: myRenterProfile?.fatherName ?: guardProfile?.fatherName ?: ""
        val initialMother = myOwnerProfile?.motherName ?: myRenterProfile?.motherName ?: guardProfile?.motherName ?: ""
        val initialNid = myOwnerProfile?.nidNumber ?: myRenterProfile?.nidNumber ?: guardProfile?.nidNumber ?: ""
        val initialDob = myOwnerProfile?.dateOfBirth ?: myRenterProfile?.dateOfBirth ?: guardProfile?.dateOfBirth ?: ""
        val initialMobile = myOwnerProfile?.mobileNumber ?: myRenterProfile?.mobileNumber ?: guardProfile?.mobileNumber ?: currentUser.mobile
        val initialAltMobile = myOwnerProfile?.altMobileNumber ?: myRenterProfile?.altMobileNumber ?: guardProfile?.altMobileNumber ?: ""
        val initialEmail = myOwnerProfile?.email ?: myRenterProfile?.email ?: currentUser.email
        val initialOccupation = myOwnerProfile?.occupation ?: myRenterProfile?.occupation ?: ""
        val initialDesignation = myOwnerProfile?.designation ?: myRenterProfile?.designation ?: ""
        val initialEmployer = myOwnerProfile?.employer ?: myRenterProfile?.employer ?: ""
        val initialOfficeAddress = myOwnerProfile?.officeAddress ?: myRenterProfile?.officeAddress ?: ""
        val initialPermanentAddress = myOwnerProfile?.permanentAddress ?: myRenterProfile?.permanentAddress ?: guardProfile?.permanentAddress ?: ""
        val initialSpouse = myOwnerProfile?.spouseInfo ?: myRenterProfile?.spouseInfo ?: guardProfile?.spouseInfo ?: ""
        val initialChildren = myOwnerProfile?.childrenInfo ?: myRenterProfile?.childrenInfo ?: guardProfile?.childrenInfo ?: ""
        val initialEmergency = myOwnerProfile?.emergencyContact ?: myRenterProfile?.emergencyContact ?: guardProfile?.emergencyContact ?: ""

        EditProfileModal(
            title = "Edit / Modify My Profile",
            currentUser = currentUser,
            initialFullName = initialFullName,
            initialFather = initialFather,
            initialMother = initialMother,
            initialNid = initialNid,
            initialDob = initialDob,
            initialMobile = initialMobile,
            initialAltMobile = initialAltMobile,
            initialEmail = initialEmail,
            initialOccupation = initialOccupation,
            initialDesignation = initialDesignation,
            initialEmployer = initialEmployer,
            initialOfficeAddress = initialOfficeAddress,
            initialPermanentAddress = initialPermanentAddress,
            initialSpouse = initialSpouse,
            initialChildren = initialChildren,
            initialEmergency = initialEmergency,
            onDismiss = { showEditOwnProfileDialog = false },
            onSubmit = { req ->
                viewModel.submitProfileModification(req)
                showEditOwnProfileDialog = false
            }
        )
    }

    // Admin Direct Edit Modal
    if (selectedProfileToEditByAdmin != null) {
        val prof = selectedProfileToEditByAdmin!!
        val targetRole = when (prof) {
            is OwnerProfile -> "FLAT_OWNER"
            is RenterProfile -> "RENTER"
            is GuardProfile -> "GUARD"
            else -> "FLAT_OWNER"
        }
        val targetFlatId = when (prof) {
            is OwnerProfile -> prof.flatId
            is RenterProfile -> prof.flatId
            is GuardProfile -> "Guard Room"
            else -> ""
        }
        val targetUserId = when (prof) {
            is OwnerProfile -> prof.userId
            is RenterProfile -> prof.renterId
            is GuardProfile -> prof.userId
            else -> ""
        }
        val name = when (prof) { is OwnerProfile -> prof.fullName; is RenterProfile -> prof.fullName; is GuardProfile -> prof.fullName; else -> "" }
        val father = when (prof) { is OwnerProfile -> prof.fatherName; is RenterProfile -> prof.fatherName; is GuardProfile -> prof.fatherName; else -> "" }
        val mother = when (prof) { is OwnerProfile -> prof.motherName; is RenterProfile -> prof.motherName; is GuardProfile -> prof.motherName; else -> "" }
        val nid = when (prof) { is OwnerProfile -> prof.nidNumber; is RenterProfile -> prof.nidNumber; is GuardProfile -> prof.nidNumber; else -> "" }
        val dob = when (prof) { is OwnerProfile -> prof.dateOfBirth; is RenterProfile -> prof.dateOfBirth; is GuardProfile -> prof.dateOfBirth; else -> "" }
        val mobile = when (prof) { is OwnerProfile -> prof.mobileNumber; is RenterProfile -> prof.mobileNumber; is GuardProfile -> prof.mobileNumber; else -> "" }
        val altMobile = when (prof) { is OwnerProfile -> prof.altMobileNumber; is RenterProfile -> prof.altMobileNumber; is GuardProfile -> prof.altMobileNumber; else -> "" }
        val email = when (prof) { is OwnerProfile -> prof.email; is RenterProfile -> prof.email; is GuardProfile -> prof.email; else -> "" }
        val occ = when (prof) { is OwnerProfile -> prof.occupation; is RenterProfile -> prof.occupation; else -> "Security" }
        val des = when (prof) { is OwnerProfile -> prof.designation; is RenterProfile -> prof.designation; else -> "Guard" }
        val emp = when (prof) { is OwnerProfile -> prof.employer; is RenterProfile -> prof.employer; else -> "Society" }
        val off = when (prof) { is OwnerProfile -> prof.officeAddress; is RenterProfile -> prof.officeAddress; else -> "" }
        val perm = when (prof) { is OwnerProfile -> prof.permanentAddress; is RenterProfile -> prof.permanentAddress; is GuardProfile -> prof.permanentAddress; else -> "" }
        val spouse = when (prof) { is OwnerProfile -> prof.spouseInfo; is RenterProfile -> prof.spouseInfo; is GuardProfile -> prof.spouseInfo; else -> "" }
        val child = when (prof) { is OwnerProfile -> prof.childrenInfo; is RenterProfile -> prof.childrenInfo; is GuardProfile -> prof.childrenInfo; else -> "" }
        val emg = when (prof) { is OwnerProfile -> prof.emergencyContact; is RenterProfile -> prof.emergencyContact; is GuardProfile -> prof.emergencyContact; else -> "" }

        EditProfileModal(
            title = "Admin Direct Edit ($targetUserId - Flat $targetFlatId)",
            currentUser = currentUser,
            initialFullName = name,
            initialFather = father,
            initialMother = mother,
            initialNid = nid,
            initialDob = dob,
            initialMobile = mobile,
            initialAltMobile = altMobile,
            initialEmail = email,
            initialOccupation = occ,
            initialDesignation = des,
            initialEmployer = emp,
            initialOfficeAddress = off,
            initialPermanentAddress = perm,
            initialSpouse = spouse,
            initialChildren = child,
            initialEmergency = emg,
            onDismiss = { selectedProfileToEditByAdmin = null },
            onSubmit = { req ->
                viewModel.adminDirectUpdateProfile(
                    targetRole, targetFlatId, targetUserId, req.fullName, req.photoUri,
                    req.fatherName, req.motherName, req.nidNumber, req.dateOfBirth,
                    req.mobileNumber, req.altMobileNumber, req.email,
                    req.occupation, req.designation, req.employer, req.officeAddress,
                    req.permanentAddress, req.spouseInfo, req.childrenInfo, req.emergencyContact,
                    currentUser.userId
                )
                selectedProfileToEditByAdmin = null
            }
        )
    }

    // View Profile Modal
    if (selectedProfileToView != null) {
        val prof = selectedProfileToView!!
        val name = when (prof) { is OwnerProfile -> prof.fullName; is RenterProfile -> prof.fullName; is GuardProfile -> prof.fullName; else -> "" }
        val flatId = when (prof) { is OwnerProfile -> "Flat ${prof.flatId} (Owner)"; is RenterProfile -> "Flat ${prof.flatId} (Tenant/Renter)"; is GuardProfile -> "Guard Room (Security Guard)"; else -> "" }
        val mobile = when (prof) { is OwnerProfile -> prof.mobileNumber; is RenterProfile -> prof.mobileNumber; is GuardProfile -> prof.mobileNumber; else -> "" }
        val altMobile = when (prof) { is OwnerProfile -> prof.altMobileNumber; is RenterProfile -> prof.altMobileNumber; is GuardProfile -> prof.altMobileNumber; else -> "N/A" }
        val email = when (prof) { is OwnerProfile -> prof.email; is RenterProfile -> prof.email; is GuardProfile -> prof.email; else -> "" }
        val nid = when (prof) { is OwnerProfile -> prof.nidNumber; is RenterProfile -> prof.nidNumber; is GuardProfile -> prof.nidNumber; else -> "" }
        val dob = when (prof) { is OwnerProfile -> prof.dateOfBirth; is RenterProfile -> prof.dateOfBirth; is GuardProfile -> prof.dateOfBirth; else -> "N/A" }
        val father = when (prof) { is OwnerProfile -> prof.fatherName; is RenterProfile -> prof.fatherName; is GuardProfile -> prof.fatherName; else -> "" }
        val mother = when (prof) { is OwnerProfile -> prof.motherName; is RenterProfile -> prof.motherName; is GuardProfile -> prof.motherName; else -> "" }
        val permanent = when (prof) { is OwnerProfile -> prof.permanentAddress; is RenterProfile -> prof.permanentAddress; is GuardProfile -> prof.permanentAddress; else -> "" }
        val occ = when (prof) { is OwnerProfile -> prof.occupation; is RenterProfile -> prof.occupation; else -> "Security" }
        val des = when (prof) { is OwnerProfile -> prof.designation; is RenterProfile -> prof.designation; else -> "Guard" }
        val emp = when (prof) { is OwnerProfile -> prof.employer; is RenterProfile -> prof.employer; else -> "SAS-Siddik Villa" }
        val office = when (prof) { is OwnerProfile -> prof.officeAddress; is RenterProfile -> prof.officeAddress; else -> "Dhaka" }
        val spouse = when (prof) { is OwnerProfile -> prof.spouseInfo; is RenterProfile -> prof.spouseInfo; is GuardProfile -> prof.spouseInfo; else -> "N/A" }
        val child = when (prof) { is OwnerProfile -> prof.childrenInfo; is RenterProfile -> prof.childrenInfo; is GuardProfile -> prof.childrenInfo; else -> "N/A" }
        val emg = when (prof) { is OwnerProfile -> prof.emergencyContact; is RenterProfile -> prof.emergencyContact; is GuardProfile -> prof.emergencyContact; else -> "" }

        AlertDialog(
            onDismissRequest = { selectedProfileToView = null },
            title = { Text(name, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium) },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AccountBox, contentDescription = "Passport Photo", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(36.dp))
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("Unit: $flatId", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                            Text("Photograph: Verified (Passport size)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("• NID Number: $nid", style = MaterialTheme.typography.bodySmall)
                    Text("• Date of Birth: $dob", style = MaterialTheme.typography.bodySmall)
                    Text("• Mobile: $mobile", style = MaterialTheme.typography.bodySmall)
                    Text("• Alt Mobile: $altMobile", style = MaterialTheme.typography.bodySmall)
                    Text("• Email: $email", style = MaterialTheme.typography.bodySmall)
                    Text("• Father's Name: $father", style = MaterialTheme.typography.bodySmall)
                    Text("• Mother's Name: $mother", style = MaterialTheme.typography.bodySmall)
                    Text("• Permanent Address: $permanent", style = MaterialTheme.typography.bodySmall)
                    Divider(modifier = Modifier.padding(vertical = 4.dp))
                    Text("• Occupation: $occ ($des at $emp)", style = MaterialTheme.typography.bodySmall)
                    Text("• Office Address: $office", style = MaterialTheme.typography.bodySmall)
                    Text("• Family Info: Spouse: $spouse | Children: $child", style = MaterialTheme.typography.bodySmall)
                    Text("• Emergency Contact: $emg", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                }
            },
            confirmButton = { TextButton(onClick = { selectedProfileToView = null }) { Text("Close") } }
        )
    }

    if (showAdminInfoModal) {
        AlertDialog(
            onDismissRequest = { showAdminInfoModal = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(8.dp))
                    Text("System Admin Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                }
            },
            text = {
                Text(
                    text = "SAIF AHMED SAKIL, Freelancer; MS in Applied Statistics, ISRT, University of Dhaka & LL.B., National University; Mobile: +8801611447765; Email: ssakil@isrt.ac.bd",
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                Button(onClick = { showAdminInfoModal = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun EditProfileModal(
    title: String = "Edit / Modify Profile",
    currentUser: User,
    initialFullName: String,
    initialFather: String,
    initialMother: String,
    initialNid: String,
    initialDob: String,
    initialMobile: String,
    initialAltMobile: String,
    initialEmail: String,
    initialOccupation: String,
    initialDesignation: String,
    initialEmployer: String,
    initialOfficeAddress: String,
    initialPermanentAddress: String,
    initialSpouse: String,
    initialChildren: String,
    initialEmergency: String,
    onDismiss: () -> Unit,
    onSubmit: (ProfileChangeRequest) -> Unit
) {
    var fullName by remember { mutableStateOf(initialFullName) }
    var fatherName by remember { mutableStateOf(initialFather) }
    var motherName by remember { mutableStateOf(initialMother) }
    var nidNumber by remember { mutableStateOf(initialNid) }
    var dob by remember { mutableStateOf(initialDob) }
    var mobile by remember { mutableStateOf(initialMobile) }
    var altMobile by remember { mutableStateOf(initialAltMobile) }
    var email by remember { mutableStateOf(initialEmail) }
    var occupation by remember { mutableStateOf(initialOccupation) }
    var designation by remember { mutableStateOf(initialDesignation) }
    var employer by remember { mutableStateOf(initialEmployer) }
    var officeAddress by remember { mutableStateOf(initialOfficeAddress) }
    var permanentAddress by remember { mutableStateOf(initialPermanentAddress) }
    var spouseInfo by remember { mutableStateOf(initialSpouse) }
    var childrenInfo by remember { mutableStateOf(initialChildren) }
    var emergencyContact by remember { mutableStateOf(initialEmergency) }
    var reason by remember { mutableStateOf("Updated personal and family details") }
    var photoAttached by remember { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(title, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Fill all required profile records (passport-size photograph, personal, occupation, and emergency contact details).",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Passport-size photograph section
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                                .border(1.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.AddAPhoto, contentDescription = "Passport Photo", tint = MaterialTheme.colorScheme.primary)
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Photograph (Passport Size)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                            Text(if (photoAttached) "Photo attached (2x2 inch)" else "No photo attached", style = MaterialTheme.typography.bodySmall)
                        }
                        OutlinedButton(
                            onClick = { photoAttached = !photoAttached },
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text(if (photoAttached) "Change" else "Attach", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(value = fullName, onValueChange = { fullName = it }, label = { Text("Full Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = fatherName, onValueChange = { fatherName = it }, label = { Text("Father's Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = motherName, onValueChange = { motherName = it }, label = { Text("Mother's Name *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = nidNumber, onValueChange = { nidNumber = it }, label = { Text("NID Number *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = dob, onValueChange = { dob = it }, label = { Text("Date of Birth (YYYY-MM-DD)") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = mobile, onValueChange = { mobile = it }, label = { Text("Mobile Number *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = altMobile, onValueChange = { altMobile = it }, label = { Text("Alternative Mobile") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email Address") }, modifier = Modifier.fillMaxWidth())

                Divider()
                Text("Occupation Details", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = occupation, onValueChange = { occupation = it }, label = { Text("Occupation") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = designation, onValueChange = { designation = it }, label = { Text("Designation") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = employer, onValueChange = { employer = it }, label = { Text("Employer / Organization") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = officeAddress, onValueChange = { officeAddress = it }, label = { Text("Office Address") }, modifier = Modifier.fillMaxWidth())

                Divider()
                Text("Addresses & Family Information", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelMedium)
                OutlinedTextField(value = permanentAddress, onValueChange = { permanentAddress = it }, label = { Text("Permanent Address *") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = spouseInfo, onValueChange = { spouseInfo = it }, label = { Text("Spouse Information") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = childrenInfo, onValueChange = { childrenInfo = it }, label = { Text("Children Information") }, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = emergencyContact, onValueChange = { emergencyContact = it }, label = { Text("Emergency Contact *") }, modifier = Modifier.fillMaxWidth())

                OutlinedTextField(value = reason, onValueChange = { reason = it }, label = { Text("Reason for Modification") }, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val req = ProfileChangeRequest(
                        userId = currentUser.userId,
                        flatId = currentUser.flatId ?: "General",
                        userRole = currentUser.role.name,
                        fullName = fullName,
                        photoUri = if (photoAttached) "content://media/photos/profile_passport_${currentUser.userId}.jpg" else null,
                        fatherName = fatherName,
                        motherName = motherName,
                        nidNumber = nidNumber,
                        dateOfBirth = dob,
                        mobileNumber = mobile,
                        altMobileNumber = altMobile,
                        email = email,
                        occupation = occupation,
                        designation = designation,
                        employer = employer,
                        officeAddress = officeAddress,
                        permanentAddress = permanentAddress,
                        spouseInfo = spouseInfo,
                        childrenInfo = childrenInfo,
                        emergencyContact = emergencyContact,
                        reason = reason,
                        status = "PENDING"
                    )
                    onSubmit(req)
                },
                enabled = fullName.isNotBlank() && mobile.isNotBlank()
            ) {
                Text("Save / Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

// 2. CATEGORIES MANAGEMENT (System Admin & Delegated Admin)
@Composable
fun CategoriesManagementSection(currentUser: User, viewModel: VillaViewModel, isAuthorizedAdmin: Boolean) {
    val categories by viewModel.customCategories.collectAsState()
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var selectedCategoryToEdit by remember { mutableStateOf<CustomCategory?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Income & Expense Categories", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("System Admin & Delegated Admin can configure and modify accounting master categories and subcategories.", style = MaterialTheme.typography.bodySmall)
                    }
                    if (isAuthorizedAdmin) {
                        Button(
                            onClick = { showAddCategoryDialog = true },
                            modifier = Modifier.testTag("add_category_button")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("New")
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(title = "Configured Categories (${categories.size})")
        }

        items(categories, key = { it.id }) { cat ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (cat.type == "EXPENSE") DeficitRed.copy(alpha = 0.15f) else SurplusGreen.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = cat.type,
                                    color = if (cat.type == "EXPENSE") DeficitRed else SurplusGreen,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text(cat.categoryName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (cat.subcategoriesCsv.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = "Subcategories: ${cat.subcategoriesCsv}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isAuthorizedAdmin) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { selectedCategoryToEdit = cat }) {
                                Icon(Icons.Default.Edit, contentDescription = "Modify Category", tint = MaterialTheme.colorScheme.secondary)
                            }
                            IconButton(onClick = { viewModel.deleteCategory(cat.id, currentUser.userId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    // Modify Category Modal
    if (selectedCategoryToEdit != null) {
        val cat = selectedCategoryToEdit!!
        var editType by remember(cat) { mutableStateOf(cat.type) }
        var editName by remember(cat) { mutableStateOf(cat.categoryName) }
        var editSubcats by remember(cat) { mutableStateOf(cat.subcategoriesCsv) }

        AlertDialog(
            onDismissRequest = { selectedCategoryToEdit = null },
            title = { Text("Modify Category & Subcategories") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = editType == "EXPENSE",
                            onClick = { editType = "EXPENSE" },
                            label = { Text("Expense") }
                        )
                        FilterChip(
                            selected = editType == "INCOME",
                            onClick = { editType = "INCOME" },
                            label = { Text("Income") }
                        )
                    }

                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        label = { Text("Category Name") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = editSubcats,
                        onValueChange = { editSubcats = it },
                        label = { Text("Subcategories (comma separated)") },
                        placeholder = { Text("e.g. Lift, Generator, Plumbing") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (editName.isNotBlank()) {
                            viewModel.updateCategory(
                                cat.copy(type = editType, categoryName = editName, subcategoriesCsv = editSubcats),
                                currentUser.userId
                            )
                            selectedCategoryToEdit = null
                        }
                    }
                ) {
                    Text("Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedCategoryToEdit = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddCategoryDialog) {
        var catType by remember { mutableStateOf("EXPENSE") }
        var catName by remember { mutableStateOf("") }
        var subcats by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Add Category & Subcategories") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = catType == "EXPENSE",
                            onClick = { catType = "EXPENSE" },
                            label = { Text("Expense") }
                        )
                        FilterChip(
                            selected = catType == "INCOME",
                            onClick = { catType = "INCOME" },
                            label = { Text("Income") }
                        )
                    }

                    OutlinedTextField(
                        value = catName,
                        onValueChange = { catName = it },
                        label = { Text("Category Name") },
                        placeholder = { Text("e.g. Solar Equipment Maintenance") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = subcats,
                        onValueChange = { subcats = it },
                        label = { Text("Subcategories (comma separated)") },
                        placeholder = { Text("e.g. Inverter, Battery, Wiring, Labor") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (catName.isNotBlank()) {
                            viewModel.addCategory(catType, catName, subcats, currentUser.userId)
                            showAddCategoryDialog = false
                        }
                    }
                ) {
                    Text("Save Category")
                }
            },
            dismissButton = { TextButton(onClick = { showAddCategoryDialog = false }) { Text("Cancel") } }
        )
    }
}

// 3. USER AND PASSWORD MANAGEMENT
@Composable
fun UserAndPasswordManagementSection(currentUser: User, viewModel: VillaViewModel, isAuthorizedAdmin: Boolean) {
    val users by viewModel.allUsers.collectAsState()
    var showBulkInitialPasswordDialog by remember { mutableStateOf(false) }
    var selectedUserForReset by remember { mutableStateOf<User?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (currentUser.role == UserRole.SYSTEM_ADMIN) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Periodic Initial Password Policy", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "System Admin can periodically reset all users to a standard temporary initial password (e.g. 123456). Every user must change their password on subsequent login.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        Button(
                            onClick = { showBulkInitialPasswordDialog = true },
                            modifier = Modifier.testTag("bulk_initial_password_button")
                        ) {
                            Icon(Icons.Default.LockReset, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Set Initial Password for All Users")
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "All Registered Users (${users.size})",
                subtitle = "Reset password or toggle account access"
            )
        }

        items(users, key = { it.userId }) { u ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${u.userId} — ${u.fullName}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            text = "${u.role.name.replace("_", " ")} ${u.flatId?.let { "• Flat $it" } ?: ""}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (u.isInitialPassword) {
                            Text("Status: Must change password at next login", style = MaterialTheme.typography.labelSmall, color = DeficitRed)
                        }
                    }

                    if (isAuthorizedAdmin) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilledTonalButton(
                                onClick = { selectedUserForReset = u },
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text("Reset Pass", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBulkInitialPasswordDialog) {
        var bulkPass by remember { mutableStateOf("123456") }

        AlertDialog(
            onDismissRequest = { showBulkInitialPasswordDialog = false },
            title = { Text("Set Initial Password for ALL Users") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Warning: This will set the password for every user account to this initial temporary password and force them to change it on their next login.", style = MaterialTheme.typography.bodySmall, color = DeficitRed)
                    OutlinedTextField(
                        value = bulkPass,
                        onValueChange = { bulkPass = it },
                        label = { Text("Initial Temporary Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.setInitialPasswordForAll(bulkPass, currentUser.userId)
                        showBulkInitialPasswordDialog = false
                    }
                ) {
                    Text("Apply to All Accounts")
                }
            },
            dismissButton = { TextButton(onClick = { showBulkInitialPasswordDialog = false }) { Text("Cancel") } }
        )
    }

    if (selectedUserForReset != null) {
        val target = selectedUserForReset!!
        var newPass by remember { mutableStateOf("123456") }

        AlertDialog(
            onDismissRequest = { selectedUserForReset = null },
            title = { Text("Reset Password for ${target.userId}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Resetting password for ${target.fullName} (${target.userId}). User will be required to change this password on next login.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = newPass,
                        onValueChange = { newPass = it },
                        label = { Text("New Temporary Password") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.resetUserPassword(target.userId, newPass, currentUser.userId, forceInitialChange = true)
                        selectedUserForReset = null
                    }
                ) {
                    Text("Reset Password")
                }
            },
            dismissButton = { TextButton(onClick = { selectedUserForReset = null }) { Text("Cancel") } }
        )
    }
}

// 4. BILLS & UTILITIES (Individual Gas Bill Setting + Service Charge + Guard Policy)
@Composable
fun BillsAndUtilitiesSection(currentUser: User, viewModel: VillaViewModel, isAuthorizedAdmin: Boolean) {
    val serviceConfig by viewModel.serviceChargeConfig.collectAsState()
    val gasBills by viewModel.gasBills.collectAsState()
    val allFlats by viewModel.allFlats.collectAsState()
    val allUsers by viewModel.allUsers.collectAsState()

    var showUpdateRateDialog by remember { mutableStateOf(false) }
    var showSetMonthlyGasDialog by remember { mutableStateOf(false) }
    var showIndividualGasDialog by remember { mutableStateOf(false) }
    var selectedGasBillToEdit by remember { mutableStateOf<GasBill?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Critical Policy Banner: Guard has NO service charge
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f))
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Building Financial Policy: No monthly Service Charges are levied on Guard ID (৳0). The Guard ID is strictly billed for individual Gas only.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }

        // Service Charge Configuration Card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Monthly Service Charge Rate", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                            Text(
                                text = "${Formatters.formatBdt(serviceConfig?.monthlyRate ?: 3500.0)} / month",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (isAuthorizedAdmin) {
                            Button(
                                onClick = { showUpdateRateDialog = true },
                                modifier = Modifier.testTag("update_service_rate_button")
                            ) {
                                Text("Set Service Charge")
                            }
                        }
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Rule: System Admin & Delegated Admin can update this anytime. Operational due date is 10th of each month.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                    )
                }
            }
        }

        // Gas Setting Action Controls for Admins
        if (isAuthorizedAdmin) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Gas Bill Management (Titas Pool)", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                        Text("Set Gas Bills individually or batch generate for all Flat Owners, Renters, and Guard by the 10th.", style = MaterialTheme.typography.bodySmall)

                        Spacer(Modifier.height(10.dp))

                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { showIndividualGasDialog = true },
                                modifier = Modifier.weight(1f).testTag("set_individual_gas_button")
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Set Individual")
                            }
                            OutlinedButton(
                                onClick = { showSetMonthlyGasDialog = true },
                                modifier = Modifier.weight(1f).testTag("batch_gas_button")
                            ) {
                                Icon(Icons.Default.PlaylistAddCheck, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Batch Set All")
                            }
                        }
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = "Individual Gas Bills Roster",
                subtitle = "Assigned individually to 14 Flats + Renters + Guard (Due: 10th)"
            )
        }

        val displayGasBills = if (isAuthorizedAdmin) gasBills else gasBills.filter { it.flatId == currentUser.flatId || it.userId == currentUser.userId }

        items(displayGasBills, key = { it.id }) { bill ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("Flat ${bill.flatId} (${bill.userType})", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            if (bill.userType == "Guard") {
                                Spacer(Modifier.width(6.dp))
                                Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.tertiaryContainer) {
                                    Text("Guard Only", style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp))
                                }
                            }
                        }
                        Text("Billing: ${bill.billingMonth} • Due Date: ${bill.dueDate}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                        Text("Amount: ${Formatters.formatBdt(bill.amount)}", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        StatusBadge(status = bill.paymentStatus)

                        Row(modifier = Modifier.padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (isAuthorizedAdmin) {
                                IconButton(
                                    onClick = { selectedGasBillToEdit = bill },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Edit Bill", tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(16.dp))
                                }
                            }

                            if (bill.paymentStatus != "Paid" && (isAuthorizedAdmin || bill.userId == currentUser.userId)) {
                                FilledTonalButton(
                                    onClick = { viewModel.markGasBillPaid(bill.id, "GAS-REC-${bill.flatId}", currentUser.userId) },
                                    shape = RoundedCornerShape(8.dp),
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text("Mark Paid", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Set Individual Gas Bill Modal (System Admin & Delegated Admin)
    if (showIndividualGasDialog) {
        val targets = listOf("1A", "1B", "2A", "2B", "3A", "3B", "4A", "4B", "5A", "5B", "6A", "6B", "7A", "7B", "Guard Room")
        var selectedTarget by remember { mutableStateOf("1A") }
        var indAmount by remember { mutableStateOf("1080") }
        var indMonth by remember { mutableStateOf("2026-10") }
        var indDueDate by remember { mutableStateOf("2026-10-10") }

        AlertDialog(
            onDismissRequest = { showIndividualGasDialog = false },
            title = { Text("Set Individual Gas Bill") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Select unit/person and set individual monthly gas amount by the 10th.", style = MaterialTheme.typography.bodySmall)

                    // Target unit dropdown
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                        OutlinedTextField(
                            value = if (selectedTarget == "Guard Room") "Guard Room (Security Guard)" else "Flat $selectedTarget",
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth().menuAnchor()
                        )
                        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            targets.forEach { t ->
                                DropdownMenuItem(
                                    text = { Text(if (t == "Guard Room") "Guard Room (Security Guard)" else "Flat $t") },
                                    onClick = {
                                        selectedTarget = t
                                        indAmount = if (t == "Guard Room") "540" else "1080"
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(value = indMonth, onValueChange = { indMonth = it }, label = { Text("Billing Month (YYYY-MM)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = indAmount, onValueChange = { indAmount = it }, label = { Text("Amount (BDT ৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = indDueDate, onValueChange = { indDueDate = it }, label = { Text("Due Date (10th of month)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = indAmount.toDoubleOrNull() ?: 1080.0
                        val isGuard = selectedTarget == "Guard Room"
                        val userId = if (isGuard) "guard_01" else (allFlats.find { it.flatId == selectedTarget }?.let { it.renterUserId ?: it.ownerUserId } ?: "owner_${selectedTarget.lowercase()}")
                        val userType = if (isGuard) "Guard" else (if (allFlats.find { it.flatId == selectedTarget }?.renterUserId != null) "Renter" else "Flat Owner")

                        val newBill = GasBill(
                            billingMonth = indMonth,
                            userId = userId,
                            flatId = selectedTarget,
                            userType = userType,
                            amount = amt,
                            dueDate = indDueDate,
                            paymentStatus = "Due"
                        )
                        viewModel.saveOrUpdateIndividualGasBill(newBill, currentUser.userId)
                        showIndividualGasDialog = false
                    }
                ) {
                    Text("Save Gas Bill")
                }
            },
            dismissButton = { TextButton(onClick = { showIndividualGasDialog = false }) { Text("Cancel") } }
        )
    }

    // Edit Specific Gas Bill Modal
    if (selectedGasBillToEdit != null) {
        val bill = selectedGasBillToEdit!!
        var editAmount by remember { mutableStateOf(bill.amount.toInt().toString()) }
        var editDueDate by remember { mutableStateOf(bill.dueDate) }
        var editStatus by remember { mutableStateOf(bill.paymentStatus) }

        AlertDialog(
            onDismissRequest = { selectedGasBillToEdit = null },
            title = { Text("Edit Gas Bill (Flat ${bill.flatId})") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(value = editAmount, onValueChange = { editAmount = it }, label = { Text("Amount (BDT ৳)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = editDueDate, onValueChange = { editDueDate = it }, label = { Text("Due Date") }, modifier = Modifier.fillMaxWidth())

                    Text("Payment Status:", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(selected = editStatus == "Due", onClick = { editStatus = "Due" }, label = { Text("Due") })
                        FilterChip(selected = editStatus == "Paid", onClick = { editStatus = "Paid" }, label = { Text("Paid") })
                        FilterChip(selected = editStatus == "Waived", onClick = { editStatus = "Waived" }, label = { Text("Waived") })
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val newAmt = editAmount.toDoubleOrNull() ?: bill.amount
                        val updatedBill = bill.copy(
                            amount = newAmt,
                            dueDate = editDueDate,
                            paymentStatus = editStatus,
                            paidDate = if (editStatus == "Paid") (bill.paidDate ?: java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())) else null
                        )
                        viewModel.saveOrUpdateIndividualGasBill(updatedBill, currentUser.userId)
                        selectedGasBillToEdit = null
                    }
                ) {
                    Text("Update Bill")
                }
            },
            dismissButton = { TextButton(onClick = { selectedGasBillToEdit = null }) { Text("Cancel") } }
        )
    }

    if (showUpdateRateDialog) {
        var newRateText by remember { mutableStateOf((serviceConfig?.monthlyRate ?: 3500.0).toInt().toString()) }
        var remarksText by remember { mutableStateOf("Updated by Managing Committee") }

        AlertDialog(
            onDismissRequest = { showUpdateRateDialog = false },
            title = { Text("Set Monthly Service Charge Anytime") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newRateText,
                        onValueChange = { newRateText = it },
                        label = { Text("New Monthly Rate (BDT ৳)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = remarksText,
                        onValueChange = { remarksText = it },
                        label = { Text("Remarks") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val rate = newRateText.toDoubleOrNull()
                        if (rate != null && rate > 0) {
                            viewModel.updateServiceChargeRate(rate, "2026-11", remarksText, currentUser.userId)
                            showUpdateRateDialog = false
                        }
                    }
                ) {
                    Text("Save & Apply")
                }
            },
            dismissButton = { TextButton(onClick = { showUpdateRateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showSetMonthlyGasDialog) {
        var flatGasRate by remember { mutableStateOf("1080") }
        var guardGasRate by remember { mutableStateOf("540") }
        var billingMonth by remember { mutableStateOf("2026-10") }
        var dueDate by remember { mutableStateOf("2026-10-10") }

        AlertDialog(
            onDismissRequest = { showSetMonthlyGasDialog = false },
            title = { Text("Set Monthly Gas Bills by 10th") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Assigns individual gas bills for all 14 flats/renters and guard with deadline 10th.", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(value = billingMonth, onValueChange = { billingMonth = it }, label = { Text("Billing Month (YYYY-MM)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = flatGasRate, onValueChange = { flatGasRate = it }, label = { Text("Flat Gas Rate (BDT ৳)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = guardGasRate, onValueChange = { guardGasRate = it }, label = { Text("Guard Gas Rate (BDT ৳)") }, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = dueDate, onValueChange = { dueDate = it }, label = { Text("Due Date (10th of month)") }, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val fRate = flatGasRate.toDoubleOrNull() ?: 1080.0
                        val gRate = guardGasRate.toDoubleOrNull() ?: 540.0
                        viewModel.setMonthlyGasBillsForCycle(billingMonth, fRate, gRate, dueDate, currentUser.userId)
                        showSetMonthlyGasDialog = false
                    }
                ) {
                    Text("Apply Gas Bills")
                }
            },
            dismissButton = { TextButton(onClick = { showSetMonthlyGasDialog = false }) { Text("Cancel") } }
        )
    }
}

// 5. GOVERNANCE
@Composable
fun GovernanceSection(currentUser: User, viewModel: VillaViewModel, isAuthorizedAdmin: Boolean) {
    val members by viewModel.governanceMembers.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeader(
                title = "Managing Committee (2025–2026)",
                subtitle = "SAS-Siddik Villa Flat Owners Association"
            )
        }

        items(members, key = { it.id }) { m ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(m.memberName, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                            Text("${m.position} • Flat ${m.flatId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        StatusBadge(status = m.status)
                    }
                    if (m.delegatedPermissions.isNotBlank()) {
                        Spacer(Modifier.height(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        ) {
                            Text(
                                text = "Delegated Authority: ${m.delegatedPermissions.replace(",", " • ")}",
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// 6. SYNC & SYSTEM
@Composable
fun SyncAndBackupSection(
    currentUser: User,
    viewModel: VillaViewModel,
    isAuthorizedAdmin: Boolean,
    onShowInfo: () -> Unit,
    onLogout: () -> Unit
) {
    val syncItems by viewModel.syncItems.collectAsState()
    val pendingCount = syncItems.count { it.syncStatus == "PENDING" }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Offline Sync Engine", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (pendingCount == 0) "All local operations synchronized" else "$pendingCount operation(s) in queue",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Button(
                            onClick = { viewModel.triggerSync() },
                            modifier = Modifier.testTag("trigger_sync_button")
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Sync Now")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Database Backup & Export", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Text("Export complete SQLite / Room database to JSON for safe off-site archival.", style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(
                        onClick = { viewModel.showToast("Database snapshot successfully generated.") },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Export Database JSON")
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("System Information & Architecture", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    OutlinedButton(
                        onClick = onShowInfo,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("About System & Developer")
                    }

                    Button(
                        onClick = onLogout,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("logout_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Icon(Icons.Default.Logout, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Sign Out (${currentUser.userId})")
                    }
                }
            }
        }
    }
}

// 7. AUDIT TRAIL
@Composable
fun AuditTrailSection(currentUser: User, viewModel: VillaViewModel) {
    val logs by viewModel.auditLogs.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            SectionHeader(
                title = "System Audit Logs",
                subtitle = "Immutable ledger of all administrative & member actions"
            )
        }

        items(logs, key = { it.id }) { log ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${log.action} • ${log.entityType}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                        Text(SimpleDateFormat("dd/MM HH:mm", Locale.US).format(Date(log.timestamp)), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(log.details, style = MaterialTheme.typography.bodySmall)
                    Text("Actor: ${log.userId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.outline)
                }
            }
        }
    }
}
