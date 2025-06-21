package com.example.nknote.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.date.datepicker
import com.vanpra.composematerialdialogs.datetime.date.DatePickerDefaults
import com.vanpra.composematerialdialogs.rememberMaterialDialogState
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DatePickerDialog(
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    initialDate: LocalDate = LocalDate.now()
) {
    var selectedDate by remember { mutableStateOf(initialDate) }
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val dialogState = rememberMaterialDialogState()

    // 显示对话框
    LaunchedEffect(Unit) {
        dialogState.show()
    }

    MaterialDialog(
        dialogState = dialogState,
        backgroundColor = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        buttons = {
            positiveButton("确定") {
                onDateSelected(selectedDate)
                onDismiss()
            }
            negativeButton("取消") {
                onDismiss()
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "选择日期",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            datepicker(
                initialDate = selectedDate,
                title = "请选择日期",
                yearRange = IntRange(2000, 2050),
                locale = Locale.SIMPLIFIED_CHINESE,
                colors = DatePickerDefaults.colors(
                    headerBackgroundColor = MaterialTheme.colorScheme.primary,
                    headerTextColor = MaterialTheme.colorScheme.onPrimary,
                    calendarHeaderTextColor = MaterialTheme.colorScheme.onBackground,
                    dateActiveBackgroundColor = MaterialTheme.colorScheme.primary,
                    dateInactiveBackgroundColor = MaterialTheme.colorScheme.surface,
                    dateActiveTextColor = MaterialTheme.colorScheme.onPrimary,
                    dateInactiveTextColor = MaterialTheme.colorScheme.onBackground
                )
            ) { date ->
                selectedDate = date
            }
        }
    }
} 