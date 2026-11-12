package org.openstrap.app.ui.glow

import android.provider.Settings
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.exp
import kotlin.math.floor
import kotlinx.coroutines.flow.first
import org.openstrap.app.ui.components.LocalCardNumbers
import org.openstrap.app.ui.components.cardOpening
import org.openstrap.app.ui.components.pressClickable
import org.openstrap.app.ui.glow.draw.GlowMotion
import org.openstrap.app.ui.glow.draw.GlowStyle
import org.openstrap.app.ui.glow.draw.LightAngle
import org.openstrap.app.ui.glow.draw.glowLevel
import org.openstrap.app.ui.glow.draw.lightVector
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.DarkGlowFill
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.darkGlow
import org.openstrap.app.ui.theme.litEdge

/**
 * A card in the Glow x Glass language (System and Matrix boards). The v2 rendered glow (rim light, dark core, tone band,
 * grain) is gone. What a [GlowStyle] draws now:
 *
 *  - a level style ([GlowStyle.lightLevel]): solid light, the style colour's blend filling the card's long side up to
 *    the level (upward on a Small or Tall, left to right on a Wide or the hero) with a dark pool behind the number;
 *  - [GlowStyle.second] or [GlowStyle.dark]: dark glow, the blend rising from the bottom edge;
 *  - [GlowStyle.outline]: clear glass with a ring in the colour (a workout on hold);
 *  - anything else (the hero): tinted glass, the blend's first stop on the side the light comes from.
 *
 * The blend is [Blends.forInk] of [GlowStyle.color], or the colour on its own where none matches. The screen's
 * [LocalGlowMood] dims tinted and dark glow (a level ignores it). A live [GlowMotion.Heartbeat] lifts the tint's
 * brightness gently with each beat, and never moves anything.
 */
/**
 * Whether glow cards should hold still right now; a scrolling screen provides its scroll state, so
 * the pulse pauses while the list moves and the scroll gets every frame to itself.
 */
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage")
val LocalGlowPaused = staticCompositionLocalOf<() -> Boolean> { { false } }

/**
 * Whether the page holding these cards is off screen or the tab pager is moving; provided by the pager, apart from
 * [LocalGlowPaused] so a screen's own scroll state doesn't override it.
 */
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage")
val LocalGlowPagePaused = staticCompositionLocalOf<() -> Boolean> { { false } }

@Composable
fun GlowCard(
    style: GlowStyle,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = GlowLayout.CardPadding,
    /** A passing lift of a level card (a personal best) to its [GlowSwell.peak], drawn as the level rises. */
    swell: GlowSwell? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val mood = LocalGlowMood.current
    val shape = GlowShapes.Card
    val blend = remember(style.color) { Blends.forInk(style.color) ?: Blend(style.color, style.color) }
    val level = glowLevel(style.lightLevel)
    val angle = remember(style, mood) { cardAngle(style, mood.belowOnly) }
    val strength = if (level != null) 1f else style.glowStrength * mood.glow

    // The heartbeat: beats counted frame by frame at the bpm of the moment, read only while drawing.
    val beats = remember { mutableFloatStateOf(0f) }
    val bpm by rememberUpdatedState((style.motion as? GlowMotion.Heartbeat)?.bpm ?: 0)
    val context = LocalContext.current
    val animate = style.motion is GlowMotion.Heartbeat && level == null && remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) > 0f
    }
    val listPaused by rememberUpdatedState(LocalGlowPaused.current)
    val pagePaused by rememberUpdatedState(LocalGlowPagePaused.current)
    if (animate) LaunchedEffect(Unit) {
        val paused = { listPaused() || pagePaused() }
        var last = withFrameNanos { it }
        while (true) {
            if (paused()) {
                snapshotFlow { paused() }.first { !it }
                last = withFrameNanos { it }
            }
            withFrameNanos { now ->
                beats.floatValue += (now - last) / NANOS_PER_SECOND * bpm / SECONDS_PER_MINUTE
                last = now
            }
        }
    }

    // Tapped, the card grows into what it opens (spec 3.5).
    val opening = cardOpening(onClick, shape)
    Box(
        modifier
            .then(opening?.modifier ?: Modifier)
            .let { if (onClick != null) it.pressClickable(opening?.onClick ?: onClick, shape) else it }
            .clip(shape)
            .let {
                when {
                    style.outline -> it.clearGlassRing(blend, shape)
                    level != null -> it.levelLight(blend, level, swell, shape)
                    style.second || style.dark -> it.darkGlow(blend, shape, strength)
                    else -> it.heroGlass(blend, angle, strength, shape)
                }
            }
            .let { if (animate) it.beatPulse(blend, { beats.floatValue }) else it },
        // Content gets the card's full size, e.g. to centre itself in a fixed-height card.
        propagateMinConstraints = true,
    ) {
        CompositionLocalProvider(LocalCardNumbers provides opening?.numbers, LocalCardOpens provides (onClick != null)) {
            Box(Modifier.padding(contentPadding), content = content)
        }
    }
}

/**
 * A passing lift of a level card's light to [peak] (its [GlowStyle.lightLevel] at the top), shown as much as [amount]
 * says, 0 to 1. [amount] is read only while drawing, so an animated swell recomposes nothing.
 */
@androidx.compose.runtime.Immutable
class GlowSwell(val peak: Float, val amount: () -> Float)

/** Where the light comes from for [style], degrees as [LightAngle]: its own angle, else the old top or bottom, else lower left. */
private fun cardAngle(style: GlowStyle, belowOnly: Boolean): Float {
    val own = style.lightAngle ?: when (style.lightFrom) {
        org.openstrap.app.ui.glow.draw.LightFrom.Top -> LightAngle.Above
        org.openstrap.app.ui.glow.draw.LightFrom.Bottom -> LightAngle.Below
        org.openstrap.app.ui.glow.draw.LightFrom.All -> LightAngle.LowerLeft
    }
    return if (belowOnly) LightAngle.Below else own
}

/**
 * Tinted glass lit from [angle] (the boards' `linear-gradient(40deg, first 30%, second 20%, white 5%)`): the first stop
 * on the light's side, the second past the middle, white 5% at the far corner. Lit top edge 35%, ring 10%.
 */
private fun Modifier.heroGlass(blend: Blend, angle: Float, strength: Float, shape: androidx.compose.ui.graphics.Shape): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val (vx, vy) = lightVector(angle)
        // The gradient line runs through the centre, from the light's corner to the opposite one.
        val c = Offset(size.width / 2f, size.height / 2f)
        val reach = Offset(vx * size.width / 2f, vy * size.height / 2f)
        val brush = Brush.linearGradient(
            0f to blend.first.copy(alpha = (HERO_FIRST * strength).coerceIn(0f, 1f)),
            0.55f to blend.second.copy(alpha = (HERO_SECOND * strength).coerceIn(0f, 1f)),
            1f to Color.White.copy(alpha = 0.05f),
            start = c + reach, end = c - reach,
        )
        onDrawBehind { drawOutline(outline, brush) }
    }.litEdge(shape, top = 0.35f, ring = 0.10f)

private const val HERO_FIRST = 0.30f
private const val HERO_SECOND = 0.20f

/** An outline card (on hold): clear glass with a 1.5 dp ring in the blend's first stop. */
private fun Modifier.clearGlassRing(blend: Blend, shape: androidx.compose.ui.graphics.Shape): Modifier =
    drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val ring = Stroke(3.dp.toPx())
        val fill = Color.White.copy(alpha = 0.04f)
        val color = blend.first.copy(alpha = 0.85f)
        onDrawBehind {
            drawOutline(outline, fill)
            // The card is clipped to its shape, so half the stroke shows.
            drawOutline(outline, color, style = ring)
        }
    }

/**
 * Solid light (Matrix board, Level fill): [blend] filling the card's long side up to [level], second stop at the value,
 * then a short fade to dark, with a dark pool behind the number so white reads on it at any level. Upward on a Small
 * or Tall, left to right where the card is wider than tall. A [swell] lifts the level toward its peak while drawing.
 */
private fun Modifier.levelLight(blend: Blend, level: Float, swell: GlowSwell?, shape: androidx.compose.ui.graphics.Shape): Modifier =
    drawWithCache {
        val across = size.width > size.height * 1.2f
        onDrawBehind {
            val lifted = swell?.let { s ->
                val a = s.amount().coerceIn(0f, 1f)
                if (a > 0f) level + (s.peak.coerceIn(level, 1f) - level) * a else level
            } ?: level
            drawLevel(blend, lifted, across)
        }
    }.litEdge(shape, top = 0.25f, ring = 0.05f)

private fun DrawScope.drawLevel(blend: Blend, level: Float, across: Boolean) {
    val dark = DarkGlowFill
    val edge = (level + 0.04f).coerceAtMost(1f)
    val fill = if (across) {
        Brush.horizontalGradient(
            0f to blend.first, level to blend.second, edge to dark.copy(alpha = 0.85f), 1f to dark.copy(alpha = 0.85f),
        )
    } else {
        val top = 1f - level
        Brush.verticalGradient(
            0f to dark.copy(alpha = 0.85f), (top - 0.04f).coerceAtLeast(0f) to dark.copy(alpha = 0.85f),
            top to blend.second, 1f to blend.first,
        )
    }
    drawRect(fill)
    // The pool behind the number: 75% by 62% at 26% 40% across, 110% by 52% at 30% 22% up.
    val c = if (across) Offset(size.width * 0.26f, size.height * 0.40f) else Offset(size.width * 0.30f, size.height * 0.22f)
    val rx = size.width * (if (across) 0.75f else 1.10f)
    val ry = size.height * (if (across) 0.62f else 0.52f)
    val pool = Brush.radialGradient(
        0f to dark.copy(alpha = if (across) 0.9f else 0.97f), 0.55f to dark.copy(alpha = if (across) 0.62f else 0.88f),
        1f to Color.Transparent, center = c, radius = rx,
    )
    scale(1f, ry / rx, pivot = c) { drawCircle(pool, rx, c) }
}

/** A gentle lift of the tint with each heartbeat (live heart rate only): the blend's first stop, up to 12% over the card. */
private fun Modifier.beatPulse(blend: Blend, beats: () -> Float): Modifier = drawBehind {
    val c = contraction(beats())
    if (c > BEAT_MIN) drawRect(blend.first.copy(alpha = BEAT_LIFT * c))
}

private const val BEAT_LIFT = 0.12f
private const val BEAT_MIN = 0.01f
private const val NANOS_PER_SECOND = 1e9f
private const val SECONDS_PER_MINUTE = 60f

/**
 * How hard the heart is squeezing over one beat (phase in beats), 0..1: a quick lub, a smaller dub just after,
 * then rest until the next beat.
 */
private fun contraction(phase: Float): Float {
    val f = phase - floor(phase)
    fun squeeze(at: Float, rise: Float, fall: Float): Float {
        val d = f - at
        val w = if (d < 0f) rise else fall
        return exp(-(d / w).let { it * it })
    }
    return (squeeze(0.08f, 0.035f, 0.09f) + 0.55f * squeeze(0.30f, 0.03f, 0.08f)).coerceAtMost(1f)
}
