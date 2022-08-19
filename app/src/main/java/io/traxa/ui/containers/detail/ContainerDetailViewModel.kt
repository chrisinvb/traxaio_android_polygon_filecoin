package io.traxa.ui.containers.detail

import android.app.Application
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.view.ContextThemeWrapper
import android.view.View
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.map
import coil.load
import com.stfalcon.imageviewer.StfalconImageViewer
import io.traxa.BuildConfig
import io.traxa.persistence.AppDatabase
import io.traxa.services.Prefs
import io.traxa.utils.showMap
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import java.io.File
import java.net.URLConnection
import java.text.SimpleDateFormat
import java.util.*

class ContainerDetailViewModel(private val app: Application,
                               savedStateHandle: SavedStateHandle) : AndroidViewModel(app), KoinComponent {

    private val db: AppDatabase by inject()
    private val prefs: Prefs by inject()
    private val containerDao = db.containerDao()
    private val dateFormat = SimpleDateFormat("dd MMMM yyyy, hh:mm")

    private val uid = savedStateHandle.get<String>("uid")!!
    val container = containerDao.getByIdLiveData(uid).map {
        it.firstOrNull()
    }

    val isContainerOnBlockchain = container.map { it?.storageType != null }

    val containerId = container.map {
        it?.containerIds?.split(",")?.firstOrNull() ?: "Unknown"
    }

    val position = container.map {
        it?.containerPositions?.split(",")?.firstOrNull() ?: "Unknown"
    }

    val containerType = container.map {
        it?.containerType?.split(",")?.firstOrNull()?.lowercase()?.let { type ->
            if (type.startsWith("h")) "Horizontal"
            else "Vertical"
        } ?: "Unknown"
    }

    val date = container.map {
        if (it != null) dateFormat.format(Date(it.timestamp))
        else "Unknown"
    }

    val storageType = container.map { it?.storageType }

    val isImageVisible = container.map {
        if(it == null) return@map false
        val folder = app.getExternalFilesDir("thumbnails")
        val file = File(folder, "${it.uid}.jpg")
        file.exists()
    }

    fun openLocation(view: View) {
        val context = view.context
        val position = position.value?.split(" ")?.map { it.toDouble() }
        val lat = position?.firstOrNull()
        val lon = position?.lastOrNull()

        if (lat == null || lon == null || container.value?.storageType == null) return
        else context.showMap(lat, lon)
    }

    fun openImage(v: View) {
        val uid = container.value!!.uid
        val folder = app.getExternalFilesDir("thumbnails")

        val file = File(folder, "$uid.jpg")
        val fileGen = File(folder, "$uid-gen.jpg")

        val files = arrayOf(if(fileGen.exists()) fileGen else file)
        val contextTheme = ContextThemeWrapper(v.context, androidx.appcompat.R.style.Theme_AppCompat)

        StfalconImageViewer.Builder(contextTheme, files) { view, image ->
            view.load(image)
        }.show()
    }

    fun openStorage(view: View) {
        val storageLink = container.value?.storageLink ?: ""
        when (container.value?.storageType) {
            "arweave" -> {
                view.context.startActivity(Intent.parseUri("https://arweave.net/$storageLink/", 0))
            }
            "nft.storage" -> {
                view.context.startActivity(Intent.parseUri(storageLink, 0))
            }
            else -> view.context.startActivity(Intent.parseUri(storageLink, 0))
        }
    }

    fun openSupport(view: View) {
        val captureKey = container.value!!.recordingId
        view.context.startActivity(
            Intent.parseUri(
                "https://support.traxa.io/?profileId=${prefs.getPlayerId()}&captureKey=$captureKey",
                0
            )
        )
    }

    fun shareImage() {
        val uid = container.value!!.uid
        val folder = app.getExternalFilesDir("thumbnails")
        val file = File(folder, "$uid.jpg")
        val intent = Intent(Intent.ACTION_SEND)

        if (file.exists()) {

            val uri = FileProvider.getUriForFile(
                app,
                "io.traxa.capture.provider",
                file
            )

            intent.type = URLConnection.guessContentTypeFromName(file.name)
            intent.putExtra(Intent.EXTRA_STREAM, uri)
            intent.putExtra(Intent.EXTRA_SUBJECT, "Traxa.io - Container Image")

            app.startActivity(
                Intent.createChooser(intent, "Share image")
                    .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }
    }

    fun copyDebugInfo() {
        val container = container.value!!
        val date = SimpleDateFormat("z", Locale.getDefault())
        val localTime: String = date.format(Date())

        var logString = "Traxa version " + BuildConfig.VERSION_CODE + "\n\n"
        logString += "Capture Id: ${container.recordingId}" + "\n"
        logString += "Capture timestamp: ${container.timestamp}" + "\n"
        logString += "Mint timestamp: ${container.mintTimestamp}" + "\n"
        logString += "Timezone: $localTime"

        val clipboard = ContextCompat.getSystemService(app, ClipboardManager::class.java)
        val clip = ClipData.newPlainText("label", logString)
        if (clipboard != null && clip != null) {
            clipboard.setPrimaryClip(clip)
            Toast.makeText(app, "Copied!", Toast.LENGTH_SHORT).show()
        }
    }
}