package io.traxa.ui.onboard

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter

class OnboardAdapter(fragmentManager: FragmentManager,
                     val lifecycle: Lifecycle,
                     private val fragList: ArrayList<Fragment>) :
    FragmentStateAdapter(fragmentManager, lifecycle)
{
    override fun getItemCount() = fragList.size
    override fun createFragment(position: Int) = fragList[position]
}