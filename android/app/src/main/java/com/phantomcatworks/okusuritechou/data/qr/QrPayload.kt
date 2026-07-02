package com.phantomcatworks.okusuritechou.data.qr

import android.util.Base64
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import org.msgpack.core.MessagePack

/**
 * QRコードに埋め込むデータのスキーマ。
 *
 * MessagePackでエンコードしたバイナリをBase64文字列化してQRコードの内容にする。
 * (JSON的なkey-valueでありながらバイナリで省サイズ・高速にデコードできるため採用)
 *
 * ルート:
 * {
 *   "type": Int,       // ペイロード種別。現状は INTAKE(=1) のみ。
 *                       // 将来「QRコードから薬を新規登録する」等の用途を見越して列挙体にしている。
 *   "version": Int,    // このペイロード自体のスキーマバージョン(将来の互換性のため)
 *   "data": {
 *     "medicine_id": String,        // ローカルDBの Medicine.id。同一端末内での照合に最優先で使う。
 *     "mst_medicine_id": String?,   // MedicineMaster.id。将来マスターデータ同期した際の照合用(現状null許容)。
 *     "medicine_name": String       // 薬の名前。medicine_id が見つからない場合のフォールバック照合、
 *                                   // および人間がQR内容を確認する際の可読性のために含める。
 *   }
 * }
 */
object QrPayloadType {
    /** 服用記録(このQRを読み取ったら「服用した」として記録する) */
    const val INTAKE = 1
}

data class QrIntakeData(
    val medicineId: String,
    val mstMedicineId: String?,
    val medicineName: String
)

data class QrPayload(
    val type: Int,
    val version: Int,
    val data: QrIntakeData
)

object QrPayloadCodec {
    private const val SCHEMA_VERSION = 1

    fun encodeIntake(medicine: Medicine): String {
        val packer = MessagePack.newDefaultBufferPacker()
        packer.packMapHeader(3)

        packer.packString("type")
        packer.packInt(QrPayloadType.INTAKE)

        packer.packString("version")
        packer.packInt(SCHEMA_VERSION)

        packer.packString("data")
        packer.packMapHeader(3)
        packer.packString("medicine_id")
        packer.packString(medicine.id)
        packer.packString("mst_medicine_id")
        if (medicine.masterId != null) packer.packString(medicine.masterId) else packer.packNil()
        packer.packString("medicine_name")
        packer.packString(medicine.name)

        packer.close()
        return Base64.encodeToString(packer.toByteArray(), Base64.NO_WRAP)
    }

    fun decode(base64: String): QrPayload? {
        return try {
            val bytes = Base64.decode(base64, Base64.NO_WRAP)
            val unpacker = MessagePack.newDefaultUnpacker(bytes)

            var type = -1
            var version = -1
            var medicineId: String? = null
            var mstMedicineId: String? = null
            var medicineName: String? = null

            val rootSize = unpacker.unpackMapHeader()
            repeat(rootSize) {
                when (unpacker.unpackString()) {
                    "type" -> type = unpacker.unpackInt()
                    "version" -> version = unpacker.unpackInt()
                    "data" -> {
                        val dataSize = unpacker.unpackMapHeader()
                        repeat(dataSize) {
                            when (unpacker.unpackString()) {
                                "medicine_id" -> medicineId = unpacker.unpackString()
                                "mst_medicine_id" ->
                                    mstMedicineId = if (unpacker.tryUnpackNil()) null else unpacker.unpackString()
                                "medicine_name" -> medicineName = unpacker.unpackString()
                                else -> unpacker.skipValue()
                            }
                        }
                    }
                    else -> unpacker.skipValue()
                }
            }
            unpacker.close()

            val finalMedicineId = medicineId ?: return null
            val finalMedicineName = medicineName ?: return null
            if (type == -1) return null

            QrPayload(type, version, QrIntakeData(finalMedicineId, mstMedicineId, finalMedicineName))
        } catch (e: Exception) {
            null
        }
    }
}
