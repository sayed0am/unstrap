package org.openstrap.app.ui.glow.live

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.openstrap.app.data.Strength
import org.openstrap.app.data.WorkoutRecorder
import org.openstrap.app.data.db.AppWorkoutSet
import org.openstrap.app.data.db.ExerciseRow
import org.openstrap.app.device.ConnectionState
import org.openstrap.app.device.WorkoutState
import org.openstrap.app.ui.components.HapticEvent
import org.openstrap.app.ui.components.pressClickable
import org.openstrap.app.ui.components.rememberHaptics
import org.openstrap.app.ui.glow.DotMatrixText
import org.openstrap.app.ui.glow.draw.DotSize
import org.openstrap.app.ui.glow.GlowLayout
import org.openstrap.app.ui.glow.GlowType
import org.openstrap.app.ui.glow.MediumBody
import org.openstrap.app.ui.glow.PageButton
import org.openstrap.app.ui.glow.PageRow
import org.openstrap.app.ui.glow.PageSheet
import org.openstrap.app.ui.glow.ProgressRing
import org.openstrap.app.ui.glow.QuietPanel
import org.openstrap.app.ui.glow.Raised
import org.openstrap.app.ui.glow.MinusPlus
import org.openstrap.app.ui.glow.RoundIcon
import org.openstrap.app.ui.glow.WorkoutButton
import org.openstrap.app.ui.glow.activeSecondsState
import org.openstrap.app.ui.glow.workoutClock
import org.openstrap.app.ui.glow.dotClock
import org.openstrap.app.ui.theme.AppIcons
import org.openstrap.app.ui.theme.Blend
import org.openstrap.app.ui.theme.Blends
import org.openstrap.app.ui.theme.GlowShapes
import org.openstrap.app.data.WorkoutTarget
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.graphics.graphicsLayer
import org.openstrap.app.presentation.WorkoutCopy
import org.openstrap.app.ui.workout.ZONE_COLORS
import org.openstrap.app.ui.workout.strengthMoment
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import org.openstrap.app.R

/** Strength's rest colour, the green the floating bar and the hero rest in too. */

/**
 * Strength (workouts spec 2.4, boards StrengthSet, StrengthLog and StrengthRest), with the total small on top.
 *
 * In a set: set N of M and its exercise (tap to name or change it), the set's clock in dots on a glow in the zone's
 * colour, the sets as dots, the heart rate and zone, the sets done with their reps, weight, time and peak, and one big
 * Set done. Resting: the rest left in dots on a card lit in rest green, the heart rate and how far it dropped, its line
 * falling toward the level that ends rest sooner, what's next, +30 s and Start set N. The strap buzzes when rest ends.
 *
 * With logging on ([WorkoutState.Recording.logSets]), the log card follows each set while rest runs underneath: reps
 * and kg filled from the last set of the exercise ([Strength.prefill]), Skip or Save, and Next exercise.
 *
 * [compact] (half a phone stood like a laptop, the heart rate on the other half): each card is one row with its clock,
 * and the sets done and what's next are left out, so the buttons stay on screen.
 */
@Composable
internal fun LiveStrengthScreen(
    r: WorkoutState.Recording,
    liveHeartRate: Int?,
    history: List<Pair<Long, Int>>,
    connection: ConnectionState,
    contentPadding: PaddingValues,
    controls: LiveControls,
    exercises: List<ExerciseRow>,
    onStartSet: () -> Unit,
    onSetDone: () -> Unit,
    onExtendRest: () -> Unit,
    onExercise: (String?) -> Unit,
    onLog: (index: Int, exercise: String?, reps: Int, kg: Float) -> Unit,
    compact: Boolean = false,
) {
    val now = nowTickingState()
    val connected = connection is ConnectionState.Connected
    val bpm = liveHeartRate.takeIf { connected && !r.paused }
    ZoneTick(bpm, r)
    // Bronze into iron lights the whole workout: from below in a set, from the top in rest (boards LiveStrength, LiveStrengthRest).
    val blend = Blends.Strength
    val rest = r.resting && !r.paused
    val target = remember(r.workout.target) { WorkoutTarget.parse(r.workout.target) }
    // Read where it shows (the cards' clocks, the paused figures, the log card), so the screen holds still between
    // seconds. The rest of the screen doesn't move with the clock: [steady] is the moment without its seconds, which
    // only the clocks read, so it needs no time at all.
    val momentNow = { strengthMoment(r, now.value, bpm, history) }
    val steady = remember(r, bpm, history) { strengthMoment(r, 0, bpm, history).copy(setSeconds = 0, restLeft = null) }
    var naming by rememberSaveable { mutableStateOf(false) }
    // Sets whose log card was skipped, so it doesn't come back.
    val skipped = rememberSaveable(r.workout.start) { mutableStateListOf<Int>() }
    val unlogged = r.sets.lastOrNull { it.end != null }?.takeIf { r.logSets && it.reps == null && it.index !in skipped && !r.inSet }
    val active = activeSecondsState(r)
    val haptics = rememberHaptics()
    val tapSetDone = { haptics.perform(HapticEvent.SetDone); onSetDone() }
    LiveFrame(
        r, blend, connected, contentPadding, controls, time = { active.value }, smallTime = true, fill = true,
        lightAngle = if (rest) 90f else 270f, lightStrength = if (rest) 0.6f else 1f, lightY = if (rest) 120f else 460f,
        lap = if (!r.paused) ({ PauseSideButton(controls.onPause) }) else null,
        center = when {
            r.inSet -> ({ CenterAction(stringResource(R.string.workout_live_sets_set_done), AppIcons.Tick02, tapSetDone) })
            else -> ({
                CenterAction(stringResource(R.string.workout_live_sets_start_set, steady.setNumber), AppIcons.PlayFill, onStartSet)
            })
        },
        paused = { LivePaused(r, liveHeartRate.takeIf { connected }, history, listOf(
            momentNow().done.let { done -> done.toString() to pluralStringResource(R.plurals.workout_live_figure_sets_done, done) },
            Strength.kgLifted(r.sets)?.let { Strength.kgText(it) } to stringResource(R.string.workout_live_figure_kg_lifted),
            r.workout.maxHeartRate?.toString() to stringResource(R.string.workout_live_figure_peak_bpm),
        )) },
    ) {
        val moment = steady
        if (compact) CompactCard(r, moment, momentNow, blend, onName = { naming = true }, onAdd30 = onExtendRest) else when {
            r.inSet -> SetCard(r, moment.setNumber, { momentNow().setSeconds }, blend) { naming = true }
            r.resting -> RestCard(
                r, { momentNow().restLeft ?: 0 }, moment.done, moment.setNumber, r.exercise, moment.nextLoad, blend, onExtendRest,
            )
            else -> ReadyCard(r, moment.setNumber, blend) { naming = true }
        }
        if (!compact) {
            // The heart under the card: in a set the zone strip, in rest the drop toward the level that ends it.
            if (r.resting && !r.inSet) RestHeart(r, bpm, moment.drop, history)
            else LiveStrip(Modifier.fillMaxWidth()) { LiveZoneStrip(bpm, r.maxHeartRate, r.paused, connected, target) }
            DoneSets(r.sets, history)
        }
        Spacer(Modifier.weight(1f))
        val setsTarget = r.workout.setsTarget
        val showAllDone = !compact && !r.inSet
        if (showAllDone && setsTarget != null && moment.done >= setsTarget) {
            Text(
                pluralStringResource(R.plurals.workout_live_sets_all_done, setsTarget, setsTarget), color = Color.White.copy(alpha = 0.6f),
                style = GlowType.secondary, modifier = Modifier.fillMaxWidth(), textAlign = TextAlign.Center,
            )
        }
    }
    if (unlogged != null) LogCard(
        unlogged, r.sets, exercises, history, restLeft = { momentNow().restLeft },
        onSave = { exercise, reps, kg -> onLog(unlogged.index, exercise, reps, kg) },
        onSkip = { skipped += unlogged.index },
        onNextExercise = { skipped += unlogged.index; naming = true },
    )
    if (naming) ExerciseSheet(r.exercise, exercises.map { it.name }, onPick = { onExercise(it); naming = false }, onDismiss = { naming = false })
}

/** The set under way (board LiveStrength): set N of M and its exercise, the set's clock in the live dots, the sets as dots. */
@Composable
private fun SetCard(r: WorkoutState.Recording, number: Int, seconds: () -> Long, blend: Blend, onName: () -> Unit) {
    LiveHero(blend) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(WorkoutCopy.setOf(number, r.workout.setsTarget), color = Color.White, style = GlowType.cardTitle, maxLines = 1, modifier = Modifier.weight(1f))
            ExercisePill(r.exercise, onName)
        }
        Column(Modifier.fillMaxWidth().padding(top = 34.dp, bottom = 26.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DotClock({ dotClock(seconds()) }, DotSize.Live)
        }
        SetDots(r.sets.count { it.end != null }, r.workout.setsTarget, inSet = true, Modifier.align(Alignment.CenterHorizontally))
        Spacer(Modifier.height(4.dp))
    }
}

/**
 * The set, the rest or the next set on one row (half a folded phone): what it is and its exercise, and the set's clock
 * or the rest left in dots, on tinted glass; a rest also has +30 s beside it, as the full card does.
 */
@Composable
private fun CompactCard(
    r: WorkoutState.Recording, moment: org.openstrap.app.ui.workout.StrengthMoment, now: () -> org.openstrap.app.ui.workout.StrengthMoment,
    blend: Blend, onName: () -> Unit, onAdd30: () -> Unit,
) {
    val (title, clock) = when {
        r.inSet -> WorkoutCopy.setOf(moment.setNumber, r.workout.setsTarget) to { dotClock(now().setSeconds) }
        r.resting -> {
            val after = WorkoutCopy.setOf(moment.done, r.workout.setsTarget).lowercase()
            stringResource(R.string.workout_live_sets_rest_after, after) to { dotClock((now().restLeft ?: 0).coerceAtLeast(0)) }
        }
        else -> {
            val title = if (moment.setNumber == 1) R.string.workout_live_sets_ready_first else R.string.workout_live_sets_rest_over
            stringResource(title) to null
        }
    }
    LiveHero(blend) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, color = Color.White, style = GlowType.cardTitle, maxLines = 1)
                ExercisePill(r.exercise, onName)
            }
            if (clock != null) DotClock(clock, DotSize.Detail)
            if (r.resting && !r.inSet) {
                Spacer(Modifier.width(12.dp))
                Add30Pill(onAdd30)
            }
        }
    }
}

/** Before the first set, or once rest is over: which set is next and its exercise. */
@Composable
private fun ReadyCard(r: WorkoutState.Recording, number: Int, blend: Blend, onName: () -> Unit) {
    LiveHero(blend) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(if (number == 1) R.string.workout_live_sets_ready_first else R.string.workout_live_sets_rest_over),
                color = Color.White, style = GlowType.cardTitle, maxLines = 1, modifier = Modifier.weight(1f),
            )
            ExercisePill(r.exercise, onName)
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.workout_live_sets_next_tap_start, WorkoutCopy.setOf(number, r.workout.setsTarget)),
            color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary,
        )
        Spacer(Modifier.height(14.dp))
        SetDots(r.sets.count { it.end != null }, r.workout.setsTarget, inSet = false, Modifier.align(Alignment.CenterHorizontally))
    }
}

/**
 * Rest (board LiveStrengthRest): bronze tinted glass with what it follows, the time left in the live dots, what's next
 * with +30 s at the foot, and a bar along it that empties as the rest runs down. The heart's drop is the card under it.
 */
@Composable
private fun RestCard(
    r: WorkoutState.Recording,
    left: () -> Long,
    done: Int,
    number: Int,
    exercise: String?,
    load: String?,
    blend: Blend,
    onAdd30: () -> Unit,
) {
    LiveHero(blend) {
        Row {
            Text(
                stringResource(R.string.workout_live_sets_rest_title), color = Color.White, style = GlowType.cardTitle, maxLines = 1,
                modifier = Modifier.weight(1f),
            )
            Text(
                stringResource(R.string.workout_live_sets_after, WorkoutCopy.setOf(done, r.workout.setsTarget).lowercase()),
                color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary,
            )
        }
        Column(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 22.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            DotClock({ dotClock(left().coerceAtLeast(0)) }, DotSize.Live)
        }
        val total = ((r.restUntil ?: 0) - (r.sets.lastOrNull()?.end ?: r.restUntil ?: 0)).coerceAtLeast(1)
        val track = Color.White.copy(alpha = 0.12f)
        Box(Modifier.fillMaxWidth().height(4.dp).clip(GlowShapes.Pill).background(track)) {
            val drain = Modifier.fillMaxHeight().fillMaxWidth().graphicsLayer {
                scaleX = (left().toFloat() / total).coerceIn(0f, 1f)
                transformOrigin = androidx.compose.ui.graphics.TransformOrigin(pivotFractionX = 0f, pivotFractionY = 0.5f)
            }
            Box(drain.background(blend.chart))
        }
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                exercise?.lowercase()?.let { e -> listOfNotNull(e, load).joinToString(" ") }
                    ?.let { stringResource(R.string.workout_live_sets_next_set_exercise, number, it) }
                    ?: stringResource(R.string.workout_live_sets_next_set, number),
                color = Color.White.copy(alpha = 0.85f), style = GlowType.secondary, maxLines = 1, modifier = Modifier.weight(1f),
            )
            Spacer(Modifier.width(10.dp))
            Add30Pill(onAdd30)
        }
    }
}

/** +30 s: a small white 14% pill in the rest card. */
@Composable
private fun Add30Pill(onClick: () -> Unit) {
    Text(
        stringResource(R.string.workout_live_sets_add_30_s), color = Color.White, style = MediumBody, maxLines = 1,
        modifier = Modifier.clip(GlowShapes.Pill).background(Color.White.copy(alpha = 0.14f)).pressClickable(onClick, GlowShapes.Pill)
            .padding(horizontal = 16.dp, vertical = 10.dp),
    )
}

/** Rest's heart (clear glass): the bpm in dots with how far it dropped, the line falling toward the level that ends rest. */
@Composable
private fun RestHeart(r: WorkoutState.Recording, bpm: Int?, drop: Int?, history: List<Pair<Long, Int>>) {
    val rule = Strength.Rest.parse(r.workout.rest) ?: Strength.Rest.DEFAULT
    LiveStrip(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.Bottom) {
            if (bpm != null) {
                DotMatrixText(bpm.toString(), dot = DotSize.Edge)
                Spacer(Modifier.width(8.dp))
                Text(
                    drop?.takeIf { it > 0 }?.let { stringResource(R.string.workout_live_bpm_down, bpm, it) }
                        ?: stringResource(R.string.workout_live_bpm, bpm),
                    color = Color.White, style = GlowType.secondary,
                )
            } else Text(WorkoutCopy.noHeartRate(r.paused), color = Color.White, style = GlowType.sentence)
        }
        val since = r.sets.lastOrNull()?.end
        if (since != null) FallingHeart(remember(history, since) { history.filter { it.first >= since - 10 } }, rule.belowBpm)
        Text(WorkoutCopy.restRule(rule.belowBpm), color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary)
    }
}

/**
 * The rest left in dots inside a ring in the family's bronze that empties as it runs down. [left] is read in draw and
 * its text. The rest card itself now counts down on a bar ([RestCard]); the ring is kept for the Live parts gallery.
 */
@Composable
internal fun RestRing(left: () -> Long, total: Long) {
    Box(Modifier.size(220.dp), contentAlignment = Alignment.Center) {
        ProgressRing({ left().toFloat() / total }, Blends.Strength.chart, Modifier.fillMaxSize().padding(4.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            DotClock({ dotClock(left()) }, DotSize.Detail)
            Spacer(Modifier.height(10.dp))
            Text(stringResource(R.string.workout_live_unit_left), color = Color.White.copy(alpha = 0.75f), style = GlowType.body)
        }
    }
}

/** A clock in dots, its own scope: the second it reads composes only it again, not the card around it. */
@Composable
private fun DotClock(text: () -> String, dot: DotSize) = DotMatrixText(text(), dot = dot)

/** The heart rate since the set ended, falling, with the level that ends rest sooner as a thin solid rule. */
@Composable
private fun FallingHeart(readings: List<Pair<Long, Int>>, level: Int?) {
    if (readings.size < 2) return
    val lo = minOf(readings.minOf { it.second }, level ?: Int.MAX_VALUE) - 5
    val hi = readings.maxOf { it.second } + 5
    val from = readings.first().first
    val to = maxOf(readings.last().first, from + 60)
    val described = stringResource(R.string.workout_live_sets_falling_description)
    Canvas(Modifier.fillMaxWidth().height(56.dp).semantics { contentDescription = described }) {
        fun x(t: Long) = (t - from).toFloat() / (to - from) * size.width
        fun y(v: Int) = size.height - (v - lo).toFloat() / (hi - lo) * size.height
        if (level != null) drawLine(Color.White.copy(alpha = 0.35f), Offset(0f, y(level)), Offset(size.width, y(level)), 1.dp.toPx())
        val path = Path().apply {
            readings.forEachIndexed { i, (t, v) -> if (i == 0) moveTo(x(t), y(v)) else lineTo(x(t), y(v)) }
        }
        drawPath(path, Blends.Strength.chart, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
    }
}

/** The sets as dots: done ones white, the one under way white too, those to go dark. */
@Composable
private fun SetDots(done: Int, target: Int?, inSet: Boolean, modifier: Modifier = Modifier) {
    val total = maxOf(target ?: 0, done + if (inSet) 1 else 0)
    if (total == 0) return
    val described = target?.let {
        pluralStringResource(R.plurals.workout_live_sets_dots_description_to_go, done, done, (it - done).coerceAtLeast(0))
    }
        ?: pluralStringResource(R.plurals.workout_live_sets_dots_description, done, done)
    Row(
        modifier.semantics { contentDescription = described },
        horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically,
    ) {
        // Done sets are white dots, the one under way a white pill, those to go a ring.
        repeat(total) { i ->
            val now = inSet && i == done
            when {
                now -> Box(Modifier.width(24.dp).height(12.dp).clip(GlowShapes.Pill).background(Color.White))
                i < done -> Box(Modifier.size(12.dp).clip(CircleShape).background(Color.White))
                else -> Box(Modifier.size(12.dp).clip(CircleShape).border(1.5.dp, Color.White.copy(alpha = 0.5f), CircleShape))
            }
        }
    }
}

/** The exercise as a pill to tap (its name and a chevron), or "Name it" while the sets are only numbered. */
@Composable
private fun ExercisePill(name: String?, onClick: () -> Unit) {
    val shape = GlowShapes.Pill
    Row(
        Modifier.clip(shape).background(Color.White.copy(alpha = 0.12f)).pressClickable(onClick, shape)
            .padding(start = 14.dp, end = 10.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            name ?: stringResource(R.string.workout_live_sets_name_it), color = Color.White, style = MediumBody, maxLines = 1,
            overflow = TextOverflow.Ellipsis, modifier = Modifier.widthIn(max = 150.dp),
        )
        Icon(AppIcons.CaretRight, null, tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.padding(start = 2.dp).size(14.dp))
    }
}

/** The sets done, newest first: "Set 2" and "10 × 60 kg, 0:51, peak 148". */
@Composable
private fun DoneSets(sets: List<AppWorkoutSet>, history: List<Pair<Long, Int>>) {
    val done = sets.filter { it.end != null }.asReversed()
    if (done.isEmpty()) return
    // The peaks scan the readings, so they're worked out when a set or the readings change, not on every pass.
    val lines = remember(done, history) {
        done.take(4).map { s -> WorkoutCopy.setLine(s, Strength.peak(history, s.start, s.end!!)) }
    }
    LiveStrip(
        Modifier.fillMaxWidth(), padding = PaddingValues(horizontal = 18.dp, vertical = 4.dp), verticalArrangement = Arrangement.Top,
    ) {
        done.take(4).forEachIndexed { i, s ->
            if (i > 0) SetRule()
            Row(Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Text(
                    stringResource(R.string.workout_live_sets_row, s.index + 1), color = Color.White, style = GlowType.body,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    lines[i], color = Color.White.copy(alpha = 0.72f), style = GlowType.body, maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The line between the rows in the sets card. */
@Composable
private fun SetRule() = Box(Modifier.fillMaxWidth().height(1.dp).background(Color.White.copy(alpha = 0.12f)))

/**
 * The log card after a set (board StrengthLog): the set done with its time and peak, its exercise, reps and kg
 * steppers filled from the last set of that exercise, Skip or Save, and Next exercise. Rest runs underneath.
 */
@Composable
private fun LogCard(
    set: AppWorkoutSet, sets: List<AppWorkoutSet>, exercises: List<ExerciseRow>, history: List<Pair<Long, Int>>, restLeft: () -> Long?,
    onSave: (exercise: String?, reps: Int, kg: Float) -> Unit, onSkip: () -> Unit, onNextExercise: () -> Unit,
) {
    val row = exercises.firstOrNull { it.name.equals(set.exercise, ignoreCase = true) }
    val (fillReps, fillKg) = remember(set.index) { Strength.prefill(set.exercise, sets, row?.lastReps, row?.lastKg) }
    var reps by rememberSaveable(set.index) { mutableStateOf(fillReps) }
    var kg by rememberSaveable(set.index) { mutableStateOf(fillKg) }
    val peak = Strength.peak(history, set.start, set.end ?: set.start)
    val time = workoutClock((set.end ?: set.start) - set.start)
    val left = restLeft()?.let { workoutClock(it.coerceAtLeast(0)) }
    val note = when {
        peak != null && left != null -> stringResource(R.string.workout_live_log_note_peak_rest, time, peak, left)
        peak != null -> stringResource(R.string.workout_live_log_note_peak, time, peak)
        left != null -> stringResource(R.string.workout_live_log_note_rest, time, left)
        else -> stringResource(R.string.workout_live_log_note, time)
    }
    PageSheet(onSkip, stringResource(R.string.workout_live_log_title, set.index + 1), note = note) {
        Text(set.exercise ?: stringResource(R.string.workout_live_log_numbered_set), color = Color.White, style = GlowType.body)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LogStepper(
                stringResource(R.string.workout_live_log_reps), reps.toString(), stringResource(R.string.workout_live_log_fewer_reps),
                stringResource(R.string.workout_live_log_more_reps), Modifier.weight(1f),
                { reps = Strength.stepReps(reps, -1) }, { reps = Strength.stepReps(reps, 1) },
            )
            LogStepper(
                stringResource(R.string.workout_live_log_kg), Strength.kgText(kg), stringResource(R.string.workout_live_log_less_weight),
                stringResource(R.string.workout_live_log_more_weight), Modifier.weight(1f),
                { kg = Strength.stepKg(kg, -1) }, { kg = Strength.stepKg(kg, 1) },
            )
        }
        Text(
            stringResource(R.string.workout_live_log_filled), color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA),
            style = GlowType.secondary,
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PageButton(stringResource(R.string.workout_live_log_skip), onSkip, Modifier.weight(1f), icon = AppIcons.X)
            PageButton(
                stringResource(R.string.workout_live_log_save), { onSave(set.exercise, reps, kg) }, Modifier.weight(1f), primary = true,
                icon = AppIcons.Tick02,
            )
        }
        PageButton(stringResource(R.string.workout_live_log_next_exercise), onNextExercise, Modifier.fillMaxWidth(), icon = AppIcons.Plus)
    }
}

/** A log card stepper: its label, the value large, and minus and plus. */
@Composable
private fun LogStepper(label: String, value: String, less: String, more: String, modifier: Modifier, onLess: () -> Unit, onMore: () -> Unit) {
    Column(modifier.clip(GlowLayout.CardShape).background(org.openstrap.app.ui.glow.ListSurface).padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, color = Color.White.copy(alpha = GlowType.SECONDARY_ALPHA), style = GlowType.secondary)
        MinusPlus(less, more, onLess, onMore) {
            Text(value, color = Color.White, style = GlowType.pageTitle, textAlign = TextAlign.Center)
        }
    }
}

/**
 * Naming the exercise: a field to type one, then yours (last used first) and the common lifts; or no name, so the sets
 * are just numbered.
 */
@Composable
private fun ExerciseSheet(current: String?, used: List<String>, onPick: (String?) -> Unit, onDismiss: () -> Unit) {
    var typed by rememberSaveable { mutableStateOf("") }
    PageSheet(onDismiss, stringResource(R.string.workout_live_exercise_title), note = stringResource(R.string.workout_live_exercise_note)) {
        BasicTextField(
            typed, { typed = it }, Modifier.fillMaxWidth(), singleLine = true,
            textStyle = GlowType.body.copy(color = Color.White), cursorBrush = SolidColor(Color.White),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { if (typed.isNotBlank()) onPick(typed) }),
            decorationBox = { field ->
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(GlowLayout.TileShape).background(Raised).padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Box(Modifier.weight(1f)) {
                        if (typed.isEmpty()) Text(
                            stringResource(R.string.workout_live_exercise_type), color = Color.White.copy(alpha = GlowType.CAPTION_ALPHA),
                            style = GlowType.body,
                        )
                        field()
                    }
                    if (typed.isNotBlank()) Icon(
                        AppIcons.Tick02, stringResource(R.string.workout_live_exercise_use), tint = Color.White,
                        // 48 dp to tap, the mark drawn 20 dp as before, and moved out so it stays where it was.
                        modifier = Modifier.offset(x = 10.dp).size(48.dp).clip(CircleShape)
                            .pressClickable({ onPick(typed) }, CircleShape).padding(14.dp),
                    )
                }
            },
        )
        val names = Strength.exerciseList(used).filter { typed.isBlank() || it.contains(typed.trim(), ignoreCase = true) }
        val nowLabel = stringResource(R.string.workout_live_exercise_now)
        Column {
            names.forEachIndexed { i, name ->
                val value = if (name.equals(current, ignoreCase = true)) nowLabel else null
                PageRow(name, value = value, divider = i > 0, onClick = { onPick(name) })
            }
        }
        if (current != null) PageButton(
            stringResource(R.string.workout_live_exercise_no_name), { onPick(null) }, Modifier.fillMaxWidth(), icon = AppIcons.X,
        )
    }
}
