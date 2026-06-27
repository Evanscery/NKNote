package io.github.nknote.ui.components

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
import io.github.nknote.model.Mood

/**
 * UI-layer mapping from [Mood] to its Material icon and localized string resource.
 * Kept out of `model/` so the enum stays portable to commonMain.
 */
object MoodIconMap {
    fun icon(mood: Mood): ImageVector = when (mood) {
        Mood.ECSTATIC -> Icons.Filled.SentimentVerySatisfied
        Mood.HAPPY -> Icons.Filled.SentimentSatisfied
        Mood.NEUTRAL -> Icons.Filled.SentimentNeutral
        Mood.SAD -> Icons.Filled.SentimentDissatisfied
        Mood.AWFUL -> Icons.Filled.SentimentVeryDissatisfied
        Mood.ANGRY -> Icons.Filled.MoodBad
        Mood.CALM -> Icons.Filled.Mood
    }

    fun labelRes(mood: Mood): Int = when (mood) {
        Mood.ECSTATIC -> R.string.mood_ecstatic
        Mood.HAPPY -> R.string.mood_happy
        Mood.NEUTRAL -> R.string.mood_neutral
        Mood.SAD -> R.string.mood_sad
        Mood.AWFUL -> R.string.mood_awful
        Mood.ANGRY -> R.string.mood_angry
        Mood.CALM -> R.string.mood_calm
    }
}