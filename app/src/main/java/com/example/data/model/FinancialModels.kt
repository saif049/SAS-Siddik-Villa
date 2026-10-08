package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class IncomeType {
    BUILDING_INCOME,
    MEMBER_DEPOSIT
}

enum class ExpenseAllocationType {
    COMMON,             // Divided equally among 14 flats (Total Expense ÷ 14)
    INDIVIDUAL,         // Assigned to a specific flat
    RENTER,             // Assigned to renter
    OWNER,              // Assigned to owner
    GUARD,              // Assigned to guard
    BUILDING_RESERVE,   // Paid entirely from building reserve fund
    INVESTMENT_CAPITAL  // Tracked separately as capital expenditure
}

@Entity(tableName = "incomes")
data class Income(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val amount: Double,
    val incomeType: String = "MEMBER_DEPOSIT", // BUILDING_INCOME or MEMBER_DEPOSIT
    val payerUserId: String? = null,
    val payerName: String,
    val flatId: String? = null, // e.g. "1A"
    val category: String, // Monthly Service Charge, Gas Charge, Wasa Charge, Special Charge, etc.
    val subcategory: String = "",
    val paymentMethod: String = "Cash", // Cash, bKash, Nagad, Bank Transfer
    val referenceNo: String = "",
    val remarks: String = "",
    val createdBy: String = "admin",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED" // LOCAL_ONLY, PENDING, SYNCED, FAILED
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: String, // "YYYY-MM-DD"
    val amount: Double,
    val category: String, // Management & Administration, Maintenance & Operations, Utilities, etc.
    val subcategory: String,
    val expenseType: String = "Operating", // Operating, Capital, Emergency
    val allocationType: String = "COMMON", // COMMON, INDIVIDUAL, RENTER, OWNER, GUARD, BUILDING_RESERVE, INVESTMENT_CAPITAL
    val assignedFlatId: String? = null, // If INDIVIDUAL / OWNER / RENTER
    val assignedUserId: String? = null,
    val vendorPayee: String = "",
    val paymentMethod: String = "Cash",
    val receiptRef: String = "",
    val remarks: String = "",
    val createdBy: String = "admin",
    val createdAt: Long = System.currentTimeMillis(),
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "service_charge_configs")
data class ServiceChargeConfig(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val effectiveMonth: String, // "YYYY-MM"
    val monthlyRate: Double = 3500.0,
    val dueDayOfMonth: Int = 10, // Default due date 10th
    val changedBy: String = "admin",
    val changedAt: Long = System.currentTimeMillis(),
    val remarks: String = "Standard monthly building service charge"
)

@Entity(tableName = "gas_bills")
data class GasBill(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billingMonth: String, // "YYYY-MM"
    val userId: String,
    val flatId: String,
    val userType: String = "Flat Owner", // Flat Owner, Renter, Guard
    val amount: Double = 1080.0, // Standard 2-burner gas line rate in Dhaka
    val dueDate: String = "", // e.g. "2026-10-10"
    val paymentStatus: String = "Due", // Paid, Due, Partially Paid, Overdue, Waived
    val paidDate: String? = null,
    val paymentReference: String? = null,
    val syncStatus: String = "SYNCED"
)

@Entity(tableName = "custom_categories")
data class CustomCategory(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // "EXPENSE" or "INCOME"
    val categoryName: String,
    val subcategoriesCsv: String = "" // comma-separated sub-categories
)

// Helper domain model for flat financial position:
// Balance = Own Eligible Deposits - Individual Share of Common Expenses (Total Common ÷ 14) - Individual Assigned Expenses
data class FlatFinancialStatement(
    val flatId: String,
    val ownerName: String,
    val totalDeposits: Double,
    val shareOfCommonExpense: Double,
    val individualAssignedExpense: Double,
    val netBalance: Double, // Positive = Surplus, Negative = Deficit
    val serviceChargeDue: Double,
    val gasBillDue: Double,
    val isServiceOverdue: Boolean = false,
    val isGasOverdue: Boolean = false
)
