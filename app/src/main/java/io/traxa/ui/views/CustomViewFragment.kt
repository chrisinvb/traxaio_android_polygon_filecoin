package io.traxa.ui.views

import android.os.Bundle
import android.view.View
import androidx.annotation.LayoutRes
import androidx.fragment.app.Fragment

class CustomViewFragment (
    @LayoutRes contentLayoutId: Int,
    private val onViewCreated: ((View) -> Unit)? = null
) : Fragment(contentLayoutId) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        onViewCreated?.invoke(view)
        super.onViewCreated(view, savedInstanceState)
    }
}