package io.traxa.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.abs

class SlidingPanel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val viewPager = ViewPager2(context)

    val fragments = arrayListOf<Fragment>()
    var isDynamicHeightEnabled = false
    var onPageSelected: ((Int) -> Unit)? = null

    inner class SlidingAdapter(fragmentManager: FragmentManager,
                         val lifecycle: Lifecycle) :
        FragmentStateAdapter(fragmentManager, lifecycle)
    {
        override fun getItemCount() = fragments.size
        override fun createFragment(position: Int) = fragments[position]
    }

    init {
        addView(viewPager)
        viewPager.isUserInputEnabled = false
    }

    var offscreenPageLimit: Int = 0
        set(value) {
            viewPager.offscreenPageLimit = value
            field = value
        }


    fun setup(fm: FragmentManager, lifecycle: Lifecycle) {
        viewPager.adapter = SlidingAdapter(fm, lifecycle)
        
        viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(isDynamicHeightEnabled) postDelayed({
                    val viewGroup = (fragments[position].requireView() as ViewGroup)
                    val bottom = viewGroup[viewGroup.childCount - 1].bottom + viewGroup.paddingBottom

                    viewPager.layoutParams.height = bottom
                    viewPager.requestLayout()
                }, 500)

                onPageSelected?.invoke(position)
            }
        })
    }

    fun autoplay(delay: Long) {
        Timer().scheduleAtFixedRate(timerTask {
            this@SlidingPanel.post { next() }
        }, delay, delay)
    }

    fun next() = with(viewPager) {
        setCurrentItem((currentItem + 1) % fragments.size, true)
    }

    fun end() = with(viewPager) {
        this.scrollBy(Integer.MAX_VALUE, 0)
        //setCurrentItem((currentItem + 1) % fragments.size, true)
    }

    fun skip(pages: Int) = with(viewPager) {
        setCurrentItem(currentItem + pages + 1, true)
    }

    fun back() = with(viewPager) {
        setCurrentItem(abs((currentItem - 1) % fragments.size), true)
    }

}