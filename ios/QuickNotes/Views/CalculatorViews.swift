import SwiftUI
import UIKit

private let keyRows: [[String]] = [
    ["7", "8", "9", "÷", "C"],
    ["4", "5", "6", "×", "⌫"],
    ["1", "2", "3", "−", "%"],
    ["0", "000", ".", "+", "="],
]

private let keyLabelKeys: [String: String] = [
    "C": "key_clear", "⌫": "key_backspace", "%": "key_percent", "÷": "key_divide", "×": "key_multiply",
    "−": "key_minus", "+": "key_plus", "=": "key_equals", ".": "key_decimal", "000": "key_triple_zero",
]

/// Thanh máy tính thu gọn: luôn hiện phép tính đang giữ.
struct CalculatorBar: View {
    @ObservedObject var calc: CalcState
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    var compact: Bool = false
    var onExpand: () -> Void

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        Button(action: onExpand) {
            HStack(spacing: 12) {
                Image(systemName: "plus.forwardslash.minus")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(p.accent)
                    .frame(width: compact ? 32 : 40, height: compact ? 32 : 40)
                    .background(p.accentSoft)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                VStack(alignment: .leading, spacing: 1) {
                    if !compact {
                        Text(s("calc_holding")).font(.system(size: 12, weight: .medium)).foregroundColor(p.muted).lineLimit(1)
                    }
                    Text(calc.miniExpression() ?? s("calc_empty"))
                        .font(.system(size: 15, weight: .semibold)).foregroundColor(p.ink).lineLimit(1)
                }
                Spacer(minLength: 4)
                Text(calc.miniResult())
                    .font(.system(size: 17, weight: .bold).monospacedDigit())
                    .foregroundColor(p.accent)
                    .lineLimit(1)
                Image(systemName: "chevron.up").font(.system(size: 14, weight: .semibold)).foregroundColor(p.muted)
            }
            .padding(.leading, 10)
            .padding(.trailing, 14)
            .frame(height: compact ? 48 : 60)
            .background(p.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(p.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(s("calc_open"))
    }
}

/// Máy tính mở rộng (bản gọn 5×4), nút Thu gọn cạnh màn hình số.
struct CalculatorPanel: View {
    @ObservedObject var calc: CalcState
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    var onCollapse: (() -> Void)?   // nil: không có nút Thu gọn (màn hình rộng)
    var onInsert: (String) -> Void

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        let result = calc.resultText()
        let full = calc.fullCalculation()
        let errorText: String? = {
            switch calc.error {
            case .divideByZero: return s("calc_err_div0")
            case .invalid: return s("calc_err_invalid")
            case nil: return nil
            }
        }()

        VStack(spacing: 6) {
            HStack(spacing: 6) {
                VStack(alignment: .trailing, spacing: 0) {
                    Text(errorText ?? calc.secondaryLine())
                        .font(.system(size: 11))
                        .foregroundColor(errorText != nil ? p.clearFg : p.muted)
                        .lineLimit(1)
                        .frame(height: 15)
                    Text(calc.mainLine())
                        .font(.system(size: 22, weight: .bold).monospacedDigit())
                        .foregroundColor(p.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.6)
                }
                .frame(maxWidth: .infinity, alignment: .trailing)
                .padding(.horizontal, 12)
                .padding(.vertical, 5)
                .background(p.keyBg)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))

                if let onCollapse { Button(action: onCollapse) {
                    VStack(spacing: 1) {
                        Image(systemName: "chevron.down").font(.system(size: 15, weight: .semibold))
                        Text(s("calc_collapse")).font(.system(size: 10, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.7)
                    }
                    .foregroundColor(p.ink)
                    .frame(width: 56)
                    .frame(maxHeight: .infinity)
                    .background(p.keyBg)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain) }
            }
            .fixedSize(horizontal: false, vertical: true)

            ForEach(keyRows, id: \.self) { row in
                HStack(spacing: 6) {
                    ForEach(row, id: \.self) { key in
                        CalcKey(key: key, label: s(keyLabelKeys[key] ?? key)) {
                            UIImpactFeedbackGenerator(style: .light).impactOccurred()
                            calc.press(key)
                        }
                    }
                }
            }

            HStack(spacing: 6) {
                Button { if let full { onInsert(full) } } label: {
                    Text(s("calc_insert_full"))
                        .font(.system(size: 13, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.8)
                        .frame(maxWidth: .infinity).frame(height: 44)
                        .foregroundColor(full != nil ? p.accent : p.muted)
                        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(full != nil ? p.accent : p.line, lineWidth: 1.5))
                }
                .buttonStyle(.plain)
                .disabled(full == nil)
                Button { if let result { onInsert(result) } } label: {
                    Text(s("calc_insert_result"))
                        .font(.system(size: 13, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.8)
                        .frame(maxWidth: .infinity).frame(height: 44)
                        .foregroundColor(p.onAccent)
                        .background(result != nil ? p.accent : p.muted.opacity(0.5))
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                .buttonStyle(.plain)
                .disabled(result == nil)
            }
        }
        .padding(8)
        .background(p.surface)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(p.line, lineWidth: 1))
    }
}

private struct CalcKey: View {
    let key: String
    let label: String
    let action: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let p = Palette.of(colorScheme)
        let (bg, fg): (Color, Color) = {
            if key == "=" { return (p.eqBg, p.eqFg) }
            if ["+", "−", "×", "÷", "%"].contains(key) { return (p.opBg, p.opFg) }
            if key == "C" || key == "⌫" { return (p.clearBg, p.clearFg) }
            return (p.keyBg, p.ink)
        }()
        Button(action: action) {
            Text(key)
                .font(.system(size: 19, weight: .semibold))
                .foregroundColor(fg)
                .frame(maxWidth: .infinity)
                .frame(height: 44)
                .background(bg)
                .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
