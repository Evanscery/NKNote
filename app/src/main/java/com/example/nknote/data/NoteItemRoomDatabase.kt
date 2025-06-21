package com.example.nknote.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.nknote.data.dao.*
import com.example.nknote.data.entities.*

@Database(
    entities = [
        Note::class,
        Tag::class,
        NoteTag::class,
        Image::class,
        SyncRecord::class
    ],
    version = 2,
    exportSchema = false
)
abstract class NoteItemRoomDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao
    abstract fun imageDao(): ImageDao
    abstract fun syncRecordDao(): SyncRecordDao

    companion object {
        private var INSTANCE: NoteItemRoomDatabase? = null

        fun getDatabaseObj(context: Context): NoteItemRoomDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NoteItemRoomDatabase::class.java,
                    "note_database"
                )
                .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}