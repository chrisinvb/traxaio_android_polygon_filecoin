package io.traxa.ui.settings.profile

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.traxa.services.Prefs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsProfileSectionViewModel : ViewModel(), KoinComponent {
    val prefs: Prefs by inject()

    //    val profileId = MutableLiveData(prefs.getString("player_id"))
    val profileId = MutableLiveData("Authenticate to view")
}