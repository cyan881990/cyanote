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
import vn.ghichunhanh.app.ads.BannerAd
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

    // ----- Các khối giao diện (ghép lại theo kích thước màn hình bên dưới) -----
    val hideKeyboard = { keyboard?.hide(); focusManager.clearFocus() }
    val openCalc = { hideKeyboard(); calcOpen = true }
    val tabBar: @Composable () -> Unit = {
        TabBar(
            notes = notes,
            selectedId = current.id,
            untitled = untitled,
            onSelect = { store.select(it) },
            onAdd = { store.newNote() },
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
                // Tự cuộn tới chỗ đang gõ (kể cả khi bàn phím vừa bật làm vùng ghi chú thu nhỏ).
                val scroll = rememberScrollState()
                var viewportPx by remember { mutableStateOf(0) }
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
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .onSizeChanged { viewportPx = it.height }
                        .verticalScroll(scroll)
                ) {
                    BasicTextField(
                        value = value,
                        onValueChange = { applyValue(it) },
                        onTextLayout = { layout = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = with(density) { viewportPx.toDp() })
                            .focusRequester(focusRequester)
                            .padding(horizontal = 20.dp, vertical = 16.dp),
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
                HorizontalDivider(color = scheme.outlineVariant)
                StatusRow(text = value.text, saving = store.isSaving)
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
    // Chỉ MỘT banner, di chuyển giữa đáy (bình thường) và đầu màn hình (khi gõ) mà không bị tạo lại.
    val banner = remember { movableContentOf { widthDp: Int -> BannerAd(visible = true, widthDp = widthDp) } }

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
                maxWidth >= 1000.dp -> 380.dp
                maxWidth >= 720.dp -> 330.dp
                else -> 320.dp
            }
            val compactHeader = imeVisible || maxHeight < 520.dp  // đang gõ hoặc màn hình thấp → đồng hồ 1 dòng
            val showTabs = !imeVisible || maxHeight >= 400.dp
            val typingCalcRow = imeVisible && !wide && maxHeight >= 300.dp
            val headerHideButton = imeVisible && !typingCalcRow
            val adOnTop = imeVisible
            // Bề rộng banner: tối đa 480dp (để không quá cao trên màn hình rộng), vừa cột máy tính khi 2 cột.
            val adWidth = if (wide) sideWidth.value.toInt() else minOf(maxWidth.value.toInt() - 24, 480)

            val header: @Composable () -> Unit = {
                if (compactHeader) {
                    CompactHeader(
                        isDark = isDark,
                        onToggleTheme = onToggleTheme,
                        onLanguageClick = { showLanguages = true },
                        onCalendarClick = { showMonth = true },
                        updateLabel = updateLabel,
                        onUpdateClick = onUpdateClick,
                        onHideKeyboard = if (headerHideButton) hideKeyboard else null,
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
                if (adOnTop) Box(Modifier.fillMaxWidth().padding(start = 12.dp, end = 12.dp, bottom = 6.dp)) { banner(adWidth) }
            }

            if (wide) {
                // Hai cột: ghi chú bên trái, máy tính luôn mở bên phải.
                Row(Modifier.fillMaxSize()) {
                    Column(Modifier.weight(1f).fillMaxHeight()) {
                        header()
                        topAd()
                        if (showTabs) tabBar()
                        editorCard(Modifier.weight(1f))
                        Spacer(Modifier.height(10.dp))
                    }
                    Column(
                        Modifier
                            .width(sideWidth)
                            .fillMaxHeight()
                            .padding(top = 10.dp, end = 12.dp, bottom = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Column(
                            Modifier.weight(1f).verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.Bottom,
                        ) { calcPanel(false) }
                        if (!adOnTop) banner(adWidth)
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
                            if (calcOpen) {
                                // Màn hình thấp: máy tính không chiếm quá ~60% chiều cao, cuộn được.
                                Box(Modifier.heightIn(max = screenH * 0.62f).verticalScroll(rememberScrollState())) {
                                    calcPanel(true)
                                }
                            } else {
                                CalculatorBar(calc = store.calc, onExpand = openCalc)
                            }
                            banner(adWidth)
                        }
                        if (typingCalcRow) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Box(Modifier.weight(1f)) {
                                    CalculatorBar(calc = store.calc, onExpand = openCalc, compact = true)
                                }
                                hideKeyboardButton(48.dp)
                            }
                        }
                    }
                }
            }
        }
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
            .padding(start = 20.dp, end = 16.dp, top = 14.dp, bottom = 10.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    time,
                    fontSize = 44.sp,
                    lineHeight = 46.sp,
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
                            modifier = Modifier.padding(start = 8.dp, end = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Icon(UpdateIcon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(16.dp))
                            Text(
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
private fun StatusRow(text: String, saving: Boolean) {
    val scheme = MaterialTheme.colorScheme
    val lines = if (text.isEmpty()) 1 else text.count { it == '\n' } + 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            stringResource(R.string.status_counts, lines, text.length),
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = scheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        if (saving) {
            Text(stringResource(R.string.status_saving), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = scheme.onSurfaceVariant)
        } else {
            Icon(Icons.Filled.Check, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(4.dp))
            Text(stringResource(R.string.status_saved), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = scheme.primary)
        }
    }
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
