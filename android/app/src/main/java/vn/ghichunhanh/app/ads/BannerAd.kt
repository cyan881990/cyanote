package vn.ghichunhanh.app.ads

import android.view.ViewGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/** AdMob đã khởi tạo xong chưa. Chỉ tạo/tải quảng cáo sau khi xong, nếu không sẽ treo luồng giao diện. */
object AdsInit {
    var ready by mutableStateOf(false)
}

object AdConfig {
    /**
     * ID THỬ NGHIỆM của Google (luôn hiện quảng cáo mẫu, không tính tiền).
     * Trước khi đăng Play Store: tạo ứng dụng + đơn vị quảng cáo trong AdMob
     * rồi thay các chuỗi này và APPLICATION_ID trong AndroidManifest.xml.
     * KHÔNG tự bấm vào quảng cáo thật của mình.
     */
    const val BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"   // banner thích ứng
    const val MREC_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"     // khung 300×250
}

/** Một quảng cáo giữ nguyên suốt phiên: đổi chỗ trên màn hình chỉ gắn sang chỗ mới, không tải lại. */
class AdHolder(val view: AdView, val size: AdSize) {
    var requested = false
}

/** Kích thước banner thích ứng cho bề rộng [widthDp], hoặc khung 300×250 khi [mrec]. */
@Composable
fun rememberAdSize(widthDp: Int, mrec: Boolean = false): AdSize {
    val context = LocalContext.current
    return remember(widthDp, mrec) {
        if (mrec) AdSize.MEDIUM_RECTANGLE
        // Lúc đổi kích thước màn hình bề rộng có thể tạm bằng 0 → giữ tối thiểu 300dp (AdMob báo lỗi nếu ≤ 0)
        else AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp.coerceAtLeast(300))
    }
}

/** Tạo (một lần) quảng cáo cho [size]; null cho tới khi AdMob khởi tạo xong. */
@Composable
fun rememberAdHolder(size: AdSize, mrec: Boolean = false): AdHolder? {
    if (!AdsInit.ready) return null
    val context = LocalContext.current
    val holder = remember(size) {
        AdHolder(
            AdView(context).apply {
                setAdSize(size)
                adUnitId = if (mrec) AdConfig.MREC_AD_UNIT_ID else AdConfig.BANNER_AD_UNIT_ID
            },
            size,
        )
    }
    DisposableEffect(holder) { onDispose { holder.view.destroy() } }
    return holder
}

/** Chỗ đặt quảng cáo. Chưa sẵn sàng thì giữ đúng khoảng trống để giao diện không nhảy. */
@Composable
fun AdSlot(holder: AdHolder?, size: AdSize, modifier: Modifier = Modifier) {
    val w = size.width.dp
    val h = size.height.dp
    if (holder == null) {
        Spacer(modifier.fillMaxWidth().height(h))
        return
    }
    // Tải sau khi vẽ xong khung hình, chỉ một lần cho mỗi quảng cáo.
    LaunchedEffect(holder) {
        if (!holder.requested) {
            holder.requested = true
            holder.view.loadAd(AdRequest.Builder().build())
        }
    }
    Box(modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        key(holder) {
            AndroidView(
                factory = {
                    (holder.view.parent as? ViewGroup)?.removeView(holder.view)
                    holder.view
                },
                modifier = Modifier.size(w, h),
            )
        }
    }
}
