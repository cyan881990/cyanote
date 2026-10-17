import Foundation
import Combine

/// Lỗi máy tính; giao diện tự đổi sang chữ theo ngôn ngữ.
enum CalcError { case divideByZero, invalid }

/// Bộ tính toán: + − × ÷ và %, dùng Decimal để không sai số với tiền.
/// Giống hệt bản Android (Calc.kt): "200000 + 10%" = 220000, "200000 × 10%" = 20000.
enum Calc {
    static let ops: Set<Character> = ["+", "−", "×", "÷"]

    struct Failure: Error { let error: CalcError }

    static func evaluate(_ input: String) throws -> Decimal {
        var parser = Parser(chars: Array(normalize(input)))
        let value = try parser.expression()
        if !parser.atEnd { throw Failure(error: .invalid) }
        return value
    }

    static func tryEvaluate(_ expr: String) -> Decimal? {
        let t = trimTrailingOps(expr)
        if t.isEmpty || t == "−" { return nil }
        return try? evaluate(t)
    }

    static func trimTrailingOps(_ expr: String) -> String {
        var s = expr
        while let last = s.last, "+−×÷.".contains(last) { s.removeLast() }
        return s
    }

    static func hasOperator(_ expr: String) -> Bool {
        expr.contains(where: { "+×÷%".contains($0) }) || expr.dropFirst().contains("−")
    }

    private static let formatter: NumberFormatter = {
        let f = NumberFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.numberStyle = .decimal
        f.usesGroupingSeparator = true
        f.groupingSeparator = ","
        f.decimalSeparator = "."
        f.groupingSize = 3
        f.minimumFractionDigits = 0
        f.maximumFractionDigits = 6
        f.roundingMode = .halfUp
        return f
    }()

    /// 1525000 → "1,525,000"
    static func format(_ v: Decimal) -> String {
        formatter.string(from: v as NSDecimalNumber) ?? "\(v)"
    }

    /// Dạng thô để tính tiếp sau khi bấm "=".
    static func raw(_ v: Decimal) -> String {
        var value = v
        var rounded = Decimal()
        NSDecimalRound(&rounded, &value, 6, .plain)
        let s = NSDecimalNumber(decimal: rounded).stringValue
        return s.hasPrefix("-") ? "−" + s.dropFirst() : s
    }

    /// "1525000.5" → "1,525,000.5" (chỉ nhóm phần nguyên của từng số).
    static func groupNumbers(_ expr: String) -> String {
        let chars = Array(expr)
        var out = ""
        var i = 0
        while i < chars.count {
            if chars[i].isASCII && chars[i].isNumber {
                var intPart = ""
                while i < chars.count, chars[i].isASCII, chars[i].isNumber { intPart.append(chars[i]); i += 1 }
                out += groupInt(intPart)
                if i < chars.count, chars[i] == "." {
                    out.append(".")
                    i += 1
                    while i < chars.count, chars[i].isASCII, chars[i].isNumber { out.append(chars[i]); i += 1 }
                }
            } else {
                out.append(chars[i])
                i += 1
            }
        }
        return out
    }

    /// "300000−260000" → "300,000 − 260,000"
    static func spaced(_ expr: String) -> String {
        let chars = Array(groupNumbers(expr))
        var out = ""
        for (i, c) in chars.enumerated() {
            if c == "+" || c == "×" || c == "÷" {
                out += " \(c) "
            } else if c == "−", i > 0, chars[i - 1].isNumber || chars[i - 1] == "%" {
                out += " − "
            } else {
                out.append(c)
            }
        }
        return out.trimmingCharacters(in: .whitespaces)
    }

    private static func groupInt(_ s: String) -> String {
        var out = ""
        for (i, c) in s.enumerated() {
            if i > 0 && (s.count - i) % 3 == 0 { out.append(",") }
            out.append(c)
        }
        return out
    }

    private static func normalize(_ s: String) -> String {
        var out = ""
        for c in s {
            switch c {
            case "−": out.append("-")
            case "×", "x", "X", "*": out.append("*")
            case "÷", "/": out.append("/")
            case ",", " ": continue
            default: out.append(c)
            }
        }
        return out
    }

    private struct Parser {
        let chars: [Character]
        var pos = 0

        var atEnd: Bool { pos >= chars.count }
        func peek() -> Character? { pos < chars.count ? chars[pos] : nil }

        mutating func expression() throws -> Decimal {
            var left = try term().value
            while let c = peek(), c == "+" || c == "-" {
                pos += 1
                let right = try term()
                let r = right.isPercent ? left * right.value : right.value
                left = c == "+" ? left + r : left - r
            }
            return left
        }

        mutating func term() throws -> (value: Decimal, isPercent: Bool) {
            var (value, isPercent) = try factor()
            while let c = peek(), c == "*" || c == "/" {
                pos += 1
                let right = try factor().value
                if c == "*" {
                    value *= right
                } else {
                    if right == 0 { throw Failure(error: .divideByZero) }
                    value /= right
                }
                isPercent = false
            }
            return (value, isPercent)
        }

        mutating func factor() throws -> (value: Decimal, isPercent: Bool) {
            guard let c = peek() else { throw Failure(error: .invalid) }
            if c == "-" {
                pos += 1
                let f = try factor()
                return (-f.value, f.isPercent)
            }
            if c == "+" {
                pos += 1
                return try factor()
            }
            let start = pos
            while pos < chars.count, (chars[pos].isASCII && chars[pos].isNumber) || chars[pos] == "." { pos += 1 }
            var text = String(chars[start..<pos])
            if text.hasSuffix(".") { text.removeLast() }
            if text.isEmpty { throw Failure(error: .invalid) }
            guard text.filter({ $0 == "." }).count <= 1,
                  var v = Decimal(string: text, locale: Locale(identifier: "en_US_POSIX")) else {
                throw Failure(error: .invalid)
            }
            var isPercent = false
            while peek() == "%" {
                pos += 1
                v /= 100
                isPercent = true
            }
            return (v, isPercent)
        }
    }
}

/// Trạng thái máy tính. Nằm trong NotesStore nên thu gọn, chuyển tab hay tắt app đều không mất phép tính.
final class CalcState: ObservableObject {
    @Published private(set) var expr = ""
    @Published private(set) var justEvaluated = false
    @Published private(set) var lastFull: String?
    @Published private(set) var error: CalcError?

    var onChange: () -> Void = {}

    func restore(expr: String, justEvaluated: Bool, lastFull: String?) {
        self.expr = expr
        self.justEvaluated = justEvaluated
        self.lastFull = lastFull
        self.error = nil
    }

    func resultText() -> String? { Calc.tryEvaluate(expr).map(Calc.format) }

    func mainLine() -> String { expr.isEmpty ? "0" : Calc.spaced(expr) }

    func secondaryLine() -> String {
        if justEvaluated { return lastFull ?? "" }
        if let r = resultText(), Calc.hasOperator(expr) { return "= \(r)" }
        return ""
    }

    /// nil khi chưa có phép tính nào.
    func miniExpression() -> String? {
        if justEvaluated, let lf = lastFull { return lf.components(separatedBy: " = ").first ?? lf }
        return expr.isEmpty ? nil : Calc.spaced(expr)
    }

    func miniResult() -> String {
        guard let r = resultText() else { return "" }
        return (justEvaluated || Calc.hasOperator(expr)) ? "= \(r)" : ""
    }

    /// "300,000 − 260,000 = 40,000" để chèn vào ghi chú.
    func fullCalculation() -> String? {
        if justEvaluated { return lastFull }
        guard let r = resultText(), Calc.hasOperator(expr) else { return nil }
        return "\(Calc.spaced(Calc.trimTrailingOps(expr))) = \(r)"
    }

    func press(_ key: String) {
        error = nil
        var e = expr
        var je = justEvaluated
        var last = lastFull
        let lastCh = e.last
        var tok = String(e.reversed().prefix(while: { ($0.isASCII && $0.isNumber) || $0 == "." }).reversed())

        switch key {
        case "C":
            e = ""; je = false; last = nil
        case "⌫":
            if !e.isEmpty { e.removeLast() }
            je = false
        case "=":
            let t = Calc.trimTrailingOps(e)
            if !t.isEmpty && t != "−" {
                do {
                    let v = try Calc.evaluate(t)
                    last = "\(Calc.spaced(t)) = \(Calc.format(v))"
                    e = Calc.raw(v)
                    je = true
                } catch let f as Calc.Failure {
                    error = f.error
                } catch {
                    self.error = .invalid
                }
            }
        case "+", "−", "×", "÷":
            je = false
            if e.isEmpty {
                e = key == "−" ? "−" : ""
            } else if let l = lastCh, Calc.ops.contains(l) || l == "." {
                let base = String(e.dropLast())
                e = base.isEmpty ? (key == "−" ? "−" : "") : base + key
            } else {
                e += key
            }
        case "%":
            je = false
            if let l = lastCh, (l.isASCII && l.isNumber) || l == "." { e += "%" }
        case ".":
            if je {
                e = "0."; je = false
            } else if !tok.contains(".") {
                if let l = lastCh, l.isASCII, l.isNumber { e += "." } else { e += "0." }
            }
        case "000":
            je = false
            if let l = lastCh, l.isASCII, l.isNumber, tok != "0" { e += "000" }
        default:
            if je {
                e = key; je = false
            } else {
                if lastCh == "%" { e += "×"; tok = "" }
                if tok == "0" { e.removeLast(); e += key } else { e += key }
            }
        }
        expr = e
        justEvaluated = je
        lastFull = last
        onChange()
    }
}
