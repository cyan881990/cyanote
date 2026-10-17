import SwiftUI
import UIKit

private func timeText(_ d: Date, _ locale: Locale) -> String {
    let f = DateFormatter(); f.locale = locale; f.dateStyle = .none; f.timeStyle = .short
    return f.string(from: d)
}

private func dayText(_ d: Date, _ locale: Locale) -> String {
    let f = DateFormatter(); f.locale = locale; f.setLocalizedDateFormatFromTemplate("EEEdMMM")
    return f.string(from: d)
}

/// Thẻ chèn vào ghi chú, ví dụ "[⏰ 18/10 15:30]".
func appointmentTag(_ a: Appointment, _ locale: Locale) -> String {
    let f = DateFormatter(); f.locale = locale; f.setLocalizedDateFormatFromTemplate("dM")
    return "[⏰ \(f.string(from: a.date)) \(timeText(a.date, locale))]"
}

func dial(_ phone: String) {
    let digits = phone.filter { $0.isNumber || $0 == "+" }
    if let url = URL(string: "tel://\(digits)") { UIApplication.shared.open(url) }
}

/// Tạo / sửa lịch hẹn.
struct AppointmentEditorSheet: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.dismiss) private var dismiss
    let initial: Appointment
    let isNew: Bool
    let noteTitle: String?
    var onSave: (Appointment, Bool) -> Void
    var onDelete: (() -> Void)?

    @State private var title = ""
    @State private var phone = ""
    @State private var date = Date()
    @State private var remind = 0
    @State private var sound = true
    @State private var linkNote = false
    @State private var insertTag = false

    var body: some View {
        let s = settings.strings
        NavigationStack {
            Form {
                Section {
                    TextField(s("appt_field_title"), text: $title, axis: .vertical).lineLimit(1...3)
                    TextField(s("appt_field_phone"), text: $phone).keyboardType(.phonePad)
                }
                Section {
                    DatePicker(s("appt_date"), selection: $date, displayedComponents: [.date, .hourAndMinute])
                        .environment(\.locale, s.locale)
                    Picker(s("appt_remind"), selection: $remind) {
                        Text(s("appt_remind_at_time")).tag(0)
                        ForEach([5, 15, 30], id: \.self) { m in Text(s.f("appt_minutes_short", m)).tag(m) }
                        Text(s("appt_hour_short")).tag(60)
                    }
                    Toggle(s("appt_sound"), isOn: $sound)
                }
                if let noteTitle {
                    Section {
                        Toggle(s("appt_link_note") + " · " + noteTitle, isOn: $linkNote)
                        if isNew { Toggle(s("appt_insert_tag"), isOn: $insertTag) }
                    }
                }
                if let onDelete {
                    Section { Button(s("action_delete"), role: .destructive) { onDelete(); dismiss() } }
                }
            }
            .navigationTitle(s(isNew ? "appt_new" : "appt_edit"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button(s("action_cancel")) { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button(s("action_save")) {
                        var a = initial
                        a.title = title.trimmingCharacters(in: .whitespacesAndNewlines)
                        a.phone = phone.trimmingCharacters(in: .whitespaces)
                        a.time = Int64(date.timeIntervalSince1970 * 1000)
                        a.remindBefore = remind
                        a.sound = sound
                        if !linkNote { a.noteId = 0 }
                        if date > Date() { a.done = false }
                        onSave(a, insertTag && linkNote)
                        dismiss()
                    }.bold()
                }
            }
        }
        .onAppear {
            title = initial.title; phone = initial.phone; date = initial.date
            remind = initial.remindBefore; sound = initial.sound
            linkNote = initial.noteId != 0
            insertTag = isNew && noteTitle != nil
        }
    }
}

/// Danh sách lịch hẹn: Hôm nay · Ngày mai · Sắp tới · Đã qua & đã xong.
struct AppointmentsPanel: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var store = AppointmentsStore.shared
    var noteTitleOf: (Int64) -> String?
    var onNew: () -> Void
    var onEdit: (Appointment) -> Void
    var onOpenNote: (Int64) -> Void
    var onCollapse: (() -> Void)?
    var header: AnyView

    private enum Group: Int, CaseIterable { case today, tomorrow, later, past }

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        let cal = Calendar.current
        let now = Date()
        func group(_ a: Appointment) -> Group {
            if a.done || a.date < now.addingTimeInterval(-60) { return .past }
            if cal.isDateInToday(a.date) { return .today }
            if cal.isDateInTomorrow(a.date) { return .tomorrow }
            return .later
        }
        let grouped = Dictionary(grouping: store.items, by: group)
        return VStack(spacing: 4) {
            HStack(spacing: 6) {
                header
                Spacer(minLength: 0)
                Button(action: onNew) {
                    Image(systemName: "plus").font(.system(size: 16, weight: .bold))
                        .foregroundColor(p.onAccent).frame(width: 38, height: 34)
                        .background(p.accent).clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }.buttonStyle(.plain).accessibilityLabel(s("appt_new"))
                if let onCollapse {
                    Button(action: onCollapse) {
                        Image(systemName: "chevron.down").font(.system(size: 14, weight: .semibold))
                            .foregroundColor(p.ink).frame(width: 34, height: 34)
                            .background(p.keyBg).clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }.buttonStyle(.plain)
                }
            }
            if store.items.isEmpty {
                Text(s("appt_empty")).font(.system(size: 14)).foregroundColor(p.muted)
                    .frame(maxWidth: .infinity, alignment: .leading).padding(.vertical, 16).padding(.horizontal, 8)
                Spacer(minLength: 0)
            } else {
                ScrollView {
                    LazyVStack(alignment: .leading, spacing: 2) {
                        ForEach(Group.allCases, id: \.self) { g in
                            if let list = grouped[g] {
                                Text(s(["appt_today", "appt_tomorrow", "appt_upcoming", "appt_past"][g.rawValue]))
                                    .font(.system(size: 12, weight: .bold)).foregroundColor(p.accent)
                                    .padding(.leading, 8).padding(.top, 8)
                                ForEach(list) { a in
                                    row(a, showDay: g == .later || g == .past, s: s, p: p)
                                }
                            }
                        }
                    }
                }
            }
        }
        .padding(8)
        .background(p.surface)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay(RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(p.line, lineWidth: 1))
    }

    private func row(_ a: Appointment, showDay: Bool, s: Strings, p: Palette) -> some View {
        HStack(spacing: 10) {
            VStack(spacing: 1) {
                Text(timeText(a.date, s.locale)).font(.system(size: 15, weight: .bold)).foregroundColor(a.done ? p.muted : p.accent)
                if showDay { Text(dayText(a.date, s.locale)).font(.system(size: 10)).foregroundColor(p.muted) }
            }
            .lineLimit(1).frame(width: 64).padding(.vertical, 6)
            .background(a.done ? p.keyBg : p.accentSoft)
            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            VStack(alignment: .leading, spacing: 2) {
                Text(a.title.isEmpty ? s("appt_untitled") : a.title)
                    .font(.system(size: 15, weight: .semibold)).foregroundColor(a.done ? p.muted : p.ink)
                    .strikethrough(a.done).lineLimit(2)
                let sub = [a.phone.isEmpty ? nil : a.phone,
                           a.remindBefore > 0 ? "🔔 −\(a.remindBefore)′" : nil,
                           a.sound ? nil : "🔕"].compactMap { $0 }.joined(separator: " · ")
                if !sub.isEmpty { Text(sub).font(.system(size: 12)).foregroundColor(p.muted).lineLimit(1) }
                if a.noteId != 0, let t = noteTitleOf(a.noteId) {
                    Button { onOpenNote(a.noteId) } label: {
                        Text("📝 " + t).font(.system(size: 12)).foregroundColor(p.accent).lineLimit(1)
                    }.buttonStyle(.plain)
                }
            }
            Spacer(minLength: 0)
            if !a.phone.isEmpty {
                Button { dial(a.phone) } label: {
                    Image(systemName: "phone.fill").font(.system(size: 14)).foregroundColor(p.onAccent)
                        .frame(width: 36, height: 36).background(p.accent).clipShape(Circle())
                }.buttonStyle(.plain).accessibilityLabel(s("appt_call"))
            }
            Button { store.setDone(a.id, !a.done) } label: {
                Image(systemName: a.done ? "checkmark.circle.fill" : "circle").font(.system(size: 22)).foregroundColor(p.accent)
            }.buttonStyle(.plain)
        }
        .padding(.horizontal, 6).padding(.vertical, 5)
        .contentShape(Rectangle())
        .onTapGesture { onEdit(a) }
    }
}

/// Thanh thu gọn chế độ Lịch hẹn: lịch hẹn kế tiếp.
struct AppointmentBar: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var store = AppointmentsStore.shared
    var compact = false
    var hideIcon = false
    var onExpand: () -> Void

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        let now = Date()
        let upcoming = store.items.filter { !$0.done && $0.date >= now.addingTimeInterval(-60) }.sorted { $0.time < $1.time }
        return Button(action: onExpand) {
            HStack(spacing: 12) {
                if !hideIcon {
                    Image(systemName: "alarm").font(.system(size: 16, weight: .semibold)).foregroundColor(p.accent)
                        .frame(width: compact ? 32 : 40, height: compact ? 32 : 40)
                        .background(p.accentSoft).clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }
                VStack(alignment: .leading, spacing: 1) {
                    if let next = upcoming.first {
                        if !compact {
                            Text(dayText(next.date, s.locale) + (upcoming.count > 1 ? " · " + s.f("appt_more", upcoming.count - 1) : ""))
                                .font(.system(size: 12)).foregroundColor(p.muted).lineLimit(1)
                        }
                        Text(next.title.isEmpty ? s("appt_untitled") : next.title)
                            .font(.system(size: 15, weight: .semibold)).foregroundColor(p.ink).lineLimit(1)
                    } else {
                        Text(s("appt_next_none")).font(.system(size: 14)).foregroundColor(p.muted).lineLimit(1)
                    }
                }
                Spacer(minLength: 4)
                if let next = upcoming.first {
                    Text(timeText(next.date, s.locale)).font(.system(size: 17, weight: .bold).monospacedDigit()).foregroundColor(p.accent)
                }
                Image(systemName: "chevron.up").font(.system(size: 14, weight: .semibold)).foregroundColor(p.muted)
            }
            .padding(.leading, 10).padding(.trailing, 14)
            .frame(height: compact ? 48 : 60)
            .background(p.surface)
            .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
            .overlay(RoundedRectangle(cornerRadius: 18, style: .continuous).stroke(p.line, lineWidth: 1))
        }
        .buttonStyle(.plain)
    }
}

/// Chuyển Máy tính ⇄ Lịch hẹn (+ ẩn bảng).
struct DockSwitcher: View {
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @ObservedObject var prefs = EditorPrefs.shared
    var showLabels: Bool
    var height: CGFloat = 36
    var showHide = true

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        HStack(spacing: 6) {
            HStack(spacing: 3) {
                seg("plus.forwardslash.minus", s("dock_calc"), prefs.dockMode == "CALC", p) { prefs.dockMode = "CALC" }
                seg("alarm", s("dock_appts"), prefs.dockMode == "APPOINTMENTS", p) { prefs.dockMode = "APPOINTMENTS" }
            }
            .padding(3).background(p.keyBg).clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
            if showHide {
                Button { prefs.dockVisible = false } label: {
                    Image(systemName: "rectangle.bottomhalf.inset.filled").font(.system(size: 15))
                        .foregroundColor(p.muted).frame(width: height, height: height)
                        .background(p.keyBg).clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                }.buttonStyle(.plain).accessibilityLabel(s("dock_hide"))
            }
        }
    }

    private func seg(_ icon: String, _ label: String, _ selected: Bool, _ p: Palette, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 5) {
                Image(systemName: icon).font(.system(size: 14, weight: .semibold))
                if showLabels { Text(label).font(.system(size: 13, weight: .semibold)).lineLimit(1) }
            }
            .foregroundColor(selected ? p.accent : p.muted)
            .padding(.horizontal, 10)
            .frame(height: height - 6)
            .background(selected ? p.surface : Color.clear)
            .clipShape(RoundedRectangle(cornerRadius: 10, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }
}
