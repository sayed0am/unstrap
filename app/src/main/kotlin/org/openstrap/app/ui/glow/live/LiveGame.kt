package org.openstrap.app.ui.glow.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.openstrap.app.data.Games
import org.openstrap.app.data.Phase
import org.openstrap.app.data.Structure
import org.openstrap.app.data.WorkoutRecorder
import org.openstrap.app.device.ConnectionState
import org.openstrap.app.device.WorkoutState
import org.openstrap.app.ui.glow.HeroValue
import org.openstrap.app.ui.glow.draw.DotSize
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.data.WorkoutTarget
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.Segment
import org.openstrap.app.ui.glow.SegmentBar
import org.openstrap.app.ui.glow.WorkoutButton
import org.openstrap.app.ui.glow.activeSecondsState
import org.openstrap.app.ui.glow.workoutClock
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.workout.StartCopy
import org.openstrap.app.presentation.WorkoutCopy
import org.openstrap.app.ui.workout.ZONE_COLORS
import org.openstrap.app.ui.workout.ZoneSeconds
import org.openstrap.app.ui.dashboard.LocalHeartZones
import androidx.compose.ui.res.stringResource
import org.openstrap.app.R

/**
 * Games and sports (workouts spec 2.7, board LiveGame), made for the phone in a bag: the period and the time in it,
 * the heart rate in dots on a glow in its zone and the zone named, an effort bar (1 to 5 from the time in zones so
 * far), then active kcal, peak bpm and hard efforts (spells of 10 s or more in zone 5). Half time starts the break
 * early. The strap buzzes at the end of each period and break, so nothing needs the phone. Free play has no periods.
 */
@Composable
internal fun LiveGameScreen(
    r: WorkoutState.Recording,
    liveHeartRate: Int?,
    readings: List<Pair<Long, Int>>,
    stats: WorkoutRecorder.Stats?,
    zoneSeconds: ZoneSeconds,
    connection: ConnectionState,
    contentPadding: PaddingValues,
    controls: LiveControls,
    kcal: WorkoutRecorder.Kcal?,
    onHalfTime: () -> Unit,
    /** Split across a fold: the heart rate is on the other half, so only the effort is left of the card. */
    split: Boolean = false,
    /** A short half: the total small on top, so the controls keep their labels. */
    compact: Boolean = false,
) {
    val heartZones = LocalHeartZones.current
    val now = nowTickingState()
    val connected = connection is ConnectionState.Connected
    val bpm = liveHeartRate.takeIf { connected && !r.paused }
    ZoneTick(bpm, r)
    val blend = Blends.Games
    val target = remember(r.workout.target) { WorkoutTarget.parse(r.workout.target) }
    val periods = r.structure as? Structure.Periods
    // The phase alone picks the buttons; its clock is read in [PeriodRow], so the screen holds still between seconds.
    val phase = remember(r) { androidx.compose.runtime.derivedStateOf { r.phase(now.value) } }.value
    val active = activeSecondsState(r)
    val effort = Games.effort(zoneSeconds.toIntArray())
    val hard = remember(readings.size / 10, heartZones) { Games.hardEfforts(readings, r.maxHeartRate, heartZones) }
    val figures = listOf(
        kcal?.active?.toInt()?.toString() to stringResource(R.string.workout_live_figure_active_kcal),
        stats?.max?.toString() to stringResource(R.string.workout_live_figure_peak_bpm),
        hard.toString() to stringResource(R.string.workout_live_figure_hard_efforts),
    )
    LiveFrame(
        r, blend, connected, contentPadding, controls, time = { active.value }, smallTime = compact || periods != null,
        paused = { LivePaused(r, liveHeartRate.takeIf { connected }, readings, figures) },
    ) {
        if (periods != null && phase != null) PeriodRow(r, periods, phase, { now.value }, { active.value })
        if (split) EffortCard(effort) else {
            // The heart on tinted glass: the zone named with its dot, the reading in the live dots, the zone's track.
            LiveHero(blend) {
                val zone = WorkoutRecorder.liveZone(bpm, r.maxHeartRate, heartZones)
                Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                    if (zone != null) {
                        org.openstrap.app.ui.glow.PulseDot(ZONE_COLORS[zone], bpm, 8.dp)
                        Spacer(Modifier.width(8.dp))
                        Text(WorkoutCopy.zoneLine(zone), color = Color.White, style = GlowType.cardTitle, maxLines = 1)
                    } else Text(WorkoutCopy.noZone(r.paused), color = Color.White, style = GlowType.cardTitle, maxLines = 1)
                }
                Spacer(Modifier.height(10.dp))
                if (bpm != null) HeroValue("$bpm", stringResource(R.string.workout_live_unit_bpm), dot = DotSize.Live, maxWidth = 280.dp)
                else Text(WorkoutCopy.noHeartRate(r.paused, connected), color = Color.White, style = GlowType.sentence)
                Spacer(Modifier.height(14.dp))
                ZoneTrack(bpm, r.maxHeartRate, target as? WorkoutTarget.Zones)
            }
            EffortCard(effort)
        }
        LiveFigures(figures)
        Spacer(Modifier.weight(1f))
        if (periods != null && phase is Phase.Work) WorkoutButton(StartCopy.breakName(periods.rounds), Color.White, Color.Black, onHalfTime, icon = AppIcons.PauseFill, height = 64.dp)
        if (periods != null && phase is Phase.Rest) WorkoutButton(
            stringResource(R.string.workout_live_game_start_period, StartCopy.periodName(phase.round + 1, periods.rounds).lowercase()),
            Color.White, Color.Black, onHalfTime, icon = AppIcons.PlayFill, height = 64.dp,
        )
    }
}

/** The period and the time in it, or the break and its time left: its own scope, so only it reads each second. */
@Composable
private fun PeriodRow(r: WorkoutState.Recording, periods: Structure.Periods, phase: Phase, now: () -> Long, active: () -> Long) {
    val (title, clock) = when (phase) {
        is Phase.Work -> StartCopy.periodName(phase.round, periods.rounds) to
            workoutClock((WorkoutRecorder.activeSeconds(r.workout, now()) - WorkoutRecorder.activeSeconds(r.workout, phase.start)).coerceAtLeast(0))
        is Phase.Rest -> StartCopy.breakName(periods.rounds) to
            stringResource(
                R.string.workout_live_game_break_left,
                workoutClock((phase.endsAt - WorkoutRecorder.activeSeconds(r.workout, now())).coerceAtLeast(0)),
            )
        else -> stringResource(R.string.workout_live_game_full_time) to workoutClock(active())
    }
    Row(Modifier.fillMaxWidth()) {
        Text(title, color = Color.White, style = GlowType.pageTitle, modifier = Modifier.weight(1f))
        Text(clock, color = Color.White, style = GlowType.pageTitle.copy(fontFeatureSettings = "tnum"))
    }
}

/** The effort card: clear glass under the heart card, [EffortBar] on it. */
@Composable
private fun EffortCard(effort: Int?) = LiveStrip(Modifier.fillMaxWidth()) { EffortBar(effort) }

/**
 * Effort 1 to 5 as five blocks, lit up to it in the zones' flat colours (the others white at 12%), "4 of 5 effort"
 * above them in the dots, "From your time in zones" beside it (board LiveGame).
 */
@Composable
internal fun EffortBar(effort: Int?) {
    val of = effort?.let { stringResource(R.string.workout_live_effort_of, it) }
    val described = of ?: stringResource(R.string.workout_live_effort_none)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.semantics { contentDescription = described }) {
        Text(of ?: stringResource(R.string.workout_live_effort), color = Color.White, style = GlowType.secondary)
        SegmentBar(
            List(5) { i -> Segment(1f, if (effort != null && i < effort) ZONE_COLORS[i] else Color.White.copy(alpha = 0.12f)) },
            height = 28.dp, gap = 6.dp, corner = 10.dp, ends = 10.dp,
        )
    }
}
