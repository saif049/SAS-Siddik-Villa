package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "audit_logs")
data class AuditLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long = System.currentTimeMillis(),
    val userId: String,
    val action: String, // LOGIN, LOGOUT, PASSWORD_CHANGE, PASSWORD_RESET, CREATE, UPDATE, DELETE, APPROVE, REJECT, SERVICE_CHARGE_CHANGE, GAS_BILL_CHANGE, BACKUP
    val entityType: String, // USER, INCOME, EXPENSE, PROFILE, BILL, GOVERNANCE, QUERY, POLL
    val entityId: String,
    val details: String
)

@Entity(tableName = "sync_queue")
data class SyncQueueItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val entityType: String,
    val entityId: String,
    val operation: String, // INSERT, UPDATE, DELETE
    val payloadJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "PENDING", // PENDING, SYNCED, FAILED
    val retryCount: Int = 0,
    val lastError: String? = null
)

// Master data lists for reference and validation
object MasterCategories {
    val ExpenseCategories = mapOf(
        "Management & Administration" to listOf(
            "Management Fees",
            "Manager Salary",
            "Guard Salary",
            "Administrative Expenses",
            "Office Expenses",
            "Printing & Postage",
            "Software/IT",
            "Community Events",
            "Beautification"
        ),
        "Maintenance & Operations" to listOf(
            "General Repairs",
            "Plumbing",
            "Electrical",
            "HVAC",
            "Appliance",
            "Roofing",
            "Painting",
            "Landscaping",
            "Pest Control",
            "Waste Management",
            "Cleaning",
            "Deep Cleaning",
            "Elevator Maintenance",
            "Generator Maintenance",
            "Substation Maintenance",
            "Water Tank Cleaning",
            "Security",
            "CCTV Maintenance",
            "Other Maintenance"
        ),
        "Utilities" to listOf(
            "Electricity",
            "Water",
            "Sewerage",
            "Deep Tube Well",
            "Generator Fuel/Oil",
            "Gas",
            "Intercom",
            "Internet",
            "CCTV/Wi-Fi"
        ),
        "Taxes & Government Charges" to listOf(
            "City Corporation Tax",
            "Government Fees",
            "Inspection Fees",
            "Property Tax",
            "Other Statutory Charges"
        ),
        "Capital Expenditure" to listOf(
            "Major Building Works",
            "Exterior Renovation",
            "Parking Works",
            "Major Equipment",
            "Electrical Upgrade",
            "Elevator Modernization",
            "Other Capital Works"
        )
    )

    val IncomeCategories = listOf(
        "Monthly Service Charge",
        "Gas Charge",
        "Wasa/Water Charge",
        "Special Charge",
        "Rental/Facility Income",
        "Penalty/Late Fee",
        "Interest/Bank Return",
        "Other Receipts"
    )

    val PaymentMethods = listOf(
        "Cash",
        "bKash",
        "Nagad",
        "Bank Transfer",
        "Cheque"
    )
}

object SystemInfo {
    const val APP_TITLE = "SAS-SIDDIK VILLA EI&CM"
    const val APP_SUBTITLE = "Expense, Income & Community Management System"
    const val BUILDING_NAME = "SAS-Siddik Villa"
    const val TOTAL_FLATS = 14
    const val CURRENCY_SYMBOL = "৳"
    const val SYSTEM_ADMIN_INFO = "SAIF AHMED SAKIL, Freelancer; MS in Applied Statistics, ISRT, University of Dhaka & LL.B., National University; Mobile: +8801611447765; Email: ssakil@isrt.ac.bd"
    const val DEVELOPER_NAME = "SAIF AHMED SAKIL"
    const val DEVELOPER_TITLE = "Freelancer"
    const val DEVELOPER_QUALIFICATION = "MS in Applied Statistics, ISRT, University of Dhaka\nLL.B., National University"
    const val DEVELOPER_CONTACT = "ssakil@isrt.ac.bd | +8801611447765"
    const val INITIAL_DEFAULT_PASSWORD = "123456"
}
