package io.traxa.ui.containers

import android.graphics.Color
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import io.traxa.containerCard
import io.traxa.containersDetail
import io.traxa.databinding.ActivityContainerYardBinding
import io.traxa.persistence.entities.ContainerColorStat
import io.traxa.utils.RecyclerBounceOverscroll
import kotlin.math.pow

class ContainerYardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityContainerYardBinding
    private val viewModel: ContainerYardViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityContainerYardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setup()
        listen()
    }

    private fun setup() {
        RecyclerBounceOverscroll().attach(binding.rvContainers)
    }

    private fun listen() {
        viewModel.containerColorStats.observe(this) {
            buildColorStats(it)
        }

        binding.toolbar.setNavigationOnClickListener { super.onBackPressed() }
    }

    private fun buildColorStats(list: List<ContainerColorStat>) {
        binding.rvContainers.withModels {
            containersDetail {
                id("details")

                val count = list.sumOf { it.count }
                val countDigits = "$count".length
                val netPower = 10.0.pow(countDigits)
                val nextRoundNumber = if (count < netPower/2) netPower/2 else netPower

                progress(list.sumOf { it.count })
                max(nextRoundNumber.toInt())
                title("$count total containers found")
                message("Reach ${nextRoundNumber.toInt()} scanned containers to reduce the upload waiting time!")
            }

            list.forEach {

                containerCard {

                    val countDigits = "${it.count}".length
                    val netPower = 10.0.pow(countDigits)
                    val nextRoundNumber = if (it.count < netPower/2) netPower/2 else netPower

                    id(it.color.ordinal)
                    colorName(getString(it.color.colorName))
                    message("${it.count} found")
                    progress(it.count)
                    max(nextRoundNumber.toInt())

                    progressColor(Color.WHITE)
                    progressBackgroundColor(Color.parseColor("#20FFFFFF"))

                    titleColor(Color.WHITE)
                    messageColor(Color.parseColor("#85FFFFFF"))
                    cardColor(ContextCompat.getColor(this@ContainerYardActivity, it.color.colorRes))
                }
            }
        }
    }
}