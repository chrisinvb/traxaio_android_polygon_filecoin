package io.traxa.ui.about

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import io.traxa.BuildConfig
import io.traxa.databinding.ActivityAboutBinding
import io.traxa.utils.Constants

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setup()
        listen()
    }

    private fun listen() {
        binding.lytWebsite.setOnClickListener {
            startActivity(Intent.parseUri(Constants.WEBSITE, 0))
        }

        binding.lytDiscord.setOnClickListener {
            startActivity(Intent.parseUri(Constants.DISCORD, 0))
        }

        binding.lytOsl.setOnClickListener {
            startActivity(Intent(this, OssLicensesMenuActivity::class.java))
        }

        binding.toolbar.setNavigationOnClickListener { super.onBackPressed() }
    }

    private fun setup() {
        binding.txtVersion.text = BuildConfig.VERSION_NAME
    }
}