package com.phantomcatworks.okusuritechou.ui.screens.notification

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import com.phantomcatworks.okusuritechou.notification.NotificationScheduler
import kotlinx.coroutines.launch

private data class SlotUiState(
    val setting: NotificationSetting,
    val hasNoApplicableMedicine: Boolean
)

/**
 * 通知設定画面。
 * 朝・昼・晩それぞれの通知時刻・文言・有効/無効を編集できる。
 * 現在有効な薬の中にそのタイミングに該当するものが無い場合はグレーアウト表示するが、選択自体は可能。
 * 保存時にAlarmManagerへ即座に反映する。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationSettingScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var slotStates by remember { mutableStateOf<List<SlotUiState>>(emptyList()) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        val settings = NotificationSetting.all().sortedBy { it.slot.ordinal }
        slotStates = settings.map { setting ->
            SlotUiState(setting, setting.hasNoApplicableMedicine())
        }
    }

    LaunchedEffect(Unit) { reload() }

    Scaffold(topBar = { SmallTopAppBar(title = { Text("通知設定") }) }) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            slotStates.forEach { state ->
                SlotEditor(
                    state = state,
                    onChanged = { updated ->
                        scope.launch {
                            updated.save()
                            NotificationScheduler.schedule(context, updated)
                            reload()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun SlotEditor(state: SlotUiState, onChanged: (NotificationSetting) -> Unit) {
    val context = LocalContext.current
    val setting = state.setting
    val grayedOut = state.hasNoApplicableMedicine
    var message by remember(setting.slot) { mutableStateOf(setting.message) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .background(if (grayedOut) Color(0xFFF0F0F0) else Color.Transparent)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(slotLabel(setting.slot), modifier = Modifier.weight(1f))
                Switch(
                    checked = setting.enabled,
                    onCheckedChange = { checked -> onChanged(setting.copy(enabled = checked)) }
                )
            }

            if (grayedOut) {
                Text(
                    "現在有効な薬の中に該当タイミングのものがないため通知されません",
                    color = Color.Gray
                )
            }

            TextButton(onClick = {
                TimePickerDialog(
                    context,
                    { _, hour, minute ->
                        onChanged(setting.copy(hour = hour, minute = minute))
                    },
                    setting.hour,
                    setting.minute,
                    true
                ).show()
            }) {
                Text("時刻: %02d:%02d".format(setting.hour, setting.minute))
            }

            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("通知文") },
                modifier = Modifier.fillMaxWidth()
            )
            TextButton(onClick = { onChanged(setting.copy(message = message)) }) {
                Text("文言を保存")
            }
        }
    }
}

private fun slotLabel(slot: TimingSlot): String = when (slot) {
    TimingSlot.morning -> "朝"
    TimingSlot.noon -> "昼"
    TimingSlot.night -> "晩"
}
