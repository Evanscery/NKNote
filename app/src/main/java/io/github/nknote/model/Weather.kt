package io.github.nknote.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.SevereCold
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Air
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.nknote.R

/** Weather options. [key] is persisted; [labelRes] resolves per-locale in UI. */
enum class Weather(val key: String, val labelRes: Int, val icon: ImageVector) {
    SUNNY("sunny", R.string.weather_sunny, Icons.Filled.WbSunny),
    CLOUDY("cloudy", R.string.weather_cloudy, Icons.Filled.Cloud),
    OVERCAST("overcast", R.string.weather_overcast, Icons.Filled.Grain),
    RAIN("rain", R.string.weather_rain, Icons.Filled.WaterDrop),
    SNOW("snow", R.string.weather_snow, Icons.Filled.SevereCold),
    STORM("storm", R.string.weather_storm, Icons.Filled.Bolt),
    FOG("fog", R.string.weather_fog, Icons.Filled.Park),
    WINDY("windy", R.string.weather_windy, Icons.Filled.Air),
    HOT("hot", R.string.weather_hot, Icons.Filled.Whatshot);

    companion object {
        fun fromKey(key: String?): Weather? = key?.takeIf { it.isNotBlank() }?.let { k -> entries.firstOrNull { it.key == k } }
    }
}