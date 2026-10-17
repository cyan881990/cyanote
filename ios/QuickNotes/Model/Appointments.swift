import Foundation
import UserNotifications
import UIKit

/// Một lịch hẹn / cuộc gọi cần nhớ (cùng định dạng JSON với bản Android).
struct Appointment: Codable, Identifiable, Equatable {
    var id: Int64
    var title: String
    var time: Int64                // mili giây
    var phone: String = ""
    var noteId: Int64 = 0
    var remindBefore: Int = 0      // phút
    var sound: Bool = true
    var done: Bool = false

    var date: Date { Date(timeIntervalSince1970: TimeInterval(time) / 1000) }
    var fireDate: Date { date.addingTimeInterval(TimeInterval(-remindBefore * 60)) }

    /// Số điện thoại đầu tiên trong dòng chữ (9–15 chữ số).
    static func findPhone(_ text: String) -> String {
        guard let r = text.range(of: #"\+?\d[\d .\-]{7,18}\d"#, options: .regularExpression) else { return "" }
        let digits = text[r].filter { $0.isNumber || $0 == "+" }
        let n = digits.filter(\.isNumber).count
        return (9...15).contains(n) ? String(digits) : ""
    }
}

/// Lưu lịch hẹn vào Documents/appointments.json và đặt thông báo cục bộ.
final class AppointmentsStore: ObservableObject {
    static let shared = AppointmentsStore()
    @Published private(set) var items: [Appointment] = []
    private let url = FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0]
        .appendingPathComponent("appointments.json")

    private init() {
        if let data = try? Data(contentsOf: url), let list = try? JSONDecoder().decode([Appointment].self, from: data) {
            items = list
        }
        sort()
    }

    private func sort() {
        items.sort { a, b in
            if a.done != b.done { return !a.done }
            return a.done ? a.time > b.time : a.time < b.time
        }
    }

    private func save() {
        if let data = try? JSONEncoder().encode(items) { try? data.write(to: url, options: .atomic) }
    }

    func get(_ id: Int64) -> Appointment? { items.first { $0.id == id } }

    func upsert(_ a: Appointment) {
        if let i = items.firstIndex(where: { $0.id == a.id }) { items[i] = a } else { items.append(a) }
        sort(); save()
        Reminders.schedule(a)
    }

    func delete(_ id: Int64) {
        items.removeAll { $0.id == id }
        save()
        Reminders.cancel(id)
    }

    func setDone(_ id: Int64, _ done: Bool) {
        guard var a = get(id) else { return }
        a.done = done
        upsert(a)
    }

    func snooze(_ id: Int64, minutes: Int) {
        guard var a = get(id) else { return }
        a.time = Int64((Date().timeIntervalSince1970 + Double(minutes * 60)) * 1000)
        a.remindBefore = 0
        a.done = false
        upsert(a)
    }

    func jsonArray() -> Any {
        (try? JSONSerialization.jsonObject(with: JSONEncoder().encode(items))) ?? []
    }

    /// Nhập từ sao lưu; [idMap] đổi id ghi chú cũ → mới.
    func importFrom(_ data: Data?, idMap: [Int64: Int64], replace: Bool) {
        guard let data, let list = try? JSONDecoder().decode([Appointment].self, from: data) else { return }
        if replace { items.forEach { Reminders.cancel($0.id) }; items.removeAll() }
        var nextId = max(Int64(Date().timeIntervalSince1970 * 1000), (items.map(\.id).max() ?? 0) + 1)
        for var a in list {
            if !replace && items.contains(where: { $0.title == a.title && $0.time == a.time }) { continue }
            a.id = nextId; nextId += 1
            a.noteId = idMap[a.noteId] ?? 0
            items.append(a)
        }
        sort(); save()
        Reminders.rescheduleAll()
    }
}

/// Thông báo cục bộ: đúng giờ hẹn (hoặc nhắc trước), có/không chuông, nút Gọi · Báo lại · Xong.
enum Reminders {
    static let category = "APPOINTMENT"
    static let categoryNoCall = "APPOINTMENT_NOCALL"
    static let snoozeMinutes = 10

    static func registerCategories(_ s: Strings) {
        let call = UNNotificationAction(identifier: "CALL", title: s("appt_call"), options: [.foreground])
        let snooze = UNNotificationAction(identifier: "SNOOZE", title: s.f("appt_snooze", snoozeMinutes), options: [])
        let done = UNNotificationAction(identifier: "DONE", title: s("appt_done"), options: [])
        UNUserNotificationCenter.current().setNotificationCategories([
            UNNotificationCategory(identifier: category, actions: [call, snooze, done], intentIdentifiers: []),
            UNNotificationCategory(identifier: categoryNoCall, actions: [snooze, done], intentIdentifiers: []),
        ])
    }

    static func requestPermission() {
        UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .sound, .badge]) { _, _ in }
    }

    static func schedule(_ a: Appointment) {
        let center = UNUserNotificationCenter.current()
        center.removePendingNotificationRequests(withIdentifiers: ["appt-\(a.id)"])
        guard !a.done, a.date > Date().addingTimeInterval(-60) else { return }
        let s = Strings.current
        let content = UNMutableNotificationContent()
        content.title = "⏰ " + (a.title.isEmpty ? s("appt_untitled") : a.title)
        let tf = DateFormatter(); tf.locale = s.locale; tf.timeStyle = .short; tf.dateStyle = .none
        var parts = [tf.string(from: a.date)]
        if a.remindBefore > 0 { parts.append(s.f("appt_in_minutes", a.remindBefore)) }
        if !a.phone.isEmpty { parts.append(a.phone) }
        content.body = parts.joined(separator: " · ")
        content.sound = a.sound ? .default : nil
        content.categoryIdentifier = a.phone.isEmpty ? categoryNoCall : category
        content.userInfo = ["id": NSNumber(value: a.id), "phone": a.phone, "noteId": NSNumber(value: a.noteId)]
        if #available(iOS 15.0, *) { content.interruptionLevel = .timeSensitive }
        let fire = max(a.fireDate, Date().addingTimeInterval(2))
        let comps = Calendar.current.dateComponents([.year, .month, .day, .hour, .minute, .second], from: fire)
        let trigger = UNCalendarNotificationTrigger(dateMatching: comps, repeats: false)
        center.add(UNNotificationRequest(identifier: "appt-\(a.id)", content: content, trigger: trigger))
    }

    static func cancel(_ id: Int64) {
        UNUserNotificationCenter.current().removePendingNotificationRequests(withIdentifiers: ["appt-\(id)"])
        UNUserNotificationCenter.current().removeDeliveredNotifications(withIdentifiers: ["appt-\(id)"])
    }

    static func rescheduleAll() {
        AppointmentsStore.shared.items.forEach(schedule)
    }
}

/// Nhận nút bấm trên thông báo & hiện thông báo cả khi đang mở app.
final class NotificationHandler: NSObject, UNUserNotificationCenterDelegate {
    static let shared = NotificationHandler()
    /// Ghi chú cần mở khi người dùng bấm vào thông báo.
    @Published var openNoteId: Int64 = 0

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification,
                                withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        completionHandler([.banner, .sound, .list])
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse,
                                withCompletionHandler completionHandler: @escaping () -> Void) {
        let info = response.notification.request.content.userInfo
        let id = (info["id"] as? NSNumber)?.int64Value ?? 0
        let phone = info["phone"] as? String ?? ""
        let noteId = (info["noteId"] as? NSNumber)?.int64Value ?? 0
        DispatchQueue.main.async {
            switch response.actionIdentifier {
            case "CALL":
                if let url = URL(string: "tel://" + phone.filter { $0.isNumber || $0 == "+" }) { UIApplication.shared.open(url) }
            case "SNOOZE":
                AppointmentsStore.shared.snooze(id, minutes: Reminders.snoozeMinutes)
            case "DONE":
                AppointmentsStore.shared.setDone(id, true)
            default:
                if noteId != 0 { NotificationCenter.default.post(name: .openNote, object: NSNumber(value: noteId)) }
            }
            completionHandler()
        }
    }
}

extension Notification.Name {
    static let openNote = Notification.Name("cyanote.openNote")
}
