package com.phantomcatworks.okusuritechou.widget.list

import android.content.Context
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Column
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import com.phantomcatworks.okusuritechou.data.db.entity.DoseForm
import com.phantomcatworks.okusuritechou.data.db.entity.FrequencyType
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine

/**
 * 薬リストウィジェット。
 * 登録されている薬(有効/無効問わず)を簡易表示する読み取り専用ウィジェット。
 */
class ListWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        AppHolder.init(context.applicationContext as android.app.Application)
        val medicines = Medicine.all()

        provideContent {
            Column(modifier = GlanceModifier.fillMaxSize().padding(8.dp)) {
                Text("薬リスト", style = TextStyle(fontWeight = FontWeight.Bold))

                if (medicines.isEmpty()) {
                    Text("登録されている薬はありません")
                } else {
                    LazyColumn {
                        items(medicines, itemId = { it.id.hashCode().toLong() }) { medicine ->
                            Column(modifier = GlanceModifier.padding(vertical = 4.dp)) {
                                Text(
                                    medicine.name + if (!medicine.isActive) "（無効）" else "",
                                )
                                Text(summary(medicine), style = TextStyle(fontWeight = FontWeight.Normal))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun summary(m: Medicine): String {
        val freq = when (m.frequencyType) {
            FrequencyType.daily -> "毎日"
            FrequencyType.interval -> "${m.intervalDays ?: 1}日に1回"
        }
        val unit = when (m.doseForm) {
            DoseForm.tablet -> "錠"
            DoseForm.injection -> "目盛"
            DoseForm.other -> "単位"
        }
        return "$freq / 残量${formatAmount(m.remainingQty)}$unit"
    }

    private fun formatAmount(value: Double): String =
        if (value == value.toLong().toDouble()) value.toLong().toString() else value.toString()
}
