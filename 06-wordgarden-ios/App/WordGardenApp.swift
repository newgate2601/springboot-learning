import SwiftUI
import WidgetKit
import WordGardenCore

@MainActor @main struct WordGardenApp: App {
    @StateObject private var model = StudyModel()
    @Environment(\.scenePhase) private var scenePhase
    var body: some Scene {
        WindowGroup {
            GardenHome(model: model)
                .onOpenURL { url in
                    guard url.scheme == "wordgarden", url.host == "word", let id = Int(url.lastPathComponent) else { return }
                    model.select(id: id)
                }
                .onChange(of: scenePhase) { _, phase in
                    if phase == .active { model.refresh() }
                }
        }
    }
}

@MainActor final class StudyModel: ObservableObject {
    @Published var selected: Word?
    @Published var query = ""
    @Published var error: String?
    let words: [Word]
    private var deck: Deck
    private let defaults = UserDefaults.standard
    init() {
        let loaded = try? Library.catalog()
        words = loaded?.words ?? []
        let now = Date()
        if let data = UserDefaults.standard.data(forKey: "wordgarden.app.deck.v1"), let saved = try? JSONDecoder().decode(Deck.self, from: data) { deck = saved }
        else { deck = Deck(order: words.map(\.id).shuffled(), now: now) }
        deck.reconcile(ids: words.map(\.id), now: now)
        selected = words.first { $0.id == deck.currentID(at: now) }
        if loaded == nil { error = "Không đọc được dữ liệu từ vựng. Hãy kiểm tra lại bản build." }
    }
    var results: [Word] {
        guard !query.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return [] }
        return Array(words.filter {
            $0.word.range(of: query, options: [.caseInsensitive, .diacriticInsensitive]) != nil ||
            $0.meaning.range(of: query, options: [.caseInsensitive, .diacriticInsensitive]) != nil
        }.prefix(60))
    }
    func select(id: Int) { NativeAudio.shared.stop(); selected = words.first { $0.id == id } }
    func refresh() { if selected == nil { selected = words.first { $0.id == deck.currentID(at: Date()) } } }
    func next() {
        NativeAudio.shared.stop()
        let now = Date()
        if let id = deck.currentID(at: now) { deck.advance(expectedID: id, now: now) }
        selected = words.first { $0.id == deck.currentID(at: now) }
        if let data = try? JSONEncoder().encode(deck) { defaults.set(data, forKey: "wordgarden.app.deck.v1") }
    }
}

@MainActor struct GardenHome: View {
    @ObservedObject var model: StudyModel
    @ObservedObject private var audio = NativeAudio.shared
    @State private var showHelp = false
    private let green = Color(red: 0.15, green: 0.37, blue: 0.25)
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 22) {
                    Text("A LITTLE ENGLISH, EVERY DAY").font(.system(size: 10, weight: .bold)).tracking(2).foregroundStyle(.secondary)
                    if !model.query.isEmpty {
                        ForEach(model.results) { word in
                            Button { model.select(id: word.id); model.query = "" } label: {
                                HStack { VStack(alignment: .leading) { Text(word.word).font(.headline); Text(word.meaning).font(.caption).foregroundStyle(.secondary) }; Spacer(); Image(systemName: "chevron.right") }
                            }.buttonStyle(.plain).padding(.vertical, 5)
                        }
                        if model.results.isEmpty { Text("Không tìm thấy từ phù hợp.").foregroundStyle(.secondary) }
                    }
                    if let word = model.selected {
                        VStack(alignment: .leading, spacing: 13) {
                            Text(word.image).font(.system(size: 50))
                            Text(word.word).font(.system(size: 42, weight: .bold, design: .serif)).minimumScaleFactor(0.5)
                            Text(word.pos).font(.caption).foregroundStyle(.secondary)
                            Text(word.meaning).font(.title3.weight(.medium))
                            HStack(spacing: 12) {
                                ForEach(["us", "uk"], id: \.self) { locale in
                                    Button {
                                        Task { do { try await audio.play(wordID: word.id, locale: locale) } catch { model.error = error.localizedDescription } }
                                    } label: {
                                        VStack(spacing: 5) {
                                            Label(locale.uppercased(), systemImage: "speaker.wave.2.fill").font(.headline)
                                            Text(word.firstRecording(locale: locale)?.ipa ?? "Chưa có bản thu").font(.caption)
                                        }.frame(maxWidth: .infinity).padding(.vertical, 12)
                                    }.buttonStyle(.bordered).disabled(word.firstRecording(locale: locale) == nil)
                                }
                            }
                            if !audio.message.isEmpty { Text(audio.message).font(.caption).foregroundStyle(.secondary) }
                            Button(action: model.next) { Label("Đổi từ", systemImage: "arrow.triangle.2.circlepath").frame(maxWidth: .infinity).padding(.vertical, 8) }.buttonStyle(.borderedProminent)
                        }.padding(22).background(.background, in: RoundedRectangle(cornerRadius: 24))
                        note("VÍ DỤ TỰ SOẠN", english: word.example, vietnamese: word.translation)
                        note("CÁCH DÙNG", english: "", vietnamese: word.usage.replacingOccurrences(of: "`", with: ""))
                        note("PHÂN BIỆT", english: "", vietnamese: word.contrast.replacingOccurrences(of: "`", with: ""))
                        ForEach(Array(word.pronunciations.enumerated()), id: \.offset) { _, pronunciation in
                            if let url = URL(string: pronunciation.source) { Link("Oxford: \(pronunciation.word) · \(pronunciation.pos) ↗", destination: url).font(.caption) }
                        }
                    }
                    Button { showHelp = true } label: { Label("Gắn widget lên màn hình khóa", systemImage: "rectangle.and.hand.point.up.left") }
                    Text("\(model.words.count) từ nhúng offline. Audio Oxford cần mạng. Chu kỳ tự đổi là mốc mong muốn; iOS kiểm soát lịch cập nhật.")
                        .font(.caption).foregroundStyle(.secondary)
                }.padding(22)
            }
            .background(Color(red: 0.95, green: 0.95, blue: 0.91))
            .navigationTitle("Word Garden ✿")
            .navigationBarTitleDisplayMode(.inline)
            .searchable(text: $model.query, prompt: "Tìm từ hoặc nghĩa tiếng Việt")
            .toolbar { ToolbarItem(placement: .topBarTrailing) { Button { showHelp = true } label: { Image(systemName: "info.circle") } } }
            .tint(green)
            .sheet(isPresented: $showHelp) { help }
            .alert("Thông báo", isPresented: Binding(get: { model.error != nil }, set: { if !$0 { model.error = nil } })) { Button("OK") { model.error = nil } } message: { Text(model.error ?? "") }
        }
    }
    private func note(_ title: String, english: String, vietnamese: String) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(title).font(.system(size: 10, weight: .bold)).tracking(1.5).foregroundStyle(.secondary)
            if !english.isEmpty { Text(english).font(.title3.weight(.semibold)) }
            Text(vietnamese).font(.body)
        }.frame(maxWidth: .infinity, alignment: .leading)
    }
    private var help: some View {
        NavigationStack {
            List {
                Section("Màn hình khóa") {
                    Text("1. Nhấn giữ màn hình khóa → Tùy chỉnh → vùng widget dưới đồng hồ.")
                    Text("2. Chọn Word Garden — Đổi & Nghe, thêm một ô chữ nhật.")
                    Text("3. Chạm chữ để đổi từ tại chỗ. Chạm loa để thử phát âm Oxford.")
                }
                Section("Chu kỳ và giọng đọc") {
                    Text("Nhấn giữ widget → Sửa tiện ích để nhập số phút và chọn US/UK. Mỗi widget có cài đặt riêng; app không cần quyền chia sẻ dữ liệu đặc biệt.")
                    Text("Chọn 1 phút không bảo đảm đổi mỗi phút. Bản thử cấp lịch hiển thị tối thiểu cách nhau 5 phút để phù hợp hướng dẫn WidgetKit.")
                }
                Section("Bản thử trên thiết bị") {
                    Text("Mở app một lần sau khi cài. Phát âm khi máy khóa cần kiểm tra thực tế trên iPhone và có thể bị hạn chế bởi cài đặt thiết bị.")
                    Text("Bộ ôn trong app độc lập với bộ trên widget. Chạm nút xem ví dụ trên widget Màn hình chính để mở đúng từ đang hiển thị.")
                }
            }.navigationTitle("Cách dùng").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Xong") { showHelp = false } } }
        }
    }
}
