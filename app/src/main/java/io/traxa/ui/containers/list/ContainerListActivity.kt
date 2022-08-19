package io.traxa.ui.containers.list

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import io.traxa.R
import io.traxa.containerListItem
import io.traxa.containersDetail
import io.traxa.databinding.ActivityContainerListBinding
import io.traxa.persistence.AppDatabase
import io.traxa.persistence.entities.ContainerCapture
import io.traxa.text
import io.traxa.ui.containers.ContainerYardOrderDialog
import io.traxa.ui.containers.detail.ContainerDetailActivity
import io.traxa.utils.RecyclerBounceOverscroll
import io.traxa.utils.combineWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import java.text.SimpleDateFormat
import java.util.*

class ContainerListActivity : AppCompatActivity() {

    private val viewModel: ContainerListViewModel by viewModels()
    private lateinit var binding: ActivityContainerListBinding

    private val db: AppDatabase by inject()
    private val containerDao = db.containerDao()

    private val orderDialog: ContainerYardOrderDialog by lazy {
        ContainerYardOrderDialog().apply {
            whenDone = { viewModel.orderType.value = it }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContainerListBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)
        setup()
        listen()
    }

    private fun listen() {
        binding.toolbar.setNavigationOnClickListener { super.onBackPressed() }
        viewModel.containers.combineWith(viewModel.orderType) { list, orderType ->
            Pair(list, orderType)
        }.observe(this) {
            val list = it.first
            val orderType = it.second

            if(list != null && list.isEmpty()) binding.lytEmpty.isVisible = true
            else if(list != null) buildContainerList(list, orderType ?: ContainerYardOrderDialog.OrderType.BY_TIME)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId) {
            R.id.menu_container_yard_sort -> openSort()
            R.id.menu_container_yard_remove_unknown -> removeUnknowns()
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_container_yard, menu)
        return super.onCreateOptionsMenu(menu)
    }

    private fun setup() {
        RecyclerBounceOverscroll().attach(binding.rvContainers)
    }

    private fun openSort() {
        orderDialog.show(supportFragmentManager, null)
    }

    private fun removeUnknowns() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Warning")
            .setMessage("This action is irreversible, are you sure you want to continue?")
            .setPositiveButton("Yes") { d, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    val result = containerDao.deleteAllUnknowns()
                    val message = resources.getQuantityString(R.plurals.container_remove_result_message, result, result)
                    Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
                }

                d.cancel()
            }
            .setNegativeButton("No", null)
            .show()
    }

    private fun buildContainerList(list: List<ContainerCapture>, orderType: ContainerYardOrderDialog.OrderType) {

        binding.rvContainers.withModels {

            containersDetail {
                id("details")

                val count = list.filter { it.storageType != null }.size
                val nextRoundNumber = 5 * ((count/5) + 1)

                progress(count)
                max(nextRoundNumber)
                title("$count total containers found")
                message("Reach $nextRoundNumber scanned containers to reduce the upload waiting time!")
            }

            val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

            val renderList = when(orderType) {
                ContainerYardOrderDialog.OrderType.BY_TIME -> list.sortedByDescending { it.timestamp }
                ContainerYardOrderDialog.OrderType.BY_NAME -> list.sortedBy { it.containerIds }
            }

            var lastDate = ""
            renderList.forEach {

                val date = Date(it.timestamp)
                val currentDate = dateFormat.format(date)
                if(lastDate != currentDate) text {
                    id(currentDate)
                    text(currentDate)
                }
                lastDate = currentDate

                containerListItem {
                    val firstContainerId = it.containerIds?.split(",")?.firstOrNull() ?: "Unknown"

                    id(it.uid)
                    containerImageId(it.uid)
                    title(firstContainerId)
                    itemClicked { _ ->
                        startActivity(
                            Intent(
                                this@ContainerListActivity,
                                ContainerDetailActivity::class.java
                            ).putExtra("uid", it.uid)
                        )
                    }
                    message(
                        if (it.storageType == null) "Not on blockchain"
                        else "Available as NFT"
                    )
                }
            }
        }
    }
}