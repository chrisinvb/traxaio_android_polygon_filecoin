package io.traxa.ui.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.viewbinding.ViewBinding

class BindingFragment<T : ViewBinding> : Fragment() {

    var binding: ((inflater: LayoutInflater, container: ViewGroup?) -> T)? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ) = binding?.invoke(inflater, container)?.root
}