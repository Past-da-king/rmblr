package io.github.pastdaking.rmblr.ui.m3

import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin

/**
 * A closed shape described by its radius at each angle, 0 to 2 PI. The radius is 1 where
 * the outline touches the edge of its box.
 *
 * Describing shapes as `r(theta)` is what makes morphing trivial: two shapes blend by
 * blending their radii, so the cookie record button turns into the rounded-square stop
 * button with one fraction and no path matching.
 */
fun interface Radial {
    fun at(theta: Float): Float
}

private const val STEPS = 240

/** Scale a radial function so its largest radius is exactly 1. */
private fun normalised(f: (Float) -> Float): Radial {
    var max = 0f
    for (k in 0 until 360) max = maxOf(max, f((2 * PI * k / 360).toFloat()))
    return Radial { th -> f(th) / max }
}

private fun regularPolygon(sides: Int, rotation: Float, softness: Float): (Float) -> Float = { th ->
    val sector = (2 * PI / sides).toFloat()
    val a = (((th - rotation) % sector) + sector) % sector - sector / 2
    val p = (cos(PI / sides) / cos(a)).toFloat()
    p * (1 - softness) + softness
}

/** The shape library from the design: cookie, clover, puffy, gem, sunny, soft burst. */
object ExpressiveShapes {
    val Cookie9: Radial = normalised { th -> 1f + 0.075f * cos(9 * th) }
    val Cookie7: Radial = normalised { th -> 1f + 0.085f * cos(7 * th) }
    val Clover4: Radial = normalised { th -> 1f + 0.20f * cos(4 * th) }
    val SoftBurst: Radial = normalised { th -> 1f + 0.07f * cos(12 * th) + 0.015f * cos(24 * th) }
    val Sunny: Radial = normalised { th -> 1f + 0.11f * abs(cos(4 * th)).pow(0.6f) }
    val Flower: Radial = normalised { th -> 1f + 0.11f * cos(8 * th) + 0.02f * cos(16 * th) }
    val Puffy: Radial = normalised { th -> 1f + 0.12f * cos(5 * th - (PI / 2).toFloat()) }
    val Gem: Radial = normalised(regularPolygon(6, (PI / 6).toFloat(), 0.55f))
    val Circle: Radial = Radial { 1f }

    /** The stop button: a squircle whose flat sides touch the box. */
    val Squircle: Radial = Radial { th ->
        val c = abs(cos(th)).pow(5f)
        val s = abs(sin(th)).pow(5f)
        (c + s).pow(-1f / 5f)
    }

    /** All of the library by name, handy for avatars and empty states. */
    val Library: Map<String, Radial> = linkedMapOf(
        "cookie9" to Cookie9, "cookie7" to Cookie7, "clover4" to Clover4,
        "softburst" to SoftBurst, "sunny" to Sunny, "flower" to Flower,
        "puffy" to Puffy, "gem" to Gem
    )
}

/** Blend two radial shapes. 0 is [from], 1 is [to]. */
fun morph(from: Radial, to: Radial, fraction: Float): Radial {
    val f = fraction.coerceIn(0f, 1f)
    return Radial { th -> from.at(th) * (1 - f) + to.at(th) * f }
}

/** A Compose [Shape] from a [Radial], optionally rotated (radians). */
class RadialShape(private val radial: Radial, private val rotation: Float = 0f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val cx = size.width / 2
        val cy = size.height / 2
        for (k in 0..STEPS) {
            val th = (2 * PI * k / STEPS).toFloat()
            val r = radial.at(th - rotation)
            val x = cx + r * cos(th) * cx
            val y = cy + r * sin(th) * cy
            if (k == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        return Outline.Generic(path)
    }
}

/**
 * Corner radii for a row in a grouped list: the first row has a big top, the last a big
 * bottom, the ones between are tight, and a lone row is fully round. Returns
 * (topStart/topEnd, bottomStart/bottomEnd) in dp.
 */
fun groupedCorners(index: Int, count: Int, outer: Int = 28, inner: Int = 8): Pair<Int, Int> {
    require(count > 0 && index in 0 until count) { "index $index out of $count" }
    val top = if (index == 0) outer else inner
    val bottom = if (index == count - 1) outer else inner
    return top to bottom
}
