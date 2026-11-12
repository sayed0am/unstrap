package org.openstrap.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.ui.graphics.graphicsLayer
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import org.openstrap.app.ui.theme.Ground
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.ScreenLight
import org.openstrap.app.ui.theme.LocalScreenLight
import org.openstrap.app.ui.theme.litEdge
import org.openstrap.app.ui.theme.screenLight
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Velocity
import kotlinx.coroutines.launch

/** The nav bar's glass: near-black, see-through, with a lit top edge. Popups use it over a blurred screen. */
val GlassTint = Color(0xFF0B0B0D)
private val ContainerDim = Color(0xFF121215)
private val ContainerMid = Color(0xFF16161A)
private val ContainerHigh = Color(0xFF1E1E22)
val GlassEdge = BorderStroke(
    1.dp, Brush.verticalGradient(0f to Color.White.copy(alpha = 0.2f), 0.5f to Color.White.copy(alpha = 0.05f), 1f to Color.Black.copy(alpha = 0.4f)),
)

/**
 * Material parts on the screen light (Glow × Glass): the ground is #030304 and the containers that Material paints
 * behind its parts are see-through, so no black slab sits over a page's light. Menus and sheets keep an opaque dark
 * container, since they must be readable over anything.
 */
@Composable
fun OledSurfaces(content: @Composable () -> Unit) {
    val c = MaterialTheme.colorScheme
    MaterialTheme(
        colorScheme = c.copy(
            background = Ground,
            surface = Color.Transparent,
            surfaceContainerLowest = Color.Transparent,
            surfaceContainerLow = Color.Transparent,
            surfaceContainer = ContainerDim,
            surfaceContainerHigh = ContainerMid,
            surfaceContainerHighest = ContainerHigh,
            outline = Color.White.copy(alpha = 0.16f),
            outlineVariant = Color.White.copy(alpha = 0.12f),
        ),
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
    )
}

/* ------------------------------------------- The page's screen light -------------------------------------- */

/** Holds the screen light a page over a tab asks for. Read by [PageLightHost], set by [PageLight]. */
@Stable
class PageLightState {
    var light: ScreenLight? by mutableStateOf(null)
}

/** The page's light holder, null outside a [PageLightHost]. */
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage") // a page deep in a stack sets the light its host draws
val LocalPageLight = staticCompositionLocalOf<PageLightState?> { null }

/**
 * Draws the screen light of the page inside it, over [Ground] (the plain ground until a page sets one), fixed to the
 * screen. A detail or a page over a tab sets its light from inside with [PageLight]. Wrap each page once: the host
 * belongs to one stack entry, so a page sliding in or out keeps its own light.
 */
@Composable
fun PageLightHost(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val state = remember { PageLightState() }
    CompositionLocalProvider(LocalPageLight provides state, LocalScreenLight provides state.light) {
        Box(modifier.fillMaxSize().screenLight(state.light)) { content() }
    }
}

/**
 * Sets the screen light for the page this is called in (the hero's blend, mood and angle). Cleared when the page
 * leaves. Does nothing outside a [PageLightHost]. Pass null for the plain ground.
 */
@Composable
fun PageLight(light: ScreenLight?) {
    val state = LocalPageLight.current ?: return
    SideEffect { state.light = light }
    DisposableEffect(state) { onDispose { state.light = null } }
}

/* ------------------------------------------- In-app overlay ----------------------------------------- */

/**
 * Popups drawn inside the app's own window, over a blur of the page, like the nav bar. A popup in its own window
 * can only blur what's behind it when the phone allows cross-window blur (many don't); drawn here, the blur
 * always works. Popups declared anywhere register here and the root draws them on top of everything.
 */
class GlassHost {
    internal val layers = androidx.compose.runtime.mutableStateListOf<GlassLayer>()
}

internal class GlassLayer(
    val sheet: Boolean,
    val onDismiss: androidx.compose.runtime.State<() -> Unit>,
    val content: androidx.compose.runtime.State<@Composable () -> Unit>,
)

/** The overlay host, provided at the app's root. */
val LocalGlassHost = staticCompositionLocalOf<GlassHost> { error("No GlassHost provided") }

/** Shows [content] in the overlay for as long as this is in the composition. */
@Composable
private fun GlassPortal(host: GlassHost, sheet: Boolean, onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val d = rememberUpdatedState(onDismiss)
    val c = androidx.compose.runtime.rememberUpdatedState(content)
    DisposableEffect(host) {
        val layer = GlassLayer(sheet, d, c)
        host.layers += layer
        onDispose { host.layers -= layer }
    }
}

/** The sheet material's fill (the boards' rgba(22,22,26)). */
private val SheetFill = Color(0xFF16161A)

/** The boards' dim behind a sheet or popup: black at 58%. */
private const val BACKDROP_DIM = 0.58f

/** The haze the overlay's sheets and popups blur (the pages' source), for [GlassPanel]. */
@Suppress("CompositionLocalAllowlist", "ComposeCompositionLocalUsage") // a design token shared by every panel in the overlay
val LocalGlassHaze = staticCompositionLocalOf<HazeState?> { null }

/**
 * Heavy glass (Sheet boards): the page behind blurred 40 dp, saturated 150%, under a 78% dark fill. The boards'
 * System panel draws 28 dp and 170% for a card-sized panel, the sheet boards 40 and 150, and the sheets win here.
 * Before Android 12, which has no blur, the fill is near solid.
 */
private val HeavyGlass = HazeBlurStyle {
    blurRadius(40.dp)
    backgroundColor(Ground)
    colorEffects(
        listOf(
            HazeColorEffect.colorFilter(ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.5f) })),
            HazeColorEffect.tint(SheetFill.copy(alpha = 0.78f)),
        ),
    )
    fallbackColorEffect(HazeColorEffect.tint(SheetFill.copy(alpha = 0.96f)))
}

/** Draws the registered popups; place it last at the root, over everything, with the pages' [haze]. */
@Composable
fun GlassOverlay(host: GlassHost, haze: dev.chrisbanes.haze.HazeState) {
    host.layers.forEach { layer ->
        androidx.compose.runtime.key(layer) {
            androidx.activity.compose.BackHandler { layer.onDismiss.value() }
            val shown = androidx.compose.runtime.remember { androidx.compose.animation.core.Animatable(0f) }
            androidx.compose.runtime.LaunchedEffect(Unit) { shown.animateTo(1f, androidx.compose.animation.core.tween(220)) }
            val tap = androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
            Box(
                Modifier.fillMaxSize()
                    .graphicsLayer { alpha = shown.value }
                    .background(Color.Black.copy(alpha = BACKDROP_DIM))
                    .clickable(tap, indication = null) { layer.onDismiss.value() },
            )
            Box(
                Modifier.fillMaxSize().imePadding().let { if (layer.sheet) it else it.systemBarsPadding().padding(24.dp) }
                    .graphicsLayer {
                        alpha = shown.value
                        translationY = (1f - shown.value) * (if (layer.sheet) 120.dp.toPx() else 16.dp.toPx())
                    },
                contentAlignment = if (layer.sheet) Alignment.BottomCenter else Alignment.Center,
            ) {
                // The panel takes its own taps, so tapping it doesn't dismiss.
                Box(Modifier.clickable(androidx.compose.runtime.remember { androidx.compose.foundation.interaction.MutableInteractionSource() }, indication = null) {}) {
                    CompositionLocalProvider(LocalGlassHaze provides haze) { layer.content.value() }
                }
            }
        }
    }
}

/**
 * The heavy glass a sheet or popup sits on: the page behind it blurred under a dark fill, with a lit top edge. Popups
 * take [GlowShapes.Card], sheets [GlowShapes.Sheet]. Outside the overlay (no page to blur) and before Android 12 it is
 * a near solid fill.
 */
@Composable
fun GlassPanel(modifier: Modifier = Modifier, shape: androidx.compose.ui.graphics.Shape = GlowShapes.Card, content: @Composable () -> Unit) {
    val haze = LocalGlassHaze.current
    Box(
        modifier.clip(shape)
            .let {
                if (haze != null) it.hazeBlur(HazeInput.Sources(haze), HeavyGlass)
                else it.background(SheetFill.copy(alpha = 0.96f))
            }
            .litEdge(shape, top = 0.2f, ring = 0f, reach = 3f),
    ) {
        CompositionLocalProvider(LocalContentColor provides Color.White, content = content)
    }
}

/**
 * A bottom sheet as glass in the overlay, at most [org.openstrap.app.ui.glow.GlowLayout.PageMaxWidth] wide and centred.
 * Pulled down by its handle, or past the top of its list, it follows the
 * finger; let go far enough down (or flung) and it closes, otherwise it springs back.
 */
@Composable
fun GlassSheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    GlassPortal(LocalGlassHost.current, sheet = true, onDismiss = onDismiss) {
        val scope = rememberCoroutineScope()
        val density = LocalDensity.current
        val dismiss = rememberUpdatedState(onDismiss)
        val pull = remember { mutableFloatStateOf(0f) }
        val release: (Float) -> Unit = { velocity ->
            if (pull.floatValue > with(density) { 120.dp.toPx() } || velocity > with(density) { 800.dp.toPx() }) dismiss.value()
            else scope.launch {
                animate(pull.floatValue, 0f, animationSpec = tween(Motion.QUICK, easing = Motion.easing)) { v, _ -> pull.floatValue = v }
            }
        }
        val pullOnTop = remember {
            object : NestedScrollConnection {
                // Pushing back up takes the pull away before the list scrolls.
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    if (available.y >= 0f || pull.floatValue <= 0f) return Offset.Zero
                    val used = maxOf(available.y, -pull.floatValue)
                    pull.floatValue += used
                    return Offset(0f, used)
                }

                // What the list can't scroll down any further pulls the sheet.
                override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                    if (source != NestedScrollSource.UserInput || available.y <= 0f) return Offset.Zero
                    pull.floatValue += available.y
                    return Offset(0f, available.y)
                }

                override suspend fun onPreFling(available: Velocity): Velocity {
                    if (pull.floatValue <= 0f) return Velocity.Zero
                    release(available.y)
                    return available
                }
            }
        }
        // At most a page's column wide, centred, on a wide screen (spec 2.4 Unfolded).
        GlassPanel(
            Modifier.widthIn(max = org.openstrap.app.ui.glow.GlowLayout.PageMaxWidth).fillMaxWidth()
                .graphicsLayer { translationY = pull.floatValue }
                .nestedScroll(pullOnTop)
                .draggable(
                    rememberDraggableState { d -> pull.floatValue = (pull.floatValue + d).coerceAtLeast(0f) },
                    Orientation.Vertical, onDragStopped = { v -> release(v) },
                ),
            GlowShapes.Sheet,
        ) {
            Column {
                // The grab handle as the boards draw it: a 36 by 4 pill, 10 dp from the top.
                Box(Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 14.dp), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(width = 36.dp, height = 4.dp).background(Color.White.copy(alpha = 0.3f), GlowShapes.Small))
                }
                content()
            }
        }
    }
}
