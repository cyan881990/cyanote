package vn.ghichunhanh.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import vn.ghichunhanh.app.R
import vn.ghichunhanh.app.UpdateInfo

/** Hộp thoại "Có bản cập nhật" cho bản cài bằng file APK. */
@Composable
fun UpdateDialog(info: UpdateInfo, onUpdate: () -> Unit, onLater: () -> Unit) {
    AlertDialog(
        onDismissRequest = onLater,
        title = { Text(stringResource(R.string.update_title)) },
        text = { Text(stringResource(R.string.update_message, info.versionName)) },
        confirmButton = { TextButton(onClick = onUpdate) { Text(stringResource(R.string.update_now)) } },
        dismissButton = { TextButton(onClick = onLater) { Text(stringResource(R.string.update_later)) } },
    )
}

/** Thanh "Đã tải xong bản cập nhật · Khởi động lại" cho bản cài từ Google Play. */
@Composable
fun UpdateReadyBar(onRestart: () -> Unit) {
    Box(Modifier.fillMaxSize().safeDrawingPadding(), contentAlignment = Alignment.TopCenter) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.inverseSurface,
            contentColor = MaterialTheme.colorScheme.inverseOnSurface,
            shadowElevation = 6.dp,
            modifier = Modifier.padding(12.dp),
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(stringResource(R.string.update_downloaded), fontWeight = FontWeight.Medium)
                TextButton(onClick = onRestart) {
                    Text(stringResource(R.string.update_restart), color = MaterialTheme.colorScheme.inversePrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
