package com.example.nknote.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "images",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class Image(
    @PrimaryKey
    val id: String,                   // 图片ID
    val noteId: String,               // 关联的笔记ID
    val originalPath: String,         // 原图路径
    val thumbnailPath: String,        // 缩略图路径
    val originalSize: Long,           // 原图大小
    val thumbnailSize: Long,          // 缩略图大小
    val mimeType: String,             // 图片类型
    val width: Int,                   // 原图宽度
    val height: Int,                  // 原图高度
    val syncStatus: Int,              // 同步状态
    val version: Int,                 // 版本号
    val createdAt: Long,              // 创建时间
    val updatedAt: Long               // 更新时间
) 