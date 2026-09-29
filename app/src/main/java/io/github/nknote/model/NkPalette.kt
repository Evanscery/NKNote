package io.github.nknote.model

/**
 * Named, reusable palette constants for NKNote.
 * Pure Kotlin (no Compose/Android deps) so it is portable to a future
 * commonMain source set.
 */
object NkPalette {
    /**
     * Curated tag palette, tuned to the warm-paper theme. A tag's color is picked
     * deterministically from this list (see [io.github.nknote.data.entity.Tag.colorFor])
     * so the same tag name maps to the same color on every device.
     */
    val tagPalette: List<String> = listOf(
        "#5E7A6E", // sage
        "#B85C5C", // warm red
        "#C08A3E", // amber
        "#5C7AB8", // dusk blue
        "#8A5CB8", // plum
        "#4E8A8A", // teal
        "#A0648A", // rose
        "#6E7A4E"  // olive
    )

    /** Default color applied to a tag when no deterministic pick is wanted. */
    val defaultTagColor: String get() = tagPalette[0]
}
