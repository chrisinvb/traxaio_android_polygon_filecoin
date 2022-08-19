package io.traxa.ui.startup

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import dispatch.core.ioDispatcher
import io.traxa.persistence.AppDatabase
import io.traxa.services.Prefs
import io.traxa.ui.main.MainActivity
import io.traxa.ui.onboard.OnboardActivity
import io.traxa.ui.upload.UploadActivity
import io.traxa.utils.Constants
import io.traxa.utils.MeanColorClassifier
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import kotlin.math.pow

class StartupActivity : Activity() {

    private val prefs: Prefs by inject()
    private val db: AppDatabase by inject()
    private val containerDao = db.containerDao()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val isFirstTimeOpened = prefs.isFirstTimeOpened()
        var waitingTime = Constants.waitingTime

        CoroutineScope(Dispatchers.Main).launch {

            val count = withContext(ioDispatcher) { containerDao.getContainerCount() }
            val rewardNumber = 5 * (count/5)
            if(rewardNumber != 0) {
                val timeReward = Constants.timeRewards[rewardNumber] ?: Constants.maxTimeReward
                println("Time reward: $timeReward")
                waitingTime -= (timeReward / 60f)
            }

            MeanColorClassifier.init()
            delay(1000)

            if(isFirstTimeOpened) {
                startActivity(Intent(this@StartupActivity, OnboardActivity::class.java))
                finish()
            }else {
                val currentTime = System.currentTimeMillis() / 1000
                val elapsed = currentTime - prefs.getUploadStartTime()

                var defaultIntent = Intent(this@StartupActivity, MainActivity::class.java)
                if (elapsed < 60 * waitingTime)
                    defaultIntent = Intent(this@StartupActivity, UploadActivity::class.java)
                else
                    prefs.clearUploadStartTime()

                startActivity(defaultIntent)
                finish()
            }

        }

    }
}