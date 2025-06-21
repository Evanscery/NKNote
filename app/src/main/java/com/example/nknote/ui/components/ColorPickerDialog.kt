package com.example.nknote.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog

@Composable
fun ColorPickerDialog(
    showDialog: Boolean,
    onDismiss: () -> Unit,
    onColorSelected: (Color) -> Unit
) {
    if (showDialog) {
        var showRgbPicker by remember { mutableStateOf(false) }
        var selectedColor by remember { mutableStateOf(Color.Black) }

        Dialog(onDismissRequest = onDismiss) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface
            ) {
                if (!showRgbPicker) {
                    // 预设颜色选择器
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "选择颜色",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        val colors = listOf(
                            Color.Black, Color.DarkGray, Color.Gray, Color.Red,
                            Color.Green, Color.Blue, Color.Yellow, Color.Magenta,
                            Color.Cyan, Color.White
                        )

                        LazyVerticalGrid(
                            columns = GridCells.Fixed(5),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(colors) { color ->
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .background(color, CircleShape)
                                        .border(1.dp, Color.Gray, CircleShape)
                                        .clickable {
                                            onColorSelected(color)
                                            onDismiss()
                                        }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(onClick = { showRgbPicker = true }) {
                            Text("自定义颜色")
                        }
                    }
                } else {
                    // RGB颜色选择器
                    var red by remember { mutableStateOf(0f) }
                    var green by remember { mutableStateOf(0f) }
                    var blue by remember { mutableStateOf(0f) }

                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "自定义颜色",
                            style = MaterialTheme.typography.titleMedium
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // 颜色预览
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .background(Color(red, green, blue))
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // RGB滑块
                        Text("红色: ${(red * 255).toInt()}")
                        Slider(
                            value = red,
                            onValueChange = { red = it },
                            colors = SliderDefaults.colors(thumbColor = Color.Red)
                        )

                        Text("绿色: ${(green * 255).toInt()}")
                        Slider(
                            value = green,
                            onValueChange = { green = it },
                            colors = SliderDefaults.colors(thumbColor = Color.Green)
                        )

                        Text("蓝色: ${(blue * 255).toInt()}")
                        Slider(
                            value = blue,
                            onValueChange = { blue = it },
                            colors = SliderDefaults.colors(thumbColor = Color.Blue)
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            TextButton(onClick = { showRgbPicker = false }) {
                                Text("返回")
                            }
                            Row {
                                TextButton(onClick = onDismiss) {
                                    Text("取消")
                                }
                                TextButton(
                                    onClick = {
                                        onColorSelected(Color(red, green, blue))
                                        onDismiss()
                                    }
                                ) {
                                    Text("确定")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}