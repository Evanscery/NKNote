package io.github.nknote.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.nknote.data.entity.Note
import io.github.nknote.data.entity.NoteTag
import io.github.nknote.data.entity.Tag

@Database(
    entities = [Note::class, Tag::class, NoteTag::class],
    version = 1,
    exportSchema = false
)
abstract class NkNoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun tagDao(): TagDao
    abstract fun noteTagDao(): NoteTagDao

    companion object {
        @Volatile
        private var instance: NkNoteDatabase? = null

        fun get(context: Context): NkNoteDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    NkNoteDatabase::class.java,
                    "nknote.db"
                ).fallbackToDestructiveMigration().build().also { instance = it }
            }
    }
}