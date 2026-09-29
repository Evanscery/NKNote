package io.github.nknote.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import io.github.nknote.model.NkPalette
import kotlin.math.abs

@Entity(tableName = "tags")
data class Tag(
    @PrimaryKey
    val id: String,
    val name: String,
    val color: String,    // ARGB hex, e.g. "#FF9C6B"
    val createdAt: Long
) {
    companion object {
        /** Canonical form: trimmed, single-spaced, lowercased (locale-neutral). */
        fun normalize(name: String): String =
            name.trim().replace(Regex("\\s+"), " ").lowercase()

        /**
         * Stable id = the normalized name itself. Bijective, therefore collision-free —
         * "Work" and " work " resolve to the same tag — and it survives backup round-trips
         * with zero id remapping. [Tag.name] keeps the first-typed display casing.
         */
        fun idFor(name: String): String = normalize(name)

        /** Deterministic curated-palette pick — the same tag name gets the same color everywhere. */
        fun colorFor(name: String): String =
            NkPalette.tagPalette[abs(normalize(name).hashCode()) % NkPalette.tagPalette.size]
    }
}