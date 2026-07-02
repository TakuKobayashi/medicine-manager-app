package com.phantomcatworks.okusuritechou.widget.actions

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallbackimport com.phantomcatworks.okusuritechou.data.db.AppHolder
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeSource
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import com.phantomcatworks.okusuritechou.widget.check.CheckWidget

/**
 * 服用チェックウィジェットのタップ操作で服用記録を作成し、ウィジェット表示を更新する。
 */
class ToggleIntakeAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        AppHolder.init(context.applicationContext as android.app.Application)
        val medicineId = parameters[KEY_MEDICINE_ID] ?: return
        val medicine = Medicine.find(medicineId) ?: return

        val today = java.time.LocalDate.now().toString()
        val slot = currentSlot()
        val already = IntakeLog.isTakenInCurrentSlot(medicine.id, slot, today)
        if (!already) {
            IntakeLog.record(medicine, slot, IntakeSource.checkbox)
        }

        CheckWidget().update(context, glanceId)
    }

    private fun currentSlot(): TimingSlot {
        val hour = java.time.LocalTime.now().hour
        return when {
            hour < 12 -> TimingSlot.morning
            hour < 18 -> TimingSlot.noon
            else -> TimingSlot.night
        }
    }

    companion object {
        val KEY_MEDICINE_ID = ActionParameters.Key<String>("medicine_id")
    }
}
