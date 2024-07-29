package com.example.nknote.data

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import java.io.ByteArrayOutputStream
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


object DataHandler {
    @OptIn(ExperimentalEncodingApi::class)
    fun bitmapToString(bitmap: Bitmap): String? {
        //将Bitmap转换成字符串
        var string: String? = null
        val bStream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, bStream)
        val bytes = bStream.toByteArray()
        string = Base64.encode(bytes)
        return string
    }
}