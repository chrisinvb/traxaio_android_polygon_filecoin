package io.traxa.persistence.entities

import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.room.Entity
import androidx.room.PrimaryKey
import io.traxa.R


enum class ColorType(@ColorRes val colorRes: Int, @StringRes val colorName: Int) {
    RED(R.color.red, R.string.color_red),
    GREEN(R.color.green, R.string.color_green),
    YELLOW(R.color.yellow, R.string.color_yellow),
    BLUE(R.color.blue, R.string.color_blue),
    //PINK(R.color.pink),
    //WHITE(R.color.white, R.string.color_white),
    BLACK(R.color.black, R.string.color_black)
}

@Entity
data class ContainerColorStat(
    @PrimaryKey val color: ColorType,
    val count: Int
)