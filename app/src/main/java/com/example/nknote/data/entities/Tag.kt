package com.example.nknote.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey
    val id: String,                   // 标签ID
    val name: String,                 // 标签名称
    val color: String,                // 标签颜色
    val createdAt: Long              // 创建时间
) 