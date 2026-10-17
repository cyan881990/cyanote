import Foundation
import UIKit
import AVFoundation

/// Ảnh (.jpg) & ghi âm (.m4a) của từng ghi chú: Documents/attachments/<id ghi chú>/<thời điểm>.<đuôi>
final class AttachmentsStore: ObservableObject {
    static let shared = AttachmentsStore()
    @Published private(set) var version = 0     // tăng mỗi khi thay đổi để giao diện vẽ lại
    private let root = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("attachments", isDirectory: true)

    static func isImage(_ u: URL) -> Bool { u.pathExtension.lowercased() == "jpg" }
    static func isAudio(_ u: URL) -> Bool { u.pathExtension.lowercased() == "m4a" }

    private func dir(_ noteId: Int64) -> URL { root.appendingPathComponent(String(noteId), isDirectory: true) }

    func list(_ noteId: Int64) -> [URL] {
        let files = (try? FileManager.default.contentsOfDirectory(at: dir(noteId), includingPropertiesForKeys: nil)) ?? []
        return files.filter { Self.isImage($0) || Self.isAudio($0) }.sorted { $0.lastPathComponent < $1.lastPathComponent }
    }

    private func millis() -> Int64 { Int64(Date().timeIntervalSince1970 * 1000) }

    /// Lưu ảnh (thu nhỏ cạnh dài 2048px, JPEG 85%).
    @discardableResult
    func addImage(_ noteId: Int64, _ image: UIImage) -> URL? {
        let maxSide: CGFloat = 2048
        let scale = min(1, maxSide / max(image.size.width, image.size.height))
        let size = CGSize(width: image.size.width * scale, height: image.size.height * scale)
        let fmt = UIGraphicsImageRendererFormat(); fmt.scale = 1
        let resized = UIGraphicsImageRenderer(size: size, format: fmt).image { _ in image.draw(in: CGRect(origin: .zero, size: size)) }
        guard let data = resized.jpegData(compressionQuality: 0.85) else { return nil }
        try? FileManager.default.createDirectory(at: dir(noteId), withIntermediateDirectories: true)
        let url = dir(noteId).appendingPathComponent("\(millis()).jpg")
        do { try data.write(to: url, options: .atomic) } catch { return nil }
        version += 1
        return url
    }

    func newRecordingURL(_ noteId: Int64) -> URL {
        try? FileManager.default.createDirectory(at: dir(noteId), withIntermediateDirectories: true)
        return dir(noteId).appendingPathComponent("\(millis()).m4a")
    }

    func putRaw(_ noteId: Int64, name: String, data: Data) {
        let safe = String(name.split(separator: "/").last ?? "").filter { $0.isLetter || $0.isNumber || $0 == "." || $0 == "-" || $0 == "_" }
        guard safe.hasSuffix(".jpg") || safe.hasSuffix(".m4a") else { return }
        try? FileManager.default.createDirectory(at: dir(noteId), withIntermediateDirectories: true)
        try? data.write(to: dir(noteId).appendingPathComponent(safe), options: .atomic)
        version += 1
    }

    func delete(_ url: URL) { try? FileManager.default.removeItem(at: url); version += 1 }
    func deleteAll(_ noteId: Int64) { try? FileManager.default.removeItem(at: dir(noteId)); version += 1 }
    func changed() { version += 1 }

    /// (id ghi chú, file) — để sao lưu.
    func all() -> [(Int64, URL)] {
        let dirs = (try? FileManager.default.contentsOfDirectory(at: root, includingPropertiesForKeys: nil)) ?? []
        return dirs.flatMap { d -> [(Int64, URL)] in
            guard let id = Int64(d.lastPathComponent) else { return [] }
            return list(id).map { (id, $0) }
        }
    }

    static func duration(_ url: URL) -> TimeInterval {
        (try? AVAudioPlayer(contentsOf: url))?.duration ?? 0
    }
}

/// Phát một bản ghi âm mỗi lúc.
final class AudioPlayback: NSObject, ObservableObject, AVAudioPlayerDelegate {
    static let shared = AudioPlayback()
    @Published private(set) var playing: URL?
    private var player: AVAudioPlayer?

    func toggle(_ url: URL) {
        if playing == url { stop(); return }
        stop()
        try? AVAudioSession.sharedInstance().setCategory(.playback)
        try? AVAudioSession.sharedInstance().setActive(true)
        player = try? AVAudioPlayer(contentsOf: url)
        player?.delegate = self
        if player?.play() == true { playing = url }
    }

    var position: TimeInterval { player?.currentTime ?? 0 }

    func stop() { player?.stop(); player = nil; playing = nil }

    func audioPlayerDidFinishPlaying(_ p: AVAudioPlayer, successfully flag: Bool) {
        DispatchQueue.main.async { self.stop() }
    }
}

/// Ghi âm vào file .m4a.
final class VoiceRecorder: ObservableObject {
    @Published private(set) var elapsed: TimeInterval = 0
    private var recorder: AVAudioRecorder?
    private var timer: Timer?
    let url: URL

    init(url: URL) { self.url = url }

    func start(completion: @escaping (Bool) -> Void) {
        AVAudioSession.sharedInstance().requestRecordPermission { granted in
            DispatchQueue.main.async {
                guard granted else { completion(false); return }
                let session = AVAudioSession.sharedInstance()
                try? session.setCategory(.playAndRecord, mode: .default, options: [.defaultToSpeaker])
                try? session.setActive(true)
                let settings: [String: Any] = [
                    AVFormatIDKey: Int(kAudioFormatMPEG4AAC),
                    AVSampleRateKey: 44_100,
                    AVNumberOfChannelsKey: 1,
                    AVEncoderBitRateKey: 96_000,
                ]
                self.recorder = try? AVAudioRecorder(url: self.url, settings: settings)
                let ok = self.recorder?.record() ?? false
                if ok {
                    self.timer = Timer.scheduledTimer(withTimeInterval: 0.2, repeats: true) { [weak self] _ in
                        self?.elapsed = self?.recorder?.currentTime ?? 0
                    }
                }
                completion(ok)
            }
        }
    }

    func stop(save: Bool) {
        timer?.invalidate(); timer = nil
        recorder?.stop(); recorder = nil
        try? AVAudioSession.sharedInstance().setActive(false, options: .notifyOthersOnDeactivation)
        if !save { try? FileManager.default.removeItem(at: url) }
    }
}

func formatDuration(_ t: TimeInterval) -> String {
    let s = Int(t.rounded(.down))
    return String(format: "%d:%02d", s / 60, s % 60)
}
