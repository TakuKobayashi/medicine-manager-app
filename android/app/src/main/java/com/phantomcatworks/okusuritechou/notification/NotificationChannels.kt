package com.phantomcatworks.okusuritechou.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {
    const val MEDICATION_CHANNEL_ID = "medication_reminders"

    fun ensureCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(NotificationManager::class.java)
            val channel = NotificationChannel(
                MEDICATION_CHANNEL_ID,
                "お薬の服用通知",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "朝・昼・晩の服用タイミングをお知らせします"
            }
            manager.createNotificationChannel(channel)
        }
    }
}
