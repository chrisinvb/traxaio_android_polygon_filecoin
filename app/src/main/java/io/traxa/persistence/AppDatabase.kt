package io.traxa.persistence

import androidx.room.*
import io.traxa.persistence.daos.CaptureFileDao
import io.traxa.persistence.daos.ContainerDao
import io.traxa.persistence.daos.RecordingDao
import io.traxa.persistence.entities.*
import io.traxa.utils.Converters

@Database(
    entities = [
        Recording::class,
        CaptureFile::class,
        ContainerCapture::class,
        ContainerColorStat::class
    ],
    exportSchema = true,
    autoMigrations = [AutoMigration(from = 1, to = 2)],
    version = 2
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recordingDao(): RecordingDao
    abstract fun captureFileDao(): CaptureFileDao
    abstract fun containerDao(): ContainerDao

    companion object {

        val openHelper: RoomOpenHelper
            get() = openHelper

        suspend fun initialize(appDatabase: AppDatabase) {
            val containerDao = appDatabase.containerDao()
            val isStatsEmpty = containerDao.getContainerColorStats().isEmpty()
            if(isStatsEmpty) ColorType.values().forEach {
                containerDao.insertContainerColorStat(ContainerColorStat(it, 0))
            }
        }

    }
}