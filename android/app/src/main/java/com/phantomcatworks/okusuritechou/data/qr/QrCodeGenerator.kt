package com.phantomcatworks.okusuritechou.data.qr

import android.graphics.Bitmap
import android.graphics.Color
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

/**
 * QRコードのBitmap画像を生成する汎用ユーティリティ。
 * 実際にQRコードへ埋め込む内容は QrPayloadCodec.encodeIntake() で生成したBase64文字列を渡す。
 */
object QrCodeGenerator {
    fun generate(content: String, sizePx: Int = 512): Bitmap {
        val writer = QRCodeWriter()
        val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, sizePx, sizePx)
        val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.RGB_565)
        for (x in 0 until sizePx) {
            for (y in 0 until sizePx) {
                bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
            }
        }
        return bitmap
    }
}
