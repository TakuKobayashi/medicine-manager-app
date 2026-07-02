package com.phantomcatworks.okusuritechou.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import java.util.Calendar

/**
 * 通知設定(NotificationSetting)に基づいてAlarmManagerへ正確な時刻のアラームを登録/解除する。
 * 実際の通知発火・次回分の再スケジュール・「該当薬なし」判定はAlarmReceiver側(発火時点)で行う。
 * これにより、設定変更後すぐ・端末再起動後(BootReceiver)どちらでも正しい時刻に通知できる。
 */
object NotificationScheduler {

    fun scheduleAll(context: Context, settings: List<NotificationSetting>) {
        settings.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, setting: NotificationSetting) {
        cancel(context, setting.slot)
        if (!setting.enabled) return

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerTimeMillis(setting.hour, setting.minute)

        val pendingIntent = buildPendingIntent(context, setting.slot)
        try {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        } catch (e: SecurityException) {
            // SCHEDULE_EXACT_ALARM が拒否されている端末向けのフォールバック
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    fun cancel(context: Context, slot: TimingSlot) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(buildPendingIntent(context, slot))
    }

    /** AlarmReceiver側から、通知発火後に翌日分を再登録するために呼ぶ */
    fun scheduleNextDay(context: Context, setting: NotificationSetting) {
        if (!setting.enabled) return
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val triggerAt = nextTriggerTimeMillis(setting.hour, setting.minute, forceTomorrow = true)
        val pendingIntent = buildPendingIntent(context, setting.slot)
        try {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        } catch (e: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAt, pendingIntent)
        }
    }

    private fun buildPendingIntent(context: Context, slot: TimingSlot): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_SLOT, slot.name)
        }
        return PendingIntent.getBroadcast(
            context,
            slot.ordinal,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextTriggerTimeMillis(hour: Int, minute: Int, forceTomorrow: Boolean = false): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (forceTomorrow || !target.after(now)) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }
}
