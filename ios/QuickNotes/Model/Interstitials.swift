import Foundation
import UIKit
import GoogleMobileAds

/// Quảng cáo toàn trang: cứ mỗi 3 ghi chú tạo mới (cộng dồn qua nhiều lần mở app) hiện một lần,
/// ngay sau khi bấm "+". Cách nhau tối thiểu 90 giây; chưa đủ thì dời sang lần tạo kế tiếp.
final class Interstitials: NSObject, GADFullScreenContentDelegate {
    static let shared = Interstitials()
    /// ID THỬ NGHIỆM của Google. Thay bằng ID thật trước khi phát hành.
    static let unitID = "ca-app-pub-3940256099942544/4411468910"
    private let every = 3
    private let minGap: TimeInterval = 90
    private var ad: GADInterstitialAd?
    private var loading = false
    private let d = UserDefaults.standard

    func preload() {
        guard ad == nil, !loading else { return }
        loading = true
        GADInterstitialAd.load(withAdUnitID: Self.unitID, request: GADRequest()) { [weak self] ad, _ in
            self?.loading = false
            self?.ad = ad
            ad?.fullScreenContentDelegate = self
        }
    }

    func onNoteCreated() {
        let count = d.integer(forKey: "notes_created_since_ad") + 1
        let now = Date().timeIntervalSince1970
        let due = count >= every && now - d.double(forKey: "interstitial_last_shown") >= minGap
        if due, let ad, let root = Self.topController() {
            ad.present(fromRootViewController: root)
            self.ad = nil
            d.set(0, forKey: "notes_created_since_ad")
            d.set(now, forKey: "interstitial_last_shown")
        } else {
            d.set(count, forKey: "notes_created_since_ad")
            preload()
        }
    }

    func adDidDismissFullScreenContent(_ ad: GADFullScreenPresentingAd) { preload() }
    func ad(_ ad: GADFullScreenPresentingAd, didFailToPresentFullScreenContentWithError error: Error) { preload() }

    private static func topController() -> UIViewController? {
        var c = UIApplication.shared.connectedScenes.compactMap { ($0 as? UIWindowScene)?.keyWindow?.rootViewController }.first
        while let p = c?.presentedViewController { c = p }
        return c
    }
}
