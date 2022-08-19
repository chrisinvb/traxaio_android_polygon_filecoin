package io.traxa.persistence.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import io.traxa.persistence.entities.Recording

@Dao
interface RecordingDao {

    @Query("SELECT * FROM recording")
    suspend fun getAll(): List<Recording>

    @Query("SELECT * FROM recording")
    fun getAllLiveData(): LiveData<List<Recording>>

    @Query("SELECT * FROM recording WHERE uid IN (:ids)")
    fun loadAllByIdsLiveData(vararg ids: Int): LiveData<List<Recording>>

    @Query("SELECT * FROM recording WHERE uid IN (:ids)")
    suspend fun loadAllByIds(vararg ids: Int): List<Recording>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(vararg entities: Recording): List<Long>

    @Update
    fun update(entity: Recording)

    @Delete
    fun delete(entity: Recording)

    @Query("DELETE FROM recording")
    fun deleteAll()
}