package com.phantomcatworks.okusuritechou.ui.screens.list

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phantomcatworks.okusuritechou.data.db.entity.DoseForm
import com.phantomcatworks.okusuritechou.data.db.entity.FrequencyType
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.widget.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * 薬リスト画面。
 * - 新規登録(FAB)
 * - 各行: 更新/削除(確認ダイアログ)/QRコード(スタブ。QR画面は今後実装)
 * - 有効/無効をその場でトグル
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MedicineListScreen(
    onNavigateToForm: (medicineId: String?) -> Unit,
    onNavigateToQrDisplay: (medicineId: String) -> Unit,
    onNavigateToOcrCapture: () -> Unit,
    onBack: () -> Unit
) {
    var medicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var pendingDelete by remember { mutableStateOf<Medicine?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    suspend fun reload() {
        medicines = Medicine.all()
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = { Text("薬リスト") },
                actions = {
                    IconButton(onClick = onNavigateToOcrCapture) {
                        Icon(
                            androidx.compose.material.icons.Icons.Default.CameraAlt,
                            contentDescription = "シール読み取り"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onNavigateToForm(null) }) {
                Icon(Icons.Default.Add, contentDescription = "新規登録")
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp)) {
            items(medicines, key = { it.id }) { medicine ->
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(medicine.name, style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                            Switch(
                                checked = medicine.isActive,
                                onCheckedChange = { checked ->
                                    scope.launch {
                                        medicine.copy(isActive = checked).save()
                                        reload()
                                        WidgetUpdater.updateAll(context)
                                    }
                                }
                            )
                        }
                        Text(frequencyLabel(medicine))
                        Text(timingLabel(medicine))
                        Text("1回量: ${formatAmount(medicine.doseAmount)} ${unitLabel(medicine.doseForm)}")
                        Text("残量: ${formatAmount(medicine.remainingQty)} ${unitLabel(medicine.doseForm)}")

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            IconButton(onClick = { onNavigateToQrDisplay(medicine.id) }) {
                                Icon(Icons.Default.QrCode, contentDescription = "QRコード")
                            }
                            IconButton(onClick = { onNavigateToForm(medicine.id) }) {
                                Icon(Icons.Default.Edit, contentDescription = "更新")
                            }
                            IconButton(onClick = { pendingDelete = medicine }) {
                                Icon(Icons.Default.Delete, contentDescription = "削除")
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("削除確認") },
            text = { Text("「${target.name}」を削除します。よろしいですか？") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        target.destroy()
                        pendingDelete = null
                        reload()
                        WidgetUpdater.updateAll(context)
                    }
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("キャンセル") }
            }
        )
    }
}

private fun frequencyLabel(m: Medicine): String = when (m.frequencyType) {
    FrequencyType.daily -> "毎日"
    FrequencyType.interval -> "${m.intervalDays ?: 1}日に1回"
}

private fun timingLabel(m: Medicine): String {
    val parts = mutableListOf<String>()
    if (m.timingMorning) parts += "朝"
    if (m.timingNoon) parts += "昼"
    if (m.timingNight) parts += "晩"
    return if (parts.isEmpty()) "タイミング未設定" else parts.joinToString("・")
}

private fun unitLabel(form: DoseForm): String = when (form) {
    DoseForm.tablet -> "錠"
    DoseForm.injection -> "目盛"
    DoseForm.other -> "単位"
}

private fun formatAmount(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
