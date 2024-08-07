package com.example.nknote.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "note",
    indices = [Index(value = ["id"], unique = true)]
)
data class NoteItem(
    @PrimaryKey
    val id : String = "",

    val title : String = "",
    val description : String= "",
    val textHtml : String= "",
    val date : String= "",
    val cover : String= "",
    //Json string, list of key value pair {uri,bitmap}
    val picture : String= "" //like [{0,{"uri":"bitmap"}},{1,{"uri":"bitmap"}}]
)
