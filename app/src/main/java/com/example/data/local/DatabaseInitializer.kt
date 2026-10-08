package com.example.data.local

import com.example.data.model.*
import com.example.data.util.SecurityUtils

object DatabaseInitializer {

    suspend fun populateInitialData(
        userDao: UserDao,
        flatDao: FlatDao,
        financialDao: FinancialDao,
        billDao: BillDao,
        communityDao: CommunityDao,
        governanceDao: GovernanceDao,
        auditDao: AuditAndSyncDao
    ) {
        val initialPasswordHash = SecurityUtils.hashPassword("123456")

        // 1. Initial Users
        val users = mutableListOf<User>()

        // System Admin - SAIF AHMED SAKIL, Freelancer; MS in Applied Statistics, ISRT, University of Dhaka & LL.B., National University; Mobile: +8801611447765; Email: ssakil@isrt.ac.bd
        users.add(
            User(
                userId = "admin",
                passwordHash = initialPasswordHash,
                fullName = "SAIF AHMED SAKIL",
                role = UserRole.SYSTEM_ADMIN,
                flatId = null,
                isInitialPassword = true,
                isActive = true,
                mobile = "+8801611447765",
                email = "ssakil@isrt.ac.bd"
            )
        )

        // Delegated Admin dedicated account (Secretary Mr. Saif Ahmed Sakil)
        users.add(
            User(
                userId = "delegated_admin",
                passwordHash = initialPasswordHash,
                fullName = "Mr. Saif Ahmed Sakil (Delegated Admin)",
                role = UserRole.DELEGATED_ADMIN,
                flatId = "6A",
                isInitialPassword = true,
                isActive = true,
                mobile = "+8801611447765",
                email = "ssakil@isrt.ac.bd"
            )
        )

        // 14 Flat Owners
        // Initial Governance:
        // President & Owner of Flat 3A — Mr. Abu Bakkar Siddiki
        // Secretary & Owner of Flat 6A — Mr. Saif Ahmed Sakil
        // Treasurer & Owner of Flat 4A — Mr. Eliash Hussain Mithu
        // Four additional governance members: Flat 1A, 1B, 2A, 3B
        val flatOwnersData = listOf(
            Triple("1A", "owner_1a", "Mr. Kazi Nazrul Islam"),
            Triple("1B", "owner_1b", "Dr. Shamsur Rahman"),
            Triple("2A", "owner_2a", "Engr. Mizanur Rahman"),
            Triple("2B", "owner_2b", "Mr. Golam Mustafa"),
            Triple("3A", "owner_3a", "Mr. Abu Bakkar Siddiki"),   // President & Owner of Flat 3A
            Triple("3B", "owner_3b", "Advocate Rokeya Begum"),
            Triple("4A", "owner_4a", "Mr. Eliash Hussain Mithu"), // Treasurer & Owner of Flat 4A
            Triple("4B", "owner_4b", "Mr. Abdul Hannan"),
            Triple("5A", "owner_5a", "Prof. Humayun Ahmed"),
            Triple("5B", "owner_5b", "Dr. Anwar Hossain"),
            Triple("6A", "owner_6a", "Mr. Saif Ahmed Sakil"),     // Secretary & Owner of Flat 6A
            Triple("6B", "owner_6b", "Engr. Faruk Ahmed"),
            Triple("7A", "owner_7a", "Mr. Zahirul Haque"),
            Triple("7B", "owner_7b", "Haji Monirul Islam")
        )

        flatOwnersData.forEach { (flatId, userId, name) ->
            users.add(
                User(
                    userId = userId,
                    passwordHash = initialPasswordHash,
                    fullName = name,
                    role = when (userId) {
                        "owner_6a" -> UserRole.DELEGATED_ADMIN
                        "owner_3a", "owner_4a", "owner_1a", "owner_1b", "owner_2a", "owner_3b" -> UserRole.GOVERNANCE_MEMBER
                        else -> UserRole.FLAT_OWNER
                    },
                    flatId = flatId,
                    isInitialPassword = true,
                    isActive = true,
                    mobile = if (userId == "owner_6a") "+8801611447765" else "+8801811${flatId.lowercase()}000",
                    email = if (userId == "owner_6a") "ssakil@isrt.ac.bd" else "$userId@siddikvilla.com"
                )
            )
        }

        // Guard User
        users.add(
            User(
                userId = "guard_01",
                passwordHash = initialPasswordHash,
                fullName = "Md. Rafiqul Islam (Security Guard)",
                role = UserRole.GUARD,
                flatId = null,
                isInitialPassword = true,
                isActive = true,
                mobile = "+8801911998877",
                email = "guard@siddikvilla.com"
            )
        )

        // Renters
        users.add(
            User(
                userId = "renter_2b",
                passwordHash = initialPasswordHash,
                fullName = "Mr. Tanvir Hasan (Tenant)",
                role = UserRole.RENTER,
                flatId = "2B",
                isInitialPassword = true,
                isActive = true,
                mobile = "+8801712345678",
                email = "tanvir.2b@gmail.com"
            )
        )
        users.add(
            User(
                userId = "renter_5b",
                passwordHash = initialPasswordHash,
                fullName = "Dr. Farhana Yasmin (Tenant)",
                role = UserRole.RENTER,
                flatId = "5B",
                isInitialPassword = true,
                isActive = true,
                mobile = "+8801798765432",
                email = "farhana.5b@gmail.com"
            )
        )

        userDao.insertUsers(users)

        // 2. Flats
        val flats = flatOwnersData.map { (flatId, ownerId, _) ->
            val floor = flatId.substring(0, 1).toInt()
            val unit = flatId.substring(1)
            val isRented = flatId == "2B" || flatId == "5B"
            Flat(
                flatId = flatId,
                floor = floor,
                unit = unit,
                ownerUserId = ownerId,
                renterUserId = if (flatId == "2B") "renter_2b" else if (flatId == "5B") "renter_5b" else null,
                apartmentStatus = if (isRented) "Rented" else "Owner Occupied",
                sizeSqFt = if (unit == "A") 1550 else 1450,
                notes = "Floor $floor, Unit $unit"
            )
        }
        flatDao.insertFlats(flats)

        // 3. Owner Profiles
        val ownerProfiles = flatOwnersData.map { (flatId, ownerId, name) ->
            OwnerProfile(
                flatId = flatId,
                userId = ownerId,
                fullName = name,
                fatherName = "Late Alhaj Mohiuddin",
                motherName = "Begum Sufia Kamal",
                nidNumber = "1980269251800${flatId.hashCode().toString().takeLast(4)}",
                dateOfBirth = "1978-05-15",
                mobileNumber = "+8801811${flatId.lowercase()}000",
                altMobileNumber = "+8801711${flatId.lowercase()}111",
                email = "$ownerId@siddikvilla.com",
                occupation = when (flatId) {
                    "2A", "6A" -> "Engineers"
                    "3A" -> "University Professor"
                    "3B" -> "Supreme Court Advocate"
                    "4A" -> "Business Executive"
                    "5A" -> "Industrialist"
                    "6B" -> "Statistician & Legal Consultant"
                    else -> "Private Service"
                },
                designation = when (flatId) {
                    "5A" -> "Managing Director"
                    "6B" -> "Senior Consultant"
                    "4A" -> "Director of Finance"
                    else -> "Senior Officer"
                },
                employer = "Dhaka Commercial Enterprise",
                officeAddress = "Motijheel C/A, Dhaka-1000",
                permanentAddress = "SAS-Siddik Villa, Flat $flatId, Dhaka",
                spouseInfo = "Married",
                childrenInfo = "2 Children",
                emergencyContact = "+8801711000000 (Spouse)",
                apartmentStatus = if (flatId == "2B" || flatId == "3A") "Rented" else "Owner Occupied"
            )
        }
        flatDao.insertOwnerProfiles(ownerProfiles)

        // 4. Renter Profiles
        flatDao.insertRenterProfiles(
            listOf(
                RenterProfile(
                    renterId = "renter_2b",
                    flatId = "2B",
                    fullName = "Mr. Tanvir Hasan",
                    fatherName = "Md. Enamul Hasan",
                    motherName = "Nazma Hasan",
                    nidNumber = "1991269384729102",
                    dateOfBirth = "1991-08-20",
                    mobileNumber = "+8801712345678",
                    altMobileNumber = "+8801822334455",
                    email = "tanvir.2b@gmail.com",
                    occupation = "Software Architect",
                    designation = "Lead Engineer",
                    employer = "FinTech Solutions Ltd",
                    officeAddress = "Gulshan-1, Dhaka",
                    familyMembersCount = 3,
                    permanentAddress = "Village: Shampur, District: Cumilla",
                    emergencyContact = "+8801712000000 (Brother)",
                    tenancyStartDate = "2024-03-01",
                    tenancyEndDate = "2026-02-28",
                    status = "Active"
                ),
                RenterProfile(
                    renterId = "renter_3a",
                    flatId = "3A",
                    fullName = "Dr. Farhana Yasmin",
                    fatherName = "Dr. M. A. Wahab",
                    motherName = "Salma Wahab",
                    nidNumber = "1988269584739201",
                    dateOfBirth = "1988-12-10",
                    mobileNumber = "+8801798765432",
                    email = "farhana.3a@gmail.com",
                    occupation = "Physician",
                    designation = "Assistant Professor",
                    employer = "Dhaka Medical College Hospital",
                    officeAddress = "Bakshibazar, Dhaka",
                    familyMembersCount = 2,
                    permanentAddress = "Sylhet Sadar, Sylhet",
                    emergencyContact = "+8801798000000 (Husband)",
                    tenancyStartDate = "2024-06-01",
                    tenancyEndDate = "2026-05-31",
                    status = "Active"
                )
            )
        )

        // Guard Profile
        flatDao.insertGuardProfile(
            GuardProfile(
                guardId = "guard_01",
                userId = "guard_01",
                fullName = "Md. Rafiqul Islam",
                mobileNumber = "+8801911998877",
                nidNumber = "1985441234567890",
                joiningDate = "2023-01-01",
                dutyHours = "Day Shift (8:00 AM - 8:00 PM)",
                emergencyContact = "+8801911000000 (Son)"
            )
        )

        // 5. Governance Members (President 3A, Secretary 6A, Treasurer 4A + 4 additional governance members)
        governanceDao.insertMembers(
            listOf(
                GovernanceMember(
                    position = "President",
                    memberName = "Mr. Abu Bakkar Siddiki",
                    userId = "owner_3a",
                    flatId = "3A",
                    mobile = "+88018113a000",
                    delegatedPermissions = "INCOME_ENTRY,EXPENSE_ENTRY,PROFILE_APPROVAL,COMMUNITY_MGMT,SERVICE_CHARGE_MGMT,GAS_BILL_MGMT,PASSWORD_RESET"
                ),
                GovernanceMember(
                    position = "Secretary",
                    memberName = "Mr. Saif Ahmed Sakil",
                    userId = "owner_6a",
                    flatId = "6A",
                    mobile = "+8801611447765",
                    delegatedPermissions = "INCOME_ENTRY,EXPENSE_ENTRY,PROFILE_APPROVAL,COMMUNITY_MGMT,SERVICE_CHARGE_MGMT,GAS_BILL_MGMT,PASSWORD_RESET,CATEGORY_MGMT"
                ),
                GovernanceMember(
                    position = "Treasurer",
                    memberName = "Mr. Eliash Hussain Mithu",
                    userId = "owner_4a",
                    flatId = "4A",
                    mobile = "+88018114a000",
                    delegatedPermissions = "INCOME_ENTRY,EXPENSE_ENTRY,GAS_BILL_MGMT,SERVICE_CHARGE_MGMT"
                ),
                GovernanceMember(
                    position = "Committee Member 1",
                    memberName = "Mr. Kazi Nazrul Islam",
                    userId = "owner_1a",
                    flatId = "1A",
                    mobile = "+88018111a000"
                ),
                GovernanceMember(
                    position = "Committee Member 2",
                    memberName = "Dr. Shamsur Rahman",
                    userId = "owner_1b",
                    flatId = "1B",
                    mobile = "+88018111b000"
                ),
                GovernanceMember(
                    position = "Committee Member 3",
                    memberName = "Engr. Mizanur Rahman",
                    userId = "owner_2a",
                    flatId = "2A",
                    mobile = "+88018112a000"
                ),
                GovernanceMember(
                    position = "Committee Member 4",
                    memberName = "Advocate Rokeya Begum",
                    userId = "owner_3b",
                    flatId = "3B",
                    mobile = "+88018113b000"
                )
            )
        )

        // 6. Service Charge Config (৳3,500/month)
        billDao.insertServiceChargeConfig(
            ServiceChargeConfig(
                effectiveMonth = "2026-10",
                monthlyRate = 3500.0,
                dueDayOfMonth = 5,
                changedBy = "admin",
                remarks = "Standard 2026 Monthly Service Charge rate per flat"
            )
        )

        // 7. Initial Gas Bills for current month (৳1,080 each, due 10th)
        val gasBills = mutableListOf<GasBill>()
        flatOwnersData.forEach { (flatId, ownerId, _) ->
            val isRented = flatId == "2B" || flatId == "5B"
            gasBills.add(
                GasBill(
                    billingMonth = "2026-10",
                    userId = if (isRented) (if (flatId == "2B") "renter_2b" else "renter_5b") else ownerId,
                    flatId = flatId,
                    userType = if (isRented) "Renter" else "Flat Owner",
                    amount = 1080.0,
                    dueDate = "2026-10-10",
                    paymentStatus = if (flatId in listOf("1A", "4A", "6A")) "Paid" else "Due",
                    paidDate = if (flatId in listOf("1A", "4A", "6A")) "2026-10-04" else null,
                    paymentReference = if (flatId in listOf("1A", "4A", "6A")) "GAS-OCT-$flatId" else null
                )
            )
        }
        // Guard gas bill
        gasBills.add(
            GasBill(
                billingMonth = "2026-10",
                userId = "guard_01",
                flatId = "Guard Room",
                userType = "Guard",
                amount = 540.0,
                dueDate = "2026-10-10",
                paymentStatus = "Paid",
                paidDate = "2026-10-02",
                paymentReference = "GAS-OCT-GUARD"
            )
        )
        billDao.insertGasBills(gasBills)

        // 8. Initial Member Deposits and Building Incomes (Separate Incomes Table)
        val sampleIncomes = listOf(
            Income(
                date = "2026-10-01",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "owner_5a",
                payerName = "Mr. Abu Bakkar Siddiki",
                flatId = "5A",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "Bank Transfer",
                referenceNo = "TRX-OCT-5A",
                remarks = "Advance service charge deposit"
            ),
            Income(
                date = "2026-10-02",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "owner_6b",
                payerName = "Mr. Saif Ahmed Sakil",
                flatId = "6B",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "bKash",
                referenceNo = "BKASH-9X124",
                remarks = "Monthly deposit"
            ),
            Income(
                date = "2026-10-02",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "owner_4a",
                payerName = "Mr. Eliash Hussain Mithu",
                flatId = "4A",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "Cash",
                referenceNo = "REC-4A-10",
                remarks = "Deposited to Treasurer"
            ),
            Income(
                date = "2026-10-03",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "owner_1a",
                payerName = "Mr. Kazi Nazrul Islam",
                flatId = "1A",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "Nagad",
                referenceNo = "NGD-88210",
                remarks = "Deposit confirmed"
            ),
            Income(
                date = "2026-10-03",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "owner_2a",
                payerName = "Engr. Mizanur Rahman",
                flatId = "2A",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "bKash",
                referenceNo = "BKASH-43110",
                remarks = "October fee"
            ),
            Income(
                date = "2026-10-04",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "owner_3b",
                payerName = "Advocate Rokeya Begum",
                flatId = "3B",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "Bank Transfer",
                referenceNo = "EBL-004921",
                remarks = "Direct bank deposit"
            ),
            Income(
                date = "2026-10-04",
                amount = 3500.0,
                incomeType = "MEMBER_DEPOSIT",
                payerUserId = "renter_2b",
                payerName = "Mr. Tanvir Hasan (Flat 2B)",
                flatId = "2B",
                category = "Monthly Service Charge",
                subcategory = "October 2026 Service Charge",
                paymentMethod = "bKash",
                referenceNo = "BKASH-99120",
                remarks = "Paid on behalf of Flat 2B"
            ),
            Income(
                date = "2026-09-28",
                amount = 50000.0,
                incomeType = "BUILDING_INCOME",
                payerUserId = null,
                payerName = "Building Reserve Fund Carryforward",
                flatId = null,
                category = "Other Receipts",
                subcategory = "Opening Reserve Balance",
                paymentMethod = "Bank Transfer",
                referenceNo = "RES-2026-OPEN",
                remarks = "Previous balance carried forward in community account"
            ),
            Income(
                date = "2026-10-04",
                amount = 4320.0,
                incomeType = "BUILDING_INCOME",
                payerUserId = null,
                payerName = "Collected Gas Bills (4 Flats)",
                flatId = null,
                category = "Gas Charge",
                subcategory = "Titas Gas Aggregate Pool",
                paymentMethod = "Cash",
                referenceNo = "GAS-POOL-10",
                remarks = "Batch gas bill collections for Titas deposit"
            )
        )
        financialDao.insertIncomes(sampleIncomes)

        // 9. Initial Expenses (Common ÷ 14, Individual, Capital, etc.)
        val sampleExpenses = listOf(
            Expense(
                date = "2026-10-02",
                amount = 8000.0,
                category = "Maintenance & Operations",
                subcategory = "Elevator Maintenance",
                expenseType = "Operating",
                allocationType = "COMMON", // Divided among 14 flats (৳571.43 each)
                vendorPayee = "Otis Elevator Bangladesh",
                paymentMethod = "Cheque",
                receiptRef = "INV-OTIS-882",
                remarks = "Monthly routine servicing and rope lubrication"
            ),
            Expense(
                date = "2026-10-03",
                amount = 12000.0,
                category = "Management & Administration",
                subcategory = "Guard Salary",
                expenseType = "Operating",
                allocationType = "COMMON", // Divided among 14 flats (৳857.14 each)
                vendorPayee = "Md. Rafiqul Islam",
                paymentMethod = "Cash",
                receiptRef = "VOUCH-SAL-OCT",
                remarks = "Day shift security guard monthly wage"
            ),
            Expense(
                date = "2026-10-03",
                amount = 4500.0,
                category = "Maintenance & Operations",
                subcategory = "Cleaning",
                expenseType = "Operating",
                allocationType = "COMMON", // Divided among 14 flats (৳321.43 each)
                vendorPayee = "Rahima Begum",
                paymentMethod = "Cash",
                receiptRef = "VOUCH-CLN-01",
                remarks = "Staircase and common corridor daily cleaning"
            ),
            Expense(
                date = "2026-10-04",
                amount = 6500.0,
                category = "Utilities",
                subcategory = "Generator Fuel/Oil",
                expenseType = "Operating",
                allocationType = "COMMON", // Divided among 14 flats (৳464.29 each)
                vendorPayee = "Padma Oil Filling Station",
                paymentMethod = "Cash",
                receiptRef = "RECEIPT-DSL-50L",
                remarks = "50 Liters diesel for standby backup generator"
            ),
            Expense(
                date = "2026-10-04",
                amount = 3200.0,
                category = "Maintenance & Operations",
                subcategory = "CCTV Maintenance",
                expenseType = "Operating",
                allocationType = "COMMON", // Divided among 14 flats (৳228.57 each)
                vendorPayee = "SecureVision Tech",
                paymentMethod = "bKash",
                receiptRef = "CCTV-REP-102",
                remarks = "Replaced faulty camera adapter in parking area"
            ),
            // Individual Assigned Expense (assigned to 2A)
            Expense(
                date = "2026-10-04",
                amount = 1500.0,
                category = "Maintenance & Operations",
                subcategory = "Plumbing",
                expenseType = "Operating",
                allocationType = "INDIVIDUAL",
                assignedFlatId = "2A",
                assignedUserId = "owner_2a",
                vendorPayee = "Master Plumber Karim",
                paymentMethod = "Cash",
                receiptRef = "PLUMB-2A-99",
                remarks = "Flat 2A individual kitchen line pipe leak repair"
            ),
            // Building Reserve Expense
            Expense(
                date = "2026-10-05",
                amount = 15000.0,
                category = "Capital Expenditure",
                subcategory = "Major Building Works",
                expenseType = "Capital",
                allocationType = "BUILDING_RESERVE",
                vendorPayee = "Dhaka Waterproofing Co.",
                paymentMethod = "Bank Transfer",
                receiptRef = "WATERPROOF-ROOF-7",
                remarks = "Rooftop membrane waterproofing installment 1 (from reserve fund)"
            )
        )
        financialDao.insertExpenses(sampleExpenses)

        // 10. Announcements
        communityDao.insertAnnouncements(
            listOf(
                Announcement(
                    title = "Monthly General Meeting - Friday 7:30 PM",
                    content = "The October General Body Meeting of SAS-Siddik Villa flat owners will be held at the Ground Floor Community Hall this Friday at 7:30 PM. Agenda includes rooftop solar installation and festival security.",
                    category = "Meeting",
                    isPinned = true,
                    publishedBy = "Saif Ahmed Sakil (Secretary)"
                ),
                Announcement(
                    title = "Water Overhead Tank Cleaning Scheduled",
                    content = "Please be informed that the main overhead and underground water reservoirs will undergo deep sanitary cleaning on Sunday between 10:00 AM and 2:00 PM. Water supply will remain suspended during this window.",
                    category = "Maintenance",
                    isPinned = false,
                    publishedBy = "Management Committee"
                ),
                Announcement(
                    title = "Notice: Service Charge Due Date Reminder",
                    content = "Respected flat owners and tenants, please deposit your monthly service charge (৳3,500) and individual gas bill (৳1,080) by the 10th of this month to ensure uninterrupted community utility services.",
                    category = "Financial",
                    isPinned = false,
                    publishedBy = "Eliash Hussain Mithu (Treasurer)"
                )
            )
        )

        // 11. Initial Community Chat
        communityDao.insertChatMessages(
            listOf(
                ChatMessage(
                    senderUserId = "owner_5a",
                    senderName = "Mr. Abu Bakkar Siddiki (President)",
                    senderRole = "President",
                    flatId = "5A",
                    message = "Assalamu Alaikum respected neighbors. Hope all families are doing well."
                ),
                ChatMessage(
                    senderUserId = "owner_6b",
                    senderName = "Mr. Saif Ahmed Sakil (Secretary)",
                    senderRole = "Secretary",
                    flatId = "6B",
                    message = "Wa Alaikum Assalam. The lift maintenance was successfully completed yesterday by Otis engineers."
                ),
                ChatMessage(
                    senderUserId = "owner_2a",
                    senderName = "Engr. Mizanur Rahman",
                    senderRole = "Flat Owner",
                    flatId = "2A",
                    message = "Thanks Secretary Sahib. The lift ride is significantly smoother now."
                ),
                ChatMessage(
                    senderUserId = "renter_2b",
                    senderName = "Mr. Tanvir Hasan",
                    senderRole = "Renter",
                    flatId = "2B",
                    message = "Hello everyone, could guard bhai please verify the parking light switch tonight?"
                ),
                ChatMessage(
                    senderUserId = "guard_01",
                    senderName = "Md. Rafiqul Islam",
                    senderRole = "Guard",
                    flatId = null,
                    message = "Ji Sir, ami ekhon check kore dichhi. All lights will be on by 6:00 PM."
                )
            )
        )

        // 12. Community Poll
        communityDao.insertPolls(
            listOf(
                CommunityPoll(
                    question = "Should we install an RFID / Biometric smart access control system on the main entrance gate?",
                    description = "Estimated cost ৳42,000 to be funded 50% from Reserve Fund and 50% distributed (approx ৳1,500/flat).",
                    optionsList = "Yes, approve immediately|Need more price quotes|No, current guard protocol is sufficient",
                    createdBy = "admin"
                )
            )
        )

        // 13. Documents
        communityDao.insertDocuments(
            listOf(
                CommunityDocument(
                    title = "SAS-Siddik Villa Bylaws & Constitution (2025)",
                    documentType = "Bylaws",
                    description = "Official building governance rules, committee responsibilities, and tenant protocols approved by the General Body.",
                    fileReference = "BYLAWS-VILLA-2025.pdf",
                    allowedRoles = "ALL"
                ),
                CommunityDocument(
                    title = "Otis Lift Annual Maintenance Contract (2025-2026)",
                    documentType = "Contract",
                    description = "Comprehensive AMC contract for Passenger Elevator (8 Persons, 550kg).",
                    fileReference = "CONTRACT-OTIS-LIFT-2526.pdf",
                    allowedRoles = "ALL"
                ),
                CommunityDocument(
                    title = "Dhaka City Corporation Holding Tax Assessment",
                    documentType = "Tax Assessment",
                    description = "Official municipal holding tax receipt and annual assessment paper.",
                    fileReference = "TAX-DNCC-HOLDING-2025.pdf",
                    allowedRoles = "OWNERS_ONLY"
                ),
                CommunityDocument(
                    title = "Dhaka WASA Deep Tube-Well Clearance Certificate",
                    documentType = "Utility Bill",
                    description = "WASA legal approval and water quality clearance report.",
                    fileReference = "WASA-CLEARANCE-DTW.pdf",
                    allowedRoles = "ALL"
                )
            )
        )

        // 14. Master Categories (Dynamic in SQLite)
        val initialCategories = mutableListOf<CustomCategory>()
        MasterCategories.ExpenseCategories.forEach { (cat, subList) ->
            initialCategories.add(
                CustomCategory(
                    type = "EXPENSE",
                    categoryName = cat,
                    subcategoriesCsv = subList.joinToString(", ")
                )
            )
        }
        MasterCategories.IncomeCategories.forEach { cat ->
            initialCategories.add(
                CustomCategory(
                    type = "INCOME",
                    categoryName = cat,
                    subcategoriesCsv = "Standard $cat, Arrears, Advance"
                )
            )
        }
        financialDao.insertCategories(initialCategories)

        // 15. Audit Log
        auditDao.insertAuditLog(
            AuditLog(
                userId = "system",
                action = "BOOTSTRAP",
                entityType = "SYSTEM",
                entityId = "SAS-SIDDIK-VILLA",
                details = "System database initialized with 14 flats, initial committee, and standard ৳3,500 service charge."
            )
        )
    }
}
