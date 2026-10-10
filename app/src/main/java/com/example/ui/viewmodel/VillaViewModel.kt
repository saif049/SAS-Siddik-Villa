package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.data.util.SecurityUtils
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class VillaViewModel(application: Application) : AndroidViewModel(application) {

    val repository = AppRepository(AppDatabase.getInstance(application))

    // UI state
    private val _selectedTab = MutableStateFlow(0)
    val selectedTab: StateFlow<Int> = _selectedTab.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    // Financial Data
    val allIncomes: StateFlow<List<Income>> = repository.getAllIncomes()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<Expense>> = repository.getAllExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _flatStatements = MutableStateFlow<List<FlatFinancialStatement>>(emptyList())
    val flatStatements: StateFlow<List<FlatFinancialStatement>> = _flatStatements.asStateFlow()

    val serviceChargeConfig: StateFlow<ServiceChargeConfig?> = repository.getLatestServiceChargeConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val gasBills: StateFlow<List<GasBill>> = repository.getAllGasBills()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Dynamic Categories in Database
    val customCategories: StateFlow<List<CustomCategory>> = repository.getAllCustomCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Flats and Profiles
    val allFlats: StateFlow<List<Flat>> = repository.getAllFlats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allOwnerProfiles: StateFlow<List<OwnerProfile>> = repository.getAllOwnerProfiles()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRenters: StateFlow<List<RenterProfile>> = repository.getAllRenters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val guardProfile: StateFlow<GuardProfile?> = repository.getGuardProfile("guard_01")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val pendingChangeRequests: StateFlow<List<ProfileChangeRequest>> = repository.getPendingChangeRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allChangeRequests: StateFlow<List<ProfileChangeRequest>> = repository.getAllChangeRequests()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allUsers: StateFlow<List<User>> = repository.getAllUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Community Data
    val announcements: StateFlow<List<Announcement>> = repository.getAllAnnouncements()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val chatMessages: StateFlow<List<ChatMessage>> = repository.getAllChatMessages()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val queries: StateFlow<List<MemberQuery>> = repository.getAllQueries()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val polls: StateFlow<List<CommunityPoll>> = repository.getAllPolls()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val documents: StateFlow<List<CommunityDocument>> = repository.getAllDocuments()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Governance
    val governanceMembers: StateFlow<List<GovernanceMember>> = repository.getAllGovernanceMembers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Audit and Sync
    val auditLogs: StateFlow<List<AuditLog>> = repository.getAllAuditLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val syncItems: StateFlow<List<SyncQueueItem>> = repository.getAllSyncItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        refreshFinancialStatements()
    }

    fun setTab(index: Int) {
        _selectedTab.value = index
    }

    fun showToast(msg: String) {
        _toastMessage.value = msg
    }

    fun clearToast() {
        _toastMessage.value = null
    }

    fun refreshFinancialStatements() {
        viewModelScope.launch {
            _flatStatements.value = repository.calculateFlatFinancialStatements()
        }
    }

    // Financial Actions
    fun addIncome(
        amount: Double,
        incomeType: String,
        payerUserId: String?,
        payerName: String,
        flatId: String?,
        category: String,
        subcategory: String,
        paymentMethod: String,
        referenceNo: String,
        remarks: String,
        actorUserId: String
    ) {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val income = Income(
                date = today,
                amount = amount,
                incomeType = incomeType,
                payerUserId = payerUserId,
                payerName = payerName,
                flatId = flatId,
                category = category,
                subcategory = subcategory,
                paymentMethod = paymentMethod,
                referenceNo = referenceNo,
                remarks = remarks,
                createdBy = actorUserId
            )
            repository.addIncome(income, actorUserId)
            refreshFinancialStatements()
            showToast("Income entry ৳$amount recorded successfully")
        }
    }

    fun addExpense(
        amount: Double,
        category: String,
        subcategory: String,
        expenseType: String,
        allocationType: String,
        assignedFlatId: String?,
        vendorPayee: String,
        paymentMethod: String,
        receiptRef: String,
        remarks: String,
        actorUserId: String
    ) {
        viewModelScope.launch {
            val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
            val expense = Expense(
                date = today,
                amount = amount,
                category = category,
                subcategory = subcategory,
                expenseType = expenseType,
                allocationType = allocationType,
                assignedFlatId = assignedFlatId,
                vendorPayee = vendorPayee,
                paymentMethod = paymentMethod,
                receiptRef = receiptRef,
                remarks = remarks,
                createdBy = actorUserId
            )
            repository.addExpense(expense, actorUserId)
            refreshFinancialStatements()
            showToast("Expense entry ৳$amount recorded ($allocationType)")
        }
    }

    fun deleteIncome(id: Long, actorUserId: String) {
        viewModelScope.launch {
            repository.deleteIncome(id, actorUserId)
            refreshFinancialStatements()
            showToast("Income deleted")
        }
    }

    fun deleteExpense(id: Long, actorUserId: String) {
        viewModelScope.launch {
            repository.deleteExpense(id, actorUserId)
            refreshFinancialStatements()
            showToast("Expense deleted")
        }
    }

    // Category Management (System Admin & Delegated Admin)
    fun addCategory(type: String, categoryName: String, subcategoriesCsv: String, actorUserId: String) {
        viewModelScope.launch {
            repository.addCustomCategory(type, categoryName, subcategoriesCsv, actorUserId)
            showToast("Added $type category: $categoryName")
        }
    }

    fun updateCategory(category: CustomCategory, actorUserId: String) {
        viewModelScope.launch {
            repository.updateCustomCategory(category, actorUserId)
            showToast("Updated category: ${category.categoryName}")
        }
    }

    fun deleteCategory(categoryId: Long, actorUserId: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(categoryId, actorUserId)
            showToast("Category deleted")
        }
    }

    // Bills & Utilities
    fun updateServiceChargeRate(newRate: Double, month: String, remarks: String, actorUserId: String) {
        viewModelScope.launch {
            repository.updateServiceChargeRate(newRate, month, actorUserId, remarks)
            refreshFinancialStatements()
            showToast("Service charge updated to ৳$newRate for $month")
        }
    }

    fun setMonthlyGasBillsForCycle(billingMonth: String, flatRate: Double, guardRate: Double, dueDate: String, actorUserId: String) {
        viewModelScope.launch {
            repository.generateOrSetMonthlyGasBills(billingMonth, flatRate, guardRate, dueDate, actorUserId)
            refreshFinancialStatements()
            showToast("Monthly Gas bills set for all units (Due: $dueDate)")
        }
    }

    fun saveOrUpdateIndividualGasBill(bill: GasBill, actorUserId: String) {
        viewModelScope.launch {
            repository.saveOrUpdateIndividualGasBill(bill, actorUserId)
            refreshFinancialStatements()
            showToast("Individual Gas Bill updated for Flat ${bill.flatId} (৳${bill.amount})")
        }
    }

    fun adminDirectUpdateProfile(
        role: String,
        flatId: String,
        userId: String,
        fullName: String,
        photoUri: String?,
        fatherName: String,
        motherName: String,
        nidNumber: String,
        dateOfBirth: String,
        mobileNumber: String,
        altMobileNumber: String,
        email: String,
        occupation: String,
        designation: String,
        employer: String,
        officeAddress: String,
        permanentAddress: String,
        spouseInfo: String,
        childrenInfo: String,
        emergencyContact: String,
        actorUserId: String
    ) {
        viewModelScope.launch {
            repository.adminDirectUpdateProfile(
                role, flatId, userId, fullName, photoUri,
                fatherName, motherName, nidNumber, dateOfBirth,
                mobileNumber, altMobileNumber, email,
                occupation, designation, employer, officeAddress,
                permanentAddress, spouseInfo, childrenInfo, emergencyContact,
                actorUserId
            )
            showToast("Profile for $fullName ($userId) updated directly by Admin")
        }
    }

    fun markGasBillPaid(billId: Long, paymentRef: String, actorUserId: String) {
        viewModelScope.launch {
            repository.markGasBillPaid(billId, paymentRef, actorUserId)
            refreshFinancialStatements()
            showToast("Gas bill marked as Paid")
        }
    }

    // Community
    fun sendChatMessage(sender: User, messageText: String) {
        if (messageText.isBlank()) return
        viewModelScope.launch {
            val msg = ChatMessage(
                senderUserId = sender.userId,
                senderName = sender.fullName,
                senderRole = sender.role.name.replace("_", " "),
                flatId = sender.flatId,
                message = messageText.trim()
            )
            repository.sendChatMessage(msg)
        }
    }

    fun postAnnouncement(title: String, content: String, category: String, isPinned: Boolean, actorUserId: String) {
        viewModelScope.launch {
            val announcement = Announcement(
                title = title,
                content = content,
                category = category,
                isPinned = isPinned,
                publishedBy = actorUserId
            )
            repository.postAnnouncement(announcement, actorUserId)
            showToast("Announcement published")
        }
    }

    fun submitQuery(user: User, subject: String, description: String, category: String) {
        viewModelScope.launch {
            val q = MemberQuery(
                userId = user.userId,
                flatId = user.flatId ?: "General",
                submitterName = user.fullName,
                subject = subject,
                description = description,
                category = category
            )
            repository.submitQuery(q)
            showToast("Query ticket submitted")
        }
    }

    fun respondToQuery(queryId: Long, response: String, status: String, actorUserId: String) {
        viewModelScope.launch {
            repository.respondToQuery(queryId, response, status, actorUserId)
            showToast("Query response updated ($status)")
        }
    }

    fun votePoll(pollId: Long, user: User, optionIndex: Int) {
        viewModelScope.launch {
            val result = repository.votePoll(pollId, user.userId, user.flatId ?: "", optionIndex)
            if (result.isSuccess) {
                showToast("Vote recorded successfully")
            } else {
                showToast(result.exceptionOrNull()?.message ?: "Vote failed")
            }
        }
    }

    fun createPoll(question: String, description: String, optionsList: String, actorUserId: String) {
        viewModelScope.launch {
            val p = CommunityPoll(
                question = question,
                description = description,
                optionsList = optionsList,
                createdBy = actorUserId
            )
            repository.createPoll(p, actorUserId)
            showToast("Poll created successfully")
        }
    }

    fun uploadDocument(title: String, type: String, description: String, ref: String, roles: String, actorUserId: String) {
        viewModelScope.launch {
            val doc = CommunityDocument(
                title = title,
                documentType = type,
                description = description,
                fileReference = ref,
                allowedRoles = roles,
                uploadedBy = actorUserId
            )
            repository.addDocument(doc, actorUserId)
            showToast("Document added to repository")
        }
    }

    // Profiles and Modifications
    fun submitProfileModification(request: ProfileChangeRequest) {
        viewModelScope.launch {
            repository.submitProfileModification(request)
            showToast("Profile modification request submitted for administrative authorisation")
        }
    }

    fun reviewProfileChange(requestId: Long, approve: Boolean, remarks: String?, actorUserId: String) {
        viewModelScope.launch {
            repository.reviewProfileChangeRequest(actorUserId, requestId, approve, remarks)
            showToast(if (approve) "Profile modification authorised & applied!" else "Profile modification request rejected")
        }
    }

    // Governance
    fun updateDelegatedPermissions(memberId: Long, permissions: String, actorUserId: String) {
        viewModelScope.launch {
            repository.updateDelegatedPermissions(memberId, permissions, actorUserId)
            showToast("Delegated permissions updated")
        }
    }

    // Admin Account & Password Mgmt
    fun setInitialPasswordForAll(initialPassword: String, actorUserId: String) {
        viewModelScope.launch {
            val result = repository.setInitialPasswordForAll(initialPassword, actorUserId)
            if (result.isSuccess) {
                showToast("Initial password policy applied for all users ($initialPassword)")
            } else {
                showToast(result.exceptionOrNull()?.message ?: "Error setting initial password")
            }
        }
    }

    fun resetUserPassword(targetUserId: String, newPass: String, actorUserId: String, forceInitialChange: Boolean = true) {
        viewModelScope.launch {
            repository.resetPasswordByAdmin(actorUserId, targetUserId, newPass, forceInitialChange)
            showToast("Password reset for $targetUserId (Temporary pass: $newPass)")
        }
    }

    fun changeOwnPassword(
        userId: String,
        currentAttempt: String,
        currentHash: String,
        newPassword: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        if (!SecurityUtils.verifyPassword(currentAttempt, currentHash)) {
            val err = "Current password does not match your existing password."
            showToast(err)
            onResult(false, err)
            return
        }
        if (newPassword.length < 6) {
            val err = "New password must be at least 6 characters."
            showToast(err)
            onResult(false, err)
            return
        }
        viewModelScope.launch {
            val result = repository.changePassword(userId, newPassword)
            if (result.isSuccess) {
                showToast("Password updated successfully!")
                onResult(true, "Password updated successfully!")
            } else {
                val error = result.exceptionOrNull()?.message ?: "Failed to update password"
                showToast(error)
                onResult(false, error)
            }
        }
    }

    fun toggleUserActive(targetUserId: String, isActive: Boolean, actorUserId: String) {
        viewModelScope.launch {
            repository.toggleUserActive(actorUserId, targetUserId, isActive)
            showToast("Account status updated for $targetUserId")
        }
    }

    // Sync & Backup
    fun triggerSync() {
        viewModelScope.launch {
            val result = repository.triggerSyncWithServer()
            showToast("Synced ${result.getOrDefault(0)} items with server")
        }
    }
}
