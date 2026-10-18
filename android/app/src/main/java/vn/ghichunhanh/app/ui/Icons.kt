package vn.ghichunhanh.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/** Icon nét 24×24 tự vẽ (bộ icon cơ bản của Compose không có mặt trời, mặt trăng, quả địa cầu). */
private fun strokeIcon(name: String, block: PathBuilder.() -> Unit): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
    ).apply {
        path(
            fill = null,
            stroke = SolidColor(Color.Black),
            strokeLineWidth = 2f,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathBuilder = block,
        )
    }.build()

val SunIcon: ImageVector by lazy {
    strokeIcon("Sun") {
        moveTo(16f, 12f)
        arcTo(4f, 4f, 0f, true, true, 8f, 12f)
        arcTo(4f, 4f, 0f, true, true, 16f, 12f)
        close()
        val rays = listOf(
            floatArrayOf(12f, 2.5f, 12f, 4.5f), floatArrayOf(12f, 19.5f, 12f, 21.5f),
            floatArrayOf(2.5f, 12f, 4.5f, 12f), floatArrayOf(19.5f, 12f, 21.5f, 12f),
            floatArrayOf(5.3f, 5.3f, 6.7f, 6.7f), floatArrayOf(17.3f, 17.3f, 18.7f, 18.7f),
            floatArrayOf(5.3f, 18.7f, 6.7f, 17.3f), floatArrayOf(17.3f, 6.7f, 18.7f, 5.3f),
        )
        for (r in rays) {
            moveTo(r[0], r[1])
            lineTo(r[2], r[3])
        }
    }
}

val MoonIcon: ImageVector by lazy {
    strokeIcon("Moon") {
        moveTo(20f, 14.5f)
        arcTo(8f, 8f, 0f, false, true, 9.5f, 4f)
        arcTo(8f, 8f, 0f, true, false, 20f, 14.5f)
        close()
    }
}

val GlobeIcon: ImageVector by lazy {
    strokeIcon("Globe") {
        moveTo(21f, 12f)
        arcTo(9f, 9f, 0f, true, true, 3f, 12f)
        arcTo(9f, 9f, 0f, true, true, 21f, 12f)
        close()
        moveTo(3f, 12f)
        horizontalLineTo(21f)
        moveTo(12f, 3f)
        curveTo(14.5f, 5.6f, 15.8f, 8.6f, 15.8f, 12f)
        curveTo(15.8f, 15.4f, 14.5f, 18.4f, 12f, 21f)
        curveTo(9.5f, 18.4f, 8.2f, 15.4f, 8.2f, 12f)
        curveTo(8.2f, 8.6f, 9.5f, 5.6f, 12f, 3f)
        close()
    }
}

/** Bàn phím + mũi tên xuống: "ẩn bàn phím". */
val KeyboardHideIcon: ImageVector by lazy {
    strokeIcon("KeyboardHide") {
        // khung bàn phím
        moveTo(5f, 3f); horizontalLineTo(19f)
        arcTo(2f, 2f, 0f, false, true, 21f, 5f); verticalLineTo(13f)
        arcTo(2f, 2f, 0f, false, true, 19f, 15f); horizontalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 3f, 13f); verticalLineTo(5f)
        arcTo(2f, 2f, 0f, false, true, 5f, 3f); close()
        // phím
        moveTo(7f, 7f); lineTo(7.01f, 7f)
        moveTo(10.5f, 7f); lineTo(10.51f, 7f)
        moveTo(14f, 7f); lineTo(14.01f, 7f)
        moveTo(17.5f, 7f); lineTo(17.51f, 7f)
        moveTo(8f, 11f); lineTo(16f, 11f)
        // mũi tên xuống
        moveTo(9f, 18.5f); lineTo(12f, 21.5f); lineTo(15f, 18.5f)
    }
}

/** Mũi tên xuống khay: "cập nhật". */
val UpdateIcon: ImageVector by lazy {
    strokeIcon("Update") {
        moveTo(12f, 4f); lineTo(12f, 15f)
        moveTo(7.5f, 10.5f); lineTo(12f, 15f); lineTo(16.5f, 10.5f)
        moveTo(5f, 19.5f); lineTo(19f, 19.5f)
    }
}

/** Đồng hồ báo thức: lịch hẹn. */
val AlarmIcon: ImageVector by lazy {
    strokeIcon("Alarm") {
        moveTo(19f, 13f)
        arcTo(7f, 7f, 0f, true, true, 5f, 13f)
        arcTo(7f, 7f, 0f, true, true, 19f, 13f)
        close()
        moveTo(12f, 9.5f); lineTo(12f, 13f); lineTo(14.5f, 14.5f)
        moveTo(5f, 3.5f); lineTo(2.5f, 6f)
        moveTo(19f, 3.5f); lineTo(21.5f, 6f)
    }
}

/** Máy ảnh: chụp ảnh dán vào ghi chú. */
val CameraIcon: ImageVector by lazy {
    strokeIcon("Camera") {
        moveTo(4f, 7.5f); horizontalLineTo(7.5f); lineTo(9f, 5f); horizontalLineTo(15f); lineTo(16.5f, 7.5f); horizontalLineTo(20f)
        arcTo(1.5f, 1.5f, 0f, false, true, 21.5f, 9f); verticalLineTo(18f)
        arcTo(1.5f, 1.5f, 0f, false, true, 20f, 19.5f); horizontalLineTo(4f)
        arcTo(1.5f, 1.5f, 0f, false, true, 2.5f, 18f); verticalLineTo(9f)
        arcTo(1.5f, 1.5f, 0f, false, true, 4f, 7.5f); close()
        moveTo(15.5f, 13f)
        arcTo(3.5f, 3.5f, 0f, true, true, 8.5f, 13f)
        arcTo(3.5f, 3.5f, 0f, true, true, 15.5f, 13f)
        close()
    }
}

/** Điện thoại: gọi. */
val PhoneIcon: ImageVector by lazy {
    strokeIcon("Phone") {
        moveTo(5f, 3.5f); horizontalLineTo(8.5f); lineTo(10f, 8f); lineTo(8f, 9.5f)
        curveTo(9f, 12f, 12f, 15f, 14.5f, 16f)
        lineTo(16f, 14f); lineTo(20.5f, 15.5f); verticalLineTo(19f)
        arcTo(1.5f, 1.5f, 0f, false, true, 19f, 20.5f)
        curveTo(10.5f, 20f, 4f, 13.5f, 3.5f, 5f)
        arcTo(1.5f, 1.5f, 0f, false, true, 5f, 3.5f)
        close()
    }
}

/** Micro: ghi âm. */
val MicIcon: ImageVector by lazy {
    strokeIcon("Mic") {
        moveTo(9f, 6f)
        arcTo(3f, 3f, 0f, false, true, 15f, 6f); verticalLineTo(11.5f)
        arcTo(3f, 3f, 0f, false, true, 9f, 11.5f); close()
        moveTo(5.5f, 11f)
        arcTo(6.5f, 6.5f, 0f, false, false, 18.5f, 11f)
        moveTo(12f, 17.5f); lineTo(12f, 21f)
        moveTo(8.5f, 21f); lineTo(15.5f, 21f)
    }
}

/** Ẩn bảng dưới (khung có mũi tên xuống). */
val PanelHideIcon: ImageVector by lazy {
    strokeIcon("PanelHide") {
        moveTo(4f, 4f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(4f); close()
        moveTo(4f, 14f); lineTo(20f, 14f)
        moveTo(9.5f, 7.5f); lineTo(12f, 10f); lineTo(14.5f, 7.5f)
    }
}

/** Hiện bảng dưới. */
val PanelShowIcon: ImageVector by lazy {
    strokeIcon("PanelShow") {
        moveTo(4f, 4f); horizontalLineTo(20f); verticalLineTo(20f); horizontalLineTo(4f); close()
        moveTo(4f, 14f); lineTo(20f, 14f)
        moveTo(9.5f, 10f); lineTo(12f, 7.5f); lineTo(14.5f, 10f)
    }
}

/** Tự xuống dòng (mũi tên quay về). */
val WrapIcon: ImageVector by lazy {
    strokeIcon("Wrap") {
        moveTo(4f, 6f); lineTo(20f, 6f)
        moveTo(4f, 12f); horizontalLineTo(17f)
        arcTo(3f, 3f, 0f, false, true, 17f, 18f); horizontalLineTo(13f)
        moveTo(15f, 16f); lineTo(13f, 18f); lineTo(15f, 20f)
        moveTo(4f, 18f); lineTo(9f, 18f)
    }
}

/** Số dòng. */
val LineNumbersIcon: ImageVector by lazy {
    strokeIcon("LineNumbers") {
        moveTo(4f, 5f); lineTo(5.5f, 4f); lineTo(5.5f, 9f)
        moveTo(4f, 13f); curveTo(4.5f, 11.5f, 7f, 11.5f, 7f, 13f); curveTo(7f, 14.5f, 4f, 15.5f, 4f, 17f); lineTo(7f, 17f)
        moveTo(10.5f, 6.5f); lineTo(20f, 6.5f)
        moveTo(10.5f, 12f); lineTo(20f, 12f)
        moveTo(10.5f, 17.5f); lineTo(20f, 17.5f)
    }
}

/** Copy (hai tờ giấy chồng nhau). */
val CopyIcon: ImageVector by lazy {
    strokeIcon("Copy") {
        moveTo(9f, 9f); horizontalLineTo(19f); verticalLineTo(20f); horizontalLineTo(9f); close()
        moveTo(5f, 15f); verticalLineTo(5f); arcTo(1f, 1f, 0f, false, true, 6f, 4f); horizontalLineTo(15f)
    }
}
