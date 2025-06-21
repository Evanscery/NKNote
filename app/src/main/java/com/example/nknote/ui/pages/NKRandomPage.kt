package com.example.nknote.ui.pages


import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nknote.ui.theme.NKNoteTheme
import kotlinx.coroutines.delay

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun NKRandomPage(
    onNavBack : ()->Unit = {}
) {
    var userInput : String by rememberSaveable{ mutableStateOf("")}
    var result : String by rememberSaveable {
        mutableStateOf("")
    }
    var finalResult: String by rememberSaveable { mutableStateOf("") }
    var randomStart : Boolean by rememberSaveable {
        mutableStateOf(false)
    }
    NKNoteTheme {

            LaunchedEffect(randomStart) {
                if(randomStart)
                {
                    val itemList = userInput.split("\n").filter { it.isNotBlank() }
                    val size = itemList.size-1
                    if (size >= 0) {
                        val baseDuration = 1200
                        val perItem = 300
                        val maxDuration = 6000
                        val durationMillis = (baseDuration + itemList.size * perItem).coerceAtMost(maxDuration)
                        val endTime = System.currentTimeMillis() + durationMillis
                        while (System.currentTimeMillis() < endTime) {
                            val delayTime = (80..180).random()
                            delay(delayTime.toLong())
                            val randomIndex = (0..size).random()
                            result = itemList[randomIndex]
                        }
                        finalResult = result
                    } else {
                        finalResult = ""
                    }
                    randomStart = false
                }
            }

                Surface(color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()) {
            Scaffold(
                topBar = {
                    CenterAlignedTopAppBar(
                        title = { Text("随机抽取", style = MaterialTheme.typography.titleLarge) },
                        navigationIcon = {
                            IconButton(onClick = { onNavBack() }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = null
                                )
                            }
                        }
                    )
                },
                bottomBar = {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Button(
                            onClick = { randomStart = true },
                            enabled = !randomStart && userInput.lines().any { it.isNotBlank() },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "开始抽取",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                userInput = ""
                                result = ""
                            },
                            enabled = !randomStart,
                            modifier = Modifier
                                .defaultMinSize(minWidth = 80.dp)
                                .height(48.dp)
                        ) {
                            Text(
                                text = "清空",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(innerPadding)
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "结果是：${if (!randomStart) finalResult else result}",
                        style = MaterialTheme.typography.headlineMedium,
                        modifier = Modifier.padding(bottom = 32.dp)
                    )
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        placeholder = { Text("请输入选项，每行一个") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 300.dp)
                    )
                }
            }
        }

    }

}

@Preview
@Composable
fun NKRandomPagePreview() {
    NKRandomPage()
}

