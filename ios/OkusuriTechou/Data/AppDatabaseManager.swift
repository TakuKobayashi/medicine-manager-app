import Foundation
import GRDB

/// アプリ本体とWidget Extensionの両方から参照する共有DB。
/// App Group有効化後は共有コンテナにSQLiteを置くことでウィジェットからも読み書き可能になる。
/// （現状はApp Group未設定のためアプリのDocumentsディレクトリにフォールバックする）
enum AppDatabaseManager {
    static let appGroupId = "group.com.phantomcatworks.okusuritechou"

    static let shared: DatabaseQueue = {
        let container = FileManager.default.containerURL(forSecurityApplicationGroupIdentifier: appGroupId)
        let dbURL = (container ?? FileManager.default.urls(for: .documentDirectory, in: .userDomainMask)[0])
            .appendingPathComponent("okusuri_techou.sqlite")

        var config = Configuration()
        config.foreignKeysEnabled = true

        let dbQueue = try! DatabaseQueue(path: dbURL.path, configuration: config)
        try! migrator.migrate(dbQueue)
        return dbQueue
    }()

    static var migrator: DatabaseMigrator = {
        var migrator = DatabaseMigrator()

        migrator.registerMigration("v1_create_tables") { db in
            try db.create(table: "medicine_master") { t in
                t.column("id", .text).primaryKey()
                t.column("name", .text).notNull()
                t.column("genericName", .text)
                t.column("manufacturer", .text)
                t.column("defaultUnit", .text)
                t.column("source", .text)
                t.column("syncedAt", .datetime)
                t.column("createdAt", .datetime).notNull()
                t.column("updatedAt", .datetime).notNull()
            }

            try db.create(table: "medicines") { t in
                t.column("id", .text).primaryKey()
                t.column("masterId", .text).references("medicine_master", onDelete: .setNull)
                t.column("name", .text).notNull()
                t.column("doseForm", .text).notNull().defaults(to: "tablet")
                t.column("doseAmount", .double).notNull()
                t.column("frequencyType", .text).notNull().defaults(to: "daily")
                t.column("intervalDays", .integer)
                t.column("timingMorning", .boolean).notNull().defaults(to: false)
                t.column("timingNoon", .boolean).notNull().defaults(to: false)
                t.column("timingNight", .boolean).notNull().defaults(to: false)
                t.column("remainingQty", .double).notNull()
                t.column("isActive", .boolean).notNull().defaults(to: true)
                t.column("qrToken", .text).notNull().unique()
                t.column("createdAt", .datetime).notNull()
                t.column("updatedAt", .datetime).notNull()
            }

            try db.create(table: "intake_logs") { t in
                t.column("id", .text).primaryKey()
                t.column("medicineId", .text).notNull().references("medicines", onDelete: .cascade)
                t.column("timingSlot", .text).notNull()
                t.column("takenAt", .datetime).notNull()
                t.column("takenDate", .text).notNull()
                t.column("source", .text).notNull().defaults(to: "checkbox")
                t.column("createdAt", .datetime).notNull()
            }

            try db.create(table: "notification_settings") { t in
                t.column("slot", .text).primaryKey()
                t.column("hour", .integer).notNull()
                t.column("minute", .integer).notNull()
                t.column("message", .text).notNull()
                t.column("enabled", .boolean).notNull().defaults(to: true)
            }
        }

        return migrator
    }()
}
