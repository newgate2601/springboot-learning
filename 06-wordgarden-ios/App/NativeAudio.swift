import AVFoundation
import Combine
import Foundation

/// Plays the Oxford recording when possible; falls back to the built-in iPhone
/// voice (offline) when the word has no recording or the download fails.
@MainActor final class NativeAudio: NSObject, ObservableObject, AVAudioPlayerDelegate, AVSpeechSynthesizerDelegate {
    static let shared = NativeAudio()
    @Published private(set) var message = ""
    @Published private(set) var isPlaying = false
    private var player: AVAudioPlayer?
    private let synth = AVSpeechSynthesizer()
    private var generation = UUID()

    override init() {
        super.init()
        synth.delegate = self
    }

    func play(wordID: Int, locale: String) async throws {
        guard let word = try Library.catalog().word(id: wordID) else { throw PlaybackError.missingRecording }
        guard let recording = word.firstRecording(locale: locale),
              let url = URL(string: recording.audio), url.scheme == "https",
              url.host == "www.oxfordlearnersdictionaries.com", url.path.contains("/media/english/") else {
            try speak(word.word, locale: locale, note: "Giọng iPhone · \(locale.uppercased())")
            return
        }
        stop()
        let token = UUID(); generation = token
        message = "Đang tải bản thu \(locale.uppercased())…"
        do {
            var request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad, timeoutInterval: 8)
            request.setValue("audio/*", forHTTPHeaderField: "Accept")
            let (data, response) = try await URLSession.shared.data(for: request)
            guard token == generation else { return }
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { throw PlaybackError.downloadFailed }
            let mime = response.mimeType ?? ""
            guard !data.isEmpty, data.count < 5_000_000,
                  mime.hasPrefix("audio/") || mime == "application/octet-stream" else { throw PlaybackError.unsupportedData }
            try activateSession()
            let nextPlayer = try AVAudioPlayer(data: data)
            nextPlayer.delegate = self
            nextPlayer.prepareToPlay()
            player = nextPlayer
            guard nextPlayer.play() else { throw PlaybackError.cannotPlay }
            isPlaying = true
            message = "Bản thu Oxford · \(locale.uppercased())"
        } catch {
            guard token == generation else { return }
            // Offline or Oxford unavailable: still pronounce the word.
            try speak(word.word, locale: locale, note: "Không tải được Oxford — dùng giọng iPhone · \(locale.uppercased())")
        }
    }

    /// Reads any text (word or example sentence) with the on-device voice.
    func speak(_ text: String, locale: String, note: String? = nil) throws {
        stop()
        generation = UUID()
        try activateSession()
        let utterance = AVSpeechUtterance(string: text)
        utterance.voice = AVSpeechSynthesisVoice(language: locale == "uk" ? "en-GB" : "en-US")
        utterance.rate = AVSpeechUtteranceDefaultSpeechRate * 0.9
        synth.speak(utterance)
        isPlaying = true
        message = note ?? "Giọng iPhone · \(locale.uppercased())"
    }

    private func activateSession() throws {
        let session = AVAudioSession.sharedInstance()
        try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
        try session.setActive(true)
    }

    func stop() {
        generation = UUID()
        player?.stop(); player = nil
        if synth.isSpeaking { synth.stopSpeaking(at: .immediate) }
        isPlaying = false
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }

    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            guard self.player === player else { return }
            self.stop()
            self.message = flag ? "Đã phát xong." : "Không phát được bản thu."
        }
    }
    nonisolated func speechSynthesizer(_ synthesizer: AVSpeechSynthesizer, didFinish utterance: AVSpeechUtterance) {
        Task { @MainActor in
            guard self.player == nil, !self.synth.isSpeaking else { return }
            self.isPlaying = false
            try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
        }
    }
}
