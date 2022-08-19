package io.traxa.ui.settings.textdialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.ClipData
import android.content.ClipboardManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat.getSystemService
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.traxa.databinding.FragmentTextDialogBinding
import io.traxa.services.Prefs
import org.koin.android.ext.android.inject


class TextDialog : BottomSheetDialogFragment() {

    var onDismiss: ((String?) -> Unit)? = null
    var key: String? = null
    var validationFunction: ((String) -> Boolean)? = null

    lateinit var title: String
    lateinit var message: String
    var errorMessage: String? = null
    var hint: String? = null

    val prefs: Prefs by inject()
    private val viewModel: TextDialogViewModel by viewModels()
    private lateinit var binding: FragmentTextDialogBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentTextDialogBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = viewLifecycleOwner
        binding.viewModel = viewModel

        setup()
        return binding.root
    }

    private fun setup() {
        if(key != null) {
            val wallet = prefs.getString(key!!)
            if(wallet != null) viewModel.text.value = wallet
        }

        viewModel.title.value = title
        viewModel.message.value = message
        viewModel.hint.value = hint ?: ""

        if(errorMessage != null)
            viewModel.errorString = errorMessage!!
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listen()
    }

    private fun listen() {
        binding.btnSave.setOnClickListener {
            if(key != null && viewModel.save(key!!, validationFunction)) {
                onDismiss?.invoke(viewModel.text.value!!)
                dismiss()
            }
        }

        binding.textInputLayout.setEndIconOnClickListener {
            val clipboard = getSystemService(it.context, ClipboardManager::class.java)
            val clip = ClipData.newPlainText("label", viewModel.text.value!!)
            if (clipboard != null && clip != null) {
                clipboard.setPrimaryClip(clip)
                Toast.makeText(it.context, "Copied!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("RestrictedApi", "VisibleForTests")
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        //Disable animations to keep rounded corners in an expanded state
        val bottomSheetDialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        bottomSheetDialog.behavior.disableShapeAnimations()
        return bottomSheetDialog
    }
}