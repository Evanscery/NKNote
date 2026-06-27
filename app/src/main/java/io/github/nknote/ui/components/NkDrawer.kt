package io.github.nknote.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.github.nknote.ui.theme.NkShapes
import io.github.nknote.ui.theme.NkSpacing
import kotlinx.coroutines.launch

data class NkDrawerItem(val label: String, val icon: ImageVector)

/**
 * Gentle modal drawer with a soft header and rounded items. The [onSelect] callback receives
 * the tapped item index; the drawer closes itself on selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NkDrawer(
    items: List<NkDrawerItem>,
    onSelect: (Int) -> Unit,
    content: @Composable (openDrawer: () -> Unit) -> Unit
) {
    val drawerState = rememberDrawerState(DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val open: () -> Unit = { scope.launch { drawerState.open() } }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(drawerContainerColor = MaterialTheme.colorScheme.surface) {
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
                        text = "NKNote",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "A gentle place for your days",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = NkSpacing.md)) {
                    items.forEachIndexed { index, item ->
                        NavigationDrawerItem(
                            label = { Text(item.label, fontWeight = FontWeight.Medium) },
                            icon = { Icon(item.icon, null) },
                            selected = false,
                            onClick = {
                                scope.launch { drawerState.close() }
                                onSelect(index)
                            },
                            colors = NavigationDrawerItemDefaults.colors(
                                selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                                unselectedContainerColor = androidx.compose.ui.graphics.Color.Transparent
                            ),
                            shape = NkShapes.smallMedium,
                            modifier = Modifier.padding(vertical = 2.dp)
                        )
                    }
                }
                Spacer(Modifier.height(NkSpacing.xl))
            }
        },
        content = { content(open) }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NkNoteCard(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, NkShapes.mediumLarge)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(NkSpacing.lg)
    ) { content() }
}