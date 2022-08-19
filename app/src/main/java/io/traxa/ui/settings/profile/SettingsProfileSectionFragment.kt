package io.traxa.ui.settings.profile

import android.os.Bundle
import android.util.Base64
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.biometric.BiometricManager.Authenticators.BIOMETRIC_WEAK
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import com.google.android.material.transition.MaterialSharedAxis
import io.traxa.R
import io.traxa.databinding.FragmentSettingsProfileSectionBinding
import io.traxa.ui.settings.SettingsBaseFragment
import io.traxa.ui.settings.textdialog.TextDialog
import java.util.concurrent.Executor

class SettingsProfileSectionFragment(override val title: String = "Profile") : SettingsBaseFragment() {

    companion object {
        fun newInstance() = SettingsProfileSectionFragment()
    }

    private val viewModel: SettingsProfileSectionViewModel by viewModels()
    private lateinit var binding: FragmentSettingsProfileSectionBinding

    private val profileDialog = TextDialog().apply {
        key = "player_id"
        title = "Your Profile Number"
        message = "Keep this number safe and secure.\nIt's linked to your NFTs and captured containers."
        hint = "Profile number"

        validationFunction = {
            if(it.isBlank()) false
            if(it.length != 48) false

            val playerId = String(Base64.decode(it, Base64.URL_SAFE or Base64.NO_WRAP))
            "[0-9a-z]{8}-(?:[0-9a-z]{4}-){3}[0-9a-z]{12}".toRegex().matches(playerId)
        }

        onDismiss = {
            if (it != null) Toast.makeText(
                requireContext(),
                R.string.profile_changed,
                Toast.LENGTH_LONG
            ).show()
            viewModel.profileId.value = it ?: "None"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enterTransition = MaterialSharedAxis(
            MaterialSharedAxis.X,true
        ).apply {
            duration = 300L
        }
        returnTransition = MaterialSharedAxis(
            MaterialSharedAxis.X,false
        ).apply {
            duration = 300L
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentSettingsProfileSectionBinding.inflate(inflater, container, false)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listen()
    }

    private fun listen() {
        binding.lytProfile.setOnClickListener {
            val executor = ContextCompat.getMainExecutor(requireContext())
            auth(executor) {
                profileDialog.show(parentFragmentManager, null)
            }
        }
    }

    private fun auth(executor: Executor, success: (() -> Unit)) {
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(getString(R.string.profile_auth_title))
            .setNegativeButtonText(getString(R.string.profile_auth_button_negative))
            .setDescription(getString(R.string.profile_auth_description))
            .setAllowedAuthenticators(BIOMETRIC_WEAK)
            .build()

        val biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(
                    result: BiometricPrompt.AuthenticationResult
                ) {
                    super.onAuthenticationSucceeded(result)
                    success()
                }

                override fun onAuthenticationError(
                    errorCode: Int, errString: CharSequence
                ) {
                    if(errorCode == BiometricPrompt.ERROR_NO_DEVICE_CREDENTIAL
                        || errorCode == BiometricPrompt.ERROR_NO_BIOMETRICS
                        || errorCode == BiometricPrompt.ERROR_HW_NOT_PRESENT) {

                        success()
                        return
                    }

                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_msg_profile_auth_error),
                        Toast.LENGTH_SHORT
                    ).show()
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.error_msg_profile_auth_failed),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })

        biometricPrompt.authenticate(promptInfo)
    }

}