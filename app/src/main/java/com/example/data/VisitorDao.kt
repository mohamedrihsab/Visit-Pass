package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface VisitorDao {
    @Query("SELECT * FROM visitors ORDER BY timestamp DESC")
    fun getAllVisitors(): Flow<List<VisitorEntity>>

    @Query("SELECT * FROM visitors WHERE id = :id")
    suspend fun getVisitorById(id: Long): VisitorEntity?

    @Query("SELECT * FROM visitors WHERE passNumber = :passNumber LIMIT 1")
    suspend fun getVisitorByPassNumber(passNumber: String): VisitorEntity?

    @Query("SELECT * FROM visitors WHERE status = :status ORDER BY timestamp DESC")
    fun getVisitorsByStatus(status: String): Flow<List<VisitorEntity>>

    @Query("SELECT passNumber FROM visitors WHERE passNumber LIKE :prefixPattern ORDER BY id DESC LIMIT 20")
    suspend fun getRecentPassNumbers(prefixPattern: String): List<String>

    @Query("SELECT * FROM visitors WHERE isSyncedToGoogleForm = 0 ORDER BY timestamp ASC")
    suspend fun getUnsyncedVisitors(): List<VisitorEntity>

    @Query("SELECT COUNT(*) FROM visitors WHERE isSyncedToGoogleForm = 0")
    fun getUnsyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM visitors WHERE isSyncedToGoogleForm = 1")
    fun getSyncedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM visitors WHERE status = 'CHECKED_IN'")
    fun getActiveCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisitor(visitor: VisitorEntity): Long

    @Update
    suspend fun updateVisitor(visitor: VisitorEntity)

    @Query("UPDATE visitors SET status = 'CHECKED_OUT', checkOutFormattedTime = :checkOutTime WHERE id = :id")
    suspend fun markCheckedOut(id: Long, checkOutTime: String)

    @Query("UPDATE visitors SET isSyncedToGoogleForm = :synced WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, synced: Boolean)

    @Query("DELETE FROM visitors WHERE id = :id")
    suspend fun deleteVisitor(id: Long)

    @Query("DELETE FROM visitors WHERE passNumber = :passNumber")
    suspend fun deleteVisitorByPassNumber(passNumber: String)

    @Query("DELETE FROM visitors")
    suspend fun clearAllVisitors()
}
