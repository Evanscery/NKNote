package io.github.nknote.model

/**
 * Mood options for a diary entry. Persisted by [key]; the icon and localized
 * label are resolved in the UI layer via [io.github.nknote.ui.components.MoodIconMap]
 * so this enum stays pure Kotlin (portable to a future commonMain source set).
 */
enum class Mood(val key: String) {
    ECSTATIC("ecstatic"),
    HAPPY("happy"),
    NEUTRAL("neutral"),
    SAD("sad"),
    AWFUL("awful"),
    ANGRY("angry"),
    CALM("calm");

    companion object {
        fun fromKey(key: String?): Mood? = key?.takeIf { it.isNotBlank() }?.let { k -> entries.firstOrNull { it.key == k } }
    }
}