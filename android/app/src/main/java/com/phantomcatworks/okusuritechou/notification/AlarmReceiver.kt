package com.phantomcatworks.okusuritechou.notification

import android.app.Application
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.phantomcatworks.okusuritechou.MainActivity
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * AlarmManagerから設定時刻に呼ばれるレシーバー。
 * - 通知対象(該当タイミングで服用すべき有効な薬)が無い場合は通知を出さない
 * - 通知後、翌日分のアラームを再登録する(繰り返し通知の実現)
 * 端末再起動後はBootReceiverが全件再登録するため、ここでは「次の1回」だけ管理すればよい。
 */
class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotName = intent.getStringExtra(EXTRA_SLOT) ?: return
        val slot = runCatching { TimingSlot.valueOf(slotName) }.getOrNull() ?: return

        // プロセスがアラームのためだけに起動された場合に備えてDBを確実に初期化する
        ensureDbInitialized(context)

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val setting = NotificationSetting.find(slot)
                if (setting != null && setting.enabled) {
                    val noApplicable = setting.hasNoApplicableMedicine()
                    if (!noApplicable) {
                        showNotification(context, slot, setting.message)
                    }
                    NotificationScheduler.scheduleNextDay(context, setting)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun ensureDbInitialized(context: Context) {
        if (!dbInitialized) {
            synchronized(this) {
                if (!dbInitialized) {
                    AppHolder.init(context.applicationContext as Application)
                    dbInitialized = true
                }
            }
        }
    }

    private fun showNotification(context: Context, slot: TimingSlot, message: String) {
        NotificationChannels.ensureCreated(context)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            context, slot.ordinal, openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, NotificationChannels.MEDICATION_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("お薬手帳")
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentIntent)
            .build()

        NotificationManagerCompat.from(context).notify(slot.ordinal, notification)
    }

    companion object {
        const val EXTRA_SLOT = "extra_slot"
        @Volatile private var dbInitialized = false
    }
}
