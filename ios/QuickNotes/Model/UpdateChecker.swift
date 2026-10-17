import Foundation

/// Kiểm tra bản mới qua https://cyan881990.github.io/update.json (mục "ios").
/// Bản từ App Store được App Store tự cập nhật; thông báo này giúp bản thử (TestFlight/cài tay) biết có bản mới.
struct IOSUpdateInfo: Identifiable {
    let build: Int
    let version: String
    let url: URL
    var id: Int { build }
}

enum UpdateChecker {
    static let manifestURL = URL(string: "https://cyan881990.github.io/update.json")!
    private static let lastCheckKey = "update_last_check"
    private static let skippedKey = "update_skipped_build"

    static var currentBuild: Int {
        Int(Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "0") ?? 0
    }

    static func check(force: Bool = false) async -> IOSUpdateInfo? {
        let defaults = UserDefaults.standard
        let now = Date().timeIntervalSince1970
        if !force && now - defaults.double(forKey: lastCheckKey) < 6 * 3600 { return nil }
        defaults.set(now, forKey: lastCheckKey)

        var request = URLRequest(url: manifestURL, cachePolicy: .reloadIgnoringLocalCacheData, timeoutInterval: 8)
        request.setValue("no-cache", forHTTPHeaderField: "Cache-Control")
        guard let (data, _) = try? await URLSession.shared.data(for: request),
              let root = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let ios = root["ios"] as? [String: Any],
              let build = ios["build"] as? Int,
              let urlString = ios["url"] as? String,
              let url = URL(string: urlString) else { return nil }
        let version = ios["version"] as? String ?? ""
        guard build > currentBuild, build != defaults.integer(forKey: skippedKey) else { return nil }
        return IOSUpdateInfo(build: build, version: version, url: url)
    }

    static func skip(_ build: Int) {
        UserDefaults.standard.set(build, forKey: skippedKey)
    }
}
