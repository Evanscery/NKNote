package io.github.nknote.ui.calendar

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.nknote.AppViewModelFactory
import io.github.nknote.R
import io.github.nknote.ui.components.NkEmptyState
import io.github.nknote.ui.components.NkNoteCard
import io.github.nknote.ui.components.NkTopAppBar
import io.github.nknote.ui.navigation.LocalDrawerState
import io.github.nknote.ui.navigation.NkNoteNavigation
import io.github.nknote.ui.theme.NkMotion
import io.github.nknote.ui.theme.NkNoteTheme
import io.github.nknote.ui.theme.NkSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarPage(
    nav: NkNoteNavigation,
    openNote: (Int) -> Unit,
    viewModel: CalendarViewModel = viewModel(factory = AppViewModelFactory.factory)
) {
    val currentMonth by viewModel.currentMonth.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val noteDates by viewModel.noteDates.collectAsStateWithLifecycle()
    val notesForDate by viewModel.notesForSelectedDate.collectAsStateWithLifecycle()

    NkNoteTheme {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            topBar = {
                NkTopAppBar(
                    title = stringResource(R.string.calendar_title),
                    drawerState = LocalDrawerState.current
                )
            }
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    // The month grid + a multi-note day list can exceed one screen — scroll.
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = NkSpacing.lg),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Month nav
                val today = LocalDate.now()
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().padding(vertical = NkSpacing.md)
                ) {
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(
                        text = currentMonth.format(
                            DateTimeFormatter.ofPattern(stringResource(R.string.calendar_month_format), Locale.getDefault())
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                if (currentMonth != YearMonth.now() || selectedDate != today) {
                    TextButton(onClick = { viewModel.goToToday() }) {
                        Text(stringResource(R.string.home_today), style = MaterialTheme.typography.labelLarge)
                    }
                }

                // Day-of-week header, localized (Monday-first).
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    val locale = Locale.getDefault()
                    DayOfWeek.entries.forEach { dow ->
                        Text(
                            text = dow.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(Modifier.height(NkSpacing.sm))

                // Calendar grid: subtle slide+fade on month change, direction-aware.
                AnimatedContent(
                    targetState = currentMonth,
                    transitionSpec = {
                        val forward = targetState > initialState
                        (slideInHorizontally(tween(NkMotion.DurationMedium, easing = NkMotion.StandardEasing)) {
                            if (forward) it / 3 else -it / 3
                        } + fadeIn(tween(NkMotion.DurationMedium))) togetherWith
                            fadeOut(tween(NkMotion.DurationShort))
                    },
                    label = "month_grid"
                ) { month ->
                    MonthGrid(
                        month = month,
                        noteDates = noteDates,
                        selectedDate = selectedDate,
                        today = today,
                        onSelect = { viewModel.selectDate(it) }
                    )
                }

                // Selected date notes
                selectedDate?.let {
                    Spacer(Modifier.height(NkSpacing.lg))
                    if (notesForDate.isEmpty()) {
                        NkEmptyState(
                            message = stringResource(R.string.calendar_no_notes),
                            fillMaxSize = false
                        )
                    } else {
                        Text(
                            stringResource(R.string.calendar_notes_count, notesForDate.size),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = NkSpacing.sm)
                        )
                        notesForDate.forEach { note ->
                            NkNoteCard(
                                onClick = { openNote(note.id) },
                                modifier = Modifier.padding(vertical = NkSpacing.xs / 2)
                            ) {
                                Column {
                                    Text(
                                        note.title.ifBlank { stringResource(R.string.common_no_title) },
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(note.date, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(NkSpacing.xl))
            }
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    noteDates: Set<String>,
    selectedDate: LocalDate?,
    today: LocalDate,
    onSelect: (LocalDate) -> Unit
) {
    Column {
        val firstDay = month.atDay(1)
        val daysInMonth = month.lengthOfMonth()
        val startOffset = firstDay.dayOfWeek.value - 1 // Monday = 0

        var dayCounter = 1 - startOffset
        while (dayCounter <= daysInMonth) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                for (col in 0 until 7) {
                    val dayNum = dayCounter + col
                    if (dayNum in 1..daysInMonth) {
                        val date = month.atDay(dayNum)
                        val dateStr = date.toString()
                        CalendarDayCell(
                            day = dayNum,
                            hasNote = dateStr in noteDates,
                            isSelected = date == selectedDate,
                            isToday = date == today,
                            onClick = { onSelect(date) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                    }
                }
            }
            dayCounter += 7
        }
    }
}

@Composable
private fun CalendarDayCell(
    day: Int,
    hasNote: Boolean,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isToday -> MaterialTheme.colorScheme.primaryContainer
        else -> Color.Transparent
    }
    val textColor = when {
        isSelected -> MaterialTheme.colorScheme.onPrimary
        isToday -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onSurface
    }
    val todayLabel = stringResource(R.string.home_today)
    val hasNoteLabel = stringResource(R.string.a11y_calendar_has_note)
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(bgColor)
            .clickable(onClick = onClick)
            .semantics {
                role = Role.Button
                selected = isSelected
                val states = buildList {
                    if (isToday) add(todayLabel)
                    if (hasNote) add(hasNoteLabel)
                }
                if (states.isNotEmpty()) stateDescription = states.joinToString(", ")
            }
    ) {
        // Fixed layout: the number is centered; the dot is anchored to the bottom so the number
        // never shifts, and the dot stays visible on the selected day (inverted color).
        Text(
            text = day.toString(),
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isToday || isSelected) FontWeight.SemiBold else FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.Center)
        )
        if (hasNote) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 6.dp)
                    .size(5.dp)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.primary,
                        CircleShape
                    )
            )
        }
    }
}
