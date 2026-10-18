package vn.ghichunhanh.app

import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/** Dữ liệu đọc được từ file sao lưu (hoặc từ file .txt rời). */
data class BackupData(
    val notes: List<Note>,
    val calcExpr: String? = null,
    val calcJustEvaluated: Boolean = false,
    val calcLastFull: String? = null,
    val monospace: Boolean? = null,
    val theme: String? = null,
    val language: String? = null,
    val appointmentsJson: String? = null,
    /** Ảnh, video, ghi âm, tệp: (id ghi chú cũ, tên file, file tạm đã giải nén). */
    val attachments: List<Triple<Long, String, java.io.File>> = emptyList(),
)

/**
 * Sao lưu & khôi phục bằng file .zip:
 *
 *   cyanote-backup-20261017-1015.zip
 *   ├── notes.json          ← dữ liệu đầy đủ để nhập lại (ghi chú, phép tính, cài đặt)
 *   └── notes/
 *       ├── 01 - Đi chợ.txt ← bản đọc được trên máy tính, mỗi ghi chú một file
 *       └── 02 - …
 *
 * Khi nhập: ưu tiên notes.json; nếu file .zip chỉ có các file .txt (ví dụ tự nén trên máy tính)
 * thì mỗi file .txt thành một ghi chú. Cũng nhận một file .txt rời.
 */
object Backup {
    const val FORMAT = "cyanote-backup-v1"
    private const val NOTES_ENTRY = "notes.json"
    private const val ATTACH_DIR = "attachments"
    private const val MAX_ENTRY_BYTES = 200L * 1024 * 1024 // chặn file nén "bom" hoặc quá lớn (video đính kèm)
    private const val MAX_ENTRIES = 5000

    /** Loại file cho bộ chọn file khi nhập. */
    val IMPORT_MIME_TYPES = arrayOf(
        "application/zip",
        "application/x-zip-compressed",
        "application/octet-stream",
        "text/plain",
    )

    fun suggestedFileName(now: LocalDateTime = LocalDateTime.now()): String =
        "cyanote-backup-" + now.format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmm")) + ".zip"

    /** Ghi file .zip. [json] lấy từ NotesStore.backupJson. Không đóng [out] hộ người gọi trước khi xong. */
    fun write(out: OutputStream, json: String, notes: List<Note>, untitled: String, files: List<Pair<Long, java.io.File>> = emptyList()) {
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(NOTES_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

            // Ảnh & ghi âm: attachments/<id ghi chú>/<tên file>
            for ((noteId, f) in files) {
                if (!f.isFile) continue
                zip.putNextEntry(ZipEntry("$ATTACH_DIR/$noteId/${f.name}"))
                f.inputStream().use { it.copyTo(zip) }
                zip.closeEntry()
            }

            val used = HashSet<String>()
            notes.forEachIndexed { i, n ->
                val base = safeFileName(n.displayTitle(untitled)).ifEmpty { "note" }
                var name = "%02d - %s".format(i + 1, base)
                var k = 2
                while (!used.add(name.lowercase())) name = "%02d - %s (%d)".format(i + 1, base, k++)
                zip.putNextEntry(ZipEntry("notes/$name.txt"))
                zip.write(n.content.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
    }

    /**
     * Đọc file người dùng chọn: .zip sao lưu, .zip chứa các file .txt, hoặc một file .txt.
     * [fileName] dùng làm tên ghi chú khi nhập file .txt rời.
     * Ném IOException nếu không đọc được hoặc không có ghi chú nào.
     */
    /** [tempDir]: nơi giải nén tạm ảnh/video (không nạp cả vào RAM, tránh tràn bộ nhớ với video lớn). */
    fun read(input: InputStream, fileName: String?, tempDir: java.io.File? = null): BackupData {
        tmp = tempDir
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        val isZip = b1 == 'P'.code && b2 == 'K'.code
        return if (isZip) readZip(buffered) else readPlainText(buffered, fileName)
    }

    private var tmp: java.io.File? = null

    /** Chép một mục trong zip ra file tạm, giới hạn dung lượng. */
    private fun extractToTemp(input: InputStream, name: String): java.io.File {
        val dir = (tmp ?: throw IOException("No temp dir")).apply { mkdirs() }
        val out = java.io.File.createTempFile("att", "_" + name.takeLast(40).filter { it.isLetterOrDigit() || it == '.' }, dir)
        var total = 0L
        val buf = ByteArray(64 * 1024)
        out.outputStream().use { o ->
            while (true) {
                val n = input.read(buf)
                if (n < 0) break
                total += n
                if (total > MAX_ENTRY_BYTES) { o.close(); out.delete(); throw IOException("Entry too large") }
                o.write(buf, 0, n)
            }
        }
        return out
    }

    private fun readZip(input: InputStream): BackupData {
        var json: String? = null
        val textNotes = mutableListOf<Note>()
        val files = mutableListOf<Triple<Long, String, java.io.File>>()
        var count = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > MAX_ENTRIES) throw IOException("Too many entries")
                if (entry.isDirectory) continue
                val path = entry.name.replace('\\', '/')
                val name = path.substringAfterLast('/')
                val parts = path.split('/')
                when {
                    parts.size == 3 && parts[0] == ATTACH_DIR && parts[1].toLongOrNull() != null && !name.startsWith(".") ->
                        if (tmp != null) files.add(Triple(parts[1].toLong(), name, extractToTemp(zip, name)))
                    name == NOTES_ENTRY && json == null ->
                        json = String(readLimited(zip), Charsets.UTF_8)
                    name.endsWith(".txt", ignoreCase = true) && !name.startsWith(".") -> {
                        val title = name.dropLast(4)
                            .replace(Regex("^\\d+ - "), "") // bỏ số thứ tự "01 - " do app tự thêm
                        val now = System.currentTimeMillis()
                        textNotes.add(Note(0L, title, String(readLimited(zip), Charsets.UTF_8), now))
                    }
                }
            }
        }
        val parsed = json?.let { runCatching { parseJson(it) }.getOrNull() }
        return when {
            parsed != null && parsed.notes.isNotEmpty() -> parsed.copy(attachments = files)
            textNotes.isNotEmpty() -> BackupData(textNotes)
            else -> throw IOException("No notes found")
        }
    }

    private fun readPlainText(input: InputStream, fileName: String?): BackupData {
        val text = String(readLimited(input), Charsets.UTF_8)
        if (text.isBlank()) throw IOException("Empty file")
        val title = fileName?.substringBeforeLast('.')?.trim().orEmpty()
        return BackupData(listOf(Note(0L, title, text, System.currentTimeMillis())))
    }

    /** Đọc notes.json (cùng định dạng file lưu trong máy, có thêm cài đặt). */
    fun parseJson(text: String): BackupData {
        val root = JSONObject(text)
        val arr = root.getJSONArray("notes")
        val notes = ArrayList<Note>(arr.length())
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            notes.add(
                Note(
                    id = o.optLong("id", 0L),
                    title = o.optString("title", ""),
                    content = o.optString("content", ""),
                    updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                )
            )
        }
        val calc = root.optJSONObject("calc")
        return BackupData(
            notes = notes,
            calcExpr = calc?.optString("expr", ""),
            calcJustEvaluated = calc?.optBoolean("justEvaluated", false) ?: false,
            calcLastFull = calc?.let { if (it.isNull("lastFull")) null else it.optString("lastFull") },
            monospace = if (root.has("monospace")) root.optBoolean("monospace") else null,
            theme = if (root.has("theme")) root.optString("theme") else null,
            language = if (root.has("language")) root.optString("language") else null,
            appointmentsJson = root.optJSONArray("appointments")?.toString(),
        )
    }

    private fun readLimited(input: InputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buf = ByteArray(16 * 1024)
        var total = 0L
        while (true) {
            val n = input.read(buf)
            if (n < 0) break
            total += n
            if (total > MAX_ENTRY_BYTES) throw IOException("Entry too large")
            out.write(buf, 0, n)
        }
        return out.toByteArray()
    }

    private fun safeFileName(s: String): String =
        s.replace(Regex("[\\\\/:*?\"<>|\\r\\n\\t]"), "_").trim().take(40)
}
