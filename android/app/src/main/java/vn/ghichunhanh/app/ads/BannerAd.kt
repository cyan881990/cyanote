package vn.ghichunhanh.app.ads

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Spacer
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** AdMob đã khởi tạo xong chưa. Chỉ tải quảng cáo sau khi xong, nếu không sẽ treo luồng giao diện. */
object AdsInit {
    var ready by mutableStateOf(false)
}

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
fun BannerAd(visible: Boolean, widthDp: Int, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val adSize = remember(widthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    }
    val height = if (visible) adSize.height.dp else 0.dp
    // Chưa khởi tạo xong AdMob: giữ chỗ trống đúng kích thước (giao diện không bị nhảy).
    if (!AdsInit.ready) {
        Spacer(modifier.fillMaxWidth().height(height))
        return
    }
    val adView = remember(adSize) {
        AdView(context).apply {
            setAdSize(adSize)
            adUnitId = AdConfig.BANNER_AD_UNIT_ID
        }
    }
    // Tải quảng cáo sau khi vẽ xong khung hình, không gọi giữa lúc dựng giao diện.
    LaunchedEffect(adView) {
        adView.loadAd(AdRequest.Builder().build())
    }
    DisposableEffect(adView) {
        onDispose { adView.destroy() }
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        key(adView) {
            AndroidView(
                factory = { adView },
                modifier = Modifier
                    .width(adSize.width.dp)
                    .height(height),
            )
        }
    }
}
