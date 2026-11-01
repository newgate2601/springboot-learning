import AppIntents
import SwiftUI
import WidgetKit
import WordGardenCore

struct GardenEntry: TimelineEntry {
    let date: Date
    let word: Word?
    let configuration: GardenConfiguration
    let error: String?
}

struct GardenProvider: AppIntentTimelineProvider {
    typealias Intent = GardenConfiguration
    typealias Entry = GardenEntry
    func placeholder(in context: Context) -> GardenEntry {
        GardenEntry(date: Date(), word: try? Library.catalog().words.first, configuration: GardenConfiguration(), error: nil)
    }
    func snapshot(for configuration: GardenConfiguration, in context: Context) async -> GardenEntry {
        await entry(configuration: configuration)
    }
    private func entry(configuration: GardenConfiguration) async -> GardenEntry {
        do {
            let now = Date(), catalog = try Library.catalog()
            let deck = try await WidgetDeckStore.shared.snapshot(minutes: configuration.minutes, now: now)
            return GardenEntry(date: now, word: deck.currentID(at: now).flatMap { catalog.word(id: $0) }, configuration: configuration, error: nil)
        } catch { return GardenEntry(date: Date(), word: nil, configuration: configuration, error: error.localizedDescription) }
    }
    func timeline(for configuration: GardenConfiguration, in context: Context) async -> Timeline<GardenEntry> {
        do {
            let now = Date(), catalog = try Library.catalog()
            let deck = try await WidgetDeckStore.shared.snapshot(minutes: configuration.minutes, now: now)
            var dates = [now]
            // Supply future entries, rather than repeatedly launching background work.
            // Small requested intervals still remain subject to WidgetKit scheduling.
            let spacing = max(5, Deck.clamp(configuration.minutes)) * 60
            let first = configuration.minutes >= 5 ? deck.nextDate(after: now) : now.addingTimeInterval(300)
            for i in 0..<24 { dates.append(first.addingTimeInterval(Double(i * spacing))) }
            let entries = dates.map { date in
                GardenEntry(date: date, word: deck.currentID(at: date).flatMap { catalog.word(id: $0) }, configuration: configuration, error: nil)
            }
            return Timeline(entries: entries, policy: .after(dates.last!.addingTimeInterval(Double(spacing))))
        } catch {
            return Timeline(entries: [GardenEntry(date: Date(), word: nil, configuration: configuration, error: error.localizedDescription)], policy: .after(Date().addingTimeInterval(1800)))
        }
    }
}

struct GardenWidgetView: View {
    @Environment(\.widgetFamily) private var family
    let entry: GardenEntry
    private var isLockScreen: Bool { family == .accessoryRectangular }
    var body: some View {
        Group {
            if let word = entry.word {
                if isLockScreen { lockScreen(word) } else { homeScreen(word) }
            } else {
                VStack(alignment: .leading) { Text("Word Garden").bold(); Text(entry.error ?? "Mở app một lần rồi thử thêm lại widget.").font(.caption2) }
            }
        }
        .containerBackground(for: .widget) {
            if !isLockScreen { Color(red: 0.92, green: 0.95, blue: 0.87) }
        }
    }
    private func ipa(_ word: Word) -> String {
        guard let p = word.pronunciations.first(where: { $0.recording(for: entry.configuration.locale.rawValue) != nil }),
              let rec = p.recording(for: entry.configuration.locale.rawValue) else { return "Chưa có bản thu" }
        let label = p.word.lowercased() == word.word.lowercased() ? "" : p.word + " "
        return entry.configuration.locale.rawValue.uppercased() + " " + label + rec.ipa
    }
    private func lockScreen(_ word: Word) -> some View {
        HStack(spacing: 5) {
            Button(intent: NextWordIntent(displayedID: word.id, minutes: entry.configuration.minutes)) {
                VStack(alignment: .leading, spacing: 1) {
                    HStack(spacing: 3) { Text(word.word).font(.system(size: 21, weight: .bold)); Image(systemName: "arrow.triangle.2.circlepath").font(.system(size: 9)) }
                    Text(ipa(word)).font(.system(size: 10))
                    if entry.configuration.showMeaning { Text(word.meaning).font(.system(size: 11)) }
                }.lineLimit(1).minimumScaleFactor(0.6).frame(maxWidth: .infinity, alignment: .leading).contentShape(Rectangle())
            }.buttonStyle(.plain).accessibilityLabel("Đổi từ. Đang hiện \(word.word)")
            Button(intent: PlayWordIntent(wordID: word.id, locale: entry.configuration.locale)) {
                Image(systemName: "speaker.wave.2.fill").font(.system(size: 16)).frame(width: 26, height: 40).contentShape(Rectangle())
            }.buttonStyle(.plain).disabled(word.firstRecording(locale: entry.configuration.locale.rawValue) == nil).accessibilityLabel("Nghe \(word.word)")
        }
    }
    private func homeScreen(_ word: Word) -> some View {
        VStack(alignment: .leading, spacing: 7) {
            Text("WORD GARDEN ✿").font(.system(size: 9, weight: .bold)).tracking(1).foregroundStyle(.secondary)
            Text(word.word).font(.system(size: family == .systemSmall ? 25 : 31, weight: .bold, design: .serif)).lineLimit(1).minimumScaleFactor(0.5)
            Text(ipa(word)).font(.caption).foregroundStyle(.secondary).lineLimit(1)
            if entry.configuration.showMeaning { Text(word.meaning).font(.subheadline).lineLimit(2) }
            if family == .systemLarge { Text(word.example).font(.body); Text(word.translation).font(.caption).foregroundStyle(.secondary) }
            Spacer(minLength: 0)
            HStack {
                Button(intent: NextWordIntent(displayedID: word.id, minutes: entry.configuration.minutes)) { Label("Đổi từ", systemImage: "arrow.triangle.2.circlepath") }
                Spacer(minLength: 0)
                Button(intent: PlayWordIntent(wordID: word.id, locale: entry.configuration.locale)) { Image(systemName: "speaker.wave.2.fill") }
                    .disabled(word.firstRecording(locale: entry.configuration.locale.rawValue) == nil)
                Link(destination: URL(string: "wordgarden://word/\(word.id)")!) { Image(systemName: "arrow.up.right.square") }.accessibilityLabel("Xem ví dụ")
            }.font(.caption).buttonStyle(.plain)
        }.foregroundStyle(Color(red: 0.14, green: 0.29, blue: 0.21))
    }
}

@main struct WordGardenWidget: Widget {
    var body: some WidgetConfiguration {
        AppIntentConfiguration(kind: "WordGardenWidget", intent: GardenConfiguration.self, provider: GardenProvider()) { entry in GardenWidgetView(entry: entry) }
            .configurationDisplayName("Word Garden — Đổi & Nghe")
            .description("Chạm từ để đổi ngay tại chỗ. Chạm loa để thử nghe Oxford.")
            .supportedFamilies([.accessoryRectangular, .systemSmall, .systemMedium, .systemLarge])
            .contentMarginsDisabled()
    }
}
