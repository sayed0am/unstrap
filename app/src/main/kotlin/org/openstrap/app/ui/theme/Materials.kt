package org.openstrap.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/*
 * The card materials of Glow × Glass (System and Elements boards, plan section 1, Materials). The boards' cards don't
 * blur what's behind them: the screen's light is already soft, so a card is a see-through fill with a lit top edge,
 * and only the nav bar, sheets and popups blur (Haze, in components/Glass.kt). That keeps a long scroll cheap and looks
 * the same on Android 11, which can't blur.
 */

/** The page ground under the screen's light. */
val Ground = Color(0xFF030304)

/** Clear glass's fill: white at 7%. */
const val CLEAR_GLASS_FILL = 0.07f

/**
 * A lit edge that follows [shape]: [top] white along the top, falling to [ring] white by [reach] down the card, as the
 * boards' `inset 0 1px 0` highlight over a 1 px ring.
 */
fun Modifier.litEdge(shape: Shape, top: Float, ring: Float, reach: Float = 28f): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    val stroke = Stroke(1.dp.toPx())
    val fall = (reach.dp.toPx() / size.height.coerceAtLeast(1f)).coerceIn(0.01f, 1f)
    val brush = Brush.verticalGradient(
        0f to Color.White.copy(alpha = top), fall to Color.White.copy(alpha = ring), 1f to Color.White.copy(alpha = ring),
    )
    onDrawWithContent {
        drawContent()
        drawOutline(outline, brush, style = stroke)
    }
}

/** Clear glass (every card but the hero): white 7% over the screen's light, lit top edge 16%, ring 5%. */
fun Modifier.clearGlass(shape: Shape = GlowShapes.Card, fill: Float = CLEAR_GLASS_FILL): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val color = Color.White.copy(alpha = fill)
        onDrawBehind { drawOutline(outline, color) }
    }.litEdge(shape, top = 0.16f, ring = 0.05f)

/**
 * Tinted glass (the hero): the blend at low alpha across the card, first stop where the light comes from, fading to
 * white 5% at the far corner. Lit top edge 35%, ring 10%.
 */
fun Modifier.tintedGlass(blend: Blend, shape: Shape = GlowShapes.Card, strength: Float = 1f): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        // The boards' linear-gradient(40deg ...): from lower left to upper right.
        val brush = Brush.linearGradient(
            0f to blend.first.copy(alpha = 0.30f * strength),
            0.55f to blend.second.copy(alpha = 0.20f * strength),
            1f to Color.White.copy(alpha = 0.05f),
            start = Offset(0f, size.height), end = Offset(size.width, size.height - size.width * 0.84f),
        )
        onDrawBehind { drawOutline(outline, brush) }
    }.litEdge(shape, top = 0.35f, ring = 0.10f)

/**
 * Solid light, where the colour is the value (the Energy level card): the blend filling the card from the bottom up to
 * [level], second stop on top, dark behind the number so white reads on it. At most two on a screen.
 */
fun Modifier.solidLight(blend: Blend, level: Float = 1f, shape: Shape = GlowShapes.Card): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val top = (1f - level.coerceIn(0f, 1f))
        val edge = (top + 0.04f).coerceAtMost(1f)
        val fill = Brush.verticalGradient(
            0f to DarkGlowFill, top to DarkGlowFill.copy(alpha = 0.85f),
            edge to blend.second, 1f to blend.first,
        )
        // A dark pool behind the number, upper left, as the boards' radial-gradient.
        val poolCenter = Offset(size.width * 0.3f, size.height * 0.22f)
        val poolSquash = 0.52f / 1.1f
        val pool = Brush.radialGradient(
            0f to DarkGlowFill.copy(alpha = 0.97f), 0.5f to DarkGlowFill.copy(alpha = 0.88f), 1f to Color.Transparent,
            center = poolCenter, radius = size.width * 1.1f,
        )
        onDrawBehind {
            drawOutline(outline, fill)
            scale(1f, poolSquash, pivot = poolCenter) { drawOutline(outline, pool) }
        }
    }.litEdge(shape, top = 0.25f, ring = 0.05f)

/** Dark glow's ground: deep violet-black, so a blend rising from below reads as light, not paint. */
val DarkGlowFill = Color(0xFF1E0A2E)

/** Dark glow (strips and moments that rise from below): near black, with the blend lit from the bottom edge. */
fun Modifier.darkGlow(blend: Blend, shape: Shape = GlowShapes.Card, strength: Float = 1f): Modifier =
    this.drawWithCache {
        val outline = shape.createOutline(size, layoutDirection, this)
        val base = Color(0xFF0B0812)
        val rise = Brush.radialGradient(
            0f to blend.second.copy(alpha = 0.75f * strength), 0.55f to blend.first.copy(alpha = 0.25f * strength), 1f to Color.Transparent,
            center = Offset(size.width / 2, size.height * 1.05f), radius = size.width * 0.75f,
        )
        onDrawBehind {
            drawOutline(outline, base)
            drawOutline(outline, rise)
        }
    }.litEdge(shape, top = 0.12f, ring = 0.05f)

/* ------------------------------------------- The screen's light ----------------------------------------- */

/**
 * The screen's light (Matrix board): the hero's blend, both stops muted, spilled behind the screen. [angle] is where the
 * hero's light comes from, in degrees counterclockwise from the right (as LightAngle): the first stop sits that way,
 * the second opposite and lower, smaller, at 90% of the strength. [strength] is the mood (Day 1, Evening 0.8, Night
 * 0.65). [heroCenterY] is the hero's centre from the top of the screen, in dp.
 */
@Immutable
data class ScreenLight(val blend: Blend, val angle: Float = 225f, val strength: Float = 1f, val heroCenterY: Float = 200f) {
    companion object {
        /** The mood's strength (Matrix board, The rule: How strong). */
        const val DAY = 1f
        const val EVENING = 0.8f
        const val NIGHT = 0.65f
    }
}

/** The light a screen is lit by, for parts (sheets, the nav bar) that want to know. Null when the screen has none. */
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage") // a design token the sheets and nav bar read
val LocalScreenLight = compositionLocalOf<ScreenLight?> { null }

/**
 * Each tab's light as its screen last drew it, so the nav bar outside the pages can tint the selected tab with it
 * (TabTodayMorning). Provided at the root; a tab page reports through [LocalScreenLightReport].
 */
class TabLights {
    val lights = androidx.compose.runtime.mutableStateMapOf<Int, ScreenLight?>()
}

val LocalTabLights = androidx.compose.runtime.staticCompositionLocalOf<TabLights?> { null }

/** Where a screen reports the light it draws ([GlowScreenList] calls it). Nothing listens outside a tab. */
val LocalScreenLightReport = androidx.compose.runtime.staticCompositionLocalOf<(ScreenLight?) -> Unit> { {} }

/**
 * How far the list behind a screen's light has scrolled, in px, so the light can go with the hero (decision changed
 * on the phone, 13 Nov: the light comes from behind the hero and fades as you scroll away from it). The list sets
 * [scrolled]; the light reads it while drawing, so a scroll only redraws.
 */
class ScrollSource {
    var scrolled: (() -> Float)? by androidx.compose.runtime.mutableStateOf<(() -> Float)?>(null)
}

val LocalScrollSource = androidx.compose.runtime.staticCompositionLocalOf<ScrollSource?> { null }

/**
 * [state]'s distance from the top in px, as a function to call while drawing. Items scrolled past count at the size
 * they had when seen (or the average of those seen), so the distance keeps growing smoothly past the first item.
 */
@androidx.compose.runtime.Composable
fun rememberScrolled(state: androidx.compose.foundation.lazy.LazyListState): () -> Float {
    val sizes = androidx.compose.runtime.remember(state) { HashMap<Int, Int>() }
    return androidx.compose.runtime.remember(state) {
        {
            val info = state.layoutInfo
            val items = info.visibleItemsInfo
            items.forEach { sizes[it.index] = it.size }
            val first = items.firstOrNull()
            if (first == null) 0f else {
                val usual = if (sizes.isEmpty()) 0 else sizes.values.sum() / sizes.size
                var before = 0f
                for (i in 0 until first.index) before += (sizes[i] ?: usual) + info.mainAxisItemSpacing
                (before - first.offset).coerceAtLeast(0f)
            }
        }
    }
}

/** How much of the light is gone once the hero is [LIGHT_FADE_DISTANCE] behind, on top of it moving up. */
private const val LIGHT_FADE = 0.6f
private val LIGHT_FADE_DISTANCE = 600.dp

/** The boards' blob opacity at full strength. */
private const val LIGHT_OPACITY = 0.6f

/** The two blobs' widths (as a share of the screen width) and the second one's strength next to the first. */
private const val FIRST_BLOB_RX = 0.82f
private const val SECOND_BLOB_RX = 0.51f
private const val SECOND_BLOB_STRENGTH = 0.9f

/**
 * Draws [light] behind this, over [Ground]. The light goes with the hero: [scrolled] (px from the top) moves it up as
 * the list scrolls, and it fades as the hero goes. With no light, just the ground: "no light until there's a hero to
 * light" (Matrix board, No data yet).
 */
fun Modifier.screenLight(light: ScreenLight?, scrolled: () -> Float = { 0f }): Modifier = clipToBounds().drawBehind {
    drawRect(Ground)
    if (light == null) return@drawBehind
    val w = size.width
    val r = light.angle / 180f * PI.toFloat()
    val vx = cos(r); val vy = -sin(r)
    val gone = scrolled()
    val heroY = light.heroCenterY.dp.toPx() - gone
    val reachX = w * 0.32f; val reachY = 90.dp.toPx()
    val fade = 1f - LIGHT_FADE * (gone / LIGHT_FADE_DISTANCE.toPx()).coerceIn(0f, 1f)
    val a = light.strength * LIGHT_OPACITY * fade
    blob(
        Offset(w / 2 + vx * reachX, heroY + vy * reachY), w * FIRST_BLOB_RX + 120.dp.toPx(), 260.dp.toPx() + 120.dp.toPx(),
        light.blend.light, a,
    )
    blob(
        Offset(w / 2 - vx * reachX, heroY - vy * reachY + 360.dp.toPx()), w * SECOND_BLOB_RX + 120.dp.toPx(),
        190.dp.toPx() + 120.dp.toPx(), light.blend.lightSecond, a * SECOND_BLOB_STRENGTH,
    )
}

/** A blurred ellipse [rx] by [ry] around [c]: what the boards draw as a circle under `filter: blur(120px)`. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.blob(c: Offset, rx: Float, ry: Float, color: Color, alpha: Float) {
    val brush = Brush.radialGradient(
        0f to color.copy(alpha = alpha), 0.35f to color.copy(alpha = alpha * 0.9f), 0.6f to color.copy(alpha = alpha * 0.55f),
        0.8f to color.copy(alpha = alpha * 0.2f), 1f to Color.Transparent,
        center = c, radius = rx,
    )
    scale(1f, ry / rx, pivot = c) { drawCircle(brush, rx, c) }
}
