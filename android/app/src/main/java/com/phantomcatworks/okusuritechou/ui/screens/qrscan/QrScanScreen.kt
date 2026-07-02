package com.phantomcatworks.okusuritechou.ui.screens.qrscan

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeSource
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import com.phantomcatworks.okusuritechou.data.qr.QrPayloadCodec
import com.phantomcatworks.okusuritechou.data.qr.QrPayloadType
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * トップ画面から起動するQRスキャン画面。
 * 薬(または薬袋)に貼られたQRシールを読み取り、その場で「服用した」として記録する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScanScreen(onDone: () -> Unit, onCancel: () -> Unit) {
    val context = LocalContext.current
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasCameraPermission = granted }

    LaunchedEffect(Unit) {
        if (!hasCameraPermission) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    var foundMedicine by remember { mutableStateOf<Medicine?>(null) }
    var notFoundToken by remember { mutableStateOf<String?>(null) }
    var recordedMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { SmallTopAppBar(title = { Text("QRコード読み取り") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission && recordedMessage == null) {
                CameraQrPreview(
                    onTokenDetected = { raw ->
                        scope.launch {
                            val payload = QrPayloadCodec.decode(raw)
                            val medicine = if (payload != null && payload.type == QrPayloadType.INTAKE) {
                                Medicine.find(payload.data.medicineId)
                                    ?: Medicine.findByName(payload.data.medicineName)
                            } else {
                                null
                            }
                            if (medicine == null) {
                                notFoundToken = raw
                            } else {
                                foundMedicine = medicine
                            }
                        }
                    }
                )
            } else if (!hasCameraPermission) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("カメラの権限が必要です。")
                    Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                        Text("権限を許可する")
                    }
                    Button(onClick = onCancel) { Text("キャンセル") }
                }
            } else {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(recordedMessage ?: "")
                    Button(onClick = onDone) { Text("閉じる") }
                }
            }
        }
    }

    foundMedicine?.let { medicine ->
        AlertDialog(
            onDismissRequest = { foundMedicine = null },
            title = { Text("服用記録") },
            text = { Text("「${medicine.name}」を服用した記録を追加します。よろしいですか？") },
            confirmButton = {
                Button(onClick = {
                    scope.launch {
                        IntakeLog.record(medicine, currentSlot(), IntakeSource.qr)
                        com.phantomcatworks.okusuritechou.widget.WidgetUpdater.updateAll(context)
                        recordedMessage = "「${medicine.name}」を服用済みとして記録しました。"
                        foundMedicine = null
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                Button(onClick = { foundMedicine = null }) { Text("キャンセル") }
            }
        )
    }

    notFoundToken?.let {
        AlertDialog(
            onDismissRequest = { notFoundToken = null },
            title = { Text("見つかりません") },
            text = { Text("このQRコードに対応する薬が登録されていません。") },
            confirmButton = { Button(onClick = { notFoundToken = null }) { Text("閉じる") } }
        )
    }
}

private fun currentSlot(): TimingSlot {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> TimingSlot.morning
        hour < 18 -> TimingSlot.noon
        else -> TimingSlot.night
    }
}

@Composable
private fun CameraQrPreview(onTokenDetected: (String) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val hasResult = remember { AtomicBoolean(false) }
    val scanner = remember {
        BarcodeScanning.getClient(
            BarcodeScannerOptions.Builder()
                .setBarcodeFormats(Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            scanner.close()
        }
    }

    AndroidView(modifier = Modifier.fillMaxWidth(), factory = { previewView })

    LaunchedEffect(Unit) {
        val cameraProvider = ProcessCameraProvider.getInstance(context).get()
        val preview = Preview.Builder().build().also {
            it.setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()

        analysis.setAnalyzer(analysisExecutor) { imageProxy: ImageProxy ->
            if (hasResult.get()) {
                imageProxy.close()
                return@setAnalyzer
            }
            val mediaImage = imageProxy.image
            if (mediaImage == null) {
                imageProxy.close()
                return@setAnalyzer
            }
            val input = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
            scanner.process(input)
                .addOnSuccessListener { barcodes ->
                    val token = barcodes.firstOrNull { it.rawValue != null }?.rawValue
                    if (token != null && hasResult.compareAndSet(false, true)) {
                        onTokenDetected(token)
                    }
                }
                .addOnCompleteListener { imageProxy.close() }
        }

        cameraProvider.unbindAll()
        cameraProvider.bindToLifecycle(
            lifecycleOwner,
            CameraSelector.DEFAULT_BACK_CAMERA,
            preview,
            analysis
        )
    }
}
