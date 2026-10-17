import Foundation

/// Dữ liệu đọc được từ file sao lưu (hoặc từ file .txt rời).
struct BackupData {
    var notes: [Note]
    var calcExpr: String?
    var calcJustEvaluated = false
    var calcLastFull: String?
    var monospace: Bool?
    var theme: String?
    var language: String?
}

/// Sao lưu & khôi phục bằng file .zip — cùng định dạng với bản Android:
///   notes.json            dữ liệu đầy đủ
///   notes/01 - <tên>.txt  bản đọc được trên máy tính
enum Backup {
    static let format = "cyanote-backup-v1"
    private static let maxEntryBytes = 20 * 1024 * 1024

    struct ReadError: Error {}

    static func suggestedFileName(_ date: Date = Date()) -> String {
        let f = DateFormatter()
        f.locale = Locale(identifier: "en_US_POSIX")
        f.dateFormat = "yyyyMMdd-HHmm"
        return "cyanote-backup-\(f.string(from: date)).zip"
    }

    static func makeZip(json: Data, notes: [Note], untitled: String) -> Data {
        var entries: [(String, Data)] = [("notes.json", json)]
        var used = Set<String>()
        for (i, n) in notes.enumerated() {
            var base = safeFileName(n.displayTitle(untitled: untitled))
            if base.isEmpty { base = "note" }
            var name = String(format: "%02d - %@", i + 1, base)
            var k = 2
            while !used.insert(name.lowercased()).inserted {
                name = String(format: "%02d - %@ (%d)", i + 1, base, k)
                k += 1
            }
            entries.append(("notes/\(name).txt", Data(n.content.utf8)))
        }
        return Zip.write(entries)
    }

    /// Đọc file người dùng chọn: .zip sao lưu, .zip chứa các file .txt, hoặc một file .txt.
    static func read(_ data: Data, fileName: String?) throws -> BackupData {
        if data.count >= 2 && data[data.startIndex] == 0x50 && data[data.startIndex + 1] == 0x4B {
            return try readZip(data)
        }
        guard data.count <= maxEntryBytes, let text = String(data: data, encoding: .utf8),
              !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { throw ReadError() }
        let title = (fileName as NSString?)?.deletingPathExtension ?? ""
        return BackupData(notes: [Note(id: 0, title: title, content: text, updatedAt: millis())])
    }

    private static func readZip(_ data: Data) throws -> BackupData {
        let entries = try Zip.read(data, maxEntryBytes: maxEntryBytes)
        var json: Data?
        var textNotes: [Note] = []
        for (path, bytes) in entries {
            let name = (path.replacingOccurrences(of: "\\", with: "/") as NSString).lastPathComponent
            if name == "notes.json" && json == nil {
                json = bytes
            } else if name.lowercased().hasSuffix(".txt") && !name.hasPrefix(".") {
                var title = String(name.dropLast(4))
                if let r = title.range(of: #"^\d+ - "#, options: .regularExpression) { title.removeSubrange(r) }
                textNotes.append(Note(id: 0, title: title, content: String(decoding: bytes, as: UTF8.self), updatedAt: millis()))
            }
        }
        if let json, let parsed = try? parseJSON(json), !parsed.notes.isEmpty { return parsed }
        if !textNotes.isEmpty { return BackupData(notes: textNotes) }
        throw ReadError()
    }

    static func parseJSON(_ data: Data) throws -> BackupData {
        let file = try JSONDecoder().decode(StoreFile.self, from: data)
        return BackupData(
            notes: file.notes,
            calcExpr: file.calc?.expr,
            calcJustEvaluated: file.calc?.justEvaluated ?? false,
            calcLastFull: file.calc?.lastFull,
            monospace: file.monospace,
            theme: file.theme,
            language: file.language
        )
    }

    private static func safeFileName(_ s: String) -> String {
        let bad = CharacterSet(charactersIn: "\\/:*?\"<>|\r\n\t")
        let cleaned = s.unicodeScalars.map { bad.contains($0) ? "_" : String($0) }.joined()
        return String(cleaned.trimmingCharacters(in: .whitespaces).prefix(40))
    }

    private static func millis() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }
}

/// Đọc/ghi ZIP tối giản (iOS không có sẵn API zip):
/// ghi kiểu "stored" (không nén); đọc được cả "stored" và "deflate" (file từ Android).
enum Zip {
    struct Bad: Error {}

    static func write(_ entries: [(String, Data)]) -> Data {
        var out = Data()
        var central = Data()
        let (dosTime, dosDate) = dosDateTime(Date())
        for (name, data) in entries {
            let nameBytes = Data(name.utf8)
            let crc = CRC32.checksum(data)
            let offset = UInt32(out.count)
            // Local file header
            out.appendLE(UInt32(0x04034b50))
            out.appendLE(UInt16(20))            // version needed
            out.appendLE(UInt16(0x0800))        // tên file UTF-8
            out.appendLE(UInt16(0))             // stored
            out.appendLE(dosTime)
            out.appendLE(dosDate)
            out.appendLE(crc)
            out.appendLE(UInt32(data.count))
            out.appendLE(UInt32(data.count))
            out.appendLE(UInt16(nameBytes.count))
            out.appendLE(UInt16(0))
            out.append(nameBytes)
            out.append(data)
            // Central directory
            central.appendLE(UInt32(0x02014b50))
            central.appendLE(UInt16(20))         // version made by
            central.appendLE(UInt16(20))
            central.appendLE(UInt16(0x0800))
            central.appendLE(UInt16(0))
            central.appendLE(dosTime)
            central.appendLE(dosDate)
            central.appendLE(crc)
            central.appendLE(UInt32(data.count))
            central.appendLE(UInt32(data.count))
            central.appendLE(UInt16(nameBytes.count))
            central.appendLE(UInt16(0))          // extra
            central.appendLE(UInt16(0))          // comment
            central.appendLE(UInt16(0))          // disk
            central.appendLE(UInt16(0))          // internal attrs
            central.appendLE(UInt32(0))          // external attrs
            central.appendLE(offset)
            central.append(nameBytes)
        }
        let centralOffset = UInt32(out.count)
        out.append(central)
        out.appendLE(UInt32(0x06054b50))
        out.appendLE(UInt16(0))
        out.appendLE(UInt16(0))
        out.appendLE(UInt16(entries.count))
        out.appendLE(UInt16(entries.count))
        out.appendLE(UInt32(central.count))
        out.appendLE(centralOffset)
        out.appendLE(UInt16(0))
        return out
    }

    static func read(_ input: Data, maxEntryBytes: Int) throws -> [(String, Data)] {
        let d = [UInt8](input)
        guard d.count >= 22 else { throw Bad() }
        // Tìm "End of central directory" từ cuối file.
        var eocd = -1
        var i = d.count - 22
        let lowest = max(0, d.count - 22 - 65_535)
        while i >= lowest {
            if u32(d, i) == 0x06054b50 { eocd = i; break }
            i -= 1
        }
        guard eocd >= 0 else { throw Bad() }
        let count = Int(u16(d, eocd + 10))
        var p = Int(u32(d, eocd + 16))
        guard count <= 5000 else { throw Bad() }
        var result: [(String, Data)] = []
        for _ in 0..<count {
            guard p + 46 <= d.count, u32(d, p) == 0x02014b50 else { throw Bad() }
            let method = u16(d, p + 10)
            let compSize = Int(u32(d, p + 20))
            let size = Int(u32(d, p + 24))
            let nameLen = Int(u16(d, p + 28))
            let extraLen = Int(u16(d, p + 30))
            let commentLen = Int(u16(d, p + 32))
            let localOffset = Int(u32(d, p + 42))
            guard p + 46 + nameLen <= d.count else { throw Bad() }
            let name = String(decoding: d[(p + 46)..<(p + 46 + nameLen)], as: UTF8.self)
            p += 46 + nameLen + extraLen + commentLen

            if name.hasSuffix("/") { continue }
            guard size <= maxEntryBytes, localOffset + 30 <= d.count, u32(d, localOffset) == 0x04034b50 else { throw Bad() }
            let lNameLen = Int(u16(d, localOffset + 26))
            let lExtraLen = Int(u16(d, localOffset + 28))
            let start = localOffset + 30 + lNameLen + lExtraLen
            guard start + compSize <= d.count else { throw Bad() }
            let raw = Data(d[start..<(start + compSize)])
            switch method {
            case 0:
                result.append((name, raw))
            case 8:
                // Deflate thô; NSData.decompressed(.zlib) của Apple chính là deflate thô.
                guard let inflated = try? (raw as NSData).decompressed(using: .zlib) as Data,
                      inflated.count <= maxEntryBytes else { throw Bad() }
                result.append((name, inflated))
            default:
                continue
            }
        }
        return result
    }

    private static func u16(_ d: [UInt8], _ i: Int) -> UInt16 { UInt16(d[i]) | UInt16(d[i + 1]) << 8 }
    private static func u32(_ d: [UInt8], _ i: Int) -> UInt32 {
        let b0 = UInt32(d[i])
        let b1 = UInt32(d[i + 1]) << 8
        let b2 = UInt32(d[i + 2]) << 16
        let b3 = UInt32(d[i + 3]) << 24
        return b0 | b1 | b2 | b3
    }

    private static func dosDateTime(_ date: Date) -> (UInt16, UInt16) {
        let c = Calendar(identifier: .gregorian).dateComponents([.year, .month, .day, .hour, .minute, .second], from: date)
        let hour: Int = c.hour ?? 0
        let minute: Int = c.minute ?? 0
        let second: Int = c.second ?? 0
        let year: Int = max(0, (c.year ?? 1980) - 1980)
        let month: Int = c.month ?? 1
        let day: Int = c.day ?? 1
        let timeValue: Int = (hour << 11) | (minute << 5) | (second / 2)
        let dateValue: Int = (year << 9) | (month << 5) | day
        return (UInt16(truncatingIfNeeded: timeValue), UInt16(truncatingIfNeeded: dateValue))
    }
}

enum CRC32 {
    private static let table: [UInt32] = (0..<256).map { n -> UInt32 in
        var c = UInt32(n)
        for _ in 0..<8 { c = (c & 1) != 0 ? 0xEDB88320 ^ (c >> 1) : c >> 1 }
        return c
    }

    static func checksum(_ data: Data) -> UInt32 {
        var crc: UInt32 = 0xFFFFFFFF
        for b in data { crc = table[Int((crc ^ UInt32(b)) & 0xFF)] ^ (crc >> 8) }
        return crc ^ 0xFFFFFFFF
    }
}

private extension Data {
    mutating func appendLE(_ v: UInt16) { var x = v.littleEndian; Swift.withUnsafeBytes(of: &x) { append(contentsOf: $0) } }
    mutating func appendLE(_ v: UInt32) { var x = v.littleEndian; Swift.withUnsafeBytes(of: &x) { append(contentsOf: $0) } }
}
