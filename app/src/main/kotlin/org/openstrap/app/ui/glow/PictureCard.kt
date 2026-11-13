package org.openstrap.app.ui.glow

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.Shader
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import org.openstrap.app.ui.components.LocalCardNumbers
import org.openstrap.app.ui.components.cardOpening
import org.openstrap.app.ui.components.pressClickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.lerp
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.clearGlass

/*
 * The middle tier (Picture card): clear glass (white 7%, lit top edge) that draws its metric's picture in the
 * blend. Plain when usual. Off your normal it takes the rim (Matrix board, Rim and tag): a 2 dp rim in the metric's
 * blend along the squircle edge, lit next to the part of the picture that is off and fading across the card, with a
 * faint wash of the first stop inside. Static: the rim only changes when the data does.
 */

/** A cool soft white for things that speak for no one metric: info sheets, the zone dots with no reading. */
val EdgeNeutral = Color.hsl(220f, 0.10f, 0.55f)

/** Where a [PictureCard]'s rim is lit: [org.openstrap.app.ui.glow.draw.Rim], drawn on a canvas by the widgets too. */
typealias Rim = org.openstrap.app.ui.glow.draw.Rim

/**
 * A screen's say over the next picture card: [plain] draws it as a list panel, a dark panel with a list's hairline,
 * for a card [varyLooks] moved to another look.
 */
@Immutable
data class CardOverride(val plain: Boolean = false)

// A layout's say over whichever card it places, several composables down: passing it would thread a parameter
// through every card, for the few a screen moves to another look.
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage")
val LocalCardOverride = staticCompositionLocalOf { CardOverride() }

private val CardShape = GlowShapes.Card

/** The rim's stroke, and how strong the ink is at its point, halfway out and at the end. */
private val RimWidth = 2.dp
private const val RIM_HALF_ALPHA = 0.35f
private const val RIM_HALF_AT = 0.55f

/** The wash inside the rim: the ink at 13%, over 80% of the rim's reach across and 70% down. */
private const val WASH_ALPHA = 0.13f
private const val WASH_REACH_X = 0.8f
private const val WASH_REACH_Y = 0.7f

/**
 * A picture card in [ink], its metric's colour (spec 2.5). With a [rim] it's off your normal and lights there; the
 * screen's [LocalGlowMood] softens the rim. [content] lays out the title row, the picture, the number and its tag.
 */
@Composable
fun PictureCard(
    ink: Color,
    modifier: Modifier = Modifier,
    rim: Rim? = null,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = GlowLayout.CardPadding,
    content: @Composable ColumnScope.() -> Unit,
) {
    val plain = LocalCardOverride.current.plain
    val blend = remember(ink) { Blends.forInk(ink) ?: Blend(ink, ink) }
    val mood = LocalGlowMood.current
    // Tapped, the card grows into what it opens (spec 3.5).
    val opening = cardOpening(onClick, CardShape)
    Column(
        modifier
            .then(opening?.modifier ?: Modifier)
            .let { if (onClick != null) it.pressClickable(opening?.onClick ?: onClick, CardShape) else it }
            .clip(CardShape)
            .clearGlass(CardShape)
            .let { if (!plain && rim != null) it.rimLight(rim, blend, mood.edge) else it }
            .padding(contentPadding),
    ) {
        CompositionLocalProvider(LocalCardNumbers provides opening?.numbers, LocalCardOpens provides (onClick != null)) { content() }
    }
}

/** The edge round a card or list panel ([GlowLayout.CardEdgeAlpha]). */
internal val CardHairline = Color.White.copy(alpha = GlowLayout.CardEdgeAlpha)

/** An elliptical radial gradient centred on [c] with radii [rx] and [ry], [colors] at [stops]. */
private fun ellipse(c: Offset, rx: Float, ry: Float, colors: List<Color>, stops: List<Float>) = object : ShaderBrush() {
    override fun createShader(size: Size): Shader =
        RadialGradientShader(c, rx, colors, stops).apply {
            setLocalMatrix(android.graphics.Matrix().apply { setScale(1f, ry / rx, c.x, c.y) })
        }
}

/**
 * The rim at [rim] in [blend], scaled by the mood's [strength]: a 2 dp stroke along the squircle edge, first stop at
 * the lit point, a step toward the second stop part way, then gone, plus a faint wash of the first stop inside.
 */
private fun Modifier.rimLight(rim: Rim, blend: Blend, strength: Float): Modifier = drawWithCache {
    val outline = CardShape.createOutline(size, layoutDirection, this)
    val c = Offset(rim.x * size.width, rim.y * size.height)
    val rx = (rim.reachX * size.width).coerceAtLeast(1f)
    val ry = (rim.reachY * size.height).coerceAtLeast(1f)
    val wash = ellipse(
        c, rx * WASH_REACH_X, ry * WASH_REACH_Y,
        listOf(blend.first.copy(alpha = WASH_ALPHA * strength), Color.Transparent), listOf(0f, 1f),
    )
    val edge = ellipse(
        c, rx, ry,
        listOf(
            blend.first.copy(alpha = strength),
            lerp(blend.first, blend.second, 0.5f).copy(alpha = RIM_HALF_ALPHA * strength),
            Color.Transparent,
        ),
        listOf(0f, RIM_HALF_AT, 1f),
    )
    // The card is clipped to its shape, so half of this stroke shows: the 2 dp rim.
    val stroke = Stroke(RimWidth.toPx() * 2f)
    onDrawBehind {
        drawRect(wash)
        drawOutline(outline, edge, style = stroke)
    }
}
