package io.traxa.ui.onboard

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import io.traxa.R
import io.traxa.databinding.ActivityOnboardBinding
import io.traxa.databinding.LayoutContainerComparisonBinding
import io.traxa.databinding.LayoutOnboardWalletBinding
import io.traxa.services.Prefs
import io.traxa.services.network.StardustService
import io.traxa.ui.main.MainActivity
import io.traxa.ui.onboard.steps.ShowcaseFragment
import io.traxa.ui.views.BindingFragment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class OnboardActivity : AppCompatActivity() {

    private val prefs: Prefs by inject()
    private val stardust: StardustService by inject()
    private lateinit var binding: ActivityOnboardBinding
    private lateinit var adapter: OnboardAdapter

    private val steps = arrayListOf<Fragment>()

    private val ethereumRegex = Regex("^0x[a-fA-F0-9]{40}$")
    private lateinit var walletBinding: LayoutOnboardWalletBinding


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOnboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        listen()
        setup()
    }

    private fun listen() {
        binding.btnNext.setOnClickListener {
            if(binding.viewPager.currentItem == adapter.itemCount-1) {
                prefs.setFirstTimeOpened(false)

                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }else next()
        }

        binding.viewPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if(position == adapter.itemCount-1) binding.btnNext.setText(R.string.get_started)
                else binding.btnNext.setText(R.string.next)
            }
        })
    }

    private fun setup() {
        binding.viewPager.isUserInputEnabled = false

        steps += ShowcaseFragment.newInstance(
            getString(R.string.onboard_slide_1_title),
            getString(R.string.onboard_slide_1_subtitle),
            R.drawable.icon_large
        )

        steps += BindingFragment<LayoutOnboardWalletBinding>().apply {
            binding = { i, c ->
                walletBinding = LayoutOnboardWalletBinding.inflate(i, c, false)
                walletBinding
            }
        }

        steps += BindingFragment<LayoutContainerComparisonBinding>().apply {
            binding = { i, c ->
                val binding = LayoutContainerComparisonBinding.inflate(i, c, false)
                binding.title = getString(R.string.onboard_slide_2_title)
                binding.subtitle = getString(R.string.onboard_slide_2_subtitle)
                binding
            }
        }

        steps += ShowcaseFragment.newInstance(
            getString(R.string.onboard_slide_3_title),
            getString(R.string.onboard_slide_3_subtitle),
            R.drawable.person_capture
        )

        steps += ShowcaseFragment.newInstance(
            getString(R.string.onboard_slide_4_title),
            getString(R.string.onboard_slide_4_subtitle),
            R.drawable.container_cloud
        )

        steps += ShowcaseFragment.newInstance(
            getString(R.string.onboard_slide_5_title),
            getString(R.string.onboard_slide_5_subtitle),
            R.drawable.container_blockchain
        )

        adapter = OnboardAdapter(supportFragmentManager, lifecycle, steps)
        binding.viewPager.adapter = adapter
    }

    fun next() {
        if(binding.viewPager.currentItem == 1) {
            val walletAddress = walletBinding.txtAddress.text?.toString() ?: ""
            if(walletAddress.isNotEmpty() && !ethereumRegex.matches(walletAddress)) {
                walletBinding.textInputLayout.error = "Invalid address"
                return
            }

            CoroutineScope(Dispatchers.IO).launch {
                val playerId = prefs.getPlayerId()
                prefs.setWallet("polygon", walletAddress)
                stardust.setPlayerWallet(playerId!!, walletAddress)
            }

            walletBinding.textInputLayout.error = null
        }


        with(binding.viewPager) {
            setCurrentItem(currentItem + 1, true)
        }
    }

    fun skip(pages: Int) = with(binding.viewPager) {
        setCurrentItem(currentItem + pages + 1, true)
    }

    fun back() = with(binding.viewPager) {
        setCurrentItem(currentItem - 1, true)
    }
}