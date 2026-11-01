import Foundation
import WordGardenCore

enum Library {
    static let result: Result<Catalog, Error> = Result {
        guard let url = Bundle.main.url(forResource: "vocabulary", withExtension: "json") else { throw CatalogError.missing }
        return try Catalog(data: Data(contentsOf: url))
    }
    static func catalog() throws -> Catalog { try result.get() }
}

actor WidgetDeckStore {
    static let shared = WidgetDeckStore()
    // Runs in the widget extension's process. App Groups are deliberately absent
    // in this first sideload build. Widget configuration is edited on the widget.
    private let defaults = UserDefaults.standard
    private let key = "wordgarden.native.widget.deck.v1"
    private func load(minutes: Int, now: Date) throws -> Deck {
        let ids = try Library.catalog().ids
        var deck: Deck
        if let data = defaults.data(forKey: key), let saved = try? JSONDecoder().decode(Deck.self, from: data) { deck = saved }
        else { deck = Deck(order: ids.shuffled(), now: now, minutes: minutes) }
        deck.reconcile(ids: ids, now: now)
        deck.setMinutes(minutes, now: now)
        try save(deck)
        return deck
    }
    private func save(_ deck: Deck) throws { defaults.set(try JSONEncoder().encode(deck), forKey: key) }
    func snapshot(minutes: Int, now: Date = Date()) throws -> Deck { try load(minutes: minutes, now: now) }
    func advance(expectedID: Int, minutes: Int) throws {
        let now = Date()
        var deck = try load(minutes: minutes, now: now)
        if deck.advance(expectedID: expectedID, now: now) { try save(deck) }
    }
}
