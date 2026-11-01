import Foundation

/// A clock-based shuffled cycle. Taps include the displayed ID so duplicate/stale
/// widget actions do not silently skip several cards.
public struct Deck: Codable, Equatable, Sendable {
    public private(set) var order: [Int]
    public private(set) var cursor: Int
    public private(set) var anchor: Date
    public private(set) var minutes: Int

    public init(order: [Int], now: Date, minutes: Int = 30) {
        var seen = Set<Int>()
        self.order = order.filter { seen.insert($0).inserted }
        self.cursor = 0
        self.anchor = now
        self.minutes = Self.clamp(minutes)
    }
    public static func clamp(_ value: Int) -> Int { min(1440, max(1, value)) }
    private func offset(at now: Date) -> Int {
        let raw = max(0, now.timeIntervalSince(anchor)) / Double(Self.clamp(minutes) * 60)
        return Int(min(raw, Double(Int.max / 4)))
    }
    public func index(at now: Date) -> Int? {
        guard !order.isEmpty else { return nil }
        return (cursor % order.count + offset(at: now) % order.count) % order.count
    }
    public func currentID(at now: Date) -> Int? { index(at: now).map { order[$0] } }
    public func nextDate(after now: Date) -> Date {
        anchor.addingTimeInterval(Double(offset(at: now) + 1) * Double(Self.clamp(minutes) * 60))
    }
    public mutating func setMinutes(_ value: Int, now: Date) {
        let value = Self.clamp(value)
        guard minutes != value else { return }
        cursor = index(at: now) ?? 0
        anchor = now
        minutes = value
    }
    @discardableResult public mutating func advance(expectedID: Int, now: Date) -> Bool {
        guard let index = index(at: now), order[index] == expectedID else { return false }
        cursor = (index + 1) % order.count
        anchor = now
        return true
    }
    public mutating func reconcile(ids: [Int], now: Date) {
        let current = currentID(at: now)
        let allowed = Set(ids)
        var seen = Set<Int>()
        var updated = order.filter { allowed.contains($0) && seen.insert($0).inserted }
        updated.append(contentsOf: ids.filter { seen.insert($0).inserted })
        guard updated != order else { return }
        order = updated
        cursor = current.flatMap { updated.firstIndex(of: $0) } ?? 0
        anchor = now
    }
}
