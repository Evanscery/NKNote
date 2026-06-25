package io.github.nknote.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.nknote.R
import io.github.nknote.model.Mood
import io.github.nknote.model.Weather

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WeatherPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    NkDialog(onDismiss = onDismiss, title = stringResource(R.string.editor_select_weather)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Weather.entries.forEach { w ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    onClick = { onPick(w.key) },
                    modifier = Modifier.padding(2.dp)
                ) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(w.icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.height(24.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(w.labelRes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MoodPickerDialog(onPick: (String) -> Unit, onDismiss: () -> Unit) {
    NkDialog(onDismiss = onDismiss, title = stringResource(R.string.editor_select_mood)) {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Mood.entries.forEach { m ->
                Surface(
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(14.dp),
                    onClick = { onPick(m.key) },
                    modifier = Modifier.padding(2.dp)
                ) {
                    Column(Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(m.icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.height(24.dp))
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(m.labelRes), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
                    }
                }
            }
        }
    }
}