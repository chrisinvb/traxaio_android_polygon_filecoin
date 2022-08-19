package io.traxa.ui.settings.wallets

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import io.traxa.services.Prefs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class SettingsWalletsSectionViewModel(app: Application) : AndroidViewModel(app), KoinComponent {

    val prefs: Prefs by inject()
    val polygonAddress = MutableLiveData<String>().apply {
        value = prefs.getWallet("polygon") ?: "None"
    }

    val arweaveAddress = MutableLiveData<String>().apply {
        value = prefs.getWallet("arweave") ?: "None"
    }

}