package io.traxa.utils

import android.graphics.drawable.Drawable
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.appcompat.widget.AppCompatImageView
import androidx.core.view.isVisible
import androidx.databinding.BindingAdapter
import coil.load
import coil.transform.CircleCropTransformation
import com.google.android.material.button.MaterialButton
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.io.File

object BindingAdapters {

    @BindingAdapter("app:srcCompat")
    @JvmStatic
    fun setBackgroundColor(view: AppCompatImageView, @DrawableRes resource: Int) {
        if (resource != -1) view.setImageResource(resource)
    }

    @BindingAdapter("app:containerImageId")
    @JvmStatic
    fun setContainerImageId(view: AppCompatImageView, containerId: String?) {
        val folder = view.context.getExternalFilesDir("thumbnails")
        val file = File(folder, "$containerId.jpg")
        val fileGen = File(folder, "$containerId-gen.jpg")

        if(fileGen.exists()) {
            view.tag = "picture"
            view.colorFilter = null
            view.load(fileGen) {
                crossfade(true)
            }
        }else if (file.exists()) {
            view.tag = "picture"
            view.colorFilter = null
            view.load(file) {
                crossfade(true)
            }
        }
    }

    @BindingAdapter("app:containerImageIdCircle")
    @JvmStatic
    fun setContainerImageIdCircle(view: AppCompatImageView, containerId: String?) {
        val folder = view.context.getExternalFilesDir("thumbnails")
        val file = File(folder, "$containerId.jpg")
        val fileGen = File(folder, "$containerId-gen.jpg")

        if(fileGen.exists()) {
            view.tag = "picture"
            view.colorFilter = null
            view.load(fileGen) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        }else if (file.exists()) {
            view.tag = "picture"
            view.colorFilter = null
            view.load(file) {
                crossfade(true)
                transformations(CircleCropTransformation())
            }
        }
    }

    @BindingAdapter("app:tint")
    @JvmStatic
    fun setTint(imageView: AppCompatImageView, color: Int) {
        if(imageView.tag != "picture")
            imageView.setColorFilter(color, android.graphics.PorterDuff.Mode.MULTIPLY)
    }

    @BindingAdapter("android:src")
    @JvmStatic
    fun setImageSrc(imageView: AppCompatImageView, @DrawableRes resource: Int) {
        imageView.setImageResource(resource)
    }

    @BindingAdapter("app:drawableStartCompat")
    @JvmStatic
    fun setDrawableStart(textView: TextView, @DrawableRes resource: Int) {
        if (resource != -1) textView.setCompoundDrawablesWithIntrinsicBounds(resource, 0, 0, 0)
    }

    @BindingAdapter("app:indicatorColor")
    @JvmStatic
    fun setIndicatorColor(progress: LinearProgressIndicator, color: Int) {
        progress.setIndicatorColor(color)
    }

    @BindingAdapter("app:icon")
    @JvmStatic
    fun setIcon(button: MaterialButton, drawable: Drawable?) {
        if(drawable != null) button.icon = drawable
    }

    @BindingAdapter("android:indeterminate")
    @JvmStatic
    fun setIndeterminate(linearProgressIndicator: LinearProgressIndicator, indeterminate: Boolean) {
        if (linearProgressIndicator.isIndeterminate != indeterminate) {
            linearProgressIndicator.isVisible = false
            linearProgressIndicator.isIndeterminate = indeterminate
            linearProgressIndicator.isVisible = true
        }
    }

    @BindingAdapter("android:progress")
    @JvmStatic
    fun setProgress(linearProgressIndicator: LinearProgressIndicator, progress: Int) {
        if (!linearProgressIndicator.isIndeterminate)
            linearProgressIndicator.progress = progress
    }
}