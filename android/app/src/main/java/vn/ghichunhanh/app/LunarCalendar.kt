package vn.ghichunhanh.app

import kotlin.math.PI
import kotlin.math.floor
import kotlin.math.sin

data class LunarDate(val day: Int, val month: Int, val year: Int, val leap: Boolean)

/** Cách gọi tên năm âm lịch theo ngôn ngữ. */
enum class LunarYearStyle {
    VIETNAMESE, // Bính Ngọ
    CHINESE,    // 丙午 (tiếng Trung, tiếng Nhật)
    KOREAN,     // 병오
    ZODIAC,     // con giáp theo ngôn ngữ: Horse, Caballo…
}

/**
 * Đổi ngày dương lịch sang âm lịch (mặc định múi giờ Việt Nam UTC+7;
 * tiếng Trung dùng UTC+8, tiếng Hàn/Nhật dùng UTC+9).
 * Thuật toán của Hồ Ngọc Đức, đã đối chiếu với các ngày Tết 2023–2026
 * và các tháng nhuận (nhuận 2/2023, nhuận 6/2025).
 */
object LunarCalendar {
    private val CAN = arrayOf("Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý")
    private val CHI = arrayOf("Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi")
    private val STEM_ZH = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
    private val BRANCH_ZH = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
    private val STEM_KO = arrayOf("갑", "을", "병", "정", "무", "기", "경", "신", "임", "계")
    private val BRANCH_KO = arrayOf("자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해")

    fun canChi(year: Int): String = CAN[(year + 6) % 10] + " " + CHI[(year + 8) % 12]

    /** Chỉ số con giáp 0..11 (0 = Tý/Chuột). */
    fun zodiacIndex(year: Int): Int = (year + 8) % 12

    /** Kiểu tên năm và múi giờ phù hợp với mã ngôn ngữ (vd "vi", "zh", "ko"). */
    fun styleFor(language: String): LunarYearStyle = when (language) {
        "vi" -> LunarYearStyle.VIETNAMESE
        "zh", "ja" -> LunarYearStyle.CHINESE
        "ko" -> LunarYearStyle.KOREAN
        else -> LunarYearStyle.ZODIAC
    }

    fun timeZoneFor(language: String): Double = when (language) {
        "vi" -> 7.0
        "ko", "ja" -> 9.0
        else -> 8.0
    }

    /** Tên năm; với ZODIAC cần truyền 12 tên con giáp đã dịch. */
    fun yearName(year: Int, style: LunarYearStyle, zodiacNames: List<String>): String = when (style) {
        LunarYearStyle.VIETNAMESE -> canChi(year)
        LunarYearStyle.CHINESE -> STEM_ZH[(year + 6) % 10] + BRANCH_ZH[(year + 8) % 12]
        LunarYearStyle.KOREAN -> STEM_KO[(year + 6) % 10] + BRANCH_KO[(year + 8) % 12]
        LunarYearStyle.ZODIAC -> zodiacNames.getOrElse(zodiacIndex(year)) { canChi(year) }
    }

    fun fromSolar(dd: Int, mm: Int, yy: Int, tz: Double = 7.0): LunarDate {
        val dayNumber = jdFromDate(dd, mm, yy)
        val k = floor((dayNumber - 2415021.076998695) / 29.530588853).toInt()
        var monthStart = newMoonDay(k + 1, tz)
        if (monthStart > dayNumber) monthStart = newMoonDay(k, tz)
        var a11 = lunarMonth11(yy, tz)
        var b11 = a11
        var lunarYear: Int
        if (a11 >= monthStart) {
            lunarYear = yy
            a11 = lunarMonth11(yy - 1, tz)
        } else {
            lunarYear = yy + 1
            b11 = lunarMonth11(yy + 1, tz)
        }
        val lunarDay = dayNumber - monthStart + 1
        val diff = floor((monthStart - a11) / 29.0).toInt()
        var leap = false
        var lunarMonth = diff + 11
        if (b11 - a11 > 365) {
            val leapDiff = leapMonthOffset(a11, tz)
            if (diff >= leapDiff) {
                lunarMonth = diff + 10
                if (diff == leapDiff) leap = true
            }
        }
        if (lunarMonth > 12) lunarMonth -= 12
        if (lunarMonth >= 11 && diff < 4) lunarYear -= 1
        return LunarDate(lunarDay, lunarMonth, lunarYear, leap)
    }

    private fun jdFromDate(dd: Int, mm: Int, yy: Int): Int {
        val a = (14 - mm) / 12
        val y = yy + 4800 - a
        val m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if (jd < 2299161) jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083
        return jd
    }

    private fun newMoon(k: Int): Double {
        val t = k / 1236.85
        val t2 = t * t
        val t3 = t2 * t
        val dr = PI / 180
        var jd1 = 2415020.75933 + 29.53058868 * k + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        val m = 359.2242 + 29.10535608 * k - 0.0000333 * t2 - 0.00000347 * t3
        val mpr = 306.0253 + 385.81691806 * k + 0.0107306 * t2 + 0.00001236 * t3
        val f = 21.2964 + 390.67050646 * k - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 = c1 - 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 = c1 + 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 = c1 - 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 = c1 - 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 = c1 + 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        val deltaT = if (t < -11) {
            0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltaT
    }

    private fun newMoonDay(k: Int, tz: Double): Int = floor(newMoon(k) + 0.5 + tz / 24).toInt()

    private fun sunLong(jdn: Double): Double {
        val t = (jdn - 2451545.0) / 36525
        val t2 = t * t
        val dr = PI / 180
        val m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        val l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl = dl + (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        val l = (l0 + dl) * dr
        return l - PI * 2 * floor(l / (PI * 2))
    }

    private fun sunLongitude(dayNumber: Int, tz: Double): Int =
        floor(sunLong(dayNumber - 0.5 - tz / 24) / PI * 6).toInt()

    private fun lunarMonth11(yy: Int, tz: Double): Int {
        val off = jdFromDate(31, 12, yy) - 2415021
        val k = floor(off / 29.530588853).toInt()
        var nm = newMoonDay(k, tz)
        if (sunLongitude(nm, tz) >= 9) nm = newMoonDay(k - 1, tz)
        return nm
    }

    private fun leapMonthOffset(a11: Int, tz: Double): Int {
        val k = floor((a11 - 2415021.076998695) / 29.530588853 + 0.5).toInt()
        var i = 1
        var arc = sunLongitude(newMoonDay(k + i, tz), tz)
        var last: Int
        do {
            last = arc
            i++
            arc = sunLongitude(newMoonDay(k + i, tz), tz)
        } while (arc != last && i < 14)
        return i - 1
    }
}
