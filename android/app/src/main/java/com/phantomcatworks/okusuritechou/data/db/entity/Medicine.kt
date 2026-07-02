package com.phantomcatworks.okusuritechou.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import com.phantomcatworks.okusuritechou.data.db.dao.MedicineDao
import kotlinx.coroutines.flow.Flow
import java.util.UUID

enum class DoseForm { tablet, injection, other }
enum class FrequencyType { daily, interval }
enum class TimingSlot { morning, noon, night }

@Entity(tableName = "medicines")
data class Medicine(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val masterId: String? = null,
    val name: String,
    val doseForm: DoseForm = DoseForm.tablet,
    val doseAmount: Double,
    val frequencyType: FrequencyType = FrequencyType.daily,
    val intervalDays: Int? = null,
    val timingMorning: Boolean = false,
    val timingNoon: Boolean = false,
    val timingNight: Boolean = false,
    val remainingQty: Double,
    val isActive: Boolean = true,
    /** レガシーフィールド。QRコードの内容は現在 QrPayloadCodec(MessagePack)が直接 id を埋め込むため未使用。
     *  一意な外部識別子として将来的に再利用する可能性があるため残してある。 */
    val qrToken: String = UUID.randomUUID().toString(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isDueOn(epochDay: Long, registeredEpochDay: Long): Boolean {
        if (!isActive) return false
        return when (frequencyType) {
            FrequencyType.daily -> true
            FrequencyType.interval -> {
                val n = intervalDays ?: 1
                if (n <= 0) true else ((epochDay - registeredEpochDay) % n) == 0L
            }
        }
    }

    fun requiredTimingsCount(): Int =
        listOf(timingMorning, timingNoon, timingNight).count { it }

    suspend fun save(): Medicine {
        val dao = AppHolder.db.medicineDao()
        val updated = copy(updatedAt = System.currentTimeMillis())
        dao.upsert(updated)
        return updated
    }

    suspend fun destroy() {
        AppHolder.db.medicineDao().delete(this)
    }

    fun withAddedQuantity(addQty: Double): Medicine =
        copy(remainingQty = remainingQty + addQty, updatedAt = System.currentTimeMillis())

    companion object {
        private val dao get() = AppHolder.db.medicineDao()

        fun observeAll(): Flow<List<Medicine>> = dao.observeAll()
        fun observeActive(): Flow<List<Medicine>> = dao.observeActive()

        suspend fun all(): List<Medicine> = dao.findAll()
        suspend fun active(): List<Medicine> = dao.findActive()
        suspend fun find(id: String): Medicine? = dao.findById(id)
        suspend fun findByQrToken(token: String): Medicine? = dao.findByQrToken(token)
        suspend fun findByName(name: String): Medicine? = dao.findByName(name)

        suspend fun create(
            name: String,
            doseForm: DoseForm,
            doseAmount: Double,
            frequencyType: FrequencyType,
            intervalDays: Int?,
            timingMorning: Boolean,
            timingNoon: Boolean,
            timingNight: Boolean,
            remainingQty: Double,
            isActive: Boolean = true,
            masterId: String? = null
        ): Medicine {
            val m = Medicine(
                masterId = masterId,
                name = name,
                doseForm = doseForm,
                doseAmount = doseAmount,
                frequencyType = frequencyType,
                intervalDays = intervalDays,
                timingMorning = timingMorning,
                timingNoon = timingNoon,
                timingNight = timingNight,
                remainingQty = remainingQty,
                isActive = isActive
            )
            dao.upsert(m)
            return m
        }
    }
}
