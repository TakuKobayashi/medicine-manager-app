package com.phantomcatworks.okusuritechou.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.phantomcatworks.okusuritechou.data.db.AppHolder
import java.util.UUID

@Entity(tableName = "medicine_master")
data class MedicineMaster(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val genericName: String? = null,
    val manufacturer: String? = null,
    val defaultUnit: String? = null,
    val source: String? = null,
    val syncedAt: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        private val dao get() = AppHolder.db.medicineMasterDao()

        suspend fun all(): List<MedicineMaster> = dao.findAll()
        suspend fun find(id: String): MedicineMaster? = dao.findById(id)
        suspend fun findByName(name: String): MedicineMaster? = dao.findByName(name)

        suspend fun findOrCreateByName(name: String): MedicineMaster {
            findByName(name)?.let { return it }
            val m = MedicineMaster(name = name, source = "local_ocr")
            dao.upsert(m)
            return m
        }
    }
}
