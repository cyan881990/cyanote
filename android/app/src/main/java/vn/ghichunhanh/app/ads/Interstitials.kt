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
 * Giới hạn: cách nhau tối thiểu 90 giây; chưa đủ thì dời sang lần tạo ghi chú kế tiếp.
 */
object Interstitials {
    /** ID THỬ NGHIỆM của Google. Thay bằng ID thật trước khi phát hành. */
    const val UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val EVERY = 3
    private const val MIN_GAP_MS = 90_000L
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
            prefs.edit().putInt(KEY_COUNT, 0).putLong(KEY_LAST, now).apply()
        } else {
            prefs.edit().putInt(KEY_COUNT, count).apply()
            preload(activity)
        }
    }
}
