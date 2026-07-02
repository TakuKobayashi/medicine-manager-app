package com.phantomcatworks.okusuritechou.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.phantomcatworks.okusuritechou.data.db.dao.IntakeLogDao
import com.phantomcatworks.okusuritechou.data.db.dao.MedicineDao
import com.phantomcatworks.okusuritechou.data.db.dao.MedicineMasterDao
import com.phantomcatworks.okusuritechou.data.db.dao.NotificationSettingDao
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import com.phantomcatworks.okusuritechou.data.db.entity.MedicineMaster
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting

@Database(
    entities = [Medicine::class, MedicineMaster::class, IntakeLog::class, NotificationSetting::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao
    abstract fun medicineMasterDao(): MedicineMasterDao
    abstract fun intakeLogDao(): IntakeLogDao
    abstract fun notificationSettingDao(): NotificationSettingDao

    companion object {
        @Volatile private var INSTANCE: AppDatabase? = null

        fun get(context: Context): AppDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "okusuri_techou.db"
                ).build().also { INSTANCE = it }
            }
    }
}
