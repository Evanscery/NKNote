package io.github.nknote.data.image

import android.net.Uri

/**
 * Stores diary images as compressed WebP files on disk. No external libraries.
 *
 * Strategy:
 *  - Decode the source with `BitmapFactory.Options.inSampleSize` to bound max dimension.
 *  - Re-encode to WebP into a per-note folder `images/<noteId>/<sha256>.webp`.
 *  - Return the absolute path, which is stored inline in the note's RichDocument content.
 *
 * This keeps the database tiny (paths only) and images small, all with platform APIs.
 *
 * Android implementation: [AndroidImageStore]. Other platforms (JVM/desktop) can supply
 * their own `actual` implementation without touching the repository or UI layers — this
 * seam exists to keep the future KMP migration open (see docs/multiplatform.md).
 *
 * Known limitation (absolute paths): stored paths are absolute (`filesDir/images/<id>/...`).
 * On app reinstall or device migration the absolute path changes, so references inside old
 * notes break. This is documented for a future fix (todo 13 of nknote-polish) — out of scope
 * here. Do NOT add a content-URI resolver or refcount scheme in this seam.
 */
interface ImageStore {

    /** Compress [source] to WebP under `images/<noteId>/`; return the absolute path, or null on failure. */
    fun saveForNote(noteId: Int, source: Uri): String?

    /** Delete the file at [path] if it exists. */
    fun delete(path: String)

    /** Delete the whole per-note image folder for [noteId]. */
    fun deleteAllForNote(noteId: Int)

    /** Whether the file at [path] exists on disk. */
    fun exists(path: String): Boolean
}
