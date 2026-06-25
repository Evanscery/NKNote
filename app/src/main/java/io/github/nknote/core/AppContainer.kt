package io.github.nknote.core

import android.content.Context
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.data.repository.NoteRepositoryImpl
import io.github.nknote.data.sync.NoopSyncEngine
import io.github.nknote.data.sync.SyncEngine

/**
 * Manual dependency container. Lightweight — no DI framework, to honor the "lightweight" constraint.
 * Created once in [io.github.nknote.NkNoteApplication] and shared across the process.
 */
class AppContainer(private val context: Context) {

    private val database: NkNoteDatabase by lazy { NkNoteDatabase.get(context) }

    val imageStore: ImageStore by lazy { ImageStore(context) }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao(), database.tagDao(), database.noteTagDao())
    }

    /**
     * Sync engine. Default is [NoopSyncEngine]; a future netdisk-API or LAN-sync implementation
     * plugs in here without touching the UI / repository layer. See docs/multiplatform.md.
     */
    val syncEngine: SyncEngine by lazy { NoopSyncEngine }
}