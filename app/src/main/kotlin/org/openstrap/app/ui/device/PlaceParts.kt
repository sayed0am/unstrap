package org.openstrap.app.ui.device

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import org.openstrap.app.ui.components.pressClickable
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.NoticeDot
import org.openstrap.app.ui.glow.TapCaret
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.ScreenLight
import org.openstrap.app.ui.theme.clearGlass
import org.openstrap.app.ui.theme.litEdge
import org.openstrap.app.ui.theme.tintedGlass

/*
 * The parts the You and Strap pages share on the Glow × Glass boards (PageYou, PageStrap and what they open): the place
 * hero in tinted glass of its own blend, the 150 dp tiles, the outlined row, and the screen lights each board draws.
 * You is copper, Strap is graphite. Both are places, so they light their pages and never a chart.
 */

/** The You pages' light (PageYou and what it opens): copper from the upper left, a little lower for a longer page. */
internal val YouLight = ScreenLight(Blends.You, angle = 165f, strength = ScreenLight.EVENING, heroCenterY = 156f)

/** The Strap pages' light (PageStrap and what it opens): graphite from above. */
internal val StrapPageLight = ScreenLight(Blends.Strap, angle = 90f, strength = 0.75f, heroCenterY = 174f)

/** The caret's alpha at the end of a tile or an outlined row (clear glass: 50%). */
private const val CARET = 0.5f

/**
 * The CSS `linear-gradient(<angle>deg, ...)` for a box of [w] by [h]: the gradient line runs through the centre at
 * [angle] clockwise from up, as long as the box's projection on it.
 */
private fun cssGradient(angle: Float, w: Float, h: Float, stops: Array<Pair<Float, Color>>): Brush {
    val a = angle / 180f * PI.toFloat()
    val dx = sin(a); val dy = -cos(a)
    val half = (abs(w * dx) + abs(h * dy)) / 2f
    val c = Offset(w / 2f, h / 2f)
    return Brush.linearGradient(
        colorStops = stops, start = Offset(c.x - dx * half, c.y - dy * half), end = Offset(c.x + dx * half, c.y + dy * half),
    )
}

/** A hero in tinted glass: [stops] along [angle] (CSS degrees), lit top edge 35%, ring 10%. */
internal fun Modifier.placeHero(angle: Float, vararg stops: Pair<Float, Color>): Modifier =
    drawWithCache {
        val outline = GlowShapes.Card.createOutline(size, layoutDirection, this)
        val brush = cssGradient(angle, size.width, size.height, arrayOf(*stops))
        onDrawBehind { drawOutline(outline, brush) }
    }.litEdge(GlowShapes.Card, top = 0.35f, ring = 0.10f)

/** The You pages' hero: white into the copper first stop and back to white, lit from the upper left (140 degrees). */
internal fun Modifier.youHero(blend: Blend = Blends.You): Modifier = placeHero(
    140f, 0f to Color.White.copy(alpha = 0.24f), 0.55f to blend.first.copy(alpha = 0.14f), 1f to Color.White.copy(alpha = 0.04f),
)

/** The Strap pages' hero: the strap blend from the top (46%) fading to white 4%. */
internal fun Modifier.strapHero(blend: Blend = Blends.Strap): Modifier = placeHero(
    180f, 0f to blend.first.copy(alpha = 0.46f), 0.55f to blend.first.copy(alpha = 0.16f), 1f to Color.White.copy(alpha = 0.04f),
)

/** A hero card of a place's page: tinted glass of [look], padded 20 dp, [onClick] makes the whole card a button. */
@Composable
internal fun PlaceHero(
    look: Modifier,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit,
) {
    val shape = GlowShapes.Card
    Box(
        modifier.fillMaxWidth().clip(shape).then(look)
            .let { if (onClick != null) it.pressClickable(onClick, shape).semantics { role = Role.Button } else it }
            .padding(horizontal = 20.dp, vertical = 18.dp),
        content = content,
    )
}

/** A round icon on a page's tile, hero or row: [size] across, white at [fill] behind the [icon]. */
@Composable
internal fun PlaceIcon(icon: ImageVector, size: androidx.compose.ui.unit.Dp, fill: Float, tint: Color = Color.White) {
    Box(Modifier.size(size).background(Color.White.copy(alpha = fill), CircleShape), contentAlignment = Alignment.Center) {
        Icon(icon, null, Modifier.size(size / 2), tint = tint)
    }
}

/**
 * A 150 dp tile of the two-up grid on the You pages (Health Connect, Backup, Copy now, Restore): clear glass, a 40 dp
 * round icon at the top left with the caret opposite when it opens something, its [title] and [note] at the bottom.
 */
@Composable
internal fun PlaceTile(
    icon: ImageVector,
    title: String,
    note: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 150.dp,
    caret: Boolean = true,
    enabled: Boolean = true,
) {
    val shape = GlowShapes.Card
    Column(
        modifier.height(height).clip(shape).clearGlass(shape)
            .let { if (enabled) it.pressClickable(onClick, shape) else it.alpha(0.4f) }
            .semantics { role = Role.Button }
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
            PlaceIcon(icon, 40.dp, 0.10f)
            if (caret) TapCaret(CARET, Modifier.padding(top = 4.dp))
        }
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = Color.White, style = GlowType.body, maxLines = 2)
            Text(note, color = Color.White.copy(alpha = 0.65f), style = GlowType.secondary, maxLines = 3, overflow = TextOverflow.Ellipsis)
        }
    }
}

/**
 * An outlined row on the You and Strap pages (Open Health Connect, Deleted sleep): no fill, a 1 dp white 16% ring, a
 * 36 dp round icon, the [title] with an optional [note] under it, then [trailing] (a chip) and the caret.
 */
@Composable
internal fun OutlineRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    note: String? = null,
    iconFill: Color = Color.White.copy(alpha = 0.10f),
    dot: Boolean = false,
    iconTint: Color = Color.White,
    trailing: (@Composable () -> Unit)? = null,
) {
    val shape = GlowShapes.Tile
    Row(
        modifier.fillMaxWidth().height(68.dp).clip(shape).border(1.dp, Color.White.copy(alpha = 0.16f), shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 14.dp, end = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(36.dp).background(iconFill, CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, Modifier.size(18.dp), tint = iconTint)
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, color = Color.White, style = GlowType.body, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (dot) { Spacer(Modifier.width(8.dp)); NoticeDot() }
            }
            if (note != null) Text(note, color = Color.White.copy(alpha = 0.65f), style = GlowType.secondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        if (trailing != null) { Spacer(Modifier.width(10.dp)); trailing() }
        Spacer(Modifier.width(12.dp))
        TapCaret(CARET)
    }
}

/**
 * A 128 dp tile of the Strap page's two-up row (PageStrap): a 36 dp round icon at the top left, the [title] at the bottom
 * left, and at the top right either a switch ([onChange] set) or a chip with [chip] ("On"). Clear glass for a switch,
 * tinted strap glass for the chip; a tap flips the switch, or runs [onClick] when the chip is something to turn on.
 */
@Composable
internal fun StatusTile(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    checked: Boolean = false,
    onChange: ((Boolean) -> Unit)? = null,
    chip: String? = null,
    onClick: (() -> Unit)? = null,
) {
    val shape = GlowShapes.Card
    Column(
        modifier.height(128.dp).clip(shape)
            .let { if (onChange != null) it.clearGlass(shape) else it.tintedGlass(Blends.Strap, shape) }
            .let {
                when {
                    onChange != null -> it.toggleable(checked, role = Role.Switch, onValueChange = onChange)
                    onClick != null -> it.pressClickable(onClick, shape).semantics { role = Role.Button }
                    else -> it
                }
            }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            PlaceIcon(icon, 36.dp, 0.08f, tint = Color.White.copy(alpha = 0.78f))
            if (onChange != null) org.openstrap.app.ui.glow.GlowSwitch(checked, onChange, Modifier.clearAndSetSemantics {})
            else if (chip != null) Text(
                chip, color = Color.White, style = GlowType.secondary,
                modifier = Modifier.clip(GlowShapes.Pill).background(Color.White.copy(alpha = 0.12f)).padding(horizontal = 10.dp, vertical = 4.dp),
            )
        }
        Text(title, color = Color.White, style = GlowType.body, maxLines = 2)
    }
}

/** A small white 14% chip with [text] ("2 to restore"), the board's count tag. */
@Composable
internal fun CountChip(text: String) {
    Text(
        text, color = Color.White, style = GlowType.caption,
        modifier = Modifier.clip(GlowShapes.Pill).background(Color.White.copy(alpha = 0.14f)).padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

/** A panel of rows with no fill and a 1 dp white 14% ring (Firmware and Zepp account on the Strap page). */
@Composable
internal fun OutlinePanel(modifier: Modifier = Modifier, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().clip(GlowShapes.Card).border(1.dp, Color.White.copy(alpha = 0.14f), GlowShapes.Card)
            .padding(horizontal = 18.dp, vertical = 6.dp),
        content = content,
    )
}

/** A pill with no fill and a white 16% ring, its words at 72% (Forget this strap). */
@Composable
internal fun OutlinePill(text: String, icon: ImageVector?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().height(52.dp).clip(GlowShapes.Pill).border(1.dp, Color.White.copy(alpha = 0.16f), GlowShapes.Pill)
            .clickable(role = Role.Button, onClick = onClick).padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically,
    ) {
        val ink = Color.White.copy(alpha = 0.72f)
        if (icon != null) { Icon(icon, null, Modifier.size(18.dp), tint = ink); Spacer(Modifier.width(8.dp)) }
        Text(text, color = ink, style = GlowType.body, maxLines = 1)
    }
}

/**
 * The You pages' switch hero (Health Connect, Backup): a 40 dp icon and the switch on one line, the [title] and [line]
 * under them. The whole top is the switch's row, so a tap anywhere on it flips it. [below] holds what follows inside the
 * card (Backup's folder rows).
 */
@Composable
internal fun SwitchHero(
    icon: ImageVector,
    title: String,
    line: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    below: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
) {
    PlaceHero(Modifier.youHero(), modifier) {
        Column {
            Column(
                Modifier.fillMaxWidth().toggleable(checked, role = Role.Switch, onValueChange = onChange),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    PlaceIcon(icon, 40.dp, 0.16f)
                    org.openstrap.app.ui.glow.GlowSwitch(checked, onChange, Modifier.clearAndSetSemantics {})
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(title, color = Color.White, style = GlowType.cardTitle, maxLines = 1)
                    Text(line, color = Color.White.copy(alpha = 0.85f), style = GlowType.body)
                }
            }
            if (below != null) { Spacer(Modifier.height(10.dp)); below() }
        }
    }
}

/** The paper the release notes are printed on (PageUpdates, What's new): near white, with ink for text. */
internal object Paper {
    val Fill = Color(0xFFF7F5F9)
    val Ink = Color(0xFF0B0B0D)
    val Body = Color(0xFF3A3A42)
    val Lead = Color(0xFF2A2A30)
    val Muted = Color(0xFF5B5B63)
    val Bullet = Color(0xFF6E6E76)
}
