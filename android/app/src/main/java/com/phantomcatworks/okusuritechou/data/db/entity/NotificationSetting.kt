package com.phantomcatworks.okusuritechou.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "notification_settings")
data class NotificationSetting(
    @PrimaryKey val slot: TimingSlot,
    val hour: Int,
    val minute: Int,
    val message: String,
    val enabled: Boolean = true
) {
    suspend fun hasNoApplicableMedicine(): Boolean {
        val actives = Medicine.active()
        return actives.none {
            when (slot) {
                TimingSlot.morning -> it.timingMorning
                TimingSlot.noon -> it.timingNoon
                TimingSlot.night -> it.timingNight
            }
        }
    }

    suspend fun save(): NotificationSetting {
        AppHolder.db.notificationSettingDao().upsert(this)
        return this
    }

    companion object {
        private val dao get() = AppHolder.db.notificationSettingDao()

        val DEFAULTS = listOf(
            NotificationSetting(TimingSlot.morning, 8, 0, "朝のお薬の時間です。お忘れなく服用しましょう。"),
            NotificationSetting(TimingSlot.noon, 12, 0, "昼のお薬の時間です。お忘れなく服用しましょう。"),
            NotificationSetting(TimingSlot.night, 18, 0, "夜のお薬の時間です。お忘れなく服用しましょう。")
        )

        fun observeAll(): Flow<List<NotificationSetting>> = dao.observeAll()
        suspend fun all(): List<NotificationSetting> = dao.findAll()
        suspend fun find(slot: TimingSlot): NotificationSetting? = dao.findBySlot(slot)

        suspend fun ensureDefaults() {
            DEFAULTS.forEach { default ->
                if (dao.findBySlot(default.slot) == null) {
                    dao.upsert(default)
                }
            }
        }
    }
}
