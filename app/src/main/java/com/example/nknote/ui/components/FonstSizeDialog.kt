package com.example.nknote.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun FontSizeDialog(
    showDialog: Boolean,
    currentSize: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // 定义4个预设字体大小选项，每个选项包含实际的字号大小
    val fontSizeOptions = listOf(
        2 to "小",
        4 to "正常",
        5 to "大",
        7 to "特大"
    )

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("选择字体大小") },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // 预设字体大小选项
                    fontSizeOptions.forEach { (size, label) ->
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = false,
                                    onClick = { onConfirm(size) }
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = false,
                                onClick = { onConfirm(size) }
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            // 显示字体大小示例
                            Text(
                                text = "A",
                                fontSize = (size * 8).sp  // 将size值转换为更合适的显示大小
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "($label)",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text("关闭")
                }
            }
        )
    }
}