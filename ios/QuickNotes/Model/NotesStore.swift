import Foundation
import Combine

struct Note: Codable, Identifiable, Equatable {
    var id: Int64
    var title: String
    var content: String
    var updatedAt: Int64

    /// Tên hiện trên tab: tên tự đặt, nếu không có thì lấy dòng đầu tiên.
    func displayTitle(untitled: String) -> String {
        let t = title.trimmingCharacters(in: .whitespacesAndNewlines)
        if !t.isEmpty { return t }
        guard let first = content.split(separator: "\n", omittingEmptySubsequences: false)
            .map({ $0.trimmingCharacters(in: .whitespaces) })
            .first(where: { !$0.isEmpty }) else { return untitled }
        return first.count > 18 ? String(first.prefix(18)) + "…" : first
    }
}

/// Định dạng file JSON — giống hệt bản Android để file sao lưu dùng chung hai nền tảng.
struct CalcFile: Codable {
    var expr: String?
    var justEvaluated: Bool?
    var lastFull: String?
}

struct StoreFile: Codable {
    var version: Int?
    var format: String?
    var exportedAt: Int64?
    var selectedId: Int64?
    var monospace: Bool?
    var theme: String?
    var language: String?
    var calc: CalcFile?
    var notes: [Note]
}

private func nowMillis() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

/// Lưu toàn bộ ghi chú + phép tính dở trong một file JSON riêng của app.
/// Tự lưu sau khi ngừng gõ 0,4 giây và lưu ngay khi app vào nền.
final class NotesStore: ObservableObject {
    @Published private(set) var notes: [Note] = []
    @Published private(set) var selectedId: Int64 = 0
    @Published private(set) var monospace = false
    @Published private(set) var isSaving = false

    let calc = CalcState()

    private let fileURL: URL
    private var saveWork: DispatchWorkItem?
    private var version = 0
    private let ioQueue = DispatchQueue(label: "notes.save")
    private var calcSink: AnyCancellable?

    init(welcomeTitle: String, welcomeText: String) {
        let dir = FileManager.default.urls(for: .applicationSupportDirectory, in: .userDomainMask)[0]
        try? FileManager.default.createDirectory(at: dir, withIntermediateDirectories: true)
        fileURL = dir.appendingPathComponent("notes.json")
        load()
        if notes.isEmpty {
            let now = nowMillis()
            notes = [Note(id: now, title: welcomeTitle, content: welcomeText, updatedAt: now)]
            selectedId = now
        }
        if !notes.contains(where: { $0.id == selectedId }) { selectedId = notes[0].id }
        calc.onChange = { [weak self] in self?.markChanged() }
        // Màn hình dùng store làm nguồn quan sát chính; báo cho nó biết khi máy tính đổi.
        calcSink = calc.objectWillChange.sink { [weak self] _ in self?.objectWillChange.send() }
    }

    var current: Note { notes.first(where: { $0.id == selectedId }) ?? notes[0] }

    func select(_ id: Int64) {
        guard selectedId != id else { return }
        selectedId = id
        markChanged()
    }

    /// Mở tab mới; nếu đã có tab trống thì chuyển sang tab đó.
    func newNote() {
        if let empty = notes.first(where: { $0.content.isEmpty && $0.title.trimmingCharacters(in: .whitespaces).isEmpty }) {
            select(empty.id)
            return
        }
        let id = max(nowMillis(), (notes.map(\.id).max() ?? 0) + 1)
        notes.append(Note(id: id, title: "", content: "", updatedAt: nowMillis()))
        selectedId = id
        markChanged()
    }

    func updateContent(_ id: Int64, _ content: String) {
        guard let i = notes.firstIndex(where: { $0.id == id }), notes[i].content != content else { return }
        notes[i].content = content
        notes[i].updatedAt = nowMillis()
        markChanged()
    }

    func rename(_ id: Int64, _ title: String) {
        guard let i = notes.firstIndex(where: { $0.id == id }) else { return }
        notes[i].title = title
        markChanged()
    }

    func delete(_ id: Int64) {
        AttachmentsStore.shared.deleteAll(id)
        guard let i = notes.firstIndex(where: { $0.id == id }) else { return }
        notes.remove(at: i)
        if notes.isEmpty {
            newNote()
            return
        }
        if selectedId == id { selectedId = notes[max(i - 1, 0)].id }
        markChanged()
    }

    func setMonospace(_ on: Bool) {
        monospace = on
        markChanged()
    }

    // MARK: Sao lưu

    func backupJSON(theme: String, language: String) -> Data {
        var file = snapshot()
        file.format = Backup.format
        file.exportedAt = nowMillis()
        file.theme = theme
        file.language = language
        let enc = JSONEncoder()
        enc.outputFormatting = [.prettyPrinted, .withoutEscapingSlashes]
        return (try? enc.encode(file)) ?? Data()
    }

    /// replace = false: thêm vào, bỏ qua ghi chú trùng y hệt. replace = true: thay toàn bộ.
    @discardableResult
    /// id ghi chú cũ → mới sau lần nhập gần nhất (để gắn lại lịch hẹn, ảnh).
    private(set) var lastImportIdMap: [Int64: Int64] = [:]

    func importBackup(_ data: BackupData, replace: Bool) -> Int {
        var idMap: [Int64: Int64] = [:]
        var nextId = max(nowMillis(), (notes.map(\.id).max() ?? 0) + 1)
        if replace { notes.removeAll() }
        var existing = Set(notes.map { "\($0.title)\u{1}\($0.content)" })
        var firstAdded: Int64?
        var added = 0
        for n in data.notes {
            let key = "\(n.title)\u{1}\(n.content)"
            if !replace && existing.contains(key) {
                if let same = notes.first(where: { $0.title == n.title && $0.content == n.content }) { idMap[n.id] = same.id }
                continue
            }
            var copy = n
            copy.id = nextId
            idMap[n.id] = nextId
            nextId += 1
            notes.append(copy)
            existing.insert(key)
            if firstAdded == nil { firstAdded = copy.id }
            added += 1
        }
        if replace {
            if let m = data.monospace { monospace = m }
            calc.restore(expr: data.calcExpr ?? "", justEvaluated: data.calcJustEvaluated, lastFull: data.calcLastFull)
        }
        if notes.isEmpty {
            newNote()
        } else {
            selectedId = firstAdded ?? selectedId
            if !notes.contains(where: { $0.id == selectedId }) { selectedId = notes[0].id }
        }
        lastImportIdMap = idMap
        markChanged()
        saveNow()
        return added
    }

    // MARK: Lưu file

    func saveNow() {
        saveWork?.cancel()
        guard isSaving else { return }
        let data = encode(snapshot())
        ioQueue.sync { try? data.write(to: fileURL, options: .atomic) }
        isSaving = false
    }

    private func markChanged() {
        version += 1
        isSaving = true
        saveWork?.cancel()
        let work = DispatchWorkItem { [weak self] in
            guard let self else { return }
            let v = self.version
            let data = self.encode(self.snapshot())
            let url = self.fileURL
            self.ioQueue.async {
                try? data.write(to: url, options: .atomic)
                DispatchQueue.main.async {
                    if self.version == v { self.isSaving = false }
                }
            }
        }
        saveWork = work
        DispatchQueue.main.asyncAfter(deadline: .now() + 0.4, execute: work)
    }

    private func snapshot() -> StoreFile {
        StoreFile(
            version: 1,
            selectedId: selectedId,
            monospace: monospace,
            calc: CalcFile(expr: calc.expr, justEvaluated: calc.justEvaluated, lastFull: calc.lastFull),
            notes: notes
        )
    }

    private func encode(_ file: StoreFile) -> Data {
        (try? JSONEncoder().encode(file)) ?? Data()
    }

    private func load() {
        guard let data = try? Data(contentsOf: fileURL),
              let file = try? JSONDecoder().decode(StoreFile.self, from: data) else { return }
        notes = file.notes
        selectedId = file.selectedId ?? 0
        monospace = file.monospace ?? false
        if let c = file.calc {
            calc.restore(expr: c.expr ?? "", justEvaluated: c.justEvaluated ?? false, lastFull: c.lastFull)
        }
    }
}
