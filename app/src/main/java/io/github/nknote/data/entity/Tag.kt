package io.github.nknote.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String,    // ARGB hex, e.g. "#FF9C6B"
    val createdAt: Long
)