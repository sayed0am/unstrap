package org.openstrap.app.ui.glow.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.openstrap.app.R
import org.openstrap.app.data.WorkoutRecorder
import org.openstrap.app.data.WorkoutTarget
import org.openstrap.app.presentation.WorkoutCopy
import org.openstrap.app.ui.dashboard.LocalHeartZones
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.DotMatrixText
import org.openstrap.app.ui.glow.draw.DotSize
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.ui.theme.clearGlass
import org.openstrap.app.ui.theme.litEdge
import org.openstrap.app.ui.theme.tintedGlass
import org.openstrap.app.ui.workout.ZONE_COLORS

/** The effort strip's bars, one per heart-rate zone. */
private const val ZONE_BARS = 5

/*
 * The glass the live screens share (Glow × Glass, boards LiveDistance to LiveGame): the hero is tinted glass in the
 * family's blend, every other card clear glass, and the heart rate's zone is only ever drawn small, as a dot and a
 * track of five steps. The screen's own light is the family's blend ([LiveFrame]); a zone never lights a screen.
 */

/** The live screen's hero: tinted glass in the family's [blend], 20 dp in from its edges. */
@Composable
internal fun LiveHero(
    blend: Blend,
    modifier: Modifier = Modifier,
    padding: PaddingValues = PaddingValues(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top, horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier.fillMaxWidth().clip(GlowShapes.Card).tintedGlass(blend, GlowShapes.Card).padding(padding),
    verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment, content = content,
)

/** A live card that isn't the hero: clear glass. */
@Composable
internal fun LiveStrip(
    modifier: Modifier = Modifier, padding: PaddingValues = PaddingValues(start = 18.dp, top = 14.dp, end = 18.dp, bottom = 16.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp), content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier.fillMaxWidth().clip(GlowShapes.Card).clearGlass(GlowShapes.Card).padding(padding),
    verticalArrangement = verticalArrangement, content = content,
)

/** A zone's flat colour as the small dot beside its name. */
@Composable
internal fun ZoneDot(zone: Int?, modifier: Modifier = Modifier, size: Dp = 8.dp) =
    Box(modifier.size(size).clip(CircleShape).background(zone?.let { ZONE_COLORS[it] } ?: Color.White))

/**
 * The zone strip (ZoneStrip board), one part under every heart card: the reading in 3 dp dots with "bpm", the zone named
 * on the right with its dot, then the track of five equal steps in zone order, dim, the current one lit and glowing, the
 * marker exactly where [bpm] sits, and the target (if one is set) outlined. Zones stay small: never the screen's light.
 */
@Composable
internal fun LiveZoneStrip(
    bpm: Int?, maxHr: Int, paused: Boolean, connected: Boolean, target: WorkoutTarget?, modifier: Modifier = Modifier,
    /** Left out where the reading is the hero already and the strip is only the track ([Games]'s heart card). */
    reading: Boolean = true,
) {
    val zones = LocalHeartZones.current
    val zone = WorkoutRecorder.liveZone(bpm, maxHr, zones)
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (reading) Row(verticalAlignment = Alignment.Bottom) {
            if (bpm != null) {
                DotMatrixText(bpm.toString(), dot = DotSize.Edge)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.workout_live_unit_bpm), color = Color.White.copy(alpha = 0.75f), style = GlowType.secondary)
            } else Text(WorkoutCopy.noHeartRate(paused, connected), color = Color.White, style = GlowType.sentence)
            Spacer(Modifier.weight(1f))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (zone != null) {
                    ZoneDot(zone)
                    Spacer(Modifier.width(7.dp))
                    Text(
                        WorkoutCopy.zoneLine(zone), color = Color.White,
                        style = GlowType.secondary.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold), maxLines = 1,
                    )
                } else Text(WorkoutCopy.noZone(paused), color = Color.White.copy(alpha = 0.75f), style = GlowType.secondary, maxLines = 1)
            }
        }
        ZoneTrack(bpm, maxHr, (target as? WorkoutTarget.Zones))
    }
}

/** The five-step track alone: dim steps, the current lit with a glow, the white marker, the target outlined. */
@Composable
internal fun ZoneTrack(bpm: Int?, maxHr: Int, target: WorkoutTarget.Zones?, modifier: Modifier = Modifier) {
    val zones = LocalHeartZones.current
    val zone = WorkoutRecorder.liveZone(bpm, maxHr, zones)
    val described = zone?.let { WorkoutCopy.zoneLine(it) } ?: stringResource(R.string.workout_live_effort_none)
    BoxWithConstraints(modifier.fillMaxWidth().height(16.dp).semantics { contentDescription = described }) {
        val gap = 3.dp
        val step = (maxWidth - gap * 4) / 5
        Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(gap)) {
            for (z in 0 until ZONE_BARS) {
                val on = z == zone
                Box(
                    Modifier.width(step).height(10.dp)
                        .then(
                            if (on) Modifier.shadow(7.dp, GlowShapes.Pill, ambientColor = ZONE_COLORS[z], spotColor = ZONE_COLORS[z])
                            else Modifier,
                        )
                        .clip(GlowShapes.Pill).background(ZONE_COLORS[z].copy(alpha = if (on) 1f else 0.26f)),
                )
            }
        }
        if (target != null) {
            val lo = target.low.coerceIn(0, 4)
            val hi = target.high.coerceIn(lo, 4)
            Box(
                Modifier.offset(x = (step + gap) * lo - 3.dp).width(step * (hi - lo + 1) + gap * (hi - lo) + 6.dp).height(16.dp)
                    .border(1.5.dp, Color.White.copy(alpha = 0.7f), GlowShapes.Tile),
            )
        }
        if (bpm != null) {
            val p = WorkoutRecorder.zonePosition(bpm, maxHr, zones).coerceIn(0f, 1f)
            Box(
                Modifier.offset(x = maxWidth * p - 11.dp).align(Alignment.CenterStart).size(22.dp).clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.55f)),
                contentAlignment = Alignment.Center,
            ) { Box(Modifier.size(16.dp).clip(CircleShape).background(Color.White)) }
        }
    }
}

/** The dark card the phase light shows round (HIIT, boards LiveIntervals): near black at 40% with a lit edge. */
@Composable
internal fun LiveDarkCard(
    modifier: Modifier = Modifier, horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top, content: @Composable ColumnScope.() -> Unit,
) = Column(
    modifier.fillMaxWidth().clip(GlowShapes.Card).background(Color.Black.copy(alpha = 0.42f), GlowShapes.Card)
        .litEdge(GlowShapes.Card, top = 0.12f, ring = 0.05f).padding(20.dp),
    verticalArrangement = verticalArrangement, horizontalAlignment = horizontalAlignment, content = content,
)
