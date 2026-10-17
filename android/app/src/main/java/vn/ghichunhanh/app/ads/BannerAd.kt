package vn.ghichunhanh.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

object AdConfig {
    /**
     * ID banner THỬ NGHIỆM của Google (luôn hiện quảng cáo mẫu, không tính tiền).
     * Trước khi đăng Play Store: tạo ứng dụng + đơn vị quảng cáo trong AdMob
     * rồi thay chuỗi này và APPLICATION_ID trong AndroidManifest.xml.
     * KHÔNG tự bấm vào quảng cáo thật của mình.
     */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"
}

/**
 * Banner thích ứng (adaptive) đặt ở đáy màn hình.
 * Khi [visible] = false (đang mở máy tính hoặc đang hiện bàn phím) banner chỉ thu chiều cao về 0,
 * không bị tạo lại, nên không gửi yêu cầu quảng cáo mới mỗi lần đóng/mở.
 */
@Composable
fun BannerAd(visible: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val widthDp = LocalConfiguration.current.screenWidthDp - 24
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = AdConfig.BANNER_AD_UNIT_ID
            loadAd(AdRequest.Builder().build())
        }
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    key(adView) {
        AndroidView(
            factory = { adView },
            modifier = modifier
                .fillMaxWidth()
                .height(if (visible) adSize.height.dp else 0.dp),
        )
    }
}
