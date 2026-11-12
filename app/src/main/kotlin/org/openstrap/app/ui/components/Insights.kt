package org.openstrap.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.ui.input.pointer.pointerInput
import org.openstrap.app.ui.glow.GlowType
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Shape
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.LocalScreenLight
import org.openstrap.app.ui.theme.litEdge
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One destination in [FloatingNavBar]; [tint] colours its capsule while it's the tab showing (white on Today). */
data class NavItem(val label: String, val icon: ImageVector, val selectedIcon: ImageVector = icon, val tint: Color = Color.White)

/**
 * The floating tab bar in black glass (spec 3.2): a pill of tabs, the content scrolling under it (marked with
 * `hazeSource` on [glass]) showing through blurred and darkened. Centred with nothing beside it: the bar only switches
 * tabs. The capsule behind the tab showing slides to it and takes that section's colour. Behind the pill a black fade
 * ([TabBarFade]) rises from the bottom edge, so cards slide under the bar softly instead of being cut by its edge. It
 * draws past the bar's own top without taking room, so the pages' bottom padding stays the bar's height.
 */
@Composable
fun FloatingNavBar(
    items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, glass: HazeState,
) {
    Box(
        modifier.fillMaxWidth().drawBehind {
            val height = maxOf(TabBarFade.Height.toPx(), size.height)
            drawRect(TabBarFade.brush(size.height - height, size.height), Offset(0f, size.height - height), Size(size.width, height))
        }.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, bottom = 22.dp, top = 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        // No shadow: it would show through the glass as a dark smudge.
        Box(Modifier.widthIn(max = 440.dp).fillMaxWidth().height(BarHeight).glassPill(glass, BarShape)) {
            // Measured width, so the pill knows how wide a cell is (BoxWithConstraints can't size to its row).
            var barWidth by remember { mutableIntStateOf(0) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(
                Modifier.padding(5.dp).fillMaxHeight()
                    .onSizeChanged { barWidth = it.width },
            ) {
                // One capsule that slides to the selected tab, rather than each cell lighting up on its own.
                val gap = 2.dp
                val cell = (with(density) { barWidth.toDp() } - gap * (items.size - 1)) / items.size
                val x by androidx.compose.animation.core.animateDpAsState(
                    (cell + gap) * selected, tween(Motion.SWIFT, easing = Motion.easing), label = "nav pill",
                )
                Box(
                    Modifier.offset(x = x).width(cell).fillMaxHeight().clip(GlowShapes.Pill)
                        .background(capsuleColor(items.getOrNull(selected)?.tint)).litEdge(GlowShapes.Pill, top = 0.22f, ring = 0f),
                )
                Row(Modifier.fillMaxHeight(), horizontalArrangement = Arrangement.spacedBy(gap)) {
                    items.forEachIndexed { i, item ->
                        NavCell(
                            item, i == selected,
                            modifier = Modifier.clip(GlowShapes.Pill).clickable { onSelect(i) }.weight(1f).fillMaxHeight(),
                            verticalArrangement = Arrangement.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * [FloatingNavBar] standing up along the left edge, for windows wider than tall (a phone on its side, an
 * unfolded foldable turned sideways): the same glass pill and sliding capsule, centred, giving the height back to
 * the pages.
 */
@Composable
fun FloatingNavRail(
    items: List<NavItem>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier, glass: HazeState,
) {
    Box(
        modifier.fillMaxHeight().systemBarsPadding().displayCutoutPadding().padding(horizontal = 10.dp, vertical = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Box(Modifier.width(NavRailWidth - 20.dp).heightIn(max = 380.dp).fillMaxHeight().glassPill(glass, BarShape)) {
            var barHeight by remember { mutableIntStateOf(0) }
            val density = androidx.compose.ui.platform.LocalDensity.current
            Box(Modifier.padding(5.dp).onSizeChanged { barHeight = it.height }) {
                val gap = 2.dp
                val cell = (with(density) { barHeight.toDp() } - gap * (items.size - 1)) / items.size
                val y by androidx.compose.animation.core.animateDpAsState(
                    (cell + gap) * selected, tween(Motion.SWIFT, easing = Motion.easing), label = "nav pill",
                )
                Box(
                    Modifier.offset(y = y).height(cell).fillMaxWidth().clip(GlowShapes.Pill)
                        .background(capsuleColor(items.getOrNull(selected)?.tint)).litEdge(GlowShapes.Pill, top = 0.22f, ring = 0f),
                )
                Column(Modifier.fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(gap)) {
                    items.forEachIndexed { i, item ->
                        NavCell(
                            item, i == selected,
                            modifier = Modifier.clip(GlowShapes.Pill).clickable { onSelect(i) }.weight(1f).fillMaxWidth(),
                            verticalArrangement = Arrangement.Center,
                        )
                    }
                }
            }
        }
    }
}

/**
 * The bar's black glass pill: what scrolls under it blurred, the glass edge round it. It takes every touch inside it,
 * so a tap between two tabs never lands on the card under the bar.
 */
private fun Modifier.glassPill(glass: HazeState, shape: Shape): Modifier =
    clip(shape).hazeBlur(HazeInput.Sources(glass), GlassBlur).litEdge(shape, top = 0.18f, ring = 0.06f).pointerInput(Unit) {}

/** The bar's height and corner (TabTodayMorning): 68 dp, radius 34 dp as a squircle (a pill, half the height). */
private val BarHeight = 68.dp
private val BarShape: Shape = GlowShapes.Pill

/**
 * The pill under the tab showing, eased as it slides: tinted with the screen's light (its first stop at 32%) when the
 * screen gives one, else the tab's own blend ([tint]), else white 12% (on Today with no light).
 */
@Composable
private fun capsuleColor(tint: Color?): Color {
    val light = LocalScreenLight.current?.blend?.first
    val ink = light ?: tint?.takeIf { it != Color.White }
    val target = if (ink != null) ink.copy(alpha = 0.32f) else GlassChip
    val eased by androidx.compose.animation.animateColorAsState(target, tween(Motion.SWIFT), label = "nav capsule")
    return eased
}

/**
 * The black fade behind the tab bar (spec 3.2): [Height] tall from the window's bottom edge (or the bar's height, if
 * that is more), clear at its top and solid black for its lower [SOLID] part.
 */
object TabBarFade {
    val Height = 120.dp
    const val SOLID = 0.45f

    /** The fade's colour stops, top to bottom: clear, then black from [SOLID] up from the bottom. */
    val stops: Array<Pair<Float, Color>> = arrayOf(0f to Color.Transparent, (1f - SOLID) to Color.Black, 1f to Color.Black)

    fun brush(top: Float, bottom: Float): Brush = Brush.verticalGradient(*stops, startY = top, endY = bottom)
}

/**
 * The black fade behind a page's pinned buttons (Profile's Save, Ready's Start, Done, Choose the sport), in the style
 * of the tab bar's ([TabBarFade]): solid black from the bottom of the screen up to the buttons' top, so nothing shows
 * between them and the tab bar, then clearing over [Ramp] above them, the same ramp as the tab bar's fade. A page
 * leaves [Ramp] clear above its pinned buttons at the end of its list, so its last content scrolls fully clear of
 * both ([listClearance]).
 */
object PinnedFade {
    /** How far the fade reaches above the buttons: the tab bar fade's clear-to-black part, 66 dp. */
    val Ramp: Dp = TabBarFade.Height * (1f - TabBarFade.SOLID)

    /** The fade's colour stops between [top] (clear) and [bottom], solid black from [solidFrom] down. */
    fun stops(top: Float, solidFrom: Float, bottom: Float): Array<Pair<Float, Color>> {
        val solid = if (bottom > top) ((solidFrom - top) / (bottom - top)).coerceIn(0f, 1f) else 0f
        return arrayOf(0f to Color.Transparent, solid to Color.Black, 1f to Color.Black)
    }

    /**
     * The bottom padding a page's list needs so its last content ends [Ramp] above pinned buttons whose top is
     * [buttonsTop] over the bottom edge, with [listEnd] the space the list adds on its own (ScreenList's 24 dp).
     */
    fun listClearance(buttonsTop: Dp, listEnd: Dp = 0.dp): Dp = buttonsTop + Ramp - listEnd
}

/**
 * Draws [PinnedFade] behind a page's pinned buttons. Put it on the box that holds them, reaching from the bottom of
 * the screen to the buttons' top: that box is solid black and the fade rises [PinnedFade.Ramp] above it.
 */
fun Modifier.pinnedFade(): Modifier = drawBehind {
    val ramp = PinnedFade.Ramp.toPx()
    drawRect(
        Brush.verticalGradient(*PinnedFade.stops(-ramp, 0f, size.height), startY = -ramp, endY = size.height),
        Offset(0f, -ramp), Size(size.width, size.height + ramp),
    )
}

/** How much of the window's width [FloatingNavRail] takes, its margins included. */
val NavRailWidth = 92.dp

/** One destination's icon and label, tinted for whether it's [on]. */
@Composable
private fun NavCell(item: NavItem, on: Boolean, modifier: Modifier, verticalArrangement: Arrangement.Vertical = Arrangement.Top) {
    Column(
        modifier,
        verticalArrangement = if (verticalArrangement == Arrangement.Center) {
            Arrangement.spacedBy(3.dp, Alignment.CenterVertically)
        } else {
            verticalArrangement
        },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val tint by androidx.compose.animation.animateColorAsState(
            if (on) Color.White else Color.White.copy(alpha = 0.6f),
            tween(Motion.QUICK), label = "nav tint",
        )
        val iconScale by animateFloatAsState(if (on) 1.08f else 1f, tween(Motion.SWIFT, easing = Motion.easing), label = "nav icon")
        Icon(
            if (on) item.selectedIcon else item.icon, item.label, tint = tint,
            modifier = Modifier.size(22.dp).graphicsLayer { scaleX = iconScale; scaleY = iconScale },
        )
        Text(item.label, style = GlowType.label, color = tint, maxLines = 1)
    }
}

/* Heavy glass (TabTodayMorning): rgba(28,28,34,0.62) over a 24 dp blur at 160% saturation; the lit edge is drawn on top. */
private val GlassBlur = HazeBlurStyle {
    blurRadius(24.dp)
    backgroundColor(Color.Black)
    colorEffects(
        listOf(
            HazeColorEffect.colorFilter(ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(1.6f) })),
            HazeColorEffect.tint(Color(0xFF1C1C22).copy(alpha = 0.62f)),
        ),
    )
    // Without blur (before Android 12), a near-opaque tint instead.
    fallbackColorEffect(HazeColorEffect.tint(Color(0xFF1C1C22).copy(alpha = 0.94f)))
}
private val GlassChip = Color.White.copy(alpha = 0.12f)
