package com.example.nknote.ui.pages

import ItemsRepository
import android.graphics.BitmapFactory
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import com.example.nknote.data.NoteItem
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import java.io.FileInputStream
import java.lang.reflect.Type
import java.security.MessageDigest


class NoteEditViewModel(private val itemsRepository: ItemsRepository): ViewModel() {
    var noteItemUiState by mutableStateOf(NoteItemUiState())
        private  set

    fun updateUiState(itemDetails : NoteItemDetails)
    {
        noteItemUiState = NoteItemUiState(noteItemDetails = itemDetails)
    }

    suspend fun insertNote() {
        itemsRepository.insertItem(noteItemUiState.noteItemDetails.toNoteItem())
    }

    suspend fun deleteById(id:Int) = itemsRepository.deleteById(id)

}

data class NoteItemUiState(
    val noteItemDetails : NoteItemDetails = NoteItemDetails(),
    )
data class NoteItemDetails(
    val id : Int = 0,
    val title : String = "无标题",
    val description : String = "这篇笔记没有描述",
    val textHtml : String = "",
    val date : String = "",
    val cover : String = "",
    val picture : Map<String,String> = mapOf("" to "")
)

fun NoteItemDetails.toNoteItem(redundantId : String = ""): NoteItem = NoteItem(
    id = id,
    title = title,
    description = description,
    textHtml = textHtml,
    date = date,
    cover = cover,
    picture = pictureToJson()
)

fun NoteItemDetails.pictureToJson() : String
{
    val gson = Gson()
    return gson.toJson(picture).toString()
}

fun NoteItem.toNoteItemDetails():NoteItemDetails  {
    val gson = Gson()
    val type: Type = object : TypeToken<Map<String?, String?>?>() {}.type
    return NoteItemDetails(
        id = id,
        title = title,
        description = description,
        textHtml = textHtml,
        date = date,
        cover = cover,
        picture = gson.fromJson(picture,type)
    )
}

