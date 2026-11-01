import AVFoundation
import Combine
import Foundation

@MainActor final class NativeAudio: NSObject, ObservableObject, AVAudioPlayerDelegate {
    static let shared = NativeAudio()
    @Published private(set) var message = ""
    @Published private(set) var isPlaying = false
    private var player: AVAudioPlayer?
    private var generation = UUID()

    func play(wordID: Int, locale: String) async throws {
        guard let word = try Library.catalog().word(id: wordID),
              let recording = word.firstRecording(locale: locale) else { throw PlaybackError.missingRecording }
        guard let url = URL(string: recording.audio), url.scheme == "https",
              url.host == "www.oxfordlearnersdictionaries.com", url.path.contains("/media/english/") else { throw PlaybackError.invalidURL }
        stop()
        let token = UUID(); generation = token
        message = "Đang tải bản thu \(locale.uppercased())…"
        do {
            var request = URLRequest(url: url, cachePolicy: .returnCacheDataElseLoad, timeoutInterval: 20)
            request.setValue("audio/*", forHTTPHeaderField: "Accept")
            let (data, response) = try await URLSession.shared.data(for: request)
            guard token == generation else { return }
            guard let http = response as? HTTPURLResponse, (200..<300).contains(http.statusCode) else { throw PlaybackError.downloadFailed }
            let mime = response.mimeType ?? ""
            guard !data.isEmpty, data.count < 5_000_000,
                  mime.hasPrefix("audio/") || mime == "application/octet-stream" else { throw PlaybackError.unsupportedData }
            let session = AVAudioSession.sharedInstance()
            try session.setCategory(.playback, mode: .spokenAudio, options: [.duckOthers])
            try session.setActive(true)
            let nextPlayer = try AVAudioPlayer(data: data)
            nextPlayer.delegate = self
            nextPlayer.prepareToPlay()
            player = nextPlayer
            guard nextPlayer.play() else { throw PlaybackError.cannotPlay }
            isPlaying = true
            message = "Bản thu Oxford · \(locale.uppercased())"
        } catch {
            if token == generation { stop(); message = error.localizedDescription }
            throw error
        }
    }
    func stop() {
        generation = UUID()
        player?.stop(); player = nil; isPlaying = false
        try? AVAudioSession.sharedInstance().setActive(false, options: [.notifyOthersOnDeactivation])
    }
    nonisolated func audioPlayerDidFinishPlaying(_ player: AVAudioPlayer, successfully flag: Bool) {
        Task { @MainActor in
            guard self.player === player else { return }
            self.stop()
            self.message = flag ? "Đã phát xong." : "Không phát được bản thu."
        }
    }
}
