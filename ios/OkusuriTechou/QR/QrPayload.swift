import Foundation
import MessagePack

/// QRコードに埋め込むデータのスキーマ。
///
/// MessagePackでエンコードしたバイナリをBase64文字列化してQRコードの内容にする。
/// (JSON的なkey-valueでありながらバイナリで省サイズ・高速にデコードできるため採用。Android版と同一スキーマ)
///
/// ルート:
/// {
///   "type": Int,       // ペイロード種別。現状は INTAKE(=1) のみ。
///                       // 将来「QRコードから薬を新規登録する」等の用途を見越して列挙体にしている。
///   "version": Int,    // このペイロード自体のスキーマバージョン(将来の互換性のため)
///   "data": {
///     "medicine_id": String,        // ローカルDBの Medicine.id。同一端末内での照合に最優先で使う。
///     "mst_medicine_id": String?,   // MedicineMaster.id。将来マスターデータ同期した際の照合用(現状null許容)。
///     "medicine_name": String       // 薬の名前。medicine_id が見つからない場合のフォールバック照合、
///                                   // および人間がQR内容を確認する際の可読性のために含める。
///   }
/// }
enum QrPayloadType {
    /// 服用記録(このQRを読み取ったら「服用した」として記録する)
    static let intake = 1
}

struct QrIntakeData {
    let medicineId: String
    let mstMedicineId: String?
    let medicineName: String
}

struct QrPayload {
    let type: Int
    let version: Int
    let data: QrIntakeData
}

enum QrPayloadCodec {
    private static let schemaVersion: Int64 = 1

    static func encodeIntake(_ medicine: Medicine) -> String {
        let dataMap: [MessagePackValue: MessagePackValue] = [
            "medicine_id": .string(medicine.id),
            "mst_medicine_id": medicine.masterId.map { MessagePackValue.string($0) } ?? .nil,
            "medicine_name": .string(medicine.name)
        ]
        let root: [MessagePackValue: MessagePackValue] = [
            "type": .int(Int64(QrPayloadType.intake)),
            "version": .int(schemaVersion),
            "data": .map(dataMap)
        ]
        let packed = pack(.map(root))
        return packed.base64EncodedString()
    }

    static func decode(_ base64: String) -> QrPayload? {
        guard let raw = Data(base64Encoded: base64) else { return nil }
        guard let (value, _) = try? unpack(raw) else { return nil }
        guard case .map(let root) = value else { return nil }

        guard case .int(let typeRaw)? = root[.string("type")] else { return nil }

        var version: Int64 = 0
        if case .int(let v)? = root[.string("version")] { version = v }

        guard case .map(let dataMap)? = root[.string("data")] else { return nil }
        guard case .string(let medicineId)? = dataMap[.string("medicine_id")] else { return nil }
        guard case .string(let medicineName)? = dataMap[.string("medicine_name")] else { return nil }

        var mstMedicineId: String? = nil
        if case .string(let m)? = dataMap[.string("mst_medicine_id")] { mstMedicineId = m }

        return QrPayload(
            type: Int(typeRaw),
            version: Int(version),
            data: QrIntakeData(medicineId: medicineId, mstMedicineId: mstMedicineId, medicineName: medicineName)
        )
    }
}
