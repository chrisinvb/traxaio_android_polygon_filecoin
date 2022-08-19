package io.traxa

import android.app.Application
import android.util.Log
import io.traxa.models.AwsConfiguration
import io.traxa.modules.androidModule
import io.traxa.modules.networkModule
import io.traxa.modules.persistenceModule
import io.traxa.modules.processingModule
import io.traxa.persistence.AppDatabase
import io.traxa.repositories.PlayerTokenRepository
import io.traxa.services.Prefs
import io.traxa.services.network.AwsService
import io.traxa.services.network.StardustService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import java.util.*

class App : Application() {

    private val awsService: AwsService by inject()
    private val db: AppDatabase by inject()
    private val tokenRepository: PlayerTokenRepository by inject()
    private val stardustService: StardustService by inject()
    private val prefs: Prefs by inject()


    override fun onCreate() {
        super.onCreate()

        startKoin {
            androidContext(this@App)
            modules(
                processingModule,
                networkModule,
                persistenceModule,
                androidModule
            )
        }

        awsService.setup(AwsConfiguration(
            BuildConfig.awsRegion,
            BuildConfig.awsBucketName,
            BuildConfig.awsAccessKey,
            BuildConfig.awsSecretKey
        ))

        CoroutineScope(Dispatchers.IO).launch {
            AppDatabase.initialize(db)

            //Update tokens
            if(prefs.getPlayerId() != null){
                Log.d("App", "PlayerId: " + prefs.getPlayerId())
                tokenRepository.getPlayerTokens()
            }
            else {
                val uuid = prefs.getPlayerUUID() ?: UUID.randomUUID().toString().also {
                    prefs.setPlayerUUID(it)
                }

                val playerId = stardustService.createPlayer(uuid)
                if(playerId != null) prefs.setPlayerId(playerId)
            }
        }
    }
}