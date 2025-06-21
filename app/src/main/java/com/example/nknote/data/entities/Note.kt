package com.example.nknote.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Int,                    // 改为自增主键
    val title: String,              // 标题
    val description: String,         // 简介
    val content: String,            // HTML 格式的文本内容
    val createdAt: Long,            // 创建时间戳
    val updatedAt: Long,            // 最后更新时间戳
    val date: String,               // 日记日期
    val weather: String,            // 天气
    val coverImageId: String?,      // 封面图片ID（关联到图片表）
    val syncStatus: Int,            // 同步状态（0:未同步, 1:已同步, 2:需要更新）
    val version: Int                // 版本号，用于冲突检测
) 