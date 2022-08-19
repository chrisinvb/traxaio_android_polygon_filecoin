package io.traxa.ui.onboard.steps

import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.DrawableRes
import androidx.fragment.app.Fragment
import io.traxa.databinding.FragmentShowcaseBinding

private const val ARG_TITLE = "title"
private const val ARG_SUBTITLE = "subtitle"
private const val ARG_IMAGE = "image"
private const val ARG_SCALE_TYPE = "scaleType"
private const val ARG_GRAVITY = "gravity"

/**
 * Fragment used to show a standard "title"/"subtitle"/"icon" view
 */
class ShowcaseFragment : Fragment() {

    private var title: String? = null
    private var subtitle: String? = null
    private var scaleType: ImageView.ScaleType? = null
    private var gravity: Int? = null

    @DrawableRes
    private var image: Int = -1

    private lateinit var binding: FragmentShowcaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            title = it.getString(ARG_TITLE)
            subtitle = it.getString(ARG_SUBTITLE)
            image = it.getInt(ARG_IMAGE)
            scaleType = ImageView.ScaleType.valueOf(it.getString(ARG_SCALE_TYPE)!!)
            gravity = it.getInt(ARG_GRAVITY)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentShowcaseBinding.inflate(inflater)
        binding.title = title
        binding.subtitle = subtitle
        binding.image = image

        binding.imgIllustration.scaleType = scaleType
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.imgIllustration.scaleType
    }

    companion object {

        @JvmStatic
        fun newInstance(title: String,
                        subtitle: String,
                        @DrawableRes image: Int,
                        scaleType: ImageView.ScaleType = ImageView.ScaleType.CENTER_INSIDE,
                        gravity: Int = Gravity.CENTER
        ) =
            ShowcaseFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TITLE, title)
                    putString(ARG_SUBTITLE, subtitle)
                    putInt(ARG_IMAGE, image)
                    putString(ARG_SCALE_TYPE, scaleType.name)
                    putInt(ARG_GRAVITY, gravity)
                }
            }
    }
}