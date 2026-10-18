package vn.ghichunhanh.app

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.compose.runtime.mutableStateMapOf
import androidx.exifinterface.media.ExifInterface
import java.io.File

/**
 * Ảnh đính kèm của từng ghi chú: files/attachments/<id ghi chú>/<thời điểm>.jpg
 * Ảnh được thu nhỏ (cạnh dài tối đa 2048px, JPEG 85%) để nhẹ máy và file sao lưu.
 */
class AttachmentsStore private constructor(private val appContext: Context) {
    private val root = File(appContext.filesDir, "attachments")
    private val cache = mutableStateMapOf<Long, List<File>>()

    private fun dir(noteId: Long) = File(root, noteId.toString())

    private fun scan(noteId: Long): List<File> =
        dir(noteId).listFiles { f -> f.isFile && !f.name.startsWith(".") }?.sortedBy { it.name } ?: emptyList()

    /** Ảnh, video, ghi âm và tệp của ghi chú, theo thứ tự thêm vào. */
    fun list(noteId: Long): List<File> = cache.getOrPut(noteId) { scan(noteId) }

    fun refresh(noteId: Long) {
        cache[noteId] = scan(noteId)
    }

    /** File mới cho bản ghi âm (MediaRecorder ghi thẳng vào đây). */
    fun newRecordingFile(noteId: Long): File {
        val d = dir(noteId).apply { mkdirs() }
        return File(d, "${System.currentTimeMillis()}.m4a")
    }

    /** File mới cho video quay bằng máy ảnh (ghi thẳng vào thư mục của ghi chú). */
    fun newVideoFile(noteId: Long): File {
        val d = dir(noteId).apply { mkdirs() }
        return File(d, "${System.currentTimeMillis()}.mp4")
    }

    /**
     * Đính kèm nguyên tệp (video, PDF, Word…): chép vào thư mục ghi chú với tên "<thời điểm>__<tên gốc>".
     * Ảnh chọn từ thư viện vẫn đi qua [add] để được thu nhỏ.
     */
    fun addFile(noteId: Long, source: Uri, displayName: String?): File? = runCatching {
        val d = dir(noteId).apply { mkdirs() }
        val base = sanitize(displayName ?: "file").ifEmpty { "file" }
        val out = File(d, "${System.currentTimeMillis()}__$base")
        appContext.contentResolver.openInputStream(source)?.use { input -> out.outputStream().use { input.copyTo(it) } } ?: return null
        refresh(noteId)
        out
    }.getOrNull()

    /** File tạm cho máy ảnh ghi ảnh vào (chia sẻ qua FileProvider). */
    fun newCameraFile(): File {
        val d = File(appContext.cacheDir, "camera").apply { mkdirs() }
        return File(d, "shot-${System.currentTimeMillis()}.jpg")
    }

    /** Thêm ảnh từ máy ảnh / thư viện; trả về file đã lưu hoặc null nếu lỗi. */
    fun add(noteId: Long, source: Uri): File? = runCatching {
        val bytes = appContext.contentResolver.openInputStream(source)?.use { it.readBytes() } ?: return null
        addBytes(noteId, bytes)
    }.getOrNull()

    fun addBytes(noteId: Long, bytes: ByteArray, name: String? = null): File? = runCatching {
        val d = dir(noteId).apply { mkdirs() }
        val out = File(d, name ?: "${System.currentTimeMillis()}.jpg")
        val bmp = decodeScaled(bytes, 2048) ?: return null
        out.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 85, it) }
        bmp.recycle()
        refresh(noteId)
        out
    }.getOrNull()

    /** Đưa file tạm (giải nén từ sao lưu) vào ghi chú. */
    fun putFile(noteId: Long, name: String, src: File) {
        val d = dir(noteId).apply { mkdirs() }
        val safe = sanitize(name.substringAfterLast('/'))
        if (safe.isEmpty() || safe.startsWith(".")) return
        val dst = File(d, safe)
        if (!src.renameTo(dst)) { src.copyTo(dst, overwrite = true); src.delete() }
        refresh(noteId)
    }

    /** Chép nguyên file (dùng khi khôi phục sao lưu). */
    fun putRaw(noteId: Long, name: String, bytes: ByteArray) {
        val d = dir(noteId).apply { mkdirs() }
        val safe = sanitize(name.substringAfterLast('/'))
        if (safe.isEmpty() || safe.startsWith(".")) return
        File(d, safe).writeBytes(bytes)
        refresh(noteId)
    }

    fun delete(noteId: Long, file: File) {
        file.delete()
        refresh(noteId)
    }

    fun deleteAll(noteId: Long) {
        dir(noteId).deleteRecursively()
        cache.remove(noteId)
    }

    /** Toàn bộ ảnh: (id ghi chú, file) — để sao lưu. */
    fun all(): List<Pair<Long, File>> =
        root.listFiles()?.flatMap { d ->
            val id = d.name.toLongOrNull() ?: return@flatMap emptyList()
            d.listFiles { f -> f.isFile && !f.name.startsWith(".") }?.map { id to it } ?: emptyList()
        } ?: emptyList()

    private fun decodeScaled(bytes: ByteArray, maxSide: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxSide) sample *= 2
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, BitmapFactory.Options().apply { inSampleSize = sample })
            ?: return null
        // Xoay đúng chiều theo EXIF (ảnh chụp dọc không bị nằm ngang)
        val rotation = runCatching {
            when (ExifInterface(bytes.inputStream()).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
        }.getOrDefault(0f)
        val scale = minOf(1f, maxSide.toFloat() / maxOf(bmp.width, bmp.height))
        if (rotation == 0f && scale == 1f) return bmp
        val m = Matrix().apply { postScale(scale, scale); postRotate(rotation) }
        return Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, m, true).also { if (it !== bmp) bmp.recycle() }
    }

    companion object {
        private val IMAGE = setOf("jpg", "jpeg", "png", "webp", "gif", "heic")
        private val VIDEO = setOf("mp4", "3gp", "mov", "webm", "mkv", "m4v")
        private val AUDIO = setOf("m4a", "mp3", "aac", "wav", "ogg", "amr", "flac")
        fun isImage(f: File) = f.extension.lowercase() in IMAGE
        fun isVideo(f: File) = f.extension.lowercase() in VIDEO
        fun isAudio(f: File) = f.extension.lowercase() in AUDIO
        fun isOther(f: File) = !isImage(f) && !isVideo(f) && !isAudio(f)

        /** Tên hiển thị: bỏ tiền tố "<thời điểm>__". */
        fun displayName(f: File): String = f.name.substringAfter("__", f.name)

        fun mimeOf(f: File): String =
            android.webkit.MimeTypeMap.getSingleton().getMimeTypeFromExtension(f.extension.lowercase()) ?: "application/octet-stream"

        /** Giữ chữ (mọi ngôn ngữ), số, dấu cách, . - _ ( ); tối đa 80 ký tự. */
        fun sanitize(name: String): String =
            name.filter { it.isLetterOrDigit() || it in " .-_()" }.trim().take(80)

        /** Ảnh đầu của video để làm hình thu nhỏ. */
        fun videoFrame(f: File): Bitmap? = runCatching {
            android.media.MediaMetadataRetriever().run {
                setDataSource(f.path)
                val b = getFrameAtTime(0)
                release()
                b
            }
        }.getOrNull()

        @Volatile private var instance: AttachmentsStore? = null
        fun get(context: Context): AttachmentsStore =
            instance ?: synchronized(this) {
                instance ?: AttachmentsStore(context.applicationContext).also { instance = it }
            }

        /** Ảnh thu nhỏ để hiện trong dải ảnh (nhanh, ít bộ nhớ). */
        fun thumbnail(file: File, size: Int): Bitmap? = runCatching {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, bounds)
            var sample = 1
            while (minOf(bounds.outWidth, bounds.outHeight) / (sample * 2) >= size) sample *= 2
            BitmapFactory.decodeFile(file.path, BitmapFactory.Options().apply { inSampleSize = sample })
        }.getOrNull()
    }
}
