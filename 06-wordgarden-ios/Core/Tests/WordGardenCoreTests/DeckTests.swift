import XCTest
@testable import WordGardenCore

final class DeckTests: XCTestCase {
    let start = Date(timeIntervalSince1970: 1_800_000_000)
    func testBoundariesAndCycle() {
        let deck = Deck(order: [10, 20, 30], now: start, minutes: 1)
        XCTAssertEqual(deck.currentID(at: start.addingTimeInterval(59)), 10)
        XCTAssertEqual(deck.currentID(at: start.addingTimeInterval(60)), 20)
        XCTAssertEqual(deck.currentID(at: start.addingTimeInterval(120)), 30)
        XCTAssertEqual(deck.currentID(at: start.addingTimeInterval(180)), 10)
        XCTAssertEqual(deck.nextDate(after: start), start.addingTimeInterval(60))
    }
    func testTapAndDuplicateTap() {
        var deck = Deck(order: [1, 2, 3], now: start)
        XCTAssertTrue(deck.advance(expectedID: 1, now: start))
        XCTAssertEqual(deck.currentID(at: start), 2)
        XCTAssertFalse(deck.advance(expectedID: 1, now: start))
        XCTAssertEqual(deck.currentID(at: start), 2)
    }
    func testIntervalChangePreservesCurrentAndPersists() throws {
        var deck = Deck(order: [1, 2, 3], now: start, minutes: 1)
        let now = start.addingTimeInterval(70)
        deck.setMinutes(30, now: now)
        XCTAssertEqual(deck.currentID(at: now), 2)
        XCTAssertEqual(deck.nextDate(after: now), now.addingTimeInterval(1800))
        let restored = try JSONDecoder().decode(Deck.self, from: JSONEncoder().encode(deck))
        XCTAssertEqual(restored, deck)
    }
    func testDatasetUpdateKeepsCurrentAndRemovesDuplicates() {
        var deck = Deck(order: [1, 2, 3, 3], now: start)
        deck.advance(expectedID: 1, now: start)
        deck.reconcile(ids: [2, 3, 4, 4], now: start)
        XCTAssertEqual(deck.order, [2, 3, 4])
        XCTAssertEqual(deck.currentID(at: start), 2)
    }
    func testClockRollbackAndInvalidIntervals() {
        let deck = Deck(order: [1], now: start, minutes: 0)
        XCTAssertEqual(deck.minutes, 1)
        XCTAssertEqual(deck.currentID(at: start.addingTimeInterval(-500)), 1)
        XCTAssertNil(Deck(order: [], now: start).currentID(at: start))
    }
}
