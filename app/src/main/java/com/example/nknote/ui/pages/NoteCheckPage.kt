package com.example.nknote.ui.pages


import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview

@Composable
fun NoteCheckPage() {
    Box(modifier = Modifier.fillMaxSize()) {
            Text(text="asd",
                textAlign = TextAlign.Center)
    }
}

@Preview
@Composable
fun NoteCheckPagePreview() {
    NoteCheckPage()
}

