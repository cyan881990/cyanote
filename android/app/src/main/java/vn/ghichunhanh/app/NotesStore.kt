package vn.ghichunhanh.app

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val updatedAt: Long,
)

/**
 * Tên hiện trên tab: tên tự đặt, nếu không có thì lấy dòng đầu tiên của ghi chú.
 * [untitled] là chữ "Ghi chú mới" theo ngôn ngữ đang dùng.
 */
fun Note.displayTitle(untitled: String): String {
    if (title.isNotBlank()) return title.trim()
    val firstLine = content.lineSequence().map { it.trim() }.firstOrNull { it.isNotEmpty() }
        ?: return untitled
    return if (firstLine.length > 18) firstLine.take(18) + "…" else firstLine
}

/**
 * Lưu toàn bộ ghi chú + phép tính dở vào một file JSON trong bộ nhớ riêng của app.
 * - Tự lưu sau khi ngừng gõ 0,4 giây (không cần bấm Lưu).
 * - Lưu ngay khi app bị ẩn (onStop), nên tắt app, hết pin cũng không mất.
 * - Ghi file kiểu "atomic": nếu đang ghi mà sập, file cũ vẫn còn nguyên.
 */
class NotesStore private constructor(context: Context, welcomeTitle: String, welcomeText: String) {
    private val appContext = context.applicationContext
    private val file = AtomicFile(File(context.filesDir, "notes.json"))
    private val lock = Any()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var saveJob: Job? = null
    private var version = 0L

    val notes = mutableStateListOf<Note>()

    var selectedId by mutableLongStateOf(0L)
        private set

    var monospace by mutableStateOf(false)
        private set

    /** true khi có thay đổi chưa ghi xuống file (để hiện "Đang lưu…"). */
    var isSaving by mutableStateOf(false)
        private set

    val calc = CalcState { markChanged() }

    init {
        load()
        if (notes.isEmpty()) {
            val now = System.currentTimeMillis()
            notes.add(Note(now, welcomeTitle, welcomeText, now))
            selectedId = now
        }
        if (notes.none { it.id == selectedId }) selectedId = notes.first().id
    }

    fun select(id: Long) {
        if (selectedId == id) return
        selectedId = id
        markChanged()
    }

    /** Mở tab mới. Nếu đã có một tab trống thì chuyển sang tab đó thay vì tạo thêm. */
    /** Tạo ghi chú mới có sẵn nội dung (nhận chữ chia sẻ từ app khác). */
    fun addNote(content: String, title: String = "") {
        val now = System.currentTimeMillis()
        val id = maxOf(now, (notes.maxOfOrNull { it.id } ?: 0L) + 1)
        notes.add(Note(id, title.take(80), content, now))
        selectedId = id
        markChanged()
    }

    /** Luôn tạo ghi chú mới (tạo liên tục nhiều ghi chú được). */
    fun newNote() {
        val now = System.currentTimeMillis()
        val id = maxOf(now, (notes.maxOfOrNull { it.id } ?: 0L) + 1)
        notes.add(Note(id, "", "", now))
        selectedId = id
        markChanged()
    }

    fun updateContent(id: Long, content: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0 || notes[i].content == content) return
        notes[i] = notes[i].copy(content = content, updatedAt = System.currentTimeMillis())
        markChanged()
    }

    fun rename(id: Long, title: String) {
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0) return
        notes[i] = notes[i].copy(title = title)
        markChanged()
    }

    fun delete(id: Long) {
        val i = notes.indexOfFirst { it.id == id }
        if (i < 0) return
        notes.removeAt(i)
        AttachmentsStore.get(appContext).deleteAll(id)   // xoá luôn ảnh của ghi chú
        if (notes.isEmpty()) {
            newNote()
            return
        }
        if (selectedId == id) selectedId = notes[(i - 1).coerceAtLeast(0)].id
        markChanged()
    }

    fun updateMonospace(enabled: Boolean) {
        monospace = enabled
        markChanged()
    }

    /**
     * JSON đầy đủ để đưa vào file sao lưu: ghi chú, phép tính dở, kiểu chữ,
     * cộng thêm cài đặt sáng/tối và ngôn ngữ.
     */
    fun backupJson(theme: String, language: String): String {
        val root = JSONObject(toJson())
        root.put("format", Backup.FORMAT)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("theme", theme)
        root.put("language", language)
        return root.toString(2)
    }

    /**
     * Nhập dữ liệu từ file sao lưu.
     * - replace = false: thêm vào ghi chú hiện có, bỏ qua ghi chú trùng y hệt.
     * - replace = true: thay toàn bộ ghi chú, phép tính và kiểu chữ bằng dữ liệu trong file.
     * Trả về số ghi chú đã thêm.
     */
    /** Ghi chú cũ → id mới sau lần nhập gần nhất (để gắn lại lịch hẹn, ảnh). */
    var lastImportIdMap: Map<Long, Long> = emptyMap()
        private set

    fun importBackup(data: BackupData, replace: Boolean): Int {
        val idMap = HashMap<Long, Long>()
        var nextId = maxOf(System.currentTimeMillis(), (notes.maxOfOrNull { it.id } ?: 0L) + 1)
        if (replace) notes.clear()
        val existing = notes.map { it.title to it.content }.toHashSet()
        var firstAddedId: Long? = null
        var added = 0
        for (n in data.notes) {
            if (!replace && (n.title to n.content) in existing) {
                notes.firstOrNull { it.title == n.title && it.content == n.content }?.let { idMap[n.id] = it.id }
                continue
            }
            val id = nextId++
            idMap[n.id] = id
            notes.add(n.copy(id = id))
            existing.add(n.title to n.content)
            if (firstAddedId == null) firstAddedId = id
            added++
        }
        if (replace) {
            data.monospace?.let { monospace = it }
            calc.restore(data.calcExpr ?: "", data.calcJustEvaluated, data.calcLastFull)
        }
        if (notes.isEmpty()) {
            newNote()
        } else {
            selectedId = firstAddedId ?: selectedId
            if (notes.none { it.id == selectedId }) selectedId = notes.first().id
        }
        lastImportIdMap = idMap
        markChanged()
        saveNow()
        return added
    }

    /** Ghi xuống file ngay lập tức (gọi khi app bị ẩn). */
    fun saveNow() {
        saveJob?.cancel()
        if (!isSaving) return
        write(toJson())
        isSaving = false
    }

    private fun markChanged() {
        version++
        isSaving = true
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(SAVE_DELAY_MS)
            val v = version
            val json = toJson()
            withContext(Dispatchers.IO) { write(json) }
            if (version == v) isSaving = false
        }
    }

    private fun load() {
        val bytes = try {
            synchronized(lock) { file.readFully() }
        } catch (e: IOException) {
            return
        }
        try {
            val root = JSONObject(String(bytes, Charsets.UTF_8))
            val arr = root.optJSONArray("notes") ?: JSONArray()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                notes.add(
                    Note(
                        id = o.getLong("id"),
                        title = o.optString("title", ""),
                        content = o.optString("content", ""),
                        updatedAt = o.optLong("updatedAt", 0L),
                    )
                )
            }
            selectedId = root.optLong("selectedId", 0L)
            monospace = root.optBoolean("monospace", false)
            root.optJSONObject("calc")?.let { c ->
                val last = if (c.isNull("lastFull")) null else c.optString("lastFull")
                calc.restore(
                    expr = c.optString("expr", ""),
                    justEvaluated = c.optBoolean("justEvaluated", false),
                    lastFull = last,
                )
            }
        } catch (e: Exception) {
            // File hỏng: bắt đầu lại với dữ liệu trống thay vì crash.
            notes.clear()
        }
    }

    private fun toJson(): String {
        val arr = JSONArray()
        for (n in notes) {
            arr.put(
                JSONObject()
                    .put("id", n.id)
                    .put("title", n.title)
                    .put("content", n.content)
                    .put("updatedAt", n.updatedAt)
            )
        }
        val calcJson = JSONObject()
            .put("expr", calc.expr)
            .put("justEvaluated", calc.justEvaluated)
            .put("lastFull", calc.lastFull ?: JSONObject.NULL)
        return JSONObject()
            .put("version", 1)
            .put("selectedId", selectedId)
            .put("monospace", monospace)
            .put("calc", calcJson)
            .put("notes", arr)
            .toString()
    }

    private fun write(json: String) {
        synchronized(lock) {
            var out: FileOutputStream? = null
            try {
                out = file.startWrite()
                out.write(json.toByteArray(Charsets.UTF_8))
                file.finishWrite(out)
            } catch (e: IOException) {
                if (out != null) file.failWrite(out)
            }
        }
    }

    companion object {
        private const val SAVE_DELAY_MS = 400L

        /** Tên hiển thị của một ghi chú (dùng trong thông báo lịch hẹn). */
        fun peekTitle(context: Context, id: Long): String? {
            val ctx = AppSettings.wrapWithLanguage(context)
            return get(ctx).notes.firstOrNull { it.id == id }?.displayTitle(ctx.getString(R.string.untitled))
        }

        @Volatile
        private var instance: NotesStore? = null

        fun get(context: Context): NotesStore =
            instance ?: synchronized(this) {
                // Lần chạy đầu tiên: tạo ghi chú hướng dẫn bằng ngôn ngữ của Activity.
                instance ?: NotesStore(
                    context.applicationContext,
                    context.getString(R.string.welcome_title),
                    context.getString(R.string.welcome_text),
                ).also { instance = it }
            }
    }
}
