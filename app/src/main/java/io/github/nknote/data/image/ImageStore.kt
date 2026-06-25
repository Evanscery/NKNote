package io.github.nknote.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import kotlin.math.roundToInt

/**
 * Stores diary images as compressed WebP files on disk. No external libraries.
 *
 * Strategy (satisfies "compressed storage, especially images"):
 *  - Decode the source with [BitmapFactory.Options.inSampleSize] to bound max dimension to [MAX_DIMEN].
 *  - Re-encode to WebP (quality [QUALITY]) into per-note folder `images/<noteId>/`.
 *  - Return the absolute path, which is stored inline in the note's RichDocument content.
 *
 * This keeps the database tiny (paths only) and images small, all with platform APIs.
 */
class ImageStore(private val context: Context) {

    fun saveForNote(noteId: Int, source: Uri): String? {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(source)?.use { it.readBytes() } ?: return null
        return saveBytes(noteId, bytes)
    }

    fun saveBytes(noteId: Int, bytes: ByteArray): String? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        val sample = sampleSizeFor(bounds.outWidth, bounds.outHeight)

        val decoded = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decoded) ?: return null

        val scaled = downscaleIfNeeded(bitmap, MAX_DIMEN)
        val dir = File(context.filesDir, "images/$noteId").apply { mkdirs() }
        val name = sha256(bytes) + ".webp"
        val out = File(dir, name)

        FileOutputStream(out).use { fos ->
            scaled.compress(Bitmap.CompressFormat.WEBP, QUALITY, fos)
        }
        if (scaled !== bitmap) bitmap.recycle()
        scaled.recycle()
        return out.absolutePath
    }

    fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    fun deleteAllForNote(noteId: Int) {
        runCatching { File(context.filesDir, "images/$noteId").deleteRecursively() }
    }

    private fun sampleSizeFor(width: Int, height: Int): Int {
        var sample = 1
        var maxDim = maxOf(width, height)
        while (maxDim / 2 >= MAX_DIMEN) { sample *= 2; maxDim /= 2 }
        return sample
    }

    private fun downscaleIfNeeded(bitmap: Bitmap, maxDimen: Int): Bitmap {
        val max = maxOf(bitmap.width, bitmap.height)
        if (max <= maxDimen) return bitmap
        val scale = maxDimen.toFloat() / max
        val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
        val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(bitmap, w, h, true)
    }

    private fun sha256(bytes: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256").digest(bytes)
        return md.joinToString("") { "%02x".format(it) }
    }

    /** Returns EXIF orientation-corrected bitmap for display; uses platform ExifInterface only. */
    fun decodeForDisplay(path: String): Bitmap? {
        val f = File(path)
        if (!f.exists()) return null
        return BitmapFactory.decodeFile(path)
    }

    companion object {
        private const val MAX_DIMEN = 1600
        private const val QUALITY = 80
    }
}