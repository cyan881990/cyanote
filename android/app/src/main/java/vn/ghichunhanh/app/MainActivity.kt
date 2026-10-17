package vn.ghichunhanh.app

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.ghichunhanh.app.ui.AppTheme
import vn.ghichunhanh.app.ui.EditorScreen

class MainActivity : ComponentActivity() {

    private val store by lazy { NotesStore.get(this) }

    // Áp dụng ngôn ngữ người dùng chọn trong app (nút 🌐).
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppSettings.wrapWithLanguage(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        AppSettings.load(this)

        // Khởi tạo AdMob ở luồng nền để app mở nhanh.
        lifecycleScope.launch(Dispatchers.IO) {
            MobileAds.initialize(this@MainActivity) {}
        }

        if (savedInstanceState == null) handleIntent(intent)

        setContent {
            val dark = when (AppSettings.themeMode) {
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            // Biểu tượng thanh trạng thái: tối trên nền sáng, sáng trên nền tối.
            SideEffect {
                WindowCompat.getInsetsController(window, window.decorView).apply {
                    isAppearanceLightStatusBars = !dark
                    isAppearanceLightNavigationBars = !dark
                }
            }
            AppTheme(dark = dark) {
                EditorScreen(
                    store = store,
                    isDark = dark,
                    onToggleTheme = {
                        AppSettings.setTheme(this, if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
                    },
                    currentLanguage = AppSettings.languageTag(this),
                    onLanguageChosen = { tag ->
                        if (tag != AppSettings.languageTag(this)) {
                            store.saveNow()
                            AppSettings.setLanguage(this, tag)
                            recreate()
                        }
                    },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    override fun onStop() {
        super.onStop()
        // App bị ẩn/tắt: ghi ngay xuống file, không chờ hết 0,4 giây.
        store.saveNow()
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.action == ACTION_NEW_NOTE) store.newNote()
    }

    companion object {
        const val ACTION_NEW_NOTE = "vn.ghichunhanh.app.NEW_NOTE"
    }
}
