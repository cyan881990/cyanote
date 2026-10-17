package vn.ghichunhanh.app.ui

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import vn.ghichunhanh.app.APP_LANGUAGES
import vn.ghichunhanh.app.AppSettings
import vn.ghichunhanh.app.Backup
import vn.ghichunhanh.app.BackupData
import vn.ghichunhanh.app.NotesStore
import vn.ghichunhanh.app.R
import vn.ghichunhanh.app.ThemeMode
import java.io.IOException

/**
 * Sao lưu & khôi phục:
 * - Xuất: chọn nơi lưu (Tải xuống, Google Drive…) → ghi một file .zip.
 * - Nhập: chọn file .zip (hoặc .txt) → hỏi "Thêm vào ghi chú hiện có" hay "Thay thế toàn bộ".
 */
@Composable
fun BackupDialog(
    store: NotesStore,
    currentLanguage: String,
    onLanguageChosen: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val untitled = stringResource(R.string.untitled)
    var pending by remember { mutableStateOf<BackupData?>(null) }
    var busy by remember { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        // Chụp dữ liệu trên luồng chính, ghi file ở luồng nền.
        val json = store.backupJson(AppSettings.themeMode.name, currentLanguage)
        val notes = store.notes.toList()
        busy = true
        scope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    val out = context.contentResolver.openOutputStream(uri) ?: throw IOException("No output")
                    out.use { Backup.write(it, json, notes, untitled) }
                }.isSuccess
            }
            busy = false
            if (ok) {
                toast(context, context.getString(R.string.backup_export_done, notes.size))
                onDismiss()
            } else {
                toast(context, context.getString(R.string.backup_export_failed))
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        busy = true
        scope.launch {
            val data = withContext(Dispatchers.IO) {
                runCatching {
                    val name = displayName(context, uri)
                    val input = context.contentResolver.openInputStream(uri) ?: throw IOException("No input")
                    input.use { Backup.read(it, name) }
                }.getOrNull()
            }
            busy = false
            if (data == null) toast(context, context.getString(R.string.backup_import_failed)) else pending = data
        }
    }

    fun applyImport(data: BackupData, replace: Boolean) {
        val added = store.importBackup(data, replace)
        if (replace) {
            data.theme
                ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?.let { AppSettings.setTheme(context, it) }
        }
        toast(context, context.getString(R.string.backup_import_done, added))
        pending = null
        onDismiss()
        if (replace) {
            val lang = data.language
            val valid = lang != null && (lang.isEmpty() || APP_LANGUAGES.any { it.first == lang })
            if (valid && lang != currentLanguage) onLanguageChosen(lang!!)
        }
    }

    val data = pending
    if (data == null) {
        AlertDialog(
            onDismissRequest = { if (!busy) onDismiss() },
            title = { Text(stringResource(R.string.backup_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_desc), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Button(
                        onClick = { exportLauncher.launch(Backup.suggestedFileName()) },
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) { Text(stringResource(R.string.backup_export), fontWeight = FontWeight.SemiBold) }
                    OutlinedButton(
                        onClick = { importLauncher.launch(Backup.IMPORT_MIME_TYPES) },
                        enabled = !busy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) { Text(stringResource(R.string.backup_import), fontWeight = FontWeight.SemiBold) }
                    if (busy) {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(Modifier.size(28.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onDismiss, enabled = !busy) { Text(stringResource(R.string.action_close)) }
            },
        )
    } else {
        AlertDialog(
            onDismissRequest = { pending = null },
            title = { Text(stringResource(R.string.backup_import)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(R.string.backup_import_found, data.notes.size))
                    Button(
                        onClick = { applyImport(data, replace = false) },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) { Text(stringResource(R.string.backup_merge), fontWeight = FontWeight.SemiBold) }
                    OutlinedButton(
                        onClick = { applyImport(data, replace = true) },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                    ) {
                        Text(
                            stringResource(R.string.backup_replace),
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text(
                        stringResource(R.string.backup_replace_warning),
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 4.dp),
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { pending = null }) { Text(stringResource(R.string.action_cancel)) }
            },
        )
    }
}

private fun displayName(context: Context, uri: Uri): String? =
    runCatching {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { c ->
            if (c.moveToFirst()) c.getString(0) else null
        }
    }.getOrNull()

private fun toast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
