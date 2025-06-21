package com.example.nknote.data

import android.graphics.Bitmap
import android.graphics.Bitmap.CompressFormat
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.Date
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi


object DataHandler {
    @OptIn(ExperimentalEncodingApi::class)
    fun bitmapToString(bitmap: Bitmap): String {
        //将Bitmap转换成字符串
        var string: String? = null
        val bStream = ByteArrayOutputStream()
        bitmap.compress(CompressFormat.PNG, 100, bStream)
        val bytes = bStream.toByteArray()
        string = Base64.encode(bytes)
        return string
    }

     fun stringWithMD5(content : String) :String
    {
        val md = MessageDigest.getInstance("MD5")
        md.update(content.toByteArray())
        val hash = md.digest()
        val hex = StringBuilder(hash.size * 2 )
        for(b in hash)
        {
            var str = Integer.toHexString(b.toInt())
            if(b<0x10) {
                str = "0$str"
            }
            hex.append(str.substring(str.length-2))
        }
        return hex.toString()
    }

}