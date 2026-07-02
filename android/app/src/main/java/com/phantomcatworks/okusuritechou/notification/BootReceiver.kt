package com.phantomcatworks.okusuritechou.notification

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** 端末再起動(BOOT_COMPLETED)後、AlarmManagerの登録は消えるため全件再登録する。 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        AppHolder.init(context.applicationContext as Application)
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                NotificationSetting.ensureDefaults()
                val settings = NotificationSetting.all()
                NotificationScheduler.scheduleAll(context, settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
