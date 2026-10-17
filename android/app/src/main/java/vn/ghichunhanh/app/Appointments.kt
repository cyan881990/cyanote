package vn.ghichunhanh.app

import android.content.Context
import android.util.AtomicFile
import androidx.compose.runtime.mutableStateListOf
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * Một lịch hẹn / cuộc gọi cần nhớ.
 * - [time]: thời điểm hẹn (mili giây).
 * - [remindBefore]: nhắc trước bao nhiêu phút (0 = đúng giờ).
 * - [phone]: số điện thoại để bấm gọi từ thông báo (có thể trống).
 * - [noteId]: ghi chú gắn kèm (0 = không gắn).
 * - [sound]: thông báo có chuông hay im lặng.
 */
data class Appointment(
    val id: Long,
    val title: String,
    val time: Long,
    val phone: String = "",
    val noteId: Long = 0L,
    val remindBefore: Int = 0,
    val sound: Boolean = true,
    val done: Boolean = false,
) {
    val fireAt: Long get() = time - remindBefore * 60_000L

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("title", title)
        .put("time", time)
        .put("phone", phone)
        .put("noteId", noteId)
        .put("remindBefore", remindBefore)
        .put("sound", sound)
        .put("done", done)

    companion object {
        fun fromJson(o: JSONObject) = Appointment(
            id = o.optLong("id"),
            title = o.optString("title", ""),
            time = o.optLong("time"),
            phone = o.optString("phone", ""),
            noteId = o.optLong("noteId", 0L),
            remindBefore = o.optInt("remindBefore", 0),
            sound = o.optBoolean("sound", true),
            done = o.optBoolean("done", false),
        )

        /** Lấy số điện thoại đầu tiên trong một dòng chữ (9–15 chữ số, cho phép dấu cách, chấm, gạch). */
        fun findPhone(text: String): String {
            val m = Regex("""(\+?\d[\d .\-]{7,18}\d)""").find(text) ?: return ""
            val digits = m.value.filter { it.isDigit() || it == '+' }
            return if (digits.count { it.isDigit() } in 9..15) digits else ""
        }
    }
}

/** Lưu lịch hẹn vào appointments.json (ghi ngay, an toàn khi tắt máy đột ngột). */
class AppointmentsStore private constructor(private val appContext: Context) {
    val items = mutableStateListOf<Appointment>()
    private val file = AtomicFile(File(appContext.filesDir, "appointments.json"))

    init {
        runCatching {
            val arr = JSONArray(String(file.readFully(), Charsets.UTF_8))
            for (i in 0 until arr.length()) items.add(Appointment.fromJson(arr.getJSONObject(i)))
        }
        sort()
    }

    /** Sắp xếp: chưa xong trước (sớm nhất lên đầu), đã xong/đã qua xuống cuối. */
    private fun sort() {
        val sorted = items.sortedWith(compareBy<Appointment> { it.done }.thenBy { if (it.done) -it.time else it.time })
        items.clear()
        items.addAll(sorted)
    }

    fun get(id: Long): Appointment? = items.firstOrNull { it.id == id }

    fun upsert(a: Appointment) {
        val i = items.indexOfFirst { it.id == a.id }
        if (i >= 0) items[i] = a else items.add(a)
        sort()
        save()
        Reminders.schedule(appContext, a)
    }

    fun delete(id: Long) {
        items.removeAll { it.id == id }
        save()
        Reminders.cancel(appContext, id)
    }

    fun setDone(id: Long, done: Boolean) {
        val a = get(id) ?: return
        upsert(a.copy(done = done))
    }

    /** Báo lại sau [minutes] phút (dời giờ nhắc, giữ giờ hẹn gốc nếu còn ở tương lai). */
    fun snooze(id: Long, minutes: Int) {
        val a = get(id) ?: return
        val now = System.currentTimeMillis()
        val newTime = now + minutes * 60_000L
        upsert(a.copy(time = newTime, remindBefore = 0, done = false))
    }

    /** Lịch hẹn của một ghi chú. */
    fun forNote(noteId: Long): List<Appointment> = items.filter { it.noteId == noteId }

    /** Số lịch hẹn chưa xong còn trong hôm nay (để hiện chấm đỏ trên nút ⏰). */
    fun pendingCount(): Int {
        val now = System.currentTimeMillis()
        return items.count { !it.done && it.time >= now - 60 * 60_000L }
    }

    fun toJsonArray(): JSONArray = JSONArray().apply { items.forEach { put(it.toJson()) } }

    /** Nhập từ file sao lưu. [idMap] đổi id ghi chú cũ → mới (ghi chú được cấp id mới khi nhập). */
    fun importFrom(arr: JSONArray?, idMap: Map<Long, Long>, replace: Boolean) {
        if (arr == null) return
        if (replace) {
            items.forEach { Reminders.cancel(appContext, it.id) }
            items.clear()
        }
        var nextId = maxOf(System.currentTimeMillis(), (items.maxOfOrNull { it.id } ?: 0L) + 1)
        for (i in 0 until arr.length()) {
            val a = Appointment.fromJson(arr.getJSONObject(i))
            if (!replace && items.any { it.title == a.title && it.time == a.time }) continue
            items.add(a.copy(id = nextId++, noteId = idMap[a.noteId] ?: 0L))
        }
        sort()
        save()
        Reminders.rescheduleAll(appContext)
    }

    private fun save() {
        val bytes = toJsonArray().toString().toByteArray(Charsets.UTF_8)
        synchronized(this) {
            val out = file.startWrite()
            try {
                out.write(bytes)
                file.finishWrite(out)
            } catch (e: Exception) {
                file.failWrite(out)
            }
        }
    }

    companion object {
        @Volatile private var instance: AppointmentsStore? = null
        fun get(context: Context): AppointmentsStore =
            instance ?: synchronized(this) {
                instance ?: AppointmentsStore(context.applicationContext).also { instance = it }
            }
    }
}
