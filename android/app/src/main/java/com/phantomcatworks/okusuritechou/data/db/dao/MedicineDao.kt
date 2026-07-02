package com.phantomcatworks.okusuritechou.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.phantomcatworks.okusuritechou.data.db.entity.Medicine
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY createdAt DESC")
    fun observeActive(): Flow<List<Medicine>>

    @Query("SELECT * FROM medicines ORDER BY createdAt DESC")
    suspend fun findAll(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE isActive = 1 ORDER BY createdAt DESC")
    suspend fun findActive(): List<Medicine>

    @Query("SELECT * FROM medicines WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): Medicine?

    @Query("SELECT * FROM medicines WHERE qrToken = :token LIMIT 1")
    suspend fun findByQrToken(token: String): Medicine?

    @Query("SELECT * FROM medicines WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): Medicine?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(medicine: Medicine)

    @Update
    suspend fun update(medicine: Medicine)

    @Delete
    suspend fun delete(medicine: Medicine)
}
