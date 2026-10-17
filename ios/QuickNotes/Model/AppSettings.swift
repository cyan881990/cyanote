import SwiftUI
import Combine

/// Ngôn ngữ có trong app: mã → tên viết bằng chính ngôn ngữ đó (giống APP_LANGUAGES của Android).
let appLanguages: [(tag: String, name: String)] = [
    ("en", "English"), ("vi", "Tiếng Việt"), ("es", "Español"), ("pt", "Português"),
    ("fr", "Français"), ("de", "Deutsch"), ("id", "Bahasa Indonesia"), ("th", "ไทย"),
    ("ja", "日本語"), ("ko", "한국어"), ("zh-CN", "简体中文"), ("hi", "हिन्दी"), ("ru", "Русский"),
]

/// Chuỗi chữ theo ngôn ngữ đang chọn trong app (độc lập với ngôn ngữ của máy).
struct Strings {
    let bundle: Bundle
    let locale: Locale
    let languageCode: String

    func callAsFunction(_ key: String) -> String {
        bundle.localizedString(forKey: key, value: Bundle.main.localizedString(forKey: key, value: key, table: nil), table: nil)
    }

    func f(_ key: String, _ args: CVarArg...) -> String {
        String(format: self(key), locale: locale, arguments: args)
    }

    var zodiac: [String] { (0..<12).map { self("zodiac_\($0)") } }
}

/// Cài đặt nhỏ: ngôn ngữ (mặc định tiếng Anh) và sáng/tối.
final class AppSettings: ObservableObject {
    static let defaultLanguage = "en"
    private let defaults = UserDefaults.standard

    /// "" = theo ngôn ngữ của điện thoại.
    @Published var languageTag: String {
        didSet { defaults.set(languageTag, forKey: "language"); strings = Self.makeStrings(languageTag) }
    }
    /// "SYSTEM" | "LIGHT" | "DARK" — cùng giá trị với Android để file sao lưu dùng chung.
    @Published var themeMode: String {
        didSet { defaults.set(themeMode, forKey: "theme") }
    }
    @Published private(set) var strings: Strings

    init() {
        let lang = UserDefaults.standard.string(forKey: "language") ?? Self.defaultLanguage
        languageTag = lang
        themeMode = UserDefaults.standard.string(forKey: "theme") ?? "SYSTEM"
        strings = Self.makeStrings(lang)
    }

    var colorSchemeOverride: ColorScheme? {
        switch themeMode {
        case "LIGHT": return .light
        case "DARK": return .dark
        default: return nil
        }
    }

    private static func lproj(for tag: String) -> String {
        switch tag {
        case "zh-CN": return "zh-Hans"
        default: return tag
        }
    }

    static func makeStrings(_ tag: String) -> Strings {
        let supported = appLanguages.map { lproj(for: $0.tag) }
        let folder: String
        let locale: Locale
        if tag.isEmpty {
            folder = Bundle.preferredLocalizations(from: supported).first ?? "en"
            locale = Locale.current
        } else {
            folder = lproj(for: tag)
            locale = Locale(identifier: tag == "zh-CN" ? "zh-Hans-CN" : tag)
        }
        let bundle = Bundle.main.path(forResource: folder, ofType: "lproj").flatMap(Bundle.init(path:)) ?? Bundle.main
        let code = String(folder.prefix(while: { $0 != "-" }))
        return Strings(bundle: bundle, locale: locale, languageCode: code)
    }
}

/// Bảng màu cyan, sáng/tối (cùng giá trị với Theme.kt của Android).
struct Palette {
    let bg, surface, ink, muted, line, accent, onAccent, accentSoft: Color
    let keyBg, opBg, opFg, eqBg, eqFg, clearBg, clearFg, sunday, adBg: Color

    static func of(_ scheme: ColorScheme) -> Palette { scheme == .dark ? dark : light }

    static let light = Palette(
        bg: Color(hex: 0xF1F5F6), surface: .white, ink: Color(hex: 0x14201F), muted: Color(hex: 0x52625F),
        line: Color(hex: 0xDDE6E8), accent: Color(hex: 0x0B7285), onAccent: .white, accentSoft: Color(hex: 0xE0F2F5),
        keyBg: Color(hex: 0xEEF3F4), opBg: Color(hex: 0xD2ECF1), opFg: Color(hex: 0x0A4D5A),
        eqBg: Color(hex: 0xF2A93B), eqFg: Color(hex: 0x2A1B00), clearBg: Color(hex: 0xF7E1D8), clearFg: Color(hex: 0x7E2A10),
        sunday: Color(hex: 0xB3261E), adBg: Color(hex: 0xFAFCFC))

    static let dark = Palette(
        bg: Color(hex: 0x0E1314), surface: Color(hex: 0x172022), ink: Color(hex: 0xE5EEF0), muted: Color(hex: 0x9AAAAE),
        line: Color(hex: 0x283437), accent: Color(hex: 0x4FC3D9), onAccent: Color(hex: 0x04161A), accentSoft: Color(hex: 0x12303A),
        keyBg: Color(hex: 0x212B2E), opBg: Color(hex: 0x143A45), opFg: Color(hex: 0xA5E4EF),
        eqBg: Color(hex: 0xF2A93B), eqFg: Color(hex: 0x2A1B00), clearBg: Color(hex: 0x3B2620), clearFg: Color(hex: 0xF4BBA6),
        sunday: Color(hex: 0xF2B8B5), adBg: Color(hex: 0x131B1D))
}

extension Color {
    init(hex: UInt32) {
        self.init(.sRGB,
                  red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255,
                  blue: Double(hex & 0xFF) / 255,
                  opacity: 1)
    }
}
