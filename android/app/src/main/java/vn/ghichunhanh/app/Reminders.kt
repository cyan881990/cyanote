package vn.ghichunhanh.app

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.text.format.DateFormat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import java.util.Date

/**
 * Nhắc lịch hẹn bằng AlarmManager + thông báo.
 * - Có quyền "Báo thức & lời nhắc" (Android 12+) → nhắc đúng phút; không có → Android có thể trễ vài phút.
 * - Hai kênh thông báo: có chuông và im lặng (người dùng chọn cho từng lịch hẹn).
 */
object Reminders {
    const val CHANNEL_SOUND = "appointments_sound_v1"
    const val CHANNEL_SILENT = "appointments_silent_v1"
    const val ACTION_FIRE = "app.cyanote.REMINDER_FIRE"
    const val ACTION_SNOOZE = "app.cyanote.REMINDER_SNOOZE"
    const val ACTION_DONE = "app.cyanote.REMINDER_DONE"
    const val EXTRA_ID = "appointment_id"
    const val EXTRA_OPEN_NOTE = "open_note_id"
    const val EXTRA_OPEN_APPOINTMENTS = "open_appointments"
    const val SNOOZE_MINUTES = 10

    private fun code(id: Long): Int = (id xor (id ushr 32)).toInt()

    /** Ngữ cảnh theo ngôn ngữ người dùng chọn trong app (thông báo hiện đúng ngôn ngữ). */
    private fun localized(context: Context): Context = AppSettings.wrapWithLanguage(context)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ctx = localized(context)
        val nm = context.getSystemService(NotificationManager::class.java)
        val sound = NotificationChannel(
            CHANNEL_SOUND, ctx.getString(R.string.appt_channel_sound), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = ctx.getString(R.string.appt_channel_desc)
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 400, 250, 400)
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
        }
        val silent = NotificationChannel(
            CHANNEL_SILENT, ctx.getString(R.string.appt_channel_silent), NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = ctx.getString(R.string.appt_channel_desc)
            setSound(null, null)
            enableVibration(false)
        }
        nm.createNotificationChannels(listOf(sound, silent))
    }

    fun canExact(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        return context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    }

    fun canNotify(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun firePending(context: Context, id: Long): PendingIntent =
        PendingIntent.getBroadcast(
            context, code(id),
            Intent(context, ReminderReceiver::class.java).setAction(ACTION_FIRE).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun schedule(context: Context, a: Appointment) {
        val am = context.getSystemService(AlarmManager::class.java)
        val pi = firePending(context, a.id)
        am.cancel(pi)
        if (a.done) return
        val now = System.currentTimeMillis()
        if (a.time < now - 60_000L) return            // đã qua giờ hẹn
        val at = maxOf(a.fireAt, now + 1_000L)        // giờ nhắc đã qua nhưng giờ hẹn chưa tới → nhắc ngay
        if (canExact(context)) {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        } else {
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        }
    }

    fun cancel(context: Context, id: Long) {
        context.getSystemService(AlarmManager::class.java).cancel(firePending(context, id))
        NotificationManagerCompat.from(context).cancel(code(id))
    }

    fun rescheduleAll(context: Context) {
        AppointmentsStore.get(context).items.forEach { schedule(context, it) }
    }

    fun show(context: Context, a: Appointment) {
        if (!canNotify(context)) return
        ensureChannels(context)
        val ctx = localized(context)
        val time = DateFormat.getTimeFormat(ctx).format(Date(a.time))
        val minutesLeft = ((a.time - System.currentTimeMillis()) / 60_000L).toInt()
        val parts = buildList {
            add(time)
            if (minutesLeft > 0) add(ctx.getString(R.string.appt_in_minutes, minutesLeft))
            if (a.phone.isNotBlank()) add(a.phone)
        }
        val noteTitle = if (a.noteId != 0L) {
            NotesStore.peekTitle(context, a.noteId)
        } else null

        val open = PendingIntent.getActivity(
            context, code(a.id),
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                .putExtra(EXTRA_OPEN_NOTE, a.noteId)
                .putExtra(EXTRA_OPEN_APPOINTMENTS, a.noteId == 0L),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        fun action(act: String) = PendingIntent.getBroadcast(
            context, code(a.id) + act.hashCode(),
            Intent(context, ReminderReceiver::class.java).setAction(act).putExtra(EXTRA_ID, a.id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val b = NotificationCompat.Builder(context, if (a.sound) CHANNEL_SOUND else CHANNEL_SILENT)
            .setSmallIcon(R.drawable.ic_stat_reminder)
            .setColor(0xFF0B7285.toInt())
            .setContentTitle("⏰ " + a.title.ifBlank { ctx.getString(R.string.appt_untitled) })
            .setContentText(parts.joinToString(" · "))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    parts.joinToString(" · ") + (noteTitle?.let { "\n📝 $it" } ?: "")
                )
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .setAutoCancel(true)
            .setWhen(a.time)
            .setShowWhen(true)
        if (!a.sound) b.setSilent(true)
        if (a.phone.isNotBlank()) {
            val dial = PendingIntent.getActivity(
                context, code(a.id) + 7,
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + a.phone)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                PendingIntent.FLAG_IMMUTABLE,
            )
            b.addAction(0, ctx.getString(R.string.appt_call), dial)
        }
        b.addAction(0, ctx.getString(R.string.appt_snooze, SNOOZE_MINUTES), action(ACTION_SNOOZE))
        b.addAction(0, ctx.getString(R.string.appt_done), action(ACTION_DONE))
        runCatching { NotificationManagerCompat.from(context).notify(code(a.id), b.build()) }
    }

    fun dismiss(context: Context, id: Long) {
        NotificationManagerCompat.from(context).cancel(code(id))
    }
}

/** Nhận báo thức của lịch hẹn và các nút trên thông báo (Báo lại / Xong). */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(Reminders.EXTRA_ID, 0L)
        val store = AppointmentsStore.get(context)
        when (intent.action) {
            Reminders.ACTION_FIRE -> store.get(id)?.takeIf { !it.done }?.let { Reminders.show(context, it) }
            Reminders.ACTION_SNOOZE -> {
                Reminders.dismiss(context, id)
                store.snooze(id, Reminders.SNOOZE_MINUTES)
            }
            Reminders.ACTION_DONE -> {
                Reminders.dismiss(context, id)
                store.setDone(id, true)
            }
        }
    }
}

/** Khởi động lại máy, đổi giờ, cập nhật app → đặt lại toàn bộ lời nhắc. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        Reminders.rescheduleAll(context)
    }
}
