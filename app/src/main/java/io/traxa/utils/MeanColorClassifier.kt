package io.traxa.utils

import android.graphics.Color
import io.traxa.persistence.entities.ColorType
import java.util.*
import kotlin.collections.HashMap
import kotlin.math.sqrt

class MeanColorClassifier {

    companion object {
        private val AVERAGE_DATA: EnumMap<ColorType, IntArray> = EnumMap(ColorType::class.java)

        init {
            val trainingData: HashMap<IntArray, ColorType> = HashMap()

            trainingData[intArrayOf(13, 11, 12)] = ColorType.BLACK
            trainingData[intArrayOf(18, 17, 17)] = ColorType.BLACK
            trainingData[intArrayOf(0, 0, 0)] = ColorType.BLACK
            trainingData[intArrayOf(31, 30, 30)] = ColorType.BLACK
            trainingData[intArrayOf(26, 9, 9)] = ColorType.BLACK
            trainingData[intArrayOf(36, 29, 29)] = ColorType.BLACK

            trainingData[intArrayOf(50, 60, 207)] = ColorType.BLUE
            trainingData[intArrayOf(3, 18, 252)] = ColorType.BLUE
            trainingData[intArrayOf(0, 10, 173)] = ColorType.BLUE
            trainingData[intArrayOf(2, 9, 120)] = ColorType.BLUE
            trainingData[intArrayOf(82, 127, 199)] = ColorType.BLUE
            trainingData[intArrayOf(78, 34, 224)] = ColorType.BLUE
            trainingData[intArrayOf(64, 153, 227)] = ColorType.BLUE
            trainingData[intArrayOf(115, 191, 255)] = ColorType.BLUE
            trainingData[intArrayOf(0, 187, 255)] = ColorType.BLUE
            trainingData[intArrayOf(67, 56, 217)] = ColorType.BLUE

//            trainingData[intArrayOf(252, 3, 207)] = ColorType.PINK
//            trainingData[intArrayOf(738, 984, 1008)] = ColorType.PINK
//            trainingData[intArrayOf(741, 991, 1026)] = ColorType.PINK
//            trainingData[intArrayOf(753, 1010, 1060)] = ColorType.PINK
//            trainingData[intArrayOf(780, 1039, 1081)] = ColorType.PINK
//            trainingData[intArrayOf(784, 1041, 1080)] = ColorType.PINK
//            trainingData[intArrayOf(784, 1046, 1123)] = ColorType.PINK
//            trainingData[intArrayOf(787, 1044, 1086)] = ColorType.PINK
//            trainingData[intArrayOf(792, 1054, 1103)] = ColorType.PINK
//            trainingData[intArrayOf(803, 1069, 1120)] = ColorType.PINK

            trainingData[intArrayOf(56, 217, 115)] = ColorType.GREEN
            trainingData[intArrayOf(112, 255, 165)] = ColorType.GREEN
            trainingData[intArrayOf(0, 191, 71)] = ColorType.GREEN
            trainingData[intArrayOf(0, 255, 4)] = ColorType.GREEN
            trainingData[intArrayOf(115, 255, 117)] = ColorType.GREEN
            trainingData[intArrayOf(34, 120, 35)] = ColorType.GREEN
            trainingData[intArrayOf(12, 77, 13)] = ColorType.GREEN
            trainingData[intArrayOf(12, 77, 29)] = ColorType.GREEN
            trainingData[intArrayOf(0, 61, 16)] = ColorType.GREEN
            trainingData[intArrayOf(47, 110, 64)] = ColorType.GREEN

            trainingData[intArrayOf(240, 26, 48)] = ColorType.RED
            trainingData[intArrayOf(217, 0, 22)] = ColorType.RED
            trainingData[intArrayOf(237, 0, 24)] = ColorType.RED
            trainingData[intArrayOf(240, 48, 80)] = ColorType.RED
            trainingData[intArrayOf(168, 8, 35)] = ColorType.RED
            trainingData[intArrayOf(214, 26, 26)] = ColorType.RED

            trainingData[intArrayOf(246, 255, 0)] = ColorType.YELLOW
            trainingData[intArrayOf(247, 255, 38)] = ColorType.YELLOW
            trainingData[intArrayOf(198, 204, 29)] = ColorType.YELLOW
            trainingData[intArrayOf(163, 168, 17)] = ColorType.YELLOW
            trainingData[intArrayOf(242, 247, 96)] = ColorType.YELLOW
            trainingData[intArrayOf(213, 255, 25)] = ColorType.YELLOW
            trainingData[intArrayOf(150, 184, 0)] = ColorType.YELLOW
            trainingData[intArrayOf(210, 252, 23)] = ColorType.YELLOW

//            trainingData[intArrayOf(255, 255, 255)] = ColorType.WHITE
//            trainingData[intArrayOf(240, 240, 240)] = ColorType.WHITE
//            trainingData[intArrayOf(237, 202, 202)] = ColorType.WHITE
//            trainingData[intArrayOf(196, 171, 171)] = ColorType.WHITE
//            trainingData[intArrayOf(200, 204, 190)] = ColorType.WHITE
//            trainingData[intArrayOf(182, 184, 176)] = ColorType.WHITE
//            trainingData[intArrayOf(246, 255, 219)] = ColorType.WHITE
//            trainingData[intArrayOf(224, 245, 255)] = ColorType.WHITE
//            trainingData[intArrayOf(209, 240, 255)] = ColorType.WHITE
//            trainingData[intArrayOf(255, 209, 209)] = ColorType.WHITE

            AVERAGE_DATA.clear()
            for (color in ColorType.values()) {

                var count = 0
                var r = 0
                var g = 0
                var b = 0

                for (entry in trainingData.entries) {
                    val value = entry.value
                    val rgb = entry.key

                    if (value == color) {
                        r += rgb[0]
                        g += rgb[1]
                        b += rgb[2]
                        count++
                    }
                }

                r /= count
                g /= count
                b /= count

                val rgb = intArrayOf(r, g, b)
                AVERAGE_DATA[color] = rgb
            }
        }

        fun init() { }

        fun classify(color: Int): ColorType {
            var minDistance = Long.MAX_VALUE.toDouble()
            var minName: ColorType = ColorType.RED

            for ((key, rgb) in AVERAGE_DATA.entries) {
                val rd = Color.red(color) - rgb[0]
                val gd = Color.green(color) - rgb[1]
                val bd = Color.blue(color) - rgb[2]
                val distance = sqrt((rd * rd + gd * gd + bd * bd).toDouble())
                if (distance >= minDistance) continue

                minDistance = distance
                minName = key
            }

            return minName
        }
    }
}