package io.github.nknote.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.nknote.R
import io.github.nknote.model.Weather

/**
 * UI-layer mapping from [Weather] to its Material icon and localized string resource.
 * Kept out of `model/` so the enum stays portable to commonMain.
 */
object WeatherIconMap {
    fun icon(weather: Weather): ImageVector = when (weather) {
        Weather.SUNNY -> Icons.Filled.WbSunny
        Weather.CLOUDY -> Icons.Filled.Cloud
        Weather.OVERCAST -> Icons.Filled.Grain
        Weather.RAIN -> Icons.Filled.WaterDrop
        Weather.SNOW -> Icons.Filled.SevereCold
        Weather.STORM -> Icons.Filled.Bolt
        Weather.FOG -> Icons.Filled.Park
        Weather.WINDY -> Icons.Filled.Air
        Weather.HOT -> Icons.Filled.Whatshot
    }

    fun labelRes(weather: Weather): Int = when (weather) {
        Weather.SUNNY -> R.string.weather_sunny
        Weather.CLOUDY -> R.string.weather_cloudy
        Weather.OVERCAST -> R.string.weather_overcast
        Weather.RAIN -> R.string.weather_rain
        Weather.SNOW -> R.string.weather_snow
        Weather.STORM -> R.string.weather_storm
        Weather.FOG -> R.string.weather_fog
        Weather.WINDY -> R.string.weather_windy
        Weather.HOT -> R.string.weather_hot
    }
}