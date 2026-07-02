package com.phantomcatworks.okusuritechou.ui.screens.ocr

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.compose.ui.platform.LocalLifecycleOwner
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import com.phantomcatworks.okusuritechou.data.db.entity.DoseForm
import com.phantomcatworks.okusuritechou.data.db.entity.FrequencyType
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.data.ocr.OcrTextParser
import kotlinx.coroutines.launch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * お薬シールをカメラで撮影し、自動でテキスト検出→解析→確認ダイアログに遷移する画面。
 * 「それらしい」情報(薬名・1回量・タイミングのいずれか)が揃った時点で自動的に解析を停止する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrCaptureScreen(onDone: () -> Unit, onCancel: () -> Unit) {
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

    var parsed by remember { mutableStateOf<OcrTextParser.ParsedSticker?>(null) }
    var matchedExisting by remember { mutableStateOf<Medicine?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(topBar = { SmallTopAppBar(title = { Text("シール読み取り") }) }) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (hasCameraPermission && parsed == null) {
                CameraOcrPreview(
                    onParsed = { result ->
                        scope.launch {
                            matchedExisting = result.name?.let { Medicine.findByName(it) }
                            parsed = result
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
            }
        }
    }

    parsed?.let { result ->
        OcrConfirmDialog(
            result = result,
            existing = matchedExisting,
            onCancel = {
                parsed = null
                matchedExisting = null
            },
            onConfirm = {
                scope.launch {
                    try {
                        applyOcrResult(result, matchedExisting)
                        onDone()
                    } catch (e: Exception) {
                        errorMessage = e.message
                        parsed = null
                    }
                }
            }
        )
    }

    errorMessage?.let { Text(it) }
}

private suspend fun applyOcrResult(result: OcrTextParser.ParsedSticker, existing: Medicine?) {
    val name = result.name ?: return
    if (existing != null) {
        val addQty = result.computeAddQuantity() ?: 0.0
        existing.withAddedQuantity(addQty).save()
    } else {
        Medicine.create(
            name = name,
            doseForm = OcrTextParser.guessDoseForm(null),
            doseAmount = result.doseAmount ?: 1.0,
            frequencyType = OcrTextParser.guessFrequencyType(),
            intervalDays = null,
            timingMorning = result.timingMorning,
            timingNoon = result.timingNoon,
            timingNight = result.timingNight,
            remainingQty = result.computeAddQuantity() ?: 0.0,
            isActive = true
        )
    }
}

@Composable
private fun OcrConfirmDialog(
    result: OcrTextParser.ParsedSticker,
    existing: Medicine?,
    onCancel: () -> Unit,
    onConfirm: () -> Unit
) {
    val addQty = result.computeAddQuantity()
    AlertDialog(
        onDismissRequest = onCancel,
        title = { Text(if (existing != null) "この内容で更新しますか？" else "この内容で新規登録しますか？") },
        text = {
            Column {
                Text("【Before】")
                if (existing != null) {
                    Text("${existing.name}: 残量 ${existing.remainingQty}")
                } else {
                    Text("(新規登録)")
                }
                Text("【After】", modifier = Modifier.padding(top = 8.dp))
                Text("薬名: ${result.name ?: "(不明)"}")
                Text("1回量: ${result.doseAmount ?: "(不明)"}")
                Text("タイミング: ${timingSummary(result)}")
                Text("日数: ${result.days?.let { "${it}日分" } ?: "(不明)"}")
                if (existing != null) {
                    Text("残量: ${existing.remainingQty} → ${existing.remainingQty + (addQty ?: 0.0)}")
                } else {
                    Text("残量: ${addQty ?: 0.0}")
                }
            }
        },
        confirmButton = { Button(onClick = onConfirm) { Text("OK") } },
        dismissButton = { Button(onClick = onCancel) { Text("やり直す") } }
    )
}

private fun timingSummary(result: OcrTextParser.ParsedSticker): String {
    val parts = mutableListOf<String>()
    if (result.timingMorning) parts += "朝"
    if (result.timingNoon) parts += "昼"
    if (result.timingNight) parts += "晩"
    return if (parts.isEmpty()) "(不明)" else parts.joinToString("・")
}

@Composable
private fun CameraOcrPreview(onParsed: (OcrTextParser.ParsedSticker) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember { PreviewView(context) }
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val hasResult = remember { AtomicBoolean(false) }
    val recognizer = remember {
        TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())
    }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
            recognizer.close()
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxWidth(),
        factory = { previewView }
    )

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
            recognizer.process(input)
                .addOnSuccessListener { visionText ->
                    val parsed = OcrTextParser.parse(visionText.text)
                    if (parsed.isConfident() && hasResult.compareAndSet(false, true)) {
                        onParsed(parsed)
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
