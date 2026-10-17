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
