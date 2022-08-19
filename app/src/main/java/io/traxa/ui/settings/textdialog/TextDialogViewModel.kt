package io.traxa.ui.settings.textdialog

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import io.traxa.services.Prefs
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class TextDialogViewModel : ViewModel(), KoinComponent {

    val prefs: Prefs by inject()
    val text = MutableLiveData("")
    val error = MutableLiveData<String?>(null)

    val title = MutableLiveData("")
    val message = MutableLiveData("")
    val hint = MutableLiveData("")

    var errorString = "Please enter a valid profile number"

    private fun check(validationFunction: ((String) -> Boolean)): Boolean {
        val matches = validationFunction(text.value!!)
        if(!matches) error.value = errorString
        return matches
    }

    fun save(key: String, validationFunction: ((String) -> Boolean)?): Boolean {
        if(validationFunction != null && !check(validationFunction)) return false
        if(text.value!!.isBlank()) return false

        prefs.edit.putString(key, text.value!!).apply()
        return true
    }
}