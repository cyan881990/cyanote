package vn.ghichunhanh.app.ads

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

/**
 * Quảng cáo toàn trang: cứ mỗi 3 ghi chú tạo mới (cộng dồn, kể cả qua nhiều lần mở app)
 * thì hiện một lần, ngay sau khi bấm "+" (lúc chuyển sang ghi chú mới, không chen ngang lúc đang gõ).
 * Cứ đủ 3 ghi chú mới là hiện (quảng cáo được tải sẵn ngay sau mỗi lần hiện).
 */
object Interstitials {
    /** ID THỬ NGHIỆM của Google. Thay bằng ID thật trước khi phát hành. */
    val UNIT_ID: String = vn.ghichunhanh.app.BuildConfig.AD_INTERSTITIAL
    private const val EVERY = 3
    private const val MIN_GAP_MS = 15_000L   // chỉ chặn hiện 2 lần dính nhau
    private const val PREFS = "settings"
    private const val KEY_COUNT = "notes_created_since_ad"
    private const val KEY_LAST = "interstitial_last_shown"

    private var ad: InterstitialAd? = null
    private var loading = false

    fun preload(context: Context) {
        if (ad != null || loading || !AdsInit.ready) return
        loading = true
        InterstitialAd.load(context.applicationContext, UNIT_ID, AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(loaded: InterstitialAd) { ad = loaded; loading = false }
            override fun onAdFailedToLoad(error: LoadAdError) { ad = null; loading = false }
        })
    }

    /** Gọi mỗi khi người dùng tạo ghi chú mới. */
    fun onNoteCreated(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val count = prefs.getInt(KEY_COUNT, 0) + 1
        val now = System.currentTimeMillis()
        val due = count >= EVERY && now - prefs.getLong(KEY_LAST, 0L) >= MIN_GAP_MS
        val current = ad
        if (due && current != null) {
            current.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() { ad = null; preload(activity) }
                override fun onAdFailedToShowFullScreenContent(e: AdError) { ad = null; preload(activity) }
            }
            current.show(activity)
            ad = null
            preload(activity)   // tải sẵn quảng cáo cho lần kế tiếp
            prefs.edit().putInt(KEY_COUNT, 0).putLong(KEY_LAST, now).apply()
        } else {
            prefs.edit().putInt(KEY_COUNT, count).apply()
            preload(activity)
        }
    }
}
