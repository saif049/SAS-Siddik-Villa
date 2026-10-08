package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "governance_members")
data class GovernanceMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val position: String, // President, Secretary, Treasurer, Committee Member
    val memberName: String,
    val userId: String,
    val flatId: String,
    val mobile: String = "",
    val startDate: String = "2025-01-01",
    val endDate: String = "2026-12-31",
    val status: String = "Active",
    val delegatedPermissions: String = "" // comma separated: "INCOME_ENTRY,EXPENSE_ENTRY,PROFILE_APPROVAL"
)

@Entity(tableName = "announcements")
data class Announcement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val content: String,
    val category: String = "General", // General, Emergency, Maintenance, Meeting, Financial
    val isPinned: Boolean = false,
    val publishedBy: String = "Management Committee",
    val publishedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val senderUserId: String,
    val senderName: String,
    val senderRole: String,
    val flatId: String? = null,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isNotice: Boolean = false
)

@Entity(tableName = "queries")
data class MemberQuery(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val userId: String,
    val flatId: String,
    val submitterName: String,
    val subject: String,
    val description: String,
    val category: String = "General", // Water, Electricity, Lift, Security, Cleaning, Billing, Other
    val status: String = "OPEN", // OPEN, IN_PROGRESS, RESOLVED, CLOSED
    val adminResponse: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val resolvedAt: Long? = null
)

@Entity(tableName = "polls")
data class CommunityPoll(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val question: String,
    val description: String = "",
    val optionsList: String, // Comma or pipe-separated: "Yes|No|Neutral" or "Option A|Option B"
    val createdBy: String = "admin",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true
)

@Entity(tableName = "poll_votes")
data class PollVote(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pollId: Long,
    val userId: String,
    val flatId: String,
    val selectedOptionIndex: Int,
    val votedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "documents")
data class CommunityDocument(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val documentType: String, // Bylaws, Land Document, Tax Assessment, Meeting Minutes, Contract, Utility Bill
    val description: String = "",
    val fileReference: String = "", // e.g. "DOC-2026-001.pdf"
    val allowedRoles: String = "ALL", // ALL, ADMIN_ONLY, OWNERS_ONLY
    val uploadedBy: String = "Admin",
    val uploadedAt: Long = System.currentTimeMillis()
)
