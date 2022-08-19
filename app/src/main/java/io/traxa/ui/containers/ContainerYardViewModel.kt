package io.traxa.ui.containers

import androidx.lifecycle.ViewModel
import io.traxa.persistence.AppDatabase
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ContainerYardViewModel : ViewModel(), KoinComponent {

    private val db: AppDatabase by inject()
    private val containerDao = db.containerDao()

    val containerColorStats = containerDao.getContainerColorStatsLiveData()

}