package io.github.nknote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nknote.R
import io.github.nknote.ui.editor.richtext.parseHexColor
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing
import io.github.nknote.ui.theme.SliderBlueAccent
import io.github.nknote.ui.theme.SliderGreenAccent
import io.github.nknote.ui.theme.SliderRedAccent
import kotlin.math.roundToInt

private val PALETTE = listOf(
    "#2A2723", "#5E7A6E", "#B08968", "#9C6B5A", "#6B7A8E",
    "#A85A5A", "#8A8E5A", "#5A8E7A", "#5A5A8E", "#8E5A8E"
)

/**
 * @param initial the color currently applied at the cursor (`#AARRGGBB`/`#RRGGBB`), used to
 * mark the matching swatch with a selected ring and to seed the RGB sliders.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorPickerDialog(
    onPick: (String) -> Unit,
    onDismiss: () -> Unit,
    initial: String? = null
) {
    val initialColor = initial?.let { parseHexColor(it) }
    var red by remember { mutableFloatStateOf(initialColor?.red ?: 0.37f) }
    var green by remember { mutableFloatStateOf(initialColor?.green ?: 0.48f) }
    var blue by remember { mutableFloatStateOf(initialColor?.blue ?: 0.43f) }
    // The hex field is user-editable; slider edits overwrite it, typed edits win until the
    // next slider move. Confirm applies whatever the field holds (falling back to sliders
    // when it doesn't parse).
    var customHex by remember { mutableStateOf(initial ?: toHex(Color(0.37f, 0.48f, 0.43f))) }
    val sliderColor = Color(red, green, blue)

    NkDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.editor_select_color),
        confirmText = stringResource(R.string.common_confirm),
        onConfirm = { onPick(if (parseHexColor(customHex) != null) customHex else toHex(sliderColor)) },
        dismissText = stringResource(R.string.common_cancel)
    ) {
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(NkSpacing.xs),
                verticalArrangement = Arrangement.spacedBy(NkSpacing.xs)
            ) {
                PALETTE.forEach { hex ->
                    val c = parseHexColor(hex) ?: Color.Black
                    val selected = initial != null && parseHexColor(initial) == c
                    Box(
                        Modifier
                            .minimumInteractiveComponentSize()
                            .size(40.dp)
                            .background(c, CircleShape)
                            .border(
                                width = if (selected) 2.dp else 1.dp,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                shape = CircleShape
                            )
                            .clickable { onPick(hex) }
                            .semantics {
                                contentDescription = hex
                                role = Role.Button
                            }
                    )
                }
            }
            Spacer(Modifier.height(NkSpacing.lg))
            Box(Modifier.size(56.dp).background(parseHexColor(customHex) ?: sliderColor, NkShapes.small))
            Spacer(Modifier.height(NkSpacing.sm))
            SliderRow(red, SliderRedAccent) { red = it; customHex = toHex(Color(it, green, blue)) }
            SliderRow(green, SliderGreenAccent) { green = it; customHex = toHex(Color(red, it, blue)) }
            SliderRow(blue, SliderBlueAccent) { blue = it; customHex = toHex(Color(red, green, it)) }
            Spacer(Modifier.height(NkSpacing.xs))
            OutlinedTextField(
                value = customHex,
                onValueChange = { customHex = it },
                singleLine = true,
                shape = NkShapes.small,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SliderRow(value: Float, thumb: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("${(value * 255).roundToInt()}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(32.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            modifier = Modifier.fillMaxWidth().height(40.dp),
            colors = SliderDefaults.colors(thumbColor = thumb)
        )
    }
}

private fun toHex(c: Color): String {
    val a = (c.alpha * 255).roundToInt(); val r = (c.red * 255).roundToInt()
    val g = (c.green * 255).roundToInt(); val b = (c.blue * 255).roundToInt()
    return String.format("#%02X%02X%02X%02X", a, r, g, b)
}

private val SIZES = listOf(0.85f to "S", 1f to "M", 1.2f to "L", 1.5f to "XL")

@Composable
fun FontSizePickerDialog(onPick: (Float) -> Unit, onDismiss: () -> Unit) {
    NkDialog(onDismiss = onDismiss, title = stringResource(R.string.editor_format_size)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(NkSpacing.sm)) {
            SIZES.forEach { (scale, label) ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = NkShapes.smallMedium,
                    onClick = { onPick(scale) },
                    modifier = Modifier.weight(1f).padding(NkSpacing.xs)
                ) {
                    Column(Modifier.padding(NkSpacing.md), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("A", style = MaterialTheme.typography.headlineSmall.copy(fontSize = (16f * scale).sp), color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(NkSpacing.xs))
                        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}
