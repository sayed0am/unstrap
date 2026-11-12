package org.openstrap.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.ui.unit.dp
import sv.lib.squircleshape.SquircleShape

/**
 * Every corner in the app (Glow × Glass, System board; plan section 1, Corners). Squircles at smoothing 50, with fixed
 * dp radii so a Small card and a tall hero in one column share a corner. Built once, never during composition.
 */
object GlowShapes {
    /** How far the squircle's curve reaches into the straight edge (the library's 0 to 100). */
    const val SMOOTHING = 50

    /** Cards, panels and strips. */
    val Card: CornerBasedShape = SquircleShape(radius = 28.dp, smoothing = SMOOTHING)

    /** Sheets: the top corners only. */
    val Sheet: CornerBasedShape = SquircleShape(topStart = 28.dp, topEnd = 28.dp, bottomStart = 0.dp, bottomEnd = 0.dp, smoothing = SMOOTHING)

    /** Tiles inside a card. */
    val Tile: CornerBasedShape = SquircleShape(radius = 16.dp, smoothing = SMOOTHING)

    /** Text buttons, chips, tags and the tab pill: half the height. */
    val Pill: CornerBasedShape = SquircleShape(percent = 50, smoothing = SMOOTHING)

    /** Square icon buttons and text fields. */
    val Icon: CornerBasedShape = SquircleShape(radius = 14.dp, smoothing = SMOOTHING)

    /** Small inner parts, bars and swatches: too small for a squircle to show, so plain rounded. */
    val Small: CornerBasedShape = RoundedCornerShape(4.dp)

    /** What Material's own controls (switches, menus, text fields, dialogs) pick up. */
    val Material = Shapes(
        extraSmall = Small,
        small = SquircleShape(radius = 14.dp, smoothing = SMOOTHING),
        medium = SquircleShape(radius = 16.dp, smoothing = SMOOTHING),
        large = SquircleShape(radius = 28.dp, smoothing = SMOOTHING),
        extraLarge = Pill,
    )
}
