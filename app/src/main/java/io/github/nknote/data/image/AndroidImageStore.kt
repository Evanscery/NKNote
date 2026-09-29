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
 * Android implementation of [ImageStore]. Compresses picked images to WebP on disk via
 * platform `BitmapFactory` + `Bitmap.compress` — no external compressor library.
 *
 * Per-note folder layout: `filesDir/images/<noteId>/<sha256>.webp`. Cross-note dedup is
 * intentionally NOT done (simpler than refcounting); see the interface doc for the
 * absolute-path-reinstall fragility, tracked in todo 13.
 */
class AndroidImageStore(private val context: Context) : ImageStore {

    override fun saveForNote(noteId: Int, source: Uri): String? {
        val resolver = context.contentResolver
        val bytes = resolver.openInputStream(source)?.use { it.readBytes() } ?: return null
        return saveBytes(noteId, bytes)
    }

    private fun saveBytes(noteId: Int, bytes: ByteArray): String? {
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

    override fun delete(path: String) {
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    override fun deleteAllForNote(noteId: Int) {
        runCatching { File(context.filesDir, "images/$noteId").deleteRecursively() }
    }

    override fun exists(path: String): Boolean = File(path).exists()

    override fun relocateToNote(path: String, noteId: Int): String {
        val src = File(path)
        val targetDir = File(context.filesDir, "images/$noteId")
        if (src.parentFile == targetDir) return path
        if (!src.exists()) return path
        return runCatching {
            targetDir.mkdirs()
            val dst = File(targetDir, src.name)
            // renameTo is atomic on the same filesystem (both live under filesDir).
            if (src.renameTo(dst)) dst.absolutePath
            else {
                src.copyTo(dst, overwrite = true)
                src.delete()
                dst.absolutePath
            }
        }.getOrDefault(path)
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

    companion object {
        // Reconciled to q75 / 1600px (see plan todo 3). README update lands in todo 13.
        private const val MAX_DIMEN = 1600
        private const val QUALITY = 75
    }
}
