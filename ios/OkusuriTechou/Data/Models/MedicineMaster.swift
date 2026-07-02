import Foundation
import GRDB

struct MedicineMaster: Codable, Identifiable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "medicine_master"

    var id: String
    var name: String
    var genericName: String?
    var manufacturer: String?
    var defaultUnit: String?
    var source: String?
    var syncedAt: Date?
    var createdAt: Date
    var updatedAt: Date

    init(
        id: String = UUID().uuidString,
        name: String,
        genericName: String? = nil,
        manufacturer: String? = nil,
        defaultUnit: String? = nil,
        source: String? = nil,
        syncedAt: Date? = nil,
        createdAt: Date = Date(),
        updatedAt: Date = Date()
    ) {
        self.id = id; self.name = name; self.genericName = genericName
        self.manufacturer = manufacturer; self.defaultUnit = defaultUnit
        self.source = source; self.syncedAt = syncedAt
        self.createdAt = createdAt; self.updatedAt = updatedAt
    }

    static func all() throws -> [MedicineMaster] {
        try AppDatabaseManager.shared.read { db in
            try MedicineMaster.order(Column("name")).fetchAll(db)
        }
    }

    static func find(_ id: String) throws -> MedicineMaster? {
        try AppDatabaseManager.shared.read { db in try MedicineMaster.fetchOne(db, key: id) }
    }

    static func findByName(_ name: String) throws -> MedicineMaster? {
        try AppDatabaseManager.shared.read { db in
            try MedicineMaster.filter(Column("name") == name).fetchOne(db)
        }
    }

    @discardableResult
    static func findOrCreate(byName name: String) throws -> MedicineMaster {
        if let existing = try findByName(name) { return existing }
        let m = MedicineMaster(name: name, source: "local_ocr")
        try AppDatabaseManager.shared.write { db in try m.insert(db) }
        return m
    }
}
