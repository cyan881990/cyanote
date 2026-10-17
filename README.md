# cyanote++

Ghi chú nhiều tab kiểu Notepad, tự lưu, có máy tính thu gọn được, đồng hồ + lịch âm luôn ở trên, sao lưu một file .zip, sáng/tối, 13 ngôn ngữ (mặc định tiếng Anh). Kiếm tiền bằng banner AdMob.

| Thư mục | Nội dung |
| --- | --- |
| `android/` | App Android (Kotlin + Jetpack Compose) — chi tiết: `android/README.vi.md` |
| `ios/` | App iOS (SwiftUI). Dự án Xcode sinh từ `ios/project.yml` bằng XcodeGen |
| `tools/i18n/` | Bản dịch gốc 13 ngôn ngữ + script sinh file chữ cho Android và iOS |
| `tools/icon/` | Script vẽ logo và xuất mọi cỡ biểu tượng |
| `store/` | Biểu tượng 512, ảnh bìa Google Play, logo |
| (kho riêng) | Trang web + Chính sách bảo mật: https://github.com/cyan881990/cyan881990.github.io |
| `.github/workflows/` | Build tự động Android, iOS |

## Tải bản thử

Mỗi lần đẩy code lên nhánh `main`, GitHub Actions tự build và đăng file lên trang **Releases → Bản thử mới nhất**:

- `cyanote-android-test.apk` — mở bằng điện thoại Android để cài.
- `cyanote-android-release.aab` — file gửi Google Play (cần ký, xem dưới).
- `cyanote-ios-unsigned.ipa` — bản iPhone chưa ký; cài bằng Sideloadly/AltStore với Apple ID, hoặc build bằng Xcode.
- `cyanote-ios-simulator.zip` — chạy trên máy ảo iOS của Xcode.

## Chạy iOS trên iPhone bằng máy Mac

```
brew install xcodegen
cd ios && xcodegen generate && open Cyanote.xcodeproj
```

Trong Xcode: chọn target **Cyanote** → Signing & Capabilities → chọn Team (Apple ID miễn phí chạy được 7 ngày; đăng App Store cần Apple Developer 99 USD/năm) → cắm iPhone → Run.

## Sửa chữ / bản dịch

```
python3 tools/i18n/gen_strings.py android/app/src/main/res
python3 tools/i18n/gen_ios_strings.py ios/QuickNotes/Resources
```

## Sửa logo

```
python3 tools/icon/make_icons.py
```
Biểu tượng Android là vector trong `android/app/src/main/res/drawable/ic_launcher_*.xml` (cùng hình học).

## Ký bản Android cho Google Play

1. Tạo keystore: `keytool -genkeypair -v -keystore cyanote.jks -alias cyanote -keyalg RSA -keysize 2048 -validity 10000`
2. Trên GitHub: Settings → Secrets and variables → Actions, thêm:
   `CYANOTE_KEYSTORE_BASE64` (= `base64 -w0 cyanote.jks`), `CYANOTE_KEYSTORE_PASSWORD`, `CYANOTE_KEY_ALIAS`, `CYANOTE_KEY_PASSWORD`.
3. Đẩy code lại → file `.aab` trong Releases đã được ký. Giữ kỹ file keystore.

## Trước khi phát hành

- Thay mã AdMob thử nghiệm: Android (`AndroidManifest.xml`, `ads/BannerAd.kt`), iOS (`ios/project.yml` → `GADApplicationIdentifier`, `Views/Sheets.swift` → `AdConfig`).
- Thêm hộp thoại đồng ý quảng cáo (Google UMP) nếu phát hành ở châu Âu/Anh.
- Chính sách bảo mật: https://cyan881990.github.io/privacy.html (kho cyan881990.github.io).
