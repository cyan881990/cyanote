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
    private const val MAX_ENTRY_BYTES = 20L * 1024 * 1024 // chặn file nén "bom" hoặc quá lớn
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
    fun write(out: OutputStream, json: String, notes: List<Note>, untitled: String) {
        ZipOutputStream(BufferedOutputStream(out)).use { zip ->
            zip.putNextEntry(ZipEntry(NOTES_ENTRY))
            zip.write(json.toByteArray(Charsets.UTF_8))
            zip.closeEntry()

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
    fun read(input: InputStream, fileName: String?): BackupData {
        val buffered = BufferedInputStream(input)
        buffered.mark(4)
        val b1 = buffered.read()
        val b2 = buffered.read()
        buffered.reset()
        val isZip = b1 == 'P'.code && b2 == 'K'.code
        return if (isZip) readZip(buffered) else readPlainText(buffered, fileName)
    }

    private fun readZip(input: InputStream): BackupData {
        var json: String? = null
        val textNotes = mutableListOf<Note>()
        var count = 0
        ZipInputStream(input).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (++count > MAX_ENTRIES) throw IOException("Too many entries")
                if (entry.isDirectory) continue
                val path = entry.name.replace('\\', '/')
                val name = path.substringAfterLast('/')
                when {
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
            parsed != null && parsed.notes.isNotEmpty() -> parsed
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
