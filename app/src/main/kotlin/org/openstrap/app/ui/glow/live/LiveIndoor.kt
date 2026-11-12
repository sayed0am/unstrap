package org.openstrap.app.ui.glow.live

import org.openstrap.app.presentation.Units
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import org.openstrap.app.data.WorkoutTarget
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.GlowShapes
import androidx.compose.foundation.layout.Arrangement
import org.openstrap.app.ui.glow.draw.DotSize
import org.openstrap.app.ui.glow.HeroValue
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.GlowCard
import org.openstrap.app.ui.glow.Segment
import org.openstrap.app.ui.glow.SegmentBar
import org.openstrap.app.presentation.WorkoutCopy
import androidx.compose.ui.Alignment
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.openstrap.app.data.WorkoutRecorder
import org.openstrap.app.device.ConnectionState
import org.openstrap.app.device.WorkoutState
import org.openstrap.app.ui.glow.activeSecondsState
import org.openstrap.app.ui.glow.workoutColor
import org.openstrap.app.ui.workout.ZONE_COLORS
import org.openstrap.app.ui.workout.ZoneSeconds
import org.openstrap.app.ui.dashboard.LocalHeartZones
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import org.openstrap.app.R

/**
 * Indoor cardio, and a distance sport without GPS (workouts spec 2.3, board Main): one view under the moving time.
 * Heart rate is the hero, on its zone arc with the time in each zone as a bar and the target if one is set; then
 * the distance from steps when there is one, the average heart rate and active kcal.
 */
@Composable
internal fun LiveIndoorScreen(
    r: WorkoutState.Recording,
    liveHeartRate: Int?,
    recent: List<Pair<Long, Int>>,
    stats: WorkoutRecorder.Stats?,
    zoneSeconds: ZoneSeconds,
    connection: ConnectionState,
    contentPadding: PaddingValues,
    controls: LiveControls,
    onLap: () -> Unit,
    kcal: WorkoutRecorder.Kcal?,
    /** Split across a fold: the heart card is on the other half, so this one keeps the time in zones. */
    split: Boolean = false,
    /** A short screen: the total small on top, so the controls keep their labels, and the heart card on one row. */
    compact: Boolean = false,
) {
    val heartZones = LocalHeartZones.current
    val connected = connection is ConnectionState.Connected
    val blend = Blends.IndoorCardio
    ZoneTick(liveHeartRate.takeIf { connected && !r.paused }, r)
    val active = activeSecondsState(r)
    val figures = listOfNotNull(
        // "2.14 km from steps", or metres under a kilometre.
        r.workout.distanceMeters?.takeIf { it >= Units.SHOWN_FROM_METERS }?.let { m ->
            val km = m >= Units.METERS_PER_KM
            val label = when {
                !r.workout.distanceFromSteps -> if (km) R.string.workout_live_figure_km else R.string.workout_live_figure_m
                km -> R.string.workout_live_figure_km_from_steps
                else -> R.string.workout_live_figure_m_from_steps
            }
            (if (km) Units.km(m, Units.Precision.Workout) else Units.count(m.toLong())) to stringResource(label)
        },
        stats?.average?.toString() to stringResource(R.string.workout_live_figure_avg_bpm),
        kcal?.active?.toInt()?.toString() to stringResource(R.string.workout_live_figure_active_kcal),
    )
    LiveFrame(
        r, blend, connected, contentPadding, controls,
        lap = if (!r.paused) ({ LapButton(onLap, label = true) }) else null,
        time = { active.value }, smallTime = compact, battery = (connection as? ConnectionState.Connected)?.batteryPercent,
        paused = { LivePaused(r, liveHeartRate.takeIf { connected }, recent, figures) },
    ) {
        // Time reps on the treadmill side: the rep above the heart.
        if (r.structure is org.openstrap.app.data.Structure.Reps) RepCard(r, emptyList(), recent, liveHeartRate.takeIf { connected && !r.paused })
        val reading = liveHeartRate.takeIf { connected && !r.paused }
        val target = remember(r.workout.target) { WorkoutTarget.parse(r.workout.target) as? WorkoutTarget.Zones }
        if (split) TimeInZonesCard(zoneSeconds, WorkoutRecorder.liveZone(reading, r.maxHeartRate, heartZones), target)
        // A whole screen this short is a phone on its side: the arc can't fit beside the controls, the number can.
        else if (compact) CompactHeartCard(r, liveHeartRate, zoneSeconds, connected, { active.value }, kcal?.active, blend, target)
        else {
            WorkoutHeartCard(
                r, liveHeartRate, recent, zoneSeconds, connected, { active.value }, Modifier.fillMaxWidth(),
                kcal = kcal?.active, zoneBar = true, blend = blend,
            )
            TimeInZonesCard(zoneSeconds, WorkoutRecorder.liveZone(reading, r.maxHeartRate, heartZones), target)
        }
        LiveFigures(figures, Modifier.padding(top = 4.dp))
    }
}

/**
 * The time in each zone as one bar, each step as wide as its share and the zone you're in lit and glowing; an empty
 * zone keeps a sliver so the bar always shows all five. The target's zones are outlined, and each zone's whole minutes
 * sit under it (board ZoneStrip, Indoor cardio). Flat zone colours, drawn small.
 */
@Composable
internal fun ZoneTimeBar(zoneSeconds: ZoneSeconds, current: Int?, target: WorkoutTarget.Zones? = null, labels: Boolean = false) {
    val total = zoneSeconds.sum().coerceAtLeast(1)
    val res = LocalResources.current
    val each = zoneSeconds.withIndex().joinToString(", ") { (z, s) ->
        if (s < 60) res.getString(R.string.workout_live_zone_time_none, z + 1)
        else res.getString(R.string.workout_live_zone_time_minutes, z + 1, s / 60)
    }
    val described = stringResource(R.string.workout_live_zone_times_description, each)
    val weights = zoneSeconds.map { maxOf(it.toFloat() / total, 0.04f) }
    Column(Modifier.semantics { contentDescription = described }) {
        BoxWithConstraints(Modifier.fillMaxWidth().height(16.dp)) {
            val gap = 3.dp
            val sum = weights.sum()
            val room = maxWidth - gap * 4
            Row(Modifier.align(Alignment.Center), horizontalArrangement = Arrangement.spacedBy(gap)) {
                zoneSeconds.forEachIndexed { z, s ->
                    val on = z == current
                    Box(
                        Modifier.width(room * (weights[z] / sum)).height(10.dp)
                            .then(if (on) Modifier.shadow(7.dp, GlowShapes.Pill, ambientColor = ZONE_COLORS[z], spotColor = ZONE_COLORS[z]) else Modifier)
                            .clip(GlowShapes.Pill)
                            .background(if (s == 0) Color.White.copy(alpha = 0.18f) else ZONE_COLORS[z].copy(alpha = if (on) 1f else 0.7f)),
                    )
                }
            }
            if (target != null) {
                val lo = target.low.coerceIn(0, 4)
                val hi = target.high.coerceIn(lo, 4)
                val start = (0 until lo).sumOf { (room * (weights[it] / sum)).value.toDouble() + gap.value }.dp
                val width = (lo..hi).sumOf { (room * (weights[it] / sum)).value.toDouble() }.dp + gap * (hi - lo)
                Box(Modifier.offset(x = start - 3.dp).width(width + 6.dp).height(16.dp).border(1.5.dp, Color.White.copy(alpha = 0.7f), GlowShapes.Tile))
            }
        }
        if (labels) Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
            zoneSeconds.forEachIndexed { z, s ->
                Text(
                    if (s >= 60) "${s / 60}" else "", color = Color.White.copy(alpha = 0.72f), style = GlowType.caption,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1, modifier = Modifier.weight(weights[z]),
                )
            }
        }
    }
}

/** "Time in zones" and the target on clear glass over [ZoneTimeBar] (board LiveIndoor): the strip that follows the arc. */
@Composable
internal fun TimeInZonesCard(zoneSeconds: ZoneSeconds, current: Int?, target: WorkoutTarget.Zones?) {
    LiveStrip(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.workout_live_time_in_zones), color = Color.White, style = GlowType.cardTitle, maxLines = 1, modifier = Modifier.weight(1f))
            if (target != null) Text(
                if (target.low == target.high) stringResource(R.string.workout_live_target_short_zone, target.low + 1)
                else stringResource(R.string.workout_live_target_short_zones, target.low + 1, target.high + 1),
                color = Color.White.copy(alpha = 0.72f), style = GlowType.secondary, maxLines = 1,
            )
        }
        ZoneTimeBar(zoneSeconds, current, target, labels = true)
    }
}

/**
 * The live heart card on a short screen (a phone on its side): the bpm in dots with the zone beside it, the time in
 * each zone as a bar, and the card's notes, tinted as the full card is ([WorkoutHeartCard]), without the arc and the
 * last ten minutes that need its height.
 */
@Composable
private fun CompactHeartCard(
    r: WorkoutState.Recording, liveHeartRate: Int?, zoneSeconds: ZoneSeconds, connected: Boolean, active: () -> Long, kcal: Float?,
    blend: Blend, target: WorkoutTarget.Zones?,
) {
    val heartZones = LocalHeartZones.current
    val bpm = liveHeartRate.takeIf { connected && !r.paused }
    val zone = WorkoutRecorder.liveZone(bpm, r.maxHeartRate, heartZones)
    LiveHero(blend) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.Bottom) {
                if (bpm != null) HeroValue("$bpm", stringResource(R.string.workout_live_unit_bpm), dot = DotSize.Detail)
                else Text(WorkoutCopy.noHeartRate(r.paused, connected), color = Color.White, style = GlowType.sentence)
                Spacer(Modifier.weight(1f))
                if (zone != null) ZoneDot(zone, modifier = Modifier.padding(bottom = 4.dp, end = 7.dp))
                Text(
                    zone?.let { WorkoutCopy.zoneLine(it) } ?: WorkoutCopy.noZone(r.paused),
                    color = Color.White, style = GlowType.secondary, maxLines = 1,
                )
            }
            ZoneTimeBar(zoneSeconds, zone, target)
            HeartNotes(r, zone, zoneSeconds, active, kcal)
        }
    }
}
