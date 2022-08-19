package io.traxa.ui.settings.wallets

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import io.traxa.databinding.FragmentWalletsBinding
import io.traxa.services.network.StardustService
import io.traxa.ui.settings.SettingsBaseFragment
import io.traxa.ui.settings.textdialog.TextDialog
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

class SettingsWalletsSectionFragment : SettingsBaseFragment() {

    private val stardustService: StardustService by inject()

    override val title: String
        get() = "Wallets"

    private lateinit var binding: FragmentWalletsBinding
    private val viewModel: SettingsWalletsSectionViewModel by viewModels()
    private val ethereumRegex = Regex("^0x[a-fA-F0-9]{40}\$")


    private val polygonWallet = TextDialog().apply {
        key = "wallet_polygon"
        title = "Add Your Wallet"
        message = "Connect your wallet to Traxa.io to receive community rewards"
        hint = "Wallet address"
        validationFunction = { ethereumRegex.matches(it) }
        onDismiss = {
            if(it != null) CoroutineScope(Dispatchers.IO).launch {
                val playerId = prefs.getPlayerId()
                stardustService.setPlayerWallet(playerId!!, it)
            }

            viewModel.polygonAddress.value = it ?: "None"
        }
    }

    private val arweaveWallet = TextDialog().apply {
        key = "wallet_arweave"
        title = "Add Your Wallet"
        message = "Connect your wallet to Traxa.io to receive community rewards"
        hint = "Wallet address"
        onDismiss = {
            viewModel.arweaveAddress.value = it ?: "None"
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentWalletsBinding.inflate(inflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = viewLifecycleOwner
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listen()
    }

    private fun listen() {
        binding.lytPolygon.setOnClickListener { polygonWallet.show(parentFragmentManager, null) }
        //binding.lytArweave.setOnClickListener { arweaveWallet.show(parentFragmentManager, null) }
    }
}