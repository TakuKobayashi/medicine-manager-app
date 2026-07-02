package com.phantomcatworks.okusuritechou.ui.screens.top

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import com.phantomcatworks.okusuritechou.widget.WidgetUpdater
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import java.time.LocalDate

/**
 * トップ画面: 現在有効な薬をチェックリスト表示し、服用記録を行う最小実装。
 * 薬リスト/カレンダー/通知設定/QRスキャン画面は今後 Navigation Compose で接続する。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun TopScreen(
    onNavigateToList: () -> Unit = {},
    onNavigateToCalendar: () -> Unit = {},
    onNavigateToQrScan: () -> Unit = {},
    onNavigateToNotificationSettings: () -> Unit = {}
) {
    var medicines by remember { mutableStateOf<List<Medicine>>(emptyList()) }
    var takenMedicineIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    val today = remember { LocalDate.now().toString() }

    suspend fun reload() {
        medicines = Medicine.active()
        val taken = mutableSetOf<String>()
        medicines.forEach { m ->
            if (IntakeLog.forMedicineAndDate(m.id, today).isNotEmpty()) {
                taken += m.id
            }
        }
        takenMedicineIds = taken
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopAppBar(title = { Text("お薬手帳") })

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Button(onClick = onNavigateToList) { Text("薬リスト") }
            Button(onClick = onNavigateToCalendar) { Text("カレンダー") }
            Button(onClick = onNavigateToQrScan) { Text("QR読取") }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.End
        ) {
            Button(onClick = onNavigateToNotificationSettings) { Text("通知設定") }
        }

        if (medicines.isEmpty()) {
            Text("現在有効な薬は登録されていません。")
        }

        LazyColumn {
            items(medicines, key = { it.id }) { medicine ->
                val taken = takenMedicineIds.contains(medicine.id)
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(medicine.name)
                        Text(if (taken) "服用済み" else "未服用")
                    }
                    Checkbox(
                        checked = taken,
                        enabled = !taken,
                        onCheckedChange = { checked ->
                            if (checked) {
                                scope.launch {
                                    IntakeLog.record(medicine, currentSlot())
                                    reload()
                                    WidgetUpdater.updateAll(context)
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

/** 現在時刻から朝/昼/晩のタイミングを簡易判定する（後で通知設定の時刻と連動させる） */
private fun currentSlot(): TimingSlot {
    val hour = java.time.LocalTime.now().hour
    return when {
        hour < 12 -> TimingSlot.morning
        hour < 18 -> TimingSlot.noon
        else -> TimingSlot.night
    }
}
