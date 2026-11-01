import Foundation

public struct Recording: Codable, Equatable, Sendable {
    public let audio: String
    public let ipa: String
}
public struct Pronunciation: Codable, Equatable, Sendable {
    public let word: String
    public let pos: String
    public let source: String
    public let us: Recording?
    public let uk: Recording?
    public func recording(for locale: String) -> Recording? { locale == "uk" ? uk : us }
}
public struct Word: Codable, Identifiable, Equatable, Sendable {
    public let id: Int
    public let word: String
    public let pos: String
    public let meaning: String
    public let image: String
    public let example: String
    public let translation: String
    public let usage: String
    public let contrast: String
    public let pronunciations: [Pronunciation]
    public func firstRecording(locale: String) -> Recording? {
        pronunciations.lazy.compactMap { $0.recording(for: locale) }.first
    }
}

public enum CatalogError: LocalizedError {
    case missing, empty, duplicateIDs
    public var errorDescription: String? {
        switch self {
        case .missing: return "Thiếu dữ liệu vocabulary.json trong bản build."
        case .empty: return "Bộ từ trống."
        case .duplicateIDs: return "Dữ liệu có ID trùng."
        }
    }
}
public struct Catalog: Sendable {
    public let words: [Word]
    private let byID: [Int: Word]
    public var ids: [Int] { words.map(\.id) }
    public init(data: Data) throws {
        words = try JSONDecoder().decode([Word].self, from: data)
        guard !words.isEmpty else { throw CatalogError.empty }
        guard Set(words.map(\.id)).count == words.count else { throw CatalogError.duplicateIDs }
        byID = Dictionary(uniqueKeysWithValues: words.map { ($0.id, $0) })
    }
    public func word(id: Int) -> Word? { byID[id] }
}
