package io.github.nknote.core

import android.content.Context
import io.github.nknote.data.db.NkNoteDatabase
import io.github.nknote.data.image.AndroidImageStore
import io.github.nknote.data.image.ImageStore
import io.github.nknote.data.repository.NoteRepository
import io.github.nknote.data.repository.NoteRepositoryImpl
import io.github.nknote.data.sync.NoopSyncEngine
import io.github.nknote.data.sync.SyncEngine
import kotlinx.coroutines.flow.MutableStateFlow

/** Theme mode the user can pick in Settings. `SYSTEM` follows the platform dark setting. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/**
 * Manual dependency container. Lightweight — no DI framework, to honor the "lightweight" constraint.
 * Created once in [io.github.nknote.NkNoteApplication] and shared across the process.
 */
class AppContainer(private val context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /**
     * Theme mode as the single source of truth. Initialized synchronously from [SharedPreferences]
     * in this constructor (which runs during [io.github.nknote.NkNoteApplication.onCreate], before
     * the first Compose tree is built) so there is no theme flash on cold start. Settings reads and
     * writes this flow; [io.github.nknote.ui.navigation.NkNoteApp] collects it and resolves the
     * dark flag for [io.github.nknote.ui.theme.NkNoteTheme].
     */
    val themeMode: MutableStateFlow<ThemeMode> = MutableStateFlow(
        runCatching {
            ThemeMode.valueOf(prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
        }.getOrDefault(ThemeMode.SYSTEM)
    )

    /** Persist [mode] to [SharedPreferences] and publish it to [themeMode]. */
    fun setThemeMode(mode: ThemeMode) {
        prefs.edit().putString(KEY_THEME_MODE, mode.name).apply()
        themeMode.value = mode
    }

    private val database: NkNoteDatabase by lazy { NkNoteDatabase.get(context) }

    val imageStore: ImageStore by lazy { AndroidImageStore(context) }

    val noteRepository: NoteRepository by lazy {
        NoteRepositoryImpl(database.noteDao(), database.tagDao(), database.noteTagDao(), imageStore)
    }

    /**
     * Sync engine. Default is [NoopSyncEngine]; a future netdisk-API or LAN-sync implementation
     * plugs in here without touching the UI / repository layer. See docs/multiplatform.md.
     */
    val syncEngine: SyncEngine by lazy { NoopSyncEngine }

    companion object {
        private const val PREFS_NAME = "nknote_prefs"
        private const val KEY_THEME_MODE = "theme_mode"
    }
}