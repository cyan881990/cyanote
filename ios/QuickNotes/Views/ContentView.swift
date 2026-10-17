import SwiftUI
import UIKit

struct ContentView: View {
    @EnvironmentObject private var store: NotesStore
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @StateObject private var keyboard = KeyboardObserver()

    @State private var calcOpen = false
    @State private var selections: [Int64: NSRange] = [:]
    @State private var focusToken = 0
    @State private var renaming: Note?
    @State private var renameText = ""
    @State private var deleting: Note?
    @State private var showMonth = false
    @State private var showLanguages = false
    @State private var showBackup = false
    @State private var update: IOSUpdateInfo?          // hộp thoại
    @State private var available: IOSUpdateInfo? = UpdateChecker.cached()   // nhãn ở đầu màn hình
    @State private var upToDate = false
    @Environment(\.openURL) private var openURL

    private var s: Strings { settings.strings }
    private var p: Palette { Palette.of(colorScheme) }

    var body: some View {
        let current = store.current
        GeometryReader { geo in
            let w = geo.size.width
            let h = geo.size.height                       // đã trừ bàn phím
            let typing = keyboard.isVisible
            let wide = w >= 720 || (w > h && w >= 560)    // iPad, iPhone xoay ngang → 2 cột
            let sideWidth: CGFloat = w >= 1000 ? 392 : (w >= 720 ? 344 : 332)
            let compactHeader = typing || h < 520
            let showTabs = true                     // luôn có thanh tab (nút + tạo ghi chú mới)
            let typingCalcRow = typing && !wide
            let adWidth: CGFloat = wide ? sideWidth - 12 : min(w - 24, 480)
            let header = HeaderView(
                onLanguage: { showLanguages = true },
                onCalendar: { showMonth = true },
                updateLabel: available == nil ? nil : s("update_now"),
                onUpdate: { update = available },
                compact: compactHeader,
                onHideKeyboard: nil
            )
            if wide {
                HStack(spacing: 0) {
                    VStack(spacing: 0) {
                        header
                        if typing { BannerAdView(width: adWidth).padding(.horizontal, 12).padding(.bottom, 6) }
                        if showTabs { TabBarView(menu: AnyView(menu(current))) }
                        editorCard(current, showHideKeyboard: typing)
                        Spacer().frame(height: 10)
                    }
                    VStack(spacing: 8) {
                        // Đủ cao: phím giãn lấp kín cột (không chừa khoảng trống); thấp: cuộn.
                        ViewThatFits(in: .vertical) {
                            CalculatorPanel(calc: store.calc, onCollapse: nil, fill: true) { text in insert(text, into: current) }
                                .frame(maxHeight: .infinity)
                            ScrollView {
                                CalculatorPanel(calc: store.calc, onCollapse: nil) { text in insert(text, into: current) }
                            }
                        }
                        if !typing {
                            // Đủ cao: khung 300×250 lấp chỗ trống dưới máy tính; không thì banner.
                            if h >= 660 { BannerAdView(width: 300, mrec: true) } else { BannerAdView(width: adWidth) }
                        }
                    }
                    .frame(width: sideWidth - 12)
                    .padding(.top, 10)
                    .padding(.trailing, 12)
                    .padding(.bottom, 6)
                }
            } else {
                VStack(spacing: 0) {
                    header
                    // Đang gõ: quảng cáo lên đầu màn hình, xa bàn phím.
                    if typing { BannerAdView(width: adWidth).padding(.horizontal, 12).padding(.bottom, 6) }
                    if showTabs { TabBarView(menu: AnyView(menu(current))) }
                    editorCard(current)
                    VStack(spacing: 8) {
                        if !typing {
                            if calcOpen {
                                // Màn hình thấp: máy tính tối đa ~60% chiều cao, cuộn được.
                                ViewThatFits(in: .vertical) {
                                    calcPanel(current)
                                    ScrollView { calcPanel(current) }
                                }
                                .frame(maxHeight: h * 0.62)
                                .transition(.move(edge: .bottom).combined(with: .opacity))
                            } else {
                                CalculatorBar(calc: store.calc) {
                                    hideKeyboard()
                                    withAnimation(.easeOut(duration: 0.2)) { calcOpen = true }
                                }
                            }
                            BannerAdView(width: adWidth)
                        }
                        if typingCalcRow {
                            HStack(spacing: 8) {
                                CalculatorBar(calc: store.calc, compact: true) {
                                    hideKeyboard()
                                    withAnimation(.easeOut(duration: 0.2)) { calcOpen = true }
                                }
                                Button(action: hideKeyboard) {
                                    Image(systemName: "keyboard.chevron.compact.down")
                                        .font(.system(size: 18, weight: .semibold))
                                        .foregroundColor(p.accent)
                                        .frame(width: 56, height: 48)
                                        .background(p.surface)
                                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                                        .overlay(RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(p.line, lineWidth: 1))
                                }
                                .buttonStyle(.plain)
                                .accessibilityLabel(s("hide_keyboard"))
                            }
                        }
                    }
                    .padding(.horizontal, 12)
                    .padding(.top, typing ? 6 : 10)
                    .padding(.bottom, 6)
                }
            }
        }
        .background(p.bg.ignoresSafeArea())
        .onChange(of: keyboard.isVisible) { visible in
            if visible { withAnimation { calcOpen = false } }
        }
        .onChange(of: store.selectedId) { _ in
            if store.current.content.isEmpty { focusToken += 1 }
        }
        .alert(s("rename_title"), isPresented: Binding(get: { renaming != nil }, set: { if !$0 { renaming = nil } })) {
            TextField(s("rename_label"), text: $renameText)
            Button(s("action_save")) {
                if let n = renaming { store.rename(n.id, renameText.trimmingCharacters(in: .whitespaces)) }
                renaming = nil
            }
            Button(s("action_cancel"), role: .cancel) { renaming = nil }
        }
        .alert(s.f("close_title", deleting?.displayTitle(untitled: s("untitled")) ?? ""),
               isPresented: Binding(get: { deleting != nil }, set: { if !$0 { deleting = nil } })) {
            Button(s("action_delete"), role: .destructive) {
                if let n = deleting { store.delete(n.id) }
                deleting = nil
            }
            Button(s("action_keep"), role: .cancel) { deleting = nil }
        } message: {
            Text(s("close_message"))
        }
        .alert(s.f("up_to_date", (Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "") + " (\(UpdateChecker.currentBuild))"), isPresented: $upToDate) {
            Button("OK", role: .cancel) {}
        }
        .task {
            let info = await UpdateChecker.check()
            available = info
            if let info, !UpdateChecker.isSkipped(info) { update = info }
        }
        .alert(s("update_title"), isPresented: Binding(get: { update != nil }, set: { if !$0 { update = nil } })) {
            Button(s("update_now")) {
                if let u = update { openURL(u.url) }
                update = nil
            }
            Button(s("update_later"), role: .cancel) {
                if let u = update { UpdateChecker.skip(u.build) }
                update = nil
            }
        } message: {
            Text(s.f("update_message", update?.version ?? ""))
        }
        .sheet(isPresented: $showMonth) { MonthCalendarView() }
        .sheet(isPresented: $showLanguages) { LanguageSheet() }
        .sheet(isPresented: $showBackup) { BackupSheet() }
    }

    // MARK: Vùng ghi chú

    private func editorCard(_ current: Note, showHideKeyboard: Bool = false) -> some View {
        let textBinding = Binding<String>(
            get: { store.current.content },
            set: { store.updateContent(store.current.id, $0) }
        )
        let selectionBinding = Binding<NSRange>(
            get: { selections[current.id] ?? NSRange(location: (current.content as NSString).length, length: 0) },
            set: { selections[current.id] = $0 }
        )
        let font: UIFont = store.monospace
            ? .monospacedSystemFont(ofSize: 16, weight: .regular)
            : .systemFont(ofSize: 16)
        return VStack(spacing: 0) {
            ZStack(alignment: .topLeading) {
                NoteTextView(
                    text: textBinding,
                    selection: selectionBinding,
                    font: font,
                    textColor: UIColor(p.ink),
                    tintColor: UIColor(p.accent),
                    focusToken: focusToken
                )
                .id(current.id)
                if current.content.isEmpty {
                    Text(s("editor_hint"))
                        .font(.system(size: 16))
                        .foregroundColor(p.muted)
                        .padding(.horizontal, 21)
                        .padding(.vertical, 16)
                        .allowsHitTesting(false)
                }
            }
            Divider().overlay(p.line)
            HStack(spacing: 6) {
                let lines = current.content.isEmpty ? 1 : current.content.filter { $0 == "\n" }.count + 1
                Text(s.f("status_counts", lines, (current.content as NSString).length))
                    .foregroundColor(p.muted)
                Spacer()
                if store.isSaving {
                    Text(s("status_saving")).foregroundColor(p.muted)
                } else {
                    Image(systemName: "checkmark").font(.system(size: 11, weight: .bold)).foregroundColor(p.accent)
                    Text(s("status_saved")).foregroundColor(p.accent)
                }
                if showHideKeyboard {
                    Button(action: hideKeyboard) {
                        Image(systemName: "keyboard.chevron.compact.down")
                            .font(.system(size: 14, weight: .semibold))
                            .foregroundColor(p.accent)
                            .frame(width: 44, height: 30)
                            .background(p.accentSoft)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                    }
                    .buttonStyle(.plain)
                    .padding(.leading, 6)
                    .accessibilityLabel(s("hide_keyboard"))
                }
            }
            .font(.system(size: 12, weight: .medium))
            .padding(.horizontal, 20)
            .padding(.vertical, 9)
        }
        .background(p.surface)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 22, style: .continuous).stroke(p.line, lineWidth: 1))
        .padding(.horizontal, 12)
    }

    private func calcPanel(_ current: Note) -> some View {
        CalculatorPanel(calc: store.calc, onCollapse: { withAnimation(.easeOut(duration: 0.2)) { calcOpen = false } }) { text in
            insert(text, into: current)
        }
    }

    // MARK: Menu ⋮

    private func menu(_ current: Note) -> some View {
        Menu {
            if keyboard.isVisible {
                Button { hideKeyboard() } label: { Label(s("hide_keyboard"), systemImage: "keyboard.chevron.compact.down") }
            }
            Button { renameText = current.title; renaming = current } label: { Label(s("menu_rename"), systemImage: "pencil") }
            Button { UIPasteboard.general.string = current.content } label: { Label(s("menu_copy"), systemImage: "doc.on.doc") }
            ShareLink(item: current.content, subject: Text(current.displayTitle(untitled: s("untitled")))) {
                Label(s("menu_share"), systemImage: "square.and.arrow.up")
            }
            Button { showMonth = true } label: { Label(s("menu_calendar"), systemImage: "calendar") }
            Button { showLanguages = true } label: { Label(s("language"), systemImage: "globe") }
            Button {
                Task {
                    let info = await UpdateChecker.check(force: true)
                    available = info
                    if let info { update = info } else { upToDate = true }
                }
            } label: { Label(s("check_updates"), systemImage: available == nil ? "arrow.triangle.2.circlepath" : "arrow.down.circle.fill") }
            Button { showBackup = true } label: { Label(s("backup_menu"), systemImage: "archivebox") }
            Toggle(isOn: Binding(get: { store.monospace }, set: { store.setMonospace($0) })) {
                Label(s("menu_monospace"), systemImage: "textformat")
            }
            Divider()
            Button(role: .destructive) {
                if current.content.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { store.delete(current.id) } else { deleting = current }
            } label: { Label(s("menu_close"), systemImage: "xmark") }
        } label: {
            Image(systemName: "ellipsis")
                .rotationEffect(.degrees(90))
                .font(.system(size: 17, weight: .semibold))
                .foregroundColor(p.muted)
                .frame(width: 36, height: 40)
                .contentShape(Rectangle())
        }
        .accessibilityLabel(s("more_options"))
    }

    // MARK: Chèn vào đúng vị trí con trỏ

    private func insert(_ text: String, into note: Note) {
        let ns = note.content as NSString
        let sel = selections[note.id] ?? NSRange(location: ns.length, length: 0)
        let start = min(max(0, sel.location), ns.length)
        let end = min(NSMaxRange(sel), ns.length)
        var piece = text
        if start > 0, let scalar = UnicodeScalar(ns.character(at: start - 1)),
           !CharacterSet.whitespacesAndNewlines.contains(scalar) {
            piece = " " + piece
        }
        let newText = ns.replacingCharacters(in: NSRange(location: start, length: max(0, end - start)), with: piece)
        store.updateContent(note.id, newText)
        selections[note.id] = NSRange(location: start + (piece as NSString).length, length: 0)
    }
}

// MARK: - Đồng hồ + lịch + nút nhanh

struct HeaderView: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    var onLanguage: () -> Void
    var onCalendar: () -> Void
    var updateLabel: String? = nil
    var onUpdate: () -> Void = {}
    var compact: Bool = false                 // 1 dòng (đang gõ / màn hình thấp)
    var onHideKeyboard: (() -> Void)? = nil

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        TimelineView(.everyMinute) { context in
            let now = context.date
            let comps = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day], from: now)
            let lang = s.languageCode
            let lunar = LunarCalendar.fromSolar(comps.day ?? 1, comps.month ?? 1, comps.year ?? 2026, tz: LunarCalendar.timeZone(for: lang))
            let yearName = LunarCalendar.yearName(lunar.year, style: LunarCalendar.style(for: lang), zodiac: s.zodiac)
            if compact {
                compactRow(now, s, p, lunarText: s.f(lunar.leap ? "lunar_date_leap" : "lunar_date", lunar.day, lunar.month))
            } else {
            HStack(alignment: .bottom, spacing: 12) {
                VStack(alignment: .leading, spacing: 2) {
                    // Logo nhỏ của app, ngang hàng với các nút nhanh bên phải
                    HStack(spacing: 7) {
                        Image("LogoMark").resizable().frame(width: 24, height: 24)
                        Text(s("app_name")).font(.system(size: 14, weight: .bold)).foregroundColor(p.ink).lineLimit(1)
                    }
                    .frame(height: 36)
                    HStack(alignment: .lastTextBaseline, spacing: 6) {
                        Text(timeString(now, s.locale))
                            .font(.system(size: 44, weight: .bold).monospacedDigit())
                            .kerning(-1.5)
                            .foregroundColor(p.ink)
                        if let ampm = amPm(now, s.locale) {
                            Text(ampm).font(.system(size: 16, weight: .semibold)).foregroundColor(p.muted)
                        }
                    }
                    Text(dateLine(now, s.locale))
                        .font(.system(size: 14, weight: .medium))
                        .foregroundColor(p.muted)
                        .lineLimit(2)
                        .minimumScaleFactor(0.85)
                }
                Spacer(minLength: 0)
                VStack(alignment: .trailing, spacing: 8) {
                    HStack(spacing: 6) {
                        // Nhãn "Có bản mới" cùng tông với thẻ âm lịch
                        if let updateLabel {
                            Button(action: onUpdate) {
                                HStack(spacing: 4) {
                                    Image(systemName: "arrow.down.to.line").font(.system(size: 13, weight: .bold))
                                    // iPhone hẹp: chỉ biểu tượng
                                    if UIScreen.main.bounds.width >= 380 {
                                        Text(updateLabel).font(.system(size: 12, weight: .bold)).lineLimit(1)
                                    }
                                }
                                .foregroundColor(p.accent)
                                .padding(.leading, 8)
                                .padding(.trailing, 10)
                                .frame(height: 36)
                                .background(p.accentSoft)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(p.accent.opacity(0.35), lineWidth: 1))
                            }
                            .buttonStyle(.plain)
                        }
                        Button(action: onLanguage) {
                            HStack(spacing: 5) {
                                Image(systemName: "globe").font(.system(size: 14, weight: .medium))
                                Text(langCode(s.languageCode)).font(.system(size: 12, weight: .bold)).kerning(0.5)
                            }
                            .foregroundColor(p.ink)
                            .padding(.horizontal, 10)
                            .frame(height: 36)
                            .background(p.surface)
                            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                            .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(p.line, lineWidth: 1))
                        }
                        .accessibilityLabel(s("language"))
                        Button {
                            settings.themeMode = (colorScheme == .dark) ? "LIGHT" : "DARK"
                        } label: {
                            Image(systemName: colorScheme == .dark ? "sun.max" : "moon")
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(p.ink)
                                .frame(width: 36, height: 36)
                                .background(p.surface)
                                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(p.line, lineWidth: 1))
                        }
                        .accessibilityLabel(s(colorScheme == .dark ? "theme_to_light" : "theme_to_dark"))
                    }
                    Button(action: onCalendar) {
                        VStack(alignment: .trailing, spacing: 1) {
                            HStack(spacing: 4) {
                                Image(systemName: "calendar").font(.system(size: 11, weight: .semibold))
                                Text(s("lunar_label")).font(.system(size: 11, weight: .semibold)).kerning(0.8)
                            }
                            .foregroundColor(p.accent)
                            Text(s.f(lunar.leap ? "lunar_date_leap" : "lunar_date", lunar.day, lunar.month))
                                .font(.system(size: 16, weight: .bold))
                                .foregroundColor(p.ink)
                            Text(s.f("lunar_year", yearName))
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(p.muted)
                        }
                        .padding(.horizontal, 12)
                        .padding(.vertical, 8)
                        .background(p.accentSoft)
                        .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
                    }
                    .accessibilityLabel(s("calendar_open"))
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 8)
            .padding(.bottom, 10)
            }
        }
    }

    @ViewBuilder
    private func compactRow(_ now: Date, _ s: Strings, _ p: Palette, lunarText: String) -> some View {
        HStack(spacing: 6) {
            Image("LogoMark").resizable().frame(width: 22, height: 22)
            Text(timeString(now, s.locale) + (amPm(now, s.locale).map { " " + $0 } ?? ""))
                .font(.system(size: 20, weight: .bold).monospacedDigit())
                .foregroundColor(p.ink)
                .lineLimit(1)
                .fixedSize()
            Text(shortDate(now, s.locale))
                .font(.system(size: 13, weight: .medium))
                .foregroundColor(p.muted)
                .lineLimit(1)
                .frame(maxWidth: .infinity, alignment: .leading)
            Button(action: onCalendar) {
                HStack(spacing: 4) {
                    Image(systemName: "calendar").font(.system(size: 11, weight: .semibold)).foregroundColor(p.accent)
                    Text(lunarText).font(.system(size: 12, weight: .bold)).foregroundColor(p.ink).lineLimit(1)
                }
                .padding(.horizontal, 8)
                .frame(height: 32)
                .background(p.accentSoft)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .buttonStyle(.plain)
            .fixedSize()
            if updateLabel != nil {
                smallButton("arrow.down.to.line", p, tint: p.accent, bg: p.accentSoft, action: onUpdate)
            }
            smallButton("globe", p, action: onLanguage)
            smallButton(colorScheme == .dark ? "sun.max" : "moon", p) {
                settings.themeMode = (colorScheme == .dark) ? "LIGHT" : "DARK"
            }
            if let onHideKeyboard {
                smallButton("keyboard.chevron.compact.down", p, tint: p.accent, action: onHideKeyboard)
            }
        }
        .padding(.horizontal, 14)
        .padding(.vertical, 6)
    }

    private func smallButton(_ icon: String, _ p: Palette, tint: Color? = nil, bg: Color? = nil, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: icon)
                .font(.system(size: 14, weight: .medium))
                .foregroundColor(tint ?? p.ink)
                .frame(width: 32, height: 32)
                .background(bg ?? p.surface)
                .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay(RoundedRectangle(cornerRadius: 10, style: .continuous).stroke(p.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }

    private func shortDate(_ d: Date, _ locale: Locale) -> String {
        let f = DateFormatter()
        f.locale = locale
        f.setLocalizedDateFormatFromTemplate("EEEdMMM")
        return f.string(from: d)
    }

    private func uses24h(_ locale: Locale) -> Bool {
        let format = DateFormatter.dateFormat(fromTemplate: "j", options: 0, locale: locale) ?? "HH"
        return !format.contains("a")
    }

    private func timeString(_ d: Date, _ locale: Locale) -> String {
        let f = DateFormatter()
        f.locale = locale
        f.dateFormat = uses24h(locale) ? "HH:mm" : "h:mm"
        return f.string(from: d)
    }

    private func amPm(_ d: Date, _ locale: Locale) -> String? {
        guard !uses24h(locale) else { return nil }
        let f = DateFormatter()
        f.locale = locale
        f.dateFormat = "a"
        return f.string(from: d)
    }

    private func dateLine(_ d: Date, _ locale: Locale) -> String {
        let f = DateFormatter()
        f.locale = locale
        f.dateStyle = .full
        f.timeStyle = .none
        let s = f.string(from: d)
        return s.prefix(1).uppercased() + s.dropFirst()
    }

    private func langCode(_ code: String) -> String { code.uppercased() }
}

// MARK: - Thanh tab

struct TabBarView: View {
    @EnvironmentObject private var store: NotesStore
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    var menu: AnyView

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        HStack(spacing: 6) {
            ScrollViewReader { proxy in
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 6) {
                        ForEach(store.notes) { note in
                            let active = note.id == store.selectedId
                            Button { store.select(note.id) } label: {
                                Text(note.displayTitle(untitled: s("untitled")))
                                    .font(.system(size: 14, weight: .semibold))
                                    .lineLimit(1)
                                    .frame(maxWidth: 160)
                                    .padding(.horizontal, 14)
                                    .padding(.vertical, 8)
                                    .foregroundColor(active ? p.onAccent : p.muted)
                                    .background(active ? p.accent : p.surface)
                                    .clipShape(Capsule())
                                    .overlay(Capsule().stroke(active ? p.accent : p.line, lineWidth: 1))
                            }
                            .id(note.id)
                        }
                    }
                    .padding(.trailing, 28)
                }
                // Mép phải mờ dần: còn tab phía sau, không cần thanh cuộn.
                .mask(
                    HStack(spacing: 0) {
                        Rectangle()
                        LinearGradient(colors: [.black, .clear], startPoint: .leading, endPoint: .trailing).frame(width: 36)
                    }
                )
                .onChange(of: store.selectedId) { id in
                    withAnimation { proxy.scrollTo(id, anchor: .center) }
                }
            }
            Button { store.newNote() } label: {
                Image(systemName: "plus")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundColor(p.ink)
                    .frame(width: 40, height: 40)
                    .background(p.surface)
                    .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(p.line, lineWidth: 1))
            }
            .accessibilityLabel(s("tab_add"))
            menu
        }
        .padding(.leading, 12)
        .padding(.trailing, 4)
        .padding(.bottom, 10)
    }
}
