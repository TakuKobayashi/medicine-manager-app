package com.phantomcatworks.okusuritechou.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phantomcatworks.okusuritechou.data.db.entity.NotificationSetting
import com.phantomcatworks.okusuritechou.data.db.entity.TimingSlot
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationSettingDao {
    @Query("SELECT * FROM notification_settings")
    fun observeAll(): Flow<List<NotificationSetting>>

    @Query("SELECT * FROM notification_settings")
    suspend fun findAll(): List<NotificationSetting>

    @Query("SELECT * FROM notification_settings WHERE slot = :slot LIMIT 1")
    suspend fun findBySlot(slot: TimingSlot): NotificationSetting?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(setting: NotificationSetting)

    @Update
    suspend fun update(setting: NotificationSetting)
}
