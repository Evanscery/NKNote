package com.example.nknote.data

import android.content.Context
import com.example.nknote.data.dao.*
import com.example.nknote.data.repository.NoteRepository
import com.example.nknote.data.repository.NoteRepositoryImpl

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val noteRepository: NoteRepository
}

/**
 * [AppContainer] implementation that provides instance of [OfflineItemsRepository]
 */
class AppDataContainer(private val context: Context) : AppContainer {
    private val database = NoteItemRoomDatabase.getDatabaseObj(context)

    /**
     * Implementation for [ItemsRepository]
     */
    override val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(
            noteDao = database.noteDao(),
            tagDao = database.tagDao(),
            noteTagDao = database.noteTagDao(),
            imageDao = database.imageDao(),
            syncRecordDao = database.syncRecordDao()
        )
    }
}