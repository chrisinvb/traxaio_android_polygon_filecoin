package io.traxa.ui.containers.detail

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.get
import io.traxa.R
import io.traxa.databinding.ActivityContainerDetailBinding

class ContainerDetailActivity : AppCompatActivity() {

    private val viewModel: ContainerDetailViewModel by viewModels()
    private lateinit var binding: ActivityContainerDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContainerDetailBinding.inflate(layoutInflater)
        binding.viewModel = viewModel
        binding.lifecycleOwner = this

        setup()
        setContentView(binding.root)
        listen()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.container_menu, menu)

        if(viewModel.isImageVisible.value == false) menu[1].isVisible = false
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_container_copy_debug_info -> viewModel.copyDebugInfo()
            R.id.menu_container_share_image -> viewModel.shareImage()
        }

        return super.onOptionsItemSelected(item)
    }

    private fun setup() {
        setSupportActionBar(binding.toolbar)
    }

    private fun listen() {
        viewModel.containerId.observe(this) {
            supportActionBar?.title = it
            binding.toolbar.title = it
        }

        binding.toolbar.setNavigationOnClickListener { super.onBackPressed() }
    }
}