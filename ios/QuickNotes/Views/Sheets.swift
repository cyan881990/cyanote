import SwiftUI
import UniformTypeIdentifiers
import GoogleMobileAds

// MARK: - Lịch tháng (ngày dương + ngày âm)

struct MonthCalendarView: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @State private var monthStart: Date = Calendar(identifier: .gregorian).date(from: Calendar(identifier: .gregorian).dateComponents([.year, .month], from: Date())) ?? Date()

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        let cal = calendar
        let lang = s.languageCode
        let tz = LunarCalendar.timeZone(for: lang)
        let comps = cal.dateComponents([.year, .month], from: monthStart)
        let year = comps.year ?? 2026
        let month = comps.month ?? 1
        let days = cal.range(of: .day, in: .month, for: monthStart)?.count ?? 30
        let firstWeekday = cal.firstWeekday // 1 = Chủ nhật
        let weekdayOfFirst = cal.component(.weekday, from: monthStart)
        let offset = (weekdayOfFirst - firstWeekday + 7) % 7
        let symbols: [String] = {
            let f = DateFormatter()
            f.locale = s.locale
            let base = f.veryShortStandaloneWeekdaySymbols ?? ["S", "M", "T", "W", "T", "F", "S"]
            return (0..<7).map { base[(firstWeekday - 1 + $0) % 7] }
        }()
        let title: String = {
            let f = DateFormatter()
            f.locale = s.locale
            f.setLocalizedDateFormatFromTemplate("LLLLyyyy")
            let t = f.string(from: monthStart)
            return t.prefix(1).uppercased() + t.dropFirst()
        }()
        let midLunar = LunarCalendar.fromSolar(15, month, year, tz: tz)
        let today = cal.dateComponents([.year, .month, .day], from: Date())
        let columns = Array(repeating: GridItem(.flexible(), spacing: 2), count: 7)

        NavigationStack {
            VStack(spacing: 16) {
                HStack {
                    Button { shift(-1) } label: { Image(systemName: "chevron.left").frame(width: 44, height: 44) }
                    Spacer()
                    VStack(spacing: 2) {
                        Text(title).font(.system(size: 18, weight: .bold)).foregroundColor(p.ink)
                        Text(s.f("lunar_year", LunarCalendar.yearName(midLunar.year, style: LunarCalendar.style(for: lang), zodiac: s.zodiac)))
                            .font(.system(size: 13)).foregroundColor(p.muted)
                    }
                    Spacer()
                    Button { shift(1) } label: { Image(systemName: "chevron.right").frame(width: 44, height: 44) }
                }
                .foregroundColor(p.accent)

                LazyVGrid(columns: columns, spacing: 4) {
                    ForEach(0..<7, id: \.self) { i in
                        let weekday = (firstWeekday - 1 + i) % 7 + 1
                        Text(symbols[i])
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(weekday == 1 ? p.sunday : p.muted)
                    }
                    ForEach(0..<(offset + days), id: \.self) { idx in
                        if idx < offset {
                            Color.clear.frame(height: 46)
                        } else {
                            let day = idx - offset + 1
                            let l = LunarCalendar.fromSolar(day, month, year, tz: tz)
                            let isToday = today.year == year && today.month == month && today.day == day
                            let weekday = (weekdayOfFirst - 1 + day - 1) % 7 + 1
                            VStack(spacing: 1) {
                                Text("\(day)")
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(isToday ? p.onAccent : (weekday == 1 ? p.sunday : p.ink))
                                Text(l.day == 1 ? "\(l.day)/\(l.month)" : "\(l.day)")
                                    .font(.system(size: 10, weight: (l.day == 1 || l.day == 15) ? .bold : .regular))
                                    .foregroundColor(isToday ? p.onAccent : p.muted)
                            }
                            .frame(width: 42, height: 46)
                            .background(isToday ? p.accent : Color.clear)
                            .clipShape(Circle())
                        }
                    }
                }
                Spacer()
            }
            .padding(20)
            .background(p.bg.ignoresSafeArea())
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button(s("calendar_today")) {
                        monthStart = cal.date(from: cal.dateComponents([.year, .month], from: Date())) ?? Date()
                    }
                }
                ToolbarItem(placement: .confirmationAction) { Button(s("action_close")) { dismiss() } }
            }
        }
        .presentationDetents([.medium, .large])
    }

    private var calendar: Calendar {
        var c = Calendar(identifier: .gregorian)
        c.locale = settings.strings.locale
        return c
    }

    private func shift(_ months: Int) {
        monthStart = Calendar(identifier: .gregorian).date(byAdding: .month, value: months, to: monthStart) ?? monthStart
    }
}

// MARK: - Chọn ngôn ngữ

struct LanguageSheet: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        let s = settings.strings
        NavigationStack {
            List {
                row(s("language_system"), tag: "")
                ForEach(appLanguages, id: \.tag) { lang in row(lang.name, tag: lang.tag) }
            }
            .navigationTitle(s("language"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(s("action_cancel")) { dismiss() } }
            }
        }
    }

    private func row(_ name: String, tag: String) -> some View {
        Button {
            settings.languageTag = tag
            dismiss()
        } label: {
            HStack {
                Text(name).foregroundColor(.primary)
                Spacer()
                if settings.languageTag == tag { Image(systemName: "checkmark").foregroundColor(.accentColor) }
            }
        }
    }
}

// MARK: - Sao lưu & khôi phục (.zip)

struct ZipDocument: FileDocument {
    static var readableContentTypes: [UTType] { [.zip] }
    var data: Data
    init(data: Data) { self.data = data }
    init(configuration: ReadConfiguration) throws { data = configuration.file.regularFileContents ?? Data() }
    func fileWrapper(configuration: WriteConfiguration) throws -> FileWrapper { FileWrapper(regularFileWithContents: data) }
}

struct BackupSheet: View {
    @EnvironmentObject private var store: NotesStore
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss

    @State private var exportDoc: ZipDocument?
    @State private var showExporter = false
    @State private var showImporter = false
    @State private var pending: BackupData?
    @State private var message: String?

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text(s("backup_desc")).font(.system(size: 15)).foregroundColor(p.muted)
                Button {
                    let json = store.backupJSON(theme: settings.themeMode, language: settings.languageTag)
                    exportDoc = ZipDocument(data: Backup.makeZip(json: json, notes: store.notes, untitled: s("untitled")))
                    showExporter = true
                } label: {
                    Label(s("backup_export"), systemImage: "square.and.arrow.down")
                        .font(.system(size: 16, weight: .semibold))
                        .frame(maxWidth: .infinity).frame(height: 50)
                        .foregroundColor(p.onAccent).background(p.accent)
                        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                Button { showImporter = true } label: {
                    Label(s("backup_import"), systemImage: "square.and.arrow.up")
                        .font(.system(size: 16, weight: .semibold))
                        .frame(maxWidth: .infinity).frame(height: 50)
                        .foregroundColor(p.accent)
                        .overlay(RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(p.accent, lineWidth: 1.5))
                }
                if let message {
                    Text(message).font(.system(size: 14, weight: .medium)).foregroundColor(p.ink)
                }
                Spacer()
            }
            .padding(20)
            .background(p.bg.ignoresSafeArea())
            .navigationTitle(s("backup_title"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .confirmationAction) { Button(s("action_close")) { dismiss() } }
            }
            .fileExporter(isPresented: $showExporter, document: exportDoc, contentType: .zip,
                          defaultFilename: Backup.suggestedFileName()) { result in
                if case .success = result {
                    message = s.f("backup_export_done", store.notes.count)
                } else if case .failure = result {
                    message = s("backup_export_failed")
                }
            }
            .fileImporter(isPresented: $showImporter, allowedContentTypes: [.zip, .plainText, .data]) { result in
                guard case .success(let url) = result else { return }
                let access = url.startAccessingSecurityScopedResource()
                defer { if access { url.stopAccessingSecurityScopedResource() } }
                if let data = try? Data(contentsOf: url),
                   let parsed = try? Backup.read(data, fileName: url.lastPathComponent) {
                    pending = parsed
                } else {
                    message = s("backup_import_failed")
                }
            }
            .confirmationDialog(s.f("backup_import_found", pending?.notes.count ?? 0),
                                isPresented: Binding(get: { pending != nil }, set: { if !$0 { pending = nil } }),
                                titleVisibility: .visible) {
                Button(s("backup_merge")) { apply(replace: false) }
                Button(s("backup_replace"), role: .destructive) { apply(replace: true) }
                Button(s("action_cancel"), role: .cancel) { pending = nil }
            } message: {
                Text(s("backup_replace_warning"))
            }
        }
    }

    private func apply(replace: Bool) {
        guard let data = pending else { return }
        let added = store.importBackup(data, replace: replace)
        if replace {
            if let theme = data.theme, ["SYSTEM", "LIGHT", "DARK"].contains(theme) { settings.themeMode = theme }
            if let lang = data.language, lang.isEmpty || appLanguages.contains(where: { $0.tag == lang }) {
                settings.languageTag = lang
            }
        }
        pending = nil
        message = settings.strings.f("backup_import_done", added)
    }
}

// MARK: - Banner AdMob

enum AdConfig {
    /// ID banner THỬ NGHIỆM của Google. Trước khi phát hành, thay bằng ID thật trong AdMob
    /// (và GADApplicationIdentifier trong project.yml).
    static let bannerUnitID = "ca-app-pub-3940256099942544/2435281174"
}

/// Banner thích ứng, tối đa [width] điểm. Chỉ có MỘT GADBannerView dùng chung: khi banner
/// đổi chỗ (đáy ↔ đầu màn hình lúc gõ) nó được gắn sang chỗ mới, không tải lại quảng cáo.
struct BannerAdView: View {
    var width: CGFloat
    var mrec: Bool = false
    var body: some View {
        let size = mrec ? GADAdSizeMediumRectangle : GADCurrentOrientationAnchoredAdaptiveBannerAdSizeWithWidth(width)
        BannerRepresentable(adSize: size)
            .frame(width: size.size.width, height: size.size.height)
            .frame(maxWidth: .infinity)
    }
}

final class SharedBanner {
    static let shared = SharedBanner()
    private var banners: [String: GADBannerView] = [:]   // mỗi kích thước một quảng cáo, dùng lại

    func view(for size: GADAdSize) -> GADBannerView {
        let key = "\(Int(size.size.width))x\(Int(size.size.height))"
        if let b = banners[key] { return b }
        let b = GADBannerView(adSize: size)
        b.adUnitID = AdConfig.bannerUnitID
        b.rootViewController = UIApplication.shared.connectedScenes
            .compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }
            .first
        b.load(GADRequest())
        banners[key] = b
        return b
    }
}

private struct BannerRepresentable: UIViewRepresentable {
    let adSize: GADAdSize

    func makeUIView(context: Context) -> UIView {
        let container = UIView()
        attach(to: container)
        return container
    }

    func updateUIView(_ container: UIView, context: Context) {
        attach(to: container)
    }

    private func attach(to container: UIView) {
        let banner = SharedBanner.shared.view(for: adSize)
        guard banner.superview !== container else { return }
        banner.removeFromSuperview()
        banner.translatesAutoresizingMaskIntoConstraints = false
        container.addSubview(banner)
        NSLayoutConstraint.activate([
            banner.centerXAnchor.constraint(equalTo: container.centerXAnchor),
            banner.centerYAnchor.constraint(equalTo: container.centerYAnchor),
        ])
    }
}
