import Foundation

struct LunarDate { let day: Int; let month: Int; let year: Int; let leap: Bool }

enum LunarYearStyle { case vietnamese, chinese, korean, zodiac }

/// Đổi ngày dương sang âm lịch (thuật toán Hồ Ngọc Đức). Giống hệt LunarCalendar.kt của Android.
/// Múi giờ: Việt Nam +7, Trung Quốc +8, Hàn/Nhật +9.
enum LunarCalendar {
    private static let can = ["Giáp", "Ất", "Bính", "Đinh", "Mậu", "Kỷ", "Canh", "Tân", "Nhâm", "Quý"]
    private static let chi = ["Tý", "Sửu", "Dần", "Mão", "Thìn", "Tỵ", "Ngọ", "Mùi", "Thân", "Dậu", "Tuất", "Hợi"]
    private static let stemZh = ["甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸"]
    private static let branchZh = ["子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥"]
    private static let stemKo = ["갑", "을", "병", "정", "무", "기", "경", "신", "임", "계"]
    private static let branchKo = ["자", "축", "인", "묘", "진", "사", "오", "미", "신", "유", "술", "해"]

    static func canChi(_ year: Int) -> String { can[(year + 6) % 10] + " " + chi[(year + 8) % 12] }
    static func zodiacIndex(_ year: Int) -> Int { (year + 8) % 12 }

    static func style(for language: String) -> LunarYearStyle {
        switch language {
        case "vi": return .vietnamese
        case "zh", "ja": return .chinese
        case "ko": return .korean
        default: return .zodiac
        }
    }

    static func timeZone(for language: String) -> Double {
        switch language {
        case "vi": return 7
        case "ko", "ja": return 9
        default: return 8
        }
    }

    static func yearName(_ year: Int, style: LunarYearStyle, zodiac: [String]) -> String {
        switch style {
        case .vietnamese: return canChi(year)
        case .chinese: return stemZh[(year + 6) % 10] + branchZh[(year + 8) % 12]
        case .korean: return stemKo[(year + 6) % 10] + branchKo[(year + 8) % 12]
        case .zodiac:
            let i = zodiacIndex(year)
            return i < zodiac.count ? zodiac[i] : canChi(year)
        }
    }

    static func fromSolar(_ dd: Int, _ mm: Int, _ yy: Int, tz: Double = 7) -> LunarDate {
        let dayNumber = jdFromDate(dd, mm, yy)
        let k = Int(floor((Double(dayNumber) - 2415021.076998695) / 29.530588853))
        var monthStart = newMoonDay(k + 1, tz)
        if monthStart > dayNumber { monthStart = newMoonDay(k, tz) }
        var a11 = lunarMonth11(yy, tz)
        var b11 = a11
        var lunarYear: Int
        if a11 >= monthStart {
            lunarYear = yy
            a11 = lunarMonth11(yy - 1, tz)
        } else {
            lunarYear = yy + 1
            b11 = lunarMonth11(yy + 1, tz)
        }
        let lunarDay = dayNumber - monthStart + 1
        let diff = Int(floor(Double(monthStart - a11) / 29.0))
        var leap = false
        var lunarMonth = diff + 11
        if b11 - a11 > 365 {
            let leapDiff = leapMonthOffset(a11, tz)
            if diff >= leapDiff {
                lunarMonth = diff + 10
                if diff == leapDiff { leap = true }
            }
        }
        if lunarMonth > 12 { lunarMonth -= 12 }
        if lunarMonth >= 11 && diff < 4 { lunarYear -= 1 }
        return LunarDate(day: lunarDay, month: lunarMonth, year: lunarYear, leap: leap)
    }

    private static func jdFromDate(_ dd: Int, _ mm: Int, _ yy: Int) -> Int {
        let a = (14 - mm) / 12
        let y = yy + 4800 - a
        let m = mm + 12 * a - 3
        var jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - y / 100 + y / 400 - 32045
        if jd < 2299161 { jd = dd + (153 * m + 2) / 5 + 365 * y + y / 4 - 32083 }
        return jd
    }

    private static func newMoon(_ k: Int) -> Double {
        let kd = Double(k)
        let t = kd / 1236.85
        let t2 = t * t
        let t3 = t2 * t
        let dr = Double.pi / 180
        var jd1 = 2415020.75933 + 29.53058868 * kd + 0.0001178 * t2 - 0.000000155 * t3
        jd1 += 0.00033 * sin((166.56 + 132.87 * t - 0.009173 * t2) * dr)
        let m = 359.2242 + 29.10535608 * kd - 0.0000333 * t2 - 0.00000347 * t3
        let mpr = 306.0253 + 385.81691806 * kd + 0.0107306 * t2 + 0.00001236 * t3
        let f = 21.2964 + 390.67050646 * kd - 0.0016528 * t2 - 0.00000239 * t3
        var c1 = (0.1734 - 0.000393 * t) * sin(m * dr) + 0.0021 * sin(2 * dr * m)
        c1 = c1 - 0.4068 * sin(mpr * dr) + 0.0161 * sin(dr * 2 * mpr)
        c1 -= 0.0004 * sin(dr * 3 * mpr)
        c1 = c1 + 0.0104 * sin(dr * 2 * f) - 0.0051 * sin(dr * (m + mpr))
        c1 = c1 - 0.0074 * sin(dr * (m - mpr)) + 0.0004 * sin(dr * (2 * f + m))
        c1 = c1 - 0.0004 * sin(dr * (2 * f - m)) - 0.0006 * sin(dr * (2 * f + mpr))
        c1 = c1 + 0.0010 * sin(dr * (2 * f - mpr)) + 0.0005 * sin(dr * (2 * mpr + m))
        let deltaT: Double
        if t < -11 {
            deltaT = 0.001 + 0.000839 * t + 0.0002261 * t2 - 0.00000845 * t3 - 0.000000081 * t * t3
        } else {
            deltaT = -0.000278 + 0.000265 * t + 0.000262 * t2
        }
        return jd1 + c1 - deltaT
    }

    private static func newMoonDay(_ k: Int, _ tz: Double) -> Int { Int(floor(newMoon(k) + 0.5 + tz / 24)) }

    private static func sunLong(_ jdn: Double) -> Double {
        let t = (jdn - 2451545.0) / 36525
        let t2 = t * t
        let dr = Double.pi / 180
        let m = 357.52910 + 35999.05030 * t - 0.0001559 * t2 - 0.00000048 * t * t2
        let l0 = 280.46645 + 36000.76983 * t + 0.0003032 * t2
        var dl = (1.914600 - 0.004817 * t - 0.000014 * t2) * sin(dr * m)
        dl = dl + (0.019993 - 0.000101 * t) * sin(dr * 2 * m) + 0.000290 * sin(dr * 3 * m)
        let l = (l0 + dl) * dr
        return l - Double.pi * 2 * floor(l / (Double.pi * 2))
    }

    private static func sunLongitude(_ dayNumber: Int, _ tz: Double) -> Int {
        Int(floor(sunLong(Double(dayNumber) - 0.5 - tz / 24) / Double.pi * 6))
    }

    private static func lunarMonth11(_ yy: Int, _ tz: Double) -> Int {
        let off = Double(jdFromDate(31, 12, yy) - 2415021)
        let k = Int(floor(off / 29.530588853))
        var nm = newMoonDay(k, tz)
        if sunLongitude(nm, tz) >= 9 { nm = newMoonDay(k - 1, tz) }
        return nm
    }

    private static func leapMonthOffset(_ a11: Int, _ tz: Double) -> Int {
        let k = Int(floor((Double(a11) - 2415021.076998695) / 29.530588853 + 0.5))
        var i = 1
        var arc = sunLongitude(newMoonDay(k + i, tz), tz)
        var last: Int
        repeat {
            last = arc
            i += 1
            arc = sunLongitude(newMoonDay(k + i, tz), tz)
        } while arc != last && i < 14
        return i - 1
    }
}
