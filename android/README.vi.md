# Ghi Chú Nhanh

App ghi chú nhiều tab cho Android, kiểu Notepad++: gõ là tự lưu, mở tab nhanh, có máy tính thu gọn được và đồng hồ + lịch âm luôn ở trên cùng. Kiếm tiền bằng banner AdMob.

## Tính năng

- **Nhiều tab**: bấm `+` để mở tab mới, vuốt thanh tab để chuyển, menu `⋮` để đổi tên, sao chép, chia sẻ, đóng tab.
- **Tự lưu**: lưu sau khi ngừng gõ 0,4 giây và lưu ngay khi thoát app. Ghi file kiểu atomic nên sập giữa chừng cũng không hỏng dữ liệu.
- **Máy tính**: `+ − × ÷ %`, nút `000`, tính bằng BigDecimal (không sai số tiền). Thu gọn thành một thanh nhỏ vẫn hiện phép tính đang giữ; tắt app mở lại vẫn còn. "Chèn kết quả" / "Chèn phép tính" đưa vào đúng chỗ con trỏ.
- **Đồng hồ + lịch**: giờ, thứ, ngày dương, ngày âm lịch (có tháng nhuận, năm can chi). Bấm ô Âm lịch để xem lịch tháng.
- **Nút nhanh ở góc trên phải**: 🌐 đổi ngôn ngữ, ☀/🌙 chuyển nền sáng/tối (app nhớ lựa chọn).
- **13 ngôn ngữ**: Anh, Việt, Tây Ban Nha, Bồ Đào Nha, Pháp, Đức, Indonesia, Thái, Nhật, Hàn, Trung (giản thể), Hindi, Nga. Mặc định là tiếng Anh (đổi bằng nút 🌐, có mục "Theo hệ thống"); ngày giờ, giờ 12/24, ngày đầu tuần tự đổi theo ngôn ngữ. Tên năm âm lịch theo từng nơi: "Bính Ngọ", "丙午年", "병오년", "Year of the Horse"…
- **Sao lưu & khôi phục** (menu ⋮): xuất toàn bộ ghi chú, phép tính đang giữ, cài đặt sáng/tối và ngôn ngữ thành một file `.zip` (lưu vào Tải xuống, Google Drive…). Sang máy khác chọn "Nhập từ file .zip", rồi chọn **Thêm vào ghi chú hiện có** (bỏ qua ghi chú trùng) hoặc **Thay thế toàn bộ**. Trong file .zip còn có bản `.txt` của từng ghi chú để mở trên máy tính. Nhập được cả file `.txt` rời hoặc file .zip tự nén chứa các file `.txt`.
- **Lối tắt**: giữ biểu tượng app trên màn hình chính → "Ghi chú mới".
- **Chế độ tối** tự theo cài đặt điện thoại.
- **Banner quảng cáo** luôn ở đáy màn hình, kể cả khi mở máy tính (máy tính bản gọn 5 cột × 4 hàng). Chỉ ẩn tạm khi bàn phím đang mở, không tạo lại quảng cáo mỗi lần.

## Chạy app trên điện thoại

1. Cài [Android Studio](https://developer.android.com/studio) (bản mới nhất).
2. Giải nén thư mục này, mở Android Studio → **Open** → chọn thư mục `GhiChuNhanh`. Đợi Android Studio tải Gradle và thư viện (lần đầu mất vài phút, cần mạng).
3. Bật **Tùy chọn nhà phát triển** + **Gỡ lỗi USB** trên điện thoại, cắm cáp vào máy tính (hoặc dùng máy ảo trong Android Studio).
4. Bấm nút **Run ▶**. App sẽ cài và mở trên điện thoại.

Tạo file APK để gửi cho người khác: **Build → Build App Bundle(s) / APK(s) → Build APK(s)**. File nằm ở `app/build/outputs/apk/debug/`.

> Mã nguồn chưa được biên dịch thử trên máy có Android SDK. Nếu Android Studio báo lỗi nhỏ (phiên bản thư viện, import), gửi nội dung lỗi để sửa.

## Cấu trúc mã nguồn

| File | Nội dung |
|---|---|
| `MainActivity.kt` | Điểm vào app, khởi tạo AdMob, lưu khi thoát |
| `NotesStore.kt` | Dữ liệu ghi chú + tự lưu xuống file JSON |
| `Calc.kt` | Bộ tính toán và trạng thái máy tính |
| `LunarCalendar.kt` | Đổi ngày dương → âm lịch Việt Nam |
| `ui/EditorScreen.kt` | Màn hình chính: đồng hồ, tab, ô ghi chú, lịch tháng |
| `ui/CalculatorPanel.kt` | Thanh máy tính thu gọn và bàn phím mở rộng |
| `ui/Theme.kt` | Màu sắc sáng/tối |
| `ads/BannerAd.kt` | Banner AdMob thích ứng |
| `AppSettings.kt` | Lưu ngôn ngữ và chế độ sáng/tối |
| `Backup.kt` | Ghi/đọc file .zip sao lưu |
| `ui/BackupDialog.kt` | Hộp thoại Sao lưu & khôi phục |
| `ui/Icons.kt` | Icon mặt trời, mặt trăng, quả địa cầu |
| `res/values*/strings.xml` | Chữ hiển thị theo 13 ngôn ngữ (tự sinh) |
| `tools/i18n/` | File bản dịch gốc + script sinh strings.xml |

## Sửa hoặc thêm bản dịch

Toàn bộ chữ trong app nằm ở `tools/i18n/translations.py`. Sửa ở đó rồi chạy:

```
python3 tools/i18n/gen_strings.py app/src/main/res
```

Script kiểm tra đủ chuỗi và đúng tham số (%1$d…) cho mọi ngôn ngữ trước khi ghi file. Thêm ngôn ngữ mới: thêm một mục vào `T` và `EXTRA`, rồi thêm tên ngôn ngữ vào `APP_LANGUAGES` trong `AppSettings.kt`. Bản dịch do AI soạn; nên nhờ người bản xứ đọc lại trước khi quảng bá ở thị trường đó.

## Trước khi đăng Play Store

1. **Đổi tên gói**: sửa `applicationId` trong `app/build.gradle.kts` (ví dụ `com.tenban.ghichu`) và `targetPackage` trong `res/xml/shortcuts.xml`.
2. **Quảng cáo thật**: tạo tài khoản [AdMob](https://admob.google.com), thêm app, tạo đơn vị "Banner". Thay ID thử nghiệm ở `AndroidManifest.xml` (APPLICATION_ID) và `ads/BannerAd.kt` (BANNER_AD_UNIT_ID). Không tự bấm vào quảng cáo của mình.
3. **Hỏi đồng ý quảng cáo** cho người dùng châu Âu bằng thư viện UMP của Google nếu phát hành ra quốc tế.
4. **Chính sách quyền riêng tư**: Play Store yêu cầu một trang chính sách (app có quảng cáo). Ghi chú chỉ lưu trong máy, không gửi đi đâu.
5. **Ký app và tạo file .aab**: Build → Generate Signed App Bundle. Giữ kỹ file keystore.
6. **Tài khoản cá nhân mới** phải chạy thử nghiệm kín với ít nhất 12 người trong 14 ngày liên tục trước khi được phát hành chính thức.

## Ý tưởng làm tiếp

- Gói "Gỡ quảng cáo" mua một lần (Google Play Billing).
- Widget ghi chú nhanh trên màn hình chính.
- Sao lưu / đồng bộ Google Drive.
- Font Be Vietnam Pro (giống bản thiết kế) bằng thư viện Google Fonts cho Compose.
