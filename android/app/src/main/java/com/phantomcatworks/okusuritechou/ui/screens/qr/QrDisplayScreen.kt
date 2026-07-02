package com.phantomcatworks.okusuritechou.ui.screens.qr

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import android.provider.MediaStore
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.data.qr.QrCodeGenerator
import com.phantomcatworks.okusuritechou.data.qr.QrPayloadCodec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * QRコード表示画面。
 * 薬の情報をMessagePackでエンコードしたペイロード(QrPayloadCodec参照)からQRコード画像を生成して表示し、
 * 端末への保存ができる。印刷機能は今後実装予定のためボタンをグレーアウトし「Coming soon」を表示する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrDisplayScreen(medicineId: String, onBack: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var medicine by remember { mutableStateOf<Medicine?>(null) }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(medicineId) {
        val m = Medicine.find(medicineId)
        medicine = m
        if (m != null) {
            val payload = QrPayloadCodec.encodeIntake(m)
            qrBitmap = QrCodeGenerator.generate(payload)
        }
    }

    Scaffold(topBar = { SmallTopAppBar(title = { Text("QRコード") }) }) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val m = medicine
            if (m == null) {
                Text("薬の情報が見つかりませんでした。")
            } else {
                Text(m.name)
                Text("このQRコードを薬(または袋)に貼ると、トップ画面の「QR読取」で服用記録ができます。")

                qrBitmap?.let { bmp ->
                    Image(
                        bitmap = bmp.asImageBitmap(),
                        contentDescription = "QRコード",
                        modifier = Modifier.size(240.dp).padding(vertical = 16.dp)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(onClick = {
                        val bmp = qrBitmap ?: return@Button
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                saveQrCodeToGallery(context, bmp, fileName = "okusuri_qr_${m.id}")
                            }
                            saveMessage = if (result) "画像を保存しました" else "保存に失敗しました"
                        }
                    }) {
                        Text("保存")
                    }

                    // 印刷機能は今後実装予定。ボタンはグレーアウトし押せないようにしておく。
                    Button(
                        onClick = { printQrCode(context, qrBitmap) },
                        enabled = false
                    ) {
                        Text("印刷 (Coming soon)")
                    }
                }

                saveMessage?.let { Text(it, modifier = Modifier.padding(top = 8.dp)) }
            }

            Button(onClick = onBack, modifier = Modifier.padding(top = 24.dp)) { Text("戻る") }
        }
    }
}

/**
 * QRコードの印刷機能。現状は未実装のスタブ。
 * TODO: UIプリントフレームワーク(PrintHelper / PrintManager)を使って実装する。
 */
private fun printQrCode(context: Context, bitmap: Bitmap?) {
    // TODO: 印刷処理を実装する
}

private fun saveQrCodeToGallery(context: Context, bitmap: Bitmap, fileName: String): Boolean {
    return try {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "$fileName.png")
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OkusuriTechou")
            }
        }
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values) ?: return false
        resolver.openOutputStream(uri)?.use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        } ?: return false
        true
    } catch (e: Exception) {
        false
    }
}
