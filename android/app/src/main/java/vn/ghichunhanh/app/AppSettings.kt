package vn.ghichunhanh.app

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.res.Configuration
import android.os.LocaleList
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.util.Locale

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Ngôn ngữ có trong app: mã ngôn ngữ → tên hiển thị bằng chính ngôn ngữ đó. */
val APP_LANGUAGES: List<Pair<String, String>> = listOf(
    "en" to "English",
    "vi" to "Tiếng Việt",
    "es" to "Español",
    "pt" to "Português",
    "fr" to "Français",
    "de" to "Deutsch",
    "id" to "Bahasa Indonesia",
    "th" to "ไทย",
    "ja" to "日本語",
    "ko" to "한국어",
    "zh-CN" to "简体中文",
    "hi" to "हिन्दी",
    "ru" to "Русский",
)

/**
 * Cài đặt nhỏ lưu trong SharedPreferences: ngôn ngữ app và chế độ sáng/tối.
 * Ngôn ngữ được áp dụng trong MainActivity.attachBaseContext.
 */
object AppSettings {
    private const val PREFS = "settings"
    private const val KEY_LANG = "language"
    private const val KEY_THEME = "theme"
    const val DEFAULT_LANGUAGE = "en"

    /** Chế độ sáng/tối hiện tại (state của Compose để giao diện đổi ngay). */
    var themeMode by mutableStateOf(ThemeMode.SYSTEM)
        private set

    private var loaded = false

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        val name = prefs(context).getString(KEY_THEME, ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name
        themeMode = runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun setTheme(context: Context, mode: ThemeMode) {
        themeMode = mode
        prefs(context).edit().putString(KEY_THEME, mode.name).apply()
    }

    /**
     * Mặc định là tiếng Anh ("en") khi người dùng chưa chọn gì.
     * "" = người dùng chọn "Theo hệ thống" (dùng ngôn ngữ của điện thoại).
     */
    fun languageTag(context: Context): String = prefs(context).getString(KEY_LANG, DEFAULT_LANGUAGE) ?: DEFAULT_LANGUAGE

    fun setLanguage(context: Context, tag: String) {
        // commit() (ghi ngay) vì Activity sẽ được tạo lại ngay sau đó.
        prefs(context).edit().putString(KEY_LANG, tag).commit()
    }

    /** Bọc Context để dùng ngôn ngữ người dùng đã chọn trong app. */
    fun wrapWithLanguage(base: Context): Context {
        val tag = languageTag(base)
        if (tag.isEmpty()) return base
        val locale = Locale.forLanguageTag(tag)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }

    private fun prefs(context: Context) = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}

fun Context.findActivity(): Activity? {
    var c: Context? = this
    while (c is ContextWrapper) {
        if (c is Activity) return c
        c = c.baseContext
    }
    return null
}
