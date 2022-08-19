package io.traxa.ui.upload

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.animation.PathInterpolator
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewModelScope
import io.traxa.databinding.ActivityUploadBinding
import io.traxa.databinding.LayoutUploadSliderCtaBinding
import io.traxa.persistence.entities.RecordingStatus
import io.traxa.services.Prefs
import io.traxa.ui.main.MainActivity
import io.traxa.ui.views.BindingFragment
import io.traxa.utils.Constants
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import nl.dionsegijn.konfetti.models.Shape
import nl.dionsegijn.konfetti.models.Size
import org.koin.android.ext.android.inject
import java.io.File
import java.util.*
import kotlin.concurrent.timerTask
import iamutkarshtiwari.github.io.ananas.editimage.ImageEditorIntentBuilder
import iamutkarshtiwari.github.io.ananas.editimage.EditImageActivity
import io.traxa.R
import io.traxa.ui.views.DialogOnboardImageEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

class UploadActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUploadBinding
    private val viewModel: UploadActivityViewModel by viewModels()

    private val prefs: Prefs by inject()
    private var progressTimer = Timer()



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUploadBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this
        setContentView(binding.root)

        setup()
        listen()
    }

    private val imageEditResult = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (it.resultCode == Activity.RESULT_OK) {
            val newFilePath = it.data!!.getStringExtra(ImageEditorIntentBuilder.OUTPUT_PATH)
            val isImageEdit = it.data?.getBooleanExtra(EditImageActivity.IS_IMAGE_EDITED, false) ?: false

            if(isImageEdit && newFilePath != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    viewModel.uploadThumbnail(File(newFilePath))
                }

                Toast.makeText(this, "Container successfully customized!", Toast.LENGTH_SHORT)
                    .show()
            }
        }
    }

    private fun listen() {
        viewModel.message.observe(this) {
            binding.txtLoadingMessage.text = getString(it)
        }

        viewModel.secondsPassed.observe(this) {
            val uploadProgress = viewModel.recordingUploadProgress.value ?: 0
            if(it >= viewModel.waitingTime * 60 && uploadProgress >= 100) {
                prefs.clearUploadStartTime()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }

        viewModel.recordingStatus.observe(this) {
            Log.d("UploadActivity", "listen: $it")
            if(it == RecordingStatus.DONE) startTimer()
        }

        viewModel.container.observe(this) {
            val uuid = it.uid

            val fromMainWindow = intent.getBooleanExtra("fromMain", false)
            if(fromMainWindow) showImageEditorOnboardOrStartDirectly(uuid)
        }
    }

    private fun startTimer() {
        val uploadStartTime = prefs.getUploadStartTime()
        Log.d("UploadActivity", "startTimer: $uploadStartTime")

        if(uploadStartTime == 0L) {
            binding.progressIndicator.isVisible = false
            binding.txtTimer.isVisible = true
            prefs.setUploadStartTime(System.currentTimeMillis() / 1000)

            progressTimer = Timer()
            progressTimer.scheduleAtFixedRate(progressIndicatorTask(), 0, 1000)
        }
    }

    private fun setup() {
        //Apply time reward
        lifecycleScope.launchWhenStarted {
            val count = viewModel.getContainerCount()
            val rewardNumber = 5 * (count/5)
            if(rewardNumber != 0 && viewModel.waitingTime == Constants.waitingTime) {
                val timeReward = Constants.timeRewards[rewardNumber] ?: Constants.maxTimeReward
                if(viewModel.waitingTime - (timeReward / 60f) > 0){
                    viewModel.waitingTime -= (timeReward / 60f)
                }
            }
        }

        binding.truckAnimation.start()

        with(binding.slidingPanel.fragments) {
            add(BindingFragment<LayoutUploadSliderCtaBinding>().apply {
                binding = { i, c ->
                    val binding = LayoutUploadSliderCtaBinding.inflate(i, c, false)
                    binding.txtHeadline.text = getString(R.string.traxa_wait_blockchain_processing)
                    binding
                }
            })

            add(BindingFragment<LayoutUploadSliderCtaBinding>().apply {
                binding = {i, c ->
                    val binding = LayoutUploadSliderCtaBinding.inflate(i, c, false)
                    binding.txtHeadline.text = getString(R.string.traxa_mvp_transaction_fee)
                    binding
                }
            })

            add(BindingFragment<LayoutUploadSliderCtaBinding>().apply {
                binding = { i, c ->
                    val binding = LayoutUploadSliderCtaBinding.inflate(i, c, false)
                    binding.txtHeadline.text = getString(R.string.future_releases_rewards)
                    binding
                }
            })

//            add(BindingFragment<LayoutUploadImageBinding>().apply {
//                binding = { i, c ->
//                    val binding = LayoutUploadImageBinding.inflate(i, c, false)
//                    binding.txtHeadline.text = getString(R.string.example_shipping_container)
//                    binding.imgBackground.setImageResource(R.drawable.shipping_container)
//                    binding
//                }
//            })

        }

        viewModel.isMonitorWalletVisible.observe(this) {

            if(it) {
                viewModel.message.removeObservers(this)
                binding.txtLoadingMessage.setText(R.string.nft_mint_title)
                binding.txtLoadingSubtitle.isVisible = true

                //Find if we need to show the reward message
                viewModel.viewModelScope.launch {
                    val count = viewModel.getContainerCount()
                    val roundNumber = 5 * (count/5)
                    val previousNumber = 5 * ((count - 1)/5)

                    if (roundNumber > previousNumber) {
                        binding.txtMessage.text =
                            getString(R.string.upload_message_subtitle, count)

                        binding.lytMessage.isVisible = true
                        binding.viewKonfetti.build()
                            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                            .setDirection(0.0, 359.0)
                            .setSpeed(1f, 5f)
                            .setFadeOutEnabled(true)
                            .setTimeToLive(2000L)
                            .addShapes(Shape.Square, Shape.Circle)
                            .addSizes(Size(12))
                            .setPosition(-50f, binding.viewKonfetti.width + 50f, -50f, -50f)
                            .streamFor(300, 1000L)

                        delay(3500)

                        val bezierInterpolator = PathInterpolator(.98f, .01f, .01f, 1f)
                        binding.lytMessage.animate()
                            .xBy((-Resources.getSystem().displayMetrics.widthPixels).toFloat())
                            .setDuration(700)
                            .setInterpolator(bezierInterpolator)
                            .withEndAction { binding.lytMessage.isVisible = false }
                            .start()
                    } else {
                        binding.viewKonfettiSmall.build()
                            .addColors(Color.YELLOW, Color.GREEN, Color.MAGENTA)
                            .setDirection(0.0, 359.0)
                            .setSpeed(1f, 5f)
                            .setFadeOutEnabled(true)
                            .setTimeToLive(2000L)
                            .addShapes(Shape.Square, Shape.Circle)
                            .addSizes(Size(12))
                            .setPosition(-50f, binding.viewKonfettiSmall.width + 50f, -50f, -50f)
                            .streamFor(300, 1000L)
                    }

                    binding.progressIndicator.isVisible = false
                    binding.progressIndicator.isIndeterminate = false
                    binding.txtTimer.isVisible = false
                }
            }
        }

        binding.slidingPanel.setup(supportFragmentManager, lifecycle)
        binding.slidingPanel.autoplay(10000)

        lifecycleScope.launchWhenStarted { viewModel.checkToken() }

    }

    private fun showImageEditorOnboardOrStartDirectly(uuid: String) {
        val hasEditedOnce = prefs.getBoolean(Prefs.HAS_EDITED_IMAGE_ONCE)

        if(!hasEditedOnce) {
            val dialog = DialogOnboardImageEditor()
            dialog.onPositiveClicked = {
                startPhotoEditor(uuid)
                prefs.setBoolean(Prefs.HAS_EDITED_IMAGE_ONCE, true)
            }

            dialog.show(supportFragmentManager, null)
        }else startPhotoEditor(uuid)
    }

    private fun startPhotoEditor(uuid: String) {
        val thumbnail = File(getExternalFilesDir("thumbnails"), "$uuid.jpg")
        val thumbnailGen = File(getExternalFilesDir("thumbnails"), "$uuid-gen.jpg")
        if(thumbnailGen.exists())
            thumbnailGen.delete()

        val intent = ImageEditorIntentBuilder(this, thumbnail.absolutePath, thumbnailGen.absolutePath)
            .withStickerFeature()
            .withAddText()
            .withPaintFeature()
            .forcePortrait(true)
            .setSupportActionBarVisibility(false)
            .withEditorTitle("Edit container")
            .build()

        EditImageActivity.start(imageEditResult, intent, this)
    }


    private fun progressIndicatorTask() = timerTask {
        viewModel.secondsPassed.postValue(
            viewModel.secondsPassed.value?.plus(1)
        )

        if(viewModel.secondsPassed.value!! > viewModel.waitingTime * 60) {
            cancel()
            prefs.clearUploadStartTime()
        }
    }

    override fun onResume() {
        super.onResume()
//        binding.pulser.start()
        binding.truckAnimation.start()

        if (prefs.getUploadStartTime() != 0L) {
            binding.progressIndicator.isVisible = false
            binding.txtTimer.isVisible = true
            viewModel.secondsPassed.value = viewModel.calculateSecondsPassed()
            progressTimer = Timer()
            progressTimer.scheduleAtFixedRate(progressIndicatorTask(), 0, 1000)
        }
    }

    override fun onStop() {
        super.onStop()
        if(prefs.getUploadStartTime() != 0L) progressTimer.cancel()
    }

    override fun onPause() {
        super.onPause()
//        binding.pulser.stop()
        binding.truckAnimation.stop()
        if (prefs.getUploadStartTime() != 0L) progressTimer.cancel()
    }
}