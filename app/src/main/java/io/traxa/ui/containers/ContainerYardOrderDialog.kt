package io.traxa.ui.containers

import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatTextView
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.core.view.get
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import io.traxa.R
import io.traxa.databinding.LayoutContainerOrderListBinding
import io.traxa.services.Prefs
import org.koin.android.ext.android.inject

class ContainerYardOrderDialog : BottomSheetDialogFragment() {

    enum class OrderType {
        BY_TIME,
        BY_NAME
    }

    var whenDone: ((option: OrderType) -> Unit)? = null
    private var orderType = OrderType.BY_TIME
    private lateinit var binding: LayoutContainerOrderListBinding
    private val prefs: Prefs by inject()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = LayoutContainerOrderListBinding.inflate(inflater)
        setup()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listen()
    }

    override fun onDismiss(dialog: DialogInterface) {
        whenDone?.invoke(orderType)
        super.onDismiss(dialog)
    }

    override fun onCancel(dialog: DialogInterface) {
        whenDone?.invoke(orderType)
        super.onCancel(dialog)
    }

    private fun setup() {
        orderType = prefs.getContainerYardOrder()

        val viewIndex = OrderType.values().indexOf(orderType)
        resetSelections()
        (binding.lytSelectionGroup[viewIndex] as AppCompatTextView)
            .setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_done_24, 0, 0, 0)
    }

    private fun resetSelections() {
        binding.lytSelectionGroup.forEach {
            val txt = it as AppCompatTextView
            txt.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_empty, 0, 0, 0)
        }
    }

    private fun listen() {
        binding.lytSelectionGroup.forEachIndexed { index, view ->

            val textView = view as AppCompatTextView
            textView.setOnClickListener {
                resetSelections()
                textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_done_24, 0, 0, 0)

                orderType = OrderType.values()[index]
                prefs.setContainerYardOrder(orderType)
                dismiss()
            }
        }
    }

}