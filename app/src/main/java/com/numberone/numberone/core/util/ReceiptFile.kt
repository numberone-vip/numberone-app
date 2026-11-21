package com.numberone.numberone.core.util

import com.numberone.numberone.core.localization.tr
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.media.ExifInterface
import com.numberone.numberone.core.error.UserFacingException
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import kotlin.math.max

/**
 * A payment receipt image read from the device, already validated and (when needed) optimized.
 *
 * [mimeType] is always one of image/jpeg, image/png, image/webp: it is detected from the file's
 * real bytes, never from the picker, so HEIC/odd providers can no longer cause "unsupported format".
 */
class ReceiptFile(
    val bytes: ByteArray,
    val mimeType: String,
    val extension: String,
    val width: Int = 0,
    val height: Int = 0,
    /** True when the original was converted/compressed to fit the upload limit. */
    val optimized: Boolean = false
)

private const val RECEIPT_MAX_BYTES = 5 * 1024 * 1024        // server-side limit
private const val RECEIPT_TARGET_BYTES = 4 * 1024 * 1024     // what we aim for when compressing
private const val RECEIPT_MAX_INPUT_BYTES = 40 * 1024 * 1024 // refuse absurd inputs before reading them
private const val RECEIPT_MAX_EDGE = 2400                    // longest side after downscaling

/**
 * Reads a picked image. Blocking: call it from a background dispatcher.
 *
 * - Valid JPEG/PNG/WEBP up to 5 MB is uploaded untouched (full detail for the admin).
 * - Anything else that Android can decode (HEIC, GIF, BMP, huge screenshots...) is converted to JPEG
 *   and shrunk until it fits, instead of being rejected.
 */
fun readReceiptFile(context: Context, uri: Uri): ReceiptFile {
    val raw = readAllBytes(context, uri)

    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    BitmapFactory.decodeByteArray(raw, 0, raw.size, bounds)
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
        throw UserFacingException(tr("الملف المختار ليس صورة قابلة للقراءة. اختر لقطة شاشة للإيصال (JPG أو PNG)."))
    }

    val mime = sniffMime(raw)
    if (mime != null && raw.size <= RECEIPT_MAX_BYTES) {
        return ReceiptFile(raw, mime, extensionFor(mime), bounds.outWidth, bounds.outHeight, optimized = false)
    }
    return optimize(raw, bounds.outWidth, bounds.outHeight)
}

private fun readAllBytes(context: Context, uri: Uri): ByteArray {
    val bytes = context.contentResolver.openInputStream(uri)?.use { input ->
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(16 * 1024)
        var total = 0
        while (true) {
            val read = input.read(buffer)
            if (read < 0) break
            total += read
            if (total > RECEIPT_MAX_INPUT_BYTES) {
                throw UserFacingException(tr("الصورة كبيرة جداً. اختر لقطة شاشة أصغر للإيصال."))
            }
            out.write(buffer, 0, read)
        }
        out.toByteArray()
    } ?: throw UserFacingException(tr("تعذر قراءة ملف الإيصال. حاول اختيار الصورة مرة أخرى."))
    if (bytes.isEmpty()) throw UserFacingException(tr("ملف الإيصال فارغ"))
    return bytes
}

private fun optimize(raw: ByteArray, srcW: Int, srcH: Int): ReceiptFile {
    var edge = RECEIPT_MAX_EDGE
    var attempt = 0
    while (attempt < 4) {
        val bitmap = decodeScaled(raw, srcW, srcH, edge)
            ?: throw UserFacingException(tr("تعذر قراءة الصورة. جرّب صيغة JPG أو PNG."))
        try {
            var quality = 88
            while (quality >= 55) {
                val out = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, out)
                if (out.size() <= RECEIPT_TARGET_BYTES) {
                    return ReceiptFile(out.toByteArray(), "image/jpeg", "jpg", bitmap.width, bitmap.height, optimized = true)
                }
                quality -= 11
            }
        } finally {
            bitmap.recycle()
        }
        edge = (edge * 0.75f).toInt()
        attempt++
    }
    throw UserFacingException(tr("تعذر تصغير الصورة إلى الحجم المسموح (5 ميغابايت). اختر لقطة شاشة أخرى."))
}

/** Decodes with sub-sampling, applies EXIF rotation, flattens transparency onto white and caps the longest side. */
private fun decodeScaled(raw: ByteArray, srcW: Int, srcH: Int, maxEdge: Int): Bitmap? {
    var sample = 1
    while (max(srcW, srcH) / (sample * 2) >= maxEdge) sample *= 2
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val decoded: Bitmap = (try {
        BitmapFactory.decodeByteArray(raw, 0, raw.size, opts)
    } catch (e: OutOfMemoryError) {
        null
    }) ?: return null

    val rotation = runCatching {
        val exif = ExifInterface(ByteArrayInputStream(raw))
        when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
    }.getOrDefault(0f)

    val longest = max(decoded.width, decoded.height)
    val scale = if (longest > maxEdge) maxEdge.toFloat() / longest else 1f
    val matrix = Matrix().apply {
        if (rotation != 0f) postRotate(rotation)
        if (scale != 1f) postScale(scale, scale)
    }
    val transformed = if (rotation != 0f || scale != 1f) {
        Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
    } else decoded
    if (transformed !== decoded) decoded.recycle()

    // JPEG has no alpha: paint on white so transparent PNG/WEBP areas don't turn black.
    val flat = Bitmap.createBitmap(transformed.width, transformed.height, Bitmap.Config.ARGB_8888)
    Canvas(flat).apply {
        drawColor(Color.WHITE)
        drawBitmap(transformed, 0f, 0f, null)
    }
    transformed.recycle()
    return flat
}

private fun sniffMime(b: ByteArray): String? = when {
    b.size > 3 && b[0] == 0xFF.toByte() && b[1] == 0xD8.toByte() && b[2] == 0xFF.toByte() -> "image/jpeg"
    b.size > 8 && b[0] == 0x89.toByte() && b[1] == 0x50.toByte() && b[2] == 0x4E.toByte() && b[3] == 0x47.toByte() -> "image/png"
    b.size > 12 && String(b, 0, 4, Charsets.ISO_8859_1) == "RIFF" && String(b, 8, 4, Charsets.ISO_8859_1) == "WEBP" -> "image/webp"
    else -> null
}

private fun extensionFor(mime: String): String = when (mime) {
    "image/png" -> "png"
    "image/webp" -> "webp"
    else -> "jpg"
}

/** Human-friendly size such as "340 KB" or "1.2 MB". */
fun formatFileSize(bytes: Int): String =
    if (bytes >= 1024 * 1024) String.format(java.util.Locale.US, "%.1f MB", bytes / 1024f / 1024f)
    else "${max(1, bytes / 1024)} KB"
