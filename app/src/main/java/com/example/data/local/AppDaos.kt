package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    fun getUserById(userId: String): Flow<User?>

    @Query("SELECT * FROM users WHERE userId = :userId LIMIT 1")
    suspend fun getUserDirect(userId: String): User?

    @Query("SELECT * FROM users ORDER BY userId ASC")
    fun getAllUsers(): Flow<List<User>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUsers(users: List<User>)

    @Update
    suspend fun updateUser(user: User)

    @Query("UPDATE users SET passwordHash = :passwordHash, isInitialPassword = 0, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updatePassword(userId: String, passwordHash: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET passwordHash = :passwordHash, isInitialPassword = 1, updatedAt = :timestamp")
    suspend fun setInitialPasswordForAll(passwordHash: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET passwordHash = :passwordHash, isInitialPassword = :isInitial, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun resetUserPassword(userId: String, passwordHash: String, isInitial: Boolean = true, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET fullName = :fullName, mobile = :mobile, email = :email, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun updateUserContactInfo(userId: String, fullName: String, mobile: String, email: String, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE users SET isActive = :isActive, updatedAt = :timestamp WHERE userId = :userId")
    suspend fun setActiveStatus(userId: String, isActive: Boolean, timestamp: Long = System.currentTimeMillis())
}

@Dao
interface FlatDao {
    @Query("SELECT * FROM flats ORDER BY floor ASC, unit ASC")
    fun getAllFlats(): Flow<List<Flat>>

    @Query("SELECT * FROM flats WHERE flatId = :flatId LIMIT 1")
    fun getFlatById(flatId: String): Flow<Flat?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFlats(flats: List<Flat>)

    @Query("SELECT * FROM owner_profiles WHERE flatId = :flatId LIMIT 1")
    fun getOwnerProfile(flatId: String): Flow<OwnerProfile?>

    @Query("SELECT * FROM owner_profiles WHERE flatId = :flatId LIMIT 1")
    suspend fun getOwnerProfileDirect(flatId: String): OwnerProfile?

    @Query("SELECT * FROM owner_profiles ORDER BY flatId ASC")
    fun getAllOwnerProfiles(): Flow<List<OwnerProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerProfile(profile: OwnerProfile)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOwnerProfiles(profiles: List<OwnerProfile>)

    @Update
    suspend fun updateOwnerProfile(profile: OwnerProfile)

    @Query("SELECT * FROM renter_profiles WHERE renterId = :renterId LIMIT 1")
    fun getRenterProfile(renterId: String): Flow<RenterProfile?>

    @Query("SELECT * FROM renter_profiles WHERE flatId = :flatId LIMIT 1")
    fun getRenterProfileForFlat(flatId: String): Flow<RenterProfile?>

    @Query("SELECT * FROM renter_profiles ORDER BY flatId ASC")
    fun getAllRenters(): Flow<List<RenterProfile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRenterProfiles(profiles: List<RenterProfile>)

    @Update
    suspend fun updateRenterProfile(profile: RenterProfile)

    @Query("SELECT * FROM guard_profiles WHERE guardId = :guardId LIMIT 1")
    fun getGuardProfile(guardId: String): Flow<GuardProfile?>

    @Query("SELECT * FROM guard_profiles WHERE userId = :userId LIMIT 1")
    suspend fun getGuardProfileDirect(userId: String): GuardProfile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGuardProfile(profile: GuardProfile)

    @Update
    suspend fun updateGuardProfile(profile: GuardProfile)

    // Profile change approval workflow
    @Query("SELECT * FROM profile_change_requests ORDER BY submittedAt DESC")
    fun getAllChangeRequests(): Flow<List<ProfileChangeRequest>>

    @Query("SELECT * FROM profile_change_requests WHERE status = 'PENDING' ORDER BY submittedAt DESC")
    fun getPendingChangeRequests(): Flow<List<ProfileChangeRequest>>

    @Query("SELECT * FROM profile_change_requests WHERE id = :id LIMIT 1")
    suspend fun getChangeRequestById(id: Long): ProfileChangeRequest?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChangeRequest(request: ProfileChangeRequest)

    @Query("UPDATE profile_change_requests SET status = :status, reviewedBy = :reviewedBy, reviewedAt = :reviewedAt, adminRemarks = :remarks WHERE id = :id")
    suspend fun updateRequestStatus(id: Long, status: String, reviewedBy: String, reviewedAt: Long, remarks: String?)
}

@Dao
interface FinancialDao {
    @Query("SELECT * FROM incomes ORDER BY date DESC, id DESC")
    fun getAllIncomes(): Flow<List<Income>>

    @Query("SELECT * FROM incomes WHERE flatId = :flatId OR payerUserId = :userId ORDER BY date DESC")
    fun getIncomesForFlatOrUser(flatId: String?, userId: String?): Flow<List<Income>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncome(income: Income): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertIncomes(incomes: List<Income>)

    @Query("DELETE FROM incomes WHERE id = :id")
    suspend fun deleteIncome(id: Long)

    @Query("SELECT * FROM expenses ORDER BY date DESC, id DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE allocationType = 'COMMON' ORDER BY date DESC")
    fun getCommonExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE assignedFlatId = :flatId OR assignedUserId = :userId ORDER BY date DESC")
    fun getAssignedExpensesForFlat(flatId: String?, userId: String?): Flow<List<Expense>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenses(expenses: List<Expense>)

    @Query("DELETE FROM expenses WHERE id = :id")
    suspend fun deleteExpense(id: Long)

    @Query("SELECT SUM(amount) FROM incomes WHERE incomeType = 'MEMBER_DEPOSIT' AND flatId = :flatId")
    suspend fun getTotalDepositsForFlat(flatId: String): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE allocationType = 'COMMON'")
    suspend fun getTotalCommonExpenses(): Double?

    @Query("SELECT SUM(amount) FROM expenses WHERE assignedFlatId = :flatId")
    suspend fun getTotalAssignedExpensesForFlat(flatId: String): Double?

    @Query("SELECT SUM(amount) FROM incomes")
    suspend fun getTotalAllIncomes(): Double?

    @Query("SELECT SUM(amount) FROM expenses")
    suspend fun getTotalAllExpenses(): Double?

    // Custom Categories Management
    @Query("SELECT * FROM custom_categories ORDER BY type ASC, categoryName ASC")
    fun getAllCategories(): Flow<List<CustomCategory>>

    @Query("SELECT * FROM custom_categories WHERE type = :type ORDER BY categoryName ASC")
    fun getCategoriesByType(type: String): Flow<List<CustomCategory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategory(category: CustomCategory): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(categories: List<CustomCategory>)

    @Update
    suspend fun updateCategory(category: CustomCategory)

    @Query("DELETE FROM custom_categories WHERE id = :id")
    suspend fun deleteCategory(id: Long)
}

@Dao
interface BillDao {
    @Query("SELECT * FROM service_charge_configs ORDER BY effectiveMonth DESC LIMIT 1")
    fun getLatestServiceChargeConfig(): Flow<ServiceChargeConfig?>

    @Query("SELECT * FROM service_charge_configs ORDER BY effectiveMonth DESC LIMIT 1")
    suspend fun getLatestServiceChargeConfigDirect(): ServiceChargeConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertServiceChargeConfig(config: ServiceChargeConfig)

    @Query("SELECT * FROM gas_bills ORDER BY billingMonth DESC, flatId ASC")
    fun getAllGasBills(): Flow<List<GasBill>>

    @Query("SELECT * FROM gas_bills WHERE id = :id LIMIT 1")
    suspend fun getGasBillById(id: Long): GasBill?

    @Query("SELECT * FROM gas_bills WHERE flatId = :flatId ORDER BY billingMonth DESC")
    fun getGasBillsForFlat(flatId: String): Flow<List<GasBill>>

    @Query("SELECT * FROM gas_bills WHERE userId = :userId ORDER BY billingMonth DESC")
    fun getGasBillsForUser(userId: String): Flow<List<GasBill>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGasBills(bills: List<GasBill>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGasBill(bill: GasBill)

    @Update
    suspend fun updateGasBill(bill: GasBill)

    @Query("UPDATE gas_bills SET paymentStatus = :status, paidDate = :paidDate, paymentReference = :ref WHERE id = :id")
    suspend fun updateGasBillPayment(id: Long, status: String, paidDate: String?, ref: String?)

    @Query("UPDATE gas_bills SET amount = :newAmount WHERE billingMonth = :billingMonth")
    suspend fun updateGasBillsForMonth(billingMonth: String, newAmount: Double)

    @Query("DELETE FROM gas_bills WHERE billingMonth = :billingMonth")
    suspend fun deleteGasBillsForMonth(billingMonth: String)
}

@Dao
interface CommunityDao {
    @Query("SELECT * FROM announcements ORDER BY isPinned DESC, publishedAt DESC")
    fun getAllAnnouncements(): Flow<List<Announcement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncement(announcement: Announcement)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnnouncements(announcements: List<Announcement>)

    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getAllChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(message: ChatMessage)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(messages: List<ChatMessage>)

    @Query("SELECT * FROM queries ORDER BY createdAt DESC")
    fun getAllQueries(): Flow<List<MemberQuery>>

    @Query("SELECT * FROM queries WHERE userId = :userId ORDER BY createdAt DESC")
    fun getQueriesForUser(userId: String): Flow<List<MemberQuery>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuery(query: MemberQuery)

    @Query("UPDATE queries SET status = :status, adminResponse = :response, resolvedAt = :resolvedAt WHERE id = :id")
    suspend fun updateQueryStatus(id: Long, status: String, response: String?, resolvedAt: Long?)

    @Query("SELECT * FROM polls ORDER BY createdAt DESC")
    fun getAllPolls(): Flow<List<CommunityPoll>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPoll(poll: CommunityPoll)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPolls(polls: List<CommunityPoll>)

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId")
    fun getVotesForPoll(pollId: Long): Flow<List<PollVote>>

    @Query("SELECT * FROM poll_votes WHERE pollId = :pollId AND userId = :userId LIMIT 1")
    suspend fun getUserVoteForPoll(pollId: Long, userId: String): PollVote?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPollVote(vote: PollVote)

    @Query("SELECT * FROM documents ORDER BY uploadedAt DESC")
    fun getAllDocuments(): Flow<List<CommunityDocument>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocument(doc: CommunityDocument)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDocuments(docs: List<CommunityDocument>)
}

@Dao
interface GovernanceDao {
    @Query("SELECT * FROM governance_members ORDER BY id ASC")
    fun getAllMembers(): Flow<List<GovernanceMember>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMembers(members: List<GovernanceMember>)

    @Query("UPDATE governance_members SET delegatedPermissions = :permissions WHERE id = :id")
    suspend fun updateDelegatedPermissions(id: Long, permissions: String)
}

@Dao
interface AuditAndSyncDao {
    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLog)

    @Query("SELECT * FROM sync_queue WHERE syncStatus = 'PENDING' ORDER BY createdAt ASC")
    fun getPendingSyncItems(): Flow<List<SyncQueueItem>>

    @Query("SELECT * FROM sync_queue ORDER BY createdAt DESC")
    fun getAllSyncItems(): Flow<List<SyncQueueItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncItem(item: SyncQueueItem)

    @Query("UPDATE sync_queue SET syncStatus = :status, retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun updateSyncItemStatus(id: Long, status: String, error: String? = null)

    @Query("DELETE FROM sync_queue WHERE syncStatus = 'SYNCED'")
    suspend fun clearSyncedQueue()
}
