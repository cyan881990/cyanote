package vn.ghichunhanh.app.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import vn.ghichunhanh.app.AttachmentsStore
import vn.ghichunhanh.app.R
import java.io.File

private fun durationMs(file: File): Long = runCatching {
    MediaMetadataRetriever().run {
        setDataSource(file.path)
        val d = extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
        release()
        d
    }
}.getOrDefault(0L)

internal fun formatDuration(ms: Long): String {
    val s = (ms / 1000).toInt()
    return "%d:%02d".format(s / 60, s % 60)
}

/** Chia sẻ ảnh / ghi âm qua ứng dụng khác. */
fun shareAttachment(context: Context, file: File, chooserTitle: String) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "app.cyanote.files", file)
        val send = Intent(Intent.ACTION_SEND)
            .setType(AttachmentsStore.mimeOf(file))
            .putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        context.startActivity(Intent.createChooser(send, chooserTitle))
    }
}

/** Mở video / tệp bằng ứng dụng phù hợp trên máy (trình xem PDF, Word, trình phát video…). */
fun openAttachment(context: Context, file: File) {
    runCatching {
        val uri = FileProvider.getUriForFile(context, "app.cyanote.files", file)
        context.startActivity(
            Intent(Intent.ACTION_VIEW).setDataAndType(uri, AttachmentsStore.mimeOf(file))
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        )
    }
}

/** Trình phát dùng chung: mỗi lúc chỉ phát một bản ghi âm. */
object AudioPlayer {
    private var player: MediaPlayer? = null
    var playing by mutableStateOf<File?>(null)
        private set

    fun toggle(file: File) {
        if (playing == file) { stop(); return }
        stop()
        runCatching {
            player = MediaPlayer().apply {
                setDataSource(file.path)
                setOnCompletionListener { stop() }
                prepare()
                start()
            }
            playing = file
        }
    }

    fun position(): Long = runCatching { player?.currentPosition?.toLong() ?: 0L }.getOrDefault(0L)

    fun stop() {
        runCatching { player?.stop() }
        runCatching { player?.release() }
        player = null
        playing = null
    }
}

@Composable
private fun Thumb(file: File, size: Int, modifier: Modifier) {
    val bmp by produceState<Bitmap?>(null, file) {
        value = withContext(Dispatchers.IO) {
            if (AttachmentsStore.isVideo(file)) AttachmentsStore.videoFrame(file) else AttachmentsStore.thumbnail(file, size)
        }
    }
    val extra = LocalExtraColors.current
    Box(modifier.background(extra.keyBg), contentAlignment = Alignment.Center) {
        bmp?.let {
            Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
        if (AttachmentsStore.isVideo(file)) {
            Box(Modifier.size(26.dp).clip(CircleShape).background(Color(0x99000000)), contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

/** Nút ghi âm thu gọn (phát/tạm dừng + thời lượng). */
@Composable
private fun AudioChip(file: File, onLongClick: (() -> Unit)? = null, height: androidx.compose.ui.unit.Dp = 56.dp) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val dur by produceState(0L, file) { value = withContext(Dispatchers.IO) { durationMs(file) } }
    val isPlaying = AudioPlayer.playing == file
    var pos by remember { mutableLongStateOf(0L) }
    LaunchedEffect(isPlaying) {
        while (isPlaying) { pos = AudioPlayer.position(); delay(250) }
        pos = 0L
    }
    Surface(
        onClick = { AudioPlayer.toggle(file) },
        shape = RoundedCornerShape(14.dp),
        color = if (isPlaying) scheme.primary else extra.accentSoft,
        contentColor = if (isPlaying) scheme.onPrimary else scheme.primary,
        modifier = Modifier.height(height),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (isPlaying) PauseGlyph() else Icon(Icons.Filled.PlayArrow, contentDescription = stringResource(R.string.att_play), modifier = Modifier.size(22.dp))
            Icon(MicIcon, contentDescription = null, modifier = Modifier.size(16.dp))
            Text(
                if (isPlaying) formatDuration(pos) + " / " + formatDuration(dur) else formatDuration(dur),
                fontSize = 14.sp, fontWeight = FontWeight.Bold,
            )
        }
    }
}

/** Tệp đính kèm (PDF, Word…): biểu tượng + tên, bấm để mở. */
@Composable
private fun FileChip(file: File, height: androidx.compose.ui.unit.Dp = 56.dp) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = { openAttachment(context, file) },
        shape = RoundedCornerShape(14.dp),
        color = LocalExtraColors.current.keyBg,
        modifier = Modifier.height(height).widthIn(max = 180.dp),
    ) {
        Row(Modifier.padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(scheme.primary), contentAlignment = Alignment.Center) {
                Text(file.extension.uppercase().take(4), color = scheme.onPrimary, fontSize = 9.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            }
            Text(AttachmentsStore.displayName(file), fontSize = 13.sp, fontWeight = FontWeight.Medium, maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis, color = scheme.onSurface)
        }
    }
}

@Composable
private fun PauseGlyph() {
    Row(Modifier.size(22.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.width(5.dp).height(14.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp)))
        Spacer(Modifier.width(4.dp))
        Box(Modifier.width(5.dp).height(14.dp).background(MaterialTheme.colorScheme.onPrimary, RoundedCornerShape(1.dp)))
    }
}

/** Dải ảnh, video, ghi âm & tệp của ghi chú, nằm ngay trên dòng trạng thái (luôn hiện; [compact] khi chật chỗ). */
@Composable
fun AttachmentStrip(files: List<File>, onOpenImage: (File) -> Unit, onManage: () -> Unit, compact: Boolean = false) {
    if (files.isEmpty()) return
    val context = LocalContext.current
    val tile = if (compact) 40.dp else 56.dp
    LazyRow(
        Modifier.fillMaxWidth().padding(vertical = if (compact) 4.dp else 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp),
    ) {
        items(files, key = { it.path }) { f ->
            when {
                AttachmentsStore.isImage(f) -> Thumb(f, 160, Modifier.size(tile).clip(RoundedCornerShape(12.dp)).clickable { onOpenImage(f) })
                AttachmentsStore.isVideo(f) -> Thumb(f, 160, Modifier.size(tile).clip(RoundedCornerShape(12.dp)).clickable { openAttachment(context, f) })
                AttachmentsStore.isAudio(f) -> AudioChip(f, height = tile)
                else -> FileChip(f, height = tile)
            }
        }
        item(key = "manage") {
            Surface(
                onClick = onManage,
                shape = RoundedCornerShape(12.dp),
                color = LocalExtraColors.current.keyBg,
                modifier = Modifier.size(tile),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text("${files.size}", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

/** Xem ảnh toàn màn hình (xoá / chia sẻ). */
@Composable
fun PhotoViewer(file: File, onDelete: () -> Unit, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val shareTitle = stringResource(R.string.photo_share)
    var confirm by remember { mutableStateOf(false) }
    val bmp by produceState<Bitmap?>(null, file) { value = withContext(Dispatchers.IO) { AttachmentsStore.thumbnail(file, 1600) } }
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            bmp?.let {
                Image(it.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Fit, modifier = Modifier.fillMaxSize())
            }
            Row(Modifier.align(Alignment.TopEnd).padding(12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    Triple(Icons.Filled.Share, shareTitle) { shareAttachment(context, file, shareTitle) },
                    Triple(Icons.Filled.Delete, stringResource(R.string.action_delete)) { confirm = true },
                    Triple(Icons.Filled.Close, stringResource(R.string.action_cancel), onDismiss),
                ).forEach { (icon, label, action) ->
                    Surface(onClick = action, shape = CircleShape, color = Color(0x99000000), contentColor = Color.White, modifier = Modifier.size(44.dp)) {
                        Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label) }
                    }
                }
            }
        }
    }
    if (confirm) {
        AlertDialog(
            onDismissRequest = { confirm = false },
            title = { Text(stringResource(R.string.photo_delete_q)) },
            confirmButton = { TextButton(onClick = { confirm = false; onDelete() }) { Text(stringResource(R.string.action_delete), color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { confirm = false }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

/** Quản lý ảnh & ghi âm của một ghi chú. */
@Composable
fun AttachmentsDialog(
    noteTitle: String,
    files: List<File>,
    onOpenImage: (File) -> Unit,
    onDelete: (File) -> Unit,
    onTakePhoto: () -> Unit,
    onPickPhoto: () -> Unit,
    onRecord: () -> Unit,
    onDismiss: () -> Unit,
    onRecordVideo: () -> Unit = {},
    onAttachFile: () -> Unit = {},
) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    val shareTitle = stringResource(R.string.photo_share)
    val images = files.filter(AttachmentsStore::isImage)
    val audios = files.filter(AttachmentsStore::isAudio)
    val videos = files.filter(AttachmentsStore::isVideo)
    val others = files.filter(AttachmentsStore::isOther)
    var confirmAudio by remember { mutableStateOf<File?>(null) }
    DisposableEffect(Unit) { onDispose { AudioPlayer.stop() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column {
                Text(stringResource(R.string.att_manage))
                Text(noteTitle, fontSize = 13.sp, color = scheme.onSurfaceVariant, maxLines = 1)
            }
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(CameraIcon, stringResource(R.string.photo_take), onTakePhoto, Modifier.weight(1f))
                    QuickAction(VideoGlyph, stringResource(R.string.video_record), onRecordVideo, Modifier.weight(1f))
                    QuickAction(MicIcon, stringResource(R.string.rec_record), onRecord, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickAction(GalleryGlyph, stringResource(R.string.media_pick), onPickPhoto, Modifier.weight(1f))
                    QuickAction(FileGlyph, stringResource(R.string.file_attach), onAttachFile, Modifier.weight(1f))
                }
                if (files.isEmpty()) {
                    Text(stringResource(R.string.att_empty), color = scheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
                }
                if (images.isNotEmpty()) {
                    Text(stringResource(R.string.att_photos) + " · ${images.size}", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 13.sp)
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(84.dp),
                        modifier = Modifier.heightIn(max = 300.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(images, key = { it.path }) { f ->
                            Thumb(f, 200, Modifier.aspectRatio(1f).clip(RoundedCornerShape(10.dp)).clickable { onOpenImage(f) })
                        }
                    }
                }
                if (videos.isNotEmpty()) {
                    Text(stringResource(R.string.att_videos) + " · ${videos.size}", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 13.sp)
                    videos.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Thumb(f, 200, Modifier.size(64.dp).clip(RoundedCornerShape(10.dp)).clickable { openAttachment(context, f) })
                            Text(AttachmentsStore.displayName(f), fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1)
                            IconButton(onClick = { shareAttachment(context, f, shareTitle) }) { Icon(Icons.Filled.Share, contentDescription = shareTitle) }
                            IconButton(onClick = { confirmAudio = f }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = scheme.error) }
                        }
                    }
                }
                if (others.isNotEmpty()) {
                    Text(stringResource(R.string.att_files) + " · ${others.size}", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 13.sp)
                    others.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { FileChip(f) }
                            IconButton(onClick = { shareAttachment(context, f, shareTitle) }) { Icon(Icons.Filled.Share, contentDescription = shareTitle) }
                            IconButton(onClick = { confirmAudio = f }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = scheme.error) }
                        }
                    }
                }
                if (audios.isNotEmpty()) {
                    Text(stringResource(R.string.att_recordings) + " · ${audios.size}", fontWeight = FontWeight.Bold, color = scheme.primary, fontSize = 13.sp)
                    audios.forEach { f ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.weight(1f)) { AudioChip(f) }
                            IconButton(onClick = { shareAttachment(context, f, shareTitle) }) { Icon(Icons.Filled.Share, contentDescription = shareTitle) }
                            IconButton(onClick = { confirmAudio = f }) { Icon(Icons.Filled.Delete, contentDescription = stringResource(R.string.action_delete), tint = scheme.error) }
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } },
    )
    confirmAudio?.let { f ->
        AlertDialog(
            onDismissRequest = { confirmAudio = null },
            title = { Text(stringResource(if (AttachmentsStore.isAudio(f)) R.string.rec_delete_q else R.string.att_delete_q)) },
            confirmButton = {
                TextButton(onClick = { AudioPlayer.stop(); onDelete(f); confirmAudio = null }) {
                    Text(stringResource(R.string.action_delete), color = scheme.error)
                }
            },
            dismissButton = { TextButton(onClick = { confirmAudio = null }) { Text(stringResource(R.string.action_cancel)) } },
        )
    }
}

@Composable
private fun QuickAction(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, onClick: () -> Unit, modifier: Modifier) {
    val scheme = MaterialTheme.colorScheme
    Surface(onClick = onClick, shape = RoundedCornerShape(14.dp), color = LocalExtraColors.current.accentSoft, contentColor = scheme.primary, modifier = modifier) {
        Column(Modifier.padding(vertical = 10.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(22.dp))
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
        }
    }
}

private fun strokeGlyph(name: String, d: String): androidx.compose.ui.graphics.vector.ImageVector =
    androidx.compose.ui.graphics.vector.ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(d).toNodes(),
            stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
    }.build()

/** Máy quay video. */
val VideoGlyph by lazy { strokeGlyph("Video", "M3,7H15V17H3Z M15,10.5L21,7V17L15,13.5") }

/** Tệp (tờ giấy + kẹp). */
val FileGlyph by lazy { strokeGlyph("File", "M6,3H14L19,8V21H6Z M14,3V8H19 M9,13H16 M9,17H14") }

/** Biểu tượng thư viện ảnh (khung + núi). */
val GalleryGlyph: androidx.compose.ui.graphics.vector.ImageVector by lazy {
    androidx.compose.ui.graphics.vector.ImageVector.Builder("Gallery", 24.dp, 24.dp, 24f, 24f).apply {
        addPath(
            pathData = androidx.compose.ui.graphics.vector.PathParser().parsePathString(
                "M4,4H20V20H4Z M4,16L9,11L13,15L15,13L20,18 M15.5,8.5A1.5,1.5 0 1 0 15.49,8.5"
            ).toNodes(),
            stroke = androidx.compose.ui.graphics.SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = androidx.compose.ui.graphics.StrokeCap.Round,
            strokeLineJoin = androidx.compose.ui.graphics.StrokeJoin.Round,
        )
    }.build()
}

/** Ghi âm: bấm Dừng & lưu → file .m4a vào ghi chú. */
@Composable
fun RecorderDialog(outFile: File, onSaved: (File) -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    val scheme = MaterialTheme.colorScheme
    var elapsed by remember { mutableLongStateOf(0L) }
    val recorder = remember {
        runCatching {
            @Suppress("DEPRECATION")
            val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(context) else MediaRecorder()
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(96_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(outFile.path)
            r.prepare()
            r.start()
            r
        }.getOrNull()
    }
    var finished by remember { mutableStateOf(false) }
    fun stop(save: Boolean) {
        if (finished) return
        finished = true
        val ok = runCatching { recorder?.stop() }.isSuccess
        runCatching { recorder?.release() }
        if (save && ok && outFile.length() > 0) onSaved(outFile) else { outFile.delete(); onCancel() }
    }
    LaunchedEffect(Unit) {
        val start = System.currentTimeMillis()
        while (!finished) { elapsed = System.currentTimeMillis() - start; delay(200) }
    }
    DisposableEffect(Unit) { onDispose { if (!finished) { finished = true; runCatching { recorder?.stop() }; runCatching { recorder?.release() } } } }
    if (recorder == null) {
        LaunchedEffect(Unit) { outFile.delete(); onCancel() }
        return
    }
    AlertDialog(
        onDismissRequest = { },
        title = { Text(stringResource(R.string.rec_recording)) },
        text = {
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Box(Modifier.size(84.dp).clip(CircleShape).background(scheme.error.copy(alpha = 0.15f)), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(56.dp).clip(CircleShape).background(scheme.error), contentAlignment = Alignment.Center) {
                        Icon(MicIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
                    }
                }
                Text(formatDuration(elapsed), fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
        },
        confirmButton = { TextButton(onClick = { stop(true) }) { Text(stringResource(R.string.rec_stop), fontWeight = FontWeight.Bold) } },
        dismissButton = { TextButton(onClick = { stop(false) }) { Text(stringResource(R.string.action_cancel)) } },
    )
}
