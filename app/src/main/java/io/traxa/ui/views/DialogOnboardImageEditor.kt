package io.traxa.ui.views

import android.app.Dialog
import android.os.Bundle
import android.view.animation.PathInterpolator
import androidx.fragment.app.DialogFragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.traxa.databinding.DialogOnboardImageEditorBinding

class DialogOnboardImageEditor : DialogFragment() {

    private lateinit var binding: DialogOnboardImageEditorBinding
    var onPositiveClicked: (() -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogOnboardImageEditorBinding.inflate(layoutInflater)
        setup()
        listen()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun setup() {
        binding.sticker.animate()
            .alpha(1f)
            .scaleX(1f)
            .scaleY(1f)
            .setDuration(1000)
            .setInterpolator(PathInterpolator(.21f,1.22f,.88f,.92f))
            .setStartDelay(1000)
            .start()
    }

    private fun listen() {
        binding.btnPositive.setOnClickListener {
            onPositiveClicked?.invoke()
            dismiss()
        }
    }
}