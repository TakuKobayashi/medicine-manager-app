package com.phantomcatworks.okusuritechou.data.db

import android.app.Application
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting
import com.phantomcatworks.okusuritechou.notification.NotificationChannels
import com.phantomcatworks.okusuritechou.notification.NotificationScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

object AppHolder {
    lateinit var db: AppDatabase
        private set

    fun init(app: Application) {
        if (!::db.isInitialized) {
            db = AppDatabase.get(app)
        }
    }
}

class OkusuriTechouApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppHolder.init(this)
        NotificationChannels.ensureCreated(this)
        CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
            NotificationSetting.ensureDefaults()
            // アプリ起動のたびに最新の設定値で再スケジュールしておく(設定変更の取りこぼし対策)
            NotificationScheduler.scheduleAll(this@OkusuriTechouApplication, NotificationSetting.all())
        }
    }
}
