package com.example.nknote.data.dao

import androidx.room.*
import com.example.nknote.data.entities.SyncRecord
import kotlinx.coroutines.flow.Flow

@Dao
interface SyncRecordDao {
    @Query("SELECT * FROM sync_records WHERE deviceId = :deviceId ORDER BY lastSyncTime DESC LIMIT 1")
    fun getLatestSyncRecord(deviceId: String): Flow<SyncRecord?>

    @Query("SELECT * FROM sync_records ORDER BY lastSyncTime DESC")
    fun getAllSyncRecords(): Flow<List<SyncRecord>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncRecord(syncRecord: SyncRecord)

    @Update
    suspend fun updateSyncRecord(syncRecord: SyncRecord)

    @Delete
    suspend fun deleteSyncRecord(syncRecord: SyncRecord)

    @Query("DELETE FROM sync_records WHERE deviceId = :deviceId")
    suspend fun deleteSyncRecordsForDevice(deviceId: String)
} 