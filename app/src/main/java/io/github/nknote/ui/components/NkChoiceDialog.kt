package io.github.nknote.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

/**
 * A single-choice dialog built on [NkDialog]: one radio row per option, selecting an
 * option applies it and dismisses. Used by Settings' theme-mode picker; generic enough
 * for any future 3+-way choice.
 */
@Composable
fun <T> NkSingleChoiceDialog(
    title: String,
    options: List<Pair<T, String>>,
    selected: T,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit
) {
    NkDialog(onDismiss = onDismiss, title = title) {
        Column(Modifier.selectableGroup()) {
            options.forEach { (value, label) ->
                val isSelected = value == selected
                Surface(
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surface,
                    shape = NkShapes.small,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                        .selectable(
                            selected = isSelected,
                            role = Role.RadioButton,
                            onClick = {
                                onSelect(value)
                                onDismiss()
                            }
                        )
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = NkSpacing.sm, vertical = NkSpacing.xs)
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Spacer(Modifier.width(NkSpacing.sm))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}
