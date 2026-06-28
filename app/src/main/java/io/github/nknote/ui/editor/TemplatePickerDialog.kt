package io.github.nknote.ui.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nknote.R
import io.github.nknote.data.templates.NoteTemplate
import io.github.nknote.data.templates.NoteTemplates
import io.github.nknote.ui.components.NkDialog
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

/**
 * Tiny template picker (todo 12). Lists the built-in [NoteTemplates] as tappable rows; selecting
 * one dismisses the dialog and hands the template back to the editor, which calls
 * [EditorViewModel.applyTemplate]. Reachable only from the editor overflow menu on a NEW note
 * (`noteId == null`), so an existing note's content is never silently replaced.
 */
@Composable
fun TemplatePickerDialog(
    onPick: (NoteTemplate) -> Unit,
    onDismiss: () -> Unit
) {
    NkDialog(
        onDismiss = onDismiss,
        title = stringResource(R.string.editor_templates),
        dismissText = stringResource(R.string.common_cancel)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(NkSpacing.sm)) {
            NoteTemplates.all.forEach { template ->
                Surface(
                    onClick = { onPick(template) },
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = NkShapes.mediumSmall,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    androidx.compose.foundation.layout.Row(
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                        modifier = Modifier.padding(NkSpacing.md)
                    ) {
                        Icon(
                            Icons.Filled.AutoAwesome,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        androidx.compose.foundation.layout.Spacer(Modifier.size(NkSpacing.sm))
                        Text(
                            text = template.title,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
