package io.github.nknote.model

/**
 * Weather options. [key] is persisted; the icon and localized label are resolved
 * in the UI layer via [io.github.nknote.ui.components.WeatherIconMap] so this
 * enum stays pure Kotlin (portable to a future commonMain source set).
 */
enum class Weather(val key: String) {
    SUNNY("sunny"),
    CLOUDY("cloudy"),
    OVERCAST("overcast"),
    RAIN("rain"),
    SNOW("snow"),
    STORM("storm"),
    FOG("fog"),
    WINDY("windy"),
    HOT("hot");

    companion object {
        fun fromKey(key: String?): Weather? = key?.takeIf { it.isNotBlank() }?.let { k -> entries.firstOrNull { it.key == k } }
    }
}