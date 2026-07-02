package com.phantomcatworks.okusuritechou.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phantomcatworks.okusuritechou.data.db.entity.IntakeLog
import kotlinx.coroutines.flow.Flow

@Dao
interface IntakeLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: IntakeLog)

    @Query("SELECT * FROM intake_logs WHERE takenDate = :date")
    suspend fun findByDate(date: String): List<IntakeLog>

    @Query("SELECT * FROM intake_logs WHERE medicineId = :medicineId AND takenDate = :date")
    suspend fun findByMedicineAndDate(medicineId: String, date: String): List<IntakeLog>

    @Query("SELECT * FROM intake_logs WHERE takenDate LIKE :likePattern")
    fun observeByMonth(likePattern: String): Flow<List<IntakeLog>>
}
