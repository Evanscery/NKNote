package com.example.nknote.ui.pages


import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.nknote.ui.theme.NKNoteTheme

@Composable
fun NKRandomPage(
    onNavBack : ()->Unit = {}
) {
    var userInput : String by rememberSaveable{ mutableStateOf("")}
    var randomResult : Int by rememberSaveable {
        mutableStateOf(0)
    }
    NKNoteTheme {
        Surface(color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.fillMaxSize()) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding()
                    .padding(20.dp)
            ){
                Row(
                    horizontalArrangement = Arrangement.Start,
                    modifier = Modifier.fillMaxWidth()
                ){
                    IconButton(onClick = { onNavBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = null
                        )
                    }
                }
                Spacer(modifier = Modifier.padding(50.dp))
                Text(text = "结果是："+if(randomResult==0){""}else{randomResult.toString()},
                    style = MaterialTheme.typography.headlineMedium,
                    )
                Spacer(modifier = Modifier.padding(50.dp))
                TextField(value = userInput,
                    onValueChange = {
                        userInput = it.filter { symbol ->
                            symbol.isDigit()
                    }
                },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    placeholder = {
                        Text(text="输入最大数字")
                    })
                Spacer(modifier = Modifier.padding(20.dp))
                Button(onClick = {
                        val maxR : Int = if(userInput.isNullOrEmpty()){1}else{userInput.toInt()}
                        randomResult = (1..maxR).random()
                },

                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .fillMaxHeight(0.2f)) {
                    Text(text = "生成随机数",style = MaterialTheme.typography.headlineSmall)
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

