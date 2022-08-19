package io.traxa.persistence.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import androidx.room.OnConflictStrategy.REPLACE
import io.traxa.persistence.entities.ColorType
import io.traxa.persistence.entities.ContainerCapture
import io.traxa.persistence.entities.ContainerColorStat

@Dao
interface ContainerDao {

    @Query("SELECT * FROM containercapture")
    suspend fun getAll(): List<ContainerCapture>

    @Query("SELECT * FROM containercapture")
    fun getAllLiveData(): LiveData<List<ContainerCapture>>

    @Query("SELECT * FROM containercapture WHERE uid = :uid LIMIT 1")
    fun getByIdLiveData(uid: String): LiveData<List<ContainerCapture>>

    @Query("SELECT * FROM containercapture WHERE recordingId = :recordingId")
    suspend fun getByRecordingId(recordingId: Int): List<ContainerCapture>

    @Query("SELECT * FROM containercolorstat")
    suspend fun getContainerColorStats(): List<ContainerColorStat>

    @Query("SELECT * FROM containercolorstat WHERE color = :color LIMIT 1")
    suspend fun getContainerColorStatsByColor(color: ColorType): List<ContainerColorStat>

    @Query("SELECT * FROM containercolorstat")
    fun getContainerColorStatsLiveData(): LiveData<List<ContainerColorStat>>

    @Query("SELECT COUNT(uid) FROM containercapture WHERE storageType IS NOT NULL")
    suspend fun getContainerCount(): Int

    @Insert(onConflict = REPLACE)
    fun insertContainerColorStat(entity: ContainerColorStat)

    @Insert(onConflict = REPLACE)
    fun insertContainer(entity: ContainerCapture)

    @Query("DELETE FROM containercapture WHERE storageType IS NULL")
    fun deleteAllUnknowns(): Int

    @Transaction
    suspend fun incrementContainerColorStat(color: ColorType) {
        val containerColor = getContainerColorStatsByColor(color).firstOrNull() ?: return
        insertContainerColorStat(containerColor.copy(count = containerColor.count + 1))
    }

    @Update(onConflict = REPLACE)
    fun updateContainerColorStat(entity: ContainerColorStat)

    @Update(onConflict = REPLACE)
    fun updateContainer(entity: ContainerCapture)
}