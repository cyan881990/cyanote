package vn.ghichunhanh.app

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.MobileAds
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import vn.ghichunhanh.app.ui.AppTheme
import vn.ghichunhanh.app.ui.EditorScreen
import vn.ghichunhanh.app.ui.UpdateDialog
import vn.ghichunhanh.app.ui.UpdateReadyBar

class MainActivity : ComponentActivity() {

    private val store by lazy { NotesStore.get(this) }

    // Tự cập nhật
    private val appUpdateManager by lazy { AppUpdateManagerFactory.create(this) }
    private var fromPlay = false
    private var playUpdateReady by mutableStateOf(false)
    private var fileUpdate by mutableStateOf<UpdateInfo?>(null)
    private val playUpdateLauncher = registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    private val installListener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) playUpdateReady = true
    }

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

        fromPlay = UpdateChecker.installedFromPlay(this)
        if (fromPlay) {
            appUpdateManager.registerListener(installListener)
            checkPlayUpdate()
        } else {
            lifecycleScope.launch { fileUpdate = UpdateChecker.check(this@MainActivity) }
        }

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
                Box {
                    EditorScreen(
                        store = store,
                        isDark = dark,
                        onToggleTheme = {
                            AppSettings.setTheme(this@MainActivity, if (dark) ThemeMode.LIGHT else ThemeMode.DARK)
                        },
                        currentLanguage = AppSettings.languageTag(this@MainActivity),
                        onLanguageChosen = { tag ->
                            if (tag != AppSettings.languageTag(this@MainActivity)) {
                                store.saveNow()
                                AppSettings.setLanguage(this@MainActivity, tag)
                                recreate()
                            }
                        },
                    )
                    if (playUpdateReady) {
                        UpdateReadyBar(onRestart = {
                            store.saveNow()
                            appUpdateManager.completeUpdate()
                        })
                    }
                    fileUpdate?.let { info ->
                        UpdateDialog(
                            info = info,
                            onUpdate = {
                                fileUpdate = null
                                // Mở link tải APK mới; Android sẽ hỏi cài đè lên bản hiện tại (giữ nguyên ghi chú).
                                runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(info.url))) }
                            },
                            onLater = {
                                UpdateChecker.skip(this@MainActivity, info.versionCode)
                                fileUpdate = null
                            },
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Bản Play đã tải xong khi app ở nền → hiện lại thanh "Khởi động lại".
        if (fromPlay) {
            appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
                if (info.installStatus() == InstallStatus.DOWNLOADED) playUpdateReady = true
            }
        }
    }

    override fun onDestroy() {
        if (fromPlay) appUpdateManager.unregisterListener(installListener)
        super.onDestroy()
    }

    /** Google Play: có bản mới thì tải ngầm (FLEXIBLE), xong báo "Khởi động lại". */
    private fun checkPlayUpdate() {
        appUpdateManager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> playUpdateReady = true
                info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> {
                    runCatching {
                        appUpdateManager.startUpdateFlowForResult(
                            info,
                            playUpdateLauncher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build(),
                        )
                    }
                }
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
