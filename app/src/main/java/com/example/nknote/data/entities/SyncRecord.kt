package com.example.nknote.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_records")
data class SyncRecord(
    @PrimaryKey
    val id: String,                   // 同步记录ID
    val deviceId: String,             // 设备ID
    val lastSyncTime: Long,           // 最后同步时间
    val syncType: Int,                // 同步类型（0:全量, 1:增量）
    val status: Int                   // 同步状态
) 