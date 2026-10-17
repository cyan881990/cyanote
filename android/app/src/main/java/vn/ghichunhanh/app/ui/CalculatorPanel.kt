package vn.ghichunhanh.app.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import vn.ghichunhanh.app.Calc
import vn.ghichunhanh.app.CalcError
import vn.ghichunhanh.app.CalcState
import vn.ghichunhanh.app.R

/** Bàn phím gọn 5 cột × 4 hàng để máy tính chiếm ít chỗ, quảng cáo vẫn hiện bên dưới. */
private val KEY_ROWS = listOf(
    listOf("7", "8", "9", "÷", "C"),
    listOf("4", "5", "6", "×", "⌫"),
    listOf("1", "2", "3", "−", "%"),
    listOf("0", "000", ".", "+", "="),
)

@Composable
private fun keyDescription(key: String): String = when (key) {
    "C" -> stringResource(R.string.key_clear)
    "⌫" -> stringResource(R.string.key_backspace)
    "%" -> stringResource(R.string.key_percent)
    "÷" -> stringResource(R.string.key_divide)
    "×" -> stringResource(R.string.key_multiply)
    "−" -> stringResource(R.string.key_minus)
    "+" -> stringResource(R.string.key_plus)
    "=" -> stringResource(R.string.key_equals)
    "." -> stringResource(R.string.key_decimal)
    "000" -> stringResource(R.string.key_triple_zero)
    else -> key
}

/** Thanh máy tính thu gọn: luôn hiện phép tính đang giữ, bấm để mở rộng. */
@Composable
fun CalculatorBar(calc: CalcState, onExpand: () -> Unit, compact: Boolean = false) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val openLabel = stringResource(R.string.calc_open)
    Surface(
        onClick = onExpand,
        shape = RoundedCornerShape(18.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(if (compact) 48.dp else 60.dp)
            .semantics { contentDescription = openLabel },
    ) {
        Row(
            modifier = Modifier.padding(start = 10.dp, end = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(if (compact) 32.dp else 40.dp)
                    .clip(RoundedCornerShape(if (compact) 10.dp else 12.dp))
                    .background(extra.accentSoft),
                contentAlignment = Alignment.Center,
            ) {
                Icon(CalculatorIcon, contentDescription = null, tint = scheme.primary, modifier = Modifier.size(22.dp))
            }
            Column(Modifier.weight(1f)) {
                if (!compact) Text(
                    stringResource(R.string.calc_holding),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = scheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    calc.miniExpression() ?: stringResource(R.string.calc_empty),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = scheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                calc.miniResult(),
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                color = scheme.primary,
                maxLines = 1,
                style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
            )
            Icon(Icons.Filled.KeyboardArrowUp, contentDescription = null, tint = scheme.onSurfaceVariant)
        }
    }
}

/** Máy tính mở rộng (bản gọn), nằm dưới ghi chú, có nút Thu gọn cạnh màn hình số. */
@Composable
fun CalculatorPanel(
    calc: CalcState,
    onCollapse: (() -> Unit)?,   // null: không có nút Thu gọn (màn hình rộng, máy tính luôn mở)
    onInsert: (String) -> Unit,
    modifier: Modifier = Modifier,
    fill: Boolean = false,       // true: phím giãn ra lấp kín chiều cao (cột bên phải trên màn hình rộng)
) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val haptic = LocalHapticFeedback.current
    val result = calc.resultText()
    val full = calc.fullCalculation()
    val errorText = when (calc.error) {
        CalcError.DIVIDE_BY_ZERO -> stringResource(R.string.calc_err_div0)
        CalcError.INVALID -> stringResource(R.string.calc_err_invalid)
        null -> null
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = scheme.surface,
        border = BorderStroke(1.dp, scheme.outlineVariant),
        modifier = modifier.fillMaxWidth(),
    ) {
        val keyHeight = if (fill) Modifier.fillMaxHeight() else Modifier.height(44.dp)
        Column(
            modifier = Modifier.padding(8.dp).then(if (fill) Modifier.fillMaxHeight() else Modifier),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            // Màn hình số + nút Thu gọn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(extra.keyBg)
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    horizontalAlignment = Alignment.End,
                ) {
                    Text(
                        errorText ?: calc.secondaryLine(),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = if (errorText != null) extra.clearFg else scheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.height(15.dp),
                    )
                    Text(
                        calc.mainLine(),
                        fontSize = 22.sp,
                        lineHeight = 27.sp,
                        fontWeight = FontWeight.Bold,
                        color = scheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = LocalTextStyle.current.copy(fontFeatureSettings = "tnum"),
                    )
                }
                if (onCollapse != null) Surface(
                    onClick = onCollapse,
                    shape = RoundedCornerShape(12.dp),
                    color = extra.keyBg,
                    contentColor = scheme.onSurface,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight(),
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(Icons.Filled.KeyboardArrowDown, contentDescription = null, modifier = Modifier.size(20.dp))
                        Text(
                            stringResource(R.string.calc_collapse),
                            fontSize = 10.sp,
                            lineHeight = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }

            for (row in KEY_ROWS) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = if (fill) Modifier.weight(1f) else Modifier,
                ) {
                    for (key in row) {
                        CalcKey(key = key, modifier = Modifier.weight(1f).then(keyHeight)) {
                            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            calc.press(key)
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                OutlinedButton(
                    onClick = { full?.let(onInsert) },
                    enabled = full != null,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.5.dp, if (full != null) scheme.primary else scheme.outlineVariant),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Text(
                        stringResource(R.string.calc_insert_full),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Button(
                    onClick = { result?.let(onInsert) },
                    enabled = result != null,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = scheme.primary, contentColor = scheme.onPrimary),
                    modifier = Modifier
                        .weight(1f)
                        .height(44.dp),
                ) {
                    Text(
                        stringResource(R.string.calc_insert_result),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun CalcKey(key: String, modifier: Modifier, onClick: () -> Unit) {
    val scheme = MaterialTheme.colorScheme
    val extra = LocalExtraColors.current
    val (bg, fg) = when {
        key == "=" -> extra.eqBg to extra.eqFg
        key in Calc.OPS || key == "%" -> extra.opBg to extra.opFg
        key == "C" || key == "⌫" -> extra.clearBg to extra.clearFg
        else -> extra.keyBg to scheme.onSurface
    }
    val description = keyDescription(key)
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = bg,
        contentColor = fg,
        modifier = modifier
            .semantics { contentDescription = description },
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(key, fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

/** Biểu tượng máy tính bỏ túi (vẽ bằng nét, không cần thư viện icon mở rộng). */
val CalculatorIcon: ImageVector by lazy {
    ImageVector.Builder(
        name = "Calculator",
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
        ) {
            moveTo(8f, 2.5f)
            horizontalLineTo(16f)
            arcTo(3f, 3f, 0f, false, true, 19f, 5.5f)
            verticalLineTo(18.5f)
            arcTo(3f, 3f, 0f, false, true, 16f, 21.5f)
            horizontalLineTo(8f)
            arcTo(3f, 3f, 0f, false, true, 5f, 18.5f)
            verticalLineTo(5.5f)
            arcTo(3f, 3f, 0f, false, true, 8f, 2.5f)
            close()
            moveTo(8.5f, 7f)
            horizontalLineTo(15.5f)
            for (y in listOf(12f, 15.5f, 19f)) {
                for (x in listOf(9f, 12f, 15f)) {
                    moveTo(x, y)
                    lineTo(x + 0.01f, y)
                }
            }
        }
    }.build()
}
