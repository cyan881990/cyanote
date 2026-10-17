package vn.ghichunhanh.app.ui

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.ghichunhanh.app.Appointment
import vn.ghichunhanh.app.AppointmentsStore
import vn.ghichunhanh.app.R
import vn.ghichunhanh.app.Reminders
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

private fun localeOf(cfg: android.content.res.Configuration): Locale = cfg.locales[0]

internal fun formatTime(millis: Long, locale: Locale): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))

internal fun formatDayShort(millis: Long, locale: Locale): String =
    LocalDateTime.ofInstant(Instant.ofEpochMilli(millis), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale))

/** Thẻ chèn vào ghi chú, ví dụ "[⏰ 18/10 15:30]". */
fun appointmentTag(a: Appointment, locale: Locale): String {
    val dt = LocalDateTime.ofInstant(Instant.ofEpochMilli(a.time), ZoneId.systemDefault())
    val day = dt.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "dM"), locale))
    return "[⏰ $day ${formatTime(a.time, locale)}]"
}

private val REMIND_OPTIONS = listOf(0, 5, 15, 30, 60)

/** Sửa / tạo lịch hẹn. [noteTitle] khác null → đang tạo từ một ghi chú (có tuỳ chọn gắn & chèn thẻ). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppointmentEditorDialog(
    initial: Appointment,
    isNew: Boolean,
    noteTitle: String?,
    onSave: (Appointment, insertTag: Boolean) -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val locale = localeOf(LocalConfiguration.current)
    val scheme = MaterialTheme.colorScheme
    var title by remember { mutableStateOf(initial.title) }
    var phone by remember { mutableStateOf(initial.phone) }
    val start = LocalDateTime.ofInstant(Instant.ofEpochMilli(initial.time), ZoneId.systemDefault())
    var date by remember { mutableStateOf(start.toLocalDate()) }
    var time by remember { mutableStateOf(start.toLocalTime().withSecond(0).withNano(0)) }
    var remind by remember { mutableStateOf(initial.remindBefore) }
    var sound by remember { mutableStateOf(initial.sound) }
    var linkNote by remember { mutableStateOf(initial.noteId != 0L || (isNew && noteTitle != null)) }
    var insertTag by remember { mutableStateOf(isNew && noteTitle != null) }
    var pickDate by remember { mutableStateOf(false) }
    var pickTime by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(if (isNew) R.string.appt_new else R.string.appt_edit)) },
        text = {
            Column(
                Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.appt_field_title)) },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it.filter { c -> c.isDigit() || c in "+ .-()" } },
                    label = { Text(stringResource(R.string.appt_field_phone)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    leadingIcon = { Icon(PhoneIcon, contentDescription = null, modifier = Modifier.size(18.dp)) },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.weight(1.3f)) {
                        Text(
                            date.format(DateTimeFormatter.ofPattern(android.text.format.DateFormat.getBestDateTimePattern(locale, "EEEdMMMyyyy"), locale)),
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                        )
                    }
                    OutlinedButton(onClick = { pickTime = true }, modifier = Modifier.weight(1f)) {
                        Text(time.format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale)), maxLines = 1)
                    }
                }
                Text(stringResource(R.string.appt_remind), fontSize = 13.sp, color = scheme.onSurfaceVariant)
                Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    for (m in REMIND_OPTIONS) {
                        FilterChip(
                            selected = remind == m,
                            onClick = { remind = m },
                            label = {
                                Text(
                                    when (m) {
                                        0 -> stringResource(R.string.appt_remind_at_time)
                                        60 -> stringResource(R.string.appt_hour_short)
                                        else -> stringResource(R.string.appt_minutes_short, m)
                                    }
                                )
                            },
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.appt_sound), modifier = Modifier.weight(1f))
                    Switch(checked = sound, onCheckedChange = { sound = it })
                }
                if (noteTitle != null) {
                    Row(Modifier.clickable { linkNote = !linkNote }, verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = linkNote, onCheckedChange = { linkNote = it })
                        Text(stringResource(R.string.appt_link_note) + " · " + noteTitle, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (isNew) {
                        Row(Modifier.clickable { insertTag = !insertTag }, verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = insertTag, onCheckedChange = { insertTag = it })
                            Text(stringResource(R.string.appt_insert_tag))
                        }
                    }
                }
                if (!Reminders.canExact(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    ExactAlarmHint()
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                val millis = LocalDateTime.of(date, time).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                onSave(
                    initial.copy(
                        title = title.trim(),
                        phone = phone.trim(),
                        time = millis,
                        remindBefore = remind,
                        sound = sound,
                        noteId = if (linkNote) initial.noteId else 0L,
                        done = if (millis > System.currentTimeMillis()) false else initial.done,
                    ),
                    insertTag && linkNote,
                )
            }) { Text(stringResource(R.string.action_save)) }
        },
        dismissButton = {
            Row {
                if (onDelete != null) {
                    TextButton(onClick = onDelete) { Text(stringResource(R.string.action_delete), color = scheme.error) }
                }
                TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
            }
        },
    )

    if (pickDate) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
        )
        DatePickerDialog(
            onDismissRequest = { pickDate = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { date = Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate() }
                    pickDate = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickDate = false }) { Text(stringResource(R.string.action_cancel)) } },
        ) { DatePicker(state = state) }
    }
    if (pickTime) {
        val state = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = android.text.format.DateFormat.is24HourFormat(context),
        )
        AlertDialog(
            onDismissRequest = { pickTime = false },
            text = { TimePicker(state = state) },
            confirmButton = {
                TextButton(onClick = { time = LocalTime.of(state.hour, state.minute); pickTime = false }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { pickTime = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Nhắc bật quyền "Báo thức & lời nhắc" (Android 12+) để báo đúng phút. */
@Composable
fun ExactAlarmHint() {
    val context = LocalContext.current
    val extra = LocalExtraColors.current
    Surface(shape = RoundedCornerShape(12.dp), color = extra.accentSoft) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(stringResource(R.string.appt_exact_needed), fontSize = 13.sp)
            TextButton(onClick = {
                runCatching {
                    context.startActivity(
                        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + context.packageName))
                    )
                }
            }) { Text(stringResource(R.string.appt_open_settings)) }
        }
    }
}

/** Mở bàn phím gọi điện với số đã điền (không tự gọi). */
fun dial(context: android.content.Context, phone: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))) }
}

private enum class Group { TODAY, TOMORROW, LATER, PAST }

/**
 * Danh sách lịch hẹn (bảng dưới hoặc cột phải). Nhóm: Hôm nay · Ngày mai · Sắp tới · Đã qua & đã xong.
 * [onCollapse] khác null → có nút thu gọn.
 */
@Composable
fun AppointmentsPanel(
    store: AppointmentsStore,
    noteTitleOf: (Long) -> String?,
    onNew: () -> Unit,
    onEdit: (Appointment) -> Unit,
    onOpenNote: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onCollapse: (() -> Unit)? = null,
    header: @Composable () -> Unit = {},
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val locale = localeOf(LocalConfiguration.current)
    val today = LocalDate.now()
    val now = System.currentTimeMillis()
    fun groupOf(a: Appointment): Group {
        if (a.done || a.time < now - 60_000L) return Group.PAST
        val d = Instant.ofEpochMilli(a.time).atZone(ZoneId.systemDefault()).toLocalDate()
        return when (d) {
            today -> Group.TODAY
            today.plusDays(1) -> Group.TOMORROW
            else -> Group.LATER
        }
    }
    val grouped = store.items.groupBy(::groupOf)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(Modifier.weight(1f)) { header() }
                Surface(onClick = onNew, shape = RoundedCornerShape(12.dp), color = scheme.primary, contentColor = scheme.onPrimary) {
                    Row(Modifier.padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.appt_new), modifier = Modifier.size(18.dp))
                    }
                }
                if (onCollapse != null) {
                    Surface(onClick = onCollapse, shape = RoundedCornerShape(12.dp), color = LocalExtraColors.current.keyBg) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.padding(7.dp).size(20.dp))
                    }
                }
            }
            if (!Reminders.canExact(context) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && store.items.any { !it.done }) {
                Spacer(Modifier.height(6.dp))
                ExactAlarmHint()
            }
            if (store.items.isEmpty()) {
                Text(
                    stringResource(R.string.appt_empty),
                    color = scheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 18.dp),
                )
            } else {
                LazyColumn(Modifier.padding(top = 4.dp)) {
                    for (g in Group.entries) {
                        val list = grouped[g] ?: continue
                        item(key = "h$g") {
                            Text(
                                stringResource(
                                    when (g) {
                                        Group.TODAY -> R.string.appt_today
                                        Group.TOMORROW -> R.string.appt_tomorrow
                                        Group.LATER -> R.string.appt_upcoming
                                        Group.PAST -> R.string.appt_past
                                    }
                                ),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                                letterSpacing = 0.6.sp,
                                modifier = Modifier.padding(start = 8.dp, top = 10.dp, bottom = 4.dp),
                            )
                        }
                        items(list, key = { it.id }) { a ->
                            AppointmentRow(
                                a = a,
                                locale = locale,
                                showDay = g == Group.LATER || g == Group.PAST,
                                noteTitle = if (a.noteId != 0L) noteTitleOf(a.noteId) else null,
                                onClick = { onEdit(a) },
                                onCall = { dial(context, a.phone) },
                                onToggleDone = { store.setDone(a.id, !a.done) },
                                onOpenNote = { onOpenNote(a.noteId) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AppointmentRow(
    a: Appointment,
    locale: Locale,
    showDay: Boolean,
    noteTitle: String?,
    onClick: () -> Unit,
    onCall: () -> Unit,
    onToggleDone: () -> Unit,
    onOpenNote: () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val faded = a.done
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(
            Modifier
                .width(64.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(if (faded) extra.keyBg else extra.accentSoft)
                .padding(vertical = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(formatTime(a.time, locale), fontSize = 15.sp, fontWeight = FontWeight.Bold, color = if (faded) scheme.onSurfaceVariant else scheme.primary, maxLines = 1)
            if (showDay) Text(formatDayShort(a.time, locale), fontSize = 10.sp, color = scheme.onSurfaceVariant, maxLines = 1)
        }
        Column(Modifier.weight(1f)) {
            Text(
                a.title.ifBlank { stringResource(R.string.appt_untitled) },
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (faded) scheme.onSurfaceVariant else scheme.onSurface,
                textDecoration = if (faded) TextDecoration.LineThrough else null,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            val sub = listOfNotNull(
                a.phone.ifBlank { null },
                if (a.remindBefore > 0) "🔔 −${a.remindBefore}′" else null,
                if (!a.sound) "🔕" else null,
            ).joinToString(" · ")
            if (sub.isNotEmpty()) Text(sub, fontSize = 12.sp, color = scheme.onSurfaceVariant, maxLines = 1)
            if (noteTitle != null) {
                Text(
                    "📝 $noteTitle",
                    fontSize = 12.sp,
                    color = scheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable(onClick = onOpenNote),
                )
            }
        }
        if (a.phone.isNotBlank()) {
            Surface(onClick = onCall, shape = CircleShape, color = scheme.primary, contentColor = scheme.onPrimary, modifier = Modifier.size(36.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(PhoneIcon, contentDescription = stringResource(R.string.appt_call), modifier = Modifier.size(18.dp))
                }
            }
        }
        Checkbox(checked = a.done, onCheckedChange = { onToggleDone() })
    }
}

/** Thanh thu gọn của chế độ Lịch hẹn: lịch hẹn kế tiếp. */
@Composable
fun AppointmentBar(store: AppointmentsStore, onExpand: () -> Unit, compact: Boolean = false, hideIcon: Boolean = false, modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val locale = localeOf(LocalConfiguration.current)
    val now = System.currentTimeMillis()
    val upcoming = store.items.filter { !it.done && it.time >= now - 60_000L }.sortedBy { it.time }
    val next = upcoming.firstOrNull()
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth().height(if (compact) 48.dp else 60.dp),
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!hideIcon) Box(
                Modifier.size(if (compact) 32.dp else 40.dp).clip(RoundedCornerShape(if (compact) 10.dp else 12.dp)).background(extra.accentSoft),
                contentAlignment = Alignment.Center,
            ) { Icon(AlarmIcon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(20.dp)) }
            Column(Modifier.weight(1f)) {
                if (next == null) {
                    Text(stringResource(R.string.appt_next_none), fontSize = 14.sp, color = scheme.onSurfaceVariant, maxLines = 1)
                } else {
                    if (!compact) Text(
                        formatDayShort(next.time, locale) + (if (upcoming.size > 1) " · " + stringResource(R.string.appt_more, upcoming.size - 1) else ""),
                        fontSize = 12.sp, color = scheme.onSurfaceVariant, maxLines = 1,
                    )
                    Text(
                        next.title.ifBlank { stringResource(R.string.appt_untitled) },
                        fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = scheme.onSurface,
                        maxLines = 1, overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (next != null) Text(formatTime(next.time, locale), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = scheme.primary, maxLines = 1)
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** Nút chuyển Máy tính ⇄ Lịch hẹn (+ nút ẩn bảng). */
@Composable
fun DockSwitcher(
    calcSelected: Boolean,
    onCalc: () -> Unit,
    onAppointments: () -> Unit,
    onHide: (() -> Unit)?,
    showLabels: Boolean,
    height: Dp = 36.dp,
) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            Modifier.clip(RoundedCornerShape(12.dp)).background(extra.keyBg).padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            @Composable
            fun seg(selected: Boolean, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit) {
                Surface(
                    onClick = onClick,
                    shape = RoundedCornerShape(10.dp),
                    color = if (selected) scheme.surface else extra.keyBg,
                    contentColor = if (selected) scheme.primary else scheme.onSurfaceVariant,
                    shadowElevation = if (selected) 1.dp else 0.dp,
                    modifier = Modifier.height(height - 6.dp),
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(icon, contentDescription = label, modifier = Modifier.size(17.dp))
                        if (showLabels) Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                    }
                }
            }
            seg(calcSelected, CalculatorIcon, stringResource(R.string.dock_calc), onCalc)
            seg(!calcSelected, AlarmIcon, stringResource(R.string.dock_appts), onAppointments)
        }
        if (onHide != null) {
            Surface(onClick = onHide, shape = RoundedCornerShape(12.dp), color = extra.keyBg, modifier = Modifier.size(height)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(PanelHideIcon, contentDescription = stringResource(R.string.dock_hide), tint = scheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}
