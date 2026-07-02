import Foundation
import GRDB

enum DoseForm: String, Codable, DatabaseValueConvertible {
    case tablet, injection, other
}

enum FrequencyType: String, Codable, DatabaseValueConvertible {
    case daily, interval
}

enum TimingSlot: String, Codable, DatabaseValueConvertible, CaseIterable {
    case morning, noon, night
}

struct Medicine: Codable, Identifiable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "medicines"

    var id: String
    var masterId: String?
    var name: String
    var doseForm: DoseForm
    var doseAmount: Double
    var frequencyType: FrequencyType
    var intervalDays: Int?
    var timingMorning: Bool
    var timingNoon: Bool
    var timingNight: Bool
    var remainingQty: Double
    var isActive: Bool
    /// レガシーフィールド。QRコードの内容は現在 QrPayloadCodec(MessagePack)が直接 id を埋め込むため未使用。
    /// 一意な外部識別子として将来的に再利用する可能性があるため残してある。
    var qrToken: String
    var createdAt: Date
    var updatedAt: Date

    init(
        id: String = UUID().uuidString,
        masterId: String? = nil,
        name: String,
        doseForm: DoseForm = .tablet,
        doseAmount: Double,
        frequencyType: FrequencyType = .daily,
        intervalDays: Int? = nil,
        timingMorning: Bool = false,
        timingNoon: Bool = false,
        timingNight: Bool = false,
        remainingQty: Double,
        isActive: Bool = true,
        qrToken: String = UUID().uuidString,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id; self.masterId = masterId; self.name = name; self.doseForm = doseForm
        self.doseAmount = doseAmount; self.frequencyType = frequencyType; self.intervalDays = intervalDays
        self.timingMorning = timingMorning; self.timingNoon = timingNoon; self.timingNight = timingNight
        self.remainingQty = remainingQty; self.isActive = isActive; self.qrToken = qrToken
        self.createdAt = createdAt; self.updatedAt = updatedAt
    }

    func requiredTimingsCount() -> Int {
        [timingMorning, timingNoon, timingNight].filter { $0 }.count
    }

    func isDue(on date: Date, calendar: Calendar = .current) -> Bool {
        guard isActive else { return false }
        switch frequencyType {
        case .daily: return true
        case .interval:
            let n = max(intervalDays ?? 1, 1)
            let days = calendar.dateComponents([.day], from: calendar.startOfDay(for: createdAt), to: calendar.startOfDay(for: date)).day ?? 0
            return days % n == 0
        }
    }

    // MARK: - ActiveRecord風 操作

    @discardableResult
    func save() throws -> Medicine {
        var copy = self
        copy.updatedAt = Date()
        try AppDatabaseManager.shared.write { db in
            try copy.save(db)
        }
        return copy
    }

    func destroy() throws {
        _ = try AppDatabaseManager.shared.write { db in
            try self.delete(db)
        }
    }

    func withAddedQuantity(_ qty: Double) -> Medicine {
        var copy = self
        copy.remainingQty += qty
        copy.updatedAt = Date()
        return copy
    }

    // MARK: - Finder (static, ActiveRecord風)

    static func all() throws -> [Medicine] {
        try AppDatabaseManager.shared.read { db in
            try Medicine.order(Column("createdAt").desc).fetchAll(db)
        }
    }

    static func active() throws -> [Medicine] {
        try AppDatabaseManager.shared.read { db in
            try Medicine.filter(Column("isActive") == true)
                .order(Column("createdAt").desc).fetchAll(db)
        }
    }

    static func find(_ id: String) throws -> Medicine? {
        try AppDatabaseManager.shared.read { db in try Medicine.fetchOne(db, key: id) }
    }

    static func findByQrToken(_ token: String) throws -> Medicine? {
        try AppDatabaseManager.shared.read { db in
            try Medicine.filter(Column("qrToken") == token).fetchOne(db)
        }
    }

    static func findByName(_ name: String) throws -> Medicine? {
        try AppDatabaseManager.shared.read { db in
            try Medicine.filter(Column("name") == name).fetchOne(db)
        }
    }

    @discardableResult
    static func create(
        name: String,
        doseForm: DoseForm,
        doseAmount: Double,
        frequencyType: FrequencyType,
        intervalDays: Int?,
        timingMorning: Bool,
        timingNoon: Bool,
        timingNight: Bool,
        remainingQty: Double,
        isActive: Bool = true,
        masterId: String? = nil
    ) throws -> Medicine {
        let m = Medicine(
            masterId: masterId, name: name, doseForm: doseForm, doseAmount: doseAmount,
            frequencyType: frequencyType, intervalDays: intervalDays,
            timingMorning: timingMorning, timingNoon: timingNoon, timingNight: timingNight,
            remainingQty: remainingQty, isActive: isActive
        )
        try AppDatabaseManager.shared.write { db in try m.insert(db) }
        return m
    }
}
