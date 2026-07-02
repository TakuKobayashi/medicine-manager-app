package com.phantomcatworks.okusuritechou.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId
import java.util.UUID

enum class IntakeSource { checkbox, qr }

@Entity(tableName = "intake_logs")
data class IntakeLog(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val medicineId: String,
    val timingSlot: TimingSlot,
    val takenAt: Long = System.currentTimeMillis(),
    val takenDate: String = LocalDate.now(ZoneId.systemDefault()).toString(),
    val source: IntakeSource = IntakeSource.checkbox,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val dao get() = AppHolder.db.intakeLogDao()

        suspend fun record(medicine: Medicine, slot: TimingSlot, source: IntakeSource = IntakeSource.checkbox): IntakeLog {
            val log = IntakeLog(medicineId = medicine.id, timingSlot = slot, source = source)
            dao.insert(log)
            return log
        }

        suspend fun forDate(date: String): List<IntakeLog> = dao.findByDate(date)
        suspend fun forMedicineAndDate(medicineId: String, date: String): List<IntakeLog> =
            dao.findByMedicineAndDate(medicineId, date)

        suspend fun isTakenInCurrentSlot(medicineId: String, slot: TimingSlot, date: String): Boolean =
            dao.findByMedicineAndDate(medicineId, date).any { it.timingSlot == slot }

        fun observeForMonth(yearMonthPrefix: String): Flow<List<IntakeLog>> =
            dao.observeByMonth("$yearMonthPrefix%")
    }
}
