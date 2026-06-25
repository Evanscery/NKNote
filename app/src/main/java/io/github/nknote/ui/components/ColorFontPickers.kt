package io.github.nknote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.nknote.R
import io.github.nknote.ui.editor.richtext.parseHexColor
import kotlin.math.roundToInt

private val PALETTE = listOf(
    "#2A2723", "#5E7A6E", "#B08968", "#9C6B5A", "#6B7A8E",
    "#A85A5A", "#8A8E5A", "#5A8E7A", "#5A5A8E", "#8E5A8E"
)

@Composable
fun ColorPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    var customHex by remember { mutableStateOf("#5E7A6E") }
    var red by remember { mutableFloatStateOf(0.37f) }
    var green by remember { mutableFloatStateOf(0.48f) }
    var blue by remember { mutableFloatStateOf(0.43f) }
    val rgb = Color(red, green, blue)
    customHex = toHex(rgb)

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = { onPick(customHex) }) { Text(stringResource(R.string.common_confirm)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_cancel)) } },
        title = { Text(stringResource(R.string.editor_select_color)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PALETTE.forEach { hex ->
                        val c = parseHexColor(hex) ?: Color.Black
                        Box(
                            Modifier.size(34.dp).background(c, CircleShape).border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                .clickable { onPick(hex); }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
                Box(Modifier.size(60.dp).background(rgb, RoundedCornerShape(8.dp)))
                Spacer(Modifier.height(8.dp))
                SliderRow(stringResource(R.string.editor_select_color), red, Color(0xFFB0524A)) { red = it }
                SliderRow(stringResource(R.string.editor_select_color), green, Color(0xFF6B8E5A)) { green = it }
                SliderRow(stringResource(R.string.editor_select_color), blue, Color(0xFF5A6B8E)) { blue = it }
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(value = customHex, onValueChange = { customHex = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        }
    )
}

@Composable
private fun SliderRow(label: String, value: Float, thumb: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text("${(value * 255).roundToInt()}", style = MaterialTheme.typography.labelMedium, modifier = Modifier.width(32.dp))
        Slider(value = value, onValueChange = onChange, modifier = Modifier.fillMaxWidth().height(40.dp), colors = androidx.compose.material3.SliderDefaults.colors(thumbColor = thumb))
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
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.common_close)) } },
        title = { Text(stringResource(R.string.editor_format_size)) },
        text = {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SIZES.forEach { (scale, label) ->
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(12.dp),
                        onClick = { onPick(scale) },
                        modifier = Modifier.weight(1f).padding(4.dp)
                    ) {
                        Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("A", style = MaterialTheme.typography.headlineSmall.copy(fontSize = (16f * scale).sp))
                            Spacer(Modifier.height(4.dp))
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
        }
    )
}