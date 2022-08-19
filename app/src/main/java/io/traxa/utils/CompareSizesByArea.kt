package io.traxa.utils

import android.util.Size

/**
 * Compares two `Size`s based on their areas.
 */
internal class CompareSizesByArea : Comparator<Size> {

    override fun compare(lhs: Size, rhs: Size): Int {

        //Cast to ensure the multiplications won't overflow
        return java.lang.Long.signum((lhs.width * lhs.height - rhs.width * rhs.height).toLong())
    }
}