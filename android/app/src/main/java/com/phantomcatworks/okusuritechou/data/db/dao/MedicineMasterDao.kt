package com.phantomcatworks.okusuritechou.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.phantomcatworks.okusuritechou.data.db.entity.MedicineMaster

@Dao
interface MedicineMasterDao {
    @Query("SELECT * FROM medicine_master ORDER BY name")
    suspend fun findAll(): List<MedicineMaster>

    @Query("SELECT * FROM medicine_master WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): MedicineMaster?

    @Query("SELECT * FROM medicine_master WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): MedicineMaster?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(master: MedicineMaster)
}
