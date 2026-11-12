@file:Suppress("TooManyFunctions") // one cohesive file: each function is one section of the workout summary

package org.openstrap.app.ui.glow

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import org.openstrap.app.ui.components.PinnedFade
import org.openstrap.app.ui.components.pinnedFade
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.litEdge
import org.openstrap.app.ui.glow.draw.GlowStyle
import org.openstrap.app.ui.glow.draw.chart
import org.openstrap.app.presentation.Ranges
import org.openstrap.app.ui.glow.draw.LightAngle
import org.openstrap.app.ui.glow.draw.heroStyle
import org.openstrap.app.ui.theme.Ground
import org.openstrap.app.R
import org.openstrap.app.presentation.Units
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import org.openstrap.app.ui.dashboard.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.openstrap.app.ui.glow.draw.DotSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.font.FontWeight
import org.openstrap.app.ui.workout.SplitRow
import org.openstrap.app.ui.workout.ZoneSeconds
import org.openstrap.app.ui.workout.familyColor
import org.openstrap.app.ui.workout.mainZoneColor
import kotlinx.coroutines.delay
import org.openstrap.app.data.Route
import org.openstrap.app.data.WorkoutAnalysis
import org.openstrap.app.data.Workouts
import org.openstrap.app.device.CalmDown
import org.openstrap.app.device.WorkoutSession
import org.openstrap.app.ui.components.HapticEvent
import org.openstrap.app.ui.components.rememberAnimationsOn
import org.openstrap.app.ui.components.rememberHaptics
import org.openstrap.app.ui.dashboard.Score
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.theme.MetricColors
import org.openstrap.app.ui.workout.CalmDownLine
import org.openstrap.app.presentation.EarnedLine
import org.openstrap.app.presentation.SummaryRewards
import org.openstrap.app.ui.workout.FamilySummary
import org.openstrap.app.ui.workout.PhoneSummary
import org.openstrap.app.ui.workout.SummaryCopy
import org.openstrap.app.ui.workout.ZONE_COLORS
import org.openstrap.app.ui.workout.ZONE_NAMES
import org.openstrap.app.ui.dashboard.OffWords
import org.openstrap.app.ui.dashboard.loadOffStatus
import org.openstrap.app.ui.dashboard.offTag
import org.openstrap.app.ui.dashboard.rimLit
import org.openstrap.app.ui.components.cardNumber
import androidx.compose.ui.text.style.TextOverflow
import org.openstrap.app.ui.dashboard.LocalHeartZones

/*
 * The summary right after a workout (workouts spec 2.8): the sport and when, a hero with the one number that matters for
 * that kind of workout and three figures, then its own section ([FamilySection]), then what it earned. Under them the
 * heart rate drop, drawing as it happens, and Load against your usual. A personal best ticks and swells the hero's
 * glow once, no modal. Past workouts open their own page ([GlowWorkoutDetailScreen]), which shares the Load card and
 * the lists.
 */

/** Where Done has got to: the summary, a personal best's card, the goal moment. */
private enum class SummaryStep { Summary, Best, Moment }

private const val BLOB_STOP_A = 0.35f
private const val BLOB_STOP_B = 0.6f
private const val BLOB_STOP_C = 0.8f
private const val FIRST_BLOB_X = 0.4f
private const val FIRST_BLOB_RX = 0.82f
private const val SECOND_BLOB_X = 0.78f
private const val SECOND_BLOB_RX = 0.51f
private const val SECOND_BLOB_FADE = 0.9f

/** How much higher the hero's light reaches at the top of a personal best's swell. */
private const val SWELL_LEVEL = 0.25f

/** The drop line shows the last minute of the workout before the end. */
private const val BEFORE_S = 60L

/**
 * The summary right after [summary]'s workout ended. [calmDown] is the heart rate the strap sent since, for this
 * workout or another (then ignored), null when nothing is live (a debug preview). [liveRoute] is the route still in
 * memory, so a GPS workout whose route wasn't kept still gets its splits. [workouts] are all of them, for your usual
 * Load, the last time and this week; [usualDrop] your usual heart rate drop.
 */
@Composable
fun GlowWorkoutSummaryScreen(
    summary: PhoneSummary?,
    calmDown: CalmDown?,
    liveRoute: List<Route.Point>,
    workouts: List<WorkoutAnalysis>,
    usualDrop: Int?,
    contentPadding: PaddingValues,
    onDone: () -> Unit,
    onDelete: () -> Unit,
    onTrim: (from: Long, to: Long) -> Unit,
    onUndoTrim: () -> Unit,
    /** What it earned ([RewardsCopy.summary]), null while it's worked out. */
    rewards: SummaryRewards? = null,
    celebrations: Celebrations? = null,
) {
    val a = summary?.analysis
    // Done goes through the celebrations it earned (workouts spec 3.4): a personal best's card, then the goal moment
    // once a week, then away.
    var step by rememberSaveable { mutableStateOf(SummaryStep.Summary) }
    var settings by rememberSaveable { mutableStateOf(false) }
    val moment = rewards != null && celebrations != null && rewards.goalMet && celebrations.goalMoment && celebrations.momentWeek != rewards.weekStart
    fun next(from: SummaryStep) {
        step = when {
            from < SummaryStep.Best && rewards?.best != null -> SummaryStep.Best
            from < SummaryStep.Moment && moment -> SummaryStep.Moment.also { celebrations.markMoment(rewards.weekStart) }
            else -> { onDone(); return }
        }
    }
    val done = { next(step) }
    if (step == SummaryStep.Best && rewards?.best != null && a != null) {
        BestCardScreen(rewards.best, a.sportName, contentPadding, onContinue = done)
        return
    }
    if (step == SummaryStep.Moment && rewards != null) {
        val starts = remember(workouts) { goalWeekStarts(workouts) }
        GoalMomentScreen(rewards.week.count, rewards.week.goal, rewards.inARow, starts, contentPadding, onSettings = { settings = true }, onContinue = done)
        if (settings && celebrations != null) CelebrationsSheet(
            celebrations.goalMoment, celebrations.weeklyRecap, celebrations.setGoalMoment, celebrations.setWeeklyRecap, onDismiss = { settings = false },
        )
        return
    }
    BackHandler(onBack = done)
    var info by rememberSaveable { mutableStateOf(false) }
    val hero = remember(summary, workouts) {
        if (summary == null || a == null) null
        else summaryHero(FamilySummary.familyOf(a.workout.sportType, summary.structure), summary, Workouts.usualLoad(workouts.filter { it.workout.start != a.workout.start }.map { it.workout.trainingLoad }))
    }
    // Share opens the share screen (workouts spec 2.9) in the summary's place; close comes back here.
    var sharing by rememberSaveable { mutableStateOf(false) }
    if (sharing && summary != null && hero != null) {
        WorkoutShareScreen(summary, hero, rewards?.earned.orEmpty(), liveRoute, contentPadding, onClose = { sharing = false })
        return
    }
    val trim = summary?.let { rememberWorkoutTrim(it, onTrim, onUndoTrim) }
    // The summary is lit by its family's blend at a size by effort (Matrix board), so both are known before the page.
    val family = remember(summary) {
        summary?.let { FamilySummary.familyOf(it.analysis.workout.sportType, it.structure) } ?: Workouts.Family.Distance
    }
    val effort = remember(summary, workouts) {
        if (summary == null) 0f else Workouts.effort(
            summary.analysis.workout.trainingLoad,
            Workouts.usualLoad(workouts.filter { it.workout.start != summary.analysis.workout.start }.map { it.workout.trainingLoad }),
        )
    }
    // A personal best swells the light once.
    val swell = remember { Animatable(0f) }
    // Share and Done stay on the bottom edge; Delete waits at the end of the page. Nothing to share until the workout
    // has loaded.
    WorkoutPage(
        null, contentPadding,
        pinned = {
            if (a != null && hero != null) SummaryButtons(onShare = { sharing = true }, onDone = done)
            else WorkoutButton(stringResource(R.string.glow_done), Color.White, Color.Black, onDone)
        },
        effortLight = { effort + SWELL_LEVEL * swell.value } to familyBlend(family),
    ) {
        // The sport, then when, with Trim beside them.
        Row(Modifier.padding(start = 4.dp, top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                val title = a?.sportName ?: stringResource(R.string.glow_workout_untitled)
                Text(title, color = Color.White, style = GlowType.pageTitle, maxLines = 1)
                if (a != null) Text(
                    FamilySummary.whenLine(a.shownStart, System.currentTimeMillis() / 1000), color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA),
                    style = GlowType.secondary, modifier = Modifier.padding(top = 2.dp),
                )
            }
            // Trim beside the title: the summary has no heart rate card of its own to carry it, as a past workout's page
            // has. Done is only on the bottom edge, beside Share.
            if (trim != null) {
                IconPill(stringResource(R.string.glow_trim), trim.open, icon = AppIcons.Scissors, height = 40.dp, style = TopPillText)
            }
        }
        if (summary == null || a == null || trim == null) {
            PageNote(stringResource(R.string.glow_workout_loading))
            return@WorkoutPage
        }
        val w = a.workout
        val others = remember(workouts, w.start) { workouts.filter { it.workout.start != w.start }.sortedByDescending { it.workout.start } }
        val usualLoad = remember(others) { Workouts.usualLoad(others.map { it.workout.trainingLoad }) }
        val earned = rewards?.earned.orEmpty()
        // A personal best ticks once and swells the light once; with animations off, only the tick.
        val haptics = rememberHaptics()
        val animationsOn = rememberAnimationsOn()
        var swelled by rememberSaveable(w.start) { mutableStateOf(false) }
        val anyBest = earned.any { it.kind == EarnedLine.Kind.Best }
        LaunchedEffect(anyBest) {
            if (!anyBest || swelled) return@LaunchedEffect
            swelled = true
            haptics.perform(HapticEvent.PersonalBest)
            if (animationsOn) {
                swell.animateTo(1f, tween(450))
                swell.animateTo(0f, tween(900))
            }
        }
        SummaryHeroCard(
            hero ?: summaryHero(family, summary, usualLoad), familyInk(family), effort,
            title = a.sportName, range = Ranges.times(a.shownStart, a.end),
        )
        // What was done to the times, under the hero that says them.
        TrimNote(summary, trim.undo, Modifier.padding(horizontal = 4.dp), onCard = false)
        val weekStarts = remember(workouts) { goalWeekStarts(workouts) }
        FamilySection(family, summary, liveRoute, others, weekStarts, week = rewards?.week)
        EarnedChips(earned)
        // The drop only with heart rate to show: recorded in the workout, or arriving live after it.
        val live = calmDown?.takeIf { it.start == w.start }
        val dropShown = summary.recorded.isNotEmpty() || !live?.readings.isNullOrEmpty()
        if (dropShown) CalmDownHero(summary, live, usualDrop, effort, 0f) { info = true }
        WorkoutLoadCard(a, usualLoad)
        DeleteWorkout(stringResource(R.string.glow_workout_delete_phone), onDelete)
    }
    if (info) InfoSheet(
        Info(stringResource(R.string.glow_summary_drop_title), SummaryCopy.DROP_MEANING, emptyList(), "", "", ""),
        MetricColors.heart, null, null,
        onDismiss = { info = false },
    )
    trim?.let { TrimSheet(it) }
}

/**
 * The summary's hero (workouts spec 2.8): a glow in the workout's [color] lit from below as high as its [effort], the
 * one number in dots, then a sentence or three figures in text.
 */
@Composable
internal fun SummaryHeroCard(
    hero: SummaryHero, color: Color, effort: Float,
    /** The family's title and the time range, on the card's title row (board SummaryRun); null on pages that head themselves. */
    title: String? = null, range: String? = null,
) {
    // Tinted glass of the family's blend, its tint as strong as the effort (Matrix board: an easy hero is 20% where a hard one is
    // 42%); the light behind the screen does the size ([effortLight]). The swell is drawn there, not here.
    val style = remember(color, effort) { heroStyle(color, LightAngle.Below, rim = 0.5f + 0.5f * effort.coerceIn(0f, 1f)) }
    GlowCard(style, Modifier.fillMaxWidth()) {
        Column {
            if (title != null) GlowHeader(title, trailing = range)
            hero.sentence?.let { HeroSentence(it, Modifier) }
            HeroValue(hero.value, hero.unit, Modifier.padding(top = if (hero.sentence != null || title != null) 14.dp else 0.dp))
            hero.line?.let { Text(it, color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary, modifier = Modifier.padding(top = 6.dp)) }
            if (hero.figures.isNotEmpty()) {
                Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.14f)))
                DetailFigures(hero.figures, Modifier.padding(top = 12.dp), valueStyle = GlowType.rowValue, missing = null, gap = 0.dp)
            }
        }
    }
}

/**
 * The calm down, the hero right after a workout (spec 6.3): a level glow in heart red lit from below as high as the
 * workout's [effort] (spec 2.1, Levels), "Heart rate drop" with its (i) ([onInfo]), what the drop says
 * ([SummaryCopy.calmDown]), the heart rate now in dots while it's live, and the line of the last minute and the two
 * after the end, growing as readings arrive. [swell] lifts the light for a personal best, then lets it settle.
 */
@Composable
private fun CalmDownHero(summary: PhoneSummary, live: CalmDown?, usualDrop: Int?, effort: Float, swell: Float, onInfo: () -> Unit) {
    val a = summary.analysis
    val now by produceState(System.currentTimeMillis() / 1000, live?.until) {
        // Ticks only while the calm down is still live, for the line and the minute.
        while (live != null && value <= live.until) {
            delay(1000 - System.currentTimeMillis() % 1000)
            value = System.currentTimeMillis() / 1000
        }
    }
    val readings = remember(summary, live?.readings) {
        (summary.recorded.map { it.timestamp to it.bpm } + live?.readings.orEmpty()).distinctBy { it.first }.sortedBy { it.first }
    }
    val line: CalmDownLine = SummaryCopy.calmDown(readings, a.end, now, live != null && now <= live.until, a.maxHeartRate, usualDrop)
    val bpmNow = live?.readings?.lastOrNull()?.takeIf { now <= live.until && now - it.first <= 5 }?.second
    // A level glow is always at full: its light is the value (spec 2.1).
    GlowCard(GlowStyle.chart(MetricColors.heart).copy(dark = true), Modifier.fillMaxWidth()) {
        Column {
            GlowHeader(
                stringResource(R.string.glow_summary_drop_title), trailing = stringResource(R.string.glow_summary_drop_since, clock(a.end)),
                trailingContent = { Spacer(Modifier.width(10.dp)); InfoButton(stringResource(R.string.glow_summary_drop_about), onInfo) },
            )
            HeroSentence(line.text)
            if (bpmNow != null) {
                HeroValue(bpmNow.toString(), stringResource(R.string.glow_summary_unit_bpm_now), Modifier.padding(top = 16.dp))
            }
            val shown = readings.filter { it.first in (a.end - BEFORE_S)..(a.end + WorkoutSession.CALM_S) }
            if (shown.size >= 2) DropLine(shown, a.end, Modifier.padding(top = 16.dp))
            // The effort as a thin bar in the heart blend (the light was a heavy red band).
            val level = (effort + SWELL_LEVEL * swell).coerceIn(0f, 1f)
            Box(Modifier.padding(top = 14.dp).fillMaxWidth().height(4.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.10f))) {
                Box(Modifier.fillMaxWidth(level).fillMaxHeight().clip(CircleShape).background(Blends.Heart.brush()))
            }
        }
    }
}

/**
 * The heart rate from a minute before the [end] to two after: the workout's last minute dim, the drop bright, a solid
 * hairline mark at the end. Gaps of more than five seconds break the line.
 */
@Composable
private fun DropLine(readings: List<Pair<Long, Int>>, end: Long, modifier: Modifier = Modifier) {
    val first = end - BEFORE_S
    val span = (BEFORE_S + WorkoutSession.CALM_S).toFloat()
    val lo = readings.minOf { it.second } - 5f
    val hi = readings.maxOf { it.second } + 5f
    Column(modifier) {
        Canvas(Modifier.fillMaxWidth().height(84.dp)) {
            fun x(t: Long) = (t - first) / span * size.width
            fun y(v: Int) = size.height - (v - lo) / (hi - lo) * size.height
            val endX = x(end)
            drawLine(Color.White.copy(alpha = 0.35f), Offset(endX, 0f), Offset(endX, size.height), strokeWidth = 1.dp.toPx())
            fun stroke(part: List<Pair<Long, Int>>, color: Color) {
                if (part.size < 2) return
                val path = Path()
                part.forEachIndexed { i, (t, v) ->
                    if (i == 0 || t - part[i - 1].first > 5) path.moveTo(x(t), y(v)) else path.lineTo(x(t), y(v))
                }
                drawPath(path, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
            }
            stroke(readings.filter { it.first <= end }, Color.White.copy(alpha = 0.45f))
            stroke(readings.filter { it.first >= end }, Color.White)
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth()) {
            Box(Modifier.weight(1f)) { PageNote(stringResource(R.string.glow_summary_drop_last_minute)) }
            Box(Modifier.weight(1f)) { PageNote(stringResource(R.string.glow_summary_drop_end)) }
            Box(Modifier.weight(1f)) {
                PageNote(stringResource(R.string.glow_summary_minutes, 1))
                Box(Modifier.align(Alignment.CenterEnd)) { PageNote(stringResource(R.string.glow_summary_minutes, 2)) }
            }
        }
    }
}

/** The kept route in its zones' colours; nothing without a route that goes somewhere. */
@Composable
internal fun RouteMap(summary: PhoneSummary, modifier: Modifier = Modifier) {
    val heartZones = LocalHeartZones.current
    if (!Route.mappable(summary.route)) return
    val stretches = remember(summary, heartZones) {
        Route.byZone(summary.route, summary.history, summary.analysis.maxHeartRate, heartZones)
    }
    Box(modifier.fillMaxWidth().height(170.dp).clip(GlowLayout.TileShape).background(Raised)) {
        WorkoutMap(stretches, routeOnly = false, Modifier.fillMaxSize())
    }
}

/**
 * Load from this workout against your [usual] workout Load (spec 2.5, Picture card): unlit when usual; above or below
 * it the rim lights at the end with an "Above usual" or "Below usual" tag after the number, which stays white. Time to
 * recover under it, then the heart rate [drop] line when there is one. [onInfo] puts an (i) beside the title, for
 * training effect and EPOC.
 */
@Composable
internal fun WorkoutLoadCard(a: WorkoutAnalysis, usual: Float?, drop: String? = null, onInfo: (() -> Unit)? = null) {
    val load = a.workout.trainingLoad
    val status = loadOffStatus(load?.toFloat(), usual, endOfDay = true)
    val tag = offTag(OffWords.Usual, status)
    PictureCard(MetricColors.load, Modifier.fillMaxWidth(), rim = Rim.End.takeIf { rimLit(OffWords.Usual, status) }) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val title = stringResource(R.string.glow_summary_load_title)
            Box(Modifier.weight(1f)) { EdgeTitle(title, SummaryCopy.loadTitle(a.workout.sportType)) }
            if (onInfo != null) {
                Spacer(Modifier.width(10.dp))
                InfoButton(stringResource(R.string.glow_summary_load_about), onInfo)
            }
        }
        if (load != null) {
            val value = load.toString()
            NumberWithTag(tag, Modifier.padding(top = 14.dp)) { DotMatrixText(value, Modifier.cardNumber(value), dot = DotSize.Edge) }
            Text(
                SummaryCopy.loadLine(load, usual), color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary,
                modifier = Modifier.padding(top = 6.dp), maxLines = 2, overflow = TextOverflow.Ellipsis,
            )
        } else EdgeValue(null, null, stringResource(R.string.glow_summary_load_none), waiting = false)
        SummaryCopy.timeToRecover(a.recoveryMinutes, a.ownRecoveryMinutes, a.workout.sportType)?.let {
            Text(it, color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary, modifier = Modifier.padding(top = 4.dp))
        }
        drop?.let { Text(it, color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary, modifier = Modifier.padding(top = 4.dp)) }
    }
}

/** Time in zones: the zone bar, then each zone with time in it, hardest first. */
@Composable
internal fun ZonesList(zones: ZoneSeconds, maxHr: Int, note: String = stringResource(R.string.glow_summary_zones_max, maxHr)) {
    ListPanel(title = stringResource(R.string.glow_summary_time_in_zones), note = note) {
        ZoneBar(zones.toList(), ZONE_COLORS, Modifier.padding(top = 2.dp, bottom = 6.dp))
        var first = true
        for (z in Workouts.ZONES - 1 downTo 0) {
            if (zones[z] <= 0) continue
            // The heart in its zone's colour, so the rows key the bar above.
            ListRow(
                stringResource(R.string.glow_summary_zone, z + 1), ZONE_COLORS[z], workoutClock(zones[z].toLong()),
                stringResource(R.string.glow_summary_join, ZONE_NAMES[z], zoneRange(z, maxHr)), divider = !first,
                icon = AppIcons.Heart,
            )
            first = false
        }
    }
}

/**
 * Splits for a GPS workout (each kilometre's time and average heart rate), with the laps under them when any were
 * marked; laps alone without GPS; sets for strength. Nothing without any of them. With [markFastest] the fastest
 * kilometre is named in the note and its row lit (workouts spec 2.8). Returns whether it showed anything.
 */
@Composable
internal fun SplitsList(summary: PhoneSummary, liveRoute: List<Route.Point>, markFastest: Boolean = false): Boolean {
    val heartZones = LocalHeartZones.current
    val a = summary.analysis
    val maxHr = a.maxHeartRate
    val route = summary.route.ifEmpty { liveRoute.filter { it.t in a.shownStart..a.end } }
    val rows = remember(route, summary) { SummaryCopy.splitRows(
        Route.splits(route, summary.pauses), summary.history, route.firstOrNull()?.t, summary.distanceMeters?.takeIf { !summary.distanceFromSteps },
    ) }
    val laps = summary.laps.takeIf { it.size > 1 }.orEmpty()
    val sets = summary.sets.filter { it.end != null }
    // Each row's heart takes the zone its average heart rate sat in.
    fun dot(bpm: Int?) = bpm?.let { zoneColor(it, maxHr, heartZones) } ?: EdgeNeutral
    val fastest = if (markFastest) FamilySummary.fastestSplit(rows) else null
    when {
        rows.isNotEmpty() || laps.isNotEmpty() -> ListPanel(
            title = stringResource(if (rows.isNotEmpty()) R.string.glow_summary_splits else R.string.glow_summary_laps),
            note = if (rows.isNotEmpty()) {
                fastest?.let { stringResource(R.string.glow_summary_fastest_split, rows[it].name.lowercase()) }
                    ?: summary.distanceMeters?.let { Units.distance(it, Units.Precision.Workout) }
            } else "${laps.size}",
        ) {
            if (markFastest) SplitTimeBars(rows, fastest) else rows.forEachIndexed { i, r ->
                ListRow(
                    r.name, dot(r.averageBpm), r.time,
                    if (i == fastest) {
                        r.averageBpm?.let { stringResource(R.string.glow_summary_split_fastest_bpm, it) }
                            ?: stringResource(R.string.glow_summary_split_fastest)
                    } else r.averageBpm?.let { stringResource(R.string.glow_summary_split_average, it) },
                    divider = i > 0, icon = AppIcons.Heart,
                )
            }
            if (rows.isNotEmpty() && laps.isNotEmpty()) WorkoutDayHeader(stringResource(R.string.glow_summary_laps))
            laps.forEachIndexed { i, lap ->
                ListRow(
                    stringResource(R.string.glow_summary_lap, lap.index + 1), dot(lap.averageHeartRate), workoutClock(lap.end - lap.start),
                    SummaryCopy.lapLine(lap.meters, lap.averageHeartRate),
                    divider = i > 0, icon = AppIcons.Heart,
                )
            }
        }
        sets.isNotEmpty() -> ListPanel(
            title = stringResource(R.string.glow_summary_sets_title),
            note = pluralStringResource(R.plurals.glow_summary_sets_done, sets.size, sets.size),
        ) {
            sets.forEachIndexed { i, set ->
                ListRow(
                    stringResource(R.string.glow_summary_set, set.index + 1), MetricColors.heart, workoutClock(set.end!! - set.start),
                    set.restEnd?.let { stringResource(R.string.glow_summary_set_rest, workoutClock(it - set.end)) },
                    divider = i > 0, icon = Score.Load.rowIcon(),
                )
            }
        }
        else -> return false
    }
    return true
}

/**
 * Each kilometre as a bar as long as its speed against the [fastest] one's (board SummaryRun): the distance's lime,
 * the fastest white and its row bold. A part kilometre at the end has its time and no bar.
 */
@Composable
private fun SplitTimeBars(rows: List<SplitRow>, fastest: Int?) {
    val seconds = rows.map { it.seconds }
    val best = rows.indices.filter { rows[it].kind == SplitRow.Kind.Whole && seconds[it] > 0 }.minOfOrNull { seconds[it] }
    val bar = Blends.Distance.chart.copy(alpha = 0.5f)
    rows.forEachIndexed { i, r ->
        val lit = i == fastest
        val text = if (lit) Color.White else Color.White.copy(alpha = GlowType.SECONDARY_ALPHA)
        val style = if (lit) GlowType.secondary.copy(fontWeight = FontWeight.SemiBold) else GlowType.secondary
        Row(Modifier.fillMaxWidth().padding(vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(r.name, color = text, style = style, maxLines = 1, modifier = Modifier.widthIn(min = 56.dp).padding(end = 8.dp))
            Box(Modifier.weight(1f).height(10.dp)) {
                if (best != null && r.kind == SplitRow.Kind.Whole && seconds[i] > 0) Box(
                    Modifier.fillMaxWidth(best.toFloat() / seconds[i]).fillMaxHeight().clip(CircleShape).background(if (lit) Color.White else bar),
                )
            }
            Text(r.time, color = Color.White, style = GlowType.secondary.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, modifier = Modifier.padding(start = 14.dp))
        }
    }
}

/** The Share and Done pair on a summary's bottom edge (boards SummaryRun to SummaryGame): Done is the way on, in white. */
@Composable
internal fun SummaryButtons(onShare: () -> Unit, onDone: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        WorkoutButton(stringResource(R.string.glow_share), Raised, Color.White, onShare, Modifier.weight(1f), icon = AppIcons.UploadSimple)
        WorkoutButton(stringResource(R.string.glow_done), Color.White, Color.Black, onDone, Modifier.weight(1f), icon = AppIcons.Tick02)
    }
}

/** The small pill beside the summary's title, Trim. */
private val TopPillText = GlowType.secondary.copy(fontWeight = FontWeight.SemiBold)


/* ------------------------------------------- Family blend and the light ------------------------------------------ */

/**
 * The blend that lights a workout of [family] (Palette and Workouts boards, decision 14). Local to the summary files until
 * the coordinator unifies it with WorkoutStyle: Distance is Steps', Mind and body the Evening's.
 */
internal fun familyBlend(family: Workouts.Family): Blend = when (family) {
    Workouts.Family.Distance -> Blends.Distance
    Workouts.Family.IndoorCardio -> Blends.IndoorCardio
    Workouts.Family.Strength -> Blends.Strength
    Workouts.Family.Intervals -> Blends.Intervals
    Workouts.Family.MindBody -> Blends.MindAndBody
    Workouts.Family.Games -> Blends.Games
}

/** [familyBlend]'s first stop, for the parts that still take one ink (they find the blend again from it). */
internal fun familyInk(family: Workouts.Family): Color = familyBlend(family).first

/** The share of a full light that a workout of no effort keeps (Matrix board, "Workout summary, easy"). */
private const val LIGHT_EASY_SIZE = 0.45f
private const val LIGHT_HARD_SIZE = 1.25f

/**
 * The summary's light by effort (Matrix board, Workout summary): the family's blend, both stops muted, over the ground.
 * An easy walk keeps a small dim light and leaves most of the screen dark, a hard HIIT floods it. [effort] is read
 * while drawing, so a personal best's swell never composes again.
 */
internal fun Modifier.effortLight(blend: Blend, effort: () -> Float): Modifier = drawBehind {
    drawRect(Ground)
    val e = effort().coerceIn(0f, 1f)
    val size = LIGHT_EASY_SIZE + (LIGHT_HARD_SIZE - LIGHT_EASY_SIZE) * e
    val w = this.size.width
    val heroY = 170.dp.toPx()
    fun blob(c: Offset, rx: Float, ry: Float, color: Color, alpha: Float) {
        val brush = Brush.radialGradient(
            0f to color.copy(alpha = alpha),
            BLOB_STOP_A to color.copy(alpha = alpha * 0.9f),
            BLOB_STOP_B to color.copy(alpha = alpha * 0.55f),
            BLOB_STOP_C to color.copy(alpha = alpha * 0.2f),
            1f to Color.Transparent,
            center = c,
            radius = rx,
        )
        scale(1f, ry / rx, pivot = c) { drawCircle(brush, rx, c) }
    }
    val first = 0.6f + 0.25f * e
    blob(
        Offset(w * FIRST_BLOB_X, heroY + 50.dp.toPx()), (w * FIRST_BLOB_RX + 120.dp.toPx()) * size, (260.dp.toPx() + 120.dp.toPx()) * size,
        blend.light, first,
    )
    // The second stop only joins once the workout was hard enough to flood.
    val second = ((e - 0.25f) / 0.5f).coerceIn(0f, 1f)
    if (second > 0f) blob(
        Offset(w * SECOND_BLOB_X, heroY + 420.dp.toPx()),
        (w * SECOND_BLOB_RX + 120.dp.toPx()) * size, (190.dp.toPx() + 120.dp.toPx()) * size,
        blend.lightSecond, first * SECOND_BLOB_FADE * second,
    )
}

