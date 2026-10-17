package vn.ghichunhanh.app.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import vn.ghichunhanh.app.APP_LANGUAGES
import vn.ghichunhanh.app.LunarCalendar
import vn.ghichunhanh.app.Note
import vn.ghichunhanh.app.NotesStore
import vn.ghichunhanh.app.R
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.movableContentOf
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.runtime.SideEffect
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import android.Manifest
import android.content.pm.PackageManager
import java.io.File
import vn.ghichunhanh.app.Appointment
import vn.ghichunhanh.app.AppointmentsStore
import vn.ghichunhanh.app.AttachmentsStore
import vn.ghichunhanh.app.DockMode
import vn.ghichunhanh.app.EditorPrefs
import vn.ghichunhanh.app.Reminders
import vn.ghichunhanh.app.ads.AdSlot
import vn.ghichunhanh.app.ads.Interstitials
import vn.ghichunhanh.app.findActivity
import vn.ghichunhanh.app.ads.rememberAdHolder
import vn.ghichunhanh.app.ads.rememberAdSize
import vn.ghichunhanh.app.displayTitle
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.time.temporal.WeekFields
import java.util.Locale

@Composable
fun EditorScreen(
    store: NotesStore,
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    currentLanguage: String,
    onLanguageChosen: (String) -> Unit,
    updateLabel: String? = null,
    onUpdateClick: () -> Unit = {},
    onCheckUpdates: () -> Unit = {},
    openAppointmentsRequest: Boolean = false,
    onAppointmentsRequestHandled: () -> Unit = {},
) {
    val context = LocalContext.current
    val keyboard = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val scheme = MaterialTheme.colorScheme
    val untitled = stringResource(R.string.untitled)

    val notes = store.notes
    val current = notes.firstOrNull { it.id == store.selectedId } ?: notes.first()

    // Giữ vị trí con trỏ của từng tab để "Chèn kết quả" vào đúng chỗ.
    val editorStates = remember { mutableStateMapOf<Long, TextFieldValue>() }
    val value = editorStates[current.id]?.takeIf { it.text == current.content }
        ?: TextFieldValue(current.content, TextRange(current.content.length))

    val focusRequester = remember { FocusRequester() }
    var calcOpen by rememberSaveable { mutableStateOf(false) }
    var menuOpen by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<Note?>(null) }
    var deleteTarget by remember { mutableStateOf<Note?>(null) }
    var showMonth by remember { mutableStateOf(false) }
    var showLanguages by remember { mutableStateOf(false) }
    var showBackup by remember { mutableStateOf(false) }
    val imeVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    fun applyValue(v: TextFieldValue) {
        editorStates[current.id] = v
        store.updateContent(current.id, v.text)
    }

    // Đang gõ (bàn phím hiện) thì thu gọn máy tính cho rộng chỗ; phép tính vẫn được giữ.
    LaunchedEffect(imeVisible) {
        if (imeVisible) calcOpen = false
    }
    // Tab mới (trống) thì đặt con trỏ sẵn để gõ ngay.
    LaunchedEffect(current.id) {
        if (current.content.isEmpty()) runCatching { focusRequester.requestFocus() }
    }

    // ----- Lịch hẹn, ảnh, ghi âm -----
    val appts = remember { AppointmentsStore.get(context) }
    val attachments = remember { AttachmentsStore.get(context) }
    val noteFiles = attachments.list(current.id)
    var editingAppt by remember { mutableStateOf<Appointment?>(null) }
    var editingIsNew by remember { mutableStateOf(false) }
    var editingFromNote by remember { mutableStateOf(false) }
    var viewerFile by remember { mutableStateOf<File?>(null) }
    var showAttachments by remember { mutableStateOf(false) }
    var recorderFile by remember { mutableStateOf<File?>(null) }
    var cameraFile by remember { mutableStateOf<File?>(null) }
    val locale = currentLocale()
    val noteTitleOf: (Long) -> String? = { id -> notes.firstOrNull { it.id == id }?.displayTitle(untitled) }

    val notifPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) recorderFile = attachments.newRecordingFile(current.id)
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        val f = cameraFile
        if (ok && f != null && f.length() > 0) attachments.add(current.id, android.net.Uri.fromFile(f))
        f?.delete()
        cameraFile = null
    }
    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) attachments.add(current.id, uri)
    }
    val startCamera = {
        val f = attachments.newCameraFile()
        cameraFile = f
        runCatching { takePicture.launch(FileProvider.getUriForFile(context, "app.cyanote.files", f)) }
        Unit
    }
    val startPick = {
        runCatching { pickPhoto.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
        Unit
    }
    val startRecord = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            recorderFile = attachments.newRecordingFile(current.id)
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }
    /** ⏰ trong ghi chú: lấy dòng đang gõ làm nội dung, tự nhận số điện thoại. */
    val tagAppointment = {
        val line = currentLine(value).replace(Regex("""\[⏰[^\]]*]"""), "").trim()
        val start = LocalDateTime.now().plusHours(1).withSecond(0).withNano(0)
        val rounded = start.withMinute(if (start.minute < 30) 30 else 0).let { if (start.minute >= 30) it.plusHours(1) else it }
        editingAppt = Appointment(
            id = System.currentTimeMillis(),
            title = line.take(120),
            time = rounded.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
            phone = Appointment.findPhone(line),
            noteId = current.id,
        )
        editingIsNew = true
        editingFromNote = true
    }
    val newAppointment = {
        val start = LocalDateTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
        editingAppt = Appointment(
            id = System.currentTimeMillis(),
            title = "",
            time = start.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
        )
        editingIsNew = true
        editingFromNote = false
    }
    // Mở từ thông báo: hiện bảng lịch hẹn
    LaunchedEffect(openAppointmentsRequest) {
        if (openAppointmentsRequest) {
            EditorPrefs.setDockMode(context, DockMode.APPOINTMENTS)
            EditorPrefs.setDockVisible(context, true)
            calcOpen = true
            onAppointmentsRequestHandled()
        }
    }

    // Nút ẩn bàn phím ở dòng trạng thái của ghi chú (dùng khi 2 cột, vì không có thanh máy tính dưới)
    var wideTyping by remember { mutableStateOf(false) }
    // Ẩn chip lịch hẹn & dải ảnh khi chỗ chật (đang gõ, hoặc điện thoại đang mở bảng dưới) để vẫn thấy chữ
    var extrasHidden by remember { mutableStateOf(false) }
    // ----- Các khối giao diện (ghép lại theo kích thước màn hình bên dưới) -----
    val hideKeyboard = { keyboard?.hide(); focusManager.clearFocus() }
    val hideKeyboardNow = hideKeyboard
    val openCalc = { hideKeyboard(); calcOpen = true }
    val tabBar: @Composable () -> Unit = {
        TabBar(
            notes = notes,
            selectedId = current.id,
            untitled = untitled,
            onSelect = { store.select(it) },
            onAdd = {
                store.newNote()
                // Mỗi 3 ghi chú mới → 1 quảng cáo toàn trang
                context.findActivity()?.let { Interstitials.onNoteCreated(it) }
            },
        ) {
            Box {
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        tint = scheme.onSurfaceVariant,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    if (imeVisible) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.hide_keyboard)) },
                            onClick = { menuOpen = false; keyboard?.hide(); focusManager.clearFocus() },
                        )
                    }
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_rename)) },
                        onClick = { menuOpen = false; renameTarget = current },
                    )
                    val copiedText = stringResource(R.string.copied)
                    val clipLabel = stringResource(R.string.clip_label)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_copy)) },
                        onClick = {
                            menuOpen = false
                            copyToClipboard(context, current.content, clipLabel, copiedText)
                        },
                    )
                    val shareTitle = stringResource(R.string.share_title)
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_share)) },
                        onClick = {
                            menuOpen = false
                            shareNote(context, current.displayTitle(untitled), current.content, shareTitle)
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_calendar)) },
                        onClick = { menuOpen = false; showMonth = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.language)) },
                        onClick = { menuOpen = false; showLanguages = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.backup_menu)) },
                        onClick = { menuOpen = false; showBackup = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.appt_screen)) },
                        leadingIcon = { Icon(AlarmIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = {
                            menuOpen = false
                            hideKeyboardNow()
                            EditorPrefs.setDockMode(context, DockMode.APPOINTMENTS)
                            EditorPrefs.setDockVisible(context, true)
                            calcOpen = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.att_manage) + if (noteFiles.isNotEmpty()) " (${noteFiles.size})" else "") },
                        leadingIcon = { Icon(CameraIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                        onClick = { menuOpen = false; showAttachments = true },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.word_wrap)) },
                        trailingIcon = { Checkbox(checked = EditorPrefs.wordWrap, onCheckedChange = null) },
                        onClick = { EditorPrefs.setWordWrap(context, !EditorPrefs.wordWrap) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.line_numbers)) },
                        trailingIcon = { Checkbox(checked = EditorPrefs.lineNumbers, onCheckedChange = null) },
                        onClick = { EditorPrefs.setLineNumbers(context, !EditorPrefs.lineNumbers) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(if (EditorPrefs.dockVisible) R.string.dock_hide else R.string.dock_show)) },
                        onClick = { menuOpen = false; EditorPrefs.setDockVisible(context, !EditorPrefs.dockVisible) },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.check_updates)) },
                        trailingIcon = {
                            if (updateLabel != null) {
                                Box(Modifier.size(8.dp).clip(CircleShape).background(scheme.primary))
                            }
                        },
                        onClick = { menuOpen = false; onCheckUpdates() },
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_monospace)) },
                        trailingIcon = { Checkbox(checked = store.monospace, onCheckedChange = null) },
                        onClick = { store.updateMonospace(!store.monospace) },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.menu_close), color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            menuOpen = false
                            if (current.content.isBlank()) store.delete(current.id) else deleteTarget = current
                        },
                    )
                }
            }
        }

    }
    val editorCard: @Composable (Modifier) -> Unit = { modifier ->
        // Vùng ghi chú
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp),
            shape = RoundedCornerShape(22.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
        ) {
            Column {
                val textStyle = MaterialTheme.typography.bodyLarge.copy(
                    color = scheme.onSurface,
                    fontSize = 16.sp,
                    lineHeight = 26.sp,
                    fontFamily = if (store.monospace) FontFamily.Monospace else FontFamily.Default,
                )
                val hint = stringResource(R.string.editor_hint)
                val extra = LocalExtraColors.current
                val wrap = EditorPrefs.wordWrap
                val showNums = EditorPrefs.lineNumbers
                // Tự cuộn tới chỗ đang gõ (kể cả khi bàn phím vừa bật làm vùng ghi chú thu nhỏ).
                val scroll = rememberScrollState()
                val hScroll = rememberScrollState()
                var viewportPx by remember { mutableStateOf(0) }
                var viewportWpx by remember { mutableStateOf(0) }
                var layout by remember { mutableStateOf<TextLayoutResult?>(null) }
                val density = LocalDensity.current
                val padTopPx = with(density) { 16.dp.toPx() }
                val marginPx = with(density) { 28.dp.toPx() }
                LaunchedEffect(value.text, value.selection, viewportPx, layout) {
                    val l = layout ?: return@LaunchedEffect
                    if (viewportPx <= 0) return@LaunchedEffect
                    val offset = value.selection.end.coerceIn(0, l.layoutInput.text.length)
                    val rect = l.getCursorRect(offset)
                    val top = padTopPx + rect.top
                    val bottom = padTopPx + rect.bottom
                    val target = when {
                        bottom + marginPx > scroll.value + viewportPx -> (bottom + marginPx - viewportPx).toInt()
                        top - marginPx < scroll.value -> (top - marginPx).toInt().coerceAtLeast(0)
                        else -> return@LaunchedEffect
                    }
                    scroll.animateScrollTo(target.coerceIn(0, scroll.maxValue.coerceAtLeast(target)))
                }
                // Số dòng (như Notepad++): cột trái, đánh số theo dòng thật (dòng dài tự xuống vẫn 1 số).
                val lineCount = if (value.text.isEmpty()) 1 else value.text.count { it == '\n' } + 1
                val gutter = if (showNums) (lineCount.toString().length * 9 + 16).dp else 0.dp
                val measurer = rememberTextMeasurer()
                val numStyle = androidx.compose.ui.text.TextStyle(
                    color = scheme.onSurfaceVariant.copy(alpha = 0.65f),
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                )
                val gutterBg = extra.keyBg.copy(alpha = 0.55f)
                val gutterLine = scheme.outlineVariant
                val availW = with(density) { viewportWpx.toDp() }
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { viewportPx = it.height; viewportWpx = it.width }
                        .verticalScroll(scroll)
                        .then(if (wrap) Modifier else Modifier.horizontalScroll(hScroll))
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { applyValue(it) },
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .then(if (wrap) Modifier.fillMaxWidth() else Modifier.defaultMinSize(minWidth = availW))
                            .defaultMinSize(minHeight = with(density) { viewportPx.toDp() })
                            .focusRequester(focusRequester)
                            .drawBehind {
                                if (!showNums) return@drawBehind
                                val gw = gutter.toPx()
                                drawRect(gutterBg, size = Size(gw, size.height))
                                drawLine(gutterLine, Offset(gw, 0f), Offset(gw, size.height), strokeWidth = 1f)
                                val l = layout ?: return@drawBehind
                                val text = l.layoutInput.text.text
                                var startIdx = 0
                                var n = 1
                                while (true) {
                                    val line = l.getLineForOffset(startIdx.coerceAtMost(text.length))
                                    val top = padTopPx + l.getLineTop(line)
                                    val h = l.getLineBottom(line) - l.getLineTop(line)
                                    val res = measurer.measure(n.toString(), numStyle)
                                    drawText(res, topLeft = Offset(gw - res.size.width - 6.dp.toPx(), top + (h - res.size.height) / 2f))
                                    val nl = text.indexOf('\n', startIdx)
                                    if (nl < 0 || n > 20000) break
                                    startIdx = nl + 1
                                    n++
                                }
                            }
                            .padding(start = if (showNums) gutter + 10.dp else 20.dp, end = 20.dp, top = 16.dp, bottom = 16.dp),
                        textStyle = textStyle,
                        cursorBrush = SolidColor(scheme.primary),
                        decorationBox = { inner ->
                            Box {
                                if (value.text.isEmpty()) {
                                    Text(hint, style = textStyle.copy(color = scheme.onSurfaceVariant))
                                }
                                inner()
                            }
                        },
                    )
                }
                // Lịch hẹn gắn với ghi chú này
                val noteAppts = if (extrasHidden) emptyList() else appts.items.filter { it.noteId == current.id && !it.done }
                if (noteAppts.isNotEmpty()) {
                    LazyRow(
                        Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                    ) {
                        items(noteAppts, key = { it.id }) { a ->
                            Surface(
                                onClick = { editingAppt = a; editingIsNew = false; editingFromNote = true },
                                shape = RoundedCornerShape(10.dp),
                                color = extra.accentSoft,
                                contentColor = scheme.primary,
                            ) {
                                Row(Modifier.padding(horizontal = 10.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                    Icon(AlarmIcon, contentDescription = null, modifier = Modifier.size(14.dp))
                                    Text(formatDayShort(a.time, locale) + " " + formatTime(a.time, locale), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                                    if (a.title.isNotBlank()) Text(a.title, fontSize = 12.sp, color = scheme.onSurface, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 160.dp))
                                    if (a.phone.isNotBlank()) Icon(PhoneIcon, contentDescription = null, modifier = Modifier.size(13.dp))
                                }
                            }
                        }
                    }
                }
                // Ảnh & ghi âm của ghi chú
                if (!extrasHidden) AttachmentStrip(noteFiles, onOpenImage = { viewerFile = it }, onManage = { showAttachments = true })
                HorizontalDivider(color = scheme.outlineVariant)
                EditorFooter(
                    text = value.text,
                    saving = store.isSaving,
                    onCamera = startCamera,
                    onPick = startPick,
                    onRecord = startRecord,
                    onAppointment = tagAppointment,
                    onHideKeyboard = if (wideTyping || (imeVisible && !EditorPrefs.dockVisible)) hideKeyboard else null,
                )
            }
        }

    }
    val calcPanel: @Composable (Boolean) -> Unit = { collapsible ->
        CalculatorPanel(
            calc = store.calc,
            onCollapse = if (collapsible) ({ calcOpen = false }) else null,
            onInsert = { text -> applyValue(insertAtCursor(value, text)) },
        )
    }
    // Cột máy tính bên phải: đủ cao thì phím giãn lấp kín cột (không chừa khoảng trống), thấp thì cuộn.
    val sideCalc: @Composable (Modifier) -> Unit = { modifier ->
        BoxWithConstraints(modifier) {
            if (maxHeight >= 360.dp) {
                CalculatorPanel(
                    calc = store.calc,
                    onCollapse = null,
                    onInsert = { text -> applyValue(insertAtCursor(value, text)) },
                    modifier = Modifier.fillMaxSize(),
                    fill = true,
                )
            } else {
                Box(Modifier.fillMaxSize().verticalScroll(rememberScrollState())) { calcPanel(false) }
            }
        }
    }
    val hideKeyboardButton: @Composable (Dp) -> Unit = { size ->
        Surface(
            onClick = hideKeyboard,
            shape = RoundedCornerShape(14.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.size(width = size + 8.dp, height = size),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(KeyboardHideIcon, contentDescription = stringResource(R.string.hide_keyboard), tint = scheme.primary)
            }
        }
    }

    Surface(Modifier.fillMaxSize(), color = scheme.background) {
        BoxWithConstraints(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
        ) {
            // maxHeight đã trừ bàn phím. Chia bố cục theo chỗ thực có:
            val screenW = maxWidth
            val screenH = maxHeight
            // Hai cột: máy tính bảng, điện thoại gập mở, hoặc điện thoại xoay ngang.
            val wide = maxWidth >= 720.dp || (maxWidth > maxHeight && maxWidth >= 560.dp)
            val sideWidth = when {
                maxWidth >= 1000.dp -> 392.dp
                maxWidth >= 720.dp -> 344.dp
                else -> 332.dp   // xoay ngang: banner tối thiểu 320dp + lề
            }
            // Đồng hồ 1 dòng khi: đang gõ, màn hình thấp, hoặc (điện thoại) đang mở bảng máy tính/lịch hẹn
            val compactHeader = imeVisible || maxHeight < 520.dp ||
                (calcOpen && EditorPrefs.dockVisible && !(maxWidth >= 720.dp || (maxWidth > maxHeight && maxWidth >= 560.dp)))
            val showTabs = true                      // luôn có thanh tab (nút + tạo ghi chú mới)
            val typingCalcRow = imeVisible && !wide  // điện thoại dọc: thanh máy tính + nút ẩn bàn phím sát bàn phím
            val adOnTop = imeVisible
            // Bề rộng banner: tối đa 480dp (để không quá cao trên màn hình rộng), vừa cột máy tính khi 2 cột.
            val adWidth = if (wide) sideWidth.value.toInt() - 12 else minOf(maxWidth.value.toInt() - 24, 480)
            // Quảng cáo: 1 banner (đổi chỗ đáy ↔ đầu màn hình khi gõ, không tải lại).
            // Màn hình rộng đủ cao: cột phải dùng khung 300×250 lấp chỗ trống dưới máy tính.
            val bannerSize = rememberAdSize(adWidth)
            val bannerHolder = rememberAdHolder(bannerSize)
            val mrecSize = rememberAdSize(300, mrec = true)
            val useMrec = wide && !imeVisible && screenH >= 660.dp
            val mrecHolder = if (wide) rememberAdHolder(mrecSize, mrec = true) else null
            val banner: @Composable () -> Unit = { AdSlot(bannerHolder, bannerSize) }
            val isWideTyping = imeVisible && wide
            val hideExtras = (imeVisible && !wide) || (!wide && calcOpen && EditorPrefs.dockVisible) || screenH < 420.dp
            SideEffect { wideTyping = isWideTyping; extrasHidden = hideExtras }

            val header: @Composable () -> Unit = {
                if (compactHeader) {
                    CompactHeader(
                        isDark = isDark,
                        onToggleTheme = onToggleTheme,
                        onLanguageClick = { showLanguages = true },
                        onCalendarClick = { showMonth = true },
                        updateLabel = updateLabel,
                        onUpdateClick = onUpdateClick,
                        onHideKeyboard = null,
                    )
                } else {
                    ClockHeader(
                        isDark = isDark,
                        onToggleTheme = onToggleTheme,
                        onLanguageClick = { showLanguages = true },
                        onCalendarClick = { showMonth = true },
                        updateLabel = updateLabel,
                        onUpdateClick = onUpdateClick,
                    )
                }
            }
            val topAd: @Composable () -> Unit = {
                if (adOnTop) Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) { banner() }
            }

            val dockOn = EditorPrefs.dockVisible
            val calcMode = EditorPrefs.dockMode == DockMode.CALC
            val setCalc = { EditorPrefs.setDockMode(context, DockMode.CALC) }
            val setAppts = { EditorPrefs.setDockMode(context, DockMode.APPOINTMENTS) }
            val hideDock = { EditorPrefs.setDockVisible(context, false) }
            val apptsPanel: @Composable (Modifier, (() -> Unit)?) -> Unit = { mod, collapse ->
                AppointmentsPanel(
                    store = appts,
                    noteTitleOf = noteTitleOf,
                    onNew = newAppointment,
                    onEdit = { a -> editingAppt = a; editingIsNew = false; editingFromNote = a.noteId == current.id },
                    onOpenNote = { id -> if (notes.any { it.id == id }) store.select(id) },
                    modifier = mod,
                    onCollapse = collapse,
                    header = { DockSwitcher(calcMode, setCalc, setAppts, hideDock, showLabels = screenW >= 360.dp) },
                )
            }

            if (wide) {
                // Hai cột: ghi chú bên trái, máy tính / lịch hẹn bên phải (ẩn được để ghi chú rộng hết màn hình).
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        header()
                        topAd()
                        if (showTabs) tabBar()
                        editorCard(Modifier.weight(1f))
                        if (!dockOn && !adOnTop) {
                            Box(Modifier.padding(start = 12.dp, end = 12.dp, top = 8.dp)) { banner() }
                        }
                        Spacer(Modifier.height(10.dp))
                    }
                    if (dockOn) {
                        Column(
                            Modifier
                                .width(sideWidth)
                                .fillMaxHeight()
                                .padding(top = 10.dp, end = 12.dp, bottom = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            if (calcMode) {
                                DockSwitcher(calcMode, setCalc, setAppts, hideDock, showLabels = true)
                                sideCalc(Modifier.weight(1f).fillMaxWidth())
                            } else {
                                apptsPanel(Modifier.weight(1f), null)
                            }
                            if (!adOnTop) {
                                if (useMrec) AdSlot(mrecHolder, mrecSize) else banner()
                            }
                        }
                    }
                }
            } else {
                Column(Modifier.fillMaxSize()) {
                    header()
                    topAd()
                    if (showTabs) tabBar()
                    editorCard(Modifier.weight(1f))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, top = if (imeVisible) 6.dp else 10.dp, bottom = if (imeVisible) 6.dp else 10.dp)
                            .animateContentSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        if (!imeVisible) {
                            if (dockOn) {
                                if (calcOpen) {
                                    if (calcMode) {
                                        DockSwitcher(calcMode, setCalc, setAppts, hideDock, showLabels = true)
                                        // Màn hình thấp: máy tính không chiếm quá ~60% chiều cao, cuộn được.
                                        Box(Modifier.heightIn(max = screenH * 0.56f).verticalScroll(rememberScrollState())) {
                                            calcPanel(true)
                                        }
                                    } else {
                                        apptsPanel(Modifier.heightIn(max = screenH * 0.5f), { calcOpen = false })
                                    }
                                } else {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        DockSwitcher(calcMode, setCalc, setAppts, null, showLabels = false, height = 60.dp)
                                        Box(Modifier.weight(1f)) {
                                            if (calcMode) CalculatorBar(calc = store.calc, onExpand = openCalc, hideIcon = true)
                                            else AppointmentBar(appts, onExpand = openCalc, hideIcon = true)
                                        }
                                    }
                                }
                            }
                            banner()
                        }
                        if (typingCalcRow && dockOn) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(Modifier.weight(1f)) {
                                    if (calcMode) CalculatorBar(calc = store.calc, onExpand = openCalc, compact = true)
                                    else AppointmentBar(appts, onExpand = openCalc, compact = true)
                                }
                                hideKeyboardButton(48.dp)
                            }
                        }
                    }
                }
            }
        }
    }

    // ----- Hộp thoại lịch hẹn / ảnh / ghi âm -----
    editingAppt?.let { a ->
        AppointmentEditorDialog(
            initial = a,
            isNew = editingIsNew,
            noteTitle = if (editingFromNote) current.displayTitle(untitled) else null,
            onSave = { saved, insertTag ->
                appts.upsert(saved)
                if (insertTag) applyValue(insertAtCursor(value, appointmentTag(saved, locale) + " "))
                if (!Reminders.canNotify(context) && Build.VERSION.SDK_INT >= 33) {
                    notifPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                editingAppt = null
            },
            onDelete = if (editingIsNew) null else ({ appts.delete(a.id); editingAppt = null }),
            onDismiss = { editingAppt = null },
        )
    }
    if (showAttachments) {
        AttachmentsDialog(
            noteTitle = current.displayTitle(untitled),
            files = noteFiles,
            onOpenImage = { viewerFile = it },
            onDelete = { attachments.delete(current.id, it) },
            onTakePhoto = { showAttachments = false; startCamera() },
            onPickPhoto = { showAttachments = false; startPick() },
            onRecord = { showAttachments = false; startRecord() },
            onDismiss = { showAttachments = false },
        )
    }
    viewerFile?.let { f ->
        PhotoViewer(f, onDelete = { attachments.delete(current.id, f); viewerFile = null }, onDismiss = { viewerFile = null })
    }
    recorderFile?.let { f ->
        RecorderDialog(
            outFile = f,
            onSaved = { attachments.refresh(current.id); recorderFile = null },
            onCancel = { attachments.refresh(current.id); recorderFile = null },
        )
    }

    renameTarget?.let { target ->
        var name by remember(target.id) { mutableStateOf(target.title) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text(stringResource(R.string.rename_title)) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    singleLine = true,
                    label = { Text(stringResource(R.string.rename_label)) },
                    placeholder = { Text(target.displayTitle(untitled)) },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    store.rename(target.id, name.trim())
                    renameTarget = null
                }) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.close_title, target.displayTitle(untitled))) },
            text = { Text(stringResource(R.string.close_message)) },
            confirmButton = {
                TextButton(onClick = {
                    store.delete(target.id)
                    deleteTarget = null
                }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.action_keep)) }
            },
        )
    }

    if (showMonth) {
        MonthCalendarDialog(onDismiss = { showMonth = false })
    }

    if (showBackup) {
        BackupDialog(
            store = store,
            currentLanguage = currentLanguage,
            onLanguageChosen = onLanguageChosen,
            onDismiss = { showBackup = false },
        )
    }

    if (showLanguages) {
        LanguageDialog(
            current = currentLanguage,
            onPick = { tag ->
                showLanguages = false
                onLanguageChosen(tag)
            },
            onDismiss = { showLanguages = false },
        )
    }
}

/** Ngôn ngữ đang hiển thị (dùng cho định dạng ngày giờ và âm lịch). */
@Composable
private fun currentLocale(): Locale = LocalConfiguration.current.locales[0]

/** Logo nhỏ của app (luôn hiện ở đầu màn hình). */
@Composable
private fun AppLogo(size: Dp) {
    Image(
        painter = painterResource(R.drawable.logo_mark),
        contentDescription = stringResource(R.string.app_name),
        modifier = Modifier.size(size),
    )
}

/** Đồng hồ thu gọn 1 dòng (khi đang gõ hoặc màn hình thấp): giờ · ngày · âm lịch · các nút nhanh. */
@Composable
private fun CompactHeader(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLanguageClick: () -> Unit,
    onCalendarClick: () -> Unit,
    updateLabel: String?,
    onUpdateClick: () -> Unit,
    onHideKeyboard: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val locale = currentLocale()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }
    val is24h = DateFormat.is24HourFormat(context)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm a", locale))
    val date = now.toLocalDate()
    val dateShort = date.format(
        DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, "EEEdMMM"), locale)
    )
    val language = locale.language
    val lunar = remember(date, language) {
        LunarCalendar.fromSolar(date.dayOfMonth, date.monthValue, date.year, LunarCalendar.timeZoneFor(language))
    }
    val lunarText = stringResource(
        if (lunar.leap) R.string.lunar_date_leap else R.string.lunar_date,
        lunar.day,
        lunar.month,
    )
    val small = 32.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        AppLogo(22.dp)
        Text(
            time,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = scheme.onBackground,
            maxLines = 1,
            style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
        )
        Text(
            dateShort,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        Surface(onClick = onCalendarClick, shape = RoundedCornerShape(10.dp), color = extra.accentSoft, modifier = Modifier.height(small)) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(Icons.Filled.DateRange, contentDescription = stringResource(R.string.lunar_label), tint = scheme.primary, modifier = Modifier.size(13.dp))
                Text(lunarText, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground, maxLines = 1)
            }
        }
        if (updateLabel != null) {
            Surface(
                onClick = onUpdateClick,
                shape = RoundedCornerShape(10.dp),
                color = extra.accentSoft,
                border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.35f)),
                modifier = Modifier.size(small),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(UpdateIcon, contentDescription = updateLabel, tint = scheme.primary, modifier = Modifier.size(16.dp))
                }
            }
        }
        val iconButton: @Composable (ImageVector, String, () -> Unit) -> Unit = { icon, label, onClick ->
            Surface(
                onClick = onClick,
                shape = RoundedCornerShape(10.dp),
                color = scheme.surface,
                border = BorderStroke(1.dp, scheme.outlineVariant),
                modifier = Modifier.size(small),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, contentDescription = label, tint = scheme.onSurface, modifier = Modifier.size(16.dp))
                }
            }
        }
        iconButton(GlobeIcon, stringResource(R.string.language), onLanguageClick)
        iconButton(
            if (isDark) SunIcon else MoonIcon,
            stringResource(if (isDark) R.string.theme_to_light else R.string.theme_to_dark),
            onToggleTheme,
        )
        if (onHideKeyboard != null) iconButton(KeyboardHideIcon, stringResource(R.string.hide_keyboard), onHideKeyboard)
    }
}

/** Đồng hồ + ngày dương + ngày âm + nút ngôn ngữ và sáng/tối, luôn nằm trên cùng. */
@Composable
private fun ClockHeader(
    isDark: Boolean,
    onToggleTheme: () -> Unit,
    onLanguageClick: () -> Unit,
    onCalendarClick: () -> Unit,
    updateLabel: String?,
    onUpdateClick: () -> Unit,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val locale = currentLocale()
    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            // Ngủ đến đúng đầu phút tiếp theo.
            delay(60_000L - System.currentTimeMillis() % 60_000L + 50L)
        }
    }
    // Màn hình hẹp (< 380dp): nhãn cập nhật chỉ còn biểu tượng để đồng hồ và logo không bị ép
    val narrow = LocalConfiguration.current.screenWidthDp < 380
    val is24h = DateFormat.is24HourFormat(context)
    val time = now.format(DateTimeFormatter.ofPattern(if (is24h) "HH:mm" else "h:mm", locale))
    val amPm = if (is24h) "" else now.format(DateTimeFormatter.ofPattern("a", locale))
    val date = now.toLocalDate()
    val dateLine = date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale))
        .replaceFirstChar { it.titlecase(locale) }

    val language = locale.language
    val lunar = remember(date, language) {
        LunarCalendar.fromSolar(date.dayOfMonth, date.monthValue, date.year, LunarCalendar.timeZoneFor(language))
    }
    val zodiac = stringArrayResource(R.array.zodiac).toList()
    val yearName = LunarCalendar.yearName(lunar.year, LunarCalendar.styleFor(language), zodiac)
    val lunarText = stringResource(
        if (lunar.leap) R.string.lunar_date_leap else R.string.lunar_date,
        lunar.day,
        lunar.month,
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 8.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            // Logo nhỏ của app, ngang hàng với các nút nhanh bên phải
            Row(
                modifier = Modifier.height(36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp),
            ) {
                AppLogo(24.dp)
                Text(
                    stringResource(R.string.app_name),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.2.sp,
                    color = scheme.onBackground,
                    maxLines = 1,
                )
            }
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    time,
                    fontSize = if (narrow) 38.sp else 44.sp,
                    lineHeight = 46.sp,
                    maxLines = 1,
                    softWrap = false,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = (-1.5).sp,
                    color = scheme.onBackground,
                    style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                )
                if (amPm.isNotEmpty()) {
                    Text(
                        amPm,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = scheme.onSurfaceVariant,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.padding(bottom = 6.dp),
                    )
                }
            }
            Text(
                dateLine,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = scheme.onSurfaceVariant,
                maxLines = 2,
            )
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                // Nhãn "Có bản mới" (cùng tông màu với thẻ âm lịch), bấm để cập nhật
                if (updateLabel != null) {
                    Surface(
                        onClick = onUpdateClick,
                        shape = RoundedCornerShape(10.dp),
                        color = extra.accentSoft,
                        border = BorderStroke(1.dp, scheme.primary.copy(alpha = 0.35f)),
                        modifier = Modifier.height(36.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(start = if (narrow) 10.dp else 8.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(UpdateIcon, contentDescription = updateLabel, tint = scheme.primary, modifier = Modifier.size(16.dp))
                            if (!narrow) Text(
                                updateLabel,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = scheme.primary,
                                maxLines = 1,
                            )
                        }
                    }
                }
                // Nút đổi ngôn ngữ nhanh
                Surface(
                    onClick = onLanguageClick,
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                    modifier = Modifier.height(36.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                    ) {
                        Icon(
                            GlobeIcon,
                            contentDescription = stringResource(R.string.language),
                            tint = scheme.onSurface,
                            modifier = Modifier.size(17.dp),
                        )
                        Text(
                            languageCode(language),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp,
                            color = scheme.onSurface,
                        )
                    }
                }
                // Nút chuyển sáng/tối nhanh
                Surface(
                    onClick = onToggleTheme,
                    shape = RoundedCornerShape(10.dp),
                    color = scheme.surface,
                    border = BorderStroke(1.dp, scheme.outlineVariant),
                    modifier = Modifier.size(36.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            if (isDark) SunIcon else MoonIcon,
                            contentDescription = stringResource(if (isDark) R.string.theme_to_light else R.string.theme_to_dark),
                            tint = scheme.onSurface,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
            }
            Surface(onClick = onCalendarClick, shape = RoundedCornerShape(14.dp), color = extra.accentSoft) {
                Column(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Icon(Icons.Filled.DateRange, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(13.dp))
                        Text(
                            stringResource(R.string.lunar_label),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp,
                            color = scheme.primary,
                        )
                    }
                    Text(lunarText, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = scheme.onBackground)
                    Text(
                        stringResource(R.string.lunar_year, yearName),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = scheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** "vi" → "VI"; tiếng Indonesia trên Android có mã cũ "in" → hiện "ID". */
private fun languageCode(language: String): String = when (language) {
    "in" -> "ID"
    else -> language.uppercase(Locale.ROOT)
}

@Composable
private fun TabBar(
    notes: List<Note>,
    selectedId: Long,
    untitled: String,
    onSelect: (Long) -> Unit,
    onAdd: () -> Unit,
    menu: @Composable () -> Unit,
) {
    val scheme = MaterialTheme.colorScheme
    val listState = rememberLazyListState()
    val selectedIndex = notes.indexOfFirst { it.id == selectedId }.coerceAtLeast(0)
    LaunchedEffect(selectedIndex, notes.size) {
        listState.animateScrollToItem(selectedIndex)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 4.dp, top = 2.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        LazyRow(
            state = listState,
            modifier = Modifier
                .weight(1f)
                // Mép phải mờ dần: gợi ý còn tab phía sau, không cần thanh cuộn.
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fade = 36.dp.toPx()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Black, Color.Transparent),
                            startX = size.width - fade,
                            endX = size.width,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
            contentPadding = PaddingValues(end = 28.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            itemsIndexed(notes, key = { _, n -> n.id }) { _, note ->
                val active = note.id == selectedId
                Surface(
                    onClick = { onSelect(note.id) },
                    shape = RoundedCornerShape(50),
                    color = if (active) scheme.primary else scheme.surface,
                    contentColor = if (active) scheme.onPrimary else scheme.onSurfaceVariant,
                    border = BorderStroke(1.dp, if (active) scheme.primary else scheme.outlineVariant),
                ) {
                    Text(
                        note.displayTitle(untitled),
                        modifier = Modifier
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                            .widthIn(max = 160.dp),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
        Surface(
            onClick = onAdd,
            shape = RoundedCornerShape(12.dp),
            color = scheme.surface,
            border = BorderStroke(1.dp, scheme.outlineVariant),
            modifier = Modifier.size(40.dp),
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Add, contentDescription = stringResource(R.string.tab_add), tint = scheme.onSurface)
            }
        }
        menu()
    }
}

@Composable
private fun EditorFooter(
    text: String,
    saving: Boolean,
    onCamera: () -> Unit,
    onPick: () -> Unit,
    onRecord: () -> Unit,
    onAppointment: () -> Unit,
    onHideKeyboard: (() -> Unit)?,
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val lines = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    var photoMenu by remember { mutableStateOf(false) }

    @Composable
    fun tool(icon: ImageVector, label: String, active: Boolean = false, onClick: () -> Unit) {
        Surface(
            onClick = onClick,
            shape = RoundedCornerShape(9.dp),
            color = if (active) extra.accentSoft else Color.Transparent,
            contentColor = if (active) scheme.primary else scheme.onSurfaceVariant,
            modifier = Modifier.size(32.dp),
        ) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, modifier = Modifier.size(18.dp)) }
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box {
            tool(CameraIcon, stringResource(R.string.photo_take)) { photoMenu = true }
            DropdownMenu(expanded = photoMenu, onDismissRequest = { photoMenu = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_take)) },
                    leadingIcon = { Icon(CameraIcon, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = { photoMenu = false; onCamera() },
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.photo_pick)) },
                    leadingIcon = { Icon(GalleryGlyph, contentDescription = null, modifier = Modifier.size(20.dp)) },
                    onClick = { photoMenu = false; onPick() },
                )
            }
        }
        tool(MicIcon, stringResource(R.string.rec_record), onClick = onRecord)
        tool(AlarmIcon, stringResource(R.string.appt_add), onClick = onAppointment)
        Text(
            "$lines · ${text.length}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(start = 6.dp),
        )
        if (saving) {
            Text("…", fontSize = 12.sp, color = scheme.onSurfaceVariant)
        } else {
            Icon(Icons.Filled.Check, contentDescription = stringResource(R.string.status_saved), tint = scheme.primary, modifier = Modifier.size(14.dp))
        }
        Spacer(Modifier.width(4.dp))
        tool(LineNumbersIcon, stringResource(R.string.line_numbers), active = EditorPrefs.lineNumbers) {
            EditorPrefs.setLineNumbers(context, !EditorPrefs.lineNumbers)
        }
        tool(WrapIcon, stringResource(R.string.word_wrap), active = EditorPrefs.wordWrap) {
            EditorPrefs.setWordWrap(context, !EditorPrefs.wordWrap)
        }
        tool(
            if (EditorPrefs.dockVisible) PanelHideIcon else PanelShowIcon,
            stringResource(if (EditorPrefs.dockVisible) R.string.dock_hide else R.string.dock_show),
            active = !EditorPrefs.dockVisible,
        ) { EditorPrefs.setDockVisible(context, !EditorPrefs.dockVisible) }
        if (onHideKeyboard != null) {
            Spacer(Modifier.width(2.dp))
            Surface(
                onClick = onHideKeyboard,
                shape = RoundedCornerShape(10.dp),
                color = extra.accentSoft,
                modifier = Modifier.size(width = 40.dp, height = 30.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(KeyboardHideIcon, contentDescription = stringResource(R.string.hide_keyboard), tint = scheme.primary, modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

/** Dòng chứa con trỏ (để lấy nội dung lịch hẹn khi bấm ⏰). */
private fun currentLine(v: TextFieldValue): String {
    val t = v.text
    val pos = v.selection.start.coerceIn(0, t.length)
    val start = t.lastIndexOf('\n', (pos - 1).coerceAtLeast(0)).let { if (it < 0 || pos == 0) 0 else it + 1 }
    val end = t.indexOf('\n', pos).let { if (it < 0) t.length else it }
    return if (start <= end) t.substring(start, end) else ""
}

/** Chọn ngôn ngữ: "Theo hệ thống" + 13 ngôn ngữ (tên viết bằng chính ngôn ngữ đó). */
@Composable
private fun LanguageDialog(current: String, onPick: (String) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.language)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                LanguageRow(stringResource(R.string.language_system), selected = current.isEmpty()) { onPick("") }
                for ((tag, name) in APP_LANGUAGES) {
                    LanguageRow(name, selected = current == tag) { onPick(tag) }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel)) }
        },
    )
}

@Composable
private fun LanguageRow(name: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = null)
        Spacer(Modifier.width(12.dp))
        Text(name, fontSize = 16.sp)
    }
}

/** Lịch tháng: ngày dương lớn, ngày âm nhỏ bên dưới (mùng 1 hiện cả tháng âm). */
@Composable
private fun MonthCalendarDialog(onDismiss: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val locale = currentLocale()
    val language = locale.language
    val tz = LunarCalendar.timeZoneFor(language)
    val style = LunarCalendar.styleFor(language)
    val zodiac = stringArrayResource(R.array.zodiac).toList()
    val today = remember { LocalDate.now() }
    var month by remember { mutableStateOf(YearMonth.from(today)) }
    val midLunar = remember(month, tz) { LunarCalendar.fromSolar(15, month.monthValue, month.year, tz) }
    val firstDayOfWeek: DayOfWeek = remember(locale) { WeekFields.of(locale).firstDayOfWeek }
    val title = month.format(DateTimeFormatter.ofPattern("LLLL yyyy", locale)).replaceFirstChar { it.titlecase(locale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_close)) } },
        dismissButton = {
            if (YearMonth.from(today) != month) {
                TextButton(onClick = { month = YearMonth.from(today) }) { Text(stringResource(R.string.calendar_today)) }
            }
        },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { month = month.minusMonths(1) }) { Text("‹", fontSize = 26.sp) }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(
                        stringResource(R.string.lunar_year, LunarCalendar.yearName(midLunar.year, style, zodiac)),
                        fontSize = 13.sp,
                        color = scheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
                IconButton(onClick = { month = month.plusMonths(1) }) { Text("›", fontSize = 26.sp) }
            }
        },
        text = {
            val first = month.atDay(1)
            val offset = (first.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
            val days = month.lengthOfMonth()
            val rows = (offset + days + 6) / 7
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row {
                    for (i in 0 until 7) {
                        val dow = firstDayOfWeek.plus(i.toLong())
                        Text(
                            dow.getDisplayName(TextStyle.SHORT, locale),
                            modifier = Modifier.weight(1f),
                            textAlign = TextAlign.Center,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            color = if (dow == DayOfWeek.SUNDAY) extra.sunday else scheme.onSurfaceVariant,
                        )
                    }
                }
                for (r in 0 until rows) {
                    Row {
                        for (c in 0 until 7) {
                            val dayNum = r * 7 + c - offset + 1
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(0.85f),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (dayNum in 1..days) {
                                    val date = month.atDay(dayNum)
                                    val l = LunarCalendar.fromSolar(dayNum, month.monthValue, month.year, tz)
                                    val isToday = date == today
                                    Column(
                                        modifier = Modifier
                                            .size(38.dp)
                                            .clip(CircleShape)
                                            .background(if (isToday) scheme.primary else Color.Transparent),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.Center,
                                    ) {
                                        Text(
                                            "$dayNum",
                                            fontSize = 15.sp,
                                            lineHeight = 17.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = when {
                                                isToday -> scheme.onPrimary
                                                date.dayOfWeek == DayOfWeek.SUNDAY -> extra.sunday
                                                else -> scheme.onSurface
                                            },
                                        )
                                        Text(
                                            if (l.day == 1) "${l.day}/${l.month}" else "${l.day}",
                                            fontSize = 10.sp,
                                            lineHeight = 11.sp,
                                            fontWeight = if (l.day == 1 || l.day == 15) FontWeight.Bold else FontWeight.Normal,
                                            color = if (isToday) scheme.onPrimary else scheme.onSurfaceVariant,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
    )
}

/** Chèn chữ vào đúng vị trí con trỏ (thay phần đang bôi đen nếu có). */
private fun insertAtCursor(v: TextFieldValue, insert: String): TextFieldValue {
    val start = minOf(v.selection.start, v.selection.end).coerceIn(0, v.text.length)
    val end = maxOf(v.selection.start, v.selection.end).coerceIn(0, v.text.length)
    val needsSpace = start > 0 && !v.text[start - 1].isWhitespace()
    val piece = (if (needsSpace) " " else "") + insert
    val newText = v.text.substring(0, start) + piece + v.text.substring(end)
    return TextFieldValue(newText, TextRange(start + piece.length))
}

private fun copyToClipboard(context: Context, text: String, label: String, copiedMessage: String) {
    val cm = context.getSystemService(ClipboardManager::class.java)
    cm?.setPrimaryClip(ClipData.newPlainText(label, text))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
    }
}

private fun shareNote(context: Context, title: String, content: String, chooserTitle: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, title)
        putExtra(Intent.EXTRA_TEXT, content)
    }
    context.startActivity(Intent.createChooser(send, chooserTitle))
}
