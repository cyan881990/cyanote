package vn.ghichunhanh.app

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.math.BigDecimal
import java.math.MathContext
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/** Lỗi máy tính; giao diện tự đổi sang câu chữ theo ngôn ngữ. */
enum class CalcError { DIVIDE_BY_ZERO, INVALID }

/** Bộ tính toán: + − × ÷ và %, dùng BigDecimal để không sai số với tiền. */
object Calc {
    const val OPS = "+−×÷"
    private val MC = MathContext(20, RoundingMode.HALF_UP)
    private val HUNDRED = BigDecimal(100)
    private val NUMBER = Regex("\\d+(\\.\\d*)?")
    private val SPACE_OPS = Regex("([+×÷])")
    private val SPACE_MINUS = Regex("(\\d|%)−")
    private val HAS_OP = Regex("[+×÷%]")
    private val HAS_MINUS = Regex(".−")

    class CalcException(val error: CalcError) : Exception(error.name)

    fun evaluate(input: String): BigDecimal {
        val parser = Parser(normalize(input))
        val result = parser.expression()
        if (!parser.atEnd()) throw CalcException(CalcError.INVALID)
        return result
    }

    fun tryEvaluate(expr: String): BigDecimal? {
        val t = trimTrailingOps(expr)
        if (t.isEmpty() || t == "−") return null
        return try {
            evaluate(t)
        } catch (e: Exception) {
            null
        }
    }

    fun trimTrailingOps(expr: String): String = expr.trimEnd('+', '−', '×', '÷', '.')

    fun hasOperator(expr: String): Boolean = HAS_OP.containsMatchIn(expr) || HAS_MINUS.containsMatchIn(expr)

    /** 1525000 → "1,525,000" */
    fun format(v: BigDecimal): String {
        val df = DecimalFormat("#,##0.######", DecimalFormatSymbols(Locale.US))
        df.roundingMode = RoundingMode.HALF_UP
        return df.format(v)
    }

    /** Dạng thô để tính tiếp sau khi bấm "=". */
    fun raw(v: BigDecimal): String {
        val s = v.setScale(6, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString()
        return if (s.startsWith("-")) "−" + s.substring(1) else s
    }

    fun groupNumbers(expr: String): String = NUMBER.replace(expr) { m ->
        val t = m.value
        val dot = t.indexOf('.')
        if (dot < 0) groupInt(t) else groupInt(t.substring(0, dot)) + t.substring(dot)
    }

    /** "300000−260000" → "300,000 − 260,000" */
    fun spaced(expr: String): String = groupNumbers(expr)
        .replace(SPACE_OPS, " \$1 ")
        .replace(SPACE_MINUS, "\$1 − ")
        .trim()

    private fun groupInt(s: String): String {
        val sb = StringBuilder()
        for (i in s.indices) {
            if (i > 0 && (s.length - i) % 3 == 0) sb.append(',')
            sb.append(s[i])
        }
        return sb.toString()
    }

    private fun normalize(s: String): String = buildString {
        for (c in s) {
            when (c) {
                '−' -> append('-')
                '×', 'x', 'X', '*' -> append('*')
                '÷', '/' -> append('/')
                ',', ' ' -> Unit
                else -> append(c)
            }
        }
    }

    /**
     * expression := term (('+'|'-') term)*
     * term       := factor (('*'|'/') factor)*
     * factor     := ('-'|'+') factor | number '%'*
     * "200000 + 10%" = 220000 (cộng thêm 10% của số trước), "200000 × 10%" = 20000.
     */
    private class Parser(private val s: String) {
        private var pos = 0

        fun atEnd(): Boolean = pos >= s.length

        private fun peek(): Char? = if (pos < s.length) s[pos] else null

        fun expression(): BigDecimal {
            var left = term().first
            while (true) {
                val c = peek()
                if (c == '+' || c == '-') {
                    pos++
                    val (right, isPercent) = term()
                    val r = if (isPercent) left.multiply(right, MC) else right
                    left = if (c == '+') left.add(r, MC) else left.subtract(r, MC)
                } else {
                    return left
                }
            }
        }

        private fun term(): Pair<BigDecimal, Boolean> {
            var (value, isPercent) = factor()
            while (true) {
                val c = peek()
                if (c == '*' || c == '/') {
                    pos++
                    val right = factor().first
                    value = if (c == '*') {
                        value.multiply(right, MC)
                    } else {
                        if (right.signum() == 0) throw CalcException(CalcError.DIVIDE_BY_ZERO)
                        value.divide(right, MC)
                    }
                    isPercent = false
                } else {
                    return Pair(value, isPercent)
                }
            }
        }

        private fun factor(): Pair<BigDecimal, Boolean> {
            val c = peek() ?: throw CalcException(CalcError.INVALID)
            if (c == '-') {
                pos++
                val (v, p) = factor()
                return Pair(v.negate(), p)
            }
            if (c == '+') {
                pos++
                return factor()
            }
            val start = pos
            while (pos < s.length && (s[pos].isDigit() || s[pos] == '.')) pos++
            var text = s.substring(start, pos)
            if (text.endsWith('.')) text = text.dropLast(1)
            if (text.isEmpty()) throw CalcException(CalcError.INVALID)
            var v = text.toBigDecimalOrNull() ?: throw CalcException(CalcError.INVALID)
            var isPercent = false
            while (peek() == '%') {
                pos++
                v = v.divide(HUNDRED, MC)
                isPercent = true
            }
            return Pair(v, isPercent)
        }
    }
}

/**
 * Trạng thái máy tính. Sống trong NotesStore nên đóng/thu gọn máy tính,
 * chuyển tab hay tắt app đều không mất phép tính đang làm.
 */
class CalcState(private val onChange: () -> Unit) {
    var expr by mutableStateOf("")
        private set
    var justEvaluated by mutableStateOf(false)
        private set
    var lastFull by mutableStateOf<String?>(null)
        private set
    var error by mutableStateOf<CalcError?>(null)
        private set

    fun restore(expr: String, justEvaluated: Boolean, lastFull: String?) {
        this.expr = expr
        this.justEvaluated = justEvaluated
        this.lastFull = lastFull
    }

    fun resultText(): String? = Calc.tryEvaluate(expr)?.let { Calc.format(it) }

    fun mainLine(): String = if (expr.isEmpty()) "0" else Calc.spaced(expr)

    /** Dòng nhỏ phía trên màn hình số (không gồm lỗi: giao diện hiển thị lỗi riêng). */
    fun secondaryLine(): String {
        if (justEvaluated) return lastFull ?: ""
        val r = resultText()
        return if (Calc.hasOperator(expr) && r != null) "= $r" else ""
    }

    /** null khi chưa có phép tính nào. */
    fun miniExpression(): String? {
        val lf = lastFull
        return when {
            justEvaluated && lf != null -> lf.substringBefore(" = ")
            expr.isNotEmpty() -> Calc.spaced(expr)
            else -> null
        }
    }

    fun miniResult(): String {
        val r = resultText() ?: return ""
        return if (justEvaluated || Calc.hasOperator(expr)) "= $r" else ""
    }

    /** "300,000 − 260,000 = 40,000" để chèn vào ghi chú. */
    fun fullCalculation(): String? {
        if (justEvaluated) return lastFull
        val r = resultText() ?: return null
        return if (Calc.hasOperator(expr)) "${Calc.spaced(Calc.trimTrailingOps(expr))} = $r" else null
    }

    fun press(key: String) {
        error = null
        var e = expr
        var je = justEvaluated
        var last = lastFull
        val lastCh = e.lastOrNull()
        var tok = e.takeLastWhile { it.isDigit() || it == '.' }
        when (key) {
            "C" -> {
                e = ""
                je = false
                last = null
            }
            "⌫" -> {
                e = e.dropLast(1)
                je = false
            }
            "=" -> {
                val t = Calc.trimTrailingOps(e)
                if (t.isNotEmpty() && t != "−") {
                    try {
                        val v = Calc.evaluate(t)
                        last = "${Calc.spaced(t)} = ${Calc.format(v)}"
                        e = Calc.raw(v)
                        je = true
                    } catch (ex: Calc.CalcException) {
                        error = ex.error
                    } catch (ex: ArithmeticException) {
                        error = CalcError.INVALID
                    }
                }
            }
            "+", "−", "×", "÷" -> {
                je = false
                e = when {
                    e.isEmpty() -> if (key == "−") "−" else ""
                    lastCh != null && (lastCh in Calc.OPS || lastCh == '.') -> {
                        val base = e.dropLast(1)
                        if (base.isEmpty()) (if (key == "−") "−" else "") else base + key
                    }
                    else -> e + key
                }
            }
            "%" -> {
                je = false
                if (lastCh != null && (lastCh.isDigit() || lastCh == '.')) e += "%"
            }
            "." -> {
                if (je) {
                    e = "0."
                    je = false
                } else if (!tok.contains('.')) {
                    e += if (lastCh != null && lastCh.isDigit()) "." else "0."
                }
            }
            "000" -> {
                je = false
                if (lastCh != null && lastCh.isDigit() && tok != "0") e += "000"
            }
            else -> {
                if (je) {
                    e = key
                    je = false
                } else {
                    if (lastCh == '%') {
                        e += "×"
                        tok = ""
                    }
                    e = if (tok == "0") e.dropLast(1) + key else e + key
                }
            }
        }
        expr = e
        justEvaluated = je
        lastFull = last
        onChange()
    }
}
