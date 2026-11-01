import AppIntents
import Foundation
import WidgetKit

enum PronunciationLocale: String, AppEnum {
    case us, uk
    static var typeDisplayRepresentation: TypeDisplayRepresentation = "Giọng đọc"
    static var caseDisplayRepresentations: [Self: DisplayRepresentation] = [.us: "US — Mỹ", .uk: "UK — Anh"]
}

struct GardenConfiguration: WidgetConfigurationIntent {
    static var title: LocalizedStringResource = "Word Garden"
    static var description = IntentDescription("Chọn chu kỳ mong muốn và giọng đọc của widget.")
    @Parameter(title: "Số phút đổi từ (1–1440)", default: 30) var minutes: Int
    @Parameter(title: "Giọng đọc", default: .us) var locale: PronunciationLocale
    @Parameter(title: "Hiện nghĩa", default: true) var showMeaning: Bool
}

struct NextWordIntent: AppIntent {
    static var title: LocalizedStringResource = "Đổi từ"
    static var description = IntentDescription("Hiện từ tiếp theo ngay trong widget.")
    static var openAppWhenRun: Bool = false
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    @Parameter(title: "ID từ đang hiện", default: 0) var displayedID: Int
    @Parameter(title: "Chu kỳ phút", default: 30) var minutes: Int
    init() {}
    init(displayedID: Int, minutes: Int) { self.displayedID = displayedID; self.minutes = minutes }
    func perform() async throws -> some IntentResult {
        try await WidgetDeckStore.shared.advance(expectedID: displayedID, minutes: minutes)
        WidgetCenter.shared.reloadTimelines(ofKind: "WordGardenWidget")
        return .result()
    }
}

/// AudioPlaybackIntent is routed to the app process without requesting its UI.
/// Actual playback while locked must still be verified on a signed physical device.
struct PlayWordIntent: AudioPlaybackIntent {
    static var title: LocalizedStringResource = "Nghe phát âm"
    static var openAppWhenRun: Bool = false
    static var authenticationPolicy: IntentAuthenticationPolicy = .alwaysAllowed
    @Parameter(title: "ID từ", default: 0) var wordID: Int
    @Parameter(title: "Giọng đọc", default: .us) var locale: PronunciationLocale
    init() {}
    init(wordID: Int, locale: PronunciationLocale) { self.wordID = wordID; self.locale = locale }
    func perform() async throws -> some IntentResult {
        #if WIDGET_EXTENSION
        // The system should dispatch AudioPlaybackIntent to the containing app.
        // Do not attempt unsupported extension audio playback if dispatch fails.
        throw PlaybackError.requiresAppProcess
        #else
        try await NativeAudio.shared.play(wordID: wordID, locale: locale.rawValue)
        #endif
        return .result()
    }
}

enum PlaybackError: LocalizedError {
    case missingRecording, invalidURL, downloadFailed, unsupportedData, cannotPlay, requiresAppProcess
    var errorDescription: String? {
        switch self {
        case .missingRecording: return "Chưa có bản thu Oxford cho từ và giọng này."
        case .invalidURL: return "Đường dẫn bản thu không hợp lệ."
        case .downloadFailed: return "Không tải được bản thu Oxford. Kiểm tra kết nối mạng."
        case .unsupportedData: return "Máy chủ không trả về dữ liệu âm thanh hợp lệ."
        case .cannotPlay: return "iPhone chưa phát được bản thu này."
        case .requiresAppProcess: return "iOS chưa chuyển thao tác nghe sang app. Hãy mở Word Garden một lần rồi thử lại."
        }
    }
}
