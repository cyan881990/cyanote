import SwiftUI
import UIKit

/// Ô soạn thảo dựa trên UITextView để biết vị trí con trỏ ("Chèn kết quả" vào đúng chỗ).
struct NoteTextView: UIViewRepresentable {
    @Binding var text: String
    @Binding var selection: NSRange
    var font: UIFont
    var textColor: UIColor
    var tintColor: UIColor
    var focusToken: Int
    var lineNumbers: Bool = true
    var wordWrap: Bool = true
    var gutterText: UIColor = .secondaryLabel
    var gutterBackground: UIColor = .clear

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> UITextView {
        let tv = GutterTextView()
        tv.delegate = context.coordinator
        tv.backgroundColor = .clear
        tv.textContainerInset = UIEdgeInsets(top: 16, left: 16, bottom: 16, right: 16)
        tv.alwaysBounceVertical = true
        tv.keyboardDismissMode = .interactive
        tv.autocorrectionType = .default
        tv.font = font
        tv.textColor = textColor
        tv.tintColor = tintColor
        tv.text = text
        tv.gutter.textColor = gutterText
        tv.gutter.fill = gutterBackground
        tv.showNumbers = lineNumbers
        tv.wrap = wordWrap
        return tv
    }

    func updateUIView(_ tv: UITextView, context: Context) {
        context.coordinator.parent = self
        context.coordinator.isUpdating = true
        defer { context.coordinator.isUpdating = false }
        if tv.text != text { tv.text = text }
        if tv.font != font { tv.font = font }
        tv.textColor = textColor
        tv.tintColor = tintColor
        if let g = tv as? GutterTextView {
            g.gutter.textColor = gutterText
            g.gutter.fill = gutterBackground
            if g.showNumbers != lineNumbers { g.showNumbers = lineNumbers }
            if g.wrap != wordWrap { g.wrap = wordWrap }
            g.refreshGutter()
        }
        let length = (tv.text as NSString).length
        if selection.location <= length, NSMaxRange(selection) <= length, tv.selectedRange != selection {
            tv.selectedRange = selection
        }
        if context.coordinator.lastFocusToken != focusToken {
            context.coordinator.lastFocusToken = focusToken
            if focusToken > 0 { DispatchQueue.main.async { tv.becomeFirstResponder() } }
        }
    }

    final class Coordinator: NSObject, UITextViewDelegate {
        var parent: NoteTextView
        var isUpdating = false
        var lastFocusToken = 0

        init(_ parent: NoteTextView) { self.parent = parent }

        func textViewDidChange(_ tv: UITextView) {
            guard !isUpdating else { return }
            parent.text = tv.text
            parent.selection = tv.selectedRange
            scrollToCaret(tv)
        }

        /// Tự cuộn tới chỗ đang gõ.
        func scrollToCaret(_ tv: UITextView) {
            DispatchQueue.main.async {
                guard let end = tv.selectedTextRange?.end else { return }
                var rect = tv.caretRect(for: end)
                rect.size.height += 24
                tv.scrollRectToVisible(rect, animated: false)
            }
        }

        func textViewDidBeginEditing(_ tv: UITextView) {
            // Bàn phím vừa bật → vùng ghi chú thu nhỏ; đợi bố cục xong rồi cuộn tới con trỏ.
            DispatchQueue.main.asyncAfter(deadline: .now() + 0.35) { [weak self] in self?.scrollToCaret(tv) }
        }

        func textViewDidChangeSelection(_ tv: UITextView) {
            guard !isUpdating else { return }
            let range = tv.selectedRange
            DispatchQueue.main.async { [weak self] in self?.parent.selection = range }
        }
    }
}

/// UITextView có cột số dòng (như Notepad++) và chế độ không tự xuống dòng (cuộn ngang).
final class GutterTextView: UITextView {
    let gutter = LineGutterView()
    var showNumbers = true { didSet { updateInsets(); setNeedsLayout() } }
    var wrap = true { didSet { applyWrap() } }
    private var lastLineCount = 0

    override init(frame: CGRect, textContainer: NSTextContainer?) {
        super.init(frame: frame, textContainer: textContainer)
        gutter.textView = self
        gutter.isUserInteractionEnabled = false
        gutter.backgroundColor = .clear
        gutter.contentMode = .redraw
        addSubview(gutter)
    }

    required init?(coder: NSCoder) { fatalError() }

    var lineCount: Int { (text as NSString).components(separatedBy: "\n").count }
    var gutterWidth: CGFloat { CGFloat(String(lineCount).count) * 9 + 16 }

    func updateInsets() {
        textContainerInset = UIEdgeInsets(top: 16, left: showNumbers ? gutterWidth + 8 : 16, bottom: 16, right: 16)
    }

    func applyWrap() {
        if wrap {
            textContainer.widthTracksTextView = true
            textContainer.size = CGSize(width: bounds.width, height: .greatestFiniteMagnitude)
        } else {
            textContainer.widthTracksTextView = false
            textContainer.size = CGSize(width: 12_000, height: CGFloat.greatestFiniteMagnitude)
        }
        alwaysBounceHorizontal = !wrap
        showsHorizontalScrollIndicator = !wrap
        setNeedsLayout()
    }

    func refreshGutter() {
        let n = lineCount
        if String(n).count != String(lastLineCount).count { updateInsets() }
        lastLineCount = n
        gutter.setNeedsDisplay()
    }

    override func layoutSubviews() {
        super.layoutSubviews()
        gutter.isHidden = !showNumbers
        guard showNumbers else { return }
        gutter.frame = CGRect(x: contentOffset.x, y: 0, width: gutterWidth, height: max(contentSize.height, bounds.height + contentOffset.y))
        gutter.setNeedsDisplay()
        bringSubviewToFront(gutter)
    }
}

/// Vẽ số dòng thật (mỗi đoạn xuống dòng một số; dòng dài tự xuống vẫn giữ một số).
final class LineGutterView: UIView {
    weak var textView: GutterTextView?
    var textColor: UIColor = .secondaryLabel
    var fill: UIColor = .clear

    override func draw(_ rect: CGRect) {
        guard let tv = textView else { return }
        fill.setFill()
        UIRectFill(bounds)
        UIColor.separator.setFill()
        UIRectFill(CGRect(x: bounds.width - 0.5, y: 0, width: 0.5, height: bounds.height))
        let lm = tv.layoutManager
        let text = tv.text as NSString
        let font = UIFont.monospacedDigitSystemFont(ofSize: 12, weight: .regular)
        let attrs: [NSAttributedString.Key: Any] = [.font: font, .foregroundColor: textColor]
        let inset = tv.textContainerInset.top
        let visible = CGRect(x: 0, y: rect.minY - inset - 40, width: 20_000, height: rect.height + 80)
        let glyphs = lm.glyphRange(forBoundingRect: visible, in: tv.textContainer)
        let chars = lm.characterRange(forGlyphRange: glyphs, actualGlyphRange: nil)
        var idx = text.length == 0 ? 0 : text.lineRange(for: NSRange(location: min(chars.location, text.length), length: 0)).location
        var lineNo = 1
        if idx > 0 { lineNo = text.substring(to: idx).reduce(1) { $1 == "\n" ? $0 + 1 : $0 } }
        let end = NSMaxRange(chars)
        var guardCount = 0
        while guardCount < 5000 {
            guardCount += 1
            var lineRect: CGRect
            if idx >= text.length {
                lineRect = lm.extraLineFragmentRect
                if text.length > 0 && lineRect == .zero { break }
                if text.length == 0 { lineRect = CGRect(x: 0, y: 0, width: 10, height: font.lineHeight * 1.6) }
            } else {
                let g = lm.glyphIndexForCharacter(at: idx)
                lineRect = lm.lineFragmentRect(forGlyphAt: g, effectiveRange: nil)
            }
            let label = "\(lineNo)" as NSString
            let size = label.size(withAttributes: attrs)
            let y = inset + lineRect.minY + (min(lineRect.height, (tv.font?.lineHeight ?? 20) + 6) - size.height) / 2
            label.draw(at: CGPoint(x: bounds.width - size.width - 6, y: y), withAttributes: attrs)
            if idx >= text.length || idx > end { break }
            let r = text.lineRange(for: NSRange(location: idx, length: 0))
            let next = NSMaxRange(r)
            if next <= idx { break }
            // Đoạn cuối không có "\n" → dừng; có "\n" ở cuối → còn một dòng trống
            if next >= text.length && !text.hasSuffix("\n") { break }
            idx = next
            lineNo += 1
        }
    }
}

/// Theo dõi bàn phím: đang gõ thì thu gọn máy tính và tạm ẩn quảng cáo.
final class KeyboardObserver: ObservableObject {
    @Published var isVisible = false
    private var tokens: [NSObjectProtocol] = []

    init() {
        let nc = NotificationCenter.default
        tokens.append(nc.addObserver(forName: UIResponder.keyboardWillShowNotification, object: nil, queue: .main) { [weak self] _ in
            self?.isVisible = true
        })
        tokens.append(nc.addObserver(forName: UIResponder.keyboardWillHideNotification, object: nil, queue: .main) { [weak self] _ in
            self?.isVisible = false
        })
    }

    deinit { tokens.forEach(NotificationCenter.default.removeObserver) }
}

func hideKeyboard() {
    UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
}
