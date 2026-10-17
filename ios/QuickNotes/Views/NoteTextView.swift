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

    func makeCoordinator() -> Coordinator { Coordinator(self) }

    func makeUIView(context: Context) -> UITextView {
        let tv = UITextView()
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
        }

        func textViewDidChangeSelection(_ tv: UITextView) {
            guard !isUpdating else { return }
            let range = tv.selectedRange
            DispatchQueue.main.async { [weak self] in self?.parent.selection = range }
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
