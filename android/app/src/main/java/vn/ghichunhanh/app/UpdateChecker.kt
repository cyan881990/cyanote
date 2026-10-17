package vn.ghichunhanh.app

import android.content.Context
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class UpdateInfo(val versionCode: Long, val versionName: String, val url: String)

/**
 * Tự cập nhật cho bản cài bằng file APK (không qua Google Play):
 * đọc https://cyan881990.github.io/update.json, nếu có versionCode lớn hơn bản đang cài
 * thì app hiện hộp thoại "Có bản cập nhật" với nút tải bản mới.
 * Bản cài từ Google Play dùng Play In-App Updates (xem MainActivity), không dùng file này.
 */
object UpdateChecker {
    const val MANIFEST_URL = "https://cyan881990.github.io/update.json"
    private const val PREFS = "settings"
    private const val KEY_LAST_CHECK = "update_last_check"
    private const val KEY_SKIPPED = "update_skipped_code"
    private const val MIN_INTERVAL_MS = 6 * 60 * 60 * 1000L // tối đa 4 lần/ngày

    fun installedFromPlay(context: Context): Boolean {
        val pm = context.packageManager
        val installer = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                pm.getInstallSourceInfo(context.packageName).installingPackageName
            } else {
                @Suppress("DEPRECATION")
                pm.getInstallerPackageName(context.packageName)
            }
        }.getOrNull()
        return installer == "com.android.vending"
    }

    fun currentVersionCode(context: Context): Long = runCatching {
        PackageInfoCompat.getLongVersionCode(context.packageManager.getPackageInfo(context.packageName, 0))
    }.getOrDefault(0L)

    /** Trả về bản mới nếu có (và người dùng chưa bấm "Để sau" cho đúng bản đó). */
    suspend fun check(context: Context, force: Boolean = false): UpdateInfo? = withContext(Dispatchers.IO) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val now = System.currentTimeMillis()
        if (!force && now - prefs.getLong(KEY_LAST_CHECK, 0L) < MIN_INTERVAL_MS) return@withContext null
        prefs.edit().putLong(KEY_LAST_CHECK, now).apply()
        runCatching {
            val conn = URL("$MANIFEST_URL?t=$now").openConnection() as HttpURLConnection
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.setRequestProperty("Cache-Control", "no-cache")
            val text = conn.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
            conn.disconnect()
            val android = JSONObject(text).getJSONObject("android")
            val info = UpdateInfo(
                versionCode = android.getLong("versionCode"),
                versionName = android.optString("versionName", ""),
                url = android.getString("url"),
            )
            val skipped = prefs.getLong(KEY_SKIPPED, 0L)
            if (info.versionCode > currentVersionCode(context) && info.versionCode != skipped) info else null
        }.getOrNull()
    }

    fun skip(context: Context, versionCode: Long) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong(KEY_SKIPPED, versionCode).apply()
    }
}
