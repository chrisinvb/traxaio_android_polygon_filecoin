package io.traxa.ui.containers.list

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.traxa.persistence.AppDatabase
import io.traxa.services.Prefs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class ContainerListViewModel : ViewModel(), KoinComponent {

    private val db : AppDatabase by inject()
    private val prefs: Prefs by inject()
    private val containerDao = db.containerDao()

    var orderType = MutableLiveData(prefs.getContainerYardOrder())
    val containers = containerDao.getAllLiveData()
}