package com.example.nknote.data

import android.provider.ContactsContract.CommonDataKinds.Note
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteItemDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(item:NoteItem)

    @Update
    suspend fun update(item : NoteItem)

    @Delete
    suspend fun delete(item:NoteItem)

    @Query("SELECT * FROM note ORDER BY id;")
    fun getAllItems() : Flow<List<NoteItem>>

    @Query("SELECT * from note WHERE id = :id")
    fun getItem(id: Int): Flow<NoteItem>

    @Query("SELECT picture from note WHERE id = :id")
    fun getPicture(id: Int): Flow<String>

    @Query("DELETE FROM note WHERE id = :id")
    suspend fun deleteById(id: Int)


}