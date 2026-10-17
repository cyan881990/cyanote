# Nội dung trang hướng dẫn (guide.html). <k>…</k> = phím/nút (hiển thị dạng kbd).
KEYS = """title lead dl install
n1t n1d n2t n2d n3t n3d n4t n4d n5t n5d n6t n6d
a_h a_p a_1 a_2 a_3
t_h t_p t_1 t_2 t_3
o_h o_p o_1 o_2 o_3 o_4
m_h m_p m_1 m_2 m_3 m_4
d_h d_p d_1 d_2
l_h l_p l_1 l_2
w_h w_p
i_h i_1 i_2 i_3 i_4
f_h f_q1 f_a1 f_q2 f_a2 f_q3 f_a3 privacy""".split()

EN = dict(
title="cyanote<span>++</span> now remembers your appointments",
lead="Appointments &amp; call reminders with sound, tagged right into your notes. Snap a photo or record a voice note in one tap. Notepad++-style line numbers and a word-wrap switch.",
dl="⬇ Download for Android (APK)", install="How to install",
n1t="Appointments &amp; calls", n1d="Today · Tomorrow · Upcoming, tap to call.",
n2t="Quick tag in a note", n2d="Uses the current line and detects the phone number.",
n3t="Reminders with sound", n3d="Call · Snooze 10 min · Done right on the notification.",
n4t="Photos &amp; voice notes", n4d="Snap, pick, record and manage them per note.",
n5t="Calculator ⇄ Appointments", n5d="Switch, or hide the panel for more note space.",
n6t="Line numbers · Word wrap", n6d="Like Notepad++, word wrap on by default.",
a_h="⏰ Appointments &amp; calls", a_p="Every meeting and call-back lives right under your note, grouped into Today, Tomorrow, Upcoming and Done.",
a_1="Tap <k>⏰ Appointments</k> in the bottom panel (or <k>⋮</k> → Appointments &amp; calls).",
a_2="Tap <k>+</k> and enter what it's about, an optional phone number, the date &amp; time and when to remind you: on time, 5, 15, 30 min or 1 hour before.",
a_3="Tap the green <k>📞</k> to open the dialer with the number filled in. Tick ☑ when it's done.",
t_h="🏷️ Tag an appointment from your note", t_p="Writing “Call Hoa about the birthday cake 0987 654 321”? Put the cursor on that line and tap <k>⏰</k> under the note.",
t_1="The line becomes the title and the <b>phone number is detected automatically</b>.",
t_2="Pick a time and tap Save. A tag like <k>[⏰ 18/10 15:30]</k> is inserted into the note.",
t_3="The note's appointments appear as chips above the status bar. Tap one to edit it.",
o_h="🔔 Reminders with sound, call from the notification", o_p="At the right time your phone rings and vibrates. Choose sound or silent for each appointment. The notification has three buttons:",
o_1="<b>Call</b> opens the dialer with the saved number.", o_2="<b>Snooze 10 min</b> reminds you again in 10 minutes.",
o_3="<b>Done</b> marks it complete. Tapping the notification opens the linked note.", o_4="Reminders are restored automatically after a restart or a time change.",
m_h="📷 🎤 Photos &amp; voice notes in a note", m_p="Snap a receipt or a whiteboard, or record a quick idea, straight into the note you're writing.",
m_1="Under the note tap <k>📷</k> → Take photo or Choose photo. Tap <k>🎤</k> to record, then Stop &amp; save.",
m_2="Photos and recordings appear as a strip inside the note. Tap a photo to view it full screen, tap a recording to play it.",
m_3="<k>⋮</k> → <b>Photos &amp; recordings</b> shows them all, to share or delete.", m_4="Photos are downsized to save space, and everything is included in your .zip backup.",
d_h="🧮 Calculator ⇄ Appointments, or hide it", d_p="The panel under your note switches between the calculator and your appointments. Your calculation in progress is kept.",
d_1="Tap <k>🧮</k> or <k>⏰</k> on the left of the panel to switch.", d_2="Tap <k>Hide panel</k> to give the note the whole screen. Tap it again to bring the panel back.",
l_h="#️⃣ Line numbers &amp; word wrap, like Notepad++", l_p="The gutter numbers real lines: a long wrapped line still gets a single number.",
l_1="Tap the <k>1 2 3</k> icon under the note to toggle line numbers.", l_2="Tap <k>↩</k> to toggle word wrap. When it's off, long lines scroll sideways. Default: on.",
w_h="📱 Tablets, landscape &amp; dark mode", w_p="Wide screens split into two columns: the note on the left, the calculator or appointments on the right. While you type, the clock and lunar date shrink to one line to give you more room.",
i_h="Install &amp; update",
i_1="<b>Android</b>: download <a href=\"download/cyanote-android.apk\">cyanote-android.apk</a>, open it and allow “Install unknown apps”. After that the app tells you when a new version is out (the ⬇ Update label at the top, or <k>⋮</k> → Check for updates).",
i_2="When you create your first appointment, allow <b>Notifications</b>. On Android 12+, turn on <b>Alarms &amp; reminders</b> so reminders arrive on the exact minute (the app links to the setting).",
i_3="Strong battery saver (Xiaomi, Oppo, Samsung…)? Set cyanote++ to “Unrestricted” so reminders aren't delayed.",
i_4="<b>iOS</b>: the beta runs through TestFlight; the App Store version updates itself.",
f_h="FAQ", f_q1="Does the app read my call log or contacts?", f_a1="No. “Call” only opens the dialer with the number filled in. The app never places calls itself and doesn't read your call history or contacts.",
f_q2="Where are photos and recordings stored?", f_a2="Only in the app's private storage on your device. When you export a .zip backup they're inside its attachments/ folder.",
f_q3="Reminders arrive a few minutes late?", f_a3="Turn on “Alarms &amp; reminders” for cyanote++ in Settings and exclude the app from battery optimization.",
privacy="Privacy Policy",
)

VI = dict(
title="cyanote<span>++</span> nay nhớ lịch hẹn giúp bạn",
lead="Lịch hẹn &amp; cuộc gọi có chuông, gắn thẳng vào ghi chú. Chụp ảnh, ghi âm dán vào ghi chú chỉ một chạm. Số dòng như Notepad++, bật/tắt tự xuống dòng.",
dl="⬇ Tải bản Android (APK)", install="Hướng dẫn cài đặt",
n1t="Lịch hẹn &amp; cuộc gọi", n1d="Hôm nay · Ngày mai · Sắp tới, bấm là gọi.",
n2t="Gắn nhanh vào ghi chú", n2d="Lấy dòng đang gõ, tự nhận số điện thoại.",
n3t="Thông báo có chuông", n3d="Gọi · Báo lại 10 phút · Xong ngay trên thông báo.",
n4t="Ảnh &amp; ghi âm", n4d="Chụp, chọn ảnh, ghi âm và quản lý theo từng ghi chú.",
n5t="Máy tính ⇄ Lịch hẹn", n5d="Đổi qua lại hoặc ẩn bảng để rộng chỗ ghi chú.",
n6t="Số dòng · Tự xuống dòng", n6d="Giống Notepad++, mặc định tự xuống dòng.",
a_h="⏰ Lịch hẹn &amp; cuộc gọi", a_p="Mọi cuộc hẹn, cuộc gọi cần gọi lại nằm ngay dưới ghi chú, chia theo Hôm nay, Ngày mai, Sắp tới và Đã xong.",
a_1="Bấm <k>⏰ Lịch hẹn</k> ở bảng dưới (hoặc menu <k>⋮</k> → Lịch hẹn &amp; cuộc gọi).",
a_2="Bấm <k>+</k>, nhập nội dung, số điện thoại (không bắt buộc), ngày giờ và thời gian nhắc trước: đúng giờ, 5, 15, 30 phút hoặc 1 giờ.",
a_3="Bấm nút <k>📞</k> xanh để mở trình gọi điện có sẵn số. Đánh dấu ☑ khi đã xong.",
t_h="🏷️ Gắn nhanh lịch hẹn vào ghi chú", t_p="Đang ghi “Gọi chị Hoa đặt bánh sinh nhật 0987 654 321”? Đặt con trỏ ở dòng đó và bấm <k>⏰</k> dưới ghi chú.",
t_1="App lấy dòng đó làm nội dung và <b>tự nhận số điện thoại</b>.",
t_2="Chọn giờ rồi bấm Lưu. Thẻ <k>[⏰ 18/10 15:30]</k> được chèn vào ghi chú.",
t_3="Lịch hẹn của ghi chú hiện thành các chip ngay trên dòng trạng thái. Bấm vào chip để sửa.",
o_h="🔔 Thông báo có chuông, gọi ngay từ thông báo", o_p="Đến giờ, điện thoại đổ chuông và rung. Bạn chọn có chuông hay im lặng cho từng lịch hẹn. Ngay trên thông báo có ba nút:",
o_1="<b>Gọi</b>: mở trình gọi điện với số đã lưu.", o_2="<b>Báo lại 10 phút</b>: nhắc lại sau 10 phút.",
o_3="<b>Xong</b>: đánh dấu hoàn thành. Bấm vào thông báo sẽ mở đúng ghi chú.", o_4="Tắt máy mở lại hay đổi giờ, các lời nhắc vẫn được đặt lại tự động.",
m_h="📷 🎤 Ảnh &amp; ghi âm trong ghi chú", m_p="Chụp hoá đơn, bảng trắng, hay ghi âm nhanh một ý tưởng, rồi dán thẳng vào ghi chú đang mở.",
m_1="Dưới ghi chú bấm <k>📷</k> → Chụp ảnh hoặc Chọn ảnh. Bấm <k>🎤</k> để ghi âm, rồi Dừng &amp; lưu.",
m_2="Ảnh và bản ghi âm hiện thành dải ngay trong ghi chú. Bấm ảnh để xem toàn màn hình, bấm ghi âm để nghe.",
m_3="Menu <k>⋮</k> → <b>Ảnh &amp; ghi âm</b> để xem tất cả, chia sẻ hoặc xoá.", m_4="Ảnh được thu nhỏ để nhẹ máy. Mọi thứ đều nằm trong file sao lưu .zip.",
d_h="🧮 Máy tính ⇄ Lịch hẹn, hoặc ẩn hẳn", d_p="Bảng dưới ghi chú đổi qua lại giữa máy tính và lịch hẹn. Phép tính đang dở vẫn được giữ.",
d_1="Bấm <k>🧮</k> hoặc <k>⏰</k> bên trái bảng để đổi.", d_2="Bấm <k>Ẩn bảng</k> để ghi chú chiếm trọn màn hình. Bấm lại để hiện.",
l_h="#️⃣ Số dòng &amp; tự xuống dòng như Notepad++", l_p="Cột số dòng đánh số theo dòng thật: một dòng dài tự xuống vẫn chỉ một số.",
l_1="Bấm biểu tượng <k>1 2 3</k> dưới ghi chú để bật/tắt số dòng.", l_2="Bấm <k>↩</k> để bật/tắt tự xuống dòng. Khi tắt, dòng dài cuộn ngang. Mặc định: bật.",
w_h="📱 Máy tính bảng, xoay ngang &amp; giao diện tối", w_p="Màn hình rộng tự chia 2 cột: ghi chú bên trái, máy tính hoặc lịch hẹn bên phải. Khi gõ, đồng hồ và âm lịch thu gọn một dòng để vùng ghi chú rộng hơn.",
i_h="Cài đặt &amp; cập nhật",
i_1="<b>Android</b>: tải <a href=\"download/cyanote-android.apk\">cyanote-android.apk</a>, mở file và cho phép “Cài ứng dụng không rõ nguồn gốc”. Các bản sau app tự báo có bản mới (nhãn ⬇ Cập nhật ở đầu màn hình, hoặc <k>⋮</k> → Kiểm tra cập nhật).",
i_2="Lần đầu tạo lịch hẹn, cho phép <b>Thông báo</b>. Android 12 trở lên: bật <b>Báo thức và lời nhắc</b> để nhắc đúng phút (app có nút mở cài đặt).",
i_3="Máy tiết kiệm pin mạnh (Xiaomi, Oppo, Samsung…)? Đặt pin của cyanote++ là “Không giới hạn” để không bị trễ nhắc.",
i_4="<b>iOS</b>: bản thử nghiệm chạy qua TestFlight; bản App Store tự cập nhật.",
f_h="Hỏi đáp", f_q1="App có đọc nhật ký cuộc gọi hay danh bạ không?", f_a1="Không. Nút “Gọi” chỉ mở trình gọi điện có sẵn số. App không tự gọi, không đọc lịch sử cuộc gọi hay danh bạ.",
f_q2="Ảnh và ghi âm lưu ở đâu?", f_a2="Chỉ trong bộ nhớ riêng của app trên máy bạn. Khi xuất sao lưu .zip, chúng nằm trong thư mục attachments/ của file.",
f_q3="Nhắc bị trễ vài phút?", f_a3="Bật “Báo thức và lời nhắc” cho cyanote++ trong Cài đặt và tắt tối ưu pin cho app.",
privacy="Chính sách bảo mật",
)
