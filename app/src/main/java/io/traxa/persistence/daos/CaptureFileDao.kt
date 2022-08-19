package io.traxa.persistence.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import io.traxa.persistence.entities.CaptureFile

@Dao
interface CaptureFileDao {

    @Query("SELECT * FROM capturefile")
    fun getAll(): LiveData<List<CaptureFile>>

    @Query("SELECT * FROM capturefile WHERE recordingId = :recordingId")
    fun loadAllByRecordingIdLiveData(recordingId: Int): LiveData<List<CaptureFile>>

    @Query("SELECT * FROM capturefile WHERE recordingId = :recordingId")
    suspend fun loadAllByRecordingId(recordingId: Int): List<CaptureFile>

    @Query("SELECT * FROM capturefile WHERE uid IN (:ids)")
    fun loadAllByIdsLiveData(vararg ids: Int): LiveData<List<CaptureFile>>


    @Query("SELECT * FROM capturefile WHERE uid IN (:ids)")
    suspend fun loadAllByIds(vararg ids: Int): List<CaptureFile>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entities: CaptureFile): List<Long>

    @Update
    fun update(entity: CaptureFile)

    @Delete
    fun delete(entity: CaptureFile)

    @Query("DELETE FROM capturefile")
    fun deleteAll()
}