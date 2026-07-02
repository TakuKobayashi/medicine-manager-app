package com.phantomcatworks.okusuritechou.ui.screens.form

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.phantomcatworks.okusuritechou.data.db.entity.DoseForm
import com.phantomcatworks.okusuritechou.data.db.entity.FrequencyType
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.widget.WidgetUpdater
import kotlinx.coroutines.launch

/**
 * 薬の登録/更新画面。
 * medicineId が null なら新規登録、非nullなら既存薬を読み込んで更新する。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun MedicineFormScreen(
    medicineId: String?,
    onSaved: () -> Unit,
    onCancel: () -> Unit
) {
    var existing by remember { mutableStateOf<Medicine?>(null) }
    var name by remember { mutableStateOf("") }
    var doseForm by remember { mutableStateOf(DoseForm.tablet) }
    var doseAmount by remember { mutableStateOf("1") }
    var frequencyType by remember { mutableStateOf(FrequencyType.daily) }
    var intervalDays by remember { mutableStateOf("2") }
    var timingMorning by remember { mutableStateOf(false) }
    var timingNoon by remember { mutableStateOf(false) }
    var timingNight by remember { mutableStateOf(false) }
    var remainingQty by remember { mutableStateOf("0") }
    var isActive by remember { mutableStateOf(true) }
    var doseFormMenuExpanded by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val isEditing = medicineId != null

    LaunchedEffect(medicineId) {
        if (medicineId != null) {
            Medicine.find(medicineId)?.let { m ->
                existing = m
                name = m.name
                doseForm = m.doseForm
                doseAmount = formatNumber(m.doseAmount)
                frequencyType = m.frequencyType
                intervalDays = (m.intervalDays ?: 2).toString()
                timingMorning = m.timingMorning
                timingNoon = m.timingNoon
                timingNight = m.timingNight
                remainingQty = formatNumber(m.remainingQty)
                isActive = m.isActive
            }
        }
    }

    fun validateAndSave() {
        val amount = doseAmount.toDoubleOrNull()
        val remaining = remainingQty.toDoubleOrNull()
        val interval = intervalDays.toIntOrNull()

        if (name.isBlank()) { errorMessage = "薬の名前を入力してください"; return }
        if (amount == null || amount <= 0) { errorMessage = "1回服用量を正しく入力してください"; return }
        if (remaining == null || remaining < 0) { errorMessage = "残量を正しく入力してください"; return }
        if (frequencyType == FrequencyType.interval && (interval == null || interval <= 0)) {
            errorMessage = "服用間隔(日数)を正しく入力してください"; return
        }
        if (!timingMorning && !timingNoon && !timingNight) {
            errorMessage = "朝・昼・晩のうち少なくとも1つを選択してください"; return
        }
        errorMessage = null

        scope.launch {
            val current = existing
            if (current != null) {
                current.copy(
                    name = name,
                    doseForm = doseForm,
                    doseAmount = amount,
                    frequencyType = frequencyType,
                    intervalDays = if (frequencyType == FrequencyType.interval) interval else null,
                    timingMorning = timingMorning,
                    timingNoon = timingNoon,
                    timingNight = timingNight,
                    remainingQty = remaining,
                    isActive = isActive
                ).save()
            } else {
                Medicine.create(
                    name = name,
                    doseForm = doseForm,
                    doseAmount = amount,
                    frequencyType = frequencyType,
                    intervalDays = if (frequencyType == FrequencyType.interval) interval else null,
                    timingMorning = timingMorning,
                    timingNoon = timingNoon,
                    timingNight = timingNight,
                    remainingQty = remaining,
                    isActive = isActive
                )
            }
            WidgetUpdater.updateAll(context)
            onSaved()
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(title = { Text(if (isEditing) "薬の更新" else "薬の登録") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("薬の名前") },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.padding(top = 12.dp))

            ExposedDropdownMenuBox(
                expanded = doseFormMenuExpanded,
                onExpandedChange = { doseFormMenuExpanded = it }
            ) {
                OutlinedTextField(
                    value = doseFormLabel(doseForm),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("剤形") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = doseFormMenuExpanded) },
                    modifier = Modifier.fillMaxWidth()
                )
                ExposedDropdownMenu(
                    expanded = doseFormMenuExpanded,
                    onDismissRequest = { doseFormMenuExpanded = false }
                ) {
                    DoseForm.values().forEach { option ->
                        DropdownMenuItem(
                            text = { Text(doseFormLabel(option)) },
                            onClick = { doseForm = option; doseFormMenuExpanded = false }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = doseAmount,
                onValueChange = { doseAmount = it },
                label = { Text("1回服用量 (${if (doseForm == DoseForm.injection) "目盛" else "錠数"})") },
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp)
            )

            Text("頻度", modifier = Modifier.padding(top = 16.dp))
            Row {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = frequencyType == FrequencyType.daily,
                        onClick = { frequencyType = FrequencyType.daily }
                    )
                    Text("毎日")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(
                        selected = frequencyType == FrequencyType.interval,
                        onClick = { frequencyType = FrequencyType.interval }
                    )
                    Text("n日に1回")
                }
            }
            if (frequencyType == FrequencyType.interval) {
                OutlinedTextField(
                    value = intervalDays,
                    onValueChange = { intervalDays = it },
                    label = { Text("何日に1回か") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("服用タイミング", modifier = Modifier.padding(top = 16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(checked = timingMorning, onCheckedChange = { timingMorning = it })
                Text("朝")
                Checkbox(checked = timingNoon, onCheckedChange = { timingNoon = it })
                Text("昼")
                Checkbox(checked = timingNight, onCheckedChange = { timingNight = it })
                Text("晩")
            }

            OutlinedTextField(
                value = remainingQty,
                onValueChange = { remainingQty = it },
                label = { Text("残量") },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("有効")
                Switch(checked = isActive, onCheckedChange = { isActive = it })
            }

            errorMessage?.let {
                Text(it, color = Color.Red, modifier = Modifier.padding(top = 8.dp))
            }

            Row(modifier = Modifier.fillMaxWidth().padding(top = 24.dp)) {
                Button(onClick = { onCancel() }) { Text("キャンセル") }
                Spacer(modifier = Modifier.padding(start = 12.dp))
                Button(onClick = { validateAndSave() }) { Text("保存") }
            }
        }
    }
}

private fun doseFormLabel(form: DoseForm): String = when (form) {
    DoseForm.tablet -> "錠剤"
    DoseForm.injection -> "注射"
    DoseForm.other -> "その他"
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
