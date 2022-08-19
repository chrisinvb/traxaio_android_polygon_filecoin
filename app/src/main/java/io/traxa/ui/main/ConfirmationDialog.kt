package io.traxa.ui.main

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.media.ThumbnailUtils
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.provider.MediaStore
import android.util.Size
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.palette.graphics.Palette
import coil.load
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import io.traxa.R
import io.traxa.databinding.DialogConfirmationBinding
import io.traxa.databinding.DialogConfirmationStep1Binding
import io.traxa.persistence.AppDatabase
import io.traxa.ui.views.BindingFragment
import io.traxa.ui.views.CustomViewFragment
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.Executors


class ConfirmationDialog : DialogFragment() {

    private lateinit var binding: DialogConfirmationBinding
    private var thumb: Bitmap? = null
    private val db: AppDatabase by inject()
    private val containerDao = db.containerDao()

    var onFinish: ((uuid: String, color: Int) -> Unit)? = null

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogConfirmationBinding.inflate(layoutInflater)
        setup()
        listen()

        return MaterialAlertDialogBuilder(requireContext())
            .setView(binding.root)
            .create()
    }

    private fun listen() {
        binding.btnNegative.setOnClickListener { dismiss() }

        binding.slidingPanel.onPageSelected = {
            when(it) {
                0 -> {
                    binding.btnPositive.text = getString(R.string.confirmation_dialog_yes)
                    binding.btnNegative.text = getString(R.string.confirmation_dialog_no)
                    binding.btnPositive.setOnClickListener { binding.slidingPanel.next() }
                }

                1 -> {
                    binding.btnPositive.text = getString(R.string.confirmation_dialog_agree)
                    binding.btnNegative.text = getString(R.string.confirmation_dialog_close)
                    binding.btnPositive.setOnClickListener {

                        val cropHorizontal = (thumb!!.width * .15).toInt()
                        val cropVertical = (thumb!!.height * .20).toInt()

                        binding.loading.isVisible = true
                        binding.btnPositive.isEnabled = false
                        binding.btnNegative.isEnabled = false
                        isCancelable = false

                        val thread = Executors.newCachedThreadPool().asCoroutineDispatcher()
                        CoroutineScope(thread).launch {
                            //Extract container color
                            val palette = Palette.from(thumb!!)
                                .setRegion(
                                    cropHorizontal,
                                    cropVertical,
                                    thumb!!.width - cropHorizontal,
                                    thumb!!.height - cropVertical
                                )
                                .generate()

                            val color = palette.getDominantColor(Color.RED)
                            val uuid = UUID.randomUUID().toString()

                            //Save thumb image
                            val folder = requireContext().getExternalFilesDir("thumbnails")
                            val file = File(folder, "$uuid.jpg")
                            val fout = FileOutputStream(file)

                            thumb?.compress(Bitmap.CompressFormat.JPEG, 70, fout)
                            fout.flush()
                            fout.close()

                            onFinish?.invoke(uuid, color)
                            dismiss()
                        }
                    }
                }
            }
        }
    }

    private fun setup() {
        //Add panels
        with(binding.slidingPanel.fragments) {
            add(BindingFragment<DialogConfirmationStep1Binding>().apply {
                binding = { i, c ->
                    val binding = DialogConfirmationStep1Binding.inflate(i, c, false)

                    //Generate video thumbnail
                    CoroutineScope(Dispatchers.IO).launch {
                        val videoFile = File(requireContext().getExternalFilesDir("captures"), "video.mp4")

                        thumb = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            ThumbnailUtils.createVideoThumbnail(
                                videoFile,
                                Size(800, 1000),
                                CancellationSignal()
                            )
                        } else {
                            ThumbnailUtils.createVideoThumbnail(
                                videoFile.absolutePath,
                                MediaStore.Images.Thumbnails.FULL_SCREEN_KIND
                            )
                        }

                        withContext(Dispatchers.Main) {
                            binding.imgRecording.load(thumb) {
                                crossfade(true)
                            }
                        }
                    }

                    binding
                }
            })

            add(CustomViewFragment(R.layout.dialog_confirmation_step2))
        }

        binding.slidingPanel.offscreenPageLimit = 2
        binding.slidingPanel.isDynamicHeightEnabled = true
        binding.slidingPanel.setup(parentFragmentManager, lifecycle)
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = ViewGroup.LayoutParams.MATCH_PARENT
            val height = ViewGroup.LayoutParams.WRAP_CONTENT
            dialog.window!!.setLayout(width, height)
        }
    }


}