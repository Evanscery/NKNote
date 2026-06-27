package io.github.nknote.ui.viewer

import android.app.Application
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import io.github.nknote.data.image.ImageStore
import java.io.File

/**
 * Backs the image viewer. Owns the image file's lifecycle (delete) and builds share intents via
 * [FileProvider] using the [Application] context (no raw [android.content.Context] is passed in —
 * [AndroidViewModel] holds the Application, keeping the VM free of Compose-side DI).
 *
 * The matching `<provider>` entry in `AndroidManifest.xml` + `res/xml/file_paths.xml` are wired in
 * todo 8 (image-viewer completion); [buildShareIntent] returns `null` if the provider isn't registered
 * yet, so the UI degrades gracefully (toast) instead of crashing.
 */
class ImageViewerViewModel(
    application: Application,
    private val imageStore: ImageStore
) : AndroidViewModel(application) {

    companion object {
        /** FileProvider authority. Wired in the manifest by todo 8. */
        const val FILE_PROVIDER_AUTHORITY = "io.github.nknote.fileprovider"
    }

    /** Delete the image file at [path] from disk. Swallows failures (file may already be gone). */
    fun delete(path: String) {
        runCatching { imageStore.delete(path) }
    }

    /**
     * Build an `ACTION_SEND` intent carrying the image at [path] via a `FileProvider` content URI,
     * with `FLAG_GRANT_READ_URI_PERMISSION` so the receiving app can read it. Returns `null` if the
     * file is missing or the provider isn't configured (caller shows a toast instead of crashing).
     */
    fun buildShareIntent(path: String): Intent? = runCatching {
        val file = File(path)
        if (!file.exists()) return null
        val uri = FileProvider.getUriForFile(getApplication(), FILE_PROVIDER_AUTHORITY, file)
        Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }.getOrNull()
}