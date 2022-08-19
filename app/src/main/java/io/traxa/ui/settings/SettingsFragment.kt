package io.traxa.ui.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialSharedAxis
import io.traxa.databinding.FragmentSettingsBinding
import io.traxa.ui.about.AboutActivity
import io.traxa.ui.settings.profile.SettingsProfileSectionFragment
import io.traxa.ui.settings.wallets.SettingsWalletsSectionFragment

/**
 * Fragment shown in [SettingsActivity].
 */
class SettingsFragment : SettingsBaseFragment() {

    private val activity by lazy { requireActivity() as SettingsActivity }
    private val viewModel: SettingsViewModel by viewModels()
    private lateinit var binding: FragmentSettingsBinding

    override val title: String
        get() = "Settings"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        exitTransition = MaterialSharedAxis(
            MaterialSharedAxis.X,true
        ).apply {
            duration = 300L
        }
        reenterTransition = MaterialSharedAxis(
            MaterialSharedAxis.X,false
        ).apply {
            duration = 300L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.lytProfile.setOnClickListener { activity.replace(SettingsProfileSectionFragment()) }
        binding.lytWallets.setOnClickListener { activity.replace(SettingsWalletsSectionFragment()) }
        binding.lytAbout.setOnClickListener { startActivity(Intent(requireContext(), AboutActivity::class.java)) }
    }

}