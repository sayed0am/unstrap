@file:Suppress("MatchingDeclarationName") // the file is the theme's Shapes; the object is the one shape set

package org.openstrap.app.ui.theme

import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import sv.lib.squircleshape.SquircleShape

/**
 * The corner values, live, so the debug corner panel can try others on the real screens. Cards and sheets take
 * [cardRadius]; every squircle takes [smoothing]. Tiles and icon buttons keep their own radii.
 */
object CornerTuning {
    const val CARD_RADIUS = 48f
    const val SMOOTHING = 35

    var cardRadius by mutableFloatStateOf(CARD_RADIUS)
    var smoothing by mutableIntStateOf(SMOOTHING)

    /** Debug builds: every level card's fill at this level, 0 to 1 (`--ei level 50`, `--ei level -1` to clear). */
    var levelOverride by mutableStateOf<Float?>(null)

    /** Whether the debug corner panel shows (debug builds, `--ez corners true`). */
    var panel by mutableStateOf(false)

    fun reset() {
        cardRadius = CARD_RADIUS
        smoothing = SMOOTHING
    }
}

/**
 * A squircle that reads [CornerTuning] each time it's outlined. [radius] null means the card radius. Equal by kind and
 * values, so a screen that reads a new one redraws its clips.
 */
private data class TunedSquircle(
    val radius: Float?, val percent: Int?, val topOnly: Boolean, val r: Float, val s: Int,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val smoothing = CornerTuning.smoothing
        val dp = (radius ?: CornerTuning.cardRadius).dp
        val shape = when {
            percent != null -> SquircleShape(percent = percent, smoothing = smoothing)
            topOnly ->
                SquircleShape(topStart = dp, topEnd = dp, bottomStart = 0.dp, bottomEnd = 0.dp, smoothing = smoothing)
            else -> SquircleShape(radius = dp, smoothing = smoothing)
        }
        return shape.createOutline(size, layoutDirection, density)
    }
}

private fun tuned(radius: Float? = null, percent: Int? = null, topOnly: Boolean = false): Shape =
    TunedSquircle(radius, percent, topOnly, CornerTuning.cardRadius, CornerTuning.smoothing)

/**
 * Every corner in the app (Glow × Glass, System board; plan section 1, Corners). Cards at 48 dp and squircles at
 * smoothing 35, chosen on the phone on 13 Nov as rounder than the canvas's 28 dp at 50. Fixed dp radii, so a Small
 * card and a tall hero in one column share a corner.
 */
object GlowShapes {
    /** How far the squircle's curve reaches into the straight edge (the library's 0 to 100). */
    const val SMOOTHING = CornerTuning.SMOOTHING

    /** Cards, panels and strips. */
    val Card: Shape get() = tuned()

    /** Sheets: the top corners only. */
    val Sheet: Shape get() = tuned(topOnly = true)

    /** Tiles inside a card. */
    val Tile: Shape get() = tuned(radius = 16f)

    /** Text buttons, chips, tags and the tab pill: half the height. */
    val Pill: Shape get() = tuned(percent = 50)

    /** Square icon buttons and text fields. */
    val Icon: Shape get() = tuned(radius = 14f)

    /** Small inner parts, bars and swatches: too small for a squircle to show, so plain rounded. */
    val Small: CornerBasedShape = RoundedCornerShape(4.dp)

    /** What Material's own controls (switches, menus, text fields, dialogs) pick up. */
    val Material = Shapes(
        extraSmall = Small,
        small = SquircleShape(radius = 14.dp, smoothing = SMOOTHING),
        medium = SquircleShape(radius = 16.dp, smoothing = SMOOTHING),
        large = SquircleShape(radius = CornerTuning.CARD_RADIUS.dp, smoothing = SMOOTHING),
        extraLarge = SquircleShape(percent = 50, smoothing = SMOOTHING),
    )
}
