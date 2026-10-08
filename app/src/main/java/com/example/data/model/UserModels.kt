package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    SYSTEM_ADMIN,
    DELEGATED_ADMIN,
    GOVERNANCE_MEMBER,
    FLAT_OWNER,
    RENTER,
    GUARD
}

enum class ApartmentStatus {
    OWNER_OCCUPIED,
    RENTED,
    VACANT
}

@Entity(tableName = "users")
data class User(
    @PrimaryKey val userId: String,
    val passwordHash: String,
    val fullName: String,
    val role: UserRole,
    val flatId: String? = null,
    val isInitialPassword: Boolean = true,
    val isActive: Boolean = true,
    val mobile: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "flats")
data class Flat(
    @PrimaryKey val flatId: String, // "1A", "1B", ... "7B" (14 flats)
    val floor: Int,
    val unit: String,
    val ownerUserId: String,
    val renterUserId: String? = null,
    val apartmentStatus: String = "Owner Occupied",
    val sizeSqFt: Int = 1450,
    val notes: String = ""
)

@Entity(tableName = "owner_profiles")
data class OwnerProfile(
    @PrimaryKey val flatId: String,
    val userId: String,
    val fullName: String,
    val photoUri: String? = null,
    val fatherName: String = "",
    val motherName: String = "",
    val nidNumber: String = "",
    val dateOfBirth: String = "",
    val mobileNumber: String = "",
    val altMobileNumber: String = "",
    val email: String = "",
    val occupation: String = "",
    val designation: String = "",
    val employer: String = "",
    val officeAddress: String = "",
    val permanentAddress: String = "",
    val familyMembersCount: Int = 4,
    val spouseInfo: String = "",
    val childrenInfo: String = "",
    val emergencyContact: String = "",
    val apartmentStatus: String = "Owner Occupied"
)

@Entity(tableName = "renter_profiles")
data class RenterProfile(
    @PrimaryKey val renterId: String, // e.g. "renter_2b"
    val flatId: String,
    val fullName: String,
    val photoUri: String? = null,
    val fatherName: String = "",
    val motherName: String = "",
    val nidNumber: String = "",
    val dateOfBirth: String = "",
    val mobileNumber: String = "",
    val altMobileNumber: String = "",
    val email: String = "",
    val occupation: String = "",
    val designation: String = "",
    val employer: String = "",
    val officeAddress: String = "",
    val familyMembersCount: Int = 3,
    val permanentAddress: String = "",
    val spouseInfo: String = "",
    val childrenInfo: String = "",
    val emergencyContact: String = "",
    val tenancyStartDate: String = "2025-01-01",
    val tenancyEndDate: String = "2026-12-31",
    val status: String = "Active"
)

@Entity(tableName = "guard_profiles")
data class GuardProfile(
    @PrimaryKey val guardId: String, // e.g. "guard_01"
    val userId: String,
    val fullName: String,
    val photoUri: String? = null,
    val fatherName: String = "",
    val motherName: String = "",
    val nidNumber: String = "",
    val dateOfBirth: String = "",
    val mobileNumber: String = "",
    val altMobileNumber: String = "",
    val email: String = "",
    val joiningDate: String = "2023-01-01",
    val dutyHours: String = "Day Shift (8:00 AM - 8:00 PM)",
    val permanentAddress: String = "",
    val spouseInfo: String = "",
    val childrenInfo: String = "",
    val emergencyContact: String = ""
)

@Entity(tableName = "profile_change_requests")
data class ProfileChangeRequest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val flatId: String,
    val userRole: String, // "FLAT_OWNER", "RENTER", "GUARD"
    val fullName: String,
    val photoUri: String? = null,
    val fatherName: String = "",
    val motherName: String = "",
    val nidNumber: String = "",
    val dateOfBirth: String = "",
    val mobileNumber: String = "",
    val altMobileNumber: String = "",
    val email: String = "",
    val occupation: String = "",
    val designation: String = "",
    val employer: String = "",
    val officeAddress: String = "",
    val permanentAddress: String = "",
    val spouseInfo: String = "",
    val childrenInfo: String = "",
    val emergencyContact: String = "",
    val reason: String = "",
    val status: String = "PENDING", // PENDING, APPROVED, REJECTED
    val submittedAt: Long = System.currentTimeMillis(),
    val reviewedBy: String? = null,
    val reviewedAt: Long? = null,
    val adminRemarks: String? = null
)
