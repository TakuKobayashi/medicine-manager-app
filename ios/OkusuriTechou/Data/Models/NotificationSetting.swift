import Foundation
import GRDB

struct NotificationSetting: Codable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "notification_settings"

    var slot: TimingSlot
    var hour: Int
    var minute: Int
    var message: String
    var enabled: Bool

    static let defaults: [NotificationSetting] = [
        NotificationSetting(slot: .morning, hour: 8, minute: 0, message: "朝のお薬の時間です。お忘れなく服用しましょう。", enabled: true),
        NotificationSetting(slot: .noon, hour: 12, minute: 0, message: "昼のお薬の時間です。お忘れなく服用しましょう。", enabled: true),
        NotificationSetting(slot: .night, hour: 18, minute: 0, message: "夜のお薬の時間です。お忘れなく服用しましょう。", enabled: true)
    ]

    func hasNoApplicableMedicine() throws -> Bool {
        let actives = try Medicine.active()
        return !actives.contains { medicine in
            switch slot {
            case .morning: return medicine.timingMorning
            case .noon: return medicine.timingNoon
            case .night: return medicine.timingNight
            }
        }
    }

    @discardableResult
    func save() throws -> NotificationSetting {
        try AppDatabaseManager.shared.write { db in try self.save(db) }
        return self
    }

    static func all() throws -> [NotificationSetting] {
        try AppDatabaseManager.shared.read { db in try NotificationSetting.fetchAll(db) }
    }

    static func find(_ slot: TimingSlot) throws -> NotificationSetting? {
        try AppDatabaseManager.shared.read { db in try NotificationSetting.fetchOne(db, key: slot.rawValue) }
    }

    static func ensureDefaults() throws {
        try AppDatabaseManager.shared.write { db in
            for setting in defaults {
                if try NotificationSetting.fetchOne(db, key: setting.slot.rawValue) == nil {
                    try setting.insert(db)
                }
            }
        }
    }
}
