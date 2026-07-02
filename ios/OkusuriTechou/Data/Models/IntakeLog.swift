import Foundation
import GRDB

enum IntakeSource: String, Codable, DatabaseValueConvertible {
    case checkbox, qr
}

struct IntakeLog: Codable, Identifiable, FetchableRecord, PersistableRecord {
    static let databaseTableName = "intake_logs"

    var id: String
    var medicineId: String
    var timingSlot: TimingSlot
    var takenAt: Date
    var takenDate: String
    var source: IntakeSource
    var createdAt: Date

    private static let dateFormatter: DateFormatter = {
        let f = DateFormatter()
        f.dateFormat = "yyyy-MM-dd"
        f.calendar = .current
        f.timeZone = .current
        return f
    }()

    init(
        id: String = UUID().uuidString,
        medicineId: String,
        timingSlot: TimingSlot,
        takenAt: Date = Date(),
        source: IntakeSource = .checkbox
    ) {
        self.id = id
        self.medicineId = medicineId
        self.timingSlot = timingSlot
        self.takenAt = takenAt
        self.takenDate = Self.dateFormatter.string(from: takenAt)
        self.source = source
        self.createdAt = Date()
    }

    @discardableResult
    static func record(medicine: Medicine, slot: TimingSlot, source: IntakeSource = .checkbox) throws -> IntakeLog {
        let log = IntakeLog(medicineId: medicine.id, timingSlot: slot, source: source)
        try AppDatabaseManager.shared.write { db in try log.insert(db) }
        return log
    }

    static func forDate(_ date: String) throws -> [IntakeLog] {
        try AppDatabaseManager.shared.read { db in
            try IntakeLog.filter(Column("takenDate") == date).fetchAll(db)
        }
    }

    static func forMedicine(_ medicineId: String, date: String) throws -> [IntakeLog] {
        try AppDatabaseManager.shared.read { db in
            try IntakeLog
                .filter(Column("medicineId") == medicineId && Column("takenDate") == date)
                .fetchAll(db)
        }
    }

    static func isTaken(medicineId: String, slot: TimingSlot, date: String) throws -> Bool {
        try forMedicine(medicineId, date: date).contains { $0.timingSlot == slot }
    }

    static func forMonth(_ yearMonthPrefix: String) throws -> [IntakeLog] {
        try AppDatabaseManager.shared.read { db in
            try IntakeLog.filter(Column("takenDate").like("\(yearMonthPrefix)%")).fetchAll(db)
        }
    }
}
