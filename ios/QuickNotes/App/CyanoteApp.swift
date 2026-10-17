import SwiftUI
import GoogleMobileAds

@main
struct CyanoteApp: App {
    @StateObject private var settings: AppSettings
    @StateObject private var store: NotesStore
    @Environment(\.scenePhase) private var scenePhase

    init() {
        let settings = AppSettings()
        _settings = StateObject(wrappedValue: settings)
        // Lần chạy đầu tiên: ghi chú hướng dẫn bằng ngôn ngữ đang chọn (mặc định tiếng Anh).
        _store = StateObject(wrappedValue: NotesStore(
            welcomeTitle: settings.strings("welcome_title"),
            welcomeText: settings.strings("welcome_text")
        ))
        GADMobileAds.sharedInstance().start(completionHandler: nil)
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(settings)
                .environmentObject(store)
                .environment(\.locale, settings.strings.locale)
                .preferredColorScheme(settings.colorSchemeOverride)
        }
        .onChange(of: scenePhase) { phase in
            // Vào nền hoặc tắt app: ghi ngay xuống file.
            if phase != .active { store.saveNow() }
        }
    }
}
