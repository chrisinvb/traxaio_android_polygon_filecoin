package io.traxa.ui.settings

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import io.traxa.R
import io.traxa.databinding.ActivitySettingsBinding
import io.traxa.ui.main.MainActivity

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        replace(SettingsFragment())

        binding.toolbar.setNavigationOnClickListener { onBackPressed() }
    }

    override fun setTitle(title: CharSequence?) {
        super.setTitle(title)
        binding.toolbar.title = title
        binding.collapsingToolbar.title = title
    }

    fun replace(fragment: SettingsBaseFragment) = java.util.UUID.randomUUID().toString().also {
        supportFragmentManager.beginTransaction()
            .replace(R.id.frame, fragment, it)
            .addToBackStack(fragment.title)
            .commit()
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        try {
            super.onActivityResult(requestCode, resultCode, data)
        } catch (e: Exception) {
        }
    }

    override fun onBackPressed() {
        if (supportFragmentManager.backStackEntryCount > 1) {
            val size = supportFragmentManager.backStackEntryCount
            val entry = supportFragmentManager.getBackStackEntryAt(size - 2)
            title = entry.name

            supportFragmentManager.popBackStackImmediate()
        } else {
            finishAndRemoveTask()
            startActivity(Intent(this, MainActivity::class.java))
        }
    }
}