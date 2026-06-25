package io.github.nknote.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Mood
import androidx.compose.material.icons.filled.MoodBad
import androidx.compose.material.icons.filled.SentimentDissatisfied
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.nknote.R

/** Mood options for a diary entry. Persisted by [key]. */
enum class Mood(val key: String, val labelRes: Int, val icon: ImageVector) {
    ECSTATIC("ecstatic", R.string.mood_ecstatic, Icons.Filled.SentimentVerySatisfied),
    HAPPY("happy", R.string.mood_happy, Icons.Filled.SentimentSatisfied),
    NEUTRAL("neutral", R.string.mood_neutral, Icons.Filled.SentimentNeutral),
    SAD("sad", R.string.mood_sad, Icons.Filled.SentimentDissatisfied),
    AWFUL("awful", R.string.mood_awful, Icons.Filled.SentimentVeryDissatisfied),
    ANGRY("angry", R.string.mood_angry, Icons.Filled.MoodBad),
    CALM("calm", R.string.mood_calm, Icons.Filled.Mood);

    companion object {
        fun fromKey(key: String?): Mood? = key?.takeIf { it.isNotBlank() }?.let { k -> entries.firstOrNull { it.key == k } }
    }
}