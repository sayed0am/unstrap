package org.openstrap.app.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.openstrap.app.R
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.StrapDrawing
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.litEdge
import org.openstrap.app.ui.theme.MetricColors

/*
 * The strap pill in the header (spec 3.1). The pill is the strap's state, not a dot with words:
 *  - connected: a green 7 dp dot and the battery percent in white (amber dot when low)
 *  - charging: the green dot, a bolt and the number
 *  - not synced: an amber dot and its words
 *  - not connected, connecting or not paired: the strap as the Strap page draws it, dimmed, with its words ("2 h ago",
 *    "Not connected"), which tell the three apart. Not connected is mostly out of range or Bluetooth off and passes, so
 *    it isn't drawn as damage.
 * Green and amber are for the strap and the sync only, never a health value (spec 2.2).
 */

/** The status dot's colour: green when fine or charging, amber when low or unsynced, none when the strap is away. */
private fun dotColor(dot: StrapChip.Dot): Color? = when (dot) {
    StrapChip.Dot.Fine, StrapChip.Dot.Charging -> MetricColors.good
    StrapChip.Dot.Low, StrapChip.Dot.Unsynced -> MetricColors.warn
    StrapChip.Dot.Warn, StrapChip.Dot.Quiet -> null
}

@Composable
internal fun StrapPill(chip: StrapChip, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        // 48 dp tall to tap, the pill drawn 36 dp tall in the middle of it; the header row is 60 dp, so nothing moves.
        // [modifier] comes after, so the Strap page grows out of the pill as drawn, not the room round it.
        Modifier.minimumInteractiveComponentSize().then(modifier)
            .clip(GlowShapes.Pill).background(ControlGlass).litEdge(GlowShapes.Pill, top = 0.2f, ring = 0f)
            .clickable(onClickLabel = stringResource(R.string.day_header_open_strap), onClick = onClick)
            .semantics { contentDescription = chip.description }
            .defaultMinSize(minWidth = MIN_WIDTH, minHeight = HEIGHT)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) { PillContent(chip) }
}

/** The header chip's glass: white 12% (TabTodayMorning). */
private val ControlGlass = Color.White.copy(alpha = 0.12f)

@Composable
private fun PillContent(chip: StrapChip) {
    val dot = dotColor(chip.dot)
    // A 7 dp status dot, then white words: the colour is the dot's, never the text's (decision 9).
    if (dot != null) Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(dot, CircleShape))
        // Charging adds a bolt after the dot, and the number alone.
        if (chip.dot == StrapChip.Dot.Charging) {
            Spacer(Modifier.width(5.dp))
            Icon(AppIcons.Bolt, null, Modifier.size(13.dp), tint = Color.White)
        }
        val words = if (chip.dot == StrapChip.Dot.Charging) chip.battery?.toString() ?: chip.text else chip.text
        if (words.isNotEmpty()) {
            Spacer(Modifier.width(if (chip.dot == StrapChip.Dot.Charging) 2.dp else 7.dp))
            PillText(words)
        }
    } else StrapAndWords(chip.text)
}

/** The Strap page's strap drawing, small (its light out and the drawing dimmed, as it is when the strap isn't there). */
@Composable
private fun StrapAndWords(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StrapDrawing(
            led = Color.White, lit = false, syncing = false, modifier = Modifier.size(width = MARK_WIDTH, height = MARK_HEIGHT),
            lineScale = MARK_LINES,
        )
        if (text.isNotEmpty()) {
            Spacer(Modifier.width(7.dp))
            PillText(text)
        }
    }
}

@Composable
private fun PillText(text: String) {
    Text(
        text, maxLines = 1, softWrap = false, color = Color.White,
        style = GlowType.secondary.copy(fontSize = 14.sp, fontWeight = FontWeight.Normal, fontFeatureSettings = "tnum"),
    )
}

private const val MARK_LINES = 4f
private val MIN_WIDTH = 56.dp
private val HEIGHT = 36.dp
private val MARK_WIDTH = 19.dp
private val MARK_HEIGHT = 24.dp
