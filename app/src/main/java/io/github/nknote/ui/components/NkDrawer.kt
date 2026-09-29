package io.github.nknote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nknote.R
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing

/** A drawer destination: [label] + [icon] + the [onClick] that navigates and closes the drawer. */
data class NkDrawerItem(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/** A grouped drawer section: a [title] header above a list of [items]. */
data class NkDrawerSection(val title: String, val items: List<NkDrawerItem>)

/**
 * The sheet content of the global drawer. Grouped into sections (Journal / Tools / System) per the
 * navigation-tiering plan. Each item's [NkDrawerItem.onClick] is responsible for navigation AND
 * closing the drawer (the caller wires both).
 */
@Composable
fun NkDrawerSheet(sections: List<NkDrawerSection>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(20.dp)
    ) {
        Box(
            modifier = Modifier
                .height(64.dp)
                .width(64.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, NkShapes.largeSmall),
            contentAlignment = Alignment.Center
        ) {
            Text("N", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = stringResource(R.string.app_name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = stringResource(R.string.drawer_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    sections.forEach { section ->
        Text(
            text = section.title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.fillMaxWidth().padding(start = NkSpacing.lg, top = NkSpacing.md, bottom = NkSpacing.xs)
        )
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = NkSpacing.md)) {
            section.items.forEach { item ->
                NavigationDrawerItem(
                    label = { Text(item.label, fontWeight = FontWeight.Medium) },
                    icon = { Icon(item.icon, null) },
                    selected = false,
                    onClick = item.onClick,
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedContainerColor = Color.Transparent
                    ),
                    shape = NkShapes.smallMedium,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }
        }
    }
    Spacer(Modifier.height(NkSpacing.xl))
}
