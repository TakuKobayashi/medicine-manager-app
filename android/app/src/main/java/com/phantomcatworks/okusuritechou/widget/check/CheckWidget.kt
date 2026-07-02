package com.phantomcatworks.okusuritechou.widget.check

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.widget.actions.ToggleIntakeAction
import java.time.LocalDate

/**
 * 服用チェックウィジェット。
 * 現在有効な薬を一覧表示し、タップすると即座に「服用した」記録を作成する。
 * 服用済みのものは表記を変えてタップ不可にする(完了であることが伝わるように)。
 */
class CheckWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppHolder.init(context.applicationContext as android.app.Application)

        val today = LocalDate.now().toString()
        val medicines = Medicine.active()
        val takenIds = medicines
            .filter { IntakeLog.forMedicineAndDate(it.id, today).isNotEmpty() }
            .map { it.id }
            .toSet()

        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
                Text("お薬チェック", style = TextStyle(fontWeight = FontWeight.Bold))

                if (medicines.isEmpty()) {
                    Text("有効な薬はありません")
                } else {
                    LazyColumn {
                        items(medicines, itemId = { it.id.hashCode().toLong() }) { medicine ->
                            val taken = takenIds.contains(medicine.id)
                            val rowModifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)
                            Row(
                                modifier = if (!taken) {
                                    rowModifier.clickable(
                                        actionRunCallback<ToggleIntakeAction>(
                                            actionParametersOf(ToggleIntakeAction.KEY_MEDICINE_ID to medicine.id)
                                        )
                                    )
                                } else {
                                    rowModifier
                                }
                            ) {
                                Text(if (taken) "✓ " else "☐ ")
                                Text(medicine.name + if (taken) "（服用済み）" else "")
                            }
                        }
                    }
                }
            }
        }
    }
}
