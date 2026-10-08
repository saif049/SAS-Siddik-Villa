package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        User::class,
        Flat::class,
        OwnerProfile::class,
        RenterProfile::class,
        GuardProfile::class,
        ProfileChangeRequest::class,
        Income::class,
        Expense::class,
        ServiceChargeConfig::class,
        GasBill::class,
        GovernanceMember::class,
        Announcement::class,
        ChatMessage::class,
        MemberQuery::class,
        CommunityPoll::class,
        PollVote::class,
        CommunityDocument::class,
        AuditLog::class,
        SyncQueueItem::class,
        CustomCategory::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun flatDao(): FlatDao
    abstract fun financialDao(): FinancialDao
    abstract fun billDao(): BillDao
    abstract fun communityDao(): CommunityDao
    abstract fun governanceDao(): GovernanceDao
    abstract fun auditAndSyncDao(): AuditAndSyncDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "siddik_villa_eicm.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Populate database in coroutine
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    DatabaseInitializer.populateInitialData(
                                        userDao = database.userDao(),
                                        flatDao = database.flatDao(),
                                        financialDao = database.financialDao(),
                                        billDao = database.billDao(),
                                        communityDao = database.communityDao(),
                                        governanceDao = database.governanceDao(),
                                        auditDao = database.auditAndSyncDao()
                                    )
                                }
                            }
                        }
                    })
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
