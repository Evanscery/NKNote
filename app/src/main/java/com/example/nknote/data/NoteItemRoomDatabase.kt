package com.example.nknote.data

import android.content.Context
import android.provider.ContactsContract.CommonDataKinds.Note
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [NoteItem::class],version = 1, exportSchema = false)
abstract class NoteItemRoomDatabase : RoomDatabase() {
    abstract fun itemDao(): NoteItemDao

    companion object{
        private var INSTANCE: NoteItemRoomDatabase? = null
        fun getDatabaseObj(context: Context): NoteItemRoomDatabase {
            return INSTANCE ?: synchronized(this){
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteItemRoomDatabase::class.java,
                    "note_item_database"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}