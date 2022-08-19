package io.traxa.repositories

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import io.traxa.models.Token
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.ColorType
import io.traxa.persistence.entities.ContainerCapture
import io.traxa.services.Prefs
import io.traxa.services.network.StardustService
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.lang.Exception
import java.util.*
import kotlin.math.abs
import kotlin.math.max


class PlayerTokenRepository(private val context: Context) : KoinComponent {

    private val prefs: Prefs by inject()
    private val playerId = prefs.getPlayerId()

    private val db: AppDatabase by inject()
    private val containerDao = db.containerDao()
    private val recordingDao = db.recordingDao()
    private val stardustService: StardustService by inject()

    suspend fun getPlayerTokens(): List<Token> {
        if(playerId == null) return emptyList()

        val tokens = stardustService.getPlayerInventory(playerId)
        val oneAndHalfDay = (1000 * 60 * 60 * 36)

        val newTokens = arrayListOf<Token>()
        tokens.forEach {
            val container = containerDao
                .getByRecordingId(it.captureKey.toInt())
                .lastOrNull { container -> abs(container.timestamp - it.timestamp) < oneAndHalfDay }

            if(container != null && !container.isMinted() ) {
                container.apply {
                    storageType = it.storageType
                    storageLink = it.storageLink
                    containerIds = it.containerIds
                    containerPositions = it.containerPositions
                    containerType = it.containerTypes
                    mintTimestamp = it.timestamp
                }

                containerDao.insertContainer(container)
                containerDao.incrementContainerColorStat(container.color)
            } else if (container == null) {
                newTokens.add(it)

                containerDao.insertContainer(
                    ContainerCapture(
                        UUID.randomUUID().toString(),
                        it.captureKey.toInt(),
                        it.timestamp,
                        ColorType.RED,
                        it.storageType,
                        it.storageLink,
                        it.containerIds,
                        it.containerPositions,
                        it.containerTypes,
                        it.timestamp
                ))

                containerDao.incrementContainerColorStat(ColorType.RED)
            }
        }


        try {
            val seqStartTokens = newTokens.maxOfOrNull { it.captureKey.toInt() }?.plus(1) ?: 0
            val seqStartLocal: Int = recordingDao.getAll().maxByOrNull { it.uid }?.uid?.plus(1) ?: 0

            changeSequenceNumber(context, max(seqStartLocal, seqStartTokens), "Recording")
        }catch (e: Exception) {

        }

        return tokens
    }

    private fun changeSequenceNumber(context: Context,
                      seqStart: Int = 0,
                      tableName: String?) {
        val database: SQLiteDatabase = SQLiteDatabase.openOrCreateDatabase(
            context.getDatabasePath("traxa"),
            null
        )

        if (database != null) {
            database.execSQL(
                "UPDATE sqlite_sequence SET seq = $seqStart WHERE name = ?;",
                arrayOf(tableName)
            )

            database.close()
        }
    }




}