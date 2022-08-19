package io.traxa.ui.settings

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment

abstract class SettingsBaseFragment : Fragment() {
    abstract val title: String

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (requireActivity() as SettingsActivity).title = title
    }
}