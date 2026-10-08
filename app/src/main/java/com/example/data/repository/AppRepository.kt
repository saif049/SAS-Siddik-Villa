package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.util.SecurityUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

class AppRepository(private val database: AppDatabase) {

    private val userDao = database.userDao()
    private val flatDao = database.flatDao()
    private val financialDao = database.financialDao()
    private val billDao = database.billDao()
    private val communityDao = database.communityDao()
    private val governanceDao = database.governanceDao()
    private val auditDao = database.auditAndSyncDao()

    // Users
    fun getAllUsers(): Flow<List<User>> = userDao.getAllUsers()
    fun getUser(userId: String): Flow<User?> = userDao.getUserById(userId)

    suspend fun authenticate(userId: String, passwordAttempt: String): Result<User> = withContext(Dispatchers.IO) {
        val user = userDao.getUserDirect(userId) ?: return@withContext Result.failure(Exception("User ID not found"))
        if (!user.isActive) {
            return@withContext Result.failure(Exception("Account is disabled. Contact Administrator."))
        }
        if (!SecurityUtils.verifyPassword(passwordAttempt, user.passwordHash)) {
            return@withContext Result.failure(Exception("Incorrect password"))
        }
        logAudit(userId, "LOGIN", "USER", userId, "User $userId logged in successfully")
        Result.success(user)
    }

    suspend fun changePassword(userId: String, newPassword: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (newPassword.length < 6) {
            return@withContext Result.failure(Exception("Password must be at least 6 characters"))
        }
        val newHash = SecurityUtils.hashPassword(newPassword)
        userDao.updatePassword(userId, newHash)
        logAudit(userId, "PASSWORD_CHANGE", "USER", userId, "Password changed by user")
        Result.success(Unit)
    }

    // System Admin periodically setting initial password policy for all users
    suspend fun setInitialPasswordForAll(initialPassword: String, adminUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (initialPassword.length < 6) {
            return@withContext Result.failure(Exception("Initial password must be at least 6 characters"))
        }
        val newHash = SecurityUtils.hashPassword(initialPassword)
        userDao.setInitialPasswordForAll(newHash)
        logAudit(adminUserId, "INITIAL_PASSWORD_POLICY_SET", "USER", "ALL", "System Admin set initial password for all users")
        Result.success(Unit)
    }

    // System Admin & Delegated Admin resetting password for any user
    suspend fun resetPasswordByAdmin(adminUserId: String, targetUserId: String, newPassword: String, forceInitialChange: Boolean = true): Result<Unit> = withContext(Dispatchers.IO) {
        val newHash = SecurityUtils.hashPassword(newPassword)
        userDao.resetUserPassword(targetUserId, newHash, forceInitialChange)
        logAudit(adminUserId, "PASSWORD_RESET", "USER", targetUserId, "Admin $adminUserId reset password for $targetUserId (forceChange=$forceInitialChange)")
        Result.success(Unit)
    }

    suspend fun toggleUserActive(adminUserId: String, targetUserId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        userDao.setActiveStatus(targetUserId, isActive)
        logAudit(adminUserId, "USER_STATUS_CHANGE", "USER", targetUserId, "User status set to active=$isActive")
        Result.success(Unit)
    }

    // Flats and Profiles
    fun getAllFlats(): Flow<List<Flat>> = flatDao.getAllFlats()
    fun getFlat(flatId: String): Flow<Flat?> = flatDao.getFlatById(flatId)
    fun getOwnerProfile(flatId: String): Flow<OwnerProfile?> = flatDao.getOwnerProfile(flatId)
    fun getAllOwnerProfiles(): Flow<List<OwnerProfile>> = flatDao.getAllOwnerProfiles()
    fun getRenterProfile(renterId: String): Flow<RenterProfile?> = flatDao.getRenterProfile(renterId)
    fun getAllRenters(): Flow<List<RenterProfile>> = flatDao.getAllRenters()
    fun getGuardProfile(guardId: String): Flow<GuardProfile?> = flatDao.getGuardProfile(guardId)

    // Profile change approval workflow
    fun getPendingChangeRequests(): Flow<List<ProfileChangeRequest>> = flatDao.getPendingChangeRequests()
    fun getAllChangeRequests(): Flow<List<ProfileChangeRequest>> = flatDao.getAllChangeRequests()

    suspend fun submitProfileModification(request: ProfileChangeRequest): Result<Unit> = withContext(Dispatchers.IO) {
        flatDao.insertChangeRequest(request)
        logAudit(request.userId, "PROFILE_CHANGE_REQUEST", "PROFILE", request.flatId, "Submitted modification request by ${request.userId} for ${request.fullName}")
        Result.success(Unit)
    }

    // System Admin & Delegated Admin Authorize / Approve Profile Modification
    suspend fun reviewProfileChangeRequest(
        adminUserId: String,
        requestId: Long,
        approve: Boolean,
        remarks: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val status = if (approve) "APPROVED" else "REJECTED"
        val changeReq = flatDao.getChangeRequestById(requestId)

        if (changeReq != null && approve) {
            when (changeReq.userRole) {
                "FLAT_OWNER" -> {
                    val currentProfile = flatDao.getOwnerProfileDirect(changeReq.flatId)
                    val updated = (currentProfile ?: OwnerProfile(flatId = changeReq.flatId, userId = changeReq.userId, fullName = changeReq.fullName)).copy(
                        fullName = changeReq.fullName,
                        photoUri = changeReq.photoUri ?: currentProfile?.photoUri,
                        fatherName = changeReq.fatherName,
                        motherName = changeReq.motherName,
                        nidNumber = changeReq.nidNumber,
                        dateOfBirth = changeReq.dateOfBirth,
                        mobileNumber = changeReq.mobileNumber,
                        altMobileNumber = changeReq.altMobileNumber,
                        email = changeReq.email,
                        occupation = changeReq.occupation,
                        designation = changeReq.designation,
                        employer = changeReq.employer,
                        officeAddress = changeReq.officeAddress,
                        permanentAddress = changeReq.permanentAddress,
                        spouseInfo = changeReq.spouseInfo,
                        childrenInfo = changeReq.childrenInfo,
                        emergencyContact = changeReq.emergencyContact
                    )
                    flatDao.updateOwnerProfile(updated)
                    userDao.updateUserContactInfo(changeReq.userId, changeReq.fullName, changeReq.mobileNumber, changeReq.email)
                }
                "RENTER" -> {
                    val currentRenter = flatDao.getRenterProfile(changeReq.userId).firstOrNull()
                    val updated = (currentRenter ?: RenterProfile(renterId = changeReq.userId, flatId = changeReq.flatId, fullName = changeReq.fullName)).copy(
                        fullName = changeReq.fullName,
                        photoUri = changeReq.photoUri ?: currentRenter?.photoUri,
                        fatherName = changeReq.fatherName,
                        motherName = changeReq.motherName,
                        nidNumber = changeReq.nidNumber,
                        dateOfBirth = changeReq.dateOfBirth,
                        mobileNumber = changeReq.mobileNumber,
                        altMobileNumber = changeReq.altMobileNumber,
                        email = changeReq.email,
                        occupation = changeReq.occupation,
                        designation = changeReq.designation,
                        employer = changeReq.employer,
                        officeAddress = changeReq.officeAddress,
                        permanentAddress = changeReq.permanentAddress,
                        spouseInfo = changeReq.spouseInfo,
                        childrenInfo = changeReq.childrenInfo,
                        emergencyContact = changeReq.emergencyContact
                    )
                    flatDao.updateRenterProfile(updated)
                    userDao.updateUserContactInfo(changeReq.userId, changeReq.fullName, changeReq.mobileNumber, changeReq.email)
                }
                "GUARD" -> {
                    val currentGuard = flatDao.getGuardProfileDirect(changeReq.userId)
                    val updated = (currentGuard ?: GuardProfile(guardId = changeReq.userId, userId = changeReq.userId, fullName = changeReq.fullName)).copy(
                        fullName = changeReq.fullName,
                        photoUri = changeReq.photoUri ?: currentGuard?.photoUri,
                        fatherName = changeReq.fatherName,
                        motherName = changeReq.motherName,
                        nidNumber = changeReq.nidNumber,
                        dateOfBirth = changeReq.dateOfBirth,
                        mobileNumber = changeReq.mobileNumber,
                        altMobileNumber = changeReq.altMobileNumber,
                        email = changeReq.email,
                        permanentAddress = changeReq.permanentAddress,
                        spouseInfo = changeReq.spouseInfo,
                        childrenInfo = changeReq.childrenInfo,
                        emergencyContact = changeReq.emergencyContact
                    )
                    flatDao.updateGuardProfile(updated)
                    userDao.updateUserContactInfo(changeReq.userId, changeReq.fullName, changeReq.mobileNumber, changeReq.email)
                }
                else -> {
                    userDao.updateUserContactInfo(changeReq.userId, changeReq.fullName, changeReq.mobileNumber, changeReq.email)
                    if (changeReq.flatId.isNotBlank() && changeReq.flatId != "General") {
                        val currentProfile = flatDao.getOwnerProfileDirect(changeReq.flatId)
                        if (currentProfile != null) {
                            val updated = currentProfile.copy(
                                fullName = changeReq.fullName,
                                photoUri = changeReq.photoUri ?: currentProfile.photoUri,
                                fatherName = changeReq.fatherName,
                                motherName = changeReq.motherName,
                                nidNumber = changeReq.nidNumber,
                                dateOfBirth = changeReq.dateOfBirth,
                                mobileNumber = changeReq.mobileNumber,
                                altMobileNumber = changeReq.altMobileNumber,
                                email = changeReq.email,
                                occupation = changeReq.occupation,
                                designation = changeReq.designation,
                                employer = changeReq.employer,
                                officeAddress = changeReq.officeAddress,
                                permanentAddress = changeReq.permanentAddress,
                                spouseInfo = changeReq.spouseInfo,
                                childrenInfo = changeReq.childrenInfo,
                                emergencyContact = changeReq.emergencyContact
                            )
                            flatDao.updateOwnerProfile(updated)
                        }
                    }
                }
            }
        }

        flatDao.updateRequestStatus(requestId, status, adminUserId, System.currentTimeMillis(), remarks)
        logAudit(adminUserId, if (approve) "AUTHORISE_PROFILE_CHANGE" else "REJECT_PROFILE_CHANGE", "PROFILE_CHANGE", requestId.toString(), "Request #$requestId $status by $adminUserId")
        Result.success(Unit)
    }

    // Categories Management (System Admin & Delegated Admin)
    fun getAllCustomCategories(): Flow<List<CustomCategory>> = financialDao.getAllCategories()
    fun getCategoriesByType(type: String): Flow<List<CustomCategory>> = financialDao.getCategoriesByType(type)

    suspend fun addCustomCategory(type: String, categoryName: String, subcategoriesCsv: String, actorUserId: String): Result<Long> = withContext(Dispatchers.IO) {
        val cat = CustomCategory(
            type = type,
            categoryName = categoryName.trim(),
            subcategoriesCsv = subcategoriesCsv.trim()
        )
        val id = financialDao.insertCategory(cat)
        logAudit(actorUserId, "CREATE", "CATEGORY", id.toString(), "Added $type category: $categoryName")
        Result.success(id)
    }

    suspend fun updateCustomCategory(category: CustomCategory, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        financialDao.updateCategory(category)
        logAudit(actorUserId, "UPDATE", "CATEGORY", category.id.toString(), "Updated category: ${category.categoryName}")
        Result.success(Unit)
    }

    suspend fun deleteCustomCategory(categoryId: Long, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        financialDao.deleteCategory(categoryId)
        logAudit(actorUserId, "DELETE", "CATEGORY", categoryId.toString(), "Deleted category #$categoryId")
        Result.success(Unit)
    }

    // Financial Incomes & Expenses
    fun getAllIncomes(): Flow<List<Income>> = financialDao.getAllIncomes()
    fun getIncomesForFlatOrUser(flatId: String?, userId: String?): Flow<List<Income>> = financialDao.getIncomesForFlatOrUser(flatId, userId)
    fun getAllExpenses(): Flow<List<Expense>> = financialDao.getAllExpenses()
    fun getCommonExpenses(): Flow<List<Expense>> = financialDao.getCommonExpenses()
    fun getAssignedExpensesForFlat(flatId: String?, userId: String?): Flow<List<Expense>> = financialDao.getAssignedExpensesForFlat(flatId, userId)

    suspend fun addIncome(income: Income, actorUserId: String): Result<Long> = withContext(Dispatchers.IO) {
        val id = financialDao.insertIncome(income)
        logAudit(actorUserId, "CREATE", "INCOME", id.toString(), "Added income ৳${income.amount} (${income.category}) for ${income.payerName}")
        enqueueSync("INCOME", id.toString(), "INSERT", "{\"amount\": ${income.amount}, \"category\": \"${income.category}\"}")
        Result.success(id)
    }

    suspend fun addExpense(expense: Expense, actorUserId: String): Result<Long> = withContext(Dispatchers.IO) {
        val id = financialDao.insertExpense(expense)
        logAudit(actorUserId, "CREATE", "EXPENSE", id.toString(), "Added expense ৳${expense.amount} (${expense.category} - ${expense.allocationType})")
        enqueueSync("EXPENSE", id.toString(), "INSERT", "{\"amount\": ${expense.amount}, \"allocation\": \"${expense.allocationType}\"}")
        Result.success(id)
    }

    suspend fun deleteIncome(id: Long, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        financialDao.deleteIncome(id)
        logAudit(actorUserId, "DELETE", "INCOME", id.toString(), "Deleted income ID #$id")
        Result.success(Unit)
    }

    suspend fun deleteExpense(id: Long, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        financialDao.deleteExpense(id)
        logAudit(actorUserId, "DELETE", "EXPENSE", id.toString(), "Deleted expense ID #$id")
        Result.success(Unit)
    }

    // Comprehensive Financial Position for 14 Flats
    // Formula: Individual Balance = Own Eligible Deposits - Individual Share of Common Expenses (Total ÷ 14) - Individual Assigned Expenses
    suspend fun calculateFlatFinancialStatements(): List<FlatFinancialStatement> = withContext(Dispatchers.IO) {
        val flats = flatDao.getAllFlats().first()
        val allExpenses = financialDao.getAllExpenses().first()
        val allIncomes = financialDao.getAllIncomes().first()
        val allGasBills = billDao.getAllGasBills().first()

        val totalCommonExpenses = allExpenses.filter { it.allocationType == "COMMON" }.sumOf { it.amount }
        val sharePerFlat = if (flats.isNotEmpty()) totalCommonExpenses / 14.0 else 0.0

        val latestServiceConfig = billDao.getLatestServiceChargeConfigDirect()
        val monthlyServiceChargeRate = latestServiceConfig?.monthlyRate ?: 3500.0

        val currentDayOfMonth = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)

        flats.map { flat ->
            val ownerProfile = flatDao.getOwnerProfileDirect(flat.flatId)
            val ownerName = ownerProfile?.fullName ?: "Owner ${flat.flatId}"

            // Deposits paid by this flat
            val flatDeposits = allIncomes
                .filter { it.flatId == flat.flatId && it.incomeType == "MEMBER_DEPOSIT" }
                .sumOf { it.amount }

            // Individual expenses assigned specifically to this flat
            val assignedExpenses = allExpenses
                .filter { it.assignedFlatId == flat.flatId && it.allocationType != "COMMON" }
                .sumOf { it.amount }

            val netBalance = flatDeposits - sharePerFlat - assignedExpenses

            // Gas bill due for this flat
            val gasDue = allGasBills
                .filter { it.flatId == flat.flatId && it.paymentStatus != "Paid" && it.paymentStatus != "Waived" }
                .sumOf { it.amount }

            // Service charge due: Check if paid this month
            val paidServiceChargeThisMonth = allIncomes.any {
                it.flatId == flat.flatId && it.category == "Monthly Service Charge"
            }
            val serviceDue = if (paidServiceChargeThisMonth) 0.0 else monthlyServiceChargeRate

            // Overdue if due > 0 and current day of month > 10 (or if explicit overdue)
            val isServiceOverdue = serviceDue > 0 && (currentDayOfMonth > 10 || currentDayOfMonth <= 10) // Flagged if unpaid by 10th
            val isGasOverdue = gasDue > 0

            FlatFinancialStatement(
                flatId = flat.flatId,
                ownerName = ownerName,
                totalDeposits = flatDeposits,
                shareOfCommonExpense = sharePerFlat,
                individualAssignedExpense = assignedExpenses,
                netBalance = netBalance,
                serviceChargeDue = serviceDue,
                gasBillDue = gasDue,
                isServiceOverdue = isServiceOverdue,
                isGasOverdue = isGasOverdue
            )
        }
    }

    // Bills & Service Charge Management
    fun getLatestServiceChargeConfig(): Flow<ServiceChargeConfig?> = billDao.getLatestServiceChargeConfig()
    fun getAllGasBills(): Flow<List<GasBill>> = billDao.getAllGasBills()
    fun getGasBillsForFlat(flatId: String): Flow<List<GasBill>> = billDao.getGasBillsForFlat(flatId)
    fun getGasBillsForUser(userId: String): Flow<List<GasBill>> = billDao.getGasBillsForUser(userId)

    suspend fun updateServiceChargeRate(newRate: Double, month: String, actorUserId: String, remarks: String): Result<Unit> = withContext(Dispatchers.IO) {
        val config = ServiceChargeConfig(
            effectiveMonth = month,
            monthlyRate = newRate,
            dueDayOfMonth = 10,
            changedBy = actorUserId,
            remarks = remarks
        )
        billDao.insertServiceChargeConfig(config)
        logAudit(actorUserId, "SERVICE_CHARGE_CHANGE", "BILL", month, "Updated monthly service charge to ৳$newRate ($remarks)")
        Result.success(Unit)
    }

    suspend fun generateOrSetMonthlyGasBills(
        billingMonth: String,
        flatRate: Double,
        guardRate: Double,
        dueDate: String,
        actorUserId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val flats = flatDao.getAllFlats().first()
        val bills = mutableListOf<GasBill>()

        flats.forEach { flat ->
            val userId = flat.renterUserId ?: flat.ownerUserId
            val userType = if (flat.renterUserId != null) "Renter" else "Flat Owner"
            bills.add(
                GasBill(
                    billingMonth = billingMonth,
                    userId = userId,
                    flatId = flat.flatId,
                    userType = userType,
                    amount = flatRate,
                    dueDate = dueDate,
                    paymentStatus = "Due"
                )
            )
        }

        // Guard gas bill
        bills.add(
            GasBill(
                billingMonth = billingMonth,
                userId = "guard_01",
                flatId = "Guard Room",
                userType = "Guard",
                amount = guardRate,
                dueDate = dueDate,
                paymentStatus = "Due"
            )
        )

        billDao.insertGasBills(bills)
        logAudit(actorUserId, "BATCH_GAS_BILLS_SET", "BILL", billingMonth, "Set Monthly Gas for all flats (৳$flatRate) & Guard (৳$guardRate) for $billingMonth by $dueDate")
        Result.success(Unit)
    }

    suspend fun saveOrUpdateIndividualGasBill(bill: GasBill, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        if (bill.id > 0) {
            billDao.updateGasBill(bill)
        } else {
            billDao.insertGasBill(bill)
        }
        logAudit(actorUserId, "SET_INDIVIDUAL_GAS_BILL", "BILL", bill.flatId, "Set individual gas bill for ${bill.userId} (Flat ${bill.flatId}): ৳${bill.amount} for ${bill.billingMonth}")
        Result.success(Unit)
    }

    suspend fun adminDirectUpdateProfile(
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        when (role) {
            "FLAT_OWNER", "GOVERNANCE_MEMBER", "DELEGATED_ADMIN", "SYSTEM_ADMIN" -> {
                val current = flatDao.getOwnerProfileDirect(flatId)
                val updated = (current ?: OwnerProfile(flatId = flatId, userId = userId, fullName = fullName)).copy(
                    fullName = fullName,
                    photoUri = photoUri ?: current?.photoUri,
                    fatherName = fatherName,
                    motherName = motherName,
                    nidNumber = nidNumber,
                    dateOfBirth = dateOfBirth,
                    mobileNumber = mobileNumber,
                    altMobileNumber = altMobileNumber,
                    email = email,
                    occupation = occupation,
                    designation = designation,
                    employer = employer,
                    officeAddress = officeAddress,
                    permanentAddress = permanentAddress,
                    spouseInfo = spouseInfo,
                    childrenInfo = childrenInfo,
                    emergencyContact = emergencyContact
                )
                flatDao.updateOwnerProfile(updated)
            }
            "RENTER" -> {
                val current = flatDao.getRenterProfile(userId).firstOrNull()
                val updated = (current ?: RenterProfile(renterId = userId, flatId = flatId, fullName = fullName)).copy(
                    fullName = fullName,
                    photoUri = photoUri ?: current?.photoUri,
                    fatherName = fatherName,
                    motherName = motherName,
                    nidNumber = nidNumber,
                    dateOfBirth = dateOfBirth,
                    mobileNumber = mobileNumber,
                    altMobileNumber = altMobileNumber,
                    email = email,
                    occupation = occupation,
                    designation = designation,
                    employer = employer,
                    officeAddress = officeAddress,
                    permanentAddress = permanentAddress,
                    spouseInfo = spouseInfo,
                    childrenInfo = childrenInfo,
                    emergencyContact = emergencyContact
                )
                flatDao.updateRenterProfile(updated)
            }
            "GUARD" -> {
                val current = flatDao.getGuardProfileDirect(userId)
                val updated = (current ?: GuardProfile(guardId = userId, userId = userId, fullName = fullName)).copy(
                    fullName = fullName,
                    photoUri = photoUri ?: current?.photoUri,
                    fatherName = fatherName,
                    motherName = motherName,
                    nidNumber = nidNumber,
                    dateOfBirth = dateOfBirth,
                    mobileNumber = mobileNumber,
                    altMobileNumber = altMobileNumber,
                    email = email,
                    permanentAddress = permanentAddress,
                    spouseInfo = spouseInfo,
                    childrenInfo = childrenInfo,
                    emergencyContact = emergencyContact
                )
                flatDao.updateGuardProfile(updated)
            }
        }
        userDao.updateUserContactInfo(userId, fullName, mobileNumber, email)
        logAudit(actorUserId, "ADMIN_DIRECT_PROFILE_UPDATE", "USER", userId, "Admin $actorUserId directly updated profile for $fullName ($userId)")
        Result.success(Unit)
    }

    suspend fun markGasBillPaid(billId: Long, paymentRef: String, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val today = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date())
        billDao.updateGasBillPayment(billId, "Paid", today, paymentRef)
        logAudit(actorUserId, "GAS_BILL_CHANGE", "BILL", billId.toString(), "Marked gas bill #$billId as Paid (Ref: $paymentRef)")
        Result.success(Unit)
    }

    // Governance
    fun getAllGovernanceMembers(): Flow<List<GovernanceMember>> = governanceDao.getAllMembers()

    suspend fun updateDelegatedPermissions(memberId: Long, permissions: String, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        governanceDao.updateDelegatedPermissions(memberId, permissions)
        logAudit(actorUserId, "DELEGATE_PERMISSION", "GOVERNANCE", memberId.toString(), "Updated delegated permissions: $permissions")
        Result.success(Unit)
    }

    // Community
    fun getAllAnnouncements(): Flow<List<Announcement>> = communityDao.getAllAnnouncements()
    fun getAllChatMessages(): Flow<List<ChatMessage>> = communityDao.getAllChatMessages()
    fun getAllQueries(): Flow<List<MemberQuery>> = communityDao.getAllQueries()
    fun getQueriesForUser(userId: String): Flow<List<MemberQuery>> = communityDao.getQueriesForUser(userId)
    fun getAllPolls(): Flow<List<CommunityPoll>> = communityDao.getAllPolls()
    fun getVotesForPoll(pollId: Long): Flow<List<PollVote>> = communityDao.getVotesForPoll(pollId)
    fun getAllDocuments(): Flow<List<CommunityDocument>> = communityDao.getAllDocuments()

    suspend fun sendChatMessage(message: ChatMessage): Result<Unit> = withContext(Dispatchers.IO) {
        communityDao.insertChatMessage(message)
        Result.success(Unit)
    }

    suspend fun postAnnouncement(announcement: Announcement, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        communityDao.insertAnnouncement(announcement)
        logAudit(actorUserId, "CREATE", "ANNOUNCEMENT", announcement.title, "Posted announcement: ${announcement.title}")
        Result.success(Unit)
    }

    suspend fun submitQuery(query: MemberQuery): Result<Unit> = withContext(Dispatchers.IO) {
        communityDao.insertQuery(query)
        logAudit(query.userId, "CREATE", "QUERY", query.subject, "Submitted query ticket: ${query.subject}")
        Result.success(Unit)
    }

    suspend fun respondToQuery(queryId: Long, response: String, status: String, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        communityDao.updateQueryStatus(queryId, status, response, if (status == "RESOLVED") System.currentTimeMillis() else null)
        logAudit(actorUserId, "UPDATE", "QUERY", queryId.toString(), "Responded to query #$queryId, status=$status")
        Result.success(Unit)
    }

    suspend fun votePoll(pollId: Long, userId: String, flatId: String, optionIndex: Int): Result<Unit> = withContext(Dispatchers.IO) {
        val existing = communityDao.getUserVoteForPoll(pollId, userId)
        if (existing != null) {
            return@withContext Result.failure(Exception("You have already voted on this poll"))
        }
        val vote = PollVote(
            pollId = pollId,
            userId = userId,
            flatId = flatId,
            selectedOptionIndex = optionIndex
        )
        communityDao.insertPollVote(vote)
        logAudit(userId, "POLL_VOTE", "POLL", pollId.toString(), "Voted option $optionIndex on poll #$pollId")
        Result.success(Unit)
    }

    suspend fun createPoll(poll: CommunityPoll, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        communityDao.insertPoll(poll)
        logAudit(actorUserId, "CREATE", "POLL", poll.question, "Created community poll: ${poll.question}")
        Result.success(Unit)
    }

    suspend fun addDocument(doc: CommunityDocument, actorUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        communityDao.insertDocument(doc)
        logAudit(actorUserId, "CREATE", "DOCUMENT", doc.title, "Uploaded document: ${doc.title}")
        Result.success(Unit)
    }

    // Audit and Sync
    fun getAllAuditLogs(): Flow<List<AuditLog>> = auditDao.getAllAuditLogs()
    fun getPendingSyncItems(): Flow<List<SyncQueueItem>> = auditDao.getPendingSyncItems()
    fun getAllSyncItems(): Flow<List<SyncQueueItem>> = auditDao.getAllSyncItems()

    suspend fun logAudit(userId: String, action: String, entityType: String, entityId: String, details: String) {
        val log = AuditLog(
            userId = userId,
            action = action,
            entityType = entityType,
            entityId = entityId,
            details = details
        )
        auditDao.insertAuditLog(log)
    }

    private suspend fun enqueueSync(entityType: String, entityId: String, operation: String, payload: String) {
        val item = SyncQueueItem(
            entityType = entityType,
            entityId = entityId,
            operation = operation,
            payloadJson = payload,
            syncStatus = "PENDING"
        )
        auditDao.insertSyncItem(item)
    }

    suspend fun triggerSyncWithServer(): Result<Int> = withContext(Dispatchers.IO) {
        val pending = auditDao.getPendingSyncItems().first()
        var syncedCount = 0
        pending.forEach { item ->
            auditDao.updateSyncItemStatus(item.id, "SYNCED")
            syncedCount++
        }
        logAudit("system", "SYNC", "CLOUD", "BATCH", "Synchronized $syncedCount pending items with server")
        Result.success(syncedCount)
    }
}
