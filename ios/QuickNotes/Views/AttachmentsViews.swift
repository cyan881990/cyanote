import SwiftUI
import UIKit
import PhotosUI

/// Ảnh thu nhỏ (đọc ở luồng nền).
struct ThumbView: View {
    let url: URL
    var side: CGFloat
    @State private var image: UIImage?
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let p = Palette.of(colorScheme)
        ZStack {
            p.keyBg
            if let image { Image(uiImage: image).resizable().scaledToFill() }
        }
        .frame(width: side, height: side)
        .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
        .task(id: url) {
            let u = url
            let target = side * 3
            image = await Task.detached {
                guard let img = UIImage(contentsOfFile: u.path) else { return nil as UIImage? }
                return img.preparingThumbnail(of: CGSize(width: target, height: target * img.size.height / max(img.size.width, 1)))
            }.value
        }
    }
}

/// Nút phát ghi âm (phát / tạm dừng + thời lượng).
struct AudioChip: View {
    let url: URL
    @ObservedObject var player = AudioPlayback.shared
    @Environment(\.colorScheme) private var colorScheme
    @State private var duration: TimeInterval = 0
    @State private var pos: TimeInterval = 0

    var body: some View {
        let p = Palette.of(colorScheme)
        let playing = player.playing == url
        Button { player.toggle(url) } label: {
            HStack(spacing: 6) {
                Image(systemName: playing ? "pause.fill" : "play.fill").font(.system(size: 15, weight: .bold))
                Image(systemName: "mic.fill").font(.system(size: 12))
                Text(playing ? "\(formatDuration(pos)) / \(formatDuration(duration))" : formatDuration(duration))
                    .font(.system(size: 14, weight: .bold).monospacedDigit())
            }
            .foregroundColor(playing ? p.onAccent : p.accent)
            .padding(.horizontal, 12).frame(height: 56)
            .background(playing ? p.accent : p.accentSoft)
            .clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
        }
        .buttonStyle(.plain)
        .task(id: url) { duration = AttachmentsStore.duration(url) }
        .onReceive(Timer.publish(every: 0.25, on: .main, in: .common).autoconnect()) { _ in
            if playing { pos = player.position }
        }
    }
}

/// Dải ảnh & ghi âm ngay trên dòng trạng thái của ghi chú.
struct AttachmentStrip: View {
    let files: [URL]
    var onOpenImage: (URL) -> Void
    var onManage: () -> Void
    @Environment(\.colorScheme) private var colorScheme

    var body: some View {
        let p = Palette.of(colorScheme)
        if !files.isEmpty {
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(files, id: \.self) { f in
                        if AttachmentsStore.isImage(f) {
                            ThumbView(url: f, side: 56).onTapGesture { onOpenImage(f) }
                        } else {
                            AudioChip(url: f)
                        }
                    }
                    Button(action: onManage) {
                        Text("\(files.count)").font(.system(size: 15, weight: .bold)).foregroundColor(p.accent)
                            .frame(width: 56, height: 56).background(p.keyBg)
                            .clipShape(RoundedRectangle(cornerRadius: 12, style: .continuous))
                    }.buttonStyle(.plain)
                }
                .padding(.horizontal, 14).padding(.vertical, 6)
            }
        }
    }
}

/// Xem ảnh toàn màn hình (chia sẻ / xoá).
struct PhotoViewer: View {
    let url: URL
    var onDelete: () -> Void
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.dismiss) private var dismiss
    @State private var confirm = false

    var body: some View {
        let s = settings.strings
        ZStack(alignment: .topTrailing) {
            Color.black.ignoresSafeArea()
            if let img = UIImage(contentsOfFile: url.path) {
                Image(uiImage: img).resizable().scaledToFit().frame(maxWidth: .infinity, maxHeight: .infinity)
            }
            HStack(spacing: 10) {
                ShareLink(item: url) { circle("square.and.arrow.up") }
                Button { confirm = true } label: { circle("trash") }
                Button { dismiss() } label: { circle("xmark") }
            }
            .padding(14)
        }
        .confirmationDialog(s("photo_delete_q"), isPresented: $confirm, titleVisibility: .visible) {
            Button(s("action_delete"), role: .destructive) { onDelete(); dismiss() }
        }
    }

    private func circle(_ icon: String) -> some View {
        Image(systemName: icon).font(.system(size: 17, weight: .semibold)).foregroundColor(.white)
            .frame(width: 44, height: 44).background(Color.black.opacity(0.55)).clipShape(Circle())
    }
}

/// Quản lý ảnh & ghi âm của ghi chú.
struct AttachmentsSheet: View {
    let noteId: Int64
    let noteTitle: String
    var onTakePhoto: () -> Void
    var onRecord: () -> Void
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.colorScheme) private var colorScheme
    @Environment(\.dismiss) private var dismiss
    @ObservedObject var store = AttachmentsStore.shared
    @State private var pickerItems: [PhotosPickerItem] = []
    @State private var viewing: URL?
    @State private var deleteAudio: URL?

    var body: some View {
        let s = settings.strings
        let p = Palette.of(colorScheme)
        let _ = store.version
        let files = store.list(noteId)
        let images = files.filter(AttachmentsStore.isImage)
        let audios = files.filter(AttachmentsStore.isAudio)
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 12) {
                    HStack(spacing: 8) {
                        action("camera", s("photo_take"), p) { dismiss(); onTakePhoto() }
                        PhotosPicker(selection: $pickerItems, maxSelectionCount: 10, matching: .images) {
                            actionLabel("photo.on.rectangle", s("photo_pick"), p)
                        }
                        action("mic", s("rec_record"), p) { dismiss(); onRecord() }
                    }
                    if files.isEmpty {
                        Text(s("att_empty")).foregroundColor(p.muted).padding(.vertical, 12)
                    }
                    if !images.isEmpty {
                        Text(s("att_photos") + " · \(images.count)").font(.system(size: 13, weight: .bold)).foregroundColor(p.accent)
                        LazyVGrid(columns: [GridItem(.adaptive(minimum: 84), spacing: 6)], spacing: 6) {
                            ForEach(images, id: \.self) { f in
                                ThumbView(url: f, side: 84).onTapGesture { viewing = f }
                            }
                        }
                    }
                    if !audios.isEmpty {
                        Text(s("att_recordings") + " · \(audios.count)").font(.system(size: 13, weight: .bold)).foregroundColor(p.accent)
                        ForEach(audios, id: \.self) { f in
                            HStack {
                                AudioChip(url: f)
                                Spacer()
                                ShareLink(item: f) { Image(systemName: "square.and.arrow.up") }
                                Button { deleteAudio = f } label: { Image(systemName: "trash").foregroundColor(.red) }
                                    .padding(.leading, 12)
                            }
                        }
                    }
                }
                .padding(16)
            }
            .navigationTitle(s("att_manage"))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .confirmationAction) { Button("OK") { dismiss() } } }
        }
        .onChange(of: pickerItems) { items in
            Task {
                for item in items {
                    if let data = try? await item.loadTransferable(type: Data.self), let img = UIImage(data: data) {
                        await MainActor.run { _ = store.addImage(noteId, img) }
                    }
                }
                await MainActor.run { pickerItems = [] }
            }
        }
        .fullScreenCover(item: Binding(get: { viewing.map(IdentifiedURL.init) }, set: { viewing = $0?.url })) { item in
            PhotoViewer(url: item.url) { store.delete(item.url) }.environmentObject(settings)
        }
        .confirmationDialog(s("rec_delete_q"), isPresented: Binding(get: { deleteAudio != nil }, set: { if !$0 { deleteAudio = nil } }), titleVisibility: .visible) {
            Button(s("action_delete"), role: .destructive) {
                if let f = deleteAudio { AudioPlayback.shared.stop(); store.delete(f) }
                deleteAudio = nil
            }
        }
        .onDisappear { AudioPlayback.shared.stop() }
    }

    private func actionLabel(_ icon: String, _ label: String, _ p: Palette) -> some View {
        VStack(spacing: 4) {
            Image(systemName: icon).font(.system(size: 20))
            Text(label).font(.system(size: 12, weight: .semibold)).lineLimit(1).minimumScaleFactor(0.8)
        }
        .foregroundColor(p.accent).frame(maxWidth: .infinity).padding(.vertical, 10)
        .background(p.accentSoft).clipShape(RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func action(_ icon: String, _ label: String, _ p: Palette, run: @escaping () -> Void) -> some View {
        Button(action: run) { actionLabel(icon, label, p) }.buttonStyle(.plain)
    }
}

struct IdentifiedURL: Identifiable {
    let url: URL
    var id: String { url.path }
}

/// Máy ảnh (UIImagePickerController).
struct CameraPicker: UIViewControllerRepresentable {
    var onImage: (UIImage) -> Void
    @Environment(\.dismiss) private var dismiss

    func makeUIViewController(context: Context) -> UIImagePickerController {
        let c = UIImagePickerController()
        c.sourceType = UIImagePickerController.isSourceTypeAvailable(.camera) ? .camera : .photoLibrary
        c.delegate = context.coordinator
        return c
    }

    func updateUIViewController(_ c: UIImagePickerController, context: Context) {}
    func makeCoordinator() -> Coordinator { Coordinator(self) }

    final class Coordinator: NSObject, UIImagePickerControllerDelegate, UINavigationControllerDelegate {
        let parent: CameraPicker
        init(_ p: CameraPicker) { parent = p }
        func imagePickerController(_ picker: UIImagePickerController, didFinishPickingMediaWithInfo info: [UIImagePickerController.InfoKey: Any]) {
            if let img = info[.originalImage] as? UIImage { parent.onImage(img) }
            parent.dismiss()
        }
        func imagePickerControllerDidCancel(_ picker: UIImagePickerController) { parent.dismiss() }
    }
}

/// Màn hình ghi âm.
struct RecorderSheet: View {
    @StateObject var recorder: VoiceRecorder
    var onDone: () -> Void
    @EnvironmentObject private var settings: AppSettings
    @Environment(\.dismiss) private var dismiss
    @State private var failed = false

    var body: some View {
        let s = settings.strings
        VStack(spacing: 22) {
            Text(failed ? s("rec_mic_needed") : s("rec_recording")).font(.headline).multilineTextAlignment(.center)
            ZStack {
                Circle().fill(Color.red.opacity(0.15)).frame(width: 96, height: 96)
                Circle().fill(Color.red).frame(width: 64, height: 64)
                Image(systemName: "mic.fill").font(.system(size: 26)).foregroundColor(.white)
            }
            Text(formatDuration(recorder.elapsed)).font(.system(size: 30, weight: .bold).monospacedDigit())
            HStack(spacing: 14) {
                Button(s("action_cancel")) { recorder.stop(save: false); dismiss() }
                    .buttonStyle(.bordered)
                Button(s("rec_stop")) { recorder.stop(save: true); onDone(); dismiss() }
                    .buttonStyle(.borderedProminent).disabled(failed)
            }
        }
        .padding(28)
        .presentationDetents([.medium])
        .interactiveDismissDisabled()
        .onAppear { recorder.start { ok in failed = !ok } }
    }
}
